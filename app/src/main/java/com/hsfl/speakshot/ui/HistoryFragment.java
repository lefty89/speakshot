package com.hsfl.speakshot.ui;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.hsfl.speakshot.Constants;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.CameraService;
import com.hsfl.speakshot.service.camera.ocr.processor.ProcessorChain;
import com.hsfl.speakshot.service.camera.ocr.processor.RetrieveAllProcessor;
import com.hsfl.speakshot.service.view.ViewService;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

public class HistoryFragment extends Fragment implements Observer {
    private static final String TAG = HistoryFragment.class.getSimpleName();

    /**
     * the inflated view
     */
    private View mInflatedView;

    /**
     * The currently selected image position
     */
    private int mCurrentPosition = 0;

    /**
     * Contains all the files within the defined image folder
     */
    private ArrayList<File> mImages = new ArrayList<>();

    /**
     * Service provider that handles the camera object
     */
    private CameraService mCameraService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflatedView = inflater.inflate(R.layout.history_fragment, container, false);

        // make buttons for settings and mode switch invisible
        ((MainActivity)getActivity()).hideButtonsSettingsModeSwitch();

        // gets the camera service
        mCameraService = CameraService.getInstance();
        // adds an observer for the text recognizer
        mCameraService.addObserver(this);

        // close button
        final FloatingActionButton closeButton = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_history_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ViewService.getInstance().back();
            }
        });

        // prev button
        final FloatingActionButton prevButton = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_history_prev);
        prevButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                nextImage(-1);
                updateBackgroundImage();
            }
        });

        // analyze button
        final FloatingActionButton analyzeButton = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_history_analyze);
        analyzeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // analyzes the given file using the processor to read all
                ProcessorChain pc = new ProcessorChain();
                pc.add(new RetrieveAllProcessor());
                mCameraService.analyzePicture(mImages.get(mCurrentPosition), pc);

                // speak hint
                MainActivity mainActivity = ((MainActivity) getActivity());
                if (mainActivity.getHintsEnabled()) {
                    AudioService.getInstance().speak(mainActivity.getResources().getString(R.string.read_mode_results_hint));
                }
            }
        });

        // next button
        final FloatingActionButton nextButton = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_history_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                nextImage(1);
                updateBackgroundImage();
            }
        });

        // delete button
        final FloatingActionButton deleteButton = (FloatingActionButton)mInflatedView.findViewById(R.id.btn_history_delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deleteImageFromGallery();
                nextImage(0);
                updateBackgroundImage();
            }
        });

        // init the gallery
        initGallery();
        updateBackgroundImage();

        return mInflatedView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraService.deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        // gets the detected texts
        ArrayList<String> texts = ((Bundle)arg).getStringArrayList(RetrieveAllProcessor.RESULT_ALL_TEXTS);
        if (texts != null) {
            if (texts.size() > 0) {
                // opens the result view with the detected texts
                Bundle bundle = new Bundle();
                bundle.putStringArrayList(ReadResultFragment.IN_TEXTS, texts);
                ViewService.getInstance().toS(new ReadResultFragment(), bundle);

            } else {
                Toast.makeText(getActivity().getBaseContext(), getResources().getString(R.string.toast_no_text_found), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Initialises the gallery
     */
    private void initGallery() {
        File imgDir = new File(Constants.IMAGE_PATH);
        for (File f : imgDir.listFiles()) {
            if (f.isFile()) {
                mImages.add(f);
            }
        }
    }

    /**
     * Updates the background image and header text
     */
    private void updateBackgroundImage() {
        if ((mCurrentPosition >= 0) && (mCurrentPosition < mImages.size())) {
            File f = mImages.get(mCurrentPosition);

            // gets a drawable to draw into the image view
            Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
            ImageView imgHistory = (ImageView)mInflatedView.findViewById(R.id.img_history);
            imgHistory.setBackground(new BitmapDrawable(getResources(), bmp));

            // updates the header name
            TextView imageNameText = (TextView)mInflatedView.findViewById(R.id.txt_history_header_name);
            imageNameText.setText(String.format(Locale.getDefault(),"[%d/%d] %s", mCurrentPosition+1, mImages.size(), f.getName()));

            // updates the header date
            TextView imageModText = (TextView)mInflatedView.findViewById(R.id.txt_history_header_mod);
            imageModText.setText(String.format(Locale.getDefault(),"%tc", f.lastModified()));

        }
    }

    /**
     * Removes the current image from the gallery
     */
    private void deleteImageFromGallery() {
        if (mImages.size() > 0) {
            // gets the current file
            File f = mImages.get(mCurrentPosition);
            if (f.exists()) {
                // deletes the file
                boolean b = f.delete();
                if (b) {
                    // remove the file from the gallery list
                    mImages.remove(f);
                    AudioService.getInstance().speak(getResources().getString(R.string.read_mode_snapshot_deleted));
                }
            }
        }
    }

    /**
     * Switches to the next/prev image
     * @param i
     */
    private void nextImage(int i) {
        mCurrentPosition = (mCurrentPosition+i) % mImages.size();
        if (mCurrentPosition < 0) {
            mCurrentPosition += mImages.size();
        }
    }
}