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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button fab;
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
        try{
            Intent serviceIntent = new Intent(this, LocationService.class);
            serviceIntent.setAction("ACTION_LISTEN_UPDATES");
            serviceIntent.putExtra("myId",userid2);
            startService(serviceIntent);
        }catch (Exception e){
            Log.d("TAG", "onCreate: "+e.getMessage());
        }


        thisname = findViewById(R.id.thisableName);
        thisid = findViewById(R.id.thisid);
        fab = findViewById(R.id.fab);
        thisid2 = findViewById(R.id.thisidcopy);

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
            Intent intent = new Intent(MainActivity.this, addingmobile.class);
            startActivity(intent);
        });

        // Restore and display user ID
        thisid.setText(sharedPreferences.getString("user_id", "0"));
        setClipboardListener(thisid);
        setClipboardListener(thisid2);

//        loadFriendsList();

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

        try {
            JSONArray friendsArray = new JSONArray(friendsJson);
            for (int i = 0; i < friendsArray.length(); i++) {
                JSONObject friendObject = friendsArray.getJSONObject(i);
                String friendId = friendObject.getString("id");
                String friendName = friendObject.getString("name");

                friendsList.add(new Friend(friendId, friendName));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        updateFriendsUI(friendsList);
    }

    private void updateFriendsUI(List<Friend> friendsList) {
        LinearLayout container = findViewById(R.id.mainlayout);
        container.removeAllViews();

        for (Friend friend : friendsList) {
            View cardView = LayoutInflater.from(this).inflate(R.layout.friend_card_view, container, false);

            EditText nameEditText = cardView.findViewById(R.id.ableName);
            TextView idTextView = cardView.findViewById(R.id.thisid1);
            Button watchButton = cardView.findViewById(R.id.btnWatch);
            Button notifyButton = cardView.findViewById(R.id.btnnotify);
            watchButton.setOnClickListener(v -> {
                // Watch button functionality
            });
            notifyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent sendLocationIntent = new Intent(MainActivity.this, LocationService.class);
                    sendLocationIntent.setAction("ACTION_SEND_LOCATION");
                    sendLocationIntent.putExtra("friendId", idTextView.getText().toString());
//                    startService(sendLocationIntent);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService ( sendLocationIntent );
                    } else {
                        startService ( sendLocationIntent );
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
            idTextView.setText("ID: " + friend.getId());

            nameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    updateFriendInPreferences(friend.getId(), s.toString());
                }
            });

            container.addView(cardView);
        }
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

        DocumentReference currentUserFriendDoc = db3.collection("users")
                .document(getnamefunco.getCurrentUserId())
                .collection("friends")
                .document(id);
        DocumentReference friendUserFriendDoc = db3.collection("users")
                .document(id)
                .collection("friends")
                .document(getnamefunco.getCurrentUserId());
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
