package com.example.sdk;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Handles loading alternate Firebase projects at runtime.
 */
public class FirebaseAppLoader {

    private static final String TAG = "FirebaseAppLoader";

    public static void initialize(Context context, String configFile) throws Exception {
        InputStream inputStream = context.getAssets().open(configFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) jsonBuilder.append(line);
        reader.close();

        JSONObject json = new JSONObject(jsonBuilder.toString());
        JSONObject projectInfo = json.getJSONObject("project_info");
        JSONObject client = json.getJSONArray("client").getJSONObject(0);
        JSONObject apiKeyObj = client.getJSONArray("api_key").getJSONObject(0);
        JSONObject clientInfo = client.getJSONObject("client_info");

        String projectId = projectInfo.getString("project_id");
        String apiKey = apiKeyObj.getString("current_key");
        String appId = clientInfo.getString("mobilesdk_app_id");
        String storageBucket = projectInfo.optString("storage_bucket", "");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApplicationId(appId)
                .setApiKey(apiKey)
                .setStorageBucket(storageBucket)
                .build();

        FirebaseApp existing = getAppByName(context, projectId);
        if (existing == null) {
            FirebaseApp app = FirebaseApp.initializeApp(context, options, projectId);
            Log.d(TAG, "✅ Firebase initialized for " + app.getName());
        } else {
            Log.d(TAG, "⚙️ Using existing Firebase app: " + existing.getName());
        }
    }

    private static FirebaseApp getAppByName(Context context, String name) {
        List<FirebaseApp> apps = FirebaseApp.getApps(context);
        for (FirebaseApp app : apps) {
            if (app.getName().equals(name)) {
                return app;
            }
        }
        return null;
    }
}
