package com.samsunghub.app.ui

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

class IndianCurrencyFormatter : ValueFormatter() {

    private val formatK = DecimalFormat("###,###.#k")
    private val formatL = DecimalFormat("###,###.#L")
    private val formatCr = DecimalFormat("###,###.#Cr")
    private val formatRaw = DecimalFormat("###,###")

    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return formatValue(value.toDouble())
    }

    override fun getFormattedValue(value: Float): String {
        return formatValue(value.toDouble())
    }

    private fun formatValue(value: Double): String {
        return when {
            value >= 10_000_000 -> formatCr.format(value / 10_000_000)
            value >= 100_000 -> formatL.format(value / 100_000)
            value >= 1_000 -> formatK.format(value / 1_000)
            else -> formatRaw.format(value)
        }
    }
}
