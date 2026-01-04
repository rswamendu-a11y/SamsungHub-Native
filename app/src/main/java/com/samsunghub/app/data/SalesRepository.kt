package com.samsunghub.app.data

import com.samsunghub.app.data.SalesDao
import com.samsunghub.app.data.SaleEntry
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SalesRepository(private val salesDao: SalesDao) {

    fun getSalesForRange(start: Long, end: Long): Flow<List<SaleEntry>> {
        return salesDao.getSalesBetweenDates(start, end)
    }

    suspend fun insertSale(sale: SaleEntry) {
        salesDao.insertSale(sale)
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

        // Check if selected month is the current real-time month
        val isCurrentMonth = (selectedDate.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                              selectedDate.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH))

        val lmtdEndCal = selectedDate.clone() as Calendar
        lmtdEndCal.add(Calendar.MONTH, -1) // Go back one month

        val lmtdStartCal = lmtdEndCal.clone() as Calendar
        lmtdStartCal.set(Calendar.DAY_OF_MONTH, 1)
        lmtdStartCal.set(Calendar.HOUR_OF_DAY, 0)
        lmtdStartCal.set(Calendar.MINUTE, 0)
        lmtdStartCal.set(Calendar.SECOND, 0)
        lmtdStartCal.set(Calendar.MILLISECOND, 0)

        if (isCurrentMonth) {
            // Scenario A: Month-To-Date vs Last Month-To-SAME-Date
            // If today is Jan 15, we want Dec 1 to Dec 15.
            val currentDay = currentCal.get(Calendar.DAY_OF_MONTH)
            // Limit the end day to the max days in that previous month (handle Jan 31 -> Feb 28)
            val maxDaysInPrevMonth = lmtdEndCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val targetDay = if (currentDay > maxDaysInPrevMonth) maxDaysInPrevMonth else currentDay

            lmtdEndCal.set(Calendar.DAY_OF_MONTH, targetDay)
        } else {
            // Scenario B: Full Month vs Full Previous Month
            // Set to end of that previous month
            lmtdEndCal.set(Calendar.DAY_OF_MONTH, lmtdEndCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        // Set End time to end of day
        lmtdEndCal.set(Calendar.HOUR_OF_DAY, 23)
        lmtdEndCal.set(Calendar.MINUTE, 59)
        lmtdEndCal.set(Calendar.SECOND, 59)
        lmtdEndCal.set(Calendar.MILLISECOND, 999)

        return Pair(lmtdStartCal.timeInMillis, lmtdEndCal.timeInMillis)
    }
}
