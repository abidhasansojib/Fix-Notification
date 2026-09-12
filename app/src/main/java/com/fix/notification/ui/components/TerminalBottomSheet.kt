package com.fix.notification.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fix.notification.model.TerminalEntry

// Terminal Color Palette (GitHub / AMOLED Dark Terminal)
private val TerminalBg = Color(0xFF0D1117)
private val TerminalSurface = Color(0xFF161B22)
private val TerminalBorder = Color(0xFF30363D)
private val PromptGreen = Color(0xFF3FB950)
private val OutputText = Color(0xFFC9D1D9)
private val ErrorRed = Color(0xFFF85149)
private val AccentCyan = Color(0xFF58A6FF)
private val SuccessBg = Color(0xFF238636)
private val ErrorBg = Color(0xFFDA3633)
private val SubduedGray = Color(0xFF8B949E)

private val COMMON_COMMAND_PRESETS = listOf(
    "cmd appops get ",
    "cmd appops set ",
    "settings get system MILLET_NO_RESTRICT_APP",
    "settings put system MILLET_NO_RESTRICT_APP ",
    "settings get system millet_white",
    "settings get system cloud_lowlatency_whitelist",
    "dumpsys deviceidle whitelist",
    "dumpsys notification",
    "pm list packages -3",
    "whoami",
    "id",
    "help",
    "clear"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalBottomSheet(
    isShizukuGranted: Boolean,
    entries: List<TerminalEntry>,
    isExecuting: Boolean,
    history: List<String>,
    onExecuteCommand: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var inputText by remember { mutableStateOf("") }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new command output arrives
    LaunchedEffect(entries.size, isExecuting) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = TerminalBg,
        contentColor = OutputText,
        dragHandle = null,
        windowInsets = WindowInsets.safeDrawing
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
        ) {
            // 1. Terminal Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ADB Terminal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isShizukuGranted) PromptGreen else ErrorRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isShizukuGranted) "Shizuku uid=2000" else "Shizuku Not Granted",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isShizukuGranted) PromptGreen else ErrorRed
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy All Output
                    IconButton(
                        onClick = {
                            if (entries.isNotEmpty()) {
                                val transcript = entries.joinToString("\n\n") { entry ->
                                    val out = if (entry.stdout.isNotBlank()) "\n${entry.stdout}" else ""
                                    val err = if (entry.stderr.isNotBlank()) "\n[stderr]\n${entry.stderr}" else ""
                                    "$ ${entry.command}$out$err (exit ${entry.exitCode})"
                                }
                                clipboardManager.setText(AnnotatedString(transcript))
                                Toast.makeText(context, "Session copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy all output",
                            tint = SubduedGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Clear Terminal
                    IconButton(
                        onClick = { onClear() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear terminal",
                            tint = SubduedGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Close Sheet
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SubduedGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = TerminalBorder, thickness = 1.dp)

            // 2. Preset Quick Commands Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalSurface.copy(alpha = 0.6f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUICK:",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SubduedGray
                )
                COMMON_COMMAND_PRESETS.forEach { preset ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (preset == "clear") {
                                    onClear()
                                } else if (preset == "help" || preset == "whoami" || preset == "id") {
                                    onExecuteCommand(preset)
                                } else {
                                    inputText = preset
                                    historyIndex = -1
                                    focusRequester.requestFocus()
                                }
                            },
                        color = TerminalSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
                    ) {
                        Text(
                            text = preset.trim(),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = AccentCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = TerminalBorder, thickness = 1.dp)

            // 3. Console Output Screen
            SelectionContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(TerminalBg)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Terminal Welcome Banner
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TerminalSurface.copy(alpha = 0.5f))
                                .border(1.dp, TerminalBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Shizuku ADB Shell Console",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "• Execute any ADB shell command locally (cmd appops, settings, pm, dumpsys).\n• Supports commands with or without 'adb shell' prefix.\n• Tap quick chips above or enter commands below.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = SubduedGray
                            )
                        }
                    }

                    // Command Entries
                    items(items = entries, key = { it.id }) { entry ->
                        TerminalEntryView(
                            entry = entry,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(entry.stdout.ifEmpty { entry.stderr }))
                                Toast.makeText(context, "Command output copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Execution Progress indicator
                    if (isExecuting) {
                        item {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Executing in Shizuku...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = AccentCyan
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = TerminalBorder, thickness = 1.dp)

            // 4. Command Input & Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalSurface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Command History Navigation (Up/Down)
                if (history.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (historyIndex < history.size - 1) {
                                historyIndex++
                                inputText = history[history.size - 1 - historyIndex]
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Previous command",
                            tint = if (historyIndex < history.size - 1) AccentCyan else SubduedGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (historyIndex > 0) {
                                historyIndex--
                                inputText = history[history.size - 1 - historyIndex]
                            } else if (historyIndex == 0) {
                                historyIndex = -1
                                inputText = ""
                            }
                        },
                        modifier = Modifier.size(32.dp),
                        enabled = historyIndex >= 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Next command",
                            tint = if (historyIndex >= 0) AccentCyan else SubduedGray.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Prompt symbol
                Text(
                    text = "$",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PromptGreen,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        historyIndex = -1
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "e.g. cmd appops get <pkg> 10008",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SubduedGray.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        keyboardType = KeyboardType.Ascii,
                        autoCorrect = false
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isExecuting) {
                                onExecuteCommand(inputText)
                                inputText = ""
                                historyIndex = -1
                            }
                        }
                    ),
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear input",
                                    tint = SubduedGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TerminalBg,
                        unfocusedContainerColor = TerminalBg,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = TerminalBorder,
                        cursorColor = AccentCyan
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Run / Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isExecuting) {
                            onExecuteCommand(inputText)
                            inputText = ""
                            historyIndex = -1
                            keyboardController?.hide()
                        }
                    },
                    enabled = inputText.isNotBlank() && !isExecuting,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (inputText.isNotBlank() && !isExecuting) AccentCyan else TerminalBorder)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Execute Command",
                        tint = if (inputText.isNotBlank() && !isExecuting) Color.Black else SubduedGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalEntryView(
    entry: TerminalEntry,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalSurface.copy(alpha = 0.7f))
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        // Command header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PromptGreen
                )
                Text(
                    text = entry.command,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Exit status pill
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (entry.isSuccess) SuccessBg.copy(alpha = 0.25f) else ErrorBg.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = if (entry.isSuccess) "✓ 0" else "exit ${entry.exitCode}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isSuccess) PromptGreen else ErrorRed,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = entry.timestamp,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SubduedGray
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy output",
                        tint = SubduedGray,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Stdout
        if (entry.stdout.isNotBlank()) {
            Text(
                text = entry.stdout,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = OutputText
            )
        }

        // Stderr
        if (entry.stderr.isNotBlank()) {
            if (entry.stdout.isNotBlank()) Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.stderr,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = ErrorRed
            )
        }

        // If command produced no output
        if (entry.stdout.isBlank() && entry.stderr.isBlank()) {
            Text(
                text = "(command completed with no output)",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = SubduedGray
            )
        }
    }
}
