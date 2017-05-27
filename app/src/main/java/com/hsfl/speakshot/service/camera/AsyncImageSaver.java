package com.hsfl.speakshot.service.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Saves the image
 */
class AsyncImageSaver extends AsyncTask<byte[], Void, Void> {
    private static final String TAG = CameraService.class.getSimpleName();

    /**
     * the camera orientation on which amout the image is rotated
     */
    private String mImageName;
    private String mPathOnStorage;
    private int mOrientation = 0;
    private Context mContext;

    AsyncImageSaver(Context context, int orientation, String path, String name) {
        mOrientation = orientation;
        mPathOnStorage = path;
        mImageName = name;
        mContext = context;
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
    protected void onPostExecute(Void result) {
        Toast.makeText(mContext, "Image saved on external storage", Toast.LENGTH_LONG).show();
    }
}
