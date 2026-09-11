# 🚀 Fix Notification

**Fix Notification** is a dedicated Android application designed to resolve **notification delay** issues on Xiaomi, Redmi, and POCO devices (especially on China ROMs, MIUI, and HyperOS).

The app leverages **[Shizuku](https://shizuku.rikka.app/)** to directly interact with low-level Android system services, optimizing apps **WITHOUT ROOT** or unlocking the bootloader.

---

## ✨ Key Features

- ⚡ **1-Click Notification Fix**: Batch optimize multiple applications simultaneously (Messenger, Telegram, WhatsApp, Instagram, Banking & E-Wallet apps, etc.).
- 🛡️ **Multi-Tier Optimization via Shizuku**:
  - **DeviceIdle Whitelist**: Adds apps to the battery optimization bypass list (Doze Mode exemption).
  - **Standby Bucket**: Sets app standby status to `ACTIVE` (highest resource priority in Android).
  - **AppOps Background**: Enables `RUN_IN_BACKGROUND` and `RUN_ANY_IN_BACKGROUND` permissions.
  - **MIUI/HyperOS System Whitelists**: Registers apps into Xiaomi system whitelists (`millet_white`, `cloud_lowlatency_whitelist`, `MILLET_NO_RESTRICT_APP`).
- 🔍 **Detailed Management & Diagnostics**: Displays the status of optimization metrics for each app, supporting manual toggling and permission revocation.
- 🌐 **Remote Recommended App List via CDN**: Automatically fetches popular apps from a high-speed CDN with fallback and smart caching.
- 🎨 **Modern UI**: Built with Jetpack Compose and Material Design 3, providing a smooth experience with Dark Mode support.

---

## 🛠️ How It Works

Notification delays on Xiaomi devices (especially China ROMs) are primarily caused by 3 mechanisms:
1. **Doze Mode & Standby Bucket**: The system demotes apps to restricted tiers (`RARE` / `RESTRICTED`), preventing them from receiving real-time FCM/GCM push messages.
2. **AppOps Restrictions**: Xiaomi restricts background execution permissions for third-party apps.
3. **Millet Power Management**: The aggressive MIUI/HyperOS background freezer automatically "freezes" apps within minutes of screen timeout.

**Fix Notification** resolves these bottlenecks by executing administrative commands directly through the Shizuku Service, promoting selected apps to top system priority.

---

## 📋 Requirements

- Device: Xiaomi / Redmi / POCO running MIUI 12+ or HyperOS (Android 7.0+).
- Prerequisite: **[Shizuku](https://shizuku.rikka.app/)** installed and **Running**.

---

## 📱 How to Use

1. Download and install **Shizuku** from Google Play or GitHub.
2. Start the Shizuku service (via Wireless Debugging on device or using ADB via PC).
3. Open **Fix Notification** and grant Shizuku permission when prompted.
4. Select the apps you want to fix notification delays for, and tap **"FIX NOTIFICATIONS"**.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
