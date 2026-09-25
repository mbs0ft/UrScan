package ru.mbsoft.urscan.data

import java.io.File

data class InventoryItem(
    val barcode: String,
    var quantity: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

data class InventoryDocument(
    val file: File,
    val name: String,
    val extension: String,
    val lastModified: Long,
    val uniqueCount: Int,
    val totalQty: Int,
    val sizeBytes: Long
)

enum class FileFormat(val ext: String) {
    CSV("csv"),
    TXT("txt")
}

enum class CsvDelimiter(val separator: String, val displayNameResName: String) {
    SEMICOLON(";", "pref_delimiter_semicolon"),
    COMMA(",", "pref_delimiter_comma"),
    TAB("\t", "pref_delimiter_tab")
}

enum class AppLanguage(val code: String) {
    AUTO("auto"),
    RU("ru"),
    EN("en")
}
