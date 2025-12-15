package com.example.enterprisehub;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private EditText etBrand, etModel, etVariant, etQuantity, etPrice;
    private SalesDatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private SaleAdapter adapter;
    private List<SaleItem> saleList;
    private PieChart pieChart;
    private BarChart barChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");

        setContentView(R.layout.activity_sales_tracker);

        dbHelper = new SalesDatabaseHelper(this);

        etBrand = findViewById(R.id.et_brand);
        etModel = findViewById(R.id.et_model);
        etVariant = findViewById(R.id.et_variant);
        etQuantity = findViewById(R.id.et_quantity);
        etPrice = findViewById(R.id.et_price);

        Button btnAddSale = findViewById(R.id.btn_add_sale);
        Button btnExportPdf = findViewById(R.id.btn_export_pdf);
        Button btnExportExcel = findViewById(R.id.btn_export_excel);

        recyclerView = findViewById(R.id.recycler_view_sales);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadSales();

        btnAddSale.setOnClickListener(v -> addSale());
        btnExportPdf.setOnClickListener(v -> exportToPdf());
        btnExportExcel.setOnClickListener(v -> exportToExcel());
    }

    private void loadSales() {
        saleList = dbHelper.getAllSales();
        adapter = new SaleAdapter(saleList);
        recyclerView.setAdapter(adapter);
        updateChart();
    }

    private void addSale() {
        String brand = etBrand.getText().toString().trim();
        String model = etModel.getText().toString().trim();
        String variant = etVariant.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (brand.isEmpty() || model.isEmpty() || variant.isEmpty() || qtyStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            double price = Double.parseDouble(priceStr);
            long timestamp = System.currentTimeMillis();

            String segment = calculateSegment(price);

            SaleItem newItem = new SaleItem(0, brand, model, variant, qty, price, segment, timestamp);
            dbHelper.addSale(newItem);

            etBrand.setText("");
            etModel.setText("");
            etVariant.setText("");
            etQuantity.setText("");
            etPrice.setText("");

            loadSales();
            Toast.makeText(this, "Sale Added", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }

    // Smart Segmentation Logic
    private String calculateSegment(double price) {
        // Formula: (Price / 10000) * 10k to get lower bound
        long lower = (long) (Math.floor(price / 10000.0) * 10000);
        long upper = lower + 10000;
        return (lower / 1000) + "k-" + (upper / 1000) + "k";
    }

    private void updateChart() {
        Map<String, Integer> brandCounts = new HashMap<>();
        for (SaleItem item : saleList) {
            brandCounts.put(item.getBrand(), brandCounts.getOrDefault(item.getBrand(), 0) + item.getQuantity());
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : brandCounts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Brand Share");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.setDescription(null);
        pieChart.invalidate();

        // Update Bar Chart
        List<BarEntry> barEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Integer> entry : brandCounts.entrySet()) {
            barEntries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Volume by Brand");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.setDescription(null);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        barChart.invalidate();
    }

    private void exportToPdf() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size approx
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);

        int y = 40;
        canvas.drawText("Sales Report", 20, y, paint);
        y += 30;

        // Headers
        canvas.drawText("Brand | Model | Qty | Price | Segment", 20, y, paint);
        y += 20;
        canvas.drawLine(20, y, 500, y, paint);
        y += 20;

        Map<String, Integer> brandVolume = new HashMap<>();
        Map<String, Double> brandValue = new HashMap<>();

        for(SaleItem item : saleList) {
             String line = item.getBrand() + " | " + item.getModel() + " | " + item.getQuantity() + " | " + item.getPrice() + " | " + item.getSegment();
             canvas.drawText(line, 20, y, paint);
             y += 20;

             // Aggregate for summary
             brandVolume.put(item.getBrand(), brandVolume.getOrDefault(item.getBrand(), 0) + item.getQuantity());
             brandValue.put(item.getBrand(), brandValue.getOrDefault(item.getBrand(), 0.0) + (item.getPrice() * item.getQuantity()));

             if (y > 750) { // Simple pagination check
                 document.finishPage(page);
                 page = document.startPage(pageInfo);
                 canvas = page.getCanvas();
                 y = 40;
             }
        }

        // Summary Table
        y += 30;
        paint.setFakeBoldText(true);
        canvas.drawText("Summary by Brand", 20, y, paint);
        y += 20;
        paint.setFakeBoldText(false);

        for (String brand : brandVolume.keySet()) {
            String summary = brand + ": Volume=" + brandVolume.get(brand) + ", Value=" + String.format("%.2f", brandValue.get(brand));
            canvas.drawText(summary, 20, y, paint);
            y += 20;
        }

        document.finishPage(page);

        File file = new File(getExternalFilesDir(null), "sales_report_v2.pdf");
        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
        }
        document.close();
    }

    private void exportToExcel() {
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

        for (SaleItem item : saleList) {
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

        File file = new File(getExternalFilesDir(null), "sales_report_v2.xlsx");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
            workbook.close();
            Toast.makeText(this, "Excel saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving Excel", Toast.LENGTH_SHORT).show();
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
            holder.tvPrice.setText(String.format("%.2f (%s)", item.getPrice(), item.getSegment()));

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
