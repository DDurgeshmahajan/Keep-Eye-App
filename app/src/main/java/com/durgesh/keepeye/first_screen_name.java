package com.durgesh.keepeye;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class first_screen_name extends AppCompatActivity {


    EditText editText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
//        FirebaseApp.initializeApp(first_screen_name.this);
        setContentView(R.layout.activity_first_screen_name);

        SharedPreferences sp =  getSharedPreferences("app_prefs", MODE_PRIVATE);
        String log= sp.getString("log","0");

        if(log.equals("1")){
            Intent i1=new Intent(first_screen_name.this, MainActivity.class);
            startActivity(i1);
            finish();
        }


        Button button = findViewById(R.id.button);
         editText = findViewById(R.id.editname);
        editText.requestFocus();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!editText.getText().toString().isEmpty()){
                    saveUserDetails(editText.getText().toString(), "Online");
                }
                else {
                    Toast.makeText(first_screen_name.this, "Enter Name First", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveUserDetails(String userName, String deviceStatus) {

        SweetAlertDialog pDialog = new SweetAlertDialog(first_screen_name.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#ffff8800"));
        pDialog.setTitleText("Loadingad");
        pDialog.setCancelable(false);
        pDialog.setTitleText("Loading...");
        pDialog.show();

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", userName);
        FirebaseFirestore db;
        db = FirebaseFirestore.getInstance();
        // Get current date and time
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        userData.put("loginDate", currentDate);
        userData.put("dateAdded", currentDate);
        userData.put("deviceStatus", deviceStatus);

        // Save to Firestore and get document ID
        db.collection("users")
                .add(userData)
                .addOnSuccessListener(documentReference -> {
                    // Retrieve the generated document ID
                    String documentId = documentReference.getId();
                    Toast.makeText(first_screen_name.this, "Your ID: " + documentId, Toast.LENGTH_LONG).show();

                    // Save the ID locally for further use
                    saveUserIdLocally(documentId);
                    pDialog.dismissWithAnimation();

                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    pDialog.dismissWithAnimation();
                    Toast.makeText(first_screen_name.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });


    }

    private void saveUserIdLocally(String documentId) {

        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putString("user_id", documentId)
                .putString("log","1")
                .putString("namethis",editText.getText().toString())
                .apply();
        Toast.makeText(this, "Name Saved ! ", Toast.LENGTH_SHORT).show();
        
        Intent intent= new Intent(first_screen_name.this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}