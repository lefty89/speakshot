package com.hsfl.speakshot;

import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.CameraService;
import com.hsfl.speakshot.ui.ReadFragment;
import com.hsfl.speakshot.ui.SearchFragment;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.preference.Preference;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.support.design.widget.FloatingActionButton;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * main app modes
     */
    private final int MODE_SEARCH = 0;
    private final int MODE_READ = 1;
    private int CurrentMode = 1;

    /**
     * the app theme
     */
    private static int Theme = R.style.ThemeDark;

    /**
     * Service provider that handles the camera object
     */
    private CameraService mCameraService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(TAG, "audio enabled: " + preferences.getBoolean("audio_output_switch", true));
        Log.d(TAG, "vibration enabled: " + preferences.getBoolean("vibration_switch", true));

        // sets theme and app settings
        setTheme(Theme);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // request camera permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            // gets the CameraService
            mCameraService = CameraService.getInstance();
            // initializes the audio service
            AudioService.getInstance().init(getApplicationContext());
        }

        // switch button to change the mode
        final FloatingActionButton modeSwitch = (FloatingActionButton)findViewById(R.id.btn_mode_select);
        modeSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CurrentMode = CurrentMode == MODE_SEARCH ? MODE_READ : MODE_SEARCH;
                setAppMode(CurrentMode);
                // swtich background color
                if (CurrentMode == MODE_SEARCH)
                    modeSwitch.setImageDrawable(getResources().getDrawable(R.drawable.ic_search_black_24dp));
                else
                    modeSwitch.setImageDrawable(getResources().getDrawable(R.drawable.ic_volume_up_black_24dp));
            }
        });
        setAppMode(MODE_READ);

        // button to show the settings activity
        final FloatingActionButton settingsButton = (FloatingActionButton)findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSettings();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // init the camera
        mCameraService.initCamera(getApplicationContext());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // releases the camera
        mCameraService.releaseCamera();
    }

    /**
     * switches the app between read and search mode
     * @param mode either MODE_READ or MODE_SEARCH
     */
    private void setAppMode(int mode) {
        Log.d(TAG, "switch mode to " + (mode == MODE_READ ? "MODE_READ" : "MODE_SEARCH"));
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        // gets the fragment
        Fragment fragment = mode == MODE_SEARCH ? new SearchFragment() : new ReadFragment();
        ft.replace(R.id.fragment_container, fragment);
        // Commit the transaction
        ft.commit();
    }

    /**
     * shows the settings activity
     */
    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

}
