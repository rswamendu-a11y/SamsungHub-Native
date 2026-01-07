package com.samsunghub.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
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
import com.samsunghub.app.utils.BackupManager
import com.samsunghub.app.utils.PdfReportGenerator
import com.samsunghub.app.utils.ReportType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SalesRepository
    private val _selectedDate = MutableLiveData<Calendar>(Calendar.getInstance())
    val selectedDate: LiveData<Calendar> = _selectedDate

    // Chart Mode: true = Value, false = Volume
    private val _chartMode = MutableLiveData(true)
    val chartMode: LiveData<Boolean> = _chartMode

    val salesList: LiveData<List<SaleEntry>>
    val mtdTotal: LiveData<Double>
    val lmtdTotal: LiveData<Double>
    val mtdVolume: LiveData<Int>
    val lmtdVolume: LiveData<Int>

    // Combined Stats
    val weeklyStats: LiveData<List<WeeklyStat>>

    // Separate streams for Fragment consumption based on toggles (or raw data)
    // To match Fragment logic: viewModel.weeklyVolume, viewModel.brandVolume
    val weeklyVolume: LiveData<List<WeeklyStat>>

    val brandStats: LiveData<Map<String, Double>> // Revenue
    val brandVolume: LiveData<Map<String, Double>> // Count (as Double for generic chart logic)

    init {
        val dao = AppDatabase.getDatabase(application).salesDao()
        repository = SalesRepository(dao)

        salesList = _selectedDate.switchMap { cal ->
            val (start, end) = repository.getMonthRange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            repository.getSalesForRange(start, end).asLiveData()
        }

        mtdTotal = salesList.map { list -> list.sumOf { it.totalValue } }
        mtdVolume = salesList.map { list -> list.sumOf { it.quantity } }

        lmtdTotal = _selectedDate.switchMap { cal ->
            val (start, end) = repository.getLmtdRange(cal)
            repository.getSalesForRange(start, end).map { list -> list.sumOf { it.totalValue } }.asLiveData()
        }

        lmtdVolume = _selectedDate.switchMap { cal ->
            val (start, end) = repository.getLmtdRange(cal)
            repository.getSalesForRange(start, end).map { list -> list.sumOf { it.quantity } }.asLiveData()
        }

        weeklyStats = salesList.map { list -> calculateWeeklyStats(list) }
        weeklyVolume = weeklyStats // Same list, Fragment picks field

        brandStats = salesList.map { list ->
            list.groupBy { it.brand }.mapValues { it.value.sumOf { s -> s.totalValue } }
        }

        brandVolume = salesList.map { list ->
            list.groupBy { it.brand }.mapValues { it.value.sumOf { s -> s.quantity }.toDouble() }
        }
    }

    private fun calculateWeeklyStats(list: List<SaleEntry>): List<WeeklyStat> {
        var w1r = 0.0; var w1c = 0
        var w2r = 0.0; var w2c = 0
        var w3r = 0.0; var w3c = 0
        var w4r = 0.0; var w4c = 0

        val cal = Calendar.getInstance()
        for (sale in list) {
            cal.timeInMillis = sale.timestamp
            val day = cal.get(Calendar.DAY_OF_MONTH)
            when {
                day <= 7 -> { w1r += sale.totalValue; w1c += sale.quantity }
                day <= 14 -> { w2r += sale.totalValue; w2c += sale.quantity }
                day <= 21 -> { w3r += sale.totalValue; w3c += sale.quantity }
                else -> { w4r += sale.totalValue; w4c += sale.quantity }
            }
        }
        // Always return 4 bars, even if zero, to prevent "No Chart Data" message
        return listOf(
            WeeklyStat("Wk 1", w1r, w1c),
            WeeklyStat("Wk 2", w2r, w2c),
            WeeklyStat("Wk 3", w3r, w3c),
            WeeklyStat("Wk 4", w4r, w4c)
        )
    }

    fun setChartMode(isValue: Boolean) {
        _chartMode.value = isValue
    }

    fun setDate(calendar: Calendar) {
        _selectedDate.value = calendar
    }

    fun setMonth(year: Int, month: Int) {
        val newCal = Calendar.getInstance()
        newCal.set(year, month, 1)
        _selectedDate.value = newCal
    }

    fun insertSale(sale: SaleEntry) {
        viewModelScope.launch { repository.insertSale(sale) }
    }

    fun updateSale(sale: SaleEntry) {
        viewModelScope.launch { repository.updateSale(sale) }
    }

    fun deleteSale(sale: SaleEntry) {
        viewModelScope.launch { repository.deleteSale(sale) }
    }

    fun deleteAllData() {
        viewModelScope.launch { repository.deleteAll() }
    }

    fun generatePdfForMonth(context: Context, year: Int, month: Int, type: ReportType, callback: (Uri?) -> Unit) {
        viewModelScope.launch {
            val (start, end) = repository.getMonthRange(year, month)
            val list = repository.getSalesForRange(start, end).first()
            val cal = Calendar.getInstance().apply { set(year, month, 1) }
            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

            // Fetch outlet info
            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val outletName = prefs.getString("outlet_name", "M/S EXCLUSIVE") ?: "M/S EXCLUSIVE"
            val secName = prefs.getString("sec_name", "") ?: ""

            val uri = PdfReportGenerator.generateMonthlyReport(context, list, monthName, outletName, secName, type)
            callback(uri)
        }
    }

    fun exportBackup(uri: Uri, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val list = repository.getAllSalesSync()
            com.samsunghub.app.utils.BackupManager.writeListToCsv(getApplication(), uri, list)
            callback(true)
        }
    }

    fun restoreData(list: List<SaleEntry>) {
        viewModelScope.launch {
            repository.deleteAll()
            list.forEach { repository.insertSale(it) }
        }
    }
}
