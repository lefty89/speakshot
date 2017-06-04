package com.hsfl.speakshot.service.dictionary;

import android.content.Context;
import com.hsfl.speakshot.service.dictionary.lib.GoogleTextService;

import java.util.*;

public class DictionaryService extends Observable {
    private static final String TAG = DictionaryService.class.getSimpleName();

    /**
     * Observable constants
     */
    public static final String OBS_TEXT = "cleantext";

    /**
     * The different spell checker tools to use
     */
    public static final int LIB_GOOGLE_TEXT_SERVICE = 0;

    /**
     * The DictionaryService singleton
     */
    private static DictionaryService instance = null;

    /**
     * Contains the selected spell checking lib
     */
    private SpellChecker mLib;

    /**
     * Empty constructor
     */
    DictionaryService() {}

    /**
     * Gets the DictionaryService instance
     * @return
     */
    public static DictionaryService getInstance() {
        if (instance == null) {
            instance = new DictionaryService();
        }
        return instance;
    }

    /**
     * Initializes the spell checker
     * @param
     */
    public void init(int lib, Context context) {
        switch (lib) {
            case LIB_GOOGLE_TEXT_SERVICE: mLib = new GoogleTextService(this, context); break;
            default: {
                throw new IllegalArgumentException("Library was not found");
            }
        }
    }

    /**
     * check
     * @param s
     */
    public void check(String s) {
        setChanged();
        mLib.check(s);
    }

    /**
     * release
     */
    public void release() {
        mLib.release();
    }

    /**
     * Interface that the different libs has to implement
     */
    public interface SpellChecker {
        /**
         * Starts the checking process
         * @param s
         */
        void check(String s);

        /**
         * Releases all resources
         */
        void release();
    }
}


