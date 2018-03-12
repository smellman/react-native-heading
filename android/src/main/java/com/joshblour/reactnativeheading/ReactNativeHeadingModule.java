package com.joshblour.reactnativeheading;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class ReactNativeHeadingModule extends ReactContextBaseJavaModule implements SensorEventListener {

    private static Context mApplicationContext;
    private float mAzimuth = 0; // degree
    private float newAzimuth = 0; // degree
    private float mFilter = 5;
    private SensorManager mSensorManager;
    private float[] mMagnetics = new float[3];
    private float[] mAccelerometers = new float[3];
    private float[] orientation = new float[3];

    private static final float alpha = 0.8f;

    public ReactNativeHeadingModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mApplicationContext = reactContext.getApplicationContext();
    }

    @Override
    public String getName() {
        return "ReactNativeHeading";
    }

    @ReactMethod
    public void start(int filter, Promise promise) {

        if (mSensorManager == null) {
            mSensorManager = (SensorManager) mApplicationContext.getSystemService(Context.SENSOR_SERVICE);
        }

        mFilter = filter;
        boolean started = mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);

        promise.resolve(started);
    }

    @ReactMethod
    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetics = event.values.clone();

                break;
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometers = event.values.clone();
                break;
        }

        if (mMagnetics != null && mAccelerometers != null) {
            float[] r = new float[16];
            float[] i = new float[16];
            float[] ir = new float[16];

            SensorManager.getRotationMatrix(r, i, mAccelerometers, mMagnetics);
            SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_X, SensorManager.AXIS_Z, ir);
            SensorManager.getOrientation(ir, orientation);

            float newAzimuth = (float) Math.toDegrees(orientation[0]);
            if (newAzimuth < 0) {
                newAzimuth = newAzimuth + 360.0f;
            }
            if (Math.abs(mAzimuth - newAzimuth) < mFilter) {
                return;
            }
            getReactApplicationContext()
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("headingUpdated", newAzimuth);
            mAzimuth = newAzimuth;

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
