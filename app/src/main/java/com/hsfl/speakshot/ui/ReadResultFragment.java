package com.hsfl.speakshot.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.view.ViewService;
import com.hsfl.speakshot.service.audio.AudioService;
import android.support.design.widget.FloatingActionButton;

import java.util.ArrayList;

public class ReadResultFragment extends Fragment {
    private static final String TAG = ReadResultFragment.class.getSimpleName();

    /**
     * Identifiers to read out of the bundle that is given
     * to this fragment
     */
    public static final String TEXTS = "texts";

    /**
     * the inflated view
     */
    private View mInflatedView;

    /**
     * the paging counter
     */
    private int counter = 0;

    /**
     * contains the returned ocr texts
     */
    private ArrayList<String> detectedTexts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflatedView = inflater.inflate(R.layout.read_result_fragment, container, false);
        // gets the detected texts
        detectedTexts = getArguments().getStringArrayList(ReadResultFragment.TEXTS);
        updateTextView();

        // close view
        final FloatingActionButton closeButton = (FloatingActionButton)mInflatedView.findViewById(R.id.read_fragment_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // make buttons for settings and mode switch invisible
                ((MainActivity)getActivity()).showButtonsSettingsModeSwitch();
                ViewService.getInstance().back();
            }
        });

        // next text block
        final FloatingActionButton nextButton = (FloatingActionButton)mInflatedView.findViewById(R.id.read_fragment_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                counter = (counter+1) % detectedTexts.size();
                updateTextView();
            }
        });

        // previous text block
        final FloatingActionButton prevButton = (FloatingActionButton)mInflatedView.findViewById(R.id.read_fragment_prev);
        prevButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                counter = (counter-1 < 0) ? detectedTexts.size()-1 : counter-1;
                updateTextView();
            }
        });

        // play current text block
        final FloatingActionButton playButton = (FloatingActionButton)mInflatedView.findViewById(R.id.read_fragment_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AudioService.getInstance().speak(detectedTexts.get(counter));
            }
        });

        return mInflatedView;
    }

    /**
     * updates the text view
     */
    private void updateTextView() {
        final TextView resultText = (TextView)mInflatedView.findViewById(R.id.txt_sections_result);
        resultText.setText(detectedTexts.get(counter));
        final TextView headerText = (TextView)mInflatedView.findViewById(R.id.txt_sections_header);
        headerText.setText(String.format("Section %1s of %2s", counter+1, detectedTexts.size()));
    }
}