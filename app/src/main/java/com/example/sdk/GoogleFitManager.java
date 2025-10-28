package com.example.sdk;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.HealthDataTypes;
import com.google.android.gms.fitness.data.HealthFields;

public class GoogleFitManager {

    private static final String TAG = "GoogleFitManager";

    public interface FitDataCallback {
        void onDataReceived(SmartWatchData data);
        void onError(Exception e);
    }

    public static void loadLatestHealthDataAsync(Context context, FitDataCallback callback) {
        SmartWatchData data = new SmartWatchData();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);

        if (account == null) {
            callback.onError(new Exception("No Google account signed in"));
            return;
        }

        // Heart Rate
        Fitness.getHistoryClient(context, account)
                .readDailyTotal(DataType.TYPE_HEART_RATE_BPM)
                .addOnSuccessListener(dataSet -> {
                    if (!dataSet.isEmpty()) {
                        int hr = Math.round(dataSet.getDataPoints().get(0)
                                .getValue(Field.FIELD_AVERAGE).asFloat());
                        data.setHeartRate(hr);
                        Log.d(TAG, "❤️ HR: " + hr);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "HR fetch failed", e));

        // SpO₂
        Fitness.getHistoryClient(context, account)
                .readDailyTotal(HealthDataTypes.TYPE_OXYGEN_SATURATION)
                .addOnSuccessListener(dataSet -> {
                    if (!dataSet.isEmpty()) {
                        int spo2 = Math.round(dataSet.getDataPoints().get(0)
                                .getValue(HealthFields.FIELD_OXYGEN_SATURATION).asFloat());
                        data.setSpo2(spo2);
                        Log.d(TAG, "🩸 SpO2: " + spo2);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "SpO2 fetch failed", e));

        // Temperature
        Fitness.getHistoryClient(context, account)
                .readDailyTotal(HealthDataTypes.TYPE_BODY_TEMPERATURE)
                .addOnSuccessListener(dataSet -> {
                    if (!dataSet.isEmpty()) {
                        double temp = dataSet.getDataPoints().get(0)
                                .getValue(HealthFields.FIELD_BODY_TEMPERATURE).asFloat();
                        data.setTemperature(temp);
                        Log.d(TAG, "🌡 Temp: " + temp);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Temp fetch failed", e));

        // Steps
        Fitness.getHistoryClient(context, account)
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(dataSet -> {
                    if (!dataSet.isEmpty()) {
                        int steps = dataSet.getDataPoints().get(0)
                                .getValue(Field.FIELD_STEPS).asInt();
                        data.setSteps(steps);
                        Log.d(TAG, "👣 Steps: " + steps);
                    }

                    // ✅ Return after all metrics collected
                    callback.onDataReceived(data);
                })
                .addOnFailureListener(callback::onError);
    }
}
