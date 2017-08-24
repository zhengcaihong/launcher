package com.aliyun.homeshell.icon;

import static com.aliyun.homeshell.icon.BubbleResources.*;


import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.CellLayout.Mode;
import com.aliyun.homeshell.CheckLongPressHelper;
import com.aliyun.homeshell.CheckVoiceCommandPressHelper;
import com.aliyun.homeshell.FastBitmapDrawable;
import com.aliyun.homeshell.FolderInfo;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.appclone.AppCloneManager;
import com.aliyun.homeshell.gadgets.HomeShellGadgetsRender;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.setting.HomeShellSetting;
import com.aliyun.utility.FeatureUtility;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;
import app.aliyun.aml.FancyDrawable;

public class BubbleTextView extends TextView {

    static final String TAG = "BubbleTextView";
    static final float LARGE_FONT_SCALE = 1.12f;

    //// Enumerations ////
    public enum LayoutStyle { WorkspaceStyle, HotseatStyle }
    public enum MaskType { NoMask, DownloadMask, PauseMask, OneKeyAccelerate }


    //// Drawing ////
    private Drawable mBackground;
    private Drawable mCardBackground;
    private boolean mSupportCard;
    private boolean mBackgroundSizeChanged;
    private int mTitleColor = 0;
    private Paint mTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mNewMarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    //// Layout Style ////
    private CellLayout.Mode mMode = Mode.NORMAL;
    private LayoutStyle mLayoutStyle = LayoutStyle.WorkspaceStyle;
    private boolean mIsTempPadding = false;
    private int mTempPaddingInHotseat = 0;
    private int mTextPaddingBottom;


    //// Touch Events Handling ////
    private float mMotionDownY, mMotionDownX;
    private CheckLongPressHelper mLongPressHelper;
    private boolean mShowClickEffect = false;


    //// Download Progress ////
    private float mDownloadProgress = PROGRESS_NOT_DOWNLOAD;
    private MaskType mMaskType = MaskType.NoMask;


    //// Corner Mark ////
    private boolean mCornerMarkVisible = false;
    private int mCornerMarkNumber = 0;


    //// Indicator (new mark or card mark) ////
    private Bitmap mImgIndicator = null;
    private StringBuilder mDecorateSpaces;
    private int mTitleHeight;
    private boolean mIndicatorVisible = true;

    //// Turn to Black & White ////
    private ColorMatrixColorFilter mAnimatedIconFadingFilter = null;
    private boolean mFadingEffectEnable = false;
    private Launcher mLauncher;
    private int mIconTitleColor;


    private int mAppCloneMarkIndex = -1;
    private Bitmap mAppCloneMarkIcon;

    public BubbleTextView(Context context) {
        super(context);
        init();
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        Resources res = getContext().getResources();
        ensureInited(getContext());

        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setGravity(Gravity.CENTER_HORIZONTAL);

        mLongPressHelper = new CheckLongPressHelper(this);
        mBackground = getBackground();
        mTitlePaint.setColor(Color.WHITE);
        mTitlePaint.setTextSize(getTextSize());
        mNumberPaint.setColor(Color.WHITE);
        mNewMarkPaint.setColor(Color.WHITE);

        computeTextPaddingBottom();
    }

    //// Properties ////

    public CellLayout.Mode getMode() {
        return mMode;
    }

    void setMode(CellLayout.Mode mode) {
        if (mode == null) mode = Mode.NORMAL;
        this.mMode = mode;
    }

    public void setSupportCard(boolean value) {
        mSupportCard = value;
    }

    public boolean setCardBackground(Drawable mCardBackground) {
        if (this.mCardBackground != mCardBackground) {
            if (this.mCardBackground != null) {
                this.mCardBackground.setCallback(null);
            }
            this.mCardBackground = mCardBackground;
            if (this.mCardBackground != null) {
                this.mCardBackground.setCallback(this);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean hasIndicator() {
        return mImgIndicator != null;
    }

    public Bitmap getIndicator() {
        return mImgIndicator;
    }

    public boolean setIndicator(Bitmap bitmap) {
        if (this.mImgIndicator != bitmap) {
            this.mImgIndicator = bitmap;
            calcEnoughSpapce();
            calcTitleHeight();
            invalidate();
            return true;
        } else {
            return false;
        }
    }

    public float getDownloadProgress() {
        return mDownloadProgress;
    }

    public void setDownloadProgress(float mProgress) {
        this.mDownloadProgress = mProgress;
        invalidate();
    }

    public void setMask(MaskType maskType) {
        this.mMaskType = maskType;
        invalidate();
    }

    public boolean isCornerMarkVisible() {
        return mCornerMarkVisible;
    }

    public void setCornerMarkVisible(boolean mCornerMarkVisible) {
        this.mCornerMarkVisible = mCornerMarkVisible;
    }

    public void setIndicatorVisible(boolean indicatorVisible) {
        mIndicatorVisible = indicatorVisible;
    }

    public void setAppCloneMarkIcon(int index) {
        if (index < AppCloneManager.MAX_APP_CLONE_COUNT) {
            mAppCloneMarkIndex = index;
            invalidate();
        }
    }

    public boolean isIndicatorVisible() {
        return mIndicatorVisible;
    }

    public int getCornerMarkNumber() {
        return mCornerMarkNumber;
    }

    public void setCornerMarkNumber(int mCornerMarkNumber) {
        this.mCornerMarkNumber = mCornerMarkNumber;
        invalidate();
    }

    public LayoutStyle getLayoutStyle() {
        return mLayoutStyle;
    }

    public void setLayoutStyle(LayoutStyle mLayoutStyle) {
        this.mLayoutStyle = mLayoutStyle;
        invalidate();
    }

    public Drawable getIconDrawable() {
        return mLayoutStyle == LayoutStyle.HotseatStyle ?
                getCompoundDrawables()[1] : mCardBackground;
    }

    public Drawable getIconDrawingCache() {
        Drawable icon = null;
        if (mSupportCard) {
            icon = new FastBitmapDrawable(getDrawingCache());
        } else {
            icon = this.getCompoundDrawables()[1];
        }
        return icon;
    }

    public void setTitle(String text) {
        //BugID:5808688 reset padding while move from hotseat or hideseat
        if(mLayoutStyle != LayoutStyle.HotseatStyle) {
            Resources res = getContext().getResources();
            int paddingLeft = res.getDimensionPixelSize(R.dimen.bubble_textview_padding_left);
            int paddingRight = res.getDimensionPixelSize(R.dimen.bubble_textview_padding_right);
            if(getPaddingLeft() != paddingLeft) {
                setPadding(paddingLeft, getPaddingTop(), paddingRight, getPaddingBottom());
            }
        }

        int textWidth = getTextWidth(text);
        boolean isExcessed = isTextWidthExcessed(textWidth);
        if(isExcessed && hasIndicator()) {
            setText(mDecorateSpaces + text);
        } else {
            setText(text);
        }
    }

    public boolean isFadingEffectEnable() {
        return mFadingEffectEnable;
    }

    public void setFadingEffectEnable(boolean on, boolean animated) {
        if (mFadingEffectEnable && !on) {
            mFadingEffectEnable = false;
            unapplyFadingEffectInHideseat();
        } else if (!mFadingEffectEnable && on) {
            mFadingEffectEnable = true;
            applyFadingEffectInHideseat();
        }
        invalidate();
    }

    public void setShowClickEffect(boolean showClickEffect) {
        mShowClickEffect = showClickEffect;
    }

    public int getTitleColor() {
        return mTitleColor;
    }

    public void setTitleColor(int titleColor) {
        this.mTitleColor = titleColor;
    }

    public void setDisableLabel(boolean disable) {
        setTextColor(disable ? getResources().getColor(
                android.R.color.transparent) : mTitleColor);
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        mTitlePaint.setTextSize(getTextSize());
    }

    //// Drawing ////

    @Override
    public void draw(Canvas canvas) {
        boolean isInHotseatOrHideseat = (mLayoutStyle == LayoutStyle.HotseatStyle);

        // (by wenliang.dwl & zhanggong.zg)
        // update downloading icon mask, and hide-seat fading mask
        updateTopCompoundDrawableFilter();

        // by xiaodong.lxd update icon click effect
        updateViewClickEffect();

        // draw card background
        final Drawable cardbg = mCardBackground;
        if (cardbg != null && mLayoutStyle == LayoutStyle.WorkspaceStyle) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            cardbg.setBounds(0, 0, getWidth(), getHeight());
            if ((scrollX | scrollY) == 0) {
                cardbg.draw(canvas);
            } else {
                canvas.translate(scrollX, scrollY);
                cardbg.draw(canvas);
                canvas.translate(-scrollX, -scrollY);
            }
        }

        boolean drawText = true;
        // If text is transparent, don't draw any shadow
        if (getCurrentTextColor() == getResources().getColor(
                android.R.color.transparent)) {
            getPaint().clearShadowLayer();
            super.draw(canvas);
            drawText = false;
        }

        if (drawText) {
            updateTextColor();
            super.draw(canvas);
        }

        // draw base image
        final Paint paint = mNumberPaint;
        if (mFadingEffectEnable) {
            paint.setColorFilter(FADING_EFFECT_FILTER);
        } else {
            paint.setColorFilter(null);
        }
        int alpha = paint.getAlpha();

        final Drawable background = mBackground;
        if (background != null) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();

            if (mBackgroundSizeChanged) {
                background.setBounds(0, 0, getRight() - getLeft(), getBottom() - getTop());
                mBackgroundSizeChanged = false;
            }

            if ((scrollX | scrollY) == 0) {
                background.draw(canvas);
                if (mAppCloneMarkIndex >= 0
                        && (mAppCloneMarkIcon = getAppCloneMarkIcon(getResources(),
                                mAppCloneMarkIndex)) != null) {
                    int appCloneMarkX = getCardIconAppCloneMarkX(getContext());
                    int appCloneMarkY = getCardIconAppCloneMarkY(getContext());
                    if (!mSupportCard || (mMode == Mode.HIDESEAT || mMode == Mode.HOTSEAT)) {
                        appCloneMarkX = getIconAppCloneMarkX(getContext(), mMode);
                        appCloneMarkY = getIconAppCloneMarkY(getContext(), mMode) + getPaddingTop();
                    }
                    canvas.drawBitmap(mAppCloneMarkIcon, appCloneMarkX, appCloneMarkY,
                            mNewMarkPaint);
                }
            } else {
                canvas.translate(scrollX, scrollY);
                background.draw(canvas);

                boolean inDownload = false;
                // When downloading stopped
                if( mMaskType == MaskType.PauseMask ){
                    inDownload = true;
                    drawDownloadIconMaskIfNeeded(canvas, isInHotseatOrHideseat);
                    drawCircle(canvas, mDownloadProgress, isInHotseatOrHideseat);
                    drawPauseImage(canvas, isInHotseatOrHideseat);
                }

                // When in downloading and install process
                if( mMaskType == MaskType.DownloadMask ) {
                    inDownload = true;
                    drawDownloadIconMaskIfNeeded(canvas, isInHotseatOrHideseat);
                    drawCircle(canvas, mDownloadProgress, isInHotseatOrHideseat);
                }

                if (mMaskType == MaskType.OneKeyAccelerate) {
                    HomeShellGadgetsRender.getRender().drawAccelerateStatus(getContext(), canvas,
                            getWidth(), getHeight(), mMode == Mode.HOTSEAT, mSupportCard);
                }

                if (!inDownload && mIndicatorVisible && hasIndicator()) {
                    drawIndicator(canvas, paint, isInHotseatOrHideseat);
                }

                if (mCornerMarkVisible && mCornerMarkNumber > 0) {
                    INDICATOR_BOUNDRY_Y = 0;

                    if (mIsTempPadding && isInHotseatOrHideseat) {
                        INDICATOR_BOUNDRY_X -= mTempPaddingInHotseat;
                    }

                    canvas.drawBitmap(mImgRTCorner, INDICATOR_BOUNDRY_X + (mMode == Mode.HOTSEAT ? sAtomSiblingOffset : 0),
                            INDICATOR_BOUNDRY_Y, paint);
                    final String finalStr = getMessageNumString(mCornerMarkNumber);
                    Point p = calcStringPosition(finalStr, IND_NUM_SIZE_NORMAL, mImgRTCorner, mNewMarkPaint);
                    canvas.drawText(finalStr, p.x + (mMode == Mode.HOTSEAT ? sAtomSiblingOffset : 0), p.y, mNewMarkPaint);

                    if (mIsTempPadding && isInHotseatOrHideseat) {
                        INDICATOR_BOUNDRY_X += mTempPaddingInHotseat;
                    }
                }

                if (mAppCloneMarkIndex >= 0
                        && (mAppCloneMarkIcon = getAppCloneMarkIcon(getResources(),
                                mAppCloneMarkIndex)) != null) {
                    int appCloneMarkX = getCardIconAppCloneMarkX(getContext());
                    int appCloneMarkY = getCardIconAppCloneMarkY(getContext());
                    if (!mSupportCard || (mMode == Mode.HIDESEAT || mMode == Mode.HOTSEAT)) {
                        appCloneMarkX = getIconAppCloneMarkX(getContext(), mMode);
                        appCloneMarkY = getIconAppCloneMarkY(getContext(), mMode) + getPaddingTop();
                    }
                    canvas.drawBitmap(mAppCloneMarkIcon, appCloneMarkX, appCloneMarkY,
                            mNewMarkPaint);
                }

                paint.setAlpha(alpha);
                canvas.translate(-scrollX, -scrollY);
            }
        }
    }

    /**
     * Updates color filter of the icon according to current state:
     * <p>
     * <ul>
     * <li>if it's downloading, put black mask on top of compoundDrawable (app icon)
     * <li>if it's frozen app, put fading filter on top of compoundDrawable
     * <li>otherwise clear the filter
     * </ul>
     * <p>
     * <strong>Note that</strong> this method does not work for <code>FancyDrawable</code>.
     * For <code>FancyDrawable</code>, see {@link #applyFadingEffectInHideseat()}.
     * @author zhanggong.zg
     * @author wenliang.dwl
     */
    private void updateTopCompoundDrawableFilter() {
        Drawable d = getCompoundDrawables()[1];
        if (d == null) return;
        // For fancy icon, see applyFadingEffectInHideseat()
        if (d instanceof FancyDrawable) return;
        // For special bitmap icon (e.g. EditFolderShortcut), no need to apply filter
        if (d instanceof BitmapDrawable) return;

        /* YUNOS BEGIN */
        // ##date:2014/9/3 ##author:zhanggong.zg ##BugID:5244146
        // fade the icon when it's in hide-seat
        if (mAnimatedIconFadingFilter != null && mFadingEffectEnable) {
            d.setColorFilter(mAnimatedIconFadingFilter);
        } else if (mFadingEffectEnable) {
            d.setColorFilter(FADING_EFFECT_FILTER);
            d.setAlpha(FADING_EFFECT_ALPHA);
        /* YUNOS END */

        } else if (mMaskType != MaskType.NoMask) {
            // if it's downloading, put black mask on top compoundDrawable(app icon)
            d.setColorFilter(0x7f000000, PorterDuff.Mode.SRC_ATOP);
        } else {
            d.clearColorFilter();
            if (d.getAlpha() != 255) d.setAlpha(255);
        }
    }

    private void updateViewClickEffect() {
        Drawable d = null;
        if (mSupportCard) {
            d = mCardBackground;
        } else {
            d = getCompoundDrawables()[1];
        }

        if (d == null) {
            return;
        }

        if (mMaskType == MaskType.NoMask) {
            if (mShowClickEffect) {
                d.setColorFilter(0x88000000, PorterDuff.Mode.SRC_ATOP);
                mShowClickEffect = false;
            } else {
                d.clearColorFilter();
            }
        }
    }

    private void drawIndicator(Canvas canvas, Paint paint, boolean isInHotseatOrHideseat) {
        if (!hasIndicator()) {
            return;
        }

        // set color filter for dynamic text color
        ItemInfo info = (ItemInfo) getTag();
        ColorFilter cFilter = paint.getColorFilter();
        if (info != null && info.isNewItem() && LauncherModel.isShowNewMarkIcon() || mSupportCard) {
            paint.setColorFilter(cFilter);
        } else {
            paint.setColorFilter(new PorterDuffColorFilter(mIconTitleColor, PorterDuff.Mode.SRC_ATOP));
        }
        Bitmap imgIndicator = getIndicator();
        int indicatorPadding = IconIndicator.getIndicatorPadding();

        float x = 0;
        float y = getExtendedPaddingTop() + (mTitleHeight  - imgIndicator.getHeight()) / 2 ;

        Pair<Float, Float> adhocPos = null;
        if (mSupportCard &&
            (adhocPos = IconIndicator.getAdhocIndicatorPosition(this)) != null) {
            x = BUBBLE_WIDTH * adhocPos.first - imgIndicator.getWidth() / 2;
            y = BUBBLE_HEIGHT * adhocPos.second - imgIndicator.getHeight() / 2;
        } else {
            int textWidth = getTextWidth(getText().toString());
            if (isInHotseatOrHideseat) {
                x = BUBBLE_WIDTH_IN_HOTSEAT / 2 - textWidth / 2 - imgIndicator.getWidth() - indicatorPadding;
            } else {
                x = isTextWidthExcessed(textWidth) ? 0 : Math.abs(BUBBLE_WIDTH - textWidth) / 2 - imgIndicator.getWidth() - indicatorPadding;
            }
            x = x < 0 ? 0 : x;
        }

        canvas.drawBitmap(imgIndicator, x, y, paint);
        paint.setColorFilter(cFilter);
    }

    /**
     * Draw downloading mask above app icon
     * @param canvas {@link Canvas} which passed by draw(Canvas canvas
     * @author wenliang.dwl
     */
    private void drawDownloadIconMaskIfNeeded(Canvas canvas, boolean isInHotseatOrHideseat){
        //draw download icon mask
        if(mSupportCard && !isInHotseatOrHideseat){
            IconManager iconManager = LauncherApplication.getLauncher().getIconManager();
            Drawable downloadmask = iconManager.getDownloadCardMask();
            if (downloadmask != null) {
                downloadmask.setBounds(0, 0, getWidth(), getHeight());
                downloadmask.draw(canvas);
            }
        }
    }

    /**
     * When downloading process was pause, draw pause image in center
     * @param canvas {@link Canvas} which passed by draw(Canvas canvas)
     * @author wenliang.dwl
     */
    private void drawPauseImage(Canvas canvas, boolean isInHotseatOrHideseat){
        int point[] = getAppIconCenter(isInHotseatOrHideseat);
        int centerX = point[0];
        int centerY = point[1];
        float left = centerX - sImgPause.getWidth()/2;
        float top  = centerY - sImgPause.getHeight()/2;
        canvas.drawBitmap(sImgPause,left,top,null);
    }

    /**
     * Draw circle when downloading apps
     * @author wenliang.dwl
     */
    private void drawCircle(Canvas canvas, float progress, boolean isInHotseatOrHideseat){
        int circleBackColor  = 0x3fffffff;//25% alpha of Color.WHITE
        int circleFrontColor = 0xffffffff;//Color.WHITE
        int point[] = getAppIconCenter(isInHotseatOrHideseat);
        int centerX = point[0];
        int centerY = point[1];
        int roundHeadRadius = DOWNLOAD_ARC_WIDTH / 2;
        float sweep = Math.min(360, 360 * progress / 100);
        canvas.save();
        RectF rect = new RectF(centerX - DOWNLOAD_ARC_DRAW_R,
                               centerY - DOWNLOAD_ARC_DRAW_R,
                               centerX + DOWNLOAD_ARC_DRAW_R,
                               centerY + DOWNLOAD_ARC_DRAW_R);
        drawHoloSector(canvas, rect, 0, 360, circleBackColor);
        if ( progress != PROGRESS_NOT_DOWNLOAD ) {
            drawHoloSector(canvas, rect, 270, sweep, circleFrontColor);
            drawRoundHead(canvas, centerX, centerY, 270, roundHeadRadius, circleFrontColor);
            drawRoundHead(canvas, centerX, centerY, 270 + sweep, roundHeadRadius, circleFrontColor);
        }
        canvas.restore();
    }

    /**
     * Make the downloading circle have a round head
     * @param canvas the Canvas will be drawn on
     * @param centerX the big circle center x coordinate
     * @param centerY the big circle center y coordinate
     * @param degree the degree from x-axis positive direction
     * @param radius the radius of the round head
     * @param color the color of the round head
     */
    private void drawRoundHead(Canvas canvas, int centerX, int centerY, float degree, int radius, int color){
        double radian = degree * Math.PI / 180;
        double drawX = centerX + Math.cos(radian) * DOWNLOAD_ARC_DRAW_R;
        double drawY = centerY + Math.sin(radian) * DOWNLOAD_ARC_DRAW_R;
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        canvas.drawCircle((float)drawX, (float)drawY, radius, paint);
    }

    private void drawHoloSector(Canvas canvas, RectF oval, float startAngle,
            float sweepAngle, int color) {
        final Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(DOWNLOAD_ARC_WIDTH);
        canvas.drawArc(oval, startAngle, sweepAngle, false, paint);
    }

    public void drawBubbleTextViewInFolderIcon(int width, int height, Canvas canvas) {
        ShortcutInfo info = (ShortcutInfo) getTag();
        IconManager iconManager = ((LauncherApplication)getContext().getApplicationContext()).getIconManager();

        Drawable cardbg = mCardBackground;
        boolean isOnKeyAccelerate = HomeShellGadgetsRender.isOneKeyAccerateShortCut(info);

        if (!mSupportCard) {
            if (isOnKeyAccelerate) {
                HomeShellGadgetsRender.getRender().drawAccelerateStatusInFolder(getContext(), canvas, width, height, false);
                return;
            } else {
                if (info.itemType != LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                    cardbg = iconManager.getAppUnifiedIcon(info, null);
                } else {
                    cardbg = info.mIcon;
                    if (cardbg == null) {
                        cardbg = iconManager.getAppUnifiedIcon(info, null);
                    }
                }
            }
        }

        if (cardbg != null) {
            cardbg.clearColorFilter();
            Drawable.Callback callback = cardbg.getCallback();
            cardbg.setCallback(null);
            Rect bounds = cardbg.copyBounds();
            cardbg.setBounds(0, 0, width, height);
            if (cardbg instanceof CardIconDrawable) {
                ((CardIconDrawable)cardbg).drawInFolder(canvas);
            } else {
                cardbg.draw(canvas);
            }
            // restore the state of drawable
            cardbg.setBounds(bounds);
            cardbg.setCallback(callback);
            if (isOnKeyAccelerate) {
                HomeShellGadgetsRender.getRender().drawAccelerateStatusInFolder(getContext(), canvas, width, height, true);
            }
            /* YUNOS BEGIN */
            //## modules(Home Shell)
            //## date: 2016/03/18 ## author: wangye.wy
            //## BugID: 8022449: draw icon on background card
            if (mSupportCard && HomeShellGadgetsRender.isOneKeyLockShortCut(info)) {
                int w = info.mIcon.getIntrinsicWidth();
                bounds = info.mIcon.copyBounds();
                info.mIcon.setBounds((width - w) / 2, bounds.top, (width - w) / 2 + w, bounds.bottom);
                info.mIcon.draw(canvas);
                info.mIcon.setBounds(bounds);
            }
            /* YUNOS END */
        }

        // draw text when view hasn't been attached
        if (mSupportCard && getCurrentTextColor() != getResources().getColor(
                android.R.color.transparent)) {
            getPaint().setColor(iconManager.getTitleColor(info));
            int index = getPaint().breakText(getText().toString(), true, width, null);
            String text = getText().subSequence(0, index).toString();

            float extendedPaddingTop = height - 18 * getResources().getDisplayMetrics().density;
            float posX = (width - getPaint().measureText(text)) / 2;
            float posY = extendedPaddingTop + mTextPaddingBottom * 2.2f;
            //BugID:8286384/8433006:Adjust text top padding by font scale.
            float fontScale = getResources().getConfiguration().fontScale;
            if (fontScale >= LARGE_FONT_SCALE) {
                posY += BUBBLE_TEXT_BOTTOM_PADDING * fontScale;
            }
            canvas.drawText(text, posX, posY, getPaint());
        }
        android.util.Log.i(TAG, "info:"+info.title + ", mAppCloneMarkIndex:"+mAppCloneMarkIndex);
        if (mAppCloneMarkIndex >= 0
                && (mAppCloneMarkIcon = getAppCloneMarkIcon(getResources(),
                        mAppCloneMarkIndex)) != null) {
            int appCloneMarkX = getCardIconAppCloneMarkX(getContext());
            int appCloneMarkY = getCardIconAppCloneMarkY(getContext());
            if (!mSupportCard || (mMode == Mode.HIDESEAT || mMode == Mode.HOTSEAT)) {
                appCloneMarkX = getIconAppCloneMarkX(getContext(), mMode);
                appCloneMarkY = getIconAppCloneMarkY(getContext(), mMode) + getPaddingTop();
            }
            canvas.drawBitmap(mAppCloneMarkIcon, appCloneMarkX, appCloneMarkY,
                    mNewMarkPaint);
        }
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        // override to extract text label from fancy drawable
        ShortcutInfo mShortcutInfo = (ShortcutInfo) getTag();
        boolean isInHotseatOrHideseat = mMode == Mode.HIDESEAT || mMode == Mode.HOTSEAT;

        /* YUNOS BEGIN */
        // ##date:2014/12/19 ##author:zhanggong.zg ##BugID:5581407,5587224
        Drawable icon = getIconDrawable();
        /* YUNOS END */
        if (icon != drawable) {
            super.invalidateDrawable(drawable);
            return;
        }
        if (drawable instanceof FancyDrawable) {
            String label = ((FancyDrawable) drawable).getVariableString("app_label");
            if (label != null) {
                // ##date:2015/1/30 ##author:zhanggong.zg ##BugID:5751734
                if (mShortcutInfo != null) {
                    BubbleController.updateIndicator(this);
                    BubbleController.updateTitle(this);
                } else {
                    setText(label);
                }
            } else if (mShortcutInfo != null) {
                BubbleController.updateIndicator(this);
                BubbleController.updateTitle(this);
            }
        }
        if (getWindowToken() == null ) {
            FolderInfo info = Launcher.findFolderInfo(mShortcutInfo.container);
            if (info != null)
                info.invalidate(this, mShortcutInfo);
        } else if (mSupportCard && !isInHotseatOrHideseat && mCardBackground != null) {
            invalidate();
        } else if (drawable instanceof FancyDrawable) {
            // ##date:2014/12/19 ##author:zhanggong.zg ##BugID:5587224
            invalidate();
        } else {
            super.invalidateDrawable(drawable);
        }
    }

    @Override
    public void postInvalidate() {
        ShortcutInfo mShortcutInfo = (ShortcutInfo) getTag();
        if (getWindowToken() == null && mShortcutInfo != null) {
            FolderInfo info = Launcher.findFolderInfo(mShortcutInfo.container);
            if (info != null)
                info.invalidate(this, mShortcutInfo);
        } else {
            super.postInvalidate();
        }
    }

    /**
     * Apply fading animation when drop an icon to hide-seat.
     * ##date:2014/9/13 ##author:zhanggong.zg ##BugID:5244146
     */
    public void applyFadingEffectInHideseat() {
        final Drawable drawable = getIconDrawable();
        if (drawable == null) return;
        /* YUNOS BEGIN */
        //## modules(Home Shell)
        //## date: 2015/12/25 ## author: wangye.wy
        //## BugID: 7721715: set alpha of whole layer
        Paint paint = new Paint();
        paint.setColorFilter(FADING_EFFECT_FILTER);
        paint.setAlpha(FADING_EFFECT_ALPHA);
        setLayerType(LAYER_TYPE_HARDWARE, paint);
    /*
        if (drawable instanceof FancyDrawable) {
            // for fancy icon, turn into gray immediately
            Paint paint = new Paint();
            paint.setColorFilter(FADING_EFFECT_FILTER);
            paint.setAlpha(FADING_EFFECT_ALPHA);
            setLayerType(LAYER_TYPE_HARDWARE, paint);
            return;
        }
        // for normal bitmap icon, play a transition animation
        final AnimatorSet as = new AnimatorSet();
        final ValueAnimator va = ValueAnimator.ofFloat(1f, 0f);
        va.setDuration(350);
        final ColorMatrix matrix = new ColorMatrix();
        va.addUpdateListener(new AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) animation.getAnimatedValue()).floatValue();
                matrix.setSaturation(value);
                mAnimatedIconFadingFilter = new ColorMatrixColorFilter(matrix);
                drawable.setAlpha(255 - (int) ((255 - FADING_EFFECT_ALPHA) * (1 - value)));
                invalidate();
            }
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator arg0) {
                mAnimatedIconFadingFilter = null;
                drawable.setAlpha(FADING_EFFECT_ALPHA);
                invalidate();
            }
            @Override
            public void onAnimationCancel(Animator arg0) {
                onAnimationEnd(arg0);
            }
        });
        as.play(va);
        as.setStartDelay(400);
        matrix.setSaturation(1);
        mAnimatedIconFadingFilter = new ColorMatrixColorFilter(matrix);
        drawable.setAlpha(255);
        invalidate();
        as.start();
    */
        /* YUNOS END */
    }

    // ##date:2014/9/16 ##author:zhanggong.zg ##BugID:5244146
    private void unapplyFadingEffectInHideseat() {
        final Drawable drawable = getIconDrawable();
        // Only fancy icons need to explicitly unapply the fading effect.
        if (drawable instanceof FancyDrawable) {
            setLayerType(LAYER_TYPE_HARDWARE, null);
        }
        invalidate();
    }

    //// Layout ////

    private void calcEnoughSpapce() {
        if (mDecorateSpaces == null) {
            mDecorateSpaces = new StringBuilder();
        }
        mDecorateSpaces.delete(0, mDecorateSpaces.length());

        if (!hasIndicator()) {
            return;
        }

        int imgWidth = getIndicator().getWidth();
        Paint paint = new Paint();
        paint.setTextSize(getTextSize());
        paint.setTypeface(getTypeface());
        while (true) {
            mDecorateSpaces.append(" ");
            float spaceWidth = paint.measureText(mDecorateSpaces.toString());
            if (spaceWidth >= imgWidth) {
                break;
            }
        }
    }

    private void calcTitleHeight() {
        Paint p = new Paint();
        p.setTextSize(getTextSize());
        FontMetrics fm = p.getFontMetrics();
        mTitleHeight = (int) (Math.ceil(fm.descent - fm.ascent));
    }

    @Override
    public int getExtendedPaddingTop() {
        // getHeight maybe 0, if bug caused by getHeight() is 0 in this method
        // the private method getExtendedPaddingTop(int top, int bottom)
        return getExtendedPaddingTop(0, getHeight());
    }

    private int getExtendedPaddingTop(int top, int bottom) {
        return bottom - top - getLayout().getLineTop(1) - computeTextPaddingBottom();
    }

    /**
     * Used to determine the bottom padding of text.
     */
    private int computeTextPaddingBottom() {
        boolean isInWorkspace = (mLayoutStyle == LayoutStyle.WorkspaceStyle);

        int standardPadding = !mSupportCard && isInWorkspace ?
                BUBBLE_TEXT_BOTTOM_PADDING_SMALL : BUBBLE_TEXT_BOTTOM_PADDING;

        final Resources res = getContext().getResources();
        float density = res.getDisplayMetrics().density;
        float fontScale = res.getConfiguration().fontScale;

        // adjust the padding for different font size:
        // standard text size = 10sp
        mTextPaddingBottom = standardPadding - (int) (density * (10 * (fontScale - 1.0f)));
        return mTextPaddingBottom;
    }

    /**
     * Calculate the position where string will be drawn
     * @return the (x,y) relative to text baseline
     */
    private Point calcStringPosition(String str, int textSize, Bitmap bg,
            Paint paint) {
        final Point tempPos = new Point();
        if (str == null) {
            tempPos.x = -1;
            tempPos.y = -1;
            return tempPos;
        }

        Rect rect = new Rect();
        paint.setTextSize(textSize);
        /*
         * If you are confused with Paint.getTextBounds,read the link below:
         * {@link http://stackoverflow.com/questions/7549182/android-paint-measuretext-vs-gettextbounds }
         */
        paint.getTextBounds(str, 0, str.length(), rect);

        int leftTopPointX = (bg.getWidth()  - rect.width() ) / 2 + INDICATOR_BOUNDRY_X;
        int leftTopPointY = (bg.getHeight() - rect.height()) / 2 + INDICATOR_BOUNDRY_Y;

        tempPos.x = leftTopPointX - rect.left;
        tempPos.y = leftTopPointY - rect.top;

        return tempPos;
    }

    /* YUNOS BEGIN */
    // ##date:2014/07/30 ##author:hongchao.ghc ##BugID:137835
    public void setTempPadding(int left) {
        if (!mIsTempPadding) {
            //mOldPaddingLeft = getPaddingLeft();
            //mOldPaddingRight = getPaddingRight();
            mTempPaddingInHotseat = left;
            mIsTempPadding = true;
        }
//        setPadding(left, top, right, bottom);
    }

    public void resetTempPadding() {
        if (mIsTempPadding) {
//            setPadding(mOldPaddingLeft, getPaddingTop(), mOldPaddingRight, getPaddingBottom());
            mTempPaddingInHotseat = 0;
        }
        mIsTempPadding = false;
    }

    private int getOneBlackStringWidth() {
        return getTextWidth(" ");
    }

    private int getTextWidth(String text) {
        return (int) mTitlePaint.measureText(text);
    }

    private boolean isTextWidthExcessed(int textWidth) {
        Bitmap imgIndicator = getIndicator();
        int indicatorPadding = IconIndicator.getIndicatorPadding();
        int indicator_width = imgIndicator == null ? 0 : imgIndicator.getWidth();
        if (mLayoutStyle == LayoutStyle.HotseatStyle) {
            return (textWidth + indicator_width + indicatorPadding + getPaddingLeft()) > BUBBLE_WIDTH_IN_HOTSEAT;
        } else {
            int diff = (textWidth + indicator_width + getPaddingLeft() + getPaddingRight()) - BUBBLE_WIDTH;
            return diff > getOneBlackStringWidth() / 2;
        }
    }

    /**
     * Find the application icon's center position (if you want draw sth around it e.g.)
     * @return the x and y Axis of the app icon center
     * @author wenliang.dwl
     */
    private int[] getAppIconCenter(boolean isInHotseatOrHideseat){
        if (mSupportCard && !isInHotseatOrHideseat){
            return new int[] {getWidth()/2,getHeight()/2};
        }else{
            int iconHeight = getCompoundPaddingTop() - getCompoundDrawablePadding() - getPaddingTop();
            if (AgedModeUtil.isAgedMode()) {
                iconHeight *= AgedModeUtil.SCALE_RATIO_FOR_AGED_MODE;
            }
            int centerY = getPaddingTop() + iconHeight / 2;
            int centerX = getWidth() / 2;
            return new int[] { centerX, centerY };
        }
    }

    /**
     * Because onThemeChange, we use the BubbleTextView has been created and do not create a new one,
     * so the padding will be reset to 0
     *
     */
    public void preThemeChange() {
        setPadding(getPaddingLeft(), 0, getPaddingRight(), 0);
    }

    //// Delegation ////

    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (getLeft() != left || getRight() != right || getTop() != top
                || getBottom() != bottom) {
            mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mBackground || super.verifyDrawable(who);
    }

    @Override
    protected void drawableStateChanged() {
        Drawable d = mBackground;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
        super.drawableStateChanged();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // this switch is for fling up gesture and avoid OnClickListener.onClick()
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mMotionDownY = event.getRawY();
                mMotionDownX = event.getRawX();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float deltaY = 0;
                int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                /* YUNOS BEGIN */
                // ##date:2014/09/3 ##author:xindong.zxd ##BugId:5221424
                // getLauncher may return null pointer
                Launcher launcher = LauncherApplication.getLauncher();
                if (launcher != null
                /* YUNOS END */
                        &&FeatureUtility.hasBigCardFeature() && getVisibility() == View.VISIBLE &&
                        launcher.getGestureLayer().getPointerCount() <= 1 &&
                        !launcher.getWorkspace().isPageMoving() &&
                        (deltaY = mMotionDownY - event.getRawY()) > slop * 2 &&
                        Math.abs(mMotionDownX - event.getRawX()) <  deltaY) {

                    cancelLongPress();
                    launcher.onIconFlingUp(this);
                    event.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(event);
                    return true;
                }
                break;
            default:
                break;
        }

        // Call the superclass onTouchEvent first, because sometimes it changes
        // the state to
        // isPressed() on an ACTION_UP
        boolean result = super.onTouchEvent(event);

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            // So that the pressed outline is visible immediately when
            // isPressed() is true,
            // we pre-create it on ACTION_DOWN (it takes a small but perceptible
            // amount of time
            // to create it)
            /*
             * commented by xiaodong.lxd if (mPressedOrFocusedBackground ==
             * null) { mPressedOrFocusedBackground =
             * createGlowingOutline(mTempCanvas, mPressedGlowColor,
             * mPressedOutlineColor); }
             */
            // Invalidate so the pressed state is visible, or set a flag so we
            // know that we
            // have to call invalidate as soon as the state is "pressed"
            /* YUNOS BEGIN added by xiaodong.lxd for push to talk */
            if (CheckVoiceCommandPressHelper.isPushTalkCanUse()) {
                CheckVoiceCommandPressHelper.getInstance().postCheckVoiceCommand(this);
            }
            /* YUNOS END */
            mLongPressHelper.postCheckForLongPress();
            break;
        case MotionEvent.ACTION_UP:
            /* YUNOS BEGIN added by xiaodong.lxd for push to talk */
            if (CheckVoiceCommandPressHelper.isPushTalkCanUse()) {
                CheckVoiceCommandPressHelper.getInstance().cancelCheckedVoiceCommand();
            }
            /* YUNOS END */
        case MotionEvent.ACTION_CANCEL:
            // If we've touched down and up on an item, and it's still not
            // "pressed", then
            // destroy the pressed outline
            if (!isPressed()) {
            }

            mLongPressHelper.cancelLongPress();
            break;
        }
        return result;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mLongPressHelper.cancelLongPress();
    }

    //// Helper Methods ////

    public void shake(){
        ObjectAnimator.ofFloat(this, "translationY", 0, 25, -25, 25, -25,15, -15, 0).start();
    }

    private String getMessageNumString(int num) {
        if (num <= 0) return null;
        else if (num > 99) return "99+";
        else return String.valueOf(num);
    }

    // need to update bubble text view text color when wallpaper is different, or text
    // is not clear in wallpaper.
    private void updateTextColor() {
        if (mLauncher == null) {
            return;
        }
        ItemInfo info = (ItemInfo) getTag();
        if (info == null || mSupportCard) {
            return;
        }
        int color = getDyncTitleColor(info);
        mIconTitleColor = color;
        setPaintShadowLayer(color);
        if (getPaint().getColor() != color) {
            setTextColor(color);
        }

    }

    private int getDyncTitleColor(ItemInfo info) {
        int color = IconUtils.TITLE_COLOR_WHITE;
        if (FeatureUtility.supportDyncColor() && HomeShellSetting.getIconDyncColor(getContext())) {
            switch ((int) info.container) {
                case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                    color = mLauncher.getTitleColorManager().getWorkSpaceTitleColor(info);
                    break;
                case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                    color = mLauncher.getTitleColorManager().getHotSeatTitleColor(info.cellX);
                    break;
                default:
                    break;
            }
        }
        return color;
    }

    public void setPaintShadowLayer(int color) {
        int shadowColor = getShadowColor();
        if (mIconTitleColor == IconUtils.TITLE_COLOR_WHITE) {
            shadowColor = Color.parseColor("#ff333333");
        } else {
            shadowColor = Color.parseColor("#ffcccccc");
        }
        getPaint().setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, shadowColor);
    }

    public void setup(Launcher launcher) {
        mLauncher = launcher;
    }

}
