package com.aliyun.homeshell;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.widget.ImageView;

public class DeleteTrashView extends ImageView {

    private Bitmap mBitmap;
    private Paint mPaint;
    private PaintFlagsDrawFilter mPaintFilter;
    private float degree;
    private float translationY;
    private float translationX;

    public DeleteTrashView(Context context) {
        this(context, null);
    }
    public DeleteTrashView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public DeleteTrashView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_trash_top);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaintFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        int imageViewHeight = getResources().getDimensionPixelSize(R.dimen.trashbin_top_image_height);
        translationY = Math.max(imageViewHeight - mBitmap.getHeight(), 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap == null) {
            return;
        }
        canvas.save();
        canvas.setDrawFilter(mPaintFilter);
        canvas.translate(translationX, translationY);
        canvas.rotate(degree);
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
        canvas.restore();
    }

    public void setDegree(float degree) {
        this.degree = degree;
    }
    public void setCanvasTranlationX(float translationX) {
        this.translationX = translationX;
    }

}
