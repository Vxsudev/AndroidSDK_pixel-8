package com.example.sdk;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Handles upload and retrieval of smartwatch data from Firestore.
 * Phase-8 additions: batch upload + realtime listener
 */
public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private static final String COLLECTION_NAME = "smartwatch_data";

    private final FirebaseFirestore db;
    private ListenerRegistration realtimeListener = null;

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    // Existing single-object upload
    public void uploadHealthData(SmartWatchData data, UploadCallback callback) {
        if (data == null) {
            Log.e(TAG, "⚠️ Cannot upload null data");
            if (callback != null) callback.onFailure(new Exception("Null data"));
            return;
        }

        CollectionReference ref = db.collection(COLLECTION_NAME);

        ref.add(data)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Uploaded to Firestore: " + documentReference.getId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Upload failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Phase-8: batch upload - atomic-ish using WriteBatch (multiple writes)
    public void uploadBatch(@NonNull List<SmartWatchData> list, UploadCallback callback) {
        if (list == null || list.isEmpty()) {
            Log.w(TAG, "⚠️ uploadBatch called with empty list");
            if (callback != null) callback.onFailure(new Exception("Empty list"));
            return;
        }

        WriteBatch batch = db.batch();
        CollectionReference colRef = db.collection(COLLECTION_NAME);

        for (SmartWatchData item : list) {
            // create a new doc reference with auto id
            com.google.firebase.firestore.DocumentReference docRef = colRef.document();
            batch.set(docRef, item, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Batch upload success (" + list.size() + " items)");
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Batch upload failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Fetch all entries once
    public void fetchAllData(FirestoreCallback callback) {
        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<SmartWatchData> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        SmartWatchData data = doc.toObject(SmartWatchData.class);
                        list.add(data);
                    }
                    Log.d(TAG, "✅ Retrieved " + list.size() + " Firestore records");
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore fetch failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // Phase-8: Realtime listener (keeps a live subscription to the collection)
    public void listenToCollection(RealTimeListener listener) {
        // Remove previous listener if any
        if (realtimeListener != null) {
            realtimeListener.remove();
            realtimeListener = null;
        }

        realtimeListener = db.collection(COLLECTION_NAME)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "❌ Realtime listener error", e);
                        if (listener != null) listener.onError(e);
                        return;
                    }
                    if (querySnapshot == null) {
                        Log.w(TAG, "⚠️ Realtime snapshot is null");
                        if (listener != null) listener.onUpdate(new ArrayList<>());
                        return;
                    }

                    List<SmartWatchData> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        SmartWatchData data = doc.toObject(SmartWatchData.class);
                        list.add(data);
                    }


                    Log.d(TAG, "🔔 Realtime update - docs: " + list.size());
                    if (listener != null) listener.onUpdate(list);
                });
    }

    // Stop realtime if needed
    public void stopRealtime() {
        if (realtimeListener != null) {
            realtimeListener.remove();
            realtimeListener = null;
            Log.d(TAG, "Realtime listener removed");
        }
    }

    // ✅ Callback interfaces
    public interface UploadCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface FirestoreCallback {
        void onSuccess(List<SmartWatchData> dataList);
        void onFailure(Exception e);
    }

    // Phase-8 realtime callback
    public interface RealTimeListener {
        void onUpdate(List<SmartWatchData> dataList);
        void onError(Exception e);
    }
}
