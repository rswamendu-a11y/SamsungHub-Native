package com.samsunghub.app.ui.fragments

import android.os.Bundle; import android.view.*; import android.widget.*; import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment; import androidx.fragment.app.activityViewModels; androidx.lifecycle.lifecycleScope
import com.samsunghub.app.databinding.FragmentProfileBinding; import com.samsunghub.app.ui.SalesViewModel
import com.samsunghub.app.utils.BackupManager; import com.samsunghub.app.utils.UserPrefs
import kotlinx.coroutines.launch; import android.app.AlertDialog; import android.widget.EditText; import android.text.InputType

class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null; private val b get() = _b!!
    private val vm: SalesViewModel by activityViewModels()

    private val back = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u ->
        if(u!=null) {
            val l = vm.salesList.value?:emptyList()
            if(l.isNotEmpty()) lifecycleScope.launch { BackupManager.writeListToCsv(requireContext(), u, l) }
            else Toast.makeText(requireContext(), "No Data to Backup", Toast.LENGTH_SHORT).show()
        }
    }
    private val rest = registerForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if(u!=null) lifecycleScope.launch { BackupManager.importFromCsv(requireContext(), u) }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View { _b = FragmentProfileBinding.inflate(i, c, false); return b.root }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s); ld()
        b.btnSaveDetails.setOnClickListener { UserPrefs.saveOutletDetails(requireContext(), b.etOutletName.text.toString(), b.etSecName.text.toString()); T("Saved") }
        b.btnSetPin.setOnClickListener { pin() }
        b.btnBackup.setOnClickListener { back.launch("Backup_${System.currentTimeMillis()}.csv") }
        b.btnRestore.setOnClickListener { rest.launch(arrayOf("text/csv", "text/comma-separated-values", "application/vnd.ms-excel")) }
        b.btnReset.setOnClickListener { T("Factory Reset Disabled") }
    }

    private fun ld() { val c=requireContext(); b.etOutletName.setText(UserPrefs.getOutletName(c)); b.etSecName.setText(UserPrefs.getSecName(c)) }
    private fun T(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    private fun pin() {
        val et = EditText(requireContext()); et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        AlertDialog.Builder(requireContext()).setTitle("Set Login PIN").setView(et).setPositiveButton("Save"){_,_->
            if(et.text.length==4) { UserPrefs.savePin(requireContext(), et.text.toString()); T("PIN Set Successfully") }
            else T("PIN must be 4 digits")
        }.setNegativeButton("Cancel", null).show()
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
