package com.samsunghub.app.data

object SegmentCalculator {
    fun getSegment(price: Double): String {
        return when {
            price < 10000 -> "<10K"
            price < 15000 -> "10K - 15K"
            price < 20000 -> "15K - 20K"
            price < 30000 -> "20K - 30K"
            price < 40000 -> "30K - 40K"
            price < 70000 -> "40K - 70K"
            price < 100000 -> "70K - 100K"
            else -> ">100K"
        }
    }
}
