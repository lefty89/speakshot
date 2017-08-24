package com.hsfl.speakshot.ui;

import android.app.Fragment;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.util.Log;

import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.camera.ocr.serialization.TextBlockParcel;
import com.hsfl.speakshot.service.navigation.NavigationService;
import com.hsfl.speakshot.service.audio.AudioService;
import android.support.design.widget.FloatingActionButton;

import java.util.ArrayList;

public class ResultFragment extends Fragment {
    private static final String TAG = ResultFragment.class.getSimpleName();

    /**
     * Identifiers to read out of the bundle that is given
     * to this fragment
     */
    public static final String IN_TEXTS = "texts";
    public static final String IN_TEXTS_PAREL = "texts_parcel";

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
    private ArrayList<TextBlockParcel> detectedTexts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflatedView = inflater.inflate(R.layout.result_fragment, container, false);

        // make buttons for settings and mode switch invisible
        ((MainActivity)getActivity()).hideButtonsSettingsModeSwitch();

        // gets the detected texts
        if (getArguments().getStringArrayList(ResultFragment.IN_TEXTS_PAREL) != null) {
            detectedTexts = getArguments().getParcelableArrayList(ResultFragment.IN_TEXTS_PAREL);
        }

        updateTextView();
        initializeControls();



        return mInflatedView;
    }

    /**
     * Inits the UI controls
     */
    private void initializeControls() {
        // close view
        final FloatingActionButton closeButton = (FloatingActionButton)mInflatedView.findViewById(R.id.read_fragment_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                NavigationService.getInstance().back();
            }
        });

        // play current text block
        final FloatingActionButton playButton = (FloatingActionButton)mInflatedView.findViewById(R.id.read_fragment_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String text = ((MainActivity)getActivity()).getResources().getString(R.string.result_audio_result_x_of_y, counter+1, detectedTexts.size(), detectedTexts.get(counter).getText());
                AudioService.getInstance().speak(text);
            }
        });

        // next text block
        final FloatingActionButton nextButton = (FloatingActionButton)mInflatedView.findViewById(R.id.read_fragment_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                counter = (counter+1) % detectedTexts.size();
                updateTextView();
                playButton.performClick();
            }
        });

        // previous text block
        final FloatingActionButton prevButton = (FloatingActionButton)mInflatedView.findViewById(R.id.read_fragment_prev);
        prevButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                counter = (counter-1 < 0) ? detectedTexts.size()-1 : counter-1;
                updateTextView();
                playButton.performClick();
            }
        });

        // set colors: buttons, textViews and background
        String themeId = ((MainActivity)getActivity()).getThemeId();
        final TextView resultText = (TextView)mInflatedView.findViewById(R.id.txt_sections_result);
        final TextView headerText = (TextView)mInflatedView.findViewById(R.id.txt_sections_header);
        final FrameLayout frameLayoutBackground = (FrameLayout) mInflatedView.findViewById(R.id.result_fragment_background);
        if (themeId.equals("light")) {

            frameLayoutBackground.setBackgroundColor(Color.WHITE);

            resultText.setTextColor(Color.BLACK);
            headerText.setTextColor(Color.BLACK);

            DrawableCompat.setTintList(DrawableCompat.wrap(closeButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
            DrawableCompat.setTintList(DrawableCompat.wrap(closeButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));

            DrawableCompat.setTintList(DrawableCompat.wrap(nextButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
            DrawableCompat.setTintList(DrawableCompat.wrap(nextButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));

            DrawableCompat.setTintList(DrawableCompat.wrap(prevButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
            DrawableCompat.setTintList(DrawableCompat.wrap(prevButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));

            DrawableCompat.setTintList(DrawableCompat.wrap(playButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));
            DrawableCompat.setTintList(DrawableCompat.wrap(playButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));

            //imageViewModeIcon.setColorFilter(ContextCompat.getColor(((MainActivity)getActivity()).getApplicationContext(), R.color.darkColorA));
        }
        else /*if (themeId.equals("dark"))*/ {

            frameLayoutBackground.setBackgroundColor(Color.BLACK);

            resultText.setTextColor(Color.WHITE);
            headerText.setTextColor(Color.WHITE);

            DrawableCompat.setTintList(DrawableCompat.wrap(closeButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
            DrawableCompat.setTintList(DrawableCompat.wrap(closeButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));

            DrawableCompat.setTintList(DrawableCompat.wrap(nextButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
            DrawableCompat.setTintList(DrawableCompat.wrap(nextButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));

            DrawableCompat.setTintList(DrawableCompat.wrap(prevButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
            DrawableCompat.setTintList(DrawableCompat.wrap(prevButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));

            DrawableCompat.setTintList(DrawableCompat.wrap(playButton.getDrawable()), ColorStateList.valueOf(getResources().getColor(R.color.lightColorA)));
            DrawableCompat.setTintList(DrawableCompat.wrap(playButton.getBackground()), ColorStateList.valueOf(getResources().getColor(R.color.darkColorA)));

            //imageViewModeIcon.setColorFilter(ContextCompat.getColor(((MainActivity)getActivity()).getApplicationContext(), R.color.lightColorA));
        }
    }

    /**
     * updates the text view
     */
    private void updateTextView() {
        final TextView resultText = (TextView)mInflatedView.findViewById(R.id.txt_sections_result);
        resultText.setText(detectedTexts.get(counter).getText());
        final TextView headerText = (TextView)mInflatedView.findViewById(R.id.txt_sections_header);
        String text = ((MainActivity)getActivity()).getResources().getString(R.string.result_textview_header_result_x_of_y, counter+1, detectedTexts.size());
        headerText.setText(text);
    }

    /**
     * Switches the icon of the play button
     * @param speaking
     */
    public void setIconPlayButtonReadResults(boolean speaking) {
        final FloatingActionButton playButtonReadResults = (FloatingActionButton)mInflatedView.findViewById(R.id.read_fragment_play);
        playButtonReadResults.setSelected(speaking);
        if (speaking) {
            Log.i(TAG, "test speaking 1");
        }
        else {
            Log.i(TAG, "test speaking 0");
        }
    }
}