package com.hsfl.speakshot;

import android.support.v7.app.AppCompatDelegate;
import com.hsfl.speakshot.service.dictionary.DictionaryService;
import com.hsfl.speakshot.service.guide.GuidingService;
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
import com.hsfl.speakshot.ui.views.CameraPreviewSurface;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * main app modes
     */
    private final boolean MODE_SEARCH  = false;
    private final boolean MODE_READ    = true;
    private boolean       mCurrentMode = MODE_READ;

    /**
     * the app theme
     */
    private static int Theme = R.style.ThemeDarkReadMode;

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
        loadSettings();
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
            // initializes the guiding service
            GuidingService.getInstance().init(this);
        }

        // switch button to change the mode
        final FloatingActionButton modeSwitch = (FloatingActionButton)findViewById(R.id.btn_mode_select);
        modeSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mCurrentMode) {
                    mViewService.to(new SearchFragment(), null);
                } else {
                    mViewService.to(new ReadFragment(), null);
                }
                modeSwitch.setSelected(mCurrentMode);
                mCurrentMode = !mCurrentMode;
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

    @Override
    public void onBackPressed() {
        showButtonsSettingsModeSwitch();
        ViewService.getInstance().back();
    }

    /**
     * shows the settings activity
     */
    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * hides the buttons for settings and mode switch
     */
    public void hideButtonsSettingsModeSwitch()
    {
        FloatingActionButton btn_settings = (FloatingActionButton)findViewById(R.id.btn_settings);
        btn_settings.setVisibility(View.GONE);

        FloatingActionButton btn_mode_select = (FloatingActionButton)findViewById(R.id.btn_mode_select);
        btn_mode_select.setVisibility(View.GONE);
    }

    /**
     * show the buttons for settings and mode switch
     */
    public void showButtonsSettingsModeSwitch()
    {
        FloatingActionButton btn_settings = (FloatingActionButton)findViewById(R.id.btn_settings);
        btn_settings.setVisibility(View.VISIBLE);

        FloatingActionButton btn_mode_select = (FloatingActionButton)findViewById(R.id.btn_mode_select);
        btn_mode_select.setVisibility(View.VISIBLE);
    }

    /**
     * reads the settings from the shared preference object and sets up the app accordingly
     */
    private void loadSettings()
    {
        // set theme
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String themeId = preferences.getString("theme", "");
        if (themeId.equals("light")) {
            setTheme(R.style.ThemeLightReadMode);
            Log.d(TAG, "switch to light theme " + themeId);
        }
        else /*if (themeId.equals("dark"))*/ {
            setTheme(R.style.ThemeDarkReadMode);
            Log.d(TAG, "switch to dark theme " + themeId);
        }
    }

    static {
        // compatibility for vector drawing on lower android versions
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}
