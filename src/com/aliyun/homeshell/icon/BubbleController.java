package com.aliyun.homeshell.icon;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import app.aliyun.aml.FancyDrawable;

import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.AppDownloadManager.AppDownloadStatus;
import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.CellLayout.Mode;
import com.aliyun.homeshell.GadgetCardHelper;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.appclone.AppCloneManager;
import com.aliyun.homeshell.gadgets.HomeShellGadgetsRender;
import com.aliyun.homeshell.icon.BubbleTextView.LayoutStyle;
import com.aliyun.homeshell.icon.BubbleTextView.MaskType;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.utils.Utils;

import static com.aliyun.homeshell.icon.BubbleResources.*;

public final class BubbleController {

    public static void applyToView(ShortcutInfo info, BubbleTextView view) {
        view.setTag(info);

        IconManager mIconManager = ((LauncherApplication) view.getContext()
                .getApplicationContext()).getIconManager();

        CellLayout.Mode mode = view.getMode();
        final boolean isInHotseatOrHideseat = mode.isHotseatOrHideseat();
        boolean supportCard = mIconManager.supprtCardIcon();
        view.setSupportCard(supportCard);

        if (!supportCard) {
            view.setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, Color.parseColor("#ff333333"));
        } else {
            view.setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, 0.0f, Color.parseColor("#00ffffff"));
        }
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getResources().getDimension(
                         supportCard ?
                         R.dimen.workspace_icon_text_size_small :
                         R.dimen.workspace_icon_text_size));

        boolean hasCardBgIcon = false;
        ComponentName component = info.intent != null ? info.intent.getComponent() : null;

        boolean needreset = false;
        boolean cardBackgroundChanged = false;
        Pair<Drawable, Integer> cardBgAndTitleColor = null;
        if (info.intent != null && supportCard) {
            cardBgAndTitleColor = mIconManager.getAppCardBgAndTitleColor(info);

            cardBackgroundChanged = view.setCardBackground(cardBgAndTitleColor.first);
            hasCardBgIcon = mIconManager.isCardBgIcon(cardBgAndTitleColor.first);
            if (isInHotseatOrHideseat && (mode == Mode.HIDESEAT || !AgedModeUtil.isAgedMode())) {
                hasCardBgIcon = false;
            }

            if(cardBgAndTitleColor.first != null){
                if(cardBackgroundChanged){
                    needreset = true;
                }
                if(view.getTitleColor() == 0 || needreset){
                    if (cardBgAndTitleColor != null && cardBgAndTitleColor.second != null) {
                        view.setTitleColor(cardBgAndTitleColor.second);
                    } else {
                        view.setTitleColor(mIconManager.getTitleColor(info));
                    }
                }
            }
            if (mode == Mode.HOTSEAT) {
                view.setTextColor(IconUtils.TITLE_COLOR_WHITE);
            }
        } else if (!supportCard) {
            view.setCardBackground(null);
            view.setTitleColor(IconUtils.TITLE_COLOR_WHITE);
        }

        if ((mode != Mode.HOTSEAT || AgedModeUtil.isAgedMode()) && mode != Mode.HIDESEAT) {
            view.setTextColor(view.getTitleColor());
        }

        Drawable icon = info.mIcon;

        if (supportCard && info.isEditFolderShortcut()) {
            view.setBackgroundResource(R.drawable.files_add_card_icon);
            icon = null;
        }

        if (hasCardBgIcon && !HomeShellGadgetsRender.isOneKeyLockShortCut(info)) {
            icon = null;
        }

        if (HomeShellGadgetsRender.isOneKeyAccerateShortCut(info)) {
            icon = HomeShellGadgetsRender.getRender().getAccesalarateIcon(view.getContext(), supportCard);
            HomeShellGadgetsRender.getRender().registerIcon(view);
            view.setMask(MaskType.OneKeyAccelerate);
        }

        if (icon != view.getCompoundDrawablesRelative()[1]) {
            view.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
        }

        if (icon != null && AgedModeUtil.isAgedMode()) {
            float scaleRatio = AgedModeUtil.SCALE_RATIO_FOR_AGED_MODE;
            int left = (int) (-icon.getIntrinsicWidth() * (scaleRatio - 1) / 2);
            int top = 0;
            int right = (int) (left + icon.getIntrinsicWidth() * scaleRatio);
            int bottom = (int) (top + icon.getIntrinsicHeight() * scaleRatio);
            icon.setBounds(left, top, right, bottom);
        }

        if (info.getAppDownloadStatus() == AppDownloadStatus.STATUS_NO_DOWNLOAD
                || info.getAppDownloadStatus() == AppDownloadStatus.STATUS_INSTALLED)
        {
            if(info != null && info.title != null ) {
                info.title = Utils.trimUTFSpace(info.title);
            }
            updateIndicator(view);
            updateTitle(view);
        }

        applyPadding(view, icon, mode);

        if(mode == Mode.HIDESEAT) {
            view.setFadingEffectEnable(true, true);
        }

        Context context = view.getContext();
        if (context instanceof Launcher && info.userId > 0) {
            int index = AppCloneManager.getMarkIconIndex(info.userId,
                    info.getPackageName());
            view.setAppCloneMarkIcon(index);
        }

        updateIndicatorAndTitle(view);
        updateCornerMarkVisible(view, info);
    }

    public static void setMode(BubbleTextView view, CellLayout.Mode mode) {
        ShortcutInfo info = (ShortcutInfo) view.getTag();
        if (mode != view.getMode() && info != null) {
            view.setMode(mode);
            if ((AgedModeUtil.isAgedMode() && (mode != Mode.HIDESEAT)
               || !mode.isHotseatOrHideseat())) {
                view.setLayoutStyle(LayoutStyle.WorkspaceStyle);
            } else {
                view.setLayoutStyle(LayoutStyle.HotseatStyle);
            }
            applyToView(info, view);
        }

        if (mode == Mode.NORMAL) {
            view.setTextColor(view.getTitleColor());
            view.resetTempPadding();
        } else if (!AgedModeUtil.isAgedMode()) {
            view.setTextColor(IconUtils.TITLE_COLOR_WHITE);
        } else if (mode == Mode.HIDESEAT) {
            view.setTextColor(IconUtils.TITLE_COLOR_WHITE);
        } else {
            view.setTextColor(view.getTitleColor());
        }
        updateCornerMarkVisible(view, (ShortcutInfo) view.getTag());
    }

    public static void updateView(BubbleTextView view) {
        ShortcutInfo info = (ShortcutInfo) view.getTag();

        if (!info.isEditFolderShortcut()) {
            if(info.itemType != LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT) {
                view.setDownloadProgress(info.getProgress());
                updateDownloadStatus(view, info);
                updateCornerMarkVisible(view, info);
                updateCornerMarkNumber(view, info);
            }

            updateIndicatorAndTitle(view);
        }
        view.invalidate();
    }

    public static boolean updateIndicator(BubbleTextView view) {
        ShortcutInfo info = (ShortcutInfo) view.getTag();
        boolean canBeFlipped = false;
        CellLayout.Mode mode = view.getMode();
        if (info != null && info.intent != null && mode != Mode.HIDESEAT &&
                // ##date:2015-3-24 ##author:zhanggong.zg ##BugID:5850679
                info.itemType == Favorites.ITEM_TYPE_APPLICATION) {
            ComponentName cn = info.intent.getComponent();
            GadgetCardHelper helper = GadgetCardHelper.getInstance(view.getContext());
            // ##date:2015-2-5 ##author:zhanggong.zg ##BugID:5746131
            canBeFlipped = helper.hasCardView(cn, view, info.messageNum > 0);
        }

        if (canBeFlipped && LauncherModel.isShowSlideUpMarkIcon()) {
            int indicatorColor;
            if (mode == Mode.NORMAL) {
                indicatorColor = view.getTitleColor();
            } else if (!AgedModeUtil.isAgedMode()) {
                indicatorColor = IconUtils.TITLE_COLOR_WHITE;
            } else if (mode == Mode.HIDESEAT) {
                indicatorColor = IconUtils.TITLE_COLOR_WHITE;
            } else {
                indicatorColor = view.getTitleColor();
            }
            if (indicatorColor == IconUtils.TITLE_COLOR_WHITE) {
                return view.setIndicator(IconIndicator.getCardIndicatorWhiteImage());
            } else {
                return view.setIndicator(IconIndicator.getCardIndicatorBlackImage());
            }
        } else if (info.isNewItem() && LauncherModel.isShowNewMarkIcon()) {
            return view.setIndicator(IconIndicator.getNewIndicatorImage());
        } else if (LauncherModel.isShowClonableMarkIcon()
                && AppCloneManager.getInstance().showClonableMark(info)) {
            int indicatorColor;
            if (mode == Mode.NORMAL) {
                indicatorColor = view.getTitleColor();
            } else if (!AgedModeUtil.isAgedMode()) {
                indicatorColor = IconUtils.TITLE_COLOR_WHITE;
            } else if (mode == Mode.HIDESEAT) {
                indicatorColor = IconUtils.TITLE_COLOR_WHITE;
            } else {
                indicatorColor = view.getTitleColor();
            }
            if (indicatorColor == IconUtils.TITLE_COLOR_WHITE) {
                return view.setIndicator(IconIndicator.getClonableMarkIconWhite());
            } else {
                return view.setIndicator(IconIndicator.getClonableMarkIconBlack());
            }
        } else {
            return view.setIndicator(null);
        }
    }

    public static void updateTitle(BubbleTextView view) {
        ShortcutInfo info = (ShortcutInfo) view.getTag();
        String title = info.title != null ? info.title.toString() : "";

        if(TextUtils.isEmpty(title)) {
            return;
        }

        if (HomeShellGadgetsRender.isOneKeyAccerateShortCut(info)) {
            view.setTitle(HomeShellGadgetsRender.getRender().getTitle(view.getContext()));
            return;
        }

        title = title.trim();

        boolean isCalendarCard = false;
        if (title.equals("Calendar")){
            isCalendarCard = true;
        }

        Drawable icon = view.getIconDrawable();
        if (icon instanceof FancyDrawable) {
            String label = ((FancyDrawable) icon).getVariableString("app_label");
            if (label != null)
                title = label;
        }

        if (isCalendarCard) {
            if (title.equals("Sunday")) {
                title = "Sun";
            } else if (title.equals("Monday")) {
                title = "Mon";
            } else if (title.equals("Tuesday")) {
                title = "Tues";
            } else if (title.equals("Wednesday")) {
                title = "Wed";
            } else if(title.equals("Thursday")) {
                title = "Thur";
            } else if(title.equals("Friday")) {
                title = "Fri";
            } else if(title.equals("Saturday")) {
                title = "Sat";
            }
        }

        view.setTitle(title);
    }

    private static void applyPadding(BubbleTextView view, Drawable icon, CellLayout.Mode mMode) {
        if (icon == null) {
            view.setPadding(view.getPaddingLeft(), 0, view.getPaddingRight(), 0);
            return;
        }

        Resources res = view.getContext().getResources();
        int padding = 0;

        if (AgedModeUtil.isAgedMode()) {
            int iconwidth = icon.getBounds().width();
            int bubblewidth = (int) (res.getDimensionPixelSize(
                    R.dimen.workspace_cell_width) * AgedModeUtil.SCALE_RATIO_FOR_AGED_MODE);
            padding = Math.abs((bubblewidth - iconwidth) / 2);
        } else if (mMode == Mode.HOTSEAT || mMode == Mode.HIDESEAT) {
            padding = res.getDimensionPixelSize(R.dimen.bubble_textview_hotseat_top_padding);
            if(mMode == Mode.HOTSEAT) {
                int paddingLeft = res.getDimensionPixelSize(R.dimen.bubble_textview_hotseat_left_padding);
                view.setPadding(paddingLeft, view.getPaddingTop(), paddingLeft, view.getPaddingBottom());
            }

        } else {
            int iconwidth = icon.getBounds().width();
            int bubblewidth = res.getDimensionPixelSize(R.dimen.workspace_cell_width);
            padding = Math.abs((bubblewidth - iconwidth) / 2);
        }
        view.setPadding(view.getPaddingLeft(), padding, view.getPaddingRight(), 0);
    }

    private static void updateIndicatorAndTitle(BubbleTextView view) {
        if (!updateIndicator(view)) {
            return;
        }
        updateTitle(view);
    }

    private static void updateDownloadStatus(BubbleTextView view, ShortcutInfo info) {
        int mDownloadStatus = info.getAppDownloadStatus();
        if ((mDownloadStatus == AppDownloadStatus.STATUS_NO_DOWNLOAD ||
             mDownloadStatus == AppDownloadStatus.STATUS_INSTALLED)){
            view.setMask(MaskType.NoMask);
            return;
        }
        String title = null;
        switch (mDownloadStatus) {
        case AppDownloadStatus.STATUS_WAITING:
            view.setMask(MaskType.PauseMask);
            title = view.getContext().getString(R.string.waiting);
            break;
        case AppDownloadStatus.STATUS_DOWNLOADING:
            view.setMask(MaskType.DownloadMask);
            title = view.getContext().getString(R.string.downloading);
            break;
        case AppDownloadStatus.STATUS_PAUSED:
            view.setMask(MaskType.PauseMask);
            title = view.getContext().getString(R.string.paused);
            break;
        case AppDownloadStatus.STATUS_INSTALLING:
            view.setMask(MaskType.DownloadMask);
            title = view.getContext().getString(R.string.installing);
            break;
        }
        if (title != null && !title.equals(view.getText())) {
            view.setText(title);
        }
    }

    private static void updateCornerMarkVisible(BubbleTextView view, ShortcutInfo info) {
        boolean disableMark = false;
        if(info.mIcon instanceof FancyDrawable) {
            disableMark = Boolean.parseBoolean(((FancyDrawable) info.mIcon).getRawAttr("hideApplicationMessage"));
        }
        view.setCornerMarkVisible(LauncherModel.showNotificationMark() && view.getMode() != Mode.HIDESEAT && !disableMark);
        view.invalidate();
    }

    private static void updateCornerMarkNumber(BubbleTextView view, ShortcutInfo info) {
        int aliMsg = info.messageNum;
        int notifMsg = GadgetCardHelper.getInstance(view.getContext()).getNotificationCount(info.intent.getComponent(), info.userId, view);
        int displayNumber = aliMsg > 0 ? aliMsg : notifMsg;
        view.setCornerMarkNumber(displayNumber);
    }

    private BubbleController() {}

}
