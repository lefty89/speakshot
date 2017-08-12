package com.hsfl.speakshot.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ImageView;

import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.camera.ocr.processor.FindTermProcessor;
import com.hsfl.speakshot.service.camera.ocr.processor.ImageProcessor;
import com.hsfl.speakshot.service.camera.ocr.processor.ProcessorChain;
import com.hsfl.speakshot.service.view.ViewService;
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
    private ArrayList<String> detectedTexts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflatedView = inflater.inflate(R.layout.search_fragment, container, false);

        // make buttons for settings and mode switch invisible
        ((MainActivity)getActivity()).showButtonsSettingsModeSwitch();

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
                bundle.putStringArrayList(ReadResultFragment.IN_TEXTS, detectedTexts);
                ViewService.getInstance().toS(new ReadResultFragment(), bundle);
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

        // populate spinner with items using the custom adapter which sets up the colors and font size
        String[] spin_arry = getResources().getStringArray(R.array.search_mode_array);
        MainActivity.CustomArrayAdapter mAdapter = new MainActivity.CustomArrayAdapter<CharSequence>((MainActivity)getActivity(), spin_arry);
        mAdapter.setThemeId(themeId);
        int spinnerPadding = (int)((MainActivity)getActivity()).getResources().getDimension(R.dimen.spinner_padding);
        mAdapter.setSpinnerPadding(spinnerPadding);
        float spinnerTextSize = ((MainActivity)getActivity()).getResources().getDimension(R.dimen.spinner_dropdown_text_size);
        mAdapter.setSpinnerTextSize(spinnerTextSize);
        final Spinner spinnerSearchMode = (Spinner)mInflatedView.findViewById(R.id.spinner_search_mode);
        spinnerSearchMode.setAdapter(mAdapter);

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
        String term = ((Bundle)arg).getString(FindTermProcessor.RESULT_TERM_FOUND);
        if (term != null) {
            Toast.makeText(getActivity().getApplicationContext(), "Term '" + searchTerm + "' found within " + "'" + term + "'", Toast.LENGTH_SHORT).show();
            ((Vibrator) getActivity().getApplication().getSystemService(android.content.Context.VIBRATOR_SERVICE)).vibrate(800);
            // reset analysing
            searchTerm = "";
            mCameraService.stopAnalyseStream();
        }
        // gets the detected texts
        ArrayList<String> texts = ((Bundle)arg).getStringArrayList(FindTermProcessor.RESULT_TERM_SEARCH);
        if (texts != null) {
            detectedTexts = texts;
            // show button
            mInflatedView.findViewById(R.id.btn_send_to_read).setEnabled(true);
        }
        // gets the snapshot path
        String snapshot = ((Bundle)arg).getString(ImageProcessor.RESULT_SNAPSHOT_PATH);
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
                            searchTerm = txt.getText().toString();
                            if (!searchTerm.equals("")) {
                                // creates a processor that searches for a given term and saves
                                // the image on success
                                ProcessorChain pc = new ProcessorChain();
                                pc.add(new FindTermProcessor(searchTerm, true));

                                // start analyzer
                                mCameraService.startAnalyseStream(pc);
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
            mCameraService.stopAnalyseStream();
            Toast.makeText(getActivity().getApplicationContext(), "Stop searching", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
