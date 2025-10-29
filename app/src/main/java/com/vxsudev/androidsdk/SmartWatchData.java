package com.vxsudev.androidsdk;

import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DataReadResponse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Represents smartwatch health metrics (heart rate, SpO₂, temperature, steps)
 * for a single timestamp — supports CSV + Google Fit hybrid data merging.
 */
public class SmartWatchData implements Serializable {

    private long timestamp;
    private int heartRate;
    private float spO2;
    private float temperature;
    private int steps;

    // Empty constructor for Firestore & CSV loader
    public SmartWatchData() {}

    public SmartWatchData(long timestamp, int heartRate, float spO2, float temperature, int steps) {
        this.timestamp = timestamp;
        this.heartRate = heartRate;
        this.spO2 = spO2;
        this.temperature = temperature;
        this.steps = steps;
    }

    // ---------------- Getters & Setters ----------------

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public float getSpO2() {
        return spO2;
    }

    public void setSpO2(float spO2) {
        this.spO2 = spO2;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    // ---------------- CSV Parsing Helper ----------------
    // Example CSV format:
    // timestamp,heartRate,spO2,temperature,steps
    public static SmartWatchData fromCSV(String[] columns) {
        try {
            long ts = Long.parseLong(columns[0]);
            int hr = Integer.parseInt(columns[1]);
            float spo2 = Float.parseFloat(columns[2]);
            float temp = Float.parseFloat(columns[3]);
            int steps = Integer.parseInt(columns[4]);
            return new SmartWatchData(ts, hr, spo2, temp, steps);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ---------------- Google Fit Data Parser ----------------
    /**
     * Converts a Google Fit DataReadResponse into a list of SmartWatchData objects.
     */
    public static List<SmartWatchData> fromFitResponse(DataReadResponse response) {
        List<SmartWatchData> list = new ArrayList<>();

        if (response == null) return list;

        for (DataSet dataSet : response.getDataSets()) {
            for (DataPoint dp : dataSet.getDataPoints()) {
                SmartWatchData data = new SmartWatchData();
                data.setTimestamp(dp.getEndTime(TimeUnit.MILLISECONDS));

                for (Field field : dp.getDataType().getFields()) {
                    String name = field.getName();
                    float value = dp.getValue(field).asFloat();

                    switch (name) {
                        case "heart_rate.bpm":
                            data.setHeartRate((int) value);
                            break;
                        case "oxygen_saturation":
                            data.setSpO2(value);
                            break;
                        case "body_temperature":
                            data.setTemperature(value);
                            break;
                        case "steps":
                            data.setSteps((int) value);
                            break;
                    }
                }
                list.add(data);
            }
        }
        return list;
    }

    // ---------------- Merge Utility ----------------
    /**
     * Combines CSV + Google Fit datasets and sorts them by timestamp.
     */
    public static List<SmartWatchData> mergeAndSort(List<SmartWatchData> csvList, List<SmartWatchData> fitList) {
        List<SmartWatchData> merged = new ArrayList<>();
        if (csvList != null) merged.addAll(csvList);
        if (fitList != null) merged.addAll(fitList);
        merged.sort(Comparator.comparingLong(SmartWatchData::getTimestamp));
        return merged;
    }

    // ---------------- Firestore Mapper ----------------
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("timestamp", timestamp);
        map.put("heartRate", heartRate);
        map.put("spO2", spO2);
        map.put("temperature", temperature);
        map.put("steps", steps);
        return map;
    }

    @Override
    public String toString() {
        return "SmartWatchData{" +
                "timestamp=" + timestamp +
                ", heartRate=" + heartRate +
                ", spO2=" + spO2 +
                ", temperature=" + temperature +
                ", steps=" + steps +
                '}';
    }
}
