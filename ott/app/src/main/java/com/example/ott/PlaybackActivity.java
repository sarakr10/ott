package com.example.ott;
import android.app.Activity;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.nio.charset.StandardCharsets;

public class PlaybackActivity extends Activity{

    private static final String TAG = "PLAYBACK";

    private MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;
    private boolean surfaceReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        surfaceView = findViewById(R.id.videoSurface);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surfaceReady = true;
                String url = "http://localhost:8080/proxy?url=http://cdnbakmi.kaltura.com/p/243342/sp/24334200/playManifest/entryId/0_uka1msg4/flavorIds/1_vqhfu6uy,1_80sohj7p/format/applehttp/protocol/http/a.m3u8";

                if (url == null || url.isEmpty()) {
                    Log.e(TAG, "Nema validnog URL-a u bazi!");
                    return;
                }

                startPlayback(holder, url);
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d("SurfacePlayback", "SurfaceChanged: "+width+"x"+height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                surfaceReady = false;
                releasePlayer();
            }
        });
    }
    private String getFirstChannelUrl() {
        Cursor cursor = getContentResolver().query(
                TvContract.Channels.CONTENT_URI,
                new String[]{TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA},
                null,
                null,
                null
        );

        if (cursor == null) {
            Log.d("CursorPlayback", "Cursor je null");
            return null;
        }

        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);

                if (data != null) {
                    String url = new String(data, StandardCharsets.UTF_8).trim();
                    Log.d(TAG, "URL iz baze: " + url);
                    url = url.replace("\"","").trim();
                    Log.d(TAG, "URL: "+url);
                    return url;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Greska pri citanju URL-a iz baze", e);
        } finally {
            cursor.close();
        }

        return null;
    }


    private void startPlayback(SurfaceHolder holder, String url) {
        try {
            if (!surfaceReady || holder == null || holder.getSurface() == null || !holder.getSurface().isValid()) {
                Log.e(TAG, "Surface nije spreman");
                return;
            }

            releasePlayer();

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "Prepared -> start");
                mp.start();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                releasePlayer();
                return true;
            });

            mediaPlayer.setOnInfoListener((mp, what, extra) -> {
                Log.d(TAG, "MediaPlayer info: what=" + what + " extra=" + extra);
                return false;
            });

            // BITNO: dataSource + display pre prepareAsync
            mediaPlayer.setDataSource(url);
            mediaPlayer.setDisplay(holder);

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "Playback failed", e);
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Greska pri releasePlayer", e);
            }
            mediaPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }
}

