package com.durgesh.keepeye;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class addingmobile extends AppCompatActivity {

    EditText serachd;
    Button button;
    FirebaseFirestore db3 = FirebaseFirestore.getInstance();
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_addingmobile);
        serachd=findViewById(R.id.idtext);
        serachd.requestFocus();
        button=findViewById(R.id.fab2);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!serachd.getText().toString().isEmpty()){

//                    SweetAlertDialog pDialog = new SweetAlertDialog(addingmobile.this, SweetAlertDialog.PROGRESS_TYPE);
//                    pDialog.getProgressHelper().setBarColor(Color.parseColor("#ffff8800"));
//                    pDialog.setTitleText("Loadingad");
//                    pDialog.setCancelable(false);
//                    pDialog.setTitleText("Loading...");
//                    pDialog.show();


                    String userid=serachd.getText().toString();
                    if(!userid.equals(getCurrentUserId())){
                        db.collection("users")
                                .document(userid)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String name = documentSnapshot.getString("name");
                                        // Display the user’s name in the search results
                                        sendFriendRequest(userid, name);
                                    } else {
                                        Toast.makeText(addingmobile.this, "User ID not found!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(addingmobile.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                    else{
                        Toast.makeText(addingmobile.this, "You cannot Put Your Own", Toast.LENGTH_SHORT).show();
                    }


                }
            }




        });



        LinearLayout requestContainer = findViewById(R.id.request_container);
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // Fetch friend requests from Firestore
        fetchFriendRequests(requestContainer);

    }

    private void fetchFriendRequests(LinearLayout requestContainer) {

        String currentUserId =getCurrentUserId();
                db3.collection("users")
                .document(currentUserId)
                .collection("friends")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String friendId = doc.getId();
                        String friendName = doc.getString("name");

                        // Add card for each pending request
                        addFriendRequestCard(requestContainer, friendId, friendName);
                    }
                });
    }

    private void addFriendRequestCard(LinearLayout requestContainer, String friendId, String friendName) {
        View cardView = getLayoutInflater().inflate(R.layout.friend_request_card, requestContainer, false);

        TextView nameTextView = cardView.findViewById(R.id.friend_name);
        Button acceptButton = cardView.findViewById(R.id.accept_button);
        Button rejectButton = cardView.findViewById(R.id.reject_button);

        nameTextView.setText(friendName);

        // Accept button click listener
        acceptButton.setOnClickListener(v -> {
            acceptFriendRequest(friendId, friendName);
            requestContainer.removeView(cardView); // Remove card from UI
        });

        // Reject button click listener
        rejectButton.setOnClickListener(v -> {
            rejectFriendRequest(friendId);
            requestContainer.removeView(cardView); // Remove card from UI
        });

        requestContainer.addView(cardView);
    }

    private void rejectFriendRequest(String friendId) {
        String currentUserId = getCurrentUserId();

        // Update Firestore with 'rejected' status
        db3.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(friendId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Friend request rejected.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error rejecting request.", Toast.LENGTH_SHORT).show());
    }

    private void acceptFriendRequest(String friendId, String friendName) {

        String currentUserId = getCurrentUserId();

        // Update Firestore with 'accepted' status
        WriteBatch batch = db3.batch();

        DocumentReference currentUserFriendDoc = db3.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(friendId);
        DocumentReference friendUserFriendDoc = db3.collection("users")
                .document(friendId)
                .collection("friends")
                .document(currentUserId);

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", "accepted");
        updateData.put("responseDate", new Date());

        batch.update(currentUserFriendDoc, updateData);
        batch.update(friendUserFriendDoc, updateData);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Save accepted friend locally in SharedPreferences
                saveFriendLocally(friendId, friendName);
                Toast.makeText(this, friendName + " is now your friend!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error accepting request. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
        
    }

    private void saveFriendLocally(String friendId, String friendName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Retrieve current friends list
        String friendsJson = sharedPreferences.getString("friends", "[]");
        JSONArray friendsArray = null;
        try {
            friendsArray = new JSONArray(friendsJson);
        } catch (JSONException e) {
            Log.d("TAG", "saveFriendLocally: "+e.getMessage());
        }

        // Add new friend
        JSONObject friendObject = new JSONObject();
        try {
            friendObject.put("id", friendId);
            friendObject.put("name", friendName);
            friendsArray.put(friendObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Save updated friends list
        editor.putString("friends", friendsArray.toString());
        editor.apply();

    }

    private void sendFriendRequest(String userid, String name) {

        SharedPreferences sp =  getSharedPreferences("app_prefs", MODE_PRIVATE);
        String userid2= sp.getString("user_id","0");
        String username= sp.getString("namethis","0");
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String currentUserId =userid2;


            // Add request in the target user’s "friends" sub-collection
            Map<String, Object> request = new HashMap<>();
            request.put("name", username); // Your logged-in user’s name
            request.put("status", "pending");
            request.put("requestSentDate", new Date().toString());

            db.collection("users")
                    .document(userid)
                    .collection("friends")
                    .document(currentUserId)
                    .set(request)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(addingmobile.this, "Request sent successfully!", Toast.LENGTH_SHORT).show();
                    });

            // Track outgoing request in current user’s "friends" sub-collection
            Map<String, Object> outgoingRequest = new HashMap<>();
            outgoingRequest.put("name", name);
            outgoingRequest.put("status", "pending");
            outgoingRequest.put("requestSentDate", new Date().toString());

            db.collection("users")
                    .document(currentUserId)
                    .collection("friends")
                    .document(userid)
                    .set(outgoingRequest);
        }

        public String getCurrentUserId(){
            SharedPreferences sp =  getSharedPreferences("app_prefs", MODE_PRIVATE);
            String userid2= sp.getString("user_id","0");
            return userid2;
        }
        public String getCurrentUserName() {
            SharedPreferences sp =  getSharedPreferences("app_prefs", MODE_PRIVATE);
            String userid2= sp.getString("namethis","0");
            return userid2;
        }


}