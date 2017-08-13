package com.hsfl.speakshot.ui;

import android.app.Fragment;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.content.Context;
import android.widget.Toast;

import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.ocr.processor.LocateTextProcessor;
import com.hsfl.speakshot.service.camera.ocr.processor.RetrieveAllProcessor;
import com.hsfl.speakshot.service.guide.GuidingService;
import com.hsfl.speakshot.service.view.ViewService;
import com.hsfl.speakshot.service.camera.CameraService;
import android.support.design.widget.FloatingActionButton;

import java.util.*;


public class ReadFragment extends Fragment implements Observer, View.OnTouchListener, View.OnLongClickListener {
    private static final String TAG = ReadFragment.class.getSimpleName();

    /**
     * Flag that helps to chose whether its a long or short tab
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
    private ArrayList<String> detectedTexts;

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

        // show results
        final FloatingActionButton resultButton = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_show_results);
        resultButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList(ReadResultFragment.IN_TEXTS, detectedTexts);
                ViewService.getInstance().toS(new ReadResultFragment(), bundle);
                // make buttons for settings and mode switch invisible
                ((MainActivity)getActivity()).hideButtonsSettingsModeSwitch();
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
                ViewService.getInstance().toS(new HistoryFragment(), null);
            }
        });

        // populate spinner with items using the custom adapter which sets up the colors and font size
        String themeId = ((MainActivity)getActivity()).getThemeId();
        String[] spin_arry = getResources().getStringArray(R.array.read_mode_array);
        MainActivity.CustomArrayAdapter mAdapter = new MainActivity.CustomArrayAdapter<CharSequence>((MainActivity)getActivity(), spin_arry);
        mAdapter.setThemeId(themeId);
        int spinnerPadding = (int)((MainActivity)getActivity()).getResources().getDimension(R.dimen.spinner_padding);
        mAdapter.setSpinnerPadding(spinnerPadding);
        float spinnerTextSize = ((MainActivity)getActivity()).getResources().getDimension(R.dimen.spinner_dropdown_text_size);
        mAdapter.setSpinnerTextSize(spinnerTextSize);
        final Spinner spinnerReadMode = (Spinner)mInflatedView.findViewById(R.id.spinner_read_mode);
        spinnerReadMode.setAdapter(mAdapter);

        return mInflatedView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraService.deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        // gets the detected texts
        ArrayList<String> texts = ((Bundle)arg).getStringArrayList(RetrieveAllProcessor.RESULT_TEXTS);
        if (texts != null) {
            if (texts.size() > 0) {
                detectedTexts = texts;
                // enable / disable result button
                mInflatedView.findViewById(R.id.btn_show_results).setEnabled(true);
            } else {
                Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.toast_no_text_found), Toast.LENGTH_SHORT).show();
                AudioService.getInstance().speak(getResources().getString(R.string.read_mode_hint_no_text_found));
            }
        }
        // toasts the snapshot path
        String snapshot = ((Bundle)arg).getString(RetrieveAllProcessor.RESULT_SNAPSHOT);
        if ((snapshot != null) && (!snapshot.equals(""))) {
            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.toast_snapshot_saved_to, snapshot), Toast.LENGTH_SHORT).show();
            AudioService.getInstance().speak(getResources().getString(R.string.read_mode_snapshot_saved_to, snapshot));
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mIsLongTab = true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mIsLongTab) {
                mCameraService.focus(event.getX(), event.getY());
            }
            mIsLongTab = false;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        if (mIsLongTab) {
            RetrieveAllProcessor processor = new RetrieveAllProcessor();
            processor.setImagePersisting(true);
            // creates a processor that returns all texts found, also save the image here
            mCameraService.analyzePicture(processor);
        }
        mIsLongTab = false;
        return false;
    }
}
