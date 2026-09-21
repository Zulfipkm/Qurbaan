package com.qurban.livestock;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class BismillahActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private boolean isNavigated = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    private static final String BISMILLAH_AUDIO_URL = "https://everyayah.com/data/Alafasy_128kbps/001001.mp3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bismillah);

        playOnlineAudio();

        timeoutRunnable = this::navigateToMain;
        handler.postDelayed(timeoutRunnable, 4500);
    }

    private void playOnlineAudio() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            );

            mediaPlayer.setDataSource(BISMILLAH_AUDIO_URL);
            mediaPlayer.setOnPreparedListener(mp -> mp.start());
            mediaPlayer.setOnCompletionListener(mp -> navigateToMain());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                navigateToMain();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            navigateToMain();
        }
    }

    private void navigateToMain() {
        if (!isNavigated) {
            isNavigated = true;
            if (timeoutRunnable != null) {
                handler.removeCallbacks(timeoutRunnable);
            }
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                } catch (Exception ignored) {}
                mediaPlayer = null;
            }
            startActivity(new Intent(BismillahActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }
}
