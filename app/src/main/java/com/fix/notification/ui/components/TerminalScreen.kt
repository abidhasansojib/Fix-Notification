package com.fix.notification.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    isShizukuGranted: Boolean,
    entries: List<TerminalEntry>,
    isExecuting: Boolean,
    history: List<String>,
    onExecuteCommand: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    // Intercept hardware and gesture back navigation
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var inputState by remember { mutableStateOf(TextFieldValue("")) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new command output arrives or when keyboard opens
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    LaunchedEffect(entries.size, isExecuting, imeBottom) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size)
        }
    }

    val customTextSelectionColors = remember {
        TextSelectionColors(
            handleColor = AccentCyan,
            backgroundColor = AccentCyan.copy(alpha = 0.4f)
        )
    }

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalBg)
        ) {
            // 1. Fixed Top App Bar (Padded for status bar via TopAppBarDefaults)
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to apps",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ADB Shell",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )

                        // Status badge
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
                },
                actions = {
                    // Copy entire session transcript
                    IconButton(
                        onClick = {
                            if (entries.isNotEmpty()) {
                                val transcript = entries.joinToString("\n\n") { entry ->
                                    val out = if (entry.stdout.isNotBlank()) "\n${entry.stdout}" else ""
                                    val err = if (entry.stderr.isNotBlank()) "\n[stderr]\n${entry.stderr}" else ""
                                    "❯ ${entry.command}$out$err (exit ${entry.exitCode})"
                                }
                                clipboardManager.setText(AnnotatedString(transcript))
                                Toast.makeText(context, "Full session transcript copied (${entries.size} commands)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No commands in session to copy", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy all output",
                            tint = SubduedGray
                        )
                    }

                    // Clear terminal buffer
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear terminal",
                            tint = SubduedGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = SubduedGray
                )
            )

            HorizontalDivider(color = TerminalBorder, thickness = 1.dp)

            // 2. Terminal Console Screen (Flexibly occupies all remaining vertical space)
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
                    // Banner
                    item {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TerminalSurface.copy(alpha = 0.5f))
                                    .border(1.dp, TerminalBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "ADB Shell",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
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

                    // Executing Progress indicator
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

            // 3. Command Input Bar (Dynamically wraps long commands, smoothly elevated above keyboard)
            Surface(
                color = TerminalSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Command History Navigation (Up/Down)
                    if (history.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (historyIndex < history.size - 1) {
                                        historyIndex++
                                        val cmd = history[history.size - 1 - historyIndex]
                                        inputState = TextFieldValue(
                                            text = cmd,
                                            selection = TextRange(cmd.length)
                                        )
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
                                        val cmd = history[history.size - 1 - historyIndex]
                                        inputState = TextFieldValue(
                                            text = cmd,
                                            selection = TextRange(cmd.length)
                                        )
                                    } else if (historyIndex == 0) {
                                        historyIndex = -1
                                        inputState = TextFieldValue("")
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
                    }

                    // Prompt indicator
                    Text(
                        text = "❯",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PromptColor,
                        modifier = Modifier.padding(start = 4.dp, end = 6.dp, bottom = 12.dp)
                    )

                    // Input Field (Auto-wraps up to 4 lines, easy to view and tap to edit long commands)
                    OutlinedTextField(
                        value = inputState,
                        onValueChange = { newValue ->
                            inputState = newValue
                            historyIndex = -1
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                text = "Command (e.g. pm list, appops)",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                color = SubduedGray.copy(alpha = 0.5f)
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color.White
                        ),
                        singleLine = false,
                        minLines = 1,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            keyboardType = KeyboardType.Ascii,
                            autoCorrect = false
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val cmd = inputState.text.trim()
                                if (cmd.isNotBlank() && !isExecuting) {
                                    onExecuteCommand(cmd)
                                    inputState = TextFieldValue("")
                                    historyIndex = -1
                                }
                            }
                        ),
                        trailingIcon = {
                            if (inputState.text.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputState = TextFieldValue("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
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

                    // Run / Send Button
                    IconButton(
                        onClick = {
                            val cmd = inputState.text.trim()
                            if (cmd.isNotBlank() && !isExecuting) {
                                onExecuteCommand(cmd)
                                inputState = TextFieldValue("")
                                historyIndex = -1
                                keyboardController?.hide()
                            }
                        },
                        enabled = inputState.text.isNotBlank() && !isExecuting,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (inputState.text.isNotBlank() && !isExecuting) AccentCyan else TerminalBorder.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Execute Command",
                            tint = if (inputState.text.isNotBlank() && !isExecuting) Color.Black else SubduedGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
    SelectionContainer {
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
}
