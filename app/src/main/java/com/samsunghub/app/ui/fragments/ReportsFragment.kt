package com.samsunghub.app.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.samsunghub.app.databinding.FragmentReportsBinding
import java.io.File

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private var filesList: MutableList<File> = mutableListOf()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvReports.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        loadReports()
    }

    private fun loadReports() {
        filesList.clear()
        // Correct Path
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        if (dir != null && dir.exists()) {
            val files = dir.listFiles { _, name -> name.endsWith(".pdf") }
            files?.sortByDescending { it.lastModified() }
            files?.forEach { filesList.add(it) }
        }

        binding.rvReports.adapter = ReportAdapter(filesList, ::openFile, ::deleteFile)

        // Handle Empty State safely
        try {
            val emptyView = binding.root.findViewById<TextView>(com.samsunghub.app.R.id.tvEmpty)
            if (emptyView != null) {
                emptyView.visibility = if (filesList.isEmpty()) View.VISIBLE else View.GONE
            }
        } catch (e: Exception) {}
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
        try {
            if (file.exists() && file.delete()) {
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                loadReports()
            }
        } catch (e: Exception) {}
    }

    class ReportAdapter(private val files: List<File>, private val onClick: (File) -> Unit, private val onDelete: (File) -> Unit) : androidx.recyclerview.widget.RecyclerView.Adapter<ReportAdapter.Vh>() {
        class Vh(v: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
            val txt: android.widget.TextView = v.findViewById(android.R.id.text1)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int): Vh {
            return Vh(LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false))
        }
        override fun onBindViewHolder(h: Vh, i: Int) {
            h.txt.text = files[i].name
            h.itemView.setOnClickListener { onClick(files[i]) }
            h.itemView.setOnLongClickListener { onDelete(files[i]); true }
        }
        override fun getItemCount() = files.size
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
