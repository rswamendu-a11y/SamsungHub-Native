package com.samsunghub.app.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.samsunghub.app.databinding.DialogReportBinding
import com.samsunghub.app.ui.SalesViewModel
import com.samsunghub.app.utils.PdfReportGenerator
import com.samsunghub.app.utils.ReportType
import com.samsunghub.app.utils.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ReportDialogFragment : DialogFragment() {

    private var _binding: DialogReportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SalesViewModel by activityViewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = DialogReportBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDateSpinners()

        binding.btnMatrix.setOnClickListener { generate(ReportType.MATRIX_ONLY) }
        binding.btnDetailed.setOnClickListener { generate(ReportType.DETAILED_ONLY) }
        binding.btnMaster.setOnClickListener { generate(ReportType.MASTER_REPORT) }
        binding.btnPriceSegmentPdf.setOnClickListener { generate(ReportType.PRICE_SEGMENT_ONLY) }
    }

    private fun generate(type: ReportType) {
        val month = binding.spinnerMonth.selectedItemPosition
        val year = binding.spinnerYear.selectedItem.toString().toInt()
        val outletName = UserPrefs.getOutletName(requireContext())
        val secName = UserPrefs.getSecName(requireContext())

        viewModel.getSalesForMonth(month, year) { sales ->
            if (sales.isNotEmpty()) {
                val pdfGenerator = PdfReportGenerator // Using Object
                val monthName = "${binding.spinnerMonth.selectedItem} $year"

                lifecycleScope.launch(Dispatchers.IO) {
                    val uri = pdfGenerator.generateReport(requireContext(), sales, monthName, outletName, secName, type)

                    withContext(Dispatchers.Main) {
                        if (uri != null) {
                            openPdf(uri)
                            Toast.makeText(requireContext(), "PDF Saved", Toast.LENGTH_SHORT).show()
                            dismiss()
                        } else {
                            Toast.makeText(requireContext(), "Error generating PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "No sales found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Open Report"))
    }

    private fun setupDateSpinners() {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val years = listOf("2024", "2025", "2026", "2027", "2028")

        binding.spinnerMonth.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        binding.spinnerYear.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, years)

        val cal = Calendar.getInstance()
        binding.spinnerMonth.setSelection(cal.get(Calendar.MONTH))
        val currentYearIndex = years.indexOf(cal.get(Calendar.YEAR).toString())
        if (currentYearIndex != -1) binding.spinnerYear.setSelection(currentYearIndex)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
