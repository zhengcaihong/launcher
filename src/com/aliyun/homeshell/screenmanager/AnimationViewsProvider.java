package com.aliyun.homeshell.screenmanager;

import java.util.ArrayList;
import java.util.List;

import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.DragLayer;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutAndWidgetContainer;
import com.aliyun.homeshell.CellLayout.LayoutParams;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.Workspace;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

class AnimationViewsProvider {

    private List<List<View>> mViews;
    private List<List<CellLayout.LayoutParams>> mParams;
    private List<FrameLayout> mBgCardList = new ArrayList<FrameLayout>();
    private List<FrameLayout> mScaleBgCardList = new ArrayList<FrameLayout>();
    private List<ScaleInfo> mScale = new ArrayList<ScaleInfo>();

    private Launcher mLauncher;

    private float mCellLayoutX;
    private float mCellLayoutY;
    private int mCurrentScreen;

    private View mHotSeat;
    private float mHotSeatX;
    private float mHotSeatY;
    private android.view.ViewGroup.LayoutParams mHotseatParam;

    private boolean mOpened;

    private static final Drawable mBackground = LauncherApplication.getContext()
            .getResources().getDrawable(R.drawable.aui_ic_theme_bg);

    static {
        mBackground.setFilterBitmap(true);
    }

    AnimationViewsProvider(Launcher launcher) {
        mLauncher = launcher;
        mViews = new ArrayList<List<View>>();
        mParams = new ArrayList<List<CellLayout.LayoutParams>>();
    }

    List<List<View>> getEntryViews() {
        clearBgCardList();
        List<List<View>> views = new ArrayList<List<View>>();
        for (List<View> list : mViews) {
            List<View> newList = new ArrayList<View>();
            for (View v : list) {
                ItemInfo info = (ItemInfo) v.getTag();
                if (info.spanX != 1 || info.spanY != 1) {
                    View newV = generalScaleCard(v, info.spanX, info.spanY, true);
                    newList.add(newV);
                } else if (mLauncher.getIconManager().supprtCardIcon()) {
                    newList.add(v);
                } else {
                    View newV = generalBgCard(v);
                    newList.add(newV);
                }
            }
            views.add(newList);
        }
        return views;
    }

    private View generalBgCard(View v) {
        FrameLayout card = new FrameLayout(v.getContext());
        card.setBackground(mBackground);
        card.addView(v);
        mBgCardList.add(card);
        return card;
    }

    private View generalScaleCard(View v, int scaleX, int scaleY, boolean background) {
        AdjustFrameLayout card = new AdjustFrameLayout(v.getContext());
        card.setBackground(mBackground);
        card.addView(v);

        mScaleBgCardList.add(card);
        ScaleInfo info = new ScaleInfo();
        info.v = v;
        mScale.add(info);
        return card;
    }

    List<List<CellLayout.LayoutParams>> getEntryParams() {
        List<List<CellLayout.LayoutParams>> params
                = new ArrayList<List<CellLayout.LayoutParams>>();
        for (List<CellLayout.LayoutParams> list : mParams) {
            List<CellLayout.LayoutParams> newList
                    = new ArrayList<CellLayout.LayoutParams>();
            for (CellLayout.LayoutParams lp : list) {
                if (lp.cellHSpan == 1 && lp.cellVSpan == 1) {
                    newList.add(lp);
                } else {
                    CellLayout.LayoutParams newLp= new CellLayout.LayoutParams(lp);
                    newLp.setX((int) (lp.getX() + (lp.getWidth() - Const.CARD_WIDTH) / 2));
                    newLp.setY((int) (lp.getY() + (lp.getHeight() - Const.CARD_HEIGHT) / 2));
                    newLp.width = (int) Const.CARD_WIDTH;
                    newLp.height = (int) Const.CARD_HEIGHT;
                    newList.add(newLp);
                }
            }
            params.add(newList);
        }
        return params;
    }

    List<List<View>> getExitViewsStage1(int[] newIndexMap) {
        clearBgCardList();
        List<List<View>> views = new ArrayList<List<View>>();
        for (int i = 0; i < newIndexMap.length; i++) {
            List<View> list = mViews.get(newIndexMap[i]);
            List<View> newList = new ArrayList<View>();
            for (View v : list) {
                ItemInfo info = (ItemInfo) v.getTag();
                if (info.spanX != 1 || info.spanY != 1) {
                    View newV = generalScaleCard(v, info.spanX, info.spanY, true);
                    newList.add(newV);
                } else if (mLauncher.getIconManager().supprtCardIcon()) {
                    newList.add(v);
                } else {
                    View newV = generalBgCard(v);
                    newList.add(newV);
                }
            }
            views.add(newList);
        }
        return views;
    }

    List<List<CellLayout.LayoutParams>> getExitParamsStage1(int[] newIndexMap) {
        List<List<CellLayout.LayoutParams>> params =
                    new ArrayList<List<CellLayout.LayoutParams>>();
        for (int i = 0; i < newIndexMap.length; i++) {
            List<CellLayout.LayoutParams> list = mParams.get(newIndexMap[i]);
            List<CellLayout.LayoutParams> newList
                    = new ArrayList<CellLayout.LayoutParams>();
            for (CellLayout.LayoutParams lp : list) {
                if (lp.cellHSpan == 1 && lp.cellVSpan == 1) {
                    newList.add(lp);
                } else {
                    CellLayout.LayoutParams newLp= new CellLayout.LayoutParams(lp);
                    newLp.setX((int) (lp.getX() + (lp.getWidth() - Const.CARD_WIDTH) / 2));
                    newLp.setY((int) (lp.getY() + (lp.getHeight() - Const.CARD_HEIGHT) / 2));
                    newLp.width = (int) Const.CARD_WIDTH;
                    newLp.height = (int) Const.CARD_HEIGHT;
                    newList.add(newLp);
                }
            }
            params.add(newList);
        }
        return params;
    }

    List<List<View>> getExitViewsStage2(int[] newIndexMap, int currentScreen) {
        clearBgCardList();
        List<List<View>> views = new ArrayList<List<View>>();
        for (int i = 0; i < newIndexMap.length; i++) {
            List<View> list = mViews.get(newIndexMap[i]);
            if (i == currentScreen) {
                views.add(list);
                continue;
            }
            List<View> newList = new ArrayList<View>();
            for (View v : list) {
                ItemInfo info = (ItemInfo) v.getTag();
                if (info.spanX != 1 || info.spanY != 1) {
                    View newV = generalScaleCard(v, info.spanX, info.spanY, true);
                    newList.add(newV);
                } else if (mLauncher.getIconManager().supprtCardIcon()) {
                    newList.add(v);
                } else {
                    View newV = generalBgCard(v);
                    newList.add(newV);
                }
            }
            views.add(newList);
        }
        return views;
    }

    List<List<CellLayout.LayoutParams>> getExitParamsStage2(
            int[] newIndexMap, int currentScreen) {
        List<List<CellLayout.LayoutParams>> params =
                new ArrayList<List<CellLayout.LayoutParams>>();
    for (int i = 0; i < newIndexMap.length; i++) {
        List<CellLayout.LayoutParams> list = mParams.get(newIndexMap[i]);
        if (i == currentScreen) {
            params.add(list);
            continue;
        }
        List<CellLayout.LayoutParams> newList
                = new ArrayList<CellLayout.LayoutParams>();
        for (CellLayout.LayoutParams lp : list) {
            if (lp.cellHSpan == 1 && lp.cellVSpan == 1) {
                newList.add(lp);
            } else {
                CellLayout.LayoutParams newLp= new CellLayout.LayoutParams(lp);
                newLp.setX((int) (lp.getX() + (lp.getWidth() - Const.CARD_WIDTH) / 2));
                newLp.setY((int) (lp.getY() + (lp.getHeight() - Const.CARD_HEIGHT) / 2));
                 newLp.width = (int) Const.CARD_WIDTH;
                newLp.height = (int) Const.CARD_HEIGHT;
                newList.add(newLp);
            }
        }
        params.add(newList);
    }
    return params;
    }


    float getCellLayoutX() {
        return mCellLayoutX;
    }

    float getCellLayoutY() {
        return mCellLayoutY;
    }

    void open() {
        DebugTools.log("AnimationViewsProvider,open:" + mOpened, false);
        mOpened = true;
        Workspace workspace = mLauncher.getWorkspace();
        int screens = workspace.getIconScreenCount();
        mCurrentScreen = workspace.getIconScreenIndex(workspace.getCurrentPage());
        for (int i = 0; i < screens; i ++) {
            CellLayout layout = (CellLayout) workspace.getChildAt(i + workspace.getIconScreenHomeIndex());
            if (layout == null) {
                Log.e(Const.TAG, "screen info error:" + i + "/" + screens);
                return;
            }
            if (i == mCurrentScreen) {

                ShortcutAndWidgetContainer container = layout
                        .getShortcutAndWidgetContainer();
                DragLayer root = mLauncher.getDragLayer();
                Rect relative = new Rect();
                root.getViewRectRelativeToSelf(container, relative);
                mCellLayoutX = relative.left;
                mCellLayoutY = relative.top;
            }
            List<View> views = new ArrayList<View>();
            List<CellLayout.LayoutParams> params = new ArrayList<CellLayout.LayoutParams>();
            saveViewsAndParams(layout, views, params);
            mViews.add(views);
            mParams.add(params);
        }
        mHotSeat = mLauncher.getHotseat();
        mHotSeatX = mHotSeat.getX();
        mHotSeatY = mHotSeat.getY();
        mHotseatParam = mHotSeat.getLayoutParams();
        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(6898647) ##date:2015/12/10
        // ##description: margin error after show or hide navbar in screen
        // manager.
        if (mHotseatParam instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams hotseatPm =
                    (ViewGroup.MarginLayoutParams) mHotseatParam;
            hotseatPm.bottomMargin -= mLauncher.getDynamicNavBarHeight();
        }
        // YUNOS END PB
        mLauncher.getDragLayer().removeView(mHotSeat);
        mHotSeat.setVisibility(View.VISIBLE);
    }

    private void saveViewsAndParams(CellLayout layout, List<View> views,
            List<LayoutParams> params) {
        int xCount = layout.getCountX();
        int yCount = layout.getCountY();
        for (int y = 0; y < yCount; y ++) {
            for (int x = 0; x < xCount; x++) {
                View child = layout.getChildAt(x, y);
                if (child != null && !views.contains(child)) {
                    CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) child
                            .getLayoutParams();
                    views.add(child);
                    params.add(layoutParams);
                }
            }
        }

        ShortcutAndWidgetContainer container = layout
                .getShortcutAndWidgetContainer();
        int childCount = container.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            container.removeViewAt(i);
        }
    }

    void close() {
        DebugTools.log("AnimationViewsProvider,close:" + mOpened, false);
        clearBgCardList();
        int count = mViews.size();
        Workspace workspace = mLauncher.getWorkspace();
        for (int i = 0; i < count; i++) {
            CellLayout layout = (CellLayout) workspace.getChildAt(i + workspace.getIconScreenHomeIndex());
            if (layout == null) {
                Log.e(Const.TAG, "screen info error:" + i + "/" + count);
                return;
            }
            List<View> views = mViews.get(i);
            List<CellLayout.LayoutParams> params = mParams.get(i);
            restoreViewsAndParams(layout, views, params);
            views.clear();
            params.clear();
        }
        if (mHotSeat.getParent() != null) {
            ((ViewGroup)mHotSeat.getParent()).removeView(mHotSeat);
        }
        mHotSeat.setX(0);
        mHotSeat.setY(0);
        mHotSeat.setTranslationX(0);
        mHotSeat.setTranslationY(0);
        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(6898647) ##date:2015/12/10
        // ##description: margin error after show or hide navbar in screen
        // manager.
        if (mHotseatParam instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams hotseatPm =
                    (ViewGroup.MarginLayoutParams) mHotseatParam;
            hotseatPm.bottomMargin += mLauncher.getDynamicNavBarHeight();
        }
        // YUNOS END PB
        mLauncher.getDragLayer().addView(mHotSeat, mHotseatParam);
        mViews.clear();
        mParams.clear();
        mOpened = false;
    }

    private void clearBgCardList() {
        for (FrameLayout bgCard : mBgCardList) {
            bgCard.removeAllViews();
        }
        mBgCardList.clear();
        for (FrameLayout bgScaleCard : mScaleBgCardList) {
            bgScaleCard.removeAllViews();
        }
        mScaleBgCardList.clear();
        for(ScaleInfo info : mScale) {
            info.v.setScaleX(1.0f);
            info.v.setScaleY(1.0f);
        }
        mScale.clear();
    }

    private void restoreViewsAndParams(CellLayout layout, List<View> views,
            List<LayoutParams> params) {
        int count = views.size();
        for (int i = 0; i < count; i++) {
            View child = views.get(i);
            child.setX(0);
            child.setY(0);
            child.setTranslationY(0);
            child.setTranslationX(0);
            child.setRotation(0);
            child.setAlpha(1.0f);
            layout.addViewToCellLayout(child, i, child.getId(),
                    params.get(i), true);
        }
    }

    int getCurrentScreen() {
        return mCurrentScreen;
    }

    List<List<View>> getViews() {
        return mViews;
    }

    int getCurrentScreen(int[] newIndexMap) {
        if (newIndexMap == null) {
            return mCurrentScreen;
        }
        for (int i = 0; i < newIndexMap.length; i++) {
            if (mCurrentScreen == newIndexMap[i]) {
                return i;
            }
        }
        return mCurrentScreen;
    }

    View getHotSeat() {
        return mHotSeat;
    }

    float getHotSeatOffsetX() {
        return mHotSeatX;
    }

    float getHotSeatOffseatY() {
        return mHotSeatY;
    }

    boolean opened() {
        DebugTools.log("lockWorkspace:" + mOpened, false);
        return mOpened;
    }

    private static class ScaleInfo {
        View v;
    }
}