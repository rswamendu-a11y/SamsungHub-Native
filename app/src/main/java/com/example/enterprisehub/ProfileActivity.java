package com.example.enterprisehub;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "EnterpriseHubPrefs";
    public static final String KEY_OWNER = "owner_name";
    public static final String KEY_OUTLET = "outlet_name";

    private EditText etOwner, etOutlet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etOwner = findViewById(R.id.et_owner);
        etOutlet = findViewById(R.id.et_outlet);
        Button btnSave = findViewById(R.id.btn_save);
        Button btnBackup = findViewById(R.id.btn_backup);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        etOwner.setText(prefs.getString(KEY_OWNER, ""));
        etOutlet.setText(prefs.getString(KEY_OUTLET, ""));

        btnSave.setOnClickListener(v -> {
            prefs.edit()
                .putString(KEY_OWNER, etOwner.getText().toString().trim())
                .putString(KEY_OUTLET, etOutlet.getText().toString().trim())
                .apply();
            Toast.makeText(this, "Profile Saved", Toast.LENGTH_SHORT).show();
            finish(); // Go back to Settings or Main
        });

        btnBackup.setOnClickListener(v -> backupDatabase());
    }

    private void backupDatabase() {
        String dbName = "sales.db";
        File dbFile = getDatabasePath(dbName);

        if (!dbFile.exists()) {
            Toast.makeText(this, "No database found to backup", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "sales_backup_" + System.currentTimeMillis() + ".db");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream"); // Generic binary
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException("Failed to create new MediaStore record.");
                fos = getContentResolver().openOutputStream(uri);
            } else {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                File file = new File(path, "sales_backup_" + System.currentTimeMillis() + ".db");
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                FileInputStream fis = new FileInputStream(dbFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.flush();
                fos.close();
                fis.close();
                Toast.makeText(this, "Database Backup Successful", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Backup Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
