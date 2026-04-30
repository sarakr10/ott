package com.example.ott.ottInput;

import android.content.ContentValues;
import android.content.Context;
import android.media.tv.TvContract;
import android.media.tv.TvInputService;
import android.util.Log;

import com.example.ott.common.ServiceChannel;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class OttTvInputService extends TvInputService {

    @Override
    public Session onCreateSession(String inputId) {
        return null; // kasnije ćemo ovde dodati OttSession za zapping/playback
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
}