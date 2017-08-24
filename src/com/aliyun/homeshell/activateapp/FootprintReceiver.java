package com.aliyun.homeshell.activateapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FootprintReceiver extends BroadcastReceiver {
    private static final String TAG = "Homeshell_FootprintReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // start ActivateAppService to handle the intent
        if (FootprintBase.getInstance().getActivateStatus() < FootprintBase.ACTIVATE_STATUS_ACTIVATE_DONE) {
            Intent serviceIntent = intent;
            serviceIntent.setClass(context, FootprintService.class);
            context.startService(serviceIntent);
        } else {
            Log.i(TAG, "it's alread activated");
        }
    }
}
