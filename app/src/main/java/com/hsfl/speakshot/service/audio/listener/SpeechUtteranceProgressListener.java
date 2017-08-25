package com.hsfl.speakshot.service.audio.listener;

import android.speech.tts.UtteranceProgressListener;
import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;

import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;

/**
 *
 */

public class SpeechUtteranceProgressListener extends UtteranceProgressListener {
    private static final String TAG = SpeechUtteranceProgressListener.class.getSimpleName();

    private Context mContext;

    public SpeechUtteranceProgressListener(Context context)
    {
        mContext = context;
    }

    @Override
    public void onStart(String utteranceId) {
        Log.d(TAG, "start speaking");
        //MainActivity mainActivity = (MainActivity) mContext;
        //mainActivity.setPlayButtonReadResults(true);
        MainActivity.getInstance().setIconPlayButtonReadResults(true);
    }

    @Override
    public void onError(String utteranceId) {
        Log.i(TAG, "an error occurred while speaking");
        //MainActivity mainActivity = (MainActivity) mContext;
        //mainActivity.setIconPlayButtonReadResults(false);
        MainActivity.getInstance().setIconPlayButtonReadResults(false);
    }

    @Override
    public void onDone(String utteranceId) {
        Log.i(TAG, "finished speaking");
        //MainActivity mainActivity = (MainActivity) mContext;
        //mainActivity.setPlayButtonReadResults(false);
        MainActivity.getInstance().setIconPlayButtonReadResults(false);
    }
}
