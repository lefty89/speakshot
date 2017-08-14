package com.hsfl.speakshot.service.guide;

import android.app.Activity;
import android.graphics.Rect;
import android.hardware.*;
import android.os.Bundle;
import android.view.ViewGroup;
import com.hsfl.speakshot.service.audio.AudioService;
import com.hsfl.speakshot.service.camera.CameraService;
import com.hsfl.speakshot.service.camera.ocr.OcrHandler;
import com.hsfl.speakshot.service.camera.ocr.serialization.TextBlockParcel;
import com.hsfl.speakshot.service.guide.orientation.*;
import com.hsfl.speakshot.ui.views.GuidingLayout;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import static android.content.Context.SENSOR_SERVICE;

public class GuidingService implements Observer {
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
     * Offset from border
     */
    private final int THRESHOLD = 80;

    /**
     * The time between guides in milliseconds
     */
    private final int GUIDE_DELAY = 3000;

    /**
     * Helper var
     */
    private long mLastCheck = System.currentTimeMillis();

    /**
     * Lock object
     */
    private final Object mLock = new Object();

    /**
     * The layout for the border marks
     */
    private GuidingLayout mGuidingLayout = null;

    /**
     * Camera service
     */
    private CameraService mCameraService = null;

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
        // deletes the observer
        mCameraService.deleteObserver(this);
    }

    @Override

    public void update(Observable o, Object arg) {
        // gets the detected texts
        String type = ((Bundle)arg).getString(OcrHandler.BUNDLE_TYPE);
        if ((type != null) && (type.equals(OcrHandler.DETECTOR_ACTION_STREAM))) {
            ArrayList<TextBlockParcel> detections = ((Bundle) arg).getParcelableArrayList(OcrHandler.BUNDLE_DETECTIONS);

            // get orientation and size
            Camera.Size size = mCameraService.getCameraPreviewSize();
            int orientation  = mCameraService.getDisplayOrientation();

            // if camera is rotated around 90/270 degrees then switch width and height
            int w = ((orientation == 90) || (orientation == 270)) ? size.height : size.width;
            int h = ((orientation == 90) || (orientation == 270)) ? size.width  : size.height;

            // critical part
            synchronized (mLock) {
                if (mLastCheck+GUIDE_DELAY <= System.currentTimeMillis()) {
                    mLastCheck = System.currentTimeMillis();

                    // temp vars
                    boolean left = false, right = false, bot = false, top = false;

                    // bounding area rectangle
                    Rect r1 = new Rect(THRESHOLD, THRESHOLD, w-THRESHOLD, h-THRESHOLD);

                    // loop through all blocks
                    for (int i=0; i<detections.size(); i++) {
                        if (detections.get(i) != null) {
                            // text rectangle
                            Rect r2 = detections.get(i).getBoundingBox();
                            // calculate the intersection
                            left  = (Math.max(r1.left, r2.left)     == r1.left)   || left;
                            top   = (Math.max(r1.top, r2.top)       == r1.top)    || top;
                            right = (Math.min(r1.right, r2.right)   == r1.right)  || right;
                            bot   = (Math.min(r1.bottom, r2.bottom) == r1.bottom) || bot;
                        }
                    }

                    // build up hits
                    int hits =  (left) ?1:0;
                    hits     |= (top)  ?2:0;
                    hits     |= (right)?4:0;
                    hits     |= (bot)  ?8:0;

                    // visual output
                    if (((hits & 1) >> 0) == 1) mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_LEFT);
                    if (((hits & 2) >> 1) == 1) mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_TOP);
                    if (((hits & 4) >> 2) == 1) mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_RIGHT);
                    if (((hits & 8) >> 3) == 1) mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_BOTTOM);

                    // acoustic output
                    AudioService.getInstance().speak(mOrientationTexts[hits]);
                }
            }
        }
    }
}
