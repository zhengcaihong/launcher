package com.aliyun.homeshell.activateapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.aliyun.homeshell.LauncherApplication;

public class FootprintBase {
    private static final String TAG = "Homeshell_FootprintBase";

    private static FootprintBase sInstance;

    private static final String FOOTPRINT = "footprint";
    private static final String ACTIVATE_STATUS = "activate_status";
    private static final String WIFI_CONNECTED = "wifi_connected";
    private static final String VALID_CALL_COUNT = "valid_call_count";
    private static final String TRIGGER_TIME = "trigger_time";
    private static final String TRIGGER_EXPIRED = "trigger_expired";
    private static final String TRIGGER_PERIODIC = "trigger_periodic";
    private static final String NOTIFY_TIME = "notify_time";
    private static final String CALL_STATE = "call_state";

    public static final int ACTIVATE_STATUS_NOT_ACTIVATE = 0;
    public static final int ACTIVATE_STATUS_NEED_ACTIVATE = 1;
    public static final int ACTIVATE_STATUS_ACTIVATE_CANCEL = 2;
    public static final int ACTIVATE_STATUS_ACTIVATE_DONE = 3;
    public static final int ACTIVATE_STATUS_ACTIVATE_SKIP = 4;

    private static final long TRIGGER_INTERVAL = AlarmManager.INTERVAL_DAY * 3;
    public static final int TRIGGER_CALL_NUM = 5;

    public static final String ACTION_TRIGGER_EXPIRED = "action.com.aliyun.homeshell.activateapp.TRIGGER_EXPIRED";
    public static final String ACTION_ACTIVATE_PERIODIC = "action.com.aliyun.homeshell.activateapp.ACTIVATE_PERIODIC";
    public static final String ACTION_ACTIVATE_TEST = "action.com.aliyun.homeshell.activateapp.ACTIVATE_TEST";

    private Context mContext = LauncherApplication.getContext();
    private SharedPreferences mSharedPrefs = mContext.getSharedPreferences(
            FOOTPRINT, Activity.MODE_PRIVATE);
    private SharedPreferences.Editor mEditor = mSharedPrefs.edit();

    // provider the singleton instance
    public static synchronized FootprintBase getInstance() {
        if (sInstance == null) {
            sInstance = new FootprintBase();
        }
        return sInstance;
    }

    // set the system as activated if the user has activated the application
    public void setActivateStatus(int activateStatus) {
        Log.i(TAG, "set activate status as " + activateStatus);
        mEditor.putInt(ACTIVATE_STATUS, activateStatus);
        mEditor.commit();
    }

    // get the activated status
    public int getActivateStatus() {
        int activateStatus = mSharedPrefs.getInt(ACTIVATE_STATUS, 0);
        return activateStatus;
    }

    public void setWifiConnected(boolean wifiConnected) {
        Log.i(TAG, "set wifiConnected as " + wifiConnected);
        mEditor.putBoolean(WIFI_CONNECTED, wifiConnected);
        mEditor.commit();
    }

    public boolean getWifiConnected() {
        boolean wifiConnected = mSharedPrefs.getBoolean(WIFI_CONNECTED, false);
        return wifiConnected;
    }

    public void addValidCall() {
        int count = getValidCall() + 1;
        Log.i(TAG, "set valid call numbers as " + count);
        mEditor.putInt(VALID_CALL_COUNT, count);
        mEditor.commit();
    }

    public void setValidCalls(int num) {
        mEditor.putInt(VALID_CALL_COUNT, num);
        mEditor.commit();
    }

    public int getValidCall() {
        int validCall = mSharedPrefs.getInt(VALID_CALL_COUNT, 0);
        return validCall;
    }

    public  void setCallState(int state) {
        mEditor.putInt(CALL_STATE, state);
        mEditor.commit();
    }

    public int getCallState( ) {
        int validCall = mSharedPrefs.getInt(CALL_STATE, TelephonyManager.CALL_STATE_IDLE);
        return validCall;
    }

    public void triggerTiming() {
        long currentTime = 0;
        long triggerTime = 0;
        long thresholdTime = 1451577600000l; // 2016.1.1
        currentTime = System.currentTimeMillis();
        Log.i(TAG, "current time is: " + currentTime);
        long savedTriggerTime = getTriggerTime();
        Log.i(TAG, "savedTriggerTime is: " + savedTriggerTime);
        
        Intent intent = new Intent(ACTION_TRIGGER_EXPIRED);
        PendingIntent expiredIntent = PendingIntent.getBroadcast(mContext,
                    0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarm = (AlarmManager) mContext
                    .getSystemService(Context.ALARM_SERVICE);

        // only trigger the time when current is after 2016
        if (currentTime > thresholdTime) {
            if ( savedTriggerTime == 0 ) {
                triggerTime = currentTime + TRIGGER_INTERVAL;
                mEditor.putLong(TRIGGER_TIME, triggerTime);
                mEditor.commit();
            } else {
                triggerTime = savedTriggerTime;
            }
            Log.i(TAG, "set the trigger time, now is: " + currentTime
                    + ", should be triggerred at: " + triggerTime);
            if(triggerTime > currentTime){
                alarm.set(AlarmManager.RTC, triggerTime, expiredIntent);
            }
        } else {
           Log.i(TAG , "time is before 2016, cancel it");
           alarm.cancel(expiredIntent);
           mEditor.putLong(TRIGGER_TIME, 0);
           mEditor.commit();
        }
    }

    public long getTriggerTime() {
        long triggerTime = mSharedPrefs.getLong(TRIGGER_TIME, 0);
        return triggerTime;
    }

    public void setTriggerExpired(boolean triggerExpired) {
        Log.i(TAG, "set triggerExpired as " + triggerExpired);
        mEditor.putBoolean(TRIGGER_EXPIRED, triggerExpired);
        mEditor.commit();
    }

    public boolean getTriggerExpired() {
        boolean triggerExpired = mSharedPrefs
                .getBoolean(TRIGGER_EXPIRED, false);
        return triggerExpired;
    }

    public void incraseTriggerPeriodic() {
        long periodic = mSharedPrefs.getLong(TRIGGER_PERIODIC, 0);
        Log.i(TAG, "previous periodic is : " + periodic);
        if (0 == periodic) {
            periodic = AlarmManager.INTERVAL_DAY;
        } else if (AlarmManager.INTERVAL_DAY == periodic) {
            periodic = AlarmManager.INTERVAL_DAY * 3;
        } else if(AlarmManager.INTERVAL_DAY * 3 == periodic){
            periodic = AlarmManager.INTERVAL_DAY * 7;
        } else{
            setActivateStatus(FootprintBase.ACTIVATE_STATUS_ACTIVATE_SKIP);
        }
        mEditor.putLong(TRIGGER_PERIODIC, periodic);
        mEditor.commit();
        Log.i(TAG , "current periodic is: " + periodic);
    }

    public void setActivateDelay() {
        Log.i(TAG, "delay to activate the apps");
        long currentTime = 0;
        long periodicTime = 0;
        currentTime = System.currentTimeMillis();
        Log.i(TAG, "current time is:" + currentTime);

        long notifyTime = mSharedPrefs.getLong(NOTIFY_TIME , 0);
        Log.i(TAG, "notify time is:" + notifyTime);
        if (notifyTime == 0) {
           notifyTime = currentTime;
        }
        
        // set/get the periodic
        long periodic = mSharedPrefs.getLong(TRIGGER_PERIODIC, 0);
        Log.i(TAG, "the periodic is : " + periodic);
        if (0 == periodic) {
            notifyTime = currentTime;
            mEditor.putLong(NOTIFY_TIME , notifyTime);
            mEditor.commit();
            periodic = AlarmManager.INTERVAL_DAY;
            mEditor.putLong(TRIGGER_PERIODIC, periodic);
            mEditor.commit();
        }

        periodicTime = notifyTime + periodic;
        Log.i(TAG,"periodicTime is :" + periodicTime);

        if (periodicTime < currentTime) {
            Log.i(TAG, "over the notify time, reset the notify rtc");
            notifyTime = currentTime;
            periodic = AlarmManager.INTERVAL_DAY;
            mEditor.putLong(NOTIFY_TIME , notifyTime);
            mEditor.commit();
            mEditor.putLong(TRIGGER_PERIODIC, periodic);
            mEditor.commit();
            periodicTime = notifyTime + periodic;
        }

        AlarmManager alarm = (AlarmManager) mContext
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_ACTIVATE_PERIODIC);
        PendingIntent periodicIntent = PendingIntent.getBroadcast(mContext, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarm.set(AlarmManager.RTC, periodicTime, periodicIntent);
    }

    public void cancelActivateNotify() {
        AlarmManager alarm = (AlarmManager) mContext
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_ACTIVATE_PERIODIC);
        PendingIntent periodicIntent = PendingIntent.getBroadcast(mContext, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarm.cancel(periodicIntent);
    }

    public final String ship_sig = "308203e1308202c9a003020102020900c9c4074fbed8cffb300d06092a864886f70d0101050500308187310b300906035504061302434e310b300906035504080c02485a3116301406035504070c0d5a68656a69616e672056696577310f300d060355040a0c06416c6979756e310f300d060355040b0c06416c6979756e310f300d06035504030c06416c6979756e3120301e06092a864886f70d0109011611616c696f7340616c6979756e2e636f6d0d301e170d3132303130363133343830385a170d3339303532343133343830385a308187310b300906035504061302434e310b300906035504080c02485a3116301406035504070c0d5a68656a69616e672056696577310f300d060355040a0c06416c6979756e310f300d060355040b0c06416c6979756e310f300d06035504030c06416c6979756e3120301e06092a864886f70d0109011611616c696f7340616c6979756e2e636f6d0d30820120300d06092a864886f70d01010105000382010d00308201080282010100b7fccba0c933625a9384e6ce04c1e33299512f7b44e33127db60c3b30fb20aef0edb36c562b8ae3f9d3bda9dce55e8279df7e6009d9d5db0d8413f457c554826f399b357a6ddd33155f67a40cb1ca29b24af341a46bbe2b8451cb6446952ec24382754b2d2d0995565a89a6a0858f7541a310f8753b30fb9515ff4e8a345233e01d968c1220861e85b0bafde1c0f838a18ca416fec91d0181d039268f93a50e2571460b07103af5c431c8e0ce67e57f02f4c37700fe4a3048ee9b49b191732e2dbff5229d64fd9afda51d278d495941e34c2aec5bb355a49d48b7f9307cbbb1c377334aefb18af20116ed12e745bdea8a3b68ebc35976a37525db27a32d3e187020103a350304e301d0603551d0e041604141ce4c122f45ab42d18828568440f883fa97640d3301f0603551d230418301680141ce4c122f45ab42d18828568440f883fa97640d3300c0603551d13040530030101ff300d06092a864886f70d01010505000382010100a94d019960a5d144c6cc68f283d4ca1f02b6ddcad10da99157819756599cec9bf08f6a5fb737d718d7154e6ba108c603b6c0627df98215ad861fdff13aefc1cb2a6a8c253afbbefb0bf5f9c92c65161c2463d9b259eb7bafb983d187045fc2a8082c7234f9cdec78247ca61128e77ff12a87803aec17112c1f85427a5241e8e22cf529c54c420335e6cd09873c03510f83a6d938789c6570699f26de6d1b940ed1ed42b16e431bf91c6b7a3c33649315d400586ebd3d5aaa06fc4caada4383f8fc8648ef4b1be7c70d8284387e7e5453e0d46b04d261661038330ce026d69e2d63a9c60f083992a4fd9a4d91195e85b589d7104c6236d5c5126e1113a058288f";

    public String getSign(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packInfo = pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNATURES);
            return packInfo.signatures[0].toCharsString();
        } catch (NameNotFoundException e) {
            Log.e(TAG, "packageInfo not found:" + context.getPackageName());
        }

        return null;
    }
}
