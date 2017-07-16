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
import android.os.Message;
import android.util.SparseArray;
import com.google.android.gms.vision.text.TextBlock;
import com.hsfl.speakshot.Constants;
import com.hsfl.speakshot.service.camera.helper.ImagePersistenceHelper;

import java.util.ArrayList;

/**
 * A very simple Processor which gets detected TextBlocks
 */
public class RetrieveAllProcessor extends BaseProcessor {
    private static final String TAG = RetrieveAllProcessor.class.getSimpleName();

    /**
     * The bundle result ids
     */
    public static final String RESULT_TEXTS    = "texts";
    public static final String RESULT_SNAPSHOT = "snapshot";

    /**
     * Constructor
     */
    public RetrieveAllProcessor() {}

    /**
     * Builds a response Bundle
     * @param texts
     */
    private void sendResponseBundle(ArrayList<String> texts, String snapshot) {
        // packs the detector texts into a bundle
        Bundle b = new Bundle();
        b.putStringArrayList(RESULT_TEXTS, texts);
        b.putString(RESULT_SNAPSHOT, snapshot);
        // create a message from the message handler to send it back to the main UI
        Message msg = mHandler.obtainMessage();
        //attach the bundle to the message
        msg.setData(b);
        //send the message back to main UI thread
        mHandler.sendMessage(msg);
    }

    @Override
    public void receiveDetections(SparseArray<TextBlock> detections, byte[] image) {
        ArrayList<String> texts = sparseToList(detections);
        String snapshot = "";
        if ((texts.size() > 0) && (mImagePersisting)) {
            snapshot = Constants.IMAGE_PATH + "/img-" + System.currentTimeMillis() + ".jpg";
            // copy the image else the buffer would be overridden before the saving is completed
            byte[] copy = new byte[image.length];
            System.arraycopy(image, 0, copy, 0, image.length);
            // saves the image asynchronously to the external storage
            new ImagePersistenceHelper(mImageFormat, mImageRotation, mImageWidth, mImageHeight, snapshot).execute(copy);
        }
        sendResponseBundle(texts, snapshot);
    }
}
