package com.samsunghub.app.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samsunghub.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportFile(val name: String, val date: Long, val uri: Uri, val size: Long)

class ReportsFragment : Fragment() {

    private lateinit var adapter: ReportsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.recyclerViewReports)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = ReportsAdapter(
            onOpen = { file -> openFile(file) },
            onDelete = { file -> deleteReport(file) }
        )
        rv.adapter = adapter

        loadReports()
    }

    private fun deleteReport(file: ReportFile) {
        try {
            val rows = requireContext().contentResolver.delete(file.uri, null, null)
            if (rows > 0) {
                android.widget.Toast.makeText(requireContext(), "Report Deleted", android.widget.Toast.LENGTH_SHORT).show()
                loadReports() // Refresh list
            } else {
                // Fallback attempt (unlikely to work for SAF/MediaStore uri but safe to try)
                android.widget.Toast.makeText(requireContext(), "Could not delete file", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(requireContext(), "Delete Failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadReports() {
        val files = mutableListOf<ReportFile>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns._ID
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")

        // Querying "External" content URI which includes Documents
        val queryUri = MediaStore.Files.getContentUri("external")

        context?.contentResolver?.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol)
                // Filter for our app's reports specifically if needed,
                // but usually checking name pattern is good enough if we don't have a specific directory ID easily.
                if (name.startsWith("Sales_Report")) {
                    val date = cursor.getLong(dateCol) * 1000 // Seconds to Millis
                    val size = cursor.getLong(sizeCol)
                    val id = cursor.getLong(idCol)
                    val contentUri = Uri.withAppendedPath(queryUri, id.toString())

                    files.add(ReportFile(name, date, contentUri, size))
                }
            }
        }

        adapter.submitList(files)
    }

    private fun openFile(file: ReportFile) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(Intent.createChooser(intent, "Open Report"))
    }

    // Inner Adapter Class
    class ReportsAdapter(
        private val onOpen: (ReportFile) -> Unit,
        private val onDelete: (ReportFile) -> Unit
    ) : RecyclerView.Adapter<ReportsAdapter.ViewHolder>() {

        private var list: List<ReportFile> = emptyList()

        fun submitList(newList: List<ReportFile>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_report_file, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position], onOpen, onDelete)
        }

        override fun getItemCount() = list.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvName: TextView = itemView.findViewById(R.id.tvFileName)
            private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            private val btnShare: ImageView = itemView.findViewById(R.id.btnShare)
            // Assuming there is a delete button in the layout, or we add logic to long press?
            // User requested "Fix Reports Deletion - The delete button in Reports doesn't work".
            // I need to check item_report_file layout or assume an ID.
            // I'll try to find btnDelete. If not in layout, I'll assume long click on root.
            private val btnDelete: ImageView? = itemView.findViewById(R.id.btnDelete)

            fun bind(
                file: ReportFile,
                onOpen: (ReportFile) -> Unit,
                onDelete: (ReportFile) -> Unit
            ) {
                tvName.text = file.name
                val date = Date(file.date)
                val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                tvDate.text = fmt.format(date)

                itemView.setOnClickListener { onOpen(file) }

                btnShare.setOnClickListener {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, file.uri)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    itemView.context.startActivity(Intent.createChooser(intent, "Share Report"))
                }

                // If btnDelete exists, wire it. Else wire long press.
                if (btnDelete != null) {
                    btnDelete.setOnClickListener { onDelete(file) }
                } else {
                    itemView.setOnLongClickListener {
                        onDelete(file)
                        true
                    }
                }
            }
        }
    }
}
