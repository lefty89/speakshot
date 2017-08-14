package com.hsfl.speakshot.service.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.media.MediaActionSound;
import com.hsfl.speakshot.service.camera.helper.ManualFocusHelper;
import com.hsfl.speakshot.service.camera.helper.MediaSoundHelper;
import com.hsfl.speakshot.service.camera.ocr.OcrHandler;

import android.content.Context;
import android.hardware.Camera;
import android.os.*;
import android.util.Log;
import android.view.WindowManager;
import com.google.android.gms.common.images.Size;
import android.view.SurfaceHolder;
import com.hsfl.speakshot.service.camera.ocr.serialization.ImageConfigParcel;

import java.io.*;
import java.util.*;


public class CameraService extends Observable {
    private static final String TAG = CameraService.class.getSimpleName();

    /**
     * The CameraService singleton
     */
    private static CameraService instance = null;

    /**
     * Contains the OCR functionality
     */
    private OcrHandler mOcrHandler;

    /**
     * Wrapper that provides sound samples for camera related actions
     */
    private MediaSoundHelper mMediaSoundHelper;

    /**
     * Contains the helper for focusing
     */
    private ManualFocusHelper mManualFocusHelper;

    /**
     * Camera Lock
     */
    private Object mCameraLock = new Object();

    /**
     * The camera orientation
     */
    private int mCameraOrientation = 0;

    /**
     * the target camera dimensions
     */
    private final int mRequestedPreviewWidth   = 1920;
    private final int mRequestedPreviewHeight  = 1080;

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
     * Empty Constructor
     */
    CameraService() {}

    /**
     * Gets the CameraService instance
     * @return
     */
    public static CameraService getInstance() {
        if (instance == null) {
            instance = new CameraService();
        }
        return instance;
    }

    /**
     * Snaps a new image and analyze it immediately
     */
    public void analyzePicture() {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        int width  = camera.getParameters().getPreviewSize().width;
                        int height = camera.getParameters().getPreviewSize().height;
                        int format = camera.getParameters().getPreviewFormat();

                        // image configuration
                        ImageConfigParcel config = new ImageConfigParcel(width, height, mCameraOrientation, format);

                        // starts the ocr for this image
                        mOcrHandler.ocrRawImage(data, config);
                        // restarts the background preview
                        mCamera.startPreview();
                    }
                };
                mCamera.setOneShotPreviewCallback(previewCallback);
                mMediaSoundHelper.play(MediaActionSound.SHUTTER_CLICK);
            }
        }
    }

    /**
     * Creates a bitmap out of a given file and analyze it
     */
    public void analyzePicture(File file) {

        final List<String> acceptedExts = Arrays.asList("jpg","jpeg","png");
        final String fileExt = file.getName().substring(file.getName().lastIndexOf(".")+1).toLowerCase();

        if ((acceptedExts.contains(fileExt)) && (file.exists())) {
            // gets a drawable to draw into the image view
            Bitmap b = BitmapFactory.decodeFile(file.getAbsolutePath());

            // image configuration
            ImageConfigParcel config = new ImageConfigParcel(b.getWidth(), b.getHeight(), 0, ImageFormat.JPEG);
            mOcrHandler.ocrBitmapImage(b, config);
        }
    }

    /**
     * Starts analyzing the images from the preview surface
     */
    public void startStream() {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                mOcrHandler.startOcrDetector(mCameraOrientation);
            }
        }
    }

    /**
     * Stops analyzing the images from the preview surface
     */
    public void stopStream() {
        synchronized (mCameraLock) {
            mOcrHandler.stopOcrDetector();
        }
    }

    /**
     * Plays a camera sound
     */
    public void play(int action) {
        if (mMediaSoundHelper != null) {
            mMediaSoundHelper.play(action);
        }
    }

    /**
     * Setting up the camera and registers the ocr processor
     */
    public void init(Context context) {
        if (mCamera == null) {
            // checks whether a suitable camera exists
            int requestedCameraId = getIdForRequestedCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            if (requestedCameraId == -1) {
                throw new RuntimeException("Could not find requested camera.");
            }

            // opens the selected camera
            if (safeCameraOpen(requestedCameraId)) {
                // loads info for the selected camera
                mCameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, mCameraInfo);

                // get Camera parameters
                Camera.Parameters params = mCamera.getParameters();

                // sets image format
                params.setPreviewFormat(ImageFormat.NV21);

                // updates the orientation
                mCameraOrientation = calculateDisplayOrientation(context);
                mCamera.setDisplayOrientation(mCameraOrientation);

                // updates the camera parameter
                mCamera.setParameters(params);

                // initial focus mode
                setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                // init the sound player
                mMediaSoundHelper = new MediaSoundHelper();

                // initializes the ocr engine
                mOcrHandler = new OcrHandler(context, mCamera, new Handler() {
                    public void handleMessage(Message msg) {
                        setChanged();
                        notifyObservers(msg.getData());
                        super.handleMessage(msg);
                    }
                });
            }
            else {
                throw new RuntimeException("Could not open camera.");
            }
        }
    }

    /**
     * Stops previewing and releases the camera object
     */
    public void release() {
        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            stopPreview();

            // releases the ocr detector
            mOcrHandler.releaseOcrDetector();

            // clears resources for the sound player
            mMediaSoundHelper.release();

            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();

            mCamera = null;
        }
    }

    /**
     * Sets the focus mode
     * @param foc
     */
    public void setFocusMode(String foc) {
        if (mCamera != null) {
            // get Camera parameters
            Camera.Parameters params = mCamera.getParameters();
            // sets the focus mode
            if (params.getSupportedFocusModes().contains(foc)) {
                params.setFocusMode(foc);
                if (foc == Camera.Parameters.FOCUS_MODE_AUTO) {
                    mManualFocusHelper = new ManualFocusHelper(mCamera);
                }
            }
            // updates the camera parameter
            mCamera.setParameters(params);
        }
    }

    /**
     * Starts updating the preview surface
     */
    public void focus(float x, float y) {
        synchronized (mCameraLock) {
            if (mManualFocusHelper != null) {
                mManualFocusHelper.focus(x, y);
            }
        }
    }

    /**
     * Starts updating the preview surface
     */
    public void startPreview(int width, int height, SurfaceHolder holder) {
        synchronized (mCameraLock) {
            try {
                if (mCamera != null) {
                    // sets the preview display
                    mCamera.setPreviewDisplay(holder);

                    SizePair sizePair = selectSizePair(mCamera, mRequestedPreviewWidth, mRequestedPreviewHeight);
                    if (sizePair == null) {
                        throw new RuntimeException("Could not find suitable preview size.");
                    }
                    // get Camera parameters
                    Camera.Parameters params = mCamera.getParameters();

                    // sets the size
                    params.setPreviewSize(sizePair.previewSize().getWidth(), sizePair.previewSize().getHeight());

                    // updates the camera parameter
                    mCamera.setParameters(params);

                    // updates the focus helper with the new with and height
                    if (mManualFocusHelper != null) {
                        mManualFocusHelper.setPreviewSize(width, height);
                    }
                    mCamera.startPreview();
                }
            } catch (IOException exception) {
                Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
            }
        }
    }

    /**
     * Stops updating the preview surface
     */
    public void stopPreview() {
        synchronized (mCameraLock) {
            try {
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.setPreviewDisplay(null);
                }
            } catch (IOException exception) {
                Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
            }
        }
    }

    /**
     * Sets the camera flashlight
     */
    public void setFlashLightEnabled(boolean b) {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            String lightState = (b) ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF;
            params.setFlashMode(lightState);
            mCamera.setParameters(params);
        }
    }

    /**
     * Checks whether the flashlight is currently enabled
     */
    public boolean isFlashLightEnabled() {
        boolean b = false;
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            String fm = params.getFlashMode();
            if (fm != null) {
                b = (fm.equals(Camera.Parameters.FLASH_MODE_TORCH));
            }
        }
        return b;
    }

    /**
     * Gets the PreviewSize
     * @return
     */
    public Camera.Size getCameraPreviewSize() {
        return (mCamera != null) ? mCamera.getParameters().getPreviewSize() : null;
    }

    /**
     * Gets the display orientation
     * @return
     */
    public int getDisplayOrientation() {
        return mCameraOrientation;
    }

    /**
     * Gets the display orientation
     */
    private int calculateDisplayOrientation(Context context) {
        int ori = 0;
        if (mCameraInfo != null) {
            WindowManager winManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            int rotation = winManager.getDefaultDisplay().getRotation();
            ori = (mCameraInfo.orientation - (rotation * 90) +360) % 360;
        }
        return ori;
    }

    /**
     * Safely opens the camera object
     * @param id
     * @return
     */
    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            release();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            //Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    /**
     * Gets the id for the requested camera if existing
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
                    Log.d(TAG, "Camera PreviewSize: " + pictureSize.width + " x " + pictureSize.height);
                    Log.d(TAG, "Camera PictureSize: " + pictureSize.width + " x " + pictureSize.height);
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
     * Selects the most suitable preview and picture size, given the desired width and height.
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
}
