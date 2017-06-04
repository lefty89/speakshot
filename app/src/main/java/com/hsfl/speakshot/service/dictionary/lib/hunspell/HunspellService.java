package com.hsfl.speakshot.service.dictionary.lib.hunspell;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import com.hsfl.speakshot.service.dictionary.DictionaryService;

import java.io.*;
import java.util.Observable;

public class HunspellService implements DictionaryService.SpellChecker {
    private static final String TAG = HunspellService.class.getSimpleName();

    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Hunspell/";
    private static final String FOLDER    = "dict";

    /**
     * Hunspell
     * @param context
     */
    private Hunspell mHunspell;

    /**
     * Reference to inform the observers about changes
     */
    private Observable mObservable;

    /**
     * Constructor
     * @param observable
     * @param context
     */
    public HunspellService(Observable observable, Context context) {
        Log.d(TAG, "HUNSPELL SERVICE");
        mObservable = observable;

        // copies the dictionaries if not already done
        copyDictionaries(context);

        // init dict
        mHunspell = new Hunspell();
        String fileBase = DATA_PATH + FOLDER + "/de_DE_frami";
        mHunspell.create( fileBase + ".aff", fileBase + ".dic");
    }

    @Override
    public void check(String s) {
        String[] suggestions = mHunspell.getSuggestions(s);
        if (suggestions.length > 0) {
            sendResponseBundle(suggestions[0]);
        }
    }

    @Override
    public void release() {

    }

    /**
     * Builds a response Bundle
     * @param s
     */
    private void sendResponseBundle(String s) {
        Bundle b = new Bundle();
        b.putString(DictionaryService.OBS_TEXT, s);
        mObservable.notifyObservers(b);
    }

    /**
     * Copies the libraries from the asset storage to the external drive
     * @param context
     */
    private void copyDictionaries(Context context) {

        // create file on sd card
        File dir = new File(DATA_PATH + FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // copy libraries
        try {
            String fileList[] = context.getAssets().list(FOLDER);
            for (String fileName : fileList) {
                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = DATA_PATH + FOLDER + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {

                    InputStream  in  = context.getAssets().open(FOLDER + "/" + fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy files" + e.toString());
        }
    }
}
