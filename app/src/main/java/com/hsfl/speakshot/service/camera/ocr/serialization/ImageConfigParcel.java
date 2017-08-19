package com.hsfl.speakshot.service.camera.ocr.serialization;

import android.os.Parcel;
import android.os.Parcelable;

public class ImageConfigParcel implements Parcelable {
    private String width;
    private String height;
    private String rotation;
    private String format;

    // Constructor
    public ImageConfigParcel(int width, int height, int rotation, int format) {
        this.width = Integer.toString(width);
        this.height = Integer.toString(height);
        this.rotation = Integer.toString(rotation);
        this.format = Integer.toString(format);
    }
    // Parcelling part
    public ImageConfigParcel(Parcel in){
        String[] data = new String[4];

        in.readStringArray(data);
        // the order needs to be the same as in writeToParcel() method
        this.width    = data[0];
        this.height   = data[1];
        this.rotation = data[2];
        this.format   = data[3];
    }

    // getter / setter
    public int getWidth()    { return Integer.valueOf(width); }
    public int getHeight()   { return Integer.valueOf(height); }
    public int getRotation() { return Integer.valueOf(rotation); }
    public int getFormat()   { return Integer.valueOf(format); }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {this.width, this.height, this.rotation, this.format});
    }
    public static final Creator CREATOR = new Creator() {
        public ImageConfigParcel createFromParcel(Parcel in) {
            return new ImageConfigParcel(in);
        }

        public ImageConfigParcel[] newArray(int size) {
            return new ImageConfigParcel[size];
        }
    };
}
