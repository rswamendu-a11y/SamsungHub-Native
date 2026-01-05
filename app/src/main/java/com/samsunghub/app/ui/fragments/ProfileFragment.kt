package com.samsunghub.app.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.samsunghub.app.R
import com.samsunghub.app.ui.SalesViewModel
import com.samsunghub.app.utils.BackupManager

class ProfileFragment : Fragment() {

    private val viewModel: SalesViewModel by activityViewModels()

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { performRestore(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etOutlet = view.findViewById<EditText>(R.id.etOutletName)
        val etSec = view.findViewById<EditText>(R.id.etSecName)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveProfile)

        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        etOutlet.setText(prefs.getString("outlet_name", ""))
        etSec.setText(prefs.getString("sec_name", ""))

        btnSave.setOnClickListener {
            prefs.edit()
                .putString("outlet_name", etOutlet.text.toString())
                .putString("sec_name", etSec.text.toString())
                .apply()
            Toast.makeText(context, "Profile Saved", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnBackup).setOnClickListener {
            // Backup needs list. We can ask VM for all sales.
            // Assuming VM has function or we add one.
            viewModel.exportBackup(requireContext()) { success ->
                val msg = if(success) "Backup Saved to Documents" else "Backup Failed"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<View>(R.id.btnRestore).setOnClickListener {
            restoreLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        }

        view.findViewById<View>(R.id.btnReset).setOnClickListener {
            // Confirm Dialog? For speed: just do it (or standard warning)
            // Ideally a dialog.
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Factory Reset")
                .setMessage("Delete ALL data? This cannot be undone.")
                .setPositiveButton("DELETE") { _, _ ->
                    viewModel.deleteAllData()
                    Toast.makeText(context, "Data Reset", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun performRestore(uri: Uri) {
        val list = BackupManager.importFromExcel(requireContext(), uri)
        if (list != null) {
            viewModel.restoreData(list)
            Toast.makeText(context, "Database Restored", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Import Failed", Toast.LENGTH_SHORT).show()
        }
    }
}
