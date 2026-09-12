package com.fix.notification.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fix.notification.model.TerminalEntry

// AMOLED Dark Terminal Theme
private val TerminalBg = Color(0xFF090D12)
private val TerminalSurface = Color(0xFF131920)
private val TerminalCardBg = Color(0xFF161E28)
private val TerminalBorder = Color(0xFF2A3441)
private val PromptColor = Color(0xFF38D39F)
private val OutputTextColor = Color(0xFFD1D7E0)
private val ErrorColor = Color(0xFFFF5F56)
private val AccentCyan = Color(0xFF58A6FF)
private val SubduedGray = Color(0xFF7D8590)
private val DotRed = Color(0xFFFF5F56)
private val DotYellow = Color(0xFFFFBD2E)
private val DotGreen = Color(0xFF27C93F)

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
        contentColor = OutputTextColor,
        dragHandle = null,
        windowInsets = WindowInsets.safeDrawing
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .imePadding()
        ) {
            // 1. macOS / Modern Terminal Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Window decoration dots & session identity
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Terminal 3-dot window buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(DotRed)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(DotYellow)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(DotGreen)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "shizuku@android",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Shizuku UID status badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isShizukuGranted) Color(0xFF1F3D2B) else Color(0xFF3D1F23)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isShizukuGranted) PromptColor else ErrorColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isShizukuGranted) "uid=2000" else "offline",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isShizukuGranted) PromptColor else ErrorColor
                            )
                        }
                    }
                }

                // Right: Actions (Copy All, Clear, Close)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy session
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
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy session",
                            tint = SubduedGray,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Clear terminal
                    IconButton(
                        onClick = { onClear() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear terminal",
                            tint = SubduedGray,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Close sheet
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SubduedGray,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = TerminalBorder, thickness = 1.dp)

            // 2. Terminal Console Screen
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Minimalist Terminal Welcome Banner
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(TerminalSurface.copy(alpha = 0.5f))
                                .border(1.dp, TerminalBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Fix Notification ADB Shell [Shizuku uid=2000]",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Execute ADB shell commands directly. Type 'help' for examples or 'clear' to wipe buffer.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = SubduedGray
                            )
                        }
                    }

                    // Command Entries
                    items(items = entries, key = { it.id }) { entry ->
                        TerminalEntryView(
                            entry = entry,
                            onCopy = {
                                val textToCopy = entry.stdout.ifEmpty { entry.stderr }
                                if (textToCopy.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    // Execution Progress indicator
                    if (isExecuting) {
                        item {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = PromptColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Executing in Shizuku shell...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = PromptColor
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = TerminalBorder, thickness = 1.dp)

            // 3. Command Input & History Bar
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
                            modifier = Modifier.size(18.dp)
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
                            tint = if (historyIndex >= 0) AccentCyan else SubduedGray.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Shell Prompt Symbol
                Text(
                    text = "❯",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PromptColor,
                    modifier = Modifier.padding(start = 4.dp, end = 6.dp)
                )

                // Input Box
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
                            color = SubduedGray.copy(alpha = 0.5f)
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
                                    modifier = Modifier.size(15.dp)
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
                        cursorColor = PromptColor
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Run / Execute Button
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (inputText.isNotBlank() && !isExecuting) AccentCyan else TerminalBorder.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Execute Command",
                        tint = if (inputText.isNotBlank() && !isExecuting) Color.Black else SubduedGray,
                        modifier = Modifier.size(16.dp)
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
            .clip(RoundedCornerShape(6.dp))
            .background(TerminalCardBg)
            .border(1.dp, TerminalBorder.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        // Command header: prompt + command on left, status + timestamp + copy on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❯ ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PromptColor
                )
                Text(
                    text = entry.command,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Exit code status badge
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = if (entry.isSuccess) Color(0xFF1F3D2B) else Color(0xFF3D1F23)
                ) {
                    Text(
                        text = if (entry.isSuccess) "✓ 0" else "✗ ${entry.exitCode}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isSuccess) PromptColor else ErrorColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = entry.timestamp,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SubduedGray
                )

                Spacer(modifier = Modifier.width(2.dp))

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy output",
                        tint = SubduedGray,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stdout
        if (entry.stdout.isNotBlank()) {
            Text(
                text = entry.stdout,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = OutputTextColor
            )
        }

        // Stderr
        if (entry.stderr.isNotBlank()) {
            if (entry.stdout.isNotBlank()) Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = entry.stderr,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = ErrorColor
            )
        }

        // Empty output indicator
        if (entry.stdout.isBlank() && entry.stderr.isBlank()) {
            Text(
                text = "(no output)",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontStyle = FontStyle.Italic,
                color = SubduedGray
            )
        }
    }
}
