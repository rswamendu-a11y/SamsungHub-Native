package com.samsunghub.app.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.samsunghub.app.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SalesTrackerActivity : AppCompatActivity() {

    private val viewModel: SalesViewModel by viewModels()
    private lateinit var adapter: SalesAdapter
    private val currencyFormatter = IndianCurrencyFormatter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales_tracker)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        // RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSales)
        adapter = SalesAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Date Picker Button
        findViewById<MaterialButton>(R.id.btnMonthPicker).setOnClickListener {
            showMonthYearPicker()
        }

        // PDF Export
        findViewById<ImageButton>(R.id.btnExportPdf).setOnClickListener {
            exportPdf()
        }

        // FAB (Placeholder for future Add Entry logic)
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            Toast.makeText(this, "Add Sale Dialog to be implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        // 1. Date Title
        viewModel.selectedDate.observe(this) { cal ->
            val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            findViewById<MaterialButton>(R.id.btnMonthPicker).text = format.format(cal.time)
        }

        // 2. Sales List
        viewModel.salesList.observe(this) { list ->
            adapter.submitList(list)
        }

        // 3. Totals
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        viewModel.mtdTotal.observe(this) { total ->
            findViewById<TextView>(R.id.tvMtdTotal).text = currencyFormat.format(total)
        }
        viewModel.lmtdTotal.observe(this) { total ->
            findViewById<TextView>(R.id.tvLmtdTotal).text = currencyFormat.format(total)
        }

        // 4. Charts
        viewModel.weeklyStats.observe(this) { stats ->
            setupWeeklyChart(stats)
        }
        viewModel.brandStats.observe(this) { stats ->
            setupBrandChart(stats)
        }
    }

    private fun setupWeeklyChart(stats: List<WeeklyStat>) {
        val chart = findViewById<BarChart>(R.id.chartWeekly)

        val entries = stats.mapIndexed { index, stat ->
            BarEntry(index.toFloat(), stat.revenue.toFloat())
        }

        val dataSet = BarDataSet(entries, "Revenue").apply {
            color = Color.parseColor("#1A237E") // Dark Blue
            valueTextSize = 12f
            valueFormatter = currencyFormatter
        }

        val labels = stats.map { it.label }

        chart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
            }

            axisLeft.apply {
                valueFormatter = currencyFormatter
                axisMinimum = 0f
            }
            axisRight.isEnabled = false

            animateY(1000)
            invalidate()
        }
    }

    private fun setupBrandChart(stats: Map<String, Double>) {
        val chart = findViewById<HorizontalBarChart>(R.id.chartBrand)

        // Sort by revenue descending
        val sortedStats = stats.toList().sortedByDescending { it.second }

        val entries = sortedStats.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())
        }

        val dataSet = BarDataSet(entries, "Brand Revenue").apply {
            color = Color.parseColor("#00897B") // Teal
            valueTextSize = 12f
            valueFormatter = currencyFormatter
        }

        val labels = sortedStats.map { it.first }

        chart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
                labelCount = labels.size
            }

            axisLeft.apply {
                valueFormatter = currencyFormatter
                axisMinimum = 0f
            }
            axisRight.isEnabled = false

            animateY(1000)
            invalidate()
        }
    }

    private fun showMonthYearPicker() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_month_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.pickerMonth)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.pickerYear)

        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months

        val currentCal = viewModel.selectedDate.value ?: Calendar.getInstance()
        monthPicker.value = currentCal.get(Calendar.MONTH)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = 2020
        yearPicker.maxValue = currentYear + 5
        yearPicker.value = currentCal.get(Calendar.YEAR)

        AlertDialog.Builder(this)
            .setTitle("Select Month")
            .setView(dialogView)
            .setPositiveButton("Set") { _, _ ->
                viewModel.setMonth(yearPicker.value, monthPicker.value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportPdf() {
        viewModel.generatePdf(this) { uri ->
            if (uri != null) {
                Toast.makeText(this, "PDF Saved: Documents/SalesReports", Toast.LENGTH_LONG).show()
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(Intent.createChooser(intent, "Open Report"))
            } else {
                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
