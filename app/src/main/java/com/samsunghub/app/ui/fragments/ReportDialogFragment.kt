package com.samsunghub.app.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.samsunghub.app.R
import com.samsunghub.app.ui.SalesViewModel
import java.util.Calendar

class ReportDialogFragment : DialogFragment() {

    private val viewModel: SalesViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_report, null)

        setupUI(view)

        builder.setView(view)
        return builder.create()
    }

    private fun setupUI(view: View) {
        val monthPicker = view.findViewById<NumberPicker>(R.id.pickerMonth)
        val yearPicker = view.findViewById<NumberPicker>(R.id.pickerYear)
        val btnPdf = view.findViewById<MaterialButton>(R.id.btnGeneratePdf)

        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months

        val currentCal = viewModel.selectedDate.value ?: Calendar.getInstance()
        monthPicker.value = currentCal.get(Calendar.MONTH)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = 2020
        yearPicker.maxValue = currentYear + 5
        yearPicker.value = currentCal.get(Calendar.YEAR)

        btnPdf.setOnClickListener {
            val selectedYear = yearPicker.value
            val selectedMonth = monthPicker.value

            viewModel.generatePdfForMonth(requireContext(), selectedYear, selectedMonth) { uri ->
                if (uri != null) {
                    Toast.makeText(context, "PDF Saved", Toast.LENGTH_SHORT).show()
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(Intent.createChooser(intent, "Open Report"))
                } else {
                    Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
                }
                dismiss()
            }
        }
    }
}
