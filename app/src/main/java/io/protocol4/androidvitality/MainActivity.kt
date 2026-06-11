package io.protocol4.androidvitality

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.protocol4.androidvitality.resource.ResourceLoggingService
import io.protocol4.androidvitality.resource.ResourceMonitorScreen
import io.protocol4.androidvitality.ui.theme.AndroidVitalityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request highest possible refresh rate (up to 360Hz+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = display
            if (display != null) {
                val modes = display.supportedModes
                val maxRefreshMode = modes.maxByOrNull { it.refreshRate }
                if (maxRefreshMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxRefreshMode.modeId
                    window.attributes = params
                }
            }
        }

        // Start background logging service
        startForegroundService(Intent(this, ResourceLoggingService::class.java))

        setContent {
            AndroidVitalityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ResourceMonitorScreen()
                }
            }
        }
    }
}
