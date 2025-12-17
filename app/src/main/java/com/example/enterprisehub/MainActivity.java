package com.example.enterprisehub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome, tvOutletName, tvProverb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWelcome = findViewById(R.id.tv_welcome);
        tvOutletName = findViewById(R.id.tv_outlet_name);
        tvProverb = findViewById(R.id.tv_proverb);

        setupWisdom();
        setupRevenueChart();

        CardView cardTracker = findViewById(R.id.card_tracker);
        CardView cardHistory = findViewById(R.id.card_history);
        CardView cardSettings = findViewById(R.id.card_settings);
        Button btnExit = findViewById(R.id.btn_exit);

        cardTracker.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SalesTrackerActivity.class);
            startActivity(intent);
        });

        cardHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SalesTrackerActivity.class);
            startActivity(intent);
        });

        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnExit.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileInfo();
    }

    private void updateProfileInfo() {
        SharedPreferences prefs = getSharedPreferences("EnterpriseHubPrefs", MODE_PRIVATE);
        String owner = prefs.getString(ProfileActivity.KEY_OWNER, "");
        String outlet = prefs.getString(ProfileActivity.KEY_OUTLET, "Samsung Hub");

        if (!owner.isEmpty()) {
            tvWelcome.setText("Welcome, " + owner);
        } else {
            tvWelcome.setText("Welcome Back.");
        }

        tvOutletName.setText(outlet);
    }

    private void setupWisdom() {
        String[] proverbs = {
            "Sow the seeds of relationship, reap the harvest of sales.",
            "Price is what you pay. Value is what you get.",
            "Don't find customers for your products, find products for your customers.",
            "Quality means doing it right when no one is looking."
        };

        int index = new Random().nextInt(proverbs.length);
        tvProverb.setText("\"" + proverbs[index] + "\"");
    }

    private void setupRevenueChart() {
        try {
            HorizontalBarChart chart = findViewById(R.id.chart_revenue);
            SalesDatabaseHelper dbHelper = new SalesDatabaseHelper(this);
            List<SaleItem> sales = dbHelper.getAllSales();

            // Aggregate Revenue
            Map<String, Double> revenueMap = new HashMap<>();
            for (SaleItem item : sales) {
                revenueMap.put(item.getBrand(), revenueMap.getOrDefault(item.getBrand(), 0.0) + (item.getPrice() * item.getQuantity()));
            }

            // Sort by Revenue (Desc)
            List<Map.Entry<String, Double>> sortedList = new ArrayList<>(revenueMap.entrySet());
            sortedList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();

            // Color Map
            int colorSamsung = android.graphics.Color.parseColor("#1428A0");
            int colorApple = android.graphics.Color.parseColor("#555555");
            int colorRealme = android.graphics.Color.parseColor("#FFC107"); // As requested in prompt
            int colorOppo = android.graphics.Color.parseColor("#4CAF50");
            int colorVivo = android.graphics.Color.parseColor("#2196F3");
            int colorXiaomi = android.graphics.Color.parseColor("#FF5722");
            int colorOthers = android.graphics.Color.parseColor("#9E9E9E");

            for (int i = 0; i < sortedList.size(); i++) {
                Map.Entry<String, Double> entry = sortedList.get(i);
                // HorizontalBarChart renders entries from bottom to top by index.
                // To show highest at top, we need to reverse the index or handle sort logic appropriately.
                // However, usually index 0 is at bottom. Let's invert the list for display purposes if needed.
                // Standard BarChart: 0 is left. Horizontal: 0 is bottom.
                // If we want Highest at TOP, we should add smallest first (index 0) up to largest (index N).
                // But let's check standard MPAndroidChart behavior.
                // Usually for HorizontalBarChart, to have top-down sorting visual, we feed data in reverse order.

                // Let's re-sort or iterate in reverse for the Chart Data
            }

            // Re-sort for Chart Display (Lowest to Highest so Highest is at Top)
            Collections.reverse(sortedList);

            for (int i = 0; i < sortedList.size(); i++) {
                Map.Entry<String, Double> entry = sortedList.get(i);
                entries.add(new BarEntry(i, entry.getValue().floatValue()));
                labels.add(entry.getKey());

                String brand = entry.getKey().toLowerCase();
                if (brand.contains("samsung")) colors.add(colorSamsung);
                else if (brand.contains("apple") || brand.contains("iphone")) colors.add(colorApple);
                else if (brand.contains("realme")) colors.add(colorRealme);
                else if (brand.contains("oppo")) colors.add(colorOppo);
                else if (brand.contains("vivo")) colors.add(colorVivo);
                else if (brand.contains("xiaomi") || brand.contains("redmi")) colors.add(colorXiaomi);
                else colors.add(colorOthers);
            }

            if (!entries.isEmpty()) {
                BarDataSet set = new BarDataSet(entries, "Revenue");
                set.setColors(colors);
                set.setValueTextSize(12f);
                set.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if (value >= 100000) return String.format("%.1fL", value / 100000);
                        if (value >= 1000) return String.format("%.1fk", value / 1000);
                        return String.format("%.0f", value);
                    }
                });

                BarData data = new BarData(set);
                data.setBarWidth(0.6f);

                chart.setData(data);
                chart.setDescription(null);
                chart.setDrawGridBackground(false);
                chart.getLegend().setEnabled(false);

                XAxis xAxis = chart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Becomes Left in Horizontal
                xAxis.setDrawGridLines(false);
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setLabelCount(labels.size());
                xAxis.setGranularity(1f);

                chart.getAxisLeft().setDrawGridLines(false); // Bottom
                chart.getAxisRight().setDrawGridLines(true); // Top
                chart.getAxisRight().setDrawLabels(true); // Top
                chart.getAxisLeft().setDrawLabels(false); // Bottom

                chart.invalidate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
