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
        Button btnExit = findViewById(R.id.btn_exit);

        btnTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Tracker: Ask Jules to build this next!", Toast.LENGTH_SHORT).show();
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
