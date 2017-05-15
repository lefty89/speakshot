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

    private android.speech.SpeechRecognizer mSpeechRecognizer;

    private boolean mTtsIsReady = false;
    private TextToSpeech mTts;
    private Context mContext;

    /**
     * tts settings
     */
    private int mPitch = 5;
    private int mSpeechRate = 2;
    private Locale mLocale = Locale.getDefault();

    /**
     * Empty constructor
     */
    AudioService() {}

    /**
     * Outputs the given text as speech
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

        if (SpeechRecognizer.isRecognitionAvailable(mContext)) {
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
                    mSpeechRecognizer.stopListening();
                }
            }.start();
        } else {
            Log.d(TAG, "Speech recognition is not available");
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

    /**
     * OcrBuilder
     */
    public static class Builder {
        private AudioService mAudioService = new AudioService();

        /**
         * Creates a ocr builder
         */
        public Builder(Context context) {
            mAudioService.mContext = context;
        }

        /**
         * Creates an instance of the camera service.
         */
        public Builder setPitch(int pitch) {
            if (pitch >= 0) {
                mAudioService.mPitch = pitch;
            }
            return this;
        }

        /**
         * Creates an instance of the camera service.
         */
        public Builder setSpeechRate(int speechRate) {
            if (speechRate >= 0) {
                mAudioService.mSpeechRate = speechRate;
            }
            return this;
        }

        /**
         * Creates an instance of the camera service.
         */
        public Builder setLocale(Locale locale) {
            mAudioService.mLocale = locale;
            return this;
        }

        /**
         * Creates an instance of the camera service.
         */
        public AudioService build() {
            mAudioService.mTts = new TextToSpeech(mAudioService.mContext, mAudioService);

            mAudioService.mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mAudioService.mContext);
            mAudioService.mSpeechRecognizer.setRecognitionListener(new SpeechListener(mAudioService.mContext));

            return mAudioService;
        }
    }
}
