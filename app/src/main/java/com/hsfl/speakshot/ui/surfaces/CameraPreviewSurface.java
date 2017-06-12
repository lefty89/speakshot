/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hsfl.speakshot.ui.surfaces;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import com.hsfl.speakshot.service.camera.CameraService;

import java.io.IOException;

public class CameraPreviewSurface extends ViewGroup implements SurfaceHolder.Callback {
    private static final String TAG = CameraPreviewSurface.class.getSimpleName();

    /**
     * Contains the surface where the camera preview is drawn into
     */
    private SurfaceHolder mHolder;

    /**
     * Service provider that handles the camera object
     */
    private CameraService mCameraService;

    public CameraPreviewSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCameraService = CameraService.getInstance();

        SurfaceView mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "onMeasure");
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "onLayout");

        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            child.layout(0, 0, width, height);

            if (mCameraService != null) {
                // inform the camera service that the dimensions of the underling surface
                // has changed
                mCameraService.onSurfaceDimensionChanged(width, height);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        try {
            if (mCameraService != null) {
                mCameraService.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mCameraService != null) {
            mCameraService.startPreview();
            requestLayout();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        // Surface will be destroyed when we return, so stop the preview.
        if (mCameraService != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCameraService.releaseCamera();
        }
    }
}