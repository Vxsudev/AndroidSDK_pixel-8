package com.vxsudev.androidsdk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button btnGenerate;
    private Button btnApplyEnv;
    private TextView tvSource;
    private LinearLayout chartContainer;
    private Spinner envSpinner;

    private CSVDataLoader csvDataLoader;
    private DataVisualizer dataVisualizer;
    private FirestoreManager firestoreManager;
    private GoogleFitManager googleFitManager;
    private CloudStorageManager cloudStorageManager;

    // Environment configurations
    private Map<String, String> environmentConfigs = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGenerate = findViewById(R.id.btnGenerateData);
        btnApplyEnv = findViewById(R.id.btnApplyEnv);
        tvSource = findViewById(R.id.tvSource);
        chartContainer = findViewById(R.id.chartContainer);
        envSpinner = findViewById(R.id.envSpinner);

        csvDataLoader = new CSVDataLoader();
        dataVisualizer = new DataVisualizer();
        firestoreManager = new FirestoreManager();
        googleFitManager = new GoogleFitManager(this);

        setupEnvironmentSelector();
        btnGenerate.setOnClickListener(v -> handleGenerateClick());
    }

    /**
     * Setup environment selector with available environments
     */
    private void setupEnvironmentSelector() {
        // Map environment names to asset filenames
        // Use .template suffix for files that are templates in the repo
        environmentConfigs.put("Default", null); // null = use default FirebaseApp
        environmentConfigs.put("Dev", "google-services-dev.json");
        environmentConfigs.put("Staging", "google-services-staging.json");

        // Setup spinner adapter
        List<String> envNames = new ArrayList<>(environmentConfigs.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                envNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        envSpinner.setAdapter(adapter);

        // Wire apply button
        btnApplyEnv.setOnClickListener(v -> handleEnvironmentSwitch());

        Log.d(TAG, "✅ Environment selector initialized with " + envNames.size() + " environments");
    }

    /**
     * Handle environment switch when Apply button is clicked
     */
    private void handleEnvironmentSwitch() {
        String selectedEnv = (String) envSpinner.getSelectedItem();
        String assetFilename = environmentConfigs.get(selectedEnv);

        Log.d(TAG, "🔄 Switching to environment: " + selectedEnv + " (config: " + assetFilename + ")");

        try {
            FirebaseApp app = CloudStorageManager.getFirebaseAppForEnvironment(this, assetFilename);
            
            if (app != null) {
                // Successfully initialized or retrieved FirebaseApp
                cloudStorageManager = new CloudStorageManager(this, assetFilename);
                
                String projectId = app.getOptions().getProjectId();
                String storageBucket = app.getOptions().getStorageBucket();
                
                String message = "✅ Switched to " + selectedEnv + "\n" +
                        "ProjectId: " + projectId + "\n" +
                        "Bucket: " + storageBucket;
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                Log.d(TAG, message);
            } else {
                // Failed to initialize - show user-friendly error
                String errorMsg = "⚠️ Config file not found for " + selectedEnv + 
                        "\nAdd " + assetFilename + " to assets/ to use this environment";
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                Log.w(TAG, errorMsg);
            }

        } catch (Exception e) {
            String errorMsg = "❌ Failed to switch to " + selectedEnv + ": " + e.getMessage();
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Environment switch failed", e);
        }
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
