package com.durgesh.keepeye;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button fab, btnservice;
    EditText thisname;
    String friendname;
    TextView thisid, thisid2;
    addingmobile getnamefunco= new addingmobile();
    SharedPreferences sharedPreferences;
    FirebaseFirestore db3 = FirebaseFirestore.getInstance();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    Context context=MainActivity.this;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sp =  getSharedPreferences("app_prefs", MODE_PRIVATE);
        String userid2= sp.getString("user_id","0");

        thisname = findViewById(R.id.thisableName);
        thisid = findViewById(R.id.thisid);
        fab = findViewById(R.id.fab);
        btnservice=findViewById(R.id.servicebutt);
        thisid2 = findViewById(R.id.thisidcopy);


        Intent intent = new Intent(this, LocationService.class);
        intent.setAction("ACTION_LISTEN_TRACKING_REQUESTS");
        intent.putExtra("myId", userid2);
        try{

           // Replace with the logged-in user's ID
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                startForegroundService(intent);
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("serstatus", "1").apply();
                btnservice.setText("Stop Service");
            } else {
                startService(intent);
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("serstatus", "1").apply();
                btnservice.setText("Stop Service");
            }

        }catch (Exception e){

            Log.d("TAG", "onCreate: "+e.getMessage());
        }




        btnservice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences sp =  getSharedPreferences("app_prefs", MODE_PRIVATE);
                String serstatus= sp.getString("serstatus","1");

                if (serstatus.equals("1")) {
                    stopService(intent);
                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("serstatus", "0").apply();
                    Toast.makeText(MainActivity.this, "Service Stopped", Toast.LENGTH_SHORT).show();
                    btnservice.setText("Start Service");

                }else if (serstatus.equals("0")){

                    btnservice.setText("Stop Service");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                    Toast.makeText(MainActivity.this, "Service Started", Toast.LENGTH_SHORT).show();

                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("serstatus", "1").apply();

                }
            }
        });


        // Request permissions
        if (!hasLocationPermissions()) {
            requestLocationPermissions();
        }

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // Restore user name
        String namethis = sharedPreferences.getString("namethis", "0");
        thisname.setText(namethis);

        // Set FAB action
        fab.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, addingmobile.class);
            startActivity(i);
        });

        // Restore and display user ID
        thisid.setText(sharedPreferences.getString("user_id", "0"));
        setClipboardListener(thisid);
//        setClipboardListener(thisid);

        loadFriendsList();

        preferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("friends")) {
                loadFriendsList(); // Reload the friends list when 'friends' key changes
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        // Save name changes in real-time
        thisname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                sharedPreferences.edit()
                        .putString("namethis", s.toString())
                        .apply();
            }
        });
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
         ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setClipboardListener(TextView textView) {
        textView.setOnClickListener(v -> {
            String text = textView.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Device ID Copied!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadFriendsList() {
        String friendsJson = sharedPreferences.getString("friends", "[]");
        List<Friend> friendsList = new ArrayList<>();
        Log.d("listerror", "loadFriendsList: "+friendsList);
        try {
            JSONArray friendsArray = new JSONArray(friendsJson);
            for (int i = 0; i < friendsArray.length(); i++) {
                JSONObject friendObject = friendsArray.getJSONObject(i);
                String friendId = friendObject.getString("id");
                String friendName = friendObject.getString("name");

                friendsList.add(new Friend(friendId, friendName));
                Log.d("listerror", "loadFriendsList: "+friendsList);
            }
        } catch (JSONException e) {
            Log.d("listerror", "loadFriendsList: "+friendsList);
        }

        updateFriendsUI(friendsList);
    }

    private void updateFriendsUI(List<Friend> friendsList) {
        LinearLayout container = findViewById(R.id.mainlayout);
        container.removeAllViews();

        for (Friend friend : friendsList) {

            View cardView = LayoutInflater.from(this).inflate(R.layout.friend_card_view, container, false);

            TextView nameEditText = cardView.findViewById(R.id.ableName);
            TextView idTextView = cardView.findViewById(R.id.thisid1);
            Button watchButton = cardView.findViewById(R.id.btnWatch);
            Button notifyButton = cardView.findViewById(R.id.btnnotify);
            String receiverId = idTextView.getText().toString(); // The user being tracked
           String  friendid= receiverId.substring(4);

            watchButton.setOnClickListener(v -> {
                // Watch button functionality
                Intent intent = new Intent(this, MapActivity.class);
                intent.putExtra("friendId", friendid); // Pass friend's ID
                startActivity(intent);

            });

            notifyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Toast.makeText(MainActivity.this, "Looking For IT!!", Toast.LENGTH_SHORT).show();
                    String receiverId = idTextView.getText().toString(); // The user being tracked
                    receiverId= receiverId.substring(4);
                    SharedPreferences sp =  getSharedPreferences("app_prefs", MODE_PRIVATE);
                    String userid2= sp.getString("user_id","0");

                    Intent sendLocationIntent = new Intent(MainActivity.this, LocationService.class);
                    sendLocationIntent.setAction("ACTION_PUT_READ");
                    sendLocationIntent.putExtra("friendId",receiverId);
                    sendLocationIntent.putExtra("myId",userid2 );
//                    startService(sendLocationIntent);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                        startForegroundService(sendLocationIntent);
                    } else {
                        startService(sendLocationIntent);
                    }

                }
            });


            cardView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showRemoveFriendDialog(friend, cardView, container);
                    return true;
                }
            });

            nameEditText.setText(friend.getName());

            nameEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEditNameDialog(friend,nameEditText);
                }
            });
            idTextView.setText("ID: " + friend.getId());


            container.addView(cardView);

        }
    }
    // Method to show edit name dialog
    private void showEditNameDialog(Friend friend, TextView nameTextView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Name");

        // Create EditText for input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(friend.getName());

        builder.setView(input);

        // Set "OK" button
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString();
            nameTextView.setText(newName); // Update TextView
            updateFriendInPreferences(friend.getId(), newName); // Update SharedPreferences
        });
        // Set "Cancel" button
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        // Show dialog
        builder.show();
    }
    private void showRemoveFriendDialog(Friend friend, View cardView, LinearLayout container) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Friend");
        builder.setMessage("Are you sure you want to remove " + friend.getName() + "?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Remove friend from container
            container.removeView(cardView);

            // Update shared preferences
            removeFriendFromPreferences(friend.getId());

            // Update database
            removeFriendFromDatabase(friend.getId());

            dialog.dismiss();
            Toast.makeText(this, friend.getName() + " has been removed.", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void removeFriendFromDatabase(String id) {

        SharedPreferences sp =  getSharedPreferences("app_prefs", MODE_PRIVATE);
        String userid2= sp.getString("user_id","0");
        DocumentReference currentUserFriendDoc = db3.collection("users")
                .document(userid2)
                .collection("friends")
                .document(id);
        DocumentReference friendUserFriendDoc = db3.collection("users")
                .document(id)
                .collection("friends")
                .document(userid2);
        try {
            currentUserFriendDoc.delete();
            friendUserFriendDoc.delete();

        }catch (Exception e){

        }

    }

    private void removeFriendFromPreferences(String friendId) {
        SharedPreferences sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String friendsJson = sharedPreferences.getString("friends", "[]");
        try {
            JSONArray friendsArray = new JSONArray(friendsJson);
            JSONArray updatedArray = new JSONArray();

            for (int i = 0; i < friendsArray.length(); i++) {
                JSONObject friendObject = friendsArray.getJSONObject(i);
                if (!friendObject.getString("id").equals(friendId)) {
                    updatedArray.put(friendObject); // Keep friends other than the one being removed
                }
            }

            editor.putString("friends", updatedArray.toString());
            editor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateFriendInPreferences(String friendId, String newName) {
        try {
//            Toast.makeText(this, "here", Toast.LENGTH_SHORT).show();

            String friendsJson = sharedPreferences.getString("friends", "[]");
            JSONArray friendsArray = new JSONArray(friendsJson);

            for (int i = 0; i < friendsArray.length(); i++) {
                JSONObject friendObject = friendsArray.getJSONObject(i);
                if (friendObject.getString("id").equals(friendId)) {
                    friendObject.put("name", newName);
                    break;
                }
            }
            sharedPreferences.edit()
                    .putString("friends", friendsArray.toString())
                    .apply();

//            nameEditText.setFocusable(true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (preferenceChangeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
        sharedPreferences.edit()
                .putString("namethis", thisname.getText().toString())
                .apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.edit()
                .putString("namethis", thisname.getText().toString())
                .apply();
    }
}
