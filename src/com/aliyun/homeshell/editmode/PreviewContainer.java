
package com.aliyun.homeshell.editmode;
import com.aliyun.homeshell.Insettable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import com.aliyun.homeshell.Alarm;
import com.aliyun.homeshell.Alarm.OnAlarmListener;
import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.DragController;
import com.aliyun.homeshell.DragScroller;
import com.aliyun.homeshell.DragView;
import com.aliyun.homeshell.DropTarget;
import com.aliyun.homeshell.Folder;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherAppWidgetHost;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.PendingAddGadgetInfo;
import com.aliyun.homeshell.PendingAddItemInfo;
import com.aliyun.homeshell.PendingAddShortcutInfo;
import com.aliyun.homeshell.PendingAddWidgetInfo;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.UserTrackerHelper;
import com.aliyun.homeshell.UserTrackerMessage;
import com.aliyun.homeshell.Workspace;
import com.aliyun.homeshell.model.LauncherModel;

public class PreviewContainer extends LinearLayout implements
        OnScrollStateChangedListener, DropTarget, DragScroller,Insettable  {
    private static final String TAG = "PreviewContainer";
    
    private boolean mIsShowing = false;

    public enum PreviewContentType {
        None,
        Widgets,
        Effects,
        Wallpapers,
        Themes,
        CellLayouts,
        MultiSelect,
        FolderSelect,
        DragScreen,
        IconSort,
        Preview
    };

    private PreviewListIndicatorView mIndicatorView;
    private AdapterView<ListAdapter> mPreviewList;
    private View mAddToFolderButton;
    private Context mContext;
    private WidgetPreviewAdapter mWidgetAdapter;
    private EffectsPreviewAdapter mEffectsAdapter;
    private WallpapersPreviewAdapter mWallpapersAdapter;
    private ThemesPreviewAdapter mThemesAdapter;
    private CelllayoutPreviewAdapter mCelllayoutAdapter;
    private PreviewContentType mContentType = PreviewContentType.None;
    
    private EditModeHelper mEditModeHelper;
    private Launcher mLauncher;
    private DragController mDragController;
    private int mTargetChildIndex = -1;
    private int indicatorW;
    private int indicatorH;
    private int focusBitmapW;
    private int focusBitmapH;
    private boolean mIsFirstIn;
    private int mItemW;
    private int mItemH;
    private int mTotalCount;
    
    private static final int DRAG_NONE = 0;
    private static final int DRAG_FLING = 1;
    private static final int DRAG_HOVER = 2;
    private int mDragMode = DRAG_NONE;
    
    private static final int TIME_SNAP_PAGE = 200;
    private static final int ANIMATE_TO_POSITION_TIME = 300;
    private static final int ALARM_TIME_OUT = 480;

    private Alarm mDragHoverAlarm = new Alarm();

    private final Rect mInsets = new Rect();
    private ViewGroup mPreviewListContainer;

    @SuppressLint("NewApi")
    public PreviewContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public PreviewContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewContainer(Context context) {
        this(context, null, 0);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPreviewListContainer = (ViewGroup) findViewById(R.id.preview_list_container);
        mPreviewList = (AdapterView<ListAdapter>) findViewById(R.id.preview_list);
        if (mPreviewList instanceof VerticalListView) {
            ((VerticalListView)mPreviewList).setOnScrollStateChangedListener(this);
        } else if (mPreviewList instanceof HorizontalListView) {
            ((HorizontalListView)mPreviewList).setOnScrollStateChangedListener(this);
        }
        mIndicatorView = (PreviewListIndicatorView)findViewById(R.id.preview_list_indicator);
        mAddToFolderButton = findViewById(R.id.add_to_folder);
        mAddToFolderButton.setOnClickListener(addToFolderListener);
    }

    private void setAdapter(BaseAdapter adapter) {
        mPreviewList.setAdapter(adapter);
    }

    public AdapterView<ListAdapter> getListView() {
        return mPreviewList;
    }

    private ListAdapter getAdapter() {
        return mPreviewList.getAdapter();
    }

    private int getListChildCount() {
        return mPreviewList.getChildCount();
    }

    private int getCurrentXY() {
        if (mPreviewList instanceof VerticalListView) {
            return ((VerticalListView)mPreviewList).mCurrentY;
        } else {
            return ((HorizontalListView)mPreviewList).mCurrentX;
        }
    }

    private int getFirstVisiblePosition() {
        return mPreviewList.getFirstVisiblePosition();
    }

    private void indicateCellLayoutHoverItem(int itemIndex, boolean in, boolean dragSource) {
        if (LauncherApplication.isInLandOrientation()) {
            ((VerticalPreviewList) mPreviewList).indicateCellLayoutHoverItem(itemIndex, in, dragSource);
        } else {
            ((HorzontalPreviewList) mPreviewList).indicateCellLayoutHoverItem(itemIndex, in, dragSource);
        }
    }

    public PreviewContentType getContentType() {
        return mContentType;
    }

    public void setContentType(PreviewContentType type) {
        if (mContentType != type) {
            mContentType = type;
            mIndicatorView.clearBitmapCache();
            mIsFirstIn = true;
            switch (mContentType) {
                case Widgets:
                    if (mWidgetAdapter == null) {
                        mWidgetAdapter = new WidgetPreviewAdapter((Launcher) mContext);
                    }
                    setAdapter(mWidgetAdapter);
                    break;
                case Effects:
                    if (mEffectsAdapter == null) {
                        mEffectsAdapter = new EffectsPreviewAdapter(mContext);
                    }
                    setAdapter(mEffectsAdapter);
                    break;
                case Wallpapers:
                    if (mWallpapersAdapter == null) {
                        mWallpapersAdapter = new WallpapersPreviewAdapter(mContext);
                    }
                    setAdapter(mWallpapersAdapter);
                    break;
                case Themes:
                    if (mThemesAdapter == null) {
                        mThemesAdapter = new ThemesPreviewAdapter(mContext);
                    }
                    setAdapter(mThemesAdapter);
                    break;
                case CellLayouts:
                    if (mCelllayoutAdapter == null) {
                        mCelllayoutAdapter = new CelllayoutPreviewAdapter(mContext);
                    }
                    if (getAdapter() != mCelllayoutAdapter) {
                        setAdapter(mCelllayoutAdapter);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void onExit(boolean themeChange) {
        Log.d(Launcher.TAG_EDITMODE, "onExit mContentType " + mContentType + " themeChange " + themeChange+","+(mCelllayoutAdapter != null));
        if (mCelllayoutAdapter != null && (!themeChange || mContentType == PreviewContentType.Themes)) {
            mCelllayoutAdapter.onExit();
            mCelllayoutAdapter = null;
        }
        if (!themeChange) {
            if (mContentType != PreviewContentType.None) {
                setContentType(PreviewContentType.None);
                mPreviewList.setAdapter(null);
            }
            if (mIndicatorView != null) {
                mIndicatorView.onExit();
            }
            if (mThemesAdapter != null) {
                mThemesAdapter.onExit();
                mThemesAdapter = null;
            }
            if (mWallpapersAdapter != null) {
                mWallpapersAdapter.onExit();
                mWallpapersAdapter = null;
            }
        }
    }

    public void onEnter() {
        if (mCelllayoutAdapter == null) {
            mCelllayoutAdapter = new CelllayoutPreviewAdapter(mContext);
        }
        mCelllayoutAdapter.loadDataAdvance();
    }

    public void clearCellLayoutSelected(){
        if(mCelllayoutAdapter != null){
            mCelllayoutAdapter.clearSelected();
        }
    }

    @Override
    public void onScrollStateChanged(ScrollState scrollState) {
    }

    @Override
    public void computeScroll() {
        if (mLauncher.getWorkspace().getFolderBatchOping() == null && getAdapter() != null) {
            if (LauncherApplication.isInLandOrientation()) {
                invalidateIndicatorViewInLand(getAdapter().getCount(), getCurrentXY());
            } else {
                invalidateIndicatorViewInPort(getAdapter().getCount(), getCurrentXY());
            }
        }
    }

    @Override
    public void onScroll(int leftIndex, int rightIndex, int count) {
    }
    
    /* YUNOS BEGIN PB*/
    //##modules(HomeShell): ##author:guoshuai.lgs
    //##BugID:(164218) ##date:2014/10/22
    //##decrpition: update widgets list when package added/deleted/update.
    public void onPackagesUpdated(ArrayList<Object> widgetsAndShortcuts) {
        if (mWidgetAdapter == null) {
            mWidgetAdapter = new WidgetPreviewAdapter((Launcher) mContext);
        }
        mWidgetAdapter.onPackagesUpdated(widgetsAndShortcuts);
        if (mContentType == PreviewContentType.Widgets) {
            mWidgetAdapter.notifyDataSetInvalidated();
            if (LauncherApplication.isInLandOrientation()) {
                invalidateIndicatorViewInLand(mWidgetAdapter.getCount(), mIndicatorView.getFocusStart());
            } else {
                invalidateIndicatorViewInPort(mWidgetAdapter.getCount(), mIndicatorView.getFocusStart());
            }
        }
    }
    /*YUNOS END PB*/

    public void initNotificationWidgetView(AppWidgetManager widgetMgr, LauncherAppWidgetHost widgetHost) {
        if (mWidgetAdapter == null) {
            mWidgetAdapter = new WidgetPreviewAdapter((Launcher) mContext);
        }
        mWidgetAdapter.initNotificationWidgetView(widgetMgr, widgetHost);
    }

    public View initWidgetView(AppWidgetManager widgetMgr, LauncherAppWidgetHost widgetHost, ComponentName cn) {
        if (mWidgetAdapter == null) {
            mWidgetAdapter = new WidgetPreviewAdapter((Launcher) mContext);
        }
        return mWidgetAdapter.initWidgetView(widgetMgr, widgetHost, cn);
    }

    public void removeWidgetView(AppWidgetHostView widgetView) {
        if (mWidgetAdapter == null) {
            mWidgetAdapter = new WidgetPreviewAdapter((Launcher) mContext);
        }
        mWidgetAdapter.removeWidgetView(widgetView);
    }

    public boolean hasWidgetView(AppWidgetManager widgetMgr, LauncherAppWidgetHost widgetHost, ComponentName cn) {
        if (mWidgetAdapter == null) {
            mWidgetAdapter = new WidgetPreviewAdapter((Launcher) mContext);
        }
        return mWidgetAdapter.hasWidgetView(widgetMgr, widgetHost, cn);
    }

    public boolean isShowing() {
        return mIsShowing;
    }
    
    public void setShowing(boolean showing) {
        mIsShowing = showing;
        mIndicatorView.setLandMode(LauncherApplication.isInLandOrientation());
        if(!showing) {
            mIndicatorView.clearBitmapCache();
        } else {
            mIsFirstIn = true;
            mIndicatorView.init();
        }
    }

    private void invalidateIndicatorViewInLand(int totalCount, int topX) {
        if (!LauncherApplication.isInLandOrientation()) {
            return;
        }
        if (mIsFirstIn || mTotalCount != totalCount) {
            if ((mPreviewList != null) && (mPreviewList.getChildAt(0) != null)) {
                mItemH = mPreviewList.getChildAt(0).getHeight();
            } else {
                mItemH = getResources().getDimensionPixelSize(R.dimen.preview_cell_height);
            }
            indicatorH = getResources().getDimensionPixelSize(R.dimen.preview_indication_length);
            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
            int containerH = dm.heightPixels;
            float focusBitmapHRatio = (float) containerH / (mItemH * totalCount);
            if (focusBitmapHRatio > 1) {
                mIndicatorView.setVisibility(GONE);
            } else {
                if (mIndicatorView.getVisibility() != VISIBLE) {
                    mIndicatorView.setVisibility(VISIBLE);
                }
            }
            focusBitmapH = (int) (indicatorH * focusBitmapHRatio);
            mIndicatorView.setFocusHeight(focusBitmapH);
            ((VerticalListView)mPreviewList).mMaxHeight = mItemH * totalCount - containerH;
            mTotalCount = totalCount;
            mIsFirstIn = false;
        }
        float focusBitmapStartRatio = (float) (topX < 0 ? 0 : topX) / (mItemH * totalCount);
        int focusBitmapTop = (int) (indicatorH * focusBitmapStartRatio);
        mIndicatorView.setFocusStart(focusBitmapTop);
        mIndicatorView.startSwitchAnimator();
    }

    private void invalidateIndicatorViewInPort(int totalCount, int leftX) {
        if (LauncherApplication.isInLandOrientation()) {
            return;
        }

        if(mIsFirstIn || mTotalCount != totalCount){
            if ((mPreviewList != null) && (mPreviewList.getChildAt(0) != null)) {
                mItemW = mPreviewList.getChildAt(0).getWidth();
            } else {
                mItemW = getResources().getDimensionPixelSize(R.dimen.preview_cell_width);
            }
            indicatorW = getResources().getDimensionPixelSize(R.dimen.preview_indication_length);
            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
            int containerW = dm.widthPixels;
            float focusBitmapWRatio = (float) containerW / (mItemW * totalCount);
            if (focusBitmapWRatio > 1) {
                mIndicatorView.setVisibility(GONE);
            } else {
                if (mIndicatorView.getVisibility() != VISIBLE) {
                    mIndicatorView.setVisibility(VISIBLE);
                }
            }
            focusBitmapW = (int) (indicatorW * focusBitmapWRatio);
            mIndicatorView.setFocusWidth(focusBitmapW);
            ((HorizontalListView)mPreviewList).mMaxWidth = mItemW * totalCount - containerW;
            Log.d(TAG, " focusBitmapWRatio " + focusBitmapWRatio + " containerW " + containerW + " mItemW " + mItemW);
            mTotalCount = totalCount;
            mIsFirstIn = false;
        }
        float focusBitmapLeftRatio = (float)(leftX < 0 ? 0 : leftX) / (mItemW * totalCount);
        int focusBitmapLeft = (int)(indicatorW * focusBitmapLeftRatio);
        mIndicatorView.setFocusStart(focusBitmapLeft);
        mIndicatorView.startSwitchAnimator();
        mIndicatorView.setTranslationY(-mInsets.bottom);
    }

    @Override
    public boolean isDropEnabled() {
        return mContentType == PreviewContentType.CellLayouts;
    }
    
    private void animateDragViewBack(DragObject dragObject, View view) {
        mLauncher.getDragLayer().animateViewIntoPosition(dragObject.dragView, view, 500, null, this);
    }

    //YUNOS BEGIN PB
    //## modules(HomeShell):
    //## date:2015/09/16 ##author:shuoxing.wsx
    //## BugID:6424969:solve the issue icons is gone when drop  in edit mode.
    public void onDropMultiViews(final DragObject dragObject) {
        Workspace workspace = mLauncher.getWorkspace();
        int newScreen = (mTargetChildIndex < 0) ? workspace.getCurrentPage()
                        :mTargetChildIndex+ getListView().getFirstVisiblePosition();
        Log.d(Launcher.TAG_EDITMODE, "sxsexe_test       onDropMultiViews newScreen " + newScreen + " mTargetChildIndex "
                + mTargetChildIndex + " firstVisiblePosition " + getFirstVisiblePosition());
        if(mDragMode == DRAG_HOVER) {
            if(mDragHoverAlarm.alarmPending()) {
                mDragHoverAlarm.cancelAlarm();
                mDragHoverAlarm.setOnAlarmListener(null);
                mLauncher.getWorkspace().snapToPage(newScreen, TIME_SNAP_PAGE);
                workspace.onDropMultiViews(dragObject, (CellLayout) workspace.getChildAt(newScreen),
                        ANIMATE_TO_POSITION_TIME);
            } else {
                workspace.onDropMultiViews(dragObject, (CellLayout) workspace.getChildAt(newScreen), 0);
            }
        } else if(mDragMode == DRAG_FLING) {
            mLauncher.getWorkspace().snapToPage(newScreen, TIME_SNAP_PAGE);
            workspace.onDropMultiViews(dragObject, (CellLayout) workspace.getChildAt(newScreen),
                    ANIMATE_TO_POSITION_TIME);
        }
        if (dragObject.dragView != null && dragObject.dragView.getParent() != null) {
            mLauncher.getDragController().onDeferredEndDrag(dragObject.dragView);
        }
    }
    
    private void snapAndAnimateToPosition(final DragView dragView, View srcView, int screen) {
        mLauncher.getWorkspace().snapToPage(screen, TIME_SNAP_PAGE);
        Runnable rr = new Runnable() {

            @Override
            public void run() {
                if (mSrcView != null && dragView != null && dragView.getParent() != null) {
                    mSrcView.setVisibility(INVISIBLE);
                    Runnable completeR = new Runnable() {
                        public void run() {
                            if (dragView != null && dragView.getParent() != null) {
                                mLauncher.getDragController().onDeferredEndDrag(dragView);
                            }
                        }
                    };
                    mLauncher.getDragLayer().animateViewIntoPosition(dragView, mSrcView, ANIMATE_TO_POSITION_TIME,
                            completeR, PreviewContainer.this);
                } else if (dragView != null && dragView.getParent() != null) {
                    // ##date:2015/6/29 ##author:zhanggong.zg ##BugID:6120542
                    // for two-stage adding widget, remove drag view immediately
                    mLauncher.getDragController().onDeferredEndDrag(dragView);
                }
            }
        };
        mLauncher.postRunnableToMainThread(rr, TIME_SNAP_PAGE + 100);
    }

    View mSrcView = null;
    DragView mDropView = null;

    public void setDragSrcView(View view) {
        mSrcView = view;
    }

    @Override
    public void onDrop(final DragObject dragObject) {
        Log.d(Launcher.TAG_EDITMODE, "sxsexe_drop onDrop mDragMode " + mDragMode + " isMultiDragging "
                + mLauncher.getWorkspace().isMultiSelectDragging() + " mTargetChildIndex " + mTargetChildIndex
                + " getFirstVisiblePosition " + getFirstVisiblePosition() + " curType " + mContentType
                + " dragInfo " + dragObject.dragInfo + " dragSource " + dragObject.dragSource);

        mDropView = dragObject.dragView;
        if (mTargetChildIndex < 0) {
            if (mContentType == PreviewContentType.Widgets
                    || (dragObject.dragInfo instanceof PendingAddItemInfo && dragObject.dragSource instanceof AdapterView<?>)) {
                mEditModeHelper.backToWidgetList(dragObject, false);
            } else {
                mEditModeHelper.backToEditmodeEntry();
            }
            clearAnimationView();
            return;
        }

        mEditModeHelper.onCellLayoutBeginDrag(-1, false);
        mEditModeHelper.onUpdateSelectNumber(null, true);
        if(mLauncher.getWorkspace().isMultiSelectDragging()) {
            onDropMultiViews(dragObject);
            mEditModeHelper.updateEditModeTips(PreviewContentType.CellLayouts);
            return;
        }
        ItemInfo dragInfo = (ItemInfo) dragObject.dragInfo;
        
        final int oldScreen = dragInfo.screen;
        boolean hasMovedLayouts = false;
        final Workspace workspace = mLauncher.getWorkspace();
        int newScreen = mTargetChildIndex + getFirstVisiblePosition();
        
        mSrcView = workspace.getDragItemFromList(dragInfo, true);
        
        CellLayout cellLayout = (CellLayout) workspace.getChildAt(newScreen);
        boolean foundCell = true;
        int []targetCell = new int[2];
        if(oldScreen == newScreen) {
            hasMovedLayouts = false;
            targetCell[0] = dragInfo.cellX;
            targetCell[1] = dragInfo.cellY;
        } else {
            if (cellLayout == null) {
                foundCell = false;
            } else {
                foundCell = cellLayout.findCellForSpan(targetCell, dragInfo.spanX, dragInfo.spanY);
            }
            hasMovedLayouts = true;
            /* YUNOS END */
        }
        if(foundCell) {
            /* YUNOS BEGIN */
            // ##date:2015/05/22 ##author: chenjian.chenjian ##BugId: 6006081
            if (mSrcView == null) {// those drag from Widget pool to preview
                                   // screen
                if (dragObject.dragInfo instanceof PendingAddWidgetInfo) {
                    mLauncher.addAppWidget(
                            (((PendingAddWidgetInfo) dragObject.dragInfo)
                                    .getAppWidgetProviderInfo()), newScreen);
                    // dragObject.dragView
                } else if (dragObject.dragInfo instanceof PendingAddGadgetInfo) {
                    mLauncher
                            .addGadgetWidget(
                                    ((PendingAddGadgetInfo) dragObject.dragInfo).gadgetInfo,
                                    newScreen);
                } else if (dragObject.dragInfo instanceof PendingAddShortcutInfo) {
                    mLauncher
                            .addShortcut(
                                    ((PendingAddShortcutInfo) dragObject.dragInfo).shortcutActivityInfo,
                                    newScreen);
                }
                cellLayout.transferAllXYsOnDataChanged();
                if (mSrcView != null && mSrcView.getVisibility() == VISIBLE) {
                    mSrcView.setVisibility(INVISIBLE);
                }
                /* YUNOS END */
            } else {
                mLauncher.getWorkspace().getParentCellLayoutForView(mSrcView)
                        .removeView(mSrcView);
                com.aliyun.homeshell.CellLayout.LayoutParams lp = (com.aliyun.homeshell.CellLayout.LayoutParams) mSrcView
                        .getLayoutParams();
                dragInfo.screen = newScreen;
                lp.cellX = dragInfo.cellX = targetCell[0];
                lp.cellY = dragInfo.cellY = targetCell[1];
                lp.useTmpCoords = false;
                workspace.addInScreen(mSrcView, dragInfo.container, newScreen,
                        targetCell[0], targetCell[1], dragInfo.spanX,
                        dragInfo.spanY, false);
                LauncherModel.moveItemInDatabase(mLauncher, dragInfo, dragInfo.container, newScreen, targetCell[0],
                        targetCell[1]);
                if (ConfigManager.isLandOrienSupport()) {
                    if (hasMovedLayouts || oldScreen != newScreen) {
                        cellLayout.updateShortcutXYOnMove(dragInfo);
                    }
                }

                // User track
                if (dragInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                        || dragInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_GADGET) {
                    Map<String, String> param = new HashMap<String, String>();
                    param.put("mode", hasMovedLayouts ? "cross" : "down");
                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_Entry_Menu_Arrange_Widget, param);
                } else {
                    Map<String, String> param = new HashMap<String, String>();
                    param.put("mode", hasMovedLayouts ? "cross" : "down");
                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_Entry_Menu_Arrange_Icon_One, param);
                }
            }

            if(mDragMode == DRAG_HOVER) {
                if(mDragHoverAlarm.alarmPending()) {
                    // animateDragViewToPosition(dragObject, mSrcView);
                    mDragHoverAlarm.cancelAlarm();
                    mDragHoverAlarm.setOnAlarmListener(null);
                    snapAndAnimateToPosition(dragObject.dragView, mSrcView, newScreen);
                } else if(mSrcView != null){
                    mSrcView.setVisibility(INVISIBLE);
                    Runnable r = new Runnable() {
                        
                        @Override
                        public void run() {
                            mSrcView.setVisibility(VISIBLE);
                        }
                    };
                    mLauncher.getDragLayer().animateViewIntoPosition(dragObject.dragView, mSrcView, 300, r, this);
                }else{
                    /* YUNOS BEGIN */
                    // ##date:2015/05/22 ##author: chenjian.chenjian ##BugId:
                    // 6006081
                    snapAndAnimateToPosition(dragObject.dragView, mSrcView, newScreen);
                    /* YUNOS END */
                }
            } else if(mDragMode == DRAG_FLING) {
                snapAndAnimateToPosition(dragObject.dragView, mSrcView, newScreen);
            }
        } else {
            mLauncher.showOutOfSpaceMessage(false);
            if (mSrcView != null) {
                animateDragViewBack(dragObject, mSrcView);
                if (dragInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    LauncherModel.assignPlace(dragInfo.screen, dragInfo.cellX, dragInfo.cellY,
                            dragInfo.spanX, dragInfo.spanY, true, CellLayout.Mode.NORMAL);
                    if (ConfigManager.isLandOrienSupport()) {
                        LauncherModel.markCellsOccupiedInNonCurrent(dragInfo, true);
                    }
                }
            }
            clearAnimationView();
        }
        
        mDragMode = DRAG_NONE;
    }

    @Override
    public void onDragEnter(DragObject dragObject) {
        
        mDragMode = DRAG_FLING;
        mDragController.setDragScoller(this);
        mDragController.setScrollView(this);
        //check dragObject.x
        mTargetChildIndex = findDropTargetCellLayoutPreview(dragObject);
        int newScreen = mTargetChildIndex + getFirstVisiblePosition();
        if (newScreen != mEditModeHelper.getBeginDragIndex()) {
            indicateCellLayoutHoverItem(mTargetChildIndex, true, false);
        }
    }

    @Override
    public void onDragOver(DragObject dragObject) {
        //check dragObject.x
        int targetIndex = findDropTargetCellLayoutPreview(dragObject);
        // Log.d(Launcher.TAG_EDITMODE, "onDragOver targetIndex " +
        // targetIndex);
        if(targetIndex != mTargetChildIndex) {
            int realIndex = targetIndex + getFirstVisiblePosition();
            // Log.d(Launcher.TAG_EDITMODE,
            // "onDragOver cancelAlarm  targetIndex  " + targetIndex +
            // " mTargetScreenIndex "
            // + mTargetChildIndex + " BeginDragIndex " +
            // mLauncher.getBeginDragIndex() + " realIndex "
            // + realIndex);
            if(mDragHoverAlarm.alarmPending()) {
                mDragHoverAlarm.setOnAlarmListener(null);
                mDragHoverAlarm.cancelAlarm();
            }
            if (realIndex != mEditModeHelper.getBeginDragIndex()) {
                indicateCellLayoutHoverItem(mTargetChildIndex, false,
                        (mTargetChildIndex + getFirstVisiblePosition()) == mEditModeHelper.getBeginDragIndex());
                indicateCellLayoutHoverItem(targetIndex, true, false);
            } else {
                indicateCellLayoutHoverItem(targetIndex, true, true);
                indicateCellLayoutHoverItem(mTargetChildIndex, false,
                        (mTargetChildIndex + getFirstVisiblePosition()) == mEditModeHelper.getBeginDragIndex());
            }
            mTargetChildIndex = targetIndex;
            mDragMode = DRAG_FLING;
        } else {
            if(mTargetChildIndex != -1 && !mDragHoverAlarm.alarmPending()
                    && mDragMode != DRAG_HOVER) {
                // Log.d(Launcher.TAG_EDITMODE,
                // "sxsexe_alarm  onDragOver setOnAlarmListener  pending " +
                // mDragHoverAlarm.alarmPending()
                // + " mTargetScreenIndex " + mTargetChildIndex);
                DragHoverAlarmListener listener = new DragHoverAlarmListener();
                mDragHoverAlarm.setOnAlarmListener(listener);
                mDragHoverAlarm.setAlarm(ALARM_TIME_OUT);
                mDragMode = DRAG_HOVER;
            }
        }
    }

    @Override
    public void onDragExit(DragObject dragObject) {
        Log.d(TAG, "onDragExit dragObject " + dragObject);
        mDragController.setDragScoller(mLauncher.getWorkspace());
        mDragHoverAlarm.setOnAlarmListener(null);
        //mDragMode = DRAG_NONE;
        indicateCellLayoutHoverItem(mTargetChildIndex, false, mTargetChildIndex == mEditModeHelper.getBeginDragIndex());
    }

    @Override
    public void onFlingToDelete(DragObject dragObject, int x, int y, PointF vec) {
        
    }

    @Override
    public DropTarget getDropTargetDelegate(DragObject dragObject) {
        return null;
    }

    @Override
    public boolean acceptDrop(DragObject dragObject) {
        Log.d(Launcher.TAG_EDITMODE, "acceptDrop  mTargetChildIndex " + mTargetChildIndex);
        return mTargetChildIndex >= 0;
    }

    @Override
    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }
    
    @Override
    public void getHitRect(Rect outRect) {
        super.getHitRect(outRect);
    }
    
    @SuppressWarnings("unchecked")
    public void setup(Launcher launcher, DragController dragController) {
        mLauncher = launcher;
        mDragController = dragController;
        mDragController.addDropTarget(this);
        mEditModeHelper = launcher.getEditModeHelper();
        if (mPreviewList == null) {
            mPreviewList = (AdapterView<ListAdapter>) findViewById(R.id.preview_list);
            if (mPreviewList instanceof VerticalListView) {
                ((VerticalListView)mPreviewList).setOnScrollStateChangedListener(this);
                ((VerticalPreviewList) mPreviewList).setup(launcher, dragController);
            } else if (mPreviewList instanceof HorizontalListView) {
                ((HorizontalListView)mPreviewList).setOnScrollStateChangedListener(this);
                ((HorzontalPreviewList)mPreviewList).setup(launcher, dragController);
            }
        }
        if (mPreviewList instanceof VerticalListView) {
            ((VerticalPreviewList) mPreviewList).setup(launcher, dragController);
        } else if (mPreviewList instanceof HorizontalListView) {
            ((HorzontalPreviewList)mPreviewList).setup(launcher, dragController);
        }
    }

    private int findDropTargetCellLayoutPreview(DragObject dragObject) {
        if (mPreviewList instanceof VerticalListView) {
            return ((VerticalListView)mPreviewList).getChildIndex(dragObject.x, dragObject.y);
        } else {
            return ((HorizontalListView)mPreviewList).getChildIndex(dragObject.x, dragObject.y);
        }
    }

    @Override
    public void scrollLeft() {
        if (! (mPreviewList instanceof HorizontalListView)) {
            return;
        }
        HorzontalPreviewList previewList = (HorzontalPreviewList) mPreviewList;
        previewList.scrollToNextItem(false);
        for (int i = 0; i < getListChildCount(); i++) {
            previewList.indicateCellLayoutHoverItem(i, false, (i + getFirstVisiblePosition() == mEditModeHelper.getBeginDragIndex()));
        }
    }

    @Override
    public void scrollDown() {
        if (! (mPreviewList instanceof VerticalListView)) {
            return;
        }
        VerticalPreviewList previewList = (VerticalPreviewList) mPreviewList;
        previewList.scrollToNextItem(true);
        for (int i = 0; i < getListChildCount(); i++) {
            previewList.indicateCellLayoutHoverItem(i, false, (i + getFirstVisiblePosition() == mEditModeHelper.getBeginDragIndex()));
        }
    }

    @Override
    public void scrollUp() {
        if (! (mPreviewList instanceof VerticalListView)) {
            return;
        }
        VerticalPreviewList previewList = (VerticalPreviewList) mPreviewList;
        previewList.scrollToNextItem(false);
        for (int i = 0; i < getListChildCount(); i++) {
            previewList.indicateCellLayoutHoverItem(i, false, (i + getFirstVisiblePosition() == mEditModeHelper.getBeginDragIndex()));
        }
    }

    @Override
    public void scrollRight() {
        if (! (mPreviewList instanceof HorizontalListView)) {
            return;
        }
        HorzontalPreviewList previewList = (HorzontalPreviewList) mPreviewList;
        previewList.scrollToNextItem(true);
        for (int i = 0; i < getListChildCount(); i++) {
            previewList.indicateCellLayoutHoverItem(i, false, (i + getFirstVisiblePosition() == mEditModeHelper.getBeginDragIndex()));
        }
    }

    @Override
    public boolean onEnterScrollArea(int x, int y, int direction) {
        return true;
    }

    @Override
    public boolean onExitScrollArea() {
        return false;
    }

    public void onUpdateSelectNumber(final CellLayout cellLayout,final boolean add){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(mCelllayoutAdapter != null) {
                    mCelllayoutAdapter.updateSelectedNum(cellLayout, add);
                }
            }
        };
        mLauncher.postRunnableToMainThread(r, 0);
    }

    public void onCellLayoutBeginDrag(final int index,final boolean in){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(mCelllayoutAdapter != null) {
                    mCelllayoutAdapter.onCelllayoutBeginDrag(index, in);
                }
            }
        };
        mLauncher.postRunnableToMainThread(r, 100);
    }

    public void onCellLayoutDataChanged(boolean add, final CellLayout cellLayout, final View view) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(mCelllayoutAdapter != null) {
                    Log.d(Launcher.TAG_EDITMODE, "PreviewContainrer. onCellLayoutDataChanged");
                    mCelllayoutAdapter.updateItemOnCellLayoutChanged(cellLayout);
                }
            }
        };
        mLauncher.postRunnableToMainThread(r, 1000);
    }
    
    public void onCellLayoutAddOrDelete(final boolean add, final CellLayout cellLayout, final int index) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(mCelllayoutAdapter != null) {
                    Log.d(Launcher.TAG_EDITMODE, "PreviewContainrer. onCellLayoutAddOrDelete");
                    mCelllayoutAdapter.addOrDeleteEmptyCellLayout(add, cellLayout, index);
                }
            }
        };
        mLauncher.postRunnableToMainThread(r, 100);
    }

    public void exchangeScreen(int prev, int next) {
        if (mCelllayoutAdapter != null) {
            mCelllayoutAdapter.exchangeScreen(prev, next);
        }
    }

    class DragHoverAlarmListener implements OnAlarmListener {

        @Override
        public void onAlarm(Alarm alarm) {
            /* YUNOS BEGIN */
            // ##date:2015/05/22 ##author: chenjian.chenjian ##BugId:6035942
            int newScreen = mTargetChildIndex + getFirstVisiblePosition();
            CellLayout cellLayout = (CellLayout) mLauncher.getWorkspace().getChildAt(newScreen);
            if(cellLayout != null && cellLayout.isFakeChild()){
                mLauncher.getWorkspace().setChildrenEditMode(true);
                mEditModeHelper.onCellLayoutDataChanged(cellLayout, null);
            }
            /* YUNOS END */
            Log.d(Launcher.TAG_EDITMODE,
                    "onAlarm snapToPage newScreen " + newScreen + " BeginDragIndex " + mEditModeHelper.getBeginDragIndex());
            mLauncher.getWorkspace().snapToPage(newScreen, 300);
            indicateCellLayoutHoverItem(mTargetChildIndex, true, newScreen == mEditModeHelper.getBeginDragIndex());
        }
    }
    OnClickListener addToFolderListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Set<View> selectedViews = mLauncher.getWorkspace().getSelectedViewsInLayout();
			Folder folder = mLauncher.getWorkspace().getFolderBatchOping();
            String name = folder.getInfo().title.toString();
            int count = selectedViews.size();
            Map<String, String> param = new HashMap<String, String>();
            param.put("FolderName", name);
            param.put("count", String.valueOf(count));
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_FOLDER_ADDAPP, param);
			if(selectedViews.size() > 0 && folder!= null) {
				mLauncher.getWorkspace().flyToFolderAndExitEditMode();
			}
		}
    };

    public void clearAnimationView() {
        if (mDropView != null) {
            mDragController.onDeferredEndDrag(mDropView);
        }
        mDropView = null;
    }

    public EffectsPreviewAdapter getEffectsPreviewAdapter() {
        if (mEffectsAdapter == null) {
            mEffectsAdapter = new EffectsPreviewAdapter(mContext);
        }
        return mEffectsAdapter;
    }

    @Override
    public void setInsets(Rect insets) {
        //expand mPreviewListContainer's background
        if(mPreviewListContainer == null)return;
        LayoutParams flp = (LayoutParams) mPreviewListContainer.getLayoutParams();
        flp.height += (insets.bottom - mInsets.bottom);
        mPreviewListContainer.setLayoutParams(flp);
        mInsets.set(insets);
    }

    public void reloadItems() {
        if(mCelllayoutAdapter != null) {
            mCelllayoutAdapter.reloadItems();
        }
    }
}
