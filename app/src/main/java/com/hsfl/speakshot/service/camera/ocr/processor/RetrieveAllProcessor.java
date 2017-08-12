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

import static com.hsfl.speakshot.service.camera.ocr.processor.ImageProcessor.RESULT_SNAPSHOT_TRIGGER;

/**
 * A very simple Processor which gets detected TextBlocks
 */
public class RetrieveAllProcessor extends BaseProcessor {
    private static final String TAG = RetrieveAllProcessor.class.getSimpleName();

    /**
     * The bundle result ids
     */
    public static final String RESULT_ALL_TEXTS = String.format("%s_texts", TAG);

    /**
     * Constructor
     */
    public RetrieveAllProcessor() {}

    /**
     * Constructor
     */
    public RetrieveAllProcessor(boolean triggerImage) {
        mTriggerImage = triggerImage;
    }

    @Override
    public void process(Bundle bundle, SparseArray<TextBlock> detections, byte[] image) {
        ArrayList<String> texts = sparseToList(detections);

        // sets a flag that the image processor shall save the image permanently
        if ((texts.size() > 0) && (mTriggerImage)) {
            bundle.putBoolean(RESULT_SNAPSHOT_TRIGGER, true);
        }
        // saves the all terms within the bundle
        bundle.putStringArrayList(RESULT_ALL_TEXTS, texts);
    }
}
