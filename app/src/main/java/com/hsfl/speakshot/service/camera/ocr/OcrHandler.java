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
import com.hsfl.speakshot.service.camera.ocr.processor.ProcessorChain;

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
     * Analyzes a given bitmap image
     * @param chain
     * @param bitmap
     */
    public void ocrBitmapImage(ProcessorChain chain, Bitmap bitmap) {
        synchronized (mCameraLock) {
            // build frame from bitmap
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            // connects the handler
            chain.setHandler(mHandler);
            // gets byte data from the bitmap image
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] image = stream.toByteArray();
            // start detection and processing
            SparseArray<TextBlock> detections = mTextRecognizer.detect(frame);
            // starts the processor chain
            chain.execute(detections, image);
        }
    }

    /**
     * Analyzes a given raw image
     * @param chain
     * @param data
     * @param format
     * @param width
     * @param height
     * @param rotation
     */
    public void ocrRawImage(ProcessorChain chain, byte[] data, int format, int width, int height, int rotation) {
        synchronized (mCameraLock) {
            // build frame from raw input
            Frame frame = new Frame.Builder().setImageData(ByteBuffer.wrap(data), width, height, format).setRotation((rotation / 90)).build();
            // connects the handler
            chain.setHandler(mHandler);
            // start detection
            SparseArray<TextBlock> detections = mTextRecognizer.detect(frame);
            // starts the processor chain
            chain.execute(detections, data);
        }
    }

    /**
     * Starts the continuous detection
     * @param chain
     * @param rotation
     */
    public void startOcrDetector(ProcessorChain chain, int rotation) {
        synchronized (mCameraLock) {
            if (mOcrDetector == null) {
                // attach handler
                chain.setHandler(mHandler);
                mOcrDetector = new OcrDetector(mTextRecognizer, mCamera, rotation, chain);
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
