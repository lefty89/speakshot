package com.hsfl.speakshot.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.OcrDetectorProcessor;
import com.hsfl.speakshot.R;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.nio.ByteBuffer;


public class ControlView extends LinearLayout {
    private static final String TAG = ControlView.class.getSimpleName();

    TextRecognizer textRecognizer;

    public ControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.control_view, this);

        final Button picButton = (Button)findViewById(R.id.btn_picture);
        picButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                //Toast.makeText(getContext(), "MOIN", Toast.LENGTH_LONG).show();
                takePic();
            }
        });

        final Button ocrButton = (Button)findViewById(R.id.btn_ocr);
        ocrButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                //Toast.makeText(getContext(), "MOIN", Toast.LENGTH_LONG).show();
                startOCR();
            }
        });

        final Button vibButton = (Button)findViewById(R.id.btn_vibrate);
        vibButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // vibration for 800 milliseconds
                ((Vibrator)getContext().getSystemService(android.content.Context.VIBRATOR_SERVICE)).vibrate(800);
            }
        });

        textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(getContext()));

    }

    public void takePic() {
        MainActivity main = (MainActivity)getContext();
        main.mCameraService.takePicture();
    }

    public void startOCR() {
        Log.d(TAG, "Start OCR");
        Context context = getContext();



        // A text recognizer is created to find text.  An associated multi-processor instance
        // is set to receive the text recognition results, track the text, and maintain
        // graphics for each text block on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each text block.

        if (true) {


            File sd = Environment.getExternalStorageDirectory();
            String photoPath = sd.getAbsolutePath() + "/camtest/img.jpg";
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);


            Frame.Builder fb = new Frame.Builder().setBitmap(bitmap);

            // use detector class
            // textRecognizer.receiveFrame(fb.build());

            // DONT use detector class
            SparseArray<TextBlock> blocks = textRecognizer.detect(fb.build());
            Toast.makeText(context, "blocks: " + blocks.size(), Toast.LENGTH_LONG).show();


            for (int i = 0; i < blocks.size(); ++i) {
                TextBlock item = blocks.valueAt(i);
                if (item != null && item.getValue() != null) {
                    Log.d("OcrDetectorProcessor", "Text detected! " + item.getValue());
                    Toast.makeText(getContext(), item.getValue(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "nothing found", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}