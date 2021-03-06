package com.hsfl.speakshot.service.camera.helper;

import android.graphics.*;
import android.os.AsyncTask;
import java.io.*;

/**
 * Saves the image asynchronously
 */
public class ImagePersistenceHelper extends AsyncTask<byte[], Void, Void> {
    private static final String TAG = ImagePersistenceHelper.class.getSimpleName();

    /**
     * File name and path of where to save the image
     */
    private String mImageName;

    /**
     * The camera orientation on which amount the image is rotated
     */
    private int mOrientation = 0;
    private int mWidth = 0;
    private int mHeight = 0;
    private int mFormat = 0;

    /**
     * Constructor
     * @param format
     * @param orientation
     * @param width
     * @param height
     * @param name
     */
    public ImagePersistenceHelper(int format, int orientation, int width, int height, String name) {
        mFormat = format;
        mOrientation = orientation;
        mWidth = width;
        mHeight = height;
        mImageName = name;
    }

    /**
     * Converts a NV21 image from the camera to a jpeg encoded bitmap
     * @param data
     * @return
     */
    private Bitmap decodeFromYuv(byte[] data) {
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        return BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
    }

    @Override
    protected Void doInBackground(byte[]... data) {
        FileOutputStream outStream = null;
        try {
            File outFile = new File(mImageName);
            // create folder if not existing
            File dir = new File (outFile.getParent());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            outStream = new FileOutputStream(outFile);
            try {
                Bitmap bitmap = (mFormat == ImageFormat.NV21) ?
                    // image is in a raw format - convert to bitmap data
                    decodeFromYuv(data[0]) :
                    // image is in a compressed format
                    BitmapFactory.decodeByteArray(data[0], 0, data[0].length);

                // rotate if required
                Matrix matrix = new Matrix();
                matrix.setRotate(mOrientation);

                // save image into storage
                Bitmap rotatedBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                rotatedBmp.compress(Bitmap.CompressFormat.PNG, 100, outStream);

                outStream.flush();
                outStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {}
}
