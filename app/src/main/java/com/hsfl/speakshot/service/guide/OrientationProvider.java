package com.hsfl.speakshot.service.guide;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class OrientationProvider implements SensorEventListener {

    /**
     * SensorManager
     */
    SensorManager mSensorManager = null;

    /**
     * Current gravitation
     */
    float[] mGravs = new float[3];

    /**
     * Current magnetic field
     */
    float[] mGeoMags = new float[3];

    /**
     * Constructor
     * @param sensorManager
     */
    OrientationProvider(SensorManager sensorManager) {
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

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mGravs = event.values.clone();
            case Sensor.TYPE_MAGNETIC_FIELD:
                mGeoMags = event.values.clone();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * Gets the current orientation
     * @return
     */
    int getRotationHits(int threshold) {
        int result = 0;

        if (mGravs != null && mGeoMags != null) {
            float[] vOri = new float[3];
            float[] tMat = new float[9];
            float[] rMat = new float[9];
            float[] iMat = new float[9];

            boolean success = SensorManager.getRotationMatrix(tMat, iMat, mGravs, mGeoMags);
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

                result |= ((degNY - (offY * 90)) < -threshold) ? 1 : 0;
                result |= ((degNY - (offY * 90)) > threshold)  ? 2 : 0;
                result |= ((degNZ - (offZ * 90)) < -threshold) ? 4 : 0;
                result |= ((degNZ - (offZ * 90)) > threshold)  ? 8 : 0;
            }
        }
        return result;
    }

    /**
     * Checks whether the required sensors are available
     * @param sensorManager
     * @return
     */
    public static boolean checkHardwarde(SensorManager sensorManager) {
        return  (sensorManager != null) &&
                (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) &&
                (sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).size() > 0);
    }
}
