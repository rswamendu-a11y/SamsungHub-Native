package com.example.enterprisehub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button btnImport = findViewById(R.id.btn_import);
        Button btnPinAction = findViewById(R.id.btn_pin_action);
        Button btnProfile = findViewById(R.id.btn_profile);
        Switch switchDarkMode = findViewById(R.id.switch_dark_mode);

        Button btnExportPdf = findViewById(R.id.btn_export_pdf);
        Button btnExportExcel = findViewById(R.id.btn_export_excel_matrix);
        Button btnExportMaster = findViewById(R.id.btn_export_master_backup);

        btnImport.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ImportActivity.class));
        });

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
        });

        btnExportPdf.setOnClickListener(v -> showPdfExportDialog());
        btnExportExcel.setOnClickListener(v -> showExportDialog(3));
        btnExportMaster.setOnClickListener(v -> showExportDialog(4));

        // PIN Logic
        SharedPreferences prefs = getSharedPreferences("EnterpriseHubPrefs", MODE_PRIVATE);
        boolean hasPin = prefs.getString("user_pin", null) != null;

        if (hasPin) {
            btnPinAction.setText("Remove PIN");
            btnPinAction.setOnClickListener(v -> {
                prefs.edit().remove("user_pin").apply();
                Toast.makeText(this, "PIN Removed", Toast.LENGTH_SHORT).show();
                recreate();
            });
        } else {
            btnPinAction.setText("Set PIN");
            btnPinAction.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                startActivity(intent);
            });
        }

        // Simple Dark Mode Logic
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    private void showPdfExportDialog() {
        android.widget.RadioButton rbSimpleMatrix, rbSegment, rbDailyMatrix;
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_pdf_export_options, null);
        rbSimpleMatrix = view.findViewById(R.id.rb_simple_matrix);
        rbSegment = view.findViewById(R.id.rb_segment);
        rbDailyMatrix = view.findViewById(R.id.rb_daily_matrix);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Report Type")
            .setView(view)
            .setPositiveButton("Next", (dialog, which) -> {
                int type = 1; // Default to Simple Matrix
                if (rbSimpleMatrix.isChecked()) type = 1;
                else if (rbSegment.isChecked()) type = 2;
                else if (rbDailyMatrix.isChecked()) type = 5; // New Type for Daily Matrix

                showExportDialog(type);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showExportDialog(int type) {
        String[] options = {"All Time", "Today", "This Month"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Period")
            .setItems(options, (dialog, which) -> {
                long start = 0;
                long end = System.currentTimeMillis();
                java.util.Calendar cal = java.util.Calendar.getInstance();

                if (which == 1) { // Today
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    cal.set(java.util.Calendar.SECOND, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    start = cal.getTimeInMillis();
                } else if (which == 2) { // This Month
                    cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    cal.set(java.util.Calendar.MINUTE, 0);
                    cal.set(java.util.Calendar.SECOND, 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    start = cal.getTimeInMillis();
                }

                SalesDatabaseHelper dbHelper = new SalesDatabaseHelper(this);
                java.util.List<SaleItem> data = (start == 0) ? dbHelper.getAllSales() : dbHelper.getSalesByDateRange(start, end);

                if (type == 1) PdfExport.createDetailedLedger(this, data);
                else if (type == 2) PdfExport.createSegmentMatrix(this, data);
                else if (type == 3) ExcelExport.generateMatrixExcel(this, data);
                else if (type == 4) ExcelExport.exportMasterBackup(this, data);
                else if (type == 5) PdfExport.createDailyMatrixReport(this, data);
            })
            .show();
    }
}
