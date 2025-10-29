package com.example.sdk;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FirestoreManager
 * - uploadHealthData(SmartWatchData, Callback) -> single upload
 * - uploadBatch(List<SmartWatchData>, Callback) -> batch upload (atomic-ish)
 * - fetchAllData(FirestoreCallback) -> one-shot fetch
 * - startRealtimeListener(RealtimeCallback) / stopRealtimeListener() -> live updates
 *
 * Note: Make sure firebase is initialized (google-services.json + init) in your app.
 */
public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private static final String COLLECTION_NAME = "smartwatch_data";

    private final FirebaseFirestore db;
    private ListenerRegistration realtimeListener = null;

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    // ---------------- Single upload ----------------
    public void uploadHealthData(SmartWatchData data, Callback callback) {
        if (data == null) {
            Log.e(TAG, "⚠️ Cannot upload null data");
            if (callback != null) callback.onFailure(new Exception("Null data"));
            return;
        }

        CollectionReference ref = db.collection(COLLECTION_NAME);
        ref.add(data.toMap())
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Uploaded to Firestore: " + documentReference.getId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Upload failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // ---------------- Batch upload ----------------
    /**
     * Batch upload list of SmartWatchData. Creates new doc per entry with generated id.
     * Calls callback.onSuccess() if overall succeeds, or onFailure on error.
     */
    public void uploadBatch(List<SmartWatchData> list, Callback callback) {
        if (list == null || list.isEmpty()) {
            Log.w(TAG, "⚠️ uploadBatch called with empty list");
            if (callback != null) callback.onSuccess(); // nothing to do
            return;
        }

        WriteBatch batch = db.batch();
        CollectionReference col = db.collection(COLLECTION_NAME);

        for (SmartWatchData item : list) {
            DocumentReference newDoc = col.document(); // auto ID
            batch.set(newDoc, item.toMap());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Batch upload successful (" + list.size() + " items)");
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Batch upload failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // ---------------- Fetch all (one-shot) ----------------
    public void fetchAllData(FirestoreCallback callback) {
        db.collection(COLLECTION_NAME)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<SmartWatchData> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Map<String, Object> map = doc.getData();
                            if (map == null) continue;
                            SmartWatchData d = mapToSmartWatchData(map);
                            if (d != null) list.add(d);
                        } catch (Exception ex) {
                            Log.w(TAG, "⚠️ Skipping malformed doc: " + doc.getId(), ex);
                        }
                    }
                    Log.d(TAG, "✅ Retrieved " + list.size() + " Firestore records");
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore fetch failed", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    // ---------------- Realtime listener ----------------
    /**
     * Start realtime listener. Caller should implement RealtimeCallback to receive adds/changes/removes.
     * Only one listener managed per FirestoreManager instance (calls to start will replace previous).
     */
    public void startRealtimeListener(RealtimeCallback callback) {
        stopRealtimeListener(); // ensure single listener

        realtimeListener = db.collection(COLLECTION_NAME)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "❌ Realtime listener error", e);
                        if (callback != null) callback.onFailure(e);
                        return;
                    }
                    if (querySnapshot == null) return;

                    List<SmartWatchData> added = new ArrayList<>();
                    List<SmartWatchData> modified = new ArrayList<>();
                    List<SmartWatchData> removed = new ArrayList<>();

                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        DocumentSnapshot doc = dc.getDocument();
                        SmartWatchData d = mapToSmartWatchData(doc.getData());
                        switch (dc.getType()) {
                            case ADDED:
                                if (d != null) added.add(d);
                                break;
                            case MODIFIED:
                                if (d != null) modified.add(d);
                                break;
                            case REMOVED:
                                if (d != null) removed.add(d);
                                break;
                        }
                    }

                    if (callback != null) callback.onUpdate(added, modified, removed);
                });
    }

    public void stopRealtimeListener() {
        if (realtimeListener != null) {
            realtimeListener.remove();
            realtimeListener = null;
            Log.d(TAG, "🛑 Realtime listener removed");
        }
    }

    // ---------------- Helpers ----------------
    private SmartWatchData mapToSmartWatchData(Map<String, Object> map) {
        if (map == null) return null;
        try {
            SmartWatchData d = new SmartWatchData();

            Object tsObj = map.get("timestamp");
            long ts = 0;
            if (tsObj instanceof Number) {
                ts = ((Number) tsObj).longValue();
            } else if (tsObj instanceof String) {
                ts = Long.parseLong((String) tsObj);
            }
            d.setTimestamp(ts);

            Object hrObj = map.get("heartRate");
            if (hrObj instanceof Number) d.setHeartRate(((Number) hrObj).intValue());
            else if (hrObj instanceof String) d.setHeartRate(Integer.parseInt((String) hrObj));

            Object spo2Obj = map.get("spO2");
            if (spo2Obj instanceof Number) d.setSpO2(((Number) spo2Obj).floatValue());
            else if (spo2Obj instanceof String) d.setSpO2(Float.parseFloat((String) spo2Obj));

            Object tempObj = map.get("temperature");
            if (tempObj instanceof Number) d.setTemperature(((Number) tempObj).floatValue());
            else if (tempObj instanceof String) d.setTemperature(Float.parseFloat((String) tempObj));

            Object stepsObj = map.get("steps");
            if (stepsObj instanceof Number) d.setSteps(((Number) stepsObj).intValue());
            else if (stepsObj instanceof String) d.setSteps(Integer.parseInt((String) stepsObj));

            return d;
        } catch (Exception ex) {
            Log.w(TAG, "⚠️ mapToSmartWatchData parse error", ex);
            return null;
        }
    }

    // ---------------- Callback interfaces ----------------
    public interface Callback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface FirestoreCallback {
        void onSuccess(List<SmartWatchData> dataList);
        void onFailure(Exception e);
    }

    public interface RealtimeCallback {
        void onUpdate(List<SmartWatchData> added, List<SmartWatchData> modified, List<SmartWatchData> removed);
        void onFailure(Exception e);
    }
}
