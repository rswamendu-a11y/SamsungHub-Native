package com.example.enterprisehub;

import android.content.ContentValues;
import android.content.Context;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelExport {

    public static void generateMatrixExcel(Context context, List<SaleItem> dataToExport) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Matrix Report");

        // 1. Prepare Data (Same logic as PdfExport)
        Set<String> brandSet = new HashSet<>();
        for (SaleItem item : dataToExport) {
            brandSet.add(item.getBrand());
        }
        List<String> uniqueBrands = new ArrayList<>(brandSet);
        Collections.sort(uniqueBrands);

        Map<String, List<SaleItem>> groupedData = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (SaleItem item : dataToExport) {
            String dateKey = sdf.format(new Date(item.getTimestamp()));
            if (!groupedData.containsKey(dateKey)) {
                groupedData.put(dateKey, new ArrayList<>());
            }
            groupedData.get(dateKey).add(item);
        }

        // 2. Create Header Row
        Row headerRow = sheet.createRow(0);
        int colIdx = 0;
        headerRow.createCell(colIdx++).setCellValue("Date");

        for (String brand : uniqueBrands) {
            headerRow.createCell(colIdx++).setCellValue(brand + " Qty");
            headerRow.createCell(colIdx++).setCellValue(brand + " Val");
        }

        headerRow.createCell(colIdx++).setCellValue("Total Qty");
        headerRow.createCell(colIdx++).setCellValue("Total Val");

        // 3. Fill Data Rows
        int rowNum = 1;
        Map<String, Integer> totalBrandQty = new HashMap<>();
        Map<String, Double> totalBrandVal = new HashMap<>();
        int grandTotalQty = 0;
        double grandTotalVal = 0;

        for (Map.Entry<String, List<SaleItem>> entry : groupedData.entrySet()) {
            String date = entry.getKey();
            List<SaleItem> items = entry.getValue();

            Row row = sheet.createRow(rowNum++);
            colIdx = 0;
            row.createCell(colIdx++).setCellValue(date);

            Map<String, Integer> brandQty = new HashMap<>();
            Map<String, Double> brandVal = new HashMap<>();
            int dayTotalQty = 0;
            double dayTotalVal = 0;

            for (SaleItem item : items) {
                int q = item.getQuantity();
                double v = item.getPrice() * q;
                brandQty.put(item.getBrand(), brandQty.getOrDefault(item.getBrand(), 0) + q);
                brandVal.put(item.getBrand(), brandVal.getOrDefault(item.getBrand(), 0.0) + v);

                dayTotalQty += q;
                dayTotalVal += v;

                totalBrandQty.put(item.getBrand(), totalBrandQty.getOrDefault(item.getBrand(), 0) + q);
                totalBrandVal.put(item.getBrand(), totalBrandVal.getOrDefault(item.getBrand(), 0.0) + v);
                grandTotalQty += q;
                grandTotalVal += v;
            }

            for (String brand : uniqueBrands) {
                row.createCell(colIdx++).setCellValue(brandQty.getOrDefault(brand, 0));
                row.createCell(colIdx++).setCellValue(brandVal.getOrDefault(brand, 0.0));
            }

            row.createCell(colIdx++).setCellValue(dayTotalQty);
            row.createCell(colIdx++).setCellValue(dayTotalVal);
        }

        // 4. Totals Row
        Row totalRow = sheet.createRow(rowNum);
        colIdx = 0;
        totalRow.createCell(colIdx++).setCellValue("TOTAL");

        for (String brand : uniqueBrands) {
            totalRow.createCell(colIdx++).setCellValue(totalBrandQty.getOrDefault(brand, 0));
            totalRow.createCell(colIdx++).setCellValue(totalBrandVal.getOrDefault(brand, 0.0));
        }
        totalRow.createCell(colIdx++).setCellValue(grandTotalQty);
        totalRow.createCell(colIdx++).setCellValue(grandTotalVal);

        // 5. Save & Share
        saveExcelToMediaStore(context, workbook);
    }

    public static void exportMasterBackup(Context context, List<SaleItem> dataToExport) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Raw Data");

        // 1. Header
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Brand", "Model", "Variant", "Quantity", "Price", "Segment", "Date"};
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        // 2. Data
        int rowNum = 1;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (SaleItem item : dataToExport) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getId());
            row.createCell(1).setCellValue(item.getBrand());
            row.createCell(2).setCellValue(item.getModel());
            row.createCell(3).setCellValue(item.getVariant());
            row.createCell(4).setCellValue(item.getQuantity());
            row.createCell(5).setCellValue(item.getPrice());
            row.createCell(6).setCellValue(item.getSegment());
            // Use Selected Date (Timestamp)
            row.createCell(7).setCellValue(sdf.format(new Date(item.getTimestamp())));
        }

        saveExcelToMediaStore(context, workbook, "Master_Backup_");
    }

    private static void saveExcelToMediaStore(Context context, Workbook workbook) {
        saveExcelToMediaStore(context, workbook, "Matrix_Excel_");
    }

    private static void saveExcelToMediaStore(Context context, Workbook workbook, String prefix) {
        Uri fileUri = null;
        try {
            OutputStream fos;
            String fileName = prefix + System.currentTimeMillis() + ".xlsx";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                fileUri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (fileUri == null) throw new IOException("Failed to create new MediaStore record.");
                fos = context.getContentResolver().openOutputStream(fileUri);
            } else {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                File file = new File(path, fileName);
                fileUri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                workbook.write(fos);
                fos.close();
                Toast.makeText(context, "Excel Matrix Exported", Toast.LENGTH_LONG).show();

                 // Trigger Share
                if (fileUri != null) {
                    android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
                    shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Excel"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving Excel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        try {
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
