package com.samsunghub.app.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.samsunghub.app.databinding.FragmentAnalyticsBinding
import com.samsunghub.app.data.SaleEntry
import com.samsunghub.app.ui.SalesViewModel
import java.util.Calendar

class AnalyticsFragment : Fragment() {
    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SalesViewModel by activityViewModels()
    private var isVolumeMode = false
    private var currentList: List<SaleEntry> = emptyList()
    private val brandColors = intArrayOf(Color.parseColor("#2196F3"), Color.parseColor("#9E9E9E"), Color.parseColor("#4CAF50"), Color.parseColor("#9C27B0"), Color.parseColor("#009688"), Color.parseColor("#FF9800"), Color.parseColor("#3F51B5"), Color.BLACK)
    private val brands = listOf("Samsung", "Apple", "Realme", "Oppo", "Vivo", "Xiaomi", "Moto", "Others")

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        binding.toggleVolume.setOnCheckedChangeListener { _, b ->
            isVolumeMode = b
            updateAllCharts(currentList)
        }
        viewModel.salesList.observe(viewLifecycleOwner) { l ->
            currentList = l ?: emptyList()
            updateAllCharts(currentList)
        }
    }

    private fun updateAllCharts(list: List<SaleEntry>) {
        updateComparisonChart(list)
        updateWeeklyStackedChart(list)
        updateBrandChart(list)
    }

    private fun getVal(s: SaleEntry): Float = if (isVolumeMode) s.quantity.toFloat() else s.totalValue.toFloat()

    private fun getFmt(): ValueFormatter {
        return if (isVolumeMode) object : ValueFormatter() { override fun getFormattedValue(v: Float) = v.toInt().toString() }
        else object : ValueFormatter() { override fun getFormattedValue(v: Float) = if(v>0) "${(v/1000).toInt()}k" else "" }
    }

    private fun updateComparisonChart(list: List<SaleEntry>) {
        val mtd = list.sumOf { getVal(it).toDouble() }.toFloat()
        val set = BarDataSet(listOf(BarEntry(0f, mtd), BarEntry(1f, 0f)), "Performance")
        set.colors = listOf(Color.parseColor("#3F51B5"), Color.LTGRAY)
        set.valueFormatter = getFmt()
        val data = BarData(set)
        binding.chartComparison.data = data
        binding.chartComparison.description.isEnabled = false
        binding.chartComparison.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("MTD", "LMTD"))
        binding.chartComparison.invalidate()
    }

    private fun updateWeeklyStackedChart(list: List<SaleEntry>) {
        val weeklyData = Array(4) { FloatArray(8) }
        list.forEach { s ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = s.timestamp
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val wk = when { d<=7->0; d<=14->1; d<=21->2; else->3 }
            val bIdx = brands.indexOf(s.brand)
            if (bIdx != -1) weeklyData[wk][bIdx] += getVal(s)
        }
        val entries = (0..3).map { BarEntry(it.toFloat(), weeklyData[it]) }
        val set = BarDataSet(entries, "Weekly")
        set.colors = brandColors.toList()
        set.stackLabels = brands.toTypedArray()
        set.valueFormatter = getFmt()
        set.valueTextColor = Color.BLACK
        binding.chartWeekly.data = BarData(set)
        binding.chartWeekly.description.isEnabled = false
        binding.chartWeekly.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Wk1","Wk2","Wk3","Wk4"))
        binding.chartWeekly.invalidate()
    }

    private fun updateBrandChart(list: List<SaleEntry>) {
        val map = list.groupBy { it.brand }
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var i = 0f
        map.forEach { (b, sales) ->
            entries.add(BarEntry(i++, sales.sumOf { getVal(it).toDouble() }.toFloat()))
            labels.add(b)
        }
        val set = BarDataSet(entries, "Brand")
        set.color = Color.parseColor("#009688")
        set.valueFormatter = getFmt()
        val data = BarData(set)
        data.barWidth = 0.5f
        binding.chartBrand.data = data
        binding.chartBrand.description.isEnabled = false
        binding.chartBrand.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.chartBrand.invalidate()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
