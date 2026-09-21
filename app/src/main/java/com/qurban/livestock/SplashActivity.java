package com.qurban.livestock;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private boolean isNavigated = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    private static final String GOAT_SOUND_URL = "https://actions.google.com/sounds/v1/animals/sheep_bleat.ogg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        playGoatSound();

        timeoutRunnable = this::navigateToBismillah;
        handler.postDelayed(timeoutRunnable, 3500);
    }

    private void playGoatSound() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
            );

            mediaPlayer.setDataSource(GOAT_SOUND_URL);
            mediaPlayer.setOnPreparedListener(mp -> mp.start());
            mediaPlayer.setOnCompletionListener(mp -> navigateToBismillah());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                navigateToBismillah();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            navigateToBismillah();
        }
    }

    private void navigateToBismillah() {
        if (!isNavigated) {
            isNavigated = true;
            if (timeoutRunnable != null) {
                handler.removeCallbacks(timeoutRunnable);
            }
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                    mediaPlayer.release();
                } catch (Exception ignored) {}
                mediaPlayer = null;
            }
            startActivity(new Intent(SplashActivity.this, BismillahActivity.class));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }
}
