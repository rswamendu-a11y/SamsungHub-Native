package com.example.enterprisehub;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PdfExport {

    public static void generateMatrixPdf(Context context, List<SaleItem> dataToExport) {
        PdfDocument document = new PdfDocument();
        // A4 Landscape: 842 x 595
        int pageWidth = 842;
        int pageHeight = 595;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(8);
        textPaint.setTypeface(Typeface.DEFAULT);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(0.5f);

        // 1. Prepare Data
        // Get Unique Brands
        Set<String> brandSet = new HashSet<>();
        for (SaleItem item : dataToExport) {
            brandSet.add(item.getBrand());
        }
        List<String> uniqueBrands = new ArrayList<>(brandSet);
        Collections.sort(uniqueBrands);

        // Group by Date
        Map<String, List<SaleItem>> groupedData = new TreeMap<>(); // Date -> Items
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (SaleItem item : dataToExport) {
            String dateKey = sdf.format(new Date(item.getTimestamp()));
            if (!groupedData.containsKey(dateKey)) {
                groupedData.put(dateKey, new ArrayList<>());
            }
            groupedData.get(dateKey).add(item);
        }

        // 2. Define Columns
        // Fixed: Date(50), Variant(40)
        // Dynamic: Per Brand (Qty 20, Val 40)
        // Fixed End: Total Qty(30), Total Val(50), Logs(150), Summary(100)

        int xStart = 20;
        int yStart = 40;
        int currentX = xStart;

        List<ColumnInfo> columns = new ArrayList<>();
        columns.add(new ColumnInfo("Date", 50));
        columns.add(new ColumnInfo("Variant", 30)); // Placeholder as per screenshot

        for (String brand : uniqueBrands) {
            columns.add(new ColumnInfo(brand + "\nQty", 25));
            columns.add(new ColumnInfo(brand + "\nVal", 45));
        }

        columns.add(new ColumnInfo("Total\nQty", 30));
        columns.add(new ColumnInfo("Total\nVal", 50));
        columns.add(new ColumnInfo("Logs", 200));
        columns.add(new ColumnInfo("Brand Summary", 150));

        // 3. Draw Header
        int headerHeight = 30;
        int y = yStart;

        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        drawRow(canvas, columns, xStart, y, headerHeight, null, textPaint, linePaint, true);
        y += headerHeight;
        textPaint.setTypeface(Typeface.DEFAULT);

        // 4. Draw Data Rows
        for (Map.Entry<String, List<SaleItem>> entry : groupedData.entrySet()) {
            String date = entry.getKey();
            List<SaleItem> items = entry.getValue();

            // Calculate Row Data
            Map<String, Integer> brandQty = new HashMap<>();
            Map<String, Double> brandVal = new HashMap<>();
            int dayTotalQty = 0;
            double dayTotalVal = 0;
            StringBuilder logsBuilder = new StringBuilder();

            // Sort items by brand/model for logs
            Collections.sort(items, Comparator.comparing(SaleItem::getBrand).thenComparing(SaleItem::getModel));

            for (SaleItem item : items) {
                brandQty.put(item.getBrand(), brandQty.getOrDefault(item.getBrand(), 0) + item.getQuantity());
                brandVal.put(item.getBrand(), brandVal.getOrDefault(item.getBrand(), 0.0) + (item.getPrice() * item.getQuantity()));

                dayTotalQty += item.getQuantity();
                dayTotalVal += (item.getPrice() * item.getQuantity());

                // Log Entry: "Samsung S23 (256GB) - 1u (Val: 75000)"
                logsBuilder.append(item.getBrand()).append(" ").append(item.getModel())
                           .append(" (").append(item.getVariant()).append(") - ")
                           .append(item.getQuantity()).append("u (Val: ")
                           .append((int)(item.getPrice() * item.getQuantity())).append(")\n");
            }

            // Brand Summary
            StringBuilder summaryBuilder = new StringBuilder();
            for (String brand : uniqueBrands) {
                int qty = brandQty.getOrDefault(brand, 0);
                double val = brandVal.getOrDefault(brand, 0.0);
                if (qty > 0) {
                    summaryBuilder.append(brand).append(": ").append(qty).append("u ('")
                                  .append((int)val).append(")\n");
                }
            }

            // Build Row Values
            List<String> rowValues = new ArrayList<>();
            rowValues.add(date);
            rowValues.add("0"); // Variant placeholder

            for (String brand : uniqueBrands) {
                rowValues.add(String.valueOf(brandQty.getOrDefault(brand, 0)));
                rowValues.add(String.valueOf(brandVal.getOrDefault(brand, 0.0).intValue()));
            }

            rowValues.add(String.valueOf(dayTotalQty));
            rowValues.add(String.valueOf((int) dayTotalVal));
            rowValues.add(logsBuilder.toString().trim());
            rowValues.add(summaryBuilder.toString().trim());

            // Determine Row Height based on Logs/Summary text
            int measuredHeight = measureMaxRowHeight(columns, rowValues, textPaint);
            int rowHeight = Math.max(20, measuredHeight + 10); // Min height 20 + padding

            // Page Break Check
            if (y + rowHeight > pageHeight - 40) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;
                // Redraw Header
                textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                drawRow(canvas, columns, xStart, y, headerHeight, null, textPaint, linePaint, true);
                y += headerHeight;
                textPaint.setTypeface(Typeface.DEFAULT);
            }

            drawRow(canvas, columns, xStart, y, rowHeight, rowValues, textPaint, linePaint, false);
            y += rowHeight;
        }

        document.finishPage(page);
        savePdfToMediaStore(context, document);
    }

    private static void drawRow(Canvas canvas, List<ColumnInfo> columns, int startX, int y, int height,
                                List<String> data, TextPaint textPaint, Paint linePaint, boolean isHeader) {
        int currentX = startX;
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo col = columns.get(i);
            String text = isHeader ? col.name : data.get(i);

            // Draw Box
            canvas.drawRect(currentX, y, currentX + col.width, y + height, linePaint);

            // Draw Text (Multiline support)
            if (text != null && !text.isEmpty()) {
                canvas.save();
                canvas.translate(currentX + 2, y + 2); // Padding
                StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, col.width - 4)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0, 1.0f)
                        .setIncludePad(false)
                        .build();
                layout.draw(canvas);
                canvas.restore();
            }

            currentX += col.width;
        }
    }

    private static int measureMaxRowHeight(List<ColumnInfo> columns, List<String> data, TextPaint paint) {
        int maxHeight = 0;
        for (int i = 0; i < columns.size(); i++) {
            String text = data.get(i);
            if (text == null || text.isEmpty()) continue;

            StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), paint, columns.get(i).width - 4)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0, 1.0f)
                    .setIncludePad(false)
                    .build();
            if (layout.getHeight() > maxHeight) {
                maxHeight = layout.getHeight();
            }
        }
        return maxHeight;
    }

    private static class ColumnInfo {
        String name;
        int width;
        public ColumnInfo(String name, int width) {
            this.name = name;
            this.width = width;
        }
    }

    private static void savePdfToMediaStore(Context context, PdfDocument document) {
        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Matrix_Report_" + System.currentTimeMillis() + ".pdf");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException("Failed to create new MediaStore record.");
                fos = context.getContentResolver().openOutputStream(uri);
            } else {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                File file = new File(path, "Matrix_Report_" + System.currentTimeMillis() + ".pdf");
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                document.writeTo(fos);
                fos.close();
                Toast.makeText(context, "Matrix PDF Exported", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        document.close();
    }
}
