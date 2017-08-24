package com.aliyun.homeshell.hideseat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PointF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.Alarm;
import com.aliyun.homeshell.Alarm.OnAlarmListener;
import com.aliyun.homeshell.AppDownloadManager.AppDownloadStatus;
import com.aliyun.homeshell.ApplicationInfo;
import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.CellLayout.Mode;
import com.aliyun.homeshell.CellLayout.ReorderAnimationListener;
import com.aliyun.homeshell.CheckVoiceCommandPressHelper;
import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.DragController;
import com.aliyun.homeshell.DragController.DragListener;
import com.aliyun.homeshell.DragSource;
import com.aliyun.homeshell.DragView;
import com.aliyun.homeshell.DropTarget;
import com.aliyun.homeshell.Folder;
import com.aliyun.homeshell.FolderIcon;
import com.aliyun.homeshell.FolderInfo;
import com.aliyun.homeshell.Hotseat;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.PageIndicatorView;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ScreenPosition;
import com.aliyun.homeshell.ShortcutAndWidgetContainer;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.SmoothPagedView;
import com.aliyun.homeshell.UserTrackerHelper;
import com.aliyun.homeshell.UserTrackerMessage;
import com.aliyun.homeshell.Workspace;
import com.aliyun.homeshell.appclone.AppCloneManager;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.icon.BubbleController;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.setting.HomeShellSetting;
import com.aliyun.utility.FeatureUtility;

public class Hideseat extends SmoothPagedView implements DropTarget, DragSource,
                                    View.OnLongClickListener, DragListener, ReorderAnimationListener {

    private static final String TAG = "Hideseat";

    //private Launcher mLauncher;
    private CellLayout mContent;

    private int mCellCountX;
    private int mCellCountY;
    private int mMaxCount;
    private int mMaxPages;

    boolean mItemsInvalidated = false;

    private DragObject mDraObj;
    private TextView mHintTextView;
    private View mCurrentDragView;
    private View mOriginalDragView;
    private ItemInfo mCurrentDragInfo;

    private int[] mTargetCell = new int[2];
    private int[] mPreviousTargetCell = new int[2];
    private int[] mEmptyCell = new int[2];
    private Alarm mReorderAlarm = new Alarm();
    private DragController mDragController;
    private PageIndicatorView mIndicator;

    private ArrayList<View> mItemsInReadingOrder = new ArrayList<View>();

    /* YUNOS BEGIN */
    // ##date:2014/10/16 ##author:zhanggong.zg ##BugID:5252746
    /** Indicates how many reorder animation is being played in hide-seat. */
    private int mCurReorderAnimationCount = 0;
    /** A flag that indicates {@link #checkLayoutConsistency()} should be called. */
    private boolean mNeedsCheckLayoutConsistency = false;
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2014/10/29 ##author:zhanggong.zg ##BugID:5383958
    /** @see #onConfigurationChanged(Configuration) */
    private int mCurrentOrientation = Configuration.ORIENTATION_PORTRAIT;
    /* YUNOS END */

    private boolean mNeedsUpdateItemInDatabase = false;

    /*YUNOS BEGIN*/
    //##date:2014/08/15 ##author:zhanggong.zg ##BugID:5186578
    /**
     * Compares two shortcuts by their screen positions.
     */
    public static final Comparator<ShortcutInfo> sItemOrderComparator = new Comparator<ShortcutInfo>() {
        @Override
        public int compare(ShortcutInfo item1, ShortcutInfo item2) {
            if (item1.screen == item2.screen) {
                return item1.cellY == item2.cellY
                       ? item1.cellX - item2.cellX
                       : item1.cellY - item2.cellY;
            } else {
                return item1.screen - item2.screen;
            }
        }
    };
    /**
     * Iteration of all hide-seat screen positions in ascending order.
     */
    public static Iterable<ScreenPosition> getScreenPosGenerator() {
        return new Iterable<ScreenPosition>() {
            private final int CELLS_X = ConfigManager.getHideseatMaxCountX();
            private final int CELLS_Y = ConfigManager.getHideseatMaxCountY();
            private final int CELLS_PER_PAGE = CELLS_X * CELLS_Y;
            private final int MAX_COUNT = ConfigManager.getHideseatItemsMaxCount();
            @Override
            public Iterator<ScreenPosition> iterator() {
                return new Iterator<ScreenPosition>() {
                    private int index = 0;
                    @Override public boolean hasNext() {
                        return index < MAX_COUNT;
                    }
                    @Override public ScreenPosition next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        int pageIndex = index % CELLS_PER_PAGE;
                        int x = pageIndex % CELLS_X;
                        int y = pageIndex / CELLS_X;
                        return new ScreenPosition(index++ / CELLS_PER_PAGE, x, y);
                    }
                    @Override public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
    /*YUNOS END*/

    /* YUNOS BEGIN */
    // ##date:2014/9/28 ##author:zhanggong.zg ##BugID:5306090
    /**
     * Determines whether hide-seat is enabled on current operating system
     * or not.
     */
    public static boolean isHideseatEnabled() {
        return FeatureUtility.hasHideSeatFeature() && !ConfigManager.isLandOrienSupport();
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2015/1/15 ##author:zhanggong.zg ##BugID:5705812
    // Maintains a list that contains all the package names of which items
    // that are in hide-seat.
    private static final Set<String> sSharedFrozenPackageList = new HashSet<String>();

    public static boolean containsSamePackageOf(ItemInfo item) {
        String packageName = getPackageName(item);
        if (packageName == null) return false;
        synchronized (sSharedFrozenPackageList) {
            return sSharedFrozenPackageList.contains(packageName);
        }
    }

    private static void addPackageToFrozenList(String packageName) {
        if (TextUtils.isEmpty(packageName)) return;
        synchronized (sSharedFrozenPackageList) {
            if (!sSharedFrozenPackageList.contains(packageName)) {
                sSharedFrozenPackageList.add(packageName);
            }
        }
    }

    public static void removePackageFromFrozenList(String packageName) {
        if (TextUtils.isEmpty(packageName)) return;
        synchronized (sSharedFrozenPackageList) {
            if (sSharedFrozenPackageList.contains(packageName)) {
                sSharedFrozenPackageList.remove(packageName);
            }
        }
    }

    private static String getPackageName(ItemInfo item) {
        if (!(item instanceof ShortcutInfo)) return null;
        Intent intent = ((ShortcutInfo) item).intent;
        ComponentName cmpt = intent != null ? intent.getComponent() : null;
        String packageName = cmpt != null ? cmpt.getPackageName() : null;
        return packageName;
    }

    private static void clearFrozenPackageList() {
        synchronized (sSharedFrozenPackageList) {
            sSharedFrozenPackageList.clear();
        }
    }
    /* YUNOS END */

    public Hideseat(Context context) {
        this(context, null);
    }

    public Hideseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hideseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContentIsRefreshable = false;
        mFadeInAdjacentScreens = false;
        setDataIsReady();
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setChildrenDrawnWithCacheEnabled(true);

        /* YUNOS BEGIN */
        //##date:2014/04/16 ##author:nater.wg ##BugID:110407
        // Get values of configures from ConfigManager
/*
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.Hotseat, defStyle, 0);
        mCellCountX = a.getInt(R.styleable.Hotseat_cellCountX, 4);
        mCellCountY = a.getInt(R.styleable.Hotseat_cellCountY, 1);
        a.recycle();
        mMaxCount = mCellCountX * mCellCountY * MAX_PAGES;
*/
        /*YUNOS BEGIN*/
        //##date:2014/10/29 ##author:zhangqiang.zq
        // aged mode
        mCellCountX = ConfigManager.getHideseatMaxCountX();
        /*YUNOS END*/
        mCellCountY = ConfigManager.getHideseatMaxCountY();
        mMaxCount = ConfigManager.getHideseatItemsMaxCount();
        /* YUNOS END */
        mMaxPages = mMaxCount / (mCellCountX * mCellCountY);

        setPageSwitchListener(new PageSwitchListener() {
            @Override
            public void onPageSwitch(View newPage, int newPageIndex) {
                Log.d(TAG, "onPageSwitch newPageIndex : " + newPageIndex);

                if (mDraObj != null) {
                    DragObject dragObj = mDraObj;
                    onDragExit(dragObj);
                    mContent = (CellLayout) newPage;
                    onDragEnter(dragObj);
                } else {
                    mContent = (CellLayout) newPage;
                }
                
                updateIndicator();
            }
        });

        //Hideseat don't need to change pageIndicator
        setNeedPageIndicator(false);

        init();
    }

    final private void init() {
        mCenterPagesVertically = false;
    }

    void setHintView(TextView hintView) {
        mHintTextView = hintView;
        switchHintVisibility();
    }

    void setPageIndicator(PageIndicatorView indicator) {
        mIndicator = indicator;
        updateIndicator();
    }        

    private void updateIndicator() {
        if (mIndicator != null) {
            mIndicator.setMax(getChildCount());
            if (!isLayoutRtl()) {
                mIndicator.setCurrentPos(getCurrentPage());
            } else {
                mIndicator.setCurrentPos(getChildCount() - getCurrentPage() - 1);
            }
            mIndicator.invalidate();
        }
    }

    public void setup(Launcher launcher) {
        mLauncher = launcher;
        /* YUNOS BEGIN */
        // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
        mDragController = new DragController(mLauncher);
        /* YUNOS END */
        setOnKeyListener(new HideseatIconKeyListener());
    }

    private int getCellLayoutHeight() {
        if (AgedModeUtil.isAgedMode()) {
            return getResources().getDimensionPixelSize(R.dimen.hideseat_cell_height_3_3);
        } else {
            return getResources().getDimensionPixelSize(R.dimen.hideseat_cell_layout_cell_height);
        }
    }

    /* YUNOS BEGIN */
    //##date:2014/10/13 ##author:zhanggong.zg ##BugID:5325771
    /**
     * Create or setup a <code>CellLayout</code> that can be added to hide-seat.
     * If the parameter <code>view</code> is <code>null</code>, a new <code>CellLayout</code>
     * object will be created and returned. Otherwise the original <code>view</code>
     * will be returned.
     * @param view an existing {@code CellLayout} or {@code null}
     * @return a {@code CellLayout} that can be added to hide-seat
     */
    private CellLayout createOrSetupCellLayout(CellLayout view) {
        if (view == null) {
            view = (CellLayout) View.inflate(getContext(), R.layout.hideseat_screen, null);
        }
        view.setGridSize(mCellCountX, mCellCountY);
        view.setMode(Mode.HIDESEAT);
        // hideseat-specific configuration for CellLayout
        int cellHeight = getCellLayoutHeight();
        view.setCellSize(view.getCellWidth(), cellHeight,
                         view.getWidthGap(), view.getHeightGap(), true);
        // ##date:2014/10/16 ##author:zhanggong.zg ##BugID:5252746
        view.setReorderAnimationListener(this);
        return view;
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2014/10/23 ##author:zhanggong.zg ##BugID:5383958
    /**
     * This method will be called when workspace is changing layout grid
     * between 4x4 and 4x5.
     */
    public void onWorkspaceLayoutChange(int workspaceCellX, int workspaceCellY) {
        // Currently, hide-seat remains its layout when workspace is
        // changing layout gird.
        /*YUNOS BEGIN*/
        //##date:2015/3/26 ##author:sunchen.sc
        // When layout change(eg: 4*4->4*5), update the hide seat space list in launcher model
        // Recreating the hide seat array has been called before
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            CellLayout page = (CellLayout) getPageAt(i);
            page.addHideseatOccupiedToModel();
        }
        /* YUNOS END */
    }
    /* YUNOS END */

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        /*YUNOS BEGIN*/
        //##date:2014/10/29 ##author:zhangqiang.zq
        // aged mode
        if (AgedModeUtil.isAgedMode()) {
            getLayoutParams().height = getResources()
                    .getDimensionPixelSize(R.dimen.workspace_cell_height_3_3);
        }
        /*YUNOS END*/
        post(new Runnable () {
            @Override
            public void run() {
                // initialize the first page in hide-seat
                mContent = createOrSetupCellLayout(null);
                mContent.setOnLongClickListener(mLongClickListener);
                addView(mContent);
                updateIndicator();
            }
        });
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /* YUNOS BEGIN */
        // ##date:2014/10/29 ##author:zhanggong.zg ##BugID:5383958
        // Cell layout will reset its layout parameters to default values when
        // orientation is changing, even in background. Therefore, hide-seat
        // needs to re-configured the cell layout again after that.
        if (mCurrentOrientation != newConfig.orientation) {
            mCurrentOrientation = newConfig.orientation;
            post(new Runnable() {
                @Override
                public void run() {
                    final int childCount = getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        CellLayout page = (CellLayout) getPageAt(i);
                        createOrSetupCellLayout(page);
                    }
                }
            });
        }
        /* YUNOS END */
    }

    @Override
    protected void onDetachedFromWindow() {
        clearFrozenPackageList();
        super.onDetachedFromWindow();
    }

    /* YUNOS BEGIN */
    // ##module(Hideseat)
    // ##date:2014/3/25 ##author:shaoguo.wangsg@alibaba-inc.com ##BugId:104977
    // hideseat do not hint.
    private void switchHintVisibility() {
        final int N = getAllChildCount();
        int visibility = View.VISIBLE;
        if (N > 0) {
            visibility = View.GONE;
        }
        
        if (mHintTextView.getVisibility() != visibility) {
            mHintTextView.setVisibility(visibility);
        }
    }
    /* YUNOS END */

    private boolean isFull() {
        final int N = getChildCount();
        if (N < mMaxPages) {
            return false;
        }

        int count = getAllChildCount();
        return count >= mMaxCount;
    }

    private int getAllChildCount() {
        int count = 0;
        final int N = getChildCount();
        for (int i = 0; i < N; i++) {
            CellLayout layout = (CellLayout) getChildAt(i);
            count += layout.getShortcutAndWidgetContainer().getChildCount();
        }

        return count;
    }

    private boolean isCellLayoutFull(CellLayout layout) {
        final int N = layout.getShortcutAndWidgetContainer().getChildCount();
        return N == mCellCountX * mCellCountY;
    }

    private void updateItemInDatabase() {
        Log.d(TAG, "updateItemInDatabase");
        if (mNeedsUpdateItemInDatabase) {
            return;
        }
        mNeedsUpdateItemInDatabase = true;
        post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "updateItemInDatabase: begin");
                int container = Favorites.CONTAINER_HIDESEAT;
                final int M = getPageCount();
                for (int i = 0; i < M; i++) {
                    CellLayout layout = (CellLayout)getPageAt(i);
                    final int N = layout.getShortcutAndWidgetContainer().getChildCount();
                    for (int j = 0; j < N; j++) {
                        View v = layout.getShortcutAndWidgetContainer().getChildAt(j);
                        ItemInfo info = (ItemInfo) v.getTag();
                        //info.cellY = 0;
                        info.container = container;
                        info.screen = i;

                        LauncherModel.modifyItemInDatabase(mLauncher, info, info.container, info.screen, info.cellX,
                                info.cellY, info.spanX, info.spanY, false);
                    }
                }
                mNeedsUpdateItemInDatabase = false;
                Log.d(TAG, "updateItemInDatabase: end");
            }
        });
    }

    public CellLayout getLayout(int index) {
        return (CellLayout) getPageAt(index);
    }

    @Override
    public void syncPages() {
        
    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
        
    }

    public void onDragEnter(DragObject d) {
        /* YUNOS BEGIN */
        // ##date:2014/10/22 ##author:zhanggong.zg ##BugID:5252746,5375257
        if (!(d.dragInfo instanceof FolderInfo || d.dragInfo instanceof ShortcutInfo) ||
            (isFull() && mCurrentDragInfo == null)) { // full, and not dragging icon from hide-seat
            return;
        }
        /* YUNOS END */

        mDraObj = d;
        mPreviousTargetCell[0] = -1;
        mPreviousTargetCell[1] = -1;

        if (mCurrentDragInfo == null) {
            if (isCellLayoutFull(mContent)) {
                dominosBackward(indexOfChild(mContent), true);
            }

            mEmptyCell[0] = findEmptyCellX(mContent);
            mEmptyCell[1] = 0;

            mCurrentDragInfo = (ItemInfo) d.dragInfo;
            if (mCurrentDragInfo instanceof ShortcutInfo) {
                mCurrentDragView = mLauncher.createShortcut(R.layout.application,
                        null, (ShortcutInfo) mCurrentDragInfo);
                CellLayout.LayoutParams lp = new CellLayout.LayoutParams(mEmptyCell[0],mEmptyCell[1], 1, 1);
                mCurrentDragView.setLayoutParams(lp);
                mCurrentDragView.setOnLongClickListener(this);
            } else if (mCurrentDragInfo instanceof FolderInfo) {
                /* YUNOS BEGIN */
                // ##date:2014/12/3 ##author:zhanggong.zg ##BugID:5617589
                mCurrentDragView = d.dragView;
                /* YUNOS END */
            }
        }

        Log.d(TAG, "onDragEnter X : " + mEmptyCell[0] + " mCurrentDragInfo : " + mCurrentDragInfo.title);
    }
    
    public void onDragExit(DragObject d) {
        mReorderAlarm.cancelAlarm();
        boolean animation = true;
        if (mAnimToNextPositon != null) {
            mAnimToNextPositon.cancel();
            mAnimToNextPositon = null;
            //animation = false;
        }

        if (d.dragComplete) {
            
        } else {
            if (mCurrentDragInfo != null) {
                mTargetCell[0] = findEmptyCellX(mContent);
                mTargetCell[1] = 0;
                realTimeReorder(mContent, mEmptyCell, mTargetCell);
                mCurrentDragInfo = null;
                dominosForward(indexOfChild(mContent), animation);
            }
        }
        
        mDraObj = null;
    }

    public void onDragOver(DragObject d) {
        if (mCurrentDragInfo == null) {
            return;
        }
        
        if(CheckVoiceCommandPressHelper.isPushTalkCanUse()) {
            CheckVoiceCommandPressHelper.getInstance().checkDragRegion(d, false, null);
        }

        float[] r = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset, d.dragView, null);
        mTargetCell = mContent.findNearestArea((int) r[0], (int) r[1], 1, 1, mTargetCell);

        if (mTargetCell[0] != mPreviousTargetCell[0] || mTargetCell[1] != mPreviousTargetCell[1]) {
            mReorderAlarm.cancelAlarm();
            mReorderAlarm.setOnAlarmListener(mReorderAlarmListener);
            mReorderAlarm.setAlarm(150);
            mPreviousTargetCell[0] = mTargetCell[0];
            mPreviousTargetCell[1] = mTargetCell[1];
        }
    }
    
    public void onDrop(DragObject d) {
        if (mCurrentDragInfo == null) {
            return;
        }

        ItemInfo item = null;
        if (d.dragInfo instanceof ApplicationInfo) {
            item = ((ApplicationInfo) d.dragInfo).makeShortcut();
            item.spanX = 1;
            item.spanY = 1;
        } else {
            item = (ItemInfo) d.dragInfo;
        }

        boolean isFolder = false;
        List<ShortcutInfo> folderContent = null;
        if (item == mCurrentDragInfo) {
            List<View> dropViews = null;

            if (mCurrentDragInfo instanceof ShortcutInfo) {
                dropViews = new ArrayList<View>();
                dropViews.add(mCurrentDragView);
            } else if (mCurrentDragInfo instanceof FolderInfo){
                FolderInfo folderInfo = (FolderInfo) mCurrentDragInfo;
                dropViews = createSubviewsFromFolder(folderInfo);
                isFolder = true;
                // Retrieve a copy of all items that contained by the folder
                folderContent = new ArrayList<ShortcutInfo>(folderInfo.contents);
            }
            /* YUNOS BEGIN */
            // ##date:2014/4/21 ##author:hongchao.ghc ##BugID:111144
            int size = 0;
            if (dropViews != null) {
                size = dropViews.size();
            }
            /* YUNOS END */
            long targetid = item.id;
            for (int i = size - 1; i >= 0; i--) {
                View v = dropViews.get(i);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
                CellLayout layout = findDropEmptySpace(mContent, mEmptyCell);
  
                ItemInfo info = (ItemInfo) v.getTag();
                if (info instanceof ShortcutInfo) {
                    if (info.id == targetid) {
                        UserTrackerHelper.sendDragIconReport(item, item.container,
                               Favorites.CONTAINER_HIDESEAT,
                               indexOfChild(layout), mEmptyCell[0], mEmptyCell[1]);
                    }
                    info.cellX = lp.cellX = mEmptyCell[0];
                    info.cellY = lp.cellY = mEmptyCell[1];
                    info.screen = indexOfChild(layout);
                    final long originalContainer = info.container;
                    info.container = Favorites.CONTAINER_HIDESEAT;
        
                    Log.d(TAG, "onDrop cellX : " + lp.cellX +
                            " cellY : " + lp.cellY +
                            " cellLayout index : " + indexOfChild(layout));
                    if (i == 0) {
                        animationAddView(layout, lp, (int)item.id, d, v);
                    } else {
                        layout.getShortcutAndWidgetContainer().setupLp(lp);
                        layout.getShortcutAndWidgetContainer().addView(v);
                        layout.animateChildToPosition(v, info.cellX, info.cellY,
                                REORDER_ANIMATION_DURATION, 0, true, false);
                    }

                    /* YUNOS BEGIN */
                    // ##date:2014/7/21 ##author:zhanggong.zg ##BugID:5244146
                    // freeze app
                    freezeApp((ShortcutInfo) info);
                    if (v instanceof BubbleTextView &&
                        originalContainer != Favorites.CONTAINER_HIDESEAT) {
                        ((BubbleTextView) v).setFadingEffectEnable(true, true);
                    }
                    /* YUNOS END */
                }
            }

            Log.d(TAG, "onDrop : " + mContent.getShortcutAndWidgetContainer().getChildCount());
            mItemsInvalidated = true;

            /* YUNOS BEGIN */
            // ##date:2014/08/14 ##author:zhanggong.zg ##BugID:5187092
            // removes the original folder from database
            if (isFolder) {
                FolderInfo folderInfo = (FolderInfo) mCurrentDragInfo;
                LauncherModel.deleteItemFromDatabase(mLauncher, folderInfo);
                mLauncher.removeFolder(folderInfo);
            }
            /* YUNOS END */
        }

        //BugID:5717551:userTrack for hide in
        Map<String, String> param = new HashMap<String, String>();
        if (isFolder == true) {
            param.put("type", "folder");
            param.put("FolderName", ((FolderInfo) mCurrentDragInfo).title.toString());
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_HIDESEAT_IN, param);
            UserTrackerHelper.sendDragIconReport(mCurrentDragInfo,
                                                 mCurrentDragInfo.container,
                                                 Favorites.CONTAINER_HIDESEAT,
                                                 -1, -1, -1);
        } else if (mCurrentDragInfo instanceof ShortcutInfo) {
            param.put("type", "app");
            Intent itemIntent = ((ShortcutInfo)mCurrentDragInfo).intent;
            if ((itemIntent != null) && (itemIntent.getComponent() != null)){
                param.put("PkgName", itemIntent.getComponent().getPackageName());
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_HIDESEAT_IN, param);
            }
        }

        switchHintVisibility();
        updateItemInDatabase();
        /* YUNOS BEGIN */
        // ##date:2014/7/21 ##author:zhanggong.zg ##BugID:5244146
        // automatically move other shortcuts in same package
        boolean associatedIconsMoved = false;
        try {
            if (item == mCurrentDragInfo && mCurrentDragInfo instanceof ShortcutInfo) {
                ShortcutInfo shortcutInfo = (ShortcutInfo) mCurrentDragInfo;
                mCurrentDragInfo = null;
                moveAssociatedIconsToHideseat(shortcutInfo);
                associatedIconsMoved = true;
                mLauncher.getWorkspace().removeWidgetsByShortcuts(Arrays.asList(shortcutInfo));
            } else if (isFolder) {
                mCurrentDragInfo = null;
                moveAssociatedIconsToHideseat(folderContent);
                associatedIconsMoved = true;
                mLauncher.getWorkspace().removeWidgetsByShortcuts(folderContent);
            } else {
                mCurrentDragInfo = null;
            }
        } catch (Exception ex) {
            if (!associatedIconsMoved) {
                Log.e(TAG, "moveAssociatedIconsToHideseat exception", ex);
            } else {
                Log.e(TAG, "removeWidgetsByShortcuts exception", ex);
            }
        }
        // ##date:2014/10/16 ##author:zhanggong.zg ##BugID:5252746
        setNeedsCheckLayoutConsistency();
        // ##date:2015/1/19 ##author:zhanggong.zg ##BugID:5716493
        LauncherModel.postCheckNoSpaceList();
        /* YUNOS END */
    }

    /* YUNOS BEGIN */
    // ##date:2014/12/3 ##author:zhanggong.zg ##BugID:5617589
    private List<View> createSubviewsFromFolder(FolderInfo folderInfo) {
        List<View> children = new ArrayList<View>();
        for (ShortcutInfo info : folderInfo.contents) {
            BubbleTextView view = (BubbleTextView) mLauncher.createShortcut(R.layout.application,
                    null, info);
            CellLayout.LayoutParams lp = new CellLayout.LayoutParams(0, 0, 1, 1);
            view.setLayoutParams(lp);
            view.setOnLongClickListener(this);
            BubbleController.setMode(view, Mode.HIDESEAT);
            children.add(view);
        }
        Log.d(TAG, "getFolderChildren size : " + children.size());
        return children;
    }
    /* YUNOS END */

    private int findEmptyCellX(CellLayout layout) {
        int emptyX = layout.getShortcutAndWidgetContainer().getChildCount();
        if (emptyX >= mCellCountX) {
            // while dominosBackward animation is true, will add icon for animation,
            // emptyX is more than mCellCountX.
            emptyX = mCellCountX - 1;
        }

        return emptyX;
    }

    private CellLayout findDropEmptySpace(CellLayout curLayout, int empty[]) {
        int index = indexOfChild(curLayout);

        // if current screen is first screen or not empty, drop to current screen.
        if (index == 0
            || curLayout.hasChild()) {
            if (curLayout.getShortcutAndWidgetContainer().getChildAt(empty[0], empty[1]) != null) {
                dominosBackward(index, false);
                //after dominosBackward,  current screen last position is null.
                int EMPTY[] = {findEmptyCellX(curLayout), 0};
                int TARGET[] = {empty[0], empty[1]};
                realTimeReorder(curLayout, EMPTY, TARGET);
            }
            return curLayout;
        }

        for (int i = index - 1; i >= 0; i--) {
            CellLayout layout = (CellLayout) getChildAt(i);
            if (!isCellLayoutFull(layout)) {
                empty[0] = findEmptyCellX(layout);
                empty[1] = 0;
                return layout;
            }
        }
        
        return curLayout;
    }

    private void dominosForward(int start, boolean animation) {
        int N = getChildCount();
        Log.d(TAG, "dominosForward start : " + start + " N : " + N);
        for (int i = start; i < N; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            
            if (i == start) {
                CellLayout nextLayout = (CellLayout) getChildAt(i + 1);
                if (nextLayout != null) {
                    View child = nextLayout.getShortcutAndWidgetContainer().getChildAt(0, 0);
                    if (child != null) {
                        nextLayout.getShortcutAndWidgetContainer().removeView(child);
                        ItemInfo info = (ItemInfo) child.getTag();
                        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                        if (animation) {
                            info.cellX = lp.cellX = mCellCountX;
                            info.cellY = lp.cellY = 0;
                            info.screen = i;
                            cl.getShortcutAndWidgetContainer().setupLp(lp);
                            cl.getShortcutAndWidgetContainer().addView(child);
                            cl.animateChildToPosition(child, mCellCountX - 1, 0,
                                    REORDER_ANIMATION_DURATION, 0, true, false);
                        } else {
                            info.cellX = lp.cellX = mCellCountX - 1;
                            info.cellY = lp.cellY = 0;
                            info.screen = i;
                            cl.addViewToCellLayout(child, -1, (int) info.id, lp, true);
                        }
                    }
                }
            } else {
                int M = cl.getShortcutAndWidgetContainer().getChildCount();

                for (int j = 0; j < M; j++) {
                    View child = cl.getShortcutAndWidgetContainer().getChildAt(j);
                    ItemInfo si = (ItemInfo) child.getTag();
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

                    si.cellX = lp.cellX = lp.cellX - 1;
                    si.cellY = lp.cellY = 0;
                }
                
                if (M == mCellCountX - 1) {
                    CellLayout layout1 = (CellLayout) getChildAt(i + 1);
                    if (layout1 != null) {
                        View child = layout1.getShortcutAndWidgetContainer().getChildAt(0, 0);
                        if (child != null) {
                            layout1.getShortcutAndWidgetContainer().removeView(child);
                            ItemInfo si = (ItemInfo) child.getTag();
                            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
    
                            si.cellX = lp.cellX = mCellCountX - 1;
                            si.cellY = lp.cellY = 0;
                            si.screen = i;
                            cl.getShortcutAndWidgetContainer().addView(child);
                        }
                    }
                }
            }
        }
    }

    private ValueAnimator mAnimToNextPositon = null;
    private void dominosBackward(int start, boolean animation) {
        int N = getChildCount();
        int count = getAllChildCount();

        if (count == N * mCellCountX * mCellCountY) {
            mAddEmptyScreen.run();
            Log.d(TAG, "dominosBackward add empty screen.");
        }

        N = getChildCount();
        int end = N - 1;

        for (int i = start; i < N; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            if (cl.getShortcutAndWidgetContainer().getChildCount() < mCellCountX) {
                end = i;
                break;
            }
        }

        Log.d(TAG, "dominosBackward start : " + start + " end : " + end + " N : " + N);

        for (int i = end; i > start; i--) {
            final CellLayout cl = (CellLayout) getChildAt(i);

            int M = cl.getShortcutAndWidgetContainer().getChildCount();

            for (int j = M - 1; j >= 0; j--) {
                View child = cl.getShortcutAndWidgetContainer().getChildAt(j);
                ItemInfo si = (ItemInfo) child.getTag();
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child
                        .getLayoutParams();

                si.cellX = lp.cellX = lp.cellX + 1;
                si.cellY = lp.cellY = 0;
            }

            if (i > start) {
                // move icon to next screen first position.
                boolean anim = animation && i - 1 == start && !isPageMoving();
                moveCellToNewPosition(i - 1, mCellCountX - 1, i, 0, anim);
            }
        }
    }

    private void moveCellToNewPosition(final int oldIndex, int oldCellX, final int newIndex, int newCellX, boolean animation) {
        final CellLayout oldLayout = (CellLayout) getChildAt(oldIndex);
        final CellLayout newLayout = (CellLayout) getChildAt(newIndex);
        final View child = oldLayout.getShortcutAndWidgetContainer().getChildAt(oldCellX, 0);
        if (child == null) {
            return;
        }

        final ItemInfo info = (ItemInfo) child.getTag();

        final CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child
                .getLayoutParams();
        if (animation) {
            // 1. move icon to next positon and dele after animation finished.
            final int oldX = lp.x;
            final int oldY = lp.y;

            final ItemInfo si = new ShortcutInfo((ShortcutInfo)info);
            si.cellX = lp.cellX = lp.cellX + 1;
            si.cellY = lp.cellY = 0;

            lp.isLockedToGrid = true;
            oldLayout.getShortcutAndWidgetContainer().setupLp(lp);
            lp.isLockedToGrid = false;
            
            final int newX = lp.x;
            final int newY = lp.y;

            ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
            mAnimToNextPositon = va;
            va.setDuration(REORDER_ANIMATION_DURATION);
            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    lp.x = (int) ((1 - r) * oldX + r * newX);
                    lp.y = (int) ((1 - r) * oldY + r * newY);
                    child.requestLayout();
                }
            });

            va.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    lp.isLockedToGrid = true;
                    oldLayout.getShortcutAndWidgetContainer().removeView(child);
                }
            });

            va.setStartDelay(150);
            va.start();

            // 2. add icon in new position.
            final View v = mLauncher.createShortcut(R.layout.application,
                    null, (ShortcutInfo) info);
            v.setOnLongClickListener(this);
            CellLayout.LayoutParams l = new CellLayout.LayoutParams(info.cellX, info.cellY, 1, 1);
            v.setLayoutParams(l);
            info.cellX = l.cellX = newCellX;
            info.cellY = l.cellY = 0;
            info.screen = newIndex;
            v.setTag(info);

            newLayout.addViewToCellLayout(v, -1, (int) info.id, l, true);
        } else {
            oldLayout.getShortcutAndWidgetContainer().removeView(child);
            info.cellX = lp.cellX = newCellX;
            info.cellY = lp.cellY = 0;
            info.screen = newIndex;

            newLayout.addViewToCellLayout(child, -1, (int) info.id, lp, true);
        }
    }

    private void animationAddView(CellLayout layout, CellLayout.LayoutParams lp, int id, DragObject d, View v) {
        layout.addViewToCellLayout(v, -1, id, lp, true);

        if (d.dragView.hasDrawn()) {
            mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, v);
        } else {
            d.deferDragViewCleanupPostAnimation = false;
            v.setVisibility(VISIBLE);
        }
    }

    protected boolean mHasDirtyData = false;
    public void onDropCompleted(View target, DragObject d, boolean isFlingToDelete,
            boolean success) {
        if (!success) {
            mCurrentDragInfo = (ItemInfo) mCurrentDragView.getTag();

            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) mCurrentDragView.getLayoutParams();
            mTargetCell[0] = lp.cellX;
            mTargetCell[1] = lp.cellY;
            CellLayout layout = (CellLayout) getPageAt(mCurrentDragInfo.screen);
            if (layout.getShortcutAndWidgetContainer().getChildAt(lp.cellX, lp.cellY) != null) {
                dominosBackward(mCurrentDragInfo.screen, false);
                int empty[] = {findEmptyCellX(layout), 0};
                realTimeReorder(layout, empty, mTargetCell);
            }
            animationAddView(layout, lp, (int)mCurrentDragInfo.id, d, mCurrentDragView);
        }
        LauncherModel.dumpUIOccupied();
        mDropAccepted = success;

        mCurrentDragInfo = null;
        mCurrentDragView = null;
        mOriginalDragView = null;

        switchHintVisibility();
        updateItemInDatabase();
        
        mLauncher.getWorkspace().checkAndRemoveEmptyCell();
        // ##date:2014/10/16 ##author:zhanggong.zg ##BugID:5252746
        setNeedsCheckLayoutConsistency();
    }

    public ArrayList<View> getItemsInReadingOrder() {
        return getItemsInReadingOrder(true);
    }

    private boolean mDropAccepted = false;
    private ArrayList<View> getItemsInReadingOrder(boolean includeCurrentDragItem) {
        if (mItemsInvalidated) {
            mItemsInReadingOrder.clear();
            for (int j = 0; j < mContent.getCountY(); j++) {
                for (int i = 0; i < mContent.getCountX(); i++) {
                    View v = mContent.getChildAt(i, j);
                    if (v != null) {
                        ItemInfo info = (ItemInfo) v.getTag();
                        if (info != mCurrentDragInfo || includeCurrentDragItem || !mDropAccepted) {
                            mItemsInReadingOrder.add(v);
                        }
                    }
                }
            }
            mItemsInvalidated = false;
        }
        return mItemsInReadingOrder;
    }

    public int getItemCount() {
        return mContent.getShortcutAndWidgetContainer().getChildCount();
    }

    OnAlarmListener mReorderAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            realTimeReorder(mContent, mEmptyCell, mTargetCell);
        }
    };

    boolean readingOrderGreaterThan(int[] v1, int[] v2) {
        if (v1[1] > v2[1] || (v1[1] == v2[1] && v1[0] > v2[0])) {
            return true;
        } else {
            return false;
        }
    }

    private static final int REORDER_ANIMATION_DURATION = 230;
    private void realTimeReorder(CellLayout layout, int[] empty, int[] target) {
        Log.d(TAG, String.format("realTimeReorder from (%d,%d) to (%d,%d)",
                                 target[0], target[1], empty[0], empty[1]));
        if (target[0] >= mCellCountX || target[1] >= mCellCountY ||
            empty[0] >= mCellCountX  || empty[1] >= mCellCountY) {
            Log.e(TAG, "realTimeReorder: invalid arguments");
            return;
        }
        boolean wrap;
        int startX;
        int endX;
        int startY;
        int delay = 0;
        float delayAmount = 30;
        if (readingOrderGreaterThan(target, empty)) {
            wrap = empty[0] >= layout.getCountX() - 1;
            startY = wrap ? empty[1] + 1 : empty[1];
            for (int y = startY; y <= target[1]; y++) {
                startX = y == empty[1] ? empty[0] + 1 : 0;
                endX = y < target[1] ? layout.getCountX() - 1 : target[0];
                for (int x = startX; x <= endX; x++) {
                    View v = layout.getChildAt(x,y);
                    if (layout.animateChildToPosition(v, empty[0], empty[1],
                            REORDER_ANIMATION_DURATION, delay, true, true)) {
                        empty[0] = x;
                        empty[1] = y;
                        delay += delayAmount;
                        delayAmount *= 0.9;
                    }
                }
            }
        } else {
            wrap = empty[0] == 0;
            startY = wrap ? empty[1] - 1 : empty[1];
            for (int y = startY; y >= target[1]; y--) {
                startX = y == empty[1] ? empty[0] - 1 : layout.getCountX() - 1;
                endX = y > target[1] ? 0 : target[0];
                for (int x = startX; x >= endX; x--) {
                    View v = layout.getChildAt(x,y);
                    if (layout.animateChildToPosition(v, empty[0], empty[1],
                            REORDER_ANIMATION_DURATION, delay, true, true)) {
                        empty[0] = x;
                        empty[1] = y;
                        delay += delayAmount;
                        delayAmount *= 0.9;
                    }
                }
            }
        }
    }

    private float[] getDragViewVisualCenter(int x, int y, int xOffset, int yOffset,
            DragView dragView, float[] recycle) {
        float res[];
        if (recycle == null) {
            res = new float[2];
        } else {
            res = recycle;
        }

        int left = x - xOffset;
        int top = y - yOffset;

        res[0] = left + dragView.getDragRegion().width() / 2;
        res[1] = top + dragView.getDragRegion().height() / 2;
        return res;
    }

    @Override
    public boolean supportsFlingToDelete() {
        return false;
    }

    @Override
    public void onFlingToDeleteCompleted() {
        
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    public void onFlingToDelete(DragObject dragObject, int x, int y, PointF vec) {
        
    }

    @Override
    public DropTarget getDropTargetDelegate(DragObject dragObject) {
        return null;
    }

    /* YUNOS BEGIN */
    // ##date:2014/7/31 ##author:zhanggong.zg ##BugID:5244146

    public void onDragIconFromHideseatToWorkspace(View view, ShortcutInfo info, Workspace workspace) {
        try {
            if (view instanceof BubbleTextView) {
                ((BubbleTextView) view).setFadingEffectEnable(false, true);
            }
            moveAssociatedIconsToWorkspace(info);
            //xiaodong.lxd #5631917 : unfreeze app after installed
            if(!info.isDownloading()) {
                unfreezeApp(info);
            }

            //BugID:5717551:userTrack for hide out
            Map<String, String> param = new HashMap<String, String>();
            param.put("type", "app");
            Intent itemIntent = info.intent;
            if ((itemIntent != null) && (itemIntent.getComponent() != null)){
                param.put("PkgName", itemIntent.getComponent().getPackageName());
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_HIDESEAT_OUT, param);
            }
        } catch (Exception ex) {
            Log.e(TAG, "moveAssociatedIconsToWorkspace exception", ex);
        }
    }

    private boolean mIsDraggingIconToFolder = false;

    /** @see #onDragIconFromHideseatToFolder(View, ShortcutInfo, Folder) */
    public boolean isDraggingIconToFolder() {
        return mIsDraggingIconToFolder;
    }

    /**
     * This method will be called when drop a hide-seat icon to a folder.
     * The <code>mIsDraggingIconToFolder</code> variable prevents this method
     * to be called recursively when move associated icons to same folder.
     */
    public void onDragIconFromHideseatToFolder(View view, ShortcutInfo info, Folder folder) {
        if (mIsDraggingIconToFolder) return;
        mIsDraggingIconToFolder = true;
        try {
            if (view instanceof BubbleTextView) {
                ((BubbleTextView) view).setFadingEffectEnable(false, true);
            } else {
                Log.w(TAG, "onDragIconFromHideseatToFolder missing corresponding view for " + info);
            }
            moveAssociatedIconsToFolder(info, folder);
          //xiaodong.lxd #5631917 : unfreeze app after installed
            if(!info.isDownloading()) {
                unfreezeApp(info);
            }

            //BugID:5717551:userTrack for hide out
            Map<String, String> param = new HashMap<String, String>();
            param.put("type", "app");
            Intent itemIntent = info.intent;
            if ((itemIntent != null) && (itemIntent.getComponent() != null)){
                param.put("PkgName", itemIntent.getComponent().getPackageName());
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_HIDESEAT_OUT, param);
            }
        } catch (Exception ex) {
            Log.e(TAG, "moveAssociatedIconsToWorkspace exception", ex);
        } finally {
            mIsDraggingIconToFolder = false;
        }
    }

    /**
     * This method is used to determine that whether there is enough space
     * in hide-seat for a dropped <code>ShortcutInfo</code>. So that hide-seat
     * will automatically move associated icons (contained by package) from workspace,
     * folders, and dock to hide-seat.
     * @param info
     * @return whether there is enough space for a drop
     * @author zhanggong.zg
     */
    private boolean hasEnoughEmptyCellsForDrop(ShortcutInfo info) {
        if (info.intent == null || info.intent.getComponent() == null) return false;
        final String pkgName = info.intent.getComponent().getPackageName();
        if (TextUtils.isEmpty(pkgName)) return false;
        return hasEnoughEmptyCellsForDrop(Arrays.asList(info));
    }

    /**
     * Similar to method {@link #hasEnoughEmptyCellsForDrop(ShortcutInfo)} but
     * with multiple dropping <code>items</code>. Note that the <code>items</code>
     * should have valid intents and component names.
     * @see #hasEnoughEmptyCellsForDrop(ShortcutInfo)
     * @param items
     * @return
     */
    private boolean hasEnoughEmptyCellsForDrop(Collection<ShortcutInfo> items) {
        Set<String> pkgNames = new HashSet<String>(items.size());
        for (ShortcutInfo item : items) {
            // ##date:2014/9/26 ##author:zhanggong.zg ##BugID:5252357
            // system app doesn't move with associated icons
            if (!item.isSystemApp) {
                // the intent, component name and package name below
                // have already been checked by caller.
                pkgNames.add(item.intent.getComponent().getPackageName());
            }
        }
        // determines extra space for associated icons
        int extra_space_needed = 0;
        if (!pkgNames.isEmpty()) {
            Map<String, Collection<ShortcutInfo>> map = LauncherModel.getAllShortcutInfoByPackageNames(pkgNames);
            for (Entry<String, Collection<ShortcutInfo>> entry : map.entrySet()) {
                for (ShortcutInfo item : entry.getValue()) {
                    boolean contained = items.contains(item);
                    if (contained && item.container == Favorites.CONTAINER_HIDESEAT) {
                        // the specified icon is already in hide-seat
                        extra_space_needed--;
                        continue;
                    } else if (contained || item.container == Favorites.CONTAINER_HIDESEAT) {
                        continue;
                    } else {
                        extra_space_needed++;
                    }
                }
            }
        }
        Log.v(TAG, "need extra empty cells: " + extra_space_needed);
        Log.v(TAG, "need total empty cells: " + (extra_space_needed + items.size()));
        if (ConfigManager.getHideseatItemsMaxCount() - getAllChildCount() >= extra_space_needed + items.size()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieves all visible icons contained specified package names in workspace,
     * folders and dock.
     * @param pkgNames
     * @param out_folderMap
     * @return a group of maps from {@code ShortcutInfo} to corresponding {@code BubbleTextView}
     * @author zhanggong.zg
     * @param origins 
     */
    private Map<String, Map<ShortcutInfo, BubbleTextView>> getAllWorkspaceIconsWithPackages(Collection<String> pkgNames, Map<ShortcutInfo, FolderIcon> out_folderMap, Collection<ShortcutInfo> origins) {
        if (pkgNames == null || pkgNames.isEmpty()) return Collections.emptyMap();
        Map<String, Map<ShortcutInfo, BubbleTextView>> result = new HashMap<String, Map<ShortcutInfo, BubbleTextView>>();
        for (String pkgName : pkgNames) {
            if (!TextUtils.isEmpty(pkgName)) {
                result.put(pkgName, new HashMap<ShortcutInfo, BubbleTextView>());
            }
        }
        if (result.isEmpty()) return result;

        for (ShortcutAndWidgetContainer container : mLauncher.getWorkspace().getAllShortcutAndWidgetContainers()) {
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof BubbleTextView) {
                    BubbleTextView icon = (BubbleTextView) child;
                    Object tag = icon.getTag();
                    //if (tag instanceof ShortcutInfo && icon.getVisibility() == View.VISIBLE) {
                    if (tag instanceof ShortcutInfo) {
                        ShortcutInfo info = (ShortcutInfo) tag;
                        ComponentName cmpt = info.intent != null ? info.intent.getComponent() : null;
                        String iconPkgName = cmpt != null ? cmpt.getPackageName() : null;
                        if (result.containsKey(iconPkgName)) {
                            if (isNotInOriginsList(origins, info)) {
                                result.get(iconPkgName).put(info, icon);
                            }
                        }
                    }
                } else if (child instanceof FolderIcon) {
                    FolderIcon icon = (FolderIcon) child;
                    Folder folder = icon.getFolder();
                    final int childCount = folder.getItemCount();
                    for (int j = 0; j < childCount; j++) {
                        View folderChild = folder.getItemAt(j);
                        if ((folderChild != null) && (folderChild instanceof BubbleTextView)) {
                            BubbleTextView internalIcon = (BubbleTextView) folderChild;
                            Object tag = folderChild.getTag();
                            if (tag instanceof ShortcutInfo) {
                                ShortcutInfo info = (ShortcutInfo) tag;
                                ComponentName cmpt = info.intent != null ? info.intent.getComponent() : null;
                                String iconPkgName = cmpt != null ? cmpt.getPackageName() : null;
                                if (result.containsKey(iconPkgName)) {
                                    result.get(iconPkgName).put(info, internalIcon);
                                    if (out_folderMap != null) out_folderMap.put(info, icon);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean isNotInOriginsList(Collection<ShortcutInfo> origins,
            ShortcutInfo info) {
        for (ShortcutInfo tmpinfo : origins) {
            if (tmpinfo.id == info.id) {
                return false;
            }
        }
        return true;
    }

    private void moveAssociatedIconsToHideseat(ShortcutInfo origin) {
        if (origin == null) return;
        moveAssociatedIconsToHideseat(Arrays.asList(origin));
    }

    /**
     * Automatically move associated icons (contained by same package) from workspace,
     * folders, and dock to hide-seat. This method only applies to non-system apps.
     * @param origins the items that have already moved to workspace
     * @author zhanggong.zg
     */
    private void moveAssociatedIconsToHideseat(Collection<ShortcutInfo> origins) {
        if (origins == null || origins.isEmpty()) return;
        final Set<String> pkgNames = new HashSet<String>();
        for (ShortcutInfo item : origins) {
            if (item.isSystemApp) continue; // ##BugID:5252357
            ComponentName cmpt = item.intent != null ? item.intent.getComponent() : null;
            String pkgName = cmpt != null ? cmpt.getPackageName() : null;
            if (!TextUtils.isEmpty(pkgName)) {
                pkgNames.add(pkgName);
            }
        }
        if (pkgNames.isEmpty()) return;

        Map<ShortcutInfo, FolderIcon> folderMap = new HashMap<ShortcutInfo, FolderIcon>();
        Map<String, Map<ShortcutInfo, BubbleTextView>> iconMap = getAllWorkspaceIconsWithPackages(pkgNames, folderMap, origins);
        Map<String, Collection<ShortcutInfo>> noSpaceItemMap = LauncherModel.getAllNoSpaceItemsWithPackages(pkgNames);
        int count = 0;
        for (Map<ShortcutInfo, BubbleTextView> submap : iconMap.values()) {
            count += submap.size();
        }
        for (Collection<ShortcutInfo> sublist : noSpaceItemMap.values()) {
            count += sublist.size();
        }
        if (count == 0) return;

        ScreenPosition[] availablePosArray = LauncherModel.findEmptyCellsInHideSeatAndOccupy(count);
        if (availablePosArray == null) return;
        final List<ScreenPosition> availablePos = new ArrayList<ScreenPosition>(
                Arrays.asList(availablePosArray));
        boolean hotseatModifFlag = false;

        for (Map<ShortcutInfo, BubbleTextView> submap : iconMap.values()) {
            for (Entry<ShortcutInfo, BubbleTextView> entry : submap.entrySet()) {
                ShortcutInfo sinfo = entry.getKey();
                BubbleTextView icon = entry.getValue();
                if (origins.contains(sinfo)) continue;
                else if (sinfo.itemType != Favorites.ITEM_TYPE_APPLICATION) continue;
                else if (sinfo.container == Favorites.CONTAINER_HIDESEAT) continue;
                else if (sinfo.container == Favorites.CONTAINER_HOTSEAT ||
                         sinfo.container == Favorites.CONTAINER_DESKTOP) {
                    if (sinfo.container == Favorites.CONTAINER_HOTSEAT) {
                        // hot-seat => hide-seat
                        Log.v(TAG, "move associated icon from hot-seat to hide-seat: " + sinfo.title);
                        hotseatModifFlag = true;
                    } else {
                        // workspace => hide-seat
                        LauncherModel.assignPlace(sinfo.screen, sinfo.cellX, sinfo.cellY, sinfo.spanX, sinfo.spanY, false, CellLayout.Mode.NORMAL);
                        Log.v(TAG, "move associated icon from workspace to hide-seat: " + sinfo.title);
                    }

                    // remove icon from workspace
                    ViewGroup parent = (ViewGroup) icon.getParent();
                    parent.removeView(icon);

                    // update info
                    ScreenPosition pos = availablePos.remove(0);
                    sinfo.container = Favorites.CONTAINER_HIDESEAT;
                    sinfo.screen = pos.s;
                    sinfo.cellX = pos.x;
                    sinfo.cellY = pos.y;

                    // add icon to hide-seat
                    BubbleTextView newIcon = (BubbleTextView) mLauncher.createShortcut(R.layout.application, null, sinfo);
                    addInScreen(newIcon, Favorites.CONTAINER_HIDESEAT, pos.s, pos.x, pos.y, 1, 1, true);
                    newIcon.setFadingEffectEnable(true, true);
                } else {
                    // folder => hide-seat
                    Log.v(TAG, "move associated icon from folder to hide-seat: " + sinfo.title);

                    FolderIcon folderIcon = folderMap.get(sinfo);
                    Folder folder = folderIcon != null ? folderIcon.getFolder() : null;
                    FolderInfo folderInfo = folder != null ? folder.getInfo() : null;
                    if (folderInfo == null) continue;

                    // remove icon from workspace
                    folderInfo.remove(sinfo);

                    // update info
                    ScreenPosition pos = availablePos.remove(0);
                    sinfo.container = Favorites.CONTAINER_HIDESEAT;
                    sinfo.screen = pos.s;
                    sinfo.cellX = pos.x;
                    sinfo.cellY = pos.y;

                    // add icon to hide-seat
                    BubbleTextView newIcon = (BubbleTextView) mLauncher.createShortcut(R.layout.application, null, sinfo);
                    addInScreen(newIcon, Favorites.CONTAINER_HIDESEAT, pos.s, pos.x, pos.y, 1, 1, true);
                    newIcon.setFadingEffectEnable(true, true);
                }
            }
        }

        // ##date:2015/1/14 ##author:zhanggong.zg ##BugID:5705812
        // no-space items
        for (Entry<String, Collection<ShortcutInfo>> entry : noSpaceItemMap.entrySet()) {
            for (ShortcutInfo info : entry.getValue()) {
                if (info.itemType != Favorites.ITEM_TYPE_NOSPACE_APPLICATION) continue;
                ScreenPosition pos = availablePos.get(0);
                if (LauncherModel.removeItemFromNoSpaceList(info, Favorites.CONTAINER_HIDESEAT, pos.s, pos.x, pos.y)) {
                    availablePos.remove(0);
                    // add icon to hide-seat
                    BubbleTextView newIcon = (BubbleTextView) mLauncher.createShortcut(R.layout.application, null, info);
                    addInScreen(newIcon, Favorites.CONTAINER_HIDESEAT, pos.s, pos.x, pos.y, 1, 1, true);
                    newIcon.setFadingEffectEnable(true, true);
                }
            }
        }

        /* YUNOS BEGIN */
        // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
        // Release not occupied places
        for (ScreenPosition sp : availablePos) {
            LauncherModel.releaseHideseatPlace(sp.s, sp.x, sp.y);
        }
        /* YUNOS END */
        // update folders
        for (FolderIcon folderIcon : new HashSet<FolderIcon>(folderMap.values())) {
            Folder folder = folderIcon.getFolder();
            FolderInfo folderInfo = folder != null ? folder.getInfo() : null;
            if (folderInfo == null) continue;
            if (folderInfo.count() <= 1) {
                // remove or replace the empty folder
                folder.replaceFolderWithFinalItem(false);
            }
        }

        // update hide-seat
        updateItemInDatabase();

        // update hot-seat
        if (hotseatModifFlag) {
            Hotseat hotseat = mLauncher.getHotseat();
            hotseat.updateItemCell();
            hotseat.updateItemInDatabase();
        }

        folderMap.clear();
        folderMap = null;
        for (Map<ShortcutInfo, BubbleTextView> submap : iconMap.values()) {
            submap.clear();
        }
        iconMap.clear();
        iconMap = null;
        noSpaceItemMap.clear();
        noSpaceItemMap = null;
    }

    /* YUNOS BEGIN */
    // ##date:2015/1/15 ##author:zhanggong.zg ##BugID:5705812
    private ScreenPosition findAvailablePosition() {
        final int max_cell_cnt = getMaxCellsPerPage();
        int i;
        for (i = 0; i < ConfigManager.getHideseatScreenMaxCount(); i++) {
            CellLayout layout = (CellLayout) getPageAt(i);
            if (layout == null) {
                return new ScreenPosition(i, 0, 0);
            }
            final int cnt = layout.getShortcutAndWidgetContainer().getChildCount();
            if (cnt < max_cell_cnt) {
                return new ScreenPosition(i,
                        cnt % ConfigManager.getHideseatMaxCountX(),
                        cnt / ConfigManager.getHideseatMaxCountX());
            }
        }
        return null;
    }

    public void bindItemInHideseat(ShortcutInfo item) {
        if (item == null) return;
        Log.v(TAG, "bindItemInHideseat: begin");
        ScreenPosition pos = findAvailablePosition();
        if (pos == null) {
            Log.e(TAG, "bindItemInHideseat: pos == null");
            return;
        }
        Log.v(TAG, "bindItemInHideseat: pos = " + pos);

        // update info
        item.container = Favorites.CONTAINER_HIDESEAT;
        item.screen = pos.s;
        item.cellX = pos.x;
        item.cellY = pos.y;

        // add icon to hide-seat
        BubbleTextView newIcon = (BubbleTextView) mLauncher.createShortcut(R.layout.application, null, item);
        addInScreen(newIcon, Favorites.CONTAINER_HIDESEAT, pos.s, pos.x, pos.y, 1, 1, true);
        newIcon.setFadingEffectEnable(true, true);

        updateIndicator();
        updateItemInDatabase();
        checkLayoutConsistency();
        Log.v(TAG, "bindItemInHideseat: end");
    }
    /* YUNOS END */

    /**
     * Automatically moves associated (contained by same package) icons from hide-seat to workspace.
     * This method only applies to non-system apps.
     * @param origin the item that is already moved to workspace
     * @author zhanggong.zg
     */
    private void moveAssociatedIconsToWorkspace(ShortcutInfo origin) {
        if (origin.intent == null || origin.intent.getComponent() == null) return;
        if (origin.isSystemApp) return; // ##BugID:5252357
        final String pkgName = origin.intent.getComponent().getPackageName();
        if (TextUtils.isEmpty(pkgName)) return;
        Map<ShortcutInfo, BubbleTextView> iconMap = getAllHideseatIconsWithPackage(pkgName);
        if (!iconMap.isEmpty()) {
            moveHideseatIconsToWorkspace(iconMap, origin.screen);
            iconMap.clear();
        }
        iconMap = null;
    }

    /**
     * Moves icons from hide-seat to workspace.
     * @param iconMap
     * @param preferredScreen
     * @author zhanggong.zg
     */
    private void moveHideseatIconsToWorkspace(Map<? extends ItemInfo, BubbleTextView> iconMap, int preferredScreen) {
        if (iconMap.isEmpty()) return;
        boolean noSpaceFlag = false;
        int reqCount = iconMap.size();
        ArrayList<ScreenPosition> availablePos = new ArrayList<ScreenPosition>(reqCount);
        if (preferredScreen == 0) {
            LauncherModel.getEmptyPosListAndOccupy(availablePos, 0, ConfigManager.getIconScreenMaxCount() - 1, reqCount);
        } else if (preferredScreen == 1) {
            LauncherModel.getEmptyPosListAndOccupy(availablePos, 1, ConfigManager.getIconScreenMaxCount() - 1, reqCount);
        } else if (preferredScreen == ConfigManager.getIconScreenMaxCount() - 1) {
            LauncherModel.getEmptyPosListAndOccupy(availablePos, preferredScreen, 1, reqCount);
        } else {
            LauncherModel.getEmptyPosListAndOccupy(availablePos, preferredScreen, ConfigManager.getIconScreenMaxCount() - 1, reqCount);
            LauncherModel.getEmptyPosListAndOccupy(availablePos, preferredScreen - 1, 1, reqCount);
        }
        for (Entry<? extends ItemInfo, BubbleTextView> entry : iconMap.entrySet()) {
            ItemInfo info = entry.getKey();
            BubbleTextView icon = entry.getValue();
            Log.v(TAG, "move associated icon from hide-seat to workspace: " + info.title);

            // remove icon from hide-seat
            if (info.screen >= 0 && info.screen < getChildCount()) {
                CellLayout layout = (CellLayout) getPageAt(info.screen);
                if (layout != null) {
                    layout.getShortcutAndWidgetContainer().removeView(icon);
                    mTargetCell[0] = findEmptyCellX(layout);
                    mTargetCell[1] = 0;
                    mEmptyCell[0] = info.cellX;
                    mEmptyCell[1] = info.cellY;
                    realTimeReorder(layout, mEmptyCell, mTargetCell);
                    dominosForward(indexOfChild(layout), false);
                }
            }

            // create new view and update database
            if (info instanceof ShortcutInfo) {
                final ShortcutInfo sinfo = (ShortcutInfo) info;
                if (!availablePos.isEmpty()) {
                    ScreenPosition pos = availablePos.remove(0);
                    sinfo.container = Favorites.CONTAINER_DESKTOP;
                    sinfo.screen = pos.s;
                    sinfo.cellX = pos.x;
                    sinfo.cellY = pos.y;
                    if (ConfigManager.isLandOrienSupport()) {
                        sinfo.cellXLand = pos.x;
                        sinfo.cellYLand = pos.y;
                    } else {
                        sinfo.cellXPort = pos.x;
                        sinfo.cellYPort = pos.y;
                    }
                    LauncherModel.updateItemInDatabase(mContext, sinfo);
                    // add icon to workspace
                    View newIcon = mLauncher.createShortcut(R.layout.application, null, sinfo);
                    mLauncher.getWorkspace().addInScreen(newIcon, Favorites.CONTAINER_DESKTOP, pos.s, pos.x, pos.y, 1, 1, true);
                    Log.d(TAG, "move to " + pos);
                } else {
                    // ##date:2015/1/14 ##author:zhanggong.zg ##BugID:5705812
                    // no-space item
                    LauncherModel.addItemToNoSpaceList(sinfo, Favorites.CONTAINER_DESKTOP);
                    Log.d(TAG, "no space");
                    noSpaceFlag = true;
                }
            } else {
                // delete invalid item in hide-seat
                Log.v(TAG, "delete invalid item in hide-seat: " + info.title);
                LauncherModel.deleteItemFromDatabase(mContext, info);
            }
        }
        /* YUNOS BEGIN */
        // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
        // Release not occupied places
        for (ScreenPosition sp : availablePos) {
            LauncherModel.releaseWorkspacePlace(sp.s, sp.x, sp.y);
        }
        /* YUNOS END */

        mRemoveEmptyCell.run();
        updateItemInDatabase();

        if (noSpaceFlag) {
            Toast.makeText(mContext, mContext.getString(R.string.icon_not_show_no_space),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Automatically moves associated (contained by same package) icons from hide-seat to folder.
     * This method only applies to non-system apps.
     * @param origin the item that is already moved to the folder
     * @param folder the target folder
     * @author zhanggong.zg
     */
    private void moveAssociatedIconsToFolder(ShortcutInfo origin, Folder folder) {
        if (origin.intent == null || origin.intent.getComponent() == null) return;
        if (origin.isSystemApp) return; // ##BugID:5252357
        final String pkgName = origin.intent.getComponent().getPackageName();
        if (TextUtils.isEmpty(pkgName)) return;
        Map<ShortcutInfo, BubbleTextView> iconMap = getAllHideseatIconsWithPackage(pkgName);
        if (!iconMap.isEmpty()) {
            moveHideseatIconsToFolder(iconMap, folder); // note that this method will modify iconMap
            if (!iconMap.isEmpty()) {
                // Folder is full. The rest icons will be moved to workspace.
                FolderInfo info = folder.getInfo();
                moveHideseatIconsToWorkspace(iconMap, info != null ? info.screen : 1);
                iconMap.clear();
            }
        }
        iconMap = null;
    }

    /**
     * Moves icons from hide-seat to specified folder. If the icon is successfully moved
     * to folder, the entry of this icon will be removed from <code>out_iconMap</code>.
     * @param out_iconMap
     * @param folder
     * @author zhanggong.zg
     */
    private void moveHideseatIconsToFolder(Map<ShortcutInfo, BubbleTextView> out_iconMap, Folder folder) {
        if (folder == null || folder.getInfo() == null) return;
        final FolderInfo folderInfo = folder.getInfo();
        Iterator<Entry<ShortcutInfo, BubbleTextView>> itr = out_iconMap.entrySet().iterator();
        while (itr.hasNext()) {
            if (folder.isFull()) break;
            Entry<ShortcutInfo, BubbleTextView> entry = itr.next();
            ShortcutInfo info = entry.getKey();
            BubbleTextView icon = entry.getValue();
            Log.v(TAG, "move associated icon from hide-seat to folder: " + info.title);

            // remove icon from hide-seat
            if (info.screen >= 0 && info.screen < getChildCount()) {
                CellLayout layout = (CellLayout) getPageAt(info.screen);
                if (layout != null) {
                    layout.getShortcutAndWidgetContainer().removeView(icon);
                    mTargetCell[0] = findEmptyCellX(layout);
                    mTargetCell[1] = 0;
                    mEmptyCell[0] = info.cellX;
                    mEmptyCell[1] = info.cellY;
                    realTimeReorder(layout, mEmptyCell, mTargetCell);
                    dominosForward(indexOfChild(layout), false);
                }
            }

            // add icon to folder
            folderInfo.add(info);
            itr.remove(); // Successfully add to folder. Remove it from out_iconMap.
        }

        mRemoveEmptyCell.run();
        updateItemInDatabase();
    }

    /**
     * Retrieves all icons in hide-seat that contained by specified package name.
     * @param pkgName
     * @return a map from {@code ShortcutInfo} to corresponding {@code BubbleTextView}
     * @author zhanggong.zg
     */
    private Map<ShortcutInfo, BubbleTextView> getAllHideseatIconsWithPackage(String pkgName) {
        Map<ShortcutInfo, BubbleTextView> result = new HashMap<ShortcutInfo, BubbleTextView>();
        for (int i = 0; i < getPageCount(); i++) {
            CellLayout layout = (CellLayout) getPageAt(i);
            final int cnt = layout.getShortcutAndWidgetContainer().getChildCount();
            for (int j = 0; j < cnt; j++) {
                View view = layout.getShortcutAndWidgetContainer().getChildAt(j);
                Object info = view.getTag();
                if (!(info instanceof ShortcutInfo)) continue;
                if (!(view instanceof BubbleTextView)) continue;
                Intent intent = ((ShortcutInfo) info).intent;
                if (intent == null || intent.getComponent() == null) continue;
                if (pkgName.equals(intent.getComponent().getPackageName())) {
                    result.put((ShortcutInfo) info, (BubbleTextView) view);
                }
            }
        }
        return result;
    }

    /**
     * Retrieves all icons in hide-seat.
     * @return a map from {@code ShortcutInfo} to corresponding {@code BubbleTextView}
     * @author zhanggong.zg
     */
    private Map<ItemInfo, BubbleTextView> getAllHideseatIcons() {
        Map<ItemInfo, BubbleTextView> result = new HashMap<ItemInfo, BubbleTextView>();
        for (int i = 0; i < getPageCount(); i++) {
            CellLayout layout = (CellLayout) getPageAt(i);
            final int cnt = layout.getShortcutAndWidgetContainer().getChildCount();
            for (int j = 0; j < cnt; j++) {
                View view = layout.getShortcutAndWidgetContainer().getChildAt(j);
                Object info = view.getTag();
                if (!(info instanceof ItemInfo)) continue;
                if (!(view instanceof BubbleTextView)) continue;
                result.put((ItemInfo) info, (BubbleTextView) view);
            }
        }
        return result;
    }

    public void rearrangeFrozenAppsInHideseat() {
        clearFrozenPackageList();
        Map<ItemInfo, BubbleTextView> iconMap = getAllHideseatIcons();
        Iterator<ItemInfo> itr = iconMap.keySet().iterator();
        while (itr.hasNext()) {
            ItemInfo info = itr.next();
            switch (info.itemType) {
            case Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
                // the app will be frozen after download.
                itr.remove();
                break;

            case Favorites.ITEM_TYPE_APPLICATION:
                if (info instanceof ShortcutInfo) {
                    if (((ShortcutInfo) info).isSystemApp) {
                        Log.v(TAG, "check hide-seat item " + info.title + ": system app");
                        itr.remove();
                    } else {
                        Intent intent = ((ShortcutInfo) info).intent;
                        ComponentName cmpt = intent != null ? intent.getComponent() : null;
                        String pkgName = cmpt != null ? cmpt.getPackageName() : null;
                        if (!TextUtils.isEmpty(pkgName) && AppFreezeUtil.isPackageFrozen(mContext, pkgName)) {
                            // the app is already frozen.
                            Log.v(TAG, "check hide-seat item " + info.title + ": frozen");
                            addPackageToFrozenList(pkgName);
                            itr.remove();
                        } else {
                            Log.w(TAG, "check hide-seat item " + info.title + ": NOT frozen");
                        }
                    }
                }
                break;
            }
        }
        if (iconMap.isEmpty()) {
            Log.v(TAG, "check hide-seat items: OK");
            iconMap.clear();
            iconMap = null;
            return;
        }

        Log.v(TAG, "begin to rearrange icons in hide-seat");
        Map<ItemInfo, BubbleTextView> moveToWorkspace = new HashMap<ItemInfo, BubbleTextView>();
        for (Entry<ItemInfo, BubbleTextView> entry : iconMap.entrySet()) {
            ItemInfo info = entry.getKey();
            BubbleTextView icon = entry.getValue();
            switch (info.itemType) {
            case Favorites.ITEM_TYPE_APPLICATION:
                if (info instanceof ShortcutInfo) {
                    Log.v(TAG, "re-freeze hide-seat item: " + info.title);
                    Hideseat.freezeApp((ShortcutInfo) info);
                    addPackageToFrozenList(getPackageName(info));
                } else {
                    moveToWorkspace.put(info, icon);
                }
                break;

            default:
                moveToWorkspace.put(info, icon);
                break;
            }
        }

        iconMap.clear();
        iconMap = null;

        try {
            // move icons that cannot be frozen to workspace
            moveHideseatIconsToWorkspace(moveToWorkspace, 1);
        } catch (Exception e) {
            Log.e(TAG, "failed to move icon to workspace", e);
        } finally {
            Log.v(TAG, "finish to rearrange icons in hide-seat");
            moveToWorkspace.clear();
            moveToWorkspace = null;
        }
    }

    /* YUNOS END */

    @Override
    public boolean acceptDrop(DragObject d) {
        Boolean accept = null;
        if (mCurrentDragInfo == null) {
            if (isFull()) {
                Toast.makeText(getContext(), R.string.hideseat_is_full, Toast.LENGTH_SHORT).show();
                accept = false;
            } else if (d.dragInfo instanceof FolderInfo) {
                accept = acceptFolderDrop((FolderInfo) d.dragInfo);
            }

        /* YUNOS BEGIN */
        // ##date:2014/12/12 ##author:zhanggong.zg ##BugID:5635280
        // always accept an item which is dragged from hide-seat, e.g., app that is downloading.
        } else if (((ItemInfo) d.dragInfo).container == Favorites.CONTAINER_HIDESEAT) {
            accept = true;
        /* YUNOS END */

        } else {
            ItemInfo itemInfo = (ItemInfo) d.dragInfo;
            final int type = itemInfo.itemType;

            /* YUNOS BEGIN */
            // ##date:2014/9/26 ##author:zhanggong.zg ##BugID:5244146,5253138
            // determine whether the item can be frozen or not
            switch (type) {
            case Favorites.ITEM_TYPE_APPLICATION:
                if (itemInfo instanceof ShortcutInfo) {
                    ShortcutInfo si = (ShortcutInfo) itemInfo;
                    // check download status
                        if (AppCloneManager.isCloneShortcutInfo(si)
                                || AppCloneManager.getInstance().hasCloneBody(si.getPackageName())) {
                            Toast.makeText(getContext(), R.string.appclone_hint_hide_cloned,
                                    Toast.LENGTH_SHORT).show();
                            accept = false;
                        } else if (si.getAppDownloadStatus() == AppDownloadStatus.STATUS_NO_DOWNLOAD
                                ||
                                si.getAppDownloadStatus() == AppDownloadStatus.STATUS_INSTALLED) {
                            // check empty cells in hide-seat
                            if (hasEnoughEmptyCellsForDrop(si)) {
                                accept = true;
                            } else {
                                Toast.makeText(getContext(), R.string.hideseat_not_enough_space,
                                        Toast.LENGTH_SHORT).show();
                                accept = false;
                            }
                        } else if (si.getAppDownloadStatus() == AppDownloadStatus.STATUS_INSTALLING) {
                            Toast.makeText(getContext(),
                                    R.string.hideseat_does_not_accept_installing_items,
                                    Toast.LENGTH_SHORT).show();
                            accept = false;
                        } else {
                            Toast.makeText(getContext(),
                                    R.string.hideseat_does_not_accept_downloading_items,
                                    Toast.LENGTH_SHORT).show();
                            accept = false;
                        }
                }
                break;

            case Favorites.ITEM_TYPE_FOLDER:
                if (itemInfo instanceof FolderInfo) {
                    accept = acceptFolderDrop((FolderInfo) itemInfo);
                }
                break;

            // items that cannot be frozen:
            case Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
                if (itemInfo instanceof ShortcutInfo) {
                    ShortcutInfo si = (ShortcutInfo) itemInfo;
                    if (si.getAppDownloadStatus() == AppDownloadStatus.STATUS_INSTALLING) {
                        Toast.makeText(getContext(),
                                R.string.hideseat_does_not_accept_installing_items,
                                Toast.LENGTH_SHORT).show();
                        accept = false;
                    } else {
                        Toast.makeText(getContext(),
                                R.string.hideseat_does_not_accept_downloading_items,
                                Toast.LENGTH_SHORT).show();
                        accept = false;
                    }
                }
                break;
            }
            /* YUNOS END */
        }

        /* YUNOS BEGIN */
        // ##date:2014/9/26 ##author:zhanggong.zg ##BugID:5244146,5253138
        // if the accept flag is still unset, show up a default toast
        if (accept == null) {
            // ITEM_TYPE_VPINSTALL/BOOKMARK/CLOUDAPP/BOOKMARK/SHORTCUT
            Toast.makeText(getContext(),
                    R.string.hideseat_does_not_accept_this_item, Toast.LENGTH_SHORT).show();
            accept = false;
        }
        if (!accept) {
            // ##date:2014/7/22 ##author:zhanggong.zg ##BugID:5244146
            // the dragging context should be recovered
            mReorderAlarm.cancelAlarm();
            if (mAnimToNextPositon != null) {
                mAnimToNextPositon.cancel();
                mAnimToNextPositon = null;
            }
            if (mContent.getShortcutAndWidgetContainer().getChildCount() < mCellCountX) {
                mTargetCell[0] = findEmptyCellX(mContent);
                mTargetCell[1] = 0;
                realTimeReorder(mContent, mEmptyCell, mTargetCell);
                mCurrentDragInfo = null;
                dominosForward(indexOfChild(mContent), true);
            } else {
                mCurrentDragInfo = null;
            }
            // ##date:2014/10/16 ##author:zhanggong.zg ##BugID:5252746
            setNeedsCheckLayoutConsistency();
        }
        /* YUNOS END */

        Log.v(TAG, String.format("acceptDrop %s: itemInfo=%s", accept, d.dragInfo));
        return accept;
    }

    private boolean acceptFolderDrop(FolderInfo folderInfo) {
        List<ShortcutInfo> content = new ArrayList<ShortcutInfo>(folderInfo.contents);
        // check state
        for (ShortcutInfo info : content) {
            if (info.getAppDownloadStatus() != AppDownloadStatus.STATUS_NO_DOWNLOAD &&
                    info.getAppDownloadStatus() != AppDownloadStatus.STATUS_INSTALLED) {
                // contains downloading apps
                Log.v(TAG, "acceptFolderDrop false: downloading, info=" + info);
                Toast.makeText(getContext(), R.string.hideseat_does_not_accept_downloading_folder,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            if (info.itemType != Favorites.ITEM_TYPE_APPLICATION) {
                // contains non-application
                Log.v(TAG, "acceptFolderDrop false: non-application, info=" + info);
                Toast.makeText(getContext(), R.string.hideseat_does_not_accept_folder_with_non_app,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            ComponentName cmpt = info.intent != null ? info.intent.getComponent() : null;
            if (cmpt == null || TextUtils.isEmpty(cmpt.getPackageName())) {
                // missing package name
                Log.w(TAG, "acceptFolderDrop false: missing package name, info=" + info);
                Toast.makeText(getContext(), R.string.hideseat_does_not_accept_folder_with_non_app,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            if ((AppCloneManager.isCloneShortcutInfo(info) || AppCloneManager.getInstance()
                    .hasCloneBody(info.getPackageName()))) {
                Log.w(TAG, "acceptFolderDrop false: has cloneApp, info=" + info);
                Toast.makeText(getContext(), R.string.appclone_hint_hide_clone_folder,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        if (!hasEnoughEmptyCellsForDrop(content)) {
            // no enough spaces
            Log.v(TAG, "acceptFolderDrop false: no enough space");
            Toast.makeText(getContext(), R.string.hideseat_not_enough_space,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        Log.v(TAG, "acceptFolderDrop true");
        return true;
    }

    @Override
    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }
    
    public boolean onLongClick(View v) {
        if (!mLauncher.isDraggingEnabled()) {
            return true;
        }
        /*YUNOS BEGIN*/
        //##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
        if (HomeShellSetting.getFreezeValue(getContext())) {
            return true;
        }
        /*YUNOS END*/

        Object tag = v.getTag();

        ItemInfo item = (ItemInfo) tag;
        if (!v.isInTouchMode()) {
            return false;
        }

        mCurrentDragInfo = item;
        mEmptyCell[0] = item.cellX;
        mEmptyCell[1] = item.cellY;
        mCurrentDragView = v;
        mOriginalDragView = v;

        mLauncher.getWorkspace().onDragStartedWithItem(v);
        mLauncher.getWorkspace().beginDragShared(v, this);

        mContent.removeView(mCurrentDragView);
        Log.d(TAG, "onLongClick : " + item.title + " cellX : " + item.cellX + " cellY : " + item.cellY);

        return true;
    }

    /* YUNOS BEGIN */
    // ##date:2014/08/18 ##author:zhanggong.zg ##BugID:5193378
    /**
     * Returns the view that is being dragged out of hide-seat.<p>
     * This method is called by DeleteDropTarget.buildCacheBitmap() to
     * generate the icon in delete dialog.
     * @return the view or {@code null} if the current drag is complete.
     */
    public final View getDragView() {
        return mOriginalDragView;
    }
    /* YUNOS END */

    private final Runnable mAddEmptyScreen = new Runnable () {

        @Override
        public void run() {
            if (getPageCount() < mMaxPages) {
                CellLayout view = createOrSetupCellLayout(null);
                view.setOnLongClickListener(mLongClickListener);
                addView(view);
                updateIndicator();
            }
        }
    };

    public void addEmptyScreen() {
        removeCallbacks(mAddEmptyScreen);
        post(mAddEmptyScreen);
    }

    private final Runnable mRemoveEmptyCell = new Runnable (){

        @Override
        public void run() {
            int count = getChildCount();
            if (count == 1) return;

            for(int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if(child != null && child instanceof CellLayout) {
                    if(!((CellLayout)child).hasChild()) {
                        
                        ((CellLayout)getChildAt(i)).cancelFlingDropDownAnimation();
                        /*YUNOS BEGIN*/
                        //##date:2015/03/30 ##author:sunchen.sc ##BugID:5735130
                        //Remove occupy array from LauncherModel ArrayList when CellLayout removed from hide seat
                        LauncherModel.removeHideseatOccupied(i);
                        /*YUNOS END*/
                        removeViewAt(i);
                        if(mCurrentPage > i)
                            mCurrentPage--;
                        if(!isPageMoving()){
                            setCurrentPage(mCurrentPage);
                            onPageEndMoving();
                        }
                        i--;
                    }
                }
            }
            
            count = getChildCount();
            if (count == 0) {
                addEmptyScreen();
                post(new Runnable(){
                    @Override
                    public void run() {
                        mContent = (CellLayout) getChildAt(0);
                    }
                });
            }

            updateIndicator();
            Log.d(TAG, "removeEmptyScreen screen count : " + getChildCount());
        }
    };

    public void removeEmptyScreen() {
        removeCallbacks(mRemoveEmptyCell);
        post(mRemoveEmptyCell);
    }

    public void makesureAddScreenIndex(int screen) {
        while (screen >= getChildCount()) {
            View view = createOrSetupCellLayout(null);
            addView(view);
            if (mLongClickListener != null) {
                view.setOnLongClickListener(mLongClickListener);
            }
        }
        updateIndicator();
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        /* YUNOS BEGIN */
        // ##date:2014/11/7 ##author:zhanggong.zg ##BugID:5441448
        // add empty screen only when necessary
        CellLayout lastPage = (CellLayout) getPageAt(getChildCount() - 1);
        int childCount = lastPage.getShortcutAndWidgetContainer().getChildCount();
        if (childCount >= getMaxCellsPerPage()) {
            if (info instanceof ItemInfo &&
                ((ItemInfo) info).container == Favorites.CONTAINER_HIDESEAT) {
                // drag & drop inside hide-seat, no need to add empty screen
            } else {
                addEmptyScreen();
            }
        }
        /* YUNOS END */
    }

    @Override
    public void onDragEnd() {
        removeEmptyScreen();
    }

    public void addInScreen(View child, long container, int screen, int x, int y, int spanX, int spanY,
            boolean insert) {
        if( screen >= getChildCount() ){
            makesureAddScreenIndex(screen);
        }
        
        CellLayout layout = (CellLayout) getChildAt(screen);
        
        child.setOnKeyListener(null);
        if (child instanceof FolderIcon) {
            ((FolderIcon) child).setTextVisible(false);
        }

        LayoutParams genericLp = child.getLayoutParams();
        CellLayout.LayoutParams lp;
        if (genericLp == null || !(genericLp instanceof CellLayout.LayoutParams)) {
            lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
        } else {
            lp = (CellLayout.LayoutParams) genericLp;
            lp.cellX = x;
            lp.cellY = y;
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
        }

        if (layout.getShortcutAndWidgetContainer().getChildAt(x, y) != null) {
            dominosBackward(screen, false);

            int empty[] = {findEmptyCellX(layout), 0};
            int target[] = {x, y};
            realTimeReorder(layout, empty, target);
        }

        int childId = LauncherModel.getCellLayoutChildId(container, screen, x, y, spanX, spanY);
        if (!layout.addViewToCellLayout(child, insert ? 0 : -1, childId, lp, true)) {
            Log.e(TAG, "Failed in addInScreen add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout");
        }

        child.setHapticFeedbackEnabled(false);
        child.setOnLongClickListener(this);

        if (child instanceof DropTarget) {
            mDragController.addDropTarget((DropTarget) child);
        }
        
        switchHintVisibility();
    }

    /* YUNOS BEGIN */
    // ##date:2014/08/04 ##author:zhanggong.zg ##BugID:5244146
    // freeze app that is already in hide-seat
    public static void freezeApp(ShortcutInfo info) {
        if (info == null) return;
        if (info.isSystemApp) return;
        Intent intent = info.intent;
        ComponentName cmpt = intent != null ? intent.getComponent() : null;
        String pkgName = cmpt != null ? cmpt.getPackageName() : null;
        if (!TextUtils.isEmpty(pkgName)) {
            // freeze the app
            addPackageToFrozenList(pkgName);
            AppFreezeUtil.asyncFreezePackage(LauncherApplication.getContext(), pkgName);
        }
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2014/10/9 ##author:zhanggong.zg ##BugID:5244146
    // Unfreeze app. Called when dropping hide-seat icon to workspace or folder.
    private static void unfreezeApp(ShortcutInfo info) {
        if (info == null) return;
        if (info.isSystemApp) return;
        Intent intent = info.intent;
        ComponentName cmpt = intent != null ? intent.getComponent() : null;
        String pkgName = cmpt != null ? cmpt.getPackageName() : null;
        if (!TextUtils.isEmpty(pkgName)) {
            removePackageFromFrozenList(pkgName);
            AppFreezeUtil.asyncUnfreezePackage(LauncherApplication.getContext(), pkgName);
        }
    }
    /* YUNOS END */

    public void readOrderOnRemoveItem(final ItemInfo info) {
        removePackageFromFrozenList(getPackageName(info));
        // ##date:2015/5/6 ##author:zhanggong.zg ##BugID:5963860
        post(new Runnable() {
            @Override
            public void run() {
                CellLayout layout = (CellLayout) getPageAt(info.screen);
                if (layout == null) {
                    Log.d(TAG, "readOrderOnRemoveItem layout is null. screen : " + info.screen);
                    return;
                }

                mTargetCell[0] = findEmptyCellX(layout);
                mTargetCell[1] = 0;

                mEmptyCell[0] = info.cellX;
                mEmptyCell[1] = info.cellY;

                realTimeReorder(layout, mEmptyCell, mTargetCell);
                dominosForward(indexOfChild(layout), layout == mContent);
                mRemoveEmptyCell.run();
                switchHintVisibility();
                updateItemInDatabase();
                setNeedsCheckLayoutConsistency();
            }
        });
    }

    public void resetLayout() {
        final int N = getChildCount();
        for (int i = 0; i < N; i++) {
            CellLayout layout = (CellLayout) getChildAt(i);
            layout.removeAllViewsInLayout();
        }
    }

    /* YUNOS BEGIN */
    // ##date:2014/10/16 ##author:zhanggong.zg ##BugID:5252746
    // fix icon overlap and empty cell issue in hide-seat

    /**
     * Check and fix icon overlap and empty cell issues in hide-seat.
     * Do not call this method directly, use {@link #setNeedsCheckLayoutConsistency()}
     * instead.
     */
    private void checkLayoutConsistency() {
        Log.d(TAG, "checkLayoutConsistency: begin");
        if (mCurrentDragInfo != null) {
            // icon is being dragged, cancel this time
            Log.d(TAG, "checkLayoutConsistency: cancelled");
            return;
        }
        boolean dirtyFlag = false;
        List<View> icons = new ArrayList<View>(1);
        for (int pageIndex = 0; pageIndex < getChildCount(); pageIndex++) {
            CellLayout cl = (CellLayout) getPageAt(pageIndex);
            if (cl == null) break;
            ShortcutAndWidgetContainer container = cl.getShortcutAndWidgetContainer();
            final int childCount = container.getChildCount();
            if (pageIndex < getChildCount() - 1) {
                // not last page, check the number of icons in this page
                if (childCount != getMaxCellsPerPage()) {
                    Log.d(TAG, String.format("checkLayoutConsistency: page %d has %d icons",
                                             pageIndex, childCount));
                    dirtyFlag |= recoverCellLayoutConsistency(pageIndex);
                    continue; // this page is fixed
                }
            }
            // check each position in this page
            Iterator<ScreenPosition> itr = getScreenPosGenerator().iterator();
            for (int x = 0; x < childCount; x++) {
                ScreenPosition pos = itr.next();
                getIconInContainer(container, pos.x, pos.y, icons);
                if (icons.size() != 1) {
                    // icon overlaps or empty cell detected
                    Log.d(TAG, String.format("checkLayoutConsistency: pos [%d,%d,%d] has %d icons",
                                             pageIndex, pos.x, pos.y, icons.size()));
                    dirtyFlag |= recoverCellLayoutConsistency(pageIndex);
                    break;
                }
            }
        }

        if (dirtyFlag) {
            updateItemInDatabase();
            removeEmptyScreen();
            Log.d(TAG, "checkLayoutConsistency: recovered");
        } else {
            Log.d(TAG, "checkLayoutConsistency: ok");
        }
    }

    /**
     * This method will fix icon overlap and empty cell issue in cell layout
     * at specified <code>pageIndex</code>.
     * @param pageIndex
     * @return whether there is an issue detected or not
     */
    private boolean recoverCellLayoutConsistency(final int pageIndex) {
        boolean dirty = false;
        CellLayout page = (CellLayout) getPageAt(pageIndex);
        ShortcutAndWidgetContainer container = page.getShortcutAndWidgetContainer();
        CellLayout nextPage = (CellLayout) getPageAt(pageIndex + 1);
        ShortcutAndWidgetContainer nextContainer = nextPage != null ?
                                                   nextPage.getShortcutAndWidgetContainer() : null;

        Map<ShortcutInfo, View> iconMap = getIconMapInContainer(container);
        List<ShortcutInfo> infos = new ArrayList<ShortcutInfo>(iconMap.keySet());
        Collections.sort(infos, sItemOrderComparator);
        Iterator<ShortcutInfo> infoItr = infos.iterator();
        Iterator<ScreenPosition> posItr = getScreenPosGenerator().iterator();

        for (int i = 0; i < getMaxCellsPerPage(); i++) {
            ScreenPosition pos = posItr.next();
            if (infoItr.hasNext()) {
                View icon = iconMap.get(infoItr.next());
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) icon.getLayoutParams();
                if (lp.cellX != pos.x || lp.cellY != pos.y) {
                    // position is inconsistent
                    syncIconLayoutParamsAndInfo(icon, pageIndex, pos.x, pos.y);
                    icon.requestLayout();
                    dirty = true;
                }
            } else if (nextContainer != null && nextContainer.getChildCount() > 0) {
                // move an icon from next page to this page
                View icon = nextContainer.getChildAt(nextContainer.getChildCount() - 1);
                nextContainer.removeView(icon);
                syncIconLayoutParamsAndInfo(icon, pageIndex, pos.x, pos.y);
                container.addView(icon);
                icon.requestLayout();
                dirty = true;
            } else {
                // no more icon
                return dirty;
            }
        }

        // move other icons to next page
        while (infoItr.hasNext()) {
            View icon = iconMap.get(infoItr.next());
            container.removeView(icon);
            syncIconLayoutParamsAndInfo(icon, pageIndex + 1, 0, 0);
            if (nextContainer == null) {
                // add new screen
                mAddEmptyScreen.run();
                nextPage = (CellLayout) getPageAt(pageIndex + 1);
                nextContainer = nextPage != null ? nextPage.getShortcutAndWidgetContainer() : null;
                if (nextContainer == null) {
                    Log.e(TAG, "recoverCellLayoutConsistency failed: hide-seat is full");
                    return dirty;
                }
            }
            nextContainer.addView(icon);
            dirty = true;
        }
        return dirty;
    }

    private int getMaxCellsPerPage() {
        return ConfigManager.getHideseatMaxCountX() * ConfigManager.getHideseatMaxCountY();
    }

    /** @see #checkLayoutConsistency() */
    private Map<ShortcutInfo, View> getIconMapInContainer(ShortcutAndWidgetContainer container) {
        Map<ShortcutInfo, View> result = new HashMap<ShortcutInfo, View>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View icon = container.getChildAt(i);
            Object tag = icon.getTag();
            if (tag instanceof ShortcutInfo) {
                result.put((ShortcutInfo) tag, icon);
            }
        }
        return result;
    }

    /**
     * Used to fix icon overlap in hide-seat.
     * @see #checkLayoutConsistency()
     */
    private void syncIconLayoutParamsAndInfo(View icon, int s, int x, int y) {
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) icon.getLayoutParams();
        lp.cellX = x;
        lp.cellY = y;
        Object tag = icon.getTag();
        if (tag instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) tag;
            info.screen = s;
            info.cellX = x;
            info.cellY = y;
            Log.w(TAG, String.format("syncIconLayoutParamsAndInfo move %s to [%d,%d,%d]",
                                     info.title, s, x, y));
        }
    }

    /**
     * Used to identify empty cell and icon overlap in hide-seat.
     * @see #checkLayoutConsistency()
     */
    private void getIconInContainer(ShortcutAndWidgetContainer container, int x, int y, List<View> out_result) {
        out_result.clear();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
            if (lp.cellX == x && lp.cellY == y) {
                out_result.add(child);
            }
        }
    }

    /**
     * Schedules to call {@link #checkLayoutConsistency()} after all
     * reordering animations are finished.
     */
    public void setNeedsCheckLayoutConsistency() {
        Log.v(TAG, "setNeedsCheckLayoutConsistency");
        if (mCurReorderAnimationCount == 0) {
            // no animation this time
            if (!mNeedsCheckLayoutConsistency) {
                mNeedsCheckLayoutConsistency = true;
                post(new Runnable() {
                    @Override
                    public void run() {
                        checkLayoutConsistency();
                        mNeedsCheckLayoutConsistency = false;
                    }
                });
            }
        } else {
            // wait until animation finish
            mNeedsCheckLayoutConsistency = true;
        }
    }

    @Override
    public void reorderAnimationStart(CellLayout cellLayout) {
        mCurReorderAnimationCount++;
    }

    @Override
    public void reorderAnimationFinish(CellLayout cellLayout) {
        mCurReorderAnimationCount--;
        // if all animations are finished and the flag is set, then check
        // layout consistency.
        if (mCurReorderAnimationCount == 0 && mNeedsCheckLayoutConsistency) {
            checkLayoutConsistency();
            mNeedsCheckLayoutConsistency = false;
        }
    }

    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2014/11/18 ##author:zhanggong.zg ##BugID:5587353
    // for 3*3 layout
    public void adjustToThreeLayout() {
        post(new Runnable() {
            @Override
            public void run() {
                updateThreeLayout();
            }
        });
    }

    public void adjustFromThreeLayout() {
        post(new Runnable() {
            @Override
            public void run() {
                updateThreeLayout();
            }
        });
    }

    private void updateThreeLayout() {
        getLayoutParams().height = mLauncher.getCustomHideseat().getCustomeHideseatHeight();
        mCellCountX = ConfigManager.getHideseatMaxCountX();
        mCellCountY = ConfigManager.getHideseatMaxCountY();
        mMaxCount = ConfigManager.getHideseatItemsMaxCount();
        mMaxPages = mMaxCount / (mCellCountX * mCellCountY);
        List<View> icons = new ArrayList<View>();
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout page = (CellLayout) getChildAt(i);
            ShortcutAndWidgetContainer container = page.getShortcutAndWidgetContainer();
            for (int j = 0; j < container.getChildCount(); j++) {
                View icon = container.getChildAt(j);
                icons.add(icon);
                page.removeViewInLayout(icon);
            }
        }
        removeAllViews();
        mContent = createOrSetupCellLayout(null);
        mContent.setOnLongClickListener(mLongClickListener);
        addView(mContent);
        updateIndicator();
        Iterator<ScreenPosition> itr = getScreenPosGenerator().iterator();
        for (View icon : icons) {
            ScreenPosition pos = itr.next();
            addInScreen(icon, Favorites.CONTAINER_HIDESEAT, pos.s, pos.x, pos.y, 1, 1, false);
        }
    }
    /*YUNOS END*/
    @Override
    public void onChildViewAdded(View parent, View child) {
        Log.d(TAG, "onChildViewAdded() start");
        super.onChildViewAdded(parent, child);
        CellLayout view = (CellLayout)child;
        /*YUNOS BEGIN*/
        //##date:2015/03/30 ##author:sunchen.sc ##BugID:5735130
        //Add occupy array to LauncherModel ArrayList when CellLayout add to hide seat
        view.addHideseatOccupiedToModel();
        /*YUNOS END*/
        Log.d(TAG, "onChildViewAdded() end");
    }
}
