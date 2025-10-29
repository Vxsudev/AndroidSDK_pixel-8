package com.example.sdk;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads smartwatch data from a CSV file in the assets folder.
 * CSV format: heartRate,spO2,temperature,steps
 */
public class CSVDataLoader {
    private static final String TAG = "CSVDataLoader";

    public List<SmartWatchData> loadFromCSV(Context context, String fileName) {
        List<SmartWatchData> dataList = new ArrayList<>();

        try {
            InputStream is = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");

                if (tokens.length < 4) continue;

                try {
                    int heartRate = Integer.parseInt(tokens[0].trim());
                    float spO2 = Float.parseFloat(tokens[1].trim());
                    float temperature = Float.parseFloat(tokens[2].trim());
                    int steps = Integer.parseInt(tokens[3].trim());

                    long timestamp = System.currentTimeMillis(); // auto timestamp
                    dataList.add(new SmartWatchData(timestamp, heartRate, spO2, temperature, steps));

                } catch (NumberFormatException e) {
                    Log.w(TAG, "⚠️ Skipping invalid row: " + line);
                }
            }

            reader.close();
            Log.d(TAG, "✅ Loaded " + dataList.size() + " entries from CSV");

        } catch (IOException e) {
            Log.e(TAG, "❌ Failed to load CSV: " + fileName, e);
        }

        return dataList;
    }
}
