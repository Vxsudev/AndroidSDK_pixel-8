package com.example.sdk;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.FileInputStream;

public class SecureStore {

    private static final String FILE_NAME = "secure_store.json";

    public static void saveSnapshot(Context context, JSONObject snapshot) {
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(snapshot.toString().getBytes());
            fos.close();
            Log.i("SecureStore", "✅ Snapshot saved securely");
        } catch (Exception e) {
            Log.e("SecureStore", "Error saving snapshot", e);
        }
    }

    public static JSONObject loadSnapshot(Context context) {
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            fis.close();
            return new JSONObject(new String(data));
        } catch (Exception e) {
            return null;
        }
    }

    public static void appendRecord(Context context, JSONObject record) {
        try {
            JSONArray history = loadAllSnapshots(context);
            history.put(record);
            saveAllSnapshots(context, history);
        } catch (Exception e) {
            Log.e("SecureStore", "Error appending snapshot", e);
        }
    }

    public static JSONArray loadAllSnapshots(Context context) {
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            fis.close();
            return new JSONArray(new String(data));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void saveAllSnapshots(Context context, JSONArray data) {
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(data.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            Log.e("SecureStore", "Error saving all snapshots", e);
        }
    }
}
