package com.hsfl.speakshot.service.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
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
     * The camera orientation
     */
    private int mDisplayOrientation = 0;

    /**
     * Contains the requested focus mode
     */
    private String FOCUS_MODE;

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
                        // starts the ocr for this image
                        mOcrHandler.ocrSingleImage(data);
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
     * Gets a bitmap out of a given file and analyze it
     */
    public void analyzePicture(File file) {

        final List<String> acceptedExts = Arrays.asList("jpg","jpeg","png");
        final String fileExt = file.getName().substring(file.getName().lastIndexOf(".")+1).toLowerCase();

        if ((acceptedExts.contains(fileExt)) && (file.exists())) {

            // gets a drawable to draw into the image view
            Bitmap b = BitmapFactory.decodeFile(file.getAbsolutePath());

            // rotate the image to the same orientation that the camera has
            Matrix matrix = new Matrix();
            matrix.postRotate(-mDisplayOrientation);
            Bitmap bmp = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);

            // gets all the pixel from the bitmap and put them into an int array
            int[] argb = new int[bmp.getWidth() * bmp.getHeight()];
            bmp.getPixels(argb, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

            // convert every pixel from rgb to yuv format
            byte[] data = new byte[bmp.getWidth()*bmp.getHeight()*3/2];
            encodeYUV420SP(data, argb, bmp.getWidth(), bmp.getHeight());

            mOcrHandler.ocrSingleImage(data);
        }
    }

    /**
     * Encodes a given bitmap into NV21 format
     *
     * @param yuv420sp
     * @param argb
     * @param width
     * @param height
     *
     * @see https://stackoverflow.com/questions/5960247/convert-bitmap-array-to-yuv-ycbcr-nv21
     */
    private void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index ++;
            }
        }
    }

    /**
     * Analyzes the images from the preview surface
     */
    public void analyseStream(String searchTerm) {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                if (!searchTerm.isEmpty()) {
                    mOcrHandler.startOcrDetector(searchTerm);
                    mMediaSoundHelper.play(MediaActionSound.START_VIDEO_RECORDING);
                } else {
                    mOcrHandler.stopOcrDetector();
                    mMediaSoundHelper.play(MediaActionSound.STOP_VIDEO_RECORDING);
                }
            }
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
                mDisplayOrientation = getDisplayOrientation(context);
                mCamera.setDisplayOrientation(mDisplayOrientation);

                // sets the focus mode
                if (params.getSupportedFocusModes().contains(FOCUS_MODE)) {
                    params.setFocusMode(FOCUS_MODE);
                    if (FOCUS_MODE == Camera.Parameters.FOCUS_MODE_AUTO) {
                        mManualFocusHelper = new ManualFocusHelper(mCamera);
                    }
                }

                // updates the camera parameter
                mCamera.setParameters(params);

                // init the sound player
                mMediaSoundHelper = new MediaSoundHelper();

                // initializes the ocr engine
                mOcrHandler = new OcrHandler(context, mCamera, mDisplayOrientation, new Handler() {
                    public void handleMessage(Message msg) {
                        setChanged();
                        notifyObservers(msg.getData());
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
        FOCUS_MODE = foc;
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
     * Gets the display orientation
     */
    private int getDisplayOrientation(Context context) {
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
}
