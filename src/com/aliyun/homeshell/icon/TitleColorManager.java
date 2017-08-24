package com.aliyun.homeshell.icon;

import java.util.ArrayList;
import java.util.HashMap;

import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.Hotseat;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.icon.IconGenerator;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import com.aliyun.utility.FeatureUtility;

import com.aliyun.homeshell.R;
import com.aliyun.homeshell.setting.HomeShellSetting;

public class TitleColorManager {
    private static String TAG = "TitleColorManager";
    private HashMap<String, Integer> mIconMap;
    private ArrayList<Integer> mHotSeatArray;
    private WallpaperManager mWallpaperManager;
    private Context mContext;
    private int mDefaultTextColor;
    private int mStartX;
    private int mStartY;
    private int mWidthGap;
    private int mHeightGap;
    private int mHotSeatStartX;
    private int mHotSeatStartY;
    private int mHotSeatWidthGap;
    private int mHotSeatHeightGap;
    private int mTextHeight;
    private int mPaddingLeft;
    private int mIconWidth;
    private int mPaddingRight;
    private int mPaddingBottom;
    private int mHotSeatPadding;
    private int mHotSeatHeight;
    private int mStatusBarSize;
    private TextView mTitleView;
    private int mDeviceWidth;
    private boolean mSupportCard;
    private Paint mPaint;

    public TitleColorManager(Context context) {
        mContext = context;
        mIconMap = new HashMap<String, Integer>();
        mHotSeatArray = new ArrayList<Integer>();
        mWallpaperManager = WallpaperManager.getInstance(context);
        mDefaultTextColor = IconUtils.TITLE_COLOR_WHITE;

        mPaddingLeft = context.getResources().getDimensionPixelSize(
                R.dimen.bubble_textview_padding_left);
        mPaddingRight = context.getResources().getDimensionPixelSize(
                R.dimen.bubble_textview_padding_right);

        mTitleView = new TextView(context);

        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            mStatusBarSize = context.getResources().getDimensionPixelSize(resourceId);
        }
        mDeviceWidth = context.getResources().getDisplayMetrics().widthPixels;
        mPaint = new Paint();

    }

    public void supprtCardIcon(boolean supportCard){
        mSupportCard = supportCard;
    }

    /**
     * Called when wallpaper is changed, and need to update colors.
     */
    public void needUpdateColor() {
        Log.d(TAG, "needUpdateColor(): mSupportCard " + mSupportCard);
        if (!isSupportChangedColor()) {
            return;
        }
        if (mWallpaperManager == null
                || mWallpaperManager.getWallpaperInfo() != null) {
            resetDefaultColor();
            return;
        }

        updateIconColorMap();
        updateHotSeatColors();

    }

    /**
     * Get hot seat icon title color
     * @param index the index get from the hotseat item cellx.
     * @return title color white or black
     */
    public int getHotSeatTitleColor(int index) {

        if (mHotSeatArray.size() <= index) {
            return mDefaultTextColor;
        }
        return mHotSeatArray.get(index);
    }

    /**
     * Get the icon title color of the workspace in the desktop.
     * @param info itemInfo in the desktop icon
     * @return title color.
     */
    public int getWorkSpaceTitleColor(ItemInfo info) {

        if (info == null) {
            return mDefaultTextColor;
        }
        String key = String.valueOf(info.cellX) + String.valueOf(info.cellY);
        if (mIconMap.get(key) == null) {
            return mDefaultTextColor;
        }
        return mIconMap.get(key);

    }

    /**
     * Called when workspace layout changed
     * @param startX
     * @param startY
     * @param widthGap
     * @param heightGap
     */
    public void updateIconParams(int startX, int startY, int widthGap, int heightGap) {
        if (!isSupportChangedColor()) {
            return;
        }
        boolean isChanged = false;
        if (mStartX != startX) {
            mStartX = startX;
            isChanged = true;
        }

        if (mStartY != startY) {
            mStartY = startY;
            isChanged = true;
        }

        if (mWidthGap != widthGap) {
            mWidthGap = widthGap;
            isChanged = true;
        }

        if (mHeightGap != heightGap) {
            mHeightGap = heightGap;
            isChanged = true;
        }

        if (isChanged) {
            updateIconColorMap();
        }
    }

    /**
     * Update hot seat text color when hot seat layout paramers changed.
     * Called by {@link com.aliyun.homeshell.Hotseat#layout(int, int, int, int)}
     *
     * @param startX begin of x to compute text color
     * @param startY begin of y for bitmap rectangle
     * @param height hotseat cell layout height
     * @param widthGap the width gap between two hotseat cell.
     */
    public void updateHotSeatParams(int startX, int startY, int height, int widthGap) {
        if (!isSupportChangedColor()) {
            return;
        }

        boolean isChanged = mHotSeatStartX != startX || mHotSeatStartY != startY
                || mHotSeatWidthGap != widthGap || height != mHotSeatHeight;
        if (isChanged) {
            mHotSeatStartX = startX;
            mHotSeatStartY = startY;
            mHotSeatWidthGap = widthGap;
            mHotSeatHeight = height;
            if (mHotSeatHeight > 0) {
                updateHotSeatColors();
            }
        }
    }


    private void updateIconColorMap() {
        if (mWallpaperManager == null
                || mWallpaperManager.getWallpaperInfo() != null) {
            resetDefaultColor();
            return;
        }

        Bitmap wallpaper = getWallpaper();
        if (wallpaper == null) {
            resetDefaultColor();
            return;
        }

        mIconMap.clear();

        int width = BubbleResources.getIconWidth(mContext.getResources());
        int deltaH = BubbleResources.getIconHeight(mContext.getResources()) + mHeightGap;
        int textMaxWidth = width - mPaddingLeft - mPaddingRight;
        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                mContext.getResources().getDimension(R.dimen.workspace_icon_text_size));
        int titleH = calcTitleHeight(mTitleView);

        int paddingBottom = computeTextPaddingBottom(true);
        int iconExtendTop = BubbleResources.getIconHeight(mContext.getResources()) - paddingBottom
                - titleH;

        for (int x = 0; x < ConfigManager.getCellCountX(); x++) {
            int startX = mStartX + mPaddingLeft + x * (width + mWidthGap);
            for (int y = 0; y < ConfigManager.getCellCountY(); y++) {

                int startY = mStartY + iconExtendTop + y * deltaH;
                long color = IconGenerator.getBitmapAverageColor(wallpaper, startX, startY,
                        textMaxWidth, titleH, 1, 1);
                mIconMap.put(String.valueOf(x) + String.valueOf(y), IconUtils.getTitleColor((int) color));

            }
        }
        wallpaper = null;
    }

    private void updateHotSeatColors() {
        if (mWallpaperManager == null
                || mWallpaperManager.getWallpaperInfo() != null) {
            resetDefaultColor();
            return;
        }

        Bitmap wallpaper = getWallpaper();

        if (wallpaper == null) {
            resetDefaultColor();
            return;
        }

        mHotSeatArray.clear();

        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                mContext.getResources().getDimension(R.dimen.workspace_icon_text_size));
        int titleH = calcTitleHeight(mTitleView);
        int paddingBottom = computeTextPaddingBottom(false);
        int hotSeatExtendTop = mHotSeatHeight - paddingBottom - titleH;
        int hotStartY = mHotSeatStartY + hotSeatExtendTop;
        int hotSeatW = BubbleResources.getHotseatIconWidth(mContext.getResources());

        for (int x = 0; x < ConfigManager.getCellCountX(); x++) {
            int startX = mHotSeatStartX + x * (hotSeatW + mHotSeatWidthGap);
            long hotSeatColor = IconGenerator.getBitmapAverageColor(wallpaper, startX, hotStartY,
                    hotSeatW, titleH, 1, 1);
            mHotSeatArray.add(IconUtils.getTitleColor((int) hotSeatColor));
        }
        wallpaper = null;

    }

    /**
     * calculate icon title height in bubble text view.
     * @param view the bubble text view.
     * @return height of icon title.
     */
    private int calcTitleHeight(TextView view) {
        mPaint.setTextSize(view.getTextSize());
        FontMetrics fm = mPaint.getFontMetrics();
        return (int) (Math.ceil(fm.descent - fm.ascent)) ;
    }

    /**
     * Used to determine the bottom padding of text.
     */
    private int computeTextPaddingBottom(boolean isInWorkspace) {

        final Resources res = mContext.getResources();
        float density = res.getDisplayMetrics().density;
        float fontScale = res.getConfiguration().fontScale;
        int standardPadding = isInWorkspace ?
                res.getDimensionPixelSize(R.dimen.bubble_text_bottom_padding_small) : res
                        .getDimensionPixelSize(R.dimen.bubble_text_bottom_padding);

        // adjust the padding for different font size:
        // standard text size = 10sp
        int bottom =  standardPadding - (int) (density * (10 * (fontScale - 1.0f)));
        return bottom > 0 ? bottom : 0;

    }

    /**
     * get wallpaper bitmap
     * @return
     */
    private Bitmap getWallpaper() {
        Bitmap wallpaper = null;
        Drawable drawable = mWallpaperManager.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            wallpaper = ((BitmapDrawable) drawable).getBitmap();
        }
        return wallpaper;
    }

    /**
     * reset default white color.
     */
    private void resetDefaultColor() {
        if (mHotSeatArray != null) {
            mHotSeatArray.clear();
        }
        mIconMap.clear();
        for (int x = 0; x < ConfigManager.getCellCountX(); x++) {
            for (int y = 0; y < ConfigManager.getCellCountY(); y++) {
                mIconMap.put(String.valueOf(x) + String.valueOf(y),
                        IconUtils.TITLE_COLOR_WHITE);
            }
            mHotSeatArray.add(IconUtils.TITLE_COLOR_WHITE);
        }
    }


    private boolean isSupportChangedColor() {
        return !mSupportCard && FeatureUtility.supportDyncColor() && HomeShellSetting.getIconDyncColor(mContext);
    }

}
