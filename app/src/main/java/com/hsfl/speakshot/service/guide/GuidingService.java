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
     * Texts that will be outputted by the AudioService
     */
    private static String   GUIDING_TEXT;
    private static String[] DIRECTION_TEXTS;
    private static String[] ROTATION_TEXTS;

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

        // init descriptions
        GUIDING_TEXT  = activity.getString(activity.getResources().getIdentifier("guiding_command", "string", activity.getPackageName()));
        DIRECTION_TEXTS = activity.getResources().getStringArray(activity.getResources().getIdentifier("guiding_direction", "array", activity.getPackageName()));
        ROTATION_TEXTS = activity.getResources().getStringArray(activity.getResources().getIdentifier("guiding_rotation", "array", activity.getPackageName()));

        // gets the camera service
        mCameraService = CameraService.getInstance();

        // inits the orientation sensor
        mSensorManager = (SensorManager)activity.getSystemService(android.content.Context.SENSOR_SERVICE);
        if (OrientationProvider.checkHardwarde(mSensorManager)) {
            mOrientationProvider = new OrientationProvider(mSensorManager);
		}
    }

    /**
     * Starts the guiding process
     */
    public void start() {
        mCameraService.addObserver(this);
        // starts the orientation provider
        if (mOrientationProvider != null)
            mOrientationProvider.start();
    }

    /**
     * Stops the guiding process
     */
    public void stop() {
        mCameraService.deleteObserver(this);
        // stops the orientation provider
        if (mOrientationProvider != null)
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
                        GUIDING_TEXT, ((dHits == 0) && (rHits == 0)) ?
                        "Ok" : DIRECTION_TEXTS[dHits] + " " + ROTATION_TEXTS[rHits]
                    ));
                }
            }
        }
    }
}
