package com.aliyun.homeshell.screenmanager;

import java.util.List;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class ScreenCardView extends FrameLayout {

    private List<View> mViews;
    private static Paint mLayerPaint = new Paint();

    static {
        mLayerPaint.setAntiAlias(true);
        mLayerPaint.setFilterBitmap(true);
    }

    public ScreenCardView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, mLayerPaint);
    }

    public ScreenCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, mLayerPaint);
    }

    public ScreenCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayerType(LAYER_TYPE_SOFTWARE, mLayerPaint);
    }

    public void open(List<View> views) {
        mViews = views;
        int count = views.size();
        count = count > Const.MAX_CARDS ? Const.MAX_CARDS : count;
        for (int i = count - 1; i >= 0; i--) {
            View v = views.get(i);
            if (v == null) {
                Log.e(Const.TAG, "ScreenCard:open, Empty view" + i);
                continue;
            }
            if (i == count - 1 && i != 0) {
                v.setAlpha(0.5f);
            }
            v.setLayoutParams(new LayoutParams((int)Const.CARD_WIDTH,
                    (int)Const.CARD_HEIGHT));
            v.setTranslationX(Const.FIRST_CARD_OFFSET_X - Const.RIGHT_TRANS[i]);
            v.setTranslationY(Const.FIRST_CARD_OFFSET_Y + Const.DOWN_TRANS[i]);
            v.setRotation(-Const.ROTATES[i]);
            v.setLayerType(LAYER_TYPE_SOFTWARE, mLayerPaint);
            addView(v);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    public void close() {
        removeAllViews();
        for (View v : mViews) {
            v.setAlpha(1.0f);
            v.setTranslationX(0);
            v.setTranslationY(0);
            v.setRotation(0);
            v.setLayerType(LAYER_TYPE_HARDWARE, null);
        }
        mViews = null;
    }

}
