package com.durgesh.keepeye;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LocationService extends Service {


    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    int counter=0;
    private FusedLocationProviderClient locationClient;
    private FirebaseFirestore db;
//10 * 60 * 1000
    private static final long CALCULATION_DURATION = 10 * 60 * 1000; // 10 minutes in milliseconds
    private static final long CALCULATION_DURATION2 = 60000; // 10 minutes in milliseconds
    private Map<String, Location> trackingRequests = new HashMap<>();
    private LocationCallback locationCallback;
    private boolean isCalculating = false;
    private Handler stopHandler;

    private  Handler stopHandler2;
    Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        stopHandler2= new Handler(Looper.getMainLooper());
        stopHandler = new Handler(Looper.getMainLooper());
        context=this;
        startForeground(1, createNotification("Listening for tracking requests"));
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();
            if ("ACTION_LISTEN_TRACKING_REQUESTS".equals(action)) {
                String myId = intent.getStringExtra("myId");
                listenForTrackingRequests(myId);
            } else if ("ACTION_PUT_READ".equals(action)) {
                String myId = intent.getStringExtra("myId");
                String friendid = intent.getStringExtra("friendId");
                sendLocationToTrackee(myId,friendid);
            }
        }
        return START_STICKY;

    }

    private void sendLocationToTrackee(String myId, String friendid) {

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5*60000); // 50 seconds


            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {

                    if (locationResult == null) return;
                    Location currentLocation = locationResult.getLastLocation();
                    if (currentLocation != null) {

                        double latitude = currentLocation.getLatitude();
                        double longitude = currentLocation.getLongitude();

                        Map<String, Object> trackingData = new HashMap<>();
                        trackingData.put("latitude", latitude);
                        trackingData.put("longitude", longitude);
                        trackingData.put("trigger", "");

                        try {
                            db.collection("users").document(friendid)
                                    .collection("trackingRequests")
                                    .document(myId)
                                    .set(trackingData)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {

                                            db.collection("users")
                                                    .document(friendid)
                                                    .collection("trackingRequests")
                                                    .document(myId)
                                                    .addSnapshotListener((snapshot, e) -> {
                                                        if (e != null) {
                                                            Log.e(TAG, "Failed to listen for triggers", e);
                                                            return;
                                                        }

                                                        if (snapshot != null && snapshot.exists()) {
                                                            String triggerValue = (String) snapshot.get("trigger");
                                                            if (triggerValue != null && !triggerValue.equals("")) {
                                                                Log.d(TAG, "Trigger field is true. Perform action here.");
                                                                // Perform your action when the trigger is true
                                                                SharedPreferences sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
                                                                String friendsJson = sharedPreferences.getString("friends", "[]");
// Convert the JSON string into a JSONArray
                                                                try {
                                                                    JSONArray friendsArray = new JSONArray(friendsJson);

                                                                    // Loop through the friends array to find the friend with the matching friendid
                                                                    for (int i = 0; i < friendsArray.length(); i++) {

                                                                        JSONObject friendObject = friendsArray.getJSONObject(i);

                                                                        // Check if the friend ID matches
                                                                        if (friendObject.has("id") && friendObject.getString("id").equals(friendid)) {
                                                                            String friendName = friendObject.getString("name");

                                                                            // Now send the notification with the friend's name
                                                                             sendNotification(friendName, " is " + triggerValue + " far from you");
                                                                            if(Float.parseFloat(triggerValue)<30){
                                                                                stopHandler2.
                                                                                        postDelayed(Objects.requireNonNull(stopDistanceCalculation2()), 1000);
                                                                            }
                                                                            break; // Exit the loop once the friend is found
                                                                        }


                                                                    }

                                                                } catch (Exception e2) {
                                                                    e2.printStackTrace(); // Handle JSON parsing exception
                                                                }

//                                                                // Reset the trigger field back to false, if necessary
//                                                                snapshot.getReference().update("trigger", false)
//                                                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Trigger field reset to false"))
//                                                                        .addOnFailureListener(error -> Log.e(TAG, "Failed to reset trigger field", error));
                                                            } else {
                                                                Log.d(TAG, "Trigger field is false or null. No action needed.");
                                                            }
                                                        } else {
                                                            Log.d(TAG, "Document does not exist");
                                                        }
                                                    });

                                        }
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(LocationService.this, "NOt uploaded", Toast.LENGTH_SHORT).show());
                        }catch (Exception e){
                            Log.e("TAG", "failedonLocationResult: ",e );
                        }
                    }
                    else{
                        Log.d(TAG, "Location is null");
                    }
                }

            };
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

//        stopHandler2.postDelayed(this::stopDistanceCalculation2, 30000);

    }
    private Runnable stopDistanceCalculation2() {
        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
            Log.d("TAG", "Location updates stopped");
        }
        return null;
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

                    if (snapshot != null && !snapshot.isEmpty() && !snapshot.getMetadata().hasPendingWrites() && !snapshot.getMetadata().isFromCache()) {
                        // Update tracking requests when changes occur
                        Map<String, Location> updatedRequests = new HashMap<>();
                        snapshot.getDocuments().forEach(document -> {
                            String id = document.getId();

                            double trackerLat = document.getDouble("latitude");
                            double trackerLon = document.getDouble("longitude");

                            Location trackerLocation = new Location("Tracker");
                            trackerLocation.setLatitude(trackerLat);
                            trackerLocation.setLongitude(trackerLon);

                            updatedRequests.put(id, trackerLocation);
                        });
                        if (hasTrackingRequestsChanged(updatedRequests)) {
                            trackingRequests = updatedRequests;
                            startDistanceCalculation(myId);
                        }

                        if (!trackingRequests.equals(updatedRequests) && counter!=0 ) {
                            trackingRequests = updatedRequests;
                            startDistanceCalculation(myId);
                        }

                        counter++;
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


    private void startDistanceCalculation( String myID) {
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
//
                        String distanceString;
                        if (distance > 1000) {
                            distanceString = String.format("%.2f km", distance / 1000); // Convert to kilometers with 2 decimal places
                        } else {
                            distanceString = String.format("%.2f m", distance); // Keep in meters with 2 decimal places
                        }

                        db.collection("users").document(myID)
                                .collection("trackingRequests")
                                .document(trackerId).update("trigger", distanceString);

//                        sendNotification("Distance Alert", "Distance from " + trackerId + ": " + distance + " meters");
                    });
                }
            }
        };

        locationClient.requestLocationUpdates( locationRequest, locationCallback , Looper.getMainLooper() );

        // Stop calculation after 10 minutes
        stopHandler.postDelayed(this::stopDistanceCalculation, 2*60000);

    }

    private void stopDistanceCalculation() {
        if (!isCalculating) return;
        isCalculating = false;

        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }

        stopHandler.removeCallbacksAndMessages(null); // Clear any pending stop actions
        Log.d("TAG", "Distance calculation stopped.");
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