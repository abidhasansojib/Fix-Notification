package com.fix.notification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fix.notification.shizuku.ShizukuManager
import com.fix.notification.ui.MainViewModel
import com.fix.notification.ui.components.AppListScreen
import com.fix.notification.ui.theme.FixNotificationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var shizukuManager: ShizukuManager
    private var wasGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        shizukuManager = ShizukuManager { isGranted ->
            viewModel.updateShizukuStatus(isGranted)
            if (isGranted && !wasGranted) {
                viewModel.loadApps(applicationContext)
            }
            wasGranted = isGranted
        }
        shizukuManager.registerListeners()

        // Initial app load
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
                            shizukuManager.requestPermissionByUser()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Only refresh status on resume without repeatedly prompting the user
        shizukuManager.refreshStatusOnly()
    }

    override fun onDestroy() {
        super.onDestroy()
        shizukuManager.unregisterListeners()
    }
}
