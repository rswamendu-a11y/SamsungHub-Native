package com.samsunghub.app.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.samsunghub.app.R
import com.samsunghub.app.ui.IndianCurrencyFormatter
import com.samsunghub.app.ui.SalesViewModel
import com.samsunghub.app.ui.WeeklyStat
import java.text.NumberFormat
import java.util.Locale

class AnalyticsFragment : Fragment() {

    private val viewModel: SalesViewModel by activityViewModels()
    private val currencyFormatter = IndianCurrencyFormatter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toggleGroup = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleGroup)
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isValue = checkedId == R.id.btnValue
                viewModel.setChartMode(isValue)
            }
        }

        observeData(view)
    }

    private fun observeData(view: View) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        viewModel.mtdTotal.observe(viewLifecycleOwner) { total ->
            view.findViewById<TextView>(R.id.tvMtdTotal).text = currencyFormat.format(total)
        }
        viewModel.lmtdTotal.observe(viewLifecycleOwner) { total ->
            view.findViewById<TextView>(R.id.tvLmtdTotal).text = currencyFormat.format(total)
        }

        // Observe Chart Data (Merged logic handled in ViewModel ideally, or fragment logic)
        // Since we need to switch between Value/Volume, let's assume ViewModel emits WeeklyStat
        // that contains BOTH revenue and count, or we need separate LiveData.
        // For simplicity, let's assume ViewModel has `weeklyStats` (value) and `weeklyVolume` (count).
        // I will add `weeklyVolume` to ViewModel.

        viewModel.chartMode.observe(viewLifecycleOwner) { isValue ->
             updateCharts(view, isValue)
        }
    }

    private fun updateCharts(view: View, isValue: Boolean) {
        val weeklyData = if (isValue) viewModel.weeklyStats.value else viewModel.weeklyVolume.value
        val brandData = if (isValue) viewModel.brandStats.value else viewModel.brandVolume.value

        if (weeklyData != null) setupWeeklyChart(view, weeklyData, isValue)
        if (brandData != null) setupBrandChart(view, brandData, isValue)
    }

    private fun setupWeeklyChart(view: View, stats: List<WeeklyStat>, isValue: Boolean) {
        val chart = view.findViewById<BarChart>(R.id.chartWeekly)
        val entries = stats.mapIndexed { index, stat ->
            val value = if (isValue) stat.revenue else stat.count.toDouble()
            BarEntry(index.toFloat(), value.toFloat())
        }

        val colorPrimary = ContextCompat.getColor(requireContext(), R.color.primaryBlue)
        val label = if (isValue) "Revenue" else "Volume"
        val dataSet = BarDataSet(entries, label).apply {
            color = colorPrimary
            valueTextSize = 12f
            valueFormatter = currencyFormatter
        }

        val labels = stats.map { it.label }
        chart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(labels as Collection<String>)
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.valueFormatter = currencyFormatter
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun setupBrandChart(view: View, stats: Map<String, Double>, isValue: Boolean) {
        val chart = view.findViewById<HorizontalBarChart>(R.id.chartBrand)
        val sortedStats = stats.toList().sortedByDescending { it.second }
        val entries = sortedStats.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())
        }

        val colorTeal = Color.parseColor("#00897B")
        val label = if (isValue) "Brand Revenue" else "Brand Volume"
        val formatter = if (isValue) currencyFormatter else null // Null uses default (simple numbers)

        val dataSet = BarDataSet(entries, label).apply {
            color = colorTeal
            valueTextSize = 12f
            valueFormatter = formatter
        }

        val labels = sortedStats.map { it.first }
        chart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.labelCount = labels.size
            axisLeft.valueFormatter = formatter
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }
}
