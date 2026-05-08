package com.example.ott;

import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView channelsText;
    private Button deleteChannelsButton;
    private Button playbackButton;
    private Button installButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        channelsText = findViewById(R.id.ChannelsText);
        installButton = findViewById(R.id.installButton);
        deleteChannelsButton = findViewById(R.id.deleteChannelsButton);
        playbackButton = findViewById(R.id.playbackButton);

        installButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SetupActivity.class));
        });

        playbackButton.setOnClickListener(v -> {

            Cursor cursor = getContentResolver().query(
                    TvContract.Channels.CONTENT_URI,
                    new String[]{
                            TvContract.Channels._ID
                    },
                    null,
                    null,
                    null
            );

            if (cursor == null) {
                Toast.makeText(this,
                        "Ne mogu da pročitam kanale",
                        Toast.LENGTH_LONG).show();
                return;
            }

            try {

                if (!cursor.moveToFirst()) {
                    Toast.makeText(this,
                            "Nema kanala",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                long channelId = cursor.getLong(0);

                // buildChannelUri(ChannelId)
                android.net.Uri channelUri =
                        TvContract.buildChannelUri(channelId);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(channelUri);

                Log.d("MAIN", "Starting channelUri: " + channelUri);

                startActivity(intent);

            } catch (Exception e) {

                Toast.makeText(this,
                        "Greška: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();

            } finally {
                cursor.close();
            }
        });

        deleteChannelsButton.setOnClickListener(v -> {
            deleteChannelsFromTvProvider();
            readChannelsFromTvProvider();
        });

        readChannelsFromTvProvider();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (channelsText != null) {
            channelsText.postDelayed(() -> {
                readChannelsFromTvProvider();
            }, 2000);
        }
    }

    private void deleteChannelsFromTvProvider() {
        try {
            int deleted = getContentResolver().delete(
                    TvContract.Channels.CONTENT_URI,
                    null,
                    null
            );

            Toast.makeText(
                    this,
                    "Obrisano kanala: " + deleted,
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Greška pri brisanju: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
    private void readChannelsFromTvProvider() {
        StringBuilder builder = new StringBuilder();

        String[] projection = {
                TvContract.Channels._ID,
                TvContract.Channels.COLUMN_DISPLAY_NAME,
                TvContract.Channels.COLUMN_INPUT_ID,
                TvContract.Channels.COLUMN_DESCRIPTION,
                TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA
        };

        Cursor cursor = getContentResolver().query(
                TvContract.Channels.CONTENT_URI,
                projection,
                null,
                null,
                null
        );

        if (cursor == null) {
            channelsText.setText("Cursor je null — ne mogu da pročitam TV Provider.");
            return;
        }

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            String inputId = cursor.getString(2);
            String description = cursor.getString(3);
            String url = "";
            try {
                byte[] data = cursor.getBlob(4);
                if (data != null) {
                    url = new String(data).trim().replace("\"", "");
                }
            } catch (Exception e) {
                url = "Greska pri citanju URL-a";
            }
            builder.append("ID: ").append(id).append("\n");
            builder.append("Name: ").append(name).append("\n");
            builder.append("InputId: ").append(inputId).append("\n");
            builder.append("Description: ").append(description).append("\n");
            builder.append("URL: ").append(url).append("\n");
            builder.append("------------------------\n");
        }

        cursor.close();

        channelsText.setText(
                builder.length() == 0
                        ? "Nema kanala u TV Provider-u."
                        : builder.toString()
        );
    }
}