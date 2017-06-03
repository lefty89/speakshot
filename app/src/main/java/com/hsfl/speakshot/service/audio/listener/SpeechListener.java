package com.hsfl.speakshot.service.audio.listener;

import android.content.Context;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class SpeechListener implements RecognitionListener {
    private static final String TAG = SpeechListener.class.getSimpleName();

    private Context mContext;

    public SpeechListener(Context context) {
        mContext = context;
    }

    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "onReadyForSpeech");
    }

    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    public void onRmsChanged(float rmsdB) {
        Log.d(TAG, "onRmsChanged");
    }

    public void onBufferReceived(byte[] buffer) {
        Log.d(TAG, "onBufferReceived");
    }

    public void onEndOfSpeech() {
        Log.d(TAG, "onEndofSpeech");
    }

    public void onError(int error) {
        Log.d(TAG,  "error " +  error);
    }

    public void onResults(Bundle results) {
        String str = new String();
        Log.d(TAG, "onResults " + results);
        ArrayList data = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
        for (int i = 0; i < data.size(); i++)
        {
            Log.d(TAG, "result " + data.get(i));
            str += data.get(i);
        }
        Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show();
    }

    public void onPartialResults(Bundle partialResults) {
        Log.d(TAG, "onPartialResults");
    }

    public void onEvent(int eventType, Bundle params) {
        Log.d(TAG, "onEvent " + eventType);
    }
}
