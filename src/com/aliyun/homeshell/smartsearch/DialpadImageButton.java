package com.aliyun.homeshell.smartsearch;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;

public class DialpadImageButton extends ImageButton {
    private AccessibilityManager mAccessibilityManager;

    private Rect mHoverBounds = new Rect();

    public interface OnPressedListener {
        public void onPressed(View view, boolean pressed);
    }

    private OnPressedListener mOnPressedListener;

    public void setOnPressedListener(OnPressedListener onPressedListener) {
        mOnPressedListener = onPressedListener;
    }

    public DialpadImageButton(Context context) {
        super(context);
        initForAccessibility(context);
    }

    public DialpadImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initForAccessibility(context);
    }

    public DialpadImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initForAccessibility(context);
    }

    private void initForAccessibility(Context context) {
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(
                Context.ACCESSIBILITY_SERVICE);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (mOnPressedListener != null) {
            mOnPressedListener.onPressed(this, pressed);
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mHoverBounds.left = getPaddingLeft();
        mHoverBounds.right = w - getPaddingRight();
        mHoverBounds.top = getPaddingTop();
        mHoverBounds.bottom = h - getPaddingBottom();
    }

    @Override
    public boolean performClick() {
        if (mAccessibilityManager.isEnabled()) {
            if (!isPressed()) {
                setPressed(true);
                setPressed(false);
            }

            return true;
        }

        return super.performClick();
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (mAccessibilityManager.isEnabled()
                && mAccessibilityManager.isTouchExplorationEnabled()) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    setClickable(false);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    if (mHoverBounds.contains((int) event.getX(), (int) event.getY())) {
                        performClick();
                    }
                    setClickable(true);
                    break;
            }
        }

        return super.onHoverEvent(event);
    }
}
