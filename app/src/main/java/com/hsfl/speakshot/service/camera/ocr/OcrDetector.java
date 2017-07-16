package com.hsfl.speakshot.service.camera.ocr;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.hsfl.speakshot.service.camera.ocr.processor.BaseProcessor;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class OcrDetector implements Camera.PreviewCallback {
    private static final String TAG = OcrDetector.class.getSimpleName();

    /**
     * The Camera object
     */
    private Camera mCamera;

    /**
     * The camera orientation
     */
    private int mRotation = 0;

    /**
     * The text processor to check against
     */
    private BaseProcessor mProcessor = null;

    /**
     * Map to convert between a byte array, received from the camera, and its associated byte
     * buffer.  We use byte buffers internally because this is a more efficient way to call into
     * native code later (avoids a potential copy).
     */
    private Map<byte[], ByteBuffer> mBytesToByteBuffer = new HashMap<>();

    /**
     * Dedicated thread and associated runnable for calling into the detector with frames, as the
     * frames become available from the camera.
     */
    private Thread mProcessingThread;
    private FrameProcessingRunnable mFrameProcessor;

    /**
     * Constructor
     */
    OcrDetector(TextRecognizer recognizer, Camera camera, int rotation, BaseProcessor processor) {
        // assigns the camera
        mCamera = camera;
        // gets the camera orientation
        mRotation = rotation;
        // text processor
        mProcessor = processor;
        // connecting the frame processor
        mFrameProcessor = new FrameProcessingRunnable(recognizer);
    }

    /**
     * Starts the continuous detection
     */
    void startOcrDetector() {
        int width  = mCamera.getParameters().getPreviewSize().width;
        int height = mCamera.getParameters().getPreviewSize().height;

        // creates preview buffers
        mCamera.addCallbackBuffer(createPreviewBuffer(width, height));
        mCamera.addCallbackBuffer(createPreviewBuffer(width, height));
        mCamera.addCallbackBuffer(createPreviewBuffer(width, height));

        // start processor
        mProcessingThread = new Thread(mFrameProcessor);
        mFrameProcessor.setActive(true);

        mProcessingThread.start();
    }

    /**
     * Closes the camera and stops sending frames to the underlying frame detector.
     * This camera source may be restarted again by calling start or start(SurfaceHolder).
     * Call release() instead to completely shut down this camera source and release the
     * resources of the underlying detector.
     */
    void stopOcrDetector() {
        mFrameProcessor.setActive(false);
        if (mProcessingThread != null) {
            try {
                // Wait for the thread to complete to ensure that we can't have multiple threads
                // executing at the same time (i.e., which would happen if we called start too
                // quickly after stop).
                mProcessingThread.join();
            } catch (InterruptedException e) {
                Log.d(TAG, "Frame processing thread interrupted on release.");
            }
            mProcessingThread = null;
        }
        // clear the buffer to prevent oom exceptions
        mBytesToByteBuffer.clear();
    }

    /**
     * Stops the camera and releases the resources of the camera and underlying detector.
     */
    void releaseOcrDetector() {
        stopOcrDetector();
        mFrameProcessor.release();
    }

    /**
     * Creates one buffer for the camera preview callback.  The size of the buffer is based off of
     * the camera preview size and the format of the camera image.
     *
     * @return a new preview buffer of the appropriate size for the current camera settings
     */
    private byte[] createPreviewBuffer(int width, int height) {
        int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);
        long sizeInBits = height * width * bitsPerPixel;
        int bufferSize = (int) Math.ceil(sizeInBits / 8.0d) + 1;

        //
        // NOTICE: This code only works when using play services v. 8.1 or higher.
        //

        // Creating the byte array this way and wrapping it, as opposed to using .allocate(),
        // should guarantee that there will be an array to work with.
        byte[] byteArray = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        if (!buffer.hasArray() || (buffer.array() != byteArray)) {
            // I don't think that this will ever happen.  But if it does, then we wouldn't be
            // passing the preview content to the underlying detector later.
            throw new IllegalStateException("Failed to create valid buffer for camera source.");
        }

        mBytesToByteBuffer.put(byteArray, buffer);
        return byteArray;
    }

    /**
     * Called when the camera has a new preview frame.
     */
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        mFrameProcessor.setNextFrame(data, camera);
    }

    /**
     * Send the incoming camera images to the ocr processor
     */
    public class FrameProcessingRunnable implements Runnable {
        private Detector<?> mDetector;
        private long mStartTimeMillis = SystemClock.elapsedRealtime();

        // This lock guards all of the member variables below.
        private final Object mLock = new Object();
        private boolean mActive = true;

        // These pending variables hold the state associated with the new frame awaiting processing.
        private long mPendingTimeMillis;
        private int mPendingFrameId = 0;
        private ByteBuffer mPendingFrameData;


        FrameProcessingRunnable(Detector<?> detector) {
            mDetector = detector;
        }

        /**
         * Releases the underlying receiver.  This is only safe to do after the associated thread
         * has completed, which is managed in camera source's release method above.
         */
        @SuppressLint("Assert")
        void release() {
            assert (mProcessingThread.getState() == Thread.State.TERMINATED);
            mDetector.release();
            mDetector = null;
        }

        /**
         * Marks the runnable as active/not active.  Signals any blocked threads to continue.
         */
        void setActive(boolean active) {
            synchronized (mLock) {
                mActive = active;
                mLock.notifyAll();
            }
        }

        /**
         * Sets the frame data received from the camera.  This adds the previous unused frame buffer
         * (if present) back to the camera, and keeps a pending reference to the frame data for
         * future use.
         */
        void setNextFrame(byte[] data, Camera camera) {
            synchronized (mLock) {
                if (mPendingFrameData != null) {
                    camera.addCallbackBuffer(mPendingFrameData.array());
                    mPendingFrameData = null;
                }

                if (!mBytesToByteBuffer.containsKey(data)) {
                    Log.d(TAG,
                            "Skipping frame.  Could not find ByteBuffer associated with the image " +
                                    "data from the camera.");
                    return;
                }

                // Timestamp and frame ID are maintained here, which will give downstream code some
                // idea of the timing of frames received and when frames were dropped along the way.
                mPendingTimeMillis = SystemClock.elapsedRealtime() - mStartTimeMillis;
                mPendingFrameId++;
                mPendingFrameData = mBytesToByteBuffer.get(data);



                // Notify the processor thread if it is waiting on the next frame (see below).
                mLock.notifyAll();
            }
        }

        /**
         * As long as the processing thread is active, this executes detection on frames
         * continuously.  The next pending frame is either immediately available or hasn't been
         * received yet.  Once it is available, we transfer the frame info to local variables and
         * run detection on that frame.  It immediately loops back for the next frame without
         * pausing.
         * <p/>
         * If detection takes longer than the time in between new frames from the camera, this will
         * mean that this loop will run without ever waiting on a frame, avoiding any context
         * switching or frame acquisition time latency.
         * <p/>
         * If you find that this is using more CPU than you'd like, you should probably decrease the
         * FPS setting above to allow for some idle time in between frames.
         */
        @Override
        public void run() {
            Frame outputFrame;
            ByteBuffer data;

            while (true) {
                synchronized (mLock) {
                    while (mActive && (mPendingFrameData == null)) {
                        try {
                            // Wait for the next frame to be received from the camera, since we don't have it yet.
                            mLock.wait();
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Frame processing loop terminated.", e);
                            return;
                        }
                    }

                    if (!mActive) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return;
                    }

                    // gets camera size and format
                    int format = mCamera.getParameters().getPreviewFormat();
                    Camera.Size size = mCamera.getParameters().getPreviewSize();

                    // build a frame
                    outputFrame = new Frame.Builder()
                            .setImageData(mPendingFrameData, size.width, size.height, format)
                            .setId(mPendingFrameId)
                            .setTimestampMillis(mPendingTimeMillis)
                            .setRotation((mRotation / 90))
                            .build();
                    // Hold onto the frame data locally, so that we can use this for detection
                    // below.  We need to clear mPendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.
                    data = mPendingFrameData;
                    mPendingFrameData = null;
                }

                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.
                try {
                    SparseArray<TextBlock> detections = (SparseArray<TextBlock>) mDetector.detect(outputFrame);
                    mProcessor.receiveDetections(detections, data.array());
                } catch (Throwable t) {
                    Log.e(TAG, "Exception thrown from receiver.", t);
                } finally {
                    mCamera.addCallbackBuffer(data.array());
                }
            }
        }
    }
}
