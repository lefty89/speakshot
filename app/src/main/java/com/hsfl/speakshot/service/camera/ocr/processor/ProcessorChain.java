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
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import com.google.android.gms.vision.text.TextBlock;

import java.util.ArrayList;

/**
 * A very simple Processor which gets detected TextBlocks
 */
public class ProcessorChain {
    private static final String TAG = ProcessorChain.class.getSimpleName();

    /**
     * Processor chain
     */
    private ArrayList<BaseProcessor> mProcessors = null;

    /**
     * Return handler
     */
    private Handler mHandler = null;

    /**
     * Constructor
     */
    public ProcessorChain() {
        mProcessors = new ArrayList<>();
    }

    /**
     * Adds a processor
     * @param pro
     */
    public void add(BaseProcessor pro) {
        mProcessors.add(pro);
    }

    /**
     * Special processor to save the images
     * @param width
     * @param height
     * @param rotation
     * @param format
     */
    public void setImageProcessor(int width, int height, int rotation, int format) {
        mProcessors.add(new ImageProcessor(width, height, rotation, format));
    }

    /**
     * Sets the handler
     * @param handler
     */
    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * Executes the processor chain
     * @param detections
     * @param data
     */
    public void execute(SparseArray<TextBlock> detections, byte[] data) {
        Bundle b = new Bundle();
        for (BaseProcessor pro : mProcessors) {
            pro.process(b, detections , data);
        }
        // create a message from the message handler to send it back to the main UI
        Message msg = mHandler.obtainMessage();
        //attach the bundle to the message
        msg.setData(b);
        //send the message back to main UI thread
        mHandler.sendMessage(msg);
    }
}
