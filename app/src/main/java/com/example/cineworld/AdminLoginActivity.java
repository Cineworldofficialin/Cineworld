package com.example.cineworld;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private TextView statusText;
    private Button loginButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private boolean loginInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admin_login);

        emailInput = findViewById(R.id.adminEmail);
        passwordInput = findViewById(R.id.adminPassword);
        statusText = findViewById(R.id.adminStatus);
        loginButton = findViewById(R.id.adminLoginButton);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (emailInput == null ||
                passwordInput == null ||
                statusText == null ||
                loginButton == null) {

            finish();
            return;
        }

        loginButton.setOnClickListener(v -> loginAdmin());
    }

    private void loginAdmin() {

        if (loginInProgress) return;

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty()) {
            emailInput.setError("Email भरें");
            emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password भरें");
            passwordInput.requestFocus();
            return;
        }

        loginInProgress = true;

        loginButton.setEnabled(false);
        loginButton.setText("PLEASE WAIT...");
        statusText.setText("Verifying Admin Account...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (!task.isSuccessful()) {

                        String error = "Unknown error";

                        if (task.getException() != null) {
                            error = task.getException().getMessage();
                        }

                        showLoginError("Login Failed: " + error);
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        showLoginError(
                                "Authentication session unavailable."
                        );
                        return;
                    }

                    verifyAdmin(user);
                });
    }

    private void verifyAdmin(FirebaseUser user) {

        statusText.setText("Checking Admin Permissions...");

        db.collection("admins")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(this, document -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (document.exists() &&
                            "admin".equalsIgnoreCase(
                                    document.getString("role"))) {

                        openAdminDashboard();

                    } else {

                        auth.signOut();

                        showLoginError(
                                "Access Denied: यह Admin Account नहीं है।"
                        );
                    }
                })
                .addOnFailureListener(this, error -> {

                    if (isFinishing() || isDestroyed()) return;

                    auth.signOut();

                    String message = error.getMessage();

                    if (message == null || message.trim().isEmpty()) {
                        message = "Unable to verify Admin account.";
                    }

                    showLoginError("Database Error: " + message);
                });
    }

    private void openAdminDashboard() {

        if (isFinishing() || isDestroyed()) return;

        statusText.setText("Opening Admin Dashboard...");

        try {

            Intent intent = new Intent(
                    AdminLoginActivity.this,
                    AdminDashboardActivity.class
            );

            startActivity(intent);

            finish();

        } catch (Exception e) {

            showLoginError(
                    "Dashboard Error: " + e.getMessage()
            );
        }
    }

    private void showLoginError(String message) {

        loginInProgress = false;

        if (isFinishing() || isDestroyed()) return;

        loginButton.setEnabled(true);
        loginButton.setText("ADMIN LOGIN");

        statusText.setText(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
