package com.example.ott.ottInput;
import android.content.Context;
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
    private static final String MANIFEST_URL = "http://127.0.0.1:8000/services.json";

    private MediaPlayer mediaPlayer;
    private float volume = 1.0f;

    public interface PlaybackCallback{
        void onPlaybackStarted();
        void onPLaybackError(int what, int extra);
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
                Log.d("OTTClient", "Parsed channel: " + channel.getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return channels;
    }

    public void startStream(Context context, String streamUrl, Surface surface, PlaybackCallback callback){

    }

}