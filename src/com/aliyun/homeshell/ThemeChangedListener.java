package com.aliyun.homeshell;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ThemeChangedListener extends BroadcastReceiver {

    public static interface IThemeChanged {
        void onThemeChanged();
    }

    private final static String ACTION_THEME_CHANGED = "com.aliyun.homeshell.aciton.THEME_CHENGED";

    private List<IThemeChanged> mListener;

    private static ThemeChangedListener mInstance;

    public static ThemeChangedListener getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ThemeChangedListener();
        }

        return mInstance;
    }

    public void release() {
        /* YUNOS BEGIN */
        // ##date:2014/4/29 ##author:hongchao.ghc ##BugID:111144
        // mInstance = null;
        /* YUNOS END */

        mListener.clear();
    }

    public void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_THEME_CHANGED);

        context.registerReceiver(this, filter);
    }

    public void unregister(Context context) {
        context.unregisterReceiver(this);
    }

    public void addListener(IThemeChanged listener) {
        mListener.add(listener);
    }

    public void removeListener(IThemeChanged listener) {
        mListener.remove(listener);
    }

    private ThemeChangedListener() {
        mListener = new ArrayList<IThemeChanged>();
    }

    private void invoke() {
        for (int i = 0; i < mListener.size(); i++) {
            mListener.get(i).onThemeChanged();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String component = intent.getStringExtra("component");

        if (ACTION_THEME_CHANGED.equals(action)) {
            // "icon" comes first, then "trafficpanel"
            if ("trafficpanel".equals(component)) {
                // ignore
                // invokeTrafficpanel();
            } else if ( "icon".equals(component)) {
                invoke();
            }
        }
    }
}
