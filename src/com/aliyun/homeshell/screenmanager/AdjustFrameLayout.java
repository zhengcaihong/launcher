package com.aliyun.homeshell.screenmanager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class AdjustFrameLayout extends FrameLayout {

    public AdjustFrameLayout(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public AdjustFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public AdjustFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        View content = getChildAt(0);
        if(content != null) {
            int width = content.getWidth();
            int height = content.getHeight();
            float scaleX = (Const.CARD_WIDTH - Const.CARD_PADDING) / width;
            float scaleY = (Const.CARD_HEIGHT - Const.CARD_PADDING) / height;
            float scale  = scaleX < scaleY ? scaleX : scaleY;
            content.setScaleX(scale);
            content.setScaleY(scale);
            content.setPivotX(content.getWidth() / 2);
            content.setPivotY(content.getHeight() / 2);
            content.setX((getWidth() - content.getWidth())/2);
            content.setY((getHeight() - content.getHeight())/2);
        }
    }

}
