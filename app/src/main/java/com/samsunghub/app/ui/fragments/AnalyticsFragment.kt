package com.samsunghub.app.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.Legend
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

    private val fixedBrands = listOf("Samsung", "Apple", "Realme", "Oppo", "Vivo", "Xiaomi", "Motorola", "Others")
    private val brandColors = listOf(
        Color.parseColor("#1428A0"), // Samsung
        Color.parseColor("#555555"), // Apple
        Color.parseColor("#FFC107"), // Realme
        Color.parseColor("#4CAF50"), // Oppo
        Color.parseColor("#2196F3"), // Vivo
        Color.parseColor("#FF5722"), // Xiaomi
        Color.parseColor("#673AB7"), // Motorola
        Color.parseColor("#9E9E9E")  // Others
    )

    private lateinit var spinnerBrand: Spinner
    private lateinit var tableSegments: TableLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerBrand = view.findViewById(R.id.spinnerSegmentBrand)
        tableSegments = view.findViewById(R.id.tableSegments)

        setupSpinner()

        val toggleGroup = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleGroup)
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isValueMode = (checkedId == R.id.btnValue)
                updateCharts(currentList)
            }
        }

        observeData(view)
    }

    private fun setupSpinner() {
        val brandList = mutableListOf("All Brands")
        brandList.addAll(fixedBrands)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, brandList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBrand.adapter = adapter

        spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateSegmentTable(currentList)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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
            updateSegmentTable(list)
        }
    }

    private fun updateCharts(list: List<SaleEntry>) {
        val view = view ?: return

        // --- PART A: Weekly Stacked Chart Logic ---
        val weeklyData = Array(4) { FloatArray(fixedBrands.size) }

        list.forEach { sale ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = sale.timestamp
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val weekIndex = when {
                day <= 7 -> 0
                day <= 14 -> 1
                day <= 21 -> 2
                else -> 3
            }

            var brandIndex = fixedBrands.indexOf(sale.brand)
            if (brandIndex == -1) brandIndex = fixedBrands.lastIndex // "Others"

            val valueToAdd = if (isValueMode) sale.totalValue.toFloat() else sale.quantity.toFloat()
            weeklyData[weekIndex][brandIndex] += valueToAdd
        }

        val weeklyEntries = weeklyData.mapIndexed { index, floats ->
            BarEntry(index.toFloat(), floats)
        }

        val chartWeekly = view.findViewById<BarChart>(R.id.chartWeekly)
        val labelWeekly = if (isValueMode) "Revenue" else "Volume"
        val formatter = if (isValueMode) currencyFormatter else null

        val dsWeekly = BarDataSet(weeklyEntries, labelWeekly).apply {
            colors = brandColors
            stackLabels = fixedBrands.toTypedArray()
            valueTextSize = 10f
            valueFormatter = formatter
            valueTextColor = Color.WHITE
        }

        chartWeekly.apply {
            data = BarData(dsWeekly)
            description.isEnabled = false

            legend.isEnabled = true
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.isWordWrapEnabled = true

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

        val barData = BarData(dsBrand)
        barData.barWidth = 0.5f // Slimmer bars

        chartBrand.apply {
            data = barData
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

    private fun updateSegmentTable(list: List<SaleEntry>) {
        tableSegments.removeAllViews()

        val selectedBrand = spinnerBrand.selectedItem.toString()
        val filteredList = if (selectedBrand == "All Brands") list else list.filter { it.brand == selectedBrand }

        // Header Row
        val headerRow = TableRow(context)
        val headers = listOf("Segment", "Qty", "Value")
        headers.forEach { text ->
            val tv = TextView(context).apply {
                this.text = text
                setPadding(16, 16, 16, 16)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setBackgroundColor(Color.LTGRAY)
            }
            headerRow.addView(tv)
        }
        tableSegments.addView(headerRow)

        // Calculate Segments
        val segments = filteredList.groupBy { getSegment(it.totalValue / it.quantity) } // Unit Price
        val sortedSegments = segments.keys.sortedBy { getSegmentSortOrder(it) }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        sortedSegments.forEach { segmentName ->
            val salesInSegment = segments[segmentName] ?: emptyList()
            val totalQty = salesInSegment.sumOf { it.quantity }
            val totalVal = salesInSegment.sumOf { it.totalValue }

            val row = TableRow(context)

            val tvName = TextView(context).apply {
                text = segmentName
                setPadding(16, 16, 16, 16)
            }
            val tvQty = TextView(context).apply {
                text = totalQty.toString()
                setPadding(16, 16, 16, 16)
            }
            val tvVal = TextView(context).apply {
                text = currencyFormat.format(totalVal)
                setPadding(16, 16, 16, 16)
            }

            row.addView(tvName)
            row.addView(tvQty)
            row.addView(tvVal)
            tableSegments.addView(row)
        }
    }

    private fun getSegment(price: Double): String {
        return when {
            price < 10000 -> "<10K"
            price < 15000 -> "10K - 15K"
            price < 20000 -> "15K - 20K"
            price < 30000 -> "20K - 30K"
            price < 50000 -> "30K - 50K"
            price < 70000 -> "50K - 70K"
            price < 100000 -> "70K - 100K"
            else -> "100K+"
        }
    }

    private fun getSegmentSortOrder(segment: String): Int {
        return when (segment) {
            "<10K" -> 1
            "10K - 15K" -> 2
            "15K - 20K" -> 3
            "20K - 30K" -> 4
            "30K - 50K" -> 5
            "50K - 70K" -> 6
            "70K - 100K" -> 7
            "100K+" -> 8
            else -> 9
        }
    }
}
