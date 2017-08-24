 /* Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aliyun.homeshell;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import app.aliyun.v3.gadget.GadgetView;
import storeaui.widget.ActionSheet;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Adapter;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.homeshell.Alarm.OnAlarmListener;
import com.aliyun.homeshell.CellLayout.Mode;
import com.aliyun.homeshell.FolderIcon.FolderRingAnimator;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.appgroup.AppGroupManager;
import com.aliyun.homeshell.appgroup.AppGroupManager.Callback;
import com.aliyun.homeshell.editmode.EffectsPreviewAdapter;
import com.aliyun.homeshell.editmode.PreviewContainer;
import com.aliyun.homeshell.editmode.PreviewContainer.PreviewContentType;
import com.aliyun.homeshell.hideseat.Hideseat;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.icon.BubbleController;
import com.aliyun.homeshell.icon.IconManager;
import com.aliyun.homeshell.lifecenter.CardBridge;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.model.LauncherModel.PackageUpdateTaskQueue;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.aliyun.homeshell.setting.HomeShellSetting;
import com.aliyun.homeshell.utils.Utils;
import com.aliyun.homeshell.widgetpage.WidgetPageManager;
import com.aliyun.utility.FeatureUtility;
import com.aliyun.homeshell.searchui.SearchBridge;

/**
 * The workspace is a wide area with a wallpaper and a finite number of pages.
 * Each page contains a number of icons, folders or widgets the user can
 * interact with. A workspace is meant to be used with a fixed width only.
 */
public class Workspace extends AnimationPagedView
        implements DropTarget, DragSource, DragScroller, View.OnTouchListener,
        DragController.DragListener, LauncherTransitionable, ViewGroup.OnHierarchyChangeListener {
    private static final String TAG = "Launcher.Workspace";
    private static final boolean DEBUG_FIND_CELL = LauncherModel.DEBUG_OCCUPY;

    // Y rotation to apply to the workspace screens
//    private static final float WORKSPACE_OVERSCROLL_ROTATION = 24f;

    private static final int CHILDREN_OUTLINE_FADE_OUT_DELAY = 0;
    private static final int CHILDREN_OUTLINE_FADE_OUT_DURATION = 375;
    private static final int CHILDREN_OUTLINE_FADE_IN_DURATION = 100;

    private static final int BACKGROUND_FADE_OUT_DURATION = 350;
    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;
    private static final int FLING_THRESHOLD_VELOCITY = 500;

    // These animators are used to fade the children's outlines
    private ObjectAnimator mChildrenOutlineFadeInAnimation;
    private ObjectAnimator mChildrenOutlineFadeOutAnimation;
    private float mChildrenOutlineAlpha = 0;

    //Fan-Shaped graphic when dragging in EditMode
    private static final int FAN_SHAPED_MAX_ICO_CNT = 4;
    private static final int FAN_SHAPED_ICO_SHIFT_X = 3;
    private static final int FAN_SHAPED_ICO_SHIFT_Y = 3;
    private static final int FAN_SHAPED_ROTATE_ANGLE = 3;

    // These properties refer to the background protection gradient used for AllApps and Customize
    /* YUNOS BEGIN */
    // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
    // private ValueAnimator mBackgroundFadeInAnimation;
    /* YUNOS END */
    private ValueAnimator mBackgroundFadeOutAnimation;
    private Drawable mBackground;
    boolean mDrawBackground = true;
    private float mBackgroundAlpha = 0;
    //private float mOverScrollMaxBackgroundAlpha = 0.0f;

    private float mWallpaperScrollRatio = 1.0f;
    private int mOriginalPageSpacing;

    private final WallpaperManager mWallpaperManager;
    private IBinder mWindowToken;
//    private static final float WALLPAPER_SCREENS_SPAN = 2f;
    private int mDefaultPage;

    /**
     * CellInfo for the cell that is currently being dragged
     */
    private CellLayout.CellInfo mDragInfo;
    private CellLayout.CellInfo mDragInfoDelete;

    /**
     * Target drop area calculated during last acceptDrop call.
     */
    private int[] mTargetCell = new int[2];
    private int mDragOverX = -1;
    private int mDragOverY = -1;

    static Rect mLandscapeCellLayoutMetrics = null;
    static Rect mPortraitCellLayoutMetrics = null;

    /**
     * The CellLayout that is currently being dragged over
     */
    private CellLayout mDragTargetLayout = null;
    /**
     * The CellLayout that we will show as glowing
     */
    private CellLayout mDragOverlappingLayout = null;

    /**
     * The CellLayout which will be dropped to
     */
    private CellLayout mDropToLayout = null;

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    private int[] mTempCell = new int[2];
    private int[] mTempEstimate = new int[2];
    private float[] mDragViewVisualCenter = new float[2];
    private float[] mTempDragCoordinates = new float[2];
    private float[] mTempCellLayoutCenterCoordinates = new float[2];
    private float[] mTempDragBottomRightCoordinates = new float[2];
    private Matrix mTempInverseMatrix = new Matrix();

    private SpringLoadedDragController mSpringLoadedDragController;
    private float mSpringLoadedShrinkFactor;

    private List<View> mDragItems = new ArrayList<View>();

    /* YUNOS BEGIN */
    //##date:2014/04/16 ##author:nater.wg ##BugID:110407
    // Get values of configures from ConfigManager
//    private static final int DEFAULT_CELL_COUNT_X = 4;
//    private static final int DEFAULT_CELL_COUNT_Y = 4;
    /* YUNOS END */

    // State variable that indicates whether the pages are small (ie when you're
    // in all apps or customize mode)
    enum State { NORMAL, SPRING_LOADED, SMALL };
    private State mState = State.NORMAL;
    private boolean mIsSwitchingState = false;

    boolean mAnimatingViewIntoPlace = false;
    boolean mIsDragOccuring = false;
    boolean mChildrenLayersEnabled = true;

    /** Is the user is dragging an item near the edge of a page? */
    private boolean mInScrollArea = false;

//    private final HolographicOutlineHelper mOutlineHelper = new HolographicOutlineHelper();
    private Bitmap mDragOutline = null;
    private final Rect mTempRect = new Rect();
    private final int[] mTempXY = new int[2];
    private float mOverscrollFade = 0;
    private boolean mOverscrollTransformsSet;
    public static final int DRAG_BITMAP_PADDING = 2;
    private boolean mWorkspaceFadeInAdjacentScreens;

    enum WallpaperVerticalOffset { TOP, MIDDLE, BOTTOM };
    int mWallpaperWidth;
    int mWallpaperHeight;
    WallpaperOffsetInterpolator mWallpaperOffset;
    boolean mUpdateWallpaperOffsetImmediately = false;
    private Runnable mDelayedResizeRunnable;
    private Runnable mDelayedSnapToPageRunnable;
    private Point mDisplaySize = new Point();
    private boolean mIsStaticWallpaper;
    private int mWallpaperTravelWidth;
    private int mSpringLoadedPageSpacing;
//    private int mCameraDistance;

    // Variables relating to the creation of user folders by hovering shortcuts over shortcuts
    private static final int FOLDER_CREATION_TIMEOUT = 0;
    /*YUNOS BEGIN*/
    private static final int REORDER_TIMEOUT = 300;//Kelude 5183145
    private boolean mEmptyScreenAdded = false;//added by xiaodong.lxd
    /*YUNOS END*/
    private final Alarm mFolderCreationAlarm = new Alarm();
    private final Alarm mReorderAlarm = new Alarm();
    private FolderRingAnimator mDragFolderRingAnimator = null;
    private FolderIcon mDragOverFolderIcon = null;
    private boolean mCreateUserFolderOnDrop = false;
    private boolean mAddToExistingFolderOnDrop = false;
    private DropTarget.DragEnforcer mDragEnforcer;
    private float mMaxDistanceForFolderCreation;

    // Variables relating to touch disambiguation (scrolling workspace vs. scrolling a widget)
    private float mXDown;
    private float mYDown;
    final static float START_DAMPING_TOUCH_SLOP_ANGLE = (float) Math.PI / 6;
    final static float MAX_SWIPE_ANGLE = (float) Math.PI / 3;
    final static float TOUCH_SLOP_DAMPING_FACTOR = 4;

    // Relating to the animation of items being dropped externally
    public static final int ANIMATE_INTO_POSITION_AND_DISAPPEAR = 0;
    public static final int ANIMATE_INTO_POSITION_AND_REMAIN = 1;
    public static final int ANIMATE_INTO_POSITION_AND_RESIZE = 2;
    public static final int COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION = 3;
    public static final int CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION = 4;

    // Related to dragging, folder creation and reordering
    private static final int DRAG_MODE_NONE = 0;
    private static final int DRAG_MODE_CREATE_FOLDER = 1;
    private static final int DRAG_MODE_ADD_TO_FOLDER = 2;
    private static final int DRAG_MODE_REORDER = 3;
    private int mDragMode = DRAG_MODE_NONE;
    private int mLastReorderX = -1;
    private int mLastReorderY = -1;

    private int mLastPage = 1;

    private SparseArray<Parcelable> mSavedStates;
    private final ArrayList<Integer> mRestoredPages = new ArrayList<Integer>();

    // These variables are used for storing the initial and final values during workspace animations
    private int mSavedScrollX;
    private float mSavedRotationY;
    private float mSavedTranslationX;
    private float mCurrentScaleX;
    private float mCurrentScaleY;
    private float mCurrentRotationY;
    private float mCurrentTranslationX;
    private float mCurrentTranslationY;
    private float[] mOldTranslationXs;
    private float[] mOldTranslationYs;
    private float[] mOldScaleXs;
    private float[] mOldScaleYs;
    private float[] mOldBackgroundAlphas;
    private float[] mOldAlphas;
    private float[] mNewTranslationXs;
    private float[] mNewTranslationYs;
    private float[] mNewScaleXs;
    private float[] mNewScaleYs;
    private float[] mNewBackgroundAlphas;
    private float[] mNewAlphas;
    private float[] mNewRotationYs;
    private float mTransitionProgress;

    protected View mDropTargetView;

    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(5735133) ##date:2015/1/30
    // ##description: add blured wallpaper to widget page.
    //private Paint mPaint;
    private Bitmap mBluredWallpaper;
    //private int mStatusBarHeight;
    private boolean mWidgetPageBlurWallpaper;
    /* YUNOS END PB */

    private Context mContext = null;

    //added by xiaodong.lxd #Kelude 5183145
    public static final float FOLDER_CREATION_FACTOR = 0.55f;
    /* YUNOS BEGIN */
    // ##date:2014/09/24 ##author:xindong.zxd ##BugId:5239349
    //adjustment the effect of the merger folder
    public static final float FOLDER_CREATION_FACTOR_CARDMODE = 0.8f;
    /* YUNOS END */
    //ended 5183145

    private Boolean mIsCatActShtStart = false;
    private final Runnable mBindPages = new Runnable() {
        @Override
        public void run() {
            mLauncher.getModel().bindRemainingSynchronousPages();
        }
    };

    private Callback mCallback = new Callback() {

        @Override
        public void onResult(final FolderIcon fi, final String folderName) {
            if (!TextUtils.isEmpty(folderName)) {
                post(new Runnable() {

                    @Override
                    public void run() {
                        fi.getFolderInfo().setTitle("");
                        fi.setTitle(folderName);
                    }
                });
            }
        }
    };

    /* YUNOS BEGIN */
    // ##date:2015-1-22 ##author:zhanggong.zg ##BugID:5645766
    // Folder optimization:
    // Make a delay when drag icon from folder to dock.
    private static final int sDragFromFolderToHotseatDelay = 1000;
    private boolean mDragFromFolderToHotseatEnable = true;
    private final Alarm mDragFromFolderToHotseatAlarm = new Alarm() {
        {
            setOnAlarmListener(new OnAlarmListener() {
                @Override
                public void onAlarm(Alarm alarm) {
                    mDragFromFolderToHotseatEnable = true;
                }
            });
        }
    };
    /* YUNOS END */
    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(5674413) ##date:2014/12/30
    // ##description: separate mainmenu slide effect from screen's.
    private SharedPreferences mSharedPref;
    private OnSharedPreferenceChangeListener mSharedPrefListener;
    private int mTransitionEffect = -1;
    /* YUNOS END PB */
    // ##date:2015-6-18 ##author:zhanggong.zg ##BugID:6087776
    // added for loop page scrolling
    private boolean mIsHotseatMovingWithWorkspace = false;

    /* YUNOS BEGIN PB */
    //Desc:BugID:6428097:merge nav bar
    //##Date: Oct 21, 2015 3:18:54 PM ##Author:chao.lc@alibaba-inc.com
    private int mNavbarHeight = 0;
    /* YUNOS END PB */
    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     */
    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        mContext = context;
    }

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     * @param defStyle Unused.
     */
    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mContentIsRefreshable = false;
        mOriginalPageSpacing = mPageSpacing;
        mIsConfigPadding = true;
        mDragEnforcer = new DropTarget.DragEnforcer(context);
        // With workspace, data is available straight from the get-go
        setDataIsReady();

        mLauncher = (Launcher) context;
        /* YUNOS BEGIN PB */
        //Desc:BugID:6428097:merge nav bar
        //##Date: Oct 21, 2015 3:18:54 PM ##Author:chao.lc@alibaba-inc.com
        mNavbarHeight = mLauncher.getNavigationBarHeight();
        /* YUNOS END PB */
        mDragController = mLauncher.getDragController();
        final Resources res = getResources();
        mWorkspaceFadeInAdjacentScreens = res.getBoolean(R.bool.config_workspaceFadeAdjacentScreens);
        mFadeInAdjacentScreens = false;
        mWallpaperManager = WallpaperManager.getInstance(context);

        /* YUNOS BEGIN */
        //##date:2014/04/16 ##author:nater.wg ##BugID:110407
        // Get values of configures from ConfigManager
//        int cellCountX = DEFAULT_CELL_COUNT_X;
//        int cellCountY = DEFAULT_CELL_COUNT_Y;
      int cellCountX = ConfigManager.DEFAULT_CELL_COUNT_X;
      int cellCountY = ConfigManager.DEFAULT_CELL_COUNT_Y;
        /* YUNOS END */

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.Workspace, defStyle, 0);

        if (LauncherApplication.isScreenLarge()) {
            // Determine number of rows/columns dynamically
            // TODO: This code currently fails on tablets with an aspect ratio < 1.3.
            // Around that ratio we should make cells the same size in portrait and
            // landscape
            TypedArray actionBarSizeTypedArray =
                context.obtainStyledAttributes(new int[] { android.R.attr.actionBarSize });
            final float actionBarHeight = actionBarSizeTypedArray.getDimension(0, 0f);
            actionBarSizeTypedArray.recycle();

            Point minDims = new Point();
            Point maxDims = new Point();
            mLauncher.getWindowManager().getDefaultDisplay().getCurrentSizeRange(minDims, maxDims);

            cellCountX = 1;
            while (CellLayout.widthInPortrait(res, cellCountX + 1) <= minDims.x) {
                cellCountX++;
            }

            cellCountY = 1;
            while (actionBarHeight + CellLayout.heightInLandscape(res, cellCountY + 1)
                <= minDims.y) {
                cellCountY++;
            }
        }

        mSpringLoadedShrinkFactor =
            res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f;
        mSpringLoadedPageSpacing =
                res.getDimensionPixelSize(R.dimen.workspace_spring_loaded_page_spacing);
        //mEditModePageSpacing = res.getDimensionPixelSize(R.dimen.edit_mode_page_spacing);
        mEditModePageSpacing = res.getDimensionPixelSize(R.dimen.drag_page_spacing);
        sEditScale = res.getInteger(R.integer.workspace_scale) / 10f;
        sHeaderHeight = getResources().getDimensionPixelSize(R.dimen.cell_layout_header_height);
        sTranslationY = getResources().getDimensionPixelSize(R.dimen.cell_layout_translation_y);
        //mCameraDistance = res.getInteger(R.integer.config_cameraDistance);

        // if the value is manually specified, use that instead
        /* YUNOS BEGIN */
        //##date:2014/04/16 ##author:nater.wg ##BugID:110407
        // Get values of configures from ConfigManager
//        cellCountX = a.getInt(R.styleable.Workspace_cellCountX, cellCountX);
//        cellCountY = a.getInt(R.styleable.Workspace_cellCountY, cellCountY);
        cellCountX = ConfigManager.getCellCountX();
        cellCountY = ConfigManager.getCellCountY();
        /* YUNOS END */

        mDefaultPage = a.getInt(R.styleable.Workspace_defaultScreen, 1);
        if (!mLauncher.isSupportLifeCenter()) {
            mDefaultPage = 0;
        }
        mIconScreenStartIndex = mDefaultPage;
        a.recycle();

        setOnHierarchyChangeListener(this);

        LauncherModel.updateWorkspaceLayoutCells(cellCountX, cellCountY);
        setHapticFeedbackEnabled(false);

        initWorkspace();

        // Disable multitouch across the workspace/all apps/customize tray
        setMotionEventSplittingEnabled(true);

        // Unless otherwise specified this view is important for accessibility.
        if (getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // dynamic cell layout padding for 4x4 and 4x5
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            CellLayout cellLayout = (CellLayout) getChildAt(i);
            if (!mLauncher.isLeftScreen(i)) {
                // normal cell layout
                adjustCellLayoutPadding(cellLayout);
            } else {
                adjustLifeCenterCellLayoutPadding(cellLayout);
            }
        }
    }

    private void adjustLifeCenterCellLayoutPadding(CellLayout layout) {
        if (layout == null) {
            return;
        }
        layout.setPadding(0, 0, 0, 0);
        layout.computePaddings();
    }

    public void adjustCellLayoutPadding(CellLayout layout) {
        if (layout == null) {
            return;
        }
        int childIndex = indexOfChild(layout);
        if (childIndex < getIconScreenHomeIndex()) {
            return;
        }
        int topPadding, bottomPadding;
        if (ConfigManager.getCellCountY() == 4) {
            topPadding = getResources().getDimensionPixelSize(R.dimen.cell_layout_top_padding_port_4_4);
            bottomPadding = getResources().getDimensionPixelSize(R.dimen.cell_layout_bottom_padding_port_4_4);
        } else {
            topPadding = getResources().getDimensionPixelSize(R.dimen.cell_layout_top_padding_port);
            bottomPadding = getResources().getDimensionPixelSize(R.dimen.cell_layout_bottom_padding_port);
        }
        int leftPadding = layout.getPaddingStart();
        int rightPadding = layout.getPaddingEnd();
        layout.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
        layout.computePaddings();
    }

    // estimate the size of a widget with spans hSpan, vSpan. return MAX_VALUE for each
    // dimension if unsuccessful
    public int[] estimateItemSize(int hSpan, int vSpan,
            ItemInfo itemInfo, boolean springLoaded) {
        int[] size = new int[2];
        if (getIconScreenCount() > 0) {
            CellLayout cl = (CellLayout) mLauncher.getWorkspace().getChildAt(getIconScreenHomeIndex());
            Rect r = estimateItemPosition(cl, itemInfo, 0, 0, hSpan, vSpan);
            size[0] = r.width();
            size[1] = r.height();
            if (springLoaded) {
                size[0] *= mSpringLoadedShrinkFactor;
                size[1] *= mSpringLoadedShrinkFactor;
            }
            return size;
        } else {
            size[0] = Integer.MAX_VALUE;
            size[1] = Integer.MAX_VALUE;
            return size;
        }
    }
    public Rect estimateItemPosition(CellLayout cl, ItemInfo pendingInfo,
            int hCell, int vCell, int hSpan, int vSpan) {
        Rect r = new Rect();
        cl.cellToRect(hCell, vCell, hSpan, vSpan, r);
        return r;
    }

    public void onDragStart(DragSource source, Object info, int dragAction) {
        mIsDragOccuring = true;
        updateChildrenLayersEnabled(false);
        mLauncher.lockScreenOrientation();
        setChildrenBackgroundAlphaMultipliers(1f);
        // Prevent any Un/InstallShortcutReceivers from updating the db while we are dragging
        InstallShortcutReceiver.enableInstallQueue();
        UninstallShortcutReceiver.enableUninstallQueue();
    }

    public void onDragEnd() {
        mIsMultiSelectDragging = false;
        selectedViews.clear();
        mIsDragOccuring = false;
        updateChildrenLayersEnabled(false);
        mLauncher.unlockScreenOrientation(false);

        // Re-enable any Un/InstallShortcutReceiver and now process any queued items
        InstallShortcutReceiver.disableAndFlushInstallQueue(getContext());
        UninstallShortcutReceiver.disableAndFlushUninstallQueue(getContext());
    }

    /**
     * Initializes various states for this workspace.
     */
    final protected void initWorkspace() {
        Context context = getContext();
        mCurrentPage = mDefaultPage;
        Launcher.setScreen(mCurrentPage);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setChildrenDrawnWithCacheEnabled(true);

        final Resources res = getResources();
        try {
            mBackground = res.getDrawable(R.drawable.apps_customize_bg);
        } catch (Resources.NotFoundException e) {
            // In this case, we will skip drawing background protection
        }

        mWallpaperOffset = new WallpaperOffsetInterpolator();
        Display display = mLauncher.getWindowManager().getDefaultDisplay();
        display.getSize(mDisplaySize);
        mWallpaperTravelWidth = (int) (mDisplaySize.x *
                wallpaperTravelToScreenWidthRatio(mDisplaySize.x, mDisplaySize.y));

        float factor = mLauncher.getIconManager().supprtCardIcon() ? FOLDER_CREATION_FACTOR_CARDMODE : FOLDER_CREATION_FACTOR;
        mMaxDistanceForFolderCreation = (factor * res.getDimensionPixelSize(R.dimen.app_icon_size));
        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * mDensity);
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(5674413) ##date:2014/12/30
        // ##description: separate mainmenu slide effect from screen's.
        mSharedPref = context.getSharedPreferences("com.aliyun.homeshell_preferences",
                Context.MODE_PRIVATE);
        mSharedPrefListener = new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (HomeShellSetting.KEY_PRE_EFFECT_STYLE.equals(key)) {
                    int newEffect = HomeShellSetting.getSlideEffectMode(mContext);
                    if (mTransitionEffect != newEffect) {
                        onTransitionEffectChanged(mTransitionEffect, newEffect);
                        mTransitionEffect = newEffect;
                    }
                }
            }
        };
        mSharedPref.registerOnSharedPreferenceChangeListener(mSharedPrefListener);
        /* YUNOS END PB */
    }

    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(5674413) ##date:2014/12/30
    // ##description: separate mainmenu slide effect from screen's.
    @Override
    protected void onTransitionEffectChanged(int oldEffect, int newEffect) {

        for (int i = 0; i < getChildCount(); i++) {
            View page = getPageAt(i);
            // YUNOS BEGIN PB
            // ##modules(HomeShell): ##author:guoshuai.lgs
            // ##BugID:(5592370) ##date:2014/11/18
            // ##decription: Do not set EDIT_SCALE for AppsCustomizePagedView

            if (mLauncher.isInLauncherEditMode()) {
                page.setScaleX(sEditScale);
                page.setScaleY(sEditScale);
            } else {
                page.setScaleX(1f);
                page.setScaleY(1f);
            }
            if (mLauncher.isInLauncherEditMode()) {
                page.setTranslationX(page.getWidth() * (1 - sEditScale) / 2);
                page.setTranslationY(sTranslationY);
            } else {
                page.setTranslationX(0f);
                page.setTranslationY(0f);
            }

            // YUNOS END PB
        }
        super.onTransitionEffectChanged(oldEffect, newEffect);
    }
    /* YUNOS END PB */
    public void updateChildEditBtnContainer() {
           if (this.mLauncher.isInLauncherEditMode()) {
               int childCount = this.getChildCount();
               if (childCount == 2) {
                   CellLayout cell = (CellLayout) getChildAt(0);
                   cell.removeEditBtnContainer();
               } else if (childCount > 2) {
                   CellLayout cell = null;
                   for (int i = 0 ; i < childCount - 1; i++) {
                       cell = (CellLayout) getChildAt(i);
                       if (!cell.hasChild()) {
                           View header = cell.findViewById(R.id.header);
                           /* YUNOS BEGIN */
                           // ## modules(Home Shell)
                           // ## date: 2016/03/10 ## author: chenliang
                           // ## BugID: 8280028: add header
                           if (header == null) {
                               cell.setClipChildren(false);
                               cell.setClipToPadding(false);
                               header = View.inflate(getContext(), R.layout.cell_layout_header, null);
                               header.setOnLongClickListener(mLongClickListener);
                               cell.addView(header);
                           }
                           /* YUNOS END */
                           cell.addEditBtnContainer();
                           cell.setEditBtnContainerMode(false);
                       }
                   }
               }
           }
       }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        super.onChildViewRemoved(parent, child);
        if (child instanceof CellLayout) {
            CellLayout cellLayout = (CellLayout) child;
            CellLayout cl = ((CellLayout) child);
            if (cl.isLeftPage() || cl.isWidgetPage()) {
                return;
            }
            cellLayout.removeWorkspaceOccupiedFromModel();
            cellLayout.onRemove();
        }
        updateChildEditBtnContainer();
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        super.onChildViewAdded(parent, child);
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        CellLayout cl = ((CellLayout) child);
        if (cl.isLeftPage() || cl.isWidgetPage()) {
            return;
        }
        cl.setOnInterceptTouchListener(this);
        cl.setClickable(true);
        cl.setContentDescription(getContext().getString(
                R.string.workspace_description_format, getChildCount()));

        /*YUNOS BEGIN*/
        //##date:2015/03/30 ##author:sunchen.sc ##BugID:5735130
        //Add occupy array to LauncherModel ArrayList when CellLayout add to workspace
        cl.addWorkspaceOccupiedToModel(this);
        /*YUNOS END*/
        updateChildEditBtnContainer();
    }

    protected boolean shouldDrawChild(View child) {
        final CellLayout cl = (CellLayout) child;
        return super.shouldDrawChild(child) &&
                (cl.getShortcutAndWidgetContainer().getAlpha() > 0 ||
             cl.getBackgroundAlpha() > 0);
    }

    /**
     * @return The open folder on the current screen, or null if there is none
     */
    public Folder getOpenFolder() {
        DragLayer dragLayer = mLauncher.getDragLayer();
        int count = dragLayer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = dragLayer.getChildAt(i);
            if (child instanceof Folder) {
                Folder folder = (Folder) child;
                if (folder.getInfo().opened)
                    return folder;
            }
        }
        return null;
    }

    boolean isTouchActive() {
        return mTouchState != TOUCH_STATE_REST;
    }


    /*YUNOS BEGIN*/
    //##date:2013/11/22 ##author:xiaodong.lxd
    //insert icon in hotseat
    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param screen The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     */
    void addInScreen(View child, long container, int screen, int x, int y, int spanX, int spanY) {
        addInScreen(child, container, screen, x, y, spanX, spanY, false);
    }

    void addInHotseat(View child, long container, int screen, int x, int y, int spanX, int spanY, int index) {

        final CellLayout layout = mLauncher.getHotseat().getLayout();
        child.setOnKeyListener(null);

        // Hide folder title in the hotseat
        if (child instanceof FolderIcon) {
            ((FolderIcon) child).setTextVisible(false);
        }

        if (screen < 0) {
            screen = mLauncher.getHotseat().getOrderInHotseat(x, y);
        } else {
            // Note: We do this to ensure that the hotseat is always laid out in the orientation
            // of the hotseat in order regardless of which orientation they were added
            x = mLauncher.getHotseat().getCellXFromOrder(screen);
            y = mLauncher.getHotseat().getCellYFromOrder(screen);
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

        if (spanX < 0 && spanY < 0) {
            lp.isLockedToGrid = false;
        }

        // Get the canonical child id to uniquely represent this view in this screen
        int childId = LauncherModel.getCellLayoutChildId(container, screen, x, y, spanX, spanY);
        boolean markCellsAsOccupied = !(child instanceof Folder);
        if(child != null && child.getParent() != null) {
            ((ViewGroup)child.getParent()).removeView(child);
        }

        //#5207506 added by xiaodong.lxd
        ShortcutAndWidgetContainer viewParent = mLauncher.getHotseat().getContainer();
        index = index >= viewParent.getChildCount() ? -1 : index;

        if (!layout.addViewToCellLayout(child, index, childId, lp, markCellsAsOccupied)) {
            // TODO: This branch occurs when the workspace is adding views
            // outside of the defined grid
            // maybe we should be deleting these items from the LauncherModel?
            Log.w(TAG, "Failed to add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout");
        }

        if (!(child instanceof Folder)) {
            child.setHapticFeedbackEnabled(false);
            child.setOnLongClickListener(mLongClickListener);
        }
        if (child instanceof DropTarget) {
            mDragController.addDropTarget((DropTarget) child);
        }

    }
    /*YUNOS END*/

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param screen The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     * @param insert When true, the child is inserted at the beginning of the children list.
     */
    public void addInScreen(View child, long container, int screen, int x, int y, int spanX, int spanY,
            boolean insert) {
        if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            Log.d(TAG, "addInScreen child " + child.getTag() + " screen " + screen);
            if (screen >= getIconScreenCount()) {
                makesureAddScreenIndex(screen);
            }
            if (screen < 0 ) {
               return;
            }
            // YUNOS END PB
            if (mInEditing) {
                CellLayout cellLayout = (CellLayout) getChildAt(screen);
                if (cellLayout != null && cellLayout.isFakeChild()) {
                    cellLayout.setFake(false);
                    /* YUNOS BEGIN */
                    // ## modules(Home Shell)
                    // ## date: 2016/03/10 ## author: wangye.wy
                    // ## BugID: 7945871: header in cell layout
                    cellLayout.setClipChildren(false);
                    cellLayout.setClipToPadding(false);
                    View header = View.inflate(getContext(), R.layout.cell_layout_header, null);
                    header.setOnLongClickListener(mLongClickListener);
                    cellLayout.addView(header);
                    /* YUNOS END */
                    if (getChildCount() < ConfigManager.getIconScreenMaxCount()) {
                        addEmptyScreenSync();
                        int screenCount = getChildCount();
                        CellLayout lastChild = (CellLayout) getChildAt(screenCount - 1);
                        lastChild.addEditBtnContainer();
                        lastChild.setEditBtnContainerMode(true);
                        initScreen(screenCount - 1);
                        mLauncher.getEditModeHelper().onCellLayoutAddOrDelete(true, lastChild, screenCount - 1);
                    }
                }
            }

            if (screen < 0 || screen >= getIconScreenCount()) {
                Log.e(TAG, "The screen must be >= 0 and < " + getIconScreenCount()
                    + " (was " + screen + "); skipping child");
                return;
            }
        }
        final CellLayout layout;
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            layout = mLauncher.getHotseat().getLayout();
            child.setOnKeyListener(null);

            // Hide folder title in the hotseat
            if (child instanceof FolderIcon) {
                ((FolderIcon) child).setTextVisible(false);
            }

            if (screen < 0) {
                screen = mLauncher.getHotseat().getOrderInHotseat(x, y);
            } else {
                // Note: We do this to ensure that the hotseat is always laid out in the orientation
                // of the hotseat in order regardless of which orientation they were added
                x = mLauncher.getHotseat().getCellXFromOrder(screen);
                y = mLauncher.getHotseat().getCellYFromOrder(screen);
            }
        } else if ( container == LauncherSettings.Favorites.CONTAINER_HIDESEAT ){
            mLauncher.getHideseat().addInScreen(child, container, screen, x, y, spanX, spanY, insert);
            return;
        }else {
            // Show folder title if not in the hotseat
            if (child instanceof FolderIcon) {
                //BugID:6626478:folder title invisible after multi drag cancelled by home key on L9
//                ((FolderIcon) child).setTextVisible(true);
                ((FolderIcon) child).getTitleText().setDisableLabel(false);
                ((FolderIcon) child).getTitleText().setTextColor(getResources().getColor(R.color.workspace_icon_text_color));
            }

            layout = (CellLayout) getChildAt(getRealScreenIndex(screen));
            //Log.d(TAG, "sxsexe--->addInScreen screen " + screen + " childCount " + getChildCount() + " layout " + layout);
            child.setOnKeyListener(new IconKeyEventListener());
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

        if (spanX < 0 && spanY < 0) {
            lp.isLockedToGrid = false;
        }

        // Get the canonical child id to uniquely represent this view in this screen
        int childId = LauncherModel.getCellLayoutChildId(container, screen, x, y, spanX, spanY);
        boolean markCellsAsOccupied = !(child instanceof Folder);
        if (layout != null && !layout.addViewToCellLayout(child, insert ? 0 : -1, childId, lp, markCellsAsOccupied)) {
            // TODO: This branch occurs when the workspace is adding views
            // outside of the defined grid
            // maybe we should be deleting these items from the LauncherModel?
            Log.w(TAG, "Failed to add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout");
        }

        if (container == Favorites.CONTAINER_HIDESEAT) {
            child.setHapticFeedbackEnabled(false);
            child.setOnLongClickListener(mLauncher.getHideseat());
        } else if (!(child instanceof Folder)) {
            child.setHapticFeedbackEnabled(false);
            child.setOnLongClickListener(mLongClickListener);
        }
        if (child instanceof DropTarget) {
            mDragController.addDropTarget((DropTarget) child);
        }
        if (child instanceof GadgetView) {
            mLauncher.addGadgetView((GadgetView)child);
        }

    }

    /**
     * Check if the point (x, y) hits a given page.
     */
    private boolean hitsPage(int index, float x, float y) {
        final View page = getChildAt(index);
        if (page != null) {
            float[] localXY = { x, y };
            mapPointFromSelfToChild(page, localXY);
            return (localXY[0] >= 0 && localXY[0] < page.getWidth()
                    && localXY[1] >= 0 && localXY[1] < page.getHeight());
        }
        return false;
    }

    @Override
    protected boolean hitsPreviousPage(float x, float y) {
        // mNextPage is set to INVALID_PAGE whenever we are stationary.
        // Calculating "next page" this way ensures that you scroll to whatever page you tap on
        final int current = (mNextPage == INVALID_PAGE) ? mCurrentPage : mNextPage;

        // Only allow tap to next page on large devices, where there's significant margin outside
        // the active workspace
        return LauncherApplication.isScreenLarge() && hitsPage(current - 1, x, y);
    }

    @Override
    protected boolean hitsNextPage(float x, float y) {
        // mNextPage is set to INVALID_PAGE whenever we are stationary.
        // Calculating "next page" this way ensures that you scroll to whatever page you tap on
        final int current = (mNextPage == INVALID_PAGE) ? mCurrentPage : mNextPage;

        // Only allow tap to next page on large devices, where there's significant margin outside
        // the active workspace
        return LauncherApplication.isScreenLarge() && hitsPage(current + 1, x, y);
    }

    /**
     * Called directly from a CellLayout (not by the framework), after we've been added as a
     * listener via setOnInterceptTouchEventListener(). This allows us to tell the CellLayout
     * that it should intercept touch events, which is not something that is normally supported.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        return (isSmall() || !isFinishedSwitchingState());
    }

    public boolean isSwitchingState() {
        return mIsSwitchingState;
    }

    /** This differs from isSwitchingState in that we take into account how far the transition
     *  has completed. */
    public boolean isFinishedSwitchingState() {
        return !mIsSwitchingState || (mTransitionProgress > 0.5f);
    }

    protected void onWindowVisibilityChanged (int visibility) {
        mLauncher.onWindowVisibilityChanged(visibility);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (isSmall() || !isFinishedSwitchingState()) {
            // when the home screens are shrunken, shouldn't allow side-scrolling
            return false;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isDisableTouchInteraction()) {
            return false;
        }

        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            mXDown = ev.getX();
            mYDown = ev.getY();

            break;
        case MotionEvent.ACTION_POINTER_UP:
        case MotionEvent.ACTION_UP:
            if (mTouchState == TOUCH_STATE_REST) {
                final CellLayout currentPage = (CellLayout) getChildAt(mCurrentPage);
                if (currentPage != null && !currentPage.lastDownOnOccupiedCell()) {
                    onWallpaperTap(ev);
                }
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (isDisableTouchInteraction()) {
            return false;
        }
        return super.onTouchEvent(ev);
    }

    private boolean isDisableTouchInteraction() {
        if (mLauncher.isSupportLifeCenter()) {
            if (mLauncher.getCardBridge().isLifecenterConsumed()) {
                return true;
            }
        }

        return false;
    }

    protected void reinflateWidgetsIfNecessary() {
        final int clCount = getIconScreenCount();
        int offSet = getIconScreenHomeIndex();
        for (int i = 0; i < clCount; i++) {
            CellLayout cl = (CellLayout) getChildAt(i + offSet);
            ShortcutAndWidgetContainer swc = cl.getShortcutAndWidgetContainer();
            final int itemCount = swc.getChildCount();
            for (int j = 0; j < itemCount; j++) {
                View v = swc.getChildAt(j);

                if ((v != null) && (v.getTag() instanceof LauncherAppWidgetInfo)) {
                    LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
                    LauncherAppWidgetHostView lahv = (LauncherAppWidgetHostView) info.hostView;
                    if (lahv != null && lahv.orientationChangedSincedInflation()) {
                        mLauncher.removeAppWidget(info);
                        // Remove the current widget which is inflated with the wrong orientation
                        cl.removeView(lahv);
                        mLauncher.bindAppWidget(info);
                    }
                }
            }
        }
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev) {
        if (isSmall()) return;
        if (!isFinishedSwitchingState()) return;

        float deltaX = Math.abs(ev.getX() - mXDown);
        float deltaY = Math.abs(ev.getY() - mYDown);

        if (Float.compare(deltaX, 0f) == 0) return;

        float slope = deltaY / deltaX;
        float theta = (float) Math.atan(slope);

        if (deltaX > mTouchSlop || deltaY > mTouchSlop) {
            cancelCurrentPageLongPress();
        }

        if (theta > MAX_SWIPE_ANGLE) {
            // Above MAX_SWIPE_ANGLE, we don't want to ever start scrolling the workspace
            return;
        } else if (theta > START_DAMPING_TOUCH_SLOP_ANGLE) {
            // Above START_DAMPING_TOUCH_SLOP_ANGLE and below MAX_SWIPE_ANGLE, we want to
            // increase the touch slop to make it harder to begin scrolling the workspace. This
            // results in vertically scrolling widgets to more easily. The higher the angle, the
            // more we increase touch slop.
            theta -= START_DAMPING_TOUCH_SLOP_ANGLE;
            float extraRatio = (float)
                    Math.sqrt((theta / (MAX_SWIPE_ANGLE - START_DAMPING_TOUCH_SLOP_ANGLE)));
            super.determineScrollingStart(ev, 1 + TOUCH_SLOP_DAMPING_FACTOR * extraRatio);
        } else {
            // Below START_DAMPING_TOUCH_SLOP_ANGLE, we don't do anything special
            super.determineScrollingStart(ev);
        }
    }

    public boolean isPageMoving() {
        return super.isPageMoving();
    }

    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        if (mLauncher.isSupportLifeCenter()) {
            if (mLauncher.getCardBridge() != null) {
                mLauncher.getCardBridge().onPageBeginMoving(mCurrentPage);
            } else {
                Log.w(TAG, "onPageBeginMoving() mLauncher.getCardBridge() == null");
                new Throwable().printStackTrace();
            }
        }
        mLastPage = mCurrentPage;
        if(SearchBridge.isHomeShellSupportGlobalSearchUI(getContext())){
            mLauncher.getSearchBridge().onAnimationStart(SearchBridge.ANIMATION_TYPE_SLIDE_TO_LIFECARD);
            //Log.d(TAG, "onPageBeginMoving!!!");
        }

        /* YUNOS BEGIN */
        //author:kerong.skr post invalidate 1000ms delayed after page moved
        //Object viewRootImpl = ACA.View.getViewRootImpl(this);
        //if(viewRootImpl != null) {
       //     ACA.ViewRootImpl.cancelInvalidate(viewRootImpl, getChildAt(mCurrentPage));
       //     ACA.ViewRootImpl.cancelInvalidate(viewRootImpl, mLauncher.getHotseat());
       // }
        // clear folder close animation when page begin moving
        mLauncher.mFolderUtils.clearAnimation();
        /* YUNOS END */

        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {
            if (mNextPage != INVALID_PAGE) {
                // we're snapping to a particular screen
                enableChildrenCache(mCurrentPage, mNextPage);
            } else {
                // this is when user is actively dragging a particular screen, they might
                // swipe it either left or right (but we won't advance by more than one screen)
                enableChildrenCache(mCurrentPage - 1, mCurrentPage + 1);
            }
        }

        // Only show page outlines as we pan if we are on large screen
        if (LauncherApplication.isScreenLarge()) {
            /*
             * Commented by xiaodong.lxd YUNOS showOutlines();
             */
            mIsStaticWallpaper = mWallpaperManager.getWallpaperInfo() == null;
        }

        // If we are not fading in adjacent screens, we still need to restore the alpha in case the
        // user scrolls while we are transitioning (should not affect dispatchDraw optimizations)
        if (!mWorkspaceFadeInAdjacentScreens) {
            int count = getIconScreenCount();
            int offSet = getIconScreenHomeIndex();
            for (int i = 0; i < count; ++i) {
                ((CellLayout) getPageAt(i + offSet)).setShortcutAndWidgetAlpha(1f);
            }
        }

        // Show the scroll indicator as you pan the page
        showScrollingIndicator(false);
        //((CellLayout) getChildAt(mCurrentPage)).cancelFlingDropDownAnimation();

        /*YUNOS BEGIN added by xiaodong.lxd for bug 101556*/
        if (CheckVoiceCommandPressHelper.isPushTalkCanUse()
                && CheckVoiceCommandPressHelper.getInstance().isVoiceUIShown()) {
            CheckVoiceCommandPressHelper.getInstance().forceDismissVoiceCommand();
        }
        /*YUNOS END*/

        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(163418) ##date:2014/08/15
        // ##description: Added support for widget page
        if (FeatureUtility.hasFullScreenWidget() && !mLauncher.isInLauncherEditMode() &&
                mLauncher.mWidgetPageManager != null) {
            mLauncher.mWidgetPageManager.onPageBeginMoving();
        }
        // YUNOS END PB

        mLauncher.onWorkspacePageBeginMoving();
    }

    protected void onPageEndMoving() {
        super.onPageEndMoving();
        final int screen = getIconScreenHomeIndex();
        final int page = getCurrentPage();
        //BugID:6181566:page number issue
        boolean hasSearchWidget = false;
        boolean hasSearchNowCardWidget = false;
        if ((page - screen) >= 0) {
            hasSearchWidget = LauncherModel.hasSearchWidget(page - screen);
            hasSearchNowCardWidget = LauncherModel.hasSearchNowCardWidget(page - screen);
        }
        Utils.notifySearchPageChanged(getContext(), hasSearchWidget, hasSearchNowCardWidget, page);
        /*YUNOS BEGIN*/
        //##date:2013/11/26 ##author:xindong.zxd
        //open all children view HardwareAccelerated
        /*
        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {
            clearChildrenCache();
        }
        */
        /*YUNOS END*/

        if (mDragController.isDragging()) {
            if (isSmall()) {
                // If we are in springloaded mode, then force an event to check if the current touch
                // is under a new page (to scroll to)
                mDragController.forceTouchMove();
            }
        } else {
            // If we are not mid-dragging, hide the page outlines if we are on a large screen
            if (LauncherApplication.isScreenLarge()) {
                /*
                 * Commented by xiaodong.lxd YUNOS hideOutlines();
                 */
            }

            // Hide the scroll indicator as you pan the page
            if (!mDragController.isDragging()) {
                hideScrollingIndicator(false);
            }
        }
        //mOverScrollMaxBackgroundAlpha = 0.0f;

        if (mDelayedResizeRunnable != null) {
            mDelayedResizeRunnable.run();
            mDelayedResizeRunnable = null;
        }

        if (mDelayedSnapToPageRunnable != null) {
            mDelayedSnapToPageRunnable.run();
            mDelayedSnapToPageRunnable = null;
        }
        /* YUNOS BEGIN */
        //author:lxd #when page end moving, remove empty cell
        if(!mDragController.isDragging()) {
            checkAndRemoveEmptyCell();
        }
        /* YUNOS END */
        CellLayout cellLayout = (CellLayout) getChildAt(mCurrentPage);
        if(cellLayout != null) {
            cellLayout.startFlingDropDownAnimation();
            cellLayout.postInvalidateDelayed(1000);
        }
        /* YUNOS BEGIN */
        //author:kerong.skr post invalidate 1000ms delayed after page moved
        Hotseat hotseat = mLauncher.getHotseat();
        if (hotseat != null) {
            hotseat.postInvalidateDelayed(1000);
        }
        /* YUNOS END */
        mLauncher.onWorkspacePageEndMoving();

        if (mAnimateScrollEffectMode) {
            animateScrollEffect(false);
        }
        if(mScrollEffectAnimator != null && mScrollEffectAnimator.isDemoAnimationAllOver()) {
            if (mLauncher.getPreviewList() != null) {
                Adapter adapter = mLauncher.getPreviewList().getAdapter();
                if (adapter instanceof EffectsPreviewAdapter) {
                    EffectsPreviewAdapter effectsPreviewAdapter = (EffectsPreviewAdapter) adapter;
                    effectsPreviewAdapter.commitEffectValueToPreference();
                }
            }
            mLauncher.getGestureLayer().setTouchEnabled(true);
        }
        if (mLauncher.isSupportLifeCenter()) {
            mLauncher.getCardBridge().onPageEndMoving(mCurrentPage, mLastPage);
        }
        if (mLauncher.isLeftScreen(mCurrentPage)) {
            hotseat.preHideHotseat();
            if (SearchBridge.isHomeShellSupportGlobalSearchUI(getContext())) {
                  mLauncher.getSearchBridge().onAnimationUpdate(SearchBridge.ANIMATION_TYPE_LIFECARD_DROP);
            }
        } else {
            if (isWidgetPageView(getChildAt(mCurrentPage))) {
                hotseat.preHideHotseat();
            } else {
                if (hotseat.getVisibility() == VISIBLE && !mLauncher.isInLauncherEditMode()) {
                    hotseat.afterShowHotseat();
                }
            }
            if (SearchBridge.isHomeShellSupportGlobalSearchUI(getContext())) {
                mLauncher.getSearchBridge().onAnimationUpdate(SearchBridge.ANIMATION_TYPE_HOMESHELL_DROP);
            }

        }

        mLastPage = mCurrentPage;
        if (mCurrentPage >= 0 && mCurrentPage < getIconScreenHomeIndex()) {
            // stay in left pages
            int w = getIconScreenHomeIndex() * getWidth();
            mLauncher.getHotseat().setTranslationX(w);
            mLauncher.getIndicatorView().setTranslationX(w);
        } else {
            // stay in icon screens
            mLauncher.getHotseat().setTranslationX(0);
            mLauncher.getIndicatorView().setTranslationX(0);
        }

        if(SearchBridge.isHomeShellSupportGlobalSearchUI(getContext())){
            mLauncher.getSearchBridge().onAnimationEnd();
        }
    }

    @Override
    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
        Launcher.setScreen(mCurrentPage);
    };

    // As a ratio of screen height, the total distance we want the parallax effect to span
    // horizontally
    private float wallpaperTravelToScreenWidthRatio(int width, int height) {
        float aspectRatio = width / (float) height;

        // At an aspect ratio of 16/10, the wallpaper parallax effect should span 1.5 * screen width
        // At an aspect ratio of 10/16, the wallpaper parallax effect should span 1.2 * screen width
        // We will use these two data points to extrapolate how much the wallpaper parallax effect
        // to span (ie travel) at any aspect ratio:

        final float ASPECT_RATIO_LANDSCAPE = 16/10f;
        final float ASPECT_RATIO_PORTRAIT = 10/16f;
        final float WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE = 1.5f;
        final float WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT = 1.2f;

        // To find out the desired width at different aspect ratios, we use the following two
        // formulas, where the coefficient on x is the aspect ratio (width/height):
        //   (16/10)x + y = 1.5
        //   (10/16)x + y = 1.2
        // We solve for x and y and end up with a final formula:
        final float x =
            (WALLPAPER_WIDTH_TO_SCREEN_RATIO_LANDSCAPE - WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT) /
            (ASPECT_RATIO_LANDSCAPE - ASPECT_RATIO_PORTRAIT);
        final float y = WALLPAPER_WIDTH_TO_SCREEN_RATIO_PORTRAIT - x * ASPECT_RATIO_PORTRAIT;
        return x * aspectRatio + y;
    }

    // The range of scroll values for Workspace
    private int getScrollRange() {
        return getChildOffset(getChildCount() - 1) - getChildOffset(0);
    }

    protected void setWallpaperDimension() {
        /* YUNOS BEGIN */
        // ##date:2013/11/26 ##author:hongxing.whx, ##bugid: 66166
        // change wallpaper dimension according to DisplayMetrics
        /*
        Point minDims = new Point();
        Point maxDims = new Point();
        mLauncher.getWindowManager().getDefaultDisplay().getCurrentSizeRange(minDims, maxDims);

        final int maxDim = Math.max(maxDims.x, maxDims.y);
        final int minDim = Math.min(minDims.x, minDims.y);

        // We need to ensure that there is enough extra space in the wallpaper for the intended
        // parallax effects
        if (LauncherApplication.isScreenLarge()) {
            mWallpaperWidth = (int) (maxDim * wallpaperTravelToScreenWidthRatio(maxDim, minDim));
            mWallpaperHeight = maxDim;
        } else {
            mWallpaperWidth = Math.max((int) (minDim * WALLPAPER_SCREENS_SPAN), maxDim);
            mWallpaperHeight = maxDim;
        }
        */
        DisplayMetrics dm = getResources().getDisplayMetrics();
        // ##date:2014/03/26  ##author: hongxing.whx ##BugId: 104985
        // forcely make height larger than width
        mWallpaperWidth =  (dm.widthPixels > dm.heightPixels) ? dm.heightPixels : dm.widthPixels;
        mWallpaperHeight = (dm.widthPixels < dm.heightPixels) ? dm.heightPixels : dm.widthPixels;
        /* YUNOS END */
        new Thread("setWallpaperDimension") {
            public void run() {
                mWallpaperManager.suggestDesiredDimensions(mWallpaperWidth, mWallpaperHeight);
            }
        }.start();
    }

    private float wallpaperOffsetForCurrentScroll() {
        // Set wallpaper offset steps (1 / (number of screens - 1))
        mWallpaperManager.setWallpaperOffsetSteps(1.0f / (getChildCount() - 1), 1.0f);

        // For the purposes of computing the scrollRange and overScrollOffset, we assume
        // that mLayoutScale is 1. This means that when we're in spring-loaded mode,
        // there's no discrepancy between the wallpaper offset for a given page.
        float layoutScale = mLayoutScale;
        mLayoutScale = 1f;
        int scrollRange = getScrollRange();

        // Again, we adjust the wallpaper offset to be consistent between values of mLayoutScale
        float adjustedScrollX = Math.max(0, Math.min(getScrollX(), mMaxScrollX));
        adjustedScrollX *= mWallpaperScrollRatio;
        mLayoutScale = layoutScale;

        float scrollProgress =
            adjustedScrollX / (float) scrollRange;

        if (LauncherApplication.isScreenLarge() && mIsStaticWallpaper) {
            // The wallpaper travel width is how far, from left to right, the wallpaper will move
            // at this orientation. On tablets in portrait mode we don't move all the way to the
            // edges of the wallpaper, or otherwise the parallax effect would be too strong.
            int wallpaperTravelWidth = Math.min(mWallpaperTravelWidth, mWallpaperWidth);

            float offsetInDips = wallpaperTravelWidth * scrollProgress +
                (mWallpaperWidth - wallpaperTravelWidth) / 2; // center it
            float offset = offsetInDips / (float) mWallpaperWidth;
            return offset;
        } else {
            return scrollProgress;
        }
    }

    private void syncWallpaperOffsetWithScroll() {
        final boolean enableWallpaperEffects = isHardwareAccelerated();
        if (enableWallpaperEffects) {
            mWallpaperOffset.setFinalX(wallpaperOffsetForCurrentScroll());
        }
    }

    public void updateWallpaperOffsetImmediately() {
        mUpdateWallpaperOffsetImmediately = true;
    }

    private void updateWallpaperOffsets() {
        boolean updateNow = false;
        boolean keepUpdating = true;
        if (mUpdateWallpaperOffsetImmediately) {
            updateNow = true;
            keepUpdating = false;
            mWallpaperOffset.jumpToFinal();
            mUpdateWallpaperOffsetImmediately = false;
        } else {
            updateNow = keepUpdating = mWallpaperOffset.computeScrollOffset();
        }
        if (updateNow) {
            if (mWindowToken != null) {
                mWallpaperManager.setWallpaperOffsets(mWindowToken,
                        mWallpaperOffset.getCurrX(), mWallpaperOffset.getCurrY());
            }
        }
        if (keepUpdating) {
            invalidate();
        }
    }

    @Override
    protected void updateCurrentPageScroll() {
        super.updateCurrentPageScroll();
        computeWallpaperScrollRatio(mCurrentPage);
    }

    @Override
    public void snapToPage(int whichPage) {
        super.snapToPage(whichPage);
        computeWallpaperScrollRatio(whichPage);
    }

    @Override
    public void snapToPage(int whichPage, int duration) {
        super.snapToPage(whichPage, duration);
        computeWallpaperScrollRatio(whichPage);
    }

    protected void snapToPage(int whichPage, Runnable r) {
        if (mDelayedSnapToPageRunnable != null) {
            mDelayedSnapToPageRunnable.run();
        }
        mDelayedSnapToPageRunnable = r;
        snapToPage(whichPage, SLOW_PAGE_SNAP_ANIMATION_DURATION);
    }

    private void computeWallpaperScrollRatio(int page) {
        // In Editmode, no need to compute wallpaper ratio
        if (mInEditing) {
            return;
        }
        // Here, we determine what the desired scroll would be with and without a layout scale,
        // and compute a ratio between the two. This allows us to adjust the wallpaper offset
        // as though there is no layout scale.
        float layoutScale = mLayoutScale;
        int scaled = getChildOffset(page) - getRelativeChildOffset(page);
        mLayoutScale = 1.0f;
        float unscaled = getChildOffset(page) - getRelativeChildOffset(page);
        mLayoutScale = layoutScale;
        if (scaled > 0) {
            mWallpaperScrollRatio = (1.0f * unscaled) / scaled;
        } else {
            mWallpaperScrollRatio = 1f;
        }
    }

    class WallpaperOffsetInterpolator {
        float mFinalHorizontalWallpaperOffset = 0.0f;
        float mFinalVerticalWallpaperOffset = 0.5f;
        float mHorizontalWallpaperOffset = 0.0f;
        float mVerticalWallpaperOffset = 0.5f;
        long mLastWallpaperOffsetUpdateTime;
        boolean mIsMovingFast;
        boolean mOverrideHorizontalCatchupConstant;
        float mHorizontalCatchupConstant = 0.35f;
        float mVerticalCatchupConstant = 0.35f;

        public WallpaperOffsetInterpolator() {
        }

        public void setOverrideHorizontalCatchupConstant(boolean override) {
            mOverrideHorizontalCatchupConstant = override;
        }

        public void setHorizontalCatchupConstant(float f) {
            mHorizontalCatchupConstant = f;
        }

        public void setVerticalCatchupConstant(float f) {
            mVerticalCatchupConstant = f;
        }

        public boolean computeScrollOffset() {
            if (Float.compare(mHorizontalWallpaperOffset, mFinalHorizontalWallpaperOffset) == 0 &&
                    Float.compare(mVerticalWallpaperOffset, mFinalVerticalWallpaperOffset) == 0) {
                mIsMovingFast = false;
                return false;
            }
            boolean isLandscape = mDisplaySize.x > mDisplaySize.y;

            long currentTime = System.currentTimeMillis();
            long timeSinceLastUpdate = currentTime - mLastWallpaperOffsetUpdateTime;
            timeSinceLastUpdate = Math.min((long) (1000/30f), timeSinceLastUpdate);
            timeSinceLastUpdate = Math.max(1L, timeSinceLastUpdate);

            float xdiff = Math.abs(mFinalHorizontalWallpaperOffset - mHorizontalWallpaperOffset);
            if (!mIsMovingFast && xdiff > 0.07) {
                mIsMovingFast = true;
            }

            float fractionToCatchUpIn1MsHorizontal;
            if (mOverrideHorizontalCatchupConstant) {
                fractionToCatchUpIn1MsHorizontal = mHorizontalCatchupConstant;
            } else if (mIsMovingFast) {
                fractionToCatchUpIn1MsHorizontal = isLandscape ? 0.5f : 0.75f;
            } else {
                // slow
                fractionToCatchUpIn1MsHorizontal = isLandscape ? 0.27f : 0.5f;
            }
            float fractionToCatchUpIn1MsVertical = mVerticalCatchupConstant;

            fractionToCatchUpIn1MsHorizontal /= 33f;
            fractionToCatchUpIn1MsVertical /= 33f;

            final float UPDATE_THRESHOLD = 0.00001f;
            float hOffsetDelta = mFinalHorizontalWallpaperOffset - mHorizontalWallpaperOffset;
            float vOffsetDelta = mFinalVerticalWallpaperOffset - mVerticalWallpaperOffset;
            boolean jumpToFinalValue = Math.abs(hOffsetDelta) < UPDATE_THRESHOLD &&
                Math.abs(vOffsetDelta) < UPDATE_THRESHOLD;

            // Don't have any lag between workspace and wallpaper on non-large devices
            if (!LauncherApplication.isScreenLarge() || jumpToFinalValue) {
                mHorizontalWallpaperOffset = mFinalHorizontalWallpaperOffset;
                mVerticalWallpaperOffset = mFinalVerticalWallpaperOffset;
            } else {
                float percentToCatchUpVertical =
                    Math.min(1.0f, timeSinceLastUpdate * fractionToCatchUpIn1MsVertical);
                float percentToCatchUpHorizontal =
                    Math.min(1.0f, timeSinceLastUpdate * fractionToCatchUpIn1MsHorizontal);
                mHorizontalWallpaperOffset += percentToCatchUpHorizontal * hOffsetDelta;
                mVerticalWallpaperOffset += percentToCatchUpVertical * vOffsetDelta;
            }

            mLastWallpaperOffsetUpdateTime = System.currentTimeMillis();
            return true;
        }

        public float getCurrX() {
            return mHorizontalWallpaperOffset;
        }

        public float getFinalX() {
            return mFinalHorizontalWallpaperOffset;
        }

        public float getCurrY() {
            return mVerticalWallpaperOffset;
        }

        public float getFinalY() {
            return mFinalVerticalWallpaperOffset;
        }

        public void setFinalX(float x) {
            mFinalHorizontalWallpaperOffset = Math.max(0f, Math.min(x, 1.0f));
        }

        public void setFinalY(float y) {
            mFinalVerticalWallpaperOffset = Math.max(0f, Math.min(y, 1.0f));
        }

        public void jumpToFinal() {
            mHorizontalWallpaperOffset = mFinalHorizontalWallpaperOffset;
            mVerticalWallpaperOffset = mFinalVerticalWallpaperOffset;
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        syncWallpaperOffsetWithScroll();
    }

    void showOutlines() {
        if (!isSmall() && !mIsSwitchingState) {
            if (mChildrenOutlineFadeOutAnimation != null) mChildrenOutlineFadeOutAnimation.cancel();
            if (mChildrenOutlineFadeInAnimation != null) mChildrenOutlineFadeInAnimation.cancel();
            mChildrenOutlineFadeInAnimation = LauncherAnimUtils.ofFloat(this, "childrenOutlineAlpha", 1.0f);
            mChildrenOutlineFadeInAnimation.setDuration(CHILDREN_OUTLINE_FADE_IN_DURATION);
            mChildrenOutlineFadeInAnimation.start();
        }
    }

    void hideOutlines() {
        if (!isSmall() && !mIsSwitchingState) {
            if (mChildrenOutlineFadeInAnimation != null) mChildrenOutlineFadeInAnimation.cancel();
            if (mChildrenOutlineFadeOutAnimation != null) mChildrenOutlineFadeOutAnimation.cancel();
            mChildrenOutlineFadeOutAnimation = LauncherAnimUtils.ofFloat(this, "childrenOutlineAlpha", 0.0f);
            mChildrenOutlineFadeOutAnimation.setDuration(CHILDREN_OUTLINE_FADE_OUT_DURATION);
            mChildrenOutlineFadeOutAnimation.setStartDelay(CHILDREN_OUTLINE_FADE_OUT_DELAY);
            mChildrenOutlineFadeOutAnimation.start();
        }
    }

    public void showOutlinesTemporarily() {
        if (!mIsPageMoving && !isTouchActive()) {
            snapToPage(mCurrentPage);
        }
    }

    public void setChildrenOutlineAlpha(float alpha) {
        mChildrenOutlineAlpha = alpha;
        int count = getIconScreenCount();
        int offSet = getIconScreenHomeIndex();
        for (int i = 0; i < count; i++) {
            CellLayout cl = (CellLayout) getChildAt(i + offSet);
            cl.setBackgroundAlpha(alpha);
        }
    }

    public float getChildrenOutlineAlpha() {
        return mChildrenOutlineAlpha;
    }

    void disableBackground() {
        mDrawBackground = false;
    }
    void enableBackground() {
        mDrawBackground = true;
    }

    private void animateBackgroundGradient(float finalAlpha, boolean animated) {
        if (mBackground == null) return;
        /* YUNOS BEGIN */
        // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
        // if (mBackgroundFadeInAnimation != null) {
        // mBackgroundFadeInAnimation.cancel();
        // mBackgroundFadeInAnimation = null;
        // }
        /* YUNOS END */
        if (mBackgroundFadeOutAnimation != null) {
            mBackgroundFadeOutAnimation.cancel();
            mBackgroundFadeOutAnimation = null;
        }
        float startAlpha = getBackgroundAlpha();
        if (finalAlpha != startAlpha) {
            if (animated) {
                mBackgroundFadeOutAnimation =
                        LauncherAnimUtils.ofFloat(this, startAlpha, finalAlpha);
                mBackgroundFadeOutAnimation.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        setBackgroundAlpha(((Float) animation.getAnimatedValue()).floatValue());
                    }
                });
                mBackgroundFadeOutAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
                mBackgroundFadeOutAnimation.setDuration(BACKGROUND_FADE_OUT_DURATION);
                mBackgroundFadeOutAnimation.start();
            } else {
                setBackgroundAlpha(finalAlpha);
            }
        }
    }

    public void setBackgroundAlpha(float alpha) {
        if (alpha != mBackgroundAlpha) {
            mBackgroundAlpha = alpha;
            invalidate();
        }
    }

    public float getBackgroundAlpha() {
        return mBackgroundAlpha;
    }

    float backgroundAlphaInterpolator(float r) {
        float pivotA = 0.1f;
        float pivotB = 0.4f;
        if (r < pivotA) {
            return 0;
        } else if (r > pivotB) {
            return 1.0f;
        } else {
            return (r - pivotA)/(pivotB - pivotA);
        }
    }

    private void updatePageAlphaValues(int screenCenter) {
        boolean isInOverscroll = mOverScrollX < 0 || mOverScrollX > mMaxScrollX;
        if (mWorkspaceFadeInAdjacentScreens &&
                mState == State.NORMAL &&
                !mIsSwitchingState &&
                !isInOverscroll) {
            for (int i = 0; i < getChildCount(); i++) {
                CellLayout child = (CellLayout) getChildAt(i);
                if (child != null) {
                    float scrollProgress = getScrollProgress(screenCenter, child, i);
                    float alpha = 1 - Math.abs(scrollProgress);
                    child.getShortcutAndWidgetContainer().setAlpha(alpha);
                    if (!mIsDragOccuring) {
                        child.setBackgroundAlphaMultiplier(
                                backgroundAlphaInterpolator(Math.abs(scrollProgress)));
                    } else {
                        child.setBackgroundAlphaMultiplier(1f);
                    }
                }
            }
        }
    }

    private void setChildrenBackgroundAlphaMultipliers(float a) {
        int count = getIconScreenCount();
        int offSet = getIconScreenHomeIndex();
        for (int i = 0; i < count; i++) {
            CellLayout child = (CellLayout) getChildAt(i + offSet);
            child.setBackgroundAlphaMultiplier(a);
        }
    }

    @Override
    protected void screenScrolled(int screenCenter) {
        super.screenScrolled(screenCenter);
        updatePageAlphaValues(screenCenter);
        /*YUNOS BEGIN*/
        //##date:2013/11/26 ##author:xindong.zxd
        //always open hardwareLayers
        //enableHwLayersOnVisiblePages();
        /*YUNOS END*/

        if (mOverScrollX < 0 || mOverScrollX > mMaxScrollX) {
            /* YUNOS BEGIN */
            // ##date:2013/11/26 ##author:xiaodong.lxd
            //disable effects
            /*
            int index = 0;
            float pivotX = 0f;
            final float leftBiasedPivot = 0.25f;
            final float rightBiasedPivot = 0.75f;
            final int lowerIndex = 0;
            final int upperIndex = getChildCount() - 1;
            if (isRtl) {
                index = mOverScrollX < 0 ? upperIndex : lowerIndex;
                pivotX = (index == 0 ? leftBiasedPivot : rightBiasedPivot);
            } else {
                index = mOverScrollX < 0 ? lowerIndex : upperIndex;
                pivotX = (index == 0 ? rightBiasedPivot : leftBiasedPivot);
            }

            CellLayout cl = (CellLayout) getChildAt(index);
            float scrollProgress = getScrollProgress(screenCenter, cl, index);
            final boolean isLeftPage = (isRtl ? index > 0 : index == 0);
            cl.setOverScrollAmount(Math.abs(scrollProgress), isLeftPage);
            float rotation = -WORKSPACE_OVERSCROLL_ROTATION * scrollProgress;
            cl.setRotationY(rotation);
            setFadeForOverScroll(Math.abs(scrollProgress));
            if (!mOverscrollTransformsSet) {
                mOverscrollTransformsSet = true;
                cl.setCameraDistance(mDensity * mCameraDistance);
                cl.setPivotX(cl.getMeasuredWidth() * pivotX);
                cl.setPivotY(cl.getMeasuredHeight() * 0.5f);
                cl.setOverscrollTransformsDirty(true);
            }
        */
            /* YUNOS END */
            } else {
            if (mOverscrollFade != 0) {
                setFadeForOverScroll(0);
            }
            if (mOverscrollTransformsSet) {
                mOverscrollTransformsSet = false;
                ((CellLayout) getChildAt(getIconScreenHomeIndex())).resetOverscrollTransforms();
                ((CellLayout) getChildAt(getChildCount() - 1)).resetOverscrollTransforms();
            }
        }

     // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(163418) ##date:2014/08/15
        // ##description: Added support for widget page
        if(FeatureUtility.hasFullScreenWidget() && !mLauncher.isInLauncherEditMode()) {
            hotseatScrolled(screenCenter);
        }
        // YUNOS END PB

        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: dragging screen
        if (mDragScreenDelayed) {
            mDragScreenDelayed = false;
            CellLayout current = (CellLayout)getChildAt(getCurrentPage());
            mLauncher.getDragLayer().animateViewIntoPosition(mDragScreen, current,
                    ADJACENT_SCREEN_DROP_DURATION, null, this);
        }
        /* YUNOS END */
    }

    private boolean mDragScreenDelayed = false;
    private DragView mDragScreen = null;

    @Override
    protected void overScroll(float amount) {
        //acceleratedOverScroll(amount);
        dampedOverScroll(amount);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWindowToken = getWindowToken();
        computeScroll();
        mDragController.setWindowToken(mWindowToken);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWindowToken = null;
    }

    protected void cancelFlingDropDownAnimation() {
        CellLayout layout = null;
        int count = 0;
        int screenCount = getIconScreenCount();
        int offSet = getIconScreenHomeIndex();
        for (int i = 0; i < screenCount; i++) {
            layout = (CellLayout) getChildAt(i + offSet);
            count = layout.cancelFlingDropDownAnimation();
            if (count > 0) {
                layout.postInvalidate();
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mFirstLayout && mCurrentPage >= 0 && mCurrentPage < getChildCount()) {
            mUpdateWallpaperOffsetImmediately = true;
        }
        super.onLayout(changed, left, top, right, bottom);
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(5674413) ##date:2014/12/30
        // ##description: separate mainmenu slide effect from screen's.
        if (mTransitionEffect == -1) {
            mTransitionEffect = HomeShellSetting.getSlideEffectMode(mContext);
            onTransitionEffectChanged(-1, mTransitionEffect);
        }
        /* YUNOS END PB */
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: header in cell layout
        if (mInEditing && !mInCategory) {
            int count = getChildCount();
            int height = (int)(sHeaderHeight / sEditScale);
            for (int i = 0; i < count; i++) {
                CellLayout child = (CellLayout)getChildAt(i);
                if (child.getMode() == CellLayout.Mode.NORMAL) {
                    child.layout(child.getLeft(), child.getTop(), child.getRight(), child.getTop() + sHeight + height);
                    child.invalidate();
                }
            }
        }
        /* YUNOS END */
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Commented by xiaodong.lxd, cause loop dispatchDraw on Pad
        // updateWallpaperOffsets();

        // Draw the background gradient if necessary
        if (mBackground != null && mBackgroundAlpha > 0.0f && mDrawBackground) {
            int alpha = (int) (mBackgroundAlpha * 255);
            mBackground.setAlpha(alpha);
            mBackground.setBounds(getScrollX(), 0, getScrollX() + getMeasuredWidth(),
                    getMeasuredHeight());
            mBackground.draw(canvas);
        }

        super.onDraw(canvas);

        // Call back to LauncherModel to finish binding after the first draw
        post(mBindPages);
    }

    boolean isDrawingBackgroundGradient() {
        return (mBackground != null && mBackgroundAlpha > 0.0f && mDrawBackground);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (!mLauncher.isAllAppsVisible()) {
            final Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                return openFolder.requestFocus(direction, previouslyFocusedRect);
            } else {
                return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
            }
        }
        return false;
    }

    @Override
    public int getDescendantFocusability() {
        if (isSmall()) {
            return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
        }
        return super.getDescendantFocusability();
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (!mLauncher.isAllAppsVisible()) {
            final Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                openFolder.addFocusables(views, direction);
            } else {
                super.addFocusables(views, direction, focusableMode);
            }
        }
    }

    public boolean isSmall() {
        return mState == State.SMALL || mState == State.SPRING_LOADED;
    }

    void enableChildrenCache(int fromPage, int toPage) {
        if (fromPage > toPage) {
            final int temp = fromPage;
            fromPage = toPage;
            toPage = temp;
        }

        final int screenCount = getIconScreenCount();

        fromPage = Math.max(fromPage, 0);
        toPage = Math.min(toPage, screenCount - 1);
        int offSet = getIconScreenHomeIndex();
        for (int i = fromPage; i <= toPage; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i + offSet);
            layout.setChildrenDrawnWithCacheEnabled(true);
            layout.setChildrenDrawingCacheEnabled(true);
        }
    }

    void clearChildrenCache() {
        int count = getIconScreenCount();
        int offSet = getIconScreenHomeIndex();
        for (int i = 0; i < count; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i + offSet);
            layout.setChildrenDrawnWithCacheEnabled(false);
            // In software mode, we don't want the items to continue to be drawn into bitmaps
            if (!isHardwareAccelerated()) {
                layout.setChildrenDrawingCacheEnabled(false);
            }
        }
    }


    private void updateChildrenLayersEnabled(boolean force) {
        /*YUNOS BEGIN*/
        //##date:2013/11/26 ##author:xindong.zxd
        //always open hardwareLayers
        /*
        boolean small = mState == State.SMALL || mIsSwitchingState;
        boolean enableChildrenLayers = force || small || mAnimatingViewIntoPlace || isPageMoving();

        if (enableChildrenLayers != mChildrenLayersEnabled) {
            mChildrenLayersEnabled = enableChildrenLayers;
            if (mChildrenLayersEnabled) {
                enableHwLayersOnVisiblePages();
            } else {
                for (int i = 0; i < getPageCount(); i++) {
                    final CellLayout cl = (CellLayout) getChildAt(i);
                    cl.disableHardwareLayers();
                }
            }
        }
        */
        /*YUNOS END*/
    }

//    private void enableHwLayersOnVisiblePages() {
//        if (mChildrenLayersEnabled) {
//            final int screenCount = getChildCount();
//            getVisiblePages(mTempVisiblePagesRange);
//            int leftScreen = mTempVisiblePagesRange[0];
//            int rightScreen = mTempVisiblePagesRange[1];
//            if (leftScreen == rightScreen) {
//                // make sure we're caching at least two pages always
//                if (rightScreen < screenCount - 1) {
//                    rightScreen++;
//                } else if (leftScreen > 0) {
//                    leftScreen--;
//                }
//            }
//            for (int i = 0; i < screenCount; i++) {
//                final CellLayout layout = (CellLayout) getPageAt(i);
//                if (!(leftScreen <= i && i <= rightScreen && shouldDrawChild(layout))) {
//                    layout.disableHardwareLayers();
//                }
//            }
//            for (int i = 0; i < screenCount; i++) {
//                final CellLayout layout = (CellLayout) getPageAt(i);
//                if (leftScreen <= i && i <= rightScreen && shouldDrawChild(layout)) {
//                    layout.enableHardwareLayers();
//                }
//            }
//        }
//    }

    public void buildPageHardwareLayers() {
        // force layers to be enabled just for the call to buildLayer
        updateChildrenLayersEnabled(true);
        if (getWindowToken() != null) {
            int screenCount = getIconScreenCount();
            int offSet = getIconScreenHomeIndex();
            for (int i = 0; i < screenCount; i++) {
                CellLayout cl = (CellLayout) getChildAt(i + offSet);
                cl.buildHardwareLayer();
            }
        }
        updateChildrenLayersEnabled(false);
    }

    protected void onWallpaperTap(MotionEvent ev) {
        final int[] position = mTempCell;
        getLocationOnScreen(position);

        int pointerIndex = ev.getActionIndex();
        position[0] += (int) ev.getX(pointerIndex);
        position[1] += (int) ev.getY(pointerIndex);

        mWallpaperManager.sendWallpaperCommand(getWindowToken(),
                ev.getAction() == MotionEvent.ACTION_UP
                        ? WallpaperManager.COMMAND_TAP : WallpaperManager.COMMAND_SECONDARY_TAP,
                position[0], position[1], 0, null);
    }

    /*
     * This interpolator emulates the rate at which the perceived scale of an object changes
     * as its distance from a camera increases. When this interpolator is applied to a scale
     * animation on a view, it evokes the sense that the object is shrinking due to moving away
     * from the camera.
     */
    static class ZInterpolator implements TimeInterpolator {
        private float focalLength;

        public ZInterpolator(float foc) {
            focalLength = foc;
        }

        public float getInterpolation(float input) {
            return (1.0f - focalLength / (focalLength + input)) /
                (1.0f - focalLength / (focalLength + 1.0f));
        }
    }

    /*
     * The exact reverse of ZInterpolator.
     */
    static class InverseZInterpolator implements TimeInterpolator {
        private ZInterpolator zInterpolator;
        public InverseZInterpolator(float foc) {
            zInterpolator = new ZInterpolator(foc);
        }
        public float getInterpolation(float input) {
            return 1 - zInterpolator.getInterpolation(1 - input);
        }
    }

    /*
     * ZInterpolator compounded with an ease-out.
     */
    static class ZoomOutInterpolator implements TimeInterpolator {
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(0.75f);
        private final ZInterpolator zInterpolator = new ZInterpolator(0.13f);

        public float getInterpolation(float input) {
            return decelerate.getInterpolation(zInterpolator.getInterpolation(input));
        }
    }

    /*
     * InvereZInterpolator compounded with an ease-out.
     */
    static class ZoomInInterpolator implements TimeInterpolator {
        private final InverseZInterpolator inverseZInterpolator = new InverseZInterpolator(0.35f);
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(3.0f);

        public float getInterpolation(float input) {
            return decelerate.getInterpolation(inverseZInterpolator.getInterpolation(input));
        }
    }

    private final ZoomInInterpolator mZoomInInterpolator = new ZoomInInterpolator();

    /*
    *
    * We call these methods (onDragStartedWithItemSpans/onDragStartedWithSize) whenever we
    * start a drag in Launcher, regardless of whether the drag has ever entered the Workspace
    *
    * These methods mark the appropriate pages as accepting drops (which alters their visual
    * appearance).
    *
    */
    public void onDragStartedWithItem(View v) {
        final Canvas canvas = new Canvas();

        // The outline is used to visualize where the item will land if dropped
        mDragOutline = createDragOutline(v, canvas, DRAG_BITMAP_PADDING);
    }

    public void onDragStartedWithItem(PendingAddItemInfo info, Bitmap b, boolean clipAlpha) {
        final Canvas canvas = new Canvas();

        int[] size = estimateItemSize(info.spanX, info.spanY, info, false);

        // The outline is used to visualize where the item will land if dropped
        mDragOutline = createDragOutline(b, canvas, DRAG_BITMAP_PADDING, size[0],
                size[1], clipAlpha);
    }

    public void exitWidgetResizeMode() {
        DragLayer dragLayer = mLauncher.getDragLayer();
        dragLayer.clearAllResizeFrames();
    }

    private void initAnimationArrays() {
        final int childCount = getChildCount();
        if (mOldTranslationXs != null && mOldTranslationXs.length == childCount) return;
        mOldTranslationXs = new float[childCount];
        mOldTranslationYs = new float[childCount];
        mOldScaleXs = new float[childCount];
        mOldScaleYs = new float[childCount];
        mOldBackgroundAlphas = new float[childCount];
        mOldAlphas = new float[childCount];
        mNewTranslationXs = new float[childCount];
        mNewTranslationYs = new float[childCount];
        mNewScaleXs = new float[childCount];
        mNewScaleYs = new float[childCount];
        mNewBackgroundAlphas = new float[childCount];
        mNewAlphas = new float[childCount];
        mNewRotationYs = new float[childCount];
    }

    Animator getChangeStateAnimation(final State state, boolean animated) {
        return getChangeStateAnimation(state, animated, 0);
    }

    Animator getChangeStateAnimation(final State state, boolean animated, int delay) {
        if (mState == state) {
            return null;
        }

        // Initialize animation arrays for the first time if necessary
        initAnimationArrays();

        AnimatorSet anim = animated ? LauncherAnimUtils.createAnimatorSet() : null;

        // Stop any scrolling, move to the current page right away
        setCurrentPage(getNextPage());

        final State oldState = mState;
        final boolean oldStateIsNormal = (oldState == State.NORMAL);
        final boolean oldStateIsSpringLoaded = (oldState == State.SPRING_LOADED);
        final boolean oldStateIsSmall = (oldState == State.SMALL);
        mState = state;
        final boolean stateIsNormal = (state == State.NORMAL);
        final boolean stateIsSpringLoaded = (state == State.SPRING_LOADED);
        final boolean stateIsSmall = (state == State.SMALL);
        float finalScaleFactor = 1.0f;
        float finalBackgroundAlpha = stateIsSpringLoaded ? 1.0f : 0f;
        float translationX = 0;
        float translationY = 0;
        boolean zoomIn = true;

        if (state != State.NORMAL) {
            finalScaleFactor = mSpringLoadedShrinkFactor - (stateIsSmall ? 0.8f : 0);
            setPageSpacing(mSpringLoadedPageSpacing);
            if (oldStateIsNormal && stateIsSmall) {
                zoomIn = false;
                setLayoutScale(finalScaleFactor);
                updateChildrenLayersEnabled(false);
            } else {
                finalBackgroundAlpha = 1.0f;
                setLayoutScale(finalScaleFactor);
            }
        } else {
            setPageSpacing(mOriginalPageSpacing);
            setLayoutScale(1.0f);
        }

        final int duration = zoomIn ?
                getResources().getInteger(R.integer.config_workspaceUnshrinkTime) :
                getResources().getInteger(R.integer.config_appsCustomizeWorkspaceShrinkTime);
        int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            final CellLayout cl = (CellLayout) getChildAt(i);
            float finalAlpha = (!mWorkspaceFadeInAdjacentScreens || stateIsSpringLoaded ||
                    (i == mCurrentPage)) ? 1f : 0f;
            float currentAlpha = cl.getShortcutAndWidgetContainer().getAlpha();
            float initialAlpha = currentAlpha;

            // Determine the pages alpha during the state transition
            if ((oldStateIsSmall && stateIsNormal) ||
                (oldStateIsNormal && stateIsSmall)) {
                // To/from workspace - only show the current page unless the transition is not
                //                     animated and the animation end callback below doesn't run;
                //                     or, if we're in spring-loaded mode
                if (i == mCurrentPage || !animated || oldStateIsSpringLoaded) {
                    finalAlpha = 1f;
                } else {
                    initialAlpha = 0f;
                    finalAlpha = 0f;
                }
            }

            mOldAlphas[i] = initialAlpha;
            mNewAlphas[i] = finalAlpha;
            if (animated) {
                mOldTranslationXs[i] = cl.getTranslationX();
                mOldTranslationYs[i] = cl.getTranslationY();
                mOldScaleXs[i] = cl.getScaleX();
                mOldScaleYs[i] = cl.getScaleY();
                mOldBackgroundAlphas[i] = cl.getBackgroundAlpha();

                mNewTranslationXs[i] = translationX;
                mNewTranslationYs[i] = translationY;
                mNewScaleXs[i] = finalScaleFactor;
                mNewScaleYs[i] = finalScaleFactor;
                mNewBackgroundAlphas[i] = finalBackgroundAlpha;
            } else {
                cl.setTranslationX(translationX);
                cl.setTranslationY(translationY);
                cl.setScaleX(finalScaleFactor);
                cl.setScaleY(finalScaleFactor);
                cl.setBackgroundAlpha(finalBackgroundAlpha);
                cl.setShortcutAndWidgetAlpha(finalAlpha);
            }
        }

        if (animated) {
            for (int index = 0; index < screenCount; index++) {
                final int i = index;
                final CellLayout cl = (CellLayout) getChildAt(i);
                float currentAlpha = cl.getShortcutAndWidgetContainer().getAlpha();
                if (mOldAlphas[i] == 0 && mNewAlphas[i] == 0) {
                    cl.setTranslationX(mNewTranslationXs[i]);
                    cl.setTranslationY(mNewTranslationYs[i]);
                    cl.setScaleX(mNewScaleXs[i]);
                    cl.setScaleY(mNewScaleYs[i]);
                    cl.setBackgroundAlpha(mNewBackgroundAlphas[i]);
                    cl.setShortcutAndWidgetAlpha(mNewAlphas[i]);
                    cl.setRotationY(mNewRotationYs[i]);
                } else {
                    LauncherViewPropertyAnimator a = new LauncherViewPropertyAnimator(cl);
                    a.translationX(mNewTranslationXs[i])
                        .translationY(mNewTranslationYs[i])
                        .scaleX(mNewScaleXs[i])
                        .scaleY(mNewScaleYs[i])
                        .setDuration(duration)
                        .setInterpolator(mZoomInInterpolator);
                    anim.play(a);

                    if (mOldAlphas[i] != mNewAlphas[i] || currentAlpha != mNewAlphas[i]) {
                        LauncherViewPropertyAnimator alphaAnim =
                                new LauncherViewPropertyAnimator(cl.getShortcutAndWidgetContainer());
                        alphaAnim.alpha(mNewAlphas[i])
                            .setDuration(duration)
                            .setInterpolator(mZoomInInterpolator);
                        anim.play(alphaAnim);
                    }
                    if (mOldBackgroundAlphas[i] != 0 ||
                        mNewBackgroundAlphas[i] != 0) {
                        ValueAnimator bgAnim =
                                ValueAnimator.ofFloat(0f, 1f).setDuration(duration);
                        bgAnim.setInterpolator(mZoomInInterpolator);
                        bgAnim.addUpdateListener(new LauncherAnimatorUpdateListener() {
                                public void onAnimationUpdate(float a, float b) {
                                    cl.setBackgroundAlpha(
                                            a * mOldBackgroundAlphas[i] +
                                            b * mNewBackgroundAlphas[i]);
                                }
                            });
                        anim.play(bgAnim);
                    }
                }
            }
            anim.setStartDelay(delay);
        }

        if (stateIsSpringLoaded) {
            // Right now we're covered by Apps Customize
            // Show the background gradient immediately, so the gradient will
            // be showing once AppsCustomize disappears
            animateBackgroundGradient(getResources().getInteger(
                    R.integer.config_appsCustomizeSpringLoadedBgAlpha) / 100f, false);
        } else {
            // Fade the background gradient away
            animateBackgroundGradient(0f, true);
        }
        return anim;
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        mIsSwitchingState = true;
        updateChildrenLayersEnabled(false);
        cancelScrollingIndicatorAnimations();
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
        mTransitionProgress = t;
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        mIsSwitchingState = false;
        mWallpaperOffset.setOverrideHorizontalCatchupConstant(false);
        updateChildrenLayersEnabled(false);
        // The code in getChangeStateAnimation to determine initialAlpha and finalAlpha will ensure
        // ensure that only the current page is visible during (and subsequently, after) the
        // transition animation.  If fade adjacent pages is disabled, then re-enable the page
        // visibility after the transition animation.
        if (!mWorkspaceFadeInAdjacentScreens) {
            for (int i = 0; i < getChildCount(); i++) {
                final CellLayout cl = (CellLayout) getChildAt(i);
                cl.setShortcutAndWidgetAlpha(1f);
            }
        }
    }

    @Override
    public View getContent() {
        return this;
    }

    /**
     * Draw the View v into the given Canvas.
     *
     * @param v the view to draw
     * @param destCanvas the canvas to draw on
     * @param padding the horizontal and vertical padding to use when drawing
     */
    private void drawDragView(View v, Canvas destCanvas, int padding, boolean pruneToDrawable) {
        final Rect clipRect = mTempRect;
        v.getDrawingRect(clipRect);

        destCanvas.save();
        if (v instanceof TextView && pruneToDrawable) {
            Drawable d = ((TextView) v).getCompoundDrawables()[1];
            clipRect.set(0, 0, d.getIntrinsicWidth() + padding, d.getIntrinsicHeight() + padding);
            destCanvas.translate(padding / 2, padding / 2);
            d.draw(destCanvas);
        } else {
            destCanvas.translate(-v.getScrollX() + padding / 2, -v.getScrollY() + padding / 2);
            destCanvas.clipRect(clipRect, Op.REPLACE);

            /*YUNOS BEGIN*/
            //##module(component name)
            //##date:2014/03/28 ##author:jun.dongj@alibaba-inc.com##BugID:105707
            //re-set the icon long press effect
            v.draw(destCanvas);
            /*YUNOS END*/
        }
        destCanvas.restore();
    }

    private boolean drawFanShapedDragView(Set<View> views,Canvas destCanvas, int ico_cnt,int padding){
        if((views == null) || (views.size() <= 1) || (ico_cnt > views.size()))
            return false;

        final Rect clipRect = mTempRect;
        int textWidth = mMultiDragNumTextView.getWidth();
        int textHeight = mMultiDragNumTextView.getHeight();
        PaintFlagsDrawFilter filter = new PaintFlagsDrawFilter(0,Paint.ANTI_ALIAS_FLAG);
        Iterator<View> it = views.iterator();
        int index = 0;
        int extraWidth = textWidth/2 - (ico_cnt - 1)*FAN_SHAPED_ICO_SHIFT_X;
        extraWidth = (extraWidth > 0) ? extraWidth:0;
        float shiftx = 0;
        boolean supportCardIcon = mLauncher.getIconManager().supprtCardIcon();
        while(it.hasNext() && (index < ico_cnt)){
            View view = it.next();
            if (!supportCardIcon) {
                if (view instanceof BubbleTextView) {
                    BubbleTextView textView = (BubbleTextView) view;
                    textView.setDisableLabel(true);
                    textView.setIndicatorVisible(false);
                } else if (view instanceof FolderIcon) {
                    FolderIcon folderIcon = (FolderIcon) view;
                    folderIcon.getTitleText().setDisableLabel(true);
                }
            }
            view.getDrawingRect(mTempRect);
            destCanvas.save();
            destCanvas.setDrawFilter(filter);
            if(index == 0){
                shiftx = padding / 2 + view.getHeight()*(float)Math.sin(FAN_SHAPED_ROTATE_ANGLE*(ico_cnt-1)*Math.PI/180) + extraWidth;
            }
            destCanvas.translate(-view.getScrollX() + shiftx + index*FAN_SHAPED_ICO_SHIFT_X,
                    -view.getScrollY() + padding / 2 + (ico_cnt - index - 1)
                            * FAN_SHAPED_ICO_SHIFT_Y+textHeight/2);
            destCanvas.rotate(-FAN_SHAPED_ROTATE_ANGLE*(ico_cnt-index-1), clipRect.right, clipRect.bottom);
            view.draw(destCanvas);
            destCanvas.restore();
            index++;
        }
        destCanvas.save();
        destCanvas.translate(-mMultiDragNumTextView.getScrollX() + shiftx
                + (index-1)* FAN_SHAPED_ICO_SHIFT_X - textWidth/2 ,
                -mMultiDragNumTextView.getScrollY() + padding / 2 );
        mMultiDragNumTextView.draw(destCanvas);
        destCanvas.restore();
        return true;
    }

    /**
     * Returns a new bitmap to show when the given View is being dragged around.
     * Responsibility for the bitmap is transferred to the caller.
     */
    TextView mMultiDragNumTextView = null;
    public Bitmap createDragBitmap(View v, Canvas canvas, int padding) {
        Bitmap b;
        int ico_cnt = 0;

        if (isMultiSelectDragging()) {
            if (mMultiDragNumTextView == null) {
                mMultiDragNumTextView = new TextView(mContext);
                mMultiDragNumTextView.setBackgroundResource(R.drawable.em_ic_corner_mark_bg);
                // mMultiDragNumTextView.setText("10");
                mMultiDragNumTextView.setTextColor(0xffffffff);
                mMultiDragNumTextView.setTextSize(12);
                // mMultiDragNumTextView.setPadding(2, 2, 2, 2);
                mMultiDragNumTextView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                mMultiDragNumTextView.setGravity(Gravity.CENTER);
            }
            mMultiDragNumTextView.setText(Integer.toString(selectedViews.size()));
            mMultiDragNumTextView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            mMultiDragNumTextView.layout(0, 0, mMultiDragNumTextView.getMeasuredWidth(), mMultiDragNumTextView.getMeasuredWidth());
            int textHeight = mMultiDragNumTextView.getHeight();
            ico_cnt = selectedViews.size() >= FAN_SHAPED_MAX_ICO_CNT ? FAN_SHAPED_MAX_ICO_CNT : selectedViews.size();
            int height = v.getHeight() + padding
                    + (int) Math.ceil(v.getWidth() * Math.sin(FAN_SHAPED_ROTATE_ANGLE * (FAN_SHAPED_MAX_ICO_CNT - 1) * Math.PI / 180))
                    + (FAN_SHAPED_MAX_ICO_CNT - 1) * FAN_SHAPED_ICO_SHIFT_Y + textHeight / 2;
            int width = v.getWidth() + padding
                    + (int) Math.ceil(Math.sin(FAN_SHAPED_ROTATE_ANGLE * (FAN_SHAPED_MAX_ICO_CNT - 1) * Math.PI / 180)) * v.getHeight()
                    + (FAN_SHAPED_MAX_ICO_CNT - 1) * FAN_SHAPED_ICO_SHIFT_X;
            b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } else {
            b = Bitmap.createBitmap(v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);
        }

        canvas.setBitmap(b);
        if (isMultiSelectDragging()) {
            if (!drawFanShapedDragView(selectedViews, canvas, ico_cnt, padding)) {
                drawDragView(v, canvas, padding, false);
            };
        }else{
            drawDragView(v, canvas, padding, false);
        }
        canvas.setBitmap(null);

        return b;
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    private Bitmap createDragOutline(View v, Canvas canvas, int padding) {
        //final int outlineColor = getResources().getColor(android.R.color.holo_blue_light);
        final Bitmap b = Bitmap.createBitmap(
                v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);

        canvas.setBitmap(b);
        drawDragView(v, canvas, padding, false);
        /*YUNOS BEGIN*/
        //##module(homeshell)
        //##date:2013/12/04 ##author:jun.dongj@alibaba-inc.com##BugID:69331
        //replace outline with bitmap
        //mOutlineHelper.applyMediumExpensiveOutlineWithBlur(b, canvas, outlineColor, outlineColor);
        /*YUNOS END*/
        canvas.setBitmap(null);
        return b;
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    private Bitmap createDragOutline(Bitmap orig, Canvas canvas, int padding, int w, int h,
            boolean clipAlpha) {
        //final int outlineColor = getResources().getColor(android.R.color.holo_blue_light);

        /*YUNOS BEGIN*/
        //##module(homeshell)
        //##date:2014/1/16 ##author:xindong.zxd@alibaba-inc.com
        // reduce bmp size 3MB about widget Outline Bitmap
        //final Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final Bitmap b = Bitmap.createBitmap(w/2,h/2, Bitmap.Config.RGB_565);
        canvas.setBitmap(b);

        Rect src = new Rect(0, 0, orig.getWidth(), orig.getHeight());
        float scaleFactor = Math.min((w - padding) / (float) orig.getWidth(),
                (h - padding) / (float) orig.getHeight());

        int scaledWidth = (int) (scaleFactor * orig.getWidth());
        int scaledHeight = (int) (scaleFactor * orig.getHeight());

        Rect dst = new Rect(0, 0, scaledWidth/2, scaledHeight/2);
        /*YUNOS END*/

        // center the image
        dst.offset((w - scaledWidth) / 2, (h - scaledHeight) / 2);

        canvas.drawBitmap(orig, src, dst, null);
        /*YUNOS BEGIN*/
        //##module(homeshell)
        //##date:2013/12/04 ##author:jun.dongj@alibaba-inc.com##BugID:69331
        //replace outline with bitmap
        //mOutlineHelper.applyMediumExpensiveOutlineWithBlur(b, canvas, outlineColor, outlineColor,
        //        clipAlpha);
        /*YUNOS END*/
        canvas.setBitmap(null);

        return b;
    }

    void startDrag(CellLayout.CellInfo cellInfo, DragSource source) {
        mScreenDragStart = getCurrentPage();
        clearSelectFlag();
        View child = cellInfo.cell;

        // Make sure the drag was started by a long press as opposed to a long click.
        if (!child.isInTouchMode()) {
            return;
        }

        //added by lxd : one app is in delete progress or the delete dialog is on, cancel the next drag
        if(mLauncher.isDragToDelete() || mLauncher.isDragToClone()) {
            return;
        }

        mDragInfo = cellInfo;
        /* YUNOS BEGIN */
        // ##date:2014/8/13 ##author:hongchao.ghc ##BugID:5185920
        ViewParent parent = child.getParent();
        if (parent == null) {
            return;
        }

        child.setVisibility(INVISIBLE);
        CellLayout layout = (CellLayout) parent.getParent();
        /* YUNOS END */
        layout.prepareChildForDrag(child);

        child.clearFocus();
        child.setPressed(false);
        LayoutParams lp = child.getLayoutParams();
        if (lp instanceof CellLayout.LayoutParams) {
            ((CellLayout.LayoutParams)lp).startWithGap = false;
        }

        final Canvas canvas = new Canvas();

        // The outline is used to visualize where the item will land if dropped
        mDragOutline = createDragOutline(child, canvas, DRAG_BITMAP_PADDING);
        LauncherModel.DragInfo dragInfo = new LauncherModel.DragInfo();
//      dragInfo.cell = cellInfo.cell;
        dragInfo.cellX = cellInfo.cellX;
        dragInfo.cellY = cellInfo.cellY;
        dragInfo.container = cellInfo.container;
        dragInfo.screen = cellInfo.screen;
        dragInfo.spanX = cellInfo.spanX;
        dragInfo.spanY = cellInfo.spanY;
        LauncherModel.addDragInfo(cellInfo.cell, dragInfo);
        beginDragShared(child, source);
    }

    public void beginDragScreenShared(View child, DragSource source) {
        if (child == null) {
            return;
        }

        final Bitmap b = createDragBitmap(child, new Canvas(), 0);

        int bmpWidth = 0;
        int bmpHeight = 0;
        if (b != null) {
            bmpWidth = b.getWidth();
            bmpHeight = b.getHeight();
        } else {
            child.setVisibility(VISIBLE);
            return;
        }

        mLauncher.getEditModeHelper().onCellLayoutBeginDrag(getCurrentPage(), true);

        float scale = mLauncher.getDragLayer().getLocationInDragLayer(child, mTempXY);
        int dragLayerX = Math.round(mTempXY[0] - (bmpWidth - bmpWidth * scale) / 2);
        int dragLayerY = Math.round(mTempXY[1] - (bmpHeight - bmpHeight * scale * DragView.SCREEN_SCALE) / 2);

        Point dragVisualizeOffset = new Point(0, 0);
        Rect dragRect = new Rect(0, 0, bmpWidth, bmpHeight);

        Object dragInfo = null;
        CellLayout.CellInfo cellInfo = (CellLayout.CellInfo)child.getTag();
        if (cellInfo != null && cellInfo.cell != null) {
            dragInfo = cellInfo.cell.getTag();
        }
        mDragController.startDrag(b, dragLayerX, dragLayerY, source, null,
                DragController.DRAG_ACTION_MOVE, dragVisualizeOffset, dragRect, scale);
        b.recycle();

        showScrollingIndicator(false);
    }

    public void beginDragShared(View child, DragSource source) {
        /* YUNOS BEGIN */
        // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
        if (child == null) {
            return;
        }
        /* YUNOS END */
        if (!mIsMultiSelectDragging) {
            View cahcedView = child;
            addToDragItems(cahcedView);
        } else {
            for (View view : selectedViews) {
                addToDragItems(view);
            }
        }

        Resources r = getResources();

        // The drag bitmap follows the touch point around on the screen
        final Bitmap b = createDragBitmap(child, new Canvas(), DRAG_BITMAP_PADDING);

        int bmpWidth = 0;
        int bmpHeight = 0;
        if (b != null) {
            bmpWidth = b.getWidth();
            bmpHeight = b.getHeight();
        } else {
            child.setVisibility(VISIBLE);
            mDragItems.remove(child);
            LauncherModel.removeDragInfo(child);
            return;
        }

        mLauncher.getEditModeHelper().onCellLayoutBeginDrag(this.getCurrentPage(), true);

        float scale = mLauncher.getDragLayer().getLocationInDragLayer(child, mTempXY);
        int dragLayerX =
                Math.round(mTempXY[0] - (bmpWidth - scale * child.getWidth()) / 2);
        int dragLayerY =
                Math.round(mTempXY[1] - (bmpHeight - scale * bmpHeight) / 2
                        - DRAG_BITMAP_PADDING / 2);

        Point dragVisualizeOffset = null;
        Rect dragRect = null;
        if (child instanceof BubbleTextView) {
            CellLayout cellLayout = (CellLayout) getChildAt(getCurrentPage());
            int iconHeight = cellLayout.getCellHeight();
            int iconWidth = cellLayout.getCellWidth();
            int iconPaddingTop = r.getDimensionPixelSize(R.dimen.app_icon_padding_top);
            int top = 0;
            int bottom = 0;
            top = child.getPaddingTop();
            bottom = top + iconHeight;
            dragLayerY += top;
            int left = (bmpWidth - iconWidth) / 2;
            int right = left + iconWidth;
            // Note: The drag region is used to calculate drag layer offsets, but the
            // dragVisualizeOffset in addition to the dragRect (the size) to position the outline.
            dragVisualizeOffset = new Point(-DRAG_BITMAP_PADDING / 2,
                    iconPaddingTop - DRAG_BITMAP_PADDING / 2);
            dragRect = new Rect(left, top, right, bottom);
        } else if (child instanceof FolderIcon) {
            int previewSize = r.getDimensionPixelSize(R.dimen.folder_preview_size);
            dragRect = new Rect(0, 0, child.getWidth(), previewSize);
        }

        // Clear the pressed state if necessary
        /*
         * if (child instanceof BubbleTextView) { BubbleTextView icon =
         * (BubbleTextView) child; icon.clearPressedOrFocusedBackground(); }
         */

        mDragController.startDrag(b, dragLayerX, dragLayerY, source, child.getTag(),
                DragController.DRAG_ACTION_MOVE, dragVisualizeOffset, dragRect, scale);
        b.recycle();

        // Show the scrolling indicator when you pick up an item
        showScrollingIndicator(false);

        /*String name = "";
        if (child != null && child.getTag() != null) {
            if (child.getTag() instanceof LauncherAppWidgetInfo
                    && ((LauncherAppWidgetInfo) child.getTag()).providerName != null) {
                name = ((LauncherAppWidgetInfo) child.getTag()).providerName
                        .toString();
            } else if (child.getTag() instanceof ItemInfo
                    && ((ItemInfo) child.getTag()).title != null) {
                name = ((ItemInfo) child.getTag()).title.toString();
            }
        }*/

        //UserTrackerHelper
        //        .sendUserReport(UserTrackerMessage.MSG_DRAG_ICON, name);
    }

    void addApplicationShortcut(ShortcutInfo info, CellLayout target, long container, int screen,
            int cellX, int cellY, boolean insertAtFirst, int intersectX, int intersectY) {
        View view = mLauncher.createShortcut(R.layout.application, target, (ShortcutInfo) info);

        final int[] cellXY = new int[2];
        target.findCellForSpanThatIntersects(cellXY, 1, 1, intersectX, intersectY);
        addInScreen(view, container, screen, cellXY[0], cellXY[1], 1, 1, insertAtFirst);
        LauncherModel.addOrMoveItemInDatabase(mLauncher, info, container, screen, cellXY[0],
                cellXY[1]);
    }

    public boolean transitionStateShouldAllowDrop() {
        return ((!isSwitchingState() || mTransitionProgress > 0.5f) && mState != State.SMALL);
    }

    /*YUNOS BEGIN*/
    //##module(homeshell)
    //##date:2013/12/04 ##author:jun.dongj@alibaba-inc.com##BugID:69318
    //traffic panel can't be drop
    /*
     * judge the drop target view whether can be drag/drop
     * return true if the target view can be drag/drop
     */
    private boolean acceptDragAction(CellLayout dropTargetLayout, int x, int y){
        View dragTargetView = dropTargetLayout.getChildAt(x, y);
        return acceptDragAction(dragTargetView);
    }

    private boolean acceptDragAction(View dragTargetView){
        if(dragTargetView == null){
            return true;
        }
        boolean accept = true;
        ItemInfo info = (ItemInfo)dragTargetView.getTag();
        if(info!=null && info.itemType==Favorites.ITEM_TYPE_ALIAPPWIDGET){
            //accept = false;
        }
        return accept;
    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##module(homeshell)
    //##date:2013/12/04 ##author:xiaodong.lxd##BugID:71373
    //check if the dragview from hotseat
    public boolean dragFromHotseat(Object dragInfo) {
        ItemInfo info = (ItemInfo)dragInfo;
        return info.container == Favorites.CONTAINER_HOTSEAT;
    }
    /*YUNOS END*/
    /**
     * {@inheritDoc}
     */
    public boolean acceptDrop(DragObject d) {
         // If it's an external drop (e.g. from All Apps), check if it should be accepted
         CellLayout dropTargetLayout = mDropToLayout;

         if (d.dragSource != this) {
            // Don't accept the drop if we're not over a screen at time of drop
            if (dropTargetLayout == null) {
                return false;
            }
            if (!transitionStateShouldAllowDrop()) return false;

            mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset,
                    d.dragView, mDragViewVisualCenter);

            // We want the point to be mapped to the dragTarget.
            if (mLauncher.isHotseatLayout(dropTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(dropTargetLayout, mDragViewVisualCenter, null);
            }

            int spanX = 1;
            int spanY = 1;
            if (mDragInfo != null) {
                final CellLayout.CellInfo dragCellInfo = mDragInfo;
                spanX = dragCellInfo.spanX;
                spanY = dragCellInfo.spanY;
            } else {
                final ItemInfo dragInfo = (ItemInfo) d.dragInfo;
                spanX = dragInfo.spanX;
                spanY = dragInfo.spanY;
            }

            int minSpanX = spanX;
            int minSpanY = spanY;
            if (d.dragInfo instanceof PendingAddWidgetInfo) {
                minSpanX = ((PendingAddWidgetInfo) d.dragInfo).minSpanX;
                minSpanY = ((PendingAddWidgetInfo) d.dragInfo).minSpanY;
            /*YUNOS BEGIN*/
            //##date:2014/7/8 ##author:zhangqiang.zq
            // aged mode
            } else if (d.dragInfo instanceof PendingAddGadgetInfo) {
                if (AgedModeUtil.isAgedMode()) {
                    PendingAddGadgetInfo info = ((PendingAddGadgetInfo)d.dragInfo);
                    if (minSpanX > 3) {
                       info.gadgetInfo.spanX = info.minSpanX = info.spanX = minSpanX = 3;
                       }
                    if (minSpanY > 3) {
                       info.gadgetInfo.spanY = info.minSpanY = info.spanY = minSpanY = 3;
                       }
                }
            /*YUNOS END*/
            }

            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, dropTargetLayout,
                    mTargetCell);
            float distance = dropTargetLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                    mDragViewVisualCenter[1], mTargetCell);
            if (willCreateUserFolder((ItemInfo) d.dragInfo, dropTargetLayout,
                    mTargetCell, distance, true)) {
                return true;
            }
            if (willAddToExistingUserFolder((ItemInfo) d.dragInfo, dropTargetLayout,
                    mTargetCell, distance)) {
                return true;
            }

            int[] resultSpan = new int[2];
            mTargetCell = dropTargetLayout.createArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY,
                    null, mTargetCell, resultSpan, CellLayout.MODE_ACCEPT_DROP);
            boolean foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0;

            Log.d(TAG, "sxsexe_pad -----------------------------> acceptDrop foundCell " + foundCell + " currentScreen " + getCurrentPage()
                    + " foundCell  " + foundCell);
            if (foundCell && ConfigManager.isLandOrienSupport()) {
                if (d.dragInfo instanceof PendingAddWidgetInfo || d.dragInfo instanceof PendingAddGadgetInfo) {
                    CellLayout targetCellLayout = (CellLayout) getChildAt(mCurrentPage);
                    int orientaition = LauncherApplication.isInLandOrientation()
                            ? Configuration.ORIENTATION_PORTRAIT
                            : Configuration.ORIENTATION_LANDSCAPE;
                    foundCell = targetCellLayout.checkSpaceAvailable((ItemInfo) d.dragInfo, orientaition);
                    if (!foundCell) {
                        String strToast = getResources().getString(R.string.toast_no_space_for_widget);
                        String strOrientation = null;
                        if (LauncherApplication.isInLandOrientation()) {
                            strOrientation = getResources().getString(R.string.orientation_port);
                        } else {
                            strOrientation = getResources().getString(R.string.orientation_land);
                        }
                        Toast.makeText(mLauncher, String.format(strToast, TextUtils.htmlEncode(strOrientation)), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            }

            // Don't accept the drop if there's no room for the item
            if (!foundCell) {
                Object info = d.dragInfo;
                if (info instanceof ItemInfo) {
                    /* YUNOS BEGIN */
                    // ##date:2015/1/12 ##author:zhanggong.zg ##BugID:5696871
                    // show out of space toast when drag from hide-seat and folder
                    if (((ItemInfo) info).container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                        // from hide-seat to workspace
                        mLauncher.showOutOfSpaceMessage(false);
                    } else if (((ItemInfo) info).container > 0) {
                        // from folder to workspace/dock
                        boolean toHotseat = mLauncher.isHotseatLayout(dropTargetLayout);
                        mLauncher.showOutOfSpaceMessage(toHotseat);
                    }
                    /* YUNOS END */
                }
                return false;
            }
        }
        return true;
    }

    boolean willCreateUserFolder(ItemInfo info, CellLayout target, int[] targetCell, float
            distance, boolean considerTimeout) {
        if (distance > mMaxDistanceForFolderCreation || mIsMultiSelectDragging) {
            return false;
        }
        if (info != null && mState == State.SPRING_LOADED
                && info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
            return false;
        }
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);

        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY)) {
                return false;
            }
        }

        boolean hasntMoved = false;
        if (mDragInfo != null) {
            if (!mIsMultiSelectDragging) {
                hasntMoved = dropOverView == mDragInfo.cell;
            } else {
                for (View view : selectedViews) {
                    if (dropOverView == view) {
                        hasntMoved = true;
                        break;
                    }
                }
            }

        } else {
            //when mDragInfo is null and dragSource is hideseat, hasntMoved should always be false
            hasntMoved = (info.container != LauncherSettings.Favorites.CONTAINER_HIDESEAT
                   && targetCell[0] == info.cellX && targetCell[1] == info.cellY);
        }

        if (dropOverView == null || hasntMoved || (considerTimeout && !mCreateUserFolderOnDrop)) {
            return false;
        }

        boolean aboveShortcut = (dropOverView.getTag() instanceof ShortcutInfo);
        boolean willBecomeShortcut =
                (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT||
                info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING
//remove vp install
//                || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL
                );


        return (aboveShortcut && willBecomeShortcut);
    }

    boolean willAddToExistingUserFolder(Object dragInfo, CellLayout target, int[] targetCell,
            float distance) {
        if (distance > mMaxDistanceForFolderCreation || mIsMultiSelectDragging)
            return false;
        if (dragInfo != null && mState == State.SPRING_LOADED
                && ((ItemInfo) dragInfo).itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
            return false;
        }
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);

        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY)) {
                return false;
            }
        }

        if (dropOverView instanceof FolderIcon) {
            FolderIcon fi = (FolderIcon) dropOverView;
            if (fi.acceptDrop(dragInfo)) {
                return true;
            }
        }
        return false;
    }

    boolean createUserFolderIfNecessary(View newView, long container, CellLayout target,
            int[] targetCell, float distance, boolean external, DragView dragView,
            Runnable postAnimationRunnable) {
        if (distance > mMaxDistanceForFolderCreation) {
            return false;
        }
        View v = null;
        if(target!=null){
            v = target.getChildAt(targetCell[0], targetCell[1]);
        }

        boolean hasntMoved = false;
        if (mDragInfo != null) {
            CellLayout cellParent = getParentCellLayoutForView(mDragInfo.cell);
            hasntMoved = (mDragInfo.cellX == targetCell[0] &&
                    mDragInfo.cellY == targetCell[1]) && (cellParent == target);
        }

        if (v == null || hasntMoved || !mCreateUserFolderOnDrop) {
            return false;
        }
        mCreateUserFolderOnDrop = false;
        final int screen = targetCell == null ? mDragInfo.screen : getIconScreenIndex(indexOfChild(target));

        boolean aboveShortcut = (v.getTag() instanceof ShortcutInfo);
        boolean willBecomeShortcut = (newView.getTag() instanceof ShortcutInfo);

        if (aboveShortcut && willBecomeShortcut) {
            ShortcutInfo sourceInfo = (ShortcutInfo) newView.getTag();
            ShortcutInfo destInfo = (ShortcutInfo) v.getTag();
            // if the drag started here, we need to remove it from the workspace
            if (!external) {
                getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
            }

            Rect folderLocation = new Rect();
            float scale = mLauncher.getDragLayer().getDescendantRectRelativeToSelf(v, folderLocation);
            target.removeView(v);

            FolderIcon fi =
 mLauncher.addFolder(target, container, screen, targetCell[0], targetCell[1], destInfo);
            destInfo.cellX = -1;
            destInfo.cellY = -1;
            sourceInfo.cellX = -1;
            sourceInfo.cellY = -1;

            // If the dragView is null, we can't animate
            boolean animate = dragView != null;
            // YUNOS BEGIN
            // ##date:2014/09/17 ##author:hongchao.ghc ##BugID:5239291
            if (AppGroupManager.isSwitchOn()) {
                String destPkgName = null;
                String sourcePkgName = null;
                try {
                    ComponentName desInfoName = destInfo.intent.getComponent();
                    ComponentName sourceInfoName = sourceInfo.intent.getComponent();
                    destPkgName = getPackageNameByComponentName(desInfoName, destInfo);
                    sourcePkgName = getPackageNameByComponentName(sourceInfoName, sourceInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (destPkgName != null || sourcePkgName != null) {
                    if (destPkgName != null && destPkgName.equals(sourcePkgName)
                            && destInfo.title != null && destInfo.title.equals(sourceInfo.title)
                            && (destInfo.userId > 0 || sourceInfo.userId > 0)) {
                        fi.getFolderInfo().setTitle("");
                        fi.setTitle(destInfo.title.toString());
                    } else {
                        AppGroupManager.getInstance().handleFolderNameByPkgNames(fi, mCallback,
                                destPkgName, sourcePkgName);
                    }
                }
            }
            // do not display folder background when folder is empty when creating
            fi.mDropMode = true;
            // YUNOS END
            if (animate) {
                fi.performCreateAnimation(destInfo, v, sourceInfo, dragView, folderLocation, scale,
                        postAnimationRunnable);
            } else {
                fi.addItem(destInfo);
                fi.addItem(sourceInfo);
            }
            return true;
        }
        return false;
    }

    private String getPackageNameByComponentName(ComponentName componentName, ShortcutInfo info) {
        String packageName = null;
        if (componentName != null) {
            packageName = componentName.getPackageName();
        } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) {
            packageName = info.intent
                    .getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
        } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
            packageName = info.intent.getPackage();
        }
        return packageName;
    }

    boolean addToExistingFolderIfNecessary(View newView, CellLayout target, int[] targetCell,
            float distance, DragObject d, boolean external) {
        if (distance > mMaxDistanceForFolderCreation) return false;

        Log.d(TAG, "sxsexe-------->addToExistingFolderIfNecessary mAddToExistingFolderOnDrop " + mAddToExistingFolderOnDrop);
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        if (!mAddToExistingFolderOnDrop) return false;
        mAddToExistingFolderOnDrop = false;

        if (dropOverView instanceof FolderIcon) {
            FolderIcon fi = (FolderIcon) dropOverView;
            if (fi.acceptDrop(d.dragInfo)) {
                fi.onDrop(d);

                // if the drag started here, we need to remove it from the workspace
                if (!external) {
                    CellLayout cellLayout = getParentCellLayoutForView(mDragInfo.cell);
                    if(cellLayout != null) {
                        cellLayout.removeView(mDragInfo.cell);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public void cancelMultiDrag() {
        for (View view : selectedViews) {
            Log.d(TAG, " cancelMultiDrag    " + view.getTag() + " parent " + view.getParent());
            if (view.getParent() != null) {
                if (view.getParent() instanceof DragLayer) {
                    ((DragLayer) view.getParent()).removeView(view);
                    ItemInfo info = (ItemInfo) view.getTag();
                    addInScreen(view, info.container, info.screen, info.cellX, info.cellY, info.spanX, info.spanY);
                    if (ConfigManager.isLandOrienSupport()) {
                        LauncherModel.markCellsOccupiedInNonCurrent(info, true);
                    }
                } else if (view.getParent().getParent() instanceof CellLayout) {
                    view.setVisibility(VISIBLE);
                    boolean supportCardIcon = mLauncher.getIconManager().supprtCardIcon();
                    if (!supportCardIcon) {
                        if (view instanceof BubbleTextView) {
                            BubbleTextView textView = (BubbleTextView) view;
                            textView.setDisableLabel(false);
                            textView.setIndicatorVisible(true);
                        } else if (view instanceof FolderIcon) {
                            FolderIcon folderIcon = (FolderIcon) view;
                            folderIcon.getTitleText().setDisableLabel(false);
                            folderIcon.getTitleText().setTextColor(getResources().getColor(R.color.workspace_icon_text_color));
                        }
                    }
                    ((CellLayout) view.getParent().getParent()).markCellsAsOccupiedForView(view);
                }
            } else {
                // parent is null
                ItemInfo info = (ItemInfo) view.getTag();
                addInScreen(view, info.container, info.screen, info.cellX, info.cellY, info.spanX, info.spanY);
                view.setVisibility(VISIBLE);
                if (view instanceof BubbleTextView) {
                    BubbleTextView textView = (BubbleTextView) view;
                    textView.setIndicatorVisible(true);
                } else if (view instanceof FolderIcon) {
                    FolderIcon folderIcon = (FolderIcon) view;
                    folderIcon.getTitleText().setDisableLabel(false);
                    folderIcon.getTitleText().setTextColor(getResources().getColor(R.color.workspace_icon_text_color));
                }
                if (ConfigManager.isLandOrienSupport()) {
                    LauncherModel.markCellsOccupiedInNonCurrent(info, true);
                }
            }
            mIsMultiSelectDragging = false;
        }
    }

    public void onDropMultiViews(final DragObject d, CellLayout cellLayout, int animStartDelay) {
        int targetScreen = indexOfChild(cellLayout);
        int reqCount = selectedViews.size();
        Map<String, String> params = new HashMap<String, String>();
        params.put("from", mIsFromDiffScreen ? "cross" : "same");
        params.put("mode", targetScreen == mScreenDragStart ? "down" : "cross");
        params.put("count", String.valueOf(reqCount));
        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_Entry_Menu_Arrange_Icon_Multi,
                params);
        boolean supportCardIcon = mLauncher.getIconManager().supprtCardIcon();
        for (View view : selectedViews) {
            if (!supportCardIcon) {
                if (view instanceof BubbleTextView) {
                    BubbleTextView textView = (BubbleTextView) view;
                    textView.setDisableLabel(false);
                    textView.setIndicatorVisible(true);
                } else if (view instanceof FolderIcon) {
                    FolderIcon folderIcon = (FolderIcon) view;
                    folderIcon.getTitleText().setDisableLabel(false);
                    folderIcon.getTitleText().setTextColor(getResources().getColor(R.color.workspace_icon_text_color));
                }
            }
            if (view.getParent() != null && view.getParent().getParent() instanceof CellLayout) {
                ((CellLayout) view.getParent().getParent()).removeView(view);
            }
        }
        ArrayList<ScreenPosition> posList = new ArrayList<ScreenPosition>();
        if (targetScreen == 0) {
            LauncherModel.getEmptyPosListAndOccupy(posList, 0, ConfigManager.getIconScreenMaxCount() - 1, reqCount);
        } else if (targetScreen == 1) {
            LauncherModel.getEmptyPosListAndOccupy(posList, 1, ConfigManager.getIconScreenMaxCount() - 1, reqCount);
        } else if (targetScreen == ConfigManager.getIconScreenMaxCount() - 1) {
            LauncherModel.getEmptyPosListAndOccupy(posList, targetScreen, 1, reqCount);
        } else {
            LauncherModel.getEmptyPosListAndOccupy(posList, targetScreen, ConfigManager.getIconScreenMaxCount() - 1, reqCount);
            LauncherModel.getEmptyPosListAndOccupy(posList, targetScreen - 1, 1, reqCount);
        }
        Log.d(TAG, "onDropMultiViews: posList.size = " + posList.size() + " posList " + posList);

        List<Animator> animList = new ArrayList<Animator>();
        final ArrayList<View> listViews = new ArrayList<View>(selectedViews.size());
        for (final View view : selectedViews) {
            ScreenPosition pos = null;
            listViews.add(view);
            if (!posList.isEmpty()) {
                pos = posList.remove(0);
            } else {
                Log.e(TAG, "onDropMultiViews: failed to find enough empty cells!");
                ItemInfo info = (ItemInfo) view.getTag();
                if (info instanceof ShortcutInfo) {
                    LauncherModel.addItemToNoSpaceList((ShortcutInfo) info, Favorites.CONTAINER_DESKTOP);
                }
                continue;
            }
            ItemInfo info = (ItemInfo) view.getTag();
            info.setPosition(pos);
            Log.d(Launcher.TAG_EDITMODE, "sxsexe_test     Workspace.onDropMultiViews pos " + pos + " info " + info);
            LauncherModel.addOrMoveItemInDatabase(mLauncher, info,
                    Favorites.CONTAINER_DESKTOP, info.screen, info.cellX, info.cellY);
            LayoutParams param = view.getLayoutParams();
            CellLayout.LayoutParams lp = null;
            if(param == null || !(param instanceof CellLayout.LayoutParams) ) {
                lp = new CellLayout.LayoutParams(info.cellX, info.cellY, info.spanX, info.spanY);
                lp.cellX = lp.tmpCellX = info.cellX;
                lp.cellY = lp.tmpCellY = info.cellY;
                lp.isLockedToGrid = true;
                view.setLayoutParams(lp);
            } else {
                lp = (CellLayout.LayoutParams)param;
                lp.cellX = lp.tmpCellX = info.cellX;
                lp.cellY = lp.tmpCellY = info.cellY;
                lp.cellHSpan = info.spanX;
                lp.cellVSpan = info.spanY;
                lp.isLockedToGrid = true;
            }
            addInScreen(view, Favorites.CONTAINER_DESKTOP, info.screen, info.cellX,
                    info.cellY, info.spanX, info.spanY);
            view.setVisibility(VISIBLE);
            if (info.screen == targetScreen) {
                int des[] = getMoveAnimCoordinateInSameCellLayout(view);
                int startTranX = (int) (mDragViewVisualCenter[0] - d.dragView.getDragRegion().width() / 2 - des[0]);
                int startTranY = (int) (mDragViewVisualCenter[1] - d.dragView.getDragRegion().height() / 2 - des[1]);
                view.setTranslationX(startTranX);
                view.setTranslationY(startTranY);
                ObjectAnimator moveX = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, startTranX, 0);
                ObjectAnimator moveY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startTranY, 0);
                animList.add(moveX);
                animList.add(moveY);
            }
            LauncherModel.assignPlace(info.screen, info.cellX, info.cellY,
                    info.spanX, info.spanY, true, CellLayout.Mode.NORMAL);
        }
        AnimatorSet anim = new AnimatorSet();
        anim.playTogether(animList);
        anim.setDuration(200);
        anim.setStartDelay(animStartDelay);
        anim.setInterpolator(new LinearInterpolator());
        anim.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                for (final View view : listViews) {
                    if (view.getVisibility() != VISIBLE) {
                        view.setVisibility(VISIBLE);
                    }
                }
            }
        });
        anim.start();
        for (ScreenPosition sp : posList) {
            LauncherModel.releaseWorkspacePlace(sp.s, sp.x, sp.y);
        }
    }

    public void onDrop(final DragObject d) {
        /* YUNOS BEGIN */
        // ##date:2013/12/23 ##author:yaodi.yd
        // optimize the uninstalling process
        //mLauncher.exitFullScreen();
        /* YUNOS END */
        mLauncher.getEditModeHelper().onCellLayoutBeginDrag(-1, false);
        mLauncher.getEditModeHelper().onUpdateSelectNumber(null, true);
        mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset, d.dragView,
                mDragViewVisualCenter);

        CellLayout dropTargetLayout = mDropToLayout;
        boolean toHotseat = false;
        // We want the point to be mapped to the dragTarget.
        if (dropTargetLayout != null) {
            if (mLauncher.isHotseatLayout(dropTargetLayout)) {
                toHotseat = true;
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
            } else {
                toHotseat = false;
                mapPointFromSelfToChild(dropTargetLayout, mDragViewVisualCenter, null);
            }
        }
        //Log.d(TAG, "sxsexe------------> onDrop d.dragInfo " + d.dragInfo + " toHotseat " + toHotseat);

        int snapScreen = -1;
        boolean resizeOnDrop = false;
        if (d.dragSource != this) {
            if (dropTargetLayout != null) {
                final int[] touchXY = new int[] { (int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1] };
                onDropExternal(touchXY, d.dragInfo, dropTargetLayout, false, d);
                /* YUNOS BEGIN */
                // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
                // Occupy drop target
                final ItemInfo item = (ItemInfo) d.dragInfo;
                final int screen = getIconScreenIndex(indexOfChild(dropTargetLayout));
                if (DEBUG_FIND_CELL)
                    Log.d(TAG, "find cell from out screen = " + screen + ", x=" + mTargetCell[0] + ",y= " + mTargetCell[1]);
                LauncherModel.assignPlace(screen, mTargetCell[0], mTargetCell[1], item.spanX, item.spanY, true, CellLayout.Mode.NORMAL);
            }
            /* YUNOS END */
        } else if (mDragInfo != null) {
            final View cell = mDragInfo.cell;

            Runnable resizeRunnable = null;
            if (dropTargetLayout != null) {
                if (mIsMultiSelectDragging && !toHotseat) {
                    onDropMultiViews(d, dropTargetLayout, 0);
                    mLauncher.getDragController().onDeferredEndDrag(d.dragView);
                    mLauncher.getEditModeHelper().updateEditModeTips(PreviewContentType.CellLayouts);
                    return;
                } else {
                // Move internally
                boolean hasMovedIntoHotseat = mLauncher.isHotseatLayout(dropTargetLayout);
                //modified by xiaodong.lxd
                boolean hasMovedLayouts = (getParentCellLayoutForView(cell) != dropTargetLayout) || hasMovedIntoHotseat;
                long container = hasMovedIntoHotseat ?
                        LauncherSettings.Favorites.CONTAINER_HOTSEAT :
                        LauncherSettings.Favorites.CONTAINER_DESKTOP;
                final int pageIndex = (mTargetCell[0] < 0) ? mDragInfo.screen : indexOfChild(dropTargetLayout);
                final int screen = getIconScreenIndex(pageIndex);

                int spanX = mDragInfo != null ? mDragInfo.spanX : 1;
                int spanY = mDragInfo != null ? mDragInfo.spanY : 1;
                // First we find the cell nearest to point at which the item is
                // dropped, without any consideration to whether there is an item there.

                mTargetCell = findNearestArea((int) mDragViewVisualCenter[0], (int)
                        mDragViewVisualCenter[1], spanX, spanY, dropTargetLayout, mTargetCell);
                float distance = dropTargetLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                        mDragViewVisualCenter[1], mTargetCell);

                // If the item being dropped is a shortcut and the nearest drop
                // cell also contains a shortcut, then create a folder with the two shortcuts.
                boolean createUserFolder = createUserFolderIfNecessary(cell, container,
                        dropTargetLayout, mTargetCell, distance, false, d.dragView, null);
                if (!mInScrollArea && createUserFolder) {
                    checkAndRemoveEmptyCell();
                    if(mLauncher.getHotseat().checkDragitem(cell)) {
                        mLauncher.getHotseat().onDrop(createUserFolder, d.x, null, cell, false);
                    }
                    return;
                }

                if (addToExistingFolderIfNecessary(cell, dropTargetLayout, mTargetCell,
                        distance, d, false)) {
                    checkAndRemoveEmptyCell();
                    if(mLauncher.getHotseat().checkDragitem(cell)) {
                        mLauncher.getHotseat().onDrop(true, d.x, null, cell, false);
                    }
                    return;
                }

                // Aside from the special case where we're dropping a shortcut onto a shortcut,
                // we need to find the nearest cell location that is vacant
                ItemInfo item = (ItemInfo) d.dragInfo;
                int minSpanX = item.spanX;
                int minSpanY = item.spanY;
                if (item.minSpanX > 0 && item.minSpanY > 0) {
                    minSpanX = item.minSpanX;
                    minSpanY = item.minSpanY;
                }

                /*YUNOS BEGIN*/
                //##date:2014/7/8 ##author:zhangqiang.zq
                // aged mode
                if (item instanceof GadgetItemInfo) {
                    if (AgedModeUtil.isAgedMode()) {
                        GadgetItemInfo info = ((GadgetItemInfo)item);
                        if (minSpanX > 3) {
                            info.gadgetInfo.spanX = info.minSpanX = info.spanX = minSpanX = 3;
                           }
                        if (minSpanY > 3) {
                           info.gadgetInfo.spanY = info.minSpanY = info.spanY = minSpanY = 3;
                           }
                    }
                }
                /*YUNOS END*/

                boolean foundCell = false;
                int[] resultSpan = new int[2];
                /*YUNOS BEGIN*/
                //##module(component name)
                //##date:2013/12/07 ##author:jun.dongj@alibaba-inc.com##BugID:72035
                //delete icon not disappear when drag icon to traffic panel and release it
                boolean targetaccepted = this.acceptDragAction(dropTargetLayout,mTargetCell[0] ,mTargetCell[1]);
                boolean isFull = mLauncher.getHotseat().isFull();

                if(toHotseat) {
                    if(dragFromHotseat(d.dragInfo)) {
                        foundCell = true;
                    } else {
                        foundCell = !isFull;
                    }
                } else {
                    if(targetaccepted){
                        mTargetCell = dropTargetLayout.createArea((int) mDragViewVisualCenter[0],
                                (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY, cell,
                                mTargetCell, resultSpan, CellLayout.MODE_ON_DROP);

                        foundCell = mTargetCell[0] >= 0 && mTargetCell[1] >= 0;
                    }
                }
                /*YUNOS END*/

                // if the widget resizes on drop
                if (foundCell && (cell instanceof AppWidgetHostView) &&
                        (resultSpan[0] != item.spanX || resultSpan[1] != item.spanY)) {
                    resizeOnDrop = true;
                    item.spanX = resultSpan[0];
                    item.spanY = resultSpan[1];
                    AppWidgetHostView awhv = (AppWidgetHostView) cell;
                    AppWidgetResizeFrame.updateWidgetSizeRanges(awhv, mLauncher, resultSpan[0],
                            resultSpan[1]);
                }

                if (mCurrentPage != pageIndex && !hasMovedIntoHotseat) {
                    snapScreen = pageIndex;
                    snapToPage(pageIndex);
                }
                Log.d(TAG, "sxsexe55------> workspace.onDrop foundCell " + foundCell + " hasMovedLayouts " + hasMovedLayouts
                            + " toHotseat " + toHotseat + " fromHotseat " + dragFromHotseat(d.dragInfo)
                            + " item " + item
                            + " cell.tag " + cell.getTag());
                final ItemInfo info = (ItemInfo) cell.getTag();
                final long oldcontainer = info.container;
                if (foundCell) {
                    if (hasMovedLayouts) {
                            if (mLauncher.isInLauncherEditMode()) {
                                if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                                        || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_GADGET) {
                                    Map<String, String> param = new HashMap<String, String>();
                                    param.put("mode", "cross");
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_Entry_Menu_Arrange_Widget,
                                            param);
                                } else {
                                    Map<String, String> param = new HashMap<String, String>();
                                    param.put("mode", "cross");
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_Entry_Menu_Arrange_Icon_One,
                                            param);
                                }
                            }

                        // Reparent the view
                        /*YUNOS BEGIN*/
                        //##date:2013/11/22 ##author:xiaodong.lxd
                        //add icon to hotseat
                        if(toHotseat) {
                            if(!dragFromHotseat(d.dragInfo)) {
                                if(mLauncher.getHotseat().isFull()) {
                                    mDragController.cancelDrag();
                                    checkAndRemoveEmptyCell();
                                    mLauncher.showOutOfSpaceMessage(true);
                                    return;
                                }
                                getParentCellLayoutForView(cell).removeView(cell);
                                    int index = mLauncher.getHotseat().getAppropriateIndex(d.x, d.y);
                                mLauncher.getHotseat().onDrop(true, d.x, d.dragView, cell, true);
//                                mLauncher.getHotseat().animateDropToPosition(d.x, cell, d.dragView);
                                    int cellX = mLauncher.getHotseat().getCellXFromOrder(index);
                                    int cellY = mLauncher.getHotseat().getCellYFromOrder(index);
                                    addInHotseat(cell, container, screen, cellX, cellY,
                                        info.spanX, info.spanY, index);
                                //BugID: 5324557:icon still in installing status after move to hotseat
                                Object obj = cell.getTag();
                                if (obj instanceof ShortcutInfo) {
                                    Log.d(TAG, "update hotseat container first " + container);
                                    ((ShortcutInfo)obj).container = container;
                                }
                            } else {
                                mLauncher.getHotseat().onDrop(true, d.x, d.dragView, cell, true);
                            }
                        } else {
                            if(cell.getParent() != null) {
                                getParentCellLayoutForView(cell).removeView(cell);
                            }
                            addInScreen(cell, container, screen, mTargetCell[0], mTargetCell[1],
                                    info.spanX, info.spanY);
                            if(dragFromHotseat(d.dragInfo)) {
                                mLauncher.getHotseat().onDrop(true, d.x, null, cell, false);
                            }
                            //BugID: 5324557:icon still in installing status after move to hotseat
                            Object obj = cell.getTag();
                            if (obj instanceof ShortcutInfo) {
                                Log.d(TAG, "update desktop container first " + container);
                                ((ShortcutInfo)obj).container = container;
                            }
                        }
                        /*YUNOS END*/
                        } else {
                            if (mLauncher.isInLauncherEditMode()) {
                                if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                                        || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_GADGET) {
                                    Map<String, String> param = new HashMap<String, String>();
                                    param.put("mode", "down");
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_Entry_Menu_Arrange_Widget,
                                            param);
                                } else {
                                    Map<String, String> param = new HashMap<String, String>();
                                    param.put("mode", "down");
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_Entry_Menu_Arrange_Icon_One,
                                            param);
                                }
                            }
                    }

                    // update the item's position after drop
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    lp.cellX = lp.tmpCellX = mTargetCell[0];
                    lp.cellY = lp.tmpCellY = mTargetCell[1];
                    lp.cellHSpan = item.spanX;
                    lp.cellVSpan = item.spanY;
                    lp.isLockedToGrid = true;

                    if (toHotseat == false) {
                         UserTrackerHelper.sendDragIconReport(item, oldcontainer, container, screen, lp.cellX, lp.cellY);
                    }

                    cell.setId(LauncherModel.getCellLayoutChildId(container, mDragInfo.screen,
                            mTargetCell[0], mTargetCell[1], mDragInfo.spanX, mDragInfo.spanY));

                    if (container != LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                            cell instanceof LauncherAppWidgetHostView) {
                        final CellLayout cellLayout = dropTargetLayout;
                        // We post this call so that the widget has a chance to be placed
                        // in its final location

                        final LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) cell;
                        AppWidgetProviderInfo pinfo = hostView.getAppWidgetInfo();
                        if (pinfo != null &&
                                pinfo.resizeMode != AppWidgetProviderInfo.RESIZE_NONE) {
                            final Runnable addResizeFrame = new Runnable() {
                                public void run() {
                                    DragLayer dragLayer = mLauncher.getDragLayer();
                                    dragLayer.addResizeFrame(info, hostView, cellLayout);
                                }
                            };
                            resizeRunnable = (new Runnable() {
                                public void run() {
                                    if (!isPageMoving()) {
                                        addResizeFrame.run();
                                    } else {
                                        mDelayedResizeRunnable = addResizeFrame;
                                    }
                                }
                            });
                        }
                    }
                    /* YUNOS BEGIN */
                    // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
                        int oldScreen = info.screen;
                    if (!toHotseat) {
                            LauncherModel.moveItemInDatabase(mLauncher, info, container, screen, lp.cellX, lp.cellY);
                            if (ConfigManager.isLandOrienSupport()) {
                                if (hasMovedLayouts || oldScreen != screen) {
                                    CellLayout cellLayout = (CellLayout) getChildAt(getRealScreenIndex(screen));
                                    if (info instanceof LauncherAppWidgetInfo || info instanceof GadgetItemInfo) {
                                        cellLayout.transferAllXYsOnDataChanged();
                                    } else {
                                        cellLayout.updateShortcutXYOnMove(info);
                                    }
                                }
                            }
                    }
                    /* YUNOS BEGIN */
                    // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
                    // Occupy drop target
                    if (DEBUG_FIND_CELL)
                        Log.d(TAG, "find cell screen = " + screen + ", x=" + mTargetCell[0] + ",y= " + mTargetCell[1]);
                    LauncherModel.assignPlace(screen, mTargetCell[0], mTargetCell[1], item.spanX, item.spanY, true, CellLayout.Mode.NORMAL);
                    /* YUNOS END */
                } else {
                    // If we can't find a drop location, we return the item to its original position
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    mTargetCell[0] = lp.cellX;
                    mTargetCell[1] = lp.cellY;
                    CellLayout layout = null;
                    if(cell.getParent() != null) {
                        layout = (CellLayout) cell.getParent().getParent();
                    } else if(mLauncher.getHotseat().checkDragitem(cell)){
                        layout = mLauncher.getHotseat().getCellLayout();
                    }
                    if(layout != null)
                        layout.markCellsAsOccupiedForView(cell);

                    /*YUNOS BEGIN*/
                    //##date:2013/12/07 ##author:xiaodong.lxd
                    //for bug 72087
                    if(toHotseat) {
                        if(!dragFromHotseat(d.dragInfo) && mLauncher.getHotseat().isFull()) {
                            mLauncher.showOutOfSpaceMessage(true);
                        }
                        mLauncher.getHotseat().onDrop(false, 0, d.dragView, cell, true);
                    } else {
                        mLauncher.showOutOfSpaceMessage(false);
                        if(dragFromHotseat(d.dragInfo)) {
                            addInHotseat(cell, info.container, info.screen,
                                    info.cellX, info.cellY, info.spanX, info.spanY, info.screen);
                        }
                        mLauncher.getHotseat().onDrop(false, 0, d.dragView, cell, true);
                    }
                    /*YUNOS END*/

                    /* YUNOS BEGIN */
                    // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
                    // Occupy drop target
                    if (DEBUG_FIND_CELL)
                        Log.d(TAG, "not find cell screen = " + mDragInfo.screen + ", x=" + mTargetCell[0] + ",y= " + mTargetCell[1]);
                    LauncherModel.assignPlace(mDragInfo.screen, mTargetCell[0], mTargetCell[1], item.spanX, item.spanY, true, CellLayout.Mode.NORMAL);
                    /* YUNOS END */
                }
            }
            }
            if(cell.getParent() != null && !toHotseat) {
                final CellLayout parent = (CellLayout) cell.getParent().getParent();
                final Runnable finalResizeRunnable = resizeRunnable;
                // Prepare it to be animated into its new position
                // This must be called after the view has been re-parented
                final Runnable onCompleteRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mAnimatingViewIntoPlace = false;
                        updateChildrenLayersEnabled(false);
                        if (finalResizeRunnable != null) {
                            finalResizeRunnable.run();
                        }
                    }
                };
                mAnimatingViewIntoPlace = true;
                if (d.dragView.hasDrawn()) {
                    final ItemInfo info = (ItemInfo) cell.getTag();
                    /* YUNOS BEGIN */
                    // ## date: 2016/05/06 ## author: yongxing.lyx
                    // ## BugID: 8215600: CellLayout will request focus when call animateViewIntoPosition
                    if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_GADGET) {
                        cell.setFocusable(false);
                    }
                    /* YUNOS END */
                    if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET) {
                        int animationType = resizeOnDrop ? ANIMATE_INTO_POSITION_AND_RESIZE :
                            ANIMATE_INTO_POSITION_AND_DISAPPEAR;
                        animateWidgetDrop(info, parent, d.dragView,
                                onCompleteRunnable, animationType, cell, false);
                    } else {
                        int duration = snapScreen < 0 ? -1 : ADJACENT_SCREEN_DROP_DURATION;
                        mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, cell, duration,
                                onCompleteRunnable, this);
                    }
                } else {
                    d.deferDragViewCleanupPostAnimation = false;
                    cell.setVisibility(VISIBLE);
                }
                parent.onDropChild(cell);

                Log.d(TAG, "sxsexe-------------------------->checkAndRemoveEmptyCell--------------------");
                checkAndRemoveEmptyCell();
                /* YUNOS BEGIN */
                //##date:2015/05/22  ##author: chenjian.chenjian ##BugId: 6006298
                boolean hasMovedLayouts = (getParentCellLayoutForView(cell) != dropTargetLayout) ;
                if( !hasMovedLayouts){
                    mLauncher.getEditModeHelper().onCellLayoutDataChanged(parent, cell);
                }
                /* YUNOS END */
            }
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: dragging screen
        } else if (d.dragInfo == null) {
            mLauncher.getEditModeHelper().updateEditModeTips(PreviewContentType.CellLayouts);
            CellLayout current = (CellLayout)getChildAt(getCurrentPage());
            if (d.dragView.hasDrawn()) {
                if (mDragController.isDraggingScreen()) {
                    if (mDragController.isDragScreenWaiting()) {
                        mDragController.clearDragScreenRunnable();
                        mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, current,
                                ADJACENT_SCREEN_DROP_DURATION, null, this);
                    } else {
                        mDragScreenDelayed = true;
                        mDragScreen = d.dragView;
                    }
                } else {
                    mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, current,
                            ADJACENT_SCREEN_DROP_DURATION, null, this);
                }
            } else {
                d.deferDragViewCleanupPostAnimation = false;
                current.setVisibility(VISIBLE);
            }
        /* YUNOS END */
        } else {
            Log.e(TAG, "sxsexe-------->Error mDraginfo is null");
        }
    }

    public void setFinalScrollForPageChange(int screen) {
        CellLayout cl = (CellLayout) getChildAt(screen);
        if (cl != null) {
            mSavedScrollX = getScrollX();
            mSavedTranslationX = cl.getTranslationX();
            mSavedRotationY = cl.getRotationY();
            final int newX = getChildOffset(screen) - getRelativeChildOffset(screen);
            setScrollX(newX);
            cl.setTranslationX(0f);
            cl.setRotationY(0f);
        }
    }

    public void resetFinalScrollForPageChange(int screen) {
        CellLayout cl = (CellLayout) getChildAt(screen);
        if (cl != null) {
            setScrollX(mSavedScrollX);
            cl.setTranslationX(mSavedTranslationX);
            cl.setRotationY(mSavedRotationY);
        }
    }

    public void getViewLocationRelativeToSelf(View v, int[] location) {
        getLocationInWindow(location);
        int x = location[0];
        int y = location[1];

        v.getLocationInWindow(location);
        int vX = location[0];
        int vY = location[1];

        location[0] = vX - x;
        location[1] = vY - y;
    }

    public void onDragEnter(DragObject d) {
        mDragEnforcer.onDragEnter();
        mCreateUserFolderOnDrop = false;
        mAddToExistingFolderOnDrop = false;

        mDropToLayout = null;
        CellLayout layout = getCurrentDropLayout();
        setCurrentDropLayout(layout);
        setCurrentDragOverlappingLayout(layout);

        // Because we don't have space in the Phone UI (the CellLayouts run to the edge) we
        // don't need to show the outlines
        /*
         * Commented by xiaodong.lxd YUNOS if
         * (LauncherApplication.isScreenLarge()) { showOutlines(); }
         */

        /* YUNOS BEGIN */
        // ##date:2013/12/04 ##author:xiaodong.lxd
        // add empty screen when start drag
        if (!mLauncher.isInLauncherEditMode() && !mEmptyScreenAdded) {
            addEmptyScreen();
        }
        /* YUNOS END */

        /* YUNOS BEGIN */
        // ##date:2015-1-22 ##author:zhanggong.zg ##BugID:5645766
        // Folder optimization:
        // Make a delay when drag icon from folder to dock.
        if (d.dragSource instanceof Folder) {
            // If the source is folder, temporarily disable the flag
            mDragFromFolderToHotseatEnable = false;
            // recover the flag after a delay
            mDragFromFolderToHotseatAlarm.setAlarm(sDragFromFolderToHotseatDelay);
        } else {
            mDragFromFolderToHotseatEnable = true;
        }
        /* YUNOS END */

     // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(163418) ##date:2014/08/15
        // ##description: Added support for widget page
        if(FeatureUtility.hasFullScreenWidget() && !mLauncher.isHideseatShowing()) {
            removeWidgetPages();
        }
        // YUNOS END PB
    }

    static Rect getCellLayoutMetrics(Launcher launcher, int orientation) {
        Resources res = launcher.getResources();
        Display display = launcher.getWindowManager().getDefaultDisplay();
        Point smallestSize = new Point();
        Point largestSize = new Point();
        display.getCurrentSizeRange(smallestSize, largestSize);
        if (orientation == CellLayout.LANDSCAPE) {
            if (mLandscapeCellLayoutMetrics == null) {
                int paddingLeft = res.getDimensionPixelSize(R.dimen.workspace_left_padding_land);
                int paddingRight = res.getDimensionPixelSize(R.dimen.workspace_right_padding_land);
                int paddingTop = res.getDimensionPixelSize(R.dimen.workspace_top_padding_land);
                int paddingBottom = res.getDimensionPixelSize(R.dimen.workspace_bottom_padding_land);
                int width = largestSize.x - paddingLeft - paddingRight;
                int height = smallestSize.y - paddingTop - paddingBottom;
                mLandscapeCellLayoutMetrics = new Rect();
                CellLayout.getMetrics(mLandscapeCellLayoutMetrics, res,
                        width, height, LauncherModel.getCellCountX(), LauncherModel.getCellCountY(),
                        orientation);
            }
            return mLandscapeCellLayoutMetrics;
        } else if (orientation == CellLayout.PORTRAIT) {
            if (mPortraitCellLayoutMetrics == null) {
                int paddingLeft = res.getDimensionPixelSize(R.dimen.workspace_left_padding_land);
                int paddingRight = res.getDimensionPixelSize(R.dimen.workspace_right_padding_land);
                int paddingTop = res.getDimensionPixelSize(R.dimen.workspace_top_padding_land);
                int paddingBottom = res.getDimensionPixelSize(R.dimen.workspace_bottom_padding_land);
                int width = smallestSize.x - paddingLeft - paddingRight;
                int height = largestSize.y - paddingTop - paddingBottom;
                mPortraitCellLayoutMetrics = new Rect();
                CellLayout.getMetrics(mPortraitCellLayoutMetrics, res,
                        width, height, LauncherModel.getCellCountX(), LauncherModel.getCellCountY(),
                        orientation);
            }
            return mPortraitCellLayoutMetrics;
        }
        return null;
    }

    boolean mDragShortcutInSpringLoadMode = false;
    public void onDragExit(DragObject d) {
        mDragEnforcer.onDragExit();

        // Here we store the final page that will be dropped to, if the workspace in fact
        // receives the drop
        if (mInScrollArea) {
            if (isPageMoving()) {
                // If the user drops while the page is scrolling, we should use that page as the
                // destination instead of the page that is being hovered over.
                mDropToLayout = (CellLayout) getPageAt(getNextPage());
            } else {
                if (!d.dragComplete) {
                    mDropToLayout = mDragOverlappingLayout;
                } else {
                    mDropToLayout = (CellLayout) getPageAt(getCurrentPage());
                }
                Log.d(TAG, "onDragExit dropLayout index : " + indexOfChild(mDropToLayout) +
                           " dragComplete : " + d.dragComplete);
            }
        } else {
            mDropToLayout = mDragTargetLayout;
        }

        if (mDragMode == DRAG_MODE_CREATE_FOLDER) {
            mCreateUserFolderOnDrop = true;
        } else if (mDragMode == DRAG_MODE_ADD_TO_FOLDER) {
            mAddToExistingFolderOnDrop = true;
        }

        /* YUNOS BEGIN */
        // 6529072 xiaodong.lxd drag pendingShortcutInfo and dragmode is in DRAG_MODE_REORDER, set temp variable true
        if (d.dragSource != this && d.dragInfo instanceof PendingAddShortcutInfo && mDragMode == DRAG_MODE_REORDER) {
            mDragShortcutInSpringLoadMode = true;
        }
        /* YUNOS END */

        // Reset the scroll area and previous drag target
        onResetScrollArea();
        setCurrentDropLayout(null);
        setCurrentDragOverlappingLayout(null);

        mSpringLoadedDragController.cancel();

        if (!mIsPageMoving) {
            hideOutlines();
        }

        /* YUNOS BEGIN */
        // ##date:2015-1-22 ##author:zhanggong.zg ##BugID:5645766
        mDragFromFolderToHotseatAlarm.cancelAlarm();
        mDragFromFolderToHotseatEnable = true;
        /* YUNOS END */

        /* YUNOS BEGIN PB*/
        // ##module:HomeShell ##author:jinjiang.wjj
        // ##BugID:5627052 ##date:2014/12/09
        // ##description:in editmode onDragExit, we don't need to add widget pages.
        if(FeatureUtility.hasFullScreenWidget() && !mLauncher.isHideseatShowing()) {
            makeSureWidgetPages();
        }
        // YUNOS END PB
    }

    void setCurrentDropLayout(CellLayout layout) {
        if (mDragTargetLayout != null) {
            mDragTargetLayout.revertTempState();
            mDragTargetLayout.onDragExit();
        }
        mDragTargetLayout = layout;
        if (mDragTargetLayout != null) {
            mDragTargetLayout.onDragEnter();
        }
        cleanupReorder(true);
        cleanupFolderCreation();
        setCurrentDropOverCell(-1, -1);
    }

    void setCurrentDragOverlappingLayout(CellLayout layout) {
        if (mDragOverlappingLayout != null) {
            mDragOverlappingLayout.setIsDragOverlapping(false);
        }
        mDragOverlappingLayout = layout;
        if (mDragOverlappingLayout != null) {
            mDragOverlappingLayout.setIsDragOverlapping(true);
        }
        invalidate();
    }

    void setCurrentDropOverCell(int x, int y) {
        if (x != mDragOverX || y != mDragOverY) {
            mDragOverX = x;
            mDragOverY = y;
            setDragMode(DRAG_MODE_NONE);
        }
    }

    void setDragMode(int dragMode) {
        if (dragMode != mDragMode) {
            if (dragMode == DRAG_MODE_NONE) {
                cleanupAddToFolder();
                // We don't want to cancel the re-order alarm every time the target cell changes
                // as this feels to slow / unresponsive.
                cleanupReorder(false);
                cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_ADD_TO_FOLDER) {
                cleanupReorder(true);
                cleanupFolderCreation();
            } else if (dragMode == DRAG_MODE_CREATE_FOLDER) {
                cleanupAddToFolder();
                cleanupReorder(true);
            } else if (dragMode == DRAG_MODE_REORDER) {
                cleanupAddToFolder();
                cleanupFolderCreation();
            }
            mDragMode = dragMode;
        }
    }

    private void cleanupFolderCreation() {
        if (mDragFolderRingAnimator != null) {
            mDragFolderRingAnimator.animateToNaturalState();
        }
        mFolderCreationAlarm.cancelAlarm();
    }

    private void cleanupAddToFolder() {
        if (mDragOverFolderIcon != null) {
            mDragOverFolderIcon.onDragExit(null);
            mDragOverFolderIcon = null;
        }
    }

    private void cleanupReorder(boolean cancelAlarm) {
        // Any pending reorders are canceled
        if (cancelAlarm) {
            mReorderAlarm.cancelAlarm();
        }
        mLastReorderX = -1;
        mLastReorderY = -1;
    }

    public DropTarget getDropTargetDelegate(DragObject d) {
        return null;
    }

    /*
    *
    * Convert the 2D coordinate xy from the parent View's coordinate space to this CellLayout's
    * coordinate space. The argument xy is modified with the return result.
    *
    */
   void mapPointFromSelfToChild(View v, float[] xy) {
       mapPointFromSelfToChild(v, xy, null);
   }

   /*
    *
    * Convert the 2D coordinate xy from the parent View's coordinate space to this CellLayout's
    * coordinate space. The argument xy is modified with the return result.
    *
    * if cachedInverseMatrix is not null, this method will just use that matrix instead of
    * computing it itself; we use this to avoid redundant matrix inversions in
    * findMatchingPageForDragOver
    *
    */
   void mapPointFromSelfToChild(View v, float[] xy, Matrix cachedInverseMatrix) {
       if (cachedInverseMatrix == null) {
           v.getMatrix().invert(mTempInverseMatrix);
           cachedInverseMatrix = mTempInverseMatrix;
       }
       int scrollX = getScrollX();
       if (mNextPage != INVALID_PAGE) {
           scrollX = mScroller.getFinalX();
       }
       xy[0] = xy[0] + scrollX - v.getLeft();
       xy[1] = xy[1] + getScrollY() - v.getTop();
       cachedInverseMatrix.mapPoints(xy);
   }


   void mapPointFromSelfToHotseatLayout(Hotseat hotseat, float[] xy) {
       hotseat.getLayout().getMatrix().invert(mTempInverseMatrix);
       xy[0] = xy[0] - hotseat.getLeft() - hotseat.getLayout().getLeft();
       xy[1] = xy[1] - hotseat.getTop() - hotseat.getLayout().getTop();
       mTempInverseMatrix.mapPoints(xy);
   }

   /*
    *
    * Convert the 2D coordinate xy from this CellLayout's coordinate space to
    * the parent View's coordinate space. The argument xy is modified with the return result.
    *
    */
   void mapPointFromChildToSelf(View v, float[] xy) {
       v.getMatrix().mapPoints(xy);
       int scrollX = getScrollX();
       if (mNextPage != INVALID_PAGE) {
           scrollX = mScroller.getFinalX();
       }
       xy[0] -= (scrollX - v.getLeft());
       xy[1] -= (getScrollY() - v.getTop());
   }

   static private float squaredDistance(float[] point1, float[] point2) {
        float distanceX = point1[0] - point2[0];
        float distanceY = point2[1] - point2[1];
        return distanceX * distanceX + distanceY * distanceY;
   }

    /*
     *
     * Returns true if the passed CellLayout cl overlaps with dragView
     *
     */
    boolean overlaps(CellLayout cl, DragView dragView,
            int dragViewX, int dragViewY, Matrix cachedInverseMatrix) {
        // Transform the coordinates of the item being dragged to the CellLayout's coordinates
        final float[] draggedItemTopLeft = mTempDragCoordinates;
        draggedItemTopLeft[0] = dragViewX;
        draggedItemTopLeft[1] = dragViewY;
        final float[] draggedItemBottomRight = mTempDragBottomRightCoordinates;
        draggedItemBottomRight[0] = draggedItemTopLeft[0] + dragView.getDragRegionWidth();
        draggedItemBottomRight[1] = draggedItemTopLeft[1] + dragView.getDragRegionHeight();

        // Transform the dragged item's top left coordinates
        // to the CellLayout's local coordinates
        mapPointFromSelfToChild(cl, draggedItemTopLeft, cachedInverseMatrix);
        float overlapRegionLeft = Math.max(0f, draggedItemTopLeft[0]);
        float overlapRegionTop = Math.max(0f, draggedItemTopLeft[1]);

        if (overlapRegionLeft <= cl.getWidth() && overlapRegionTop >= 0) {
            // Transform the dragged item's bottom right coordinates
            // to the CellLayout's local coordinates
            mapPointFromSelfToChild(cl, draggedItemBottomRight, cachedInverseMatrix);
            float overlapRegionRight = Math.min(cl.getWidth(), draggedItemBottomRight[0]);
            float overlapRegionBottom = Math.min(cl.getHeight(), draggedItemBottomRight[1]);

            if (overlapRegionRight >= 0 && overlapRegionBottom <= cl.getHeight()) {
                float overlap = (overlapRegionRight - overlapRegionLeft) *
                         (overlapRegionBottom - overlapRegionTop);
                if (overlap > 0) {
                    return true;
                }
             }
        }
        return false;
    }

    /*
     *
     * This method returns the CellLayout that is currently being dragged to. In order to drag
     * to a CellLayout, either the touch point must be directly over the CellLayout, or as a second
     * strategy, we see if the dragView is overlapping any CellLayout and choose the closest one
     *
     * Return null if no CellLayout is currently being dragged over
     *
     */
    private CellLayout findMatchingPageForDragOver(
            DragView dragView, float originX, float originY, boolean exact) {
        // We loop through all the screens (ie CellLayouts) and see which ones overlap
        // with the item being dragged and then choose the one that's closest to the touch point
        final int screenCount = getIconScreenCount();
        int offSet = getIconScreenHomeIndex();
        CellLayout bestMatchingScreen = null;
        float smallestDistSoFar = Float.MAX_VALUE;

        for (int i = offSet; i < screenCount + offSet; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);

            final float[] touchXy = {originX, originY};
            // Transform the touch coordinates to the CellLayout's local coordinates
            // If the touch point is within the bounds of the cell layout, we can return immediately
            cl.getMatrix().invert(mTempInverseMatrix);
            mapPointFromSelfToChild(cl, touchXy, mTempInverseMatrix);

            if (touchXy[0] >= 0 && touchXy[0] <= cl.getWidth() &&
                    touchXy[1] >= 0 && touchXy[1] <= cl.getHeight()) {
                return cl;
            }

            if (!exact) {
                // Get the center of the cell layout in screen coordinates
                final float[] cellLayoutCenter = mTempCellLayoutCenterCoordinates;
                cellLayoutCenter[0] = cl.getWidth()/2;
                cellLayoutCenter[1] = cl.getHeight()/2;
                mapPointFromChildToSelf(cl, cellLayoutCenter);

                touchXy[0] = originX;
                touchXy[1] = originY;

                // Calculate the distance between the center of the CellLayout
                // and the touch point
                float dist = squaredDistance(touchXy, cellLayoutCenter);

                if (dist < smallestDistSoFar) {
                    smallestDistSoFar = dist;
                    bestMatchingScreen = cl;
                }
            }
        }
        return bestMatchingScreen;
    }

    // This is used to compute the visual center of the dragView. This point is then
    // used to visualize drop locations and determine where to drop an item. The idea is that
    // the visual center represents the user's interpretation of where the item is, and hence
    // is the appropriate point to use when determining drop location.
    private float[] getDragViewVisualCenter(int x, int y, int xOffset, int yOffset,
            DragView dragView, float[] recycle) {
        float res[];
        if (recycle == null) {
            res = new float[2];
        } else {
            res = recycle;
        }

        // First off, the drag view has been shifted in a way that is not represented in the
        // x and y values or the x/yOffsets. Here we account for that shift.
        x += getResources().getDimensionPixelSize(R.dimen.dragViewOffsetX);
        y += getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY);

        // These represent the visual top and left of drag view if a dragRect was provided.
        // If a dragRect was not provided, then they correspond to the actual view left and
        // top, as the dragRect is in that case taken to be the entire dragView.
        // R.dimen.dragViewOffsetY.
        int left = x - xOffset;
        int top = y - yOffset;

        // In order to find the visual center, we shift by half the dragRect
        res[0] = left + dragView.getDragRegion().width() / 2;
        res[1] = top + dragView.getDragRegion().height() / 2;
        return res;
    }

    private boolean isDragWidget(DragObject d) {
        return (d.dragInfo instanceof LauncherAppWidgetInfo ||
                d.dragInfo instanceof PendingAddWidgetInfo);
    }

    /* YUNOS BEGIN */
    // ##date:2014/1/9 ##author:zhangqiang.zq
    // gadget (forbid gadget dragging to hotseat)
    private boolean isDragGadget(DragObject d) {
        return d.dragInfo instanceof GadgetItemInfo
                || d.dragInfo instanceof PendingAddGadgetInfo;
    }
    /* YUNOS END */

    private boolean isExternalDragWidget(DragObject d) {
        return d.dragSource != this && isDragWidget(d);
    }

    public void onDragOver(DragObject d) {
        // Skip drag over events while we are dragging over side pages
        if (mInScrollArea || mIsSwitchingState || mState == State.SMALL) return;

        Rect r = new Rect();
        CellLayout layout = null;
        ItemInfo item = (ItemInfo) d.dragInfo;
        if (item == null) return;

        // Ensure that we have proper spans for the item that we are dropping
        if (item.spanX < 0 || item.spanY < 0) throw new RuntimeException("Improper spans found");
        mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset,
            d.dragView, mDragViewVisualCenter);

        final View child = (mDragInfo == null) ? null : mDragInfo.cell;
        // Identify whether we have dragged over a side page
        if (isSmall()) {
            if (mLauncher.getHotseat() != null
                && mLauncher.getHotseat().getVisibility() == View.VISIBLE
                && !isExternalDragWidget(d)
                && mDragFromFolderToHotseatEnable) {
                mLauncher.getHotseat().getHitRect(r);
                if (r.contains(d.x, d.y)) {
                    layout = mLauncher.getHotseat().getLayout();
                }
            }
            if (layout == null) {
                layout = findMatchingPageForDragOver(d.dragView, d.x, d.y, false);
            }
            if (layout != mDragTargetLayout) {

                setCurrentDropLayout(layout);
                setCurrentDragOverlappingLayout(layout);

                boolean isInSpringLoadedMode = (mState == State.SPRING_LOADED);
                if (isInSpringLoadedMode) {
                    int index = indexOfChild(layout);
                    if (mLauncher.isHotseatLayout(layout) || index < getIconScreenHomeIndex()) {
                        mSpringLoadedDragController.cancel();
                    } else {
                        mSpringLoadedDragController.setAlarm(mDragTargetLayout);
                    }
                }
            }
        } else {
            // Test to see if we are over the hotseat otherwise just use the current page
            if (mLauncher.getHotseat() != null
                    && mLauncher.getHotseat().getVisibility() == View.VISIBLE
                    && !isDragWidget(d) && !isDragGadget(d)
                    && mDragFromFolderToHotseatEnable) {
                mLauncher.getHotseat().getHitRect(r);
                if (r.contains(d.x, d.y)) {
                    layout = mLauncher.getHotseat().getLayout();
                }
            }
            if (layout == null) {
                layout = getCurrentDropLayout();
            }
            if (layout != mDragTargetLayout) {
                setCurrentDropLayout(layout);
                setCurrentDragOverlappingLayout(layout);
            }
        }

        /*YUNOS BEGIN added by xiaodong.lxd for push to talk*/
        if(CheckVoiceCommandPressHelper.isPushTalkCanUse()) {
            CheckVoiceCommandPressHelper.getInstance().checkDragRegion(d, true, mDragViewVisualCenter);
        }
        /*YUNOS END*/
        //final boolean supportCard = mLauncher.getIconManager().supprtCardIcon();

        // Handle the drag over
        if (mDragTargetLayout != null) {
            // We want the point to be mapped to the dragTarget.
            if (mLauncher.isHotseatLayout(mDragTargetLayout)) {
                mapPointFromSelfToHotseatLayout(mLauncher.getHotseat(), mDragViewVisualCenter);
                mLauncher.getHotseat().onEnterHotseat(d.x, d.y, item.screen, dragFromHotseat(d.dragInfo), d);
            } else {
                mapPointFromSelfToChild(mDragTargetLayout, mDragViewVisualCenter, null);
                /* YUNOS BEGIN */
                // ##date:2014/07/30 ##author:hongchao.ghc ##BugID:137835
                boolean isDragFromHotseat = dragFromHotseat(d.dragInfo);
                if (isDragFromHotseat && (mDragInfo != null)) {
                    View view = mDragInfo.cell;
                    if (view instanceof BubbleTextView) {
                        ((BubbleTextView) view).resetTempPadding();
                    } else if (view instanceof FolderIcon) {
                        FolderIcon fi = (FolderIcon) view;
                        fi.resetTempPadding();
                    }
                }
                mLauncher.getHotseat().onExitHotseat(isDragFromHotseat);
                /* YUNOS END */
            }

            ItemInfo info = (ItemInfo) d.dragInfo;

            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], item.spanX, item.spanY,
                    mDragTargetLayout, mTargetCell);

            setCurrentDropOverCell(mTargetCell[0], mTargetCell[1]);

            float targetCellDistance = mDragTargetLayout.getDistanceFromCell(
                    mDragViewVisualCenter[0], mDragViewVisualCenter[1], mTargetCell);
            /* YUNOS BEGIN */
            // ##date:2014/09/24 ##author:xindong.zxd ##BugId:5239349
            //adjustment the effect of the merger folder
            // divide distance to make hover big icon easily
            //if (supportCard)
            //    targetCellDistance /= 1.3f;
            /* YUNOS END */

            final View dragOverView = mDragTargetLayout.getChildAt(mTargetCell[0],
                    mTargetCell[1]);

            /*YUNOS BEGIN*/
            //##module(homeshell)
            //##date:2013/12/04 ##author:jun.dongj@alibaba-inc.com##BugID:69318
            //traffic panel can't be drop
            if(!acceptDragAction(dragOverView)){
                if(mReorderAlarm!=null){
                    mReorderAlarm.setOnAlarmListener(null);
                }
                return;
            }
            /*YUNOS END*/

            manageFolderFeedback(info, mDragTargetLayout, mTargetCell,
                    targetCellDistance, dragOverView);

            int minSpanX = item.spanX;
            int minSpanY = item.spanY;
            if (item.minSpanX > 0 && item.minSpanY > 0) {
                minSpanX = item.minSpanX;
                minSpanY = item.minSpanY;
            }

            boolean nearestDropOccupied = mDragTargetLayout.isNearestDropLocationOccupied((int)
                    mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], item.spanX,
                    item.spanY, child, mTargetCell);
            boolean isHotseatLayout = mLauncher.isHotseatLayout(mDragTargetLayout);

            if(isHotseatLayout) {
                if(dragFromHotseat(d.dragInfo)) {
                    int [] topLeft = new int[2];
                    mLauncher.getHotseat().touchToPoint(d.x, d.y, topLeft, true, true);
                    mTargetCell[0] = topLeft[0];
                } else {
                    int [] topLeft = new int[2];
                    mLauncher.getHotseat().touchToPoint(d.x, d.y, topLeft, false, false);
                    mTargetCell[0] = topLeft[0];
                }
            }
            if (!nearestDropOccupied || isHotseatLayout) {
                mDragTargetLayout.visualizeDropLocation(child, mDragOutline,
                        (int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1],
                        mTargetCell[0], mTargetCell[1], item.spanX, item.spanY, false,
                        d.dragView.getDragVisualizeOffset(), d.dragView.getDragRegion(),
 mLauncher.isHotseatLayout(mDragTargetLayout),
                        dragFromHotseat(d.dragInfo), d.x, d.y);
            } else if ((mDragMode == DRAG_MODE_NONE || mDragMode == DRAG_MODE_REORDER)
                    && !mReorderAlarm.alarmPending()
                    && (mLastReorderX != mTargetCell[0] || mLastReorderY != mTargetCell[1])
                    && !mLauncher.isHotseatLayout(mDragTargetLayout) && !mIsMultiSelectDragging) {
                // Otherwise, if we aren't adding to or creating a folder and there's no pending
                // reorder, then we schedule a reorder
                ReorderAlarmListener listener = new ReorderAlarmListener(mDragViewVisualCenter,
                        minSpanX, minSpanY, item.spanX, item.spanY, d.dragView, child, d);
                mReorderAlarm.setOnAlarmListener(listener);
                mReorderAlarm.setAlarm(REORDER_TIMEOUT);
            }

            if (mDragMode == DRAG_MODE_CREATE_FOLDER || mDragMode == DRAG_MODE_ADD_TO_FOLDER ||
                    !nearestDropOccupied) {
                if (mDragTargetLayout != null) {
                    mDragTargetLayout.revertTempState();
                }
            }
        }
    }

    private void manageFolderFeedback(ItemInfo info, CellLayout targetLayout,
            int[] targetCell, float distance, View dragOverView) {
        boolean userFolderPending = willCreateUserFolder(info, targetLayout, targetCell, distance,
                false);

        //xiaodong.lxd added
        if(mLauncher.isHotseatLayout(targetLayout)) {
            userFolderPending = false;
        }
        if (mDragMode == DRAG_MODE_NONE && userFolderPending &&
                !mFolderCreationAlarm.alarmPending()) {
            mFolderCreationAlarm.setOnAlarmListener(new
                    FolderCreationAlarmListener(targetLayout, targetCell[0], targetCell[1]));
            mFolderCreationAlarm.setAlarm(FOLDER_CREATION_TIMEOUT);
            return;
        }

        boolean willAddToFolder =
                willAddToExistingUserFolder(info, targetLayout, targetCell, distance);
        //xiaodong.lxd added
        if(mLauncher.isHotseatLayout(targetLayout)) {
            willAddToFolder = false;
        }
        if (willAddToFolder && mDragMode == DRAG_MODE_NONE) {
            mDragOverFolderIcon = ((FolderIcon) dragOverView);
            mDragOverFolderIcon.onDragEnter(info);
            if (targetLayout != null) {
                targetLayout.clearDragOutlines();
            }
            setDragMode(DRAG_MODE_ADD_TO_FOLDER);
            return;
        }

        if (mDragMode == DRAG_MODE_ADD_TO_FOLDER && !willAddToFolder) {
            setDragMode(DRAG_MODE_NONE);
        }
        if (mDragMode == DRAG_MODE_CREATE_FOLDER && !userFolderPending) {
            setDragMode(DRAG_MODE_NONE);
        }

        return;
    }

    class FolderCreationAlarmListener implements OnAlarmListener {
        CellLayout layout;
        int cellX;
        int cellY;

        public FolderCreationAlarmListener(CellLayout layout, int cellX, int cellY) {
            this.layout = layout;
            this.cellX = cellX;
            this.cellY = cellY;
        }

        public void onAlarm(Alarm alarm) {
            if (mDragFolderRingAnimator == null) {
                mDragFolderRingAnimator = new FolderRingAnimator(mLauncher, null);
            }
            mDragFolderRingAnimator.setCell(cellX, cellY);
            mDragFolderRingAnimator.setCellLayout(layout);
            mDragFolderRingAnimator.animateToAcceptState();
            layout.showFolderAccept(mDragFolderRingAnimator);
            layout.clearDragOutlines();
            setDragMode(DRAG_MODE_CREATE_FOLDER);
        }
    }

    class ReorderAlarmListener implements OnAlarmListener {
        float[] dragViewCenter;
        int minSpanX, minSpanY, spanX, spanY;
        DragView dragView;
        View child;
        DragObject dragObject;

        public ReorderAlarmListener(float[] dragViewCenter, int minSpanX, int minSpanY, int spanX,
                int spanY, DragView dragView, View child, DragObject dragObject) {
            this.dragViewCenter = dragViewCenter;
            this.minSpanX = minSpanX;
            this.minSpanY = minSpanY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.child = child;
            this.dragView = dragView;
            this.dragObject = dragObject;
        }

        public void onAlarm(Alarm alarm) {
            int[] resultSpan = new int[2];
            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], spanX, spanY, mDragTargetLayout, mTargetCell);
            mLastReorderX = mTargetCell[0];
            mLastReorderY = mTargetCell[1];

            mTargetCell = mDragTargetLayout.createArea((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY,
                child, mTargetCell, resultSpan, CellLayout.MODE_DRAG_OVER);

            if (mTargetCell[0] < 0 || mTargetCell[1] < 0) {
                mDragTargetLayout.revertTempState();
            } else {
                setDragMode(DRAG_MODE_REORDER);
            }

            boolean resize = resultSpan[0] != spanX || resultSpan[1] != spanY;
            mDragTargetLayout.visualizeDropLocation(child, mDragOutline,
                (int) mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1],
                mTargetCell[0], mTargetCell[1], resultSpan[0], resultSpan[1], resize,
                dragView.getDragVisualizeOffset(), dragView.getDragRegion(),
 mLauncher.isHotseatLayout(mDragTargetLayout), dragFromHotseat(dragObject.dragInfo),
                    dragObject.x, dragObject.y);
        }
    }

    @Override
    public void getHitRect(Rect outRect) {
        // We want the workspace to have the whole area of the display (it will find the correct
        // cell layout to drop to in the existing drag/drop logic.
        outRect.set(0, 0, mDisplaySize.x, mDisplaySize.y);
    }

    /**
     * Add the item specified by dragInfo to the given layout.
     * @return true if successful
     */
    public boolean addExternalItemToScreen(ItemInfo dragInfo, CellLayout layout) {
        if (layout.findCellForSpan(mTempEstimate, dragInfo.spanX, dragInfo.spanY)) {
            onDropExternal(dragInfo.dropPos, (ItemInfo) dragInfo, (CellLayout) layout, false);
            return true;
        }
        mLauncher.showOutOfSpaceMessage(mLauncher.isHotseatLayout(layout));
        return false;
    }

    private void onDropExternal(int[] touchXY, Object dragInfo,
            CellLayout cellLayout, boolean insertAtFirst) {
        onDropExternal(touchXY, dragInfo, cellLayout, insertAtFirst, null);
    }

    /**
     * Drop an item that didn't originate on one of the workspace screens.
     * It may have come from Launcher (e.g. from all apps or customize), or it may have
     * come from another app altogether.
     *
     * NOTE: This can also be called when we are outside of a drag event, when we want
     * to add an item to one of the workspace screens.
     */
    private void onDropExternal(final int[] touchXY, final Object dragInfo,
            final CellLayout cellLayout, boolean insertAtFirst, DragObject d) {

        ItemInfo info = (ItemInfo) dragInfo;
        int spanX = info.spanX;
        int spanY = info.spanY;
        if (mDragInfo != null) {
            spanX = mDragInfo.spanX;
            spanY = mDragInfo.spanY;
        }

        final long container = mLauncher.isHotseatLayout(cellLayout) ?
                LauncherSettings.Favorites.CONTAINER_HOTSEAT :
                    LauncherSettings.Favorites.CONTAINER_DESKTOP;
        /*YUNOS BEGIN*/
        //##module(homeshell)
        //##date:2013/12/06 ##author:xiaodong.lxd##BugID:71742
        //check if the dragview from hotseat
        if(mLauncher.isHotseatLayout(cellLayout)){
            if(mLauncher.getHotseat().isFull() && !dragFromHotseat(d.dragInfo)) {
                mDragController.cancelDrag();
                checkAndRemoveEmptyCell();
                mLauncher.showOutOfSpaceMessage(true);
                return;
            }
        }
        /*YUNOS END*/

        final int page = indexOfChild(cellLayout);
        final int screen = getIconScreenIndex(page);
        if (!mLauncher.isHotseatLayout(cellLayout) && page != mCurrentPage
                && mState != State.SPRING_LOADED) {
            snapToPage(page);
        }

        if (info instanceof PendingAddItemInfo) {
            final PendingAddItemInfo pendingInfo = (PendingAddItemInfo) dragInfo;

            boolean findNearestVacantCell = true;
            if (pendingInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                mTargetCell = findNearestArea((int) touchXY[0], (int) touchXY[1], spanX, spanY,
                        cellLayout, mTargetCell);
                float distance = cellLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                        mDragViewVisualCenter[1], mTargetCell);
                if (willCreateUserFolder((ItemInfo) d.dragInfo, cellLayout, mTargetCell,
                        distance, true)) {
                    findNearestVacantCell = false;
                } else if (willAddToExistingUserFolder((ItemInfo) d.dragInfo, cellLayout, mTargetCell, distance)) {
                    if (mDragShortcutInSpringLoadMode) {
                        findNearestVacantCell = true;
                        mDragShortcutInSpringLoadMode = false;
                    } else {
                        findNearestVacantCell = false;
                    }
                }
            }

            final ItemInfo item = (ItemInfo) d.dragInfo;
            boolean updateWidgetSize = false;
            if (findNearestVacantCell) {
                int minSpanX = item.spanX;
                int minSpanY = item.spanY;
                if (item.minSpanX > 0 && item.minSpanY > 0) {
                    minSpanX = item.minSpanX;
                    minSpanY = item.minSpanY;
                }
                int[] resultSpan = new int[2];
                mTargetCell = cellLayout.createArea((int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], minSpanX, minSpanY, info.spanX, info.spanY,
                        null, mTargetCell, resultSpan, CellLayout.MODE_ON_DROP_EXTERNAL);

                if (resultSpan[0] != item.spanX || resultSpan[1] != item.spanY) {
                    updateWidgetSize = true;
                }
                item.spanX = resultSpan[0];
                item.spanY = resultSpan[1];
            }

            Runnable onAnimationCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    // When dragging and dropping from customization tray, we deal with creating
                    // widgets/shortcuts/folders in a slightly different way
                    switch (pendingInfo.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                        int span[] = new int[2];
                        span[0] = item.spanX;
                        span[1] = item.spanY;
                            mLauncher.addAppWidgetFromDrop((PendingAddWidgetInfo) pendingInfo, container, screen, mTargetCell, span, null);
                        break;
                    /* YUNOS BEGIN */
                    // ##gadget
                    // ##date:2014/02/27
                    // ##author:kerong.skr@alibaba-inc.com##BugID:96378
                    case LauncherSettings.Favorites.ITEM_TYPE_GADGET:
                        span = new int[2];
                        span[0] = item.spanX;
                        span[1] = item.spanY;
                        GadgetItemInfo info = new GadgetItemInfo(((PendingAddGadgetInfo) pendingInfo).gadgetInfo);
                            if (AgedModeUtil.isAgedMode()) {
                            info.spanX = info.spanX > 3 ? 3 : info.spanX;
                            info.spanY = info.spanX > 3 ? 3 : info.spanY;
                            }
                            mLauncher.addGadgetFromDrop(info, container, screen, mTargetCell, span, null);
                        break;
                    /* YUNOS END */
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                            mLauncher.processShortcutFromDrop(pendingInfo.componentName, container, screen, mTargetCell, null);
                        break;
                    default:
                        throw new IllegalStateException("Unknown item type: " +
                                pendingInfo.itemType);
                    }
                }
            };
            View finalView = pendingInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                    ? ((PendingAddWidgetInfo) pendingInfo).boundWidget : null;

            if (finalView instanceof AppWidgetHostView && updateWidgetSize) {
                AppWidgetHostView awhv = (AppWidgetHostView) finalView;
                AppWidgetResizeFrame.updateWidgetSizeRanges(awhv, mLauncher, item.spanX,
                        item.spanY);
            }

            int animationStyle = ANIMATE_INTO_POSITION_AND_DISAPPEAR;
            /*YUNOS BEGIN*/
            //2013-11-23 author : xiaodong.lxd
            if ((pendingInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET &&
                    ((PendingAddWidgetInfo) pendingInfo).info.configure != null)
                    ||
                    (pendingInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT &&
                    ((PendingAddShortcutInfo) pendingInfo).shortcutActivityInfo != null)) {
                animationStyle = ANIMATE_INTO_POSITION_AND_REMAIN;
            }
            /*YUNOS END*/
            animateWidgetDrop(info, cellLayout, d.dragView, onAnimationCompleteRunnable,
                    animationStyle, finalView, true);
        } else {
            // This is for other drag/drop cases, like dragging from All Apps
            View view = null;

            switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                if (info.container == NO_ID && info instanceof ApplicationInfo) {
                    // Came from all apps -- make a copy
                    info = new ShortcutInfo((ApplicationInfo) info);
                }
                view = mLauncher.createShortcut(R.layout.application, cellLayout,
                        (ShortcutInfo) info);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                view = FolderIcon.fromXml(R.layout.folder_icon, mLauncher, cellLayout,
                        (FolderInfo) info);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
                view = mLauncher.createShortcut(R.layout.application, cellLayout,
                        (ShortcutInfo) info);
                break;

//remove vp install
//            case LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL:
//                 view = mLauncher.createShortcut(R.layout.application, cellLayout, (ShortcutInfo) info);
//                break;

            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
            }

            // First we find the cell nearest to point at which the item is
            // dropped, without any consideration to whether there is an item there.
            if (touchXY != null) {
                mTargetCell = findNearestArea((int) touchXY[0], (int) touchXY[1], spanX, spanY,
                        cellLayout, mTargetCell);
                float distance = cellLayout.getDistanceFromCell(mDragViewVisualCenter[0],
                        mDragViewVisualCenter[1], mTargetCell);
                d.postAnimationRunnable = null;
                if (createUserFolderIfNecessary(view, container, cellLayout, mTargetCell, distance,
                        true, d.dragView, d.postAnimationRunnable)) {
                    return;
                }
                if (addToExistingFolderIfNecessary(view, cellLayout, mTargetCell, distance, d,
                        true)) {
                    return;
                }
            }

            if (touchXY != null) {
                // when dragging and dropping, just find the closest free spot
                mTargetCell = cellLayout.createArea((int) mDragViewVisualCenter[0],
                        (int) mDragViewVisualCenter[1], 1, 1, 1, 1,
                        null, mTargetCell, null, CellLayout.MODE_ON_DROP_EXTERNAL);
            } else {
                cellLayout.findCellForSpan(mTargetCell, 1, 1);
            }

            /*YUNOS BEGIN*/
            //##date:2013/12/09 ##author:xiaodong.lxd
            //for bug 72662
            if(mLauncher.isHotseatLayout(cellLayout)) {
                int index = mLauncher.getHotseat().getAppropriateIndex(d.x, d.y);
                mLauncher.getHotseat().onDrop(true, d.x, d.dragView, view, true);
                addInHotseat(view, container, index, index, 0,
                        info.spanX, info.spanY, index);
                mLauncher.getHotseat().updateItemCell();
                mLauncher.getHotseat().updateItemInDatabase();
            } else {
                addInScreen(view, container, screen, mTargetCell[0], mTargetCell[1], info.spanX,
                        info.spanY, insertAtFirst);
                // YUNOS BEGIN
                // ##modules(HomeShell): ##yongxing.lyx
                // ##BugID:(8185568) ##date:2016/4/22
                // ##description: Sometime cellLayout was setAlpha(0) at
                // EditModeHelper.resetScreenParamsOnExitEditMode()
                if (cellLayout.getAlpha() <= 0.01f) {
                    cellLayout.setAlpha(1.0f);
                }
                // YUNOS END
                cellLayout.onDropChild(view);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
                cellLayout.getShortcutAndWidgetContainer().measureChild(view);
                final long originalContainer = info.container;
                UserTrackerHelper.sendDragIconReport(info, originalContainer, container, screen, lp.cellX, lp.cellY);
                LauncherModel.addOrMoveItemInDatabase(mLauncher, info, container, screen,
                        lp.cellX, lp.cellY);
                if (ConfigManager.isLandOrienSupport()) {
                    cellLayout.updateShortcutXYOnMove(info);
                }

                /* YUNOS BEGIN */
                // ##date:2014/7/24 ##author:zhanggong.zg ##BugID:5244146
                // drag icon from hide-seat to workspace
                if (originalContainer == LauncherSettings.Favorites.CONTAINER_HIDESEAT &&
                    info instanceof ShortcutInfo) {
                    if (mLauncher != null && mLauncher.getHideseat() != null) {
                        mLauncher.getHideseat().onDragIconFromHideseatToWorkspace(view, (ShortcutInfo) info, this);
                    }
                }
                /* YUNOS END */
            }
            if (d.dragView != null) {
                // We wrap the animation call in the temporary set and reset of the current
                // cellLayout to its final transform -- this means we animate the drag view to
                // the correct final location.
                setFinalTransitionTransform(cellLayout);
                mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, view, null);
                resetTransitionTransform(cellLayout);
            }
            /*YUNOS END*/
        }
    }

    public Bitmap createWidgetBitmap(ItemInfo widgetInfo, View layout) {
        int[] unScaledSize = mLauncher.getWorkspace().estimateItemSize(widgetInfo.spanX,
                widgetInfo.spanY, widgetInfo, false);
        int visibility = layout.getVisibility();
        layout.setVisibility(VISIBLE);

        int width = MeasureSpec.makeMeasureSpec(unScaledSize[0], MeasureSpec.EXACTLY);
        int height = MeasureSpec.makeMeasureSpec(unScaledSize[1], MeasureSpec.EXACTLY);
        if (unScaledSize[0] > 0 &&  unScaledSize[1] > 0) {
            Bitmap b = Bitmap.createBitmap(unScaledSize[0], unScaledSize[1],
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);

            layout.measure(width, height);
            layout.layout(0, 0, unScaledSize[0], unScaledSize[1]);
            layout.draw(c);
            c.setBitmap(null);
            layout.setVisibility(visibility);
            return b;
        }
        return null;
    }

    private void getFinalPositionForDropAnimation(int[] loc, float[] scaleXY,
            DragView dragView, CellLayout layout, ItemInfo info, int[] targetCell,
            boolean external, boolean scale) {
        // Now we animate the dragView, (ie. the widget or shortcut preview) into its final
        // location and size on the home screen.
        int spanX = info.spanX;
        int spanY = info.spanY;

        Rect r = estimateItemPosition(layout, info, targetCell[0], targetCell[1], spanX, spanY);
        loc[0] = r.left;
        loc[1] = r.top;

        setFinalTransitionTransform(layout);
        float cellLayoutScale =
                mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(layout, loc);
        resetTransitionTransform(layout);

        float dragViewScaleX;
        float dragViewScaleY;
        if (scale) {
            dragViewScaleX = (1.0f * r.width()) / dragView.getMeasuredWidth();
            dragViewScaleY = (1.0f * r.height()) / dragView.getMeasuredHeight();
        } else {
            dragViewScaleX = 1f;
            dragViewScaleY = 1f;
        }

        // The animation will scale the dragView about its center, so we need to center about
        // the final location.
        loc[0] -= (dragView.getMeasuredWidth() - cellLayoutScale * r.width()) / 2;
        loc[1] -= (dragView.getMeasuredHeight() - cellLayoutScale * r.height()) / 2;

        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/22 ## author: wangye.wy
        // ## BugID: 8047692: header in cell layout
        if (mInEditing) {
            loc[1] += (int)sHeaderHeight;
        }
        /* YUNOS END */

        scaleXY[0] = dragViewScaleX * cellLayoutScale;
        scaleXY[1] = dragViewScaleY * cellLayoutScale;
    }

    public void animateWidgetDrop(ItemInfo info, CellLayout cellLayout, DragView dragView,
            final Runnable onCompleteRunnable, int animationType, final View finalView,
            boolean external) {
        Rect from = new Rect();
        mLauncher.getDragLayer().getViewRectRelativeToSelf(dragView, from);

        int[] finalPos = new int[2];
        float scaleXY[] = new float[2];
        /* YUNOS BEGIN */
        // ##date:2015/1/12 ##author:zhanggong.zg ##BugID:5697618
        // boolean scalePreview = !(info instanceof PendingAddShortcutInfo);
        boolean scalePreview = true;
        /* YUNOS END */
        getFinalPositionForDropAnimation(finalPos, scaleXY, dragView, cellLayout, info, mTargetCell,
                external, scalePreview);

        Resources res = mLauncher.getResources();
        int duration = res.getInteger(R.integer.config_dropAnimMaxDuration) - 200;

        // In the case where we've prebound the widget, we remove it from the DragLayer
        if (finalView instanceof AppWidgetHostView && external) {
            Log.d(TAG, "6557954 Animate widget drop, final view is appWidgetHostView");
            mLauncher.getDragLayer().removeView(finalView);
        }
        if ((animationType == ANIMATE_INTO_POSITION_AND_RESIZE || external) && finalView != null) {
            Bitmap crossFadeBitmap = createWidgetBitmap(info, finalView);
            dragView.setCrossFadeBitmap(crossFadeBitmap);
            dragView.crossFade((int) (duration * 0.8f));
        } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET && external) {
            scaleXY[0] = scaleXY[1] = Math.min(scaleXY[0],  scaleXY[1]);
        }

        DragLayer dragLayer = mLauncher.getDragLayer();
        if (animationType == CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION) {
            mLauncher.getDragLayer().animateViewIntoPosition(dragView, finalPos, 0f, 0.1f, 0.1f,
                    DragLayer.ANIMATION_END_DISAPPEAR, onCompleteRunnable, duration);
        } else {
            int endStyle;
            if (animationType == ANIMATE_INTO_POSITION_AND_REMAIN) {
                endStyle = DragLayer.ANIMATION_END_REMAIN_VISIBLE;
            } else {
                endStyle = DragLayer.ANIMATION_END_DISAPPEAR;;
            }

            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    if (finalView != null) {
                        finalView.setVisibility(VISIBLE);
                    }
                    if (onCompleteRunnable != null) {
                        onCompleteRunnable.run();
                    }
                }
            };
            float scale = Math.min(scaleXY[0], scaleXY[1]);
            dragLayer.animateViewIntoPosition(dragView, from.left, from.top, finalPos[0],
                    finalPos[1], 1, 1, 1, scale, scale, onComplete, endStyle,
                    duration, this);
        }
    }

    public void setFinalTransitionTransform(CellLayout layout) {
        if (isSwitchingState()) {
            int index = indexOfChild(layout);
            mCurrentScaleX = layout.getScaleX();
            mCurrentScaleY = layout.getScaleY();
            mCurrentTranslationX = layout.getTranslationX();
            mCurrentTranslationY = layout.getTranslationY();
            mCurrentRotationY = layout.getRotationY();
            layout.setScaleX(mNewScaleXs[index]);
            layout.setScaleY(mNewScaleYs[index]);
            layout.setTranslationX(mNewTranslationXs[index]);
            layout.setTranslationY(mNewTranslationYs[index]);
            layout.setRotationY(mNewRotationYs[index]);
        }
    }
    public void resetTransitionTransform(CellLayout layout) {
        if (isSwitchingState()) {
            mCurrentScaleX = layout.getScaleX();
            mCurrentScaleY = layout.getScaleY();
            mCurrentTranslationX = layout.getTranslationX();
            mCurrentTranslationY = layout.getTranslationY();
            mCurrentRotationY = layout.getRotationY();
            layout.setScaleX(mCurrentScaleX);
            layout.setScaleY(mCurrentScaleY);
            layout.setTranslationX(mCurrentTranslationX);
            layout.setTranslationY(mCurrentTranslationY);
            layout.setRotationY(mCurrentRotationY);
        }
    }

    /**
     * Return the current {@link CellLayout}, correctly picking the destination
     * screen while a scroll is in progress.
     */
    public CellLayout getCurrentDropLayout() {
        return (CellLayout) getChildAt(getNextPage());
    }

    /**
     * Return the current CellInfo describing our current drag; this method exists
     * so that Launcher can sync this object with the correct info when the activity is created/
     * destroyed
     *
     */
    public CellLayout.CellInfo getDragInfo() {
        /*YUNOS BEGIN*/
        //##date:2014/01/02 ##author:jun.dongj@alibaba-inc.com##BugID:81225
        //return mDragInfo first, then return the retored draginfo mDragInfoDelete;
        if(mDragInfo!=null){
            return mDragInfo;
        }else{
            return mDragInfoDelete;
        }
        /*YUNOS END*/
    }

    /**
     * Calculate the nearest cell where the given object would be dropped.
     *
     * pixelX and pixelY should be in the coordinate system of layout
     */
    private int[] findNearestArea(int pixelX, int pixelY,
            int spanX, int spanY, CellLayout layout, int[] recycle) {
        return layout.findNearestArea(
                pixelX, pixelY, spanX, spanY, recycle);
    }

    void setup(DragController dragController) {
        mSpringLoadedDragController = new SpringLoadedDragController(mLauncher);
        mDragController = dragController;

        // hardware layers on children are enabled on startup, but should be disabled until
        // needed
        updateChildrenLayersEnabled(false);
        setWallpaperDimension();
    }

    /**
     * Called at the end of a drag which originated on the workspace.
     */
    public void onDropCompleted(View target, final DragObject d, boolean isFlingToDelete,
            boolean success) {

        mDropTargetView = target;
        if (success) {
            if (target != this) {
                if (mDragInfo != null) {
                    /* YUNOS BEGIN */
                    // ##date:2013/11/25 ##author:xiaodong.lxd
                    // not remove the view in case user click the cancel uninstall
                    //getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
                   // Log.d(TAG, "sxsexe----> onDropComplete d.dragInfo " + d.dragInfo);
                    if(d.dragInfo instanceof ShortcutInfo) {
                        ShortcutInfo shortcutInfo = (ShortcutInfo)d.dragInfo;
                        if(d.isFlingToMove) {
                            mDragInfo.cell.setVisibility(View.GONE);
                        } else {
                            if(shortcutInfo.itemType == Favorites.ITEM_TYPE_APPLICATION) {
                                if (target instanceof Hideseat) {
                                    getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
                                } else {
                                    if(!shortcutInfo.isDeletable()) {
                                        mLauncher.reVisibileDraggedItem(shortcutInfo);
                                    } else if (!(target instanceof PreviewContainer)) {
                                        mDragInfo.cell.setVisibility(View.INVISIBLE);
                                    }
                                }
                            } else if(shortcutInfo.itemType == Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING || shortcutInfo.itemType == Favorites.ITEM_TYPE_SHORTCUT) {
                                if (!shortcutInfo.isDeletable()) {
                                    mLauncher.reVisibileDraggedItem(shortcutInfo);
                                } else {
                                    if (target instanceof DeleteDropTarget) {
                                        CellLayout layout = getParentCellLayoutForView(mDragInfo.cell);
                                        if (layout != null) {
                                            layout.removeView(mDragInfo.cell);
                                        }
                                    } else {
                                        mLauncher.reVisibileDraggedItem(shortcutInfo);
                                    }
                                }
                            } else {
                                DataCollector.getInstance(
                                        getContext().getApplicationContext())
                                        .collectDeleteShortcutData(shortcutInfo);
                                CellLayout layout = getParentCellLayoutForView(mDragInfo.cell);
                                if (layout != null) {
                                    layout.removeView(mDragInfo.cell);
                                }
                            }
                        }
                    } else if(d.dragInfo instanceof LauncherAppWidgetInfo) {
                        if(target instanceof DeleteDropTarget) {
                            getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
                        }
                        /*YUNOS BEGIN*/
                        //##date:2013/11/29 ##author:xindong.zxd
                        //release widget view referenece
                         mDragInfo.cell = null;
                        mDragOutline = null;
                        /*YUNOS END*/

                        /* YUNOS BEGIN */
                        // ##date:2014/03/10 ##author:zhangqiang.zq
                        // gadget
                    } else if (d.dragInfo instanceof GadgetItemInfo) {
                        if(target instanceof DeleteDropTarget) {
                            CellLayout layout = getParentCellLayoutForView(mDragInfo.cell);
                            if (layout != null) {
                                layout.removeView(mDragInfo.cell);
                            }
                        }
                        /* YUNOS END */
                    } else {
                        if (d.dragInfo instanceof FolderInfo && target instanceof Hideseat) {
                            getParentCellLayoutForView(mDragInfo.cell).removeView(mDragInfo.cell);
                        } else if (!(target instanceof PreviewContainer)){ //BugID:6521096
                            mDragInfo.cell.setVisibility(View.INVISIBLE);
                        }
                    }
                    /* YUNOS END */
                    if (mDragInfo.cell instanceof DropTarget) {
                        mDragController.removeDropTarget((DropTarget) mDragInfo.cell);
                    }
                }
            }
        } else if (mDragInfo != null) {
            CellLayout cellLayout;
            if (mLauncher.isHotseatLayout(target)) {
                cellLayout = mLauncher.getHotseat().getLayout();
            } else {
                cellLayout = (CellLayout) getChildAt(mDragInfo.screen);
            }
            /* YUNOS BEGIN */
            // ##date:2014/06/23 ##author:hongchao.ghc ##BugID:142294
            if (cellLayout != null) {
                cellLayout.onDropChild(mDragInfo.cell);
                // ##date:2014/9/9 ##author:zhanggong.zg ##BugID:5244146
                cellLayout.markCellsAsOccupiedForView(mDragInfo.cell);
            }
            /* YUNOS END */
            if (target != this) {
                if(mLauncher.getHotseat().checkDragitem(mDragInfo.cell)) {
                    //back to hotseat
                    ItemInfo info = (ItemInfo)mDragInfo.cell.getTag();
                    addInHotseat(mDragInfo.cell, info.container, info.screen,
                            info.cellX, info.cellY, info.spanX, info.spanY, info.screen);
                    mLauncher.getHotseat().onDrop(false, 0, d.dragView, mDragInfo.cell, true);
                } else {
                    //YUNOS BEGIN PB
                    //## modules(HomeShell):
                    //## date:2015/09/16 ##author:shuoxing.wsx
                    //## BugID:6424969:solve the issue icons is gone when drop  in edit mode.
                    if(mIsMultiSelectDragging) {
                        if(target instanceof PreviewContainer){
                            ((PreviewContainer)target).onDropMultiViews(d);
                        } else if (target == null) {
                            // the drag has been cancel, selected views go back
                            cancelMultiDrag();
                        }
                    }else{
                        Runnable onAnimationEndRunnable = new Runnable() {

                            @Override
                            public void run() {
                                ItemInfo info = (ItemInfo) d.dragInfo;
                                LauncherModel.assignPlace(info.screen, info.cellX, info.cellY, info.spanX, info.spanY, true, Mode.NORMAL);
                                if (ConfigManager.isLandOrienSupport() && info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                                    LauncherModel.markCellsOccupiedInNonCurrent(info, false);
                                }
                            }
                        };
                        mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, mDragInfo.cell, onAnimationEndRunnable);
                    }
                    //YUNOS END PB
                }
            }
        }

        /* YUNOS BEGIN */
        // ##date:2014/01/03 ##author:zhangqiang.zq
        // 82193: fix crash
        if (d.cancelled && mDragInfo != null && mDragInfo.cell != null) {
            mDragInfo.cell.setVisibility(VISIBLE);
        }

        if (d.cancelled && mDragInfoDelete != null
                && mDragInfoDelete.cell != null) {
            mDragInfoDelete.cell.setVisibility(VISIBLE);
        }
        /* YUNOS END */

        /*YUNOS BEGIN*/
        //##date:2014/01/02 ##author:jun.dongj@alibaba-inc.com##BugID:81225
        //set mDragInfo to null to keep android logic and restore mDragInfo for delete
        mDragInfoDelete = mDragInfo;
        mDragOutline = null;
        mDragInfo = null;
        /* YUNOS END */

        // Hide the scrolling indicator after you pick up an item
        hideScrollingIndicator(false);

        if (!d.isFlingToMove && !mLauncher.isDragToDelete() && !mLauncher.isDragToClone()) {
            if (mIsMultiSelectDragging) {
                for (View view : selectedViews) {
                    if (view.getTag() != null) {
                        removeDragItemFromList((ItemInfo) view.getTag());
                    }
                }
            } else if (d.dragInfo != null) {
                removeDragItemFromList((ItemInfo) d.dragInfo);
            }
        }

        // onFling to delete target, checkAndRemoveEmptyCell is called in DeleteDropTarget's ondrop.
        if (d.dragSource != target && !(target instanceof DeleteDropTarget)) {
            checkAndRemoveEmptyCell();
        }

        mDropTargetView = null;
    }

    /* YUNOS BEGIN added by xiaodong.lxd*/
    void cleanDragInfo() {
        if(mDragInfo != null && mDragInfo.cell != null) {
            ItemInfo info = (ItemInfo) mDragInfo.cell.getTag();
            View view = getDragItemFromList(info, false);
            if(view != null) {
                return;
            }
        }
        /*YUNOS BEGIN*/
        //##date:2014/01/02 ##author:jun.dongj@alibaba-inc.com##BugID:81225
        //clear the backup delete drag info
        mDragInfoDelete = null;
        /* YUNOS END */
        mDragOutline = null;
        mDragInfo = null;
    }
    /* YUNOS END */
    void cleanDragInfoFromFolder() {
        mDragInfoDelete = mDragInfo;
        mDragOutline = null;
        mDragInfo = null;
    }

    void updateItemLocationsInDatabase(CellLayout cl) {
        int count = cl.getShortcutAndWidgetContainer().getChildCount();

        int screen = getIconScreenIndex(indexOfChild(cl));
        int container = Favorites.CONTAINER_DESKTOP;

        if (mLauncher.isHotseatLayout(cl)) {
            screen = -1;
            container = Favorites.CONTAINER_HOTSEAT;
        }

        for (int i = 0; i < count; i++) {
            View v = cl.getShortcutAndWidgetContainer().getChildAt(i);
            ItemInfo info = (ItemInfo) v.getTag();
            // Null check required as the AllApps button doesn't have an item info
            if (info != null && info.requiresDbUpdate) {
                info.requiresDbUpdate = false;
                LauncherModel.modifyItemInDatabase(mLauncher, info, container, screen, info.cellX,
                        info.cellY, info.spanX, info.spanY);
            }
        }
    }

    @Override
    public boolean supportsFlingToDelete() {
        if(mLauncher.isInLauncherEditMode() && mIsMultiSelectDragging) {
            return false;
        }
        return true;
    }

    @Override
    public void onFlingToDelete(DragObject d, int x, int y, PointF vec) {
        // Do nothing
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // Do nothing
    }

    public boolean isDropEnabled() {
        return true;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        Launcher.setScreen(mCurrentPage);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        // We don't dispatch restoreInstanceState to our children using this code path.
        // Some pages will be restored immediately as their items are bound immediately, and
        // others we will need to wait until after their items are bound.
        mSavedStates = container;
    }

    public void restoreInstanceStateForChild(int child) {
        if (mSavedStates != null) {
            mRestoredPages.add(child);
            CellLayout cl = (CellLayout) getChildAt(child);
            cl.restoreInstanceState(mSavedStates);
        }
    }

    public void restoreInstanceStateForRemainingPages() {
        int screenCount = getIconScreenCount();
        int offSet = getIconScreenHomeIndex();
        for (int i = offSet; i < screenCount + offSet; i++) {
            if (!mRestoredPages.contains(i)) {
                restoreInstanceStateForChild(i);
            }
        }
        mRestoredPages.clear();
    }

    private void moveAlongWithWorkspace(int translationX) {
        mIsHotseatMovingWithWorkspace = (translationX != 0);
        mLauncher.getIndicatorView().setTranslationX(translationX);
        mLauncher.getHotseat().setTranslationX(translationX);
        if (mLauncher.isHideseatShowing()) {
            mLauncher.getCustomHideseat().setTranslationX(translationX);
            mLauncher.getGestureLayer().setWallpaperTranslationX(translationX);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        boolean hotseatMovable = false;
        int w = getRight() - getLeft();
        int homeTotalW = (getChildCount() - (mLauncher.isSupportLifeCenter() ? 1 : 0)) * w;
        int leftPageTotalW = getIconScreenHomeIndex() * w;

        if (mLauncher.isSupportLifeCenter()) {
            if (mCurrentPage >= 0 && mCurrentPage < getIconScreenHomeIndex()) {
                // touch in left pages
                if (mCurrentPage == 0 && x <= 0 && sContinuousHomeShellFeature) {
                    hotseatMovable = true;
                }
                if (mCurrentPage == (getIconScreenHomeIndex() - 1) && x >= 0 && x <= leftPageTotalW) {
                    hotseatMovable = true;
                }
            } else if (mCurrentPage == getIconScreenHomeIndex() && x <= leftPageTotalW) {
                hotseatMovable = true;
            /* YUNOS BEGIN PB */
            // ##date:2016/1/20    ##author:suhang.sh
            // ##module:homeshell
            // ##description:Fixed 7804939
            } else if (mCurrentPage == (getChildCount() - 1) && (x <= 0 || x >= homeTotalW) && sContinuousHomeShellFeature) {
            /* YUNOS END PB */
                hotseatMovable = true;
            }
        }

        if (FeatureUtility.hasFullScreenWidget() && mLauncher != null && mLauncher.mWidgetPageManager != null
                && mLauncher.mWidgetPageManager.getWigetPageCount() > 0) {
            int nonWidgetCount = getScreenCountExcludeWidgetPages();

            CellLayout cellLayout = (CellLayout) getChildAt(mCurrentPage);
            if (cellLayout != null && cellLayout.isWidgetPage()) {
                // touch in widget page
                if (mCurrentPage == (getChildCount() - 1) || mCurrentPage == nonWidgetCount) {
                    hotseatMovable = false;
                    mIsHotseatMovingWithWorkspace = false;
                    if (mCurrentPage == nonWidgetCount && x <= (nonWidgetCount * w)) {
                        if (mLauncher.isHideseatShowing()) {
                            int translationX = nonWidgetCount * w - x - w;
                            mLauncher.getCustomHideseat().setTranslationX(translationX);
                            mLauncher.getGestureLayer().setWallpaperTranslationX(translationX);
                        }
                    }
                }
            } else {
                if (mLauncher.isLeftScreen(mCurrentPage) && x <= 0 && sContinuousHomeShellFeature) {
                    hotseatMovable = false;
                    mIsHotseatMovingWithWorkspace = false;
                }
                if (mCurrentPage == (nonWidgetCount - 1) && x >= ((nonWidgetCount - 1) * w)) {
                    hotseatMovable = false;
                    if (mLauncher.isHideseatShowing()) {
                        int translationX = nonWidgetCount * w - x - w;
                        mLauncher.getCustomHideseat().setTranslationX(translationX);
                        mLauncher.getGestureLayer().setWallpaperTranslationX(translationX);
                    }
                }
            }
        }

        float progress = 0.f;
        if (hotseatMovable) {
            int hotseatW = mLauncher.getHotseat().getWidth();
            int tranX = 0;
            if (TOUCH_STATE_REST != mTouchState || !mScroller.isFinished() || mIsPageMoving) {
                if (x >= 0 && x < leftPageTotalW) {
                    // 1 :from first home screen, scroll right to show lifecenter
                    // 2 :from lifecenter, scroll left to show first home screen
                    tranX = hotseatW - x + leftPageTotalW - w;
                } else {
                    if (x > homeTotalW) {
                        // from last home screen, scroll left to show lifecenter
                        tranX = homeTotalW - x;
                    } else if (x < 0) {
                        // from lifecenter, scroll right to show last home
                        // screen
                        tranX = -hotseatW - x;
                    }
                }
                moveAlongWithWorkspace(tranX);
                progress = tranX / (float) hotseatW;
            }
        } else if (mIsHotseatMovingWithWorkspace) {
            // ##date:2015-6-29 ##author:zhanggong.zg ##BugID:6096959
            // reset the position if hotseat is not yet located at 0
            if (sContinuousHomeShellFeature) {
                moveAlongWithWorkspace(0);
                progress = 0f;
            } else if (mLauncher.isSupportLifeCenter()) {
                if (!(mCurrentPage == 0 && x <= 0)) {
                    moveAlongWithWorkspace(0);
                    progress = 0f;
                }
            }
        }

        if (mLauncher.isSupportLifeCenter() && mLauncher.getCardBridge() != null) {
            mLauncher.getCardBridge().onWorkspaceScrolled(progress);
        }

        super.scrollTo(x, y);
    }

    @Override
    public void scrollLeft() {
        if (!isSmall() && !mIsSwitchingState) {
            if (isScrollHideseat) {
                mLauncher.getHideseat().scrollLeft();
            } else {
                int iconScreenIndex = getIconScreenIndex(mCurrentPage);
                if (mScroller.isFinished()) {
                    if (iconScreenIndex > 0)
                        snapToPage(mCurrentPage - 1);
                } else {
                    if (iconScreenIndex > 0 && mNextPage > 0)
                        snapToPage(mNextPage - 1);
                }
            }
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.completeDragExit();
        }
    }

    @Override
    public void scrollRight() {
        if (!isSmall() && !mIsSwitchingState) {
            if (isScrollHideseat) {
                     mLauncher.getHideseat().scrollRight();
            } else {
                int iconScreenIndex = getIconScreenIndex(mCurrentPage);
                if (mScroller.isFinished()) {
                    if (iconScreenIndex < getIconScreenCount() - 1)
                        snapToPage(mCurrentPage + 1);
                } else {
                    if ((iconScreenIndex < getIconScreenCount() - 1) && (mNextPage < getChildCount() - 1))
                        snapToPage(mNextPage + 1);
                }
            }
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.completeDragExit();
        }
    }

    @Override
    public void scrollUp() {

    }

    @Override
    public void scrollDown() {

    }

    private boolean isScrollHideseat = false;
    @Override
    public boolean onEnterScrollArea(int x, int y, int direction) {
        // Ignore the scroll area if we are dragging over the hot seat
        boolean isPortrait = !LauncherApplication.isScreenLandscape(getContext());
        isScrollHideseat = false;
        if (mLauncher.isHideseatShowing() && isPortrait) {
            Rect r = new Rect();
            mLauncher.getCustomHideseat().getHitRect(r);
            if (r.contains(x, y)) {
                isScrollHideseat = true;
                return true;
            }
        }

        if (mLauncher.getHotseat() != null && isPortrait) {
            Rect r = new Rect();
            mLauncher.getHotseat().getHitRect(r);
            if (r.contains(x, y)) {
                return false;
            }
        }

        boolean result = false;
        if (!isSmall() && !mIsSwitchingState) {
            mInScrollArea = true;

            final int page = getNextPage() +
                       (direction == DragController.SCROLL_LEFT ? -1 : 1);

            // We always want to exit the current layout to ensure parity of enter / exit
            setCurrentDropLayout(null);
            int iconScreenHomeIndex = getIconScreenHomeIndex();
            int iconScreenCount = getIconScreenCount();
            if (iconScreenHomeIndex <= page && page < iconScreenHomeIndex + iconScreenCount) {
                CellLayout layout = (CellLayout) getChildAt(page);
                setCurrentDragOverlappingLayout(layout);

                // Workspace is responsible for drawing the edge glow on adjacent pages,
                // so we need to redraw the workspace when this may have changed.
                invalidate();
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean onExitScrollArea() {
        boolean result = false;
        if (mInScrollArea) {
            invalidate();
            CellLayout layout = getCurrentDropLayout();
            setCurrentDropLayout(layout);
            setCurrentDragOverlappingLayout(layout);

            result = true;
            mInScrollArea = false;
        }
        return result;
    }

    private void onResetScrollArea() {
        setCurrentDragOverlappingLayout(null);
        mInScrollArea = false;
    }

    /**
     * Returns a specific CellLayout
     */
    public CellLayout getParentCellLayoutForView(View v) {
        ArrayList<CellLayout> layouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layout : layouts) {
            if (layout.getShortcutAndWidgetContainer().indexOfChild(v) > -1) {
                return layout;
            }
        }

        if(mLauncher.getHotseat().checkDragitem(v)) {
            return mLauncher.getHotseat().getCellLayout();
        }
        return null;
    }

    /**
     * Returns a list of all the CellLayouts in the workspace.
     */
    ArrayList<CellLayout> getWorkspaceAndHotseatCellLayouts() {
        ArrayList<CellLayout> layouts = new ArrayList<CellLayout>();
        int screenCount = getIconScreenCount();
        int offSet = getIconScreenHomeIndex();
        for (int screen = offSet; screen < screenCount + offSet; screen++) {
            layouts.add(((CellLayout) getChildAt(screen)));
        }
        if (mLauncher.getHotseat() != null) {
            layouts.add(mLauncher.getHotseat().getLayout());
        }

        final Hideseat hideseat= mLauncher.getHideseat();
        if(hideseat != null){
            screenCount = hideseat.getChildCount();
            for (int screen = 0; screen < screenCount; screen++) {
                layouts.add((CellLayout) hideseat.getChildAt(screen));
            }
        }

        return layouts;
    }

    /**
     * We should only use this to search for specific children.  Do not use this method to modify
     * ShortcutsAndWidgetsContainer directly. Includes ShortcutAndWidgetContainers from
     * the hotseat and workspace pages
     */
    public ArrayList<ShortcutAndWidgetContainer> getAllShortcutAndWidgetContainers() {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                new ArrayList<ShortcutAndWidgetContainer>();
        int screenCount = getIconScreenCount();
        int offSet = getIconScreenHomeIndex();
        for (int screen = offSet; screen < screenCount + offSet; screen++) {
            childrenLayouts.add(((CellLayout) getChildAt(screen)).getShortcutAndWidgetContainer());
        }
        if (mLauncher.getHotseat() != null) {
            childrenLayouts.add(mLauncher.getHotseat().getLayout().getShortcutAndWidgetContainer());
        }
        return childrenLayouts;
    }

    public Folder getFolderForTag(Object tag) {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout: childrenLayouts) {
            int count = layout.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = layout.getChildAt(i);
                if (child instanceof Folder) {
                    Folder f = (Folder) child;
                    if (f.getInfo() == tag && f.getInfo().opened) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public View getViewForTag(Object tag) {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout: childrenLayouts) {
            int count = layout.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = layout.getChildAt(i);
                if (child.getTag() == tag) {
                    return child;
                }
            }
        }
        return null;
    }

    void clearReference() {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts =
                getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout: childrenLayouts) {
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View v = layout.getChildAt(j);
                if (v instanceof DropTarget) {
                    mDragController.removeDropTarget((DropTarget) v);
                }

                if (v instanceof FolderIcon) {
                    mLauncher.removeFolder((FolderInfo)v.getTag());
                }
            }
        }
    }

    // Removes ALL items that match a given package name, this is usually called when a package
    // has been removed and we want to remove all components (widgets, shortcuts, apps) that
    // belong to that package.
    void removeItemsByPackageName(final ArrayList<String> packages) {
        HashSet<String> packageNames = new HashSet<String>();
        packageNames.addAll(packages);

        // Just create a hash table of all the specific components that this will affect
        HashSet<ComponentName> cns = new HashSet<ComponentName>();
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layoutParent : cellLayouts) {
            ViewGroup layout = layoutParent.getShortcutAndWidgetContainer();
            int childCount = layout.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                View view = layout.getChildAt(i);
                Object tag = view.getTag();
                /*YUNOS BEGIN*/
                //##date:2013/12/04 ##author:hao.liuhaolh ##BugID:70318
                //NullPointer Exception fix
                if (tag instanceof ShortcutInfo) {
                    ShortcutInfo info = (ShortcutInfo) tag;
                    if ((info == null) || (info.intent == null) || (info.intent.getComponent() == null)) {
                        continue;
                    }
                    ComponentName cn = info.intent.getComponent();
                    if ((cn != null) && packageNames.contains(cn.getPackageName())) {
                        cns.add(cn);
                    }
                } else if (tag instanceof FolderInfo) {
                    FolderInfo info = (FolderInfo) tag;
                    for (ShortcutInfo s : info.contents) {
                        if ((s == null) || (s.intent == null)) {
                            continue;
                        }
                        ComponentName cn = s.intent.getComponent();
                        if ((cn != null) && packageNames.contains(cn.getPackageName())) {
                            cns.add(cn);
                        }
                    }
                } else if (tag instanceof LauncherAppWidgetInfo) {
                    LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) tag;
                    if ((info == null) || ( info.providerName == null)) {
                        continue;
                    }
                    ComponentName cn = info.providerName;
                    if ((cn != null) && packageNames.contains(cn.getPackageName())) {
                        cns.add(cn);
                    }
                }
                /*YUNOS END*/
            }
        }

        // Remove all the things
        removeItemsByComponentName(cns);
    }

    /* YUNOS BEGIN */
    // ##date:2014/12/8 ##author:zhanggong.zg ##BugID:5601309
    /**
     * Clean up all gadgets in workspace. This method should be called when
     * launcher is being destroyed to avoid memory leak.
     */
    void cleanUpAllGadgets() {
        for (CellLayout layout : getWorkspaceAndHotseatCellLayouts()) {
            ViewGroup container = layout.getShortcutAndWidgetContainer();
            int childCount = container.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = container.getChildAt(i);
                if (view instanceof GadgetView) {
                    ((GadgetView) view).cleanUp();
                }
            }
        }
    }
    /* YUNOS END */

    //BugID:139616:uninstall an app in folder, the info about this app not removed from db
    void removeItemsByPackageNameForAppUninstall(final ArrayList<String> packages) {
        HashSet<String> packageNames = new HashSet<String>();
        packageNames.addAll(packages);

        // Just create a hash table of all the specific components that this will affect
        HashSet<ComponentName> cns = new HashSet<ComponentName>();
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layoutParent : cellLayouts) {
            ViewGroup layout = layoutParent.getShortcutAndWidgetContainer();
            int childCount = layout.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                View view = layout.getChildAt(i);
                Object tag = view.getTag();
                /*YUNOS BEGIN*/
                //##date:2013/12/04 ##author:hao.liuhaolh ##BugID:70318
                //NullPointer Exception fix
                if (tag instanceof ShortcutInfo) {
                    ShortcutInfo info = (ShortcutInfo) tag;
                    if ((info == null) || (info.intent == null) || (info.intent.getComponent() == null)) {
                        continue;
                    }
                    ComponentName cn = info.intent.getComponent();
                    if ((cn != null) && packageNames.contains(cn.getPackageName())) {
                        cns.add(cn);
                    }
                } else if (tag instanceof FolderInfo) {
                    FolderInfo info = (FolderInfo) tag;
                    for (ShortcutInfo s : info.contents) {
                        if ((s == null) || (s.intent == null)) {
                            continue;
                        }
                        ComponentName cn = s.intent.getComponent();
                        if ((cn != null) && packageNames.contains(cn.getPackageName())) {
                            cns.add(cn);
                        }
                    }
                } else if (tag instanceof LauncherAppWidgetInfo) {
                    LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) tag;
                    if ((info == null) || ( info.providerName == null)) {
                        continue;
                    }
                    ComponentName cn = info.providerName;
                    if ((cn != null) && packageNames.contains(cn.getPackageName())) {
                        cns.add(cn);
                    }
                }
                /*YUNOS END*/
            }
        }

        // Remove all the things
        if (cns.isEmpty() == true) {
            mLauncher.getModel().deleteItemsInDatabaseByPackageName(packages);
        } else {
            removeItemsByComponentName(cns);
            /* YUNOS BEGIN */
            // ## date: 2016/07/11 ## author: yongxing.lyx
            // ## BugID:8527293:view will re-add to workspace after uninstalled
            mLauncher.getModel().deleteItemsInDatabaseByPackageName(packages);
            /* YUNOS END */
        }
    }

    // Removes items that match the application info specified, when applications are removed
    // as a part of an update, this is called to ensure that other widgets and application
    // shortcuts are not removed.
    void removeItemsByApplicationInfo(final ArrayList<ApplicationInfo> appInfos) {
        // Just create a hash table of all the specific components that this will affect
        HashSet<ComponentName> cns = new HashSet<ComponentName>();
        for (ApplicationInfo info : appInfos) {
            cns.add(info.componentName);
        }

        // Remove all the things
        removeItemsByComponentName(cns);
    }

    /*YUNOS BEGIN*/
    //##date:2014/04/26##author:hao.liuhaolh@alibaba-inc.com##BugID:114988
    //find and remove one item folder after all restore app handled by appstore
    void removeItemsViewByItemInfo(final ArrayList<ItemInfo> items) {
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (final CellLayout layoutParent: cellLayouts) {
            final ViewGroup layout = layoutParent.getShortcutAndWidgetContainer();

            // Avoid ANRs by treating each screen separately
            post(new Runnable() {
                public void run() {
                    final ArrayList<View> childrenToRemove = new ArrayList<View>();
                    childrenToRemove.clear();

                    int childCount = layout.getChildCount();
                    ArrayList<ItemInfo> itemsToRemove = new ArrayList<ItemInfo>();
                    for (ItemInfo item: items) {
                        for (int j = 0; j < childCount; j++) {
                            final View view = layout.getChildAt(j);
                            Object tag = view.getTag();
                            final long id = ((ItemInfo)tag).id;
                            int itemtype = ((ItemInfo)tag).itemType;

                            if (id == item.id) {
                                Log.d(TAG, "find same id " + id);
                                childrenToRemove.add(view);
                                itemsToRemove.add(item);
                                continue;
                            } else if (itemtype == Favorites.ITEM_TYPE_FOLDER) {
                                ArrayList<View> childviews = (((FolderIcon)view).getFolder()).getItemsInReadingOrder();
                                for (View bubbleview : childviews){
                                    if (item.id == ((ItemInfo)(bubbleview.getTag())).id) {
                                        Log.d(TAG, "find in folder same id " + item.id);
                                        childrenToRemove.add(bubbleview);
                                        itemsToRemove.add((ItemInfo)(bubbleview.getTag()));
                                        continue;
                                    }
                                }
                            }
                        }
                    }

                    childCount = childrenToRemove.size();
                    CellLayout cellLayout = null;
                    boolean dockHit = false;
                    for (int j = 0; j < childCount; j++) {
                        View child = childrenToRemove.get(j);
                        // Note: We can not remove the view directly from CellLayoutChildren as this
                        // does not re-mark the spaces as unoccupied.
                        ItemInfo info = (ItemInfo)child.getTag();
                        cellLayout = (CellLayout) getChildAt(info.screen);
                        if (cellLayout != null) {
                            cellLayout.removePendingFlingDropDownItem(child);
                        }

                        if(info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                            dockHit = true;
                        }

                        layoutParent.removeViewInLayout(child);
                        if (child instanceof DropTarget) {
                            mDragController.removeDropTarget((DropTarget)child);
                        }

                        if (info.container == Favorites.CONTAINER_HIDESEAT) {
                            mLauncher.getHideseat().readOrderOnRemoveItem(info);
                        }

                        // ##date:2015/1/12 ##author:zhanggong.zg ##BugID:5697755
                        if (info.itemType == Favorites.ITEM_TYPE_FOLDER) {
                            mLauncher.removeFolder((FolderInfo) info);
                        }
                    }

                    if (childCount > 0) {
                        layout.requestLayout();
                        layout.invalidate();
                        if(dockHit) {
                            mLauncher.getHotseat().requestLayout();
                        }
                    }
                    for(ItemInfo item: itemsToRemove) {
                        items.remove(item);
                    }
                }
            });
        }
    }
    /*YUNOS END*/

    //BugID:5197690:bind remove item null pointer exception
    void removeItemsByItemInfo(final ArrayList<ItemInfo> items) {
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (final CellLayout layoutParent: cellLayouts) {
            final ViewGroup layout = layoutParent.getShortcutAndWidgetContainer();

            // Avoid ANRs by treating each screen separately
            post(new Runnable() {
                public void run() {
                    final ArrayList<View> childrenToRemove = new ArrayList<View>();

                    int childCount = layout.getChildCount();
                    ArrayList<ItemInfo> itemsToRemove = new ArrayList<ItemInfo>();
                    for (ItemInfo item: items) {
                        for (int j = 0; j < childCount; j++) {
                            final View view = layout.getChildAt(j);
                            Object tag = view.getTag();
                            final long id = ((ItemInfo)tag).id;
                            int itemtype = ((ItemInfo)tag).itemType;

                            if (id == item.id) {
                                Log.d(TAG, "find same id " + id);
                                childrenToRemove.add(view);
                                itemsToRemove.add(item);
                                final ItemInfo finalitem = item;
                                LauncherModel.deleteItemFromDatabase(mLauncher, finalitem);
                                continue;
                            } else if (itemtype == Favorites.ITEM_TYPE_FOLDER) {
                                ArrayList<View> childviews = (((FolderIcon)view).getFolder()).getItemsInReadingOrder();
                                for (View bubbleview : childviews){
                                    if (item.id == ((ItemInfo)(bubbleview.getTag())).id) {
                                        Log.d(TAG, "find in folder same id " + item.id);
                                        childrenToRemove.add(bubbleview);
                                        itemsToRemove.add((ItemInfo)(bubbleview.getTag()));
                                        final ItemInfo finalitem = item;
                                        LauncherModel.deleteItemFromDatabase(mLauncher, finalitem);
                                        continue;
                                    }
                                }
                            }
                        }
                    }

                    childCount = childrenToRemove.size();
                    CellLayout cellLayout = null;
                    boolean dockHit = false;
                    for (int j = 0; j < childCount; j++) {
                        View child = childrenToRemove.get(j);
                        // Note: We can not remove the view directly from CellLayoutChildren as this
                        // does not re-mark the spaces as unoccupied.
                        ItemInfo info = (ItemInfo)child.getTag();
                        cellLayout = (CellLayout) getChildAt(info.screen);
                        if (cellLayout == null) {
                            continue;
                        }
                        cellLayout.removePendingFlingDropDownItem(child);
                        if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                            /* YUNOS BEGIN */
                            // ## date: 2016/06/02 ## author: yongxing.lyx
                            // ## BugID:8358496:hotseat will add a removed clone
                            // app when drop a clone app to hotseat.
                            mLauncher.getHotseat().removeViewFromCacheList(child);
                            /* YUNOS END */
                            dockHit = true;
                        }

                        layoutParent.removeViewInLayout(child);
                        if (child instanceof DropTarget) {
                            mDragController.removeDropTarget((DropTarget)child);
                        }

                        if (info.container == Favorites.CONTAINER_HIDESEAT) {
                            mLauncher.getHideseat().readOrderOnRemoveItem(info);
                        }

                        // BugID:6180537 if the view was selected, remove it in
                        // selected views
                        if (mInEditing && layoutParent.getSelectedView() != null) {
                            Set<View> selectViews = layoutParent.getSelectedView();
                            if (selectViews.contains(child)) {
                                selectViews.remove(child);
                                layoutParent.invalidate();
                            }
                        }
                    }

                    if (childCount > 0) {
                        layout.requestLayout();
                        layout.invalidate();
                        if(dockHit) {
                            mLauncher.getHotseat().requestLayout();
                        }
                    }
                    for(ItemInfo item: itemsToRemove) {
                        items.remove(item);
                    }
                }
            });
        }
    }


    void removeItemsByComponentName(final HashSet<ComponentName> componentNames) {
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (final CellLayout layoutParent: cellLayouts) {
            final ViewGroup layout = layoutParent.getShortcutAndWidgetContainer();

            // Avoid ANRs by treating each screen separately
            post(new Runnable() {
                public void run() {
                    final ArrayList<View> childrenToRemove = new ArrayList<View>();
                    childrenToRemove.clear();

                    int childCount = layout.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        final View view = layout.getChildAt(j);
                        Object tag = view.getTag();

                        if (tag instanceof ShortcutInfo) {
                            final ShortcutInfo info = (ShortcutInfo) tag;
                            final Intent intent = info.intent;
                            /* YUNOS BEGIN */
                            // ##date:2014/07/29 ##author:hongchao.ghc
                            // ##BugID:141658
                            ComponentName name = null;
                            if (intent != null) {
                                name = intent.getComponent();
                            }
                            /* YUNOS END */
                            if (name != null) {
                                if (componentNames.contains(name)) {
                                    LauncherModel.deleteItemFromDatabase(mLauncher, info);
                                    LauncherModel.deleteItemFromAnotherTable(mLauncher, info);
                                    childrenToRemove.add(view);
                                }
                            }
                            //uninstall is finished and the item's view will be removed
                            info.mIsUninstalling = false;
                        } else if (tag instanceof FolderInfo) {
                            final FolderInfo info = (FolderInfo) tag;
                            final ArrayList<ShortcutInfo> contents = info.contents;
                            final int contentsCount = contents.size();
                            final ArrayList<ShortcutInfo> appsToRemoveFromFolder =
                                    new ArrayList<ShortcutInfo>();

                            for (int k = 0; k < contentsCount; k++) {
                                final ShortcutInfo appInfo = contents.get(k);
                                /*YUNOS BEGIN added by lxd #137571*/
                                if(appInfo.isEditFolderShortcut()) {
                                    continue;
                                }
                                /*YUNOS END*/
                                final Intent intent = appInfo.intent;
                                final ComponentName name = intent.getComponent();

                                if (name != null) {
                                    if (componentNames.contains(name)) {
                                        appsToRemoveFromFolder.add(appInfo);
                                    }
                                }
                            }
                            for (ShortcutInfo item: appsToRemoveFromFolder) {
                                info.remove(item);
                                LauncherModel.deleteItemFromDatabase(mLauncher, item);
                                LauncherModel.deleteItemFromAnotherTable(mLauncher, item);
                            }
                        } else if (tag instanceof LauncherAppWidgetInfo) {
                            final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) tag;
                            final ComponentName provider = info.providerName;
                            if (provider != null) {
                                if (componentNames.contains(provider)) {
                                    LauncherModel.deleteItemFromDatabase(mLauncher, info);
                                    childrenToRemove.add(view);
                                }
                            }
                        }
                    }

                    childCount = childrenToRemove.size();
                    CellLayout cellLayout = null;
                    boolean dockHit = false;
                    for (int j = 0; j < childCount; j++) {
                        View child = childrenToRemove.get(j);
                        // Note: We can not remove the view directly from CellLayoutChildren as this
                        // does not re-mark the spaces as unoccupied.
                        ItemInfo info = (ItemInfo)child.getTag();

                        /*YUNOS BEGIN*/
                        //##date:2014/04/29##author:hao.liuhaolh@alibaba-inc.com##BugID:115661
                        //icon in hotseat doesn't removed after uninstall from app store
                        if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                            cellLayout = mLauncher.getHotseat().getLayout();
                        } else if (info.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                            cellLayout = (CellLayout) mLauncher.getHideseat().getChildAt(info.screen);
                        } else {
                            cellLayout = (CellLayout) getChildAt(info.screen);
                        }
                        /*YUNOS END*/
                        /* YUNOS BEGIN */
                        // ##date:2014/2/17 ##author:yaodi.yd ##BugID:91192
                        // there is a nullpointerExcetion
                        if (cellLayout == null) {
                            continue;
                        }
                        /* YUNOS END */
                        cellLayout.removePendingFlingDropDownItem(child);
                        /* YUNOS BEGIN added by xiaodong.lxd #97959 2014/03/07*/
                        if(info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                            dockHit = true;
                        }
                        /* YUNOS END */

                        layoutParent.removeViewInLayout(child);
                        if (child instanceof DropTarget) {
                            mDragController.removeDropTarget((DropTarget)child);
                        }

                        if (info.container == Favorites.CONTAINER_HIDESEAT) {
                            mLauncher.getHideseat().readOrderOnRemoveItem(info);
                        }

                        // BugID:6180537 if the view was selected, remove it in
                        // selected views
                        if (mInEditing && layoutParent.getSelectedView() != null) {
                            Set<View> selectViews = layoutParent.getSelectedView();
                            if (selectViews.contains(child)) {
                                selectViews.remove(child);
                                layoutParent.invalidate();
                            }
                        }
                    }

                    if (childCount > 0) {
                        layout.requestLayout();
                        layout.invalidate();
                        /* YUNOS BEGIN added by xiaodong.lxd #97959 2014/03/07*/
                        if(dockHit) {
                            mLauncher.getHotseat().requestLayout();
                            /* YUNOS BEGIN */
                            // ##date:2014/8/7 ##author:hongchao.ghc ##BugID:146290
                            mLauncher.getHotseat().initViewCacheList();
                            /* YUNOS END */
                        }
                        /* YUNOS END */
                    }
                }
            });
        }

        // Clean up new-apps animation list
        final Context context = getContext();
        post(new Runnable() {
            @Override
            public void run() {
                String spKey = LauncherApplication.getSharedPreferencesKey();
                SharedPreferences sp = context.getSharedPreferences(spKey,
                        Context.MODE_PRIVATE);
                Set<String> newApps = sp.getStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY,
                        null);

                // Remove all queued items that match the same package
                if (newApps != null) {
                    synchronized (newApps) {
                        Iterator<String> iter = newApps.iterator();
                        while (iter.hasNext()) {
                            try {
                                Intent intent = Intent.parseUri(iter.next(), 0);
                                if (componentNames.contains(intent.getComponent())) {
                                    iter.remove();
                                }

                                // It is possible that we've queued an item to be loaded, yet it has
                                // not been added to the workspace, so remove those items as well.
                                ArrayList<ItemInfo> shortcuts;
                                shortcuts = LauncherModel.getWorkspaceShortcutItemInfosWithIntent(
                                        intent);
                                for (ItemInfo info : shortcuts) {
                                    LauncherModel.deleteItemFromDatabase(context, info);
                                }
                            } catch (URISyntaxException e) {}
                        }
                    }
                }
            }
        });
    }

    void updateShortcuts(ArrayList<ApplicationInfo> apps) {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        IconManager iconManager = ((LauncherApplication)mContext.getApplicationContext()).getIconManager();
        for (ShortcutAndWidgetContainer layout: childrenLayouts) {
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                final View view = layout.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ShortcutInfo) {
                    ShortcutInfo info = (ShortcutInfo) tag;
                    // We need to check for ACTION_MAIN otherwise getComponent() might
                    // return null for some shortcuts (for instance, for shortcuts to
                    // web pages.)
                    final Intent intent = info.intent;
                    if (intent == null) {
                        continue;
                    }
                    final ComponentName name = intent.getComponent();
                    if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
                            Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
                        final int appCount = apps.size();
                        for (int k = 0; k < appCount; k++) {
                            ApplicationInfo app = apps.get(k);
                            if (app.componentName.equals(name)) {
                                BubbleTextView shortcut = (BubbleTextView) view;
                                /*YUNOS BEGIN*/
                                //##date:2013/11/27 ##author:hao.liuhaolh
                                //update icon from theme first
                                //Context context = getContext().getApplicationContext();
                                //info.updateIcon(mIconCache);
                                Drawable icon = iconManager.getAppUnifiedIcon(info,null);
                                info.setIcon(icon);
                                /*YUNOS END*/
                                info.title = app.title.toString();
                                /* YUNOS BEGIN */
                                // ## date: 2016/06/03 ## author: yongxing.lyx
                                // ## BugID:8363218:show uninstalling after
                                // uninstall an app then install again quickly.
                                if (info.mIsUninstalling) {
                                    info.mIsUninstalling = false;
                                    Log.d(TAG, "-AC- set mIsUninstalling=false for:" + info);
                                }
                                /* YUNOS BEGIN */
                                BubbleController.applyToView(info, shortcut);
                            }
                        }
                    }
                }
            }
        }
    }

    /* YUNOS BEGIN */
    // ##date:2014/09/18 ##author:zhanggong.zg ##BugID:5244146
    // automatically remove widgets (and gadgets) for frozen apps

    public void removeWidgetsByShortcuts(Collection<ShortcutInfo> shortcuts) {
        if (shortcuts == null || shortcuts.isEmpty()) return;
        Set<String> pkgNames = new HashSet<String>();
        for (ShortcutInfo item : shortcuts) {
            if (item.intent == null) continue;
            ComponentName cmpt = item.intent.getComponent();
            if (cmpt == null || TextUtils.isEmpty(cmpt.getPackageName())) continue;
            pkgNames.add(cmpt.getPackageName());
        }
        removeWidgetsByPackageNames(pkgNames);
    }

    private void removeWidgetsByPackageNames(Collection<String> packageNames) {
        if (packageNames == null || packageNames.isEmpty()) return;
        Map<ItemInfo, View> itemsToRemove = new HashMap<ItemInfo, View>();
        for (ShortcutAndWidgetContainer layout: getAllShortcutAndWidgetContainers()) {
            for (int j = 0; j < layout.getChildCount(); j++) {
                final View view = layout.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof LauncherAppWidgetInfo) {
                    LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) tag;
                    String pkgName = info.providerName != null ? info.providerName.getPackageName() : null;
                    if (!TextUtils.isEmpty(pkgName) && packageNames.contains(pkgName)) {
                        itemsToRemove.put(info, view);
                    }
                }
            }
        }
        if (itemsToRemove.isEmpty()) return;
        for (ItemInfo item : itemsToRemove.keySet()) {
            Log.v(TAG, "prepare to remove frozen widget: " + item.title);
        }
        removeWidgets(itemsToRemove);
        itemsToRemove.clear();
        itemsToRemove = null;
    }

    private void removeWidgets(Map<ItemInfo, View> itemsToRemove) {
        if (itemsToRemove == null || itemsToRemove.isEmpty()) return;
        Log.d(TAG, "removeWidgets begin");
        // remove from database
        for (Entry<ItemInfo, View> entry : itemsToRemove.entrySet()) {
            ItemInfo item = entry.getKey();
            if (item instanceof LauncherAppWidgetInfo) {
                LauncherModel.deleteItemFromDatabase(mLauncher, item);
            }
        }
        // remove from workspace
        for (Entry<ItemInfo, View> entry : itemsToRemove.entrySet()) {
            ItemInfo item = entry.getKey();
            View view = entry.getValue();
            if (item instanceof LauncherAppWidgetInfo) {
                CellLayout layout = (CellLayout) getChildAt(getRealScreenIndex(item.screen));
                if (item.container != Favorites.CONTAINER_DESKTOP) {
                    Log.w(TAG, "removeWidgets incorrect state: item.container=" + item.container);
                    continue;
                }
                if (layout == null) {
                    Log.w(TAG, "removeWidgets incorrect state: item.screen=" + item.container);
                    continue;
                }
                layout.removePendingFlingDropDownItem(view);
                layout.removeViewInLayout(view);
                if (view instanceof DropTarget) {
                    mDragController.removeDropTarget((DropTarget) view);
                }
                layout.requestLayout();
                layout.invalidate();
            }
        }
        Log.d(TAG, "removeWidgets end");
    }

    /*YUNOS BEGIN*/

    void moveToDefaultScreen(boolean animate) {
        if (!isSmall()) {
            if (mDefaultPage != mCurrentPage) {
                if (animate) {
                    snapToPage(mDefaultPage);
                    // Xunhu start: Added by yangjie, Bug fixed for hotseat icon overlapped  @{{{
                    setCurrentPage(mDefaultPage);
                    // Xunhu end: Added by yangjie  @}}}
                } else {
                    setCurrentPage(mDefaultPage);
                }
            }
        }
        /* YUNOS BEGIN */
        // ##date:2013/12/13 ##author:zhangqiang.zq
        // 74550
        if (getChildAt(mDefaultPage) != null) {
            getChildAt(mDefaultPage).requestFocus();
        }
        /* YUNOS END */

    }

    @Override
    protected String getCurrentPageDescription() {
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        return String.format(getContext().getString(R.string.workspace_scroll_format),
                page + 1, getChildCount());
    }

    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }

    void setFadeForOverScroll(float fade) {
        if (!isScrollingIndicatorEnabled()) return;

        mOverscrollFade = fade;
        cancelScrollingIndicatorAnimations();
        // scrollIndicator.setAlpha(1 - fade);
    }

    private final Runnable mAddEmptyScreen = new Runnable () {
        @Override
        public void run() {
            Log.d(TAG, "sxsexe---->mAddEmptyScreen run add new screen ");

         // YUNOS BEGIN PB
            // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
            // ##BugID:(163418) ##date:2014/08/15
            // ##description: Added support for widget page
            /* YUNOS BEGIN */
            // 2014/08/05 author##yihuan.yyh@alibaba-inc.com
            // Fixed 145784, adde new feature page management: mark mEmptyScreenAdded if mPageManageFlag ==true

            if(getIconScreenCount() >= ConfigManager.getIconScreenMaxCount()) {
            /* YUNOS END */
                //Toast.makeText(mLauncher, R.string.toast_max_screen_count, Toast.LENGTH_SHORT).show();
                return;
            }
            View view = View.inflate(getContext(), R.layout.workspace_screen, null);
            if(view != null && mLongClickListener != null) {
                addView(view, getScreenCountExcludeWidgetPages());
                view.setOnLongClickListener(mLongClickListener);
            }
            CellLayout layout = (CellLayout) getChildAt(getIconScreenCount()
                    + getIconScreenHomeIndex() - 1);
            adjustCellLayoutPadding(layout);

            invalidatePageIndicator(true);
            mEmptyScreenAdded = true;

            /* YUNOS BEGIN */
            // ##module(Hideseat)
            // ##date:2014/3/24 ##author:wenliang.dwl@aliyun-inc.com ##BugId:103941
            // newly added celllayout must enter hideseatmode if hideseat is open
            CellLayout currentLayout = (CellLayout)getChildAt(getCurrentPage());
            CellLayout lastAddLayout = (CellLayout) getChildAt(getIconScreenCount()
                    + getIconScreenHomeIndex() - 1);
            if( currentLayout != null && lastAddLayout != null && currentLayout.isHideseatOpen ){
                lastAddLayout.enterHideseatMode();
            }
            /* YUNOS END */
        }
    };

    public void addEmptyScreen() {
        removeCallbacks(mAddEmptyScreen);
        post(mAddEmptyScreen);
    }
    /* YUNOS BEGIN */
    // ##date:2014/06/13 ##author:yangshan.ys
    // batch operations to the icons in folder
    public boolean addEmptyScreenSync() {
        if(getIconScreenCount() >= ConfigManager.getIconScreenMaxCount()) {
            return false;
        }
        View view = View.inflate(getContext(), R.layout.workspace_screen, Workspace.this);
        if(view != null && mLongClickListener != null) {
            view.setOnLongClickListener(mLongClickListener);
        }
        invalidatePageIndicator(true);
        CellLayout layout = (CellLayout) getChildAt(getIconScreenCount() + getIconScreenHomeIndex()
                - 1);
        adjustCellLayoutPadding(layout);

        /* YUNOS BEGIN */
        // ##module(Hideseat)
        // ##date:2014/3/24 ##author:wenliang.dwl@aliyun-inc.com ##BugId:103941
        // newly added celllayout must enter hideseatmode if hideseat is open
        CellLayout currentLayout = (CellLayout)getChildAt(getCurrentPage());
        CellLayout lastAddLayout = (CellLayout) getChildAt(getIconScreenCount()
                + getIconScreenHomeIndex() - 1);
        if( currentLayout != null && lastAddLayout != null && currentLayout.isHideseatOpen ){
            lastAddLayout.enterHideseatMode();
        }
        return true;
    }
    /* YUNOS END */

    private final Runnable mRemoveEmptyCell = new Runnable (){

        @Override
        public void run() {
            /* YUNOS BEGIN */
            // ##date:2014/2/24/ ##author:yaodi.yd ##BugID:95234:icons lost when screen unlocked
            if (isPlayingAnimation()) {
                removeCallbacks(mRemoveEmptyCell);
                postDelayed(mRemoveEmptyCell, 100);
                return;
            }
            /* YUNOS END */
            /*YUNOS BEGIN*/
            //##date:2014/7/8 ##author:zhangqiang.zq
            // aged mode
            if (mLauncher.getModel().isLoadingWorkspace()) {
                return;
            }
            /* YUNOS END */

            /*YUNOS BEGIN*/
            //##date:2014/10/10 ##author:zhangqiang.zq
            // screen manager
            if (mLauncher.isEmptyCellCanBeRemoved()) {
                Log.d(TAG, "can not remove empty cell!!!");
                return;
            }
            /* YUNOS END */

            /*YUNOS BEGIN added by xiaodong.lxd #107695*/
            /* added by hongxing.whx #117214*/
            if((mLauncher.getDragController() != null) && ( mLauncher.getDragController().isDragging() == true)) {
                return;
            }
            /*YUNOS END*/

            /* YUNOS BEGIN */
            // ##date:2015/7/30 ##author:zhanggong.zg ##BugID:6228028
            // added for edit mode animation
            if (mLauncher.getEditModeHelper().getRunningEditModeAnimations() != null) {
                return;
            }
            /* YUNOS END */

            final int count = getIconScreenCount();

            /*YUNOS BEGIN added by xiaodong.lxd #135639 at least maintain one screen*/
            if (count <= 1 && mWindowToken != null) {
                makesureAddScreenIndex(0);
                return;
            }
            /*YUNOS END*/

            //BugID:5214634:widget and item icon overlap after homeshell start
            if((mLauncher.getModel() != null) &&
                (mLauncher.getModel().isLoadingWorkspace() == true)) {
                return;
            }

            //BugID:5199250:long click invalid in restore state
            int start = 0;
            if((mLauncher.getModel() != null) &&
                (mLauncher.getModel().isEmptyCellCanBeRemoved() == false)) {
                Log.d(TAG, "in workspace loading, can not empty remove");
                //only check the last cell
                if (mEmptyScreenAdded == true) {
                    start = count - 1;
                } else {
                    start = count;
                }
            }
            int offSet = getIconScreenHomeIndex();

            boolean hasCelllayoutRemoved = false;
            for (int i = start + offSet; i < count + offSet; i++) {
                View child = getChildAt(i);
                if (getIconScreenCount() <= 1) {
                    break;
                }
                if (child != null && child instanceof CellLayout
                        && !((CellLayout)child).isFakeChild() && !((CellLayout)child).isOnlyChild()) {
                    //boolean isAllChildGone = isAllChildUnvisiable((CellLayout)child);
                    //Log.d(TAG, "sxsexe---->mRemoveEmptyCell isAllChildGone " + isAllChildGone + " ((CellLayout)child).hasChild() " + ((CellLayout)child).hasChild());
                    if(((CellLayout)child).hasChild() == false /*|| isAllChildGone*/
                            // YUNOS BEGIN PB
                            // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
                            // ##BugID:(163418) ##date:2014/08/15
                            // ##description: Added support for widget page
                            && !isWidgetPageView(child)) {
                            // YUNOS END PB
                        Log.d(TAG, "sxsexe---->mRemoveEmptyCell removeViewAt " + i + " count " + count);
                        hasCelllayoutRemoved = true;
                        ((CellLayout)getChildAt(i)).cancelFlingDropDownAnimation();
                        /*YUNOS BEGIN*/
                        //##date:2015/03/30 ##author:sunchen.sc ##BugID:5735130
                        //Remove occupy array from LauncherModel ArrayList when CellLayout removed from workspace
                        // commented by xiaodong.lxd, call removeWorkspaceOccupiedFromModel in Worksapce.onChildRemoved
                        // ((CellLayout)getChildAt(i)).removeWorkspaceOccupiedFromModel();
                        // ((CellLayout)getChildAt(i)).onRemove();
                        /*YUNOS END*/
                        removeViewAt(i);
                        invalidatePageIndicator(true);
                        if(mCurrentPage > i) {
                            mCurrentPage--;
                        }
                        ///xunhu:add by zch for hotseat overlap when add empty screen in editmode then press the home button and unlock screen twice at 2016-10-19{&&
                        if(!mInEditing&&isWidgetPageView(getChildAt(mCurrentPage))&&mCurrentPage>0){
                            mCurrentPage--;
                        }
                        ///&&}
                        /* YUNOS BEGIN */
                        // ## modules(Home Shell)
                        // ## date: 2016/03/10 ## author: wangye.wy
                        // ## BugID: 7945871: re-order all screens
                        if (mInEditing) {
                            mLauncher.getEditModeHelper().onCellLayoutAddOrDelete(false, (CellLayout)child, i);
                            CellLayout currentChild = (CellLayout) getChildAt(mCurrentPage);
                            if (currentChild != null && currentChild.isFakeChild()) {
                                mCurrentPage--;
                            }
                        }
                        /* YUNOS END */
                        //BugID:118844:empty celllayout remove after restore but no update in launchermodel
                        if (getIconScreenIndex(i) < (count - 1)) {
                            LauncherModel.checkEmptyScreen(mContext.getApplicationContext(),
                                    getIconScreenIndex(i));
                        }
                        /*YUNOS END*/
                        if(!isPageMoving()){
                            setCurrentPage(mCurrentPage);
                            onPageEndMoving();
                        }
                        i--;
                    }
                }
            }

            //BugID:6717591:update screen to avoid wrong value
            //during celllayout removed
            if (hasCelllayoutRemoved) {
                int newCount = getIconScreenCount();
                for (int i = start + offSet; i < newCount + offSet; i++) {
                    View child = getChildAt(i);
                    if (child != null && child instanceof CellLayout) {
                        ((CellLayout)child).updateTagScreen(i);
                    }
                }
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/03/10 ## author: wangye.wy
                // ## BugID: 7945871: re-order all screens
                if (mInEditing) {
                    CellLayout lastChild = (CellLayout) getChildAt(newCount - 1);
                    if (lastChild != null && !lastChild.isFakeChild()) {
                        boolean saved = mEmptyScreenAdded;
                        addEmptyScreenSync();
                        mEmptyScreenAdded = saved;
                        lastChild = (CellLayout) getChildAt(newCount);
                        if (lastChild != null) {
                            lastChild.addEditBtnContainer();
                            lastChild.setEditBtnContainerMode(true);
                            initScreen(newCount);
                        }
                    }
                }
                /* YUNOS END */
            }
            mEmptyScreenAdded = false;
            mDropTargetView = null;
        }
    };

    public void removeEmptyCellLayoutSync() {
        if (mRemoveEmptyCell != null) {
            mRemoveEmptyCell.run();
        }
    }

    public void checkAndRemoveEmptyCell() {
        /* YUNOS BEGIN */
        // author yaodi.yd ## if is playing unlock screen animation
        if (isPlayingAnimation()) {
            return;
        }
        /* YUNOS END */

        /* YUNOS BEGIN */
        // ##date:2014/3/13 ##author:zhangqiang.zq
        // bugID:99944,104983
        // empty screen is useful, and delay check empty screen.
        if (mLauncher.isEmptyCellCanBeRemoved()) {
            return;
        }
        /* YUNOS END */

        /* YUNOS BEGIN */
        //author lxd # if page is still moving, don't remove empty cell
        if(isPageMoving()) {
            return;
        }
        /* YUNOS END */

        /*YUNOS BEGIN*/
        //##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
        if (mLauncher.getModel().isLoadingWorkspace()) {
            return;
        }
        /* YUNOS END */

        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: re-order all screens
        if(mInEditing && !mReOrderAll) {
            return;
        }
        mReOrderAll = false;
        /* YUNOS END */

        removeCallbacks(mRemoveEmptyCell);
        post(mRemoveEmptyCell);
    }

    private boolean mReOrderAll = false;
    public void setReOrderAll(boolean reOrderAll) {
        mReOrderAll = reOrderAll;
    }

/*    public void bindRemoveScreen(final int screen) {
        post(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "sxsexe---->bindRemoveScreen removeViewAt " + screen + " getChildCount() " + getChildCount());
                if(screen >= getChildCount()) {
                    return;
                }

                for(int i = screen; i < getChildCount(); i ++) {
                    View child = getChildAt(i);

                    if(child != null && child instanceof CellLayout) {
                        if(((CellLayout)child).hasChild() == false) {
                            removeViewAt(i);
                            if(mCurrentPage > i)
                                mCurrentPage--;
                            setCurrentPage(mCurrentPage);
                            i--;
                        }
                    }
                }
            }
        });
    }*/

    public void makesureAddScreenIndex(int screen) {
        CellLayout currentPage = (CellLayout) getChildAt(getCurrentPage());
        boolean hideseatMode = currentPage != null ? currentPage.isHideseatOpen : false;

        if (FeatureUtility.hasFullScreenWidget() && !mLauncher.isWorkspaceLoading()) {
            removeWidgetPages();
        }

        while (screen >= getIconScreenCount()) {
            View view = View.inflate(getContext(), R.layout.workspace_screen, null);
            if(view != null && mLongClickListener != null) {
                addView(view, getScreenCountExcludeWidgetPages());
                view.setOnLongClickListener(mLongClickListener);
            }

            CellLayout layout = (CellLayout) getChildAt(getIconScreenCount()
                    + getIconScreenHomeIndex() - 1);
            adjustCellLayoutPadding(layout);

            invalidatePageIndicator(true);

            /* YUNOS BEGIN */
            // ##date:2014/11/7 ##author:zhanggong.zg ##BugID:5441448,5431906
            // newly added page must enter hideseat mode if hideseat is open
            if (hideseatMode) {
                CellLayout lastPage = (CellLayout) getChildAt(getIconScreenCount()
                        + getIconScreenHomeIndex() - 1);
                lastPage.enterHideseatMode();
            }

            if(mInEditing) {
                mLauncher.getEditModeHelper().onCellLayoutAddOrDelete(true, (CellLayout) view, getCellLayoutIndex((CellLayout) view));
            }
            /* YUNOS END */
        }
        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(163418) ##date:2014/08/15
        // ##description: Added support for widget page
        if (FeatureUtility.hasFullScreenWidget() && !mLauncher.isWorkspaceLoading()) {
            makeSureWidgetPages();
        }
        // YUNOS END PB
    }

    public void addToDragItems(View dragItem) {
        if(mDragItems == null)
            mDragItems = new ArrayList<View>();

        ItemInfo info = (ItemInfo)dragItem.getTag();

        //clear the same view of which parent has changed
        for(View cell : mDragItems) {
            ItemInfo tmpInfo = (ItemInfo)cell.getTag();
            if(info.id == tmpInfo.id) {
                mDragItems.remove(cell);
                LauncherModel.removeDragInfo(cell);
                break;
            }
        }

        Log.d(TAG, "sxsexe----------------->addToDragItems cell " + info + " parent " + dragItem.getParent());
        mDragItems.add(dragItem);
    }

    //BugID: 5324557:icon still in installing status after move to hotseat
    public View searchDragItemFromList(ItemInfo itemInfo) {
        if(mDragItems != null && mDragItems.size() > 0) {
            for(View cell : mDragItems) {
                ItemInfo info = (ItemInfo)cell.getTag();
                if(info.id == itemInfo.id) {
                    Log.d(TAG, "----------------->find DragItemFromList info " + info.title);
                    return cell;
                }
            }
        }
        Log.d(TAG, "----------------->no find in DragItemFromList");
        return null;
    }

    public View getDragItemFromList(ItemInfo itemInfo, boolean remove) {
        if(mDragItems != null && mDragItems.size() > 0) {
            for(View cell : mDragItems) {
                ItemInfo info = (ItemInfo)cell.getTag();
                if(info.id == itemInfo.id) {
                    if(remove) {
                        mDragItems.remove(cell);
                        LauncherModel.removeDragInfo(cell);
                    }
                    Log.d(TAG, "sxsexe111----------------->get DragItemFromList info " + info.title);
                    return cell;
                }
            }
        }
        /*YUNOS BEGIN modified by xiaodong.lxd#113493*/
        if(mDragInfo != null) {
            return mDragInfo.cell;
        } else {
            Log.e(TAG, "sxsexe------------------->mDragInfo is null and gragList is empty itemInfo " + itemInfo);
            return null;
        }
        /*YUNOS END*/
    }

    public void removeDragItemFromList(ItemInfo itemInfo) {
        if(mDragItems != null && mDragItems.size() > 0) {
            for(View tmpCell : mDragItems) {
                ItemInfo info = (ItemInfo)tmpCell.getTag();
                if(info.id == itemInfo.id) {
                    Log.d(TAG, "sxsexe111----------------->remove DragItemFromList info " + info.title + " parent " + tmpCell.getParent());
                    mDragItems.remove(tmpCell);
                    LauncherModel.removeDragInfo(tmpCell);
                    return;
                }
            }
        }
    }

    public void cleanDragItemList() {
        if(mDragItems != null && mDragItems.size() > 0) {
            mDragItems.clear();
            mDragItems = null;
            LauncherModel.clearDragInfo();
        }
    }

    /**
     * @return Is it playing unlock Animation
     */
    public boolean isPlayingAnimation() {
        return LockScreenAnimator.getInstance(mLauncher).isRuning();
    }

    public void setAllItemsOfCurrentPageVisibility(int visibility) {
        int currentPageIndex = getCurrentPage();
        final CellLayout currentPage = (CellLayout) getChildAt(currentPageIndex);
        ShortcutAndWidgetContainer container = null;
        if (currentPage != null) {
            container = currentPage.getShortcutAndWidgetContainer();
        }
        int childCount = 0;
        if (container != null) {
            childCount = container.getChildCount();
        }
        for (int i = 0; i < childCount; i++) {
            final View child = container.getChildAt(i);
            child.setVisibility(visibility);
            child.setAlpha(1);
        }
        CellLayout hotseat = mLauncher.getHotseat().getLayout();
        ShortcutAndWidgetContainer hotseatContainer = null;
        if (hotseat != null) {
            hotseatContainer = hotseat.getShortcutAndWidgetContainer();
        }
        int hotseatChildCount = 0;
        if (hotseatContainer != null) {
            hotseatChildCount = hotseatContainer.getChildCount();
        }
        for(int i=0;i<hotseatChildCount;i++) {
            final View child = hotseatContainer.getChildAt(i);
            child.setVisibility(visibility);
            child.setAlpha(1);
        }
    }
    /* YUNOS END */

    public void dismissFolder(FolderIcon folder) {
        // BugID:5305301,wenliang.dwl,get FolderIcon from parameter instead of mDragInfoDelete
        if (folder == null)
            return;// return if folder is N/A
        FolderInfo folderInfo = (FolderInfo) folder.getTag();

        CellLayout layout = mLauncher.getCellLayout(folderInfo.container, folderInfo.screen);
        //BugID:5632072:in special case layout is removed and layout is null
        if (layout != null) {
            layout.removeView(folder);
        }
        LauncherModel.deleteItemFromDatabase(mLauncher, folderInfo);

        if (folder instanceof DropTarget) {
            mDragController.removeDropTarget((DropTarget) folder);
        }
        mLauncher.removeFolder(folderInfo);

        Folder f = folder.getFolder();
        ShortcutAndWidgetContainer container = f.getContent().getShortcutAndWidgetContainer();

        container.removeAllViews();
        container = layout.getShortcutAndWidgetContainer();
        //int cellXY[] = new int[2];
        int curScreen = getIconScreenIndex(getCurrentPage());

        int reqCount = f.mInfo.contents.size();
        CellLayout.LayoutParams fromLp = container.buildLayoutParams(2, -1, 1, 1);
        ArrayList<ScreenPosition> posList = new ArrayList<ScreenPosition>(reqCount);

        Log.d(TAG, "folder's screen is " + folderInfo.screen + " reqCount " + reqCount);
        if (folderInfo.container != Favorites.CONTAINER_DESKTOP) {
            int currentIconScreen = getIconScreenIndex(getCurrentPage());
            if ((currentIconScreen < 0) ||
                 (currentIconScreen >= ConfigManager.getIconScreenMaxCount())){
                currentIconScreen = 1;
            }
            LauncherModel.getEmptyPosListAndOccupy(posList, currentIconScreen, ConfigManager.getIconScreenMaxCount() - 1, reqCount);
            //BugID:5583238:icon lost during dismiss folder
            if (currentIconScreen > 1) {
                LauncherModel.getEmptyPosListAndOccupy(posList, currentIconScreen - 1, 1, reqCount);
            }
        } else if (folderInfo.screen == 0) {
            LauncherModel.getEmptyPosListAndOccupy(posList, 0, ConfigManager.getIconScreenMaxCount() - 1, reqCount);
        } else if (folderInfo.screen == 1) {
            LauncherModel.getEmptyPosListAndOccupy(posList, 1, ConfigManager.getIconScreenMaxCount() - 1, reqCount);
        } else if (folderInfo.screen == ConfigManager.getIconScreenMaxCount() - 1) {
            LauncherModel.getEmptyPosListAndOccupy(posList, folderInfo.screen, 1, reqCount);
        } else {
            LauncherModel.getEmptyPosListAndOccupy(posList, folderInfo.screen, ConfigManager.getIconScreenMaxCount() - 1, reqCount);
            LauncherModel.getEmptyPosListAndOccupy(posList, folderInfo.screen - 1, 1, reqCount);
        }
        Log.d(TAG, "posList size is " + posList.size());

        for (ShortcutInfo info :  folderInfo.contents) {
            //int screen = findCellForSpan(cellXY, curScreen, info.spanX, info.spanY);
            ScreenPosition screenPos = null;
            if (!posList.isEmpty()) {
                screenPos = posList.remove(0);
            }
            if (screenPos == null) {
                if ((info.itemType == Favorites.ITEM_TYPE_APPLICATION) ||
                    (info.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION)) {
                    info.itemType = Favorites.ITEM_TYPE_NOSPACE_APPLICATION;
                    info.container = Favorites.CONTAINER_DESKTOP;
                    info.screen = -1;
                    info.cellX = -1;
                    info.cellY = -1;
                    LauncherModel.updateItemInDatabase(mLauncher, info);
                } else {
                    LauncherModel.deleteItemFromDatabase(mLauncher, info);
                }
                continue;
            }

            info.setPosition(screenPos);

            LauncherModel.addOrMoveItemInDatabase(mLauncher, info, Favorites.CONTAINER_DESKTOP, info.screen, info.cellX, info.cellY);
            View v = mLauncher.createShortcut(R.layout.application, null, info);
            addInScreen(v, Favorites.CONTAINER_DESKTOP, info.screen, info.cellX, info.cellY, info.spanX, info.spanY);

            if (info.screen == curScreen) {
                CellLayout.LayoutParams toLp = container.buildLayoutParams(info.cellX, info.cellY, info.spanX, info.spanY);
                ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v,
                        PropertyValuesHolder.ofFloat("translationX", (fromLp.x - toLp.x), 0),
                        PropertyValuesHolder.ofFloat("translationY", (fromLp.y - toLp.y), 0));
                a.setDuration(500);
                a.start();
            } else {
                String title = "";
                if (info.title != null) {
                    title = info.title.toString();
                }
                ((CellLayout) getPageAt(getRealScreenIndex(info.screen)))
                        .addPengindFlingDropDownTarget(v, 0, 0, true, title, 0, 0);
            }
        }
        /* YUNOS BEGIN */
        // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
        // Release not occupied places
        for (ScreenPosition sp : posList) {
            LauncherModel.releaseWorkspacePlace(sp.s, sp.x, sp.y);
        }
        LauncherModel.dumpUIOccupied();
        /* YUNOS END */
        Log.d(TAG, "dismissFolder finish");

        checkAndRemoveEmptyCell();
    }

//    private int findCellForSpan(int[] cellXY, int start, int spanX, int spanY) {
//        final int N = getPageCount();
//        for (int i = start; i < N; i++) {
//            CellLayout layout = (CellLayout) getPageAt(i);
//            if (layout.findCellForSpan(cellXY, spanX, spanY)) {
//                return i;
//            }
//        }
//
//        int defautlStartScreen = ConfigManager.DEFAULT_FIND_EMPTY_SCREEN_START;
//        if (start > defautlStartScreen) {
//            for (int i = defautlStartScreen; i < start; i++) {
//                CellLayout layout = (CellLayout) getPageAt(i);
//                if (layout.findCellForSpan(cellXY, spanX, spanY)) {
//                    return i;
//                }
//            }
//        }
//
//        return -1;
//    }

    public void arrangeAllPages(){
        arrangeCurrentPageAsync();
        int offSet = getIconScreenHomeIndex();
        int screenCount = getIconScreenCount();
        for (int i = offSet; i < screenCount + offSet; i++) {
            if( i == getCurrentPage() ) continue;
            arrangePageAsync(i);
        }
    }
     /* YUNOS BEGIN */
     // ##date:2014/08/12 ##author:hongchao.ghc ##BugID:146637
    public void arrangeAllPagesPostDelay(final HomeShellSetting.Callback callback){
        postDelayed(new Runnable() {
            @Override
            public void run() {
                int offSet = getIconScreenHomeIndex();
                int screenCount = getIconScreenCount();
                for (int i = offSet; i < screenCount + offSet; i++) {
                    arrangePage(i);
                }
                callback.onFinish();
            }
        },200);
    }
    /* YUNOS END */
    public void arrangePageAsync( final int index ){
        post(new Runnable() {
            @Override
            public void run() {
               arrangePage(index);
            }
        });
    }

    /**
     * Arrange 'index' page's icons one by one and update UI.
     */
    public void arrangePage( int index ){
        CellLayout cl = (CellLayout)getChildAt(index);
        int maxX = cl.getCountX();
        int maxY = cl.getCountY();

        //first screen differ from the others
        int startY, endY, deltaY;
        if (index == getIconScreenHomeIndex()) {
            startY = maxY - 1;
            endY   = -1;
            deltaY = -1;
        }else{
            startY = 0;
            endY   = maxY;
            deltaY = 1;
        }

        for( int y = startY; y != endY; y += deltaY ){
            for( int x = 0; x < maxX; x++ ){
                View view = cl.getChildAt(x, y);
                if( view == null ) continue;
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams)view.getLayoutParams();
                ItemInfo info = (ItemInfo)view.getTag();
                if (info == null || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                        || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_GADGET) {
                    continue;
                }
                int[] position = findArrangePosition(index, lp.cellX, lp.cellY, maxX, maxY);

                cl.onMove(view, position[0], position[1], 1, 1);

                lp.useTmpCoords = false;
                info.cellX = lp.cellX = position[0];
                info.cellY = lp.cellY = position[1];

                view.setLayoutParams(lp);
                LauncherModel.updateItemInDatabase(mLauncher, info);
            }
        }
    }


    public void arrangeCurrentPageAsync(){
        post(new Runnable() {
            @Override
            public void run() {
                arrangeCurrentPage();
            }
        });
    }

    public void arrangeCurrentPage(){
        arrangePage(getCurrentPage());
    }

    /**
     * find the first empty cell from top to bottom, from left to right
     */
    public int[] findArrangePosition(int screen, int x, int y, int maxX, int maxY){
        CellLayout cl = (CellLayout)getChildAt(screen);

        int startY, endY, deltaY;
        if( screen != getIconScreenHomeIndex() ){
            startY = 0;
            endY   = maxY;
            deltaY = 1;
        }else{
            startY = maxY - 1;
            endY   = -1;
            deltaY = -1;
        }

        for( int indexY = startY; indexY != endY; indexY += deltaY ){
            for( int indexX = 0; indexX < maxX; indexX++ ){
                if( indexX == x && indexY == y ) return new int[]{x,y};
                if( cl.getChildAt(indexX, indexY) == null ) {
                    return new int[]{indexX,indexY};
                }
            }
        }
        return null;
    }
    /* YUNOS BEGIN */
    // ##date:2014/06/03 ##author:yangshan.ys
    // batch operations to the icons in folder
    public void updateWorkspaceAfterDelItems(List<ItemInfo> removedItems) {
        for ( int i = 0; i < removedItems.size(); i++) {
            ItemInfo item = removedItems.get(i);
            CellLayout layout = null;
            if(item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                layout = (CellLayout) getChildAt(getRealScreenIndex(item.screen));
            } else if(item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                layout = (CellLayout) mLauncher.getHotseat().getLayout();
            } else if (item.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                layout = (CellLayout)mLauncher.getHideseat().getChildAt(item.screen);
            }
            if(layout == null) {
                Log.e(TAG,"fail when find the cellLayout in workspace");
                return;
            }
            ShortcutAndWidgetContainer container = layout.getShortcutAndWidgetContainer();
            int childCount = container.getChildCount();
            for(int j = 0; j < childCount; j++) {
                View view = container.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ShortcutInfo) {
                    ItemInfo info = (ItemInfo) tag;
                    if(item.id == info.id) {
                        layout.removeView(view);
                        layout.markCellsAsUnoccupiedForView(view);
                        break;
                    }
                }
            }
        }
    }
    /* YUNOS END */

    private static int mIconScreenStartIndex = 1;
    /**
     *
     * @return icon screen's start index
     */
    public int getIconScreenHomeIndex() {
        return mLauncher.isSupportLifeCenter() ? mIconScreenStartIndex : 0;
    }

    /**
     *
     * @param allindex
     *            : index in view tree
     * @return index in icon screens
     */
    public int getIconScreenIndex(int allindex) {
        return mLauncher.isSupportLifeCenter() ? allindex - mIconScreenStartIndex : allindex;
    }

    /**
     *
     * @param oldscreen
     *            : index in icon screens
     * @return index in view tree
     */
    public int getRealScreenIndex(int oldscreen) {
        return mLauncher.isSupportLifeCenter() ? oldscreen + mIconScreenStartIndex : oldscreen;
    }

    /**
     *
     * @return icon screen count
     */
    public int getIconScreenCount() {
        int iconScreenCount = mLauncher.isSupportLifeCenter() ? getChildCount() - mIconScreenStartIndex : this.getChildCount();
        if(FeatureUtility.hasFullScreenWidget()) {
            int count = getChildCount();
            for(int i = 0; i < count ; i++) {
                CellLayout layout = (CellLayout) getChildAt(i);
                if(layout.isWidgetPage()) {
                    iconScreenCount -= 1;
                }
            }
        }
        return iconScreenCount;
    }
    /* YUNOS BEGIN */
    // ##date:2014/10/16 ##author:yangshan.ys##5157204
    // for 3*3 layout
    public void adjustToThreeLayout() {
        refreshMaxDistanceForFolderCreation();
        int offSet = getIconScreenHomeIndex();
        int screenCount = getIconScreenCount();
        for (int i = offSet; i < offSet + screenCount; i++) {
            ((CellLayout) getChildAt(i)).adjustToThreeLayout();
        }
    }

    public void adjustFromThreeLayout() {
        refreshMaxDistanceForFolderCreation();
        int offSet = getIconScreenHomeIndex();
        int screenCount = getIconScreenCount();
        for (int i = offSet; i < offSet + screenCount; i++) {
            ((CellLayout) getChildAt(i)).adjustFromThreeLayout();
        }
    }

    private void refreshMaxDistanceForFolderCreation() {
        float factor = mLauncher.getIconManager().supprtCardIcon() ? FOLDER_CREATION_FACTOR_CARDMODE
                : FOLDER_CREATION_FACTOR;
        if (AgedModeUtil.isAgedMode()) {
            mMaxDistanceForFolderCreation = (factor * getResources().getDimensionPixelSize(
                    R.dimen.app_icon_size) * AgedModeUtil.SCALE_RATIO_FOR_AGED_MODE);
        } else {
            mMaxDistanceForFolderCreation = (factor * getResources().getDimensionPixelSize(
                    R.dimen.app_icon_size));
        }

    }
    /* YUNSO END */

    /* YUNOS BEGIN */
    //## modules(Home Shell): [Category]
    //## date: 2015/07/30 ## author: wangye.wy
    //## BugID: 6221911: category on desk top
    private boolean mInCategory;
    public static final float CATEGORY_SCALE = 0.8f;

    public boolean isInCategory() {
        return mInCategory;
    }

    public void enterLauncherCategoryMode() {
        mInCategory = true;
        /* YUNOS BEGIN */
        //20160722 by chusheng.xcs
        setLayoutScale(CATEGORY_SCALE);
        /* YUNOS END */
        final int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            initCategoryScreen(i);
        }
    }

    public void exitLauncherCategoryMode() {
        mInCategory = false;
        final int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            initCategoryScreen(i);
        }
        setChildrenEditMode();
        mLauncher.getEditModeHelper().reloadItems();
    }

    private void initCategoryScreen(int i) {
        final CellLayout cl = (CellLayout) getChildAt(i);
        if (cl != null) {
            if (mInCategory) {
                View header = cl.findViewById(R.id.header);
                if (header != null) {
                    cl.removeHeader(header);
                }
                cl.setTranslationY(sTranslationY + (sHeaderHeight / 2));
            } else {
                cl.setClipChildren(false);
                cl.setClipToPadding(false);
                View header = cl.findViewById(R.id.header);
                if (header == null) {
                    header = View.inflate(getContext(), R.layout.cell_layout_header, null);
                    cl.addView(header);
                }
                header.setOnLongClickListener(mLongClickListener);
                cl.setTranslationY(sTranslationY);
            }
            Drawable drawable = cl.getBackground();
            if (drawable == null) {
                drawable = getResources().getDrawable(R.drawable.em_celllayout_bg);
                cl.setBackground(drawable);
            }
            cl.setBackgroundAlpha(255);
            resetScreenParams(cl);
            float cellTransX = getChildAt(getIconScreenHomeIndex()).getWidth() * ((1.0f - sEditScale) / 2);
            cl.setTranslationX(cellTransX);
            cl.setScaleX(sEditScale);
            cl.setScaleY(sEditScale);
            ShortcutAndWidgetContainer container = cl.getShortcutAndWidgetContainer();
            container.setTranslationY(mInCategory ? 0 : sHeaderHeight / sEditScale);
        }
    }
    /* YUNSO END */

    /*YUNOS BEGIN  for LauncherEditMode*/
    private boolean mInEditing;
    public static float sEditScale;
    public static float sHeaderHeight;
    private static float sTranslationY;
    private int mEditModePageSpacing;
    public static int sHeight;

    public void enterLauncherEditMode() {
        mInEditing = true;
        setPageSpacing(mEditModePageSpacing);
        /* YUNOS BEGIN */
        // ##date:2015/05/12 ##author:yongxing.lyx
        // BugID:8214476:maybe RenderNode.setOutline() was reentrant
        try {
            setLayoutScale(sEditScale);
        } catch (java.lang.IllegalArgumentException iae) {
            iae.printStackTrace();
        }
        /* YUNOS END */

        final int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            if (i != mCurrentPage) {
                initScreen(i);
            }
            ///ZDP - Modified on 2016 - 07 - 11 to fix the mal-display in edit mode when the animation is off @{
            else{
                final CellLayout cl = (CellLayout) getChildAt(i);
                final float diff = (1 - sEditScale) / 2;
                if(cl != null){
                    float cellTransX = getChildAt(getIconScreenHomeIndex()).getWidth() * diff;
                    float cellTransY = sTranslationY;
                    cl.setTranslationX(mInEditing ? cellTransX : 0);
                    cl.setTranslationY(mInEditing ? cellTransY : 0);
                    cl.setScaleX(mInEditing ? sEditScale : 1.0f);
                    cl.setScaleY(mInEditing ? sEditScale : 1.0f);
                    ShortcutAndWidgetContainer container = cl.getShortcutAndWidgetContainer();
                    container.setTranslationY(mInEditing ? sHeaderHeight / sEditScale : 0);
                }
            }
            ///@}
        }
        setChildrenEditMode(mInEditing);
    }

    public void exitLauncherEditMode() {
        mInEditing = false;
        setPageSpacing(0);
        setLayoutScale(1.0f);

        final int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            if (i != mCurrentPage) {
                initScreen(i);
            }
        }

        setChildrenEditMode(mInEditing);
    }

    public List<Animator> getCurrentPageAnimList(final boolean editMode) {
        sHeight = getChildAt(getIconScreenHomeIndex()).getHeight();
        List<Animator> animList = new ArrayList<Animator>(5);
        CellLayout cl = (CellLayout) getChildAt(mCurrentPage);
        if(cl.isWidgetPage()) {
            setCurrentPage(getIconScreenCount());
            cl = (CellLayout) getChildAt(mCurrentPage);
        }
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: header in cell layout
        if (editMode) {
            mInEditing = editMode;
            cl.setClipChildren(false);
            cl.setClipToPadding(false);
            View header = View.inflate(getContext(), R.layout.cell_layout_header, null);
            header.setOnLongClickListener(mLongClickListener);
            cl.addView(header);
        } else {
            View header = cl.findViewById(R.id.header);
            if (header != null) {
                cl.removeHeader(header);
            }
        }
        /* YUNOS END */
        resetScreenParams(cl);
        int iconScreenIndex = getIconScreenHomeIndex();
        float cellTransX = getChildAt(iconScreenIndex).getWidth() * ((1.0f - sEditScale) / 2);
        float cellTransY = sTranslationY;

        ObjectAnimator translationX = ObjectAnimator.ofFloat(cl, "translationX", editMode ? 0 : cellTransX, editMode ? cellTransX : 0);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(cl, "translationY", editMode ? 0 : cellTransY, editMode ? cellTransY : 0);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cl, "scaleY", editMode ? 1 : sEditScale, editMode ? sEditScale : 1);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cl, "scaleX", editMode ? 1 : sEditScale, editMode ? sEditScale : 1);
        ShortcutAndWidgetContainer container = cl.getShortcutAndWidgetContainer();
        ObjectAnimator containerTranslationY = ObjectAnimator.ofFloat(container, "translationY",
                editMode ? 0 : sHeaderHeight / sEditScale, editMode ? sHeaderHeight / sEditScale : 0);
        Drawable drawable = cl.getBackground();
        if (drawable == null) {
            drawable = getResources().getDrawable(R.drawable.em_celllayout_bg);
            cl.setBackground(drawable);
        }
        final CellLayout cellLayout = cl;
        ObjectAnimator bgAnimator = ObjectAnimator.ofInt(drawable, "alpha", editMode ? 0 : 255, editMode ? 255 : 0);
        bgAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (editMode) {
                    cellLayout.setClipChildren(false);
                    cellLayout.setClipToPadding(false);
                    cellLayout.setBackgroundAlpha(255);
                } else {
                    cellLayout.setBackground(null);
                }
            }
        });

        animList.add(translationX);
        animList.add(translationY);
        animList.add(scaleX);
        animList.add(scaleY);
        animList.add(containerTranslationY);
        animList.add(bgAnimator);
        return animList;
    }

    private void initScreen(int i) {
        final CellLayout cl = (CellLayout) getChildAt(i);
        final float diff = (1 - sEditScale) / 2;
        if (cl != null) {
            if (mInEditing) {
                Drawable drawable = cl.getBackground();
                if (drawable == null) {
                    drawable = getResources().getDrawable(R.drawable.em_celllayout_bg);
                    cl.setBackground(drawable);
                }
                cl.setBackgroundAlpha(255);
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: header in cell layout
                if (!cl.isFakeChild()) {
                    cl.setClipChildren(false);
                    cl.setClipToPadding(false);
                    View header = View.inflate(getContext(), R.layout.cell_layout_header, null);
                    header.setOnLongClickListener(mLongClickListener);
                    cl.addView(header);
                }
            } else {
                View header = cl.findViewById(R.id.header);
                if (header != null) {
                    cl.removeHeader(header);
                }
                cl.setBackground(null);
            }
            /* YUNOS END */
            resetScreenParams(cl);
            float cellTransX = getChildAt(getIconScreenHomeIndex()).getWidth() * diff;
            float cellTransY = sTranslationY;

            cl.setTranslationX(mInEditing ? cellTransX : 0);
            cl.setTranslationY(mInEditing ? cellTransY : 0);
            cl.setScaleX(mInEditing ? sEditScale : 1.0f);
            cl.setScaleY(mInEditing ? sEditScale : 1.0f);
            ShortcutAndWidgetContainer container = cl.getShortcutAndWidgetContainer();
            container.setTranslationY(mInEditing ? sHeaderHeight / sEditScale : 0);
        }
    }

    private void resetScreenParams(CellLayout cl) {
        cl.setPivotX(0);
        cl.setPivotY(0);
        cl.setRotation(0);
        cl.setRotationY(0);
        cl.setAlpha(1.0f);
        if (cl.getVisibility() != VISIBLE) {
            cl.setVisibility(VISIBLE);
        }
        cl.invalidate();
    }

    private int mScreenCount = 0;
    private boolean mIsFakeChild = false;

    public void getScreenCount() {
        mScreenCount = getChildCount();
        mIsFakeChild = ((CellLayout)getChildAt(mScreenCount - 1)).isFakeChild();
    }

    public void setChildrenEditMode() {
        int screenCount = getChildCount();
        for (; screenCount < mScreenCount; screenCount++) {
            boolean saved = mEmptyScreenAdded;
            addEmptyScreenSync();
            mEmptyScreenAdded = saved;
            if (screenCount == mScreenCount - 1 && mIsFakeChild) {
                CellLayout lastChild = (CellLayout) getChildAt(screenCount);
                lastChild.addEditBtnContainer();
                lastChild.setEditBtnContainerMode(true);
            }
            initScreen(screenCount);
        }
        for (int i = 1; i < screenCount; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            if (!cl.hasChild() && !cl.isFakeChild()) {
                cl.addEditBtnContainer();
                cl.setEditBtnContainerMode(false);
            }
        }
    }

    public boolean setChildrenEditMode(boolean editMode) {
        boolean addResult = false;
        if (editMode) {
            int screenCount = getChildCount();
            if (screenCount > ConfigManager.getIconScreenMaxCount()) {
                return addResult;
            } else if (screenCount == ConfigManager.getIconScreenMaxCount()) {
                CellLayout lastChild = (CellLayout) getChildAt(screenCount - 1);
                if (lastChild.isFakeChild() && lastChild.getEditBtnContainer() != null) {
                    lastChild.setEditBtnContainerMode(false);
                }
            } else {
                boolean saved = mEmptyScreenAdded;
                addEmptyScreenSync();
                addResult = true;
                mEmptyScreenAdded = saved;
                screenCount = getChildCount();
                CellLayout lastChild = (CellLayout) getChildAt(screenCount - 1);
                lastChild.addEditBtnContainer();
                lastChild.setEditBtnContainerMode(true);
                initScreen(screenCount - 1);
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/04/22 ## author: wangye.wy
                // ## BugID: 8176977: update previous screen to fake screen
                if (screenCount > 1) {
                    CellLayout prevChild = (CellLayout) getChildAt(screenCount - 2);
                    // YUNOS BEGIN
                    // ##date: 2016/04/27 ## author: yongxing.lyx
                    // ##BugID:8158127:the only screen should not been deleted.
                    if (!prevChild.hasChild() && !prevChild.isOnlyChild()) {
                    // YUNOS END
                        prevChild.addEditBtnContainer();
                        prevChild.setEditBtnContainerMode(false);
                    } else {
                        prevChild.removeEditBtnContainer();
                    }
                }
                /* YUNOS END */
                mLauncher.getEditModeHelper().onCellLayoutAddOrDelete(true, lastChild, screenCount - 1);
            }
            screenCount = getChildCount();
            for (int i = 1; i < screenCount; i++) {
                CellLayout cl = (CellLayout) getChildAt(i);
                if (!cl.hasChild() && i == screenCount - 1 && !cl.isFakeChild()) {
                    cl.addEditBtnContainer();
                    cl.setEditBtnContainerMode(false);
                } else if (i != screenCount - 1 && !cl.hasChild()) {
                    cl.addEditBtnContainer();
                    cl.setEditBtnContainerMode(false);
                }
            }
        } else {
            final int screenCount = getChildCount();
            for (int i = 0; i < screenCount; i++) {
                CellLayout cl = (CellLayout) getChildAt(i);
                cl.removeEditBtnContainer();
            }
            CellLayout lastCl = (CellLayout) getChildAt(screenCount -1);
            if (screenCount <= ConfigManager.getIconScreenMaxCount()
                    && lastCl.isFakeChild()) {
                if (mCurrentPage == screenCount - 1 ) {
                    mCurrentPage--;
                }
                /* YUNOS BEGIN PB */
                // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
                // ##BugID:(5703673) ##date:2015/01/13
                // ##description: set to last screen when scrolling to add button screen.
                if (isPageMoving() && mNextPage == screenCount - 1) {
                    snapToPage(screenCount - 2);
                }
                /* YUNOS END PB */
                if (!isPageMoving()) {
                    setCurrentPage(mCurrentPage);
                    onPageEndMoving();
                }
                removeViewAt(screenCount - 1);
                invalidatePageIndicator(true);
            } else if (screenCount == ConfigManager.getIconScreenMaxCount()) {
                /* YUNOS BEGIN */
                //##date:2015/05/22  ##author: chenjian.chenjian ##BugId: 5997343
                if(!isPageMoving()){
                    onPageEndMoving();
                }
                /* YUNOS END */
            }
        }
        return addResult;
    }

    public void animateAddEmptyScreen(final View celllayout) {
        View editBtnContainer = ((CellLayout) celllayout).getEditBtnContainer();
        if (editBtnContainer == null) return;
        final View delBtn = editBtnContainer.findViewById(R.id.delete_btn);
        final View addBtn = editBtnContainer.findViewById(R.id.add_btn);
        final float originTranX = celllayout.getTranslationX();
        ValueAnimator transAnim = new ValueAnimator();
        transAnim.setFloatValues(0, 1);
        transAnim.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float percent = (Float) animation.getAnimatedValue();
                if (celllayout != null) {
                    celllayout.setTranslationX(originTranX + celllayout.getWidth() * percent);
                    if (celllayout.getBackground() != null) {
                        celllayout.getBackground().setAlpha((int) (255 * (1 - percent)));
                    }
                }
            }
        });
        transAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/03/10 ## author: wangye.wy
                // ## BugID: 7945871: header in cell layout
                ((CellLayout)celllayout).setClipChildren(false);
                ((CellLayout)celllayout).setClipToPadding(false);
                View header = View.inflate(Workspace.this.getContext(), R.layout.cell_layout_header, null);
                header.setOnLongClickListener(mLongClickListener);
                ((CellLayout)celllayout).addView(header);
                /* YUNOS END */
                celllayout.getBackground().setAlpha(0);
                celllayout.setTranslationX(originTranX);
                addBtn.setVisibility(INVISIBLE);
                delBtn.setVisibility(VISIBLE);
                delBtn.setAlpha(0);
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
        });

        ValueAnimator alphaAnim = new ValueAnimator();
        alphaAnim.setFloatValues(0, 1);
        alphaAnim.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float percent = (Float) animation.getAnimatedValue();
                int alpha = (int) (percent * 255);
                celllayout.getBackground().setAlpha(alpha);
                delBtn.setAlpha(percent);
            }
        });
        alphaAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // to add new empty screen
                boolean addResult = setChildrenEditMode(true);
                if (addResult) {
                    UserTrackerHelper
                            .sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_ARRANGE_ADDSCREEN);
                }
                CellLayout currentLayout = (CellLayout) getChildAt(mCurrentPage);
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/04/22 ## author: wangye.wy
                // ## BugID: 8176977: no need to update cell layout preview
                //mLauncher.getEditModeHelper().onCellLayoutDataChanged(currentLayout, null);
                /* YUNOS END */
            }

            @Override
            public void onAnimationStart(Animator animation) {
                delBtn.setVisibility(VISIBLE);
                delBtn.setAlpha(0);
                celllayout.getBackground().setAlpha(0);
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(Launcher.TAG_EDITMODE, "animateAddEmptyScreen onAnimationStart");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(Launcher.TAG_EDITMODE, "animateAddEmptyScreen onAnimationEnd");
            }
        });
        set.play(alphaAnim).after(transAnim);
        set.setDuration(CellLayout.ADD_DELETE_ANIM_TIMEOUT);
        set.start();
    }

    public void deleteEmptyScreen(CellLayout cl) {
        int screenCount = getChildCount();
        int index = getCellLayoutIndex(cl);
        if (index == (screenCount - 1)) {
            CellLayout lastChild = (CellLayout)getChildAt(screenCount - 1);
            if (!lastChild.isFakeChild() && !lastChild.hasChild()) {
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/03/10 ## author: wangye.wy
                // ## BugID: 7945871: header in cell layout
                View header = lastChild.findViewById(R.id.header);
                if (header != null) {
                    lastChild.removeHeader(header);
                }
                /* YUNOS END */
                lastChild.getBackground().setAlpha(255);
                lastChild.setEditBtnContainerMode(true);
                mLauncher.getEditModeHelper().onCellLayoutDataChanged(lastChild, null);
                return;
            } else if (lastChild.hasChild()){
                removeView(cl);
                if(index != -1 && index < (screenCount - 1)) {
                    LauncherModel.checkEmptyScreen(mContext.getApplicationContext(), index);
                }
                mLauncher.getEditModeHelper().onCellLayoutAddOrDelete(false, cl, index);
                setChildrenEditMode(true);
                return;
            }
        } else {
            CellLayout lastChild = (CellLayout)getChildAt(screenCount - 1);
            if (!lastChild.isFakeChild() && !lastChild.hasChild()) {
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/03/10 ## author: wangye.wy
                // ## BugID: 7945871: header in cell layout
                View header = lastChild.findViewById(R.id.header);
                if (header != null) {
                    lastChild.removeHeader(header);
                }
                /* YUNOS END */
                lastChild.getBackground().setAlpha(255);
                lastChild.setEditBtnContainerMode(true);
                mLauncher.getEditModeHelper().onCellLayoutDataChanged(lastChild, null);
            } else if (lastChild.hasChild()) {
                removeView(cl);
                if (index != -1 && index < (screenCount - 1)) {
                    LauncherModel.checkEmptyScreen(mContext.getApplicationContext(), index);
                }
                mLauncher.getEditModeHelper().onCellLayoutAddOrDelete(false, cl, index);
                setChildrenEditMode(true);
                return;
            }
        }
        removeView(cl);
        if(index != -1 && index < (screenCount - 1)) {
            LauncherModel.checkEmptyScreen(mContext.getApplicationContext(), index);
        }
        mLauncher.getEditModeHelper().onCellLayoutAddOrDelete(false, cl, index);
        invalidatePageIndicator(true);
    }

    private int getCellLayoutIndex(CellLayout  cl) {
        int index = -1;
        int count = getChildCount();
        for(int i= 0; i < count; i++) {
            if(cl == getChildAt(i)) {
                index = i;
                break;
            }
        }
        return index;
    }

    public void clearSelectFlag() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((CellLayout) getChildAt(i)).clearSelectedView();
            ((CellLayout) getChildAt(i)).invalidate();
        }
        mLauncher.getEditModeHelper().onUpdateSelectNumber(null, true);
    }

    public Set<View> getSelectedViewsInLayout() {
        int childCount = getChildCount();
        int hasSelected = 0;
        mIsFromDiffScreen = false;
        Set<View> views = new HashSet<View>();
        for (int i = 0; i < childCount; i++) {
            Set<View> vs = ((CellLayout) getChildAt(i)).getSelectedView();
            if (vs.size() > 0) {
                hasSelected++;
            }
            if (hasSelected > 1) {
                mIsFromDiffScreen = true;
            }
            views.addAll(((CellLayout) getChildAt(i)).getSelectedView());
        }
        return views;
    }

//    private void removeViewsInWorkspace(Set<View> views) {
//        for (View view : views) {
//            CellLayout layout = (CellLayout) getChildAt(((ItemInfo) view.getTag()).screen);
//            if (layout != null) {
//                layout.removeView(view);
//            }
//        }
//    }

    private Set<View> selectedViews = new HashSet<View>();
    private float[] multiSelectAnimDes = new float[2];
    private int[] multiSelectAnimDesInDragLayer = new int[2];
    private boolean mIsMultiSelectDragging = false;
    private Folder mFolderBatchOping = null;
    private String FolderBatchTag = "FolderBatchOp";
    private int mScreenDragStart = 0;
    private boolean mIsFromDiffScreen = false;
    public boolean isMultiSelectDragging() {
        return mIsMultiSelectDragging;
    }
    public Folder getFolderBatchOping() {
        return mFolderBatchOping;
    }
    public void setFolderBatchOping(Folder folder) {
        mFolderBatchOping = folder;
    }

    public void startDragScreenInEditMode() {
        View dragView = (CellLayout)getChildAt(getCurrentPage());
        selectedViews = getSelectedViewsInLayout();
        mLauncher.getEditModeHelper().switchToDragScreenEntry();
        clearSelectFlag();
        for (View view : selectedViews) {
            view.clearFocus();
            view.setPressed(false);
        }
        dragView.setVisibility(INVISIBLE);
        beginDragScreenShared(dragView, this);
    }

    public void startDragInEditMode(CellLayout.CellInfo cellInfo, DragSource source) {
        mScreenDragStart = getCurrentPage();
        View dragView = cellInfo.cell;
        //mDragInfo is not null means the last drag is not complete, we should wait util it is complete to start next drag
        if (dragView instanceof GadgetView || dragView instanceof LauncherAppWidgetHostView
                || dragView.getParent() == null
                || /*YUNOS BEGIN,author:chenjian.chenjian,bug:7868453*/mDragInfo != null/*YUNOS END*/)
            return;
        mDragInfo = cellInfo;
        mIsMultiSelectDragging = true;
        final Canvas canvas = new Canvas();

        // The outline is used to visualize where the item will land if dropped
        mDragOutline = createDragOutline(dragView, canvas, DRAG_BITMAP_PADDING);
        selectedViews = getSelectedViewsInLayout();
        if(!selectedViews.contains(dragView)){
            mLauncher.getEditModeHelper().onUpdateSelectNumber((CellLayout) (dragView.getParent().getParent()), true);
        }
        selectedViews.add(dragView);
        mLauncher.getEditModeHelper().updateEditModeTips(PreviewContentType.MultiSelect);
        clearSelectFlag();
        for (View view : selectedViews) {
            CellLayout layout = (CellLayout) view.getParent().getParent();
            layout.prepareChildForDrag(view);
            view.clearFocus();
            view.setPressed(false);
            LauncherModel.DragInfo dragInfo = new LauncherModel.DragInfo();
            ItemInfo info = (ItemInfo) view.getTag();
            // dragInfo.cell = cellInfo.cell;
            dragInfo.cellX = info.cellX;
            dragInfo.cellY = info.cellY;
            dragInfo.container = info.container;
            dragInfo.screen = info.screen;
            dragInfo.spanX = info.spanX;
            dragInfo.spanY = info.spanY;
            // LauncherModel.addDragInfo(view, dragInfo);
        }
        int[] temp = new int[2];
        ItemInfo tp = (ItemInfo)dragView.getTag();
        CellLayout layout = (CellLayout) getChildAt(tp.screen);
        layout.cellToPoint(tp.cellX, tp.cellY, temp);
        multiSelectAnimDes[0] = temp[0] - layout.getPaddingLeft();
        multiSelectAnimDes[1] = temp[1] - layout.getPaddingTop();
        multiSelectAnimDesInDragLayer = getMoveAnimCoordinateInDiffCellLayout(dragView);
        mAnimSet = getMultiSelectAnim();
        mAnimSet.addListener(multiSelectListener);
        mAnimSet.start();
    }

    AnimatorSet mAnimSet;
    boolean mIsReversingAnim = false;
    public boolean reverseGatherAmin(){
        if(mAnimSet != null && mAnimSet.isRunning() && !mIsReversingAnim){
            mAnimSet.removeAllListeners();
            // YUNOS BEGIN PB
            // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
            // ##BugID:(7981040) ##date:2015/03/12
            // ##description: icon disappear after canceled drag when playing gather amin
            mAnimSet.cancel();
            for (View view : selectedViews) {
                view.setTranslationX(0.0f);
                view.setTranslationY(0.0f);
            }
            mDragOutline = null;
            mDragInfo = null;
            cancelMultiDrag();
            mLauncher.getEditModeHelper().onUpdateSelectNumber(null, true);
            mLauncher.getEditModeHelper().backToEditmodeEntry();
            // YUNOS END PB
            return true;
        }
        return false;
    }

    private AnimatorListener reverseListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            if(mIsReversingAnim){}
            mIsReversingAnim = false;
            mDragInfoDelete = mDragInfo;
            mDragOutline = null;
            mDragInfo = null;
            /* YUNOS END */

            // Hide the scrolling indicator after you pick up an item
            hideScrollingIndicator(false);
            if(mIsMultiSelectDragging) {
                for (View view : selectedViews) {
                    if(view.getTag() != null) {
                        removeDragItemFromList((ItemInfo)view.getTag());
                    }
                }
            }
            mDropTargetView = null;
            Workspace.this.onDragEnd();
            mLauncher.getEditModeHelper().onUpdateSelectNumber(null, true);
        }
    };

    public AnimatorSet getMultiSelectAnim() {
        List<Animator> anims = new ArrayList<Animator>();
        int[] start = new int[2];
        for (View view : selectedViews) {
            ItemInfo info = (ItemInfo) view.getTag();
            if (info == null) {
                continue;
            }
            CellLayout layout = (CellLayout) getChildAt(info.screen);
            if (layout == null) {
                continue;
            }
            if (info.screen == getCurrentPage()) {
                start = getMoveAnimCoordinateInSameCellLayout(view);
                anims.add(ObjectAnimator.ofFloat(view, View.X, start[0], multiSelectAnimDes[0]));
                anims.add(ObjectAnimator.ofFloat(view, View.Y, start[1], multiSelectAnimDes[1]));
            } else {
                start = getMoveAnimCoordinateInDiffCellLayout(view);
                ((CellLayout)view.getParent().getParent()).removeView(view);
                mLauncher.getDragLayer().addView(view);
                anims.add(ObjectAnimator.ofFloat(view, View.X, start[0], multiSelectAnimDesInDragLayer[0]));
                anims.add(ObjectAnimator.ofFloat(view, View.Y, start[1], multiSelectAnimDesInDragLayer[1]));
            }
        }
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(anims);
        animSet.setDuration(300);
        animSet.setInterpolator(new AccelerateDecelerateInterpolator());
        return animSet;
    }

    private AnimatorListener multiSelectListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            /* YUNOS BEGIN*/
            //bug:7868453 author:chenjian.chenjian
            if( null == mDragInfo){
                Log.e(TAG, "multiSelectListener,error happed,mDragInfo is null");
                return;
            }
            /* YUNOS END*/
            for (View view : selectedViews) {
                view.setVisibility(INVISIBLE);
                view.setTranslationX(0.0f);
                view.setTranslationY(0.0f);
                if(view.getParent() instanceof DragLayer) {
                    ((DragLayer)view.getParent()).removeView(view);
                }
            }
            beginDragShared(mDragInfo.cell,Workspace.this);
        }
    };

    private boolean mAnimateScrollEffectMode = false;
    private ScrollEffectAnimator mScrollEffectAnimator;
    private class ScrollEffectAnimator implements Runnable {
        private int runCount = 0;
        private int mLastDemoPage = 0;
        private static final int DEMO_COUNT = 2;

        public boolean isDemoAnimationAllOver() {
            return runCount != 0 && runCount % DEMO_COUNT == 0;
        }

        public void run() {
            int toPage = 0;
            runCount = runCount + 1;
            if(mAnimateScrollEffectMode) {
                mLastDemoPage = mCurrentPage;
                toPage = mCurrentPage == 0 ? 1 : mCurrentPage - 1;
            } else {
                toPage = mLastDemoPage;
            }
            snapToPage(toPage, 300);
        }
    };
    public void animateScrollEffect(boolean bStartAnim) {
        if(mScrollEffectAnimator == null) {
            mScrollEffectAnimator = new ScrollEffectAnimator();
        }
        mAnimateScrollEffectMode = bStartAnim;
        mLauncher.getGestureLayer().setTouchEnabled(false);
        long animationDelay = bStartAnim ? 100L : 50L;
        mLauncher.getHandler().removeCallbacks(mScrollEffectAnimator);
        mLauncher.getHandler().postDelayed(mScrollEffectAnimator, animationDelay);
    }

    public boolean isAnimateScrollEffectOver() {
        return mScrollEffectAnimator == null
                || (mScrollEffectAnimator != null && mScrollEffectAnimator.isDemoAnimationAllOver() && !isPageMoving());
    }

    public int[] getMoveAnimCoordinateInSameCellLayout(View view) {
        int[] coordinate = {-1,-1};
        if(view != null && view.getTag() != null && view.getTag() instanceof ItemInfo) {
            ItemInfo info = (ItemInfo)view.getTag();
            CellLayout layout = (CellLayout)getChildAt(info.screen);
            if(layout != null) {
                layout.cellToPoint(info.cellX, info.cellY, coordinate);
                coordinate[0] -= layout.getPaddingLeft();
                coordinate[1] -= layout.getPaddingTop();
            }
        }
        return coordinate;
    }
    public int[] getMoveAnimCoordinateInDiffCellLayout(View view) {
        return getMoveAnimCoordinateInDiffCellLayout(view, mCurrentPage);
    }
    private int[] getMoveAnimCoordinateInDiffCellLayout(View view, int screen) {
        //the view should add to DragLayer before move in diffrent cellLayout
        int[] coordinate = {-1,-1};
        if(view != null && view.getTag() != null && view.getTag() instanceof ItemInfo) {
            mLauncher.getDragLayer().getLocationInDragLayer(view, coordinate);
        }
        if (screen != mCurrentPage) {
            coordinate[0] += getChildOffset(mCurrentPage) - getChildOffset(screen);
        }
        return coordinate;
    }
    public boolean flyToFolderAndExitEditMode() {
        boolean result = true;
        final Folder folder = mLauncher.getWorkspace().getFolderBatchOping();
        if(folder == null)  {
            Log.e(FolderBatchTag,"Not in FolderBatchOp Mode when flayToFolder is called");
            return false;
        }
        final Set<View> views = getSelectedViewsInLayout();
        if(views.size() == 0) {
            Log.e(FolderBatchTag,"No icon is selected when flayToFolder is called");
            return false;
        }
        AnimatorSet animSet = getBatchFlyToFolderAnim(folder, views);
        if(animSet == null) return false;

        final PackageUpdateTaskQueue taskQueue = mLauncher.getModel().getPackageUpdateTaskQueue();
        animSet.addListener(new AnimatorListenerAdapter(){
            @Override
            public void onAnimationStart(Animator animation) {
                // temporarily disable package update events
                taskQueue.retainLock("FlyToFolderAndExitEditMode Animation");
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                for(View view : views) {
                    if(view.getTag() instanceof ShortcutInfo) {
                        if(view.getParent() != null) {
                            if(view.getParent() instanceof ShortcutAndWidgetContainer) {
                                ((CellLayout)view.getParent().getParent()).removeView(view);
                            } else if(view.getParent() instanceof DragLayer) {
                                ((DragLayer)view.getParent()).removeView(view);
                            }
                        }
                        folder.getInfo().add((ShortcutInfo) view.getTag());
                        view = null;
                    }
                }
                runOnPageStopMoving(new Runnable() {
                    @Override
                    public void run() {
                        mLauncher.exitLauncherEditMode(true);
                        mLauncher.openFolder(folder.getFolderIcon(), new Runnable() {
                            public void run() {
                                taskQueue.releaseLock();
                            }
                        });
                        setScrollingEnable(true);
                        Log.d(TAG, "flyToFolderAndExitEditMode: scrolling enabled");
                    }
                });
            }
        });

        FolderInfo info = folder.getInfo();
        if (info.container == Favorites.CONTAINER_DESKTOP &&
            getCurrentPage() != info.screen) {
            // snap to the page which contains the folder
            snapToPage(info.screen);

        } else if (info.container == Favorites.CONTAINER_HOTSEAT) {
            CellLayout cellLayout = (CellLayout) getChildAt(mCurrentPage);
            if (cellLayout.isFakeChild() ||
                cellLayout.getShortcutAndWidgetContainer().getChildCount() == 0) {
                // current page is empty, snap to next or previous page
                int page = getNearestNonEmptyPage(mCurrentPage);
                if (page != -1) {
                    snapToPage(page);
                }
            }
        }

        // Temporarily disable page scrolling to guarantee the folder
        // will be open on correct page
        setScrollingEnable(false);
        Log.d(TAG, "flyToFolderAndExitEditMode: scrolling disabled");
        clearSelectFlag();
        animSet.start();
        return result;
    }
    private int getNearestNonEmptyPage(int currentPage) {
        final int childCount = getChildCount();
        for (int i = currentPage + 1; i < childCount - 1; i++) {
            CellLayout cellLayout = (CellLayout) getPageAt(i);
            if (cellLayout.isFakeChild() || cellLayout.getShortcutAndWidgetContainer().getChildCount() == 0) {
                continue;
            } else {
                return i;
            }
        }
        for (int i = currentPage - 1; i >= 0; i--) {
            CellLayout cellLayout = (CellLayout) getPageAt(i);
            if (cellLayout.isFakeChild() || cellLayout.getShortcutAndWidgetContainer().getChildCount() == 0) {
                continue;
            } else {
                return i;
            }
        }
        return -1;
    }
    private AnimatorSet getBatchFlyToFolderAnim(Folder folder, Set<View> views) {
        if(views.size() == 0 || folder == null) {
            return null;
        }
        AnimatorSet animSet = new AnimatorSet();
        List<Animator> animList = new ArrayList<Animator>();
        int[] endCoord = getMoveAnimCoordinateInSameCellLayout(folder.getFolderIcon());
        FolderInfo folderInfo = folder.getInfo();
        int pageOfFolder = folderInfo.screen;
        long container = folderInfo.container;
        int[] endCoordInDiffScreen;
        if (container == Favorites.CONTAINER_DESKTOP) {
            endCoordInDiffScreen = getMoveAnimCoordinateInDiffCellLayout(folder.getFolderIcon(), pageOfFolder);
        } else {
            endCoordInDiffScreen = getMoveAnimCoordinateInDiffCellLayout(folder.getFolderIcon(), mCurrentPage);;
        }
        int totalDuration = 600;
        float expansion =0.6f;
        int stage = (int)(totalDuration * expansion / views.size());
        int duration = (int)(totalDuration * (1 - expansion));
        for(View view : views) {
            duration += stage;
            view.setPivotX(view.getWidth() / 2);
            view.setPivotY(view.getHeight() / 2);
            int screen = ((ItemInfo)view.getTag()).screen;
            if(container == Favorites.CONTAINER_DESKTOP &&
               screen == pageOfFolder) {
                int[] start = getMoveAnimCoordinateInSameCellLayout(view);
                animList.add(ObjectAnimator.ofFloat(view, "X", start[0], endCoord[0]).setDuration(duration));
                animList.add(ObjectAnimator.ofFloat(view, "Y", start[1], endCoord[1]).setDuration(duration));
            } else {
                int[] start = getMoveAnimCoordinateInDiffCellLayout(view, screen);
                if(view.getParent() != null) {
                    ((CellLayout)view.getParent().getParent()).removeView(view);
                    mLauncher.getDragLayer().addView(view);
                    view.setTranslationX(start[0]);
                    view.setTranslationY(start[1]);
                    view.setLeft(0);
                    view.setTop(0);
                }
                animList.add(ObjectAnimator.ofFloat(view, "X", start[0], endCoordInDiffScreen[0]).setDuration(duration));
                animList.add(ObjectAnimator.ofFloat(view, "Y", start[1], endCoordInDiffScreen[1]).setDuration(duration));
            }
            animList.add(ObjectAnimator.ofFloat(view, View.SCALE_X, 1, 0.2f).setDuration(duration));
            animList.add(ObjectAnimator.ofFloat(view, View.SCALE_Y, 1, 0.2f).setDuration(duration));
        }
        animSet.playTogether(animList);
        animSet.setInterpolator(new AccelerateInterpolator());
        return animSet;
    }
    /*YUNOS END  for LauncherEditMode*/
    // YUNOS BEGIN PB
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(163418) ##date:2014/08/15
    // ##description: Added support for widget page
    public void makeSureWidgetPages() {

        if (mLauncher.isInLauncherEditMode()) {
            // in editmode, no widget pages
            return;
        }
        int screenCount = getChildCount();
        int normalCount = getScreenCountExcludeWidgetPages();
        int widgePageIndex = 0;
        WidgetPageManager wpManager = mLauncher.mWidgetPageManager;
        boolean removeAllWidgetPage = false;

        for (int i = normalCount; i < screenCount; i++, widgePageIndex++) {
            View child = getChildAt(i);
            CellLayout cl = (CellLayout) child;
            String curName = cl.getWidgetPagePackageName();
            WidgetPageManager.WidgetPageInfo info = wpManager.getWidgetPageInfo(widgePageIndex);
            if (curName != null && !curName.equals(info.getPackageName())) {
                removeAllWidgetPage = true;
                break;
            }
        }

        if (removeAllWidgetPage) {
            for (int i = screenCount - 1; i >= normalCount; i--) {
                View child = getChildAt(i);
                removeView(child);
            }
        }
        screenCount = getChildCount();
        int wpCount = wpManager.getWigetPageCount();
        Log.d(TAG, "  makeSureWidgetPages  screenCount " + screenCount + " wpCount " + wpCount);
        if (screenCount != normalCount + wpCount) {
            for (int i = 0; i < wpCount; i++) {
                WidgetPageManager.WidgetPageInfo info = wpManager.getWidgetPageInfo(i);
                addWidgetPage(normalCount + i, info);
            }
            invalidatePageIndicator(true);
        }

    }

    private void addWidgetPage(int position, WidgetPageManager.WidgetPageInfo info) {
        if (info == null) {
            return;
        }
        CellLayout cl = (CellLayout) (View.inflate(getContext(), R.layout.workspace_screen, null));
        cl.setWidgetPageInfo(info);
        addView(cl, position);
        addWidgetPageToLayout(cl, info.getRootView(), position);
    }

    private void addWidgetPageToLayout(CellLayout cl, View widgetPageView, int position) {
        int childId = LauncherModel.getCellLayoutChildId(
                LauncherSettings.Favorites.CONTAINER_DESKTOP, position, 0, 0, cl.getCountX(),
                cl.getCountY());
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(0, 0, cl.getCountX(),
                cl.getCountY());
        /* YUNOS BEGIN PB*/
        // ##module:HomeShell ##author:jinjiang.wjj
        // ##BugID:5633752 ##date:2014/12/11
        // ##description:Handle IllegalStateException caused by add widgetpage
        ShortcutAndWidgetContainer container = cl.getShortcutAndWidgetContainer();
        if (container.indexOfChild(widgetPageView) != -1) {
            container.removeView(widgetPageView);
        }
        /* YUNOS END PB*/
        cl.addViewToCellLayout(widgetPageView, 0, childId, lp, true);
    }

    public void bindWidgetPage(int position) {
        CellLayout cl = (CellLayout)getChildAt(position);
        String widgetPageName = cl.getWidgetPagePackageName();
        if (widgetPageName!= null) {
            View v = mLauncher.mWidgetPageManager.getWidgetPageRootView(widgetPageName);
            addWidgetPageToLayout(cl, v, position);
        }
    }

    public void removeWidgetPages() {
        final int screenCount = getChildCount();
        Log.d(TAG, "    removeWidgetPages screenCount " + screenCount);
        for (int i = screenCount - 1; i >= 0; i--) {
            CellLayout child = (CellLayout) getChildAt(i);
            if (isWidgetPageView(child)) {
                /* YUNOS BEGIN PB */
                // ##modules(HomeShell): ##author:yongxing.lyx
                // ##BugID:(5617312) ##date:2014/12/03
                // ##decrpition: hide the private hotseat when remove the widget page.
                String widgetPackagename = child.getWidgetPagePackageName();
                View hotseatView = mLauncher.mWidgetPageManager.getHotseatView(widgetPackagename);
                if (hotseatView != null) {
                    hotseatView.setVisibility(View.INVISIBLE);
                }
                /* YUNOS END PB */
                removeView(child);
                child.removeAllViews();
            }
        }

    }

    /* YUNOS BEGIN PB*/
    // ##module:HomeShell ##author:jinjiang.wjj
    // ##BugID:5600944 ##date:2014/11/25
    // ##description:in musicwidget page, don't show global searchbar
    public boolean iscurrMusicWidgetPage() {
        int curPage = getCurrentPage();
        View v = getChildAt(curPage);
        if(v instanceof CellLayout) {
            CellLayout cell = (CellLayout) v;
            return cell.isWidgetPage() && cell.getWidgetPagePackageName().equals("com.android.music_widgetpage");
        }
        return false;
    }
    /* YUNOS END PB*/

    /* YUNOS BEGIN PB*/
    // ##module:HomeShell ##author:jinjiang.wjj
    // ##BugID:5620070 ##date:2014/12/05
    // ##description:widget hotseat and Workspace hoseat icon position overlap
    public boolean isNextWidgetPage() {
        int nextPage = getNextPage();
        View v = getChildAt(nextPage);
        if (isWidgetPageView(v)) {
            return true;
        }
        return false;
    }

    public boolean isCurPageConsumedFlingDown() {
        return (mLauncher.mWidgetPageManager.getConsumedFlingDirection(mCurrentPage)
                & WidgetPageManager.FLING_DIRECTION_DOWN) != 0;
    }
    /* YUNOS END PB*/

    public boolean iscurrWidgetPage() {
        int curPage = getCurrentPage();
        View v = getChildAt(curPage);
        if (isWidgetPageView(v)) {
            return true;
        }
        return false;
    }

    public static boolean isWidgetPageView(View view) {
        if (FeatureUtility.hasFullScreenWidget() && view instanceof CellLayout) {
            return ((CellLayout) view).isWidgetPage();
        }
        return false;
    }

    public int getScreenCountExcludeWidgetPages() {
        int screenCount = getChildCount();
        if(screenCount == 0 || !FeatureUtility.hasFullScreenWidget()) {
            return screenCount;
        }
        int normalCount = screenCount;
        for (int i = 0; i < screenCount; i++) {
            View child = getChildAt(i);
            if (isWidgetPageView(child)) {
                normalCount--;
            }
        }
        return normalCount;
    }

    private void hotseatScrolled(int screenCenter) {
        int childCount = getChildCount();
        if (mLauncher.mWidgetPageManager.getWigetPageCount() == 0) {
            return;
        }
        View hotseatView;

        float pubHotseatProgress = 0;
        int pubHotseatCount = 0;
        for (int i = 0; i < childCount; i++) {
            hotseatView = null;
            CellLayout widgetPage = (CellLayout) getChildAt(i);
            float progress = getScrollProgress(screenCenter, widgetPage, i);
            float absProgress = Math.abs(progress);
            String widgetPackagename = widgetPage.getWidgetPagePackageName();
            /* YUNOS BEGIN PB */
            // ##modules(HomeShell): ##author:yongxing.lyx
            // ##BugID:(5624276) ##date:2014/12/05
            // ##decrpition: flash the private hotseat when homeshell startup.
            /* YUNOS BEGIN PB*/
            // ##module:HomeShell ##author:jinjiang.wjj
            // ##BugID:5631637 ##date:2014/12/11
            // ##description:widget hotseat and Workspace hoseat icon position overlap
            if (widgetPackagename != null) {
                hotseatView = mLauncher.mWidgetPageManager.getHotseatView(widgetPackagename);
                if (!mLauncher.isHideseatShowing()) {
                    //YUNOS BEGIN PB
                    //## modules(HomeShell):
                    //## date:2015/07/31 ##author:shuoxing.wsx
                    //## BugID:6230696:camera widget page's hotseat overlaps with music widget page's.
                    if (hotseatView != null/* && hotseatView.getVisibility() != VISIBLE && isNextWidgetPage()*/) {
                    //YUNOS END PB
                        hotseatView.setVisibility(VISIBLE);
                    } else {
                        mLauncher.getHotseat().setVisibility(VISIBLE);
                    }
                }
            }
            /* YUNOS END PB */
            /* YUNOS END PB */
            if (hotseatView == null) {
                pubHotseatProgress = absProgress + pubHotseatProgress;
                pubHotseatCount ++;
            } else {
                absProgress = Math.min(absProgress * 2, 1.0f);
                /*YUNOS BEGIN PB*/
                //Desc:BugID:6428097:hide or show nav bar by user
                //##Date: Oct 21, 2015 3:18:54 PM ##Author:chao.lc@alibaba-inc.com
                hotseatView.setTranslationY(absProgress * (hotseatView.getHeight() + mNavbarHeight));
                /*YUNOS END PB*/
            }
        }
        pubHotseatProgress =  pubHotseatProgress - (pubHotseatCount - 1);
        pubHotseatProgress =  Math.min(pubHotseatProgress * 2, 1.0f);
        hotseatView = mLauncher.getHotseat();
        if (mLauncher.isSupportLifeCenter() && mCurrentPage == CardBridge.LEFT_SCREEN_INDEX && mNextPage == CardBridge.LEFT_SCREEN_INDEX) {
            // do nothing
        } else {
             /*YUNOS BEGIN PB*/
             //Desc:BugID:6428097:hide or show nav bar by user
             //##Date: Oct 21, 2015 3:18:54 PM ##Author:chao.lc@alibaba-inc.com
             hotseatView.setTranslationY(pubHotseatProgress * (hotseatView.getHeight() + mNavbarHeight));
             /*YUNOS END PB*/
        }
    }

    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##author:yongxing.lyx
    // ##BugID:(6732199) ##date:2015/12/04
    // ##description: hotseat overlap after unlock in widget page.
    public View getWidgetHotseatView(int page) {
        CellLayout widgetPage = (CellLayout) getChildAt(page);
        if (widgetPage == null) {
            return null;
        }
        String widgetPackagename = widgetPage.getWidgetPagePackageName();
        if (widgetPackagename != null) {
            return mLauncher.mWidgetPageManager.getHotseatView(widgetPackagename);
        }
        return null;
    }
    /* YUNOS END PB */

    public WidgetPageManager.WidgetPageInfo getWidgetPageInfoAt(int page) {
        CellLayout widgetPage = (CellLayout) getChildAt(page);
        String packagename = widgetPage.getWidgetPagePackageName();
        return mLauncher.mWidgetPageManager.getWidgetPageInfo(packagename);
    }

    public void recycleBlurWallpaper() {
        if (mBluredWallpaper != null && !mBluredWallpaper.isRecycled()) {
            mBluredWallpaper.recycle();
        }
        mBluredWallpaper = null;
    }

    public boolean hasStatusBar() {
        Window window = mLauncher.getWindow();
        return (window == null || (WindowManager.LayoutParams.FLAG_FULLSCREEN & window
                .getAttributes().flags) == 0);
    }

    public void showStatusBar() {
        Window window = mLauncher.getWindow();
        if (window != null && !hasStatusBar()) {
            window.setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public void hideStatusBar() {
        Window window = mLauncher.getWindow();
        if (window != null && hasStatusBar())
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    public boolean isWidgetPageBlurWallpaper() {
        return mWidgetPageBlurWallpaper;
    }
    // YUNOS END PB

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mLauncher.isSupportLifeCenter()) {
            mLauncher.getCardBridge().onScrollChanged(l, t, oldl, oldt);
        }
    }

    public Boolean isCatActShtStart() {
        return mIsCatActShtStart;
    }

    public void setIsCatActShtStart(Boolean bool) {
        mIsCatActShtStart = bool;
    }
    // add for nav bar.
    public void setInsets(Rect insets) {
        mInsets.set(insets);
    }

    /* YUNOS BEGIN */
    // ## date: 2016/06/20 ## author: yongxing.lyx
    // ## BugID:8424225: icons disappear after press home when enter searching
    // from folder name editor.
    public void resetChildrenVisibility() {
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) {
                child.setVisibility(View.VISIBLE);
            }
            if (child.getAlpha() < 0.5f) {
                child.setAlpha(1.0f);
            }
        }
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ## date: 2016/06/03 ## author: yongxing.lyx
    // ## BugID:8364282:icon overlap after exchange screen and fling icon.
    public void rebuildScreenOccupied() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            cl.addWorkspaceOccupiedToModel(this);
        }
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ## date: 2016/09/26 ## author: yongxing.lyx
    // ## BugID:8364282:icon duplicated after cloned.
    public boolean isShortcutExist(ShortcutInfo info) {
        if (info == null) {
            return false;
        }
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            int count = layout.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = layout.getChildAt(i);

                if (child != null && child.getTag() == info) {
                    return true;
                } else  if  (child.getTag() instanceof FolderInfo) {
                    ArrayList<ShortcutInfo> contents = ((FolderInfo)child.getTag()).contents;
                    for (ShortcutInfo folderItem : contents) {
                        if (folderItem == info) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    /* YUNOS END */
}
