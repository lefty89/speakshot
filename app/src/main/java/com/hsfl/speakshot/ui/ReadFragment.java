package com.hsfl.speakshot.ui;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.Toast;

import com.hsfl.speakshot.Constants;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.helper.ImagePersistenceHelper;
import com.hsfl.speakshot.service.camera.ocr.OcrHandler;
import com.hsfl.speakshot.service.camera.ocr.serialization.*;
import com.hsfl.speakshot.service.navigation.NavigationService;
import com.hsfl.speakshot.service.camera.CameraService;
import android.support.design.widget.FloatingActionButton;

import java.util.*;


public class ReadFragment extends Fragment implements Observer, View.OnTouchListener {
    private static final String TAG = ReadFragment.class.getSimpleName();

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflatedView = inflater.inflate(R.layout.read_fragment, container, false);

        // make buttons for settings and mode switch invisible
        ((MainActivity)getActivity()).showButtonsSettingsModeSwitch();

        // adds an observer for the text recognizer
        mCameraService = CameraService.getInstance();
        mCameraService.addObserver(this);

        // set focus mode
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String focusMode = prefs.getBoolean("use_autofocus_switch", true) ?
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO : Camera.Parameters.FOCUS_MODE_AUTO;
        mCameraService.setFocusMode(focusMode);

        // adds a gesture detector
        ReadFragmentGestureDetector readGestureDetector = new ReadFragmentGestureDetector();
        // Create a GestureDetector
        mGestureDetector = new GestureDetector(getActivity(), readGestureDetector);
        // add touch listener
        mInflatedView.setOnTouchListener(this);

        // init controls
        initializeControls();
        return mInflatedView;
    }

    /**
     * Init the UI controls
     */
    private void initializeControls() {
        // light toggle
        final FloatingActionButton lightSwitch = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_light_toggle);
        lightSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCameraService.setFlashLightEnabled(!mCameraService.isFlashLightEnabled());
                lightSwitch.setSelected(mCameraService.isFlashLightEnabled());
            }
        });
        lightSwitch.setSelected(mCameraService.isFlashLightEnabled());

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraService.deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {

        String type = ((Bundle)arg).getString(OcrHandler.BUNDLE_TYPE);
        if ((type != null) && (type.equals(OcrHandler.DETECTOR_ACTION_RAW))) {
            ArrayList<TextBlockParcel> texts = ((Bundle)arg).getParcelableArrayList(OcrHandler.BUNDLE_DETECTIONS);

            if (texts != null) {
                if (texts.size() > 0) {
                    detectedTexts = texts;

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

                } else {
                    Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.toast_no_text_found), Toast.LENGTH_SHORT).show();
                    AudioService.getInstance().speak(getResources().getString(R.string.read_mode_hint_no_text_found));
                }
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }
}
