package com.samsunghub.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samsunghub.app.R
import com.samsunghub.app.data.SaleEntry
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalesAdapter(private val onClick: (SaleEntry) -> Unit) : ListAdapter<SaleEntry, SalesAdapter.SaleViewHolder>(SaleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sale_entry, parent, false)
        return SaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class SaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvModel: TextView = itemView.findViewById(R.id.tvModel)
        private val tvSegment: TextView = itemView.findViewById(R.id.tvSegment)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)

        private val dateFormat = SimpleDateFormat("dd-MMM", Locale.getDefault())
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        fun bind(sale: SaleEntry) {
            tvDate.text = dateFormat.format(Date(sale.timestamp))
            tvModel.text = "${sale.brand} ${sale.model} (${sale.variant})"
            tvSegment.text = sale.segment
            tvPrice.text = currencyFormat.format(sale.totalValue)

            // Special Styling for Premier
            if (sale.segment.contains("Premier") || sale.segment.contains(">100k")) {
                tvSegment.setTextColor(Color.parseColor("#1A237E")) // Dark Blue
            } else {
                // Reset to secondary text color (approximate, since we can't easily get attr here without context theme lookup)
                // For simplicity, using Dark Gray
                tvSegment.setTextColor(Color.DKGRAY)
            }
        }
    }

    class SaleDiffCallback : DiffUtil.ItemCallback<SaleEntry>() {
        override fun areItemsTheSame(oldItem: SaleEntry, newItem: SaleEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SaleEntry, newItem: SaleEntry): Boolean {
            return oldItem == newItem
        }
    }
}
