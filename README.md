# 🚀 Fix Notification

**Fix Notification** is a high-performance Android utility engineered to resolve **notification delays** and aggressive background-killing on Xiaomi, Redmi, and POCO devices (MIUI and HyperOS, particularly China ROMs).

The app interfaces directly with low-level Android system frameworks and MIUI power management via **[Shizuku](https://shizuku.rikka.app/)**, optimizing apps **WITHOUT ROOT** and without unlocking the bootloader.

---

## ✨ Key Features

- ⚡ **1-Click Notification Fix**: Batch optimize multiple applications simultaneously with real-time execution logs and progress tracking.
- 🛡️ **Multi-Tier System Optimization**:
  - **DeviceIdle Whitelist**: Bypasses Android Doze mode and deep sleep restrictions.
  - **Standby Bucket Priority**: Elevates app standby status to `ACTIVE` (level 10).
  - **AppOps Background Permissions**: Enables `RUN_IN_BACKGROUND` and `RUN_ANY_IN_BACKGROUND` with `--user 0`.
  - **Auto-Revoke Disabling**: Exempts apps from Android's unused permission revoker (`AUTO_REVOKE_PERMISSIONS_IF_UNUSED`).
  - **MIUI/HyperOS System Whitelists**: Registers apps into Xiaomi kernel/system whitelists (`millet_white`, `cloud_lowlatency_whitelist`, `MILLET_NO_RESTRICT_APP`).
  - **MIUI Auto-Start Management**: Toggles AppOp `10008` directly and provides a 1-tap shortcut to the MIUI Security Center Autostart manager.
- 🔍 **Detailed Diagnostics**: Live state inspection for notification permissions, battery optimization, standby bucket, and Xiaomi-specific parameters.
- 🌐 **Remote Recommended App List**: Dynamically loaded from CDN with local fallback.
- 🎨 **Modern Jetpack Compose UI**: Clean Material Design 3 interface with full Dark Mode support.

---

## 🆕 Recent Improvements & Fixes

### 🛡️ System Integrity & Data Safety
- **System Table Protection**: Shell command outputs are strictly validated before writing. If a command fails or returns `null`, no data is written, preventing error strings (e.g. `SecurityException`) from corrupting `millet_white` or `MILLET_NO_RESTRICT_APP`.
- **Package Name Validation**: Every token is validated against package regex (`^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$`).
- **Delimiter Preservation**: Automatically preserves ROM-specific delimiters (e.g., `", "` on HyperOS 3 vs `","` or `";"`).
- **Process Deadlock & Timeout Fix**: Standardized on synchronous stream handling with error stream draining and pure `waitFor()`, eliminating `IllegalThreadStateException` issues caused by `waitFor(timeout)` in `ShizukuRemoteProcess`.
- **Automatic `WRITE_SETTINGS` Grant**: On HyperOS, `com.android.shell` frequently loses `WRITE_SETTINGS`. The app automatically verifies and re-grants this AppOp before system settings operations.

### 🎯 Accurate Detection & Diagnostics
- **Exact Whitelist Matching**: Replaced loose substring searches with token-based matching to prevent false positives (e.g., matching `com.foo` when only `com.foo.bar` is present).
- **Accurate Standby Bucket Checking**: Fixed false positive where `RESTRICTED (45)` matched `5` (`EXEMPTED`).
- **Notification Permission Inspection**: Reads real notification importance from `dumpsys notification` to alert if alerts are disabled in Android Settings.
- **Hidden App Detection**: Discovers user apps concealed by MIUI via `pm list packages -3 --user 0`.

### ⚡ Batch Performance Optimization
- **Single-Pass System Snapshot**: Caches system tables and whitelist state in memory during batch runs, slashing redundant shell calls.
- **Batch Command Execution**: Groups consecutive commands into a single shell execution, reducing binder process spawns by **over 90%** (from ~3,000 to ~200 spawns for 200 apps).
- **Single-Write Table Updates**: Updates MIUI settings tables once for the entire batch rather than reading and rewriting per app.

---

## ⚠️ Manual Checks Required on Xiaomi Devices

While the app automates background permissions, two options should be verified:
1. **Notification Permissions**: If system notifications are toggled off in Android Settings, background fixes cannot display alerts. Check Item #1 in the app's detail sheet.
2. **MIUI Security Center Autostart**: On certain HyperOS ROMs, Security Center controls autostart independently. If CLI toggle does not take effect, tap the **"Settings"** button next to Auto-Start in the detail sheet to open the MIUI Autostart screen.

---

## 📋 Requirements

- **Device**: Xiaomi / Redmi / POCO running MIUI 12+ or HyperOS (Android 7.0+).
- **Prerequisite**: **[Shizuku](https://shizuku.rikka.app/)** installed and **Running**.
  - *Tip for MIUI*: In Developer Options, enable **"USB debugging (Security settings)"** if prompted.

---

## 📱 How to Use

1. Start **Shizuku** via Wireless Debugging or ADB.
2. Launch **Fix Notification** and grant Shizuku access.
3. Select your essential messaging and banking apps.
4. Tap **"FIX NOTIFICATIONS"**.
5. Review the live execution log. Green indicates success, and red highlights any failed commands.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
