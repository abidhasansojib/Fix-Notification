package com.fix.notification.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import android.util.LruCache
import com.fix.notification.model.AppInfo
import com.fix.notification.ui.theme.SuccessGreen
import com.fix.notification.ui.theme.SuccessGreenContainer
import com.fix.notification.ui.theme.WarningAmber
import com.fix.notification.ui.theme.WarningAmberContainer

/**
 * Global LRU cache for application icons to avoid re-converting Drawables to Bitmaps on every scroll.
 */
object IconCache {
    private val cache = LruCache<String, ImageBitmap>(300)

    fun getOrConvert(packageName: String, drawable: android.graphics.drawable.Drawable?): ImageBitmap? {
        if (drawable == null) return null
        cache.get(packageName)?.let { return it }
        return try {
            val bitmap = drawable.toBitmap().asImageBitmap()
            cache.put(packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppItemRow(
    app: AppInfo,
    onToggleSelect: () -> Unit,
    onOpenDetail: () -> Unit
) {
    // Instant O(1) cache lookup avoids CPU Canvas software drawing overhead on scroll
    val iconBitmap = remember(app.packageName) {
        IconCache.getOrConvert(app.packageName, app.icon)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggleSelect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else if (app.isGoogleGms) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (app.isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. App Selection Checkbox
            Checkbox(
                checked = app.isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 2. App Icon with Fallback Initial
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 3. App Title, Package & Status Badges
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (app.isGoogleGms) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "GMS",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (app.isHiddenByMiui) {
                    Text(
                        text = "Concealed by MIUI — detected via Shizuku",
                        fontSize = 10.sp,
                        color = WarningAmber
                    )
                }

                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Status Badges with FlowRow to prevent horizontal overflow and clipping
                app.detailStatus?.let { status ->
                    Spacer(modifier = Modifier.height(4.dp))
                    if (status.isAllOptimized(app.isGoogleGms)) {
                        BadgeChip(
                            text = "✓ Fully Optimized",
                            isSuccess = true
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BadgeChip(
                                text = if (status.isWhitelisted) "WL: OK" else "No Whitelist",
                                isSuccess = status.isWhitelisted
                            )

                            val cleanBucket = status.standbyBucket
                                .replace("STANDBY_BUCKET_", "")
                                .lowercase()
                                .replaceFirstChar { it.uppercase() }

                            BadgeChip(
                                text = "Bucket: $cleanBucket",
                                isSuccess = status.isBucketOk
                            )
                        }
                    }
                }
            }

            // 4. Details info button (i)
            IconButton(
                onClick = onOpenDetail,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Details for ${app.appName}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun BadgeChip(text: String, isSuccess: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isSuccess) SuccessGreenContainer else WarningAmberContainer
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = if (isSuccess) SuccessGreen else WarningAmber,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
