package com.aliyun.homeshell.screenmanager;

import android.view.MotionEvent;
import android.view.View;

class ExchangeController {

    private boolean mIsDraging;
    private View mDragView;
    private static final int DRAG_OFFSET = 4;
    private float mMotionDownX;
    private float mMotionDownY;

    private ScreenManagerParam mParam;
    private float TH = 0.4f;
    private ExchangeScreenListener mExchangeScreenListener;
    private int mDropViewIndex = Const.IVALID_SCREEN_INDEX;
    private int mDragViewIndex;
    private float mX;
    private float mY;

    ExchangeController(ScreenManagerParam param) {
        mParam = param;
    }

    void setExchangeScreenListener(ExchangeScreenListener l) {
        mExchangeScreenListener = l;
    }

    void startDrag(View v, int index) {
        mIsDraging = true;
        mDragViewIndex = index;
        mDragView = v;
        mDragView.bringToFront();
        mX = v.getX();
        mY = v.getY();
        v.setX(v.getX() + DRAG_OFFSET);
        v.setY(v.getY() + DRAG_OFFSET);
    }

    boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
                mMotionDownX = ev.getX();
                mMotionDownY = ev.getY();
            break;
        case MotionEvent.ACTION_UP:
            break;
        case MotionEvent.ACTION_MOVE:
            break;
        case MotionEvent.ACTION_CANCEL:
            cancelDrag();
            break;
        }
        return mIsDraging;
    }

    boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            if (mIsDraging) {
                int index = handleMoveEvent(ev.getX(), ev.getY());
                mDropViewIndex = index;
                drop(index);
            }
            endDrag();
            break;
        case MotionEvent.ACTION_MOVE:
            if (mIsDraging) {
                handleMoveEvent(ev.getX(), ev.getY());
            }
            break;
        }
        return true;
    }

    private int handleMoveEvent(float x, float y) {
        float startX = x - mMotionDownX + mX;
        float startY = y - mMotionDownY + mY;
        mDragView.setX(startX + DRAG_OFFSET);
        mDragView.setY(startY + DRAG_OFFSET);

        int index = findDropIndex(startX, startY);
        if (index > Const.IVALID_SCREEN_INDEX && mDropViewIndex != index) {
            mDropViewIndex = index;
            mExchangeScreenListener.onDragOver(mDragViewIndex, mDropViewIndex);
            mDragViewIndex = index;
        }
        return index;
    }

    private int findDropIndex(float startX, float startY) {
        int dragX = mParam.getX(mDragViewIndex);
        int dragY = mParam.getY(mDragViewIndex);
        int dragW = mParam.getW(mDragViewIndex);
        int dragH = mParam.getH(mDragViewIndex);
        for (int i = 0; i < mParam.getSize(); i++) {
            int x = mParam.getX(i);
            int y = mParam.getY(i);
            if (x != dragX || y != dragY) {
                if (Math.abs(x - startX) < TH * dragW &&
                        Math.abs(y - startY) < TH * dragH) {
                    return i;
                }
            }
        }
        return Const.IVALID_SCREEN_INDEX;
    }

    private void cancelDrag() {
        mIsDraging = false;
        mDragView = null;
    }

    private void endDrag() {
        mIsDraging = false;
        mDragView = null;
        mDropViewIndex = Const.IVALID_SCREEN_INDEX;
    }

    private void drop(int index) {
        mExchangeScreenListener.onDrop(index);
    }
}
