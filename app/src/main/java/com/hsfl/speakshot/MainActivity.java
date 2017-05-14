package com.hsfl.speakshot;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.hsfl.speakshot.service.CameraService;
import com.hsfl.speakshot.ui.CameraSourcePreview;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Service provider that handles the camera object
     */
    private CameraService mCameraService;

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
            Context context = getApplicationContext();
            // Creates and starts the camera.  Note that this uses a higher resolution in comparison
            // to other detection examples to enable the text recognizer to detect small pieces of text.
            mCameraService = new CameraService.Builder(context)
                    .setFacing(android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK).build();
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
}
