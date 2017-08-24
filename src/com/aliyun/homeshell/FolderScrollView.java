
package com.aliyun.homeshell;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class FolderScrollView extends ScrollView {
    private boolean mDisableScroll;

    public FolderScrollView(Context context) {
        super(context);
    }

    public FolderScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean awakenScrollBars(int startDelay, boolean invalidate) {
        return super.awakenScrollBars(startDelay, invalidate);
    }

    public void setDisableScroll(boolean disable) {
        mDisableScroll = disable;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return mDisableScroll ? false : super.onTouchEvent(ev);
    }
}
