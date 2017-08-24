package com.aliyun.homeshell.editmode;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.aliyun.homeshell.R;

public class PreviewListIndicatorView extends View {
    
    private Bitmap mFocus;
    private Bitmap mDecoratedBitmap;
    private int mFocusWidth;
    private int mFocusHeight;
    
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private ValueAnimator mSwitchAnimator;
    
    private int mStart;
    private boolean mInLandOrientation;
    
    private static final String TAG = "PreviewListIndicatorView";
    
    public PreviewListIndicatorView(Context context) {
        super(context, null);
    }

    public PreviewListIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public PreviewListIndicatorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void init() {
        if(mFocus == null) {
            mFocus = BitmapFactory.decodeResource(getContext().getResources(),
                    R.drawable.scorbar);
        }
        mPaint.setColor(0xffffffff);
        mPaint.setStrokeWidth(getResources().getDisplayMetrics().density);
    }
    
    public void setLandMode(boolean landMode) {
        mInLandOrientation = landMode;
    }

    public void setFocusStart(int start) {
        if (start != mStart) {
            mStart = start;
            Log.d(TAG, "sxsexe_indi setFocusStart " + mStart);
        }
    }
    
    public int getFocusStart() {
        return mStart;
    }
    
    public void setFocusHeight(int height) {
        if (height != mFocusHeight && mFocus != null) {
            mFocusHeight = height;
            Bitmap bitmap = mDecoratedBitmap;
            float scale = mFocusHeight / mFocus.getHeight();
            Matrix matrix = new Matrix();
            matrix.postScale(2.0f, scale);
            mDecoratedBitmap = Bitmap.createBitmap(mFocus, 0, 0, mFocus.getWidth(), mFocus.getHeight(), matrix, true);

            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
        }
    }

    public void setFocusWidth(int width) {
        if (width != mFocusWidth && mFocus != null) {
            mFocusWidth = width;
            Bitmap bitmap = mDecoratedBitmap;
            float scale = mFocusWidth / mFocus.getWidth();
            Matrix matrix = new Matrix();
            matrix.postScale(scale, 2.0f);
            mDecoratedBitmap = Bitmap.createBitmap(mFocus, 0, 0, mFocus.getWidth(), mFocus.getHeight(), matrix, true);
            
            if(bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
        }
    }
    
    public void startSwitchAnimator() {
        cancelSwitchAnimator();
        mSwitchAnimator = ValueAnimator.ofFloat(0, 1f);
        mSwitchAnimator.setDuration(200);
        mSwitchAnimator.addUpdateListener(mAnimatorUpdateListener);
        mSwitchAnimator.addListener(mAnimatorListener);
        mSwitchAnimator.start();
    }
    
    private ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
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

    @Override
    protected void onDraw(Canvas canvas) {
        boolean b = mDecoratedBitmap == null;
        boolean c = b ? false : mDecoratedBitmap.isRecycled();
        if(b || c) {
            Log.d(TAG, "sxsexe_indi onDraw  b " + b + " c " + c);
            return;
        }
        if (mInLandOrientation) {
            canvas.drawBitmap(mDecoratedBitmap, 0, mStart, mPaint);
        } else {
            canvas.drawBitmap(mDecoratedBitmap, mStart, 0, mPaint);
        }
    }
    
    public void clearBitmapCache() {
        if(mDecoratedBitmap != null && !mDecoratedBitmap.isRecycled()) {
            mDecoratedBitmap.recycle();
            mDecoratedBitmap = null;
        }
        mFocusWidth = -1;
        mStart = -1;
    }
    
    public void onExit() {
        if(mFocus != null && !mFocus.isRecycled()) {
            mFocus.recycle();
            mFocus = null;
        }
        clearBitmapCache();
    }
}
