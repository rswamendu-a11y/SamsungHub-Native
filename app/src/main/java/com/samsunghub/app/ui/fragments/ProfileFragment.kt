package com.samsunghub.app.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.samsunghub.app.databinding.FragmentProfileBinding
import com.samsunghub.app.ui.SalesViewModel
import com.samsunghub.app.utils.BackupManager
import com.samsunghub.app.utils.UserPrefs
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SalesViewModel by activityViewModels()

    // Backup Launcher (Save As)
    private val backupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val list = viewModel.salesList.value ?: emptyList()
            if (list.isNotEmpty()) {
                lifecycleScope.launch {
                    BackupManager.writeListToCsv(requireContext(), uri, list)
                }
            } else {
                Toast.makeText(requireContext(), "No Data to Backup", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Restore Launcher (Open File)
    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                BackupManager.importFromCsv(requireContext(), uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadOutletDetails()

        // Button Listeners
        binding.btnSaveDetails.setOnClickListener {
            UserPrefs.saveOutletDetails(requireContext(), binding.etOutletName.text.toString(), binding.etSecName.text.toString())
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
        }

        binding.btnSetPin.setOnClickListener {
            showPinDialog()
        }

        binding.btnBackup.setOnClickListener {
            val fileName = "Backup_${System.currentTimeMillis()}.csv"
            backupLauncher.launch(fileName)
        }

        binding.btnRestore.setOnClickListener {
            // Allow CSV and Excel-like MIME types to be safe
            restoreLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/vnd.ms-excel", "text/plain"))
        }

        binding.btnReset.setOnClickListener {
            Toast.makeText(requireContext(), "Factory Reset Disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOutletDetails() {
        val context = requireContext()
        binding.etOutletName.setText(UserPrefs.getOutletName(context))
        binding.etSecName.setText(UserPrefs.getSecName(context))
    }

    private fun showPinDialog() {
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        AlertDialog.Builder(requireContext())
            .setTitle("Set Login PIN")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val pin = input.text.toString()
                if (pin.length == 4) {
                    UserPrefs.savePin(requireContext(), pin)
                    Toast.makeText(requireContext(), "PIN Set Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
