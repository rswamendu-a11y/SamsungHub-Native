package com.example.enterprisehub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome, tvOutletName, tvProverb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWelcome = findViewById(R.id.tv_welcome);
        tvOutletName = findViewById(R.id.tv_outlet_name);
        tvProverb = findViewById(R.id.tv_proverb);

        setupWisdom();

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

    private void setupWisdom() {
        String[] proverbs = {
            "Sow the seeds of relationship, reap the harvest of sales.",
            "Price is what you pay. Value is what you get.",
            "Don't find customers for your products, find products for your customers.",
            "Quality means doing it right when no one is looking."
        };

        int index = new Random().nextInt(proverbs.length);
        tvProverb.setText("\"" + proverbs[index] + "\"");
    }
}
