package com.nancy.privacyanalyzerr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nancy.privacyanalyzerr.ui.theme.PrivacyAnalyzerrTheme

class MainActivity : ComponentActivity() {

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
                PrivacyScreen(onStartClick = { checkAndStartService() })
            }
        }
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
        startForegroundService(intent)
    }
}

@Composable
fun PrivacyScreen(onStartClick: () -> Unit) {
    var statusText by remember { mutableStateOf("Press button to start monitoring") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Privacy Analyzer", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = statusText)

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            statusText = "Monitoring started..."
            onStartClick()
        }) {
            Text("Start Monitoring")
        }
    }
}