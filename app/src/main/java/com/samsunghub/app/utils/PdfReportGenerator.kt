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
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.AreaBreakType
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.samsunghub.app.data.SaleEntry
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportType { MATRIX, DETAILED, MASTER }

object PdfReportGenerator {

    private const val TAG = "PdfReportGenerator"
    private val HEADER_BG_COLOR = DeviceRgb(33, 150, 243)
    private val HEADER_TEXT_COLOR = ColorConstants.WHITE

    suspend fun generateMonthlyReport(
        context: Context,
        salesList: List<SaleEntry>,
        monthName: String,
        outletName: String,
        secName: String,
        type: ReportType
    ): Uri? {
        return try {
            val typeSuffix = when(type) {
                ReportType.MATRIX -> "Matrix"
                ReportType.DETAILED -> "Detailed"
                ReportType.MASTER -> "Master"
            }
            val fileName = "Sales_Report_${typeSuffix}_${monthName.replace(" ", "_")}.pdf"

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

                // Set Page Orientation
                if (type == ReportType.DETAILED) {
                    pdf.setDefaultPageSize(PageSize.A4)
                } else {
                    pdf.setDefaultPageSize(PageSize.A4.rotate())
                }

                val document = Document(pdf)

                // Common Header Info
                val headerTitle = "SALES REPORT ($typeSuffix) - $monthName"

                if (type == ReportType.MATRIX) {
                    addHeader(document, headerTitle)
                    addSubHeader(document, "Outlet: $outletName | SEC: $secName")
                    drawMatrixTable(document, salesList)
                }
                else if (type == ReportType.DETAILED) {
                    addHeader(document, headerTitle)
                    addSubHeader(document, "Outlet: $outletName | SEC: $secName")
                    drawBrandPerformance(document, salesList)
                    drawSegmentAnalysis(document, salesList)
                    document.add(AreaBreak(AreaBreakType.NEXT_PAGE))
                    drawTransactionLog(document, salesList)
                }
                else if (type == ReportType.MASTER) {
                    // Landscape Page 1
                    addHeader(document, headerTitle)
                    addSubHeader(document, "Outlet: $outletName | SEC: $secName")
                    drawMatrixTable(document, salesList)

                    document.add(AreaBreak(AreaBreakType.NEXT_PAGE))

                    // Page 2+ (Landscape to keep it simple, tables will stretch)
                    drawBrandPerformance(document, salesList)
                    drawSegmentAnalysis(document, salesList)

                    document.add(AreaBreak(AreaBreakType.NEXT_PAGE))
                    drawTransactionLog(document, salesList)
                }

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

    private fun addSectionTitle(document: Document, text: String) {
        val paragraph = Paragraph(text)
            .setBold()
            .setFontSize(14f)
            .setMarginTop(15f)
            .setMarginBottom(5f)
        document.add(paragraph)
    }

    // --- 1. MATRIX TABLE (Landscape) ---
    private fun drawMatrixTable(document: Document, salesList: List<SaleEntry>) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f)))
            .useAllAvailableWidth()

        val brands = listOf("Samsung", "Apple", "Oppo", "Vivo", "Realme", "Xiaomi", "Moto", "Other")

        addCell(table, "Date", isHeader = true)
        brands.forEach { addCell(table, it, isHeader = true) }
        addCell(table, "TOTAL", isHeader = true)

        val salesByDate = salesList.groupBy {
            val d = Date(it.timestamp)
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(d)
        }
        val sortedDates = salesByDate.keys.sortedBy {
             SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(it)?.time ?: 0L
        }

        val colTotalQty = IntArray(brands.size + 1)
        val colTotalVal = DoubleArray(brands.size + 1)

        val formatCompact = { qty: Int, value: Double ->
            if (qty == 0) "-" else "$qty (${formatValue(value)})"
        }

        for (dateStr in sortedDates) {
            val dailySales = salesByDate[dateStr] ?: emptyList()
            addCell(table, dateStr)

            var dailyTotalQty = 0
            var dailyTotalVal = 0.0

            brands.forEachIndexed { index, brand ->
                val brandSales = dailySales.filter {
                    if (brand == "Other") it.brand !in brands.subList(0, 7)
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

            colTotalQty[brands.size] += dailyTotalQty
            colTotalVal[brands.size] += dailyTotalVal
            addCell(table, formatCompact(dailyTotalQty, dailyTotalVal), isBold = true)
        }

        addCell(table, "TOTAL", isHeader = true)
        brands.forEachIndexed { index, _ ->
             addCell(table, formatCompact(colTotalQty[index], colTotalVal[index]), isBold = true)
        }
        addCell(table, formatCompact(colTotalQty[brands.size], colTotalVal[brands.size]), isBold = true)

        document.add(table)
    }

    // --- 2. BRAND PERFORMANCE ---
    private fun drawBrandPerformance(document: Document, salesList: List<SaleEntry>) {
        addSectionTitle(document, "Brand Performance Summary")

        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 30f, 30f)))
            .useAllAvailableWidth()

        listOf("Brand", "Total Units", "Total Revenue").forEach { addCell(table, it, isHeader = true) }

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

        addCell(table, "GRAND TOTAL", isBold = true)
        addCell(table, grandTotalUnits.toString(), isBold = true)
        addCell(table, currencyFormat.format(grandTotalRevenue), isBold = true)

        document.add(table)
    }

    // --- 3. SEGMENT ANALYSIS ---
    private fun drawSegmentAnalysis(document: Document, salesList: List<SaleEntry>) {
        addSectionTitle(document, "Price Segment Analysis")

        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 40f, 20f)))
            .useAllAvailableWidth()

        listOf("Price Segment", "Brand", "Units Sold").forEach { addCell(table, it, isHeader = true) }

        val segmentOrder = listOf(
            "Entry (<10k)", "10k-30k", "30k-40k", "40k-70k", "70k-100k", "Premier (>100k)"
        )

        val salesBySegment = salesList.groupBy { it.segment }

        // Ordered segments
        for (segmentName in segmentOrder) {
            processSegment(table, segmentName, salesBySegment[segmentName])
        }

        // Other segments
        val knownSegments = segmentOrder.toSet()
        val otherSegments = salesBySegment.keys.filter { !knownSegments.contains(it) }
        for (segmentName in otherSegments) {
            processSegment(table, segmentName, salesBySegment[segmentName])
        }

        document.add(table)
    }

    private fun processSegment(table: Table, segmentName: String, salesInSegment: List<SaleEntry>?) {
        if (!salesInSegment.isNullOrEmpty()) {
            val salesByBrand = salesInSegment.groupBy { it.brand }
            for ((brand, sales) in salesByBrand) {
                addCell(table, segmentName)
                addCell(table, brand)
                addCell(table, sales.sumOf { it.quantity }.toString())
            }
        }
    }

    // --- 4. TRANSACTION LOG ---
    private fun drawTransactionLog(document: Document, salesList: List<SaleEntry>) {
        addSectionTitle(document, "Detailed Transaction Log")

        val table = Table(UnitValue.createPercentArray(floatArrayOf(15f, 15f, 15f, 15f, 15f, 10f, 15f)))
            .useAllAvailableWidth()

        listOf("Date", "Brand", "Model", "Variant", "Price", "Qty", "Total").forEach { addCell(table, it, isHeader = true) }

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

    private fun addCell(table: Table, text: String, isHeader: Boolean = false, isBold: Boolean = false) {
        val cell = Cell().add(Paragraph(text).setFontSize(if(isHeader) 10f else 9f))

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
