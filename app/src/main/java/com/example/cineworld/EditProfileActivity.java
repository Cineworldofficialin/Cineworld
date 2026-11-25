
package com.example.cineworld;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editName, editEmail, editPhone;
    private EditText editDob, editGender, editCountry, editBio;

    private ImageView profileImage;
    private TextView profilePercentage;
    private ProgressBar profileProgress;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ActivityResultLauncher<String> photoPicker;
    private Uri selectedPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupPhotoPicker();
        loadProfile();

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        findViewById(R.id.changePhoto).setOnClickListener(v ->
                photoPicker.launch("image/*"));

        findViewById(R.id.removePhoto).setOnClickListener(v -> {
            selectedPhotoUri = null;
            profileImage.setImageResource(
                    android.R.drawable.ic_menu_myplaces);
            Toast.makeText(this, "Photo removed from preview",
                    Toast.LENGTH_SHORT).show();
        });

        editDob.setOnClickListener(v -> showDatePicker());

        findViewById(R.id.saveProfile).setOnClickListener(v ->
                saveProfile());

        findViewById(R.id.changePassword).setOnClickListener(v ->
                changePassword());

        findViewById(R.id.logoutDevices).setOnClickListener(v ->
                showLogoutDevicesInfo());

        findViewById(R.id.notificationSettings).setOnClickListener(v ->
                showOptions("Notifications",
                        new String[]{"On", "Off"}));

        findViewById(R.id.languageSettings).setOnClickListener(v ->
                showOptions("Language",
                        new String[]{"English", "Hindi"}));

        findViewById(R.id.qualitySettings).setOnClickListener(v ->
                showOptions("Video Quality",
                        new String[]{"Auto", "720p", "1080p", "4K"}));

        findViewById(R.id.deleteAccount).setOnClickListener(v ->
                confirmDeleteAccount());
    }

    private void initializeViews() {

        profileImage = findViewById(R.id.profileImage);
        profilePercentage = findViewById(R.id.profilePercentage);
        profileProgress = findViewById(R.id.profileProgress);

        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editDob = findViewById(R.id.editDob);
        editGender = findViewById(R.id.editGender);
        editCountry = findViewById(R.id.editCountry);
        editBio = findViewById(R.id.editBio);
    }

    private void setupPhotoPicker() {

        photoPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPhotoUri = uri;
                        profileImage.setImageURI(uri);
                    }
                }
        );
    }

    private void loadProfile() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please login first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (user.getDisplayName() != null) {
            editName.setText(user.getDisplayName());
        }

        if (user.getEmail() != null) {
            editEmail.setText(user.getEmail());
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {
                        editName.setText(getValue(document, "name"));
                        editEmail.setText(getValue(document, "email"));
                        editPhone.setText(getValue(document, "phone"));
                        editDob.setText(getValue(document, "dob"));
                        editGender.setText(getValue(document, "gender"));
                        editCountry.setText(getValue(document, "country"));
                        editBio.setText(getValue(document, "bio"));
                    }

                    updateCompletion();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Profile loading failed",
                                Toast.LENGTH_SHORT).show());
    }

    private String getValue(
            com.google.firebase.firestore.DocumentSnapshot document,
            String key) {

        String value = document.getString(key);
        return value == null ? "" : value;
    }

    private void saveProfile() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please login first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> profile = new HashMap<>();

        profile.put("name", name);
        profile.put("email", email);
        profile.put("phone", editPhone.getText().toString().trim());
        profile.put("dob", editDob.getText().toString().trim());
        profile.put("gender", editGender.getText().toString().trim());
        profile.put("country", editCountry.getText().toString().trim());
        profile.put("bio", editBio.getText().toString().trim());

        db.collection("users")
                .document(user.getUid())
                .set(profile, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    user.updateProfile(
                            new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build()
                    );

                    Toast.makeText(this, "Profile saved successfully!",
                            Toast.LENGTH_LONG).show();

                    updateCompletion();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void updateCompletion() {

        int filled = 0;

        EditText[] fields = {
                editName, editEmail, editPhone,
                editDob, editGender, editCountry, editBio
        };

        for (EditText field : fields) {
            if (!field.getText().toString().trim().isEmpty()) {
                filled++;
            }
        }

        int percent = (filled * 100) / fields.length;

        profileProgress.setProgress(percent);
        profilePercentage.setText(percent + "% Complete");
    }

    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) ->
                        editDob.setText(
                                String.format("%02d/%02d/%04d",
                                        day, month + 1, year)
                        ),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMaxDate(
                System.currentTimeMillis());

        dialog.show();
    }

    private void changePassword() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Login required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(user.getEmail())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this,
                                "Password reset email sent",
                                Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void showOptions(String title, String[] options) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(options, (dialog, which) ->
                        Toast.makeText(this,
                                title + ": " + options[which],
                                Toast.LENGTH_SHORT).show())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDevicesInfo() {

        new AlertDialog.Builder(this)
                .setTitle("Logout from All Devices")
                .setMessage("This feature requires secure server-side session management. It is not enabled yet.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmDeleteAccount() {

        new AlertDialog.Builder(this)
                .setTitle("Delete Account?")
                .setMessage("This action is permanent. You may need to login again before deleting your account.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue", (dialog, which) ->
                        deleteAccount())
                .show();
    }

    private void deleteAccount() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .delete()
                .addOnSuccessListener(unused -> {

                    user.delete()
                            .addOnSuccessListener(done -> {
                                Toast.makeText(this,
                                        "Account deleted",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Please login again before deleting: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Delete failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}
