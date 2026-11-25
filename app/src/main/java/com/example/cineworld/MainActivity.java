package com.example.cineworld;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import android.graphics.Color;

import com.hbb20.CountryCodePicker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cineworld.databinding.ActivityMainBinding;
import com.bumptech.glide.Glide;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@UnstableApi
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FrameLayout skeletonOverlay;

    private TextView profileNameView, profileEmailView;
    private ImageView profileAvatarView;
    private static final int REQUEST_PROFILE_PHOTO = 7401;
    private static final int RC_GOOGLE_SIGN_IN = 7402;
    private ExoPlayer player;
    private DownloadManager downloadManager;

    private Handler downloadHandler =
            new Handler(Looper.getMainLooper());

    private Runnable downloadRunnable;

    private TextView[] navigationItems;
    private int currentTab = 0;
    private boolean tabTransitionRunning = false;
    private String selectedMovieCategory = "All";
    private float pullStartY = 0f;
    private float pullStartX = 0f;
    private boolean pullRefreshTriggered = false;
    private boolean horizontalDragActive = false;
    private boolean swipePreviewPrepared = false;
    private int swipePreviewTab = -1;
    private View swipeCurrentView;
    private View swipeNextView;
    private float swipeCurrentDx = 0f;
    private ProgressBar pullRefreshProgress;
    private final Handler pullRefreshHandler =
            new Handler(Looper.getMainLooper());
    private LinearLayout downloadsList;
    private TextView storageText;
    private ProgressBar storageProgress;
    private Switch wifiSwitch, mobileDataSwitch;
    private Spinner qualitySpinner;

    private FrameLayout authOverlay;
    private FrameLayout languageOverlay;
    private boolean authGateActive = false;
    private boolean authBusy = false;
    private boolean offlineMode = false;

    // ADMIN SESSION VERIFICATION
    private boolean sessionCheckPending = false;
    private boolean sessionCheckFailed = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String KEY_WIFI = "allow_wifi";
    private static final String KEY_MOBILE = "allow_mobile";
    private static final String KEY_QUALITY = "download_quality";

    private final int gold = 0xFFFFC107;
    private final int gray = 0xFFAAAAAA;
    private final int white = 0xFFFFFFFF;
    private final int dark = 0xFF191920;
    private final int black = 0xFF101018;

    private final ArrayList<Long> downloadIds = new ArrayList<>();

    private static final String INDIA_STATES_URL =
            "https://raw.githubusercontent.com/codingmation/indian-states-districts/main/data/india_states_districts.min.json";
    private static final String LOCATION_PREFS = "cineworld_location_data";
    private final ExecutorService locationExecutor = Executors.newSingleThreadExecutor();

    private long initialFreeBytes = 0;
    private long storageLimitBytes = 0;

    private static final String PREFS = "cineworld_downloads";
    private static final String PROFILE_PREFS = "cineworld_profile";
    private static final String LANGUAGE_PREFS = "cineworld_audio_languages";

    // Big Buck Bunny 4K UHD (3840x2160, H.264 MP4, 60 fps).
    // Original film by Blender Foundation; CC BY 3.0 attribution is shown in the demo card.
    private static final String DEMO_URL =
            "https://drive.google.com/uc?export=download&id=18cLDFgHKfHuq7C17gmqHYsH8TjNvk-bt";

    private static class ContentItem {
        String title, genre, description, image, videoUrl;
        boolean series;

        ContentItem(String title, String genre, String description,
                    String image, boolean series) {
            this(title, genre, description, image, series, "");
        }

        ContentItem(String title, String genre, String description,
                    String image, boolean series, String videoUrl) {
            this.title = title;
            this.genre = genre;
            this.description = description;
            this.image = image;
            this.series = series;
            this.videoUrl = videoUrl == null ? "" : videoUrl.trim();
        }
    }

    private final ArrayList<ContentItem> movies =
            new ArrayList<>(Arrays.asList(

                    new ContentItem("Shadow Strike", "Action • Thriller",
                            "A mysterious hero faces a dangerous enemy in this demo action movie.",
                            "https://images.unsplash.com/photo-1509347528160-9a9e33742cdb?w=500",
                            false),

                    new ContentItem("Lost Kingdom", "Adventure • Fantasy",
                            "An adventure through a forgotten kingdom. Demo movie description.",
                            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500",
                            false),

                    new ContentItem("Dark Mystery", "Mystery • Thriller",
                            "A detective investigates a mystery that changes everything.",
                            "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500",
                            false),

                    new ContentItem("Final Mission", "Action • Drama",
                            "A special team takes on its most difficult mission.",
                            "https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=500",
                            false)
            ));

    private final ArrayList<ContentItem> shows =
            new ArrayList<>(Arrays.asList(

                    new ContentItem("The Hidden Truth", "Mystery • Drama",
                            "A mystery series about secrets, family and unexpected discoveries.",
                            "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=500",
                            true),

                    new ContentItem("City of Dreams", "Drama • Romance",
                            "A story of ambition and relationships in a big city.",
                            "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=500",
                            true),

                    new ContentItem("Survival Zone", "Action • Adventure",
                            "A group of survivors must work together to stay alive.",
                            "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=500",
                            true)
            ));

    // ----------------------------------------------------
    // ACTIVITY START
    // ----------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use the skeleton background even during the Activity's first frame.
        getWindow().setBackgroundDrawable(new ColorDrawable(0xFF101018));

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        binding.getRoot().setBackgroundColor(black);
        setContentView(binding.getRoot());
        offlineMode = getIntent().getBooleanExtra("offline_mode", false);

        // Show a simple skeleton page immediately to cover the startup gap.
        showSkeletonLoading();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupDownloadManager();
        setupPlayer();
        setupNavigation();
        setupPullToRefresh();
        setupHeader();
        setupHome();
        setupCatalogScreens();
        setupDownloadsScreen();
        setupProfileScreen();
        loadAllImages();

        startDownloadMonitoring();

        FirebaseUser user = mAuth.getCurrentUser();

        if (offlineMode) {
            // Offline entry is deliberately limited to local Downloads/Profile.
            // It does not grant or assume an Admin/User cloud role.
            authGateActive = false;
            sessionCheckPending = false;
            sessionCheckFailed = false;
            setAppContentLocked(false);
            hideSkeletonLoading();
            selectTab(3);
            showMessage("Offline mode: only saved content and local settings are available.");
            return;
        }

        if (user == null) {
            hideSkeletonLoading();
            authGateActive = true;
            setAppContentLocked(true);
            showAuthScreen("login");
        } else {
            // Do not open normal user screen before checking admin role.
            authGateActive = true;
            setAppContentLocked(true);
            verifyAndRouteSession(user);
        }
    }

    // ----------------------------------------------------
    // SIMPLE SKELETON LOADING SCREEN
    // ----------------------------------------------------

    private void showSkeletonLoading() {

        if (skeletonOverlay != null) return;

        ViewGroup root = findViewById(android.R.id.content);
        if (root == null) return;

        skeletonOverlay = new FrameLayout(this);
        skeletonOverlay.setBackgroundColor(black);
        skeletonOverlay.setClickable(true);
        skeletonOverlay.setFocusable(true);

        LinearLayout loadingBox = new LinearLayout(this);
        loadingBox.setOrientation(LinearLayout.VERTICAL);
        loadingBox.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams boxParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER);

        skeletonOverlay.addView(loadingBox, boxParams);

        TextView title = makeText("CINEWORLD", 28, gold, true);
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0.12f);
        loadingBox.addView(title);

        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);

        if (Build.VERSION.SDK_INT >= 21) {
            progress.setIndeterminateTintList(
                    android.content.res.ColorStateList.valueOf(gold));
        }

        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(dp(35), dp(35));
        progressParams.topMargin = dp(25);
        loadingBox.addView(progress, progressParams);

        TextView message = makeText(
                "PREPARING YOUR CINEMA EXPERIENCE", 10, gray, false);
        message.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = dp(15);
        loadingBox.addView(message, messageParams);

        root.addView(skeletonOverlay,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void startSkeletonPulse(View view, long delay) {
        android.view.animation.AlphaAnimation pulse =
                new android.view.animation.AlphaAnimation(0.72f, 1.0f);
        pulse.setDuration(850);
        pulse.setStartOffset(delay);
        pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
        pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
        view.startAnimation(pulse);
    }

    private void hideSkeletonLoading() {

        if (skeletonOverlay == null) return;

        FrameLayout overlay = skeletonOverlay;
        skeletonOverlay = null;

        overlay.animate()
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> {
                    overlay.clearAnimation();
                    ViewGroup parent = (ViewGroup) overlay.getParent();
                    if (parent != null) parent.removeView(overlay);
                })
                .start();
    }

    // ----------------------------------------------------
    // ADMIN SESSION VERIFICATION
    // ----------------------------------------------------

    private boolean isGoogleProvider(FirebaseUser user) {
        if (user == null || user.getProviderData() == null) return false;
        for (com.google.firebase.auth.UserInfo info : user.getProviderData()) {
            if ("google.com".equals(info.getProviderId())) return true;
        }
        return false;
    }

    private void verifyAndRouteSession(FirebaseUser user) {

        if (isFinishing() || isDestroyed()) return;

        if (user == null) {
            sessionCheckPending = false;
            sessionCheckFailed = false;
            authGateActive = true;
            setAppContentLocked(true);

            if (authOverlay == null) {
                showAuthScreen("login");
            }
            return;
        }

        if (!user.isEmailVerified() && !isGoogleProvider(user)) {
            sessionCheckPending = false;
            sessionCheckFailed = false;
            authGateActive = true;
            setAppContentLocked(true);
            showAuthScreen("verify");
            return;
        }

        if (sessionCheckPending) return;

        sessionCheckPending = true;
        sessionCheckFailed = false;

        authGateActive = true;
        setAppContentLocked(true);

        db.collection("admins")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (isFinishing() || isDestroyed()) return;

                    sessionCheckPending = false;

                    String role = document.getString("role");

                    if (document.exists() &&
                            role != null &&
                            "admin".equalsIgnoreCase(role.trim())) {

                        openAdminDashboard();

                    } else {
                        // Normal user session.
                        finishAuthentication();
                    }
                })
                .addOnFailureListener(error -> {

                    if (isFinishing() || isDestroyed()) return;

                    sessionCheckPending = false;
                    sessionCheckFailed = true;

                    authGateActive = true;
                    setAppContentLocked(true);

                    hideSkeletonLoading();
                    showMessage(
                            "Account verification failed. Check internet and reopen the app."
                    );
                });
    }

    private void openAdminDashboard() {

        if (isFinishing() || isDestroyed()) return;
        hideSkeletonLoading();

        sessionCheckPending = false;
        sessionCheckFailed = false;

        Intent intent = new Intent(
                MainActivity.this,
                AdminDashboardActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // ----------------------------------------------------
    // FULL SCREEN FIREBASE AUTHENTICATION
    // ----------------------------------------------------

    private void refreshEmailVerification(Button button) {
        FirebaseUser current = mAuth.getCurrentUser();
        if (current == null) {
            showAuthScreen("login");
            return;
        }
        button.setEnabled(false);
        button.setText("CHECKING...");
        current.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshed = mAuth.getCurrentUser();
            if (refreshed != null && refreshed.isEmailVerified()) {
                button.setText("VERIFIED — OPENING CINEWORLD...");
                verifyAndRouteSession(refreshed);
            } else {
                button.setEnabled(true);
                button.setText("I HAVE VERIFIED — CONTINUE");
                showMessage(task.isSuccessful()
                        ? "Email is not verified yet. Open the link in your email, then try again."
                        : "Could not check verification. Please check your internet and try again.");
            }
        });
    }

    private void showAuthScreen(String screen) {

        if (isFinishing() || isDestroyed()) return;

        FirebaseUser existingUser = mAuth.getCurrentUser();

        if (existingUser != null && !"verify".equals(screen)) {
            if (!sessionCheckPending && !sessionCheckFailed) {
                verifyAndRouteSession(existingUser);
            }
            return;
        }

        authGateActive = true;
        setAppContentLocked(true);

        ViewGroup content = findViewById(android.R.id.content);

        if (authOverlay != null) {
            ViewGroup oldParent = (ViewGroup) authOverlay.getParent();
            if (oldParent != null) oldParent.removeView(authOverlay);
            authOverlay = null;
        }

        authOverlay = new FrameLayout(this);
        authOverlay.setBackgroundColor(black);
        authOverlay.setClickable(true);
        authOverlay.setFocusable(true);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);

        FrameLayout.LayoutParams scrollParams =
                new FrameLayout.LayoutParams(-1, -1);

        authOverlay.addView(scroll, scrollParams);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(25), dp(35), dp(25), dp(30));

        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        TextView logo = makeText("CINEWORLD", 35, gold, true);
        logo.setGravity(Gravity.CENTER);
        logo.setLetterSpacing(0.08f);
        page.addView(logo);

        TextView tagline = makeText(
                "YOUR MOVIES. YOUR WORLD.", 11, gray, true);

        tagline.setGravity(Gravity.CENTER);
        tagline.setPadding(0, dp(5), 0, dp(25));
        page.addView(tagline);

        View line = new View(this);
        line.setBackgroundColor(gold);

        LinearLayout.LayoutParams lineParams =
                new LinearLayout.LayoutParams(dp(65), dp(3));

        lineParams.gravity = Gravity.CENTER;
        lineParams.bottomMargin = dp(25);
        page.addView(line, lineParams);

        LinearLayout card = verticalLayout();
        card.setPadding(dp(20), dp(24), dp(20), dp(22));
        card.setBackground(
                roundedBackground(0xFF191920, 0xFF806019, 20));

        page.addView(card,
                new LinearLayout.LayoutParams(-1, -2));

        if ("register".equals(screen)) {

            addAuthHeading(card, "CREATE NEW ACCOUNT",
                    "Join CineWorld and enjoy your entertainment.");

            EditText name = authInput("Full name");
            name.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            card.addView(name);

            TextView countryLabel = makeText("COUNTRY & MOBILE NUMBER", 11, gray, true);
            countryLabel.setPadding(0, dp(14), 0, dp(7));
            card.addView(countryLabel);

            LinearLayout phoneRow = new LinearLayout(this);
            phoneRow.setOrientation(LinearLayout.HORIZONTAL);
            phoneRow.setGravity(Gravity.CENTER_VERTICAL);
            phoneRow.setPadding(dp(8), 0, dp(8), 0);
            phoneRow.setBackground(roundedBackground(0xFF25252F, 0xFF514629, 12));
            LinearLayout.LayoutParams phoneRowParams =
                    new LinearLayout.LayoutParams(-1, dp(56));
            phoneRowParams.topMargin = dp(4);
            card.addView(phoneRow, phoneRowParams);

            CountryCodePicker countryPicker = new CountryCodePicker(this);
            countryPicker.setDefaultCountryUsingNameCode("IN");
            countryPicker.resetToDefaultCountry();
            countryPicker.setCountryPreference("IN");
            countryPicker.setContentColor(white);
            phoneRow.addView(countryPicker,
                    new LinearLayout.LayoutParams(-2, -1));

            EditText phone = new EditText(this);
            phone.setSingleLine(true);
            phone.setTextColor(white);
            phone.setHintTextColor(0xFF888888);
            phone.setHint("Mobile number");
            phone.setTextSize(14);
            phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            phone.setPadding(dp(8), dp(8), dp(5), dp(8));
            phone.setBackgroundColor(Color.TRANSPARENT);
            phoneRow.addView(phone,
                    new LinearLayout.LayoutParams(0, -1, 1f));
            countryPicker.registerCarrierNumberEditText(phone);

            TextView locationLabel = makeText("LOCATION DETAILS", 11, gray, true);
            locationLabel.setPadding(0, dp(16), 0, dp(5));
            card.addView(locationLabel);

            LinearLayout indiaLocationBox = new LinearLayout(this);
            indiaLocationBox.setOrientation(LinearLayout.VERTICAL);
            card.addView(indiaLocationBox,
                    new LinearLayout.LayoutParams(-1, -2));

            Spinner stateSpinner = authSpinner();
            Spinner districtSpinner = authSpinner();
            addAuthInputSpacing(indiaLocationBox, stateSpinner);
            addAuthInputSpacing(indiaLocationBox, districtSpinner);

            LinearLayout otherLocationBox = new LinearLayout(this);
            otherLocationBox.setOrientation(LinearLayout.VERTICAL);
            card.addView(otherLocationBox,
                    new LinearLayout.LayoutParams(-1, -2));
            EditText otherState = authInput("State / Province");
            EditText otherDistrict = authInput("District / City");
            addAuthInputSpacing(otherLocationBox, otherState);
            addAuthInputSpacing(otherLocationBox, otherDistrict);
            otherLocationBox.setVisibility(View.GONE);

            loadIndianStates(stateSpinner, districtSpinner);
            stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    updateIndianDistricts(position, districtSpinner);
                }
                @Override public void onNothingSelected(AdapterView<?> parent) { }
            });

            countryPicker.setOnCountryChangeListener(() -> {
                boolean isIndia = "IN".equalsIgnoreCase(
                        countryPicker.getSelectedCountryNameCode());
                indiaLocationBox.setVisibility(isIndia ? View.VISIBLE : View.GONE);
                otherLocationBox.setVisibility(isIndia ? View.GONE : View.VISIBLE);
                if (isIndia) loadIndianStates(stateSpinner, districtSpinner);
            });

            EditText email = authInput("Email address");
            email.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            addAuthInputSpacing(card, email);

            EditText password = authInput("Password (minimum 6 characters)");
            password.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            addAuthInputSpacing(card, password);

            EditText confirm = authInput("Confirm password");
            confirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            addAuthInputSpacing(card, confirm);

            Button register = authButton("CREATE ACCOUNT");
            addAuthButtonSpacing(card, register);

            register.setOnClickListener(v -> {
                String userName = name.getText().toString().trim();
                String userEmail = email.getText().toString().trim();
                String userPassword = password.getText().toString();
                String confirmPassword = confirm.getText().toString();
                String selectedCountry = countryPicker.getSelectedCountryEnglishName();
                String selectedCountryCode = countryPicker.getSelectedCountryCodeWithPlus();
                String userPhone = countryPicker.getFullNumberWithPlus();
                boolean isIndia = "IN".equalsIgnoreCase(
                        countryPicker.getSelectedCountryNameCode());
                String userState = isIndia
                        ? (stateSpinner.getSelectedItemPosition() > 0
                            ? String.valueOf(stateSpinner.getSelectedItem()) : "")
                        : otherState.getText().toString().trim();
                String userDistrict = isIndia
                        ? (districtSpinner.getSelectedItemPosition() > 0
                            ? String.valueOf(districtSpinner.getSelectedItem()) : "")
                        : otherDistrict.getText().toString().trim();

                if (userName.isEmpty()) { name.setError("Enter your name"); return; }
                if (phone.getText().toString().trim().isEmpty() ||
                        !countryPicker.isValidFullNumber()) {
                    phone.setError("Enter a valid mobile number"); return;
                }
                if (userState.isEmpty()) {
                    if (isIndia) showMessage("Please select your State / UT.");
                    else otherState.setError("Enter your State / Province");
                    return;
                }
                if (userDistrict.isEmpty()) {
                    if (isIndia) showMessage("Please select your District.");
                    else otherDistrict.setError("Enter your District / City");
                    return;
                }
                if (!isValidEmail(userEmail)) { email.setError("Enter a valid email"); return; }
                if (userPassword.length() < 6) { password.setError("Minimum 6 characters required"); return; }
                if (!userPassword.equals(confirmPassword)) { confirm.setError("Passwords do not match"); return; }
                if (authBusy) return;

                authBusy = true;
                register.setEnabled(false);
                register.setText("CREATING ACCOUNT...");

                mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                        .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        authBusy = false;
                        register.setEnabled(true);
                        register.setText("CREATE ACCOUNT");
                        showMessage(authError(task.getException()));
                        return;
                    }

                    FirebaseUser newUser = mAuth.getCurrentUser();
                    if (newUser == null) {
                        authBusy = false;
                        register.setEnabled(true);
                        register.setText("CREATE ACCOUNT");
                        showMessage("Account created, but session unavailable.");
                        return;
                    }

                    Map<String, Object> profile = new HashMap<>();
                    profile.put("uid", newUser.getUid());
                    profile.put("name", userName);
                    profile.put("email", userEmail);
                    profile.put("mobile", userPhone);
                    profile.put("country", selectedCountry);
                    profile.put("countryCode", selectedCountryCode);
                    profile.put("state", userState);
                    profile.put("district", userDistrict);
                    profile.put("createdAt", FieldValue.serverTimestamp());
                    saveLocalProfile(userName, userEmail);
                    getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE).edit()
                            .putString("profile_mobile", userPhone)
                            .putString("profile_country", selectedCountry)
                            .putString("profile_country_code", selectedCountryCode)
                            .putString("profile_state", userState)
                            .putString("profile_district", userDistrict).apply();

                    db.collection("users").document(newUser.getUid()).set(profile)
                            .addOnCompleteListener(profileTask ->
                        newUser.sendEmailVerification().addOnCompleteListener(mailTask -> {
                            authBusy = false;
                            register.setEnabled(true);
                            register.setText("CREATE ACCOUNT");
                            if (mailTask.isSuccessful()) {
                                showAuthScreen("verify");
                                showMessage("Verification link sent to " + userEmail +
                                        ". Open Gmail and verify your email.");
                            } else {
                                showAuthScreen("verify");
                                showMessage("Account created, but email could not be sent: " +
                                        authError(mailTask.getException()));
                            }
                        })
                    );
                });
            });

            addAuthTextButton(card, "Already have an account? LOGIN",
                    () -> showAuthScreen("login"));

        } else if ("verify".equals(screen)) {

            FirebaseUser verifyUser = mAuth.getCurrentUser();
            String verifyEmail = verifyUser != null ? verifyUser.getEmail() : "your email";

            addAuthHeading(card, "VERIFY YOUR EMAIL",
                    "We sent a verification link to " + verifyEmail +
                    ". Open the email and tap Verify Email. Keep CineWorld open or return here after verification.");

            Button checkVerification = authButton("I HAVE VERIFIED — CONTINUE");
            addAuthButtonSpacing(card, checkVerification);
            checkVerification.setOnClickListener(v -> refreshEmailVerification(checkVerification));

            Button resendVerification = authButton("RESEND VERIFICATION LINK");
            addAuthButtonSpacing(card, resendVerification);
            resendVerification.setOnClickListener(v -> {
                FirebaseUser current = mAuth.getCurrentUser();
                if (current == null) {
                    showAuthScreen("login");
                    return;
                }
                resendVerification.setEnabled(false);
                current.sendEmailVerification().addOnCompleteListener(task -> {
                    resendVerification.setEnabled(true);
                    showMessage(task.isSuccessful()
                            ? "A new verification link has been sent."
                            : "Could not send link: " + authError(task.getException()));
                });
            });

            addAuthTextButton(card, "BACK TO LOGIN", () -> {
                mAuth.signOut();
                showAuthScreen("login");
            });

        } else if ("forgot".equals(screen)) {

            addAuthHeading(card, "FORGOT PASSWORD",
                    "Enter your registered email to receive a reset link.");

            EditText email = authInput("Registered email address");

            email.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

            card.addView(email);

            Button send = authButton("SEND RESET LINK");
            addAuthButtonSpacing(card, send);

            send.setOnClickListener(v -> {

                String userEmail = email.getText().toString().trim();

                if (!isValidEmail(userEmail)) {
                    email.setError("Enter a valid email");
                    return;
                }

                if (authBusy) return;

                authBusy = true;
                send.setEnabled(false);
                send.setText("SENDING...");

                mAuth.sendPasswordResetEmail(userEmail)
                        .addOnCompleteListener(task -> {

                            authBusy = false;
                            send.setEnabled(true);
                            send.setText("SEND RESET LINK");

                            if (task.isSuccessful()) {
                                showMessage(
                                        "Password reset link sent to your email.");

                                showAuthScreen("login");

                            } else {
                                showMessage(authError(task.getException()));
                            }
                        });
            });

            addAuthTextButton(card, "BACK TO LOGIN",
                    () -> showAuthScreen("login"));

        } else {

            addAuthHeading(card, "WELCOME BACK",
                    "Login to continue watching your favourite movies.");

            EditText email = authInput("Email address");
            email.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            card.addView(email);

            EditText password = authInput("Password");
            password.setInputType(
                    android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            addAuthInputSpacing(card, password);

            // Terms acceptance is required for both login methods.
            LinearLayout termsRow = new LinearLayout(this);
            termsRow.setOrientation(LinearLayout.HORIZONTAL);
            termsRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams termsRowParams =
                    new LinearLayout.LayoutParams(-1, -2);
            termsRowParams.topMargin = dp(12);
            card.addView(termsRow, termsRowParams);

            CheckBox termsCheck = new CheckBox(this);
            termsCheck.setText("I agree to");
            termsCheck.setTextColor(gray);
            termsCheck.setTextSize(12);
            termsCheck.setButtonTintList(
                    android.content.res.ColorStateList.valueOf(gold));
            termsRow.addView(termsCheck,
                    new LinearLayout.LayoutParams(-2, -2));

            TextView termsLink = makeText(
                    "Terms & Conditions", 12, gold, true);
            termsLink.setPadding(dp(3), dp(8), dp(4), dp(8));
            termsRow.addView(termsLink);
            termsLink.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Terms & Conditions")
                    .setMessage("By using CineWorld, you agree to use the app lawfully and not misuse its content or services. Please replace this sample text with your complete Terms & Conditions and Privacy Policy before publishing.")
                    .setPositiveButton("CLOSE", null)
                    .show());

            TextView forgot = makeText(
                    "Forgot Password?", 12, gold, true);
            forgot.setGravity(Gravity.END);
            forgot.setPadding(0, dp(12), 0, dp(8));
            forgot.setOnClickListener(v -> showAuthScreen("forgot"));
            card.addView(forgot);

            Button login = authButton("LOGIN");
            addAuthButtonSpacing(card, login);
            login.setEnabled(false);
            login.setAlpha(0.5f);

            Button googleButton = authButton("CONTINUE WITH GOOGLE");
            addAuthButtonSpacing(card, googleButton);
            googleButton.setEnabled(false);
            googleButton.setAlpha(0.5f);

            Runnable updateLoginState = () -> {
                boolean accepted = termsCheck.isChecked();
                boolean canLogin = accepted
                        && !email.getText().toString().trim().isEmpty()
                        && !password.getText().toString().isEmpty();
                login.setEnabled(canLogin && !authBusy);
                login.setAlpha(canLogin && !authBusy ? 1.0f : 0.5f);
                googleButton.setEnabled(accepted && !authBusy);
                googleButton.setAlpha(accepted && !authBusy ? 1.0f : 0.5f);
            };

            android.text.TextWatcher inputWatcher = new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
                @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                    updateLoginState.run();
                }
                @Override public void afterTextChanged(android.text.Editable e) { }
            };
            email.addTextChangedListener(inputWatcher);
            password.addTextChangedListener(inputWatcher);
            termsCheck.setOnCheckedChangeListener((buttonView, checked) ->
                    updateLoginState.run());

            login.setOnClickListener(v -> {
                if (!termsCheck.isChecked()) {
                    showMessage("Please accept Terms & Conditions first.");
                    return;
                }
                String userEmail = email.getText().toString().trim();
                String userPassword = password.getText().toString();
                if (!isValidEmail(userEmail)) {
                    showLoginError(email, password, login,
                            "Enter a valid email address.");
                    updateLoginState.run();
                    return;
                }
                if (userPassword.isEmpty()) {
                    showLoginError(email, password, login,
                            "Enter your password.");
                    updateLoginState.run();
                    return;
                }
                if (authBusy) return;
                authBusy = true;
                login.setEnabled(false);
                login.setText("PLEASE WAIT...");
                googleButton.setEnabled(false);
                mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                        .addOnCompleteListener(task -> {
                            authBusy = false;
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    if (!isGoogleProvider(user) && !user.isEmailVerified()) {
                                        user.sendEmailVerification();
                                        showAuthScreen("verify");
                                        showMessage("Please verify your email to continue.");
                                    } else {
                                        verifyAndRouteSession(user);
                                        showMessage("Login successful!");
                                    }
                                } else {
                                    login.setText("LOGIN");
                                    updateLoginState.run();
                                    showMessage("Login session unavailable.");
                                }
                            } else {
                                login.setText("LOGIN");
                                updateLoginState.run();
                                showLoginError(email, password, login,
                                        authError(task.getException()));
                            }
                        });
            });

            addAuthTextButton(card, "CREATE NEW ACCOUNT",
                    () -> showAuthScreen("register"));

            View divider = new View(this);
            divider.setBackgroundColor(0xFF514629);
            LinearLayout.LayoutParams dividerParams =
                    new LinearLayout.LayoutParams(-1, dp(1));
            dividerParams.topMargin = dp(20);
            dividerParams.bottomMargin = dp(16);
            card.addView(divider, dividerParams);

            TextView orText = makeText("OR", 11, gray, true);
            orText.setGravity(Gravity.CENTER);
            card.addView(orText);

            googleButton.setOnClickListener(v -> {
                if (!termsCheck.isChecked()) {
                    showMessage("Please accept Terms & Conditions first.");
                    return;
                }
                startGoogleSignIn();
            });
        }


        TextView footer = makeText(
                "SECURE ACCOUNT • CINEWORLD", 10, gray, true);

        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(25), 0, 0);
        page.addView(footer);

        content.addView(authOverlay,
                new ViewGroup.LayoutParams(-1, -1));
    }

    private void addAuthHeading(LinearLayout parent,
                                String title, String subtitle) {

        TextView heading = makeText(title, 21, white, true);
        heading.setGravity(Gravity.CENTER);
        parent.addView(heading);

        TextView description = makeText(subtitle, 12, gray, false);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, dp(8), 0, dp(22));
        parent.addView(description);
    }

    private Spinner authSpinner() {
        Spinner spinner = new Spinner(this);
        spinner.setBackground(roundedBackground(0xFF25252F, 0xFF514629, 12));
        spinner.setPadding(dp(10), 0, dp(10), 0);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(54)));
        return spinner;
    }

    private ArrayAdapter<String> locationSpinnerAdapter(ArrayList<String> items) {
        return new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(white);
                view.setTextSize(14);
                view.setPadding(dp(12), 0, dp(12), 0);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(0xFF202020);
                view.setTextSize(14);
                view.setPadding(dp(14), dp(12), dp(14), dp(12));
                return view;
            }
        };
    }

    private void loadIndianStates(Spinner stateSpinner, Spinner districtSpinner) {
        String cached = getSharedPreferences(LOCATION_PREFS, MODE_PRIVATE)
                .getString("india_states_json", null);
        if (cached != null) {
            try {
                applyIndianStates(new JSONArray(cached), stateSpinner, districtSpinner);
                return;
            } catch (Exception ignored) { }
        }

        ArrayAdapter<String> loadingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Loading States..."});
        loadingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stateSpinner.setAdapter(loadingAdapter);
        stateSpinner.setEnabled(false);

        locationExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(INDIA_STATES_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("Accept", "application/json");
                StringBuilder result = new StringBuilder();
                try (InputStream input = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);
                }
                String json = result.toString();
                JSONArray states = new JSONArray(json);
                getSharedPreferences(LOCATION_PREFS, MODE_PRIVATE).edit()
                        .putString("india_states_json", json).apply();
                runOnUiThread(() -> applyIndianStates(states, stateSpinner, districtSpinner));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    stateSpinner.setEnabled(false);
                    Toast.makeText(this,
                            "Could not load States. Check internet and reopen registration.",
                            Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void applyIndianStates(JSONArray states, Spinner stateSpinner,
                                   Spinner districtSpinner) {
        if (isFinishing() || isDestroyed()) return;
        ArrayList<String> names = new ArrayList<>();
        names.add("Select State / UT");
        for (int i = 0; i < states.length(); i++) {
            JSONObject state = states.optJSONObject(i);
            if (state != null) names.add(state.optString("name", ""));
        }
        ArrayAdapter<String> adapter = locationSpinnerAdapter(names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stateSpinner.setAdapter(adapter);
        stateSpinner.setEnabled(true);
        stateSpinner.setSelection(0);
        updateIndianDistricts(0, districtSpinner);
    }

    private void updateIndianDistricts(int statePosition, Spinner districtSpinner) {
        ArrayList<String> districts = new ArrayList<>();
        districts.add("Select District");
        String cached = getSharedPreferences(LOCATION_PREFS, MODE_PRIVATE)
                .getString("india_states_json", null);
        if (cached != null && statePosition > 0) {
            try {
                JSONArray states = new JSONArray(cached);
                JSONObject state = states.optJSONObject(statePosition - 1);
                JSONArray list = state != null ? state.optJSONArray("districts") : null;
                if (list != null) {
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject district = list.optJSONObject(i);
                        if (district != null) districts.add(district.optString("name", ""));
                    }
                }
            } catch (Exception ignored) { }
        }
        ArrayAdapter<String> adapter = locationSpinnerAdapter(districts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        districtSpinner.setAdapter(adapter);
        districtSpinner.setEnabled(statePosition > 0 && districts.size() > 1);
        districtSpinner.setSelection(0);
    }

    private EditText authInput(String hint) {

        EditText input = new EditText(this);

        input.setSingleLine(true);
        input.setTextColor(white);
        input.setHintTextColor(0xFF888888);
        input.setHint(hint);
        input.setTextSize(14);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));

        input.setBackground(
                roundedBackground(0xFF25252F, 0xFF514629, 12));

        input.setLayoutParams(
                new LinearLayout.LayoutParams(-1, dp(54)));

        return input;
    }

    private void addAuthInputSpacing(LinearLayout parent, View input) {

        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) input.getLayoutParams();

        params.topMargin = dp(12);
        input.setLayoutParams(params);
        parent.addView(input);
    }

    private Button authButton(String label) {

        Button button = new Button(this);

        button.setText(label);
        button.setTextColor(black);
        button.setTextSize(14);
        button.setTypeface(null, Typeface.BOLD);
        button.setAllCaps(false);

        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(gold));

        button.setLayoutParams(
                new LinearLayout.LayoutParams(-1, dp(52)));

        return button;
    }

    private void addAuthButtonSpacing(LinearLayout parent,
                                      Button button) {

        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) button.getLayoutParams();

        params.topMargin = dp(12);
        button.setLayoutParams(params);
        parent.addView(button);
    }

    private void addAuthTextButton(LinearLayout parent,
                                   String label, Runnable action) {

        TextView button = makeText(label, 13, gold, true);

        button.setGravity(Gravity.CENTER);
        button.setPadding(0, dp(20), 0, dp(5));
        button.setOnClickListener(v -> action.run());

        parent.addView(button);
    }

    private void finishAuthentication() {

        hideSkeletonLoading();
        sessionCheckPending = false;
        sessionCheckFailed = false;

        authGateActive = false;
        authBusy = false;

        if (authOverlay != null) {
            ViewGroup parent = (ViewGroup) authOverlay.getParent();
            if (parent != null) parent.removeView(authOverlay);
            authOverlay = null;
        }

        setAppContentLocked(false);
        selectTab(0);
        loadSignedInUserProfile();

        // First successful normal-user login shows language selection once.
        // Admin accounts are routed to AdminDashboard before reaching here.
        if (!areAudioLanguagesSaved()) {
            showAudioLanguageScreen(false);
        }
    }

    private String getLanguagePreferenceKey() {
        FirebaseUser user = mAuth == null ? null : mAuth.getCurrentUser();
        String uid = user == null ? "guest" : user.getUid();
        return "selected_languages_" + uid;
    }

    private boolean areAudioLanguagesSaved() {
        return getSharedPreferences(LANGUAGE_PREFS, MODE_PRIVATE)
                .getBoolean(getLanguagePreferenceKey() + "_done", false);
    }

    private void showAudioLanguageScreen(boolean editing) {

        if (isFinishing() || isDestroyed()) return;

        if (languageOverlay != null) {
            ViewGroup old = (ViewGroup) languageOverlay.getParent();
            if (old != null) old.removeView(languageOverlay);
            languageOverlay = null;
        }

        final String[] names = {
                "Hindi", "English", "Tamil", "Telugu",
                "Malayalam", "Bengali", "Marathi", "Kannada"
        };
        final String[] ids = {
                "hindi", "english", "tamil", "telugu",
                "malayalam", "bengali", "marathi", "kannada"
        };

        android.content.SharedPreferences prefs =
                getSharedPreferences(LANGUAGE_PREFS, MODE_PRIVATE);
        String saved = prefs.getString(getLanguagePreferenceKey(), "");
        final java.util.HashSet<String> selected =
                new java.util.HashSet<>();
        if (!saved.isEmpty()) {
            selected.addAll(Arrays.asList(saved.split(",")));
        }

        languageOverlay = new FrameLayout(this);
        languageOverlay.setBackgroundColor(0xFF101014);
        languageOverlay.setClickable(true);
        languageOverlay.setFocusable(true);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(0xFF101014);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        screen.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(62), dp(18), dp(18));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        TextView heading = makeText("Build Your Home Page", 26, 0xFFE4E6EF, true);
        heading.setGravity(Gravity.CENTER);
        page.addView(heading);

        TextView subtitle = makeText("◖))  Choose your favourite languages",
                17, 0xFF9AA3B8, false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams =
                new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(10);
        subParams.bottomMargin = dp(28);
        page.addView(subtitle, subParams);

        android.widget.GridLayout grid = new android.widget.GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(false);
        page.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        int[] imageIds = {
                R.drawable.lang_hindi, R.drawable.lang_english,
                R.drawable.lang_tamil, R.drawable.lang_telugu,
                R.drawable.lang_malayalam, R.drawable.lang_bengali,
                R.drawable.lang_marathi, R.drawable.lang_kannada
        };

        final TextView[] checks = new TextView[8];
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            FrameLayout card = new FrameLayout(this);
            android.graphics.drawable.GradientDrawable border =
                    new android.graphics.drawable.GradientDrawable();
            border.setColor(0xFF25252B);
            border.setCornerRadius(dp(14));
            border.setStroke(dp(1), selected.contains(ids[i])
                    ? gold : 0xFF55555B);
            card.setBackground(border);
            card.setClipToOutline(true);

            ImageView image = new ImageView(this);
            image.setImageResource(imageIds[i]);
            image.setScaleType(ImageView.ScaleType.FIT_XY);
            card.addView(image, new FrameLayout.LayoutParams(-1, -1));

            TextView check = makeText("✓", 23, white, true);
            check.setGravity(Gravity.CENTER);
            check.setBackground(roundedBackground(0xCCB8860B, gold, 30));
            FrameLayout.LayoutParams checkParams =
                    new FrameLayout.LayoutParams(dp(34), dp(34),
                            Gravity.TOP | Gravity.END);
            checkParams.setMargins(0, dp(7), dp(7), 0);
            card.addView(check, checkParams);
            checks[i] = check;
            check.setVisibility(selected.contains(ids[i]) ? View.VISIBLE : View.GONE);

            android.widget.GridLayout.LayoutParams gp =
                    new android.widget.GridLayout.LayoutParams();
            gp.width = 0;
            gp.height = dp(104);
            gp.columnSpec = android.widget.GridLayout.spec(i % 2, 1f);
            gp.rowSpec = android.widget.GridLayout.spec(i / 2);
            gp.setMargins(dp(0), dp(0), dp(10), dp(12));
            card.setLayoutParams(gp);

            card.setOnClickListener(v -> {
                if (selected.contains(ids[index])) {
                    selected.remove(ids[index]);
                    checks[index].setVisibility(View.GONE);
                } else {
                    selected.add(ids[index]);
                    checks[index].setVisibility(View.VISIBLE);
                }
                android.graphics.drawable.GradientDrawable updated =
                        new android.graphics.drawable.GradientDrawable();
                updated.setColor(0xFF25252B);
                updated.setCornerRadius(dp(14));
                updated.setStroke(dp(2), selected.contains(ids[index])
                        ? gold : 0xFF55555B);
                card.setBackground(updated);
            });
            grid.addView(card);
        }

        TextView terms = makeText(
                "We will use your information to personalize and improve your CINEWORLD experience and to send you information about the service. By clicking \"Start Watching\" you agree to our Terms of Use and acknowledge that you have read our Privacy Policy. CINEWORLD may use your selected languages to offer personalized video suggestions.",
                12, 0xFF41485B, false);
        terms.setPadding(0, dp(14), 0, dp(18));
        page.addView(terms);

        Button continueButton = authButton("START WATCHING");
        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(-1, dp(52));
        buttonParams.setMargins(0, dp(4), 0, dp(24));
        page.addView(continueButton, buttonParams);

        continueButton.setOnClickListener(v -> {
            if (selected.isEmpty()) {
                showMessage("Please select at least one audio language.");
                return;
            }
            StringBuilder value = new StringBuilder();
            for (String id : ids) {
                if (selected.contains(id)) {
                    if (value.length() > 0) value.append(",");
                    value.append(id);
                }
            }
            prefs.edit()
                    .putString(getLanguagePreferenceKey(), value.toString())
                    .putBoolean(getLanguagePreferenceKey() + "_done", true)
                    .apply();

            FrameLayout overlay = languageOverlay;
            languageOverlay = null;
            if (overlay != null) {
                ViewGroup parent = (ViewGroup) overlay.getParent();
                if (parent != null) parent.removeView(overlay);
            }
            if (editing) showMessage("Audio languages updated.");
            selectTab(0);
        });

        languageOverlay.addView(screen,
                new FrameLayout.LayoutParams(-1, -1));
        ((ViewGroup) findViewById(android.R.id.content)).addView(
                languageOverlay, new ViewGroup.LayoutParams(-1, -1));
    }

    private boolean isValidEmail(String email) {

        return email != null &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // ----------------------------------------------------
    // LOGIN ERROR EFFECT: VIBRATION + RED BORDER + SHAKE
    // ----------------------------------------------------
    private void showLoginError(EditText email, EditText password,
                                Button login, String message) {

        // Vibrate the phone (VIBRATE permission is required in Manifest).
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(
                            250, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(250);
                }
            }
        } catch (Exception ignored) {
        }

        // Highlight both fields in red.
        email.setBackground(roundedBackground(
                0xFF25252F, 0xFFE53935, 12));
        password.setBackground(roundedBackground(
                0xFF25252F, 0xFFE53935, 12));

        // Shake both fields.
        TranslateAnimation emailShake = new TranslateAnimation(
                -dp(7), dp(7), 0, 0);
        emailShake.setDuration(55);
        emailShake.setRepeatCount(5);
        emailShake.setRepeatMode(android.view.animation.Animation.REVERSE);
        email.startAnimation(emailShake);

        TranslateAnimation passwordShake = new TranslateAnimation(
                -dp(7), dp(7), 0, 0);
        passwordShake.setDuration(55);
        passwordShake.setRepeatCount(5);
        passwordShake.setRepeatMode(android.view.animation.Animation.REVERSE);
        password.startAnimation(passwordShake);

        showMessage(message);

        // Restore the normal gold border after the error effect.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            email.setBackground(roundedBackground(
                    0xFF25252F, 0xFF514629, 12));
            password.setBackground(roundedBackground(
                    0xFF25252F, 0xFF514629, 12));
        }, 1800);
    }

    private String authError(Exception exception) {

        if (exception == null) {
            return "Authentication failed. Please try again.";
        }

        String message = exception.getMessage();

        if (message == null) {
            return "Authentication failed. Please try again.";
        }

        if (message.contains("password is invalid") ||
                message.contains("INVALID_LOGIN_CREDENTIALS")) {
            return "Incorrect email or password.";
        }

        if (message.contains("no user record")) {
            return "No account found with this email.";
        }

        if (message.contains("email address is already in use")) {
            return "This email is already registered.";
        }

        if (message.contains("network")) {
            return "Network error. Check your internet connection.";
        }

        return message;
    }

    // ----------------------------------------------------
    // FIREBASE USER PROFILE
    // ----------------------------------------------------

    private void loadSignedInUserProfile() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            showGuestProfile();
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(document -> {

                    String name = document.getString("name");
                    String email = document.getString("email");

                    if (name == null || name.trim().isEmpty()) {
                        name = user.getDisplayName();
                    }

                    if (name == null || name.trim().isEmpty()) {
                        name = "CineWorld User";
                    }

                    if (email == null || email.trim().isEmpty()) {
                        email = user.getEmail();
                    }

                    saveLocalProfile(name, email);

                    if (profileNameView != null) {
                        profileNameView.setText(name);
                    }

                    if (profileEmailView != null) {
                        profileEmailView.setText(
                                email == null ? "" : email);
                    }

                    setupProfileScreen();
                })
                .addOnFailureListener(e -> {

                    String name = user.getDisplayName();

                    if (name == null || name.trim().isEmpty()) {
                        name = "CineWorld User";
                    }

                    saveLocalProfile(name, user.getEmail());
                    setupProfileScreen();
                });
    }

    private void saveLocalProfile(String name, String email) {

        getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE).edit()
                .putString("profile_name", name)
                .putString("profile_email",
                        email == null ? "Welcome to CineWorld" : email)
                .apply();
    }

    private void showGuestProfile() {

        getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE).edit()
                .putString("profile_name", "CineWorld User")
                .putString("profile_email", "Welcome to CineWorld")
                .apply();

        if (profileNameView != null) {
            profileNameView.setText("CineWorld User");
        }

        if (profileEmailView != null) {
            profileEmailView.setText("Welcome to CineWorld");
        }

        setupProfileScreen();
    }

    private void setAppContentLocked(boolean locked) {

        if (binding == null) return;

        binding.mainScroll.setVisibility(
                locked ? View.GONE : View.VISIBLE);

        if (navigationItems != null) {

            for (TextView item : navigationItems) {

                if (item != null) {

                    // Keep Shows and bottom Profile hidden even after login.
                    boolean permanentlyHidden =
                            item == binding.navShows ||
                            item == binding.navProfile;

                    item.setVisibility(
                            (locked || permanentlyHidden)
                                    ? View.GONE
                                    : View.VISIBLE);
                }
            }
        }

        binding.searchButton.setVisibility(
                locked ? View.GONE : View.VISIBLE);

        binding.notificationButton.setVisibility(
                locked ? View.GONE : View.VISIBLE);

        binding.profileButton.setVisibility(
                locked ? View.GONE : View.VISIBLE);
    }

    private void logoutUser() {

        new AlertDialog.Builder(this)
                .setTitle("LOGOUT")
                .setMessage("Are you sure you want to logout?")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("LOGOUT", (dialog, which) -> {

                    mAuth.signOut();

                    sessionCheckPending = false;
                    sessionCheckFailed = false;
                    authGateActive = true;

                    showGuestProfile();
                    setAppContentLocked(true);
                    showAuthScreen("login");

                    showMessage("Logged out successfully");
                })
                .show();
    }

    // ----------------------------------------------------
    // IMAGE LOADING
    // ----------------------------------------------------

    private void loadAllImages() {

        loadPoster(binding.featuredImage,
                "https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=1200");

        loadPoster(binding.posterOne, movies.get(0).image);
        loadPoster(binding.posterTwo, movies.get(1).image);
        loadPoster(binding.posterThree, movies.get(2).image);
    }

    private void loadPoster(ImageView image, String url) {

        Glide.with(this).load(url)
                .placeholder(new ColorDrawable(0xFF292018))
                .error(new ColorDrawable(0xFF292018))
                .centerCrop()
                .into(image);
    }

    // ----------------------------------------------------
    // NETWORK STATE
    // ----------------------------------------------------

    private boolean hasInternetConnection() {
        try {
            android.net.ConnectivityManager cm =
                    (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network network = cm.getActiveNetwork();
                if (network == null) return false;
                android.net.NetworkCapabilities caps =
                        cm.getNetworkCapabilities(network);
                return caps != null
                        && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }

            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception ignored) {
            return false;
        }
    }

    // ----------------------------------------------------
    // DOWNLOAD MANAGER
    // ----------------------------------------------------

    private void setupDownloadManager() {

        downloadManager =
                (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        File folder = getDownloadFolder();

        if (folder != null) {
            initialFreeBytes = folder.getUsableSpace();
            storageLimitBytes = initialFreeBytes * 80L / 100L;
        }

        loadDownloadIds();
    }

    private File getDownloadFolder() {

        File folder =
                getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        if (folder == null) {
            folder = new File(getFilesDir(), "CineWorldMovies");
        }

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    private void saveDownloadIds() {

        StringBuilder value = new StringBuilder();

        for (Long id : downloadIds) {

            if (value.length() > 0) {
                value.append(",");
            }

            value.append(id);
        }

        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("ids", value.toString())
                .apply();
    }

    private void loadDownloadIds() {

        String saved = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString("ids", "");

        downloadIds.clear();

        if (saved == null || saved.isEmpty()) return;

        for (String value : saved.split(",")) {

            try {
                downloadIds.add(Long.parseLong(value));
            } catch (Exception ignored) {
            }
        }
    }

    private void startMovieDownload(ContentItem item) {

        if (offlineMode || !hasInternetConnection()) {
            showMessage("Internet connection required to start a new download.");
            return;
        }

        if (downloadManager == null) {
            showMessage("Download Manager unavailable");
            return;
        }

        if (item.videoUrl == null || item.videoUrl.trim().isEmpty()) {
            showMessage("Is demo movie ka video URL abhi set nahi hai.");
            return;
        }

        File folder = getDownloadFolder();

        if (folder == null) {
            showMessage("Storage unavailable");
            return;
        }

        long available = folder.getUsableSpace();

        if (available <= 0 ||
                (storageLimitBytes > 0 &&
                        getTotalDownloadedBytes() >= storageLimitBytes)) {

            showMessage("Storage limit reached");
            return;
        }

        String fileName =
                item.title.replaceAll("[^a-zA-Z0-9_-]", "_")
                        + "_" + System.currentTimeMillis() + ".mp4";

        try {

            DownloadManager.Request request =
                    new DownloadManager.Request(Uri.parse(item.videoUrl));

            request.setTitle(item.title);
            request.setDescription("Downloading in CineWorld");

            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE);

            request.setDestinationInExternalFilesDir(
                    this, Environment.DIRECTORY_MOVIES, fileName);

            android.content.SharedPreferences prefs =
                    getSharedPreferences(PREFS, MODE_PRIVATE);

            boolean allowWifi =
                    prefs.getBoolean(KEY_WIFI, true);

            boolean allowMobile =
                    prefs.getBoolean(KEY_MOBILE, true);

            if (!allowWifi && !allowMobile) {
                showMessage(
                        "Enable Wi-Fi or Mobile Data in Download Settings");
                return;
            }

            int allowedNetworks = 0;

            if (allowWifi) {
                allowedNetworks |=
                        DownloadManager.Request.NETWORK_WIFI;
            }

            if (allowMobile) {
                allowedNetworks |=
                        DownloadManager.Request.NETWORK_MOBILE;
            }

            request.setAllowedNetworkTypes(allowedNetworks);

            long id = downloadManager.enqueue(request);

            downloadIds.add(id);
            saveDownloadIds();
            refreshDownloads();

            showMessage("Download started: " + item.title);

        } catch (Exception e) {
            showMessage("Download failed to start");
        }
    }

    private long getTotalDownloadedBytes() {

        long total = 0;

        if (downloadManager == null) return total;

        for (Long id : downloadIds) {

            DownloadManager.Query query =
                    new DownloadManager.Query().setFilterById(id);

            Cursor cursor = null;

            try {

                cursor = downloadManager.query(query);

                if (cursor != null && cursor.moveToFirst()) {

                    int column = cursor.getColumnIndex(
                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);

                    if (column >= 0) {
                        total += cursor.getLong(column);
                    }
                }

            } catch (Exception ignored) {

            } finally {

                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return total;
    }

    private void startDownloadMonitoring() {

        if (downloadRunnable != null) {
            downloadHandler.removeCallbacks(downloadRunnable);
        }

        downloadRunnable = new Runnable() {

            @Override
            public void run() {

                refreshDownloads();
                downloadHandler.postDelayed(this, 1000);
            }
        };

        downloadHandler.post(downloadRunnable);
    }

    private void stopDownloadMonitoring() {

        if (downloadRunnable != null) {
            downloadHandler.removeCallbacks(downloadRunnable);
        }
    }

    // ----------------------------------------------------
    // PULL DOWN TO REFRESH
    // ----------------------------------------------------

    private void setupPullToRefresh() {

        // A small gold spinner is shown while the current page refreshes.
        View parent = (View) binding.mainScroll.getParent();

        if (parent instanceof FrameLayout) {
            FrameLayout frame = (FrameLayout) parent;

            pullRefreshProgress = new ProgressBar(this);
            pullRefreshProgress.setIndeterminate(true);
            pullRefreshProgress.setVisibility(View.GONE);

            if (Build.VERSION.SDK_INT >= 21) {
                pullRefreshProgress.setIndeterminateTintList(
                        android.content.res.ColorStateList.valueOf(gold));
            }

            FrameLayout.LayoutParams progressParams =
                    new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            progressParams.topMargin = dp(8);
            frame.addView(pullRefreshProgress, progressParams);
            pullRefreshProgress.bringToFront();
        }

        binding.mainScroll.setOnTouchListener((view, event) -> {

            switch (event.getActionMasked()) {

                case android.view.MotionEvent.ACTION_DOWN:
                    pullStartY = event.getY();
                    pullStartX = event.getX();
                    pullRefreshTriggered = false;
                    horizontalDragActive = false;
                    swipePreviewPrepared = false;
                    swipePreviewTab = -1;
                    swipeCurrentDx = 0f;
                    return false;

                case android.view.MotionEvent.ACTION_MOVE: {
                    float dx = event.getX() - pullStartX;
                    float dy = event.getY() - pullStartY;

                    if (!horizontalDragActive
                            && Math.abs(dx) > dp(12)
                            && Math.abs(dx) > Math.abs(dy) * 1.25f) {
                        int candidate = getAdjacentSwipeTab(dx);
                        if (candidate >= 0 && !authGateActive && !sessionCheckPending
                                && !sessionCheckFailed && !tabTransitionRunning) {
                            horizontalDragActive = true;
                            swipePreviewTab = candidate;
                            swipeCurrentView = getTabContentView(currentTab);
                            swipeNextView = getTabContentView(candidate);
                            prepareSwipePreview();
                        }
                    }

                    if (horizontalDragActive) {
                        swipeCurrentDx = dx;
                        updateSwipePreview(dx);
                        return true;
                    }
                    return false;
                }

                case android.view.MotionEvent.ACTION_UP: {
                    float swipeX = event.getX() - pullStartX;
                    float swipeY = event.getY() - pullStartY;

                    if (horizontalDragActive) {
                        float width = Math.max(1f, binding.mainScroll.getWidth());
                        boolean completeSwipe = Math.abs(swipeX) >= width * 0.22f;
                        int destination = swipePreviewTab;
                        clearSwipePreview(completeSwipe, destination);
                        horizontalDragActive = false;
                        pullStartY = 0f;
                        pullStartX = 0f;
                        pullRefreshTriggered = false;
                        return true;
                    }

                    // Refresh only after a deliberate, mostly vertical downward pull
                    // while the ScrollView is already at its top edge.
                    boolean deliberatePullDown = swipeY > dp(150)
                            && swipeY > Math.abs(swipeX) * 1.5f
                            && binding.mainScroll.getScrollY() <= 0;

                    if (deliberatePullDown && !pullRefreshTriggered) {
                        pullRefreshTriggered = true;
                        performPullRefresh();
                    }

                    pullStartY = 0f;
                    pullStartX = 0f;
                    pullRefreshTriggered = false;
                    return false;
                }

                case android.view.MotionEvent.ACTION_CANCEL:
                    if (horizontalDragActive) {
                        clearSwipePreview(false, -1);
                    }
                    horizontalDragActive = false;
                    pullStartY = 0f;
                    pullStartX = 0f;
                    pullRefreshTriggered = false;
                    return false;
            }

            return false;
        });
    }

    // Swipe order is Home -> Movies -> Downloads and the reverse.
    private int getAdjacentSwipeTab(float dx) {
        if (dx < 0f) {
            if (currentTab == 0) return 1;
            if (currentTab == 1) return 3;
        } else {
            if (currentTab == 3) return 1;
            if (currentTab == 1) return 0;
        }
        return -1;
    }

    private void prepareSwipePreview() {
        if (swipeCurrentView == null || swipeNextView == null) return;

        swipeNextView.animate().cancel();
        swipeCurrentView.animate().cancel();
        swipeNextView.setVisibility(View.VISIBLE);
        swipeNextView.setAlpha(1f);
        swipeCurrentView.setAlpha(1f);
        binding.mainScroll.requestLayout();

        // The page sections are vertical siblings in the ScrollView's content.
        // Offset the incoming section vertically so it overlays the outgoing one.
        binding.mainScroll.post(() -> {
            if (!horizontalDragActive || swipeCurrentView == null || swipeNextView == null) return;
            swipeNextView.setTranslationY(
                    swipeCurrentView.getTop() - swipeNextView.getTop());
            updateSwipePreview(swipeCurrentDx);
            swipePreviewPrepared = true;
        });
    }

    private void updateSwipePreview(float dx) {
        if (swipeCurrentView == null || swipeNextView == null) return;
        float width = Math.max(1f, binding.mainScroll.getWidth());
        float boundedDx = Math.max(-width, Math.min(width, dx));
        swipeCurrentView.setTranslationX(boundedDx);
        float incomingStart = boundedDx < 0f ? width : -width;
        swipeNextView.setTranslationX(incomingStart + boundedDx);
    }

    private void clearSwipePreview(boolean complete, int destination) {
        View outgoing = swipeCurrentView;
        View incoming = swipeNextView;
        if (outgoing == null || incoming == null) return;

        if (complete && destination >= 0) {
            // Keep the drag continuous until the release, then commit the tab.
            outgoing.setTranslationX(0f);
            incoming.setTranslationX(0f);
            incoming.setTranslationY(0f);
            incoming.setVisibility(View.GONE);
            outgoing.setAlpha(1f);
            outgoing.setTranslationY(0f);
            swipeCurrentView = null;
            swipeNextView = null;
            swipePreviewTab = -1;
            swipePreviewPrepared = false;
            selectTab(destination);
        } else {
            outgoing.animate().translationX(0f).setDuration(170).start();
            incoming.animate().translationX(0f).translationY(0f)
                    .alpha(1f).setDuration(170)
                    .withEndAction(() -> {
                        incoming.setVisibility(View.GONE);
                        outgoing.setTranslationX(0f);
                        outgoing.setAlpha(1f);
                    }).start();
            swipeCurrentView = null;
            swipeNextView = null;
            swipePreviewTab = -1;
            swipePreviewPrepared = false;
        }
    }

    private void performPullRefresh() {

        if (authGateActive || sessionCheckPending || sessionCheckFailed) {
            return;
        }

        if (pullRefreshProgress != null) {
            pullRefreshProgress.setVisibility(View.VISIBLE);
            pullRefreshProgress.bringToFront();
        }

        // Refresh silently: no Toast/Snackbar/pop-up message is shown.
        // The small refresh indicator is removed as soon as the reload finishes.
        pullRefreshHandler.postDelayed(() -> {

            if (isFinishing() || isDestroyed()) {
                if (pullRefreshProgress != null) {
                    pullRefreshProgress.setVisibility(View.GONE);
                }
                return;
            }

            try {
                switch (currentTab) {

                    case 0:
                        // Refresh featured and trending artwork.
                        loadAllImages();
                        break;

                    case 1:
                    case 2:
                        // Rebuild the movie/show catalogue content.
                        setupCatalogScreens();
                        break;

                    case 3:
                        // Reload locally downloaded items.
                        refreshDownloads();
                        break;

                    case 4:
                        // Reload locally stored profile information.
                        setupProfileScreen();
                        break;
                }
            } finally {
                if (pullRefreshProgress != null) {
                    pullRefreshProgress.setVisibility(View.GONE);
                }
            }
        }, 700);
    }

    // ----------------------------------------------------
    // NAVIGATION
    // ----------------------------------------------------

    private void setupNavigation() {

        navigationItems = new TextView[]{
                binding.navHome,
                binding.navMovies,
                binding.navShows,
                binding.navDownloads,
                binding.navProfile
        };

        // Hide these two bottom tabs every time navigation is initialized.
        binding.navShows.setVisibility(View.GONE);
        binding.navProfile.setVisibility(View.GONE);

        binding.navHome.setOnClickListener(v -> selectTab(0));
        binding.navMovies.setOnClickListener(v -> selectTab(1));
        binding.navShows.setOnClickListener(v -> selectTab(2));
        binding.navDownloads.setOnClickListener(v -> selectTab(3));
        binding.navProfile.setOnClickListener(v -> selectTab(4));
    }

    private View getTabContentView(int position) {
        switch (position) {
            case 0: return binding.homeContent;
            case 1: return binding.moviesContent;
            case 2: return binding.showsContent;
            case 3: return binding.downloadsContent;
            case 4: return binding.profileContent;
            default: return null;
        }
    }

    private void selectTab(int position) {

        if (offlineMode) {
            if (position != 3 && position != 4) {
                showMessage("Offline mode: open Downloads or Profile.");
                return;
            }
        } else if (mAuth != null && mAuth.getCurrentUser() == null) {

            authGateActive = true;
            setAppContentLocked(true);

            if (authOverlay == null) {
                showAuthScreen("login");
            }
            return;
        }

        if (authGateActive || sessionCheckPending || sessionCheckFailed
                || tabTransitionRunning || position == currentTab) {
            return;
        }

        final int previousTab = currentTab;
        final boolean movingForward = position > previousTab;
        final View page = binding.mainScroll;
        final float width = Math.max(1f, page.getWidth());
        final float exitX = movingForward ? -width * 0.32f : width * 0.32f;
        final float enterX = -exitX;

        tabTransitionRunning = true;

        // Cinematic push transition: move the current viewport away, switch the
        // visible section, then bring the new viewport in from the opposite side.
        page.animate().cancel();
        page.setPivotX(movingForward ? 0f : page.getWidth());
        page.setPivotY(page.getHeight() / 2f);

        page.animate()
                .translationX(exitX)
                .alpha(0.42f)
                .scaleX(0.975f)
                .setDuration(145)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {

                    if (isFinishing() || isDestroyed()) {
                        tabTransitionRunning = false;
                        return;
                    }

                    binding.homeContent.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                    binding.moviesContent.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                    binding.showsContent.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
                    binding.downloadsContent.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
                    binding.profileContent.setVisibility(position == 4 ? View.VISIBLE : View.GONE);

                    currentTab = position;

                    for (int i = 0; i < navigationItems.length; i++) {
                        navigationItems[i].setTextColor(i == position ? gold : gray);
                    }

                    page.scrollTo(0, 0);
                    page.setTranslationX(enterX);
                    page.setAlpha(0.42f);
                    page.setScaleX(0.975f);

                    page.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .scaleX(1f)
                            .setDuration(360)
                            .setInterpolator(new android.view.animation.PathInterpolator(
                                    0.18f, 0.82f, 0.25f, 1f))
                            .withEndAction(() -> {
                                page.setPivotX(page.getWidth() / 2f);
                                page.setPivotY(page.getHeight() / 2f);
                                tabTransitionRunning = false;
                            })
                            .start();

                    if (position == 3) {
                        refreshDownloads();
                    }
                })
                .start();
    }

    // ----------------------------------------------------
    // HOME
    // ----------------------------------------------------

    private void setupHome() {

        binding.movieOne.setOnClickListener(v ->
                showDetails(movies.get(0)));

        binding.movieTwo.setOnClickListener(v ->
                showDetails(movies.get(1)));

        binding.movieThree.setOnClickListener(v ->
                showDetails(movies.get(2)));

        binding.seeAllTrending.setOnClickListener(v ->
                selectTab(1));

        binding.featuredPlayButton.setOnClickListener(v ->
                showDetails(movies.get(0)));

        addHomeDemoVideoSection();
    }

    // One downloadable sample demo video below the Home movie banner.
    private void addHomeDemoVideoSection() {

        if (!(binding.homeContent instanceof ViewGroup)) return;
        ViewGroup home = (ViewGroup) binding.homeContent;
        if (home.findViewWithTag("cineworld_home_demo_video") != null) return;

        LinearLayout section = verticalLayout();
        section.setTag("cineworld_home_demo_video");
        section.setPadding(dp(14), dp(16), dp(14), dp(18));

        TextView heading = makeText("CINEWORLD DEMO VIDEO", 17, gold, true);
        section.addView(heading);

        TextView subtitle = makeText("4K Ultra HD Demo • In-app playback and download", 12, gray, false);
        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(4);
        section.addView(subtitle, subtitleParams);

        FrameLayout preview = new FrameLayout(this);
        preview.setBackground(roundedBackground(0xFF080808, 0xFF806019, 14));
        preview.setClipToOutline(true);
        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(-1, dp(205));
        previewParams.topMargin = dp(12);
        section.addView(preview, previewParams);

        LinearLayout poster = verticalLayout();
        poster.setGravity(Gravity.CENTER);
        poster.setPadding(dp(12), dp(12), dp(12), dp(12));
        poster.setBackground(roundedBackground(0xFF101018, 0xFF806019, 14));
        preview.addView(poster, new FrameLayout.LayoutParams(-1, -1));

        TextView filmIcon = makeText("▶", 46, gold, true);
        filmIcon.setGravity(Gravity.CENTER);
        poster.addView(filmIcon);

        TextView filmTitle = makeText("BIG BUCK BUNNY • 4K UHD", 17, white, true);
        filmTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams filmTitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        filmTitleParams.topMargin = dp(8);
        poster.addView(filmTitle, filmTitleParams);

        TextView filmCaption = makeText("3840 × 2160 • 60 FPS • CC BY 3.0 • BLENDER FOUNDATION", 10, gray, true);
        filmCaption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams filmCaptionParams =
                new LinearLayout.LayoutParams(-1, -2);
        filmCaptionParams.topMargin = dp(5);
        poster.addView(filmCaption, filmCaptionParams);

        PlayerView videoPlayerView = new PlayerView(this);
        videoPlayerView.setUseController(true);
        videoPlayerView.setControllerAutoShow(true);
        videoPlayerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        videoPlayerView.setVisibility(View.GONE);
        preview.addView(videoPlayerView, new FrameLayout.LayoutParams(-1, -1));

        TextView playButton = makeText("▶  PLAY DEMO", 14, black, true);
        playButton.setGravity(Gravity.CENTER);
        playButton.setPadding(dp(20), dp(12), dp(20), dp(12));
        playButton.setBackground(roundedBackground(gold, gold, 24));
        LinearLayout.LayoutParams playParams =
                new LinearLayout.LayoutParams(-1, -2);
        playParams.topMargin = dp(12);
        section.addView(playButton, playParams);

        TextView downloadButton = makeText("⬇  DOWNLOAD VIDEO", 14, gold, true);
        downloadButton.setGravity(Gravity.CENTER);
        downloadButton.setPadding(dp(20), dp(12), dp(20), dp(12));
        downloadButton.setBackground(roundedBackground(0xFF191920, gold, 24));
        LinearLayout.LayoutParams downloadParams =
                new LinearLayout.LayoutParams(-1, -2);
        downloadParams.topMargin = dp(9);
        section.addView(downloadButton, downloadParams);

        playButton.setOnClickListener(v -> {
            if (offlineMode || !hasInternetConnection()) {
                showMessage("Internet connection is required to play this demo.");
                return;
            }
            if (player == null) setupPlayer();
            videoPlayerView.setPlayer(player);
            poster.setVisibility(View.GONE);
            videoPlayerView.setVisibility(View.VISIBLE);
            playOnline(DEMO_URL);
        });

        downloadButton.setOnClickListener(v -> {
            ContentItem demoItem = new ContentItem(
                    "CineWorld Demo - Big Buck Bunny 4K",
                    "Demo • 4K UHD",
                    "3840 x 2160, 60 fps sample demo video",
                    "",
                    false,
                    DEMO_URL);
            startMovieDownload(demoItem);
        });

        // The first Home child is the complete featured banner area.
        int insertAt = home.getChildCount() > 0 ? 1 : 0;
        home.addView(section, Math.min(insertAt, home.getChildCount()),
                new ViewGroup.LayoutParams(-1, -2));
    }

    // ----------------------------------------------------
    // MOVIES AND SHOWS
    // ----------------------------------------------------

    private void setupCatalogScreens() {

        // Rebuild the catalogue without touching the locked Home Page layout.
        binding.moviesContent.removeAllViews();
        binding.showsContent.removeAllViews();

        addMovieCategoryFilters();
        renderMoviesByCategory(selectedMovieCategory);
        addCatalogItems(binding.showsContent, shows);
    }

    private void addMovieCategoryFilters() {

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        horizontal.setFillViewport(false);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(8), 0, dp(8));
        horizontal.addView(chips, new HorizontalScrollView.LayoutParams(-2, -2));

        ArrayList<String> categories = new ArrayList<>();
        categories.add("All");
        for (ContentItem item : movies) {
            if (item.genre == null) continue;
            for (String part : item.genre.split("\\s*•\\s*")) {
                String category = part.trim();
                if (!category.isEmpty() && !categories.contains(category)) {
                    categories.add(category);
                }
            }
        }

        for (String category : categories) {
            TextView chip = makeText(category, 12,
                    category.equalsIgnoreCase(selectedMovieCategory) ? black : gold, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(16), dp(9), dp(16), dp(9));
            chip.setBackground(roundedBackground(
                    category.equalsIgnoreCase(selectedMovieCategory) ? gold : dark,
                    0xFF806019, 20));

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(-2, -2);
            params.setMargins(0, 0, dp(8), 0);
            chips.addView(chip, params);

            chip.setOnClickListener(v -> {
                selectedMovieCategory = category;
                setupCatalogScreens();
            });
        }

        LinearLayout.LayoutParams filterParams =
                new LinearLayout.LayoutParams(-1, -2);
        binding.moviesContent.addView(horizontal, filterParams);
    }

    private void renderMoviesByCategory(String category) {

        ArrayList<ContentItem> filtered = new ArrayList<>();
        for (ContentItem item : movies) {
            if (category == null || category.equalsIgnoreCase("All")
                    || (item.genre != null && item.genre.toLowerCase()
                    .contains(category.toLowerCase()))) {
                filtered.add(item);
            }
        }

        addCatalogItems(binding.moviesContent, filtered);
    }

    private void addCatalogItems(LinearLayout parent,
                                 ArrayList<ContentItem> items) {

        for (ContentItem item : items) {

            LinearLayout card = new LinearLayout(this);

            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(10, 10, 10, 10);
            card.setBackgroundColor(dark);

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(-1, -2);

            cardParams.setMargins(0, 14, 0, 0);
            parent.addView(card, cardParams);

            ImageView poster = new ImageView(this);

            poster.setLayoutParams(
                    new LinearLayout.LayoutParams(105, 145));

            poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(poster);

            loadPoster(poster, item.image);

            LinearLayout info = verticalLayout();
            info.setPadding(12, 4, 4, 4);

            card.addView(info,
                    new LinearLayout.LayoutParams(0, -2, 1));

            info.addView(makeText(item.title, 17, white, true));
            info.addView(makeText(item.genre, 12, gold, false));

            TextView description =
                    makeText(item.description, 12, gray, false);

            LinearLayout.LayoutParams descParams =
                    new LinearLayout.LayoutParams(-1, -2);

            descParams.topMargin = 8;
            info.addView(description, descParams);

            Button details = new Button(this);

            details.setText("VIEW DETAILS");
            details.setTextColor(dark);
            details.setTextSize(10);

            details.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(gold));

            info.addView(details);

            details.setOnClickListener(v -> showDetails(item));

            Button downloadButton = new Button(this);

            downloadButton.setText("DOWNLOAD");
            downloadButton.setTextColor(dark);
            downloadButton.setTextSize(10);

            downloadButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(gold));

            info.addView(downloadButton);

            downloadButton.setOnClickListener(v ->
                    startMovieDownload(item));

            poster.setOnClickListener(v ->
                    showDetails(item));
        }
    }

    private TextView makeText(String value, int size,
                              int color, boolean bold) {

        TextView text = new TextView(this);

        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);

        if (bold) {
            text.setTypeface(null, Typeface.BOLD);
        }

        return text;
    }

    // ----------------------------------------------------
    // MOVIE DETAILS
    // ----------------------------------------------------

    private void showDetails(ContentItem item) {

        if (offlineMode) {
            showMessage("Movie details require an internet connection.");
            return;
        }

        Intent intent = new Intent(
                MainActivity.this, MovieDetailsActivity.class);

        intent.putExtra("title", item.title);
        intent.putExtra("genre", item.genre);
        intent.putExtra("description", item.description);
        intent.putExtra("image", item.image);
        intent.putExtra("series", item.series);
        intent.putExtra("video_url", item.videoUrl);

        startActivity(intent);
    }

    // ----------------------------------------------------
    // EXOPLAYER
    // ----------------------------------------------------

    private void setupPlayer() {

        player = new ExoPlayer.Builder(this).build();

        player.addListener(new Player.Listener() {

            @Override
            public void onPlaybackStateChanged(int state) {

                // Keep playback quiet; the PlayerView provides its own controls.
            }

            @Override
            public void onPlayerError(PlaybackException error) {

                showMessage("Playback Error: " +
                        error.getErrorCodeName());
            }
        });
    }

    private void playVideo(Uri uri) {

        if (player == null) return;

        MediaItem item = MediaItem.fromUri(uri);

        player.setMediaItem(item);
        player.prepare();
        player.play();

        showMessage("Playing Video");
    }

    private void playOnline(String url) {

        if (url == null || url.trim().isEmpty()) {
            showMessage("Enter video URL");
            return;
        }

        Uri uri = Uri.parse(url.trim());

        if (!"https".equalsIgnoreCase(uri.getScheme()) &&
                !"http".equalsIgnoreCase(uri.getScheme())) {

            showMessage("Invalid video URL");
            return;
        }

        MediaItem.Builder builder =
                new MediaItem.Builder().setUri(uri);

        String path = uri.getPath();

        if (path != null) {

            String lower = path.toLowerCase();

            if (lower.endsWith(".m3u8")) {
                builder.setMimeType(MimeTypes.APPLICATION_M3U8);

            } else if (lower.endsWith(".mpd")) {
                builder.setMimeType(MimeTypes.APPLICATION_MPD);
            }
        }

        player.setMediaItem(builder.build());
        player.prepare();
        player.play();
    }

    // ----------------------------------------------------
    // DOWNLOAD CENTER HELPERS
    // ----------------------------------------------------

    private int dp(float value) {

        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable roundedBackground(
            int color, int strokeColor, int radiusDp) {

        GradientDrawable bg = new GradientDrawable();

        bg.setColor(color);
        bg.setCornerRadius(dp(radiusDp));

        if (strokeColor != 0) {
            bg.setStroke(dp(1), strokeColor);
        }

        return bg;
    }

    private LinearLayout verticalLayout() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        return layout;
    }

    private LinearLayout horizontalLayout() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        return layout;
    }

    private TextView sectionLabel(String value) {

        TextView label = makeText(value, 14, gold, true);

        label.setPadding(dp(2), dp(18), dp(2), dp(8));

        return label;
    }

    // ----------------------------------------------------
    // DOWNLOADS SCREEN
    // ----------------------------------------------------

    private void setupDownloadsScreen() {

        loadDownloadIds();

        binding.downloadsContent.removeAllViews();

        binding.downloadsContent.setPadding(
                dp(16), dp(14), dp(16), dp(24));

        binding.downloadsContent.setBackgroundColor(black);

        TextView heading =
                makeText("DOWNLOAD CENTER", 25, gold, true);

        binding.downloadsContent.addView(heading);

        TextView subtitle = makeText(
                "Your movies. Ready whenever you are.",
                12, gray, false);

        subtitle.setPadding(0, dp(4), 0, dp(16));
        binding.downloadsContent.addView(subtitle);

        LinearLayout storageCard = verticalLayout();

        storageCard.setPadding(
                dp(16), dp(15), dp(16), dp(15));

        storageCard.setBackground(
                roundedBackground(0xFF1B1B23, 0xFF6D5314, 18));

        binding.downloadsContent.addView(storageCard,
                new LinearLayout.LayoutParams(-1, -2));

        LinearLayout storageTop = horizontalLayout();

        storageTop.addView(
                makeText("PHONE STORAGE", 13, white, true),
                new LinearLayout.LayoutParams(0, -2, 1));

        storageTop.addView(
                makeText("CINEWORLD", 10, gold, true));

        storageCard.addView(storageTop);

        storageText = makeText(
                "Calculating storage...", 12, gray, false);

        storageText.setPadding(0, dp(10), 0, dp(9));
        storageCard.addView(storageText);

        storageProgress = new ProgressBar(
                this, null,
                android.R.attr.progressBarStyleHorizontal);

        storageProgress.setMax(100);

        if (android.os.Build.VERSION.SDK_INT >= 21) {

            storageProgress.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(gold));

            storageProgress.setProgressBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF38333A));
        }

        storageCard.addView(storageProgress,
                new LinearLayout.LayoutParams(-1, dp(8)));

        Button manageStorage = new Button(this);

        manageStorage.setText("MANAGE STORAGE");
        manageStorage.setTextSize(11);
        manageStorage.setTextColor(dark);

        manageStorage.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(gold));

        LinearLayout.LayoutParams manageParams =
                new LinearLayout.LayoutParams(-1, dp(44));

        manageParams.topMargin = dp(12);

        storageCard.addView(manageStorage, manageParams);

        manageStorage.setOnClickListener(v ->
                showManageStorageDialog());

        binding.downloadsContent.addView(
                sectionLabel("DOWNLOADING"));

        downloadsList = verticalLayout();

        binding.downloadsContent.addView(downloadsList,
                new LinearLayout.LayoutParams(-1, -2));

        binding.downloadsContent.addView(
                sectionLabel("DOWNLOAD SETTINGS"));

        LinearLayout settingsCard = verticalLayout();

        settingsCard.setPadding(
                dp(15), dp(8), dp(15), dp(8));

        settingsCard.setBackground(
                roundedBackground(0xFF1B1B23, 0xFF39343A, 16));

        binding.downloadsContent.addView(settingsCard,
                new LinearLayout.LayoutParams(-1, -2));

        android.content.SharedPreferences prefs =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        wifiSwitch = new Switch(this);

        wifiSwitch.setText("Allow downloads over Wi-Fi");
        wifiSwitch.setTextColor(white);
        wifiSwitch.setTextSize(13);
        wifiSwitch.setChecked(prefs.getBoolean(KEY_WIFI, true));

        settingsCard.addView(wifiSwitch);

        wifiSwitch.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(KEY_WIFI, checked).apply());

        mobileDataSwitch = new Switch(this);

        mobileDataSwitch.setText("Allow downloads over Mobile Data");
        mobileDataSwitch.setTextColor(white);
        mobileDataSwitch.setTextSize(13);
        mobileDataSwitch.setChecked(prefs.getBoolean(KEY_MOBILE, true));

        settingsCard.addView(mobileDataSwitch);

        mobileDataSwitch.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(KEY_MOBILE, checked).apply());

        TextView qualityLabel =
                makeText("DOWNLOAD QUALITY", 11, gold, true);

        qualityLabel.setPadding(0, dp(12), 0, dp(4));
        settingsCard.addView(qualityLabel);

        qualitySpinner = new Spinner(this);

        String[] qualities = {"480p", "720p", "1080p"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                qualities);

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        qualitySpinner.setAdapter(adapter);

        String savedQuality =
                prefs.getString(KEY_QUALITY, "720p");

        int qualityIndex =
                Arrays.asList(qualities).indexOf(savedQuality);

        qualitySpinner.setSelection(Math.max(0, qualityIndex));

        settingsCard.addView(qualitySpinner);

        qualitySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view,
                                               int position, long id) {

                        prefs.edit()
                                .putString(KEY_QUALITY, qualities[position])
                                .apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        TextView qualityNote = makeText(
                "Demo video uses one fixed source; this setting is saved for future video sources.",
                10, gray, false);

        qualityNote.setPadding(0, dp(5), 0, dp(8));
        settingsCard.addView(qualityNote);

        refreshDownloads();
    }

    private void showManageStorageDialog() {

        File folder = getDownloadFolder();

        long free = folder == null ? 0 : folder.getUsableSpace();
        long downloaded = getTotalDownloadedBytes();

        String path = folder == null
                ? "Unavailable" : folder.getAbsolutePath();

        new AlertDialog.Builder(this)
                .setTitle("MANAGE STORAGE")
                .setMessage("Downloaded by CineWorld: " +
                        formatBytes(downloaded) +
                        "\nFree space: " + formatBytes(free) +
                        "\n\nSave location:\n" + path)
                .setPositiveButton("DONE", null)
                .setNeutralButton("DELETE ALL DOWNLOADS",
                        (dialog, which) ->

                                new AlertDialog.Builder(this)
                                        .setTitle("Delete all downloads?")
                                        .setMessage("All CineWorld downloaded videos will be removed.")
                                        .setNegativeButton("CANCEL", null)
                                        .setPositiveButton("DELETE",
                                                (d, w) -> {

                                                    if (downloadManager != null) {

                                                        for (Long id :
                                                                new ArrayList<>(downloadIds)) {

                                                            downloadManager.remove(id);
                                                        }
                                                    }

                                                    downloadIds.clear();
                                                    saveDownloadIds();
                                                    refreshDownloads();

                                                    showMessage(
                                                            "All downloads deleted");
                                                })
                                        .show())
                .show();
    }

    private void updateStorageMeter() {

        File folder = getDownloadFolder();

        if (folder == null || storageText == null ||
                storageProgress == null) return;

        long free = folder.getUsableSpace();

        long used = Math.max(0, initialFreeBytes - free);

        int percent = 0;

        if (initialFreeBytes > 0) {
            percent = (int) (used * 100L / initialFreeBytes);
        }

        percent = Math.max(0, Math.min(100, percent));

        storageProgress.setProgress(percent);

        storageText.setText(
                "Used since opening: " + percent
                        + "%   •   Free: " + formatBytes(free)
                        + "   •   Download budget: "
                        + formatBytes(storageLimitBytes));
    }

    private String formatBytes(long bytes) {

        if (bytes < 0) bytes = 0;

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024L * 1024L) {
            return String.format(java.util.Locale.US,
                    "%.1f KB", bytes / 1024.0);
        }

        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(java.util.Locale.US,
                    "%.1f MB", bytes / (1024.0 * 1024.0));
        }

        return String.format(java.util.Locale.US,
                "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static class DownloadEntry {

        long id, downloaded, total;
        int status;
        String title, localUri;
    }

    private void refreshDownloads() {

        if (downloadsList == null || downloadManager == null) return;

        downloadsList.removeAllViews();

        updateStorageMeter();

        ArrayList<DownloadEntry> active = new ArrayList<>();
        ArrayList<DownloadEntry> completed = new ArrayList<>();

        for (Long id : new ArrayList<>(downloadIds)) {

            DownloadManager.Query query =
                    new DownloadManager.Query().setFilterById(id);

            Cursor cursor = null;

            try {

                cursor = downloadManager.query(query);

                if (cursor == null || !cursor.moveToFirst()) continue;

                int statusColumn = cursor.getColumnIndex(
                        DownloadManager.COLUMN_STATUS);

                int downloadedColumn = cursor.getColumnIndex(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);

                int totalColumn = cursor.getColumnIndex(
                        DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                int titleColumn = cursor.getColumnIndex(
                        DownloadManager.COLUMN_TITLE);

                int uriColumn = cursor.getColumnIndex(
                        DownloadManager.COLUMN_LOCAL_URI);

                if (statusColumn < 0) continue;

                DownloadEntry entry = new DownloadEntry();

                entry.id = id;

                entry.status = cursor.getInt(statusColumn);

                entry.downloaded = downloadedColumn >= 0
                        ? cursor.getLong(downloadedColumn) : 0;

                entry.total = totalColumn >= 0
                        ? cursor.getLong(totalColumn) : -1;

                entry.title = titleColumn >= 0
                        ? cursor.getString(titleColumn) : "CineWorld Video";

                entry.localUri = uriColumn >= 0
                        ? cursor.getString(uriColumn) : null;

                if (storageLimitBytes > 0 &&
                        getTotalDownloadedBytes() > storageLimitBytes &&
                        entry.status == DownloadManager.STATUS_RUNNING) {

                    downloadManager.remove(id);

                    entry.status = DownloadManager.STATUS_FAILED;
                }

                if (entry.status == DownloadManager.STATUS_SUCCESSFUL) {
                    completed.add(entry);
                } else {
                    active.add(entry);
                }

            } catch (Exception ignored) {

            } finally {

                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (active.isEmpty()) {

            TextView empty = makeText(
                    "No active downloads.\nExplore Movies or Shows to download a video.",
                    12, gray, false);

            empty.setPadding(
                    dp(12), dp(12), dp(12), dp(18));

            downloadsList.addView(empty);

        } else {

            for (DownloadEntry entry : active) {
                addDownloadCard(entry);
            }
        }

        downloadsList.addView(sectionLabel("DOWNLOADED"));

        if (completed.isEmpty()) {

            TextView empty = makeText(
                    "No downloaded videos yet.", 12, gray, false);

            empty.setPadding(
                    dp(12), dp(8), dp(12), dp(18));

            downloadsList.addView(empty);

        } else {

            for (DownloadEntry entry : completed) {
                addDownloadCard(entry);
            }
        }
    }

    private void addDownloadCard(DownloadEntry entry) {

        long id = entry.id;
        String title = entry.title;
        int status = entry.status;
        long downloaded = entry.downloaded;
        long total = entry.total;
        String localUri = entry.localUri;

        LinearLayout card = verticalLayout();

        card.setPadding(
                dp(14), dp(13), dp(14), dp(13));

        card.setBackground(
                roundedBackground(0xFF1B1B23, 0xFF4C3B18, 15));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(-1, -2);

        cardParams.setMargins(0, dp(5), 0, dp(10));

        downloadsList.addView(card, cardParams);

        card.addView(makeText(title, 15, white, true));

        int progress = 0;

        if (total > 0) {
            progress = (int) (downloaded * 100L / total);
        }

        progress = Math.max(0, Math.min(100, progress));

        String statusText;

        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            statusText = "READY TO WATCH OFFLINE";

        } else if (status == DownloadManager.STATUS_FAILED) {
            statusText = "DOWNLOAD FAILED";

        } else if (status == DownloadManager.STATUS_PAUSED) {
            statusText = "DOWNLOAD PAUSED";

        } else {
            statusText = "DOWNLOADING  •  " + progress + "%";
        }

        TextView statusLabel =
                makeText(statusText, 11, gold, true);

        statusLabel.setPadding(0, dp(6), 0, dp(7));

        card.addView(statusLabel);

        ProgressBar progressBar = new ProgressBar(
                this, null,
                android.R.attr.progressBarStyleHorizontal);

        progressBar.setMax(100);
        progressBar.setProgress(progress);

        if (android.os.Build.VERSION.SDK_INT >= 21) {

            progressBar.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(gold));
        }

        card.addView(progressBar,
                new LinearLayout.LayoutParams(-1, dp(7)));

        TextView size = makeText(
                formatBytes(downloaded) + " / " +
                        (total > 0
                                ? formatBytes(total)
                                : "Unknown size"),
                10, gray, false);

        size.setPadding(0, dp(6), 0, dp(7));

        card.addView(size);

        if (status == DownloadManager.STATUS_SUCCESSFUL) {

            Button play = new Button(this);

            play.setText("PLAY OFFLINE");
            play.setTextColor(dark);

            play.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(gold));

            card.addView(play);

            play.setOnClickListener(v -> {

                if (localUri == null || localUri.isEmpty()) {
                    showMessage("Downloaded file not found");
                    return;
                }

                Intent intent = new Intent(
                        MainActivity.this, PlayerActivity.class);

                intent.putExtra("title", title);
                intent.putExtra("video_url", localUri);

                startActivity(intent);
            });

        } else if (status == DownloadManager.STATUS_FAILED) {

            Button retry = new Button(this);

            retry.setText(offlineMode || !hasInternetConnection()
                    ? "RETRY UNAVAILABLE OFFLINE" : "RETRY DOWNLOAD");
            retry.setEnabled(!offlineMode && hasInternetConnection());
            retry.setTextColor(dark);

            retry.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(gold));

            card.addView(retry);

            retry.setOnClickListener(v -> {

                downloadManager.remove(id);

                downloadIds.remove(id);
                saveDownloadIds();

                ContentItem retryItem =
                        new ContentItem(title, "", "", "", false);

                startMovieDownload(retryItem);
            });
        }

        Button delete = new Button(this);

        delete.setText("DELETE");
        delete.setTextColor(white);

        delete.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF4A2525));

        card.addView(delete);

        delete.setOnClickListener(v ->

                new AlertDialog.Builder(this)
                        .setTitle("Delete Download?")
                        .setMessage("Remove this video from CineWorld?")
                        .setNegativeButton("CANCEL", null)
                        .setPositiveButton("DELETE",
                                (dialog, which) -> {

                                    downloadManager.remove(id);

                                    downloadIds.remove(id);
                                    saveDownloadIds();
                                    refreshDownloads();

                                    showMessage("Download deleted");
                                })
                        .show());
    }

    // ----------------------------------------------------
    // PROFILE SECTION
    // ----------------------------------------------------

    private void setupProfileScreen() {

        if (binding == null) return;

        binding.profileContent.removeAllViews();

        android.content.SharedPreferences prefs =
                getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);

        LinearLayout profile = verticalLayout();

        profile.setPadding(20, 20, 20, 30);
        profile.setBackgroundColor(black);

        binding.profileContent.addView(profile);

        profile.addView(makeText("MY PROFILE", 25, gold, true));

        profile.addView(makeText(
                "Manage your CineWorld account", 13, gray, false));

        LinearLayout profileCard = verticalLayout();

        profileCard.setGravity(Gravity.CENTER);
        profileCard.setPadding(20, 25, 20, 25);

        profileCard.setBackground(
                roundedBackground(0xFF191920, gold, 22));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(-1, -2);

        cardParams.setMargins(0, 25, 0, 20);

        profile.addView(profileCard, cardParams);

        // Circular profile photo. Tap to choose an image from the Gallery.
        profileAvatarView = new ImageView(this);
        int avatarSize = dp(112);
        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(avatarSize, avatarSize);
        avatarParams.gravity = Gravity.CENTER;
        avatarParams.bottomMargin = dp(14);
        profileAvatarView.setLayoutParams(avatarParams);
        profileAvatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        profileAvatarView.setImageResource(android.R.drawable.sym_def_app_icon);
        profileAvatarView.setBackground(roundedBackground(0xFF25252B, gold, 100));
        profileAvatarView.setClipToOutline(true);
        profileAvatarView.setOnClickListener(v -> openProfilePhotoPicker());
        profileCard.addView(profileAvatarView);

        String savedPhoto = prefs.getString("profile_photo_uri", "");
        if (!savedPhoto.isEmpty()) {
            try {
                Glide.with(this).load(Uri.parse(savedPhoto))
                        .circleCrop().into(profileAvatarView);
            } catch (Exception ignored) {
                profileAvatarView.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }

        String savedName = prefs.getString(
                "profile_name", "CineWorld User");

        String savedEmail = prefs.getString(
                "profile_email", "Welcome to CineWorld");

        profileNameView = makeText(savedName, 21, white, true);

        profileNameView.setGravity(Gravity.CENTER);

        profileCard.addView(profileNameView);

        profileEmailView = makeText(savedEmail, 13, gray, false);

        profileEmailView.setGravity(Gravity.CENTER);

        profileCard.addView(profileEmailView);

        TextView membership =
                makeText("✦  CINEWORLD MEMBER  ✦", 14, gold, true);

        membership.setGravity(Gravity.CENTER);
        membership.setPadding(0, 15, 0, 0);

        profileCard.addView(membership);

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {

            Button loginButton = new Button(this);

            loginButton.setText("LOGIN / CREATE ACCOUNT");
            loginButton.setTextColor(dark);

            loginButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(gold));

            profile.addView(loginButton);

            loginButton.setOnClickListener(v -> {
                if (offlineMode || !hasInternetConnection()) {
                    showMessage("Login requires an internet connection.");
                } else {
                    showAuthScreen("login");
                }
            });

        } else {

            TextView signedIn =
                    makeText("✓ ACCOUNT SIGNED IN", 12, gold, true);

            signedIn.setPadding(0, 0, 0, 12);

            profile.addView(signedIn);
        }

        addProfileOption(profile, "✎", "Edit Profile",
                "Edit your profile");

        addProfileOption(profile, "♡", "My Watchlist",
                "Your watchlist will appear here.");

        addProfileOption(profile, "◷", "Watch History",
                "Your watch history will appear here.");

        addProfileOption(profile, "⬇", "My Downloads",
                "Your downloaded movies are in Downloads section.");

        addProfileOption(profile, "◖))", "Audio Languages",
                "Choose your preferred movie audio languages.");

        addProfileOption(profile, "⚙", "App Settings",
                "Settings will be available soon.");

        addProfileOption(profile, "?", "Help & Support",
                "CineWorld support.");

        addProfileOption(profile, "♧", "Privacy Policy",
                "CineWorld Privacy Policy.");

        if (currentUser != null && !offlineMode) {

            addProfileOption(profile, "⇥", "Logout",
                    "Logout from CineWorld");
        }

        TextView about = makeText(
                "CineWorld\nYour Movies. Your World.\nVersion 1.0 Demo",
                13, gray, false);

        about.setGravity(Gravity.CENTER);
        about.setPadding(0, 30, 0, 10);

        profile.addView(about);
    }

    private void showLocalSettingsDialog() {
        android.content.SharedPreferences prefs =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout content = verticalLayout();
        content.setPadding(dp(8), dp(8), dp(8), dp(4));

        TextView note = makeText(
                "These settings are saved on this phone and work offline.",
                12, gray, false);
        content.addView(note);

        Switch wifi = new Switch(this);
        wifi.setText("Allow downloads over Wi-Fi");
        wifi.setTextColor(white);
        wifi.setChecked(prefs.getBoolean(KEY_WIFI, true));
        content.addView(wifi);
        wifi.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(KEY_WIFI, checked).apply());

        Switch mobile = new Switch(this);
        mobile.setText("Allow downloads over Mobile Data");
        mobile.setTextColor(white);
        mobile.setChecked(prefs.getBoolean(KEY_MOBILE, true));
        content.addView(mobile);
        mobile.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(KEY_MOBILE, checked).apply());

        TextView qualityTitle = makeText("DOWNLOAD QUALITY", 12, gold, true);
        qualityTitle.setPadding(0, dp(14), 0, dp(4));
        content.addView(qualityTitle);

        Spinner quality = new Spinner(this);
        String[] values = {"480p", "720p", "1080p"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        quality.setAdapter(adapter);
        String saved = prefs.getString(KEY_QUALITY, "720p");
        quality.setSelection(Math.max(0, Arrays.asList(values).indexOf(saved)));
        content.addView(quality);
        quality.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                prefs.edit().putString(KEY_QUALITY, values[position]).apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        new AlertDialog.Builder(this)
                .setTitle("CINEWORLD SETTINGS")
                .setView(content)
                .setPositiveButton("SAVE", null)
                .show();
    }

    private void addProfileOption(LinearLayout parent,
                                  String icon, String title,
                                  String message) {

        LinearLayout item = horizontalLayout();

        item.setPadding(16, 16, 16, 16);

        item.setBackground(
                roundedBackground(0xFF191920, 0xFF45403A, 14));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.setMargins(0, 0, 0, 10);

        parent.addView(item, params);

        item.addView(makeText(icon, 23, gold, true));

        TextView titleView =
                makeText(title, 15, white, true);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(0, -2, 1);

        titleParams.setMargins(18, 0, 0, 0);

        item.addView(titleView, titleParams);

        item.addView(makeText("›", 25, gold, true));

        item.setOnClickListener(v -> {

            if (title.equals("Edit Profile")) {

                if (offlineMode || !hasInternetConnection()) {
                    showMessage("Profile editing requires an internet connection.");
                    return;
                }

                startActivity(new Intent(
                        MainActivity.this,
                        EditProfileActivity.class));

            } else if (title.equals("App Settings")) {

                showLocalSettingsDialog();

            } else if (title.equals("Audio Languages")) {

                showAudioLanguageScreen(true);

            } else if (title.equals("Logout")) {

                logoutUser();

            } else if (title.equals("My Downloads")) {

                selectTab(3);

            } else {

                Toast.makeText(MainActivity.this,
                        message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ----------------------------------------------------
    // HEADER
    // ----------------------------------------------------

    private void setupHeader() {

        // Apply the drawable created at res/drawable/cw_header_circle.xml.
        binding.searchButton.setBackgroundResource(R.drawable.cw_header_circle);
        binding.notificationButton.setBackgroundResource(R.drawable.cw_header_circle);
        binding.profileButton.setBackgroundResource(R.drawable.cw_header_circle);

        binding.searchButton.setOnClickListener(v -> {

            EditText input = new EditText(this);

            input.setHint("Search Movies and Shows");

            new AlertDialog.Builder(this)
                    .setTitle("CineWorld Search")
                    .setView(input)
                    .setPositiveButton("Search", (dialog, which) -> {

                        String query =
                                input.getText().toString().trim();

                        if (query.isEmpty()) {
                            showMessage("Enter a name");
                            return;
                        }

                        searchCatalog(query);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        binding.notificationButton.setOnClickListener(v ->

                new AlertDialog.Builder(this)
                        .setTitle("Notifications")
                        .setMessage("No new notifications")
                        .setPositiveButton("OK", null)
                        .show());

        binding.profileButton.setOnClickListener(v ->
                selectTab(4));
    }

    private void startGoogleSignIn() {
        if (authBusy) return;
        try {
            GoogleSignInOptions options = new GoogleSignInOptions.Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            GoogleSignInClient client = GoogleSignIn.getClient(this, options);
            startActivityForResult(client.getSignInIntent(), RC_GOOGLE_SIGN_IN);
        } catch (Exception e) {
            showMessage("Google sign-in setup error: " + e.getMessage());
        }
    }

    private void openProfilePhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Choose profile photo"),
                    REQUEST_PROFILE_PHOTO);
        } catch (Exception e) {
            showMessage("Gallery could not be opened.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // GOOGLE SIGN-IN RESULT
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            authBusy = false;

            if (data == null) {
                showMessage("Google Sign-In: No data returned");
                return;
            }

            try {
                GoogleSignInAccount account =
                        GoogleSignIn.getSignedInAccountFromIntent(data)
                                .getResult(ApiException.class);

                if (account == null) {
                    showMessage("Google account is null");
                    return;
                }

                String idToken = account.getIdToken();
                if (idToken == null || idToken.isEmpty()) {
                    showMessage("Google ID Token Missing");
                    return;
                }

                authBusy = true;
                mAuth.signInWithCredential(
                        GoogleAuthProvider.getCredential(idToken, null)
                ).addOnCompleteListener(task -> {
                    authBusy = false;
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            verifyAndRouteSession(user);
                        } else {
                            showMessage("Firebase User Not Found");
                        }
                    } else {
                        showMessage("Firebase Error: " + authError(task.getException()));
                    }
                });

            } catch (ApiException e) {
                authBusy = false;
                showMessage("Google Error Code: " + e.getStatusCode()
                        + " | " + e.getStatusMessage());
            }
            return;
        }

        // PROFILE PHOTO RESULT
        if (requestCode == REQUEST_PROFILE_PHOTO
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            Uri photoUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(
                        photoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
                // Some document providers do not support persistable grants.
            }

            getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE).edit()
                    .putString("profile_photo_uri", photoUri.toString())
                    .apply();

            if (profileAvatarView != null) {
                Glide.with(this).load(photoUri).circleCrop().into(profileAvatarView);
            }
            showMessage("Profile photo updated.");
        }
    }

    private void searchCatalog(String query) {

        String key = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        if (key.isEmpty()) {
            showMessage("Enter a name");
            return;
        }

        ArrayList<ContentItem> matches = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (ContentItem item : movies) {
            if (matchesSearch(item, key)) {
                matches.add(item);
                labels.add("MOVIE  •  " + item.title + "\n" + item.genre);
            }
        }

        for (ContentItem item : shows) {
            if (matchesSearch(item, key)) {
                matches.add(item);
                labels.add("SHOW  •  " + item.title + "\n" + item.genre);
            }
        }

        if (matches.isEmpty()) {
            showMessage("No matching movie or show found");
            return;
        }

        String[] results = labels.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("SEARCH RESULTS (" + matches.size() + ")")
                .setItems(results, (dialog, which) -> {
                    ContentItem selected = matches.get(which);
                    selectTab(selected.series ? 2 : 1);
                    showDetails(selected);
                })
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private boolean matchesSearch(ContentItem item, String key) {
        return (item.title != null && item.title.toLowerCase(java.util.Locale.ROOT).contains(key))
                || (item.genre != null && item.genre.toLowerCase(java.util.Locale.ROOT).contains(key))
                || (item.description != null && item.description.toLowerCase(java.util.Locale.ROOT).contains(key));
    }

    // ----------------------------------------------------
    // HELPERS
    // ----------------------------------------------------

    private void showMessage(String message) {

        Toast.makeText(this,
                message, Toast.LENGTH_SHORT).show();
    }

    // ----------------------------------------------------
    // LIFECYCLE
    // ----------------------------------------------------

    @Override
    public void onBackPressed() {

        if (languageOverlay != null) {
            showMessage("Please choose your audio languages to continue.");
            return;
        }

        if (offlineMode) {
            finish();
            return;
        }

        if (mAuth != null && mAuth.getCurrentUser() == null) {

            authGateActive = true;

            if (authOverlay == null) {
                showAuthScreen("login");
            }

            return;
        }

        if (authGateActive || sessionCheckPending || sessionCheckFailed) {
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onResume() {

        super.onResume();

        startDownloadMonitoring();

        if (offlineMode) {
            setAppContentLocked(false);
            refreshDownloads();
            return;
        }

        if (mAuth == null) return;

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {

            sessionCheckPending = false;
            sessionCheckFailed = false;

            authGateActive = true;

            setAppContentLocked(true);

            if (authOverlay == null) {
                showAuthScreen("login");
            }

        } else if (sessionCheckPending) {

            authGateActive = true;
            setAppContentLocked(true);

        } else if (sessionCheckFailed) {

            authGateActive = true;
            setAppContentLocked(true);

        } else if (authGateActive) {

            if (!user.isEmailVerified() && !isGoogleProvider(user)) {
                showAuthScreen("verify");
                user.reload().addOnCompleteListener(task -> {
                    FirebaseUser refreshed = mAuth.getCurrentUser();
                    if (refreshed != null && refreshed.isEmailVerified()) {
                        verifyAndRouteSession(refreshed);
                    }
                });
            } else {
                verifyAndRouteSession(user);
            }

        } else {

            setAppContentLocked(false);
        }
    }

    @Override
    protected void onStop() {

        stopDownloadMonitoring();

        if (player != null) {
            player.pause();
        }

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        if (skeletonOverlay != null) {
            ViewGroup parent = (ViewGroup) skeletonOverlay.getParent();
            if (parent != null) parent.removeView(skeletonOverlay);
            skeletonOverlay = null;
        }

        stopDownloadMonitoring();

        if (player != null) {
            player.release();
            player = null;
        }

        binding = null;

        super.onDestroy();
    }
}
