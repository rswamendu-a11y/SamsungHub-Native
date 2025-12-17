package com.example.enterprisehub;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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

public class PdfExport {

    public static void createDailyMatrixReport(Context context, List<SaleItem> dataToExport) {
        generateMatrixPdfInternal(context, dataToExport);
    }

    public static void createDetailedLedger(Context context, List<SaleItem> dataToExport) {
        PdfDocument document = new PdfDocument();
        int pageWidth = 842;
        int pageHeight = 595;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(10);
        textPaint.setTypeface(Typeface.DEFAULT);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(0.5f);

        // Define Columns
        List<ColumnInfo> columns = new ArrayList<>();
        columns.add(new ColumnInfo("Date", 80));
        columns.add(new ColumnInfo("Brand", 100));
        columns.add(new ColumnInfo("Model", 150));
        columns.add(new ColumnInfo("Variant", 100));
        columns.add(new ColumnInfo("Qty", 50));
        columns.add(new ColumnInfo("Price", 80));
        columns.add(new ColumnInfo("Total", 80));

        // Draw Header
        SharedPreferences prefs = context.getSharedPreferences("EnterpriseHubPrefs", Context.MODE_PRIVATE);
        String ownerName = prefs.getString(ProfileActivity.KEY_OWNER, "");
        String outletName = prefs.getString(ProfileActivity.KEY_OUTLET, "Samsung Hub");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        drawDocumentHeader(canvas, ownerName, outletName, "Detailed Ledger - " + sdf.format(new Date()), pageWidth);

        int xStart = 40;
        int yStart = 80;
        int y = yStart;
        int rowHeight = 25;

        // Draw Table Header
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        drawRow(canvas, columns, xStart, y, rowHeight, null, textPaint, linePaint, true);
        y += rowHeight;
        textPaint.setTypeface(Typeface.DEFAULT);

        // Sort Data by Date Descending
        Collections.sort(dataToExport, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Draw Data Rows
        for (SaleItem item : dataToExport) {
            List<String> rowValues = new ArrayList<>();
            rowValues.add(dateSdf.format(new Date(item.getTimestamp())));
            rowValues.add(item.getBrand());
            rowValues.add(item.getModel());
            rowValues.add(item.getVariant());
            rowValues.add(String.valueOf(item.getQuantity()));
            rowValues.add(String.valueOf((int)item.getPrice()));
            rowValues.add(String.valueOf((int)(item.getPrice() * item.getQuantity())));

            if (y + rowHeight > pageHeight - 40) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                drawDocumentHeader(canvas, ownerName, outletName, "Detailed Ledger", pageWidth);
                y = yStart;
                textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                drawRow(canvas, columns, xStart, y, rowHeight, null, textPaint, linePaint, true);
                y += rowHeight;
                textPaint.setTypeface(Typeface.DEFAULT);
            }

            drawRow(canvas, columns, xStart, y, rowHeight, rowValues, textPaint, linePaint, false);
            y += rowHeight;
        }

        document.finishPage(page);
        savePdfToMediaStore(context, document, "Detailed_Ledger");
    }

    private static void generateMatrixPdfInternal(Context context, List<SaleItem> dataToExport) {
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
        Set<String> brandSet = new HashSet<>();
        long minDate = Long.MAX_VALUE;
        long maxDate = Long.MIN_VALUE;

        for (SaleItem item : dataToExport) {
            brandSet.add(item.getBrand());
            if (item.getTimestamp() < minDate) minDate = item.getTimestamp();
            if (item.getTimestamp() > maxDate) maxDate = item.getTimestamp();
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
        // Fixed: Date(50)
        // Dynamic: Per Brand (Qty 25, Val 45)
        // Fixed End: Total Qty(30), Total Val(50), Logs(200), Summary(150)

        int xStart = 20;
        int yStart = 80; // Shifted down for Header

        List<ColumnInfo> columns = new ArrayList<>();
        columns.add(new ColumnInfo("Date", 50));

        for (String brand : uniqueBrands) {
            columns.add(new ColumnInfo(brand + "\nQty", 25));
            columns.add(new ColumnInfo(brand + "\nVal", 45));
        }

        columns.add(new ColumnInfo("Total\nQty", 30));
        columns.add(new ColumnInfo("Total\nVal", 50));

        columns.add(new ColumnInfo("Logs", 200));
        columns.add(new ColumnInfo("Brand Summary", 150));

        // 3. Draw Document Header (Profile + Month/Year)
        SharedPreferences prefs = context.getSharedPreferences("EnterpriseHubPrefs", Context.MODE_PRIVATE);
        String ownerName = prefs.getString(ProfileActivity.KEY_OWNER, "");
        String outletName = prefs.getString(ProfileActivity.KEY_OUTLET, "Samsung Hub");

        // Determine Month/Year string
        SimpleDateFormat headerSdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String dateRangeStr;
        if (minDate == Long.MAX_VALUE) {
            dateRangeStr = headerSdf.format(new Date());
        } else {
            String start = headerSdf.format(new Date(minDate));
            String end = headerSdf.format(new Date(maxDate));
            if (start.equals(end)) dateRangeStr = start;
            else dateRangeStr = start + " - " + end;
        }

        drawDocumentHeader(canvas, ownerName, outletName, dateRangeStr, pageWidth);

        // 4. Draw Table Header
        int headerHeight = 30;
        int y = yStart;

        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        drawRow(canvas, columns, xStart, y, headerHeight, null, textPaint, linePaint, true);
        y += headerHeight;
        textPaint.setTypeface(Typeface.DEFAULT);

        // Initialize Total Accumulators
        Map<String, Integer> totalBrandQty = new HashMap<>();
        Map<String, Double> totalBrandVal = new HashMap<>();
        int grandTotalQty = 0;
        double grandTotalVal = 0;

        // 5. Draw Data Rows
        for (Map.Entry<String, List<SaleItem>> entry : groupedData.entrySet()) {
            String date = entry.getKey();
            List<SaleItem> items = entry.getValue();

            // Calculate Row Data
            Map<String, Integer> brandQty = new HashMap<>();
            Map<String, Double> brandVal = new HashMap<>();
            int dayTotalQty = 0;
            double dayTotalVal = 0;
            StringBuilder logsBuilder = new StringBuilder();

            Collections.sort(items, Comparator.comparing(SaleItem::getBrand).thenComparing(SaleItem::getModel));

            for (SaleItem item : items) {
                int q = item.getQuantity();
                double v = item.getPrice() * q;

                brandQty.put(item.getBrand(), brandQty.getOrDefault(item.getBrand(), 0) + q);
                brandVal.put(item.getBrand(), brandVal.getOrDefault(item.getBrand(), 0.0) + v);

                dayTotalQty += q;
                dayTotalVal += v;

                // Accumulate Grand Totals
                totalBrandQty.put(item.getBrand(), totalBrandQty.getOrDefault(item.getBrand(), 0) + q);
                totalBrandVal.put(item.getBrand(), totalBrandVal.getOrDefault(item.getBrand(), 0.0) + v);
                grandTotalQty += q;
                grandTotalVal += v;

                logsBuilder.append(item.getBrand()).append(" ").append(item.getModel())
                           .append(" (").append(item.getVariant()).append(") - ")
                           .append(q).append("u (Val: ")
                           .append((int)v).append(")\n");
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
                // Redraw Headers on new page
                drawDocumentHeader(canvas, ownerName, outletName, dateRangeStr, pageWidth);
                y = yStart;
                textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                drawRow(canvas, columns, xStart, y, headerHeight, null, textPaint, linePaint, true);
                y += headerHeight;
                textPaint.setTypeface(Typeface.DEFAULT);
            }

            drawRow(canvas, columns, xStart, y, rowHeight, rowValues, textPaint, linePaint, false);
            y += rowHeight;
        }

        // 6. Draw Total Row
        List<String> totalRowValues = new ArrayList<>();
        totalRowValues.add("TOTAL");

        for (String brand : uniqueBrands) {
            totalRowValues.add(String.valueOf(totalBrandQty.getOrDefault(brand, 0)));
            totalRowValues.add(String.valueOf(totalBrandVal.getOrDefault(brand, 0.0).intValue()));
        }

        totalRowValues.add(String.valueOf(grandTotalQty));
        totalRowValues.add(String.valueOf((int) grandTotalVal));

        totalRowValues.add(""); // Logs empty
        totalRowValues.add(""); // Summary empty

        // Check space for Total Row
        if (y + 30 > pageHeight - 40) {
            document.finishPage(page);
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            drawDocumentHeader(canvas, ownerName, outletName, dateRangeStr, pageWidth);
            y = yStart;
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            drawRow(canvas, columns, xStart, y, headerHeight, null, textPaint, linePaint, true);
            y += headerHeight;
            textPaint.setTypeface(Typeface.DEFAULT);
        }

        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        drawRow(canvas, columns, xStart, y, 30, totalRowValues, textPaint, linePaint, false);

        document.finishPage(page);
        savePdfToMediaStore(context, document, "Daily_Matrix_Log");
    }

    private static void drawDocumentHeader(Canvas canvas, String owner, String outlet, String dateStr, int pageWidth) {
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(14);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        Paint subPaint = new Paint();
        subPaint.setColor(Color.DKGRAY);
        subPaint.setTextSize(10);
        subPaint.setTypeface(Typeface.DEFAULT);
        subPaint.setTextAlign(Paint.Align.CENTER);

        float centerX = pageWidth / 2f;

        canvas.drawText((owner.isEmpty() ? "" : owner + " - ") + outlet, centerX, 30, titlePaint);
        canvas.drawText(dateStr, centerX, 50, subPaint);
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

    public static void createSegmentMatrix(Context context, List<SaleItem> dataToExport) {
        PdfDocument document = new PdfDocument();
        int pageWidth = 842;
        int pageHeight = 595;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        TextPaint textPaint = new TextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(10);
        textPaint.setTypeface(Typeface.DEFAULT);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(0.5f);

        // 1. Prepare Data
        Set<String> brandSet = new HashSet<>();
        Set<String> segmentSet = new HashSet<>();

        // Map: Segment -> (Brand -> Volume)
        Map<String, Map<String, Integer>> matrix = new TreeMap<>();

        for (SaleItem item : dataToExport) {
            brandSet.add(item.getBrand());
            segmentSet.add(item.getSegment());

            if (!matrix.containsKey(item.getSegment())) matrix.put(item.getSegment(), new HashMap<>());

            Map<String, Integer> brandMap = matrix.get(item.getSegment());
            brandMap.put(item.getBrand(), brandMap.getOrDefault(item.getBrand(), 0) + item.getQuantity());
        }

        List<String> uniqueBrands = new ArrayList<>(brandSet);
        Collections.sort(uniqueBrands);

        // 2. Define Columns
        int xStart = 40;
        int yStart = 80;

        List<ColumnInfo> columns = new ArrayList<>();
        columns.add(new ColumnInfo("Segment", 80));

        for (String brand : uniqueBrands) {
            columns.add(new ColumnInfo(brand, 60));
        }
        columns.add(new ColumnInfo("Total", 60));

        // 3. Draw Header
        SharedPreferences prefs = context.getSharedPreferences("EnterpriseHubPrefs", Context.MODE_PRIVATE);
        String ownerName = prefs.getString(ProfileActivity.KEY_OWNER, "");
        String outletName = prefs.getString(ProfileActivity.KEY_OUTLET, "Samsung Hub");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        drawDocumentHeader(canvas, ownerName, outletName, "Segment Matrix - " + sdf.format(new Date()), pageWidth);

        // 4. Draw Table
        int y = yStart;
        int rowHeight = 30;

        // Header Row
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        drawRow(canvas, columns, xStart, y, rowHeight, null, textPaint, linePaint, true);
        y += rowHeight;
        textPaint.setTypeface(Typeface.DEFAULT);

        // Data Rows
        for (Map.Entry<String, Map<String, Integer>> entry : matrix.entrySet()) {
            String segment = entry.getKey();
            Map<String, Integer> brandData = entry.getValue();
            List<String> rowValues = new ArrayList<>();
            rowValues.add(segment);

            int rowTotal = 0;
            for(String brand : uniqueBrands) {
                int val = brandData.getOrDefault(brand, 0);
                rowValues.add(val == 0 ? "-" : String.valueOf(val));
                rowTotal += val;
            }
            rowValues.add(String.valueOf(rowTotal));

            drawRow(canvas, columns, xStart, y, rowHeight, rowValues, textPaint, linePaint, false);
            y += rowHeight;
        }

        document.finishPage(page);
        savePdfToMediaStore(context, document, "Segment_Report");
    }

    private static void savePdfToMediaStore(Context context, PdfDocument document) {
        savePdfToMediaStore(context, document, "Matrix_Report");
    }

    private static void savePdfToMediaStore(Context context, PdfDocument document, String fileNamePrefix) {
        Uri pdfUri = null;
        try {
            OutputStream fos;
            String fileName = fileNamePrefix + "_" + System.currentTimeMillis() + ".pdf";

            // 1. Save to Downloads (User visible)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                pdfUri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (pdfUri == null) throw new IOException("Failed to create new MediaStore record.");
                fos = context.getContentResolver().openOutputStream(pdfUri);
            } else {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                File file = new File(path, fileName);
                pdfUri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                document.writeTo(fos);
                fos.close();
                Toast.makeText(context, fileNamePrefix + " Exported", Toast.LENGTH_LONG).show();

                // 2. Auto-Save to Locker (Internal Storage)
                try {
                    File lockerDir = new File(context.getExternalFilesDir(null), "Sales_Vault");
                    if (!lockerDir.exists()) lockerDir.mkdirs();
                    File lockerFile = new File(lockerDir, fileName);
                    FileOutputStream lockerFos = new FileOutputStream(lockerFile);
                    document.writeTo(lockerFos);
                    lockerFos.close();
                } catch (Exception e) {
                    e.printStackTrace(); // Fail silently for locker backup
                }

                // Trigger Share
                if (pdfUri != null) {
                    android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, pdfUri);
                    shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Report"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        document.close();
    }
}
