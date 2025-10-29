package com.example.sdk;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleFitManager {
    private static final String TAG = "GoogleFitManager";
    private final Activity activity;
    private GoogleSignInAccount account;
    private final FitnessOptions fitnessOptions;
    public static final int FIT_PERMISSIONS_REQUEST_CODE = 1001;

    public GoogleFitManager(Activity activity) {
        this.activity = activity;

        FitnessOptions.Builder builder = FitnessOptions.builder()
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ);

        // Safely add optional data types (not always available)
        try {
            Class<?> dt = Class.forName("com.google.android.gms.fitness.data.HealthDataTypes");
            builder.addDataType((DataType) dt.getField("TYPE_BODY_TEMPERATURE").get(null), FitnessOptions.ACCESS_READ);
            builder.addDataType((DataType) dt.getField("TYPE_OXYGEN_SATURATION").get(null), FitnessOptions.ACCESS_READ);
        } catch (Throwable ignored) {
            Log.w(TAG, "⚠️ Skipping optional Fit data types");
        }

        fitnessOptions = builder.build();
    }

    public boolean hasPermission() {
        account = GoogleSignIn.getAccountForExtension(activity, fitnessOptions);
        return GoogleSignIn.hasPermissions(account, fitnessOptions);
    }

    public void requestPermission() {
        account = GoogleSignIn.getAccountForExtension(activity, fitnessOptions);
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(activity, FIT_PERMISSIONS_REQUEST_CODE, account, fitnessOptions);
        }
    }

    public void handlePermissionResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIT_PERMISSIONS_REQUEST_CODE) {
            if (hasPermission()) {
                Toast.makeText(activity, "✅ Google Fit permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "❌ Permission denied — using CSV only", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void fetchFitData(FitDataCallback callback) {
        if (!hasPermission()) {
            callback.onFailure(new Exception("Permission not granted"));
            return;
        }

        long end = System.currentTimeMillis();
        long start = end - TimeUnit.HOURS.toMillis(24);

        DataReadRequest request = new DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_BPM)
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                .setTimeRange(start, end, TimeUnit.MILLISECONDS)
                .build();

        Fitness.getHistoryClient(activity, account)
                .readData(request)
                .addOnSuccessListener(response -> callback.onSuccess(parseFitResponse(response)))
                .addOnFailureListener(callback::onFailure);
    }

    private List<SmartWatchData> parseFitResponse(DataReadResponse response) {
        List<SmartWatchData> list = new ArrayList<>();
        for (DataSet set : response.getDataSets()) {
            for (DataPoint dp : set.getDataPoints()) {
                SmartWatchData d = new SmartWatchData();
                d.setTimestamp(dp.getEndTime(TimeUnit.MILLISECONDS));

                for (Field f : dp.getDataType().getFields()) {
                    float v = dp.getValue(f).asFloat();
                    switch (f.getName()) {
                        case "heart_rate.bpm": d.setHeartRate((int) v); break;
                        case "steps": d.setSteps((int) v); break;
                        case "oxygen_saturation": d.setSpO2(v); break;
                        case "body_temperature": d.setTemperature(v); break;
                    }
                }
                list.add(d);
            }
        }
        return list;
    }

    public interface FitDataCallback {
        void onSuccess(List<SmartWatchData> fitList);
        void onFailure(Exception e);
    }
}
