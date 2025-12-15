package com.example.enterprisehub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "EnterpriseHubPrefs";
    private static final String KEY_PIN = "user_pin";
    public static final String KEY_LAST_ACTIVE = "last_active";

    private EditText etPin;
    private TextView tvStatus;
    private boolean isSettingUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPin = findViewById(R.id.et_pin);
        tvStatus = findViewById(R.id.tv_status);
        Button btnConfirm = findViewById(R.id.btn_confirm);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedPin = prefs.getString(KEY_PIN, null);

        if (savedPin == null) {
            isSettingUp = true;
            tvStatus.setText("Create New PIN (4 Digits)");
        } else {
            isSettingUp = false;
            tvStatus.setText("Enter PIN");
        }

        btnConfirm.setOnClickListener(v -> {
            String input = etPin.getText().toString();
            if (input.length() != 4) {
                Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isSettingUp) {
                prefs.edit().putString(KEY_PIN, input).apply();
                Toast.makeText(this, "PIN Set!", Toast.LENGTH_SHORT).show();
                launchMain();
            } else {
                if (input.equals(savedPin)) {
                    launchMain();
                } else {
                    etPin.setText("");
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void launchMain() {
        // Reset Inactivity Timer
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
