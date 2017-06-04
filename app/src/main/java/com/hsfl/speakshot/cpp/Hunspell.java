package com.hsfl.speakshot.cpp;

public class Hunspell {

	public native String test();
	
	static {
        System.loadLibrary("native-lib");
    }
}
