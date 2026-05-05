package com.example.ott;

import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

        playbackButton.setOnClickListener(v-> {
            startActivity(new Intent(this, PlaybackActivity.class));
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
                TvContract.Channels.COLUMN_DESCRIPTION
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

            builder.append("ID: ").append(id).append("\n");
            builder.append("Name: ").append(name).append("\n");
            builder.append("InputId: ").append(inputId).append("\n");
            builder.append("Description: ").append(description).append("\n");
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