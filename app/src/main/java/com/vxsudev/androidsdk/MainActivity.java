package com.vxsudev.androidsdk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button btnGenerate;
    private TextView tvSource;
    private LinearLayout chartContainer;

    private CSVDataLoader csvDataLoader;
    private DataVisualizer dataVisualizer;
    private FirestoreManager firestoreManager;
    private GoogleFitManager googleFitManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGenerate = findViewById(R.id.btnGenerateData);
        tvSource = findViewById(R.id.tvSource);
        chartContainer = findViewById(R.id.chartContainer);

        csvDataLoader = new CSVDataLoader();
        dataVisualizer = new DataVisualizer();
        firestoreManager = new FirestoreManager();
        googleFitManager = new GoogleFitManager(this);

        btnGenerate.setOnClickListener(v -> handleGenerateClick());
    }

    private void handleGenerateClick() {
        Log.d(TAG, "▶️ Generate Watch Data clicked");

        // Load CSV first
        List<SmartWatchData> csvList = csvDataLoader.loadFromCSV(this, "smartwatch_data.csv");
        if (csvList == null || csvList.isEmpty()) {
            Toast.makeText(this, "⚠️ No data in CSV file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check Fit permission
        if (!googleFitManager.hasPermission()) {
            Log.d(TAG, "🔒 No Google Fit permission — showing CSV only.");
            tvSource.setText("📊 Source: CSV File (Fit not granted)");
            dataVisualizer.renderCharts(this, chartContainer, csvList);
            Toast.makeText(this, "Grant Google Fit permission to use live data", Toast.LENGTH_LONG).show();
            googleFitManager.requestPermission();
            return;
        }

        // Fetch G-Fit data
        tvSource.setText("📡 Fetching Google Fit data...");
        googleFitManager.fetchFitData(new GoogleFitManager.FitDataCallback() {
            @Override
            public void onSuccess(List<SmartWatchData> fitList) {
                List<SmartWatchData> merged = SmartWatchData.mergeAndSort(csvList, fitList);
                runOnUiThread(() -> {
                    tvSource.setText("📊 Source: CSV + Fit");
                    dataVisualizer.renderCharts(MainActivity.this, chartContainer, merged);
                    firestoreManager.uploadBatch(merged, new FirestoreManager.Callback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, "✅ Data uploaded", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(MainActivity.this, "❌ Upload failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Fit fetch failed", e);
                runOnUiThread(() -> {
                    tvSource.setText("📊 Source: CSV only (Fit failed)");
                    dataVisualizer.renderCharts(MainActivity.this, chartContainer, csvList);
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        googleFitManager.handlePermissionResult(requestCode, resultCode, data);
    }
}
