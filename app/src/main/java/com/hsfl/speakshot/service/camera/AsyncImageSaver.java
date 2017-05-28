package com.hsfl.speakshot.service.camera;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Saves the image asynchronously
 */
class AsyncImageSaver extends AsyncTask<byte[], Void, Void> {
    private static final String TAG = AsyncImageSaver.class.getSimpleName();

    /**
     * File name and path of where to save the image
     */
    private String mImageName;
    private String mPathOnStorage;

    /**
     * The camera orientation on which amount the image is rotated
     */
    private int mOrientation = 0;

    AsyncImageSaver(int orientation, String path, String name) {
        mOrientation = orientation;
        mPathOnStorage = path;
        mImageName = name;
    }

    @Override
    protected Void doInBackground(byte[]... data) {
        FileOutputStream outStream = null;

        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath() + mPathOnStorage);
            dir.mkdirs();

            File outFile = new File(dir, mImageName);
            outStream = new FileOutputStream(outFile);
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data[0], 0, data[0].length);
                Matrix matrix = new Matrix();
                matrix.setRotate(mOrientation);

                Bitmap rotatedBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

                outStream.flush();
                outStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

            //refreshGallery(outFile);
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
