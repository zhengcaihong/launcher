package com.aliyun.homeshell;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;

public class LauncherRootView extends InsettableFrameLayout {
    public LauncherRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        Log.d(TAG, "fitSystemWindows insets" +insets );
        setInsets(insets);
        return true; // I'll take it from here
    }
}