package com.hsfl.speakshot.ui;

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;

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

        // dark theme
        final Button themeDarkButton = (Button)findViewById(R.id.btn_theme_dark);
        themeDarkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.THEME =  R.style.AppTheme_Dark;
                main.recreate();
            }
        });

        // light theme
        final Button themeLightButton = (Button)findViewById(R.id.btn_theme_light);
        themeLightButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.THEME =  R.style.AppTheme;
                main.recreate();
            }
        });

        // start audio in
        final Button audioInButton = (Button)findViewById(R.id.btn_audio_in);
        audioInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                main.mAudioService.listen();
            }
        });

        // text view
        final TextView textOutput = (TextView)findViewById(R.id.txt_output);

        // start audio out
        final Button audioOutButton = (Button)findViewById(R.id.btn_audio_out);
        audioOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                main.mAudioService.speak(textOutput.getText().toString());
            }
        });
    }
}