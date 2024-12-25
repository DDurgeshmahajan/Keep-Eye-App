package com.durgesh.keepeye;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service {


    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";

    private FusedLocationProviderClient locationClient;
    private FirebaseFirestore db;

    private static final long CALCULATION_DURATION = 10 * 60 * 1000; // 10 minutes in milliseconds
    private Map<String, Location> trackingRequests = new HashMap<>();
    private LocationCallback locationCallback;
    private boolean isCalculating = false;

    private Handler stopHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        stopHandler = new Handler(Looper.getMainLooper());
        startForeground(1, createNotification("Listening for tracking requests"));
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && "ACTION_LISTEN_TRACKING_REQUESTS".equals(intent.getAction())) {
            String myId = intent.getStringExtra("myId");
            listenForTrackingRequests(myId);
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

    private void listenForTrackingRequests(String myId) {

        db.collection("users").document(myId)
                .collection("trackingRequests")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Failed to listen for tracking requests", e);
                        return;
                    }

                    if (snapshot != null && !snapshot.isEmpty()) {
                        // Update tracking requests when changes occur
                        Map<String, Location> updatedRequests = new HashMap<>();
                        snapshot.getDocuments().forEach(document -> {
                            String trackerId = document.getId();
                            double trackerLat = document.getDouble("latitude");
                            double trackerLon = document.getDouble("longitude");

                            Location trackerLocation = new Location("Tracker");
                            trackerLocation.setLatitude(trackerLat);
                            trackerLocation.setLongitude(trackerLon);

                            updatedRequests.put(trackerId, trackerLocation);
                        });
                        if (hasTrackingRequestsChanged(updatedRequests)) {
                            trackingRequests = updatedRequests;
                            startDistanceCalculation();
                        }
//                        if (!trackingRequests.equals(updatedRequests)) {
//                            trackingRequests = updatedRequests;
//                            startDistanceCalculation();
//                        }
                    } else {
                        stopDistanceCalculation();
                    }
                });
    }

    private boolean hasTrackingRequestsChanged(Map<String, Location> newRequests) {
        if (trackingRequests.size() != newRequests.size()) return true;
        for (String key : newRequests.keySet()) {
            if (!trackingRequests.containsKey(key)) return true;

            Location oldLoc = trackingRequests.get(key);
            Location newLoc = newRequests.get(key);

            if (oldLoc.getLatitude() != newLoc.getLatitude() ||
                    oldLoc.getLongitude() != newLoc.getLongitude()) {
                return true;
            }
        }

        return false;
    }


    private void startDistanceCalculation() {
        if (isCalculating) return; // Prevent duplicate calculations
        isCalculating = true;

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000); // 5 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location currentLocation = locationResult.getLastLocation();
                if (currentLocation != null) {
                    trackingRequests.forEach((trackerId, trackerLocation) -> {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude(),
                                trackerLocation.getLatitude(),
                                trackerLocation.getLongitude(),
                                results
                        );

                        float distance = results[0];
                        snapshot.getMetadata().isFromCache()
//                        sendNotification("Distance Alert",
//                                "Distance from " + trackerId + ": " + distance + " meters");
                    });
                }
            }
        };

        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // Stop calculation after 10 minutes
        stopHandler.postDelayed(this::stopDistanceCalculation, CALCULATION_DURATION);

    }

    private void stopDistanceCalculation() {
        if (!isCalculating) return;
        isCalculating = false;

        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }

        stopHandler.removeCallbacksAndMessages(null); // Clear any pending stop actions
        Log.d(TAG, "Distance calculation stopped.");
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopDistanceCalculation(); // Ensure location updates are stopped when service is destroyed
    }

}