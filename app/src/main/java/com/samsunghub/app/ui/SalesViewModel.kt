package com.samsunghub.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.samsunghub.app.data.AppDatabase
import com.samsunghub.app.data.SaleEntry
import com.samsunghub.app.data.SalesRepository
import com.samsunghub.app.utils.PdfReportGenerator
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class WeeklyStat(val label: String, val revenue: Double)

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SalesRepository
    private val _selectedDate = MutableLiveData<Calendar>(Calendar.getInstance())
    val selectedDate: LiveData<Calendar> = _selectedDate

    val salesList: LiveData<List<SaleEntry>>
    val mtdTotal: LiveData<Double>
    val lmtdTotal: LiveData<Double>

    val weeklyStats: LiveData<List<WeeklyStat>>
    val brandStats: LiveData<Map<String, Double>>

    init {
        val dao = AppDatabase.getDatabase(application).salesDao()
        repository = SalesRepository(dao)

        // Main Sales List Source
        salesList = _selectedDate.switchMap { cal ->
            val (start, end) = repository.getMonthRange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            repository.getSalesForRange(start, end).asLiveData()
        }

        // MTD Total
        mtdTotal = salesList.map { list ->
            list.sumOf { it.totalValue }
        }

        // LMTD Total (Comparison)
        lmtdTotal = _selectedDate.switchMap { cal ->
            val (start, end) = repository.getLmtdRange(cal)
            repository.getSalesForRange(start, end).map { list ->
                list.sumOf { it.totalValue }
            }.asLiveData()
        }

        // Weekly Stats Transformation
        weeklyStats = salesList.map { list ->
            val stats = mutableListOf(
                WeeklyStat("Wk 1", 0.0),
                WeeklyStat("Wk 2", 0.0),
                WeeklyStat("Wk 3", 0.0),
                WeeklyStat("Wk 4", 0.0)
            )

            // Map simply for accumulation
            var w1 = 0.0
            var w2 = 0.0
            var w3 = 0.0
            var w4 = 0.0

            val cal = Calendar.getInstance()
            for (sale in list) {
                cal.timeInMillis = sale.timestamp
                val day = cal.get(Calendar.DAY_OF_MONTH)
                when {
                    day <= 7 -> w1 += sale.totalValue
                    day <= 14 -> w2 += sale.totalValue
                    day <= 21 -> w3 += sale.totalValue
                    else -> w4 += sale.totalValue
                }
            }

            listOf(
                WeeklyStat("Wk 1", w1),
                WeeklyStat("Wk 2", w2),
                WeeklyStat("Wk 3", w3),
                WeeklyStat("Wk 4", w4)
            )
        }

        // Brand Stats Transformation
        brandStats = salesList.map { list ->
            list.groupBy { it.brand }
                .mapValues { entry -> entry.value.sumOf { it.totalValue } }
        }
    }

    fun setMonth(year: Int, month: Int) {
        val newCal = Calendar.getInstance()
        newCal.set(year, month, 1)
        _selectedDate.value = newCal
    }

    fun generatePdf(context: Context, callback: (Uri?) -> Unit) {
        val list = salesList.value ?: emptyList()
        val cal = _selectedDate.value ?: Calendar.getInstance()
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

        viewModelScope.launch {
            val uri = PdfReportGenerator.generateMonthlyReport(context, list, monthName)
            callback(uri)
        }
    }
}
