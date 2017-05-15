package com.hsfl.speakshot.ui;

import android.content.Context;
import android.os.Environment;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import java.io.File;

public class ControlView extends LinearLayout {
    private static final String TAG = ControlView.class.getSimpleName();

    public ControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.control_view, this);

        final Button picButton = (Button)findViewById(R.id.btn_picture);
        picButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                takePic();
            }
        });

        final Button ocrButton = (Button)findViewById(R.id.btn_ocr);
        ocrButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startOCR();
            }
        });

        final Button vibButton = (Button)findViewById(R.id.btn_vibrate);
        vibButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((Vibrator)getContext().getSystemService(android.content.Context.VIBRATOR_SERVICE)).vibrate(800);
            }
        });
    }

    private void takePic() {
        Log.d(TAG, "takePic");
        MainActivity main = (MainActivity)getContext();
        main.mCameraService.takePicture();
    }

    private void startOCR() {
        Log.d(TAG, "startOCR");

        File sd = Environment.getExternalStorageDirectory();
        String photoPath = sd.getAbsolutePath() + "/camtest/img.jpg";

        MainActivity main = (MainActivity)getContext();
        main.mOcrService.analyseImage(photoPath);
    }
}