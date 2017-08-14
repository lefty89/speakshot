package com.hsfl.speakshot.service.guide;

import android.app.Activity;
import android.hardware.*;
import android.os.Bundle;
import android.view.ViewGroup;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.CameraService;
import com.hsfl.speakshot.service.camera.ocr.processor.LocateTextProcessor;
import com.hsfl.speakshot.service.camera.ocr.processor.ProcessorChain;
import com.hsfl.speakshot.service.guide.orientation.*;
import com.hsfl.speakshot.ui.views.GuidingLayout;

import java.util.Observable;
import java.util.Observer;

import static android.content.Context.SENSOR_SERVICE;

public class GuidingService implements Observer{
    private static final String TAG = GuidingService.class.getSimpleName();

    /**
     * Orientation texts
     */
    private static String[] mOrientationTexts = {
            "OK",                   // 0000
            "weiter links",         // 0001 : left
            "weiter oben",          // 0010 : top
            "weiter oben-links",    // 0011 : left, top
            "weiter rechts",        // 0100 : right
            "weiter zurück",        // 0101 : right, left
            "weiter oben-rechts",   // 0110 : right, top
            "weiter zurück",        // 0111 : right, left, top
            "weiter unten",         // 1000 : bot
            "weiter unten-links",   // 1001 : bot, left
            "weiter zurück",        // 1010 : bot, top
            "weiter zurück",        // 1011 : bot, left, top
            "weiter unten-rechts",  // 1100 : bot, right
            "weiter zurück",        // 1101 : bot, right, left
            "weiter zurück"};       // 1111 : bot, left, top, right

    /**
     * The GuidingService singleton
     */
    private static GuidingService instance = null;

    /**
     * The time between guides in milliseconds
     */
    private final int GUIDE_DELAY = 3000;

    /**
     * The layout for the border marks
     */
    private GuidingLayout mGuidingLayout = null;

    /**
     * Camera service
     */
    private CameraService mCameraService = null;

    /**
     * Flag that indicates whether audio is enabled
     */
    private boolean mAudioEnabled = true;

    /**
     * SensorManager
     */
    private SensorManager mSensorManager = null;

    /**
     * The current orientation provider that delivers device orientation.
     */
    private OrientationProvider currentOrientationProvider;

    /**
     * Empty Constructor
     */
    GuidingService() {}

    /**
     * Gets the GuidingService instance
     * @return
     */
    public static GuidingService getInstance() {
        if (instance == null) {
            instance = new GuidingService();
        }
        return instance;
    }

    /**
     * Setting up the GuidingService
     */
    public void init(Activity activity) {
        // initializes the view
        if (mGuidingLayout == null) {
            mGuidingLayout = new GuidingLayout(activity);
            activity.addContentView(mGuidingLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        // gets the camera service
        mCameraService = CameraService.getInstance();

        // inits the orientation sensor
        mSensorManager = (SensorManager)activity.getSystemService(SENSOR_SERVICE);
        if (mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).size() > 0) {
            currentOrientationProvider = new CalibratedGyroscopeProvider(mSensorManager);
		}
    }

    /**
     * Starts the guiding process
     */
    public void start() {
        mCameraService.addObserver(this);

        Camera.Size size = mCameraService.getCameraPreviewSize();
        int orientation  = mCameraService.getDisplayOrientation();

        if (size != null) {
            // if camera is rotated around 90/270 degrees then switch width and height
            int w = ((orientation == 90) || (orientation == 270)) ? size.height : size.width;
            int h = ((orientation == 90) || (orientation == 270)) ? size.width  : size.height;

            // creates the container
            ProcessorChain pc = new ProcessorChain();
            pc.add(new LocateTextProcessor(w, h, GUIDE_DELAY));

            mCameraService.startAnalyseStream(pc);
        }

        // starts the orientation provider
        if (currentOrientationProvider != null) {
            currentOrientationProvider.start();
        }
    }

    /**
     * Stops the guiding process
     */
    public void stop() {

        // stops the orientation provider
        if (currentOrientationProvider != null) {
            currentOrientationProvider.stop();
        }
        // stops the stream analyzer analyzer
        mCameraService.stopAnalyseStream();
        // deletes the observer
        mCameraService.deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        // gets the detected texts
        int hits = ((Bundle)arg).getInt(LocateTextProcessor.RESULT_BORDER_HITS);
        if (hits != 0) {

            // visual output
            if (((hits & 1) >> 0) == 1) mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_LEFT);
            if (((hits & 2) >> 1) == 1) mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_TOP);
            if (((hits & 4) >> 2) == 1) mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_RIGHT);
            if (((hits & 8) >> 3) == 1) mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_BOTTOM);

            // acoustic output
            if (mAudioEnabled) {
                AudioService.getInstance().speak(mOrientationTexts[hits]);
            }

        }
    }
}
