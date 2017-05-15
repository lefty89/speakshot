package com.hsfl.speakshot.service.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;

public class OcrService {
    private static final String TAG = OcrService.class.getSimpleName();

    private Context mContext;
    private TextRecognizer mTextRecognizer;

    /**
     * Empty constructor
     */
    OcrService() {}

    /**
     * Starts updating the preview surface
     */
    public void setProcessor(Detector.Processor<TextBlock> detector) {
        if (mTextRecognizer != null) {
            mTextRecognizer.setProcessor(detector);
        }
    }

    /**
     * Analyzes a given image
     * @param name
     */
    public void analyseImage(String name) {

        File file = new File(name);
        if(file.exists() && file.canRead()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            Frame.Builder fb = new Frame.Builder().setBitmap(bmp);
            // use detector class
            mTextRecognizer.receiveFrame(fb.build());
            // DONT use detector class
            //SparseArray<TextBlock> blocks = mTextRecognizer.detect(fb.build());
        }
    }

    /**
     * OcrBuilder
     */
    public static class Builder {
        private OcrService mOcrService = new OcrService();

        /**
         * Creates a ocr builder
         */
        public Builder(Context context) {
            mOcrService.mContext = context;
        }

        /**
         * Creates an instance of the camera service.
         */
        public OcrService build() {
            mOcrService.mTextRecognizer = new TextRecognizer.Builder(mOcrService.mContext).build();
            return mOcrService;
        }
    }

    /**
     * IOcrCallback
     */
    public interface IOcrCallback {
        void receiveDetections(SparseArray<TextBlock> items);
    }
}
