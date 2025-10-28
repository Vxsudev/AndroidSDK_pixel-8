package com.example.sdk;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
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
        try {
            if (cachedStorages.containsKey(configFile)) {
                storage = cachedStorages.get(configFile);
                storageRoot = storage.getReference();
                Log.d(TAG, "✅ Using cached Firebase Storage for " + configFile);
                return;
            }

            // Re-initialize FirebaseApp using the configFile
            InputStream stream = context.getAssets().open(configFile);
            FirebaseOptions options = FirebaseOptions.fromResource(context);

            FirebaseApp app = FirebaseApp.initializeApp(context, options, configFile);
            storage = FirebaseStorage.getInstance(app);
            storageRoot = storage.getReference();

            cachedStorages.put(configFile, storage);
            Log.d(TAG, "✅ CloudStorage initialized for: " + configFile);

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize CloudStorage for " + configFile, e);
            storage = FirebaseStorage.getInstance();
            storageRoot = storage.getReference();
        }
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
