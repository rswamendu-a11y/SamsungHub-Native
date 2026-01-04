package com.samsunghub.app.data

object SegmentCalculator {
    fun getSegment(price: Double): String {
        return when {
            price < 10000 -> "Entry (<10k)"
            price < 30000 -> "10k-30k"
            price < 40000 -> "30k-40k"
            price < 70000 -> "40k-70k"
            price < 100000 -> "70k-100k"
            else -> "Premier (>100k)"
        }
    }
}
