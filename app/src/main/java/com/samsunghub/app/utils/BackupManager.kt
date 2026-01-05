package com.samsunghub.app.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.samsunghub.app.data.SaleEntry
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

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

    fun importDatabaseFromExcel(context: Context, data: android.content.Intent): List<SaleEntry>? {
        val uri = data.data ?: return null
        return importFromExcel(context, uri)
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
