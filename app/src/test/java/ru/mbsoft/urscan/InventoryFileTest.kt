package ru.mbsoft.urscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mbsoft.urscan.data.CsvDelimiter
import ru.mbsoft.urscan.data.FileFormat
import ru.mbsoft.urscan.data.InventoryItem
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class InventoryFileTest {

    @Test
    fun testGroupingDuplicates() {
        val items = listOf(
            InventoryItem("123456", 1),
            InventoryItem("789012", 2),
            InventoryItem("123456", 3)
        )

        val map = linkedMapOf<String, Int>()
        items.forEach { item ->
            map[item.barcode] = (map[item.barcode] ?: 0) + item.quantity
        }
        val grouped = map.map { InventoryItem(it.key, it.value) }

        assertEquals(2, grouped.size)
        assertEquals("123456", grouped[0].barcode)
        assertEquals(4, grouped[0].quantity)
        assertEquals("789012", grouped[1].barcode)
        assertEquals(2, grouped[1].quantity)
    }

    @Test
    fun testCsvSerializationWithBOM() {
        val items = listOf(
            InventoryItem("ИНВ-00123", 2),
            InventoryItem("ИНВ-00456", 1)
        )

        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        OutputStreamWriter(out, StandardCharsets.UTF_8).use { writer ->
            items.forEach { item ->
                writer.write("${item.barcode}${CsvDelimiter.SEMICOLON.separator}${item.quantity}\r\n")
            }
            writer.flush()
        }

        val resultBytes = out.toByteArray()
        assertTrue(resultBytes.size > 3)
        assertEquals(0xEF.toByte(), resultBytes[0])
        assertEquals(0xBB.toByte(), resultBytes[1])
        assertEquals(0xBF.toByte(), resultBytes[2])

        val text = String(resultBytes, 3, resultBytes.size - 3, StandardCharsets.UTF_8)
        val lines = text.trim().split("\r\n")
        assertEquals(2, lines.size)
        assertEquals("ИНВ-00123;2", lines[0])
        assertEquals("ИНВ-00456;1", lines[1])
    }
}
