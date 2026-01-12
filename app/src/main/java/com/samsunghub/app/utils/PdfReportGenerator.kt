package com.samsunghub.app.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.*
import com.samsunghub.app.data.SaleEntry
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {
    private val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val nf = DecimalFormat("#,##,###.00")
    private val blue = DeviceRgb(33, 150, 243)
    private val white = ColorConstants.WHITE

    fun generateReport(ctx: Context, list: List<SaleEntry>, mth: String, out: String, sec: String, type: ReportType): Uri? {
        return try {
            val fileName = "Report_${type.name}_${System.currentTimeMillis()}.pdf"
            val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)

            // Orientation
            if (type == ReportType.MATRIX_ONLY || type == ReportType.MASTER_REPORT) {
                pdf.defaultPageSize = PageSize.A4.rotate()
            } else {
                pdf.defaultPageSize = PageSize.A4
            }

            val doc = Document(pdf)
            doc.setMargins(20f, 20f, 20f, 20f)

            // Header
            val title = when(type) {
                ReportType.MATRIX_ONLY -> "SALES MATRIX"
                ReportType.PRICE_SEGMENT_ONLY -> "PRICE SEGMENT ANALYSIS"
                else -> "SALES REPORT"
            }

            doc.add(Paragraph("$title - $mth").setBold().setFontSize(14f).setTextAlignment(TextAlignment.CENTER).setBackgroundColor(ColorConstants.CYAN).setFontColor(white))
            doc.add(Paragraph("Outlet: $out | SEC: $sec").setBold().setFontSize(10f).setTextAlignment(TextAlignment.CENTER).setBackgroundColor(ColorConstants.LIGHT_GRAY))
            doc.add(Paragraph("\n"))

            when (type) {
                ReportType.MATRIX_ONLY -> mat(doc, list)
                ReportType.PRICE_SEGMENT_ONLY -> seg(doc, list)
                ReportType.DETAILED_ONLY -> {
                    // This is the ONLY mode that shows the full log now
                    brd(doc, list); doc.add(Paragraph("\n"))
                    seg(doc, list); doc.add(Paragraph("\n"))
                    log(doc, list)
                }
                else -> { // MASTER REPORT (Concise)
                    mat(doc, list); doc.add(AreaBreak(AreaBreakType.NEXT_PAGE))
                    brd(doc, list); doc.add(Paragraph("\n"))
                    seg(doc, list)
                    // REMOVED: log(doc, list) -> To save paper space as requested
                }
            }

            doc.close()
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    // --- DRAWING FUNCTIONS ---

    private fun head(t: Table, txt: String) {
        t.addHeaderCell(Cell().add(Paragraph(txt).setBold().setFontSize(9f)).setBackgroundColor(blue).setFontColor(white).setTextAlignment(TextAlignment.CENTER))
    }

    private fun cell(t: Table, txt: String, isBold: Boolean = false, alignLeft: Boolean = false) {
        val p = Paragraph(txt).setFontSize(8f)
        if (!alignLeft) p.setTextAlignment(TextAlignment.CENTER)
        if (isBold) p.setBold()
        t.addCell(Cell().add(p))
    }

    private fun seg(doc: Document, list: List<SaleEntry>) {
        doc.add(Paragraph("Price Segment Analysis").setBold().setFontSize(12f))
        val t = Table(UnitValue.createPercentArray(floatArrayOf(2f,1f,1f,1f,1f,1f,1f,1f,1f))).useAllAvailableWidth()
        listOf("Brand","<10K","10-15K","15-20K","20-30K","30-40K","40-70K","70-100K",">100K").forEach { head(t, it) }
        val brands = listOf("Samsung", "Apple", "Realme", "Oppo", "Vivo", "Xiaomi", "Moto", "Other")
        brands.forEach { b ->
            cell(t, b, true)
            val sl = list.filter { if(b=="Other") !brands.contains(it.brand) && it.brand!="Other" else it.brand==b }
            val cnt = IntArray(8)
            sl.forEach { s ->
                if (s.quantity > 0) {
                    val p = s.unitPrice
                    val i = when { p<10000->0; p<15000->1; p<20000->2; p<30000->3; p<40000->4; p<70000->5; p<100000->6; else->7 }
                    cnt[i] += s.quantity
                }
            }
            cnt.forEach { c -> cell(t, if(c>0) c.toString() else "-") }
        }
        doc.add(t)
    }

    private fun brd(doc: Document, list: List<SaleEntry>) {
        doc.add(Paragraph("Brand Performance").setBold().setFontSize(12f))
        val t = Table(UnitValue.createPercentArray(floatArrayOf(2f,1f,1f))).useAllAvailableWidth()
        listOf("Brand", "Total Units", "Total Revenue").forEach { head(t, it) }
        var gq=0; var gv=0.0
        list.groupBy { it.brand }.forEach { (b, l) ->
            val q = l.sumOf { it.quantity }; val v = l.sumOf { it.totalValue }
            gq+=q; gv+=v
            cell(t, b); cell(t, q.toString()); cell(t, nf.format(v))
        }
        cell(t, "GRAND TOTAL", true); cell(t, gq.toString(), true); cell(t, nf.format(gv), true)
        doc.add(t)
    }

    private fun log(doc: Document, list: List<SaleEntry>) {
        doc.add(Paragraph("Transaction Log").setBold().setFontSize(12f))
        val t = Table(UnitValue.createPercentArray(floatArrayOf(1.5f,1.5f,2f,1.5f,1.5f,0.8f,1.5f))).useAllAvailableWidth()
        listOf("Date","Brand","Model","Variant","Price","Qty","Total").forEach { head(t, it) }
        list.sortedByDescending { it.timestamp }.forEach { s ->
            cell(t, df.format(Date(s.timestamp))); cell(t, s.brand); cell(t, s.model); cell(t, s.variant)
            cell(t, nf.format(s.unitPrice)); cell(t, s.quantity.toString()); cell(t, nf.format(s.totalValue))
        }
        doc.add(t)
    }

    private fun mat(doc: Document, list: List<SaleEntry>) {
        doc.add(Paragraph("Matrix Overview").setBold().setFontSize(12f))
        val t = Table(UnitValue.createPercentArray(floatArrayOf(1.2f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.8f, 0.8f, 3f))).useAllAvailableWidth()

        val brands = listOf("Samsung","Apple","Oppo","Vivo","Realme","Xiaomi","Moto","Other","TOTAL")
        head(t, "Date"); brands.forEach { head(t, it) }; head(t, "Tot"); head(t, "Sam%"); head(t, "Logs")

        val dates = list.groupBy { df.format(Date(it.timestamp)) }.toSortedMap()
        val gq = IntArray(9)
        var grandTotalQty = 0
        var grandSamQty = 0

        dates.forEach { (d, dl) ->
            cell(t, d, true)
            var dq = 0
            var dSam = 0
            val dayLogs = ArrayList<String>()

            for (i in 0 until 8) {
                val bName = brands[i]
                val bSales = dl.filter { if(bName=="Other") !brands.contains(it.brand) && it.brand!="Other" && it.brand!="TOTAL" else it.brand == bName }
                val q = bSales.sumOf { it.quantity }
                val v = bSales.sumOf { it.totalValue }
                gq[i] += q
                dq += q
                if(bName=="Samsung") dSam = q
                if(q > 0) bSales.forEach { s -> dayLogs.add("${s.brand} ${s.model}") }
                cell(t, if (q > 0) "$q (${(v/1000).toInt()}k)" else "-")
            }
            gq[8] += dq
            grandTotalQty += dq
            grandSamQty += dSam

            val dv = dl.sumOf { it.totalValue }
            cell(t, "$dq (${(dv/1000).toInt()}k)", true)

            val share = if (dq > 0) (dSam.toDouble() / dq * 100).toInt() else 0
            cell(t, "$share%", true)

            val logString = if(dayLogs.isNotEmpty()) dayLogs.joinToString(", ") else "-"
            cell(t, logString, false, true)
        }

        cell(t, "TOTAL", true)
        for(i in 0 until 8) cell(t, "${gq[i]}", true)
        cell(t, "$grandTotalQty", true)

        val grandShare = if (grandTotalQty > 0) (grandSamQty.toDouble() / grandTotalQty * 100).toInt() else 0
        cell(t, "$grandShare%", true)
        cell(t, "", false)

        doc.add(t)
    }
}
