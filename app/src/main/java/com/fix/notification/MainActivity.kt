package com.fix.notification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fix.notification.shizuku.ShizukuManager
import com.fix.notification.ui.MainViewModel
import com.fix.notification.ui.components.AppListScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var shizukuManager: ShizukuManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register Shizuku listener & automatically request permission on app launch
        shizukuManager = ShizukuManager { isGranted ->
            viewModel.updateShizukuStatus(isGranted)
            if (isGranted) {
                viewModel.loadApps(applicationContext)
            }
        }
        shizukuManager.registerListeners()

        // Load app list on launch
        viewModel.loadApps(applicationContext)

        setContent {
            FixNotificationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppListScreen(
                        viewModel = viewModel,
                        onRequestShizukuPermission = {
                            shizukuManager.checkAndRequestPermissionWithRetry()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check & request Shizuku permission on resume if not yet granted
        shizukuManager.checkAndRequestPermissionWithRetry()
    }

    override fun onDestroy() {
        super.onDestroy()
        shizukuManager.unregisterListeners()
    }
}

@Composable
fun FixNotificationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}
