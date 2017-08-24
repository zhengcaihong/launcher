package com.aliyun.homeshell.gadgets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;

public class GadgetsMainActivity extends Activity {

    private static final String ACTION_REQUEST_ACCELERATION = "aliyun.intent.action.REQUEST_ACCELERATION";
    private static final String EXTRA_PACKAGE = "package";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int gadgetType = getIntent().getIntExtra(GadgetsConsts.TYPE_SHORTCUT, GadgetsConsts.TYPE_ERROE);
        switch (gadgetType) {
            case GadgetsConsts.TYPE_ONE_KEY_LOCK:
                handleOneKeyLock();
                break;
            case GadgetsConsts.TYPE_ONE_KEY_ACCELERATE:
                handleOneKeyAccelerate();
                break;
            default:
                break;
        }
        finish();
    }

    private void handleOneKeyAccelerate() {
        Intent intent = new Intent();
        intent.setAction(ACTION_REQUEST_ACCELERATION);
        intent.putExtra(EXTRA_PACKAGE, getApplicationInfo().packageName);
        sendBroadcast(intent);
    }

    private void handleOneKeyLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        try {
            pm.goToSleep(SystemClock.uptimeMillis());
        } catch (SecurityException ex) {
        }
    }

}
