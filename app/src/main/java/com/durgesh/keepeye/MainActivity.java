package com.durgesh.keepeye;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.appcompat.app.AppCompatActivity;

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
    SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        thisname = findViewById(R.id.thisableName);
        thisid = findViewById(R.id.thisid);
        fab = findViewById(R.id.fab);
        thisid2 = findViewById(R.id.thisidcopy);

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

            watchButton.setOnClickListener(v -> {
                // Watch button functionality
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
