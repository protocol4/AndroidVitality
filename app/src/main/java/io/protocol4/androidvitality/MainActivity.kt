package io.protocol4.androidvitality

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.protocol4.androidvitality.resource.ResourceMonitorScreen
import io.protocol4.androidvitality.resource.ResourceMonitorViewModel
import io.protocol4.androidvitality.ui.theme.AndroidVitalityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

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
            val viewModel: ResourceMonitorViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            
            AndroidVitalityTheme(themeMode = uiState.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ResourceMonitorScreen(viewModel = viewModel)
                }
            }
        }
    }
}
