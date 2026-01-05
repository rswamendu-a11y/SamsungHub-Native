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
import com.samsunghub.app.data.SaleEntry
import com.samsunghub.app.R
import com.samsunghub.app.ui.IndianCurrencyFormatter
import com.samsunghub.app.ui.SalesViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class AnalyticsFragment : Fragment() {

    private val viewModel: SalesViewModel by activityViewModels()
    private val currencyFormatter = IndianCurrencyFormatter()

    private var currentList: List<SaleEntry> = emptyList()
    private var isValueMode = true // true = Revenue, false = Volume

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
                isValueMode = (checkedId == R.id.btnValue)
                updateCharts(currentList)
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

        // Observe Source of Truth directly
        viewModel.salesList.observe(viewLifecycleOwner) { list ->
            currentList = list
            updateCharts(list)
        }
    }

    private fun updateCharts(list: List<SaleEntry>) {
        val view = view ?: return

        // --- PART A: Weekly Chart Logic ---
        val weeklyBuckets = DoubleArray(4) // [0.0, 0.0, 0.0, 0.0]

        list.forEach { sale ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = sale.timestamp
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val index = when {
                day <= 7 -> 0
                day <= 14 -> 1
                day <= 21 -> 2
                else -> 3
            }
            val valueToAdd = if (isValueMode) sale.totalValue else sale.quantity.toDouble()
            weeklyBuckets[index] += valueToAdd
        }

        val weeklyEntries = listOf(
            BarEntry(0f, weeklyBuckets[0].toFloat()),
            BarEntry(1f, weeklyBuckets[1].toFloat()),
            BarEntry(2f, weeklyBuckets[2].toFloat()),
            BarEntry(3f, weeklyBuckets[3].toFloat())
        )

        val chartWeekly = view.findViewById<BarChart>(R.id.chartWeekly)
        val colorPrimary = ContextCompat.getColor(requireContext(), R.color.primaryBlue)
        val labelWeekly = if (isValueMode) "Revenue" else "Volume"
        val formatter = if (isValueMode) currencyFormatter else null

        val dsWeekly = BarDataSet(weeklyEntries, labelWeekly).apply {
            color = colorPrimary
            valueTextSize = 12f
            valueFormatter = formatter
        }

        chartWeekly.apply {
            data = BarData(dsWeekly)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4"))
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            axisLeft.valueFormatter = formatter
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            animateY(500)
            invalidate()
        }

        // --- PART B: Brand Chart Logic ---
        val brandMap = list.groupBy { it.brand }
        val brandEntries = ArrayList<BarEntry>()
        val brandLabels = ArrayList<String>()

        // Calculate totals and sort
        val sortedBrands = brandMap.map { (brand, sales) ->
            val total = if (isValueMode) sales.sumOf { it.totalValue } else sales.sumOf { it.quantity }.toDouble()
            brand to total
        }.sortedByDescending { it.second } // Sort high to low

        sortedBrands.forEachIndexed { index, (brand, total) ->
            brandEntries.add(BarEntry(index.toFloat(), total.toFloat()))
            brandLabels.add(brand)
        }

        val chartBrand = view.findViewById<HorizontalBarChart>(R.id.chartBrand)
        val colorTeal = Color.parseColor("#00897B")
        val labelBrand = if (isValueMode) "Brand Revenue" else "Brand Volume"

        val dsBrand = BarDataSet(brandEntries, labelBrand).apply {
            color = colorTeal
            valueTextSize = 12f
            valueFormatter = formatter
        }

        chartBrand.apply {
            data = BarData(dsBrand)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = IndexAxisValueFormatter(brandLabels)
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.labelCount = brandLabels.size
            axisLeft.valueFormatter = formatter
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            animateY(500)
            invalidate()
        }
    }
}
