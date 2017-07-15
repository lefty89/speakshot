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

import android.os.Handler;
import android.util.SparseArray;
import com.google.android.gms.vision.text.TextBlock;

import java.util.ArrayList;

/**
 * A very simple Processor which gets detected TextBlocks
 */
public abstract class BaseProcessor {
    private static final String TAG = BaseProcessor.class.getSimpleName();

    /**
     * Callback handler
     */
    Handler mHandler;

    /**
     * Image Settings
     */
    int mImageWidth    = 0;
    int mImageHeight   = 0;
    int mImageFormat   = 0;
    int mImageRotation = 0;

    /**
     * Flag that indicates image storing
     */
    boolean mImagePersisting = false;

    /**
     * Constructor
     */
    BaseProcessor() {}

    /**
     * Gets an array list from the sparse array so that it can be packed into
     * a Bundle
     * @param items
     * @return
     */
    ArrayList<String> sparseToList(SparseArray<TextBlock> items) {
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
     * Sets the handler to whose the result is published
     * @param handler
     */
    public void attachHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * Sets the image settings
     * @param width
     * @param height
     * @param rotation
     * @param format
     */
    public void setImageFormat(int width, int height, int rotation, int format) {
        mImageWidth = width;
        mImageHeight = height;
        mImageRotation = rotation;
        mImageFormat = format;
    }

    /**
     * Sets the image persisting flag
     * @param b
     */
    public void setImagePersisting(boolean b) {
        mImagePersisting = b;
    }

    /**
     * Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    public abstract void receiveDetections(SparseArray<TextBlock> detections, byte[] image);
}