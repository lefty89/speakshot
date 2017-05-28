package com.hsfl.speakshot.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.audio.AudioService;

import java.util.ArrayList;

public class ReadResultFragment extends Fragment {
    private static final String TAG = ReadResultFragment.class.getSimpleName();

    /**
     * the inflated view
     */
    private View mInflatedView;

    /**
     * Service provider that handles the camera object
     */
    private AudioService mAudioService;

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
        detectedTexts = this.getArguments().getStringArrayList("texts");
        updateTextView();

        // gets the AudioService
        mAudioService = AudioService.getInstance();

        // close view
        final Button closeButton = (Button)mInflatedView.findViewById(R.id.read_fragment_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction ft = fragmentManager.beginTransaction();
                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack so the user can navigate back
                ft.replace(R.id.fragment_container, (new ReadFragment()));
                ft.addToBackStack(null);
                // Commit the transaction
                ft.commit();
            }
        });

        // close view
        final Button nextButton = (Button)mInflatedView.findViewById(R.id.read_fragment_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                counter = (counter+1) % detectedTexts.size();
                updateTextView();
            }
        });

        // close view
        final Button prevButton = (Button)mInflatedView.findViewById(R.id.read_fragment_prev);
        prevButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                counter = (counter-1 < 0) ? detectedTexts.size()-1 : counter-1;
                updateTextView();
            }
        });

        // play view
        final Button playButton = (Button)mInflatedView.findViewById(R.id.read_fragment_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mAudioService.speak(detectedTexts.get(counter));
            }
        });

        return mInflatedView;
    }

    /**
     * updates the text view
     */
    private void updateTextView() {
        // text view
        final TextView textOutput = (TextView)mInflatedView.findViewById(R.id.read_fragment_orc_output);
        textOutput.setText(detectedTexts.get(counter));
    }
}