package com.example.enterprisehub;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportLockerActivity extends AppCompatActivity {

    private static final String LOCKER_DIR = "Sales_Vault";
    private static final int PICK_IMPORT_FILE = 303;

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private FileAdapter adapter;
    private List<File> fileList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_locker);

        recyclerView = findViewById(R.id.recycler_view_reports);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        FloatingActionButton fabImport = findViewById(R.id.fab_import);
        ImageView btnBack = findViewById(R.id.btn_back);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnBack.setOnClickListener(v -> finish());

        fabImport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            // Also allow Excel? User said "Import external PDF". Sticking to PDF for now based on prompt logic "Import external PDF", but let's allow excel too just in case.
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
            startActivityForResult(intent, PICK_IMPORT_FILE);
        });

        loadFiles();
    }

    private void loadFiles() {
        File dir = new File(getExternalFilesDir(null), LOCKER_DIR);
        if (!dir.exists()) dir.mkdirs();

        File[] files = dir.listFiles();
        fileList = new ArrayList<>();
        if (files != null) {
            fileList.addAll(Arrays.asList(files));
        }

        // Sort by Date Descending
        Collections.sort(fileList, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        if (fileList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new FileAdapter(fileList);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMPORT_FILE && resultCode == RESULT_OK && data != null) {
            importFile(data.getData());
        }
    }

    private void importFile(Uri uri) {
        try {
            // Get Name
            String fileName = "Imported_" + System.currentTimeMillis();

            // Try to resolve name from URI if possible (simplified here)
            // Just saving with timestamp to avoid complexity

            InputStream is = getContentResolver().openInputStream(uri);
            File dir = new File(getExternalFilesDir(null), LOCKER_DIR);
            if (!dir.exists()) dir.mkdirs();

            // Detect extension from MIME or just default to .pdf if unknown?
            // Let's check type
            String type = getContentResolver().getType(uri);
            String ext = ".pdf";
            if (type != null && type.contains("spreadsheet")) ext = ".xlsx";

            File dest = new File(dir, fileName + ext);
            FileOutputStream fos = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) fos.write(buffer, 0, length);

            fos.close();
            is.close();

            loadFiles();
            Toast.makeText(this, "File Imported to Vault", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Import Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
        private List<File> list;

        public FileAdapter(List<File> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_file, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
            File file = list.get(position);
            holder.tvName.setText(file.getName());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(file.lastModified())));

            holder.tvSize.setText(Formatter.formatShortFileSize(ReportLockerActivity.this, file.length()));

            if (file.getName().endsWith(".xlsx")) {
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_my_calendar); // Placeholder for Excel
                holder.ivIcon.setColorFilter(getColor(R.color.samsung_blue)); // Green-ish ideally, but using theme
            } else {
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_save); // PDF
                holder.ivIcon.setColorFilter(getColor(R.color.samsung_blue));
            }

            holder.itemView.setOnClickListener(v -> openFile(file));
            holder.itemView.setOnLongClickListener(v -> {
                showDeleteDialog(file);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class FileViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvName, tvDate, tvSize;

            public FileViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_file_icon);
                tvName = itemView.findViewById(R.id.tv_file_name);
                tvDate = itemView.findViewById(R.id.tv_file_date);
                tvSize = itemView.findViewById(R.id.tv_file_size);
            }
        }
    }

    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (file.getName().endsWith(".xlsx")) {
                intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                intent.setDataAndType(uri, "application/pdf");
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteDialog(File file) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to delete '" + file.getName() + "'?")
            .setPositiveButton("Delete", (dialog, which) -> {
                if (file.delete()) {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadFiles();
                } else {
                    Toast.makeText(this, "Delete Failed", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
