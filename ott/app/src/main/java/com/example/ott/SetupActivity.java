package com.example.ott;

import android.app.Activity;
import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.util.Log;

import com.example.ott.ottInput.OttTvInputService;

import java.util.List;

public class SetupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        TvInputManager manager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);

        List<TvInputInfo> inputs = manager.getTvInputList();

        for (TvInputInfo input : inputs) {
            String inputId = input.getId();
            Log.d("SetupActivity", "Found Input: "+inputId);

            if (inputId.contains(getPackageName())) {
                OttTvInputService.startScan(this, inputId);
                break;
            }
        }


    }

}