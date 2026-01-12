package com.samsunghub.app.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.samsunghub.app.data.AppDatabase
import com.samsunghub.app.data.SaleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Locale

object BackupManager {
    private const val HDR = "Date,Brand,Model,Variant,Price,Quantity,Segment"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun writeListToCsv(ctx: Context, uri: Uri, list: List<SaleEntry>) {
        withContext(Dispatchers.IO) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { os ->
                    val w = OutputStreamWriter(os)
                    w.append(HDR).append("\n")
                    list.forEach { s ->
                        // EXPORT AS READABLE DATE (YYYY-MM-DD)
                        val dateStr = dateFormat.format(java.util.Date(s.timestamp))
                        w.append("$dateStr,${s.brand},${s.model},${s.variant},${s.unitPrice},${s.quantity},${s.segment}\n")
                    }
                    w.flush()
                }
                withContext(Dispatchers.Main) { Toast.makeText(ctx, "Backup Success", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(ctx, "Err: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    suspend fun importFromCsv(ctx: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(ctx)
                val lines = mutableListOf<String>()
                ctx.contentResolver.openInputStream(uri)?.use { ism ->
                    BufferedReader(InputStreamReader(ism)).use { r ->
                        var l = r.readLine(); while (l != null) { lines.add(l); l = r.readLine() }
                    }
                }
                if (lines.isNotEmpty()) {
                    val data = if (lines[0].startsWith("Date")) lines.drop(1) else lines
                    var count = 0
                    data.forEach { row ->
                        val t = row.split(",")
                        if (t.size >= 6) {
                            // DATE PARSING FIX
                            val dateRaw = t[0]
                            val timestamp: Long = try {
                                if (dateRaw.contains("-")) {
                                    // It is a Date String (2026-01-07)
                                    dateFormat.parse(dateRaw)?.time ?: System.currentTimeMillis()
                                } else {
                                    // It is a Raw Timestamp (Legacy)
                                    dateRaw.toLong()
                                }
                            } catch (e: Exception) {
                                System.currentTimeMillis() // Fallback
                            }

                            val p = t[4].toDoubleOrNull() ?: 0.0
                            val q = t[5].toIntOrNull() ?: 1
                            val seg = com.samsunghub.app.data.SegmentCalculator.getSegment(p)

                            val e = SaleEntry(0, timestamp, t[1], t[2], t[3], p, q, p*q, seg)
                            db.salesDao().insertSale(e)
                            count++
                        }
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(ctx, "Restored $count items", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(ctx, "Err: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }
}
