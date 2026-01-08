package com.samsunghub.app.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.samsunghub.app.databinding.FragmentReportsBinding
import java.io.File

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ArrayAdapter<String>
    private var filesList: MutableList<File> = mutableListOf()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup List
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        // We assume there is a ListView with id 'listViewReports' in the XML from previous steps.
        // If the XML is custom, we might need to find it by ID or tag.
        // Using a safe try-catch approach to find the list or creating a simple UI if missing.
        // Assuming the ID is 'rvReports' or 'listReports'. Let's try finding views dynamically or use a simple logic.
        // SAFE FIX: We will look for a ListView or RecyclerView.
        // Actually, to be 100% safe without seeing XML, let's assume the binding has a 'listView' or 'recyclerView'.
        // Based on Phase 5, it likely has a RecyclerView. Let's stick to standard logic.

        // RE-READING: The user said "saved in reports section".
        // I will implement a standard File Scanner here.
        loadReports()

        binding.swipeRefresh?.setOnRefreshListener { loadReports() }
    }

    private fun loadReports() {
        binding.swipeRefresh?.isRefreshing = false
        filesList.clear()

        // CORRECT PATH: Matches PdfReportGenerator
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        if (dir != null && dir.exists()) {
            val files = dir.listFiles { _, name -> name.endsWith(".pdf") }
            files?.sortByDescending { it.lastModified() }
            files?.forEach { filesList.add(it) }
        }

        // Update Adapter (Assuming simple RecyclerView setup or ListView)
        // Since I cannot see the XML, I will assume a RecyclerView 'rvReports' exists
        // and set a simple Adapter.
        val rv = binding.rvReports
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        rv.adapter = ReportAdapter(filesList, ::openFile, ::deleteFile)

        if (filesList.isEmpty()) {
            Toast.makeText(context, "No Reports Found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Open Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFile(file: File) {
        if (file.exists() && file.delete()) {
            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            loadReports()
        }
    }

    // Inner Adapter Class for safety
    class ReportAdapter(
        private val files: List<File>,
        private val onClick: (File) -> Unit,
        private val onDelete: (File) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<ReportAdapter.Vh>() {

        class Vh(v: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
            val txt: android.widget.TextView = v.findViewById(android.R.id.text1)
            val del: android.widget.ImageView? = v.findViewById(android.R.id.icon) // Assuming simple_list_item_icon logic or similar
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): Vh {
            // Using standard layout to guarantee no crash
            val v = LayoutInflater.from(p.context).inflate(android.R.layout.activity_list_item, p, false)
            return Vh(v)
        }

        override fun onBindViewHolder(h: Vh, i: Int) {
            h.txt.text = files[i].name
            h.itemView.setOnClickListener { onClick(files[i]) }
            // Long press to delete if no icon
            h.itemView.setOnLongClickListener { onDelete(files[i]); true }
        }
        override fun getItemCount() = files.size
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
