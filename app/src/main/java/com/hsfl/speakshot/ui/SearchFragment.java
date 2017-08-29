package com.hsfl.speakshot.ui;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.*;
import android.widget.Toast;
import android.widget.ImageView;

import com.hsfl.speakshot.Constants;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.helper.ImagePersistenceHelper;
import com.hsfl.speakshot.service.camera.ocr.OcrHandler;
import com.hsfl.speakshot.service.camera.ocr.serialization.*;
import com.hsfl.speakshot.service.navigation.NavigationService;
import com.hsfl.speakshot.service.camera.CameraService;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;

public class SearchFragment extends Fragment implements Observer, View.OnTouchListener {
    private static final String TAG = SearchFragment.class.getSimpleName();

    /**
     * the inflated view
     */
    private View mInflatedView;

    /**
     * Service provider that handles the camera object
     */
    private CameraService mCameraService;

    /**
     * contains the returned ocr texts
     */
    private ArrayList<TextBlockParcel> detectedTexts;

    /**
     * Gesture detector
     */
    private GestureDetector mGestureDetector = null;

    /**
     * The team to search for
     */
    String searchTerm = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflatedView = inflater.inflate(R.layout.search_fragment, container, false);

        // make buttons for settings and mode switch invisible
        ((MainActivity)getActivity()).showButtonsSettingsModeSwitch();

        mCameraService = CameraService.getInstance();
        mCameraService.addObserver(this);

        // set focus mode
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String focusMode = prefs.getBoolean("use_autofocus_switch", true) ?
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO : Camera.Parameters.FOCUS_MODE_AUTO;
        mCameraService.setFocusMode(focusMode);

        // adds a gesture detector
        SearchFragmentGestureDetector searchGestureDetector = new SearchFragmentGestureDetector(this);
        // Create a GestureDetector
        mGestureDetector = new GestureDetector(getActivity(), searchGestureDetector);
        // add touch listener
        mInflatedView.setOnTouchListener(this);

        // init controls
        initializeControls();
        return mInflatedView;
    }

    /**
     * Inits the UI controls
     */
    private void initializeControls() {
        // show result view
        final FloatingActionButton sendToReadButton = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_send_to_read);
        sendToReadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(ResultFragment.IN_TEXTS_PAREL, detectedTexts);
                NavigationService.getInstance().toS(new ResultFragment(), bundle);
                // speak hint
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity.getHintsEnabled()) {
                    AudioService.getInstance().speak(mainActivity.getResources().getString(R.string.read_mode_results_hint));
                }
            }
        });
        sendToReadButton.setEnabled(false);

        // show history view
        final FloatingActionButton historyButton = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_history);
        historyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NavigationService.getInstance().toS(new HistoryFragment(), null);
                // speak hint
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity.getHintsEnabled()) {
                    AudioService.getInstance().speak(mainActivity.getResources().getString(R.string.read_mode_history_hint));
                }
            }
        });

        // light toggle
        final FloatingActionButton lightSwitch = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_light_toggle);
        lightSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCameraService.setFlashLightEnabled(!mCameraService.isFlashLightEnabled());
                lightSwitch.setSelected(mCameraService.isFlashLightEnabled());
            }
        });
        lightSwitch.setSelected(mCameraService.isFlashLightEnabled());

        final ImageView imageViewModeIcon = (ImageView)mInflatedView.findViewById(R.id.mode_icon);
        final ImageView imageViewIconSwitchToReadMode = (ImageView)mInflatedView.findViewById(R.id.icon_switch_to_read_mode);

        // set button colors
        String themeId = ((MainActivity)getActivity()).getThemeId();
        if (themeId.equals("light")) {
            DrawableCompat.setTintList(DrawableCompat.wrap(lightSwitch.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
            DrawableCompat.setTintList(DrawableCompat.wrap(lightSwitch.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));

            DrawableCompat.setTintList(DrawableCompat.wrap(sendToReadButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
            DrawableCompat.setTintList(DrawableCompat.wrap(sendToReadButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));

            DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
            DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));

            imageViewModeIcon.setColorFilter(ContextCompat.getColor(((MainActivity)getActivity()).getApplicationContext(), R.color.darkColorB));
            imageViewIconSwitchToReadMode.setColorFilter(ContextCompat.getColor(((MainActivity)getActivity()).getApplicationContext(), R.color.darkColorB));
        }
        else /*if (themeId.equals("dark"))*/ {
            DrawableCompat.setTintList(DrawableCompat.wrap(lightSwitch.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
            DrawableCompat.setTintList(DrawableCompat.wrap(lightSwitch.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));

            DrawableCompat.setTintList(DrawableCompat.wrap(sendToReadButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
            DrawableCompat.setTintList(DrawableCompat.wrap(sendToReadButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));

            DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
            DrawableCompat.setTintList(DrawableCompat.wrap(historyButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));

            imageViewModeIcon.setColorFilter(ContextCompat.getColor(((MainActivity)getActivity()).getApplicationContext(), R.color.lightColorB));
            imageViewIconSwitchToReadMode.setColorFilter(ContextCompat.getColor(((MainActivity)getActivity()).getApplicationContext(), R.color.lightColorB));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraService.deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {

        String type = ((Bundle)arg).getString(OcrHandler.BUNDLE_TYPE);
        if ((!searchTerm.isEmpty()) && (type != null) && (type.equals(OcrHandler.DETECTOR_ACTION_STREAM))) {
            ArrayList<TextBlockParcel> texts = ((Bundle)arg).getParcelableArrayList(OcrHandler.BUNDLE_DETECTIONS);

            // find the term
            int found = -1;
            for(int i=0; i<texts.size(); i++) {
                if (texts.get(i).getText().toLowerCase().contains(searchTerm.toLowerCase())) { found = i; }
            }

            // gets the search term
            if (found != -1) {
                Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.toast_search_term_found_within_text, searchTerm, texts.get(found)), Toast.LENGTH_SHORT).show();
                ((MainActivity)getActivity()).vibrate(500);
                // reset analysing
                searchTerm = "";
                mCameraService.play(MediaActionSound.STOP_VIDEO_RECORDING);

                // get image and image config
                ImageConfigParcel config = ((Bundle)arg).getParcelable(OcrHandler.BUNDLE_IMG_CONFIG);
                byte[] image = ((Bundle)arg).getByteArray(OcrHandler.BUNDLE_IMG_DATA);

                // save an image
                if ((config != null) && (image != null)) {
                    String snapshot = Constants.IMAGE_PATH + "/img-" + System.currentTimeMillis() + ".jpg";
                    // saves the image asynchronously to the external storage
                    new ImagePersistenceHelper(config.getFormat(), config.getRotation(), config.getWidth(), config.getHeight(), snapshot).execute(image);
                    Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.toast_snapshot_saved_to, snapshot), Toast.LENGTH_SHORT).show();
                    AudioService.getInstance().speak(getResources().getString(R.string.read_mode_snapshot_saved));
                }

                // gets the detected texts
                detectedTexts = texts;
                // show button
                mInflatedView.findViewById(R.id.btn_send_to_read).setEnabled(true);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }
}
