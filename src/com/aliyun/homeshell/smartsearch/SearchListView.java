package com.aliyun.homeshell.smartsearch;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListAdapter;
import android.widget.ListView;

public class SearchListView extends ListView {

    public static interface OnSearchListSlideListener {
        void onSlideUp();
        void onSlideDown();
    }

    private static final int TH_SLIDE = 20;
    private static final int TH_SHOULD_DETECTED = 3;

    private OnSearchListSlideListener mOnSearchListSlideListener;
    private boolean mStartDetected;
    private int mStartPosition;

    public SearchListView(Context context) {
        super(context);
    }

    public SearchListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnSearchListSlideListener(OnSearchListSlideListener l) {
        mOnSearchListSlideListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter.getCount() > TH_SHOULD_DETECTED) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mStartDetected = true;
                    mStartPosition = (int) ev.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mOnSearchListSlideListener != null &&  mStartDetected) {
                        int diff = (int) ev.getY() - mStartPosition;
                        if (diff > TH_SLIDE) {
                            mOnSearchListSlideListener.onSlideDown();
                        } else if (diff < -TH_SLIDE) {
                            mOnSearchListSlideListener.onSlideUp();
                        }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mStartDetected = false;
                break;
            }
        }
        return super.onTouchEvent(ev);
    }

}
