package com.example.enterprisehub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnTracker = findViewById(R.id.btn_tracker);
        Button btnIncentive = findViewById(R.id.btn_incentive);
        Button btnExit = findViewById(R.id.btn_exit);

        btnTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, SalesTrackerActivity.class);
                startActivity(intent);
            }
        });

        btnIncentive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, IncentiveActivity.class);
                startActivity(intent);
            }
        });

        // THIS IS THE NATIVE EXIT FUNCTION HTML CANNOT DO
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                System.exit(0);
            }
        });
    }
}
