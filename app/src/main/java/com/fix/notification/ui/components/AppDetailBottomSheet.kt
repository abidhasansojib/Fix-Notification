package com.fix.notification.ui.components

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
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.fix.notification.model.AppDetailStatus
import com.fix.notification.model.AppInfo
import com.fix.notification.model.OpStatus
import com.fix.notification.ui.theme.ErrorRed
import com.fix.notification.ui.theme.SuccessGreen
import com.fix.notification.ui.theme.SuccessGreenContainer
import com.fix.notification.ui.theme.WarningAmber
import com.fix.notification.ui.theme.WarningAmberContainer

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
    val iconBitmap = remember(app.packageName, app.icon) {
        app.icon?.toBitmap()?.asImageBitmap()
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        dragHandle = { BottomSheetDefaults.DragHandle() },
        windowInsets = WindowInsets.safeDrawing
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header: App Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.appName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Background Permissions & Configurations",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading || status == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Checking system permissions via Shizuku...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. DeviceIdle Whitelist
                    DetailCardItem(
                        title = "1. Battery Optimization Whitelist",
                        subtitle = "Bypass Android Doze and battery saving restrictions",
                        isOk = status.isWhitelisted,
                        statusBadge = if (status.isWhitelisted) "BYPASS ACTIVE" else "NOT BYPASSED",
                        actionText = if (status.isWhitelisted) "Revoke" else null,
                        onActionClick = if (status.isWhitelisted) { { onRevokeSinglePermission("WHITELIST") } } else null
                    )

                    // 2. Standby Bucket
                    val isBucketOk = status.standbyBucket.contains("ACTIVE", ignoreCase = true) ||
                            status.standbyBucket.contains("EXEMPTED", ignoreCase = true) ||
                            status.standbyBucket.contains("10") ||
                            status.standbyBucket.contains("5")
                    val cleanBucket = status.standbyBucket.replace("STANDBY_BUCKET_", "")

                    DetailCardItem(
                        title = "2. Standby Priority Bucket",
                        subtitle = "Ensures background tasks receive highest CPU priority",
                        isOk = isBucketOk,
                        statusBadge = cleanBucket,
                        actionText = if (isBucketOk) "Revoke" else null,
                        onActionClick = if (isBucketOk) { { onRevokeSinglePermission("STANDBY_BUCKET") } } else null
                    )

                    // 3. RUN_IN_BACKGROUND
                    DetailCardItem(
                        title = "3. Background Service (AppOp 63)",
                        subtitle = "Allow service execution when app is in background",
                        isOk = status.runInBackground.isOk(),
                        statusBadge = status.runInBackground.name,
                        actionText = if (status.runInBackground.isOk()) "Revoke" else null,
                        onActionClick = if (status.runInBackground.isOk()) { { onRevokeSinglePermission("RUN_IN_BACKGROUND") } } else null
                    )

                    // 4. RUN_ANY_IN_BACKGROUND
                    DetailCardItem(
                        title = "4. Any Background Task (AppOp 70)",
                        subtitle = "Allow alarms, broadcasts, and jobs in background",
                        isOk = status.runAnyInBackground.isOk(),
                        statusBadge = status.runAnyInBackground.name,
                        actionText = if (status.runAnyInBackground.isOk()) "Revoke" else null,
                        onActionClick = if (status.runAnyInBackground.isOk()) { { onRevokeSinglePermission("RUN_ANY_IN_BACKGROUND") } } else null
                    )

                    // 5. Auto Start
                    val autoStartText = when (status.autoStart) {
                        OpStatus.ALLOWED -> "ALLOWED"
                        OpStatus.IGNORED -> "IGNORED"
                        OpStatus.DENIED -> "DENIED"
                        OpStatus.DEFAULT -> "DEFAULT"
                        OpStatus.UNKNOWN -> "UNKNOWN"
                    }
                    DetailCardItem(
                        title = "5. MIUI Auto-Start (AppOp 10008)",
                        subtitle = "Allow launch on device boot and notifications",
                        isOk = status.autoStart.isOk(),
                        statusBadge = autoStartText,
                        actionText = "Settings",
                        onActionClick = { onOpenAppSettings() }
                    )

                    // 6. Manage if unused
                    val isAutoRevokeOk = status.autoRevokePermissions == OpStatus.IGNORED
                    val autoRevokeBadge = when (status.autoRevokePermissions) {
                        OpStatus.IGNORED -> "DISABLED (SAFE)"
                        OpStatus.ALLOWED -> "ENABLED (AT RISK)"
                        OpStatus.DENIED -> "DENIED"
                        OpStatus.DEFAULT -> "DEFAULT"
                        OpStatus.UNKNOWN -> "UNKNOWN"
                    }
                    DetailCardItem(
                        title = "6. Auto-Revoke if Unused",
                        subtitle = "Prevent Android from stripping permissions if unused",
                        isOk = isAutoRevokeOk,
                        statusBadge = autoRevokeBadge,
                        actionText = if (isAutoRevokeOk) "Revoke" else null,
                        onActionClick = if (isAutoRevokeOk) { { onRevokeSinglePermission("AUTO_REVOKE_IF_UNUSED") } } else null
                    )

                    if (status.isMilletWhiteSupported) {
                        DetailCardItem(
                            title = "7. MIUI Millet White",
                            subtitle = "Millet deep freeze killer whitelist",
                            isOk = status.isMilletWhite,
                            statusBadge = if (status.isMilletWhite) "WHITELISTED" else "NOT WHITELISTED",
                            actionText = if (status.isMilletWhite) "Revoke" else null,
                            onActionClick = if (status.isMilletWhite) { { onRevokeSinglePermission("MILLET_WHITE") } } else null
                        )
                    }

                    if (status.isCloudLowLatencySupported) {
                        DetailCardItem(
                            title = "8. MIUI Low Latency Whitelist",
                            subtitle = "Cloud low-latency network & push priority",
                            isOk = status.isCloudLowLatency,
                            statusBadge = if (status.isCloudLowLatency) "WHITELISTED" else "NOT WHITELISTED",
                            actionText = if (status.isCloudLowLatency) "Revoke" else null,
                            onActionClick = if (status.isCloudLowLatency) { { onRevokeSinglePermission("CLOUD_LOWLATENCY") } } else null
                        )
                    }

                    if (status.isMilletNoRestrictSupported) {
                        DetailCardItem(
                            title = "9. MIUI Millet No Restrict",
                            subtitle = "Exempt from aggressive MIUI background restriction",
                            isOk = status.isMilletNoRestrict,
                            statusBadge = if (status.isMilletNoRestrict) "UNRESTRICTED" else "RESTRICTED",
                            actionText = if (status.isMilletNoRestrict) "Revoke" else null,
                            onActionClick = if (status.isMilletNoRestrict) { { onRevokeSinglePermission("MILLET_NO_RESTRICT") } } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action: Optimize this app
                Button(
                    onClick = onFixSingleApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Outlined.Build, contentDescription = "Optimize App")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "OPTIMIZE THIS APP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action: Reset all configurations to default
                OutlinedButton(
                    onClick = onRevokeAllPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                ) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Revoke All")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "RESET ALL SETTINGS TO DEFAULT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DetailCardItem(
    title: String,
    subtitle: String,
    isOk: Boolean,
    statusBadge: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isOk) SuccessGreen else WarningAmber,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isOk) SuccessGreenContainer else WarningAmberContainer
                ) {
                    Text(
                        text = statusBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOk) SuccessGreen else WarningAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (actionText != null && onActionClick != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onActionClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = actionText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (actionText == "Settings") MaterialTheme.colorScheme.primary else ErrorRed
                        )
                    }
                }
            }
        }
    }
}
