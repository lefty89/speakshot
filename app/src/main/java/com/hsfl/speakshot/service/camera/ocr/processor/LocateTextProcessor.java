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

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Message;
import android.util.SparseArray;
import com.google.android.gms.vision.text.TextBlock;

/**
 * A very simple Processor which gets detected TextBlocks
 */
public class LocateTextProcessor extends BaseProcessor {
    private static final String TAG = LocateTextProcessor.class.getSimpleName();

    /**
     * The bundle result ids
     */
    public static final String RESULT_BORDER_HITS = "border_hits";

    /**
     * Offset from border
     */
    private final int THRESHOLD = 80;

    /**
     * Dimensions
     */
    private int  mWidth     = 0;
    private int  mHeight    = 0;
    private int  mInterval  = 0;

    /**
     * Helper var
     */
    private long mLastCheck = System.currentTimeMillis();

    /**
     * Lock object
     */
    private final Object mLock = new Object();

    /**
     * Constructor
     */
    public LocateTextProcessor(int width, int height, int interval) {
        mWidth    = width;
        mHeight   = height;
        mInterval = interval;
    }

    /**
     * Builds a response Bundle
     * @param hits
     */
    private void sendResponseBundle(int hits) {
        // packs the detector texts into a bundle
        Bundle b = new Bundle();
        b.putInt(RESULT_BORDER_HITS, hits);
        // create a message from the message handler to send it back to the main UI
        Message msg = mHandler.obtainMessage();
        //attach the bundle to the message
        msg.setData(b);
        //send the message back to main UI thread
        mHandler.sendMessage(msg);
    }

    @Override
    public void receiveDetections(SparseArray<TextBlock> detections, byte[] image) {
        // critical part
        synchronized (mLock) {
            if (mLastCheck+mInterval <= System.currentTimeMillis()) {
                mLastCheck = System.currentTimeMillis();

                // temp vars
                boolean left = false, right = false, bot = false, top = false;

                // bounding area rectangle
                Rect r1 = new Rect(THRESHOLD, THRESHOLD, mWidth-THRESHOLD, mHeight-THRESHOLD);

                // loop through all blocks
                for (int i=0; i<detections.size(); i++) {
                    if (detections.get(i) != null) {
                        // text rectangle
                        Rect r2 = detections.get(i).getBoundingBox();
                        // calculate the intersection
                        left  = (Math.max(r1.left, r2.left)     == r1.left)   || left;
                        top   = (Math.max(r1.top, r2.top)       == r1.top)    || top;
                        right = (Math.min(r1.right, r2.right)   == r1.right)  || right;
                        bot   = (Math.min(r1.bottom, r2.bottom) == r1.bottom) || bot;
                    }
                }

                // build up hits
                int hits =  (left) ?1:0;
                hits     |= (top)  ?2:0;
                hits     |= (right)?4:0;
                hits     |= (bot)  ?8:0;

                // notifies the observer
                sendResponseBundle(hits);
            }
        }
    }
}
