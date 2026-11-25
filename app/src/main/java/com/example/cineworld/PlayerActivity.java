package com.example.cineworld;

import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.util.Locale;

public class PlayerActivity extends AppCompatActivity {

    private static final String DEMO_VIDEO =
            "https://storage.googleapis.com/shaka-demo-assets/sintel/dash.mpd";

    private ExoPlayer player;
    private PlayerView playerView;

    private ImageButton playPauseButton;
    private ImageButton rewindButton;
    private ImageButton forwardButton;

    private SeekBar seekBar;

    private TextView currentTime;
    private TextView totalTime;
    private TextView playerTitle;

    private Button speedButton;
    private Button qualityButton;
    private Button popupButton;
    private Button lockButton;
    private Button rotateButton;
    private Button aspectButton;

    private View controlsOverlay;
    private View lockOverlay;
    private Button unlockButton;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean userSeeking = false;
    private boolean isLocked = false;
    private boolean isPortrait = false;
    private boolean controlsVisible = true;

    private final Runnable hideControlsRunnable = new Runnable() {
        @Override
        public void run() {
            hideControls();
        }
    };

    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                updatePlayerControls();
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_player);

        initializeViews();

        playerView.setUseController(false);

        playerView.setOnClickListener(v -> toggleControls());

        controlsOverlay.setOnClickListener(v -> toggleControls());

        setupButtons();
        setupSeekBar();
        initializePlayer();
    }

    private void initializeViews() {

        playerView = findViewById(R.id.fullPlayerView);

        playPauseButton = findViewById(R.id.playerPlayPause);
        rewindButton = findViewById(R.id.playerRewind);
        forwardButton = findViewById(R.id.playerForward);

        seekBar = findViewById(R.id.playerSeekBar);

        currentTime = findViewById(R.id.playerCurrentTime);
        totalTime = findViewById(R.id.playerTotalTime);
        playerTitle = findViewById(R.id.playerTitle);

        speedButton = findViewById(R.id.playerSpeed);
        qualityButton = findViewById(R.id.playerQuality);
        popupButton = findViewById(R.id.playerPopup);
        lockButton = findViewById(R.id.playerLock);
        rotateButton = findViewById(R.id.playerRotate);
        aspectButton = findViewById(R.id.playerAspect);

        controlsOverlay = findViewById(R.id.controlsOverlay);
        lockOverlay = findViewById(R.id.lockOverlay);
        unlockButton = findViewById(R.id.playerUnlock);

        ImageButton closeButton = findViewById(R.id.playerClose);

        String title = getIntent().getStringExtra("title");

        if (title == null || title.trim().isEmpty()) {
            title = "CineWorld Movie";
        }

        playerTitle.setText(title);

        closeButton.setOnClickListener(v -> finish());
    }

    // Show and hide controls

    private void showControls() {

        if (isLocked || isInPictureInPictureModeCompat()) {
            return;
        }

        controlsVisible = true;
        controlsOverlay.setVisibility(View.VISIBLE);

        scheduleHideControls();
    }

    private void hideControls() {

        if (isLocked || isInPictureInPictureModeCompat()) {
            return;
        }

        controlsVisible = false;
        controlsOverlay.setVisibility(View.GONE);
    }

    private void scheduleHideControls() {

        handler.removeCallbacks(hideControlsRunnable);

        if (!isLocked && controlsVisible) {
            handler.postDelayed(hideControlsRunnable, 3000);
        }
    }

    private void toggleControls() {

        if (isLocked) return;

        if (controlsVisible) {

            handler.removeCallbacks(hideControlsRunnable);
            hideControls();

        } else {

            showControls();
        }
    }

    private void setupButtons() {

        playPauseButton.setOnClickListener(v -> {
            togglePlayPause();
            scheduleHideControls();
        });

        rewindButton.setOnClickListener(v -> {
            seekBy(-10000);
            scheduleHideControls();
        });

        forwardButton.setOnClickListener(v -> {
            seekBy(10000);
            scheduleHideControls();
        });

        speedButton.setOnClickListener(v -> showSpeedOptions());

        qualityButton.setOnClickListener(v -> showQualityOptions());

        popupButton.setOnClickListener(v -> openPopupPlayer());

        lockButton.setOnClickListener(v -> lockScreen());

        unlockButton.setOnClickListener(v -> unlockScreen());

        rotateButton.setOnClickListener(v -> {
            toggleRotation();
            scheduleHideControls();
        });

        aspectButton.setOnClickListener(v -> showAspectOptions());
    }

    private void setupSeekBar() {

        seekBar.setMax(1000);

        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar bar,
                            int progress,
                            boolean fromUser
                    ) {
                        if (fromUser && player != null) {

                            long duration = player.getDuration();

                            if (duration > 0) {
                                currentTime.setText(
                                        formatTime(duration * progress / 1000)
                                );
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar bar) {
                        userSeeking = true;
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar bar) {

                        if (player != null) {

                            long duration = player.getDuration();

                            if (duration > 0) {
                                player.seekTo(
                                        duration * bar.getProgress() / 1000
                                );
                            }
                        }

                        userSeeking = false;
                        scheduleHideControls();
                    }
                }
        );
    }

    // Initialize video player with online or offline URL

    private void initializePlayer() {

        LoadControl loadControl =
                new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                                1000,
                                10000,
                                250,
                                500
                        )
                        .build();

        player = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();

        playerView.setPlayer(player);

        // MainActivity se aaya hua video URL
        String videoUrl = getIntent().getStringExtra("video_url");

        // URL nahi mila to demo video chalega
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            videoUrl = DEMO_VIDEO;
        }

        Uri videoUri = Uri.parse(videoUrl);

        MediaItem mediaItem = MediaItem.fromUri(videoUri);

        player.setMediaItem(mediaItem);

        player.addListener(new Player.Listener() {

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcon();
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                updatePlayerControls();
            }

            @Override
            public void onPlayerError(PlaybackException error) {

                Toast.makeText(
                        PlayerActivity.this,
                        "Video play nahi ho saka. Video URL ya file check karein.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        player.prepare();
        player.play();

        updatePlayPauseIcon();

        scheduleHideControls();
    }

    private void togglePlayPause() {

        if (player == null) return;

        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }

        updatePlayPauseIcon();
    }

    private void updatePlayPauseIcon() {

        if (playPauseButton == null || player == null) return;

        if (player.isPlaying()) {

            playPauseButton.setImageResource(
                    android.R.drawable.ic_media_pause
            );

        } else {

            playPauseButton.setImageResource(
                    android.R.drawable.ic_media_play
            );
        }
    }

    private void seekBy(long milliseconds) {

        if (player == null) return;

        long position = player.getCurrentPosition() + milliseconds;
        long duration = player.getDuration();

        if (duration > 0) {
            position = Math.max(0, Math.min(position, duration));
        } else {
            position = Math.max(0, position);
        }

        player.seekTo(position);

        updatePlayerControls();
    }

    private void updatePlayerControls() {

        if (player == null) return;

        long position = player.getCurrentPosition();
        long duration = player.getDuration();

        currentTime.setText(formatTime(position));

        if (duration > 0) {

            totalTime.setText(formatTime(duration));

            if (!userSeeking) {
                seekBar.setProgress(
                        (int) (position * 1000 / duration)
                );
            }

        } else {

            totalTime.setText("00:00");

            if (!userSeeking) {
                seekBar.setProgress(0);
            }
        }

        updatePlayPauseIcon();
    }

    private String formatTime(long milliseconds) {

        if (milliseconds < 0) milliseconds = 0;

        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        if (hours > 0) {
            return String.format(
                    Locale.getDefault(),
                    "%d:%02d:%02d",
                    hours, minutes, remainingSeconds
            );
        }

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes, remainingSeconds
        );
    }

    // Playback speed

    private void showSpeedOptions() {

        String[] speeds = {
                "0.5x", "0.75x", "1.0x Normal",
                "1.25x", "1.5x", "1.75x",
                "2.0x", "2.5x", "3.0x", "4.0x", "5.0x"
        };

        float[] values = {
                0.5f, 0.75f, 1.0f, 1.25f, 1.5f,
                1.75f, 2.0f, 2.5f, 3.0f, 4.0f, 5.0f
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Playback Speed")
                .setItems(speeds, (d, which) -> {

                    if (player != null) {
                        player.setPlaybackSpeed(values[which]);
                        speedButton.setText(speeds[which]);
                    }
                })
                .create();

        dialog.setOnDismissListener(d -> scheduleHideControls());
        dialog.show();
    }

    // Video quality

    private void showQualityOptions() {

        String[] qualities = {
                "Auto", "144p", "360p", "480p",
                "720p", "1080p", "1440p", "2160p"
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Video Quality")
                .setItems(qualities, (d, which) -> {

                    if (which == 0) {

                        qualityButton.setText("Auto");

                        Toast.makeText(
                                this,
                                "Demo video ki available quality use ho rahi hai.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Toast.makeText(
                                this,
                                qualities[which] +
                                        " ke liye alag video stream chahiye.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .create();

        dialog.setOnDismissListener(d -> scheduleHideControls());
        dialog.show();
    }

    // Aspect ratio

    private void showAspectOptions() {

        String[] options = {
                "Fit", "Fill", "Zoom", "16:9", "4:3"
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Aspect Ratio")
                .setItems(options, (d, which) -> {

                    switch (which) {

                        case 0:
                            playerView.setResizeMode(
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                            );
                            playerView.post(() -> setContentAspectRatio(0f));
                            aspectButton.setText("Fit");
                            break;

                        case 1:
                            playerView.setResizeMode(
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL
                            );
                            playerView.post(() -> setContentAspectRatio(0f));
                            aspectButton.setText("Fill");
                            break;

                        case 2:
                            playerView.setResizeMode(
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            );
                            playerView.post(() -> setContentAspectRatio(0f));
                            aspectButton.setText("Zoom");
                            break;

                        case 3:
                            playerView.setResizeMode(
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                            );
                            playerView.post(
                                    () -> setContentAspectRatio(16f / 9f)
                            );
                            aspectButton.setText("16:9");
                            break;

                        case 4:
                            playerView.setResizeMode(
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                            );
                            playerView.post(
                                    () -> setContentAspectRatio(4f / 3f)
                            );
                            aspectButton.setText("4:3");
                            break;
                    }
                })
                .create();

        dialog.setOnDismissListener(d -> scheduleHideControls());
        dialog.show();
    }

    private void setContentAspectRatio(float ratio) {

        View contentFrame = playerView.findViewById(
                androidx.media3.ui.R.id.exo_content_frame
        );

        if (contentFrame instanceof AspectRatioFrameLayout) {
            ((AspectRatioFrameLayout) contentFrame).setAspectRatio(ratio);
        }
    }

    // Screen lock

    private void lockScreen() {

        isLocked = true;

        handler.removeCallbacks(hideControlsRunnable);

        controlsVisible = false;
        controlsOverlay.setVisibility(View.GONE);
        lockOverlay.setVisibility(View.VISIBLE);

        Toast.makeText(
                this, "Screen Locked", Toast.LENGTH_SHORT
        ).show();
    }

    private void unlockScreen() {

        isLocked = false;

        lockOverlay.setVisibility(View.GONE);

        controlsVisible = true;
        controlsOverlay.setVisibility(View.VISIBLE);

        scheduleHideControls();

        Toast.makeText(
                this, "Screen Unlocked", Toast.LENGTH_SHORT
        ).show();
    }

    // Rotation

    private void toggleRotation() {

        if (!isPortrait) {

            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            );

            isPortrait = true;
            rotateButton.setText("Portrait");

        } else {

            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            );

            isPortrait = false;
            rotateButton.setText("Landscape");
        }
    }

    // Popup player

    private void openPopupPlayer() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            try {

                PictureInPictureParams params =
                        new PictureInPictureParams.Builder()
                                .setAspectRatio(new Rational(16, 9))
                                .build();

                enterPictureInPictureMode(params);

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Popup Player open nahi ho saka.",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } else {

            Toast.makeText(
                    this,
                    "Popup Player ke liye Android 8.0 ya usse naya version chahiye.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onUserLeaveHint() {

        super.onUserLeaveHint();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && player != null
                && player.isPlaying()
                && !isInPictureInPictureMode()) {

            openPopupPlayer();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(
            boolean isInPictureInPictureMode,
            Configuration newConfig
    ) {

        super.onPictureInPictureModeChanged(
                isInPictureInPictureMode,
                newConfig
        );

        if (isInPictureInPictureMode) {

            handler.removeCallbacks(hideControlsRunnable);

            controlsOverlay.setVisibility(View.GONE);
            lockOverlay.setVisibility(View.GONE);

        } else if (!isLocked) {

            controlsVisible = true;
            controlsOverlay.setVisibility(View.VISIBLE);

            scheduleHideControls();
        }
    }

    @Override
    protected void onStart() {

        super.onStart();

        handler.removeCallbacks(updateProgress);
        handler.post(updateProgress);
    }

    @Override
    protected void onStop() {

        handler.removeCallbacks(updateProgress);
        handler.removeCallbacks(hideControlsRunnable);

        if (player != null && !isInPictureInPictureModeCompat()) {
            player.pause();
        }

        super.onStop();
    }

    private boolean isInPictureInPictureModeCompat() {

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && isInPictureInPictureMode();
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacks(updateProgress);
        handler.removeCallbacks(hideControlsRunnable);

        if (player != null) {

            playerView.setPlayer(null);
            player.release();
            player = null;
        }

        super.onDestroy();
    }
}
