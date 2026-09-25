package ru.mbsoft.urscan.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.mbsoft.urscan.R
import ru.mbsoft.urscan.data.AppLanguage
import ru.mbsoft.urscan.data.CsvDelimiter
import ru.mbsoft.urscan.data.FileFormat
import ru.mbsoft.urscan.data.SettingsRepository
import ru.mbsoft.urscan.update.GitHubUpdateChecker
import ru.mbsoft.urscan.update.UpdateCheckResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    onBack: () -> Unit,
    onLanguageChanged: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var downloadProgress by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = stringResource(R.string.pref_audio_section))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_error_sound),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.pref_error_sound_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = settingsRepo.errorSound.value,
                            onCheckedChange = { settingsRepo.setErrorSound(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_haptic),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.pref_haptic_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = settingsRepo.vibrate.value,
                            onCheckedChange = { settingsRepo.setVibrate(it) }
                        )
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.pref_export_section))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.pref_file_format),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { settingsRepo.setFileFormat(FileFormat.CSV) }
                        ) {
                            RadioButton(
                                selected = settingsRepo.fileFormat.value == FileFormat.CSV,
                                onClick = { settingsRepo.setFileFormat(FileFormat.CSV) }
                            )
                            Text(stringResource(R.string.pref_format_csv))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { settingsRepo.setFileFormat(FileFormat.TXT) }
                        ) {
                            RadioButton(
                                selected = settingsRepo.fileFormat.value == FileFormat.TXT,
                                onClick = { settingsRepo.setFileFormat(FileFormat.TXT) }
                            )
                            Text(stringResource(R.string.pref_format_txt))
                        }
                    }

                    if (settingsRepo.fileFormat.value == FileFormat.CSV) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.pref_delimiter),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CsvDelimiter.values().forEach { del ->
                            val label = when (del) {
                                CsvDelimiter.SEMICOLON -> stringResource(R.string.pref_delimiter_semicolon)
                                CsvDelimiter.COMMA -> stringResource(R.string.pref_delimiter_comma)
                                CsvDelimiter.TAB -> stringResource(R.string.pref_delimiter_tab)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { settingsRepo.setDelimiter(del) }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = settingsRepo.delimiter.value == del,
                                    onClick = { settingsRepo.setDelimiter(del) }
                                )
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.pref_grouping),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.pref_grouping_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = settingsRepo.groupDuplicates.value,
                            onCheckedChange = { settingsRepo.setGroupDuplicates(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.pref_storage_location),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.pref_storage_location_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.pref_general_section))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.pref_language),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    listOf(
                        AppLanguage.AUTO to stringResource(R.string.lang_auto),
                        AppLanguage.RU to stringResource(R.string.lang_ru),
                        AppLanguage.EN to stringResource(R.string.lang_en)
                    ).forEach { (lang, title) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepo.setLanguage(lang)
                                    onLanguageChanged()
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settingsRepo.language.value == lang,
                                onClick = {
                                    settingsRepo.setLanguage(lang)
                                    onLanguageChanged()
                                }
                            )
                            Text(title)
                        }
                    }
                }
            }

            SectionHeader(title = stringResource(R.string.pref_about_section))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.about_app_name),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_version, "1.0.0"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GitHubUpdateChecker.GITHUB_REPO_URL)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_github),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isCheckingUpdates = true
                            updateResult = null
                            coroutineScope.launch {
                                val result = GitHubUpdateChecker.checkForUpdates("1.0.0")
                                isCheckingUpdates = false
                                updateResult = result
                            }
                        },
                        enabled = !isCheckingUpdates,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.checking_updates))
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.check_updates))
                        }
                    }

                    updateResult?.let { res ->
                        Spacer(modifier = Modifier.height(12.dp))
                        when (res) {
                            is UpdateCheckResult.UpToDate -> {
                                Text(
                                    text = stringResource(R.string.up_to_date),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            is UpdateCheckResult.Available -> {
                                Column {
                                    Text(
                                        text = stringResource(R.string.update_available, res.newVersion),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (downloadProgress != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { (downloadProgress!!.toFloat() / 100f) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "${downloadProgress}%",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val apkFile = GitHubUpdateChecker.downloadApk(
                                                        context,
                                                        res.downloadUrl
                                                    ) { progress ->
                                                        downloadProgress = progress
                                                    }
                                                    downloadProgress = null
                                                    if (apkFile != null && apkFile.exists()) {
                                                        GitHubUpdateChecker.installApk(context, apkFile)
                                                    } else {
                                                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(stringResource(R.string.download_update))
                                        }
                                    }
                                }
                            }
                            is UpdateCheckResult.Error -> {
                                Text(
                                    text = stringResource(R.string.update_failed),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}
