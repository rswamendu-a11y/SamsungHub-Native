package com.samsunghub.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales_table")
data class SaleEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val brand: String,
    val model: String,
    val variant: String,
    val unitPrice: Double,
    val quantity: Int,
    val totalValue: Double,
    val segment: String
) {
    companion object {
        fun create(
            timestamp: Long,
            brand: String,
            model: String,
            variant: String,
            unitPrice: Double,
            quantity: Int
        ): SaleEntry {
            val totalValue = unitPrice * quantity
            val segment = SegmentCalculator.getSegment(unitPrice)
            return SaleEntry(
                timestamp = timestamp,
                brand = brand,
                model = model,
                variant = variant,
                unitPrice = unitPrice,
                quantity = quantity,
                totalValue = totalValue,
                segment = segment
            )
        }
    }
}
