package com.hsfl.speakshot.service.camera.ocr;

import android.content.Context;
import android.graphics.*;
import android.hardware.Camera;
import android.os.*;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.hsfl.speakshot.service.camera.ocr.serialization.ImageConfigParcel;
import com.hsfl.speakshot.service.camera.ocr.serialization.TextBlockParcel;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class OcrHandler {
    private static final String TAG = OcrHandler.class.getSimpleName();

    /**
     * Detector actions
     */
    public static final String DETECTOR_ACTION_BITMAP = String.format("%s_detect_bitmap", TAG);
    public static final String DETECTOR_ACTION_RAW    = String.format("%s_detect_raw", TAG);
    public static final String DETECTOR_ACTION_STREAM = String.format("%s_detect_stream", TAG);

    /**
     * Bundle settings
     */
    public static final String BUNDLE_TYPE       = String.format("%s_bundle_type", TAG);
    public static final String BUNDLE_IMG_DATA   = String.format("%s_bundle_img_data", TAG);
    public static final String BUNDLE_IMG_CONFIG = String.format("%s_bundle_img_config", TAG);
    public static final String BUNDLE_DETECTIONS = String.format("%s_bundle_detections", TAG);

    /**
     * The current camera object
     */
    private Camera mCamera;

    /**
     * The camera source return handler
     */
    private Handler mHandler;

    /**
     * The text recognizer
     */
    private TextRecognizer mTextRecognizer;

    /**
     * Continious OCR detector
     */
    private OcrDetector mOcrDetector = null;

    /**
     * Camera lock object
     */
    private final Object mCameraLock = new Object();

    /**
     * Constructor
     */
    public OcrHandler(final Context context, Camera camera, Handler handler) {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                return;
            }
            mCamera   = camera;
            mHandler  = handler;

            // create the TextRecognizer
            mTextRecognizer = new TextRecognizer.Builder(context).build();
            if (!mTextRecognizer.isOperational()) {
                Log.d(TAG, "Text recognizer is not ready");
            }
        }
    }

    /**
     * Returns the ocr detector
     * @return
     */
    public OcrDetector getOcrDetector() {
        return mOcrDetector;
    }

    /**
     * Analyzes a given bitmap image
     * @param bitmap
     * @param config
     */
    public void ocrBitmapImage(Bitmap bitmap, ImageConfigParcel config) {
        synchronized (mCameraLock) {
            // build frame from bitmap
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();

            // gets byte data from the bitmap image
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] image = stream.toByteArray();

            // process detection
            SparseArray<TextBlock> detections = mTextRecognizer.detect(frame);

            // send response
            sendResponse(DETECTOR_ACTION_BITMAP, config, image, detections);
        }
    }

    /**
     * Analyzes a given raw image
     * @param data
     * @param config
     */
    public void ocrRawImage(byte[] data, ImageConfigParcel config) {
        synchronized (mCameraLock) {
            // build frame from raw input
            Frame frame = new Frame.Builder().setImageData(ByteBuffer.wrap(data), config.getWidth(), config.getHeight(), config.getFormat()).setRotation((config.getRotation() / 90)).build();

            // process detection
            SparseArray<TextBlock> detections = mTextRecognizer.detect(frame);

            // send response
            sendResponse(DETECTOR_ACTION_RAW, config, data, detections);
        }
    }

    /**
     * Starts the continuous detection
     * @param rotation
     */
    public void startOcrDetector(int rotation) {
        synchronized (mCameraLock) {
            if (mOcrDetector == null) {
                int format = mCamera.getParameters().getPreviewFormat();
                Camera.Size size = mCamera.getParameters().getPreviewSize();
                // preview config
                ImageConfigParcel config = new ImageConfigParcel(size.width, size.height, rotation, format);
                mOcrDetector = new OcrDetector(this, mTextRecognizer, mCamera, config);
                // attaches a buffer callback
                mCamera.setPreviewCallbackWithBuffer(mOcrDetector);
            }
            // starts the detector
            mOcrDetector.startOcrDetector();
        }
    }

    /**
     * Stops the continuous detection
     */
    public void stopOcrDetector() {
        synchronized (mCameraLock) {
            if (mOcrDetector != null) {
                mOcrDetector.stopOcrDetector();
            }
        }
    }

    /**
     * Releases the continuous detector
     */
    public void releaseOcrDetector() {
        synchronized (mCameraLock) {
            if (mOcrDetector != null) {
                mOcrDetector.releaseOcrDetector();
                mOcrDetector = null;
            }
        }
    }

    /**
     * Releases the continuous detector
     */
    protected void sendResponse(String type, ImageConfigParcel config, byte[] image, SparseArray<TextBlock> detections) {

        // convert text block sparse array to parcel text array list
        ArrayList<TextBlockParcel> tp = new ArrayList<>();
        for (int i = 0; i < detections.size(); ++i) {
            TextBlock item = detections.valueAt(i);
            if (item != null && item.getValue() != null) {
                tp.add(new TextBlockParcel(detections.valueAt(i)));
            }
        }

        // create response bundle
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_TYPE, type);
        bundle.putParcelable(BUNDLE_IMG_CONFIG, config);
        bundle.putByteArray(BUNDLE_IMG_DATA, image);
        bundle.putParcelableArrayList(BUNDLE_DETECTIONS, tp);

        // create a message from the message handler to send it back to the main UI
        Message msg = mHandler.obtainMessage();
        //attach the bundle to the message
        msg.setData(bundle);
        //send the message back to main UI thread
        mHandler.sendMessage(msg);
    }
}
