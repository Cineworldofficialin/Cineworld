package com.example.cineworld;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean navigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(8, 8, 8)));
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splashLogo);
        TextView title = findViewById(R.id.splashTitle);
        ProgressBar progress = findViewById(R.id.splashProgress);

        logo.setAlpha(0f);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(1200).start();

        if (title != null) {
            title.setAlpha(0f);
            title.animate().alpha(1f).setDuration(900).setStartDelay(250).start();
        }

        if (progress != null) {
            progress.setVisibility(View.VISIBLE);
        }

        // Keep the branded splash visible briefly, then check real internet access.
        handler.postDelayed(() -> {
            if (hasInternetConnection()) {
                openMain(false);
            } else {
                showOfflineScreen();
            }
        }, 1600);
    }

    private boolean hasInternetConnection() {
        try {
            ConnectivityManager manager =
                    (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (manager == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = manager.getActiveNetwork();
                if (network == null) return false;
                NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
                return capabilities != null
                        && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }

            NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void showOfflineScreen() {
        if (isFinishing() || navigating) return;

        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(Color.rgb(8, 8, 8));
        root.setPadding(dp(28), dp(24), dp(28), dp(24));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);

        RelativeLayout.LayoutParams contentParams =
                new RelativeLayout.LayoutParams(-1, -2);
        contentParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        root.addView(content, contentParams);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.cineworld_icon);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(dp(104), dp(104));
        logoParams.bottomMargin = dp(22);
        content.addView(logo, logoParams);

        ImageView wifiOff = new ImageView(this);
        wifiOff.setImageResource(R.drawable.ic_wifi_off);
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(62), dp(62));
        iconParams.bottomMargin = dp(16);
        content.addView(wifiOff, iconParams);

        TextView heading = makeText("NO INTERNET CONNECTION", 19, 0xFFFFC107, true);
        heading.setGravity(Gravity.CENTER);
        content.addView(heading);

        TextView message = makeText(
                "Please check your Wi-Fi or mobile data.\nYou can still watch videos saved on this device.",
                13, 0xFFAAAAAA, false);
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(dp(5), 1f);
        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(-1, -2);
        messageParams.topMargin = dp(12);
        messageParams.bottomMargin = dp(25);
        content.addView(message, messageParams);

        Button retry = makeButton("RETRY CONNECTION");
        content.addView(retry, buttonParams());
        retry.setOnClickListener(v -> {
            if (hasInternetConnection()) {
                openMain(false);
            } else {
                // As requested: if retry still fails, open the local Downloads tab.
                openMain(true);
            }
        });

        Button downloads = makeButton("OPEN DOWNLOADS");
        downloads.setTextColor(0xFFFFC107);
        downloads.setBackground(roundedBackground(0xFF171717, 0xFFFFC107, 12));
        LinearLayout.LayoutParams downloadsParams = buttonParams();
        downloadsParams.topMargin = dp(12);
        content.addView(downloads, downloadsParams);
        downloads.setOnClickListener(v -> openMain(true));

        TextView footer = makeText("CINEWORLD  •  YOUR CINEMA EXPERIENCE", 10,
                0xFF777777, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(28);
        content.addView(footer, footerParams);

        setContentView(root);
    }

    private void openMain(boolean offline) {
        if (navigating || isFinishing()) return;
        navigating = true;
        handler.removeCallbacksAndMessages(null);

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("offline_mode", offline);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private Button makeButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(0xFF101018);
        button.setAllCaps(false);
        button.setBackground(roundedBackground(0xFFFFC107, 0xFFFFC107, 12));
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        return new LinearLayout.LayoutParams(-1, dp(50));
    }

    private TextView makeText(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(null, Typeface.BOLD);
        return text;
    }

    private GradientDrawable roundedBackground(int color, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
