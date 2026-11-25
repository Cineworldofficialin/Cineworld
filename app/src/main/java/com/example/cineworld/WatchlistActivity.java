package com.example.cineworld;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class WatchlistActivity extends AppCompatActivity {

    private static final int GOLD = 0xFFFFC107;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int DARK = 0xFF101014;

    private LinearLayout itemsContainer;
    private TextView countView;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buildScreen();
        loadWatchlist();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(DARK);
        root.setPadding(18, 18, 18, 12);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(38);
        back.setTextColor(GOLD);
        back.setGravity(Gravity.CENTER);
        header.addView(back, new LinearLayout.LayoutParams(48, 55));
        back.setOnClickListener(v -> finish());

        TextView heading = new TextView(this);
        heading.setText("MY WATCHLIST");
        heading.setTextColor(WHITE);
        heading.setTextSize(20);
        heading.setTypeface(null, Typeface.BOLD);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));

        root.addView(header);

        countView = new TextView(this);
        countView.setText("YOUR SAVED MOVIES & SHOWS");
        countView.setTextColor(GOLD);
        countView.setTextSize(12);
        countView.setPadding(4, 12, 4, 16);
        root.addView(countView);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        itemsContainer = new LinearLayout(this);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(itemsContainer);

        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1
        ));

        setContentView(root);
    }

    private void loadWatchlist() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showEmpty("Please login to view your Watchlist.");
            return;
        }

        itemsContainer.removeAllViews();

        TextView loading = makeText("Loading your Watchlist...", GRAY, 15);
        itemsContainer.addView(loading);

        db.collection("users")
                .document(user.getUid())
                .collection("watchlist")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    itemsContainer.removeAllViews();

                    int total = snapshot.size();
                    countView.setText(total + " SAVED MOVIES & SHOWS");

                    if (total == 0) {
                        showEmpty("Your Watchlist is empty.\n\nExplore movies and tap ADD TO WATCHLIST.");
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        addWatchlistItem(doc);
                    }
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    showEmpty("Unable to load Watchlist.\n" + error.getMessage());
                });
    }

    private void addWatchlistItem(DocumentSnapshot doc) {
        String title = doc.getString("title");
        String genre = doc.getString("genre");
        String description = doc.getString("description");
        String image = doc.getString("image");
        Boolean seriesValue = doc.getBoolean("series");

        boolean isSeries = seriesValue != null && seriesValue;

        if (title == null) title = "CineWorld Content";
        if (genre == null) genre = "Entertainment";
        if (description == null) description = "Enjoy this content.";
        if (image == null) image = "";

        final String itemTitle = title;
        final String itemGenre = genre;
        final String itemDescription = description;
        final String itemImage = image;
        final boolean itemIsSeries = isSeries;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(10, 10, 10, 10);

        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFF1D1D24);
        background.setCornerRadius(18);
        background.setStroke(1, 0xFF55430A);
        card.setBackground(background);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(-1, 126);
        cardParams.bottomMargin = 12;
        itemsContainer.addView(card, cardParams);

        ImageView poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout.LayoutParams posterParams =
                new LinearLayout.LayoutParams(82, 106);
        posterParams.rightMargin = 12;
        card.addView(poster, posterParams);

        Glide.with(this)
                .load(itemImage)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(poster);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);

        card.addView(details, new LinearLayout.LayoutParams(
                0, -2, 1
        ));

        TextView name = makeText(itemTitle, WHITE, 15);
        name.setTypeface(null, Typeface.BOLD);
        details.addView(name);

        TextView category = makeText(
                (itemIsSeries ? "TV SHOW" : "MOVIE") + "  •  " + itemGenre,
                GOLD, 11
        );
        category.setPadding(0, 5, 0, 7);
        details.addView(category);

        TextView open = makeText("VIEW DETAILS  ›", WHITE, 12);
        open.setTypeface(null, Typeface.BOLD);
        details.addView(open);

        card.setOnClickListener(v ->
                openDetails(itemTitle, itemGenre,
                        itemDescription, itemImage, itemIsSeries));

        Button remove = new Button(this);
        remove.setText("REMOVE");
        remove.setTextSize(10);
        remove.setTextColor(GOLD);
        remove.setAllCaps(true);
        remove.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF303039)
        );

        LinearLayout.LayoutParams removeParams =
                new LinearLayout.LayoutParams(-2, 42);
        removeParams.leftMargin = 5;
        card.addView(remove, removeParams);

        remove.setOnClickListener(v -> {
            remove.setEnabled(false);

            doc.getReference().delete()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this,
                                "Removed from Watchlist",
                                Toast.LENGTH_SHORT).show();
                        loadWatchlist();
                    })
                    .addOnFailureListener(error -> {
                        remove.setEnabled(true);
                        Toast.makeText(this,
                                "Remove failed: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void openDetails(String title, String genre,
                             String description, String image,
                             boolean isSeries) {
        Intent intent = new Intent(this, MovieDetailsActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("genre", genre);
        intent.putExtra("description", description);
        intent.putExtra("image", image);
        intent.putExtra("series", isSeries);
        startActivity(intent);
    }

    private void showEmpty(String message) {
        itemsContainer.removeAllViews();

        TextView empty = makeText(
                "☆\n\n" + message,
                GRAY, 16
        );
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(20, 90, 20, 50);
        itemsContainer.addView(empty);
    }

    private TextView makeText(String value, int color, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(size);
        return text;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (itemsContainer != null) {
            loadWatchlist();
        }
    }
}
