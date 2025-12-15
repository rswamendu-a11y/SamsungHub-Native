package com.example.enterprisehub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button btnImport = findViewById(R.id.btn_import);
        Button btnPinAction = findViewById(R.id.btn_pin_action);
        Button btnProfile = findViewById(R.id.btn_profile);
        Switch switchDarkMode = findViewById(R.id.switch_dark_mode);

        btnImport.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ImportActivity.class));
        });

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
        });

        // PIN Logic
        SharedPreferences prefs = getSharedPreferences("EnterpriseHubPrefs", MODE_PRIVATE);
        boolean hasPin = prefs.getString("user_pin", null) != null;

        if (hasPin) {
            btnPinAction.setText("Remove PIN");
            btnPinAction.setOnClickListener(v -> {
                prefs.edit().remove("user_pin").apply();
                Toast.makeText(this, "PIN Removed", Toast.LENGTH_SHORT).show();
                recreate();
            });
        } else {
            btnPinAction.setText("Set PIN");
            btnPinAction.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                startActivity(intent);
            });
        }

        // Simple Dark Mode Logic
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }
}
