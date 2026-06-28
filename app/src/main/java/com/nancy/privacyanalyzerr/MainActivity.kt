package com.nancy.privacyanalyzerr

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nancy.privacyanalyzerr.ui.theme.PrivacyAnalyzerrTheme

class MainActivity : ComponentActivity() {

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val high = intent?.getStringArrayListExtra("high_list") ?: arrayListOf()
            val medium = intent?.getStringArrayListExtra("medium_list") ?: arrayListOf()
            val low = intent?.getStringArrayListExtra("low_list") ?: arrayListOf()
            
            highRiskApps.value = high
            mediumRiskApps.value = medium
            lowRiskApps.value = low
            isMonitoring.value = true
        }
    }

    private val highRiskApps = mutableStateOf<List<String>>(emptyList())
    private val mediumRiskApps = mutableStateOf<List<String>>(emptyList())
    private val lowRiskApps = mutableStateOf<List<String>>(emptyList())
    private val isMonitoring = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startPrivacyService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrivacyAnalyzerrTheme {
                DashboardScreen(
                    highApps = highRiskApps.value,
                    mediumApps = mediumRiskApps.value,
                    lowApps = lowRiskApps.value,
                    monitoring = isMonitoring.value,
                    onStartClick = { checkAndStartService() },
                    onStopClick = { stopPrivacyService() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.nancy.privacyanalyzerr.UPDATE_RESULTS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(resultReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(resultReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        }
        
        sendBroadcast(Intent("com.nancy.privacyanalyzerr.REQUEST_UPDATE").setPackage(packageName))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(resultReceiver)
    }

    private fun checkAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startPrivacyService()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startPrivacyService()
        }
    }

    private fun startPrivacyService() {
        val intent = Intent(this, PrivacyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isMonitoring.value = true
    }

    private fun stopPrivacyService() {
        val intent = Intent(this, PrivacyService::class.java)
        stopService(intent)
        isMonitoring.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    highApps: List<String>,
    mediumApps: List<String>,
    lowApps: List<String>,
    monitoring: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    var selectedList by remember { mutableStateOf<Pair<String, List<String>>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (monitoring) "System Status: Active Monitoring" else "System Status: Idle",
                style = MaterialTheme.typography.bodyLarge,
                color = if (monitoring) Color(0xFF4CAF50) else Color.Gray,
                fontWeight = FontWeight.Bold
            )

            val currentSelected = selectedList
            if (currentSelected != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Viewing: ${currentSelected.first}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { selectedList = null }) {
                        Text("Back to Summary")
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(currentSelected.second) { appData ->
                            val parts = appData.split("|")
                            val name = parts.getOrNull(0) ?: "Unknown"
                            val reason = parts.getOrNull(1) ?: "No reason provided"
                            
                            ListItem(
                                headlineContent = { Text(name, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(reason, style = MaterialTheme.typography.bodySmall) },
                                leadingContent = { 
                                    Icon(
                                        imageVector = Icons.Default.Shield, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(24.dp),
                                        tint = if (currentSelected.first.contains("High")) Color.Red else if (currentSelected.first.contains("Medium")) Color(0xFFFFB300) else Color(0xFF43A047)
                                    ) 
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))

                RiskCard(
                    title = "High Risk Apps",
                    count = highApps.size,
                    color = Color(0xFFE53935),
                    icon = Icons.Default.Warning,
                    onClick = { selectedList = "High Risk" to highApps }
                )

                Spacer(modifier = Modifier.height(16.dp))

                RiskCard(
                    title = "Medium Risk Apps",
                    count = mediumApps.size,
                    color = Color(0xFFFFB300),
                    icon = Icons.Default.Info,
                    onClick = { selectedList = "Medium Risk" to mediumApps }
                )

                Spacer(modifier = Modifier.height(16.dp))

                RiskCard(
                    title = "Safe Apps",
                    count = lowApps.size,
                    color = Color(0xFF43A047),
                    icon = Icons.Default.Shield,
                    onClick = { selectedList = "Safe Apps" to lowApps }
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!monitoring) {
                Button(
                    onClick = onStartClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("START PRIVACY SCAN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onStopClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("STOP MONITORING", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Powered by ML Engine — IIT Jammu",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RiskCard(title: String, count: Int, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = color)
                Text(
                    text = "$count Applications detected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Tap to view list & reasons",
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}