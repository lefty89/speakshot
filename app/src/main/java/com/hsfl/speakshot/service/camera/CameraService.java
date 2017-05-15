package com.hsfl.speakshot.service.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.*;
import java.util.List;

public class CameraService {
    private static final String TAG = CameraService.class.getSimpleName();

    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private Context mContext;

    /**
     * Camera facing, chooses the back camera as default
     */
    private int mFacing = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;

    /**
     * Empty constructor
     */
    CameraService() {}

    /**
     * Sets the surface where the camera output shall drawn into
     * @param holder
     * @throws IOException
     */
    public void setPreviewDisplay(SurfaceHolder holder) throws IOException {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);

            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    /**
     * Starts updating the preview surface
     */
    public void initCamera() {

        if (mCamera == null) {
            createCamera();

            // loads info for the selected camera
            mCameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mFacing, mCameraInfo);

            // get Camera parameters
            Camera.Parameters params = mCamera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                // set Camera parameters
                mCamera.setParameters(params);
            }
        }
    }

    /**
     * Starts updating the preview surface
     */
    public void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    /**
     * Stops updating the preview surface
     */
    public void releaseCamera() {

        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.stopPreview();

            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();

            mCamera = null;
        }
    }

    /**
     * Sets the display orientation (automatically adds the camera default rotation)
     * @param orientation
     */
    public void setDisplayOrientation(int orientation) {
        if (mCamera != null) {
            int ori = (orientation + mCameraInfo.orientation) % 360;
            if (ori < 0) {
                ori = 360 + ori;
            }
            mCamera.setDisplayOrientation(ori);
        }
    }

    /**
     * Selects an optimal camera size based on the available options an the
     * dimensions of the surface
     * @param w
     * @param h
     */
    public void setOptimalPreviewSize(int w, int h) {
        if (mCamera != null) {
            List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();;

            final double ASPECT_TOLERANCE = 0.0;
            double targetRatio = (double) w / h;
            if (sizes == null) return;

            Camera.Size optimalSize = null;
            double minDiff = Double.MAX_VALUE;

            // Try to find an size match aspect ratio and size
            for (Camera.Size size : sizes) {
                double ratio = (double) size.width / size.height;
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }

            // Cannot find the one match the aspect ratio, ignore the requirement
            if (optimalSize == null) {
                minDiff = Double.MAX_VALUE;
                for (Camera.Size size : sizes) {
                    if (Math.abs(size.height - h) < minDiff) {
                        optimalSize = size;
                        minDiff = Math.abs(size.height - h);
                    }
                }
            }

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            mCamera.setParameters(parameters);
        }
    }

    /**
     * Takes a picture
     */
    public void takePicture() {

        if (mCamera != null) {
            Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
                public void onShutter() {}
            };
            Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
                public void onPictureTaken(byte[] data, Camera camera) {}
            };
            Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
                public void onPictureTaken(byte[] data, Camera camera) {
                    new AsyncImageSaver(mCameraInfo.orientation).execute(data);
                    startPreview();
                }
            };
            mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
        }
    }

    /**
     * safeCameraOpen
     * @param id
     * @return
     */
    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            releaseCamera();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            //Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    /**
     * getIdForRequestedCamera
     * @param facing
     * @return
     */
    private int getIdForRequestedCamera(int facing) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing) {
                return i;
            }
        }
        return -1;
    }

    /**
     * createCamera
     */
    private void createCamera() {

        int requestedCameraId = getIdForRequestedCamera(mFacing);
        if (requestedCameraId == -1) {
            throw new RuntimeException("Could not find requested camera.");
        }
        safeCameraOpen(requestedCameraId);
    }

    /**
     * CameraBuilder
     */
    public static class Builder {
        private CameraService mCameraService = new CameraService();

        /**
         * Creates a camera source builder with the supplied context and detector.  Camera preview
         * images will be streamed to the associated detector upon starting the camera source.
         */
        public Builder(Context context) {
            mCameraService.mContext = context;
        }

        /**
         * Sets the camera to use (either {@link #CAMERA_FACING_BACK} or
         * {@link #CAMERA_FACING_FRONT}). Default: back facing.
         */
        public Builder setFacing(int facing) {
            if ((facing != android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) && (facing != android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT)) {
                throw new IllegalArgumentException("Invalid camera: " + facing);
            }
            mCameraService.mFacing = facing;
            return this;
        }

        /**
         * Creates an instance of the camera service.
         */
        public CameraService build() {
            return mCameraService;
        }
    }
}
