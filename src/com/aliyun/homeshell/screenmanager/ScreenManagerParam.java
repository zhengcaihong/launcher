package com.aliyun.homeshell.screenmanager;

import android.content.Context;
import com.aliyun.homeshell.R;

abstract class ScreenManagerParam {

    protected final int SCREEN_CARD_WIDTH;
    protected final int SCREEN_CARD_HEIGHT;
    protected Context mContext;
    protected int mCenterX;
    protected int mCenterY;
    protected int mSize;
    protected int mCurrentScreen;

    ScreenManagerParam(Context context, int width,
            int height, int size, int current) {
        SCREEN_CARD_WIDTH = context.getResources()
                .getDimensionPixelSize(R.dimen.screen_edit_cell_width);
        SCREEN_CARD_HEIGHT = context.getResources()
                .getDimensionPixelSize(R.dimen.screen_edit_cell_height);
        mContext = context;
        mCenterX = width / 2;
        mCenterY = Const.PADDIND_TOP + (height - Const.PADDIND_TOP) / 2;
        mSize = size;
        mCurrentScreen = current;
    }

    abstract int getX(int index);
    abstract int getY(int index);

    int getW(int index) {
        return SCREEN_CARD_WIDTH;
    }

    int getH(int index) {
        return SCREEN_CARD_HEIGHT;
    }

    int getSize() {
        return mSize;
    }

    int getCurrentScreen() {
        return mCurrentScreen;
    }
}