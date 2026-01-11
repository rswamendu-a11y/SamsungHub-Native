package com.samsunghub.app.ui.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.samsunghub.app.R
import com.samsunghub.app.data.SaleEntry
import com.samsunghub.app.ui.SalesViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditSaleDialog(private val sale: SaleEntry) : DialogFragment() {

    private val viewModel: SalesViewModel by activityViewModels()
    private var selectedDate = Calendar.getInstance().apply { timeInMillis = sale.timestamp }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_sale, null)
        setupUI(view)
        builder.setView(view)
        return builder.create()
    }

    private fun setupUI(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinnerEditBrand)
        val etModel = view.findViewById<EditText>(R.id.etEditModel)
        val etVariant = view.findViewById<EditText>(R.id.etEditVariant)
        val etQty = view.findViewById<EditText>(R.id.etEditQty)
        val etPrice = view.findViewById<EditText>(R.id.etEditPrice)
        val tvDate = view.findViewById<TextView>(R.id.tvEditDate)

        // Setup Brand Spinner
        val brands = arrayOf("Samsung", "Apple", "Oppo", "Vivo", "Realme", "Xiaomi", "Moto", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, brands)
        spinner.adapter = adapter
        val brandIdx = brands.indexOf(sale.brand)
        if(brandIdx >= 0) spinner.setSelection(brandIdx)

        etModel.setText(sale.model)
        etVariant.setText(sale.variant)
        etQty.setText(sale.quantity.toString())
        etPrice.setText(sale.unitPrice.toString())

        updateDateText(tvDate)

        tvDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate.set(y, m, d)
                updateDateText(tvDate)
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        view.findViewById<View>(R.id.btnSave).setOnClickListener {
            val newBrand = spinner.selectedItem.toString()
            val newModel = etModel.text.toString()
            val newPrice = etPrice.text.toString().toDoubleOrNull() ?: 0.0
            val newQty = etQty.text.toString().toIntOrNull() ?: 1

            // Re-create using factory to recalc segment/total
            // Preserve ID!
            val updated = SaleEntry.create(selectedDate.timeInMillis, newBrand, newModel, etVariant.text.toString(), newPrice, newQty).copy(id = sale.id)

            viewModel.updateSale(updated)
            Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        view.findViewById<View>(R.id.btnDelete).setOnClickListener {
            viewModel.deleteSale(sale)
            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun updateDateText(tv: TextView) {
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        tv.text = "Date: " + fmt.format(selectedDate.time)
    }
}
