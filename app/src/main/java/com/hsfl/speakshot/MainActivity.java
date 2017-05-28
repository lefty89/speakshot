package com.hsfl.speakshot;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ToggleButton;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.CameraService;
import com.hsfl.speakshot.ui.CameraSourcePreview;
import com.hsfl.speakshot.ui.ReadFragment;
import com.hsfl.speakshot.ui.SearchFragment;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * main app modes
     */
    private final boolean MODE_SEARCH = false;
    private final boolean MODE_READ = true;

    /**
     * the app theme
     */
    public static  int THEME = R.style.AppTheme;

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

        // sets theme and app settings
        setTheme(THEME);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // background camera view
        mPreview = (CameraSourcePreview)findViewById(R.id.camera_view);
        // request camera permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            final Context context = getApplicationContext();
            // initializes the camera service
            mCameraService = new CameraService.Builder(context).setFacing(android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK).build();
            // initializes the audio service
            mAudioService = new AudioService.Builder(context).setSpeechRate(0).setPitch(0).setLocale(Locale.GERMAN).build();
        }

        // switch button to change the mode
        final ToggleButton modeSwitch = (ToggleButton)findViewById(R.id.btn_mode_select);
        modeSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setAppMode(modeSwitch.isChecked());
            }
        });
        setAppMode(MODE_SEARCH);
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

    /**
     * switches the app between read and search mode
     * @param b either MODE_READ or MODE_SEARCH
     */
    private void setAppMode(boolean b) {

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        // gets the fragment
        Fragment fragment = (b) ? new ReadFragment() : new SearchFragment();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(null);

        // Commit the transaction
        ft.commit();
    }
}
