package com.samsunghub.app.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.samsunghub.app.data.SaleEntry
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val TAG = "PdfReportGenerator"
    private val HEADER_BG_COLOR = DeviceRgb(33, 150, 243)
    private val HEADER_TEXT_COLOR = ColorConstants.WHITE

    suspend fun generateMonthlyReport(
        context: Context,
        salesList: List<SaleEntry>,
        monthName: String,
        outletName: String,
        secName: String
    ): Uri? {
        return try {
            val fileName = "Sales_Report_${monthName.replace(" ", "_")}.pdf"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/SalesReports")
            }

            val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                ?: return null

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = PdfWriter(outputStream)
                val pdf = PdfDocument(writer)
                // Matrix Report -> Landscape A4
                pdf.setDefaultPageSize(PageSize.A4.rotate())
                val document = Document(pdf)

                // 1. Header
                addHeader(document, "SALES MATRIX REPORT - $monthName")
                addSubHeader(document, "Outlet: $outletName | SEC: $secName")

                // 2. Matrix Table
                createMatrixTable(document, salesList)

                document.close()
            }
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF", e)
            null
        }
    }

    private fun addHeader(document: Document, text: String) {
        val paragraph = Paragraph(text)
            .setBold()
            .setFontSize(18f)
            .setTextAlignment(TextAlignment.CENTER)
            .setBackgroundColor(HEADER_BG_COLOR)
            .setFontColor(HEADER_TEXT_COLOR)
            .setPadding(10f)
            .setMarginBottom(5f)
        document.add(paragraph)
    }

    private fun addSubHeader(document: Document, text: String) {
        val paragraph = Paragraph(text)
            .setBold()
            .setFontSize(12f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
        document.add(paragraph)
    }

    private fun createMatrixTable(document: Document, salesList: List<SaleEntry>) {
        // Columns: Date | Samsung | Apple | Oppo | Vivo | Realme | Xiaomi | Moto | Others | Total
        // Total 10 columns.
        val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f)))
            .useAllAvailableWidth()

        val brands = listOf("Samsung", "Apple", "Oppo", "Vivo", "Realme", "Xiaomi", "Moto", "Other")

        // Header Row
        addCell(table, "Date", isHeader = true)
        brands.forEach { addCell(table, it, isHeader = true) }
        addCell(table, "TOTAL", isHeader = true)

        // Group by Date
        val salesByDate = salesList.groupBy {
            // Normalize to start of day
            val d = Date(it.timestamp)
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(d)
        }

        // Sort by Date
        val sortedDates = salesByDate.keys.sortedBy {
             SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(it)?.time ?: 0L
        }

        // Column Totals Init
        val colTotalQty = IntArray(brands.size + 1) // +1 for Grand Total
        val colTotalVal = DoubleArray(brands.size + 1)

        val formatCompact = { qty: Int, value: Double ->
            if (qty == 0) "-" else "$qty (${formatValue(value)})"
        }

        for (dateStr in sortedDates) {
            val dailySales = salesByDate[dateStr] ?: emptyList()

            // Date Cell
            addCell(table, dateStr)

            var dailyTotalQty = 0
            var dailyTotalVal = 0.0

            // Brand Cells
            brands.forEachIndexed { index, brand ->
                // Handle "Other" vs "Others" if naming varies, strict logic:
                val brandSales = dailySales.filter {
                    if (brand == "Other") it.brand !in brands.subList(0, 7) // If brand is none of top 7, it's Other
                    else it.brand.equals(brand, ignoreCase = true)
                }

                val qty = brandSales.sumOf { it.quantity }
                val value = brandSales.sumOf { it.totalValue }

                dailyTotalQty += qty
                dailyTotalVal += value

                colTotalQty[index] += qty
                colTotalVal[index] += value

                addCell(table, formatCompact(qty, value))
            }

            // Daily Total Cell
            colTotalQty[brands.size] += dailyTotalQty
            colTotalVal[brands.size] += dailyTotalVal
            addCell(table, formatCompact(dailyTotalQty, dailyTotalVal), isBold = true)
        }

        // Grand Total Row
        addCell(table, "TOTAL", isHeader = true)
        brands.forEachIndexed { index, _ ->
             addCell(table, formatCompact(colTotalQty[index], colTotalVal[index]), isBold = true)
        }
        addCell(table, formatCompact(colTotalQty[brands.size], colTotalVal[brands.size]), isBold = true)

        document.add(table)
    }

    private fun addCell(table: Table, text: String, isHeader: Boolean = false, isBold: Boolean = false) {
        val cell = Cell().add(Paragraph(text).setFontSize(9f)) // Smaller font for matrix

        if (isHeader) {
            cell.setBackgroundColor(HEADER_BG_COLOR)
            cell.setFontColor(HEADER_TEXT_COLOR)
            cell.setBold()
            cell.setTextAlignment(TextAlignment.CENTER)
        } else {
            if (isBold) cell.setBold()
            cell.setTextAlignment(TextAlignment.CENTER)
        }
        table.addCell(cell)
    }

    private fun formatValue(value: Double): String {
        val formatK = DecimalFormat("#.#k")
        val formatL = DecimalFormat("#.#L")
        return when {
            value >= 100_000 -> formatL.format(value / 100_000)
            value >= 1_000 -> formatK.format(value / 1_000)
            else -> value.toInt().toString()
        }
    }
}
