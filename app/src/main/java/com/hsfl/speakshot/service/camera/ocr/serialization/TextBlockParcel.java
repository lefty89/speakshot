package com.hsfl.speakshot.service.camera.ocr.serialization;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.vision.text.TextBlock;

public class TextBlockParcel implements Parcelable {
    private String text;
    private String rect_left;
    private String rect_top;
    private String rect_right;
    private String rect_bottom;

    // Constructor
    public TextBlockParcel(TextBlock tb) {
        this.text        = tb.getValue();
        this.rect_left   = Integer.toString(tb.getBoundingBox().left);
        this.rect_top    = Integer.toString(tb.getBoundingBox().top);
        this.rect_right  = Integer.toString(tb.getBoundingBox().right);
        this.rect_bottom = Integer.toString(tb.getBoundingBox().bottom);
    }

    // getter / setter
    public String getText() {
        return this.text;
    }
    // gets the bounding box
    public Rect getBoundingBox() {
        return new Rect(
            Integer.parseInt(this.rect_left),
            Integer.parseInt(this.rect_top),
            Integer.parseInt(this.rect_right),
            Integer.parseInt(this.rect_bottom)
        );
    }

    // Parcelling part
    public TextBlockParcel(Parcel in){
        String[] data = new String[5];

        in.readStringArray(data);
        // the order needs to be the same as in writeToParcel() method
        this.text        = data[0];
        this.rect_left   = data[1];
        this.rect_top    = data[2];
        this.rect_right  = data[3];
        this.rect_bottom = data[4];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {this.text, this.rect_left, this.rect_top, this.rect_right, this.rect_bottom});
    }
    public static final Creator CREATOR = new Creator() {
        public TextBlockParcel createFromParcel(Parcel in) {
            return new TextBlockParcel(in);
        }

        public TextBlockParcel[] newArray(int size) {
            return new TextBlockParcel[size];
        }
    };
}
