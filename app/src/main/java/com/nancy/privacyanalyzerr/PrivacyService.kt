package com.nancy.privacyanalyzerr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.IBinder
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.concurrent.thread

class PrivacyService : Service() {

    private var tflite: Interpreter? = null
    
    private var highRiskApps = ArrayList<String>()
    private var mediumRiskApps = ArrayList<String>()
    private var lowRiskApps = ArrayList<String>()

    private var isAnalyzing = false

    private val requestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            sendResultsToActivity()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val filter = IntentFilter("com.nancy.privacyanalyzerr.REQUEST_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(requestReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(requestReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        }

        try {
            tflite = Interpreter(loadModelFile())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startAnalysisTask() {
        if (isAnalyzing) return
        isAnalyzing = true
        
        thread {
            try {
                analyzeInstalledApps()
                
                val resultString = "High: ${highRiskApps.size} | Med: ${mediumRiskApps.size} | Low: ${lowRiskApps.size}"
                val notification = buildNotification(resultString)
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(1, notification)
                
                sendResultsToActivity()
            } finally {
                isAnalyzing = false
            }
        }
    }

    private fun sendResultsToActivity() {
        val intent = Intent("com.nancy.privacyanalyzerr.UPDATE_RESULTS")
        intent.putStringArrayListExtra("high_list", ArrayList(highRiskApps))
        intent.putStringArrayListExtra("medium_list", ArrayList(mediumRiskApps))
        intent.putStringArrayListExtra("low_list", ArrayList(lowRiskApps))
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assets.openFd("privacy_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Analyzing apps...")
        startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        startAnalysisTask()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun analyzeInstalledApps() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val tempHigh = ArrayList<String>()
        val tempMed = ArrayList<String>()
        val tempLow = ArrayList<String>()

        for (app in apps) {
            try {
                val appName = pm.getApplicationLabel(app).toString()
                val packageInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                val permissions = packageInfo.requestedPermissions ?: continue

                val numPermissions = permissions.size.toFloat()
                val hasLocation = if (permissions.any { it.contains("LOCATION") }) 1f else 0f
                val hasCamera = if (permissions.any { it.contains("CAMERA") }) 1f else 0f
                val hasMicrophone = if (permissions.any { it.contains("RECORD_AUDIO") }) 1f else 0f
                val hasContacts = if (permissions.any { it.contains("CONTACTS") }) 1f else 0f
                val hasSms = if (permissions.any { it.contains("SMS") }) 1f else 0f
                val hasBackground = if (permissions.any { it.contains("BACKGROUND") }) 1f else 0f
                val hasStorage = if (permissions.any { it.contains("STORAGE") }) 1f else 0f

                val inputVal = floatArrayOf(
                    numPermissions, hasLocation, hasCamera, hasMicrophone,
                    hasContacts, hasSms, hasBackground, hasStorage
                )
                
                val outputVal = Array(1) { FloatArray(1) }
                
                var riskScore: Int
                if (tflite != null) {
                    try {
                        tflite?.run(inputVal, outputVal)
                        val score = outputVal[0][0]
                        riskScore = when {
                            score > 0.7f -> 2
                            score > 0.3f -> 1
                            else -> 0
                        }
                    } catch (e: Exception) {
                        riskScore = calculateHeuristicRisk(numPermissions, hasLocation, hasCamera, hasMicrophone, hasContacts, hasSms)
                    }
                } else {
                    riskScore = calculateHeuristicRisk(numPermissions, hasLocation, hasCamera, hasMicrophone, hasContacts, hasSms)
                }

                val reason = getRiskReason(permissions)
                val appData = "$appName|$reason"

                when (riskScore) {
                    2 -> tempHigh.add(appData)
                    1 -> tempMed.add(appData)
                    else -> tempLow.add(appData)
                }

            } catch (e: Exception) {
                continue
            }
        }
        
        highRiskApps = tempHigh
        mediumRiskApps = tempMed
        lowRiskApps = tempLow
    }

    private fun getRiskReason(permissions: Array<String>): String {
        val sensitive = mutableListOf<String>()
        if (permissions.any { it.contains("LOCATION") }) sensitive.add("Location")
        if (permissions.any { it.contains("CAMERA") }) sensitive.add("Camera")
        if (permissions.any { it.contains("RECORD_AUDIO") }) sensitive.add("Microphone")
        if (permissions.any { it.contains("CONTACTS") }) sensitive.add("Contacts")
        if (permissions.any { it.contains("SMS") }) sensitive.add("SMS")
        if (permissions.any { it.contains("BACKGROUND") }) sensitive.add("Background")
        
        return if (sensitive.isEmpty()) "Minimal sensitive permissions" 
               else "Accesses: ${sensitive.joinToString(", ")}"
    }

    private fun calculateHeuristicRisk(
        numPermissions: Float,
        hasLocation: Float,
        hasCamera: Float,
        hasMicrophone: Float,
        hasContacts: Float,
        hasSms: Float
    ): Int {
        return when {
            numPermissions > 8 && hasLocation > 0 && hasCamera > 0 && hasMicrophone > 0 -> 2
            numPermissions > 4 && (hasLocation > 0 || hasCamera > 0 || hasContacts > 0 || hasSms > 0) -> 1
            else -> 0
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "privacy_channel",
            "Privacy Analyzer",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(result: String): Notification {
        return Notification.Builder(this, "privacy_channel")
            .setContentTitle("Privacy Analyzer")
            .setContentText(result)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(requestReceiver)
        } catch (e: Exception) {}
        tflite?.close()
        super.onDestroy()
    }
}