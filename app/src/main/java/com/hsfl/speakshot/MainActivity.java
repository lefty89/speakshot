package com.hsfl.speakshot;

import android.app.Dialog;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatDelegate;
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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.support.design.widget.FloatingActionButton;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import com.hsfl.speakshot.ui.surfaces.CameraPreviewSurface;


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

    /**
     * Service provider that handles the dictionary
     */
    private DictionaryService mDictionaryService;

    /**
     * Service provider that handles the views
     */
    private ViewService mViewService;

    /**
     * The camera background surface
     */
    private CameraPreviewSurface mPreview;

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

        // gets the background surface
        mPreview = (CameraPreviewSurface)findViewById(R.id.camera_view);

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
        final FloatingActionButton modeSwitch = (FloatingActionButton)findViewById(R.id.btn_mode_select);
        modeSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CurrentMode = CurrentMode == MODE_SEARCH ? MODE_READ : MODE_SEARCH;
                // swtich background color
                if (CurrentMode == MODE_SEARCH) {
                    modeSwitch.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_volume_up_black_24dp, getTheme()));
                    mViewService.to(new SearchFragment(), null);
                }
                else {
                    modeSwitch.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_search_black_24dp, getTheme()));
                    mViewService.to(new ReadFragment(), null);
                }
            }
        });
        mViewService.to(new ReadFragment(), null);

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
        Log.d(TAG, "onResume");
        // init the preview view and attaches the camera
        mPreview.update();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        // stops the preview and releases all resources
        mPreview.pause();
        super.onPause();
    }

    /**
     * shows the settings activity
     */
    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    static {
        // compatibility for vector drawing on lower android versions
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}
