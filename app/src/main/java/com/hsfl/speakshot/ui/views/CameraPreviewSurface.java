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
package com.hsfl.speakshot.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.*;
import com.hsfl.speakshot.service.camera.CameraService;

public class CameraPreviewSurface extends ViewGroup implements SurfaceHolder.Callback {
    private static final String TAG = CameraPreviewSurface.class.getSimpleName();

    /**
     * Indicates an available surface to draw into
     */
    private boolean mSurfaceAvailable;

    /**
     * Indicates a required update
     */
    private boolean mRequireUpdate;

    /**
     * Contains the surface where the camera preview is drawn into
     */
    private SurfaceView mSurfaceView;

    /**
     * Service provider that handles the camera object
     */
    private CameraService mCameraService;

    /**
     * Constructor
     * @param context
     * @param attrs
     */
    public CameraPreviewSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCameraService = CameraService.getInstance();

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
    }

    /**
     * Pauses the preview
     */
    public void pause() {
        mRequireUpdate = true;
        mCameraService.stopStream();
        mCameraService.stopPreview();
        mCameraService.release();
    }

    /**
     * Resumes the preview
     */
    public void update() {
        int w = getWidth();
        int h = getHeight();

        if ((w>0) && (h>0)) {
            // if orientation was changed w and h are 0
            // if 'unpause' is triggered then they contains the real values
            update(w, h);
        }
    }

    /**
     * Resumes the preview
     */
    public void update(int width, int height) {
        // updates the cameras surface information
        if ((mCameraService != null) && (mSurfaceAvailable) && (mRequireUpdate)) {
            // inform the camera service that the dimensions of the underling surface has changed
            mCameraService.init(getContext()); // !!!
            mCameraService.startPreview(width, height, mSurfaceView.getHolder());
            mCameraService.startStream();
            mRequireUpdate = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;
        final int height = b - t;
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);
            child.layout(0, 0, width, height);

        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceAvailable = true;
        update();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mRequireUpdate = true;
        update(w, h);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCameraService != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCameraService.stopPreview();
        }
        mSurfaceAvailable = false;
    }
}
