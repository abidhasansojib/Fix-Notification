package com.fix.notification.model

import android.graphics.drawable.Drawable

enum class OpStatus {
    ALLOWED,
    IGNORED,
    DENIED,
    DEFAULT,
    UNKNOWN;

    fun isOk(): Boolean = this == ALLOWED
}

data class AppDetailStatus(
    /**
     * App notification permission status (checked via dumpsys notification importance).
     * If disabled, background optimizations will not show notifications to the user.
     */
    val notifications: OpStatus = OpStatus.UNKNOWN,
    val isWhitelisted: Boolean = false,
    val standbyBucket: String = "UNKNOWN",
    val runInBackground: OpStatus = OpStatus.UNKNOWN,
    val runAnyInBackground: OpStatus = OpStatus.UNKNOWN,
    val autoStart: OpStatus = OpStatus.UNKNOWN,
    val autoRevokePermissions: OpStatus = OpStatus.UNKNOWN,
    val isMilletWhiteSupported: Boolean = false,
    val isMilletWhite: Boolean = false,
    val isCloudLowLatencySupported: Boolean = false,
    val isCloudLowLatency: Boolean = false,
    val isMilletNoRestrictSupported: Boolean = false,
    val isMilletNoRestrict: Boolean = false
) {
    val isBucketOk: Boolean
        get() = standbyBucket.startsWith("ACTIVE") ||
                standbyBucket.startsWith("EXEMPTED") ||
                standbyBucket == "10" ||
                standbyBucket == "5"

    /** Confirmed active notification permission. UNKNOWN is not considered OK. */
    val isNotificationOk: Boolean
        get() = notifications == OpStatus.ALLOWED || notifications == OpStatus.DEFAULT

    /** True only if notifications are explicitly disabled / blocked. */
    val needsManualNotifications: Boolean
        get() = notifications == OpStatus.IGNORED || notifications == OpStatus.DENIED

    /** True if autostart is blocked on Xiaomi / MIUI ROMs. DEFAULT means device/ROM lacks this op. */
    val needsManualAutoStart: Boolean
        get() = autoStart != OpStatus.ALLOWED && autoStart != OpStatus.DEFAULT

    fun isAllOptimized(isGms: Boolean = false): Boolean {
        val baseOk = isNotificationOk &&
                isWhitelisted &&
                isBucketOk &&
                runInBackground.isOk() &&
                runAnyInBackground.isOk() &&
                !needsManualAutoStart &&
                autoRevokePermissions == OpStatus.IGNORED

        val milletWhiteOk = !isMilletWhiteSupported || isMilletWhite
        val cloudLowLatencyOk = !isCloudLowLatencySupported || isCloudLowLatency
        val milletNoRestrictOk = !isMilletNoRestrictSupported || isMilletNoRestrict

        return baseOk && milletWhiteOk && cloudLowLatencyOk && milletNoRestrictOk
    }
}

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isSelected: Boolean = false,
    val isGoogleGms: Boolean = false,
    /** True if package is only discovered via Shizuku because MIUI PackageManager concealed it. */
    val isHiddenByMiui: Boolean = false,
    val detailStatus: AppDetailStatus? = null
)

data class FixLog(
    val appName: String,
    val packageName: String,
    val actionText: String,
    val isSuccess: Boolean = true,
    /** True when a shell command genuinely failed — rendered in error red in log console. */
    val isError: Boolean = false
)

data class TerminalEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val timestamp: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}
