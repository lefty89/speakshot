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
import android.util.Log;
import android.util.SparseArray;
import com.google.android.gms.vision.text.TextBlock;
import com.hsfl.speakshot.Constants;
import com.hsfl.speakshot.service.camera.helper.ImagePersistenceHelper;
import java.util.ArrayList;

import static com.hsfl.speakshot.service.camera.ocr.processor.ImageProcessor.RESULT_SNAPSHOT_TRIGGER;

/**
 * A very simple Processor which gets detected TextBlocks
 */
public class FindTermProcessor extends BaseProcessor {
    private static final String TAG = FindTermProcessor.class.getSimpleName();

    /**
     * The bundle result ids
     */
    public static final String RESULT_TERM_SEARCH = String.format("%s_term_search", TAG);
    public static final String RESULT_TERM_FOUND  = String.format("%s_term_found", TAG);

    /**
     * The Search Term
     */
    private String mSearchTerm;

    /**
     * Constructor
     * @param searchTerm
     */
    public FindTermProcessor(String searchTerm) {
        mSearchTerm = searchTerm;
    }

    /**
     * Constructor
     * @param searchTerm
     * @param triggerImage
     */
    public FindTermProcessor(String searchTerm, boolean triggerImage) {
        mSearchTerm   = searchTerm;
        mTriggerImage = triggerImage;
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

    @Override
    public void process(Bundle bundle, SparseArray<TextBlock> detections, byte[] image) {
        ArrayList<String> texts = sparseToList(detections);
        String s = findSearchTerm(texts);

        if (!s.isEmpty()) {
            // saves the complete term within the bundle
            bundle.putString(RESULT_TERM_SEARCH, mSearchTerm);
            bundle.putString(RESULT_TERM_FOUND, s);

            // sets a flag that the image processor shall save the image permanently
            if (mTriggerImage) {bundle.putBoolean(RESULT_SNAPSHOT_TRIGGER, true);}
        }
    }
}
