package com.aliyun.homeshell.gadgets;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;

import com.aliyun.homeshell.FastBitmapDrawable;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;

/**
 * A drawable that draws big card icon, which is composed of a
 * gradient background and a small icon on it.
 * @author zhanggong.zg
 */
public final class GadgetsCardIconDrawable extends FastBitmapDrawable {

    private int mColor;
    private int mWidth, mHeight;
    private float mCornerRadius;
    private RectF mBounds;

    private Paint mBgPaint;         // used to draw background
    private Paint mIconPaint;       // used to draw small icon

    private final Drawable mCardBorder;
    public GadgetsCardIconDrawable(Resources res, int color,
                            int width, int height, int topPadding) {
        super(null);
        mColor = color;
        mWidth = width;
        mHeight = height;
        mCornerRadius = res.getDimensionPixelSize(R.dimen.card_icon_corner_radius);
        mBounds = new RectF(0, 0, width, height);

        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        mCardBorder = LauncherApplication.getLauncher().getIconManager().getCardBorder();
        mBgPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_ATOP));
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        final int left = bounds.left;
        final int top = bounds.top;
        final int width = bounds.width();
        final int height = bounds.height();
        if (left == 0 && top == 0 && width == mWidth && height == mHeight) {
            // original size
            drawCardIcon(canvas);
        } else {
            // scale to bounds
            final float sx = width / (float) mWidth;
            final float sy = height / (float) mHeight;
            canvas.save();
            canvas.translate(left, top);
            canvas.scale(sx, sy);
            drawCardIcon(canvas);
            canvas.restore();
        }
    }

    private void drawCardIcon(Canvas canvas) {
        final Drawable cardmask = LauncherApplication.getLauncher().getIconManager().getCardMask();
        if(cardmask != null){
            cardmask.setBounds(0, 0, mWidth, mHeight);
            cardmask.draw(canvas);
        }
        mBgPaint.setColor(mColor);
        canvas.drawRoundRect(mBounds, mCornerRadius, mCornerRadius, mBgPaint);

        if(mCardBorder != null){
            mCardBorder.setBounds(0, 0, mWidth, mHeight);
            mCardBorder.draw(canvas);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }

    @Override
    public int getMinimumWidth() {
        return mWidth;
    }

    @Override
    public int getMinimumHeight() {
        return mHeight;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mBgPaint.setColorFilter(cf);
        mIconPaint.setColorFilter(cf);
    }

    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        mBgPaint.setAlpha(alpha);
        mIconPaint.setAlpha(alpha);
    }

    public void setFilterBitmap(boolean filterBitmap) {
        mBgPaint.setFilterBitmap(filterBitmap);
        mIconPaint.setFilterBitmap(filterBitmap);
    }

    @Deprecated
    @Override
    public Bitmap getBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    @Deprecated
    public void setBitmap(Bitmap b) {
        throw new UnsupportedOperationException();
    }

}
