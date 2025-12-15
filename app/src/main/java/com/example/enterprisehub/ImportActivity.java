package com.example.enterprisehub;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ImportActivity extends AppCompatActivity {

    private static final int PICK_EXCEL_FILE = 101;
    private TextView tvStatus;
    private SalesDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");

        dbHelper = new SalesDatabaseHelper(this);
        tvStatus = findViewById(R.id.tv_status);
        Button btnSelectFile = findViewById(R.id.btn_select_file);

        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); // .xlsx
        startActivityForResult(intent, PICK_EXCEL_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_EXCEL_FILE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importExcelData(uri);
            }
        }
    }

    private void importExcelData(Uri uri) {
        tvStatus.setText("Importing... Please wait.");
        new Thread(() -> {
            int successCount = 0;
            int failCount = 0;
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                Workbook workbook = new XSSFWorkbook(inputStream);
                Sheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();

                // Skip Header
                if (rowIterator.hasNext()) rowIterator.next();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    try {
                        // Expected Format: Brand | Model | Variant | Qty | Price
                        // Optional: Date? If no date, use now.
                        // Let's look for columns.
                        // 0: ID (Skip)
                        // 1: Brand
                        // 2: Model
                        // 3: Variant
                        // 4: Qty
                        // 5: Price
                        // 6: Segment (Skip, calculate)
                        // 7: Date (String yyyy-MM-dd HH:mm)

                        String brand = getCellValue(row.getCell(1));
                        String model = getCellValue(row.getCell(2));
                        String variant = getCellValue(row.getCell(3));
                        int qty = (int) Double.parseDouble(getCellValue(row.getCell(4)));
                        double price = Double.parseDouble(getCellValue(row.getCell(5)));

                        long timestamp = System.currentTimeMillis();
                        Cell dateCell = row.getCell(7);
                        if (dateCell != null) {
                            if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                                timestamp = dateCell.getDateCellValue().getTime();
                            } else {
                                String dateStr = getCellValue(dateCell);
                                if (!dateStr.isEmpty()) {
                                    try {
                                        Date date = sdf.parse(dateStr);
                                        if (date != null) timestamp = date.getTime();
                                    } catch (Exception e) {
                                        // Ignore parse error, use current time
                                    }
                                }
                            }
                        }

                        if (!brand.isEmpty() && !model.isEmpty()) {
                            String segment = calculateSegment(price);
                            SaleItem item = new SaleItem(0, brand, model, variant, qty, price, segment, timestamp);
                            dbHelper.addSale(item);
                            successCount++;
                        }
                    } catch (Exception e) {
                        failCount++;
                        Log.e("Import", "Row error: " + e.getMessage());
                    }
                }
                workbook.close();
                inputStream.close();

                int finalSuccess = successCount;
                int finalFail = failCount;
                runOnUiThread(() -> {
                    tvStatus.setText("Import Complete.\nSuccess: " + finalSuccess + "\nFailed: " + finalFail);
                    Toast.makeText(ImportActivity.this, "Import Complete", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvStatus.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
        return "";
    }

    private String calculateSegment(double price) {
        long lower = (long) (Math.floor(price / 10000.0) * 10000);
        long upper = lower + 10000;
        return (lower / 1000) + "k-" + (upper / 1000) + "k";
    }
}
