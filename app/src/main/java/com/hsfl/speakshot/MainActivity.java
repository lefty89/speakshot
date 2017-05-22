package com.hsfl.speakshot;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;
import com.google.android.gms.vision.text.TextBlock;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.CameraService;
import com.hsfl.speakshot.ui.CameraSourcePreview;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity  implements Observer {
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Service provider that handles the camera object
     */
    public CameraService mCameraService;

    /**
     * Service provider that handles the ocr handlig
     */
    public AudioService mAudioService;

    /**
     * Target surface to draw the camera view into
     */
    private CameraSourcePreview mPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // hides the title bar
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mPreview = (CameraSourcePreview)findViewById(R.id.camera_view);
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            final Context context = getApplicationContext();
            // Creates and starts the camera.  Note that this uses a higher resolution in comparison
            // to other detection examples to enable the text recognizer to detect small pieces of text.
            mCameraService = new CameraService.Builder(context)
                    .setFacing(android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK).build();
            // adds observer
            mCameraService.addObserver(this);

            // audio service
            mAudioService = new AudioService.Builder(context)
                    .setSpeechRate(6)
                    .setPitch(5)
                    .setLocale(Locale.GERMAN)
                    .build();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPreview.setCamera(mCameraService);
    }

    @Override
    protected void onPause() {
        mCameraService.releaseCamera();
        super.onPause();
    }

    @Override
    public void update(Observable o, Object arg) {
        Bundle b = (Bundle)arg;
        ArrayList<String> texts = b.getStringArrayList("texts");

        Toast.makeText(getApplicationContext(), "Items found: " + String.valueOf(texts.size()), Toast.LENGTH_SHORT).show();
        for (int i=0; i<texts.size(); i++) {
            Toast.makeText(getApplicationContext(), "Text " + i + ": " + texts.get(i), Toast.LENGTH_SHORT).show();
        }

    }
}
