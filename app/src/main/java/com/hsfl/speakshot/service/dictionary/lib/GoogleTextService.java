package com.hsfl.speakshot.service.dictionary.lib;

import android.content.Context;
import android.os.Bundle;
import android.view.textservice.*;
import com.hsfl.speakshot.service.dictionary.DictionaryService;

import java.util.Locale;
import java.util.Observable;

public class GoogleTextService implements DictionaryService.SpellChecker, SpellCheckerSession.SpellCheckerSessionListener {
    private static final String TAG = GoogleTextService.class.getSimpleName();

    /**
     * Contains the current session
     */
    private SpellCheckerSession mScs;

    /**
     * Reference to inform the observers about changes
     */
    private Observable mObservable;

    /**
     * Constructor
     * @param observable
     * @param context
     */
    public GoogleTextService(Observable observable, Context context) {
        mObservable = observable;

        TextServicesManager mTextServicesManager = (TextServicesManager)context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        mScs = mTextServicesManager.newSpellCheckerSession(null, Locale.getDefault(), this, true);
    }

    @Override
    public void check(String s) {
        mScs.getSentenceSuggestions(new TextInfo[] {new TextInfo(s)}, 1);
    }

    @Override
    public void release() {
        if (!mScs.isSessionDisconnected()) {
            mScs.close();
        }
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        // not implemented
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.length; ++i) {
            // Returned suggestions are contained in SuggestionsInfo
            final int len = results[i].getSuggestionsCount();
            for (int j = 0; j < len; ++j) {
                sb.append(results[i].getSuggestionsInfoAt(j).getSuggestionAt(i)).append(" ");
            }
            sendResponseBundle(sb.substring(0, sb.length()-1));
        }
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
}
