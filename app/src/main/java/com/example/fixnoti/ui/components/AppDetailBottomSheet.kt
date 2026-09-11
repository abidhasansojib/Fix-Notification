package com.example.fixnoti.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.fixnoti.model.AppDetailStatus
import com.example.fixnoti.model.AppInfo
import com.example.fixnoti.model.OpStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheet(
    app: AppInfo,
    status: AppDetailStatus?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onFixSingleApp: () -> Unit,
    onRevokeSinglePermission: (String) -> Unit,
    onRevokeAllPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header: App Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconBitmap = app.icon?.toBitmap()?.asImageBitmap()
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.appName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Background Configuration & System Permissions Status:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading || status == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 1. DeviceIdle Whitelist
                DetailItemRow(
                    title = "1. DeviceIdle Whitelist",
                    subtitle = "Bypass system battery optimization list",
                    isOk = status.isWhitelisted,
                    statusText = if (status.isWhitelisted) "BATTERY OPTIMIZATION BYPASSED" else "NOT BYPASSING BATTERY OPTIMIZATION",
                    actionText = if (status.isWhitelisted) "Revoke" else null,
                    onActionClick = if (status.isWhitelisted) { { onRevokeSinglePermission("WHITELIST") } } else null
                )

                // 2. Standby Bucket
                val isBucketOk = status.standbyBucket.contains("ACTIVE", ignoreCase = true) ||
                        status.standbyBucket.contains("EXEMPTED", ignoreCase = true) ||
                        status.standbyBucket.contains("10") ||
                        status.standbyBucket.contains("5")

                DetailItemRow(
                    title = "2. Standby Bucket",
                    subtitle = "Background standby priority bucket",
                    isOk = isBucketOk,
                    statusText = "Current: ${status.standbyBucket}",
                    actionText = if (isBucketOk) "Revoke" else null,
                    onActionClick = if (isBucketOk) { { onRevokeSinglePermission("STANDBY_BUCKET") } } else null
                )

                // 3. RUN_IN_BACKGROUND
                DetailItemRow(
                    title = "3. RUN_IN_BACKGROUND Permission",
                    subtitle = "Allow app background service execution",
                    isOk = status.runInBackground.isOk(),
                    statusText = "Status: ${status.runInBackground.name}",
                    actionText = if (status.runInBackground.isOk()) "Revoke" else null,
                    onActionClick = if (status.runInBackground.isOk()) { { onRevokeSinglePermission("RUN_IN_BACKGROUND") } } else null
                )

                // 4. RUN_ANY_IN_BACKGROUND
                DetailItemRow(
                    title = "4. RUN_ANY_IN_BACKGROUND Permission",
                    subtitle = "Allow background tasks/alarms/broadcasts",
                    isOk = status.runAnyInBackground.isOk(),
                    statusText = "Status: ${status.runAnyInBackground.name}",
                    actionText = if (status.runAnyInBackground.isOk()) "Revoke" else null,
                    onActionClick = if (status.runAnyInBackground.isOk()) { { onRevokeSinglePermission("RUN_ANY_IN_BACKGROUND") } } else null
                )

                // 5. Auto Start (10008)
                val autoStartText = when (status.autoStart) {
                    OpStatus.ALLOWED -> "ENABLED (ALLOW)"
                    OpStatus.IGNORED -> "DISABLED (IGNORE)"
                    OpStatus.DENIED -> "DISABLED (DENY)"
                    OpStatus.DEFAULT -> "Default"
                    OpStatus.UNKNOWN -> "Unable to retrieve value"
                }
                DetailItemRow(
                    title = "5. Auto Start",
                    subtitle = "System auto-start permission (AppOp 10008)",
                    isOk = status.autoStart.isOk(),
                    statusText = autoStartText,
                    actionText = "Settings",
                    onActionClick = { onOpenAppSettings() }
                )

                // 6. Manage if unused (AUTO_REVOKE_PERMISSIONS_IF_UNUSED)
                val isAutoRevokeOk = status.autoRevokePermissions == OpStatus.IGNORED
                val autoRevokeStatusText = when (status.autoRevokePermissions) {
                    OpStatus.IGNORED -> "AUTO-REVOKE DISABLED (IGNORE - Safe)"
                    OpStatus.ALLOWED -> "AUTO-REVOKE ENABLED (ALLOW - Risk of losing perms)"
                    OpStatus.DENIED -> "DISABLED (DENY)"
                    OpStatus.DEFAULT -> "Default"
                    OpStatus.UNKNOWN -> "Unable to retrieve value"
                }
                DetailItemRow(
                    title = "6. Manage if unused",
                    subtitle = "Auto-revoke permissions if app is unused",
                    isOk = isAutoRevokeOk,
                    statusText = autoRevokeStatusText,
                    actionText = if (isAutoRevokeOk) "Revoke" else null,
                    onActionClick = if (isAutoRevokeOk) { { onRevokeSinglePermission("AUTO_REVOKE_IF_UNUSED") } } else null
                )

                if (status.isMilletWhiteSupported) {
                    DetailItemRow(
                        title = "7. MIUI millet_white",
                        subtitle = "Millet Freeze Killer whitelist",
                        isOk = status.isMilletWhite,
                        statusText = if (status.isMilletWhite) "INCLUDED IN MILLET_WHITE" else "NOT IN MILLET_WHITE",
                        actionText = if (status.isMilletWhite) "Revoke" else null,
                        onActionClick = if (status.isMilletWhite) { { onRevokeSinglePermission("MILLET_WHITE") } } else null
                    )
                }

                if (status.isCloudLowLatencySupported) {
                    DetailItemRow(
                        title = "8. MIUI cloud_lowlatency_whitelist",
                        subtitle = "Cloud low-latency priority list",
                        isOk = status.isCloudLowLatency,
                        statusText = if (status.isCloudLowLatency) "INCLUDED IN LOWLATENCY_WHITELIST" else "NOT IN LOWLATENCY_WHITELIST",
                        actionText = if (status.isCloudLowLatency) "Revoke" else null,
                        onActionClick = if (status.isCloudLowLatency) { { onRevokeSinglePermission("CLOUD_LOWLATENCY") } } else null
                    )
                }

                if (status.isMilletNoRestrictSupported) {
                    DetailItemRow(
                        title = "9. MIUI MILLET_NO_RESTRICT_APP",
                        subtitle = "Millet unrestricted app list",
                        isOk = status.isMilletNoRestrict,
                        statusText = if (status.isMilletNoRestrict) "INCLUDED IN MILLET_NO_RESTRICT" else "NOT IN MILLET_NO_RESTRICT",
                        actionText = if (status.isMilletNoRestrict) "Revoke" else null,
                        onActionClick = if (status.isMilletNoRestrict) { { onRevokeSinglePermission("MILLET_NO_RESTRICT") } } else null
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Button: Optimize this app individually
                Button(
                    onClick = { onFixSingleApp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Outlined.Build, contentDescription = "Fix App")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "OPTIMIZE THIS APP", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button: Reset all configurations to default
                OutlinedButton(
                    onClick = { onRevokeAllPermissions() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Revoke All")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "RESET ALL SETTINGS TO DEFAULT", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DetailItemRow(
    title: String,
    subtitle: String,
    isOk: Boolean,
    statusText: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isOk) Color(0xFF2E7D32) else Color(0xFFD32F2F),
            modifier = Modifier.size(26.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOk) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (actionText == "Settings") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = actionText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
