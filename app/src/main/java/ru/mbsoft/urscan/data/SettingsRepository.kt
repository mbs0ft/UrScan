package ru.mbsoft.urscan.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("urscan_prefs", Context.MODE_PRIVATE)

    private val _fileFormat = mutableStateOf(FileFormat.valueOf(prefs.getString(KEY_FILE_FORMAT, FileFormat.CSV.name) ?: FileFormat.CSV.name))
    val fileFormat: State<FileFormat> = _fileFormat

    private val _delimiter = mutableStateOf(CsvDelimiter.valueOf(prefs.getString(KEY_DELIMITER, CsvDelimiter.SEMICOLON.name) ?: CsvDelimiter.SEMICOLON.name))
    val delimiter: State<CsvDelimiter> = _delimiter

    private val _groupDuplicates = mutableStateOf(prefs.getBoolean(KEY_GROUP_DUPLICATES, true))
    val groupDuplicates: State<Boolean> = _groupDuplicates

    private val _errorSound = mutableStateOf(prefs.getBoolean(KEY_ERROR_SOUND, true))
    val errorSound: State<Boolean> = _errorSound

    private val _vibrate = mutableStateOf(prefs.getBoolean(KEY_VIBRATE, true))
    val vibrate: State<Boolean> = _vibrate

    private val _language = mutableStateOf(AppLanguage.valueOf(prefs.getString(KEY_LANGUAGE, AppLanguage.AUTO.name) ?: AppLanguage.AUTO.name))
    val language: State<AppLanguage> = _language

    fun setFileFormat(format: FileFormat) {
        _fileFormat.value = format
        prefs.edit().putString(KEY_FILE_FORMAT, format.name).apply()
    }

    fun setDelimiter(del: CsvDelimiter) {
        _delimiter.value = del
        prefs.edit().putString(KEY_DELIMITER, del.name).apply()
    }

    fun setGroupDuplicates(group: Boolean) {
        _groupDuplicates.value = group
        prefs.edit().putBoolean(KEY_GROUP_DUPLICATES, group).apply()
    }

    fun setErrorSound(enabled: Boolean) {
        _errorSound.value = enabled
        prefs.edit().putBoolean(KEY_ERROR_SOUND, enabled).apply()
    }

    fun setVibrate(enabled: Boolean) {
        _vibrate.value = enabled
        prefs.edit().putBoolean(KEY_VIBRATE, enabled).apply()
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs.edit().putString(KEY_LANGUAGE, lang.name).apply()
    }

    fun applyLocale(targetContext: Context): Context {
        val lang = _language.value
        val locale = when (lang) {
            AppLanguage.RU -> Locale("ru")
            AppLanguage.EN -> Locale("en")
            AppLanguage.AUTO -> return targetContext
        }

        Locale.setDefault(locale)
        val config = Configuration(targetContext.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
            return targetContext.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            targetContext.resources.updateConfiguration(config, targetContext.resources.displayMetrics)
            return targetContext
        }
    }

    companion object {
        private const val KEY_FILE_FORMAT = "key_file_format"
        private const val KEY_DELIMITER = "key_delimiter"
        private const val KEY_GROUP_DUPLICATES = "key_group_duplicates"
        private const val KEY_ERROR_SOUND = "key_error_sound"
        private const val KEY_VIBRATE = "key_vibrate"
        private const val KEY_LANGUAGE = "key_language"
    }
}
