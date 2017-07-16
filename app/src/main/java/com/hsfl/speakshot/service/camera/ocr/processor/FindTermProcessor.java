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
public class FindTermProcessor extends BaseProcessor {
    private static final String TAG = FindTermProcessor.class.getSimpleName();

    /**
     * The Search Term
     */
    private String mSearchTerm;

    /**
     * Constructor
     * @param handler
     * @param searchTerm
     */
    public FindTermProcessor(String searchTerm) {
        mSearchTerm = searchTerm;
    }

    /**
     * Searches for a given word within the textblocks
     */
    private String findSearchTerm(ArrayList<String> texts) {
        for (int i = 0; i < texts.size(); ++i) {
            String item = texts.get(i);
            if (item.toLowerCase().contains(mSearchTerm.toLowerCase())) {
                return item;
            }
        }
        return "";
    }

    /**
     * Builds a response Bundle
     * @param s
     */
    private void sendResponseBundle(String s, ArrayList<String> texts, String snapshot) {
        // packs the detector texts into a bundle
        Bundle b = new Bundle();
        b.putString("term", s);
        b.putString("snapshot", snapshot);
        b.putStringArrayList("texts", texts);
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

        String s = findSearchTerm(texts);
        if (!s.isEmpty()) {
            String snapshot = "";
            // saves the image to the storage
            if (mImagePersisting) {
                snapshot = Constants.IMAGE_PATH + "/img-" + System.currentTimeMillis() + ".jpg";
                // copy the image else the buffer would be overridden before the saving is completed
                byte[] copy = new byte[image.length];
                System.arraycopy(image, 0, copy, 0, image.length);
                // saves the image asynchronously to the external storage
                new ImagePersistenceHelper(mImageFormat, mImageRotation, mImageWidth, mImageHeight, snapshot).execute(copy);
            }
            // response to the listener
            sendResponseBundle(s, texts, snapshot);
        }
    }
}
