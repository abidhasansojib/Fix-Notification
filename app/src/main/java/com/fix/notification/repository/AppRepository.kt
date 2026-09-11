package com.fix.notification.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.fix.notification.model.AppDetailStatus
import com.fix.notification.model.AppInfo
import com.fix.notification.model.FixLog
import com.fix.notification.model.OpStatus
import com.fix.notification.shizuku.BatchResult
import com.fix.notification.shizuku.ShizukuShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * System state snapshot shared across multiple apps.
 * Avoids redundant shell process spawns (e.g. dumpsys deviceidle whitelist and settings tables).
 */
data class SystemSnapshot(
    val idleWhitelist: Set<String> = emptySet(),
    val milletWhite: SettingTable? = null,
    val cloudLowLatency: SettingTable? = null,
    val milletNoRestrict: SettingTable? = null,
    /** Packages with notification importance=NONE in `dumpsys notification`. */
    val blockedNotifications: Set<String> = emptySet(),
    /** False when dumpsys notification output is unavailable. */
    val notificationDataAvailable: Boolean = false
)

/**
 * MIUI/HyperOS system settings table with separator preservation.
 */
data class SettingTable(
    val values: Set<String>,
    val separator: String
) {
    operator fun contains(packageName: String) = values.contains(packageName)
}

class AppRepository {

    companion object {
        private const val GITHUB_RAW_URL = "https://raw.githubusercontent.com/abidhasansojib/Fix-Notification/master/user_apps.txt"
        private const val JSDELIVR_RAW_URL = "https://cdn.jsdelivr.net/gh/abidhasansojib/Fix-Notification@master/user_apps.txt"

        private const val GMS_PACKAGE = "com.google.android.gms"

        const val KEY_MILLET_WHITE = "millet_white"
        const val KEY_CLOUD_LOW_LATENCY = "cloud_lowlatency_whitelist"
        const val KEY_MILLET_NO_RESTRICT = "MILLET_NO_RESTRICT_APP"

        private val APP_SETTINGS_REGEX = Regex("""AppSettings:\s+(\S+)\s+\(\d+\)(.*)""")
        private val PACKAGE_NAME_REGEX = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")

        @Volatile
        private var cachedRecommendedPackages: Set<String>? = null

        private val DEFAULT_RECOMMENDED_PACKAGES = setOf(
            // Global Social Media & Messaging
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.facebook.orca",
            "com.facebook.katana",
            "com.instagram.android",
            "com.instagram.barcelona",
            "com.imo.android.imoim",
            "org.thoughtcrime.securesms",
            "com.discord",
            "com.viber.voip",
            "com.twitter.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.locket.Locket",
            "com.skype.raider",
            "com.tencent.mm",
            "jp.naver.line.android",
            "com.zing.zalo",

            // Bangladesh Mobile Financial Services (MFS) & Banking
            "com.bKash.customerapp",
            "com.konasl.nagad",
            "bd.com.upay.customer",
            "com.dbbl.mbs.apps.main",
            "com.dbbl.nexus.pay",
            "com.ibbl.cellfin",
            "com.bracbank.astha",
            "com.thecitybank.citytouch",
            "com.trustandpay.customer",

            // Crypto & Trading Exchanges
            "com.binance.dev",
            "com.bybit.app",

            // Bangladesh Daily Services & Food Delivery
            "com.pathao.user",
            "com.global.foodpanda.android",
            "com.daraz.android",

            // Bangladesh Telecom & Utilities
            "com.portonics.mygp",
            "com.arena.banglalinkmela.app",
            "net.omobio.robisc",
            "net.omobio.airtelsc",

            // VPN, DNS & Network Utilities
            "com.cloudflare.onedotonedotonedotone",
            "com.wireguard.android",
            "com.adguard.android",
            "ch.protonvpn.android",

            // Productivity, Email & Workplace Messaging
            "com.google.android.gm",
            "com.google.android.apps.messaging",
            "com.google.android.apps.maps",
            "com.google.android.calendar",
            "com.microsoft.office.outlook",
            "com.microsoft.teams",
            "com.Slack",

            // Other Popular Apps
            "com.reddit.frontpage",
            "com.snapchat.android",
            "com.spotify.music",
            "com.google.android.youtube",

            // International & Regional E-Wallets / Banking
            "com.mservice.momotransfer",
            "com.mbmobile",
            "com.vietcombank.phone",
            "vn.com.techcombank.bb.app",
            "com.vpb.neo",
            "com.vnpay.bidv",
            "com.vietinbank.ipay",
            "com.vnpay.agribank3g",
            "com.acb.mobile",
            "com.tpb.mb.gprsauto",
            "com.sacombank.mbanking",
            "com.msb.mb",
            "com.vib.myvib2",
            "vn.cake.app",
            "vn.vnpay.vnpaywallet",
            "vn.com.vng.zalopay",
            "com.bplus.vtpay",
            "com.airpay.consumer"
        )
    }

    // ---------------------------------------------------------------- App Discovery

    suspend fun fetchRecommendedPackageNames(forceRefresh: Boolean = false): Set<String> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            cachedRecommendedPackages?.let { return@withContext it }
        }

        for (urlString in listOf(GITHUB_RAW_URL, JSDELIVR_RAW_URL)) {
            try {
                val result = withTimeoutOrNull(3000L) {
                    val url = java.net.URL(if (forceRefresh) "$urlString?t=${System.currentTimeMillis()}" else urlString)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.useCaches = false
                    connection.connectTimeout = 2500
                    connection.readTimeout = 2500
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "FixNotification/1.0")
                    connection.setRequestProperty("Cache-Control", "no-cache")

                    if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        val text = connection.inputStream.bufferedReader().use { it.readText() }
                        text.lines()
                            .map { it.trim().removePrefix("package:").trim() }
                            .filter { it.isNotEmpty() && !it.startsWith("#") }
                            .toSet()
                            .takeIf { it.isNotEmpty() }
                    } else null
                }
                if (result != null) {
                    cachedRecommendedPackages = result
                    return@withContext result
                }
            } catch (e: Exception) {
                // Fallback to next or default
            }
        }

        val fallback = cachedRecommendedPackages ?: DEFAULT_RECOMMENDED_PACKAGES
        cachedRecommendedPackages = fallback
        fallback
    }

    /**
     * Retrieves user-installed applications.
     * Uses `pm list packages -3 --user 0` via Shizuku as the primary source to detect apps hidden by MIUI.
     */
    suspend fun getInstalledApps(context: Context, showAll: Boolean = false, forceRefresh: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val userPackages = mutableSetOf<String>()

        // 1. Primary source: pm list packages via Shizuku (bypasses MIUI PackageManager restrictions)
        if (ShizukuShellExecutor.isPermissionGranted()) {
            var shellOutput = ShizukuShellExecutor.executeCommand("pm list packages -3 --user 0")
            if (shellOutput.isBlank() || shellOutput.startsWith("Error", ignoreCase = true) || shellOutput.contains("Exception", ignoreCase = true)) {
                shellOutput = ShizukuShellExecutor.executeCommand("pm list packages -3")
            }
            if (shellOutput.isNotBlank() && !shellOutput.startsWith("Error", ignoreCase = true)) {
                shellOutput.lines()
                    .map { it.trim().removePrefix("package:").trim() }
                    .filter { it.isNotEmpty() && !it.contains(' ') }
                    .forEach { userPackages.add(it) }
            }
        }

        // 2. Secondary source: Android PackageManager
        try {
            for (pkg in pm.getInstalledPackages(0)) {
                val appInfo = pkg.applicationInfo ?: continue
                if (isUserInstalled(appInfo)) userPackages.add(pkg.packageName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val recommendedPackages = fetchRecommendedPackageNames(forceRefresh)

        // 3. Robust MIUI Fallback: Direct getApplicationInfo probe for all recommended apps.
        // MIUI frequently restricts `pm.getInstalledPackages` and Shizuku might not yet be
        // ready on initial startup. Direct getApplicationInfo queries bypass MIUI enumeration blocks.
        for (pkg in recommendedPackages) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                if (appInfo != null) {
                    userPackages.add(pkg)
                }
            } catch (ignored: Exception) {
            }
        }

        val targetPackages = if (showAll) {
            userPackages + GMS_PACKAGE
        } else {
            userPackages.intersect(recommendedPackages) + GMS_PACKAGE
        }

        val apps = targetPackages.mapNotNull { packageName ->
            val isGms = packageName == GMS_PACKAGE

            val appInfo = try {
                pm.getApplicationInfo(packageName, 0)
            } catch (e: Exception) {
                null
            }
            if (isGms && appInfo == null) return@mapNotNull null

            val label = appInfo?.let {
                try {
                    pm.getApplicationLabel(it).toString()
                } catch (e: Exception) {
                    packageName
                }
            } ?: packageName

            val icon = appInfo?.let {
                try {
                    pm.getApplicationIcon(it)
                } catch (e: Exception) {
                    null
                }
            }

            AppInfo(
                appName = if (isGms) "$label (Google Play Services)" else label,
                packageName = packageName,
                icon = icon,
                isGoogleGms = isGms,
                isHiddenByMiui = appInfo == null
            )
        }

        apps.sortedWith(compareByDescending<AppInfo> { it.isGoogleGms }.thenBy { it.appName.lowercase() })
    }

    private fun isUserInstalled(appInfo: ApplicationInfo): Boolean =
        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

    // ---------------------------------------------------------------- Status Inspection

    /**
     * Captures shared system snapshot in a single batch shell process.
     */
    suspend fun loadSystemSnapshot(): SystemSnapshot = withContext(Dispatchers.IO) {
        val results = ShizukuShellExecutor.runBatch(
            listOf(
                "dumpsys deviceidle whitelist",
                "settings get system $KEY_MILLET_WHITE",
                "settings get system $KEY_CLOUD_LOW_LATENCY",
                "settings get system $KEY_MILLET_NO_RESTRICT",
                "dumpsys notification | grep AppSettings:"
            )
        )

        val notificationResult = results[4]
        SystemSnapshot(
            idleWhitelist = if (results[0].isSuccess) parseIdleWhitelist(results[0].output) else emptySet(),
            milletWhite = parseSettingTable(KEY_MILLET_WHITE, results[1]),
            cloudLowLatency = parseSettingTable(KEY_CLOUD_LOW_LATENCY, results[2]),
            milletNoRestrict = parseSettingTable(KEY_MILLET_NO_RESTRICT, results[3]),
            blockedNotifications = if (notificationResult.isSuccess) {
                parseBlockedNotifications(notificationResult.output)
            } else {
                emptySet()
            },
            notificationDataAvailable = notificationResult.isSuccess &&
                    notificationResult.output.contains("AppSettings:")
        )
    }

    private fun parseBlockedNotifications(output: String): Set<String> =
        output.lines().mapNotNull { line ->
            val match = APP_SETTINGS_REGEX.find(line) ?: return@mapNotNull null
            if (match.groupValues[2].contains("importance=NONE")) match.groupValues[1] else null
        }.toSet()

    suspend fun checkAppDetailStatus(packageName: String): AppDetailStatus =
        checkAppDetailStatus(packageName, loadSystemSnapshot())

    /**
     * Checks an individual app's status against the given snapshot using 1 batch shell process for 5 commands.
     */
    suspend fun checkAppDetailStatus(
        packageName: String,
        snapshot: SystemSnapshot
    ): AppDetailStatus = withContext(Dispatchers.IO) {
        val results = ShizukuShellExecutor.runBatch(
            listOf(
                "am get-standby-bucket $packageName",
                "cmd appops get $packageName RUN_IN_BACKGROUND",
                "cmd appops get $packageName RUN_ANY_IN_BACKGROUND",
                "cmd appops get $packageName 10008",
                "cmd appops get $packageName AUTO_REVOKE_PERMISSIONS_IF_UNUSED"
            )
        )

        fun opAt(index: Int): OpStatus =
            if (results[index].isSuccess) parseOpStatus(results[index].output) else OpStatus.UNKNOWN

        AppDetailStatus(
            notifications = when {
                !snapshot.notificationDataAvailable -> OpStatus.UNKNOWN
                snapshot.blockedNotifications.contains(packageName) -> OpStatus.IGNORED
                else -> OpStatus.ALLOWED
            },
            isWhitelisted = snapshot.idleWhitelist.contains(packageName),
            standbyBucket = if (results[0].isSuccess) parseStandbyBucket(results[0].output) else "UNKNOWN",
            runInBackground = opAt(1),
            runAnyInBackground = opAt(2),
            autoStart = opAt(3),
            autoRevokePermissions = opAt(4),
            isMilletWhiteSupported = snapshot.milletWhite != null,
            isMilletWhite = snapshot.milletWhite?.contains(packageName) == true,
            isCloudLowLatencySupported = snapshot.cloudLowLatency != null,
            isCloudLowLatency = snapshot.cloudLowLatency?.contains(packageName) == true,
            isMilletNoRestrictSupported = snapshot.milletNoRestrict != null,
            isMilletNoRestrict = snapshot.milletNoRestrict?.contains(packageName) == true
        )
    }

    // ---------------------------------------------------------------- Optimization

    suspend fun fixApp(app: AppInfo, onLog: suspend (FixLog) -> Unit): AppDetailStatus =
        fixApps(listOf(app), onLog).getValue(app.packageName)

    /**
     * Optimizes multiple apps simultaneously.
     * MIUI system tables (millet_white, etc.) are written ONCE for the whole batch.
     */
    suspend fun fixApps(
        apps: List<AppInfo>,
        onLog: suspend (FixLog) -> Unit,
        onAppStart: suspend (AppInfo, Int) -> Unit = { _, _ -> }
    ): Map<String, AppDetailStatus> = withContext(Dispatchers.IO) {
        if (apps.isEmpty()) return@withContext emptyMap()

        val snapshot = loadSystemSnapshot()

        apps.forEachIndexed { index, app ->
            onAppStart(app, index)
            val name = app.appName
            val pkg = app.packageName

            onLog(FixLog(name, pkg, "Applying background & power optimizations...", isSuccess = false))

            val commands = listOf(
                "Add to DeviceIdle Whitelist" to "cmd deviceidle whitelist +$pkg",
                "Set Standby Bucket -> ACTIVE" to "am set-standby-bucket $pkg active",
                "Enable RUN_IN_BACKGROUND -> ALLOW" to "cmd appops set --user 0 $pkg RUN_IN_BACKGROUND allow",
                "Enable RUN_ANY_IN_BACKGROUND -> ALLOW" to "cmd appops set --user 0 $pkg RUN_ANY_IN_BACKGROUND allow",
                "Enable MIUI Auto-Start (10008) -> ALLOW" to "cmd appops set $pkg 10008 allow",
                "Set Manage if unused -> IGNORE" to "cmd appops set --user 0 $pkg AUTO_REVOKE_PERMISSIONS_IF_UNUSED ignore"
            )

            val results = ShizukuShellExecutor.runBatch(commands.map { it.second })
            results.forEachIndexed { i, result ->
                val label = commands[i].first
                if (result.isSuccess) {
                    onLog(FixLog(name, pkg, "✓ $label", isSuccess = true))
                } else {
                    onLog(FixLog(name, pkg, "✗ $label — ${result.errorMessage}", isSuccess = false, isError = true))
                }
            }
        }

        // Write MIUI system tables once for the entire list
        val allPackages = apps.map { it.packageName }
        val scope = if (apps.size == 1) apps[0].appName else "${apps.size} apps"
        applySystemTable(KEY_MILLET_WHITE, snapshot.milletWhite, allPackages, add = true, scope = scope, onLog = onLog)
        applySystemTable(KEY_CLOUD_LOW_LATENCY, snapshot.cloudLowLatency, allPackages, add = true, scope = scope, onLog = onLog)
        applySystemTable(KEY_MILLET_NO_RESTRICT, snapshot.milletNoRestrict, allPackages, add = true, scope = scope, onLog = onLog)

        // Verify results
        val verifySnapshot = loadSystemSnapshot()
        val statuses = apps.associate { it.packageName to checkAppDetailStatus(it.packageName, verifySnapshot) }

        // Alert user if notification permission is blocked in system settings
        val needNotifications = apps.filter { statuses[it.packageName]?.needsManualNotifications == true }
        if (needNotifications.isNotEmpty()) {
            onLog(
                FixLog(
                    "Action Required",
                    "notifications",
                    "⚠ ${needNotifications.size} app(s) have notifications DISABLED in system settings: " +
                            needNotifications.joinToString(", ") { it.appName } +
                            ". Notifications must be enabled in Settings > Apps for fixes to work.",
                    isSuccess = false,
                    isError = true
                )
            )
        }

        // Remind user if Autostart toggle was not accepted
        val needAutoStart = apps.filter { statuses[it.packageName]?.needsManualAutoStart == true }
        if (needAutoStart.isNotEmpty()) {
            onLog(
                FixLog(
                    "Notice",
                    "autostart",
                    "ℹ ${needAutoStart.size} app(s) may require toggling Autostart manually in Security > Autostart: " +
                            needAutoStart.joinToString(", ") { it.appName },
                    isSuccess = false
                )
            )
        }

        statuses
    }

    suspend fun revokeAllPermissions(app: AppInfo, onLog: suspend (FixLog) -> Unit): AppDetailStatus =
        withContext(Dispatchers.IO) {
            val name = app.appName
            val pkg = app.packageName
            val snapshot = loadSystemSnapshot()

            val commands = listOf(
                "Remove from DeviceIdle Whitelist" to "cmd deviceidle whitelist -$pkg",
                "Reset Standby Bucket -> RARE" to "am set-standby-bucket $pkg rare",
                "Disable RUN_IN_BACKGROUND -> IGNORE" to "cmd appops set --user 0 $pkg RUN_IN_BACKGROUND ignore",
                "Disable RUN_ANY_IN_BACKGROUND -> IGNORE" to "cmd appops set --user 0 $pkg RUN_ANY_IN_BACKGROUND ignore",
                "Disable MIUI Auto-Start (10008) -> IGNORE" to "cmd appops set $pkg 10008 ignore",
                "Reset Manage if unused -> ALLOW" to "cmd appops set --user 0 $pkg AUTO_REVOKE_PERMISSIONS_IF_UNUSED allow"
            )

            val results = ShizukuShellExecutor.runBatch(commands.map { it.second })
            results.forEachIndexed { i, result ->
                val label = commands[i].first
                if (result.isSuccess) {
                    onLog(FixLog(name, pkg, "✓ $label", isSuccess = true))
                } else {
                    onLog(FixLog(name, pkg, "✗ $label — ${result.errorMessage}", isSuccess = false, isError = true))
                }
            }

            val pkgs = listOf(pkg)
            applySystemTable(KEY_MILLET_WHITE, snapshot.milletWhite, pkgs, add = false, scope = name, onLog = onLog)
            applySystemTable(KEY_CLOUD_LOW_LATENCY, snapshot.cloudLowLatency, pkgs, add = false, scope = name, onLog = onLog)
            applySystemTable(KEY_MILLET_NO_RESTRICT, snapshot.milletNoRestrict, pkgs, add = false, scope = name, onLog = onLog)

            onLog(FixLog(name, pkg, "Completed resetting configurations for $name", isSuccess = true))
            checkAppDetailStatus(pkg, loadSystemSnapshot())
        }

    suspend fun enableSinglePermission(
        app: AppInfo,
        permissionType: String,
        onLog: (suspend (FixLog) -> Unit)? = null
    ): AppDetailStatus = withContext(Dispatchers.IO) {
        val name = app.appName
        val pkg = app.packageName
        val logIt: suspend (String) -> Unit = { text -> onLog?.invoke(FixLog(name, pkg, text)) }

        when (permissionType) {
            "WHITELIST" -> {
                logIt("Adding to DeviceIdle Whitelist...")
                ShizukuShellExecutor.run("cmd deviceidle whitelist +$pkg")
            }
            "STANDBY_BUCKET" -> {
                logIt("Setting Standby Bucket -> ACTIVE...")
                ShizukuShellExecutor.run("am set-standby-bucket $pkg active")
            }
            "RUN_IN_BACKGROUND" -> {
                logIt("Setting RUN_IN_BACKGROUND -> ALLOW...")
                ShizukuShellExecutor.run("cmd appops set --user 0 $pkg RUN_IN_BACKGROUND allow")
            }
            "RUN_ANY_IN_BACKGROUND" -> {
                logIt("Setting RUN_ANY_IN_BACKGROUND -> ALLOW...")
                ShizukuShellExecutor.run("cmd appops set --user 0 $pkg RUN_ANY_IN_BACKGROUND allow")
            }
            "AUTO_START" -> {
                logIt("Enabling MIUI Auto-Start -> ALLOW...")
                ShizukuShellExecutor.run("cmd appops set $pkg 10008 allow")
            }
            "AUTO_REVOKE_IF_UNUSED" -> {
                logIt("Setting Manage if unused -> IGNORE...")
                ShizukuShellExecutor.run("cmd appops set --user 0 $pkg AUTO_REVOKE_PERMISSIONS_IF_UNUSED ignore")
            }
            "MILLET_WHITE" -> addToTable(KEY_MILLET_WHITE, pkg)
            "CLOUD_LOWLATENCY" -> addToTable(KEY_CLOUD_LOW_LATENCY, pkg)
            "MILLET_NO_RESTRICT" -> addToTable(KEY_MILLET_NO_RESTRICT, pkg)
        }

        checkAppDetailStatus(pkg)
    }

    suspend fun revokeSinglePermission(
        app: AppInfo,
        permissionType: String,
        onLog: (suspend (FixLog) -> Unit)? = null
    ): AppDetailStatus = withContext(Dispatchers.IO) {
        val name = app.appName
        val pkg = app.packageName
        val logIt: suspend (String) -> Unit = { text -> onLog?.invoke(FixLog(name, pkg, text)) }

        when (permissionType) {
            "WHITELIST" -> {
                logIt("Removing from DeviceIdle Whitelist...")
                ShizukuShellExecutor.run("cmd deviceidle whitelist -$pkg")
            }
            "STANDBY_BUCKET" -> {
                logIt("Setting Standby Bucket -> RARE...")
                ShizukuShellExecutor.run("am set-standby-bucket $pkg rare")
            }
            "RUN_IN_BACKGROUND" -> {
                logIt("Setting RUN_IN_BACKGROUND -> IGNORE...")
                ShizukuShellExecutor.run("cmd appops set --user 0 $pkg RUN_IN_BACKGROUND ignore")
            }
            "RUN_ANY_IN_BACKGROUND" -> {
                logIt("Setting RUN_ANY_IN_BACKGROUND -> IGNORE...")
                ShizukuShellExecutor.run("cmd appops set --user 0 $pkg RUN_ANY_IN_BACKGROUND ignore")
            }
            "AUTO_START" -> {
                logIt("Disabling MIUI Auto-Start -> IGNORE...")
                ShizukuShellExecutor.run("cmd appops set $pkg 10008 ignore")
            }
            "AUTO_REVOKE_IF_UNUSED" -> {
                logIt("Setting Manage if unused -> ALLOW...")
                ShizukuShellExecutor.run("cmd appops set --user 0 $pkg AUTO_REVOKE_PERMISSIONS_IF_UNUSED allow")
            }
            "MILLET_WHITE" -> removeFromTable(KEY_MILLET_WHITE, pkg)
            "CLOUD_LOWLATENCY" -> removeFromTable(KEY_CLOUD_LOW_LATENCY, pkg)
            "MILLET_NO_RESTRICT" -> removeFromTable(KEY_MILLET_NO_RESTRICT, pkg)
        }

        checkAppDetailStatus(pkg)
    }

    // ---------------------------------------------------------------- System Table Management

    private suspend fun applySystemTable(
        key: String,
        table: SettingTable?,
        packages: List<String>,
        add: Boolean,
        scope: String,
        onLog: suspend (FixLog) -> Unit
    ) {
        if (table == null) return

        val updated = if (add) table.values + packages else table.values - packages.toSet()
        if (updated == table.values) return

        ensureWriteSettingsPermission()
        val result = ShizukuShellExecutor.run(buildSettingsPut(key, updated, table.separator))
        val verb = if (add) "Added to" else "Removed from"
        if (result.isSuccess) {
            onLog(FixLog(scope, key, "✓ $verb MIUI System: $key", isSuccess = true))
        } else {
            onLog(FixLog(scope, key, "✗ $verb $key failed — ${result.errorMessage}", isSuccess = false, isError = true))
        }
    }

    private fun addToTable(key: String, packageName: String) {
        val read = ShizukuShellExecutor.run("settings get system $key")
        val table = parseSettingTable(key, BatchResult(read.stdout, if (read.isSuccess) 0 else 1)) ?: return
        if (table.contains(packageName)) return
        ensureWriteSettingsPermission()
        ShizukuShellExecutor.run(buildSettingsPut(key, table.values + packageName, table.separator))
    }

    private fun removeFromTable(key: String, packageName: String) {
        val read = ShizukuShellExecutor.run("settings get system $key")
        val table = parseSettingTable(key, BatchResult(read.stdout, if (read.isSuccess) 0 else 1)) ?: return
        if (!table.contains(packageName)) return
        ensureWriteSettingsPermission()
        ShizukuShellExecutor.run(buildSettingsPut(key, table.values - packageName, table.separator))
    }

    private fun buildSettingsPut(key: String, values: Set<String>, separator: String): String {
        if (values.isEmpty()) return "settings delete system $key"

        val value = values.joinToString(separator) +
                if (key.equals(KEY_MILLET_WHITE, ignoreCase = true)) ";" else ""
        return "settings put system $key \"$value\""
    }

    /**
     * HyperOS revokes WRITE_SETTINGS from `com.android.shell`, causing `settings put system` to fail with SecurityException.
     * We ensure it is allowed via appops before writing system settings.
     */
    private fun ensureWriteSettingsPermission() {
        val current = parseOpStatus(
            ShizukuShellExecutor.executeCommand("cmd appops get com.android.shell WRITE_SETTINGS")
        )
        if (current == OpStatus.ALLOWED) return
        ShizukuShellExecutor.run("cmd appops set com.android.shell WRITE_SETTINGS allow")
    }

    // ---------------------------------------------------------------- Navigation Shortcuts

    suspend fun openGcmDiagnostics(): String = withContext(Dispatchers.IO) {
        ShizukuShellExecutor.executeCommand("am start -n $GMS_PACKAGE/.gcm.GcmDiagnostics")
    }

    suspend fun openAppSettings(packageName: String): String = withContext(Dispatchers.IO) {
        ShizukuShellExecutor.executeCommand(
            "am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:$packageName"
        )
    }

    suspend fun openNotificationSettings(packageName: String): String = withContext(Dispatchers.IO) {
        ShizukuShellExecutor.executeCommand(
            "am start -a android.settings.APP_NOTIFICATION_SETTINGS --es android.provider.extra.APP_PACKAGE $packageName"
        )
    }

    suspend fun openAutoStartSettings(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val result = ShizukuShellExecutor.run(
            "am start -n com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
        if (result.isSuccess) return@withContext true

        openAppSettings(packageName)
        false
    }

    // ---------------------------------------------------------------- Output Parsing

    private fun parseIdleWhitelist(output: String): Set<String> =
        output.lines()
            .flatMap { line -> line.trim().split(',', ' ', '\t') }
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains('.') }
            .toSet()

    private fun parseSettingTable(key: String, result: BatchResult): SettingTable? {
        if (!result.isSuccess) return null

        val trimmed = result.output.trim()
        if (trimmed.isBlank() || trimmed == "null") return null

        val values = trimmed.split(';', ',', ':', ' ', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "null" }
            .toSet()

        if (values.isEmpty() || values.any { !PACKAGE_NAME_REGEX.matches(it) }) return null

        val separator = when {
            key.equals(KEY_MILLET_WHITE, ignoreCase = true) -> ";"
            trimmed.contains(", ") -> ", "
            trimmed.contains(",") -> ","
            trimmed.contains(";") -> ";"
            else -> ", "
        }
        return SettingTable(values, separator)
    }

    private fun parseStandbyBucket(output: String): String {
        val value = output.trim()
        return when {
            value.contains("EXEMPTED", ignoreCase = true) || value == "5" -> "EXEMPTED (5)"
            value.contains("ACTIVE", ignoreCase = true) || value == "10" -> "ACTIVE (10)"
            value.contains("WORKING_SET", ignoreCase = true) || value == "20" -> "WORKING_SET (20)"
            value.contains("FREQUENT", ignoreCase = true) || value == "30" -> "FREQUENT (30)"
            value.contains("RARE", ignoreCase = true) || value == "40" -> "RARE (40)"
            value.contains("RESTRICTED", ignoreCase = true) || value == "45" -> "RESTRICTED (45)"
            value.isNotEmpty() -> value
            else -> "UNKNOWN"
        }
    }

    private fun parseOpStatus(output: String): OpStatus {
        val lines = output.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val explicit = lines.firstOrNull {
            !it.startsWith("Uid mode:", ignoreCase = true) &&
                    !it.startsWith("Default mode:", ignoreCase = true) &&
                    it.contains(':')
        }
        if (explicit != null) {
            modeOf(explicit.substringAfter(':'))?.let { return it }
        }

        lines.firstOrNull { it.startsWith("Default mode:", ignoreCase = true) }?.let { line ->
            modeOf(line.substringAfter(':'))?.let { return it }
        }

        if (lines.any { it.contains("No operations", ignoreCase = true) }) return OpStatus.DEFAULT

        return OpStatus.UNKNOWN
    }

    private fun modeOf(value: String): OpStatus? {
        val mode = value.substringBefore(';').trim().lowercase()
        return when (mode) {
            "allow" -> OpStatus.ALLOWED
            "ignore" -> OpStatus.IGNORED
            "deny" -> OpStatus.DENIED
            "default" -> OpStatus.DEFAULT
            else -> null
        }
    }
}
