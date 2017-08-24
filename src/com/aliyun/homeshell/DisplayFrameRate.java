package com.aliyun.homeshell;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;

import java.util.Arrays;


public class DisplayFrameRate {

    static final int FPS_CHANGE_INTERVAL = 200 * 1000 * 1000; // NanoSec
    static final boolean CONTINUE_INVALIDATE = true;
    static final int FONT_SIZE = 24;

    static private long sDrawTime;
    static private int sDrawCount;
    static StringBuilder sb = new StringBuilder();
    static Paint mPaint = new Paint();
    static View mRootView;

    static {
        mPaint.setColor(Color.BLACK);
        mPaint.setTextSize(FONT_SIZE);
    }

    static View generateFpsView(final Context context) {
        return new View(context) {
            @Override
            public void draw(Canvas canvas) {
                if (sDrawTime == 0) {
                    Log.d("TestHelper", "canvas HardwareAcceleration:");
                }
                deliverDraw(false);

                float width = mPaint.measureText(sb, 0, sb.length());

                int left = 0;
                int top = 0;
                int right = (int) (left + width);
                int bottom = top + FONT_SIZE;

                mPaint.setColor(Color.WHITE);
                canvas.drawRect(left, top, right + 2, bottom + 2, mPaint);
                mPaint.setColor(Color.BLACK);
                canvas.drawText(sb, 0, sb.length(), left, bottom, mPaint);

                if (CONTINUE_INVALIDATE && ((PowerManager)getContext().getSystemService(Context.POWER_SERVICE)).isScreenOn()) {
                    invalidate();
                }
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                mRootView = this.getRootView();
                setMeasuredDimension(FONT_SIZE * 3, FONT_SIZE * 2);
            }
        };
    }

    static private int deliverDraw(boolean isLog) {
        long now = System.nanoTime();
        int fps = -1;
        sDrawCount++;
        boolean testedFps = false;
        if (sDrawTime != 0) {
            int interval = (int) (now - sDrawTime);

            if (interval > FPS_CHANGE_INTERVAL) {
                sb.delete(0, sb.length());

                testedFps = true;
                fps = (int) (1000000000L * sDrawCount / interval);
                sb.append(fps);

                sDrawCount = 0;

                if (isLog) {
                    Log.d("FPS", Integer.toString(fps));
                }
            }
        }

        long checkTime = System.nanoTime() - now;

        if (sDrawTime == 0 || testedFps)
            sDrawTime = now;
        sDrawTime += checkTime;

        return fps;
    }

    static public void logd(String tag, Object... nameAndValues) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nameAndValues.length; i++) {
            Object obj = nameAndValues[i];
            if (obj instanceof int[]) {
                sb.append(Arrays.toString((int[]) obj));
            } else {
                sb.append(obj);
            }
            if (i != nameAndValues.length - 1) {
                boolean isName = (i % 2 == 0);
                sb.append(isName ? "=" : ", ");
            }
        }
        Log.d(tag, sb.toString());
    }
}
