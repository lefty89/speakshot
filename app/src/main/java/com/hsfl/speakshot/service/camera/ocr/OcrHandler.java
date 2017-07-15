package com.hsfl.speakshot.service.camera.ocr;

import android.content.Context;
import android.graphics.*;
import android.hardware.Camera;
import android.os.*;
import android.util.SparseArray;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.hsfl.speakshot.service.camera.ocr.processor.BaseProcessor;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class OcrHandler {
    private static final String TAG = OcrHandler.class.getSimpleName();

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
     * The text processor
     */
    private BaseProcessor mProcessor = null;

    /**
     * Continious OCR detector
     */
    private OcrDetector mOcrDetector = null;

    /**
     * Camera lock object
     */
    private final Object mCameraLock = new Object();

    /**
     * constructor
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
        }
    }

    /**
     * Sets a processor after whose the text is analyzed
     * @param processor
     */
    public void setProcessor(BaseProcessor processor) {
        mProcessor = processor;
        mProcessor.attachHandler(mHandler);
    }

    /**
     * Analyzes a given bitmap image
     * @param bitmap
     */
    public void ocrBitmapImage(Bitmap bitmap) {
        synchronized (mCameraLock) {
            // build frame from bitmap
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();

            // gets byte data from the bitmap image
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] image = stream.toByteArray();

            // start detection and processing
            SparseArray<TextBlock> detections = mTextRecognizer.detect(frame);
            mProcessor.receiveDetections(detections, image);
        }
    }

    /**
     * Analyzes a given raw image
     * @param data
     * @param format
     * @param width
     * @param height
     */
    public void ocrRawImage(byte[] data, int format, int width, int height, int rotation) {
        synchronized (mCameraLock) {
            // build frame from raw input
            Frame frame = new Frame.Builder().setImageData(ByteBuffer.wrap(data), width, height, format).setRotation((rotation / 90)).build();

            // simply returns all found texts
            mProcessor.setImagePersisting(true);
            mProcessor.setImageFormat(width, height, rotation, format);

            // start detection
            SparseArray<TextBlock> detections = mTextRecognizer.detect(frame);
            mProcessor.receiveDetections(detections, data);
        }
    }

    /**
     * Starts the continuous detection
     */
    public void startOcrDetector(int rotation) {
        synchronized (mCameraLock) {
            if (mOcrDetector == null) {
                mOcrDetector = new OcrDetector(mTextRecognizer, mCamera, rotation, mProcessor);
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
}
