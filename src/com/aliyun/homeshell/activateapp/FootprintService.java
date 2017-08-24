package com.aliyun.homeshell.activateapp;

import com.aliyun.homeshell.LauncherApplication;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.aliyun.homeshell.R;

public class FootprintService extends Service {
    private static final String TAG = "Homeshell_FootprintService";

    private static final String ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
    private static final String ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";

    public FootprintBase mFootprintBase = FootprintBase.getInstance();
    private Context mContext = LauncherApplication.getContext();
    String[] invalidCallNumber = mContext.getResources().getStringArray(
            R.array.invalid_call_number);
    private int mOldState = TelephonyManager.CALL_STATE_IDLE;
    private int mCurrentState = TelephonyManager.CALL_STATE_IDLE;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final String action = intent.getAction();

        // count the valid call
        if (action.equals(ACTION_PHONE_STATE)) {
            String number = intent
                    .getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            TelephonyManager telephony = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            mCurrentState = telephony.getCallState();
            mOldState = mFootprintBase.getCallState();
            if (mOldState == TelephonyManager.CALL_STATE_OFFHOOK
                    && mCurrentState == TelephonyManager.CALL_STATE_IDLE) {
                int index = -1;
                for (int i = 0; i < invalidCallNumber.length; i++) {
                    if (invalidCallNumber[i].equals(number)) {
                        Log.i(TAG, "the number is invalid: " + number);
                        index = i;
                        break;
                    }
                }
                if (-1 == index) {
                    mFootprintBase.addValidCall();
                    handleFootprint();
                }
            }
            mFootprintBase.setCallState(mCurrentState);
        }

        // handle the wifi connect status
        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo info = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                mFootprintBase.setWifiConnected(true);
                if (FootprintBase.ACTIVATE_STATUS_ACTIVATE_DONE != mFootprintBase.getActivateStatus()) {
                    Log.i(TAG, "reset triggerring the time");
                    mFootprintBase.triggerTiming();
                }
                handleFootprint();
            }
        }

        // get the network changes
        if (action.equals(ACTION_CONNECTIVITY_CHANGE)) {
            ConnectivityManager mConnMgr = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (mConnMgr != null) {
                NetworkInfo aActiveInfo = mConnMgr.getActiveNetworkInfo(); // get
                                                                           // the
                                                                           // network
                                                                           // connectivity
                                                                           // info
                if (aActiveInfo != null &&
                    FootprintBase.ACTIVATE_STATUS_ACTIVATE_DONE != mFootprintBase.getActivateStatus()) {
                    Log.i(TAG,
                            "try to trigger timing");
                    mFootprintBase.triggerTiming();
                }
            }
        }

        // get the trigger alarm
        if (action.equals(FootprintBase.ACTION_TRIGGER_EXPIRED)) {
            Log.i(TAG, "now the trigger time is expired");
            mFootprintBase.setTriggerExpired(true);
            handleFootprint();
        }

        // get the periodic alarm
        if (action.equals(FootprintBase.ACTION_ACTIVATE_PERIODIC)) {
            Log.i(TAG, "it's the periodic alarm to notify the user again");
            if (FootprintBase.ACTIVATE_STATUS_ACTIVATE_CANCEL == mFootprintBase.getActivateStatus()) {
                // reset the activate status, and handle footprint again
                mFootprintBase.setActivateStatus(FootprintBase.ACTIVATE_STATUS_NEED_ACTIVATE);
                handleFootprint();
            }
        }

        if (action.equals(FootprintBase.ACTION_ACTIVATE_TEST)) {
            Log.i(TAG , "just for testing");
            int calls = intent.getIntExtra("validcalls", -1);
            boolean expired = intent.getBooleanExtra("expired", false);
            if (calls > 0) {
               mFootprintBase.setValidCalls(calls);
            }
            mFootprintBase.setTriggerExpired(expired);
        }

        return START_NOT_STICKY;
    }

    public void handleFootprint() {
        int activateStatus = mFootprintBase.getActivateStatus();
        boolean wifiConnected = mFootprintBase.getWifiConnected();
        int validCallCount = mFootprintBase.getValidCall();
        boolean triggerExpired = mFootprintBase.getTriggerExpired();

        Log.i(TAG, "get the user footpring: " + "activateStatus = "
                + activateStatus + ", validCallCount = " + validCallCount
                + ", wifi_connected = " + wifiConnected
                + ", trigger expired = " + triggerExpired);

        if (FootprintBase.TRIGGER_CALL_NUM <= validCallCount && true == triggerExpired
                && true == wifiConnected) {
            if ( activateStatus == FootprintBase.ACTIVATE_STATUS_NOT_ACTIVATE ) {
               mFootprintBase.setActivateStatus(FootprintBase.ACTIVATE_STATUS_NEED_ACTIVATE);
               Log.i(TAG , "ready to activate");
            } else if ( activateStatus == FootprintBase.ACTIVATE_STATUS_ACTIVATE_CANCEL) {
               mFootprintBase.setActivateDelay();
               Log.i(TAG, "delay activate");
            }
        }
    }
}
