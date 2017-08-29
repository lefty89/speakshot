package com.hsfl.speakshot;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import com.hsfl.speakshot.service.dictionary.DictionaryService;
import com.hsfl.speakshot.service.guide.GuidingService;
import com.hsfl.speakshot.service.navigation.NavigationService;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.ui.HistoryFragment;
import com.hsfl.speakshot.ui.ReadFragment;
import com.hsfl.speakshot.ui.ResultFragment;
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
import android.support.v4.graphics.drawable.DrawableCompat;
import android.content.res.ColorStateList;
import android.os.Vibrator;

import com.hsfl.speakshot.ui.views.CameraPreviewSurface;
import com.hsfl.speakshot.service.camera.CameraService;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static MainActivity instance;

    /**
     * main app modes
     */
    private final boolean MODE_SEARCH  = false;
    private final boolean MODE_READ    = true;
    private boolean       mCurrentMode = MODE_READ;

    /**
     * Service provider that handles the dictionary
     */
    private DictionaryService mDictionaryService;

    /**
     * Service provider that handles the views
     */
    private NavigationService mNavigationService;

    /**
     * The camera background surface
     */
    private CameraPreviewSurface mPreview;

    /**
     * This listener calls a method to apply preferences changes to the app
     */
    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    /**
     * TEST VAR - REMOVE LATER
     */
    private boolean mGuidedEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // sets theme and app settings
        loadSettings();
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // gets the background surface
        mPreview = (CameraPreviewSurface)findViewById(R.id.camera_view);

        // initializes the NavigationService
        mNavigationService = NavigationService.getInstance();
        mNavigationService.init(getFragmentManager());

        // initializes the DictionaryService
        mDictionaryService = DictionaryService.getInstance();
        mDictionaryService.init(DictionaryService.LIB_GOOGLE_TEXT_SERVICE, getApplicationContext());

        // request camera permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            // initializes the audio service
            AudioService.getInstance().init(getApplicationContext());
            // initializes the guiding service
            GuidingService.getInstance().init(this);
        }

        // button to show the settings activity
        final FloatingActionButton settingsButton = (FloatingActionButton)findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showSettings();
            }
        });

        /////////////////////////////////////////
        // FOR TESTING PURPOSES ONLY
        /////////////////////////////////////////
        final FloatingActionButton guidedButton = (FloatingActionButton)findViewById(R.id.btn_guided);
        guidedButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!mGuidedEnabled) {
                    GuidingService.getInstance().start();
                }
                else {
                    GuidingService.getInstance().stop();
                }
                mGuidedEnabled = !mGuidedEnabled;
                guidedButton.setSelected(mGuidedEnabled);
            }
        });

        // apply preferences changes to the app
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float speechRate = Float.valueOf(prefs.getString("speech_rate", "1"));
        AudioService.setSpeechRate(speechRate);
        mSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("speech_rate")) {
                float speechRate = Float.valueOf(prefs.getString(key, "1"));
                AudioService.setSpeechRate(speechRate);
                AudioService.getInstance().speak(getResources().getString(R.string.speech_demo_sentence));
            }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        // show history view
        final FloatingActionButton historyButton = (FloatingActionButton)findViewById(R.id.btn_history);
        historyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NavigationService.getInstance().toS(new HistoryFragment(), null);
                // speak hint
                if (getHintsEnabled()) {
                    AudioService.getInstance().speak(getResources().getString(R.string.read_mode_history_hint));
                }
            }
        });

        // light toggle
        final FloatingActionButton lightSwitch = (FloatingActionButton)findViewById(R.id.btn_light_toggle);
        lightSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CameraService.getInstance().setFlashLightEnabled(!CameraService.getInstance().isFlashLightEnabled());
                lightSwitch.setSelected(CameraService.getInstance().isFlashLightEnabled());
            }
        });
        lightSwitch.setSelected(CameraService.getInstance().isFlashLightEnabled());

        mNavigationService.to(new ReadFragment(), null);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        // init the preview view and attaches the camera
        mPreview.update();
        instance = this;
        speakModeHint();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        // stops the preview and releases all resources
        mPreview.pause();
        AudioService.getInstance().stopSpeaking();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        showButtonsSettingsModeSwitch();
        NavigationService.getInstance().back();
        AudioService.getInstance().stopSpeaking();
    }

    /**
     * Vibrates for the specified period of time in milliseconds.
     * @params period
     */
    public void vibrate(long period)
    {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(period);
    }

    /**
     * shows the settings activity
     */
    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra( SettingsActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName() );
        intent.putExtra( SettingsActivity.EXTRA_NO_HEADERS, true );
        startActivity(intent);
    }

    /**
     * hides the buttons for settings and mode switch
     */
    public void hideButtonsSettingsModeSwitch() {
        FloatingActionButton btn_settings = (FloatingActionButton)findViewById(R.id.btn_settings);
        btn_settings.setVisibility(View.GONE);

        FloatingActionButton btn_mode_guide = (FloatingActionButton)findViewById(R.id.btn_guided);
        btn_mode_guide.setVisibility(View.GONE);

        FloatingActionButton btn_history = (FloatingActionButton)findViewById(R.id.btn_history);
        btn_history.setVisibility(View.GONE);

        FloatingActionButton btn_light = (FloatingActionButton)findViewById(R.id.btn_light_toggle);
        btn_light.setVisibility(View.GONE);
    }

    /**
     * show the buttons for settings and mode switch
     */
    public void showButtonsSettingsModeSwitch() {
        FloatingActionButton btn_settings = (FloatingActionButton)findViewById(R.id.btn_settings);
        btn_settings.setVisibility(View.VISIBLE);

        FloatingActionButton btn_mode_guide = (FloatingActionButton)findViewById(R.id.btn_guided);
        btn_mode_guide.setVisibility(View.VISIBLE);

        FloatingActionButton btn_history = (FloatingActionButton)findViewById(R.id.btn_history);
        btn_history.setVisibility(View.VISIBLE);

        FloatingActionButton btn_light = (FloatingActionButton)findViewById(R.id.btn_light_toggle);
        btn_light.setVisibility(View.VISIBLE);
    }

    /**
     * reads the settings from the shared preference object and sets up the app accordingly
     */
    private void loadSettings() {
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

    /**
     * returns the theme id ("light"|"dark")
     * @return String
     */
    public String getThemeId() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String themeId = preferences.getString("theme", "");
        return themeId;
    }

    /**
     * returns the reading speed (float value)
     * @return float
     */
    public float getSpeechRate() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return Float.valueOf(preferences.getString("speech_rate", ""));
    }

    /**
     * returns whether audio output is enabled
     * @return boolean
     */
    public boolean getAudioEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("audio_output_switch", true);
    }

    /**
     * returns whether the hints are enabled
     * @return boolean
     */
    public boolean getHintsEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("hints_switch", true);
    }

    /**
     * returns whether vibration is enabled
     * @return boolean
     */
    public boolean getVibrationEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("vibration_switch", true);
    }

    /**
     * Switches the icon of the read result button
     * @param speaking
     */
    public void setIconPlayButtonReadResults(boolean speaking)
    {
        ResultFragment readResultsFragment = (ResultFragment) getFragmentManager().findFragmentById(R.id.result_fragment_background);
        readResultsFragment.setIconPlayButtonReadResults(speaking);
    }

    /**
     * Speaks the mode hint (general hint for Read | Search Mode).
     */
    public void speakModeHint()
    {
        // speak hint text
        if (getHintsEnabled()) {
            if (mCurrentMode) {
                AudioService.getInstance().speak(getResources().getString(R.string.read_mode_hint_general));
            }
            else {
                AudioService.getInstance().speak(getResources().getString(R.string.search_mode_hint_general));
            }
        }
    }

    /**
     * Switches between read and search mode
     */
    public void switchMode() {
        final FloatingActionButton guidedButton = (FloatingActionButton)findViewById(R.id.btn_guided);
        final FloatingActionButton settingsButton = (FloatingActionButton)findViewById(R.id.btn_settings);
        final FloatingActionButton historyButton = (FloatingActionButton)findViewById(R.id.btn_history);
        final FloatingActionButton lightButton = (FloatingActionButton)findViewById(R.id.btn_light_toggle);

        // switch background color
        String themeId = getThemeId();
        if (mCurrentMode) {
            mNavigationService.to(new SearchFragment(), null);
            // set button colors
            if (themeId.equals("light")) {
                DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
                DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));

                DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
                DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));

                DrawableCompat.setTintList(DrawableCompat.wrap(lightButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
                DrawableCompat.setTintList(DrawableCompat.wrap(lightButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));

                DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
                DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
            }
            else /*if (themeId.equals("dark"))*/ {
                DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
                DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));

                DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
                DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));

                DrawableCompat.setTintList(DrawableCompat.wrap(lightButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
                DrawableCompat.setTintList(DrawableCompat.wrap(lightButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));

                DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
                DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
            }
        } else {
            mNavigationService.to(new ReadFragment(), null);
            // set button colors
            if (themeId.equals("light")) {
                DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
                DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));

                DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
                DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));

                DrawableCompat.setTintList(DrawableCompat.wrap(lightButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
                DrawableCompat.setTintList(DrawableCompat.wrap(lightButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));

                DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
                DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
            }
            else /*if (themeId.equals("dark"))*/ {
                DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
                DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));

                DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
                DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));

                DrawableCompat.setTintList(DrawableCompat.wrap(lightButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
                DrawableCompat.setTintList(DrawableCompat.wrap(lightButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));

                DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
                DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
            }
        }
        mCurrentMode = !mCurrentMode;
        speakModeHint();
        vibrate(10);
    }

    public static MainActivity getInstance()
    {
        return instance;
    }

    /**
     * compatibility for vector drawing on lower android versions
     */
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}
