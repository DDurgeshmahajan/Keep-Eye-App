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


public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";

    private FusedLocationProviderClient locationClient;
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;

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
                        Object location = snapshot.get("location");
                        if (location != null) {
                            sendNotification("Location Update", "Your location has been updated!");
                        }
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

