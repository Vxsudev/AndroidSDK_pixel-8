package com.example.sdk;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * Main activity for Smartwatch Health Monitor SDK demo (Phase-8)
 * - batch upload CSV -> Firestore
 * - realtime listener to render charts automatically when Firestore updates
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // UI
    private Button btnApplyEnv;
    private Button btnGenerate;
    private LinearLayout chartContainer;
    private Spinner envSpinner;
    private TextView tvSource;

    // Helpers
    private CSVDataLoader csvDataLoader;
    private DataVisualizer dataVisualizer;
    private FirestoreManager firestoreManager;

    // Flag: are we listening?
    private boolean realtimeStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // ensure IDs match layout

        // bind UI
        btnApplyEnv = findViewById(R.id.btnApplyEnv);
        btnGenerate = findViewById(R.id.btnGenerate);
        chartContainer = findViewById(R.id.chartContainer);
        envSpinner = findViewById(R.id.envSpinner);
        tvSource = findViewById(R.id.tvSource);

        // Log to confirm bind
        Log.d(TAG, "UI bound: btnGenerate=" + (btnGenerate != null) + " chartContainer=" + (chartContainer != null));

        // init utils
        csvDataLoader = new CSVDataLoader(this);    // ensure CSVDataLoader has loadFromCSV(Context,String)
        dataVisualizer = new DataVisualizer();
        firestoreManager = new FirestoreManager();

        // Apply env button (existing behaviour may be environment switch)
        btnApplyEnv.setOnClickListener(v -> {
            // your existing env switching logic likely resides in EnvironmentSwitcher
            // For now show a toast and log
            Toast.makeText(this, "Apply Env clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Apply Env clicked");
            // If you use EnvironmentSwitcher, call it here (example)
            // environmentSwitcher.applySelected(envSpinner.getSelectedItemPosition());
        });

        // Generate Watch Data -> load CSV, batch upload to Firestore, display
        btnGenerate.setOnClickListener(v -> {
            Log.d(TAG, "▶️ Generate Watch Data clicked");

            // 1) load CSV from assets folder
            List<SmartWatchData> dataList = null;
            try {
                // CSVDataLoader signature expected: loadFromCSV(Context context, String assetFileName)
                dataList = csvDataLoader.loadFromCSV("smartwatch_data.csv");

                Log.d(TAG, "CSV load returned: " + (dataList == null ? "null" : dataList.size()));
            } catch (Exception e) {
                Log.e(TAG, "❌ CSV load failed", e);
                Toast.makeText(this, "CSV load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (dataList == null || dataList.isEmpty()) {
                Toast.makeText(this, "⚠️ No data in CSV file", Toast.LENGTH_SHORT).show();
                tvSource.setText("CSV: empty or missing.");
                return;
            }

            tvSource.setText("Source: CSV (" + dataList.size() + ")");

            // 2) render charts immediately from CSV (local preview)
            chartContainer.removeAllViews();
            dataVisualizer.renderCharts(this, chartContainer, dataList);
            Toast.makeText(this, "✅ CSV Data Visualized", Toast.LENGTH_SHORT).show();

            // 3) upload batch to Firestore (Phase-8)
            firestoreManager.uploadBatch(dataList, new FirestoreManager.UploadCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "✅ Firestore batch upload success");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Uploaded CSV -> Firestore", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "❌ Firestore batch upload failed", e);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });

            // 4) start realtime listener (only once)
            if (!realtimeStarted) {
                startRealtimeListener();
            }
        });

        // Optionally: fetch existing Firestore records on start and render
        fetchAndDisplayFirestoreData();
    }

    private void startRealtimeListener() {
        firestoreManager.listenToCollection(new FirestoreManager.RealTimeListener() {
            @Override
            public void onUpdate(List<SmartWatchData> dataList) {
                Log.d(TAG, "🔔 Realtime onUpdate size=" + (dataList == null ? 0 : dataList.size()));
                runOnUiThread(() -> {
                    chartContainer.removeAllViews();
                    if (dataList != null && !dataList.isEmpty()) {
                        dataVisualizer.renderCharts(MainActivity.this, chartContainer, dataList);
                        tvSource.setText("Source: Firestore (" + dataList.size() + ")");
                    } else {
                        tvSource.setText("Source: Firestore (empty)");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Realtime listener error", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Realtime error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });

        realtimeStarted = true;
        Toast.makeText(this, "Realtime sync started", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Realtime listener registered");
    }

    private void fetchAndDisplayFirestoreData() {
        firestoreManager.fetchAllData(new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(final List<SmartWatchData> dataList) {
                Log.d(TAG, "✅ Firestore data fetched: " + (dataList == null ? 0 : dataList.size()));
                runOnUiThread(() -> {
                    chartContainer.removeAllViews();
                    if (dataList != null && !dataList.isEmpty()) {
                        dataVisualizer.renderCharts(MainActivity.this, chartContainer, dataList);
                        tvSource.setText("Source: Firestore (" + dataList.size() + ")");
                    } else {
                        tvSource.setText("Source: Firestore (empty)");
                    }
                });
                // ensure realtime is running so future updates show up automatically
                if (!realtimeStarted) {
                    startRealtimeListener();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Firestore fetch failed", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Firestore fetch failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // stop realtime subscription to avoid leaks
        firestoreManager.stopRealtime();
    }
}
