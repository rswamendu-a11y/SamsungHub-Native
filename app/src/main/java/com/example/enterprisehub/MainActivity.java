package com.example.enterprisehub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CardView cardTracker = findViewById(R.id.card_tracker);
        CardView cardIncentive = findViewById(R.id.card_incentive);
        Button btnExit = findViewById(R.id.btn_exit);

        cardTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, SalesTrackerActivity.class);
                startActivity(intent);
            }
        });

        cardIncentive.setOnClickListener(new View.OnClickListener() {
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
