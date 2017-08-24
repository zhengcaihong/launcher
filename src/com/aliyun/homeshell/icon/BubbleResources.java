package com.aliyun.homeshell.icon;

import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.CellLayout.Mode;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.appclone.AppCloneManager;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

public final class BubbleResources {

    //// Static Layout Parameters ////
    private static boolean sInited = false;

    static int DOWNLOAD_ARC_DRAW_R;
    static int DOWNLOAD_ARC_WIDTH;

    static Bitmap mImgRTCorner;
    static int INDICATOR_BOUNDRY_X;
    static int INDICATOR_BOUNDRY_Y;

    static int IND_NUM_SIZE_NORMAL;

    static int BUBBLE_WIDTH;
    static int BUBBLE_HEIGHT;
    static int BUBBLE_WIDTH_IN_HOTSEAT;

    static int sTopPaddingHotseat;

    static Bitmap sImgPause;

    static int BUBBLE_TEXT_BOTTOM_PADDING;
    static int BUBBLE_TEXT_BOTTOM_PADDING_SMALL;

    static final ColorFilter FADING_EFFECT_FILTER = makeFadingEffectColorFilter();
    static final int FADING_EFFECT_ALPHA = 200;

    static final float SHADOW_LARGE_RADIUS = 1.0f;
    static final float SHADOW_Y_OFFSET = 1.0f;

    public static int sAtomSiblingOffset = 0;

    static Integer APP_CLONE_MARK_SIZE;
    static Integer ICON_SIZE;

    static Bitmap[] sAppCloneMarkBmps = new Bitmap[AppCloneManager.MAX_APP_CLONE_COUNT];
    private static int[] sAppCloneMarkIds = {
        R.drawable.ic_appclone_mark1, R.drawable.ic_appclone_mark2,
        R.drawable.ic_appclone_mark3, R.drawable.ic_appclone_mark4,
        R.drawable.ic_appclone_mark5, R.drawable.ic_appclone_mark6,
        R.drawable.ic_appclone_mark7
};

    public static ColorFilter makeFadingEffectColorFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.0f);
        return new ColorMatrixColorFilter(matrix);
    }

    static final float PROGRESS_NOT_DOWNLOAD = -1f;

    public static void ensureInited(Context context) {
        Resources res = context.getResources();
        if (!sInited) {

            sImgPause = BitmapFactory.decodeResource(res,R.drawable.home_icon_pause);
            mImgRTCorner = BitmapFactory.decodeResource(res, R.drawable.ic_corner_mark_bg);

            if (AgedModeUtil.isAgedMode()) {
                Bitmap tmp =  Bitmap.createBitmap(mImgRTCorner, 0, 0, mImgRTCorner.getWidth(),
                         mImgRTCorner.getHeight(), AgedModeUtil.sScaleUp, true);
                if(!mImgRTCorner.isRecycled()){
                    mImgRTCorner.recycle();
                }
                mImgRTCorner = tmp;
            }

            BUBBLE_WIDTH = getIconWidth(res);
            BUBBLE_HEIGHT = getIconHeight(res);

            BUBBLE_WIDTH_IN_HOTSEAT = getHotseatIconWidth(res);

            ICON_SIZE = IconManager.getIconSize(context);
            INDICATOR_BOUNDRY_X = BUBBLE_WIDTH - mImgRTCorner.getWidth();
            INDICATOR_BOUNDRY_Y = 0;

            // YUNOS BEGIN
            // ##modules(HomeShell): ##yongxing.lyx
            // ##BugID:(8129789) ##date:2016/04/12
            // ##description: remove atom of xiaoyun in hotseat
            //sAtomSiblingOffset = res.getDimensionPixelSize(R.dimen.dock_atom_sibling_offset);
            // YUNOS END

            IND_NUM_SIZE_NORMAL = res.getDimensionPixelSize(R.dimen.bubble_num_normal_size);

            DOWNLOAD_ARC_DRAW_R = res.getDimensionPixelSize(R.dimen.download_arc_draw_r);
            DOWNLOAD_ARC_WIDTH = res.getDimensionPixelSize(R.dimen.download_arc_width);

            BUBBLE_TEXT_BOTTOM_PADDING = res.getDimensionPixelSize(R.dimen.bubble_text_bottom_padding);
            BUBBLE_TEXT_BOTTOM_PADDING_SMALL = res.getDimensionPixelSize(R.dimen.bubble_text_bottom_padding_small);

            IconIndicator.init(res);

            sInited = true;
        }
    }

    public static void setNeedsReload() {
        sInited = false;
    }

    public static int getIconHeight(Resources res) {
        if (AgedModeUtil.isAgedMode()) {
            return res.getDimensionPixelSize(R.dimen.bubble_icon_height_3_3);
        } else {
            return res.getDimensionPixelSize(R.dimen.workspace_cell_height);
        }
    }

    public static int getIconWidth(Resources res) {
        if (AgedModeUtil.isAgedMode()) {
            return res.getDimensionPixelSize(R.dimen.bubble_icon_width_3_3);
        } else {
            return res.getDimensionPixelSize(R.dimen.workspace_cell_width);
        }
    }

    public static int getHotseatIconWidth(Resources res) {
        if (AgedModeUtil.isAgedMode()) {
            return res.getDimensionPixelSize(R.dimen.bubble_icon_width_3_3);
        } else {
            return res.getDimensionPixelSize(R.dimen.hotseat_cell_width);
        }
    }

    private BubbleResources() {}

    public static Bitmap getAppCloneMarkIcon(Resources res, int index) {
        if (res == null || index >= AppCloneManager.MAX_APP_CLONE_COUNT) {
            return null;
        }
        if (sAppCloneMarkBmps[index] == null || sAppCloneMarkBmps[index].isRecycled()) {
            sAppCloneMarkBmps[index] = BitmapFactory.decodeResource(res, sAppCloneMarkIds[index]);
        }
        return sAppCloneMarkBmps[index];
    }

    public static int getCardIconAppCloneMarkX(Context context) {
        if (APP_CLONE_MARK_SIZE == null) {
            APP_CLONE_MARK_SIZE = getAppCloneMarkIcon(context.getResources(), 0).getHeight();
        }
        return BUBBLE_WIDTH - APP_CLONE_MARK_SIZE;
    }

    public static int getCardIconAppCloneMarkY(Context context) {
        if (APP_CLONE_MARK_SIZE == null) {
            APP_CLONE_MARK_SIZE = getAppCloneMarkIcon(context.getResources(), 0).getHeight();
        }
        return BUBBLE_HEIGHT - APP_CLONE_MARK_SIZE;
    }

    public static int getIconAppCloneMarkX(Context context, CellLayout.Mode mode) {

        if (APP_CLONE_MARK_SIZE == null) {
            APP_CLONE_MARK_SIZE = getAppCloneMarkIcon(context.getResources(), 0).getHeight();
        }
        int x;
        if (mode == Mode.HOTSEAT) {
            x = ICON_SIZE - APP_CLONE_MARK_SIZE / 2 + (BUBBLE_WIDTH_IN_HOTSEAT - ICON_SIZE) / 2;
        } else {
            x = BUBBLE_WIDTH - APP_CLONE_MARK_SIZE;
        }
        return x;
    }

    public static int getIconAppCloneMarkY(Context context, CellLayout.Mode mode) {
        if (APP_CLONE_MARK_SIZE == null) {
            APP_CLONE_MARK_SIZE = getAppCloneMarkIcon(context.getResources(), 0).getHeight();
        }

        int iconSize = ICON_SIZE;
        if (AgedModeUtil.isAgedMode()) {
            iconSize *= AgedModeUtil.SCALE_RATIO_FOR_AGED_MODE;
        }
        return iconSize - APP_CLONE_MARK_SIZE / 2;
    }

}
