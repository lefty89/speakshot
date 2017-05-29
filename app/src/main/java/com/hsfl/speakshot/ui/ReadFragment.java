package com.hsfl.speakshot.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.camera.CameraService;

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
    private ArrayList<String> detectedTexts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflatedView = inflater.inflate(R.layout.read_fragment, container, false);

        // add touch listener
        mInflatedView.setOnTouchListener(this);

        mCameraService = CameraService.getInstance();
        // adds an observer for the text recognizer
        mCameraService.addObserver(this);
        mCameraService.startPreview();

        // show results
        final Button resultButton = (Button)mInflatedView.findViewById(R.id.btn_show_results);
        resultButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // create fragment and add arguments
                ReadResultFragment fragment = new ReadResultFragment();
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("texts", detectedTexts);
                fragment.setArguments(bundle);
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.replace(R.id.fragment_container, fragment);
                ft.commit();
            }
        });

        // light toggle
        final ToggleButton lightSwitch = (ToggleButton)mInflatedView.findViewById(R.id.btn_light_toggle);
        lightSwitch.setChecked(mCameraService.isFlashLightEnabled());
        lightSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCameraService.setFlashLightEnabled(lightSwitch.isChecked());
            }
        });

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
        if (snapshot != null) {
            Toast.makeText(getActivity().getApplicationContext(), "Snapshot saved to: " + snapshot, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // take picture
        mCameraService.analyzePicture();
        return false;
    }
}
