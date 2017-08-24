package com.aliyun.homeshell.setting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aliyun.homeshell.PagedView;

public class ContinuousHomeShellReceiver extends BroadcastReceiver {

    public static final String CONTINUOUS_HOMESHELL_SHOW_KEY = "ContinuousHomeShellChecked";
    public static final String KEY_CONTINUOUS_HOMESHELL_STYLE = "continuous_homeshell";
    public static final String CONTINUOUS_HOMESHELL_SHOW_ACTION = "aliyun.settings.CONTINUOUS_HOMESHELL_SHOW_CHECKED";
    private SharedPreferences mSharedPreferences;

    public ContinuousHomeShellReceiver(Context mContext) {
        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(CONTINUOUS_HOMESHELL_SHOW_ACTION)) {
            boolean isContinuousHomeshellOn = intent.getBooleanExtra(
                    CONTINUOUS_HOMESHELL_SHOW_KEY, false);
            PagedView.sContinuousHomeShellFeature = isContinuousHomeshellOn;
            Log.d("ContinuousHomeShellReceiver","receive ContinuousHomeShellFeature is "+PagedView.sContinuousHomeShellFeature);
            if (mSharedPreferences != null) {
                SharedPreferences.Editor preEditor = mSharedPreferences.edit();
                preEditor.putBoolean(KEY_CONTINUOUS_HOMESHELL_STYLE,PagedView.sContinuousHomeShellFeature);
                preEditor.commit();
                Log.d("ContinuousHomeShellReceiver","setting ContinuousHomeShellFeature to "+PagedView.sContinuousHomeShellFeature);
            }
        }
    }
}

