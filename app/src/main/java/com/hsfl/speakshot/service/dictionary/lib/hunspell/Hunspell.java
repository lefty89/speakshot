package com.hsfl.speakshot.service.dictionary.lib.hunspell;

public class Hunspell {

    public native void create(String aff, String dic);

    public native int spell(String word);

    public native String[] getSuggestions(String word);

    static {
        System.loadLibrary("hunspell-lib");
    }
}