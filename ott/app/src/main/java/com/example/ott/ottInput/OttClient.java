package com.example.ott.ottInput;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;

import com.example.ott.common.ServiceChannel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OttClient {

    private static final String TAG = "OTTClient";

    private static final String MANIFEST_URL =
            "http://127.0.0.1:8000/services.json";

    private MediaPlayer mediaPlayer;
    private float volume = 1.0f;

    public interface PlaybackCallback {
        void onPlaybackStarted();
        void onPlaybackError(int what, int extra);
    }

    public static List<ServiceChannel> fetchServices() {
        List<ServiceChannel> channels = new ArrayList<>();

        try {
            URL url = new URL(MANIFEST_URL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            JSONArray array = new JSONArray(response.toString());

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                ServiceChannel channel = ServiceChannel.createOttChannel(
                        obj.getString("serviceId"),
                        obj.getString("name"),
                        obj.optString("description", ""),
                        obj.getString("streamUrl")
                );

                channels.add(channel);
                Log.d(TAG, "Parsed channel: " + channel.getName());
            }

        } catch (Exception e) {
            Log.e(TAG, "fetchServices failed", e);
        }

        return channels;
    }

    public void startStream(
            Context context,
            String streamUrl,
            Surface surface,
            PlaybackCallback callback
    ) {
        try {
            release();

            Log.d(TAG, "startStream: " + streamUrl);

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            mediaPlayer.setVolume(volume, volume);

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "Prepared -> start");
                mp.start();

                if (callback != null) {
                    callback.onPlaybackStarted();
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);

                if (callback != null) {
                    callback.onPlaybackError(what, extra);
                }

                release();
                return true;
            });

            mediaPlayer.setOnInfoListener((mp, what, extra) -> {
                Log.d(TAG, "MediaPlayer info: what=" + what + " extra=" + extra);
                return false;
            });

            mediaPlayer.setDataSource(streamUrl);

            if (surface != null) {
                mediaPlayer.setSurface(surface);
            } else {
                Log.w(TAG, "Surface je null u startStream");
            }

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "startStream failed", e);

            if (callback != null) {
                callback.onPlaybackError(-1, -1);
            }

            release();
        }
    }

    public void setSurface(Surface surface) {
        if (mediaPlayer != null && surface != null) {
            mediaPlayer.setSurface(surface);
        }
    }

    public void setVolume(float volume) {
        this.volume = volume;

        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "release failed", e);
            }

            mediaPlayer = null;
        }
    }
}