package com.example.enterprisehub;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class ChartsActivity extends AppCompatActivity {

    private BarChart chartWeekly;
    private BarChart chartMtdComparison;
    private RadioGroup rgChartType;
    private TableLayout tablePriceRange;
    private SalesDatabaseHelper dbHelper;

    // Price Buckets
    private static final String BUCKET_100K_PLUS = "100K & ABOVE";
    private static final String BUCKET_70K_100K = "70K - <100K";
    private static final String BUCKET_40K_70K = "40K - <70K";
    private static final String BUCKET_30K_40K = "30K - <40K";
    private static final String BUCKET_20K_30K = "20K - <30K";
    private static final String BUCKET_15K_20K = "15K - <20K";
    private static final String BUCKET_10K_15K = "10K - <15K";
    private static final String BUCKET_LT_10K = "<10K";

    // Ordered list of buckets for display
    private final String[] ORDERED_BUCKETS = {
        BUCKET_100K_PLUS, BUCKET_70K_100K, BUCKET_40K_70K, BUCKET_30K_40K,
        BUCKET_20K_30K, BUCKET_15K_20K, BUCKET_10K_15K, BUCKET_LT_10K
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        dbHelper = new SalesDatabaseHelper(this);

        chartWeekly = findViewById(R.id.chart_weekly);
        chartMtdComparison = findViewById(R.id.chart_mtd_comparison);
        rgChartType = findViewById(R.id.rg_chart_type);
        tablePriceRange = findViewById(R.id.table_price_range);

        rgChartType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean showValue = (checkedId == R.id.rb_value);
            updateWeeklyChart(showValue);
            updateMtdChart(showValue);
        });

        loadData();
    }

    private void loadData() {
        boolean showValue = (rgChartType.getCheckedRadioButtonId() == R.id.rb_value);
        updateWeeklyChart(showValue);
        updateMtdChart(showValue);
        updatePriceRangeTable();
    }

    private void updateWeeklyChart(boolean showValue) {
        // Range: Last 7 Days
        Calendar cal = Calendar.getInstance();
        long now = System.currentTimeMillis();
        cal.add(Calendar.DAY_OF_YEAR, -6); // Go back 6 days to get 7 days total including today
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        List<SaleItem> sales = dbHelper.getSalesByDateRange(start, now);

        // Group by Date
        Map<String, Double> dataMap = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault()); // e.g., 12-18

        // Initialize last 7 days with 0
        Calendar iterateCal = (Calendar) cal.clone();
        for (int i = 0; i < 7; i++) {
            dataMap.put(sdf.format(iterateCal.getTime()), 0.0);
            iterateCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Fill Data
        for (SaleItem item : sales) {
            String dateKey = sdf.format(new java.util.Date(item.getTimestamp()));
            if (dataMap.containsKey(dateKey)) {
                double val = showValue ? (item.getPrice() * item.getQuantity()) : item.getQuantity();
                dataMap.put(dateKey, dataMap.get(dateKey) + val);
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Double> entry : dataMap.entrySet()) {
            entries.add(new BarEntry(index++, entry.getValue().floatValue()));
            labels.add(entry.getKey());
        }

        BarDataSet set = new BarDataSet(entries, showValue ? "Value (INR)" : "Volume (Qty)");
        set.setColor(Color.parseColor("#1428A0")); // Samsung Blue
        set.setValueTextSize(12f);
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                 if (value == 0) return "";
                 if (showValue) {
                     if (value >= 100000) return String.format("%.1fL", value / 100000);
                     if (value >= 1000) return String.format("%.1fk", value / 1000);
                 }
                 return String.format("%.0f", value);
            }
        });

        BarData data = new BarData(set);
        data.setBarWidth(0.6f);

        chartWeekly.setData(data);

        XAxis xAxis = chartWeekly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        chartWeekly.getAxisLeft().setDrawGridLines(true);
        chartWeekly.getAxisRight().setEnabled(false);
        chartWeekly.getDescription().setEnabled(false);
        chartWeekly.animateY(1000);
        chartWeekly.invalidate();
    }

    private void updateMtdChart(boolean showValue) {
        try {
            Calendar cal = Calendar.getInstance();
            long now = System.currentTimeMillis();

            // MTD Range
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long mtdStart = cal.getTimeInMillis();

            // LMTD Range
            // 1st of Last Month
            cal.add(Calendar.MONTH, -1);
            long lmtdStart = cal.getTimeInMillis();

            // Same Day Last Month (End)
            cal.setTimeInMillis(now);
            cal.add(Calendar.MONTH, -1);
            long lmtdEnd = cal.getTimeInMillis();

            List<SaleItem> mtdSales = dbHelper.getSalesByDateRange(mtdStart, now);
            List<SaleItem> lmtdSales = dbHelper.getSalesByDateRange(lmtdStart, lmtdEnd);

            Map<String, Double> mtdMap = new HashMap<>();
            Map<String, Double> lmtdMap = new HashMap<>();

            // Aggregate Data
            for(SaleItem item : mtdSales) {
                double val = showValue ? (item.getPrice() * item.getQuantity()) : item.getQuantity();
                mtdMap.put(item.getBrand(), mtdMap.getOrDefault(item.getBrand(), 0.0) + val);
            }
            for(SaleItem item : lmtdSales) {
                double val = showValue ? (item.getPrice() * item.getQuantity()) : item.getQuantity();
                lmtdMap.put(item.getBrand(), lmtdMap.getOrDefault(item.getBrand(), 0.0) + val);
            }

            // Collect Brands
            List<String> brands = new ArrayList<>();
            brands.addAll(mtdMap.keySet());
            for(String b : lmtdMap.keySet()) if(!brands.contains(b)) brands.add(b);
            java.util.Collections.sort(brands);

            List<BarEntry> mtdEntries = new ArrayList<>();
            List<BarEntry> lmtdEntries = new ArrayList<>();

            for(int i=0; i<brands.size(); i++) {
                String brand = brands.get(i);
                mtdEntries.add(new BarEntry(i, mtdMap.getOrDefault(brand, 0.0).floatValue()));
                lmtdEntries.add(new BarEntry(i, lmtdMap.getOrDefault(brand, 0.0).floatValue()));
            }

            if (!brands.isEmpty()) {
                BarDataSet set1 = new BarDataSet(mtdEntries, showValue ? "Current Month (Value)" : "Current Month (Qty)");
                set1.setColor(Color.parseColor("#1428A0")); // Samsung Blue
                set1.setValueTextSize(10f);
                set1.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if (value == 0) return "";
                        if (showValue) {
                            if (value >= 100000) return String.format("%.1fL", value / 100000);
                            if (value >= 1000) return String.format("%.1fk", value / 1000);
                        }
                        return String.format("%.0f", value);
                    }
                });

                BarDataSet set2 = new BarDataSet(lmtdEntries, showValue ? "Last Month (Value)" : "Last Month (Qty)");
                set2.setColor(Color.LTGRAY);
                set2.setValueTextSize(10f);
                set2.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if (value == 0) return "";
                        if (showValue) {
                            if (value >= 100000) return String.format("%.1fL", value / 100000);
                            if (value >= 1000) return String.format("%.1fk", value / 1000);
                        }
                        return String.format("%.0f", value);
                    }
                });

                float groupSpace = 0.08f;
                float barSpace = 0.03f;
                float barWidth = 0.43f;

                BarData data = new BarData(set1, set2);
                data.setBarWidth(barWidth);

                chartMtdComparison.setData(data);
                chartMtdComparison.groupBars(0, groupSpace, barSpace);

                // Axis Setup
                XAxis xAxis = chartMtdComparison.getXAxis();
                xAxis.setAxisMinimum(0);
                xAxis.setAxisMaximum(brands.size());
                xAxis.setValueFormatter(new IndexAxisValueFormatter(brands));
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setCenterAxisLabels(true);
                xAxis.setGranularity(1f);
                xAxis.setDrawGridLines(false);

                chartMtdComparison.getAxisLeft().setDrawGridLines(true);
                chartMtdComparison.getAxisRight().setDrawGridLines(false);
                chartMtdComparison.getAxisRight().setDrawLabels(false);

                chartMtdComparison.getDescription().setEnabled(false);
                chartMtdComparison.setDrawGridBackground(false);
                chartMtdComparison.getLegend().setWordWrapEnabled(true);

                chartMtdComparison.invalidate();
            } else {
                chartMtdComparison.clear();
            }

        } catch(Exception e) {
             e.printStackTrace();
        }
    }

    private void updatePriceRangeTable() {
        // Data Source: MTD (Month to Date) usually makes sense for such tables unless specified otherwise.
        // Or "All Time". Let's stick to MTD to be consistent with the "Tracker" philosophy.
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long now = System.currentTimeMillis();

        List<SaleItem> sales = dbHelper.getSalesByDateRange(start, now);

        Map<String, Integer> bucketCounts = new HashMap<>();
        for(String b : ORDERED_BUCKETS) bucketCounts.put(b, 0);

        for (SaleItem item : sales) {
            double price = item.getPrice(); // Unit price usually determines the bucket
            String bucket = getBucket(price);
            int qty = item.getQuantity(); // Add quantity? Or count unique sales? Usually Volume.
            // Screenshot shows "Mobile Phones" row with counts.
            bucketCounts.put(bucket, bucketCounts.get(bucket) + qty);
        }

        tablePriceRange.removeAllViews();

        // Header Row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#EEEEEE"));
        addCellToRow(headerRow, "CATEGORY", true);
        for (String bucket : ORDERED_BUCKETS) {
            addCellToRow(headerRow, bucket, true);
        }
        tablePriceRange.addView(headerRow);

        // Data Row (Assuming only Mobile Phones for now, as we don't distinguish Product Types in DB explicitly other than Brand)
        // If we had 'Product Category', we'd iterate. DB has Brand/Model. Assuming all are Phones.
        TableRow dataRow = new TableRow(this);
        addCellToRow(dataRow, "Mobile Phones", false);
        for (String bucket : ORDERED_BUCKETS) {
            addCellToRow(dataRow, String.valueOf(bucketCounts.get(bucket)), false);
        }
        tablePriceRange.addView(dataRow);
    }

    private void addCellToRow(TableRow row, String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16, 16, 16, 16);
        tv.setGravity(Gravity.CENTER);
        if (isHeader) {
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTextColor(Color.BLACK);
        } else {
            tv.setTextColor(Color.DKGRAY);
        }
        // Add border if needed, usually background drawable on cell
        // For simplicity, just text
        row.addView(tv);
    }

    private String getBucket(double price) {
        if (price >= 100000) return BUCKET_100K_PLUS;
        if (price >= 70000) return BUCKET_70K_100K;
        if (price >= 40000) return BUCKET_40K_70K;
        if (price >= 30000) return BUCKET_30K_40K;
        if (price >= 20000) return BUCKET_20K_30K;
        if (price >= 15000) return BUCKET_15K_20K;
        if (price >= 10000) return BUCKET_10K_15K;
        return BUCKET_LT_10K;
    }
}
