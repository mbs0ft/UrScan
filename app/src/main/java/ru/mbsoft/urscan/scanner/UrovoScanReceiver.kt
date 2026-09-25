package ru.mbsoft.urscan.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

class UrovoScanReceiver(
    private val onBarcodeScanned: (barcode: String) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val barcode = extractBarcode(intent)
        if (!barcode.isNullOrBlank()) {
            onBarcodeScanned(barcode.trim())
        }
    }

    private fun extractBarcode(intent: Intent): String? {
        intent.getStringExtra("barcode_string")?.let { return it }

        val barcodeBytes = intent.getByteArrayExtra("barcode")
        if (barcodeBytes != null && barcodeBytes.isNotEmpty()) {
            val length = intent.getIntExtra("length", barcodeBytes.size)
            val text = String(barcodeBytes, 0, length.coerceAtMost(barcodeBytes.size)).trim()
            if (text.isNotEmpty()) return text
        }

        intent.getStringExtra("barcode")?.let { return it }
        intent.getStringExtra("value")?.let { return it }
        intent.getStringExtra("data")?.let { return it }
        intent.getStringExtra("android.device.scanner.extra.BARCODE")?.let { return it }
        intent.getStringExtra("SCAN_RESULT")?.let { return it }

        return null
    }

    companion object {
        val INTENT_ACTIONS = arrayOf(
            "android.intent.ACTION_DECODE_DATA",
            "action.BARCODE_DECODE_DATA",
            "urovo.rcv.message",
            "android.intent.action.SCANRESULT",
            "com.android.server.scannerservice.broadcast",
            "com.android.server.scannerservice.urovo.broadcast"
        )

        fun createIntentFilter(): IntentFilter {
            val filter = IntentFilter()
            INTENT_ACTIONS.forEach { filter.addAction(it) }
            return filter
        }

        fun register(context: Context, receiver: BroadcastReceiver) {
            val filter = createIntentFilter()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        }

        fun unregister(context: Context, receiver: BroadcastReceiver) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }
}
