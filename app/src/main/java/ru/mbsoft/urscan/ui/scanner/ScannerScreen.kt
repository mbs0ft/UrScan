package ru.mbsoft.urscan.ui.scanner

import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.mbsoft.urscan.R
import ru.mbsoft.urscan.data.FileManager
import ru.mbsoft.urscan.data.InventoryItem
import ru.mbsoft.urscan.data.SettingsRepository
import ru.mbsoft.urscan.scanner.SoundManager
import ru.mbsoft.urscan.scanner.UrovoScanReceiver
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    fileName: String,
    existingFile: File?,
    fileManager: FileManager,
    settingsRepo: SettingsRepository,
    soundManager: SoundManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val screenFocusRequester = remember { FocusRequester() }

    val items = remember {
        val initialList = if (existingFile != null) {
            fileManager.loadItems(existingFile)
        } else {
            emptyList()
        }
        mutableStateListOf<InventoryItem>().apply { addAll(initialList) }
    }

    var lastScannedBarcode by remember { mutableStateOf(items.firstOrNull()?.barcode ?: "") }
    var manualBarcode by remember { mutableStateOf("") }
    var keyboardScanBuffer by remember { mutableStateOf("") }

    var lastScanTime by remember { mutableStateOf(0L) }
    var lastScannedCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }

    fun saveCurrentState() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                fileManager.saveDocument(
                    fileName = fileName,
                    items = items.toList(),
                    format = settingsRepo.fileFormat.value,
                    delimiter = settingsRepo.delimiter.value,
                    groupDuplicates = settingsRepo.groupDuplicates.value
                )
            } catch (_: Exception) {}
        }
    }

    fun handleScannedBarcode(rawCode: String) {
        val code = rawCode.trim()
        if (code.isEmpty()) return

        val currentTime = System.currentTimeMillis()
        if (code == lastScannedCode && (currentTime - lastScanTime) < 150) {
            return
        }
        lastScannedCode = code
        lastScanTime = currentTime

        if (settingsRepo.vibrate.value) {
            soundManager.vibrate(60)
        }
        soundManager.playSuccess()

        val group = settingsRepo.groupDuplicates.value
        val existingIndex = items.indexOfFirst { it.barcode == code }

        if (group && existingIndex != -1) {
            val existing = items[existingIndex]
            items[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            items.add(0, InventoryItem(barcode = code, quantity = 1))
        }

        lastScannedBarcode = code
        saveCurrentState()
    }

    DisposableEffect(Unit) {
        val receiver = UrovoScanReceiver { scannedCode ->
            handleScannedBarcode(scannedCode)
        }
        UrovoScanReceiver.register(context, receiver)

        onDispose {
            UrovoScanReceiver.unregister(context, receiver)
            saveCurrentState()
        }
    }

    val totalUnique = items.size
    val totalQty = items.sumOf { it.quantity }

    Scaffold(
        modifier = Modifier
            .focusRequester(screenFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                    if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                        if (keyboardScanBuffer.isNotBlank()) {
                            handleScannedBarcode(keyboardScanBuffer)
                            keyboardScanBuffer = ""
                            true
                        } else false
                    } else {
                        val unicodeChar = keyEvent.nativeKeyEvent.unicodeChar
                        if (unicodeChar > 31 && unicodeChar != 127) {
                            keyboardScanBuffer += unicodeChar.toChar()
                            true
                        } else false
                    }
                } else false
            },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = fileName,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.items_count, totalUnique, totalQty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            saveCurrentState()
                            Toast.makeText(
                                context,
                                context.getString(R.string.saved_toast, fileName),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.status_hardware_scanner),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.last_scanned).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (lastScannedBarcode.isNotEmpty()) lastScannedBarcode else "—",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    placeholder = { Text(stringResource(R.string.barcode_input_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (manualBarcode.isNotBlank()) {
                                handleScannedBarcode(manualBarcode)
                                manualBarcode = ""
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (manualBarcode.isNotBlank()) {
                            handleScannedBarcode(manualBarcode)
                            manualBarcode = ""
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(stringResource(R.string.manual_add))
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_scan_list),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(items, key = { index, item -> "${item.barcode}_$index" }) { index, item ->
                        ScannedItemRow(
                            item = item,
                            onIncrement = {
                                items[index] = item.copy(quantity = item.quantity + 1)
                                saveCurrentState()
                            },
                            onDecrement = {
                                if (item.quantity > 1) {
                                    items[index] = item.copy(quantity = item.quantity - 1)
                                    saveCurrentState()
                                } else {
                                    items.removeAt(index)
                                    saveCurrentState()
                                }
                            },
                            onDelete = {
                                items.removeAt(index)
                                saveCurrentState()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedItemRow(
    item: InventoryItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.barcode,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "-",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                FilledTonalIconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "+",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
