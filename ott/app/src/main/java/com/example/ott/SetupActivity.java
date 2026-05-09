package com.example.ott;

import android.app.Activity;
import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ott.ottInput.OttTvInputService;

import java.util.List;

public class SetupActivity extends Activity {

    private static final String TAG = "SetupActivity";
    private boolean scanStarted = false;

    private Button installButton;
    private ProgressBar installProgress;
    private TextView statusText;
    private RecyclerView channelRecyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        installProgress = findViewById(R.id.installProgress);
        installButton = findViewById(R.id.installButton);
        statusText = findViewById(R.id.statusText);
        channelRecyclerView = findViewById(R.id.channelRecyclerView);

        installProgress.setVisibility(View.GONE);
        channelRecyclerView.setVisibility(View.GONE);
        statusText.setText("");

        channelRecyclerView.setLayoutManager(new LinearLayoutManager(this));


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

                    setResult(Activity.RESULT_OK);
                    finish();
                });

                return;
            }
        }

        if (!scanStarted) {
            Log.e(TAG, "Input nije pronadjen");
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }
}