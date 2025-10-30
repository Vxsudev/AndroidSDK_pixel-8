package com.vxsudev.androidsdk;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CloudStorageManager
 * --------------------------------------------------------
 * Handles uploads and downloads of files (CSV, logs, JSON, etc.)
 * to Firebase Storage, matching the current Firestore environment.
 *
 * Works seamlessly with FirebaseAppLoader + FirestoreEnv.
 */
public class CloudStorageManager {

    private static final String TAG = "CloudStorageManager";

    private FirebaseStorage storage;
    private StorageReference storageRoot;

    // Cache for re-use between uploads
    private static final Map<String, FirebaseStorage> cachedStorages = new HashMap<>();
    private static final Map<String, FirebaseApp> cachedApps = new HashMap<>();

    /**
     * Initialize with default Firebase Storage (google-services.json)
     */
    public CloudStorageManager() {
        storage = FirebaseStorage.getInstance();
        storageRoot = storage.getReference();
        Log.d(TAG, "✅ Initialized default Firebase Storage");
    }

    /**
     * Initialize with specific Firebase config (from assets/)
     */
    public CloudStorageManager(Context context, String configFile) {
        FirebaseApp app = getFirebaseAppForEnvironment(context, configFile);
        if (app != null) {
            storage = FirebaseStorage.getInstance(app);
            storageRoot = storage.getReference();
            Log.d(TAG, "✅ CloudStorage initialized for: " + configFile + " (app: " + app.getName() + ")");
        } else {
            // Fall back to default
            storage = FirebaseStorage.getInstance();
            storageRoot = storage.getReference();
            Log.d(TAG, "⚠️ Falling back to default Firebase Storage");
        }
    }

    /**
     * Get or initialize a FirebaseApp for the given environment config file.
     * Returns null if initialization fails (file not found, parsing error, etc.)
     * 
     * @param context Android context
     * @param assetFilename Name of the JSON config file in assets/ (e.g., "google-services-dev.json")
     * @return FirebaseApp instance or null
     */
    public static FirebaseApp getFirebaseAppForEnvironment(Context context, String assetFilename) {
        if (assetFilename == null || assetFilename.isEmpty()) {
            Log.d(TAG, "No asset filename provided, using default FirebaseApp");
            return getDefaultFirebaseApp(context);
        }

        // Check cache first
        if (cachedApps.containsKey(assetFilename)) {
            Log.d(TAG, "✅ Using cached FirebaseApp for " + assetFilename);
            return cachedApps.get(assetFilename);
        }

        try {
            // Parse FirebaseOptions from asset file
            FirebaseOptions options = parseFirebaseOptionsFromAsset(context, assetFilename);
            if (options == null) {
                Log.e(TAG, "❌ Failed to parse FirebaseOptions from " + assetFilename);
                return getDefaultFirebaseApp(context);
            }

            // Use a unique app name based on the config file
            String appName = assetFilename.replace(".json", "").replace("google-services-", "");
            
            // Check if already initialized
            FirebaseApp existingApp = getAppByName(context, appName);
            if (existingApp != null) {
                Log.d(TAG, "⚙️ Using existing FirebaseApp: " + appName);
                cachedApps.put(assetFilename, existingApp);
                return existingApp;
            }

            // Initialize new FirebaseApp
            FirebaseApp app = FirebaseApp.initializeApp(context, options, appName);
            cachedApps.put(assetFilename, app);
            Log.d(TAG, "✅ FirebaseApp initialized: " + appName + " (projectId: " + options.getProjectId() + ")");
            return app;

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize FirebaseApp for " + assetFilename, e);
            return getDefaultFirebaseApp(context);
        }
    }

    /**
     * Parse FirebaseOptions from a google-services JSON file in assets.
     * Returns null if file not found or parsing fails.
     */
    private static FirebaseOptions parseFirebaseOptionsFromAsset(Context context, String assetFilename) {
        try (InputStream inputStream = context.getAssets().open(assetFilename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONObject json = new JSONObject(jsonBuilder.toString());
            JSONObject projectInfo = json.getJSONObject("project_info");
            JSONObject client = json.getJSONArray("client").getJSONObject(0);
            JSONObject apiKeyObj = client.getJSONArray("api_key").getJSONObject(0);
            JSONObject clientInfo = client.getJSONObject("client_info");

            String projectId = projectInfo.getString("project_id");
            String apiKey = apiKeyObj.getString("current_key");
            String appId = clientInfo.getString("mobilesdk_app_id");
            String storageBucket = projectInfo.optString("storage_bucket", "");

            FirebaseOptions.Builder builder = new FirebaseOptions.Builder()
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .setApiKey(apiKey);
            
            if (!storageBucket.isEmpty()) {
                builder.setStorageBucket(storageBucket);
            }

            Log.d(TAG, "📋 Parsed config: projectId=" + projectId + ", storageBucket=" + storageBucket);
            return builder.build();

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to parse " + assetFilename, e);
            return null;
        }
    }

    /**
     * Get default FirebaseApp (initialized from google-services.json in root).
     * Returns null if not initialized.
     */
    private static FirebaseApp getDefaultFirebaseApp(Context context) {
        try {
            List<FirebaseApp> apps = FirebaseApp.getApps(context);
            for (FirebaseApp app : apps) {
                if (FirebaseApp.DEFAULT_APP_NAME.equals(app.getName())) {
                    return app;
                }
            }
            // Try to get default app directly
            return FirebaseApp.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "⚠️ Default FirebaseApp not initialized", e);
            return null;
        }
    }

    /**
     * Find FirebaseApp by name in the list of initialized apps.
     */
    private static FirebaseApp getAppByName(Context context, String name) {
        List<FirebaseApp> apps = FirebaseApp.getApps(context);
        for (FirebaseApp app : apps) {
            if (app.getName().equals(name)) {
                return app;
            }
        }
        return null;
    }

    // 🔹 Upload local file (e.g., CSV or snapshot)
    public void uploadFile(File file, String remotePath, UploadCallback callback) {
        if (file == null || !file.exists()) {
            if (callback != null) callback.onFailure(new Exception("File not found"));
            Log.e(TAG, "❌ File not found: " + file);
            return;
        }

        Uri uri = Uri.fromFile(file);
        StorageReference ref = storageRoot.child(remotePath);

        UploadTask uploadTask = ref.putFile(uri);
        uploadTask
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        Log.d(TAG, "✅ Uploaded: " + remotePath + " → " + downloadUri);
                        if (callback != null) callback.onSuccess(downloadUri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Upload failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // 🔹 Upload InputStream directly (for in-memory data)
    public void uploadStream(InputStream stream, String remotePath, UploadCallback callback) {
        try {
            StorageReference ref = storageRoot.child(remotePath);
            UploadTask uploadTask = ref.putStream(stream);
            uploadTask
                    .addOnSuccessListener(taskSnapshot -> {
                        ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            Log.d(TAG, "✅ Stream uploaded → " + downloadUri);
                            if (callback != null) callback.onSuccess(downloadUri.toString());
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Stream upload failed", e);
                        if (callback != null) callback.onFailure(e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "❌ Stream upload error", e);
            if (callback != null) callback.onFailure(e);
        }
    }

    // 🔹 Download URL for an existing file
    public void getDownloadUrl(String remotePath, DownloadCallback callback) {
        StorageReference ref = storageRoot.child(remotePath);
        ref.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Log.d(TAG, "✅ Download URL fetched: " + uri);
                    if (callback != null) callback.onSuccess(uri.toString());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to fetch download URL", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // --------------------------------------------------------
    // Callback interfaces
    // --------------------------------------------------------
    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    public interface DownloadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }
}
