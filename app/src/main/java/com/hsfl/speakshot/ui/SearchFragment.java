package com.hsfl.speakshot.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
     * The team to search for
     */
    private String searchTerm = "";

    /**
     * contains the returned ocr texts
     */
    private ArrayList<TextBlockParcel> detectedTexts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflatedView = inflater.inflate(R.layout.search_fragment, container, false);

        // make buttons for settings and mode switch invisible
        ((MainActivity)getActivity()).showButtonsSettingsModeSwitch();

        // add touch listener
        mInflatedView.setOnTouchListener(this);

        mCameraService = CameraService.getInstance();
        mCameraService.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        // add observer
        mCameraService.addObserver(this);

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
                bundle.putParcelableArrayList(ReadResultFragment.IN_TEXTS_PAREL, detectedTexts);
                NavigationService.getInstance().toS(new ReadResultFragment(), bundle);
                // speak hint
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity.getHintsEnabled()) {
                    AudioService.getInstance().speak(mainActivity.getResources().getString(R.string.read_mode_results_hint));
                }
            }
        });
        sendToReadButton.setEnabled(false);

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
        // set button colors
        String themeId = ((MainActivity)getActivity()).getThemeId();
        if (themeId.equals("light")) {
            DrawableCompat.setTintList(DrawableCompat.wrap(lightSwitch.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
            DrawableCompat.setTintList(DrawableCompat.wrap(lightSwitch.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));

            DrawableCompat.setTintList(DrawableCompat.wrap(sendToReadButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));
            DrawableCompat.setTintList(DrawableCompat.wrap(sendToReadButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));

            imageViewModeIcon.setColorFilter(ContextCompat.getColor(((MainActivity)getActivity()).getApplicationContext(), R.color.darkColorB));
        }
        else /*if (themeId.equals("dark"))*/ {
            DrawableCompat.setTintList(DrawableCompat.wrap(lightSwitch.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
            DrawableCompat.setTintList(DrawableCompat.wrap(lightSwitch.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));

            DrawableCompat.setTintList(DrawableCompat.wrap(sendToReadButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorB)));
            DrawableCompat.setTintList(DrawableCompat.wrap(sendToReadButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorB)));

            imageViewModeIcon.setColorFilter(ContextCompat.getColor(((MainActivity)getActivity()).getApplicationContext(), R.color.lightColorB));
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
        final SearchFragment sf = this;

        if (searchTerm.isEmpty()) {
            // open modal
            final EditText txt = new EditText(getActivity());
            new AlertDialog.Builder(getActivity())
                    .setMessage(getResources().getString(R.string.search_mode_dialog_search_term))
                    .setView(txt)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            searchTerm = txt.getText().toString();
                            if (!searchTerm.isEmpty()) {
                                mCameraService.play(MediaActionSound.START_VIDEO_RECORDING);
                                Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.toast_searching_for, searchTerm), Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();
        } else {
            // stop searching
            searchTerm = "";
            mCameraService.play(MediaActionSound.STOP_VIDEO_RECORDING);
            Toast.makeText(getActivity().getApplicationContext(), "Stop searching", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
