package com.samsunghub.app.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.*
import com.samsunghub.app.databinding.FragmentAnalyticsBinding
import com.samsunghub.app.data.SaleEntry
import com.samsunghub.app.ui.SalesViewModel
import java.util.Calendar

class AnalyticsFragment : Fragment() {
    private var _b: FragmentAnalyticsBinding? = null
    private val b get() = _b!!
    private val vm: SalesViewModel by activityViewModels()
    private var isVol = false
    private var currentList: List<SaleEntry> = emptyList()

    // Fixed Colors & Brands
    private val cols = intArrayOf(Color.parseColor("#2196F3"), Color.parseColor("#9E9E9E"), Color.parseColor("#4CAF50"), Color.parseColor("#9C27B0"), Color.parseColor("#009688"), Color.parseColor("#FF9800"), Color.parseColor("#3F51B5"), Color.BLACK)
    private val brs = listOf("Samsung", "Apple", "Realme", "Oppo", "Vivo", "Xiaomi", "Moto", "Others")
    private val rngs = listOf("<10K", "10K-15K", "15K-20K", "20K-30K", "30K-40K", "40K-70K", "70K-100K", ">100K")

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentAnalyticsBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        try { b.root.findViewWithTag<View>("top_scorecard")?.visibility = View.GONE } catch (e: Exception) {}
        b.toggleVolume.setOnCheckedChangeListener { _, c -> isVol = c; upd(vm.salesList.value?:emptyList()) }
        vm.salesList.observe(viewLifecycleOwner) { l -> upd(l?:emptyList()) }
    }

    private fun upd(l: List<SaleEntry>) { currentList = l; updWk(l); updBr(l); updTbl(l); b.chartComparison.visibility=View.GONE }
    private fun getV(s: SaleEntry) = if (isVol) s.quantity.toFloat() else s.totalValue.toFloat()

    // K-Formatter for Axis (50k) and Values
    private fun getF(isAxis: Boolean = false): ValueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(v: Float): String {
            if (v == 0f) return "0"
            if (isVol) return v.toInt().toString()
            return if (v >= 1000) "${(v/1000).toInt()}k" else v.toInt().toString()
        }
    }

    private fun updWk(l: List<SaleEntry>) {
        val d = Array(4) { FloatArray(8) }
        l.forEach { s ->
            val c = Calendar.getInstance(); c.timeInMillis = s.timestamp
            val w = when(c.get(Calendar.DAY_OF_MONTH)) { in 1..7->0; in 8..14->1; in 15..21->2; else->3 }
            val idx = brs.indexOf(s.brand); if (idx!=-1) d[w][idx] += getV(s)
        }
        val set = BarDataSet((0..3).map { BarEntry(it.toFloat(), d[it]) }, "Weekly Achievement")
        set.colors = cols.toList(); set.stackLabels = brs.toTypedArray()
        set.valueFormatter = getF(); set.valueTextColor = Color.WHITE // WHITE TEXT INSIDE BARS
        set.valueTextSize = 10f

        b.chartWeekly.apply {
            data = BarData(set)
            xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Wk1","Wk2","Wk3","Wk4"))
            axisLeft.valueFormatter = getF(true) // 50k on Axis
            legend.isWordWrapEnabled=true; legend.orientation=Legend.LegendOrientation.HORIZONTAL
            invalidate()
        }
    }

    private fun updBr(l: List<SaleEntry>) {
        val map = l.groupBy { it.brand }; val ents = ArrayList<BarEntry>(); val labs = ArrayList<String>(); var i = 0f
        brs.forEach { b ->
            val total = map[b]?.sumOf { getV(it).toDouble() }?.toFloat() ?: 0f
            ents.add(BarEntry(i++, total)); labs.add(b)
        }
        val set = BarDataSet(ents, "Brand Performance")
        set.colors = cols.toList() // COLORFUL BARS
        set.valueFormatter = getF(); set.valueTextSize = 10f

        b.chartBrand.apply {
            data = BarData(set); data.barWidth = 0.6f
            xAxis.valueFormatter = IndexAxisValueFormatter(labs); xAxis.granularity=1f; xAxis.labelCount=labs.size
            xAxis.labelRotationAngle = -45f // Rotate labels
            axisLeft.valueFormatter = getF(true)
            axisRight.valueFormatter = getF(true)
            invalidate()
        }
    }

    private fun updTbl(l: List<SaleEntry>) {
        try {
            val t = b.root.findViewById<TableLayout>(com.samsunghub.app.R.id.tablePriceSegments) ?: return
            t.removeAllViews()
            val hr = TableRow(context); hr.addView(tv("Brand", true))
            rngs.forEach { hr.addView(tv(it, true)) }; t.addView(hr)
            brs.forEach { b ->
                val r = TableRow(context); r.addView(tv(b, false, true))
                val sl = l.filter { it.brand == b }
                rngs.indices.forEach { i ->
                    val c = sl.count { s -> val p = s.unitPrice; when(i) { 0->p<10000; 1->p<15000; 2->p<20000; 3->p<30000; 4->p<40000; 5->p<70000; 6->p<100000; else->p>=100000 } }
                    r.addView(tv(if(c>0) c.toString() else "-", false))
                }
                t.addView(r)
            }
        } catch (e: Exception) {}
    }
    private fun tv(txt: String, bg: Boolean=false, bld: Boolean=false): TextView = TextView(context).apply { text=txt; setPadding(8,8,8,8); gravity=Gravity.CENTER; if(bg) setBackgroundColor(Color.LTGRAY); if(bld) setTypeface(null, android.graphics.Typeface.BOLD) }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
