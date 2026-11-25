package com.example.cineworld;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    private final int BG = Color.rgb(9, 9, 9);
    private final int CARD = Color.rgb(24, 24, 24);
    private final int GOLD = Color.rgb(240, 196, 79);
    private final int WHITE = Color.WHITE;
    private final int GRAY = Color.rgb(165, 165, 165);
    private final int BORDER = Color.rgb(91, 72, 35);

    private FrameLayout drawerOverlay;
    private LinearLayout drawerPanel;
    private LinearLayout menuContainer;
    private LinearLayout contentContainer;

    private boolean drawerOpen = false;
    private String currentPage = "Dashboard";
    private int drawerWidth;

    private final String[] mainFeatures = {
            "Dashboard",
            "Users",
            "Mailbox",
            "Categories",
            "Dramas",
            "Notification",
            "Reports",
            "Coin Packs",
            "Rewards System",
            "Ads",
            "Pages"
    };

    private final String[] settingsFeatures = {
            "General",
            "SEO & Metadata",
            "Performance & PWA",
            "Plugins",
            "Authentication",
            "Backup & Restore",
            "Payment Gateway",
            "Languages",
            "Mobile App",
            "Storage"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admin_dashboard);

        drawerOverlay = findViewById(R.id.drawerOverlay);
        drawerPanel = findViewById(R.id.drawerPanel);
        menuContainer = findViewById(R.id.menuContainer);
        contentContainer = findViewById(R.id.contentContainer);

        View menuButton = findViewById(R.id.menuButton);

        if (drawerOverlay == null || drawerPanel == null ||
                menuContainer == null || contentContainer == null ||
                menuButton == null) {
            finish();
            return;
        }

        drawerWidth = dp(290);

        ViewGroup.LayoutParams params = drawerPanel.getLayoutParams();
        params.width = drawerWidth;
        drawerPanel.setLayoutParams(params);

        drawerPanel.setTranslationX(-drawerWidth);
        drawerPanel.setVisibility(View.GONE);

        menuButton.setOnClickListener(v -> {
            if (drawerOpen) closeDrawer();
            else openDrawer();
        });

        drawerOverlay.setOnClickListener(v -> closeDrawer());

        setupMenu();
        showPage("Dashboard");

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (drawerOpen) {
                            closeDrawer();
                        } else if (!currentPage.equals("Dashboard")) {
                            showPage("Dashboard");
                        } else {
                            finish();
                        }
                    }
                }
        );
    }

    // ---------------- DRAWER ----------------

    private void setupMenu() {
        menuContainer.removeAllViews();

        addSection("MAIN MENU");

        for (String item : mainFeatures) {
            addMenuItem(item);
        }

        addSection("SETTINGS");

        for (String item : settingsFeatures) {
            addMenuItem(item);
        }
    }

    private void addSection(String title) {
        TextView section = new TextView(this);
        section.setText(title);
        section.setTextColor(GOLD);
        section.setTextSize(10);
        section.setTypeface(null, Typeface.BOLD);
        section.setPadding(dp(17), dp(19), dp(8), dp(9));
        menuContainer.addView(section);
    }

    private void addMenuItem(String title) {
        TextView item = new TextView(this);
        item.setText(getIcon(title) + "   " + title);
        item.setTextSize(13);
        item.setTextColor(WHITE);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(15), 0, dp(8), 0);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        lp.setMargins(dp(8), dp(3), dp(8), dp(3));

        menuContainer.addView(item, lp);
        updateMenuItem(item, title);

        item.setOnClickListener(v -> {
            showPage(title);
            closeDrawer();
        });
    }

    private void updateMenuItem(TextView item, String title) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(9));

        if (currentPage.equals(title)) {
            bg.setColor(Color.rgb(54, 43, 22));
            bg.setStroke(dp(1), GOLD);
            item.setTextColor(GOLD);
        } else {
            bg.setColor(CARD);
            item.setTextColor(WHITE);
        }

        item.setBackground(bg);
    }

    private void refreshMenuSelection() {
        for (int i = 0; i < menuContainer.getChildCount(); i++) {
            View view = menuContainer.getChildAt(i);

            if (view instanceof TextView) {
                TextView item = (TextView) view;

                String text = item.getText().toString();
                for (String feature : mainFeatures) {
                    if (text.endsWith(feature)) {
                        updateMenuItem(item, feature);
                    }
                }
                for (String feature : settingsFeatures) {
                    if (text.endsWith(feature)) {
                        updateMenuItem(item, feature);
                    }
                }
            }
        }
    }

    private void openDrawer() {
        if (drawerOpen) return;

        drawerOpen = true;
        drawerOverlay.setVisibility(View.VISIBLE);
        drawerPanel.setVisibility(View.VISIBLE);
        drawerPanel.setTranslationX(-drawerWidth);

        drawerPanel.animate()
                .translationX(0)
                .setDuration(250)
                .start();
    }

    private void closeDrawer() {
        if (!drawerOpen) return;

        drawerOpen = false;

        drawerPanel.animate()
                .translationX(-drawerWidth)
                .setDuration(220)
                .withEndAction(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    drawerPanel.setVisibility(View.GONE);
                    drawerOverlay.setVisibility(View.GONE);
                })
                .start();
    }

    // ---------------- PAGE NAVIGATION ----------------

    private void showPage(String title) {
        currentPage = title;
        contentContainer.removeAllViews();

        refreshMenuSelection();

        if (title.equals("Dashboard")) {
            buildDashboard();
        } else {
            buildModulePage(title);
        }
    }

    // ---------------- DASHBOARD HOME ----------------

    private void buildDashboard() {
        addHeading("ADMIN OVERVIEW");
        addSubtitle("Welcome back to CineWorld Control Center");

        addBanner(
                "CINEWORLD ADMIN",
                "Manage your OTT platform from one place."
        );

        addSectionTitle("PLATFORM OVERVIEW");

        LinearLayout row1 = createRow();
        addMetric(row1, "TOTAL USERS", "—", "Registered accounts");
        addMetric(row1, "TOTAL DRAMAS", "—", "Content library");
        contentContainer.addView(row1);

        LinearLayout row2 = createRow();
        addMetric(row2, "COIN SALES", "—", "Payment overview");
        addMetric(row2, "REPORTS", "—", "Platform activity");
        contentContainer.addView(row2);

        addSectionTitle("QUICK ACCESS");

        addQuickCard("Users", "Manage registered accounts");
        addQuickCard("Dramas", "Manage dramas and episodes");
        addQuickCard("Notification", "Send user notifications");
        addQuickCard("Reports", "View platform reports");
        addQuickCard("Coin Packs", "Manage coin packages");
        addQuickCard("Storage", "Manage media storage");

        addSectionTitle("ADMIN MODULES");

        addInfoCard(
                "21 MODULES AVAILABLE",
                "11 Main Menu modules  •  10 Settings modules"
        );

        addInfoCard(
                "SECURE ADMIN AREA",
                "Only authorized administrators should access this panel."
        );

        addFooter();
    }

    private LinearLayout createRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));
        return row;
    }

    private void addMetric(LinearLayout row, String label,
                           String value, String description) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(15), dp(10), dp(15));

        setCardBackground(card, CARD, BORDER);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));

        row.addView(card, lp);

        TextView name = makeText(label, 10, GOLD, true);
        card.addView(name);

        TextView number = makeText(value, 25, WHITE, true);
        number.setPadding(0, dp(8), 0, dp(4));
        card.addView(number);

        TextView detail = makeText(description, 10, GRAY, false);
        card.addView(detail);
    }

    private void addQuickCard(String title, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(13), dp(15), dp(13));
        setCardBackground(card, CARD, BORDER);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));

        contentContainer.addView(card, lp);

        TextView heading = makeText(
                getIcon(title) + "   " + title + "   ›",
                15, GOLD, true);
        card.addView(heading);

        TextView sub = makeText(description, 11, GRAY, false);
        sub.setPadding(0, dp(6), 0, 0);
        card.addView(sub);

        card.setOnClickListener(v -> showPage(title));
    }

    // ---------------- MODULE PAGES ----------------

    private void buildModulePage(String title) {
        addHeading(title.toUpperCase());
        addSubtitle(getDescription(title));

        addBanner(title, getDescription(title));

        addSectionTitle("MODULE OVERVIEW");

        String[] actions = getActions(title);

        for (String action : actions) {
            addActionCard(action);
        }

        addSectionTitle("MODULE STATUS");

        addInfoCard(
                "READY FOR CONFIGURATION",
                "The " + title + " interface is prepared. Firebase functionality will be connected in the next stage."
        );

        addFooter();
    }

    private String[] getActions(String title) {
        switch (title) {

            case "Users":
                return new String[]{
                        "All Registered Users",
                        "Search Users",
                        "User Details",
                        "Active Accounts",
                        "Blocked Accounts"
                };

            case "Mailbox":
                return new String[]{
                        "All Messages",
                        "Unread Messages",
                        "Support Requests",
                        "Replied Messages"
                };

            case "Categories":
                return new String[]{
                        "All Categories",
                        "Add New Category",
                        "Edit Categories",
                        "Delete Categories"
                };

            case "Dramas":
                return new String[]{
                        "All Dramas",
                        "Add New Drama",
                        "Manage Seasons",
                        "Manage Episodes",
                        "Featured Dramas"
                };

            case "Notification":
                return new String[]{
                        "Send Notification",
                        "Notification History",
                        "Scheduled Notifications",
                        "Push Notification Settings"
                };

            case "Reports":
                return new String[]{
                        "User Reports",
                        "Content Reports",
                        "Revenue Reports",
                        "Activity Reports"
                };

            case "Coin Packs":
                return new String[]{
                        "All Coin Packs",
                        "Add Coin Pack",
                        "Edit Coin Pack",
                        "Coin Purchase History"
                };

            case "Rewards System":
                return new String[]{
                        "Daily Rewards",
                        "Watch Rewards",
                        "Bonus Coins",
                        "Reward History"
                };

            case "Ads":
                return new String[]{
                        "Ad Settings",
                        "Banner Ads",
                        "Interstitial Ads",
                        "Rewarded Ads"
                };

            case "Pages":
                return new String[]{
                        "About Us",
                        "Privacy Policy",
                        "Terms & Conditions",
                        "Contact Us"
                };

            case "General":
                return new String[]{
                        "App Name",
                        "App Logo",
                        "App Details",
                        "Maintenance Mode"
                };

            case "SEO & Metadata":
                return new String[]{
                        "SEO Title",
                        "Meta Description",
                        "Keywords",
                        "Social Metadata"
                };

            case "Performance & PWA":
                return new String[]{
                        "Performance Settings",
                        "Cache Settings",
                        "PWA Settings",
                        "Optimization"
                };

            case "Plugins":
                return new String[]{
                        "Installed Plugins",
                        "Available Plugins",
                        "Plugin Settings"
                };

            case "Authentication":
                return new String[]{
                        "Login Settings",
                        "Registration Settings",
                        "Password Settings",
                        "Account Verification"
                };

            case "Backup & Restore":
                return new String[]{
                        "Create Backup",
                        "Available Backups",
                        "Restore Backup",
                        "Backup Settings"
                };

            case "Payment Gateway":
                return new String[]{
                        "Payment Providers",
                        "Gateway Configuration",
                        "Payment History",
                        "Transaction Settings"
                };

            case "Languages":
                return new String[]{
                        "Available Languages",
                        "Default Language",
                        "Add Language",
                        "Translation Settings"
                };

            case "Mobile App":
                return new String[]{
                        "Android App Settings",
                        "App Version",
                        "Update Information",
                        "Mobile Configuration"
                };

            case "Storage":
                return new String[]{
                        "Storage Overview",
                        "Posters & Images",
                        "Video Files",
                        "Storage Settings"
                };

            default:
                return new String[]{
                        "Overview",
                        "Settings",
                        "Manage Module"
                };
        }
    }

    private String getDescription(String title) {
        switch (title) {
            case "Users":
                return "Manage registered users and account status.";
            case "Mailbox":
                return "Manage messages and support requests.";
            case "Categories":
                return "Create and manage content categories.";
            case "Dramas":
                return "Manage dramas, seasons and episodes.";
            case "Notification":
                return "Manage notifications for CineWorld users.";
            case "Reports":
                return "Review platform activity and reports.";
            case "Coin Packs":
                return "Manage coin packages and pricing.";
            case "Rewards System":
                return "Manage rewards and bonus coins.";
            case "Ads":
                return "Manage advertisements and placements.";
            case "Pages":
                return "Manage information and policy pages.";
            case "General":
                return "Manage general application settings.";
            case "SEO & Metadata":
                return "Manage SEO and metadata information.";
            case "Performance & PWA":
                return "Manage performance and PWA settings.";
            case "Plugins":
                return "Manage application plugins.";
            case "Authentication":
                return "Manage login and registration settings.";
            case "Backup & Restore":
                return "Manage database backup and restoration.";
            case "Payment Gateway":
                return "Manage payment gateway configuration.";
            case "Languages":
                return "Manage supported application languages.";
            case "Mobile App":
                return "Manage Android application settings.";
            case "Storage":
                return "Manage media files and storage.";
            default:
                return "CineWorld Admin Control Panel.";
        }
    }

    // ---------------- REUSABLE UI ----------------

    private void addHeading(String text) {
        TextView view = makeText(text, 19, GOLD, true);
        view.setPadding(0, dp(5), 0, dp(5));
        contentContainer.addView(view);
    }

    private void addSubtitle(String text) {
        TextView view = makeText(text, 12, GRAY, false);
        view.setPadding(0, dp(3), 0, dp(16));
        contentContainer.addView(view);
    }

    private void addSectionTitle(String text) {
        TextView view = makeText(text, 12, GOLD, true);
        view.setPadding(dp(2), dp(20), 0, dp(9));
        contentContainer.addView(view);
    }

    private void addBanner(String title, String description) {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(dp(18), dp(20), dp(18), dp(20));

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.rgb(48, 37, 17),
                        Color.rgb(25, 23, 18)
                });
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), BORDER);
        banner.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(7));

        contentContainer.addView(banner, lp);

        banner.addView(makeText(title, 19, GOLD, true));

        TextView sub = makeText(description, 12, WHITE, false);
        sub.setPadding(0, dp(9), 0, 0);
        banner.addView(sub);
    }

    private void addActionCard(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(15), dp(15), dp(12), dp(15));
        setCardBackground(card, CARD, BORDER);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));

        contentContainer.addView(card, lp);

        TextView icon = makeText("◈", 18, GOLD, true);
        card.addView(icon);

        TextView name = makeText(title, 13, WHITE, true);
        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.setMargins(dp(12), 0, 0, 0);
        card.addView(name, nameParams);

        card.addView(makeText("›", 22, GOLD, true));
    }

    private void addInfoCard(String title, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(15), dp(15), dp(15));
        setCardBackground(card, CARD, BORDER);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));

        contentContainer.addView(card, lp);

        card.addView(makeText(title, 13, GOLD, true));

        TextView sub = makeText(description, 11, GRAY, false);
        sub.setPadding(0, dp(7), 0, 0);
        card.addView(sub);
    }

    private TextView makeText(String value, float size,
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

    private void setCardBackground(View view, int color, int stroke) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), stroke);
        view.setBackground(bg);
    }

    private String getIcon(String title) {
        switch (title) {
            case "Dashboard": return "⌂";
            case "Users": return "♙";
            case "Mailbox": return "✉";
            case "Categories": return "▤";
            case "Dramas": return "▶";
            case "Notification": return "♢";
            case "Reports": return "▥";
            case "Coin Packs": return "◉";
            case "Rewards System": return "★";
            case "Ads": return "▣";
            case "Pages": return "▤";
            case "General": return "⚙";
            case "SEO & Metadata": return "⌕";
            case "Performance & PWA": return "⚡";
            case "Plugins": return "◈";
            case "Authentication": return "♙";
            case "Backup & Restore": return "⟳";
            case "Payment Gateway": return "₹";
            case "Languages": return "文";
            case "Mobile App": return "▣";
            case "Storage": return "▤";
            default: return "◈";
        }
    }

    private void addFooter() {
        TextView footer = makeText(
                "CINEWORLD ADMIN  •  CONTROL CENTER",
                10, GRAY, true);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(28), 0, dp(18));
        contentContainer.addView(footer);
    }

    private int dp(float value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (drawerPanel != null) {
            drawerPanel.animate().cancel();
        }
        super.onDestroy();
    }
}
