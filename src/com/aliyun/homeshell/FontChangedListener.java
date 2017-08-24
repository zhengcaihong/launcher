package com.aliyun.homeshell;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class FontChangedListener extends BroadcastReceiver {

    public static interface IFontChanged {
        void onFontChanged();
    }

    private final static String ACTION_FONT_CHANGED = "com.aliyun.action.FONT_CHANGED";

    private List<IFontChanged> mListener;

    private static FontChangedListener mInstance = new FontChangedListener();

    public static FontChangedListener getInstance(Context context) {
        return mInstance;
    }

    public void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FONT_CHANGED);
        context.registerReceiver(this, filter);
    }

    public void unregister(Context context) {
        context.unregisterReceiver(this);
    }

    public void addListener(IFontChanged listener) {
        mListener.add(listener);
    }

    public void removeListener(IFontChanged listener) {
        mListener.remove(listener);
    }

    private FontChangedListener() {
        mListener = new ArrayList<IFontChanged>();
    }

    private void invoke() {
        for (int i = 0; i < mListener.size(); i++) {
            mListener.get(i).onFontChanged();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_FONT_CHANGED.equals(action)) {
            invoke();
        }
    }
}
