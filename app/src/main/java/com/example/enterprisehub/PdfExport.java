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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfExport {

    public static void generateMatrixPdf(Context context, List<SaleItem> dataToExport) {
        PdfDocument document = new PdfDocument();
        // A4 Portrait for Ledger
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(10);

        int y = 40;
        int x = 20;

        // Get Profile Info
        SharedPreferences prefs = context.getSharedPreferences("EnterpriseHubPrefs", Context.MODE_PRIVATE);
        String owner = prefs.getString(ProfileActivity.KEY_OWNER, "Owner");
        String outlet = prefs.getString(ProfileActivity.KEY_OUTLET, "Samsung Hub");

        // Title Header
        paint.setTextSize(18);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("Sales Transaction Report", x, y, paint);
        y += 25;

        paint.setTextSize(12);
        canvas.drawText("Outlet: " + outlet, x, y, paint);
        y += 20;
        canvas.drawText("Owner: " + owner, x, y, paint);
        y += 30;

        paint.setTextSize(10);
        paint.setTypeface(Typeface.DEFAULT);

        // Sort by Date Ascending
        Collections.sort(dataToExport, Comparator.comparingLong(SaleItem::getTimestamp));

        // Draw Table Header
        // Cols: Date | Brand | Model | Variant | Qty | Price | Segment
        // Widths: 60 | 60 | 80 | 60 | 30 | 50 | 50
        int[] colX = {20, 80, 140, 220, 280, 310, 360};

        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("Date", colX[0], y, paint);
        canvas.drawText("Brand", colX[1], y, paint);
        canvas.drawText("Model", colX[2], y, paint);
        canvas.drawText("Variant", colX[3], y, paint);
        canvas.drawText("Qty", colX[4], y, paint);
        canvas.drawText("Price (\u20B9)", colX[5], y, paint); // Rupee
        canvas.drawText("Segment", colX[6], y, paint);

        y += 10;
        canvas.drawLine(x, y, 550, y, paint);
        y += 15;
        paint.setTypeface(Typeface.DEFAULT);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        int totalQty = 0;
        double totalValue = 0;

        for (SaleItem item : dataToExport) {
            canvas.drawText(sdf.format(new Date(item.getTimestamp())), colX[0], y, paint);
            canvas.drawText(item.getBrand(), colX[1], y, paint);
            canvas.drawText(item.getModel(), colX[2], y, paint);
            canvas.drawText(item.getVariant(), colX[3], y, paint);
            canvas.drawText(String.valueOf(item.getQuantity()), colX[4], y, paint);
            canvas.drawText(String.format(Locale.US, "%.0f", item.getPrice()), colX[5], y, paint);
            canvas.drawText(item.getSegment(), colX[6], y, paint);

            totalQty += item.getQuantity();
            totalValue += (item.getPrice() * item.getQuantity());

            y += 20;

            if (y > 800) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;
                // Skip header redraw for simplicity
            }
        }

        y += 10;
        canvas.drawLine(x, y, 550, y, paint);
        y += 20;

        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("Total Qty: " + totalQty, x, y, paint);
        canvas.drawText("Total Value: \u20B9" + String.format(Locale.US, "%.2f", totalValue), x + 150, y, paint);

        document.finishPage(page);
        savePdfToMediaStore(context, document);
    }

    private static void savePdfToMediaStore(Context context, PdfDocument document) {
        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Sales_Report_" + System.currentTimeMillis() + ".pdf");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException("Failed to create new MediaStore record.");
                fos = context.getContentResolver().openOutputStream(uri);
            } else {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                File file = new File(path, "Sales_Report_" + System.currentTimeMillis() + ".pdf");
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                document.writeTo(fos);
                fos.close();
                Toast.makeText(context, "Report Exported to Downloads", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        document.close();
    }
}
