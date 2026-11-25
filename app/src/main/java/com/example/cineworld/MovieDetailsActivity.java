package com.example.cineworld;

import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/**
 * Shared movie-details screen opened from MainActivity and WatchlistActivity.
 * Intent extras: title, genre, description, image, series, video_url.
 */
@UnstableApi
public class MovieDetailsActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private TextView statusView;

    private final int background = 0xFF101018;
    private final int gold = 0xFFFFC107;
    private final int white = 0xFFFFFFFF;
    private final int gray = 0xFFAAAAAA;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String title = getIntent().getStringExtra("title");
        String genre = getIntent().getStringExtra("genre");
        String description = getIntent().getStringExtra("description");
        String image = getIntent().getStringExtra("image");
        String videoUrl = getIntent().getStringExtra("video_url");

        if (title == null || title.trim().isEmpty()) title = "Movie Details";
        if (genre == null) genre = "";
        if (description == null) description = "";

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(22));
        page.setBackgroundColor(background);

        TextView back = makeText("‹  BACK", 15, gold, true);
        back.setPadding(0, dp(5), 0, dp(15));
        back.setOnClickListener(v -> finish());
        page.addView(back, new LinearLayout.LayoutParams(-1, -2));

        TextView heading = makeText(title, 24, white, true);
        page.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        TextView genreView = makeText(genre, 13, gold, false);
        genreView.setPadding(0, dp(6), 0, dp(12));
        page.addView(genreView, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout mediaFrame = new FrameLayout(this);
        mediaFrame.setBackgroundColor(0xFF000000);
        LinearLayout.LayoutParams mediaParams =
                new LinearLayout.LayoutParams(-1, dp(230));
        mediaParams.bottomMargin = dp(14);
        page.addView(mediaFrame, mediaParams);

        ImageView poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mediaFrame.addView(poster, new FrameLayout.LayoutParams(-1, -1));
        if (image != null && !image.trim().isEmpty()) {
            Glide.with(this).load(image).centerCrop().into(poster);
        }

        playerView = new PlayerView(this);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);
        playerView.setVisibility(View.GONE);
        mediaFrame.addView(playerView, new FrameLayout.LayoutParams(-1, -1));

        statusView = makeText("", 12, gray, false);
        statusView.setPadding(0, 0, 0, dp(10));
        page.addView(statusView, new LinearLayout.LayoutParams(-1, -2));

        TextView play = makeText("▶  PLAY", 15, background, true);
        play.setGravity(Gravity.CENTER);
        play.setPadding(dp(16), dp(13), dp(16), dp(13));
        play.setBackgroundColor(gold);
        LinearLayout.LayoutParams playParams =
                new LinearLayout.LayoutParams(-1, -2);
        playParams.bottomMargin = dp(16);
        page.addView(play, playParams);

        TextView descriptionView = makeText(description, 14, white, false);
        descriptionView.setLineSpacing(dp(3), 1f);
        page.addView(descriptionView, new LinearLayout.LayoutParams(-1, -2));

        play.setOnClickListener(v -> {
            if (videoUrl == null || videoUrl.trim().isEmpty()) {
                statusView.setText("Video is not available for this title yet.");
                return;
            }
            startPlayback(videoUrl, poster);
        });

        setContentView(page);
    }

    private void startPlayback(String url, ImageView poster) {
        try {
            if (player == null) {
                player = new ExoPlayer.Builder(this).build();
                player.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_READY) {
                            statusView.setText("");
                        } else if (state == Player.STATE_BUFFERING) {
                            statusView.setText("Buffering…");
                        }
                    }

                    @Override
                    public void onPlayerError(PlaybackException error) {
                        statusView.setText("Unable to play this video. Please try again later.");
                    }
                });
            }
            playerView.setPlayer(player);
            playerView.setVisibility(View.VISIBLE);
            poster.setVisibility(View.GONE);
            player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
            player.prepare();
            player.play();
        } catch (Exception e) {
            statusView.setText("Unable to open this video.");
        }
    }

    private TextView makeText(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
