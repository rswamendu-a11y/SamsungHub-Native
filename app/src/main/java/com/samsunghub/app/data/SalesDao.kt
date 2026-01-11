package com.samsunghub.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {
    @Insert
    suspend fun insertSale(sale: SaleEntry)

    @Update
    suspend fun updateSale(sale: SaleEntry)

    @Delete
    suspend fun deleteSale(sale: SaleEntry)

    @Query("SELECT * FROM sales_table WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getSalesBetweenDates(start: Long, end: Long): Flow<List<SaleEntry>>

    @Query("SELECT * FROM sales_table ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntry>>

    @Query("DELETE FROM sales_table")
    suspend fun deleteAllSales()
}
