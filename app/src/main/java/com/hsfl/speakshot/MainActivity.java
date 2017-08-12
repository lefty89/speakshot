package com.hsfl.speakshot;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AppCompatDelegate;
import com.hsfl.speakshot.service.dictionary.DictionaryService;
import com.hsfl.speakshot.service.guide.GuidingService;
import com.hsfl.speakshot.service.view.ViewService;
import com.hsfl.speakshot.service.audio.AudioService;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.support.design.widget.FloatingActionButton;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.content.res.ColorStateList;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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

    /**
     * This listener calls a method to apply preferences changes to the app
     */
    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;

    /**
     * TEST VAR - REMOVE LATER
     */
    private boolean mGuidedEnabled = false;

    /**
     * nested class which is used to style the mode spinner
     */
    public static class CustomArrayAdapter<T> extends ArrayAdapter<T> {
        private String themeId = "";
        private int spinnerPadding = 0;
        private float spinnerTextSize = 0;

        public CustomArrayAdapter(Context ctx, T [] objects)
        {
            super(ctx, android.R.layout.simple_spinner_item, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent)
        {
            View view = super.getView(position, convertView, parent);
            TextView text = (TextView)view.findViewById(android.R.id.text1);

            text.setPadding(spinnerPadding, spinnerPadding, spinnerPadding, spinnerPadding);
            text.setTextSize(spinnerTextSize);

            if (themeId.equals("light")) {
                text.setBackgroundColor(Color.WHITE);
                text.setTextColor(Color.BLACK);
            }
            else /*if (themeId.equals("dark"))*/ {
                text.setBackgroundColor(Color.BLACK);
                text.setTextColor(Color.WHITE);
            }

            return view;
        }

        public void setThemeId(String themeId)
        {
            this.themeId = themeId;
        }

        public void setSpinnerPadding(int spinnerPadding)
        {
            this.spinnerPadding = spinnerPadding;
        }

        public void setSpinnerTextSize(float spinnerTextSize)
        {
            this.spinnerTextSize = spinnerTextSize;
        }
    }

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

        // switch button to change the mode
        final FloatingActionButton modeSwitch = (FloatingActionButton)findViewById(R.id.btn_mode_select);
        modeSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // switch background color
                String themeId = getThemeId();
                if (mCurrentMode) {
                    mViewService.to(new SearchFragment(), null);
                    // set button colors
                    if (themeId.equals("light")) {
                        DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));

                        DrawableCompat.setTintList(DrawableCompat.wrap(modeSwitch.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(modeSwitch.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));

                        DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
                    }
                    else /*if (themeId.equals("dark"))*/ {
                        DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));

                        DrawableCompat.setTintList(DrawableCompat.wrap(modeSwitch.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(modeSwitch.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));

                        DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
                    }
                } else {
                    mViewService.to(new ReadFragment(), null);
                    // set button colors
                    if (themeId.equals("light")) {
                        DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));

                        DrawableCompat.setTintList(DrawableCompat.wrap(modeSwitch.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(modeSwitch.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));

                        DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
                    }
                    else /*if (themeId.equals("dark"))*/ {
                        DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(settingsButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));

                        DrawableCompat.setTintList(DrawableCompat.wrap(modeSwitch.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(modeSwitch.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));

                        DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
                        DrawableCompat.setTintList(DrawableCompat.wrap(guidedButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
                    }
                }
                modeSwitch.setSelected(mCurrentMode);
                mCurrentMode = !mCurrentMode;
            }
        });

        // apply preferences changes to the app
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float speechRate = Float.valueOf(prefs.getString("speech_rate", "1"));
        AudioService.setSpeechRate(speechRate);
        // this class field "li
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

        mViewService.to(new ReadFragment(), null);
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

        FloatingActionButton btn_mode_select = (FloatingActionButton)findViewById(R.id.btn_mode_select);
        btn_mode_select.setVisibility(View.GONE);

        FloatingActionButton btn_mode_guide = (FloatingActionButton)findViewById(R.id.btn_guided);
        btn_mode_guide.setVisibility(View.GONE);
    }

    /**
     * show the buttons for settings and mode switch
     */
    public void showButtonsSettingsModeSwitch() {
        FloatingActionButton btn_settings = (FloatingActionButton)findViewById(R.id.btn_settings);
        btn_settings.setVisibility(View.VISIBLE);

        FloatingActionButton btn_mode_select = (FloatingActionButton)findViewById(R.id.btn_mode_select);
        btn_mode_select.setVisibility(View.VISIBLE);

        FloatingActionButton btn_mode_guide = (FloatingActionButton)findViewById(R.id.btn_guided);
        btn_mode_guide.setVisibility(View.VISIBLE);
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
     * compatibility for vector drawing on lower android versions
     */
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}
