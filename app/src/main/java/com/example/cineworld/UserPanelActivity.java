package com.example.cineworld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserPanelActivity extends AppCompatActivity {

    private static final String PROFILE_PREFS = "cineworld_profile";
    private static final int GOLD = 0xFFFFC107;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int DARK = 0xFF191920;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView nameView;
    private TextView emailView;
    private TextView memberIdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_panel);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        findViewById(R.id.userPanelBack).setOnClickListener(v -> finish());

        nameView = findViewById(R.id.userPanelName);
        emailView = findViewById(R.id.userPanelEmail);
        memberIdView = findViewById(R.id.userPanelMemberId);

        findViewById(R.id.userPanelEdit).setOnClickListener(v -> showEditProfile());
        findViewById(R.id.userPanelWatchlist).setOnClickListener(v ->
                showInfo("MY WATCHLIST", "Your saved movies and shows will appear here."));
        findViewById(R.id.userPanelHistory).setOnClickListener(v ->
                showInfo("WATCH HISTORY", "Your recently watched movies and shows will appear here."));
        findViewById(R.id.userPanelDownloads).setOnClickListener(v -> openMainTab(3));
        findViewById(R.id.userPanelSettings).setOnClickListener(v ->
                showInfo("APP SETTINGS", "CineWorld app settings will be available here."));
        findViewById(R.id.userPanelHelp).setOnClickListener(v ->
                showInfo("HELP & SUPPORT", "For help with your CineWorld account, contact CineWorld support."));
        findViewById(R.id.userPanelPrivacy).setOnClickListener(v ->
                showInfo("PRIVACY POLICY", "CineWorld privacy information."));
        findViewById(R.id.userPanelLogout).setOnClickListener(v -> logout());
        findViewById(R.id.userPanelLogin).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_auth", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        refreshProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProfile();
    }

    private void refreshProfile() {
        SharedPreferences prefs = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        String savedName = prefs.getString("profile_name", "CineWorld User");
        String savedEmail = prefs.getString("profile_email", "Welcome to CineWorld");

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            nameView.setText(savedName);
            emailView.setText(savedEmail);
            memberIdView.setText("GUEST MEMBER");
            findViewById(R.id.userPanelLogin).setVisibility(android.view.View.VISIBLE);
            findViewById(R.id.userPanelLogout).setVisibility(android.view.View.GONE);
            return;
        }

        String fallbackName = user.getDisplayName();
        if (fallbackName == null || fallbackName.trim().isEmpty()) {
            fallbackName = savedName;
        }
        String fallbackEmail = user.getEmail() == null ? savedEmail : user.getEmail();

        nameView.setText(fallbackName);
        emailView.setText(fallbackEmail);
        memberIdView.setText("CINEWORLD MEMBER  •  " + shortId(user.getUid()));

        findViewById(R.id.userPanelLogin).setVisibility(android.view.View.GONE);
        findViewById(R.id.userPanelLogout).setVisibility(android.view.View.VISIBLE);

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) return;
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    if (name != null && !name.trim().isEmpty()) nameView.setText(name);
                    if (email != null && !email.trim().isEmpty()) emailView.setText(email);
                });
    }

    private String shortId(String uid) {
        if (uid == null) return "MEMBER";
        return uid.length() <= 8 ? uid.toUpperCase()
                : uid.substring(0, 8).toUpperCase();
    }

    private void showEditProfile() {
        SharedPreferences prefs = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 12, 24, 8);

        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setHint("Your name");
        nameInput.setTextColor(WHITE);
        nameInput.setHintTextColor(GRAY);
        nameInput.setText(nameView.getText());
        layout.addView(nameInput);

        EditText emailInput = new EditText(this);
        emailInput.setSingleLine(true);
        emailInput.setHint("Your email");
        emailInput.setTextColor(WHITE);
        emailInput.setHintTextColor(GRAY);
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        String currentEmail = emailView.getText().toString();
        if ("Welcome to CineWorld".equals(currentEmail)) currentEmail = "";
        emailInput.setText(currentEmail);
        LinearLayout.LayoutParams emailParams =
                new LinearLayout.LayoutParams(-1, -2);
        emailParams.topMargin = 12;
        layout.addView(emailInput, emailParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("EDIT PROFILE")
                .setView(layout)
                .setPositiveButton("SAVE", null)
                .setNegativeButton("CANCEL", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(GOLD);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(GOLD);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                String email = emailInput.getText().toString().trim();

                if (name.isEmpty()) {
                    nameInput.setError("Name required");
                    return;
                }
                if (!email.isEmpty()
                        && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailInput.setError("Enter a valid email");
                    return;
                }

                String shownEmail = email.isEmpty() ? "Welcome to CineWorld" : email;
                prefs.edit().putString("profile_name", name)
                        .putString("profile_email", shownEmail).apply();
                nameView.setText(name);
                emailView.setText(shownEmail);

                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("name", name);
                    db.collection("users").document(user.getUid()).update(updates);
                }

                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("LOGOUT")
                .setMessage("Are you sure you want to logout of CineWorld?")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("LOGOUT", (d, which) -> {
                    auth.signOut();
                    getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE).edit()
                            .putString("profile_name", "CineWorld User")
                            .putString("profile_email", "Welcome to CineWorld")
                            .apply();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    refreshProfile();
                })
                .show();
    }

    private void openMainTab(int tab) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("open_tab", tab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showInfo(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
