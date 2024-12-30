package com.durgesh.keepeye;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private String friendId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapactivity);

        friendId = getIntent().getStringExtra("friendId");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MapActivity", "MapFragment not found");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(LatLng point) {
        // Place marker at clicked location
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(point).title("Target Location"));

        // Save the location in Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", point.latitude);
        data.put("longitude", point.longitude);
        data.put("isWatchActive", true);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(friendId)
                .collection("watch")
                .document("targetLocation")
                .set(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Watch location set!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Log.e("MapActivity", "Failed to set watch location", e));
    }
}
