
package com.aliyun.homeshell.smartsearch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TableLayout;

public class DigitalView extends TableLayout {

    public DigitalView(Context context) {
        super(context);
    }

    public DigitalView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private MotionEvent downStart = null;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downStart = MotionEvent.obtain(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = ev.getX() - downStart.getX();
                if (Math.abs(deltaX) > ViewConfiguration.getTouchSlop() * 2) {
                    return true;
                }
                break;
            default:
                break;
        }

        return false;
    }
}
