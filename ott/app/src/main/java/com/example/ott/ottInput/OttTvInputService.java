package com.example.ott.ottInput;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;
import android.media.tv.TvContract;
import android.media.tv.TvInputService;
import android.util.Log;

import com.example.ott.common.ServiceChannel;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class OttTvInputService extends TvInputService {

    private static final String TAG = "OTT_TV_INPUT";
    @Override
    public Session onCreateSession(String inputId) {
        Log.d(TAG, "onCreateSession: " + inputId);
        return new OttSession(this);
    }

    public static void startScan(Context context, String inputId) {
        new Thread(() -> {
            List<ServiceChannel> channels = OttClient.fetchServices();

            for (ServiceChannel channel : channels) {
                createService(context, inputId, channel);
            }
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

        context.getContentResolver().insert(
                TvContract.Channels.CONTENT_URI,
                values
        );

        Log.d("OTT", "Inserted channel: " + channel.getName());
    }

    private static class OttSession extends  TvInputService.Session {
        private final Context context;
        private Surface surface;
        private float volume = 1.0f;

        OttSession(Context context){
            super(context);
            this.context = context;
        }

        @Override
        public boolean onSetSurface(Surface surface){
            Log.d(TAG, "onSetSurface");
            this.surface = surface;
            if(surface == null){
                Log.w(TAG, "Surface je null");
            }
            return true;
        }

        @Override
        public boolean onTune(Uri channelUri){
            Log.d(TAG, "onTune: " + channelUri);

            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);

            String streamUrl = getStreamUrlFromChannel(channelUri);

            if(streamUrl == null || streamUrl.isEmpty()){
                Log.e(TAG, "Nema stream URL-a za kanal");
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                return false;
            }
            Log.d(TAG, "Extracted stream URL: " + streamUrl);

            startStream(streamUrl);
            return true;
        }

        private String getStreamUrlFromChannel(Uri channelUri){
            Cursor cursor = context.getContentResolver().query(
                    channelUri,
                    new String[]{
                            TvContract.Channels.COLUMN_DISPLAY_NAME,
                            TvContract.Channels.COLUMN_DESCRIPTION,
                            TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA
                    },
                    null, null, null
            );

            if(cursor == null){
                Log.e(TAG, "Cursor je null za channel Uri: " + channelUri);
                return null;
            }

            try {
                if (cursor.moveToFirst()) {
                    String name = cursor.getString(0);
                    String description = cursor.getString(1);
                    byte[] data = cursor.getBlob(2);

                    Log.d(TAG, "Channel name: " + name);
                    Log.d(TAG, "Channel description: " + description);

                    if (data != null) {
                        return new String(data, StandardCharsets.UTF_8)
                                .replace("\"", "")
                                .trim();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Greška pri čitanju kanala iz TvProvider-a", e);
            } finally {
                cursor.close();
            }

            return null;
        }
        private void startStream(String streamUrl){
            Log.d(TAG, " startStream: " + streamUrl);
            new Thread(() -> {
                try {
                    Log.d("OTT_CLIENT", "HTTP GET manifest: " + streamUrl);
                    Thread.sleep(500);

                    Log.d("OTT_CLIENT", "HTTP GET media segments");
                    Thread.sleep(500);

                    Log.d("OTT_CLIENT", "Configure decoder / MediaCodec");
                    Thread.sleep(500);

                    Log.d("OTT_CLIENT", "Playback started");

                    notifyVideoAvailable();

                } catch (InterruptedException e) {
                    Log.e(TAG, "startStream interrupted", e);
                    notifyVideoUnavailable(
                            TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN
                    );
                }
            }).start();
    }

        @Override
        public void onSetStreamVolume(float volume) {
            Log.d(TAG, "onSetStreamVolume: " + volume);
            this.volume = volume;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            Log.d(TAG, "onSetCaptionEnabled: " + enabled);
        }

        @Override
        public void onRelease() {
            Log.d(TAG, "onRelease");

            surface = null;
        }

    }

}