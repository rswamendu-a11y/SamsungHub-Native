package com.samsunghub.app.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.samsunghub.app.data.SaleEntry
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val TAG = "PdfReportGenerator"
    private val HEADER_BG_COLOR = DeviceRgb(33, 150, 243) // Blue
    private val HEADER_TEXT_COLOR = ColorConstants.WHITE

    suspend fun generateMonthlyReport(
        context: Context,
        salesList: List<SaleEntry>,
        monthName: String
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
                val document = Document(pdf)

                // 1. Header
                addHeader(document, "Sales Report - $monthName")

                // 2. Table 1: Transaction Log
                addSectionTitle(document, "1. Transaction Log")
                createTransactionLogTable(document, salesList)

                // 3. Table 2: Brand Performance
                addSectionTitle(document, "2. Brand Performance")
                createBrandPerformanceTable(document, salesList)

                // 4. Table 3: Segment Analysis
                addSectionTitle(document, "3. Price Segment Analysis")
                createSegmentAnalysisTable(document, salesList)

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
            .setMarginBottom(20f)
        document.add(paragraph)
    }

    private fun addSectionTitle(document: Document, text: String) {
        val paragraph = Paragraph(text)
            .setBold()
            .setFontSize(14f)
            .setMarginTop(15f)
            .setMarginBottom(5f)
        document.add(paragraph)
    }

    private fun createTransactionLogTable(document: Document, salesList: List<SaleEntry>) {
        // Date | Brand | Model | Variant | Price | Qty | Total
        val table = Table(UnitValue.createPercentArray(floatArrayOf(15f, 15f, 15f, 15f, 15f, 10f, 15f)))
            .useAllAvailableWidth()

        // Headers
        val headers = listOf("Date", "Brand", "Model", "Variant", "Price", "Qty", "Total")
        headers.forEach { addCell(table, it, isHeader = true) }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        for (sale in salesList) {
            addCell(table, dateFormat.format(Date(sale.timestamp)))
            addCell(table, sale.brand)
            addCell(table, sale.model)
            addCell(table, sale.variant)
            addCell(table, currencyFormat.format(sale.unitPrice))
            addCell(table, sale.quantity.toString())
            addCell(table, currencyFormat.format(sale.totalValue))
        }

        document.add(table)
    }

    private fun createBrandPerformanceTable(document: Document, salesList: List<SaleEntry>) {
        // Brand | Units | Revenue
        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 30f, 30f)))
            .useAllAvailableWidth()

        val headers = listOf("Brand", "Total Units", "Total Revenue")
        headers.forEach { addCell(table, it, isHeader = true) }

        val brandGroups = salesList.groupBy { it.brand }
        var grandTotalUnits = 0
        var grandTotalRevenue = 0.0

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        for ((brand, sales) in brandGroups) {
            val totalUnits = sales.sumOf { it.quantity }
            val totalRevenue = sales.sumOf { it.totalValue }

            grandTotalUnits += totalUnits
            grandTotalRevenue += totalRevenue

            addCell(table, brand)
            addCell(table, totalUnits.toString())
            addCell(table, currencyFormat.format(totalRevenue))
        }

        // Grand Total Row
        addCell(table, "GRAND TOTAL", isBold = true)
        addCell(table, grandTotalUnits.toString(), isBold = true)
        addCell(table, currencyFormat.format(grandTotalRevenue), isBold = true)

        document.add(table)
    }

    private fun createSegmentAnalysisTable(document: Document, salesList: List<SaleEntry>) {
        // Segment | Brand | Units
        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 40f, 20f)))
            .useAllAvailableWidth()

        val headers = listOf("Price Segment", "Brand", "Units Sold")
        headers.forEach { addCell(table, it, isHeader = true) }

        // Logical Sort Order
        val segmentOrder = listOf(
            "Entry (<10k)",
            "10k-20k",
            "20k-30k",
            "30k-40k",
            "40k-50k",
            "Premium (>50k)"
        )

        val salesBySegment = salesList.groupBy { it.segment }

        for (segmentName in segmentOrder) {
            val salesInSegment = salesBySegment[segmentName]
            if (!salesInSegment.isNullOrEmpty()) {
                val salesByBrand = salesInSegment.groupBy { it.brand }
                for ((brand, sales) in salesByBrand) {
                    val units = sales.sumOf { it.quantity }
                    addCell(table, segmentName)
                    addCell(table, brand)
                    addCell(table, units.toString())
                }
            }
        }

        // Handle any segments not in the logical order (fallback)
        val knownSegments = segmentOrder.toSet()
        val otherSegments = salesBySegment.keys.filter { !knownSegments.contains(it) }

        for (segmentName in otherSegments) {
            val salesInSegment = salesBySegment[segmentName] ?: continue
            val salesByBrand = salesInSegment.groupBy { it.brand }
            for ((brand, sales) in salesByBrand) {
                val units = sales.sumOf { it.quantity }
                addCell(table, segmentName)
                addCell(table, brand)
                addCell(table, units.toString())
            }
        }

        document.add(table)
    }

    private fun addCell(table: Table, text: String, isHeader: Boolean = false, isBold: Boolean = false) {
        val cell = Cell().add(Paragraph(text))

        if (isHeader) {
            cell.setBackgroundColor(HEADER_BG_COLOR)
            cell.setFontColor(HEADER_TEXT_COLOR)
            cell.setBold()
            cell.setTextAlignment(TextAlignment.CENTER)
        } else {
            if (isBold) cell.setBold()
            cell.setTextAlignment(TextAlignment.LEFT)
        }

        table.addCell(cell)
    }
}
