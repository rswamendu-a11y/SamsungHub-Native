package com.samsunghub.app.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.samsunghub.app.R
import com.samsunghub.app.data.SaleEntry
import com.samsunghub.app.ui.SalesAdapter
import com.samsunghub.app.ui.SalesViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TrackerFragment : Fragment() {

    private val viewModel: SalesViewModel by activityViewModels()
    private lateinit var adapter: SalesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tracker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI(view)
        observeData(view)
    }

    private fun setupUI(view: View) {
        // 1. RecyclerView
        val rv = view.findViewById<RecyclerView>(R.id.recyclerViewDaily)
        adapter = SalesAdapter()
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter

        // 2. Spinner
        val spinner = view.findViewById<Spinner>(R.id.spinnerBrand)
        val brands = arrayOf("Samsung", "Apple", "Oppo", "Vivo", "Realme", "Xiaomi", "Moto", "Other")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, brands)
        spinner.adapter = spinnerAdapter

        // 3. Date Pick
        view.findViewById<View>(R.id.btnDatePick).setOnClickListener {
            showDatePicker()
        }

        // 4. Add Button
        view.findViewById<MaterialButton>(R.id.btnAdd).setOnClickListener {
            addSale(view)
        }

        // 5. Report Dialog
        view.findViewById<ImageButton>(R.id.btnReport).setOnClickListener {
            ReportDialogFragment().show(parentFragmentManager, "ReportDialog")
        }
    }

    private fun observeData(view: View) {
        val tvHeaderDate = view.findViewById<TextView>(R.id.tvHeaderDate)
        val tvListHeader = view.findViewById<TextView>(R.id.tvListHeader)

        viewModel.selectedDate.observe(viewLifecycleOwner) { cal ->
            val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            tvHeaderDate.text = fmt.format(cal.time)
        }

        viewModel.salesList.observe(viewLifecycleOwner) { list ->
            // Filter list to show only selected date's entries?
            // Note: The ViewModel currently pulls the *whole month*.
            // We need to filter locally for the "Daily Log" view or update ViewModel to support daily query.
            // For now, let's filter locally in the Observer for the RecyclerView.
            val selectedCal = viewModel.selectedDate.value ?: Calendar.getInstance()

            val dailyList = list.filter {
                val c = Calendar.getInstance()
                c.timeInMillis = it.timestamp
                c.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR) &&
                c.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)
            }

            adapter.submitList(dailyList)
            tvListHeader.text = "SALES LOG (${dailyList.size})"
        }
    }

    private fun addSale(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinnerBrand)
        val etModel = view.findViewById<EditText>(R.id.etModel)
        val etVariant = view.findViewById<EditText>(R.id.etVariant)
        val etQty = view.findViewById<EditText>(R.id.etQty)
        val etPrice = view.findViewById<EditText>(R.id.etPrice)

        val brand = spinner.selectedItem.toString()
        val model = etModel.text.toString()
        val variant = etVariant.text.toString()
        val qtyStr = etQty.text.toString()
        val priceStr = etPrice.text.toString()

        if (model.isBlank() || priceStr.isBlank()) {
            Toast.makeText(context, "Model and Price are required", Toast.LENGTH_SHORT).show()
            return
        }

        val qty = qtyStr.toIntOrNull() ?: 1
        val price = priceStr.toDoubleOrNull() ?: 0.0
        val timestamp = viewModel.selectedDate.value?.timeInMillis ?: System.currentTimeMillis()

        val sale = SaleEntry.create(timestamp, brand, model, variant, price, qty)

        // We need to access repository to insert.
        // Best practice: Expose an insert method in ViewModel.
        // Assuming I'll add `insertSale` to ViewModel in next step.
        (viewModel as? SalesViewModel)?.insertSale(sale)

        // Clear Fields
        etModel.text.clear()
        etVariant.text.clear()
        etPrice.text.clear()
        etQty.setText("1")

        Toast.makeText(context, "Added to Queue", Toast.LENGTH_SHORT).show()
    }

    private fun showDatePicker() {
        val cal = viewModel.selectedDate.value ?: Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, day)
                viewModel.setDate(newCal)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
}
