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

enum class ReportType { MATRIX_ONLY, DETAILED_ONLY, MASTER_REPORT, PRICE_SEGMENT_ONLY }

object PdfReportGenerator {

    private const val TAG = "PdfReportGenerator"
    private val HEADER_BG_COLOR = DeviceRgb(33, 150, 243)
    private val HEADER_TEXT_COLOR = ColorConstants.WHITE

    suspend fun generateReport(
        context: Context,
        salesList: List<SaleEntry>,
        monthName: String,
        outletName: String,
        secName: String,
        type: ReportType
    ): Uri? {
        return try {
            val typeSuffix = when(type) {
                ReportType.MATRIX_ONLY -> "Matrix"
                ReportType.DETAILED_ONLY -> "Detailed"
                ReportType.MASTER_REPORT -> "Master"
                ReportType.PRICE_SEGMENT_ONLY -> "Price_Segment_Analysis"
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

                if (type == ReportType.PRICE_SEGMENT_ONLY) {
                    addHeader(document, headerTitle)
                    addSubHeader(document, "Outlet: $outletName | SEC: $secName")
                    drawSegmentAnalysis(document, salesList)
                }
                else if (type == ReportType.MATRIX_ONLY) {
                    addHeader(document, headerTitle)
                    addSubHeader(document, "Outlet: $outletName | SEC: $secName")
                    drawMatrixTable(document, salesList)
                }
                else if (type == ReportType.DETAILED_ONLY) {
                    addHeader(document, headerTitle)
                    addSubHeader(document, "Outlet: $outletName | SEC: $secName")
                    drawBrandPerformance(document, salesList)
                    drawSegmentAnalysis(document, salesList)
                    document.add(AreaBreak(AreaBreakType.NEXT_PAGE))
                    drawTransactionLog(document, salesList)
                }
                else if (type == ReportType.MASTER_REPORT) {
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

    // --- 1. SUPER MATRIX TABLE (Landscape) ---
    private fun drawMatrixTable(document: Document, salesList: List<SaleEntry>) {
        // Brands list
        val brands = listOf("Samsung", "Apple", "Realme", "Oppo", "Vivo", "Xiaomi", "Moto", "Other")

        // Define column widths: Date(1) + 8 Brands * 2 (Q,V) + Total(Q,V) + Logs(3) = 1 + 16 + 2 + 3 = 22 parts
        // Let's approximate percentages
        val widths = FloatArray(20)
        widths[0] = 3f // Date (Reduced width for more space)
        for (i in 1..18) widths[i] = 2.6f // Q, V columns (Increased width)
        widths[19] = 10f // Logs

        val table = Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth()

        // --- Header Row 1 ---
        // Date (Rowspan 2)
        table.addCell(Cell(2, 1).add(Paragraph("Date").setFontSize(8f).setBold())
            .setBackgroundColor(HEADER_BG_COLOR).setFontColor(HEADER_TEXT_COLOR).setTextAlignment(TextAlignment.CENTER))

        // Brands (Colspan 2)
        brands.forEach { brand ->
            table.addCell(Cell(1, 2).add(Paragraph(brand).setFontSize(8f).setBold())
                .setBackgroundColor(HEADER_BG_COLOR).setFontColor(HEADER_TEXT_COLOR).setTextAlignment(TextAlignment.CENTER))
        }
        // Total (Colspan 2)
        table.addCell(Cell(1, 2).add(Paragraph("TOTAL").setFontSize(8f).setBold())
            .setBackgroundColor(HEADER_BG_COLOR).setFontColor(HEADER_TEXT_COLOR).setTextAlignment(TextAlignment.CENTER))

        // Logs (Rowspan 2)
        table.addCell(Cell(2, 1).add(Paragraph("Logs").setFontSize(8f).setBold())
            .setBackgroundColor(HEADER_BG_COLOR).setFontColor(HEADER_TEXT_COLOR).setTextAlignment(TextAlignment.CENTER))

        // --- Header Row 2 ---
        // Q | V sub-headers
        for (i in 0..brands.size) { // 8 brands + 1 total = 9 pairs
            table.addCell(Cell().add(Paragraph("Q").setFontSize(8f).setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER))
            table.addCell(Cell().add(Paragraph("V").setFontSize(8f).setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER))
        }

        // --- Data Processing ---
        val salesByDate = salesList.groupBy {
            val d = Date(it.timestamp)
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(d)
        }
        val sortedDates = salesByDate.keys.sortedBy {
             SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(it)?.time ?: 0L
        }

        // Column Totals
        val colTotalQty = IntArray(brands.size + 1)
        val colTotalVal = DoubleArray(brands.size + 1)

        // --- Data Rows ---
        for (dateStr in sortedDates) {
            val dailySales = salesByDate[dateStr] ?: emptyList()

            // Date Cell
            addCell(table, dateStr.substring(0, 5), textSize = 9f) // dd-MM

            var dailyTotalQty = 0
            var dailyTotalVal = 0.0

            // Brand Columns
            brands.forEachIndexed { index, brand ->
                val brandSales = dailySales.filter {
                    if (brand == "Other") it.brand !in brands.subList(0, 7) // Exclude known brands
                    else it.brand.equals(brand, ignoreCase = true) || (brand == "Moto" && it.brand == "Motorola")
                }

                val qty = brandSales.sumOf { it.quantity }
                val value = brandSales.sumOf { it.totalValue }

                dailyTotalQty += qty
                dailyTotalVal += value
                colTotalQty[index] += qty
                colTotalVal[index] += value

                // Qty Cell
                addCell(table, if (qty > 0) qty.toString() else "-", textSize = 9f, align = TextAlignment.CENTER)
                // Value Cell
                addCell(table, if (value > 0) formatValueSmall(value) else "-", textSize = 9f, align = TextAlignment.CENTER)
            }

            // Daily Total Columns
            colTotalQty[brands.size] += dailyTotalQty
            colTotalVal[brands.size] += dailyTotalVal

            addCell(table, dailyTotalQty.toString(), isBold = true, textSize = 9f, align = TextAlignment.CENTER)
            addCell(table, formatValueSmall(dailyTotalVal), isBold = true, textSize = 9f, align = TextAlignment.CENTER)

            // Logs Cell
            val logText = dailySales.joinToString(", ") {
                "${it.brand} ${it.model} ${it.variant}"
            }
            addCell(table, logText, textSize = 7f, align = TextAlignment.LEFT)
        }

        // --- Grand Total Row ---
        addCell(table, "TOTAL", isBold = true, textSize = 9f)
        brands.forEachIndexed { index, _ ->
            addCell(table, colTotalQty[index].toString(), isBold = true, textSize = 9f, align = TextAlignment.CENTER)
            addCell(table, formatValueSmall(colTotalVal[index]), isBold = true, textSize = 9f, align = TextAlignment.CENTER)
        }
        // Total of Totals
        addCell(table, colTotalQty[brands.size].toString(), isBold = true, textSize = 9f, align = TextAlignment.CENTER)
        addCell(table, formatValueSmall(colTotalVal[brands.size]), isBold = true, textSize = 9f, align = TextAlignment.CENTER)

        // Empty Log footer
        addCell(table, "", textSize = 9f)

        document.add(table)
    }

    private fun formatValueSmall(value: Double): String {
        return (value / 1000).toInt().toString() + "k"
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

    // --- 3. SEGMENT ANALYSIS (GRID) ---
    private fun drawSegmentAnalysis(document: Document, salesList: List<SaleEntry>) {
        addSectionTitle(document, "Price Segment Analysis (Brand-Wise)")

        // 9 Columns: Brand + 8 Ranges
        val table = Table(UnitValue.createPercentArray(9)).useAllAvailableWidth()

        // Headers
        val headers = listOf("Brand", "<10K", "10-15K", "15-20K", "20-30K", "30-40K", "40-70K", "70-100K", ">100K")
        headers.forEach { addCell(table, it, isHeader = true, textSize = 8f) }

        val brands = listOf("Samsung", "Apple", "Realme", "Oppo", "Vivo", "Xiaomi", "Moto", "Other")
        val segments = listOf("<10K", "10K - 15K", "15K - 20K", "20K - 30K", "30K - 40K", "40K - 70K", "70K - 100K", ">100K")

        // Recalculate segments on the fly to ensure accuracy with new ranges
        val salesWithSegments = salesList.map { sale ->
             // Determine segment using current SegmentCalculator logic (which matches headers)
             val segment = com.samsunghub.app.data.SegmentCalculator.getSegment(sale.totalValue / sale.quantity)
             sale to segment
        }

        for (brand in brands) {
            // Brand Column
            addCell(table, brand, isBold = true, textSize = 8f)

            for (segment in segments) {
                // Filter: Match brand AND segment
                val count = salesWithSegments.count { (sale, seg) ->
                     val saleBrand = sale.brand
                     val matchBrand = if (brand == "Other") saleBrand !in brands.subList(0, 7)
                                      else saleBrand.equals(brand, ignoreCase = true) || (brand == "Moto" && saleBrand == "Motorola")

                     matchBrand && seg == segment
                }

                val display = if (count > 0) count.toString() else "-"
                addCell(table, display, textSize = 8f, align = TextAlignment.CENTER)
            }
        }

        document.add(table)
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

    private fun addCell(table: Table, text: String, isHeader: Boolean = false, isBold: Boolean = false, textSize: Float = 9f, align: TextAlignment = TextAlignment.LEFT) {
        val cell = Cell().add(Paragraph(text).setFontSize(if(isHeader) 10f else textSize))

        if (isHeader) {
            cell.setBackgroundColor(HEADER_BG_COLOR)
            cell.setFontColor(HEADER_TEXT_COLOR)
            cell.setBold()
            cell.setTextAlignment(TextAlignment.CENTER)
        } else {
            if (isBold) cell.setBold()
            cell.setTextAlignment(align)
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
