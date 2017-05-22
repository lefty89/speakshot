package com.hsfl.speakshot.ui;

import android.content.Context;
import android.os.Environment;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.camera.CameraService;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

public class ControlView extends LinearLayout {
    private static final String TAG = ControlView.class.getSimpleName();

    public ControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.control_view, this);
        final MainActivity main = (MainActivity)getContext();

        // snap picture
        final Button picButton = (Button)findViewById(R.id.btn_picture);
        picButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                main.mCameraService.takePicture();
            }
        });

        // start picture ocr
        final Button ocrPicButton = (Button)findViewById(R.id.btn_ocr_pic);
        ocrPicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                main.mCameraService.analyseImage();
            }
        });

        // start stream ocr
        final Button ocrStreamButton = (Button)findViewById(R.id.btn_ocr_stream);
        ocrStreamButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                main.mCameraService.analyseStream();
            }
        });

        // start vibration
        final Button vibButton = (Button)findViewById(R.id.btn_vibrate);
        vibButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((Vibrator)getContext().getSystemService(android.content.Context.VIBRATOR_SERVICE)).vibrate(800);
            }
        });

        // text view
        final TextView textOutput = (TextView)findViewById(R.id.txt_output);

        // start audio in
        final Button audioInButton = (Button)findViewById(R.id.btn_audio_in);
        audioInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                main.mAudioService.listen();
            }
        });

        // start audio out
        final Button audioOutButton = (Button)findViewById(R.id.btn_audio_out);
        audioOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                main.mAudioService.speak(textOutput.getText().toString());
            }
        });
    }
}