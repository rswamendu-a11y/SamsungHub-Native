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
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfExport {

    public static void generateMatrixPdf(Context context, List<SaleItem> dataToExport) {
        PdfDocument document = new PdfDocument();
        // A4 Landscape for Matrix
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(842, 595, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(10);

        int y = 40;
        int x = 20;

        // Title
        paint.setTextSize(16);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("Sales Matrix Report (Vol | Val)", x, y, paint);
        y += 30;
        paint.setTextSize(9); // Compact font
        paint.setTypeface(Typeface.DEFAULT);

        // 1. Prepare Data
        List<String> dates = new ArrayList<>();
        // Brands fixed order + Others
        String[] brandOrder = {"Samsung", "Apple", "Realme", "Xiaomi", "Oppo", "Vivo", "Motorola", "Others"};
        List<String> brands = new ArrayList<>();
        for (String b : brandOrder) brands.add(b);

        // Maps for Pivot
        // Map<Date, Map<Brand, int[]{Volume, Value}>>
        Map<String, Map<String, double[]>> matrix = new HashMap<>();
        // Totals
        Map<String, double[]> dateTotals = new HashMap<>(); // Date -> [Vol, Val]
        Map<String, double[]> brandTotals = new HashMap<>(); // Brand -> [Vol, Val]
        double grandTotalVol = 0;
        double grandTotalVal = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (SaleItem item : dataToExport) {
            String dateStr = sdf.format(new Date(item.getTimestamp()));
            if (!dates.contains(dateStr)) dates.add(dateStr);

            String brand = item.getBrand();
            if (!brands.contains(brand)) brand = "Others"; // Normalize unknown brands to Others for matrix safety

            // Update Matrix Cell
            Map<String, double[]> row = matrix.getOrDefault(dateStr, new HashMap<>());
            double[] cell = row.getOrDefault(brand, new double[]{0, 0});
            cell[0] += item.getQuantity();
            cell[1] += (item.getQuantity() * item.getPrice());
            row.put(brand, cell);
            matrix.put(dateStr, row);

            // Update Date Total
            double[] dTotal = dateTotals.getOrDefault(dateStr, new double[]{0, 0});
            dTotal[0] += item.getQuantity();
            dTotal[1] += (item.getQuantity() * item.getPrice());
            dateTotals.put(dateStr, dTotal);

            // Update Brand Total
            double[] bTotal = brandTotals.getOrDefault(brand, new double[]{0, 0});
            bTotal[0] += item.getQuantity();
            bTotal[1] += (item.getQuantity() * item.getPrice());
            brandTotals.put(brand, bTotal);

            // Grand Total
            grandTotalVol += item.getQuantity();
            grandTotalVal += (item.getQuantity() * item.getPrice());
        }

        Collections.sort(dates, Collections.reverseOrder());

        // 2. Draw Header
        int dateColWidth = 60;
        int brandColWidth = 75; // Wider for "Vol | Val"

        canvas.drawText("Date", x, y, paint);
        int currentX = x + dateColWidth;

        paint.setTypeface(Typeface.DEFAULT_BOLD);
        for (String brand : brands) {
            canvas.drawText(brand, currentX, y, paint);
            currentX += brandColWidth;
        }
        canvas.drawText("DAILY TOTAL", currentX, y, paint);
        paint.setTypeface(Typeface.DEFAULT);

        y += 10;
        canvas.drawLine(x, y, currentX + brandColWidth, y, paint);
        y += 15;

        // 3. Draw Rows
        for (String date : dates) {
            canvas.drawText(date, x, y, paint);
            currentX = x + dateColWidth;
            Map<String, double[]> rowData = matrix.get(date);

            for (String brand : brands) {
                double[] cell = rowData.getOrDefault(brand, new double[]{0, 0});
                if (cell[0] > 0) {
                    String cellText = String.format(Locale.US, "%.0f | \u20B9%.0fk", cell[0], cell[1]/1000);
                    canvas.drawText(cellText, currentX, y, paint);
                } else {
                     canvas.drawText("-", currentX, y, paint);
                }
                currentX += brandColWidth;
            }

            // Daily Total
            double[] dTotal = dateTotals.get(date);
            if (dTotal != null) {
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                String totalText = String.format(Locale.US, "%.0f | \u20B9%.0fk", dTotal[0], dTotal[1]/1000);
                canvas.drawText(totalText, currentX, y, paint);
                paint.setTypeface(Typeface.DEFAULT);
            }

            y += 20;

            if (y > 550) { // Pagination
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;
                // Skip header redraw for simplicity in v1
            }
        }

        // 4. Draw Brand Totals (Bottom Row)
        y += 5;
        canvas.drawLine(x, y, currentX + brandColWidth, y, paint);
        y += 15;

        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("GRAND TOTAL", x, y, paint);
        currentX = x + dateColWidth;

        for (String brand : brands) {
            double[] bTotal = brandTotals.getOrDefault(brand, new double[]{0, 0});
            String cellText = String.format(Locale.US, "%.0f | \u20B9%.0fk", bTotal[0], bTotal[1]/1000);
            canvas.drawText(cellText, currentX, y, paint);
            currentX += brandColWidth;
        }

        // Bottom Right Grand Total
        String grandText = String.format(Locale.US, "%.0f | \u20B9%.0fk", grandTotalVol, grandTotalVal/1000);
        canvas.drawText(grandText, currentX, y, paint);

        y += 30;
        paint.setTextSize(12);
        canvas.drawText("Summary: Total Vol: " + (int)grandTotalVol + " | Total Value: \u20B9" + String.format("%.2f", grandTotalVal), x, y, paint);

        document.finishPage(page);
        savePdfToMediaStore(context, document);
    }

    private static void savePdfToMediaStore(Context context, PdfDocument document) {
        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Sales_Matrix_" + System.currentTimeMillis() + ".pdf");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException("Failed to create new MediaStore record.");
                fos = context.getContentResolver().openOutputStream(uri);
            } else {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                File file = new File(path, "Sales_Matrix_" + System.currentTimeMillis() + ".pdf");
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                document.writeTo(fos);
                fos.close();
                Toast.makeText(context, "Matrix Report Exported to Downloads", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        document.close();
    }
}
