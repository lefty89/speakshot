package com.hsfl.speakshot.ui;

import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.hsfl.speakshot.R;
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
                bundle.putStringArrayList("texts", detectedTexts);
                ViewService.getInstance().toS(new ReadResultFragment(), bundle);
            }
        });
        resultButton.setEnabled(false);

        // light toggle
        final FloatingActionButton lightSwitch = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_light_toggle);
        lightSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            mCameraService.setFlashLightEnabled(!mCameraService.isFlashLightEnabled());
            if (mCameraService.isFlashLightEnabled())
                lightSwitch.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_flash_on_black_24dp, getActivity().getTheme()));
            else
                lightSwitch.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_flash_off_black_24dp, getActivity().getTheme()));
            }
        });
        if (mCameraService.isFlashLightEnabled())
            lightSwitch.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_flash_on_black_24dp, getActivity().getTheme()));
        else
            lightSwitch.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_flash_off_black_24dp, getActivity().getTheme()));

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
        ArrayList<String> texts = ((Bundle)arg).getStringArrayList("texts");
        if (texts != null) {
            if (texts.size() > 0) {
                detectedTexts = texts;
                // enable / disable result button
                mInflatedView.findViewById(R.id.btn_show_results).setEnabled(true);
            } else {
                Toast.makeText(getActivity().getApplicationContext(), "Nothing found", Toast.LENGTH_SHORT).show();
            }
        }
        // toasts the snapshot path
        String snapshot = ((Bundle)arg).getString("snapshot");
        if ((snapshot != null) && (!snapshot.equals(""))) {
            Toast.makeText(getActivity().getApplicationContext(), "Snapshot saved to: " + snapshot, Toast.LENGTH_SHORT).show();
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
            mCameraService.analyzePicture();
        }
        mIsLongTab = false;
        return false;
    }
}
