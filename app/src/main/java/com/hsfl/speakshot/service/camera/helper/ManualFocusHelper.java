package com.hsfl.speakshot.service.camera.helper;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.Log;
import java.util.Arrays;

/**
 * A helper that can play camera related sound samples
 */
public class ManualFocusHelper implements Camera.AutoFocusCallback, Camera.AutoFocusMoveCallback {
    private static final String TAG = ManualFocusHelper.class.getSimpleName();

    /**
     * The camera object
     */
    private Camera mCamera;

    /**
     * The size if the preview surface
     */
    private int mWidth  = 0;
    private int mHeight = 0;

    /**
     * Constructor
     */
    public ManualFocusHelper(Camera camera) {
        mCamera = camera;
    }

    /**
     * Updates the size of the camera preview surface
     * @param width
     * @param height
     */
    public void setPreviewSize(int width, int height) {
        mWidth  = width;
        mHeight = height;
    }

    /**
     * Sets a new focus
     * @param x
     * @param y
     */
    public void focus(float x, float y) {
        if (mCamera != null) {
            mCamera.cancelAutoFocus();
            Rect focusRect = calculateTapArea(x, y, 1f);
            Rect meteringRect = calculateTapArea(x, y, 1.5f);

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusAreas(Arrays.asList(new Camera.Area(focusRect, 1000)));

            if (mCamera.getParameters().getMaxNumMeteringAreas() > 0) {
                parameters.setMeteringAreas(Arrays.asList(new Camera.Area(meteringRect, 1000)));
            }

            try {
                mCamera.setParameters(parameters);
            }
            catch (Exception e) {}
            mCamera.autoFocus(this);
        }
    }

    /**
     * Convert touch position x:y to {@link Camera.Area} position -1000:-1000 to 1000:1000.
     */
    private Rect calculateTapArea(float x, float y, float coefficient) {
        int focusAreaSize = 1;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, mWidth - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, mHeight - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        Matrix matrix = new Matrix();
        matrix.mapRect(rectF);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    /**
     * Clamp
     * @param x
     * @param min
     * @param max
     * @return
     */
    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        Log.d(TAG, "onAutoFocus");
    }

    @Override
    public void onAutoFocusMoving(boolean start, Camera camera) {
        Log.d(TAG, "onAutoFocusMoving");
    }
}
