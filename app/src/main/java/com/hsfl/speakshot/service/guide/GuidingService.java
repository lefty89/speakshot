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
import com.hsfl.speakshot.ui.views.GuidingLayout;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class GuidingService implements Observer {
    private static final String TAG = GuidingService.class.getSimpleName();

    /**
     * Direction texts
     */
    private static String[] mDirectionTexts = {
            "",                             // 0000
            "weiter links bewegen",         // 0001 : left
            "weiter oben bewegen",          // 0010 : top
            "weiter oben-links bewegen",    // 0011 : left, top
            "weiter rechts bewegen",        // 0100 : right
            "weiter zurück bewegen",        // 0101 : right, left
            "weiter oben-rechts bewegen",   // 0110 : right, top
            "weiter zurück bewegen",        // 0111 : right, left, top
            "weiter unten bewegen",         // 1000 : bot
            "weiter unten-links bewegen",   // 1001 : bot, left
            "weiter zurück bewegen",        // 1010 : bot, top
            "weiter zurück bewegen",        // 1011 : bot, left, top
            "weiter unten-rechts bewegen",  // 1100 : bot, right
            "weiter zurück bewegen",        // 1101 : bot, right, left
            "weiter zurück bewegen"};       // 1111 : bot, left, top, right

    /**
     * Orientation texts
     */
    private static String[] mRotationTexts = {
            "",                                         // 0000
            "weiter vor neigen",                        // 0001 : to
            "weiter zurück neigen",                     // 0010 : back
            "weiter zurück-vorn neigen",                // 0011 : to, back              => never
            "weiter rechts neigen",                     // 0100 : right
            "weiter rechts-vor neigen",                 // 0101 : right, to
            "weiter rechts-zurück neigen",              // 0110 : right, back
            "weiter zurück-vorn neigen",                // 0111 : right, to, back       => never
            "weiter links neigen",                      // 1000 : left
            "weiter links-vor neigen",                  // 1001 : left, to
            "weiter links-zurück neigen",               // 1010 : left, back
            "weiter links-zurück-vorn neigen",          // 1011 : left, to, back        => never
            "weiter links-rechts neigen",               // 1100 : left, right           => never
            "weiter links-rechts-vorn neigen",          // 1101 : left, right, to       => never
            "weiter links-rechts-zurück-vorn neigen"};  // 1111 : left, to, back, right => never

    /**
     * The GuidingService singleton
     */
    private static GuidingService instance = null;

    /**
     * Offset from border
     */
    private final int THRESHOLD = 80;

    /**
     * Offset from border
     */
    private final int ROTATION_THRESHOLD = 20;

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
    private OrientationProvider mOrientationProvider;

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
        mSensorManager = (SensorManager)activity.getSystemService(android.content.Context.SENSOR_SERVICE);
        if (mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).size() > 0) {
            mOrientationProvider = new OrientationProvider(mSensorManager);
		}
    }

    /**
     * Starts the guiding process
     */
    public void start() {
        mCameraService.addObserver(this);
        // starts the orientation provider
        mOrientationProvider.start();
    }

    /**
     * Stops the guiding process
     */
    public void stop() {
        mCameraService.deleteObserver(this);
        // stops the orientation provider
        mOrientationProvider.stop();
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

                    // directional adjustments
                    int dHits = 0;
                    if (left)  { dHits |= 1; mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_LEFT); }
                    if (top)   { dHits |= 2; mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_TOP); }
                    if (right) { dHits |= 4; mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_RIGHT); }
                    if (bot)   { dHits |= 8; mGuidingLayout.playAnimationFor(GuidingLayout.BORDER_BOTTOM); }

                    // rotational adjustments
                    int rHits = (mOrientationProvider != null) ? mOrientationProvider.getRotationHits(ROTATION_THRESHOLD) : 0;

                    // acoustic output
                    AudioService.getInstance().speak(String.format(
                        "Ausrichtung: %s", ((dHits == 0) && (rHits == 0)) ?
                            "Ok" : mDirectionTexts[dHits] + " " + mRotationTexts[rHits]
                    ));
                }
            }
        }
    }
}
