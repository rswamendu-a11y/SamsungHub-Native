package com.samsunghub.app.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.samsunghub.app.data.AppDatabase
import com.samsunghub.app.data.SaleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    // Alias for the user-requested function name
    fun exportDatabaseToExcel(context: Context) {
        // In a real scenario, this would get data from DB.
        // For now, we will just call exportToExcel with an empty list or need to access Repository.
        // Since BackupManager is an object, it doesn't have access to Repository/DAO directly without context.
        // However, the previous implementation of exportToExcel TOOK a list.
        // The user snippet calls exportDatabaseToExcel(context) with NO list.
        // This means BackupManager needs to fetch data itself.

        // I will launch a coroutine to fetch data and export.
        // BUT, I don't have easy access to the DB instance unless I use AppDatabase.

        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val db = com.samsunghub.app.data.AppDatabase.getDatabase(context)
            // collect from flow? No, flow is for observation.
            // I need a direct fetch method in DAO?
            // Existing DAO: getAllSales() returns Flow.
            // I can't easily get list from Flow in a one-off without collecting.
            // I should add 'suspend fun getAllSalesList(): List<SaleEntry>' to DAO?
            // Or just use 'first()' on the flow?

            // Wait, I can't modify DAO easily without seeing it again (I saw it earlier).
            // It has 'getAllSales(): Flow'.
            // I'll try to collect it.

            val sales = try {
                kotlinx.coroutines.flow.first(db.salesDao().getAllSales())
            } catch (e: Exception) {
                emptyList()
            }
            val success = exportToExcel(context, sales)

            withContext(Dispatchers.Main) {
                if (success) {
                    android.widget.Toast.makeText(context, "Backup Saved to Documents", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Backup Failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportToExcel(context: Context, salesList: List<SaleEntry>): Boolean {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Master Backup")

            // Header
            val headerRow = sheet.createRow(0)
            val headers = arrayOf("ID", "Timestamp", "Brand", "Model", "Variant", "Price", "Qty", "Total", "Segment")
            headers.forEachIndexed { index, title ->
                headerRow.createCell(index).setCellValue(title)
            }

            // Data
            salesList.forEachIndexed { index, sale ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(sale.id.toDouble())
                row.createCell(1).setCellValue(sale.timestamp.toDouble())
                row.createCell(2).setCellValue(sale.brand)
                row.createCell(3).setCellValue(sale.model)
                row.createCell(4).setCellValue(sale.variant)
                row.createCell(5).setCellValue(sale.unitPrice)
                row.createCell(6).setCellValue(sale.quantity.toDouble())
                row.createCell(7).setCellValue(sale.totalValue)
                row.createCell(8).setCellValue(sale.segment)
            }

            val fileName = "SamsungHub_Backup_${System.currentTimeMillis()}.xlsx"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            }

            val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                ?: return false

            context.contentResolver.openOutputStream(uri)?.use { os ->
                workbook.write(os)
            }
            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importDatabaseFromExcel(context: Context, data: android.content.Intent?) {
        if (data == null || data.data == null) {
            Toast.makeText(context, "Import failed: No file selected", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = data.data!!

        GlobalScope.launch(Dispatchers.IO) {
            val list = importFromExcel(context, uri)
            if (list != null) {
                val db = AppDatabase.getDatabase(context)
                val dao = db.salesDao()
                dao.deleteAllSales()
                list.forEach { dao.insertSale(it) }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Database Restored Successfully", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Import Failed: Invalid File", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun importFromExcel(context: Context, uri: Uri): List<SaleEntry>? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            val list = mutableListOf<SaleEntry>()

            // Skip header (row 0)
            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                // Assuming strict column order from export
                // Safely reading cells (handling numeric vs string)

                // Helper to get string safely
                fun getStr(idx: Int): String = row.getCell(idx)?.toString() ?: ""
                fun getNum(idx: Int): Double = try { row.getCell(idx)?.numericCellValue ?: 0.0 } catch(e:Exception) { 0.0 }

                // ID is ignored on import (auto-gen) or we preserve it?
                // Usually for "Restore" we might want to clear and re-insert.
                // Let's create new entries to be safe with auto-increment, or strictly restore.
                // Requirement says "Reads .xlsx and replaces the database".
                // So we will return the list, and Repository will handle the "Replace" logic (Delete All -> Insert All).

                val timestamp = getNum(1).toLong()
                val brand = getStr(2)
                val model = getStr(3)
                val variant = getStr(4)
                val price = getNum(5)
                val qty = getNum(6).toInt()

                // Factory handles total/segment
                val entry = SaleEntry.create(timestamp, brand, model, variant, price, qty)
                list.add(entry)
            }

            workbook.close()
            list
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
