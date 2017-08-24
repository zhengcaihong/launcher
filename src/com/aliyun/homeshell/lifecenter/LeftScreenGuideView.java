package com.aliyun.homeshell.lifecenter;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LeftScreenGuideView extends FrameLayout {
    private static String TAG = "LeftScreenGuideView";
    private static String GUIDE = "guide";
    private static String SHOW_GUIDE = "show_leftscreen_guide";
    private static int ANI_DURATION = 400;

    private Drawable mDrawable = null;
    public LeftScreenGuideView(Context context) {
        super(context);
    }

    public LeftScreenGuideView(Context context, Drawable d) {
        super(context);
        mDrawable = d;
        build(context);
    }

    public static boolean isShow(Context context) {
        SharedPreferences sp = context.getSharedPreferences(GUIDE,
                Context.MODE_PRIVATE);
        return sp.getInt(SHOW_GUIDE, 1) == 1;
    }

    public static void update(Context context, boolean isShow) {
        Log.d(TAG, "update isShow : " + isShow);

        int value = isShow ? 1 : 0;
        SharedPreferences sp = context.getSharedPreferences(GUIDE,
                Context.MODE_PRIVATE);
        int v = sp.getInt(SHOW_GUIDE, 1);
        if (v == value) {
            return;
        }

        Editor e = sp.edit();
        e.putInt(SHOW_GUIDE, value);
        e.commit();
    }

    private void build(Context context) {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 2; i++) {
            TextView tv = getTextView(context);
            ll.addView(tv);
        }

        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.LEFT | Gravity.CENTER;
        addView(ll, lp);

        mParent = ll;

        mAnimRun = new Runnable() {
            @Override
            public void run() {
                final int N = mParent.getChildCount();
                int W = mParent.getWidth();
                for (int i = 0; i < N; i++) {
                    final TextView tv = (TextView) mParent.getChildAt(i);
                    int l = tv.getLeft();
                    int w = tv.getWidth();
                    AnimatorSet as = new AnimatorSet();
                    List<Animator> list = new ArrayList<Animator>();
                    final int index = i;

                    if (l + w >= W) {
                        // back to first.
                        list.add(ObjectAnimator.ofFloat(tv, "translationX", 0, -W + w));
                        //list.add(ObjectAnimator.ofFloat(tv, "alpha", 255, 125));
                    } else {
                        // move to next.
                        list.add(ObjectAnimator.ofFloat(tv, "translationX", 0, w));
                        //list.add(ObjectAnimator.ofFloat(tv, "alpha", 125, 255));
                    }

                    as.addListener(new AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (index == N -1) {
                                if (!stop) {
                                    postDelayed(mAnimRun, ANI_DURATION + 50);
                                }
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }
                    });

                    as.setInterpolator(new DecelerateInterpolator());
                    as.setDuration(ANI_DURATION);
                    as.playTogether(list);
                    as.start();
                }
            }
        };
    }

    private TextView getTextView(Context context) {
        TextView tv = new TextView(context);
        if (mDrawable != null) {
            tv.setBackground(mDrawable);
        } else {
            tv.setText("> ");
            tv.setTextColor(Color.WHITE);
        }

        return tv;
    }

    private ViewGroup mParent;
    private Runnable mAnimRun;
    private boolean stop = false;

    private void startAnimation() {
        stop = false;
        postDelayed(mAnimRun, ANI_DURATION);
    }

    private void stopAnimation() {
        stop = true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }
}
