package com.hsfl.speakshot.ui;

import android.view.*;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.CameraService;
import com.hsfl.speakshot.service.navigation.NavigationService;


public class ReadFragmentGestureDetector implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private static final String TAG = ReadFragmentGestureDetector.class.getSimpleName();

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        CameraService.getInstance().focus(e.getX(), e.getY());
        AudioService.getInstance().stopSpeaking();

        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        CameraService.getInstance().analyzePicture();
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getX() < e2.getX()) {
            MainActivity.getInstance().switchMode();
            NavigationService.getInstance().to(new SearchFragment(), null);
        }

        return true;
    }
}
