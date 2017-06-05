package com.hsfl.speakshot.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.view.ViewService;
import com.hsfl.speakshot.service.camera.CameraService;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import android.support.design.widget.FloatingActionButton;

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
    private ArrayList<String> detectedTexts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mInflatedView = inflater.inflate(R.layout.search_fragment, container, false);
        // add touch listener
        mInflatedView.setOnTouchListener(this);

        mCameraService = CameraService.getInstance();
        // adds an observer for the text recognizer
        mCameraService.addObserver(this);
        mCameraService.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        // show result view
        final FloatingActionButton sendToReadButton = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_send_to_read);
        sendToReadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("texts", detectedTexts);
                ViewService.getInstance().toS(new ReadResultFragment(), bundle);
            }
        });
        sendToReadButton.setEnabled(false);

        // light toggle
        final FloatingActionButton lightSwitch = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_light_toggle);
        lightSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            mCameraService.setFlashLightEnabled(!mCameraService.isFlashLightEnabled());
            if (mCameraService.isFlashLightEnabled())
                lightSwitch.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_flash_off_black_24dp, getActivity().getTheme()));
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
        // gets the search term
        String term = ((Bundle)arg).getString("term");
        if (term != null) {
            Toast.makeText(getActivity().getApplicationContext(), "Term '" + searchTerm + "' found within " + "'" + term + "'", Toast.LENGTH_SHORT).show();
            ((Vibrator) getActivity().getApplication().getSystemService(android.content.Context.VIBRATOR_SERVICE)).vibrate(800);
            // reset analysing
            searchTerm = "";
            mCameraService.analyseStream("");
        }
        // gets the detected texts
        ArrayList<String> texts = ((Bundle)arg).getStringArrayList("texts");
        if (texts != null) {
            detectedTexts = texts;
            // show button
            mInflatedView.findViewById(R.id.btn_send_to_read).setEnabled(true);
        }
        // gets the snapshot path
        String snapshot = ((Bundle)arg).getString("snapshot");
        if (snapshot != null) {
            Toast.makeText(getActivity().getApplicationContext(), "Snapshot saved to: " + snapshot, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (searchTerm.equals("")) {
            // open modal
            final EditText txt = new EditText(getActivity());
            new AlertDialog.Builder(getActivity())
                    .setMessage("Search for word")
                    .setView(txt)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // start searching
                            searchTerm = txt.getText().toString();
                            if (!searchTerm.equals("")) {
                                mCameraService.analyseStream(searchTerm);
                                Toast.makeText(getActivity().getApplicationContext(), "Searching for: " + searchTerm, Toast.LENGTH_SHORT).show();
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
            mCameraService.analyseStream("");
            Toast.makeText(getActivity().getApplicationContext(), "Stop searching", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
