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

import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.vision.text.TextBlock;
import com.hsfl.speakshot.Constants;
import com.hsfl.speakshot.service.camera.helper.ImagePersistenceHelper;

import java.util.ArrayList;

/**
 * A very simple Processor which gets detected TextBlocks
 */
public class ImageProcessor extends BaseProcessor {
    private static final String TAG = ImageProcessor.class.getSimpleName();

    /**
     * The bundle result ids
     */
    public static final String RESULT_SNAPSHOT_PATH    = String.format("%s_snapshot_path", TAG);
    public static final String RESULT_SNAPSHOT_TRIGGER = String.format("%s_snapshot_trigger", TAG);

    /**
     * Lock object
     */
    private final Object mLock = new Object();

    private int mImageWidth;
    private int mImageHeight;
    private int mImageRotation;
    private int mImageFormat;

    /**
     * Flag that indicates the trigger process
     */
    private boolean mSavingTriggered = false;

    /**
     * Constructor
     * @param handler
     * @param searchTerm
     */
    ImageProcessor(int width, int height, int rotation, int format) {
        mImageWidth    = width;
        mImageHeight   = height;
        mImageRotation = rotation;
        mImageFormat   = format;
    }

    public void process(Bundle bundle, SparseArray<TextBlock> detections, byte[] image) {
        // critical part
        synchronized (mLock) {
            if (bundle.getBoolean(RESULT_SNAPSHOT_TRIGGER) && !mSavingTriggered) {
                mSavingTriggered = true;
                // saves the image to the storage
                String snapshot = Constants.IMAGE_PATH + "/img-" + System.currentTimeMillis() + ".jpg";
                // copy the image else the buffer would be overridden before the saving is completed
                byte[] copy = new byte[image.length];
                System.arraycopy(image, 0, copy, 0, image.length);
                // saves the image asynchronously to the external storage
                new ImagePersistenceHelper(mImageFormat, mImageRotation, mImageWidth, mImageHeight, snapshot).execute(copy);
                // response to the listener
                bundle.putString(RESULT_SNAPSHOT_PATH, snapshot);
            }
        }
    }
}
