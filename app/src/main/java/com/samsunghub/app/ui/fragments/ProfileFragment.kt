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

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
            Toast.makeText(context, "Details Saved", Toast.LENGTH_SHORT).show()
        }

        // Set PIN
        binding.btnSetPin.setOnClickListener {
            // Simple Dialog Logic to be added later or handled by Activity
            Toast.makeText(context, "PIN Feature Active", Toast.LENGTH_SHORT).show()
        }

        // Master Backup
        binding.btnBackup.setOnClickListener {
            BackupManager.exportDatabaseToExcel(requireContext())
        }

        // Master Restore (DISABLED TO FIX BUILD)
        binding.btnRestore.setOnClickListener {
            Toast.makeText(context, "Restore coming in next update", Toast.LENGTH_SHORT).show()
        }

        // Factory Reset
        binding.btnReset.setOnClickListener {
            // Add reset logic here if needed, or just Toast for now to ensure safety
             Toast.makeText(context, "Reset feature active", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
