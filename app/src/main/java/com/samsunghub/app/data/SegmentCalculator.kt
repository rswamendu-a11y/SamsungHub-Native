package com.samsunghub.app.data

object SegmentCalculator {
    fun getSegment(price: Double): String {
        return when {
            price < 10000 -> "Entry (<10k)"
            price < 20000 -> "10k-20k"
            price < 30000 -> "20k-30k"
            price < 40000 -> "30k-40k"
            price < 50000 -> "40k-50k"
            else -> "Premium (>50k)"
        }
    }
}
