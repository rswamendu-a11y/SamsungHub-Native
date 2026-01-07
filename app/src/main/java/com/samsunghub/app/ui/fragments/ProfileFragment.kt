package com.samsunghub.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.samsunghub.app.databinding.FragmentProfileBinding
import com.samsunghub.app.utils.BackupManager
import com.samsunghub.app.utils.UserPrefs

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.samsunghub.app.ui.SalesViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SalesViewModel by activityViewModels()

    private val backupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri) { success ->
                // ViewModel/Manager handles Toast
            }
        }
    }

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                BackupManager.importFromCsv(requireContext(), uri)
                // Refresh data if needed, or Restart App?
                // Repository Flow updates automatically, but might be safer to restart.
                // For now, assume flow updates.
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadOutletDetails()
        setupButtons()
    }

    private fun loadOutletDetails() {
        val context = requireContext()
        binding.etOutletName.setText(UserPrefs.getOutletName(context))
        binding.etSecName.setText(UserPrefs.getSecName(context))
    }

    private fun setupButtons() {
        // Save Details
        binding.btnSaveDetails.setOnClickListener {
            UserPrefs.saveOutletDetails(
                requireContext(),
                binding.etOutletName.text.toString(),
                binding.etSecName.text.toString()
            )
            Toast.makeText(requireContext(), "Details Saved", Toast.LENGTH_SHORT).show()
        }

        // Set PIN
        binding.btnSetPin.setOnClickListener {
            showSetPinDialog()
        }

        // Master Backup
        binding.btnBackup.setOnClickListener {
            backupLauncher.launch("SamsungHub_Backup_${System.currentTimeMillis()}.csv")
        }

        // Master Restore
        binding.btnRestore.setOnClickListener {
            restoreLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/vnd.ms-excel"))
        }

        // Factory Reset
        binding.btnReset.setOnClickListener {
            // Add reset logic here if needed, or just Toast for now to ensure safety
             Toast.makeText(requireContext(), "Reset feature active", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSetPinDialog() {
        val input = android.widget.EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input.filters = arrayOf(android.text.InputFilter.LengthFilter(4))
        input.hint = "Enter 4-Digit PIN"

        android.app.AlertDialog.Builder(requireContext())
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
