package com.hsfl.speakshot;

import android.os.Environment;

/**
 * App wide shared information
 */
public interface Constants {

    /**
     * The Path where the App data are located
     */
    String APP_PATH = Environment.getExternalStorageDirectory() + "/SpeakShot";

    /**
     * The Path where the captured images are saved
     */
    String IMAGE_PATH = APP_PATH + "/images";
}
