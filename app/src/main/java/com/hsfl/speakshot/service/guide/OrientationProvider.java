package com.hsfl.speakshot.service.guide;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class OrientationProvider implements SensorEventListener {

    /**
     * SensorManager
     */
    private SensorManager mSensorManager = null;

    /**
     * Current gravitation
     */
    private float[] mGravy = new float[3];

    /**
     * Current magnetic field
     */
    private float[] mGeoMags = new float[3];

    /**
     * Current magnetic field
     */
    private int mRotationHits = 0;

    /**
     * Current magnetic field
     */
    private int mRotationThreshold = 20;

    /**
     * Constructor
     * @param threshold
     * @param sensorManager
     */
    OrientationProvider(int threshold, SensorManager sensorManager) {
        mRotationThreshold = threshold;
        mSensorManager = sensorManager;
    }

    /**
     * Adds listener
     */
    void start() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * Removes listener
     */
    void stop() {
        mSensorManager.unregisterListener(this);
    }

    /**
     * Gets the current orientation
     * @return
     */
    int getRotationHits() {
        return mRotationHits;
    }

    /**
     * Checks whether the required sensors are available
     * @param sensorManager
     * @return
     */
    static boolean checkHardware(SensorManager sensorManager) {
        return  (sensorManager != null) &&
                (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) &&
                (sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).size() > 0);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mGravy = event.values.clone();
            case Sensor.TYPE_MAGNETIC_FIELD:
                mGeoMags = event.values.clone();
        }

        if (mGravy != null && mGeoMags != null) {
            float[] vOri = new float[3];
            float[] tMat = new float[9];
            float[] rMat = new float[9];
            float[] iMat = new float[9];

            boolean success = SensorManager.getRotationMatrix(tMat, iMat, mGravy, mGeoMags);
            if (success) {

                // remaps the matrix
                SensorManager.remapCoordinateSystem(tMat, SensorManager.AXIS_X, SensorManager.AXIS_Z, rMat);
                SensorManager.getOrientation(rMat, vOri);

                // get angles in degrees
                double degNY = Math.toDegrees(vOri[1]);
                double degNZ = Math.toDegrees(vOri[2]);

                // 90 deg steps
                int offY = (int) Math.round(degNY / 90);
                int offZ = (int) Math.round(degNZ / 90);

                mRotationHits = 0;
                mRotationHits |= ((degNY - (offY * 90)) < -mRotationThreshold) ? 1 : 0;
                mRotationHits |= ((degNY - (offY * 90)) > mRotationThreshold)  ? 2 : 0;
                mRotationHits |= ((degNZ - (offZ * 90)) < -mRotationThreshold) ? 4 : 0;
                mRotationHits |= ((degNZ - (offZ * 90)) > mRotationThreshold)  ? 8 : 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
