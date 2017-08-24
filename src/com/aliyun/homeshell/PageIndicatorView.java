
package com.aliyun.homeshell;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;

public class PageIndicatorView extends View {

    private final Bitmap mDefault;
    private final Bitmap mFocus;

    private int mMax;
    private int mPos;
    private int mPrePos;
    private int mPointGap;
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private int mLineTop;
    private int mLinePadding;
    private ValueAnimator mSwitchAnimator;
    private float mAnimationProgress = 1;
    private boolean needLine = false;
    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##author:yongxing.lyx
    // ##BugID:(5735133) ##date:2015/3/3
    // ##description: add indicator icon for widget page.
    private Launcher mLauncher;

    /* YUNOS BEGIN PB */

    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##author:yongxing.lyx
    // ##BugID:(5735133) ##date:2015/3/3
    // ##description: add indicator icon for widget page.
    public PageIndicatorView(Context context) {
        super(context, null);
        mLauncher = (Launcher) context;
    }

    public PageIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        mLauncher = (Launcher) context;
    }

    public PageIndicatorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = (Launcher) context;
    }

    /* YUNOS END PB */

    {
        mDefault = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.homeshell_page_indicator_default);
        mFocus = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.homeshell_page_indicator_focused);
        mPaint.setColor(0xffffffff);
        mLineTop = mDefault.getHeight() / 2;
        mPaint.setStrokeWidth(getResources().getDisplayMetrics().density);
        mLinePadding = mDefault.getWidth();
        mPointGap = getResources().getDimensionPixelSize(R.dimen.page_indicator_padding_left);
    }

    public void setNeedLine(boolean need) {
        needLine = need;
    }

    public void setMax(int max) {
        mMax = max;
    }

    public int getMax() {
        return mMax;
    }

    public void setCurrentPos(int pos) {
        if (mPos != pos) {
            mPrePos = mPos;
            mPos = pos;
            startSwitchAnimator();
        }
    }

    private void startSwitchAnimator() {
        cancelSwitchAnimator();
        mSwitchAnimator = ValueAnimator.ofFloat(0, 1f);
        mSwitchAnimator.setDuration(200);
        mSwitchAnimator.addUpdateListener(mAnimatorUpdateListener);
        mSwitchAnimator.addListener(mAnimatorListener);
        mSwitchAnimator.start();
    }

    private ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            mAnimationProgress = (Float) animation.getAnimatedValue();
            invalidate();
        }
    };

    private AnimatorListener mAnimatorListener = new AnimatorListener() {
        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
        }

        public void onAnimationCancel(Animator animation) {
        }
    };

    public void cancelSwitchAnimator() {
        if (mSwitchAnimator != null && mSwitchAnimator.isRunning()) {
            mSwitchAnimator.cancel();
        }
        mSwitchAnimator = null;
    }

    public int getCurrentPos() {
        return mPos;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mDefault != null) {
            int h = Math.max(mDefault.getHeight(), mFocus.getHeight());
            setMinimumHeight(h);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDefault == null || mFocus == null || mMax <= 0) {
            return;
        }
        int padding = mLinePadding;

        /* YUNOS BEGIN */
        // ##module(Hideseat)
        // ##date:2014/4/2 ##author:wenliang.dwl@aliyun-inc.com ##BugId:106698
        // due to size changes of indicator images, we need to add mPointGap
        // between two points
        int left = (getWidth() - mMax * mDefault.getWidth() - mPointGap * (mMax - 1)) / 2;
        /* YUNOS END */
        final int hDefault = (getHeight() - mDefault.getHeight()) / 2;
        final int hFocus = (getHeight() - mFocus.getHeight()) / 2;
        int lineTop = mLineTop + hDefault;
        mPaint.setAlpha(0x10);
        if (needLine) {
            canvas.drawLine(0, lineTop, left - padding, lineTop, mPaint);
        }
        float p = mAnimationProgress;
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##author:yongxing.lyx
        // ##BugID:(5735133) ##date:2015/3/3
        // ##description: add indicator icon for widget page.
        for (int i = 0; i < mMax; i++) {
            Bitmap defaultBmp = mDefault;
            Bitmap focusBmp = mFocus;
            CellLayout child = null;
            child = (CellLayout) mLauncher.getWorkspace().getChildAt(i);
            if (this.getId() == R.id.pageindicator_view && child != null && Workspace.isWidgetPageView(child)) {
                BitmapDrawable d = (BitmapDrawable) child.getWidgetPageInfo().getIndicatorIcon();
                if (d != null) {
                    defaultBmp = d.getBitmap();
                }
                d = (BitmapDrawable) child.getWidgetPageInfo().getIndicatorIconFocus();
                if (d != null) {
                    focusBmp = d.getBitmap();
                }
            }
            if (i != mPos)
                if (i != mPrePos) {
                    mPaint.setAlpha(0xff);
                    canvas.drawBitmap(defaultBmp, left, hDefault, null);
                } else {
                    mPaint.setAlpha((int) (0xff * p));
                    canvas.drawBitmap(defaultBmp, left, hDefault, mPaint);
                    mPaint.setAlpha((int) (0xff * (1 - p)));
                    canvas.drawBitmap(focusBmp, left, hFocus, mPaint);
                }
            else {
                mPaint.setAlpha((int) (0xff * (1 - p)));
                canvas.drawBitmap(defaultBmp, left, hDefault, mPaint);
                mPaint.setAlpha((int) (0xff * p));
                canvas.drawBitmap(focusBmp, left, hFocus, mPaint);
            }
            left += defaultBmp.getWidth();
            /* YUNOS END PB */
            if (i != mMax - 1) {
                // (by wenliang.dwl,BugID:106698) add mPointGap between points
                left += mPointGap;
            }
        }
        mPaint.setAlpha(0x10);
        if (needLine) {
            canvas.drawLine(left + padding, lineTop, getWidth(), lineTop, mPaint);
        }
    }

}
