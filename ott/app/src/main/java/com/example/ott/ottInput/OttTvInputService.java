package com.example.ott.ottInput;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.tv.TvContract;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import com.example.ott.common.ServiceChannel;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class OttTvInputService extends TvInputService {

    private static final String TAG = "OttvTvInputService";

    @Override
    public Session onCreateSession(String inputId) {
        Log.d(TAG, "onCreateSession: " + inputId);
        return new OttSession(this);
    }

    public static void startScan(Context context, String inputId, Runnable onDone) {
        new Thread(() -> {
            Log.d(TAG, "StartScan poceo za inputId: " + inputId);

            List<ServiceChannel> channels = OttClient.fetchServices();
            Log.d(TAG, "Fetchovao kanala: " + channels.size());

            for (ServiceChannel channel : channels) {
                createService(context, inputId, channel);
            }

            Log.d(TAG, "Svi kanali upisani, pozivam callback");

            new android.os.Handler(android.os.Looper.getMainLooper()).post(onDone);
        }).start();
    }

    private static void createService(Context context,
                                      String inputId,
                                      ServiceChannel channel) {
        ContentValues values = new ContentValues();

        values.put(TvContract.Channels.COLUMN_INPUT_ID, inputId);
        values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, channel.getName());
        values.put(TvContract.Channels.COLUMN_DESCRIPTION, channel.getDescription());
        values.put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_OTHER);

        values.put(
                TvContract.Channels.COLUMN_SERVICE_TYPE,
                TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO
        );

        values.put(
                TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                channel.getStreamUrl().getBytes(StandardCharsets.UTF_8)
        );

        Uri insertedUri = context.getContentResolver().insert(
                TvContract.Channels.CONTENT_URI,
                values
        );

        if (insertedUri == null) {
            Log.e(TAG, "Insert kanala nije uspeo: " + channel.getName());
            return;
        }

        long channelId = ContentUris.parseId(insertedUri);

        TvContract.requestChannelBrowsable(
                context,
                channelId
        );

        Log.d(TAG, "Inserted channel: " + channel.getName() + ", id=" + channelId);
    }

    private static class OttSession extends TvInputService.Session {
        private final Context context;
        private Surface surface;
        private MediaPlayer mediaPlayer;

        OttSession(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            Log.d(TAG, "onSetSurface: " + surface);
            this.surface = surface;
            return true;
        }

        @Override
        public boolean onTune(Uri channelUri) {
            Log.d(TAG, "onTune called: " + channelUri);

            String url = getUrlFromChannel(channelUri);
            Log.d(TAG, "URL dobijen iz TV provajdera: " + url);

            if (url == null || url.isEmpty()) {
                Log.e(TAG, "Nema URL-a za kanal");
                return false;
            }

            Log.d(TAG, "Pozivam start playback sa: " + url);
            startPlayback(url);

            return true;
        }

        private String getUrlFromChannel(Uri channelUri) {
            Cursor cursor = context.getContentResolver().query(
                    channelUri,
                    new String[]{TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA},
                    null,
                    null,
                    null
            );

            if (cursor == null) {
                Log.e(TAG, "Cursor je null za channelUri: " + channelUri);
                return null;
            }

            try {
                if (cursor.moveToFirst()) {
                    byte[] data = cursor.getBlob(0);

                    if (data != null) {
                        String url = new String(data, StandardCharsets.UTF_8)
                                .replace("\"", "")
                                .trim();

                        Log.d(TAG, "URL iz TvProvider-a: " + url);
                        return url;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Greška pri čitanju URL-a", e);
            } finally {
                cursor.close();
            }

            return null;
        }

        private void startPlayback(String url) {
            Log.d(TAG, "startPlayback pocetak, surface=" + surface);

            try {
                if (surface == null || !surface.isValid()) {
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
                    notifyVideoAvailable();
                    mp.start();
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                    releasePlayer();
                    return true;
                });

                Log.d(TAG, "Pozivam setDataSource: " + url);

                mediaPlayer.setDataSource(url);
                mediaPlayer.setSurface(surface);
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
                    Log.e(TAG, "releasePlayer failed", e);
                }

                mediaPlayer = null;
            }
        }

        @Override
        public void onRelease() {
            Log.d(TAG, "onRelease");
            releasePlayer();
            surface = null;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            Log.d(TAG, "onSetCaptionEnabled: " + enabled);
        }

        @Override
        public void onSetStreamVolume(float volume) {
            Log.d(TAG, "onSetStreamVolume: " + volume);

            if (mediaPlayer != null) {
                mediaPlayer.setVolume(volume, volume);
            }
        }
    }
}