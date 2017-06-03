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
package com.hsfl.speakshot.service.camera.ocr.processor;

import android.graphics.ImageFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.hsfl.speakshot.service.camera.AsyncImageSaver;
import com.hsfl.speakshot.service.camera.CameraService;

import java.util.ArrayList;

/**
 * A very simple Processor which gets detected TextBlocks
 */
public class RetrieveAllProcessor implements Detector.Processor<TextBlock> {
    private static final String TAG = RetrieveAllProcessor.class.getSimpleName();

    /**
     * Callback handler
     */
    private Handler mHandler;

    /**
     * The camera buffer
     */
    private byte[] mCameraBuffer;

    /**
     * the images size
     */
    private int mOrientation = 0;
    /**
     * Constructor
     * @param handler
     */
    public RetrieveAllProcessor(Handler handler, byte[] buffer, int orientation) {
        mCameraBuffer = buffer;
        mHandler = handler;
        mOrientation = orientation;
    }

    /**
     * Builds a response Bundle
     * @param texts
     */
    private void sendResponseBundle(ArrayList<String> texts, String snapshot) {
        // packs the detector texts into a bundle
        Bundle b = new Bundle();
        b.putStringArrayList("texts", texts);
        b.putString("snapshot", snapshot);
        // create a message from the message handler to send it back to the main UI
        Message msg = mHandler.obtainMessage();
        //attach the bundle to the message
        msg.setData(b);
        //send the message back to main UI thread
        mHandler.sendMessage(msg);
    }

    /**
     * Gets an array list from the sparse array so that it can be packed into
     * a Bundle
     * @param items
     * @return
     */
    private ArrayList<String> sparseToList(SparseArray<TextBlock> items) {
        ArrayList<String> texts = new ArrayList<>();
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                texts.add(item.getValue());
            }
        }
        return texts;
    }

    /**
     * Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        ArrayList<String> texts = sparseToList(detections.getDetectedItems());
        String snapshot = "";
        if (texts.size() > 0) {
            snapshot = CameraService.DATA_DIR + "/img-" + System.currentTimeMillis() + ".jpg";
            // saves the image asynchronously to the external storage
            Frame.Metadata md = detections.getFrameMetadata();
            // BUG Metadata's getFormat() returns -1 every time
            // BUG getWidth() and getHeight() results are switched
            new AsyncImageSaver(ImageFormat.NV21, mOrientation, md.getHeight(), md.getWidth(), snapshot).execute(mCameraBuffer);
        }
        sendResponseBundle(texts, snapshot);
    }

    /**
     * Frees the resources associated with this detection processor.
     */
    @Override
    public void release() {
        Log.d(TAG, "release");
    }
}
