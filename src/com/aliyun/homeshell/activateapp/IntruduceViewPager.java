package com.aliyun.homeshell.activateapp;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class IntruduceViewPager extends ViewPager {

    public IntruduceViewPager(Context context) {
        super(context);
    }

    public IntruduceViewPager(Context context, AttributeSet atts) {
        super(context, atts);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.onTouchEvent(event);
    }
}
