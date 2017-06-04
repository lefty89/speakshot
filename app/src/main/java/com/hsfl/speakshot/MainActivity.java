package com.hsfl.speakshot;

import android.util.Log;
import com.hsfl.speakshot.cpp.Hunspell;
import com.hsfl.speakshot.service.dictionary.DictionaryService;
import com.hsfl.speakshot.service.view.ViewService;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.CameraService;
import com.hsfl.speakshot.ui.ReadFragment;
import com.hsfl.speakshot.ui.SearchFragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ToggleButton;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * the app theme
     */
    private static  int THEME = R.style.AppTheme;

    /**
     * Service provider that handles the camera object
     */
    private CameraService mCameraService;

    /**
     * Service provider that handles the dictionary
     */
    private DictionaryService mDictionaryService;

    /**
     * Service provider that handles the views
     */
    private ViewService mViewService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // sets theme and app settings
        setTheme(THEME);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // initializes the ViewService
        mViewService = ViewService.getInstance();
        mViewService.init(getFragmentManager());

        // initializes the DictionaryService
        mDictionaryService = DictionaryService.getInstance();
        mDictionaryService.init(DictionaryService.LIB_GOOGLE_TEXT_SERVICE, getApplicationContext());

        // request camera permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            // gets the CameraService
            mCameraService = CameraService.getInstance();
            // initializes the audio service
            AudioService.getInstance().init(getApplicationContext());
        }

        // switch button to change the mode
        final ToggleButton modeSwitch = (ToggleButton)findViewById(R.id.btn_mode_select);
        modeSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mViewService.to(((modeSwitch.isChecked()) ? new SearchFragment() : new ReadFragment()), null);
            }
        });
        mViewService.to(new ReadFragment(), null);

        // button to show the settings activity
        final Button settingsButton = (Button)findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSettings();
            }
        });

        Hunspell h =new Hunspell();
        Log.d(TAG, h.test());
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
     * shows the settings activity
     */
    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
