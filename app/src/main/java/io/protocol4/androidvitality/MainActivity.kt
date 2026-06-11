package io.protocol4.androidvitality

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.protocol4.androidvitality.resource.ResourceMonitorScreen
import io.protocol4.androidvitality.ui.theme.AndroidVitalityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request highest possible refresh rate (up to the display's maximum)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.let { display ->
                display.supportedModes
                    .maxByOrNull { it.refreshRate }
                    ?.let { maxRefreshMode ->
                        window.attributes = window.attributes.apply {
                            preferredDisplayModeId = maxRefreshMode.modeId
                        }
                    }
            }
        }

        setContent {
            AndroidVitalityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ResourceMonitorScreen()
                }
            }
        }
    }
}