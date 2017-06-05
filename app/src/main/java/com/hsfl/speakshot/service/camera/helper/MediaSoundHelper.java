package com.hsfl.speakshot.service.camera.helper;

import android.media.MediaActionSound;
import android.util.Log;

/**
 * A helper that can play camera related sound samples
 */
public class MediaSoundHelper {
    private static final String TAG = MediaSoundHelper.class.getSimpleName();

    /**
     * MediaActionSound
     */
    private MediaActionSound mSound;

    /**
     * Constructor
     */
    public MediaSoundHelper() {
        mSound = new MediaActionSound();
        mSound.load(MediaActionSound.SHUTTER_CLICK);
        mSound.load(MediaActionSound.START_VIDEO_RECORDING);
        mSound.load(MediaActionSound.STOP_VIDEO_RECORDING);
        mSound.load(MediaActionSound.FOCUS_COMPLETE);
    }

    /**
     * Frees the resources for the sound player
     */
    public void release() {
        if (mSound != null) {
            mSound.release();
            mSound = null;
        }
    }

    /**
     * Plays a camera specific action sound
     * @param action
     */
    public synchronized void play(int action) {
        switch(action) {
            case MediaActionSound.SHUTTER_CLICK:
                mSound.play(MediaActionSound.SHUTTER_CLICK);
                break;
            case MediaActionSound.FOCUS_COMPLETE:
                mSound.play(MediaActionSound.FOCUS_COMPLETE);
                break;
            case MediaActionSound.START_VIDEO_RECORDING:
                mSound.play(MediaActionSound.START_VIDEO_RECORDING);
                break;
            case MediaActionSound.STOP_VIDEO_RECORDING:
                mSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
                break;
            default:
                Log.w(TAG, "Unrecognized action:" + action);
        }
    }
}
