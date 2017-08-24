
package com.aliyun.homeshell;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;

import com.aliyun.utility.utils.ACA;

public class LauncherMotionHelper {
    public static final boolean SUPPORT_GRAVITY_MOTION = ACA.SystemProperties.getBoolean("ro.aliyun.gravity_wallpaper", false);
    private static final float sDensity = Resources.getSystem().getDisplayMetrics().density;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private View mPage;
    private Launcher mLauncher;
    private float mMaxValueX, mMaxValueY;

    public LauncherMotionHelper(Launcher launcher) {
        mLauncher = launcher;
        mSensorManager = (SensorManager) launcher.getSystemService(Context.SENSOR_SERVICE);
    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        float mX, mY;

        public void onSensorChanged(SensorEvent event) {
            if (event.values == null || event.values.length == 0)
                return;
            float x = event.values[0];
            float y = event.values[1];
            if (Math.abs(mX - x) >= .2f || Math.abs(mY - y) >= .2f) {
                mPage.setTranslationX((mX = x) / mMaxValueX);
                mPage.setTranslationY((mX = y) / mMaxValueY);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public void register() {
        if (!SUPPORT_GRAVITY_MOTION)
            return;
        mPage = mLauncher.getDragLayer();
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        try {
            if (mSensor != null) {
                mSensorManager.registerListener(mSensorEventListener, mSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
                float max = mSensor.getMaximumRange() / 2;
                mMaxValueX = max / (sDensity * -10);
                mMaxValueY = max / (sDensity * -12);
            }
        } catch (Exception e) {
            mSensor = null;
        }
    }

    public void unregister() {
        if (mSensor != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
            mSensor = null;
        }
    }
}
