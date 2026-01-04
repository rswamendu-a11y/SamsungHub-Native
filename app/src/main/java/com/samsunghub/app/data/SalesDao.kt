package com.samsunghub.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {
    @Insert
    suspend fun insertSale(sale: SaleEntry)

    @Query("SELECT * FROM sales_table WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getSalesBetweenDates(start: Long, end: Long): Flow<List<SaleEntry>>

    @Query("SELECT * FROM sales_table ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntry>>
}
