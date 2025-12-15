package com.example.enterprisehub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome, tvOutletName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWelcome = findViewById(R.id.tv_welcome);
        tvOutletName = findViewById(R.id.tv_outlet_name);

        CardView cardTracker = findViewById(R.id.card_tracker);
        CardView cardHistory = findViewById(R.id.card_history);
        CardView cardSettings = findViewById(R.id.card_settings);
        Button btnExit = findViewById(R.id.btn_exit);

        cardTracker.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SalesTrackerActivity.class);
            startActivity(intent);
        });

        cardHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SalesTrackerActivity.class);
            startActivity(intent);
        });

        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnExit.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileInfo();
    }

    private void updateProfileInfo() {
        SharedPreferences prefs = getSharedPreferences("EnterpriseHubPrefs", MODE_PRIVATE);
        String owner = prefs.getString(ProfileActivity.KEY_OWNER, "");
        String outlet = prefs.getString(ProfileActivity.KEY_OUTLET, "Samsung Hub");

        if (!owner.isEmpty()) {
            tvWelcome.setText("Welcome, " + owner);
        } else {
            tvWelcome.setText("Welcome Back.");
        }

        tvOutletName.setText(outlet);
    }
}
