package com.samsunghub.app.data

import com.samsunghub.app.data.SalesDao
import com.samsunghub.app.data.SaleEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class SalesRepository(private val salesDao: SalesDao) {

    fun getSalesForRange(start: Long, end: Long): Flow<List<SaleEntry>> {
        return salesDao.getSalesBetweenDates(start, end)
    }

    suspend fun insertSale(sale: SaleEntry) {
        salesDao.insertSale(sale)
    }

    suspend fun updateSale(sale: SaleEntry) {
        salesDao.updateSale(sale)
    }

    suspend fun deleteSale(sale: SaleEntry) {
        salesDao.deleteSale(sale)
    }

    suspend fun deleteAll() {
        salesDao.deleteAllSales()
    }

    suspend fun getAllSalesSync(): List<SaleEntry> {
        return salesDao.getAllSales().first()
    }

    // Helper to calculate start/end timestamps for a given month
    fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    // Smart LMTD Logic
    fun getLmtdRange(selectedDate: Calendar): Pair<Long, Long> {
        val currentCal = Calendar.getInstance()
        val isCurrentMonth = (selectedDate.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                              selectedDate.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH))

        val lmtdEndCal = selectedDate.clone() as Calendar
        lmtdEndCal.add(Calendar.MONTH, -1)

        val lmtdStartCal = lmtdEndCal.clone() as Calendar
        lmtdStartCal.set(Calendar.DAY_OF_MONTH, 1)
        lmtdStartCal.set(Calendar.HOUR_OF_DAY, 0)
        lmtdStartCal.set(Calendar.MINUTE, 0)
        lmtdStartCal.set(Calendar.SECOND, 0)
        lmtdStartCal.set(Calendar.MILLISECOND, 0)

        if (isCurrentMonth) {
            val currentDay = currentCal.get(Calendar.DAY_OF_MONTH)
            val maxDaysInPrevMonth = lmtdEndCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val targetDay = if (currentDay > maxDaysInPrevMonth) maxDaysInPrevMonth else currentDay
            lmtdEndCal.set(Calendar.DAY_OF_MONTH, targetDay)
        } else {
            lmtdEndCal.set(Calendar.DAY_OF_MONTH, lmtdEndCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        lmtdEndCal.set(Calendar.HOUR_OF_DAY, 23)
        lmtdEndCal.set(Calendar.MINUTE, 59)
        lmtdEndCal.set(Calendar.SECOND, 59)
        lmtdEndCal.set(Calendar.MILLISECOND, 999)

        return Pair(lmtdStartCal.timeInMillis, lmtdEndCal.timeInMillis)
    }
}
