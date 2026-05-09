package com.example.ott;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ott.ottInput.OttTvInputService;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends Activity {

    private static final String TAG = "SetupActivity";
    private boolean scanStarted = false;

    private Button installButton;
    private ProgressBar installProgress;
    private TextView statusText;
    private ScrollView channelScroll;
    private TextView channelsText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        installProgress = findViewById(R.id.installProgress);
        installButton = findViewById(R.id.installButton);
        statusText = findViewById(R.id.statusText);
        channelScroll = findViewById(R.id.channelScroll);
        channelsText = findViewById(R.id.ChannelsText);

        installProgress.setVisibility(View.GONE);
        channelScroll.setVisibility(View.GONE);
        statusText.setText("");

        installButton.setOnClickListener(v -> {
            installProgress.setVisibility(View.GONE);
            statusText.setText("Installing OTT channels...");
            TvInputManager manager =
                    (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);

            if (manager == null) {
                Log.e(TAG, "TvInputManager je null");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            List<TvInputInfo> inputs = manager.getTvInputList();

            Log.d(TAG, "Ukupno inputa: " + inputs.size());

            for (TvInputInfo input : inputs) {
                String inputId = input.getId();
                Log.d(TAG, "Input: " + inputId);

                if (inputId.contains(getPackageName())) {
                    scanStarted = true;

                    Log.d(TAG, "Pronasao nas input, startScan...");

                    OttTvInputService.startScan(this, inputId, () -> {
                        Log.d(TAG, "Sken zavrsen");

                        runOnUiThread(() -> {
                            installProgress.setVisibility(View.GONE);
                            statusText.setText("Channels installed successfully!");
                            readChannelsFromTvProvider();
                            channelScroll.setVisibility(View.VISIBLE);
                            installButton.setText("Done");
                            installButton.setVisibility(View.VISIBLE);
                            installButton.setOnClickListener(done -> {
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
                                    Toast.makeText(
                                            this,
                                            "Ne mogu da pročitam kanale",
                                            Toast.LENGTH_LONG
                                    ).show();
                                    return;
                                }

                                try {

                                    if (!cursor.moveToFirst()) {
                                        Toast.makeText(
                                                this,
                                                "Nema kanala",
                                                Toast.LENGTH_LONG
                                        ).show();
                                        return;
                                    }

                                    long channelId = cursor.getLong(0);

                                    android.net.Uri channelUri =
                                            TvContract.buildChannelUri(channelId);
                                    Log.d("MAIN", "Playback clicked, channelUri=" + channelUri);

                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(channelUri);
                                    intent.setPackage("com.android.tv");
                                    startActivity(intent);

                                } catch (Exception e) {

                                    Toast.makeText(
                                            this,
                                            "Greška: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();

                                } finally {
                                    cursor.close();
                                }
                            });
                        });
                    });

                    return;
                }
            }

            if (!scanStarted) {
                Log.e(TAG, "Input nije pronadjen");
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
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