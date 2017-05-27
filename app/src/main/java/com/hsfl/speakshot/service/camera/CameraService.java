package com.hsfl.speakshot.service.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.*;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import com.google.android.gms.common.images.Size;
import android.view.SurfaceHolder;
import com.hsfl.speakshot.service.camera.ocr.OcrHandler;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class CameraService extends Observable {
    private static final String TAG = CameraService.class.getSimpleName();

    private Context mContext;

    /**
     * the interface for the ocr
     */
    private OcrHandler mOcrHandler;

    private Object mCameraLock = new Object();

    /**
     * the target camera dimensions
     */
    private final int mRequestedPreviewWidth   = 1920;
    private final int mRequestedPreviewHeight  = 1080;
    private Size mPreviewSize;

    /**
     * image storage path
     */
    private static final String mImageName     = "img.jpg";
    private static final String mPathOnStorage = "/camtest"; // "/data/data/com.hsfl.speakshot"

    /**
     * the camera and info object
     */
    public Camera mCamera;
    private Camera.CameraInfo mCameraInfo;

    /**
     * If the absolute difference between a preview size aspect ratio and a picture size aspect
     * ratio is less than this tolerance, they are considered to be the same aspect ratio.
     */
    private static final float ASPECT_RATIO_TOLERANCE = 0.01f;

    /**
     * Camera facing, chooses the back camera as default
     */
    private int mFacing = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
    private String mFocusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;

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
     * Takes a picture
     */
    public void analyzePicture() {

        if (mCamera != null) {
            Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
                public void onShutter() {}
            };
            Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
                public void onPictureTaken(byte[] data, Camera camera) {}
            };
            Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
                public void onPictureTaken(byte[] data, Camera camera) {

                    // saves the image asynchronously to the external storage
                    new AsyncImageSaver(mContext, mCameraInfo.orientation, mPathOnStorage, mImageName).execute(data);

                    // starts the ocr for this image
                    mOcrHandler.ocrSingleImage(data);

                    // restarts the background preview
                    // startPreview();
                }
            };
            mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
        }
    }

    /**
     * Takes a picture
     */
    public void analyseImage() {
        if (mOcrHandler != null) {
            File sd = Environment.getExternalStorageDirectory();
            mOcrHandler.ocrSingleImage((sd + mPathOnStorage + "/" + mImageName));
        }
    }

    /**
     * Takes a picture
     */
    public void analyseStream() {
        if (mOcrHandler != null) {
            mOcrHandler.toggleOcrDetector();
        }
    }

    /**
     * Starts updating the preview surface
     */
    public void initCamera() {

        if (mCamera == null) {

            // checks whether a suitable camera exists
            int requestedCameraId = getIdForRequestedCamera(mFacing);
            if (requestedCameraId == -1) {
                throw new RuntimeException("Could not find requested camera.");
            }

            // opens the selected camera
            if (safeCameraOpen(requestedCameraId)) {

                SizePair sizePair = selectSizePair(mCamera, mRequestedPreviewWidth, mRequestedPreviewHeight);
                if (sizePair == null) {
                    throw new RuntimeException("Could not find suitable preview size.");
                }
                mPreviewSize = sizePair.previewSize();

                // loads info for the selected camera
                mCameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(mFacing, mCameraInfo);

                // get Camera parameters
                Camera.Parameters params = mCamera.getParameters();
                List<String> focusModes = params.getSupportedFocusModes();
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    // set the focus mode
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                // sets the size
                params.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                // sets autofocus
                if (mFocusMode != null) {
                    if (params.getSupportedFocusModes().contains(mFocusMode)) {
                        params.setFocusMode(mFocusMode);
                    } else {
                        Log.i(TAG, "Camera focus mode: " + mFocusMode + " is not supported on this device.");
                    }
                }

                // updates the camera parameter
                mCamera.setParameters(params);

                // initializes the ocr engine
                mOcrHandler = new OcrHandler(mContext, mCamera, mPreviewSize, getDisplayOrientation(), new Handler() {
                    public void handleMessage(Message msg) {
                        if (msg.what == 1) {
                            setChanged();
                            notifyObservers(msg.getData());
                        }
                        super.handleMessage(msg);
                    }
                });
                mCamera.setPreviewCallbackWithBuffer(mOcrHandler);
            }
            else {
                throw new RuntimeException("Could not open camera.");
            }
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
     * Starts updating the preview surface
     */
    public void startPreview() {
        if (mCamera != null) {
            mCamera.setDisplayOrientation(getDisplayOrientation());
            mCamera.startPreview();
        }
    }

    /**
     * Starts updating the preview surface
     */
    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    /**
     * Sets the display orientation (automatically adds the camera default rotation)
     */
    private int getDisplayOrientation() {
        int ori = 0;
        if (mCamera != null) {
            WindowManager winManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            int rotation = winManager.getDefaultDisplay().getRotation();

            ori = (rotation + mCameraInfo.orientation) % 360;
            if (ori < 0) {
                ori = 360 + ori;
            }
        }
        return ori;
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
     * Selects the most suitable preview and picture size, given the desired width and height.
     * <p/>
     * Even though we may only need the preview size, it's necessary to find both the preview
     * size and the picture size of the camera together, because these need to have the same aspect
     * ratio.  On some hardware, if you would only set the preview size, you will get a distorted
     * image.
     *
     * @param camera        the camera to select a preview size from
     * @param desiredWidth  the desired width of the camera preview frames
     * @param desiredHeight the desired height of the camera preview frames
     * @return the selected preview and picture size pair
     */
    private static SizePair selectSizePair(Camera camera, int desiredWidth, int desiredHeight) {
        List<SizePair> validPreviewSizes = generateValidPreviewSizeList(camera);

        // The method for selecting the best size is to minimize the sum of the differences between
        // the desired values and the actual values for width and height.  This is certainly not the
        // only way to select the best size, but it provides a decent tradeoff between using the
        // closest aspect ratio vs. using the closest pixel area.
        SizePair selectedPair = null;
        int minDiff = Integer.MAX_VALUE;
        for (SizePair sizePair : validPreviewSizes) {
            Size size = sizePair.previewSize();
            int diff = Math.abs(size.getWidth() - desiredWidth) +
                    Math.abs(size.getHeight() - desiredHeight);
            if (diff < minDiff) {
                selectedPair = sizePair;
                minDiff = diff;
            }
        }

        return selectedPair;
    }

    /**
     * Stores a preview size and a corresponding same-aspect-ratio picture size.  To avoid distorted
     * preview images on some devices, the picture size must be set to a size that is the same
     * aspect ratio as the preview size or the preview may end up being distorted.  If the picture
     * size is null, then there is no picture size with the same aspect ratio as the preview size.
     */
    private static class SizePair {
        private Size mPreview;
        private Size mPicture;

        public SizePair(android.hardware.Camera.Size previewSize, android.hardware.Camera.Size pictureSize) {
            mPreview = new Size(previewSize.width, previewSize.height);
            if (pictureSize != null) {
                mPicture = new Size(pictureSize.width, pictureSize.height);
            }
        }

        public Size previewSize() {
            return mPreview;
        }

        @SuppressWarnings("unused")
        public Size pictureSize() {
            return mPicture;
        }
    }

    /**
     * Generates a list of acceptable preview sizes.  Preview sizes are not acceptable if there is
     * not a corresponding picture size of the same aspect ratio.  If there is a corresponding
     * picture size of the same aspect ratio, the picture size is paired up with the preview size.
     * <p/>
     * This is necessary because even if we don't use still pictures, the still picture size must be
     * set to a size that is the same aspect ratio as the preview size we choose.  Otherwise, the
     * preview images may be distorted on some devices.
     */
    private static List<SizePair> generateValidPreviewSizeList(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<android.hardware.Camera.Size> supportedPreviewSizes =
                parameters.getSupportedPreviewSizes();
        List<android.hardware.Camera.Size> supportedPictureSizes =
                parameters.getSupportedPictureSizes();
        List<SizePair> validPreviewSizes = new ArrayList<>();
        for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
            float previewAspectRatio = (float) previewSize.width / (float) previewSize.height;

            // By looping through the picture sizes in order, we favor the higher resolutions.
            // We choose the highest resolution in order to support taking the full resolution
            // picture later.
            for (android.hardware.Camera.Size pictureSize : supportedPictureSizes) {
                float pictureAspectRatio = (float) pictureSize.width / (float) pictureSize.height;
                if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                    validPreviewSizes.add(new SizePair(previewSize, pictureSize));
                    break;
                }
            }
        }

        // If there are no picture sizes with the same aspect ratio as any preview sizes, allow all
        // of the preview sizes and hope that the camera can handle it.  Probably unlikely, but we
        // still account for it.
        if (validPreviewSizes.size() == 0) {
            Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size");
            for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
                // The null picture size will let us know that we shouldn't set a picture size.
                validPreviewSizes.add(new SizePair(previewSize, null));
            }
        }

        return validPreviewSizes;
    }

    /**
     * Callback interface used to notify on completion of camera auto focus.
     */
    public interface AutoFocusCallback {
        /**
         * Called when the camera auto focus completes.  If the camera
         * does not support auto-focus and autoFocus is called,
         * onAutoFocus will be called immediately with a fake value of
         * <code>success</code> set to <code>true</code>.
         * <p/>
         * The auto-focus routine does not lock auto-exposure and auto-white
         * balance after it completes.
         *
         * @param success true if focus was successful, false if otherwise
         */
        void onAutoFocus(boolean success);
    }

    /**
     * Callback interface used to notify on auto focus start and stop.
     * <p/>
     * <p>This is only supported in continuous autofocus modes -- {@link
     * Camera.Parameters#FOCUS_MODE_CONTINUOUS_VIDEO} and {@link
     * Camera.Parameters#FOCUS_MODE_CONTINUOUS_PICTURE}. Applications can show
     * autofocus animation based on this.</p>
     */
    public interface AutoFocusMoveCallback {
        /**
         * Called when the camera auto focus starts or stops.
         *
         * @param start true if focus starts to move, false if focus stops to move
         */
        void onAutoFocusMoving(boolean start);
    }

    /**
     * Wraps the camera1 auto focus callback so that the deprecated API isn't exposed.
     */
    private class CameraAutoFocusCallback implements Camera.AutoFocusCallback {
        private AutoFocusCallback mDelegate;

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (mDelegate != null) {
                mDelegate.onAutoFocus(success);
            }
        }
    }

    /**
     * Wraps the camera1 auto focus move callback so that the deprecated API isn't exposed.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private class CameraAutoFocusMoveCallback implements Camera.AutoFocusMoveCallback {
        private AutoFocusMoveCallback mDelegate;

        @Override
        public void onAutoFocusMoving(boolean start, Camera camera) {
            if (mDelegate != null) {
                mDelegate.onAutoFocusMoving(start);
            }
        }
    }

    /**
     * Starts camera auto-focus and registers a callback function to run when
     * the camera is focused.  This method is only valid when preview is active
     * (between {@link #start()} or {@link #start(SurfaceHolder)} and before {@link #stop()}
     * or {@link #release()}).
     * <p/>
     * <p>Callers should check
     * {@link #getFocusMode()} to determine if
     * this method should be called. If the camera does not support auto-focus,
     * it is a no-op and {@link AutoFocusCallback#onAutoFocus(boolean)}
     * callback will be called immediately.
     * <p/>
     * <p>If the current flash mode is not
     * {@link Camera.Parameters#FLASH_MODE_OFF}, flash may be
     * fired during auto-focus, depending on the driver and camera hardware.<p>
     *
     * @param cb the callback to run
     * @see #cancelAutoFocus()
     */
    public void autoFocus(@Nullable AutoFocusCallback cb) {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                CameraAutoFocusCallback autoFocusCallback = null;
                if (cb != null) {
                    autoFocusCallback = new CameraAutoFocusCallback();
                    autoFocusCallback.mDelegate = cb;
                }
                mCamera.autoFocus(autoFocusCallback);
            }
        }
    }

    /**
     * Sets camera auto-focus move callback.
     *
     * @param cb the callback to run
     * @return {@code true} if the operation is supported (i.e. from Jelly Bean), {@code false}
     * otherwise
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public boolean setAutoFocusMoveCallback(@Nullable AutoFocusMoveCallback cb) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return false;
        }

        synchronized (mCameraLock) {
            if (mCamera != null) {
                CameraAutoFocusMoveCallback autoFocusMoveCallback = null;
                if (cb != null) {
                    autoFocusMoveCallback = new CameraAutoFocusMoveCallback();
                    autoFocusMoveCallback.mDelegate = cb;
                }
                mCamera.setAutoFocusMoveCallback(autoFocusMoveCallback);
            }
        }

        return true;
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
