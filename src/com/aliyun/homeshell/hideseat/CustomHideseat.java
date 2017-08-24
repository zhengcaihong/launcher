package com.aliyun.homeshell.hideseat;


import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.PageIndicatorView;
import com.aliyun.homeshell.R;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

public class CustomHideseat extends FrameLayout {

    private int mCustomHideseatHeight;

    public CustomHideseat(Context context) {
        this(context, null);
    }

    public CustomHideseat(Context context, AttributeSet attr) {
        this(context, attr, -1);
    }
    
    public CustomHideseat(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Hideseat hideseat = (Hideseat) findViewById(R.id.hideseat);

        PageIndicatorView indicator = (PageIndicatorView) findViewById(R.id.hideseat_pageindicator);
        indicator.setNeedLine(false);
        hideseat.setPageIndicator(indicator);
        
        TextView hintView = (TextView) findViewById(R.id.cling_hint);
        hideseat.setHintView(hintView);

        /*YUNOS BEGIN*/
        //##date:2014/10/29 ##author:zhangqiang.zq
        // aged mode
        if (AgedModeUtil.isAgedMode()) {
            getLayoutParams().height = getResources()
                    .getDimensionPixelSize(R.dimen.workspace_cell_height_3_3);
        }
        /*YUNOS END*/
    }

    /* YUNOS BEGIN */
    // ##date:2015-1-16 ##author:zhanggong.zg ##BugID:5712973
    // clips the contents below hide-seat during animation
    private int mVerticalClip; // 0: no clip;
                               // 1 ~ hideseat.height: clipped

    @Override
    public void draw(Canvas canvas) {
        if (mVerticalClip == 0) {
            super.draw(canvas);
        } else {
            canvas.save();
            canvas.clipRect(0, 0, getWidth(), getHeight() - mVerticalClip);
            super.draw(canvas);
            canvas.restore();
        }
    }

    public void setVerticalClip(int value) {
        if (mVerticalClip != value) {
            mVerticalClip = value;
            invalidate();
        }
    }
    /*YUNOS END*/

    public int getCustomeHideseatHeight() {
        if (AgedModeUtil.isAgedMode()) {
            return getResources().getDimensionPixelSize(R.dimen.workspace_cell_height_3_3);
        } else {
            return getResources().getDimensionPixelSize(R.dimen.hideseat_height);
        }
    }

}
