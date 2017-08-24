package com.aliyun.homeshell.iconupdate;

import com.aliyun.homeshell.model.LauncherModel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class IconUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "IconUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "liuhao test receive icon update intent: ");
        final Intent finalIntent = intent;
        Runnable IURunnable = new Runnable() {
            @Override
            public void run() {
                IconUpdateManager.getInstance().handleIconUpdateInent(finalIntent);
            }
        };
        LauncherModel.runOnWorkerThread(IURunnable);
    }
}