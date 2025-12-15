package com.example.enterprisehub;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SalesTrackerActivity extends AppCompatActivity {

    private EditText etModel, etVariant, etQuantity, etPrice, etDate;
    private Spinner spBrand;
    private TextView tvDashboardSummary;
    private SalesDatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private SaleAdapter adapter;
    private List<SaleItem> saleList;
    private PieChart pieChart;
    private BarChart barChart;
    private long selectedTimestamp = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");

        setContentView(R.layout.activity_sales_tracker);

        checkSecurity();

        dbHelper = new SalesDatabaseHelper(this);

        spBrand = findViewById(R.id.sp_brand);
        etModel = findViewById(R.id.et_model);
        etVariant = findViewById(R.id.et_variant);
        etQuantity = findViewById(R.id.et_quantity);
        etPrice = findViewById(R.id.et_price);
        etDate = findViewById(R.id.et_date);
        tvDashboardSummary = findViewById(R.id.tv_dashboard_summary);

        Button btnAddSale = findViewById(R.id.btn_add_sale);
        Button btnExportPdf = findViewById(R.id.btn_export_pdf);
        Button btnExportExcel = findViewById(R.id.btn_export_excel);
        ImageView btnDatePicker = findViewById(R.id.btn_date_picker);

        recyclerView = findViewById(R.id.recycler_view_sales);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupSpinner();
        loadSales();
        updateDateLabel();

        btnAddSale.setOnClickListener(v -> addSale());
        btnExportPdf.setOnClickListener(v -> showExportDialog(true));
        btnExportExcel.setOnClickListener(v -> showExportDialog(false));

        View.OnClickListener datePickerListener = v -> showDatePicker();
        etDate.setOnClickListener(datePickerListener);
        btnDatePicker.setOnClickListener(datePickerListener);
    }

    private void checkSecurity() {
        long lastActive = getSharedPreferences("EnterpriseHubPrefs", MODE_PRIVATE).getLong(LoginActivity.KEY_LAST_ACTIVE, 0);
        long now = System.currentTimeMillis();

        // 2 minutes timeout
        if (now - lastActive > 2 * 60 * 1000) {
             startActivity(new Intent(this, LoginActivity.class));
             finish();
        } else {
            // Reset timer on interaction (here approximated by onResume/Activity start)
            getSharedPreferences("EnterpriseHubPrefs", MODE_PRIVATE).edit().putLong(LoginActivity.KEY_LAST_ACTIVE, now).apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSecurity();
    }

    private void setupSpinner() {
        String[] brands = {"Samsung", "Apple", "Realme", "Xiaomi", "Oppo", "Vivo", "Motorola", "Others"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, brands);
        spBrand.setAdapter(adapter);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedTimestamp);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            Calendar newDate = Calendar.getInstance();
            newDate.set(year1, month1, dayOfMonth);
            selectedTimestamp = newDate.getTimeInMillis();
            updateDateLabel();
        }, year, month, day);
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etDate.setText(sdf.format(new Date(selectedTimestamp)));
    }

    private void showExportDialog(boolean isPdf) {
        String[] options = {"All Time", "Today", "This Month"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Export Period")
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

                List<SaleItem> data = (start == 0) ? dbHelper.getAllSales() : dbHelper.getSalesByDateRange(start, end);

                if (isPdf) PdfExport.generateMatrixPdf(this, data);
                else exportToExcel(data);
            })
            .show();
    }

    private void loadSales() {
        try {
            saleList = dbHelper.getAllSales();
            adapter = new SaleAdapter(saleList);
            recyclerView.setAdapter(adapter);
            updateChart();
        } catch (Exception e) {
            Log.e("SalesTracker", "Error loading sales: " + e.getMessage());
            Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
        }
    }

    private void addSale() {
        String brand = spBrand.getSelectedItem().toString();
        String model = etModel.getText().toString().trim();
        String variant = etVariant.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (model.isEmpty() || variant.isEmpty() || qtyStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            double price = Double.parseDouble(priceStr);

            String segment = calculateSegment(price);

            // Use selectedTimestamp. Do NOT reset it immediately before adding.
            SaleItem newItem = new SaleItem(0, brand, model, variant, qty, price, segment, selectedTimestamp);
            dbHelper.addSale(newItem);

            // Clear fields but KEEP the Date
            etModel.setText("");
            etVariant.setText("");
            etQuantity.setText("");
            etPrice.setText("");

            // Reload
            loadSales();
            Toast.makeText(this, "Sale Added", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("SalesTracker", "Error adding sale: " + e.getMessage());
            Toast.makeText(this, "Error adding sale", Toast.LENGTH_SHORT).show();
        }
    }

    // Smart Segmentation Logic
    private String calculateSegment(double price) {
        long lower = (long) (Math.floor(price / 10000.0) * 10000);
        long upper = lower + 10000;
        return (lower / 1000) + "k-" + (upper / 1000) + "k";
    }

    private void updateChart() {
        try {
            Map<String, Integer> brandCounts = new HashMap<>(); // For Bar Chart (Volume)
            Map<String, Double> brandValues = new HashMap<>(); // For Pie Chart (Value)

            for (SaleItem item : saleList) {
                brandCounts.put(item.getBrand(), brandCounts.getOrDefault(item.getBrand(), 0) + item.getQuantity());
                brandValues.put(item.getBrand(), brandValues.getOrDefault(item.getBrand(), 0.0) + (item.getPrice() * item.getQuantity()));
            }

            // PIE CHART: Show VALUE (Rupees)
            List<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : brandValues.entrySet()) {
                // Formatting as Integer for cleaner look on chart, or Float
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }

            if (!entries.isEmpty()) {
                PieDataSet dataSet = new PieDataSet(entries, "Brand Share (Value)");
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                dataSet.setValueTextSize(14f); // UPGRADE: Bold/Size
                dataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);

                PieData pieData = new PieData(dataSet);
                pieChart.setData(pieData);
                pieChart.setDescription(null);
                pieChart.setEntryLabelTextSize(12f);
                pieChart.setEntryLabelColor(Color.BLACK);
                pieChart.invalidate();
            } else {
                 pieChart.clear();
            }

            // BAR CHART: Show VOLUME (Counts) - Kept as requested
            List<BarEntry> barEntries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();

            int index = 0;
            // Define colors
            int colorSamsung = Color.parseColor("#1428A0");
            int colorRealme = Color.parseColor("#FFD700");
            int colorApple = Color.parseColor("#555555");
            int colorGreen = Color.parseColor("#008000"); // Oppo/Vivo
            int colorOthers = Color.parseColor("#999999");

            for (Map.Entry<String, Integer> entry : brandCounts.entrySet()) {
                barEntries.add(new BarEntry(index, entry.getValue()));
                labels.add(entry.getKey());

                String brand = entry.getKey().toLowerCase();
                if (brand.contains("samsung")) colors.add(colorSamsung);
                else if (brand.contains("realme")) colors.add(colorRealme);
                else if (brand.contains("apple") || brand.contains("iphone")) colors.add(colorApple);
                else if (brand.contains("oppo") || brand.contains("vivo")) colors.add(colorGreen);
                else colors.add(colorOthers);

                index++;
            }

            if (!barEntries.isEmpty()) {
                BarDataSet barDataSet = new BarDataSet(barEntries, "Volume by Brand");
                barDataSet.setColors(colors);
                barDataSet.setDrawValues(true);
                barDataSet.setValueTextSize(14f); // UPGRADE: Bold/Size
                barDataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);

                BarData barData = new BarData(barDataSet);
                barData.setBarWidth(0.9f);

                barChart.setData(barData);
                barChart.setDescription(null);
                barChart.setDrawGridBackground(true);
                barChart.setGridBackgroundColor(Color.WHITE);

                XAxis xAxis = barChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setGranularity(1f);
                xAxis.setGranularityEnabled(true);
                xAxis.setDrawGridLines(false); // Only Y axis grid usually needed
                xAxis.setTextSize(12f);

                barChart.getAxisLeft().setDrawGridLines(true);
                barChart.getAxisRight().setDrawGridLines(false);

                barChart.invalidate();
            } else {
                barChart.clear();
            }

            updateSummaryText();
        } catch (Exception e) {
            Log.e("SalesTracker", "Error updating charts: " + e.getMessage());
        }
    }

    private void updateSummaryText() {
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();

            // This Month
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            long thisMonthStart = cal.getTimeInMillis();
            long now = System.currentTimeMillis();

            // Last Month
            cal.add(java.util.Calendar.MONTH, -1);
            long lastMonthStart = cal.getTimeInMillis();
            cal.add(java.util.Calendar.MONTH, 1);
            cal.add(java.util.Calendar.MILLISECOND, -1);
            long lastMonthEnd = cal.getTimeInMillis();

            List<SaleItem> thisMonthSales = dbHelper.getSalesByDateRange(thisMonthStart, now);
            List<SaleItem> lastMonthSales = dbHelper.getSalesByDateRange(lastMonthStart, lastMonthEnd);

            double thisMonthValue = 0;
            for (SaleItem item : thisMonthSales) thisMonthValue += (item.getPrice() * item.getQuantity());

            double lastMonthValue = 0;
            for (SaleItem item : lastMonthSales) lastMonthValue += (item.getPrice() * item.getQuantity());

            String summary = String.format(Locale.getDefault(), "This Month: \u20B9%.2f | Last Month: \u20B9%.2f", thisMonthValue, lastMonthValue);
            tvDashboardSummary.setText(summary);
        } catch (Exception e) {
             Log.e("SalesTracker", "Error updating summary: " + e.getMessage());
        }
    }

    private void exportToExcel(List<SaleItem> dataToExport) {
        // PATCH: Sort Ascending for Excel too
        Collections.sort(dataToExport, Comparator.comparingLong(SaleItem::getTimestamp));

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sales");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Brand");
        headerRow.createCell(2).setCellValue("Model");
        headerRow.createCell(3).setCellValue("Variant");
        headerRow.createCell(4).setCellValue("Quantity");
        headerRow.createCell(5).setCellValue("Price");
        headerRow.createCell(6).setCellValue("Segment");
        headerRow.createCell(7).setCellValue("Date");

        int rowNum = 1;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (SaleItem item : dataToExport) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getId());
            row.createCell(1).setCellValue(item.getBrand());
            row.createCell(2).setCellValue(item.getModel());
            row.createCell(3).setCellValue(item.getVariant());
            row.createCell(4).setCellValue(item.getQuantity());
            row.createCell(5).setCellValue(item.getPrice());
            row.createCell(6).setCellValue(item.getSegment());
            row.createCell(7).setCellValue(sdf.format(new Date(item.getTimestamp())));
        }

        // Robust Save using MediaStore
        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "sales_report_" + System.currentTimeMillis() + ".xlsx");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException("Failed to create new MediaStore record.");
                fos = getContentResolver().openOutputStream(uri);
            } else {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                java.io.File file = new java.io.File(path, "sales_report_" + System.currentTimeMillis() + ".xlsx");
                fos = new java.io.FileOutputStream(file);
            }

            if (fos != null) {
                workbook.write(fos);
                fos.close();
                Toast.makeText(this, "Excel Exported Successfully", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving Excel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        try {
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.SaleViewHolder> {
        private List<SaleItem> list;

        public SaleAdapter(List<SaleItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public SaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale, parent, false);
            return new SaleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SaleViewHolder holder, int position) {
            SaleItem item = list.get(position);
            holder.tvName.setText(item.getItemName()); // Uses "Brand Model Variant"
            holder.tvQty.setText("Qty: " + item.getQuantity());
            // Use Rupee Symbol
            holder.tvPrice.setText(String.format("\u20B9%.2f (%s)", item.getPrice(), item.getSegment()));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(item.getTimestamp())));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class SaleViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvQty, tvPrice, tvDate;

            public SaleViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_item_name);
                tvQty = itemView.findViewById(R.id.tv_quantity);
                tvPrice = itemView.findViewById(R.id.tv_price);
                tvDate = itemView.findViewById(R.id.tv_date);
            }
        }
    }
}
