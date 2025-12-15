package com.example.enterprisehub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class IncentiveActivity extends AppCompatActivity {

    private EditText etTarget, etAchieved;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incentive);

        etTarget = findViewById(R.id.et_target);
        etAchieved = findViewById(R.id.et_achieved);
        tvResult = findViewById(R.id.tv_result);
        Button btnCalculate = findViewById(R.id.btn_calculate);

        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateIncentive();
            }
        });
    }

    private void calculateIncentive() {
        String targetStr = etTarget.getText().toString().trim();
        String achievedStr = etAchieved.getText().toString().trim();

        if (targetStr.isEmpty() || achievedStr.isEmpty()) {
            Toast.makeText(this, "Please enter both amounts", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double target = Double.parseDouble(targetStr);
            double achieved = Double.parseDouble(achievedStr);

            if (target == 0) {
                tvResult.setText("Target cannot be zero");
                return;
            }

            double percentage = (achieved / target) * 100;
            // Placeholder logic:
            // < 80% = 0 incentive
            // 80-100% = 5% of achieved
            // > 100% = 10% of achieved

            double incentive = 0;
            if (percentage >= 100) {
                incentive = achieved * 0.10;
            } else if (percentage >= 80) {
                incentive = achieved * 0.05;
            }

            // Use Rupee Symbol
            String result = String.format("Achievement: %.1f%%\nIncentive: \u20B9%.2f", percentage, incentive);
            tvResult.setText(result);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }
}
