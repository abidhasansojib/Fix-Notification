package com.fix.notification.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fix.notification.ui.MainViewModel
import com.fix.notification.ui.theme.ErrorRed
import com.fix.notification.ui.theme.ErrorRedContainer
import com.fix.notification.ui.theme.OnErrorRedContainer
import com.fix.notification.ui.theme.OnSuccessGreenContainer
import com.fix.notification.ui.theme.SuccessGreen
import com.fix.notification.ui.theme.SuccessGreenContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: MainViewModel,
    onRequestShizukuPermission: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Automatically check and request Shizuku permission when screen is created
    LaunchedEffect(Unit) {
        if (!uiState.isShizukuGranted) {
            onRequestShizukuPermission()
        }
    }

    // Memoize filtered list and selection calculations to avoid redundant computations
    val filteredApps = remember(uiState.appList, uiState.searchQuery) {
        MainViewModel.getFilteredApps(uiState.appList, uiState.searchQuery)
    }
    val selectedCount = remember(uiState.appList) {
        uiState.appList.count { it.isSelected }
    }
    val allFilteredSelected = remember(filteredApps) {
        filteredApps.isNotEmpty() && filteredApps.all { it.isSelected }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Fix Notification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    // GCM Diagnostics action
                    IconButton(
                        onClick = { viewModel.openGcmDiagnostics() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "GCM Diagnostics",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.loadApps(context, forceRefresh = true) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload app list",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedCount > 0,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.fixSelectedApps() },
                    icon = { Icon(imageVector = Icons.Default.Build, contentDescription = "Fix") },
                    text = {
                        Text(
                            text = "OPTIMIZE $selectedCount APPS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Shizuku Status Banner
            if (!uiState.isShizukuGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRedContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shizuku Permission Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = OnErrorRedContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start the Shizuku app and grant permission to allow background service and notification optimization.",
                            fontSize = 12.sp,
                            color = OnErrorRedContainer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onRequestShizukuPermission() },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "GRANT SHIZUKU PERMISSION", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = SuccessGreenContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Shizuku Service Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSuccessGreenContainer
                            )
                        }
                        Text(
                            text = "${filteredApps.size} apps found",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSuccessGreenContainer
                        )
                    }
                }
            }

            // 2. Modern Segmented Mode Selector: Recommended vs All Apps
            val isRecommended = !uiState.isShowAllApps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Segment 1: Recommended Apps
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (uiState.isShowAllApps) viewModel.toggleShowAllApps(context)
                        },
                    color = if (isRecommended) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = if (isRecommended) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recommended",
                            fontSize = 12.sp,
                            fontWeight = if (isRecommended) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Segment 2: All Installed Apps
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (!uiState.isShowAllApps) viewModel.toggleShowAllApps(context)
                        },
                    color = if (!isRecommended) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = if (!isRecommended) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (!isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "All Applications",
                            fontSize = 12.sp,
                            fontWeight = if (!isRecommended) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Mode Hint
            if (isRecommended) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtered by repo list (${filteredApps.size} installed). Select 'All Applications' to see all apps.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // 3. Search Bar with Clear Button
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = {
                    Text(
                        text = if (isRecommended) "Search banking & social apps..." else "Search all applications...",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            // 4. Select All Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.toggleSelectAll() }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Checkbox(
                        checked = allFilteredSelected,
                        onCheckedChange = { viewModel.toggleSelectAll() },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select all (${filteredApps.size} apps)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                if (selectedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$selectedCount selected",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 2.dp))

            // 5. Application List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading installed applications...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching applications found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppItemRow(
                            app = app,
                            onToggleSelect = { viewModel.toggleAppSelection(app.packageName) },
                            onOpenDetail = { viewModel.openAppDetail(app) }
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet displaying app details when tapping (i)
    uiState.detailApp?.let { app ->
        AppDetailBottomSheet(
            app = app,
            status = uiState.detailStatus,
            isLoading = uiState.isDetailLoading,
            onDismiss = { viewModel.closeAppDetail() },
            onFixSingleApp = { viewModel.fixAppFromDetail(app) },
            onEnableSinglePermission = { permType -> viewModel.enableSinglePermission(app, permType) },
            onRevokeSinglePermission = { permType -> viewModel.revokeSinglePermission(app, permType) },
            onRevokeAllPermissions = { viewModel.revokeAllPermissionsFromDetail(app) },
            onOpenAppSettings = { viewModel.openAppSettings(app.packageName) },
            onOpenNotificationSettings = { viewModel.openNotificationSettings(app.packageName) },
            onOpenAutoStartSettings = { viewModel.openAutoStartSettings(app.packageName) }
        )
    }

    // Modal Progress Dialog during Fix execution (Displays real-time progress & logs)
    if (uiState.isFixing) {
        FixProgressDialog(
            progress = uiState.fixProgress,
            currentApp = uiState.currentFixApp,
            logs = uiState.fixLogs,
            isFinished = uiState.isFixFinished,
            onDismiss = { viewModel.closeFixProgressDialog() }
        )
    }
}
