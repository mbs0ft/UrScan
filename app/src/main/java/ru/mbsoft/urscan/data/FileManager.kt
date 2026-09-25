package ru.mbsoft.urscan.data

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class FileManager(private val context: Context) {

    fun getStorageDir(): File {
        val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val urScanDir = File(publicDocs, "UrScan")
        if (!urScanDir.exists()) {
            urScanDir.mkdirs()
        }
        return if (urScanDir.canWrite()) {
            urScanDir
        } else {
            val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "UrScan")
            if (!fallback.exists()) fallback.mkdirs()
            fallback
        }
    }

    fun listDocuments(): List<InventoryDocument> {
        val dir = getStorageDir()
        val files = dir.listFiles { file ->
            file.isFile && (file.extension.equals("csv", ignoreCase = true) || file.extension.equals("txt", ignoreCase = true))
        } ?: emptyArray()

        return files.map { file ->
            val (unique, total) = countFileItems(file)
            InventoryDocument(
                file = file,
                name = file.nameWithoutExtension,
                extension = file.extension.lowercase(),
                lastModified = file.lastModified(),
                uniqueCount = unique,
                totalQty = total,
                sizeBytes = file.length()
            )
        }.sortedByDescending { it.lastModified }
    }

    private fun countFileItems(file: File): Pair<Int, Int> {
        var linesCount = 0
        var totalQty = 0
        try {
            file.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    linesCount++
                    val parts = trimmed.split(';', ',', '\t')
                    val qty = if (parts.size >= 2) parts[1].trim().toIntOrNull() ?: 1 else 1
                    totalQty += qty
                }
            }
        } catch (_: Exception) {
        }
        return Pair(linesCount, totalQty)
    }

    fun loadItems(file: File): List<InventoryItem> {
        val items = mutableListOf<InventoryItem>()
        if (!file.exists()) return items

        try {
            file.forEachLine { rawLine ->
                val line = rawLine.trim().removePrefix("\uFEFF")
                if (line.isNotEmpty() && !line.startsWith("#")) {
                    val parts = line.split(';', ',', '\t')
                    val barcode = parts[0].trim()
                    val qty = if (parts.size >= 2) parts[1].trim().toIntOrNull() ?: 1 else 1
                    if (barcode.isNotEmpty()) {
                        items.add(InventoryItem(barcode = barcode, quantity = qty))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    fun saveDocument(
        fileName: String,
        items: List<InventoryItem>,
        format: FileFormat,
        delimiter: CsvDelimiter,
        groupDuplicates: Boolean
    ): File {
        val dir = getStorageDir()
        val safeName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "Inventory" }
        val targetFile = File(dir, "$safeName.${format.ext}")

        val itemsToSave = if (groupDuplicates) {
            val map = linkedMapOf<String, Int>()
            items.forEach { item ->
                map[item.barcode] = (map[item.barcode] ?: 0) + item.quantity
            }
            map.map { InventoryItem(it.key, it.value) }
        } else {
            items
        }

        FileOutputStream(targetFile).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                itemsToSave.forEach { item ->
                    if (format == FileFormat.CSV) {
                        writer.write("${item.barcode}${delimiter.separator}${item.quantity}\r\n")
                    } else {
                        writer.write("${item.barcode}\r\n")
                    }
                }
                writer.flush()
            }
        }

        notifyMediaScanner(targetFile)
        return targetFile
    }

    fun deleteDocument(file: File): Boolean {
        val deleted = file.delete()
        if (deleted) {
            notifyMediaScanner(file)
        }
        return deleted
    }

    fun renameDocument(file: File, newNameWithoutExt: String): File? {
        val safeName = newNameWithoutExt.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        if (safeName.isEmpty()) return null

        val newFile = File(file.parentFile, "$safeName.${file.extension}")
        if (file.renameTo(newFile)) {
            notifyMediaScanner(file)
            notifyMediaScanner(newFile)
            return newFile
        }
        return null
    }

    fun shareDocument(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension.equals("csv", true)) "text/comma-separated-values" else "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, file.name).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun notifyMediaScanner(file: File) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null,
                null
            )
        } catch (_: Exception) {
        }
    }
}
