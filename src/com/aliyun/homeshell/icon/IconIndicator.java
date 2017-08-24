package com.aliyun.homeshell.icon;

import java.util.HashMap;
import java.util.Map;

import android.content.ComponentName;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Pair;

import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.CellLayout.Mode;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutInfo;

/* YUNOS BEGIN */
// ##date:2015-1-17 ##author:zhanggong.zg ##BugID:5681346
/**
 * The small image indicator on the left side of the title in
 * <code>BubbleTextView</code>. Currently we have "new" indicator
 * and "flip card" indicator.
 * @author zhanggong.zg
 */
public final class IconIndicator {

    private static boolean initialized = false;

    private static Bitmap sImgNewIndicator;
    private static Bitmap sImgCardIndicatorWhite;
    private static Bitmap sImgCardIndicatorBlack;
    private static Bitmap sImgClonableMarkWhite;
    private static Bitmap sImgClonableMarkBlack;
    private static int sImgIndicatorPadding; // the padding between indicator and text

    public static void init(Resources res) {
        if (!initialized) {
            sImgNewIndicator = BitmapFactory.decodeResource(res, R.drawable.ic_new_normal);
            sImgCardIndicatorWhite = IconIndicator.createCardIndicator(res, IconUtils.TITLE_COLOR_WHITE);
            sImgCardIndicatorBlack = IconIndicator.createCardIndicator(res, IconUtils.TITLE_COLOR_BLACK);
            sImgClonableMarkWhite = IconIndicator.createClonableCardIndicator(res, IconUtils.TITLE_COLOR_WHITE);
            sImgClonableMarkBlack = IconIndicator.createClonableCardIndicator(res, IconUtils.TITLE_COLOR_BLACK);
            sImgIndicatorPadding = res.getDimensionPixelSize(R.dimen.ic_indicator_padding);
            initialized = true;
        }
    }

    public static int getIndicatorPadding() {
        return sImgIndicatorPadding;
    }

    public static Bitmap getNewIndicatorImage() {
        return sImgNewIndicator;
    }

    public static Bitmap getCardIndicatorWhiteImage() {
        return sImgCardIndicatorWhite;
    }

    public static Bitmap getCardIndicatorBlackImage() {
        return sImgCardIndicatorBlack;
    }

    public static Bitmap getClonableMarkIconWhite() {
        return sImgClonableMarkWhite;
    }

    public static Bitmap getClonableMarkIconBlack() {
        return sImgClonableMarkBlack;
    }

    // ad-hoc indicator position
    private static final Map<String, Pair<Float, Float>> sAdhocIndicatorPositionMap;
    static {
        sAdhocIndicatorPositionMap = new HashMap<String, Pair<Float,Float>>();
        sAdhocIndicatorPositionMap.put("com.yunos.weatherservice", new Pair<Float, Float>(0.22f, 0.89f));
        sAdhocIndicatorPositionMap.put("com.moji.aliyun", new Pair<Float, Float>(0.22f, 0.89f));
        sAdhocIndicatorPositionMap.put("sina.mobile.tianqitongyunos", new Pair<Float, Float>(0.22f, 0.89f));
        sAdhocIndicatorPositionMap.put("com.android.calendar", new Pair<Float, Float>(0.5f, 0.89f));
    }

    /**
     * Retrieves the ad-hoc indicator position for fancy icon (e.g. the weather
     * and calendar app). If the specified <code>icon</code> do not need special
     * indicator layout currently, returns <code>null</code>. Otherwise, returns
     * a pair of float values that represent the percentage of x and y axis of
     * the entire icon.
     * @param icon
     * @return the (x, y) position in percentage or {@code null}
     */
    public static Pair<Float, Float> getAdhocIndicatorPosition(BubbleTextView icon){
        if (icon == null) return null;
        if (!TextUtils.isEmpty(icon.getText())) return null;
        if (!(icon.getTag() instanceof ShortcutInfo)) return null;
        if (icon.getMode() != Mode.NORMAL &&
            !(AgedModeUtil.isAgedMode() && icon.getMode() == Mode.HOTSEAT)) {
            return null;
        }
        ShortcutInfo item = (ShortcutInfo) icon.getTag();
        ComponentName cmpt = item.intent != null ? item.intent.getComponent() : null;
        String packageName = cmpt != null ? cmpt.getPackageName() : null;
        if (TextUtils.isEmpty(packageName)) return null;

        Pair<Float, Float> value = sAdhocIndicatorPositionMap.get(packageName);
        if (value != null) {
            return new Pair<Float, Float>(value.first, value.second);
        } else {
            return null;
        }
    }

    private static Bitmap createCardIndicator(Resources res, int color) {
        if (color == IconUtils.TITLE_COLOR_WHITE) {
            return BitmapFactory.decodeResource(res, R.drawable.ic_card_indicator_light);
        } else if (color == IconUtils.TITLE_COLOR_BLACK) {
            return BitmapFactory.decodeResource(res, R.drawable.ic_card_indicator_dark);
        } else {
            return null;
        }
    }

    private static Bitmap createClonableCardIndicator(Resources res, int color) {
        if (color == IconUtils.TITLE_COLOR_WHITE) {
            return BitmapFactory.decodeResource(res, R.drawable.ic_clonable_mark_light);
        } else if (color == IconUtils.TITLE_COLOR_BLACK) {
            return BitmapFactory.decodeResource(res, R.drawable.ic_clonable_mark_dark);
        } else {
            return null;
        }
    }

    private IconIndicator() {}
}
/* YUNOS END */
