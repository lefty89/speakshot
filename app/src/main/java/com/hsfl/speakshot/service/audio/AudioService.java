package com.hsfl.speakshot.service.audio;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.speech.SpeechRecognizer;

import java.util.Locale;

public class AudioService implements TextToSpeech.OnInitListener {
    private static final String TAG = AudioService.class.getSimpleName();

    /**
     * The AudioService singleton
     */
    private static AudioService instance = null;

    /**
     * The speech recognizer
     */
    private SpeechRecognizer mSpeechRecognizer;

    /**
     * The speech synthesizer
     */
    private TextToSpeech mTts;
    /**
     * Flag that indicates whether the tts is ready
     */
    private boolean mTtsIsReady = false;

    /**
     * tts settings
     */
    private int mPitch = 0;
    private int mSpeechRate = 0;
    private Locale mLocale = Locale.getDefault();

    /**
     * Empty constructor
     */
    AudioService() {}

    /**
     * Gets the AudioService instance
     * @return
     */
    public static AudioService getInstance() {
        if (instance == null) {
            instance = new AudioService();
        }
        return instance;
    }

    /**
     * Outputs the given string as speech
     * @param text
     */
    public void speak(String text) {
        if (mTtsIsReady) {
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    /**
     * Showing google speech input dialog
     * */
    public void listen() {
        if (mSpeechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");

            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
            mSpeechRecognizer.startListening(intent);
            new CountDownTimer(4000, 1000) {
                public void onTick(long millisUntilFinished) {
                    //do nothing, just let it tick
                }
                public void onFinish() {
                    mSpeechRecognizer.cancel();
                }
            }.start();
        } else {
            Log.d(TAG, "Speech recognition is not available");
        }
    }

    /**
     * Initializes the AudioService
     * @param context
     */
    public void init(Context context) {
        // creates speech synthesizer
        mTts = new TextToSpeech(context, this);
        // creates speech recognizer
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            mSpeechRecognizer.setRecognitionListener(new SpeechListener(context));
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            mTts.setPitch(mPitch);              // set pitch level
            mTts.setSpeechRate(mSpeechRate);    // set speech speed rate

            int result = mTts.setLanguage(mLocale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, "Language is not supported");
            } else {
                mTtsIsReady = true;
            }
        } else {
            Log.d(TAG, "Initilization Failed");
        }
    }
}
