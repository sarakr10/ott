package com.example.ott;

import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView channelsText;
    private Button installButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        channelsText = findViewById(R.id.ChannelsText);
        installButton = findViewById(R.id.installButton);

        installButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SetupActivity.class));
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