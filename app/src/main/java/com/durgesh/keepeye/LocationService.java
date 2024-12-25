package com.durgesh.keepeye;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.Set;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";

    private FusedLocationProviderClient locationClient;
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;
    private Set<String> trackers = new HashSet<>(); // Trackers who want to track you

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        startForeground(1, createNotification("Location Service Running"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("ACTION_SEND_LOCATION".equals(action)) {
                String friendId = intent.getStringExtra("friendId");
                sendCurrentLocation(friendId);
            } else if ("ACTION_LISTEN_UPDATES".equals(action)) {
                String myId = intent.getStringExtra("myId");
                listenForLocationUpdates(myId);
            } else if ("ACTION_LISTEN_PROXIMITY".equals(action)) {
                String trackerId = intent.getStringExtra("trackerId");
                String trackeeId = intent.getStringExtra("trackeeId");
                listenForProximityUpdates(trackerId, trackeeId);
            }
        }
        return START_STICKY;
    }

    private Notification createNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Friend Tracker")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void sendCurrentLocation(String friendId) {
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                db.collection("users").document(friendId)
                        .update("location.latitude", latitude, "location.longitude", longitude)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Location sent to friend's document"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to send location", e));
            }
        });
    }

    private void listenForLocationUpdates(String myId) {
        listenerRegistration = db.collection("users").document(myId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Failed to listen for updates", e);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Object trackersField = snapshot.get("trackers");
                        if (trackersField instanceof Set<?>) {
                            trackers.clear();
                            trackers.addAll((Set<String>) trackersField);
                            Log.d(TAG, "Updated trackers: " + trackers);

                            for (String trackerId : trackers) {
                                sendNotification("Tracker Alert", "User " + trackerId + " is tracking you.");
                                checkDistanceToTracker(trackerId);
                            }
                        }
                    }
                });
    }

    private void listenForProximityUpdates(String trackerId, String trackeeId) {
        db.collection("users").document(trackeeId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Failed to listen for updates", e);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> location = (Map<String, Object>) snapshot.get("location");
                        if (location != null) {
                            double trackeeLat = (double) location.get("latitude");
                            double trackeeLon = (double) location.get("longitude");

                            // Fetch tracker's location
                            locationClient.getLastLocation().addOnSuccessListener(trackerLocation -> {
                                if (trackerLocation != null) {
                                    double trackerLat = trackerLocation.getLatitude();
                                    double trackerLon = trackerLocation.getLongitude();

                                    // Calculate distance
                                    float[] results = new float[1];
                                    Location.distanceBetween(trackerLat, trackerLon, trackeeLat, trackeeLon, results);
                                    float distanceInMeters = results[0];

                                    // Notify if within proximity (e.g., 100 meters)
                                    if (distanceInMeters <= 100) {
                                        sendNotification("Proximity Alert", "Trackee is within 100 meters!");
                                    }
                                }
                            });
                        }
                    }
                });
    }
    private void checkDistanceToTracker(String trackerId) {
        db.collection("users").document(trackerId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                double trackerLat = documentSnapshot.getDouble("location.latitude");
                double trackerLon = documentSnapshot.getDouble("location.longitude");

                locationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(location.getLatitude(), location.getLongitude(), trackerLat, trackerLon, results);
                        float distance = results[0];
                        Log.d(TAG, "Distance to " + trackerId + ": " + distance + " meters");

                        if (distance < 100) { // Notify if within 100 meters
                            sendNotification("Distance Alert", "User " + trackerId + " is within 100 meters!");
                        }
                    }
                });
            }
        });
    }

    private void sendNotification(String title, String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
