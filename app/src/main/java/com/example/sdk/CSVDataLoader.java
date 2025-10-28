package com.example.sdk;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads smartwatch health data (HeartRate, SpO2, Temperature, Steps)
 * from a CSV file stored in assets/.
 */
public class CSVDataLoader {

    private static final String TAG = "CSVDataLoader";
    private final Context context;

    // ✅ Constructor expects a Context
    public CSVDataLoader(Context context) {
        this.context = context;
    }

    /**
     * Loads smartwatch data from the given CSV file in assets folder.
     *
     * @param fileName The CSV file name (e.g., "smartwatch_data.csv")
     * @return List of SmartWatchData objects parsed from CSV
     */
    public List<SmartWatchData> loadFromCSV(String fileName) {
        List<SmartWatchData> dataList = new ArrayList<>();
        AssetManager assetManager = context.getAssets();

        try (InputStream inputStream = assetManager.open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) { // skip header line
                    isHeader = false;
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length < 4) continue;

                try {
                    int heartRate = Integer.parseInt(tokens[0].trim());
                    int spo2 = Integer.parseInt(tokens[1].trim());
                    double temperature = Double.parseDouble(tokens[2].trim());
                    int steps = Integer.parseInt(tokens[3].trim());

                    dataList.add(new SmartWatchData(heartRate, spo2, temperature, steps));

                } catch (NumberFormatException e) {
                    Log.w(TAG, "⚠️ Skipping invalid row: " + line);
                }
            }

            Log.d(TAG, "✅ Loaded " + dataList.size() + " entries from CSV");
        } catch (IOException e) {
            Log.e(TAG, "❌ Error reading CSV file: " + fileName, e);
        }

        return dataList;
    }
}
