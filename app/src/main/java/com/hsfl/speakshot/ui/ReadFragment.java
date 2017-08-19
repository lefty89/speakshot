package com.hsfl.speakshot.ui;

import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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


public class ReadFragment extends Fragment implements Observer, View.OnTouchListener, View.OnLongClickListener {
    private static final String TAG = ReadFragment.class.getSimpleName();

    /**
     * Flag that helps to choose whether it's a long or short tab
     */
    private boolean mIsLongTab = false;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflatedView = inflater.inflate(R.layout.read_fragment, container, false);

        // make buttons for settings and mode switch invisible
        ((MainActivity)getActivity()).showButtonsSettingsModeSwitch();

        // add touch listener
        mInflatedView.setOnTouchListener(this);
        mInflatedView.setOnLongClickListener(this);

        mCameraService = CameraService.getInstance();
        // adds an observer for the text recognizer
        mCameraService.addObserver(this);
        mCameraService.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        // init controls
        initializeControls();

        return mInflatedView;
    }

    /**
     * Inits the UI controls
     */
    private void initializeControls() {
        // show results
        final FloatingActionButton resultButton = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_show_results);
        resultButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(ReadResultFragment.IN_TEXTS_PAREL, detectedTexts);
                NavigationService.getInstance().toS(new ReadResultFragment(), bundle);
                // make buttons for settings and mode switch invisible
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.hideButtonsSettingsModeSwitch();
                // speak hint
                if (mainActivity.getHintsEnabled()) {
                    AudioService.getInstance().speak(mainActivity.getResources().getString(R.string.read_mode_results_hint));
                }
            }
        });
        resultButton.setEnabled(false);

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

                    // enable / disable result button
                    mInflatedView.findViewById(R.id.btn_show_results).setEnabled(true);

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
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mIsLongTab = true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mIsLongTab) {
                // TODO sometimes the app crashes here.
                mCameraService.focus(event.getX(), event.getY());
                AudioService.getInstance().stopSpeaking();
            }
            mIsLongTab = false;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        if (mIsLongTab) {
            // creates a processor that returns all texts found, also save the image here
            mCameraService.analyzePicture();
        }
        mIsLongTab = false;
        return false;
    }
}
