package com.aliyun.homeshell.widgetpage;

import android.graphics.drawable.Drawable;

public interface IAliWidgetPage {
    public static final int FLING_DIRECTION_UP = 1;
    public static final int FLING_DIRECTION_DOWN = 2;
    public static final int ICON_INDEX_NORMAL = 0;
    public static final int ICON_INDEX_FOCUS = 1;

    void onPause();

    void onResume();

    void onPageBeginMoving();

    void enterWidgetPage(int page);

    void leaveWidgetPage(int page);

    int getConsumedFlingDirection(int page);

    Drawable[] getIndicatorIcons(int page);
}
