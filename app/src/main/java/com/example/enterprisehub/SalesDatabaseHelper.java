package com.example.enterprisehub;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class SalesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sales.db";
    private static final int DATABASE_VERSION = 2; // Incrementing version just in case

    public static final String TABLE_SALES = "sales";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_BRAND = "brand";
    public static final String COLUMN_MODEL = "model";
    public static final String COLUMN_VARIANT = "variant";
    public static final String COLUMN_QUANTITY = "quantity";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_SEGMENT = "segment";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public SalesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SALES_TABLE = "CREATE TABLE " + TABLE_SALES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_BRAND + " TEXT,"
                + COLUMN_MODEL + " TEXT,"
                + COLUMN_VARIANT + " TEXT,"
                + COLUMN_QUANTITY + " INTEGER,"
                + COLUMN_PRICE + " REAL,"
                + COLUMN_SEGMENT + " TEXT,"
                + COLUMN_TIMESTAMP + " INTEGER" + ")";
        db.execSQL(CREATE_SALES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SALES);
        onCreate(db);
    }

    public void addSale(SaleItem sale) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BRAND, sale.getBrand());
        values.put(COLUMN_MODEL, sale.getModel());
        values.put(COLUMN_VARIANT, sale.getVariant());
        values.put(COLUMN_QUANTITY, sale.getQuantity());
        values.put(COLUMN_PRICE, sale.getPrice());
        values.put(COLUMN_SEGMENT, sale.getSegment());
        values.put(COLUMN_TIMESTAMP, sale.getTimestamp());

        db.insert(TABLE_SALES, null, values);
        db.close();
    }

    public List<SaleItem> getAllSales() {
        List<SaleItem> saleList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SALES + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String brand = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BRAND));
                String model = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MODEL));
                String variant = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VARIANT));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE));
                String segment = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SEGMENT));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));

                SaleItem sale = new SaleItem(id, brand, model, variant, qty, price, segment, timestamp);
                saleList.add(sale);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return saleList;
    }

    public List<SaleItem> getSalesByDateRange(long startTime, long endTime) {
        List<SaleItem> saleList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SALES +
                             " WHERE " + COLUMN_TIMESTAMP + " >= ? AND " + COLUMN_TIMESTAMP + " <= ?" +
                             " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(startTime), String.valueOf(endTime)});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String brand = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BRAND));
                String model = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MODEL));
                String variant = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VARIANT));
                int qty = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE));
                String segment = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SEGMENT));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));

                SaleItem sale = new SaleItem(id, brand, model, variant, qty, price, segment, timestamp);
                saleList.add(sale);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return saleList;
    }

    public void updateSale(SaleItem sale) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BRAND, sale.getBrand());
        values.put(COLUMN_MODEL, sale.getModel());
        values.put(COLUMN_VARIANT, sale.getVariant());
        values.put(COLUMN_QUANTITY, sale.getQuantity());
        values.put(COLUMN_PRICE, sale.getPrice());
        values.put(COLUMN_SEGMENT, sale.getSegment());
        values.put(COLUMN_TIMESTAMP, sale.getTimestamp());

        db.update(TABLE_SALES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(sale.getId())});
        db.close();
    }

    public void deleteSale(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SALES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
