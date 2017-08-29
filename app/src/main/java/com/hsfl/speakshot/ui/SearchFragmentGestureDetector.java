package com.hsfl.speakshot.ui;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaActionSound;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.Toast;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.CameraService;
import com.hsfl.speakshot.service.navigation.NavigationService;


public class SearchFragmentGestureDetector implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private static final String TAG = SearchFragmentGestureDetector.class.getSimpleName();

    /**
     * Parent Fragment
     */
    private SearchFragment mSearchFragment = null;

    /**
     * Constructor
     */
    SearchFragmentGestureDetector(SearchFragment searchFragment) {
        mSearchFragment = searchFragment;
    }

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
        if (mSearchFragment.searchTerm.isEmpty()) {
            // open modal
            final EditText txt = new EditText(mSearchFragment.getActivity());
            new AlertDialog.Builder(mSearchFragment.getActivity())
                    .setMessage(mSearchFragment.getResources().getString(R.string.search_mode_dialog_search_term))
                    .setView(txt)
                    .setPositiveButton(mSearchFragment.getResources().getString(R.string.btn_search), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mSearchFragment.searchTerm = txt.getText().toString();
                            if (!mSearchFragment.searchTerm.isEmpty()) {
                                CameraService.getInstance().play(MediaActionSound.START_VIDEO_RECORDING);
                                Toast.makeText(mSearchFragment.getActivity().getApplicationContext(), mSearchFragment.getResources().getString(R.string.toast_searching_for, mSearchFragment.searchTerm), Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton(mSearchFragment.getResources().getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();
        } else {
            // stop searching
            mSearchFragment.searchTerm = "";
            CameraService.getInstance().play(MediaActionSound.STOP_VIDEO_RECORDING);
            Toast.makeText(mSearchFragment.getActivity().getApplicationContext(), "Stop searching", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getX() > e2.getX()) {
            MainActivity.getInstance().switchMode();
            NavigationService.getInstance().to(new ReadFragment(), null);
        }
        return true;
    }
}
