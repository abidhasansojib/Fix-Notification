package com.fix.notification.shizuku

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import rikka.shizuku.Shizuku

class ShizukuManager(
    private val onPermissionResult: (Boolean) -> Unit
) {

    private val requestCode = 1001
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Automatically requests permission only once per app session.
     * Prevents re-prompting dialog repeatedly when returning to the app on onResume.
     */
    private var hasAutoRequested = false

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { reqCode, grantResult ->
        if (reqCode == requestCode) {
            onPermissionResult(grantResult == PackageManager.PERMISSION_GRANTED)
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refresh(userInitiated = false)
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        onPermissionResult(false)
    }

    fun registerListeners() {
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)

            requestWithRetry(userInitiated = false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unregisterListeners() {
        try {
            handler.removeCallbacksAndMessages(null)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Explicitly requested by user clicking the grant button. */
    fun requestPermissionByUser() {
        hasAutoRequested = false
        requestWithRetry(userInitiated = true)
    }

    /** Refresh status without prompting dialog. Safe for onResume. */
    fun refreshStatusOnly() {
        onPermissionResult(ShizukuShellExecutor.isPermissionGranted())
    }

    private fun requestWithRetry(userInitiated: Boolean, retryCount: Int = 3) {
        if (refresh(userInitiated)) return

        if (retryCount > 0 && !ShizukuShellExecutor.isShizukuAvailable()) {
            handler.postDelayed({ requestWithRetry(userInitiated, retryCount - 1) }, 500)
        }
    }

    private fun refresh(userInitiated: Boolean): Boolean {
        if (!ShizukuShellExecutor.isShizukuAvailable()) {
            onPermissionResult(false)
            return false
        }

        return try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                onPermissionResult(true)
                true
            } else {
                onPermissionResult(false)
                if (userInitiated || !hasAutoRequested) {
                    hasAutoRequested = true
                    Shizuku.requestPermission(requestCode)
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            onPermissionResult(false)
            false
        }
    }
}
