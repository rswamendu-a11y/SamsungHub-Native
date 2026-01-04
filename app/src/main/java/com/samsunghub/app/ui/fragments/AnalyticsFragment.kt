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
        viewModel.weeklyStats.observe(viewLifecycleOwner) { stats ->
            setupWeeklyChart(view, stats)
        }
        viewModel.brandStats.observe(viewLifecycleOwner) { stats ->
            setupBrandChart(view, stats)
        }
    }

    private fun setupWeeklyChart(view: View, stats: List<WeeklyStat>) {
        val chart = view.findViewById<BarChart>(R.id.chartWeekly)
        val entries = stats.mapIndexed { index, stat ->
            BarEntry(index.toFloat(), stat.revenue.toFloat())
        }

        val colorPrimary = ContextCompat.getColor(requireContext(), R.color.primaryBlue)
        val dataSet = BarDataSet(entries, "Revenue").apply {
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
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.valueFormatter = currencyFormatter
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }

    private fun setupBrandChart(view: View, stats: Map<String, Double>) {
        val chart = view.findViewById<HorizontalBarChart>(R.id.chartBrand)
        val sortedStats = stats.toList().sortedByDescending { it.second }
        val entries = sortedStats.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())
        }

        val colorTeal = Color.parseColor("#00897B")
        val dataSet = BarDataSet(entries, "Brand Revenue").apply {
            color = colorTeal
            valueTextSize = 12f
            valueFormatter = currencyFormatter
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
            axisLeft.valueFormatter = currencyFormatter
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }
}
