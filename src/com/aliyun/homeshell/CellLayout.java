/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import com.aliyun.homeshell.FolderIcon.FolderRingAnimator;
import com.aliyun.homeshell.editmode.EditModeHelper;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.icon.BubbleController;
import com.aliyun.homeshell.icon.TitleColorManager;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.setting.HomeShellSetting;
import com.aliyun.homeshell.widgetpage.WidgetPageManager;
import com.aliyun.utility.FeatureUtility;
import com.aliyun.homeshell.searchui.SearchBridge;

import storeaui.widget.ActionSheet;

public class CellLayout extends ViewGroup {
    static final String TAG = "CellLayout";

    /*YUNOS BEGIN*/
    //##date:2014/10/29 ##author:zhangqiang.zq
    // aged mode
    //private static  int HIDESEAT_HEIGHT_3_3;
    /*YUNOS END*/
    public static final int HIDESEAT_CELLY = 2;
    private Launcher mLauncher;
    private int mCellWidth;
    private int mCellHeight;

    private int mCountX;
    private int mCountY;

    private int mOriginalWidthGap;
    private int mOriginalHeightGap;
    private int mWidthGap;
    private int mHeightGap;
    private int mMaxGap;
    private boolean mScrollingTransformsDirty = false;

    private final Rect mRect = new Rect();
    private final CellInfo mCellInfo = new CellInfo();

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    private final int[] mTmpXY = new int[2];
    private final int[] mTmpPoint = new int[2];
    int[] mTempLocation = new int[2];

    boolean[][] mOccupied;
    boolean[][] mTmpOccupied;
    private boolean mLastDownOnOccupiedCell = false;

    private OnTouchListener mInterceptTouchListener;

    private ArrayList<FolderRingAnimator> mFolderOuterRings = new ArrayList<FolderRingAnimator>();
    private int[] mFolderLeaveBehindCell = {-1, -1};

    private int mForegroundAlpha = 0;
    private float mBackgroundAlpha;
    private float mBackgroundAlphaMultiplier = 1.0f;

    private Drawable mNormalBackground;
    private Drawable mActiveGlowBackground;
    private Drawable mOverScrollForegroundDrawable;
    private Drawable mOverScrollLeft;
    private Drawable mOverScrollRight;
    private Rect mBackgroundRect;
    private Rect mForegroundRect;
    private int mForegroundPadding;
    private int mLastOrientation;

    private ActionSheet mLayoutActionSheet;
    private String[] mLayoutTitle;

    // If we're actively dragging something over this screen, mIsDragOverlapping is true
    private boolean mIsDragOverlapping = false;
    private final Point mDragCenter = new Point();

    // These arrays are used to implement the drag visualization on x-large screens.
    // They are used as circular arrays, indexed by mDragOutlineCurrent.
    private Rect[] mDragOutlines = new Rect[4];
    private float[] mDragOutlineAlphas = new float[mDragOutlines.length];
    private InterruptibleInOutAnimator[] mDragOutlineAnims =
            new InterruptibleInOutAnimator[mDragOutlines.length];

    // Used as an index into the above 3 arrays; indicates which is the most current value.
    private int mDragOutlineCurrent = 0;
    private final Paint mDragOutlinePaint = new Paint();

    private BubbleTextView mPressedOrFocusedIcon;

    private HashMap<CellLayout.LayoutParams, Animator> mReorderAnimators = new
            HashMap<CellLayout.LayoutParams, Animator>();
    private HashMap<View, ReorderHintAnimation>
            mShakeAnimators = new HashMap<View, ReorderHintAnimation>();
    /** @see ReorderAnimationListener */
    private ReorderAnimationListener mReorderAnimationListener = null;

    private boolean mItemPlacementDirty = false;

    // When a drag operation is in progress, holds the nearest cell to the touch point
    private final int[] mDragCell = new int[2];

    private boolean mDragging = false;

    private TimeInterpolator mEaseOutInterpolator;
    private ShortcutAndWidgetContainer mShortcutsAndWidgets;
    private Map<View, int[]> selectedViewToPos = new HashMap<View, int[]>();
    /* YUNOS BEGIN */
    // ##date:2014/11/24 ##author:zhanggong.zg ##BugID:5444195

    /**
     * This enumeration is used to distinguish different modes for both
     * <code>CellLayout</code> and <code>BubbleTextView</code>. The default
     * mode is <code>NORMAL</code>.
     * @see #setMode(Mode)
     * @author zhanggong.zg
     */
    public enum Mode {
        NORMAL,     // for workspace, folder and other ordinary context
        HOTSEAT,    // specifically for hot-seat
        HIDESEAT;    // specifically for hide-seat

        public boolean isHotseatOrHideseat() {
            return this == Mode.HOTSEAT || this == Mode.HIDESEAT;
        }
    }

    /** @see #setMode(Mode) */
    private Mode mMode = Mode.NORMAL;

    /* YUNOS END */

    private float mHotseatScale = 1f;

    public static final int MODE_DRAG_OVER = 0;
    public static final int MODE_ON_DROP = 1;
    public static final int MODE_ON_DROP_EXTERNAL = 2;
    public static final int MODE_ACCEPT_DROP = 3;
    private static final boolean DESTRUCTIVE_REORDER = false;
    private static final boolean DEBUG_VISUALIZE_OCCUPIED = false;
    private static final boolean DEBUG_OCCUPIED = false;

    static final int LANDSCAPE = 0;
    static final int PORTRAIT = 1;

    private static final float REORDER_HINT_MAGNITUDE = 0.12f;
    private static final int REORDER_ANIMATION_DURATION = 150;
    private float mReorderHintAnimationMagnitude;

    private ArrayList<View> mIntersectingViews = new ArrayList<View>();
    private Rect mOccupiedRect = new Rect();
    private int[] mDirectionVector = new int[2];
    int[] mPreviousReorderDirection = new int[2];
    private static final int INVALID_DIRECTION = -100;
    private DropTarget.DragEnforcer mDragEnforcer;

    private final static PorterDuffXfermode sAddBlendMode =
            new PorterDuffXfermode(PorterDuff.Mode.ADD);
    private final static Paint sPaint = new Paint();
    private ColorDrawable mCd = new ColorDrawable(Color.RED);
 // YUNOS BEGIN PB
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(163418) ##date:2014/08/15
    // ##description: Added support for widget page
    private WidgetPageManager.WidgetPageInfo mWidgetPageInfo;
    // YUNOS END PB

    private boolean mIsLeftPage;

    public CellLayout(Context context) {
        this(context, null);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mDragEnforcer = new DropTarget.DragEnforcer(context);

        // A ViewGroup usually does not draw, but CellLayout needs to draw a rectangle to show
        // the user where a dragged item will land when dropped.
        setWillNotDraw(false);
        setClipToPadding(false);
        mLauncher = (Launcher) context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);
        mCountX = LauncherModel.getCellCountX();
        mCountY = LauncherModel.getCellCountY();
        if (AgedModeUtil.isAgedMode()) {
            adjustToThreeLayout();
        } else {
            adjustFromThreeLayout();
        }

        computePaddings();

        mOccupied = new boolean[mCountX][mCountY];
        mTmpOccupied = new boolean[mCountX][mCountY];
        mPreviousReorderDirection[0] = INVALID_DIRECTION;
        mPreviousReorderDirection[1] = INVALID_DIRECTION;

        a.recycle();
        /*YUNOS BEGIN*/
        //##date:2013/11/26 ##author:xindong.zxd
        //open draw cache
        setAlwaysDrawnWithCacheEnabled(true);
        /*YUNOS END*/

        final Resources res = getResources();
        mHotseatScale = (res.getInteger(R.integer.hotseat_item_scale_percentage) / 100f);
        /*YUNOS BEGIN*/
        //##date:2014/10/29 ##author:zhangqiang.zq
        // aged mode
        /*YUNOS END*/

        mLayoutTitle = getResources().getStringArray(R.array.icon_sort_array);

        mNormalBackground = res.getDrawable(R.drawable.homescreen_blue_normal);
        mActiveGlowBackground = res.getDrawable(R.drawable.homescreen_blue_strong);

        mOverScrollLeft = res.getDrawable(R.drawable.overscroll_glow_left);
        mOverScrollRight = res.getDrawable(R.drawable.overscroll_glow_right);
        mForegroundPadding =
                res.getDimensionPixelSize(R.dimen.workspace_overscroll_drawable_padding);

        mReorderHintAnimationMagnitude = (REORDER_HINT_MAGNITUDE *
                res.getDimensionPixelSize(R.dimen.app_icon_size));

        mNormalBackground.setFilterBitmap(true);
        mActiveGlowBackground.setFilterBitmap(true);

        // Initialize the data structures used for the drag visualization.

        mEaseOutInterpolator = new DecelerateInterpolator(2.5f); // Quint ease out


        mDragCell[0] = mDragCell[1] = -1;
        for (int i = 0; i < mDragOutlines.length; i++) {
            mDragOutlines[i] = new Rect(-1, -1, -1, -1);
        }

        // When dragging things around the home screens, we show a green outline of
        // where the item will land. The outlines gradually fade out, leaving a trail
        // behind the drag path.
        // Set up all the animations that are used to implement this fading.
        final int duration = res.getInteger(R.integer.config_dragOutlineFadeTime);
        final float fromAlphaValue = 0;
        final float toAlphaValue = (float)res.getInteger(R.integer.config_dragOutlineMaxAlpha);

        Arrays.fill(mDragOutlineAlphas, fromAlphaValue);

        for (int i = 0; i < mDragOutlineAnims.length; i++) {
            final InterruptibleInOutAnimator anim =
                new InterruptibleInOutAnimator(this, duration, fromAlphaValue, toAlphaValue);
            anim.getAnimator().setInterpolator(mEaseOutInterpolator);
            final int thisIndex = i;
            anim.getAnimator().addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final Bitmap outline = (Bitmap)anim.getTag();

                    // If an animation is started and then stopped very quickly, we can still
                    // get spurious updates we've cleared the tag. Guard against this.
                    if (outline == null) {
                        @SuppressWarnings("all") // suppress dead code warning
                        final boolean debug = false;
                        if (debug) {
                            Object val = animation.getAnimatedValue();
                            Log.d(TAG, "anim " + thisIndex + " update: " + val +
                                     ", isStopped " + anim.isStopped());
                        }
                        // Try to prevent it from continuing to run
                        animation.cancel();
                    } else {
                        mDragOutlineAlphas[thisIndex] = (Float) animation.getAnimatedValue();
                        CellLayout.this.invalidate(mDragOutlines[thisIndex]);
                    }
                }
            });
            // The animation holds a reference to the drag outline bitmap as long is it's
            // running. This way the bitmap can be GCed when the animations are complete.
            anim.getAnimator().addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if ((Float) ((ValueAnimator) animation).getAnimatedValue() == 0f) {
                        anim.setTag(null);
                    }
                }
            });
            mDragOutlineAnims[i] = anim;
        }

        mBackgroundRect = new Rect();
        mForegroundRect = new Rect();
        if (mLauncher.isSupportLifeCenter() && this.getId() == R.id.lifecenter_cell) {
        } else {
            mShortcutsAndWidgets = new ShortcutAndWidgetContainer(context);
            mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap, mCountX);
            addView(mShortcutsAndWidgets);
        }
    }

    static int widthInPortrait(Resources r, int numCells) {
        // We use this method from Workspace to figure out how many rows/columns Launcher should
        // have. We ignore the left/right padding on CellLayout because it turns out in our design
        // the padding extends outside the visible screen size, but it looked fine anyway.
        int cellWidth = r.getDimensionPixelSize(R.dimen.workspace_cell_width);
        int minGap = Math.min(r.getDimensionPixelSize(R.dimen.workspace_width_gap),
                r.getDimensionPixelSize(R.dimen.workspace_height_gap));

        return  minGap * (numCells - 1) + cellWidth * numCells;
    }

    static int heightInLandscape(Resources r, int numCells) {
        // We use this method from Workspace to figure out how many rows/columns Launcher should
        // have. We ignore the left/right padding on CellLayout because it turns out in our design
        // the padding extends outside the visible screen size, but it looked fine anyway.
        int cellHeight = r.getDimensionPixelSize(R.dimen.workspace_cell_height);
        int minGap = Math.min(r.getDimensionPixelSize(R.dimen.workspace_width_gap),
                r.getDimensionPixelSize(R.dimen.workspace_height_gap));

        return minGap * (numCells - 1) + cellHeight * numCells;
    }

    public void enableHardwareLayers() {
        mShortcutsAndWidgets.setLayerType(LAYER_TYPE_HARDWARE, sPaint);
    }

    public void disableHardwareLayers() {
        mShortcutsAndWidgets.setLayerType(LAYER_TYPE_NONE, sPaint);
    }

    public void buildHardwareLayer() {
        mShortcutsAndWidgets.buildLayer();
    }

    public float getChildrenScale() {
        /* YUNOS BEGIN */
        // ##date:2014/11/24 ##author:zhanggong.zg ##BugID:5444195
        switch (mMode) {
        case HOTSEAT:
        case HIDESEAT:
            return mHotseatScale;
        default:
            return 1.0f; // Mode.MORMAL
        }
        /* YUNOS END */
    }

    public void setGridSize(int cellW, int cellH, int wGap, int hGap) {
        mCellWidth = cellW;
        mCellHeight = cellH;
        mWidthGap = wGap;
        mHeightGap = hGap;

        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap,
                mCountX);
        requestLayout();
    }

    public void setGridSize(int x, int y) {
        mCountX = x;
        mCountY = y;
        mOccupied = new boolean[mCountX][mCountY];
        mTmpOccupied = new boolean[mCountX][mCountY];
        mTempRectStack.clear();
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap,
                mCountX);
        requestLayout();
    }
    /*YUNOS BEGIN*/
    //##date:2015/03/30 ##author:sunchen.sc ##BugID:5735130
    //Add occupy array to LauncherModel ArrayList, or remove from LauncherModel ArrayList
    public void addWorkspaceOccupiedToModel(Workspace workspace) {
        int screen = ((ViewGroup) getParent()).indexOfChild(this);
        screen = workspace.getIconScreenIndex(screen);
        LauncherModel.setWorkspaceOccupied(screen, mOccupied);
    }

    public void removeWorkspaceOccupiedFromModel() {
        LauncherModel.removeWorkspaceOccupied(mOccupied);
    }

    public void addHideseatOccupiedToModel() {
        int screen = ((ViewGroup) getParent()).indexOfChild(this);
        if (DEBUG_OCCUPIED) {
            Log.d(TAG, "addHideseatOccupiedToModel() screen " + screen);
            dumpOccupied();
        }
        LauncherModel.setHideseatOccupied(screen, mOccupied);
    }

    public void removeHideseatOccupiedFromModel(int screen) {
        LauncherModel.removeHideseatOccupied(screen);
    }

    private void dumpOccupied() {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                if (DEBUG_OCCUPIED)
                    Log.d(TAG, "mOccupied[" + i + "][" + j + "] = " + mOccupied[i][j]);
            }
        }
    }
    /*YUNOS END*/
    void resetGridSize(int countX, int countY) {
        if (countX == mCountX && countY == mCountY) {
            return;
        }

        mCountX = countX;
        mCountY = countY;

        computePaddings();

        mOccupied = new boolean[mCountX][mCountY];
        mTmpOccupied = new boolean[mCountX][mCountY];

        mTempRectStack.clear();
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight,
                mWidthGap, mHeightGap, mCountX);

        requestLayout();
        invalidate();
    }

    void computePaddings() {
        mLastOrientation = getResources().getConfiguration().orientation;
        if (!ConfigManager.isLandOrienSupport() &&
                mLastOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            computePaddingsRevert();
            return;
        }
        Resources res = getResources();
        int heighGlobalSearch = 0;
        int layoutH = res.getDisplayMetrics().heightPixels;
        int qsbBarHeight = getResources().getDimensionPixelSize(R.dimen.qsb_bar_height);
        layoutH = layoutH - qsbBarHeight;
        /* YUNOS BEGIN */
        // ##date:2014/10/16 ##author:yangshan.ys##5157204
        // for 3*3 layout
        int workspaceTopPadding = res.getDimensionPixelSize(R.dimen.workspace_top_padding);
        if (AgedModeUtil.isAgedMode()) {
            layoutH = layoutH
                    - workspaceTopPadding
                    - res.getDimensionPixelSize(R.dimen.workspace_bottom_padding_3_3);
        } else {
            layoutH = layoutH
                    - workspaceTopPadding
                    - res.getDimensionPixelSize(R.dimen.workspace_bottom_padding);
        }
        /* YUNSO END */

        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(6737848) ##date:2015/12/10
        // ##description: hide seat margin error with navbar.
        if (Launcher.hasNavigationBar()) {
            /* YUNOS BEGIN */
            // ##date: 2016/08/19 ## author: yongxing.lyx
            // ##BugID:8740916:adjust dimens for 480x854 with navbar.
            if (!AgedModeUtil.isAgedMode()) {
                layoutH += res.getDimensionPixelSize(R.dimen.workspace_bottom_padding_nav_offset);
            }
            /* YUNOS END */
            layoutH = layoutH - this.getPaddingTop();
        } else {
            layoutH = layoutH - this.getPaddingTop() - this.getPaddingBottom();
        }
        // YUNOS END PB
        if(SearchBridge.isHomeShellSupportGlobalSearchUI(getContext()) && !AgedModeUtil.isAgedMode()){
            heighGlobalSearch = res.getDimensionPixelSize(R.dimen.cell_layout_globalSearch_h);
            layoutH = layoutH - heighGlobalSearch;
        }

        mHeightGap = mOriginalHeightGap = (int) ((layoutH - mCellHeight
                * mCountY) / (float) (mCountY - 1));

        if (mHeightGap < 0) {
            mHeightGap = mOriginalHeightGap = 0;
        }

        int layoutW = res.getDisplayMetrics().widthPixels;
        layoutW = layoutW
                - res.getDimensionPixelSize(R.dimen.workspace_left_padding)
                - res.getDimensionPixelSize(R.dimen.workspace_right_padding);
        layoutW = layoutW
                - res.getDimensionPixelSize(R.dimen.cell_layout_left_padding)
                - res.getDimensionPixelSize(R.dimen.cell_layout_right_padding);
        mWidthGap = mOriginalWidthGap = (int) ((layoutW - mCellWidth * mCountX) / (float) (mCountX - 1));
        if (mWidthGap < 0) {
            mWidthGap = mOriginalWidthGap = 0;
        }

        mMaxGap = getResources().getDimensionPixelSize(
                R.dimen.workspace_max_gap);

        Log.d(TAG, "computePaddings mHeightGap : " + mHeightGap
                + " mWidthGap : " + mWidthGap + " mMaxGap : " + mMaxGap);
        if (getParent() instanceof Workspace && mLauncher.getTitleColorManager() != null) {
            int startX = res.getDimensionPixelSize(R.dimen.workspace_left_padding) + res.getDimensionPixelSize(R.dimen.cell_layout_left_padding);
            int startY = qsbBarHeight + workspaceTopPadding + this.getPaddingTop() + heighGlobalSearch;
            mLauncher.getTitleColorManager().updateIconParams(startX, startY, mWidthGap, mHeightGap);
        }
    }

    void computePaddingsRevert() {
        Resources res = getResources();
        int heighGlobalSearch = 0;
        int layoutH = res.getDisplayMetrics().widthPixels;
        int qsbBarHeight = getResources().getDimensionPixelSize(R.dimen.qsb_bar_height);
        layoutH = layoutH - qsbBarHeight;
        int workspaceTopPadding = 0;
        if (AgedModeUtil.isAgedMode()) {
            workspaceTopPadding = res.getDimensionPixelSize(R.dimen.workspace_top_padding);
            layoutH = layoutH - workspaceTopPadding
                    - res.getDimensionPixelSize(R.dimen.workspace_bottom_padding_3_3);
        } else {
            workspaceTopPadding = res.getDimensionPixelSize(R.dimen.workspace_top_padding_port);
            layoutH = layoutH - workspaceTopPadding
                    - res.getDimensionPixelSize(R.dimen.workspace_bottom_padding_port);
        }
        layoutH = layoutH - this.getPaddingTop() - this.getPaddingBottom();
        if(SearchBridge.isHomeShellSupportGlobalSearchUI(getContext())){
            heighGlobalSearch = res.getDimensionPixelSize(R.dimen.cell_layout_globalSearch_h);
            layoutH = layoutH - heighGlobalSearch;
        }

        mHeightGap = mOriginalHeightGap = (int) ((layoutH - mCellHeight
                * mCountY) / (float) (mCountY - 1));

        if (mHeightGap < 0) {
            mHeightGap = mOriginalHeightGap = 0;
        }

        int layoutW = res.getDisplayMetrics().heightPixels;
        layoutW = layoutW
                - res.getDimensionPixelSize(R.dimen.workspace_left_padding_port)
                - res.getDimensionPixelSize(R.dimen.workspace_right_padding_port);
        layoutW = layoutW
                - res.getDimensionPixelSize(R.dimen.cell_layout_left_padding_port)
                - res.getDimensionPixelSize(R.dimen.cell_layout_right_padding_port);
        mWidthGap = mOriginalWidthGap = (int) ((layoutW - mCellWidth * mCountX) / (float) (mCountX - 1));
        if (mWidthGap < 0) {
            mWidthGap = mOriginalWidthGap = 0;
        }

        mMaxGap = getResources().getDimensionPixelSize(
                R.dimen.workspace_max_gap);

        Log.d(TAG, "computePaddingsRevert mHeightGap : " + mHeightGap
                + " mWidthGap : " + mWidthGap + " mMaxGap : " + mMaxGap);
        if (getParent() instanceof Workspace && mLauncher.getTitleColorManager() != null) {
            int startX = res.getDimensionPixelSize(R.dimen.workspace_left_padding_port) + res.getDimensionPixelSize(R.dimen.cell_layout_left_padding_port);
            int startY = qsbBarHeight + workspaceTopPadding + this.getPaddingTop() + heighGlobalSearch;
            mLauncher.getTitleColorManager().updateIconParams(startX, startY, mWidthGap, mHeightGap);
        }
    }

    /*YUNOS BEGIN*/
    //##date:2014/1/9 ##author:zhangqiang.zq
    //screen edit
    public void setCellSize(int w, int h, int widthGap, int heightGap,
            boolean requestLayout) {
        mCellWidth = w;
        mCellHeight = h;
        mWidthGap = mOriginalWidthGap = widthGap;
        mHeightGap = mOriginalHeightGap = heightGap;
        if (requestLayout) {
            mTempRectStack.clear();
            mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap,
                    mCountX);
            requestLayout();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // recalculate cell config after screen orientation changed
        if(mLastOrientation != newConfig.orientation)
            computePaddings();
    }

    public Animator getToPositionAnimation(final View child, int cellX,
            int cellY, int duration, int delay, boolean permanent,
            boolean adjustOccupied) {
        ShortcutAndWidgetContainer clc = getShortcutAndWidgetContainer();

        if (clc.indexOfChild(child) != -1) {
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final ItemInfo info = (ItemInfo) child.getTag();

            final int oldX = lp.x;
            final int oldY = lp.y;
            lp.isLockedToGrid = true;
            if (permanent) {
                lp.cellX = info.cellX = cellX;
                lp.cellY = info.cellY = cellY;
            } else {
                lp.tmpCellX = cellX;
                lp.tmpCellY = cellY;
            }
            clc.setupLp(lp);
            lp.isLockedToGrid = false;
            final int newX = lp.x;
            final int newY = lp.y;

            lp.x = oldX;
            lp.y = oldY;

            Animator va = LauncherAnimUtils.ofPropertyValuesHolder(child,
                    PropertyValuesHolder.ofFloat(ViewHidePropertyName.X, newX),
                    PropertyValuesHolder.ofFloat(ViewHidePropertyName.Y, newY));
            return va;
        }
        return null;
    }

    public void setupChildXY(final View child, final float startX,
            final float startY) {
        final CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child
                .getLayoutParams();
        lp.isLockedToGrid = true;
        getShortcutAndWidgetContainer().setupLp(lp);
        lp.isLockedToGrid = false;
        Animator va = LauncherAnimUtils.ofPropertyValuesHolder(child,
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.X, lp.x),
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.Y, lp.y));
        va.setDuration(5);
        va.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                // TODO Auto-generated method stub
                child.setX(startX);
                child.setY(startY);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // TODO Auto-generated method stub

            }

        });
        va.start();
    }
    /*YUNOS END*/

    // Set whether or not to invert the layout horizontally if the layout is in RTL mode.
    public void setInvertIfRtl(boolean invert) {
        mShortcutsAndWidgets.setInvertIfRtl(invert);
    }

    private void invalidateBubbleTextView(BubbleTextView icon) {
        final int padding = HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS / 2;
        invalidate(icon.getLeft() + getPaddingLeft() - padding,
                icon.getTop() + getPaddingTop() - padding,
                icon.getRight() + getPaddingLeft() + padding,
                icon.getBottom() + getPaddingTop() + padding);
    }

    void setOverScrollAmount(float r, boolean left) {
        if (left && mOverScrollForegroundDrawable != mOverScrollLeft) {
            mOverScrollForegroundDrawable = mOverScrollLeft;
        } else if (!left && mOverScrollForegroundDrawable != mOverScrollRight) {
            mOverScrollForegroundDrawable = mOverScrollRight;
        }

        mForegroundAlpha = (int) Math.round((r * 255));
        mOverScrollForegroundDrawable.setAlpha(mForegroundAlpha);
        invalidate();
    }

    public void setPressedOrFocusedIcon(BubbleTextView icon) {
        // We draw the pressed or focused BubbleTextView's background in CellLayout because it
        // requires an expanded clip rect (due to the glow's blur radius)
        BubbleTextView oldIcon = mPressedOrFocusedIcon;
        mPressedOrFocusedIcon = icon;
        if (oldIcon != null) {
            invalidateBubbleTextView(oldIcon);
        }
        if (mPressedOrFocusedIcon != null) {
            invalidateBubbleTextView(mPressedOrFocusedIcon);
        }
    }

    void setIsDragOverlapping(boolean isDragOverlapping) {
        if (mIsDragOverlapping != isDragOverlapping) {
            mIsDragOverlapping = isDragOverlapping;
            invalidate();
        }
    }

    boolean getIsDragOverlapping() {
        return mIsDragOverlapping;
    }

    protected void setOverscrollTransformsDirty(boolean dirty) {
        mScrollingTransformsDirty = dirty;
    }

    protected void resetOverscrollTransforms() {
        if (mScrollingTransformsDirty) {
            setOverscrollTransformsDirty(false);
            setTranslationX(0);
            setRotationY(0);
            // It doesn't matter if we pass true or false here, the important thing is that we
            // pass 0, which results in the overscroll drawable not being drawn any more.
            setOverScrollAmount(0, false);
            setPivotX(getMeasuredWidth() / 2);
            setPivotY(getMeasuredHeight() / 2);
        }
    }

    public void scaleRect(Rect r, float scale) {
        if (scale != 1.0f) {
            r.left = (int) (r.left * scale + 0.5f);
            r.top = (int) (r.top * scale + 0.5f);
            r.right = (int) (r.right * scale + 0.5f);
            r.bottom = (int) (r.bottom * scale + 0.5f);
        }
    }

    Rect temp = new Rect();
    void scaleRectAboutCenter(Rect in, Rect out, float scale) {
        int cx = in.centerX();
        int cy = in.centerY();
        out.set(in);
        out.offset(-cx, -cy);
        scaleRect(out, scale);
        out.offset(cx, cy);
    }

    public boolean isLayoutRtl() {
        return getResources().getConfiguration().getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // When we're large, we are either drawn in a "hover" state (ie when dragging an item to
        // a neighboring page) or with just a normal background (if backgroundAlpha > 0.0f)
        // When we're small, we are either drawn normally or in the "accepts drops" state (during
        // a drag). However, we also drag the mini hover background *over* one of those two
        // backgrounds
        if (mBackgroundAlpha > 0.0f) {
             // In the mini case, we draw the active_glow bg *over* the active background
            Drawable bg = mIsDragOverlapping ? mActiveGlowBackground : mNormalBackground;

            bg.setAlpha((int) (mBackgroundAlpha * mBackgroundAlphaMultiplier * 255));
            bg.setBounds(mBackgroundRect);
            bg.draw(canvas);
        }

        final Paint paint = mDragOutlinePaint;
        for (int i = 0; i < mDragOutlines.length; i++) {
            final float alpha = mDragOutlineAlphas[i];
            if (alpha > 0) {
                final Rect r = mDragOutlines[i];
                scaleRectAboutCenter(r, temp, getChildrenScale());
                final Bitmap b = (Bitmap) mDragOutlineAnims[i].getTag();
                paint.setAlpha((int)(alpha + .5f));
                canvas.drawBitmap(b, null, temp, paint);
            }
        }

        // We draw the pressed or focused BubbleTextView's background in CellLayout because it
        // requires an expanded clip rect (due to the glow's blur radius)
        if (mPressedOrFocusedIcon != null) {
            /*
            final int padding = mPressedOrFocusedIcon.getPressedOrFocusedBackgroundPadding();
            final Bitmap b = mPressedOrFocusedIcon.getPressedOrFocusedBackground();
            if (b != null) {
                canvas.drawBitmap(b,
                        mPressedOrFocusedIcon.getLeft() + getPaddingLeft() - padding,
                        mPressedOrFocusedIcon.getTop() + getPaddingTop() - padding,
                        null);
            }
            */
        }

        if (DEBUG_VISUALIZE_OCCUPIED) {
            int[] pt = new int[2];
            mCd.setBounds(0, 0,  mCellWidth, mCellHeight);
            for (int i = 0; i < mCountX; i++) {
                for (int j = 0; j < mCountY; j++) {
                    if (mOccupied[i][j]) {
                        cellToPoint(i, j, pt);
                        canvas.save();
                        canvas.translate(pt[0], pt[1]);
                        mCd.draw(canvas);
                        canvas.restore();
                    }
                }
            }
        }

        int previewOffset = FolderRingAnimator.sPreviewOffset;

        final boolean rtl = isLayoutRtl();
        // The folder outer / inner ring image(s)
        FolderRingAnimator fra;
        Drawable d = FolderRingAnimator.sSharedOuterRingDrawable;
        int width,height,centerX,centerY;
        for (int i = 0; i < mFolderOuterRings.size(); i++) {
            fra = mFolderOuterRings.get(i);

            // Draw outer outline
            width = (int) fra.getOuterOutlineWidth();
            height = (int) fra.getOuterOutlineHeight();

            if (!rtl) {
                cellToPoint(fra.mCellX, fra.mCellY, mTempLocation);
            } else {
                cellToPoint(mCountX - fra.mCellX - 1, fra.mCellY, mTempLocation);
            }

            centerX = mTempLocation[0] + mCellWidth / 2;
            centerY = mTempLocation[1] + previewOffset / 2;

            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/16 ## author: wangye.wy
            // ## BugID: 8016476: header in cell layout
            if (mLauncher.isInLauncherEditMode()) {
                centerY += (int)(Workspace.sHeaderHeight / Workspace.sEditScale);
            }
            /* YUNOS END */

            canvas.save();
            canvas.translate(centerX - width / 2, centerY - height / 2);
            d.setBounds(0, 0, width, height);
            d.draw(canvas);
            canvas.restore();
        }

        if (mFolderLeaveBehindCell[0] >= 0 && mFolderLeaveBehindCell[1] >= 0) {
            d = FolderIcon.sSharedFolderLeaveBehind;
            width = d.getIntrinsicWidth();
            height = d.getIntrinsicHeight();

            cellToPoint(mFolderLeaveBehindCell[0], mFolderLeaveBehindCell[1], mTempLocation);
            centerX = mTempLocation[0] + mCellWidth / 2;
            centerY = mTempLocation[1] + previewOffset / 2;

            canvas.save();
            canvas.translate(centerX - width / 2, centerY - width / 2);
            d.setBounds(0, 0, width, height);
            d.draw(canvas);
            canvas.restore();
        }
    }

    /* YUNOS BEGIN */
    // ##date:2015-1-16 ##author:zhanggong.zg ##BugID:5712973
    // clips the contents below hide-seat during animation
    @Override
    public void draw(Canvas canvas) {
        if (!mHideseatAnimationPlaying) {
            super.draw(canvas);
        } else {
            canvas.save();
            int y = mShortcutsAndWidgets.buildLayoutParams(
                    0, CellLayout.HIDESEAT_CELLY, 1, 1, true).y;
            //##date:2015-1-29 ##author:zhanggong.zg ##BugID:5732475
            y += mShortcutsAndWidgets.getTop();
            canvas.clipRect(0, 0, getWidth(), y);
            super.draw(canvas);
            canvas.restore();
        }
        this.drawSelectFlag(canvas);
    }
    /* YUNOS END */

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mForegroundAlpha > 0) {
            mOverScrollForegroundDrawable.setBounds(mForegroundRect);
            Paint p = ((NinePatchDrawable) mOverScrollForegroundDrawable).getPaint();
            p.setXfermode(sAddBlendMode);
            mOverScrollForegroundDrawable.draw(canvas);
            p.setXfermode(null);
        }

        if (isHideseatOpen) {
            /* YUNOS BEGIN */
            // ##date:2015-1-16 ##author:zhanggong.zg ##BugID:5712973
            // clips the contents below hide-seat during animation
            int y = mShortcutsAndWidgets.buildLayoutParams(
                    0, CellLayout.HIDESEAT_CELLY, 1, 1, true).y;
            final int N = mCachedBmps.size();
            Bitmap clipWidget;
            Rect rect;
            for (int i = 0; i < N; i++) {
                clipWidget = mCachedBmps.get(i);
                rect = mCachedRects.get(i);
                if (!mHideseatAnimationPlaying || rect.top < y) {
                    canvas.drawBitmap(clipWidget, rect.left, rect.top, null);
                }
            }
            /* YUNOS END */
        }
    }

    /* YUNOS BEGIN */
    // ##date:2014/11/20 ##author:zhanggong.zg ##BugId:5412034
    /**
     * Manually draw the content of this <code>CellLayout</code> on the specified
     * <code>Canvas</code>. This method is intended to be used to generate a bitmap
     * cache of this <code>CellLayout</code>. The result is similar to call
     * {@link #getDrawingCache()} but with less memory usage. Because this method
     * call {@link #getDrawingCache()} on each single child view instead of the
     * entire <code>CellLayout</code>.
     * @param canvas the target canvas
     * @return {@code true} if succeed
     * @author zhanggong.zg
     */
    public boolean drawShortcutsAndWidgetsOnCanvas(Canvas canvas) {
        canvas.save();
        canvas.translate(mShortcutsAndWidgets.getLeft(), mShortcutsAndWidgets.getTop());
        try {
            View view;
            int left,top;
            Bitmap cache;
            for (int i = 0; i < mShortcutsAndWidgets.getChildCount(); i++) {
                view = mShortcutsAndWidgets.getChildAt(i);
                left = view.getLeft();
                top = view.getTop();
                canvas.translate(left, top);

                // get drawing cache for each child (bubble text view, widget or gadget)
                view.setDrawingCacheEnabled(true);
                view.buildDrawingCache();
                cache = view.getDrawingCache();
                if (cache != null) {
                    canvas.drawBitmap(cache, 0, 0, null);
                }
                view.setDrawingCacheEnabled(false);

                canvas.translate(-left, -top);
            }
            return true;
        } catch (Exception ex) {
            Log.w(TAG, "drawShortcutsAndWidgetsOnCanvas() failed.", ex);
            return false;
        } catch (Error err) {
            // getDrawingCache() can easily lead to OOM error. Due to the framework
            // implementation, the OOM error won't be thrown out. So we have to
            // catch all kinds possible errors here.
            Log.w(TAG, "drawShortcutsAndWidgetsOnCanvas() failed. (probably OOM)", err);
            return false;
        } finally {
            canvas.restore();
        }
    }
    /* YUNOS END */

    public boolean drawEditBtnContainerOnCanvas(Canvas canvas) {
        if (mEditBtnContainer == null) {
            return false;
        }
        try {
            int left = mEditBtnContainer.getLeft();
            int top = mEditBtnContainer.getTop();
            canvas.save();

            mEditBtnContainer.setDrawingCacheEnabled(true);
            mEditBtnContainer.buildDrawingCache();
            Bitmap cache = mEditBtnContainer.getDrawingCache();

            float scale = getResources().getDimension(R.dimen.ic_celllayout_add_btn_width_preview)
                    / getResources().getDimension(R.dimen.ic_celllayout_add_btn_width);
            canvas.scale(scale, scale);
            if (cache != null) {
                canvas.translate((canvas.getWidth() / scale - cache.getWidth()) / 2,
                        (canvas.getHeight() / scale - cache.getHeight()) / 2);
                canvas.drawBitmap(cache, 0, 0, null);
            }
            mEditBtnContainer.setDrawingCacheEnabled(false);

            return true;
        } catch (Exception ex) {
            Log.w(TAG, "drawEditBtnContainerOnCanvas() failed.", ex);
            return false;
        } catch (Error err) {
            // getDrawingCache() can easily lead to OOM error. Due to the framework
            // implementation, the OOM error won't be thrown out. So we have to
            // catch all kinds possible errors here.
            Log.w(TAG, "drawEditBtnContainerOnCanvas() failed. (probably OOM)", err);
            return false;
        } finally {
            canvas.restore();
        }
    }

    public void showFolderAccept(FolderRingAnimator fra) {
        mFolderOuterRings.add(fra);
    }

    public void hideFolderAccept(FolderRingAnimator fra) {
        if (mFolderOuterRings.contains(fra)) {
            mFolderOuterRings.remove(fra);
        }
        invalidate();
    }

    public void setFolderLeaveBehindCell(int x, int y) {
        mFolderLeaveBehindCell[0] = x;
        mFolderLeaveBehindCell[1] = y;
        invalidate();
    }

    public void clearFolderLeaveBehind() {
        mFolderLeaveBehindCell[0] = -1;
        mFolderLeaveBehindCell[1] = -1;
        invalidate();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void restoreInstanceState(SparseArray<Parcelable> states) {
        dispatchRestoreInstanceState(states);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    public void setOnInterceptTouchListener(View.OnTouchListener listener) {
        mInterceptTouchListener = listener;
    }

    public int getCountX() {
        return mCountX;
    }

    public int getCountY() {
        return mCountY;
    }

    /* YUNOS BEGIN */
    // ##date:2014/11/24 ##author:zhanggong.zg ##BugID:5444195

    public Mode getMode() {
        return mMode;
    }

    /**
     * This method is used to replace old <code>setIsHotseat(boolean)</code>
     * method. By default, cell layout is always in <code>MORMAL</code>.
     * When a <code>BubbleTextView</code> is added to cell layout, same mode
     * will be set on that <code>BubbleTextView</code>.
     * @param mode
     */
    public void setMode(Mode mode) {
        mMode = mode;
    }

    /* YUNOS END */

    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params,
            boolean markCells) {
        final LayoutParams lp = params;
        //View view = this.getChildAt(lp.cellX, lp.cellY);
        // Hotseat icons - remove text

        //BugID:6223203:null pointer exception
        if (child == null) {
            return false;
        }
        if (child instanceof BubbleTextView) {
            BubbleTextView bubbleChild = (BubbleTextView) child;
            BubbleController.setMode(bubbleChild, mMode);
        } else if(child instanceof FolderIcon) {
            FolderIcon bubbleChild = (FolderIcon) child;
            bubbleChild.setHotseatMode(mMode == Mode.HOTSEAT);
        }

        child.setScaleX(getChildrenScale());
        child.setScaleY(getChildrenScale());

        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        if (lp.cellX >= 0 && lp.cellX <= mCountX - 1 && lp.cellY >= 0 && lp.cellY <= mCountY - 1) {
            // If the horizontal or vertical span is set to -1, it is taken to
            // mean that it spans the extent of the CellLayout
            if (lp.cellHSpan < 0) lp.cellHSpan = mCountX;
            if (lp.cellVSpan < 0) lp.cellVSpan = mCountY;

            child.setId(childId);

            ItemInfo info = (ItemInfo)child.getTag();
            //BugID:6632546
            if (child.getParent() != null) {
                ((ViewGroup)child.getParent()).removeView(child);
            }
            mShortcutsAndWidgets.addView(child, index, lp);
            if (this == mLauncher.getHotseat().getCellLayout()) {
                mLauncher.getHotseat().initViewCacheList();
            } else {
                if (mLauncher.isContainerHotseat(info.container)) {
                    mLauncher.getHotseat().removeViewByItemInfo(info);
                    mLauncher.getHotseat().initViewCacheList();
                }
            }
            if (markCells) markCellsAsOccupiedForView(child);
            mLauncher.getEditModeHelper().onCellLayoutDataChanged(this, child);

            if(mLauncher.isInLauncherEditMode() && mEditBtnContainer != null) {
                removeEditBtnContainer();
            }
            return true;
        }
        return false;
    }

    @Override
    public void removeAllViews() {
        clearOccupiedCells();
        mShortcutsAndWidgets.removeAllViews();
    }

    @Override
    public void removeAllViewsInLayout() {
        if (mShortcutsAndWidgets.getChildCount() > 0) {
            clearOccupiedCells();
            mShortcutsAndWidgets.removeAllViewsInLayout();
        }
    }

    public void removeViewWithoutMarkingCells(View view) {
        mShortcutsAndWidgets.removeView(view);
    }

    public boolean isOnlyChild() {
        boolean isOnlyChild = false;
        ViewGroup parent = (ViewGroup) getParent();
        int index = -1;
        int childCount = -1;
        if (parent != null) {
            index = parent.indexOfChild(this);
            childCount = parent.getChildCount();
        }
        if (((childCount == 2) && mLauncher.isInLauncherEditMode() && index == 0) ||
                ((childCount == 1) && !mLauncher.isInLauncherEditMode())) {
            isOnlyChild = true;
        }
        return isOnlyChild;
    }

    public void removeHeader(View header) {
        super.removeView(header);
    }

    @Override
    public void removeView(View view) {
        if (isLeftPage()) {
            super.removeView(view);
            return;
        }
        markCellsAsUnoccupiedForView(view);
        mShortcutsAndWidgets.removeView(view);
        mLauncher.getEditModeHelper().onCellLayoutDataChanged(this, null);
        if(mLauncher.isInLauncherEditMode() && !hasChild() && !isOnlyChild()) {
            if(mEditBtnContainer == null) {
                addEditBtnContainer();
                setEditBtnContainerMode(false);
            }
        }
    }

    @Override
    public void removeViewAt(int index) {
        if (isLeftPage()) {
            super.removeViewAt(index);
            return;
        }
        markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(index));
        mShortcutsAndWidgets.removeViewAt(index);
        mLauncher.getEditModeHelper().onCellLayoutDataChanged(this, null);
        if(mLauncher.isInLauncherEditMode() && !hasChild() && !isOnlyChild()) {
            if(mEditBtnContainer == null) {
                addEditBtnContainer();
                setEditBtnContainerMode(false);
            }
        }
    }

    @Override
    public void removeViewInLayout(View view) {
        if (isLeftPage()) {
            super.removeViewInLayout(view);
            return;
        }
        markCellsAsUnoccupiedForView(view);
        mShortcutsAndWidgets.removeViewInLayout(view);
        mLauncher.getEditModeHelper().onCellLayoutDataChanged(this, null);
        if(mLauncher.isInLauncherEditMode() && !hasChild() && !isOnlyChild()) {
            if(mEditBtnContainer == null) {
                addEditBtnContainer();
                setEditBtnContainerMode(false);
            }
        }
    }

    @Override
    public void removeViews(int start, int count) {
        if (isLeftPage()) {
            super.removeViews(start, count);
            return;
        }
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(i));
        }
        mShortcutsAndWidgets.removeViews(start, count);
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        if (isLeftPage()) {
            super.removeViewsInLayout(start, count);
            return;
        }
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(mShortcutsAndWidgets.getChildAt(i));
        }
        mShortcutsAndWidgets.removeViewsInLayout(start, count);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mCellInfo.screen = ((ViewGroup) getParent()).indexOfChild(this);
    }

    public void updateTagScreen(int screen) {
        //BugID:6717591:update screen to avoid wrong value
        //during celllayout removed
        mCellInfo.screen = screen;
    }

    public void setTagToCellInfoForPoint(int touchX, int touchY) {
        final CellInfo cellInfo = mCellInfo;
        Rect frame = mRect;
        final int x = touchX + getScrollX();
        final int y = touchY + getScrollY();
        final int count = mShortcutsAndWidgets.getChildCount();

        boolean found = false;
        for (int i = count - 1; i >= 0; i--) {
            final View child = mShortcutsAndWidgets.getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if ((child.getVisibility() == VISIBLE || child.getAnimation() != null) &&
                    lp.isLockedToGrid) {
                child.getHitRect(frame);

                float scale = child.getScaleX();
                frame = new Rect(child.getLeft(), child.getTop(), child.getRight(),
                        child.getBottom());
                // The child hit rect is relative to the CellLayoutChildren parent, so we need to
                // offset that by this CellLayout's padding to test an (x,y) point that is relative
                // to this view.
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/03/10 ## author: wangye.wy
                // ## BugID: 7945871: header in cell layout
                if (mLauncher.isInLauncherEditMode()) {
                    frame.offset(getPaddingLeft(), getPaddingTop() + (int) (Workspace.sHeaderHeight / Workspace.sEditScale));
                /* YUNOS END */
                } else {
                    frame.offset(getPaddingLeft(), getPaddingTop());
                }
                frame.inset((int) (frame.width() * (1f - scale) / 2),
                        (int) (frame.height() * (1f - scale) / 2));

                if (frame.contains(x, y)) {
                    cellInfo.cell = child;
                    cellInfo.cellX = lp.cellX;
                    cellInfo.cellY = lp.cellY;
                    cellInfo.spanX = lp.cellHSpan;
                    cellInfo.spanY = lp.cellVSpan;
                    found = true;
                    break;
                }
            }
        }

        mLastDownOnOccupiedCell = found;

        if (!found) {
            final int cellXY[] = mTmpXY;
            pointToCellExact(x, y, cellXY);

            cellInfo.cell = null;
            cellInfo.cellX = cellXY[0];
            cellInfo.cellY = cellXY[1];
            cellInfo.spanX = 1;
            cellInfo.spanY = 1;
        }
        setTag(cellInfo);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // First we clear the tag to ensure that on every touch down we start with a fresh slate,
        // even in the case where we return early. Not clearing here was causing bugs whereby on
        // long-press we'd end up picking up an item from a previous drag operation.
        final int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            clearTagCellInfo();
            if (isHideseatOpen) {
                Rect r = new Rect();
                final int N = mShortcutsAndWidgets.getChildCount();
                View child;
                Object tag;
                for (int i = 0; i < N; i++) {
                    // if press on widget, exit hideseat mode.
                    child = mShortcutsAndWidgets.getChildAt(i);
                    /* YUNOS BEGIN */
                    // ##date:2014/9/18 ##author:zhanggong.zg ##BugID:5254249
                    // widgets and gadgets cannot be dragged when hideseat is open
                    tag = child.getTag();
                    if (tag instanceof LauncherAppWidgetInfo ||
                        tag instanceof GadgetItemInfo) {
                        child.getHitRect(r);
                        if (r.contains((int) ev.getX(), (int) ev.getY())) {
                            return true;
                        }
                    }
                    /* YUNOS END */
                }
            }
        }

        if (mInterceptTouchListener != null && mInterceptTouchListener.onTouch(this, ev)) {
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            setTagToCellInfoForPoint((int) ev.getX(), (int) ev.getY());
            //YUNOS BEGIN for 6092554
            if (mLauncher.isInLauncherEditMode() || isHideseatOpen) {
                LauncherAppWidgetHostView widgetView = isTouchWidget(ev);
                View gadgetView = isTouchGadget(ev);
                if (widgetView != null || gadgetView != null) {
                    return true;
                }
            }
            //YUNO END

            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherCategoryMode()) {
                return true;
            }
            /* YUNOS END */
        }
        //BugID:5859927:user track for widget click
        else if (action == MotionEvent.ACTION_UP) {
            LauncherAppWidgetHostView widgetView = isTouchWidget(ev);
            View gadgetView = isTouchGadget(ev);
            if (widgetView != null || gadgetView != null) {
                /*YUNOS BEGIN xiaodong.lxd added for editmode*/
                if (mLauncher.isInLauncherEditMode()) {
                    return true;
                }
                /*YUNOS END*/
            }

            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherCategoryMode()) {
                return true;
            }
            /* YUNOS END */

            if (widgetView != null) {
                Object tag = widgetView.getTag();
                LauncherAppWidgetInfo widget = null;
                if (tag instanceof LauncherAppWidgetInfo) {
                    widget = (LauncherAppWidgetInfo)tag;
                }
                if ((widget != null) && (widget.providerName != null) &&
                    (widget.providerName.getPackageName() != null)) {

                    Map<String, String> param = new HashMap<String, String>();

                    AppWidgetProviderInfo pinfo = ((LauncherAppWidgetHostView) widgetView).getAppWidgetInfo();
                    if (pinfo != null) {
                        param.put("WidgetName", pinfo.label);
                    }

                    String pkgName = widget.providerName.getPackageName();
                    param.put("PkgName", pkgName);
                    param.put("Screen", String.valueOf(widget.screen));
                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_CLICK_WIDGET, param);
                }
            }
        }

        return false;
    }

    public int getShortcutAndFolderNum(){
        final int N = mShortcutsAndWidgets.getChildCount();
        int num = 0;
        View child;
        for (int i = 0; i < N; i++) {
            child = mShortcutsAndWidgets.getChildAt(i);
            if ((child instanceof BubbleTextView) || (child instanceof FolderIcon)) {
                num++;
            }
        }
        return num;
    }

    //BugID:5859927:user track for widget click
    private LauncherAppWidgetHostView isTouchWidget(MotionEvent ev) {
        Rect r = new Rect();
        final int N = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < N; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof LauncherAppWidgetInfo) {
                if(mLauncher.isInLauncherEditMode())
                {
                    int location[] = new int[2];
                    float scaleX = this.getScaleX();
                    float scaleY = this.getScaleY();
                    child.getLocationInWindow(location);
                    float right = location[0] + scaleX * child.getWidth();
                    float bottom = location[1] + scaleY * child.getHeight();
                    if(ev.getRawX()>= location[0] && ev.getRawX() <= right && ev.getRawY()>=location[1] && ev.getRawY()<= bottom){
                        if (child instanceof LauncherAppWidgetHostView) {
                            return (LauncherAppWidgetHostView)child;
                        }else{
                            return null;
                        }
                    }
                }else{
                    child.getHitRect(r);
                    if (r.contains((int) ev.getX(), (int) ev.getY())) {
                        if (child instanceof LauncherAppWidgetHostView) {
                            return (LauncherAppWidgetHostView)child;
                        } else {
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }
    private View isTouchGadget(MotionEvent ev) {
        Rect r = new Rect();
        final int N = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < N; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof GadgetItemInfo) {
                if(mLauncher.isInLauncherEditMode())
                {
                    int location[] = new int[2];
                    float scaleX = this.getScaleX();
                    float scaleY = this.getScaleY();
                    child.getLocationInWindow(location);
                    float right = location[0] + scaleX * child.getWidth();
                    float bottom = location[1] + scaleY * child.getHeight();
                    if(ev.getRawX()>= location[0] && ev.getRawX() <= right && ev.getRawY()>=location[1] && ev.getRawY()<= bottom){
                        return child;
                    }
                }else{
                    child.getHitRect(r);
                    if (r.contains((int) ev.getX(), (int) ev.getY())) {
                        return child;
                    }
                }
            }
        }
        return null;
    }
    private void clearTagCellInfo() {
        final CellInfo cellInfo = mCellInfo;
        cellInfo.cell = null;
        cellInfo.cellX = -1;
        cellInfo.cellY = -1;
        cellInfo.spanX = 0;
        cellInfo.spanY = 0;
        setTag(cellInfo);
    }

    public CellInfo getTag() {
        return (CellInfo) super.getTag();
    }

    /**
     * Given a point, return the cell that strictly encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    void pointToCellExact(int x, int y, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();

        result[0] = (x - hStartPadding) / (mCellWidth + mWidthGap);
        result[1] = (y - vStartPadding) / (mCellHeight + mHeightGap);

        final int xAxis = mCountX;
        final int yAxis = mCountY;

        //BugID:6132913:ArrayIndexOutOfBoundsException
        if (result[0] >= xAxis) result[0] = xAxis - 1;
        if (result[0] < 0) result[0] = 0;
        if (result[1] >= yAxis) result[1] = yAxis - 1;
        if (result[1] < 0) result[1] = 0;
    }

    /**
     * Given a point, return the cell that most closely encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    void pointToCellRounded(int x, int y, int[] result) {
        pointToCellExact(x + (mCellWidth / 2), y + (mCellHeight / 2), result);
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    public void cellToPoint(int cellX, int cellY, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();

        result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap);
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap);

        if (isHideseatOpen) {
            if (cellY >= HIDESEAT_CELLY) {
                result[1] += mLauncher.getCustomHideseat().getCustomeHideseatHeight();
            }
        }
    }

    /**
     * Given a cell coordinate, return the point that represents the center of the cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    void cellToCenterPoint(int cellX, int cellY, int[] result) {
        regionToCenterPoint(cellX, cellY, 1, 1, result);
    }

    /**
     * Given a cell coordinate and span return the point that represents the center of the regio
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    void regionToCenterPoint(int cellX, int cellY, int spanX, int spanY, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();
        if (!isLayoutRtl()) {
            result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap) +
                    (spanX * mCellWidth + (spanX - 1) * mWidthGap) / 2;
        } else {
            // RTL
            result[0] = getWidth() - hStartPadding - cellX * (mCellWidth + mWidthGap) -
                    (spanX * mCellWidth + (spanX - 1) * mWidthGap) / 2;
        }
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap) +
                (spanY * mCellHeight + (spanY - 1) * mHeightGap) / 2;
        if (isHideseatOpen) {
            if (cellY >= HIDESEAT_CELLY) {
                result[1] += mLauncher.getCustomHideseat().getCustomeHideseatHeight();
            }
        }
    }

     /**
     * Given a cell coordinate and span fills out a corresponding pixel rect
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     * @param result Rect in which to write the result
     */
     void regionToRect(int cellX, int cellY, int spanX, int spanY, Rect result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();
        final int left = hStartPadding + cellX * (mCellWidth + mWidthGap);
        final int top = vStartPadding + cellY * (mCellHeight + mHeightGap);
        result.set(left, top, left + (spanX * mCellWidth + (spanX - 1) * mWidthGap),
                top + (spanY * mCellHeight + (spanY - 1) * mHeightGap));
    }

    public float getDistanceFromCell(float x, float y, int[] cell) {
        cellToCenterPoint(cell[0], cell[1], mTmpPoint);
        float distance = (float) Math.sqrt( Math.pow(x - mTmpPoint[0], 2) +
                Math.pow(y - mTmpPoint[1], 2));
        return distance;
    }

    public int getCellWidth() {
        return mCellWidth;
    }

    public int getCellHeight() {
        return mCellHeight;
    }

    public int getWidthGap() {
        return mWidthGap;
    }

    public int getHeightGap() {
        return mHeightGap;
    }

    Rect getContentRect(Rect r) {
        if (r == null) {
            r = new Rect();
        }
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = left + getWidth() - getPaddingLeft() - getPaddingRight();
        int bottom = top + getHeight() - getPaddingTop() - getPaddingBottom();
        r.set(left, top, right, bottom);
        return r;
    }

    static void getMetrics(Rect metrics, Resources res, int measureWidth, int measureHeight,
            int countX, int countY, int orientation) {
        int numWidthGaps = countX - 1;
        int numHeightGaps = countY - 1;

        int widthGap;
        int heightGap;
        int cellWidth;
        int cellHeight;
        int paddingLeft;
        int paddingRight;
        int paddingTop;
        int paddingBottom;

        int maxGap = res.getDimensionPixelSize(R.dimen.workspace_max_gap);
        if (orientation == LANDSCAPE) {
            cellWidth = res.getDimensionPixelSize(R.dimen.workspace_cell_width_land);
            cellHeight = res.getDimensionPixelSize(R.dimen.workspace_cell_height_land);
            widthGap = res.getDimensionPixelSize(R.dimen.workspace_width_gap_land);
            heightGap = res.getDimensionPixelSize(R.dimen.workspace_height_gap_land);
            paddingLeft = res.getDimensionPixelSize(R.dimen.cell_layout_left_padding_land);
            paddingRight = res.getDimensionPixelSize(R.dimen.cell_layout_right_padding_land);
            paddingTop = res.getDimensionPixelSize(R.dimen.cell_layout_top_padding_land);
            paddingBottom = res.getDimensionPixelSize(R.dimen.cell_layout_bottom_padding_land);
        } else {
            // PORTRAIT
            cellWidth = res.getDimensionPixelSize(R.dimen.workspace_cell_width_port);
            cellHeight = res.getDimensionPixelSize(R.dimen.workspace_cell_height_port);
            widthGap = res.getDimensionPixelSize(R.dimen.workspace_width_gap_port);
            heightGap = res.getDimensionPixelSize(R.dimen.workspace_height_gap_port);
            paddingLeft = res.getDimensionPixelSize(R.dimen.cell_layout_left_padding_port);
            paddingRight = res.getDimensionPixelSize(R.dimen.cell_layout_right_padding_port);
            paddingTop = res.getDimensionPixelSize(R.dimen.cell_layout_top_padding_port);
            paddingBottom = res.getDimensionPixelSize(R.dimen.cell_layout_bottom_padding_port);
        }

        if (widthGap < 0 || heightGap < 0) {
            int hSpace = measureWidth - paddingLeft - paddingRight;
            int vSpace = measureHeight - paddingTop - paddingBottom;
            int hFreeSpace = hSpace - (countX * cellWidth);
            int vFreeSpace = vSpace - (countY * cellHeight);
            widthGap = Math.min(maxGap, numWidthGaps > 0 ? (hFreeSpace / numWidthGaps) : 0);
            heightGap = Math.min(maxGap, numHeightGaps > 0 ? (vFreeSpace / numHeightGaps) : 0);
        }
        metrics.set(cellWidth, cellHeight, widthGap, heightGap);
    }

    boolean isHideseatOpen = false;
    private List<View> mCachedChildren = new ArrayList<View>();
    private List<Rect> mCachedRects = new ArrayList<Rect>();
    private List<Bitmap> mCachedBmps = new ArrayList<Bitmap>();
    public void enterHideseatMode() {
        if (isHideseatOpen) {
            return;
        }

        isHideseatOpen = true;
        buildChildrenCacheIfNeed();

        setOnClickListener(mLauncher);
        /* YUNOS BEGIN */
        // ##date:2015-1-19 ##author:zhanggong.zg ##BugID:5621070
        /* The layout process can be slow. Delay it to the end of
           hide-seat open animation. See didEnterHideseatMode(). */
        // requestLayout();
        /* YUNOS END */
    }

    /* YUNOS BEGIN */
    // ##date:2015-1-19 ##author:zhanggong.zg ##BugID:5621070
    // Get called when the hide-seat open animation is finished.
    void didEnterHideseatMode() {
        requestLayout();
    }
    /* YUNOS END */

    public void exitHideseatMode() {
        if (!isHideseatOpen) {
            return;
        }

        isHideseatOpen = false;
        clearChildrenCache();

        // TODO : consider old listener not null case.
        setOnClickListener(null);
        requestLayout();
    }

    /* YUNOS BEGIN */
    // ##date:2015-1-16 ##author:zhanggong.zg ##BugID:5712973
    // clips the contents below hide-seat during animation
    boolean mHideseatAnimationPlaying = false;
    public boolean isHideseatAnimationPlaying() {
        return mHideseatAnimationPlaying;
    }
    public void setHideseatAnimationPlaying(boolean value) {
        if (mHideseatAnimationPlaying != value) {
            mHideseatAnimationPlaying = value;
            invalidate();
        }
    }
    /* YUNOS END */

    void buildChildrenCacheIfNeed() {
        ShortcutAndWidgetContainer container = getShortcutAndWidgetContainer();
        final int N = container.getChildCount();
        for (int i = 0; i < N; i++) {
            View child = container.getChildAt(i);
            ItemInfo info = (ItemInfo) child.getTag();
            if (isWidgetPage() || (info.cellY < HIDESEAT_CELLY && info.cellY + info.spanY > HIDESEAT_CELLY &&
                child.getWidth() > 0 && child.getHeight() > 0)) {
                buildChildCache(child);
            }
        }
    }

    private void buildChildCache(View child) {
        buildChildCache(child, false);
    }

    /* YUNOS BEGIN */
    // ##date:2014/9/18 ##author:zhanggong.zg ##BugID:5254249
    /**
     * Builds bitmap cache for specified child view. The generated bitmap will
     * be stored in <code>mCachedBmps</code>, and corresponding bounds will be
     * stored in <code>mCachedRects</code>.<p>
     * If the child is already cached, this method will update the bounds of the
     * cached bitmap; Furthermore, if <code>rebuildCache</code> is <code>true</code>,
     * the bitmap will be also rebuilt.
     * @param child
     * @param rebuildCache
     */
    private void buildChildCache(View child, boolean rebuildCache) {
        CellLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        ShortcutAndWidgetContainer container = getShortcutAndWidgetContainer();

        boolean alreadyCached = false;
        final int N = mCachedChildren.size();
        for (int i = 0; i < N; i++) {
            View v = mCachedChildren.get(i);
            if (v == child) {
                alreadyCached = true;
                if (rebuildCache) {
                    // update bounds and bitmap cache
                    mCachedRects.remove(i);
                    mCachedBmps.remove(i).recycle();
                    break;
                } else {
                    // update bounds only
                    Rect r = mCachedRects.get(i);
                    int l = lp.x + container.getLeft();
                    int t = lp.y + container.getTop();
                    r.offset(l - r.left, t - r.top);
                    return;
                }
            }
        }

        if (!alreadyCached) {
            child.setVisibility(View.INVISIBLE);
            mCachedChildren.add(child);
        }

        boolean enabled = child.isDrawingCacheEnabled();
        if (!enabled) {
            child.setDrawingCacheEnabled(true);
        }
        Bitmap cache = child.getDrawingCache();

        int cellY = lp.useTmpCoords ? lp.tmpCellY : lp.cellY;
        int W = child.getWidth();
        int H = child.getHeight();
        int vSpan = HIDESEAT_CELLY - cellY;
        int top = vSpan * mCellHeight + (vSpan - 1) * mHeightGap + mHeightGap / 2;

        Bitmap bmp = Bitmap
                .createBitmap(W, H + mLauncher.getCustomHideseat().getCustomeHideseatHeight(), Config.ARGB_8888);

        Canvas c = new Canvas(bmp);
        Rect src = new Rect(0, 0, W, top);
        Rect dst = src;
        c.drawBitmap(cache, src, dst, null);

        src = new Rect(0, top, W, H);
        dst = new Rect(0, top + mLauncher.getCustomHideseat().getCustomeHideseatHeight(), W, H
                + mLauncher.getCustomHideseat().getCustomeHideseatHeight());

        c.drawBitmap(cache, src, dst, null);

        int l = lp.x + container.getLeft();
        int t = lp.y + container.getTop();
        Rect rect = new Rect(l, t, l + bmp.getWidth(), t + bmp.getHeight());

        mCachedRects.add(rect);
        mCachedBmps.add(bmp);

        if (!enabled) {
            child.setDrawingCacheEnabled(false);
        }
    }
    /* YUNOS END */

    private void clearChildCache(View child) {
        int size = mCachedChildren.size();
        for (int i = 0; i < size; i++) {
            if (child == mCachedChildren.get(i)) {
                child.setVisibility(View.VISIBLE);
                mCachedChildren.remove(i);
                mCachedRects.remove(i);
                Bitmap cache = mCachedBmps.remove(i);
                cache.recycle();
                break;
            }
        }
    }

    private void clearChildrenCache() {
        for (View child : mCachedChildren) {
            child.setVisibility(View.VISIBLE);
        }

        for (Bitmap cache : mCachedBmps) {
            cache.recycle();
        }

        mCachedChildren.clear();
        mCachedRects.clear();
        mCachedBmps.clear();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        if (mLauncher.isSupportLifeCenter() && this.getId() == R.id.lifecenter_cell) {
            widthSpecMode = heightSpecMode = MeasureSpec.EXACTLY;// match_parent
        }

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            widthSpecMode = heightSpecMode = MeasureSpec.AT_MOST;
            widthSpecSize = getDesiredWidth();
            heightSpecSize = getDesiredHeight();
         //use default config for scrollview
//            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        int numWidthGaps = mCountX - 1;
        int numHeightGaps = mCountY - 1;

        int heightGap = 0;
        int widthGap = 0;
//        if (mOriginalWidthGap < 0 || mOriginalHeightGap < 0) {
        int hSpace = widthSpecSize - getPaddingLeft() - getPaddingRight();
        int vSpace = heightSpecSize - getPaddingTop() - getPaddingBottom();
        int hFreeSpace = hSpace - (mCountX * mCellWidth);
        int vFreeSpace = vSpace - (mCountY * mCellHeight);
        widthGap = numWidthGaps > 0 ? (hFreeSpace / numWidthGaps) : mMaxGap;
        heightGap = numHeightGaps > 0 ? (vFreeSpace / numHeightGaps) : mMaxGap;
//            mWidthGap = Math.min(mMaxGap, numWidthGaps > 0 ? (hFreeSpace / numWidthGaps) : 0);
//            mHeightGap = Math.min(mMaxGap,numHeightGaps > 0 ? (vFreeSpace / numHeightGaps) : 0);
        mWidthGap = mOriginalWidthGap < 0 ? widthGap : Math.min(widthGap, mOriginalWidthGap);
        mHeightGap = mOriginalHeightGap < 0 ? heightGap : Math.min(heightGap, mOriginalHeightGap);
        mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap, mHeightGap,
                mCountX);
        // Initial values correspond to widthSpecMode == MeasureSpec.EXACTLY
        int newWidth = widthSpecSize;
        int newHeight = heightSpecSize;
        if (widthSpecMode == MeasureSpec.AT_MOST) {
            newWidth = getPaddingLeft() + getPaddingRight() + (mCountX * mCellWidth)
                    + (widthGap == mWidthGap && numWidthGaps != 0
                            ? ((mCountX - 1) * hFreeSpace / numWidthGaps)
                            : (mCountX - 1) * mWidthGap);
            newHeight = getPaddingTop()
                    + getPaddingBottom()
                    + (mCountY * mCellHeight)
                    + (heightGap == mHeightGap && numHeightGaps != 0
                            ? ((mCountY - 1) * vFreeSpace / numHeightGaps)
                            : (mCountY - 1)
                            * mHeightGap);
            if (isHideseatOpen) {
                newHeight += mLauncher.getCustomHideseat().getCustomeHideseatHeight();
            }
            setMeasuredDimension(newWidth, newHeight);
        }

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(newWidth - getPaddingLeft() -
                    getPaddingRight(), MeasureSpec.EXACTLY);
            int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(newHeight - getPaddingTop() -
                    getPaddingBottom(), MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        }
        setMeasuredDimension(newWidth, newHeight);
//        Log.d(TAG, "sxsexe99----> onMeasure end newHeight " + newHeight
//                + " heightSpecSize " + heightSpecSize + " mOriginalHeightGap " + mOriginalHeightGap);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View header = findViewById(R.id.header);
        int height = (int)(Workspace.sHeaderHeight / Workspace.sEditScale);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getId() == R.id.background_container) {
                child.layout(0, getPaddingTop(), r - l, b - t - getPaddingBottom());
            } else {
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/03/10 ## author: wangye.wy
                // ## BugID: 7945871: header in cell layout
                if (child.getId() == R.id.header) {
                    child.layout(0, 0, r - l, height);
                /* YUNOS END */
                } else {
                    child.layout(getPaddingLeft(), getPaddingTop(), r - l - getPaddingRight(), b - t - getPaddingBottom());
                }
            }
            child.invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBackgroundRect.set(0, 0, w, h);
        mForegroundRect.set(mForegroundPadding, mForegroundPadding,
                w - mForegroundPadding, h - mForegroundPadding);
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        mShortcutsAndWidgets.setChildrenDrawingCacheEnabled(enabled);
    }

    @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        mShortcutsAndWidgets.setChildrenDrawnWithCacheEnabled(enabled);
    }

    public float getBackgroundAlpha() {
        return mBackgroundAlpha;
    }

    public void setBackgroundAlphaMultiplier(float multiplier) {
        if (mBackgroundAlphaMultiplier != multiplier) {
            mBackgroundAlphaMultiplier = multiplier;
            invalidate();
        }
    }

    public float getBackgroundAlphaMultiplier() {
        return mBackgroundAlphaMultiplier;
    }

    public void setBackgroundAlpha(float alpha) {
        if (mBackgroundAlpha != alpha) {
            mBackgroundAlpha = alpha;
            invalidate();
        }
    }

    public void setShortcutAndWidgetAlpha(float alpha) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).setAlpha(alpha);
        }
    }

    public View getChildAt(int x, int y) {
        return mShortcutsAndWidgets.getChildAt(x, y);
    }

    public boolean animateChildToPosition(final View child, int cellX, int cellY, int duration,
            int delay, final boolean permanent, boolean adjustOccupied) {
        ShortcutAndWidgetContainer clc = getShortcutAndWidgetContainer();
        boolean[][] occupied = mOccupied;
        if (!permanent) {
            occupied = mTmpOccupied;
        }

        if (clc.indexOfChild(child) != -1) {
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final ItemInfo info = (ItemInfo) child.getTag();

            // We cancel any existing animations
            if (mReorderAnimators.containsKey(lp)) {
                mReorderAnimators.get(lp).cancel();
                mReorderAnimators.remove(lp);
            }

            final int oldX = lp.x;
            final int oldY = lp.y;
            if (adjustOccupied) {
                occupied[lp.cellX][lp.cellY] = false;
                occupied[cellX][cellY] = true;
            }
            lp.isLockedToGrid = true;
            if (permanent) {
                lp.cellX = info.cellX = cellX;
                lp.cellY = info.cellY = cellY;
            } else {
                lp.tmpCellX = cellX;
                lp.tmpCellY = cellY;
            }
            clc.setupLp(lp);
            lp.isLockedToGrid = false;
            final int newX = lp.x;
            final int newY = lp.y;

            lp.x = oldX;
            lp.y = oldY;

            // Exit early if we're not actually moving the view
            if (oldX == newX && oldY == newY) {
                lp.isLockedToGrid = true;
                return true;
            }

            ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
            va.setDuration(duration);
            /* YUNOS BEGIN */
            // ##date:2014/10/16 ##author:zhanggong.zg ##BugID:5252746
            // notify that reorder animation has started
            if (mReorderAnimationListener != null &&
                mReorderAnimators.isEmpty()) {
                mReorderAnimationListener.reorderAnimationStart(this);
            }
            /* YUNOS END */
            mReorderAnimators.put(lp, va);

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
                boolean cancelled = false;
                public void onAnimationEnd(Animator animation) {
                    // If the animation was cancelled, it means that another animation
                    // has interrupted this one, and we don't want to lock the item into
                    // place just yet.
                    if (!cancelled) {
                        lp.isLockedToGrid = true;
                        child.requestLayout();
                    }
                    if (mReorderAnimators.containsKey(lp)) {
                        mReorderAnimators.remove(lp);
                    }
                    /* YUNOS BEGIN */
                    // ##date:2014/10/16 ##author:zhanggong.zg ##BugID:5252746
                    // notify that reorder animation has finished
                    if (mReorderAnimators.isEmpty() &&
                        mReorderAnimationListener != null) {
                        mReorderAnimationListener.reorderAnimationFinish(CellLayout.this);
                    }
                    /* YUNOS END */
                    if (isHideseatOpen) {
                        int vSpan = lp.cellVSpan;
                        if (vSpan > 1) {
                            int y = permanent ? lp.cellY : lp.tmpCellY;
                            if (y < HIDESEAT_CELLY && y + vSpan > HIDESEAT_CELLY) {
                                buildChildCache(child, true);
                            } else {
                                clearChildCache(child);
                            }
                        }
                    }
                }
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }
            });
            va.setStartDelay(delay);
            va.start();
            return true;
        }
        return false;
    }


    /**
     * Estimate where the top left cell of the dragged item will land if it is dropped.
     *
     * @param originX The X value of the top left corner of the item
     * @param originY The Y value of the top left corner of the item
     * @param spanX The number of horizontal cells that the item spans
     * @param spanY The number of vertical cells that the item spans
     * @param result The estimated drop cell X and Y.
     */
    void estimateDropCell(int originX, int originY, int spanX, int spanY, int[] result) {
        final int countX = mCountX;
        final int countY = mCountY;

        // pointToCellRounded takes the top left of a cell but will pad that with
        // cellWidth/2 and cellHeight/2 when finding the matching cell
        pointToCellRounded(originX, originY, result);

        // If the item isn't fully on this screen, snap to the edges
        int rightOverhang = result[0] + spanX - countX;
        if (rightOverhang > 0) {
            result[0] -= rightOverhang; // Snap to right
        }
        result[0] = Math.max(0, result[0]); // Snap to left
        int bottomOverhang = result[1] + spanY - countY;
        if (bottomOverhang > 0) {
            result[1] -= bottomOverhang; // Snap to bottom
        }
        result[1] = Math.max(0, result[1]); // Snap to top
    }

    void visualizeDropLocation(View v, Bitmap dragOutline, int originX, int originY, int cellX,
            int cellY, int spanX, int spanY, boolean resize, Point dragOffset, Rect dragRegion,
            boolean toHotseat, boolean fromHotseat, int touchX, int touchY) {
        if (isLayoutRtl()) {
            cellX = mCountX - cellX - 1 - (spanX - 1);
        }
        final int oldDragCellX = mDragCell[0];
        final int oldDragCellY = mDragCell[1];

        if (v != null && dragOffset == null) {
            mDragCenter.set(originX + (v.getWidth() / 2), originY + (v.getHeight() / 2));
        } else {
            mDragCenter.set(originX, originY);
        }

        if (dragOutline == null || v == null) {
            return;
        }

        if (toHotseat && mLauncher.getHotseat().isFull()) {
            // BugID:5238420:if hotseat is full , don't draw dragOutline
            return;
        }

        if (cellX != oldDragCellX || cellY != oldDragCellY) {
            mDragCell[0] = cellX;
            mDragCell[1] = cellY;
            // Find the top left corner of the rect the object will occupy
            final int[] topLeft = mTmpPoint;
            cellToPoint(cellX, cellY, topLeft);

            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: header in cell layout
            if (mLauncher.isInLauncherEditMode()) {
                topLeft[1] += (int) (Workspace.sHeaderHeight / Workspace.sEditScale);
            }
            /* YUNOS END */

            /* YUNOS BEGIN */
            //##date:2013/12/03 ##author:xiaodong.lxd
            if(toHotseat) {
                mLauncher.getHotseat().touchToPoint(touchX, touchY, topLeft, fromHotseat, toHotseat);
            }
            int left = topLeft[0];
            int top = topLeft[1];
            /* YUNOS END */
            /* commented by xiaodong.lxd casue no need to offset the dragOutline by UEDs in YUNOS
             * if (v != null && dragOffset == null) {
                // When drawing the drag outline, it did not account for margin offsets
                // added by the view's parent.
                MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
                left += lp.leftMargin;
                top += lp.topMargin;

                // Offsets due to the size difference between the View and the dragOutline.
                // There is a size difference to account for the outer blur, which may lie
                // outside the bounds of the view.
                top += (v.getHeight() - dragOutline.getHeight()) / 2;
                // We center about the x axis
                left += ((mCellWidth * spanX) + ((spanX - 1) * mWidthGap)
                        - dragOutline.getWidth()) / 2;
            } else {
                if (dragOffset != null && dragRegion != null) {
                    // Center the drag region *horizontally* in the cell and apply a drag
                    // outline offset
                    left += dragOffset.x + ((mCellWidth * spanX) + ((spanX - 1) * mWidthGap)
                             - dragRegion.width()) / 2;
                    top += dragOffset.y;
                } else {
                    // Center the drag outline in the cell
                    left += ((mCellWidth * spanX) + ((spanX - 1) * mWidthGap)
                            - dragOutline.getWidth()) / 2;
                    top += ((mCellHeight * spanY) + ((spanY - 1) * mHeightGap)
                            - dragOutline.getHeight()) / 2;
                }
            }*/

            final int oldIndex = mDragOutlineCurrent;
            mDragOutlineAnims[oldIndex].animateOut();
            mDragOutlineCurrent = (oldIndex + 1) % mDragOutlines.length;
            Rect r = mDragOutlines[mDragOutlineCurrent];
            r.set(left, top, left + dragOutline.getWidth(), top + dragOutline.getHeight());
            if (resize) {
                cellToRect(cellX, cellY, spanX, spanY, r);
            }
            mDragOutlineAnims[mDragOutlineCurrent].setTag(dragOutline);
            mDragOutlineAnims[mDragOutlineCurrent].animateIn();
        }
    }

    public void clearDragOutlines() {
        final int oldIndex = mDragOutlineCurrent;
        mDragOutlineAnims[oldIndex].animateOut();
        mDragCell[0] = mDragCell[1] = -1;
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY,
            int[] result) {
        return findNearestVacantArea(pixelX, pixelY, spanX, spanY, null, result);
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestVacantArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX,
            int spanY, int[] result, int[] resultSpan) {
        return findNearestVacantArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, null,
                result, resultSpan);
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestArea(int pixelX, int pixelY, int spanX, int spanY, View ignoreView,
            boolean ignoreOccupied, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY,
                spanX, spanY, ignoreView, ignoreOccupied, result, null, mOccupied);
    }

    private final Stack<Rect> mTempRectStack = new Stack<Rect>();
    private void lazyInitTempRectStack() {
        if (mTempRectStack.isEmpty()) {
            for (int i = 0; i < mCountX * mCountY; i++) {
                mTempRectStack.push(new Rect());
            }
        }
    }

    private void recycleTempRects(Stack<Rect> used) {
        while (!used.isEmpty()) {
            mTempRectStack.push(used.pop());
        }
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View ignoreView, boolean ignoreOccupied, int[] result, int[] resultSpan,
            boolean[][] occupied) {
        lazyInitTempRectStack();
        // mark space take by ignoreView as available (method checks if ignoreView is null)
        markCellsAsUnoccupiedForView(ignoreView, occupied);

        // For items with a spanX / spanY > 1, the passed in point (pixelX, pixelY) corresponds
        // to the center of the item, but we are searching based on the top-left cell, so
        // we translate the point over to correspond to the top-left.
        pixelX -= (mCellWidth + mWidthGap) * (spanX - 1) / 2f;
        pixelY -= (mCellHeight + mHeightGap) * (spanY - 1) / 2f;

        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: header in cell layout
        if (mLauncher.isInLauncherEditMode()) {
            pixelY -= (int) (Workspace.sHeaderHeight / Workspace.sEditScale);
        }
        /* YUNOS END */

        // Keep track of best-scoring drop area
        final int[] bestXY = result != null ? result : new int[2];
        double bestDistance = Double.MAX_VALUE;
        final Rect bestRect = new Rect(-1, -1, -1, -1);
        final Stack<Rect> validRegions = new Stack<Rect>();

        final int countX = mCountX;
        final int countY = mCountY;

        if (minSpanX <= 0 || minSpanY <= 0 || spanX <= 0 || spanY <= 0 ||
                spanX < minSpanX || spanY < minSpanY) {
            return bestXY;
        }

        for (int y = 0; y < countY - (minSpanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (minSpanX - 1); x++) {
                int ySize = -1;
                int xSize = -1;
                if (ignoreOccupied) {
                    // First, let's see if this thing fits anywhere
                    for (int i = 0; i < minSpanX; i++) {
                        for (int j = 0; j < minSpanY; j++) {
                            if (occupied[x + i][y + j]) {
                                continue inner;
                            }
                        }
                    }
                    xSize = minSpanX;
                    ySize = minSpanY;

                    // We know that the item will fit at _some_ acceptable size, now let's see
                    // how big we can make it. We'll alternate between incrementing x and y spans
                    // until we hit a limit.
                    boolean incX = true;
                    boolean hitMaxX = xSize >= spanX;
                    boolean hitMaxY = ySize >= spanY;
                    while (!(hitMaxX && hitMaxY)) {
                        if (incX && !hitMaxX) {
                            for (int j = 0; j < ySize; j++) {
                                if (x + xSize > countX -1 || occupied[x + xSize][y + j]) {
                                    // We can't move out horizontally
                                    hitMaxX = true;
                                }
                            }
                            if (!hitMaxX) {
                                xSize++;
                            }
                        } else if (!hitMaxY) {
                            for (int i = 0; i < xSize; i++) {
                                if (y + ySize > countY - 1 || occupied[x + i][y + ySize]) {
                                    // We can't move out vertically
                                    hitMaxY = true;
                                }
                            }
                            if (!hitMaxY) {
                                ySize++;
                            }
                        }
                        hitMaxX |= xSize >= spanX;
                        hitMaxY |= ySize >= spanY;
                        incX = !incX;
                    }
                    incX = true;
                    hitMaxX = xSize >= spanX;
                    hitMaxY = ySize >= spanY;
                }
                final int[] cellXY = mTmpXY;
                cellToCenterPoint(x, y, cellXY);

                // We verify that the current rect is not a sub-rect of any of our previous
                // candidates. In this case, the current rect is disqualified in favour of the
                // containing rect.
                Rect currentRect = mTempRectStack.pop();
                currentRect.set(x, y, x + xSize, y + ySize);
                boolean contained = false;
                for (Rect r : validRegions) {
                    if (r.contains(currentRect)) {
                        contained = true;
                        break;
                    }
                }
                validRegions.push(currentRect);
                double distance = Math.sqrt(Math.pow(cellXY[0] - pixelX, 2)
                        + Math.pow(cellXY[1] - pixelY, 2));

                if ((distance <= bestDistance && !contained) ||
                        currentRect.contains(bestRect)) {
                    bestDistance = distance;
                    bestXY[0] = x;
                    bestXY[1] = y;
                    //for atom hotseat begin
                    if (getParent() instanceof Hotseat && ((Hotseat)getParent()).isStaticHotsaet()) {
                            int atomRank = ConfigManager.getHotseatAtomRank();
                            boolean isLand = LauncherApplication.isInLandOrientation();
                            if (!isLand && atomRank > 0 && bestXY[0] > 0 &&
                                    bestXY[0] >= atomRank) bestXY[0] = x - 1;
                            if (isLand && atomRank > 0 && bestXY[1] > 0 &&
                                    bestXY[1] >= atomRank) bestXY[1] = y - 1;
                    }
                    //for atom hotseat end
                    if (resultSpan != null) {
                        resultSpan[0] = xSize;
                        resultSpan[1] = ySize;
                    }
                    bestRect.set(currentRect);
                }
            }
        }
        // re-mark space taken by ignoreView as occupied
        markCellsAsOccupiedForView(ignoreView, occupied);

        // Return -1, -1 if no suitable location found
        if (bestDistance == Double.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        recycleTempRects(validRegions);
        return bestXY;
    }

     /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location, and will also weigh in a suggested direction vector of the
     * desired location. This method computers distance based on unit grid distances,
     * not pixel distances.
     *
     * @param cellX The X cell nearest to which you want to search for a vacant area.
     * @param cellY The Y cell nearest which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param direction The favored direction in which the views should move from x, y
     * @param exactDirectionOnly If this parameter is true, then only solutions where the direction
     *        matches exactly. Otherwise we find the best matching direction.
     * @param occoupied The array which represents which cells in the CellLayout are occupied
     * @param blockOccupied The array which represents which cells in the specified block (cellX,
     *        cellY, spanX, spanY) are occupied. This is used when try to move a group of views.
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    private int[] findNearestArea(int cellX, int cellY, int spanX, int spanY, int[] direction,
            boolean[][] occupied, boolean blockOccupied[][], int[] result) {
        // Keep track of best-scoring drop area
        final int[] bestXY = result != null ? result : new int[2];
        float bestDistance = Float.MAX_VALUE;
        int bestDirectionScore = Integer.MIN_VALUE;

        final int countX = mCountX;
        final int countY = mCountY;

        for (int y = 0; y < countY - (spanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (spanX - 1); x++) {
                // First, let's see if this thing fits anywhere
                for (int i = 0; i < spanX; i++) {
                    for (int j = 0; j < spanY; j++) {
                        if (occupied[x + i][y + j] && (blockOccupied == null || blockOccupied[i][j])) {
                            continue inner;
                        }
                    }
                }

                float distance = (float)
                        Math.sqrt((x - cellX) * (x - cellX) + (y - cellY) * (y - cellY));
                int[] curDirection = mTmpPoint;
                computeDirectionVector(x - cellX, y - cellY, curDirection);
                // The direction score is just the dot product of the two candidate direction
                // and that passed in.
                int curDirectionScore = direction[0] * curDirection[0] +
                        direction[1] * curDirection[1];
                boolean exactDirectionOnly = false;
                /* YUNOS BEGIN */
                // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
                boolean directionMatches = (direction[0] == curDirection[0])
                        && (direction[1] == curDirection[1]);
                /* YUNOS END */
                if ((directionMatches || !exactDirectionOnly) &&
                        Float.compare(distance,  bestDistance) < 0 || (Float.compare(distance,
                        bestDistance) == 0 && curDirectionScore > bestDirectionScore)) {
                    bestDistance = distance;
                    bestDirectionScore = curDirectionScore;
                    bestXY[0] = x;
                    bestXY[1] = y;
                }
            }
        }

        // Return -1, -1 if no suitable location found
        if (bestDistance == Float.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        return bestXY;
    }

    private boolean addViewToTempLocation(View v, Rect rectOccupiedByPotentialDrop,
            int[] direction, ItemConfiguration currentState) {
        CellAndSpan c = currentState.map.get(v);
        boolean success = false;
        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        markCellsForRect(rectOccupiedByPotentialDrop, mTmpOccupied, true);

        findNearestArea(c.x, c.y, c.spanX, c.spanY, direction, mTmpOccupied, null, mTempLocation);

        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            c.x = mTempLocation[0];
            c.y = mTempLocation[1];
            success = true;
        }
        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        return success;
    }

    /**
     * This helper class defines a cluster of views. It helps with defining complex edges
     * of the cluster and determining how those edges interact with other views. The edges
     * essentially define a fine-grained boundary around the cluster of views -- like a more
     * precise version of a bounding box.
     */
    private class ViewCluster {
        final static int LEFT = 0;
        final static int TOP = 1;
        final static int RIGHT = 2;
        final static int BOTTOM = 3;

        ArrayList<View> views;
        ItemConfiguration config;
        Rect boundingRect = new Rect();

        int[] leftEdge = new int[mCountY];
        int[] rightEdge = new int[mCountY];
        int[] topEdge = new int[mCountX];
        int[] bottomEdge = new int[mCountX];
        boolean leftEdgeDirty, rightEdgeDirty, topEdgeDirty, bottomEdgeDirty, boundingRectDirty;

        @SuppressWarnings("unchecked")
        public ViewCluster(ArrayList<View> views, ItemConfiguration config) {
            this.views = (ArrayList<View>) views.clone();
            this.config = config;
            resetEdges();
        }

        final private void resetEdges() {
            for (int i = 0; i < mCountX; i++) {
                topEdge[i] = -1;
                bottomEdge[i] = -1;
            }
            for (int i = 0; i < mCountY; i++) {
                leftEdge[i] = -1;
                rightEdge[i] = -1;
            }
            leftEdgeDirty = true;
            rightEdgeDirty = true;
            bottomEdgeDirty = true;
            topEdgeDirty = true;
            boundingRectDirty = true;
        }

        void computeEdge(int which, int[] edge) {
            int count = views.size();
            for (int i = 0; i < count; i++) {
                CellAndSpan cs = config.map.get(views.get(i));
                switch (which) {
                    case LEFT:
                        int left = cs.x;
                        for (int j = cs.y; j < cs.y + cs.spanY; j++) {
                            if (left < edge[j] || edge[j] < 0) {
                                edge[j] = left;
                            }
                        }
                        break;
                    case RIGHT:
                        int right = cs.x + cs.spanX;
                        for (int j = cs.y; j < cs.y + cs.spanY; j++) {
                            if (right > edge[j]) {
                                edge[j] = right;
                            }
                        }
                        break;
                    case TOP:
                        int top = cs.y;
                        for (int j = cs.x; j < cs.x + cs.spanX; j++) {
                            if (top < edge[j] || edge[j] < 0) {
                                edge[j] = top;
                            }
                        }
                        break;
                    case BOTTOM:
                        int bottom = cs.y + cs.spanY;
                        for (int j = cs.x; j < cs.x + cs.spanX; j++) {
                            if (bottom > edge[j]) {
                                edge[j] = bottom;
                            }
                        }
                        break;
                }
            }
        }

        boolean isViewTouchingEdge(View v, int whichEdge) {
            CellAndSpan cs = config.map.get(v);

            int[] edge = getEdge(whichEdge);

            switch (whichEdge) {
                case LEFT:
                    for (int i = cs.y; i < cs.y + cs.spanY; i++) {
                        if (edge[i] == cs.x + cs.spanX) {
                            return true;
                        }
                    }
                    break;
                case RIGHT:
                    for (int i = cs.y; i < cs.y + cs.spanY; i++) {
                        if (edge[i] == cs.x) {
                            return true;
                        }
                    }
                    break;
                case TOP:
                    for (int i = cs.x; i < cs.x + cs.spanX; i++) {
                        if (edge[i] == cs.y + cs.spanY) {
                            return true;
                        }
                    }
                    break;
                case BOTTOM:
                    for (int i = cs.x; i < cs.x + cs.spanX; i++) {
                        if (edge[i] == cs.y) {
                            return true;
                        }
                    }
                    break;
            }
            return false;
        }

        void shift(int whichEdge, int delta) {
            for (View v: views) {
                CellAndSpan c = config.map.get(v);
                switch (whichEdge) {
                    case LEFT:
                        c.x -= delta;
                        break;
                    case RIGHT:
                        c.x += delta;
                        break;
                    case TOP:
                        c.y -= delta;
                        break;
                    case BOTTOM:
                    default:
                        c.y += delta;
                        break;
                }
            }
            resetEdges();
        }

        public void addView(View v) {
            views.add(v);
            resetEdges();
        }

        public Rect getBoundingRect() {
            if (boundingRectDirty) {
                boolean first = true;
                for (View v: views) {
                    CellAndSpan c = config.map.get(v);
                    if (first) {
                        boundingRect.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                        first = false;
                    } else {
                        boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                    }
                }
            }
            return boundingRect;
        }

        public int[] getEdge(int which) {
            switch (which) {
                case LEFT:
                    return getLeftEdge();
                case RIGHT:
                    return getRightEdge();
                case TOP:
                    return getTopEdge();
                case BOTTOM:
                default:
                    return getBottomEdge();
            }
        }

        public int[] getLeftEdge() {
            if (leftEdgeDirty) {
                computeEdge(LEFT, leftEdge);
            }
            return leftEdge;
        }

        public int[] getRightEdge() {
            if (rightEdgeDirty) {
                computeEdge(RIGHT, rightEdge);
            }
            return rightEdge;
        }

        public int[] getTopEdge() {
            if (topEdgeDirty) {
                computeEdge(TOP, topEdge);
            }
            return topEdge;
        }

        public int[] getBottomEdge() {
            if (bottomEdgeDirty) {
                computeEdge(BOTTOM, bottomEdge);
            }
            return bottomEdge;
        }

        PositionComparator comparator = new PositionComparator();
        class PositionComparator implements Comparator<View> {
            int whichEdge = 0;
            public int compare(View left, View right) {
                CellAndSpan l = config.map.get(left);
                CellAndSpan r = config.map.get(right);
                switch (whichEdge) {
                    case LEFT:
                        return (r.x + r.spanX) - (l.x + l.spanX);
                    case RIGHT:
                        return l.x - r.x;
                    case TOP:
                        return (r.y + r.spanY) - (l.y + l.spanY);
                    case BOTTOM:
                    default:
                        return l.y - r.y;
                }
            }
        }

        public void sortConfigurationForEdgePush(int edge) {
            comparator.whichEdge = edge;
            Collections.sort(config.sortedViews, comparator);
        }
    }

    private boolean pushViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
            int[] direction, View dragView, ItemConfiguration currentState) {

        ViewCluster cluster = new ViewCluster(views, currentState);
        Rect clusterRect = cluster.getBoundingRect();
        int whichEdge;
        int pushDistance;
        boolean fail = false;

        // Determine the edge of the cluster that will be leading the push and how far
        // the cluster must be shifted.
        if (direction[0] < 0) {
            whichEdge = ViewCluster.LEFT;
            pushDistance = clusterRect.right - rectOccupiedByPotentialDrop.left;
        } else if (direction[0] > 0) {
            whichEdge = ViewCluster.RIGHT;
            pushDistance = rectOccupiedByPotentialDrop.right - clusterRect.left;
        } else if (direction[1] < 0) {
            whichEdge = ViewCluster.TOP;
            pushDistance = clusterRect.bottom - rectOccupiedByPotentialDrop.top;
        } else {
            whichEdge = ViewCluster.BOTTOM;
            pushDistance = rectOccupiedByPotentialDrop.bottom - clusterRect.top;
        }

        // Break early for invalid push distance.
        if (pushDistance <= 0) {
            return false;
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        }

        // We save the current configuration -- if we fail to find a solution we will revert
        // to the initial state. The process of finding a solution modifies the configuration
        // in place, hence the need for revert in the failure case.
        currentState.save();

        // The pushing algorithm is simplified by considering the views in the order in which
        // they would be pushed by the cluster. For example, if the cluster is leading with its
        // left edge, we consider sort the views by their right edge, from right to left.
        cluster.sortConfigurationForEdgePush(whichEdge);

        while (pushDistance > 0 && !fail) {
            for (View v: currentState.sortedViews) {
                // For each view that isn't in the cluster, we see if the leading edge of the
                // cluster is contacting the edge of that view. If so, we add that view to the
                // cluster.
                if (!cluster.views.contains(v) && v != dragView) {
                    if (cluster.isViewTouchingEdge(v, whichEdge)) {
                        LayoutParams lp = (LayoutParams) v.getLayoutParams();
                        if (!lp.canReorder) {
                            // The push solution includes the all apps button, this is not viable.
                            fail = true;
                            break;
                        }
                        cluster.addView(v);
                        CellAndSpan c = currentState.map.get(v);

                        // Adding view to cluster, mark it as not occupied.
                        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
                    }
                }
            }
            pushDistance--;

            // The cluster has been completed, now we move the whole thing over in the appropriate
            // direction.
            cluster.shift(whichEdge, 1);
        }

        boolean foundSolution = false;
        clusterRect = cluster.getBoundingRect();

        // Due to the nature of the algorithm, the only check required to verify a valid solution
        // is to ensure that completed shifted cluster lies completely within the cell layout.
        if (!fail && clusterRect.left >= 0 && clusterRect.right <= mCountX && clusterRect.top >= 0 &&
                clusterRect.bottom <= mCountY) {
            foundSolution = true;
        } else {
            currentState.restore();
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (View v: cluster.views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        }

        return foundSolution;
    }

    private boolean addViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
            int[] direction, View dragView, ItemConfiguration currentState) {
        if (views.size() == 0) return true;

        boolean success = false;
        Rect boundingRect = null;
        // We construct a rect which represents the entire group of views passed in
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            if (boundingRect == null) {
                boundingRect = new Rect(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            } else {
                boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            }
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        }

        boolean[][] blockOccupied = new boolean[boundingRect.width()][boundingRect.height()];
        int top = boundingRect.top;
        int left = boundingRect.left;
        // We mark more precisely which parts of the bounding rect are truly occupied, allowing
        // for interlocking.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x - left, c.y - top, c.spanX, c.spanY, blockOccupied, true);
        }

        markCellsForRect(rectOccupiedByPotentialDrop, mTmpOccupied, true);

        findNearestArea(boundingRect.left, boundingRect.top, boundingRect.width(),
                boundingRect.height(), direction, mTmpOccupied, blockOccupied, mTempLocation);

        // If we successfuly found a location by pushing the block of views, we commit it
        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            int deltaX = mTempLocation[0] - boundingRect.left;
            int deltaY = mTempLocation[1] - boundingRect.top;
            for (View v: views) {
                CellAndSpan c = currentState.map.get(v);
                c.x += deltaX;
                c.y += deltaY;
            }
            success = true;
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        }
        return success;
    }

    private void markCellsForRect(Rect r, boolean[][] occupied, boolean value) {
        markCellsForView(r.left, r.top, r.width(), r.height(), occupied, value);
    }

    // This method tries to find a reordering solution which satisfies the push mechanic by trying
    // to push items in each of the cardinal directions, in an order based on the direction vector
    // passed.
    private boolean attemptPushInDirection(ArrayList<View> intersectingViews, Rect occupied,
            int[] direction, View ignoreView, ItemConfiguration solution) {
        if ((Math.abs(direction[0]) + Math.abs(direction[1])) > 1) {
            // If the direction vector has two non-zero components, we try pushing
            // separately in each of the components.
            int temp = direction[1];
            direction[1] = 0;

            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;

            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Revert the direction
            direction[0] = temp;

            // Now we try pushing in each component of the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            temp = direction[1];
            direction[1] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }

            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // revert the direction
            direction[0] = temp;
            direction[0] *= -1;
            direction[1] *= -1;

        } else {
            // If the direction vector has a single non-zero component, we push first in the
            // direction of the vector
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Then we try the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Switch the direction back
            direction[0] *= -1;
            direction[1] *= -1;

            // If we have failed to find a push solution with the above, then we try
            // to find a solution by pushing along the perpendicular axis.

            // Swap the components
            int temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }

            // Then we try the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Switch the direction back
            direction[0] *= -1;
            direction[1] *= -1;

            // Swap the components back
            temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
        }
        return false;
    }

    private boolean rearrangementExists(int cellX, int cellY, int spanX, int spanY, int[] direction,
            View ignoreView, ItemConfiguration solution) {
        // Return early if get invalid cell positions
        if (cellX < 0 || cellY < 0) return false;

        mIntersectingViews.clear();
        mOccupiedRect.set(cellX, cellY, cellX + spanX, cellY + spanY);

        // Mark the desired location of the view currently being dragged.
        if (ignoreView != null) {
            CellAndSpan c = solution.map.get(ignoreView);
            if (c != null) {
                c.x = cellX;
                c.y = cellY;
            }
        }
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        for (View child: solution.map.keySet()) {
            if (child == ignoreView) continue;
            CellAndSpan c = solution.map.get(child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            r1.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            if (Rect.intersects(r0, r1)) {
                if (!lp.canReorder) {
                    return false;
                }
                mIntersectingViews.add(child);
            }
        }

        // First we try to find a solution which respects the push mechanic. That is,
        // we try to find a solution such that no displaced item travels through another item
        // without also displacing that item.
        if (attemptPushInDirection(mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        // Next we try moving the views as a block, but without requiring the push mechanic.
        if (addViewsToTempLocation(mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        // Ok, they couldn't move as a block, let's move them individually
        for (View v : mIntersectingViews) {
            if (!addViewToTempLocation(v, mOccupiedRect, direction, solution)) {
                return false;
            }
        }
        return true;
    }

    /*
     * Returns a pair (x, y), where x,y are in {-1, 0, 1} corresponding to vector between
     * the provided point and the provided cell
     */
    private void computeDirectionVector(float deltaX, float deltaY, int[] result) {
        double angle = Math.atan(((float) deltaY) / deltaX);

        result[0] = 0;
        result[1] = 0;
        if (Math.abs(Math.cos(angle)) > 0.5f) {
            result[0] = (int) Math.signum(deltaX);
        }
        if (Math.abs(Math.sin(angle)) > 0.5f) {
            result[1] = (int) Math.signum(deltaY);
        }
    }

    private void copyOccupiedArray(boolean[][] occupied) {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                occupied[i][j] = mOccupied[i][j];
            }
        }
    }

    ItemConfiguration simpleSwap(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX,
            int spanY, int[] direction, View dragView, boolean decX, ItemConfiguration solution) {
        // Copy the current state into the solution. This solution will be manipulated as necessary.
        copyCurrentStateToSolution(solution, false);
        // Copy the current occupied array into the temporary occupied array. This array will be
        // manipulated as necessary to find a solution.
        copyOccupiedArray(mTmpOccupied);

        // We find the nearest cell into which we would place the dragged item, assuming there's
        // nothing in its way.
        int result[] = new int[2];
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);

        boolean success = false;
        // First we try the exact nearest position of the item being dragged,
        // we will then want to try to move this around to other neighbouring positions
        success = rearrangementExists(result[0], result[1], spanX, spanY, direction, dragView,
                solution);

        if (!success) {
            // We try shrinking the widget down to size in an alternating pattern, shrink 1 in
            // x, then 1 in y etc.
            if (spanX > minSpanX && (minSpanY == spanY || decX)) {
                return simpleSwap(pixelX, pixelY, minSpanX, minSpanY, spanX - 1, spanY, direction,
                        dragView, false, solution);
            } else if (spanY > minSpanY) {
                return simpleSwap(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY - 1, direction,
                        dragView, true, solution);
            }
            solution.isSolution = false;
        } else {
            solution.isSolution = true;
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = spanX;
            solution.dragViewSpanY = spanY;
        }
        return solution;
    }

    private void copyCurrentStateToSolution(ItemConfiguration solution, boolean temp) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c;
            if (temp) {
                c = new CellAndSpan(lp.tmpCellX, lp.tmpCellY, lp.cellHSpan, lp.cellVSpan);
            } else {
                c = new CellAndSpan(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan);
            }
            solution.add(child, c);
        }
    }

    private void copySolutionToTempState(ItemConfiguration solution, View dragView) {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                mTmpOccupied[i][j] = false;
            }
        }

        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                lp.tmpCellX = c.x;
                lp.tmpCellY = c.y;
                lp.cellHSpan = c.spanX;
                lp.cellVSpan = c.spanY;
                markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
            }
        }
        markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX,
                solution.dragViewSpanY, mTmpOccupied, true);
    }

    private void animateItemsToSolution(ItemConfiguration solution, View dragView, boolean
            commitDragView) {

        boolean[][] occupied = DESTRUCTIVE_REORDER ? mOccupied : mTmpOccupied;
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                occupied[i][j] = false;
            }
        }

        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                animateChildToPosition(child, c.x, c.y, REORDER_ANIMATION_DURATION, 0,
                        DESTRUCTIVE_REORDER, false);
                markCellsForView(c.x, c.y, c.spanX, c.spanY, occupied, true);
            }
        }
        if (commitDragView) {
            markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX,
                    solution.dragViewSpanY, occupied, true);
        }
    }

    // This method starts or changes the reorder hint animations
    private void beginOrAdjustHintAnimations(ItemConfiguration solution, View dragView, int delay) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (c != null) {
                ReorderHintAnimation rha = new ReorderHintAnimation(child, lp.cellX, lp.cellY,
                        c.x, c.y, c.spanX, c.spanY);
                rha.animate();
            }
        }
    }

    // Class which represents the reorder hint animations. These animations show that an item is
    // in a temporary state, and hint at where the item will return to.
    class ReorderHintAnimation {
        View child;
        float finalDeltaX;
        float finalDeltaY;
        float initDeltaX;
        float initDeltaY;
        float finalScale;
        float initScale;
        private static final int DURATION = 300;
        Animator a;

        public ReorderHintAnimation(View child, int cellX0, int cellY0, int cellX1, int cellY1,
                int spanX, int spanY) {
            regionToCenterPoint(cellX0, cellY0, spanX, spanY, mTmpPoint);
            final int x0 = mTmpPoint[0];
            final int y0 = mTmpPoint[1];
            regionToCenterPoint(cellX1, cellY1, spanX, spanY, mTmpPoint);
            final int x1 = mTmpPoint[0];
            final int y1 = mTmpPoint[1];
            final int dX = x1 - x0;
            final int dY = y1 - y0;
            finalDeltaX = 0;
            finalDeltaY = 0;
            if (dX == dY && dX == 0) {
            } else {
                if (dY == 0) {
                    finalDeltaX = - Math.signum(dX) * mReorderHintAnimationMagnitude;
                } else if (dX == 0) {
                    finalDeltaY = - Math.signum(dY) * mReorderHintAnimationMagnitude;
                } else {
                    double angle = Math.atan( (float) (dY) / dX);
                    finalDeltaX = (int) (- Math.signum(dX) *
                            Math.abs(Math.cos(angle) * mReorderHintAnimationMagnitude));
                    finalDeltaY = (int) (- Math.signum(dY) *
                            Math.abs(Math.sin(angle) * mReorderHintAnimationMagnitude));
                }
            }
            initDeltaX = child.getTranslationX();
            initDeltaY = child.getTranslationY();

            finalScale = getChildrenScale() - 4.0f / child.getWidth();
            initScale = child.getScaleX();
            this.child = child;
        }

        void animate() {
            if (mShakeAnimators.containsKey(child)) {
                ReorderHintAnimation oldAnimation = mShakeAnimators.get(child);
                oldAnimation.cancel();
                mShakeAnimators.remove(child);
                if (finalDeltaX == 0 && finalDeltaY == 0) {
                    completeAnimationImmediately();
                    return;
                }
            }
            if (finalDeltaX == 0 && finalDeltaY == 0) {
                return;
            }
            ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
            a = va;
            va.setRepeatMode(ValueAnimator.REVERSE);
            va.setRepeatCount(ValueAnimator.INFINITE);
            va.setDuration(DURATION);
            va.setStartDelay((int) (Math.random() * 60));
            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    float x = r * finalDeltaX + (1 - r) * initDeltaX;
                    float y = r * finalDeltaY + (1 - r) * initDeltaY;
                    child.setTranslationX(x);
                    child.setTranslationY(y);
                    float s = r * finalScale + (1 - r) * initScale;
                    child.setScaleX(s);
                    child.setScaleY(s);
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                public void onAnimationRepeat(Animator animation) {
                    // We make sure to end only after a full period
                    initDeltaX = 0;
                    initDeltaY = 0;
                    initScale = getChildrenScale();
                }
            });
            mShakeAnimators.put(child, this);
            va.start();
        }

        private void cancel() {
            if (a != null) {
                a.cancel();
            }
        }

        private void completeAnimationImmediately() {
            if (a != null) {
                a.cancel();
            }

            AnimatorSet s = LauncherAnimUtils.createAnimatorSet();
            a = s;
            s.playTogether(
                LauncherAnimUtils.ofFloat(child, "scaleX", getChildrenScale()),
                LauncherAnimUtils.ofFloat(child, "scaleY", getChildrenScale()),
                LauncherAnimUtils.ofFloat(child, "translationX", 0f),
                LauncherAnimUtils.ofFloat(child, "translationY", 0f)
            );
            s.setDuration(REORDER_ANIMATION_DURATION);
            s.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
            s.start();
        }
    }

    private void completeAndClearReorderHintAnimations() {
        for (ReorderHintAnimation a: mShakeAnimators.values()) {
            a.completeAnimationImmediately();
        }
        mShakeAnimators.clear();
    }

    /* YUNOS BEGIN */
    // ##date:2014/10/16 ##author:zhanggong.zg ##BugID:5252746
    /**
     * This listener is used to track the reorder animation in this cell layout.
     */
    public interface ReorderAnimationListener {
        void reorderAnimationStart(CellLayout cellLayout);
        void reorderAnimationFinish(CellLayout cellLayout);
    }

    /** @see ReorderAnimationListener */
    public void setReorderAnimationListener(ReorderAnimationListener listener) {
        mReorderAnimationListener = listener;
    }
    /* YUNOS END */

    private void commitTempPlacement() {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                mOccupied[i][j] = mTmpOccupied[i][j];
            }
        }
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            ItemInfo info = (ItemInfo) child.getTag();
            // We do a null check here because the item info can be null in the case of the
            // AllApps button in the hotseat.
            if (info != null) {
                if (info.cellX != lp.tmpCellX || info.cellY != lp.tmpCellY ||
                        info.spanX != lp.cellHSpan || info.spanY != lp.cellVSpan) {
                    info.requiresDbUpdate = true;
                }
                info.cellX = lp.cellX = lp.tmpCellX;
                info.cellY = lp.cellY = lp.tmpCellY;
                info.spanX = lp.cellHSpan;
                info.spanY = lp.cellVSpan;
            }
        }
        mLauncher.getWorkspace().updateItemLocationsInDatabase(this);
    }

    public void setUseTempCoords(boolean useTempCoords) {
        int childCount = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) mShortcutsAndWidgets.getChildAt(i).getLayoutParams();
            lp.useTmpCoords = useTempCoords;
        }
    }

    ItemConfiguration findConfigurationNoShuffle(int pixelX, int pixelY, int minSpanX, int minSpanY,
            int spanX, int spanY, View dragView, ItemConfiguration solution) {
        int[] result = new int[2];
        int[] resultSpan = new int[2];
        findNearestVacantArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, null, result,
                resultSpan);
        if (result[0] >= 0 && result[1] >= 0) {
            copyCurrentStateToSolution(solution, false);
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = resultSpan[0];
            solution.dragViewSpanY = resultSpan[1];
            solution.isSolution = true;
        } else {
            solution.isSolution = false;
        }
        return solution;
    }

    public void prepareChildForDrag(View child) {
        markCellsAsUnoccupiedForView(child);
    }

    /* This seems like it should be obvious and straight-forward, but when the direction vector
    needs to match with the notion of the dragView pushing other views, we have to employ
    a slightly more subtle notion of the direction vector. The question is what two points is
    the vector between? The center of the dragView and its desired destination? Not quite, as
    this doesn't necessarily coincide with the interaction of the dragView and items occupying
    those cells. Instead we use some heuristics to often lock the vector to up, down, left
    or right, which helps make pushing feel right.
    */
    private void getDirectionVectorForDrop(int dragViewCenterX, int dragViewCenterY, int spanX,
            int spanY, View dragView, int[] resultDirection) {
        int[] targetDestination = new int[2];

        findNearestArea(dragViewCenterX, dragViewCenterY, spanX, spanY, targetDestination);
        Rect dragRect = new Rect();
        regionToRect(targetDestination[0], targetDestination[1], spanX, spanY, dragRect);
        dragRect.offset(dragViewCenterX - dragRect.centerX(), dragViewCenterY - dragRect.centerY());

        Rect dropRegionRect = new Rect();
        getViewsIntersectingRegion(targetDestination[0], targetDestination[1], spanX, spanY,
                dragView, dropRegionRect, mIntersectingViews);

        int dropRegionSpanX = dropRegionRect.width();
        int dropRegionSpanY = dropRegionRect.height();

        regionToRect(dropRegionRect.left, dropRegionRect.top, dropRegionRect.width(),
                dropRegionRect.height(), dropRegionRect);
        /*YUNOS BEGIN*/
        //##date:2013/12/5 ##author:xindong.zxd
        //spanX,spanY not 0
        if (spanX == 0 || spanY == 0) {
            return;
        }
        /*YUNOS END*/
        int deltaX = (dropRegionRect.centerX() - dragViewCenterX) / spanX;
        int deltaY = (dropRegionRect.centerY() - dragViewCenterY) / spanY;

        if (dropRegionSpanX == mCountX || spanX == mCountX) {
            deltaX = 0;
        }
        if (dropRegionSpanY == mCountY || spanY == mCountY) {
            deltaY = 0;
        }

        if (deltaX == 0 && deltaY == 0) {
            // No idea what to do, give a random direction.
            resultDirection[0] = 1;
            resultDirection[1] = 0;
        } else {
            computeDirectionVector(deltaX, deltaY, resultDirection);
        }
    }

    // For a given cell and span, fetch the set of views intersecting the region.
    private void getViewsIntersectingRegion(int cellX, int cellY, int spanX, int spanY,
            View dragView, Rect boundingRect, ArrayList<View> intersectingViews) {
        if (boundingRect != null) {
            boundingRect.set(cellX, cellY, cellX + spanX, cellY + spanY);
        }
        intersectingViews.clear();
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        final int count = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            if (child == dragView) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            r1.set(lp.cellX, lp.cellY, lp.cellX + lp.cellHSpan, lp.cellY + lp.cellVSpan);
            if (Rect.intersects(r0, r1)) {
                mIntersectingViews.add(child);
                if (boundingRect != null) {
                    boundingRect.union(r1);
                }
            }
        }
    }

    boolean isNearestDropLocationOccupied(int pixelX, int pixelY, int spanX, int spanY,
            View dragView, int[] result) {
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);
        getViewsIntersectingRegion(result[0], result[1], spanX, spanY, dragView, null,
                mIntersectingViews);
        return !mIntersectingViews.isEmpty();
    }

    void revertTempState() {
        if (!isItemPlacementDirty() || DESTRUCTIVE_REORDER) return;
        final int count = mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mShortcutsAndWidgets.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY) {
                lp.tmpCellX = lp.cellX;
                lp.tmpCellY = lp.cellY;
                animateChildToPosition(child, lp.cellX, lp.cellY, REORDER_ANIMATION_DURATION,
                        0, false, false);
            }
        }
        completeAndClearReorderHintAnimations();
        setItemPlacementDirty(false);
    }

    boolean createAreaForResize(int cellX, int cellY, int spanX, int spanY,
            View dragView, int[] direction, boolean commit) {
        int[] pixelXY = new int[2];
        regionToCenterPoint(cellX, cellY, spanX, spanY, pixelXY);

        // First we determine if things have moved enough to cause a different layout
        ItemConfiguration swapSolution = simpleSwap(pixelXY[0], pixelXY[1], spanX, spanY,
                 spanX,  spanY, direction, dragView,  true,  new ItemConfiguration());

        setUseTempCoords(true);
        if (swapSolution != null && swapSolution.isSolution) {
            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            copySolutionToTempState(swapSolution, dragView);
            setItemPlacementDirty(true);
            animateItemsToSolution(swapSolution, dragView, commit);

            if (commit) {
                commitTempPlacement();
                completeAndClearReorderHintAnimations();
                setItemPlacementDirty(false);
            } else {
                beginOrAdjustHintAnimations(swapSolution, dragView,
                        REORDER_ANIMATION_DURATION);
            }
            mShortcutsAndWidgets.requestLayout();
        }
        /* YUNOS BEGIN */
        // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
        if (swapSolution != null) {
            return swapSolution.isSolution;
        }
        return false;
        /* YUNOS END */
    }

    int[] createArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View dragView, int[] result, int resultSpan[], int mode) {
        // First we determine if things have moved enough to cause a different layout
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);

        if (resultSpan == null) {
            resultSpan = new int[2];
        }

        // When we are checking drop validity or actually dropping, we don't recompute the
        // direction vector, since we want the solution to match the preview, and it's possible
        // that the exact position of the item has changed to result in a new reordering outcome.
        if ((mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL || mode == MODE_ACCEPT_DROP)
               && mPreviousReorderDirection[0] != INVALID_DIRECTION) {
            mDirectionVector[0] = mPreviousReorderDirection[0];
            mDirectionVector[1] = mPreviousReorderDirection[1];
            // We reset this vector after drop
            if (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                mPreviousReorderDirection[0] = INVALID_DIRECTION;
                mPreviousReorderDirection[1] = INVALID_DIRECTION;
            }
        } else {
            getDirectionVectorForDrop(pixelX, pixelY, spanX, spanY, dragView, mDirectionVector);
            mPreviousReorderDirection[0] = mDirectionVector[0];
            mPreviousReorderDirection[1] = mDirectionVector[1];
        }

        ItemConfiguration swapSolution = simpleSwap(pixelX, pixelY, minSpanX, minSpanY,
                 spanX,  spanY, mDirectionVector, dragView,  true,  new ItemConfiguration());

        // We attempt the approach which doesn't shuffle views at all
        ItemConfiguration noShuffleSolution = findConfigurationNoShuffle(pixelX, pixelY, minSpanX,
                minSpanY, spanX, spanY, dragView, new ItemConfiguration());

        ItemConfiguration finalSolution = null;
        if (swapSolution.isSolution && swapSolution.area() >= noShuffleSolution.area()) {
            finalSolution = swapSolution;
        } else if (noShuffleSolution.isSolution) {
            finalSolution = noShuffleSolution;
        }

        boolean foundSolution = true;
        if (!DESTRUCTIVE_REORDER) {
            setUseTempCoords(true);
        }

        if (finalSolution != null) {
            result[0] = finalSolution.dragViewX;
            result[1] = finalSolution.dragViewY;
            resultSpan[0] = finalSolution.dragViewSpanX;
            resultSpan[1] = finalSolution.dragViewSpanY;

            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            if (mode == MODE_DRAG_OVER || mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                if (!DESTRUCTIVE_REORDER) {
                    copySolutionToTempState(finalSolution, dragView);
                }
                setItemPlacementDirty(true);
                animateItemsToSolution(finalSolution, dragView, mode == MODE_ON_DROP);

                if (!DESTRUCTIVE_REORDER &&
                        (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL)) {
                    commitTempPlacement();
                    completeAndClearReorderHintAnimations();
                    setItemPlacementDirty(false);
                } else {
                    beginOrAdjustHintAnimations(finalSolution, dragView,
                            REORDER_ANIMATION_DURATION);
                }
            }
        } else {
            foundSolution = false;
            result[0] = result[1] = resultSpan[0] = resultSpan[1] = -1;
        }

        if ((mode == MODE_ON_DROP || !foundSolution) && !DESTRUCTIVE_REORDER) {
            setUseTempCoords(false);
        }

        mShortcutsAndWidgets.requestLayout();
        return result;
    }

    void setItemPlacementDirty(boolean dirty) {
        mItemPlacementDirty = dirty;
    }
    boolean isItemPlacementDirty() {
        return mItemPlacementDirty;
    }

    private class ItemConfiguration {
        HashMap<View, CellAndSpan> map = new HashMap<View, CellAndSpan>();
        private HashMap<View, CellAndSpan> savedMap = new HashMap<View, CellAndSpan>();
        ArrayList<View> sortedViews = new ArrayList<View>();
        boolean isSolution = false;
        int dragViewX, dragViewY, dragViewSpanX, dragViewSpanY;

        void save() {
            // Copy current state into savedMap
            for (View v: map.keySet()) {
                map.get(v).copy(savedMap.get(v));
            }
        }

        void restore() {
            // Restore current state from savedMap
            for (View v: savedMap.keySet()) {
                savedMap.get(v).copy(map.get(v));
            }
        }

        void add(View v, CellAndSpan cs) {
            map.put(v, cs);
            savedMap.put(v, new CellAndSpan());
            sortedViews.add(v);
        }

        int area() {
            return dragViewSpanX * dragViewSpanY;
        }
    }

    private class CellAndSpan {
        int x, y;
        int spanX, spanY;

        public CellAndSpan() {
        }

        public void copy(CellAndSpan copy) {
            copy.x = x;
            copy.y = y;
            copy.spanX = spanX;
            copy.spanY = spanY;
        }

        public CellAndSpan(int x, int y, int spanX, int spanY) {
            this.x = x;
            this.y = y;
            this.spanX = spanX;
            this.spanY = spanY;
        }

        public String toString() {
            return "(" + x + ", " + y + ": " + spanX + ", " + spanY + ")";
        }

    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreView Considers space occupied by this view as unoccupied
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestVacantArea(
            int pixelX, int pixelY, int spanX, int spanY, View ignoreView, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY, ignoreView, true, result);
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreView Considers space occupied by this view as unoccupied
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestVacantArea(int pixelX, int pixelY, int minSpanX, int minSpanY,
            int spanX, int spanY, View ignoreView, int[] result, int[] resultSpan) {
        return findNearestArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, ignoreView, true,
                result, resultSpan, mOccupied);
    }

    /**
     * Find a starting cell position that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreView Considers space occupied by this view as unoccupied
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    public int[] findNearestArea(
            int pixelX, int pixelY, int spanX, int spanY, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY, null, false, result);
    }

    boolean existsEmptyCell() {
        return findCellForSpan(null, 1, 1);
    }

    /**
     * Finds the upper-left coordinate of the first rectangle in the grid that can
     * hold a cell of the specified dimensions. If intersectX and intersectY are not -1,
     * then this method will only return coordinates for rectangles that contain the cell
     * (intersectX, intersectY)
     *
     * @param cellXY The array that will contain the position of a vacant cell if such a cell
     *               can be found.
     * @param spanX The horizontal span of the cell we want to find.
     * @param spanY The vertical span of the cell we want to find.
     *
     * @return True if a vacant cell of the specified dimension was found, false otherwise.
     */
    public boolean findCellForSpan(int[] cellXY, int spanX, int spanY) {
        return findCellForSpanThatIntersectsIgnoring(cellXY, spanX, spanY, -1, -1, null, mOccupied);
    }

    /**
     * Like above, but ignores any cells occupied by the item "ignoreView"
     *
     * @param cellXY The array that will contain the position of a vacant cell if such a cell
     *               can be found.
     * @param spanX The horizontal span of the cell we want to find.
     * @param spanY The vertical span of the cell we want to find.
     * @param ignoreView The home screen item we should treat as not occupying any space
     * @return
     */
    boolean findCellForSpanIgnoring(int[] cellXY, int spanX, int spanY, View ignoreView) {
        return findCellForSpanThatIntersectsIgnoring(cellXY, spanX, spanY, -1, -1,
                ignoreView, mOccupied);
    }

    /**
     * Like above, but if intersectX and intersectY are not -1, then this method will try to
     * return coordinates for rectangles that contain the cell [intersectX, intersectY]
     *
     * @param spanX The horizontal span of the cell we want to find.
     * @param spanY The vertical span of the cell we want to find.
     * @param ignoreView The home screen item we should treat as not occupying any space
     * @param intersectX The X coordinate of the cell that we should try to overlap
     * @param intersectX The Y coordinate of the cell that we should try to overlap
     *
     * @return True if a vacant cell of the specified dimension was found, false otherwise.
     */
    boolean findCellForSpanThatIntersects(int[] cellXY, int spanX, int spanY,
            int intersectX, int intersectY) {
        return findCellForSpanThatIntersectsIgnoring(
                cellXY, spanX, spanY, intersectX, intersectY, null, mOccupied);
    }

    /**
     * The superset of the above two methods
     */
    boolean findCellForSpanThatIntersectsIgnoring(int[] cellXY, int spanX, int spanY,
            int intersectX, int intersectY, View ignoreView, boolean occupied[][]) {
        // mark space take by ignoreView as available (method checks if ignoreView is null)
        markCellsAsUnoccupiedForView(ignoreView, occupied);

        boolean foundCell = false;
        while (true) {
            int startX = 0;
            if (intersectX >= 0) {
                startX = Math.max(startX, intersectX - (spanX - 1));
            }
            int endX = mCountX - (spanX - 1);
            if (intersectX >= 0) {
                endX = Math.min(endX, intersectX + (spanX - 1) + (spanX == 1 ? 1 : 0));
            }
            int startY = 0;
            if (intersectY >= 0) {
                startY = Math.max(startY, intersectY - (spanY - 1));
            }
            int endY = mCountY - (spanY - 1);
            if (intersectY >= 0) {
                endY = Math.min(endY, intersectY + (spanY - 1) + (spanY == 1 ? 1 : 0));
            }

            for (int y = startY; y < endY && !foundCell; y++) {
                inner:
                for (int x = startX; x < endX; x++) {
                    for (int i = 0; i < spanX; i++) {
                        for (int j = 0; j < spanY; j++) {
                            if (occupied[x + i][y + j]) {
                                // small optimization: we can skip to after the column we just found
                                // an occupied cell
                                x += i;
                                continue inner;
                            }
                        }
                    }
                    if (cellXY != null) {
                        cellXY[0] = x;
                        cellXY[1] = y;
                    }
                    foundCell = true;
                    break; //NOPMD
                }
            }
            if (intersectX == -1 && intersectY == -1) {
                break;
            } else {
                // if we failed to find anything, try again but without any requirements of
                // intersecting
                intersectX = -1;
                intersectY = -1;
                continue;
            }
        }

        // re-mark space taken by ignoreView as occupied
        markCellsAsOccupiedForView(ignoreView, occupied);
        return foundCell;
    }

    /**
     * A drag event has begun over this layout.
     * It may have begun over this layout (in which case onDragChild is called first),
     * or it may have begun on another layout.
     */
    void onDragEnter() {
        mDragEnforcer.onDragEnter();
        mDragging = true;
    }

    /**
     * Called when drag has left this CellLayout or has been completed (successfully or not)
     */
    void onDragExit() {
        mDragEnforcer.onDragExit();
        // This can actually be called when we aren't in a drag, e.g. when adding a new
        // item to this layout via the customize drawer.
        // Guard against that case.
        if (mDragging) {
            mDragging = false;
        }
        // Invalidate the drag data
        mDragCell[0] = mDragCell[1] = -1;
        mDragOutlineAnims[mDragOutlineCurrent].animateOut();
        mDragOutlineCurrent = (mDragOutlineCurrent + 1) % mDragOutlineAnims.length;
        revertTempState();
        setIsDragOverlapping(false);
    }

    /**
     * Mark a child as having been dropped.
     * At the beginning of the drag operation, the child may have been on another
     * screen, but it is re-parented before this method is called.
     *
     * @param child The child that is being dropped
     */
    void onDropChild(final View child) {
        if (child != null && child.getLayoutParams() instanceof LayoutParams) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.dropped = true;
            child.requestLayout();

            if (isHideseatOpen) {
                final int y = lp.cellY;
                int vSpan = lp.cellVSpan;
                if (vSpan > 1) {
                    if (y < HIDESEAT_CELLY && y + vSpan > HIDESEAT_CELLY) {
                        post(new Runnable() {
                            public void run() {
                               buildChildCache(child, true);
                            }
                        });
                    } else {
                        clearChildCache(child);
                    }
                }
            }
        }
    }

    /**
     * Computes a bounding rectangle for a range of cells
     *
     * @param cellX X coordinate of upper left corner expressed as a cell position
     * @param cellY Y coordinate of upper left corner expressed as a cell position
     * @param cellHSpan Width in cells
     * @param cellVSpan Height in cells
     * @param resultRect Rect into which to put the results
     */
    public void cellToRect(int cellX, int cellY, int cellHSpan, int cellVSpan, Rect resultRect) {
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;
        final int widthGap = mWidthGap;
        final int heightGap = mHeightGap;

        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();

        int width = cellHSpan * cellWidth + ((cellHSpan - 1) * widthGap);
        int height = cellVSpan * cellHeight + ((cellVSpan - 1) * heightGap);

        int x = hStartPadding + cellX * (cellWidth + widthGap);
        int y = vStartPadding + cellY * (cellHeight + heightGap);

        resultRect.set(x, y, x + width, y + height);
    }

    /**
     * Computes the required horizontal and vertical cell spans to always
     * fit the given rectangle.
     *
     * @param width Width in pixels
     * @param height Height in pixels
     * @param result An array of length 2 in which to store the result (may be null).
     */
    public int[] rectToCell(int width, int height, int[] result) {
        return rectToCell(getResources(), width, height, result, LauncherModel.getCellCountX(), LauncherModel.getCellCountY());
    }

    public static int[] rectToCell(Resources resources, int width, int height, int[] result, int countX, int countY) {
        // Always assume we're working with the smallest span to make sure we
        // reserve enough space in both orientations.
        int actualWidth = resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        int actualHeight = resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        int smallerSize = Math.min(actualWidth, actualHeight);

        // Always round up to next largest cell
        int spanX = (int) Math.ceil(width / (float) smallerSize);
        int spanY = (int) Math.ceil(height / (float) smallerSize);

        spanX = Math.min(spanX, countX);
        spanY = Math.min(spanY, countY);

        if (result == null) {
            return new int[] { spanX, spanY };
        }
        result[0] = spanX;
        result[1] = spanY;
        return result;
    }

    public int[] cellSpansToSize(int hSpans, int vSpans) {
        int[] size = new int[2];
        size[0] = hSpans * mCellWidth + (hSpans - 1) * mWidthGap;
        size[1] = vSpans * mCellHeight + (vSpans - 1) * mHeightGap;
        return size;
    }

    /**
     * Calculate the grid spans needed to fit given item
     */
    public void calculateSpans(ItemInfo info) {
        final int minWidth;
        final int minHeight;

        if (info instanceof LauncherAppWidgetInfo) {
            minWidth = ((LauncherAppWidgetInfo) info).minWidth;
            minHeight = ((LauncherAppWidgetInfo) info).minHeight;
        } else if (info instanceof PendingAddWidgetInfo) {
            minWidth = ((PendingAddWidgetInfo) info).minWidth;
            minHeight = ((PendingAddWidgetInfo) info).minHeight;

            /* YUNOS BEGIN */
            // ##date:2014/3/18 ##author:zhangqiang.zq
            // BugID:101468
        } else if (info instanceof PendingAddGadgetInfo) {
            return;
            /* YUNOS END */

        } else {
            // It's not a widget, so it must be 1x1
            info.spanX = info.spanY = 1;
            return;
        }
        int[] spans = rectToCell(minWidth, minHeight, null);
        info.spanX = spans[0];
        info.spanY = spans[1];
    }

    /**
     * Find the first vacant cell, if there is one.
     *
     * @param vacant Holds the x and y coordinate of the vacant cell
     * @param spanX Horizontal cell span.
     * @param spanY Vertical cell span.
     *
     * @return True if a vacant cell was found
     */
    public boolean getVacantCell(int[] vacant, int spanX, int spanY) {

        return findVacantCell(vacant, spanX, spanY, mCountX, mCountY, mOccupied);
    }

    static boolean findVacantCell(int[] vacant, int spanX, int spanY,
            int xCount, int yCount, boolean[][] occupied) {

        for (int y = 0; y < yCount; y++) {
            for (int x = 0; x < xCount; x++) {
                boolean available = !occupied[x][y];
out:            for (int i = x; i < x + spanX - 1 && x < xCount; i++) {
                    for (int j = y; j < y + spanY - 1 && y < yCount; j++) {
                        available = available && !occupied[i][j];
                        if (!available) break out;
                    }
                }

                if (available) {
                    vacant[0] = x;
                    vacant[1] = y;
                    return true;
                }
            }
        }

        return false;
    }

    private void clearOccupiedCells() {
        for (int x = 0; x < mCountX; x++) {
            for (int y = 0; y < mCountY; y++) {
                mOccupied[x][y] = false;
            }
        }
    }

    public void onMove(View view, int newCellX, int newCellY, int newSpanX, int newSpanY) {
        markCellsAsUnoccupiedForView(view);
        markCellsForView(newCellX, newCellY, newSpanX, newSpanY, mOccupied, true);
    }

    public void markCellsAsOccupiedForView(View view) {
        markCellsAsOccupiedForView(view, mOccupied);
    }
    public void markCellsAsOccupiedForView(View view, boolean[][] occupied) {
        if (view == null || view.getParent() != mShortcutsAndWidgets) return;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, occupied, true);
    }

    public void markCellsAsUnoccupiedForView(View view) {
        markCellsAsUnoccupiedForView(view, mOccupied);
    }
    public void markCellsAsUnoccupiedForView(View view, boolean occupied[][]) {
        if (view == null || view.getParent() != mShortcutsAndWidgets) return;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, occupied, false);
        ItemInfo info = (ItemInfo) (view.getTag());
        if (ConfigManager.isLandOrienSupport() && info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            LauncherModel.markCellsOccupiedInNonCurrent(info, false);
        }
    }

    private void markCellsForView(int cellX, int cellY, int spanX, int spanY, boolean[][] occupied,
            boolean value) {
        if (cellX < 0 || cellY < 0) return;
        for (int x = cellX; x < cellX + spanX && x < mCountX; x++) {
            for (int y = cellY; y < cellY + spanY && y < mCountY; y++) {
                occupied[x][y] = value;
                if (occupied == mOccupied) {
                    LauncherModel.dumpUIOccupied();
                }
            }
        }
    }

    public int getDesiredWidth() {
        return getPaddingLeft() + getPaddingRight() + (mCountX * mCellWidth) +
                (Math.max((mCountX - 1), 0) * mWidthGap);
    }

    public int getDesiredHeight()  {
        return getPaddingTop() + getPaddingBottom() + (mCountY * mCellHeight) +
                (Math.max((mCountY - 1), 0) * mHeightGap);
    }

    public boolean isOccupied(int x, int y) {
        if (x < mCountX && y < mCountY) {
            return mOccupied[x][y];
        } else {
            throw new RuntimeException("Position exceeds the bound of this CellLayout");
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CellLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CellLayout.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CellLayout.LayoutParams(p);
    }

    public static class CellLayoutAnimationController extends LayoutAnimationController {
        public CellLayoutAnimationController(Animation animation, float delay) {
            super(animation, delay);
        }

        @Override
        protected long getDelayForView(View view) {
            return (int) (Math.random() * 150);
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        /**
         * Horizontal location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellX;

        /**
         * Vertical location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellY;

        /**
         * Temporary horizontal location of the item in the grid during reorder
         */
        public int tmpCellX;

        /**
         * Temporary vertical location of the item in the grid during reorder
         */
        public int tmpCellY;

        /**
         * Indicates that the temporary coordinates should be used to layout the items
         */
        public boolean useTmpCoords;

        /**
         * Number of cells spanned horizontally by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellHSpan;

        /**
         * Number of cells spanned vertically by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellVSpan;

        /**
         * Indicates whether the item will set its x, y, width and height parameters freely,
         * or whether these will be computed based on cellX, cellY, cellHSpan and cellVSpan.
         */
        public boolean isLockedToGrid = true;

        /**
         * Indicates whether this item can be reordered. Always true except in the case of the
         * the AllApps button.
         */
        public boolean canReorder = true;

        // X coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        public int x;
        // Y coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        public int y;

        boolean dropped;
        boolean startWithGap;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.cellX = source.cellX;
            this.cellY = source.cellY;
            this.cellHSpan = source.cellHSpan;
            this.cellVSpan = source.cellVSpan;
        }

        public LayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            this.cellX = cellX;
            this.cellY = cellY;
            this.cellHSpan = cellHSpan;
            this.cellVSpan = cellVSpan;
        }

        boolean isHideseatMode = false;

        /* YUNOS BEGIN */
        //##date:2014/11/12 ##author:zhanggong.zg ##BugID:5444810
        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap,
                boolean invertHorizontally, int colCount) {
            setup(cellWidth, cellHeight, widthGap, heightGap, invertHorizontally, colCount, false);
        }
        /* YUNOS END */

        /* YUNOS BEGIN */
        // ##date:2015/12/17 ##author:xiaodong.lxd 7526018
        // The static hotseat icon's layoutparams'x is computed in Hotseat.java
        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap,
                boolean invertHorizontally, int colCount, boolean hotseatMode, boolean staticHotseat) {
            if (isLockedToGrid) {
                final int myCellHSpan = cellHSpan;
                final int myCellVSpan = cellVSpan;
                int myCellX = useTmpCoords ? tmpCellX : cellX;
                int myCellY = useTmpCoords ? tmpCellY : cellY;

                if (invertHorizontally) {
                    myCellX = colCount - myCellX - cellHSpan;
                }

                width = myCellHSpan * cellWidth + ((myCellHSpan - 1) * widthGap) - leftMargin - rightMargin;
                height = myCellVSpan * cellHeight + ((myCellVSpan - 1) * heightGap) - topMargin - bottomMargin;
                if (!(hotseatMode && staticHotseat)) {
                    x = (int) (myCellX * (cellWidth + widthGap) + leftMargin) + (startWithGap ? widthGap : 0);
                }
                y = (int) (myCellY * (cellHeight + heightGap) + topMargin);
            }
        }
        /* YUNOS END */

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap,
                boolean invertHorizontally, int colCount, boolean ignoreHideseat) {
            if (isLockedToGrid) {
                final int myCellHSpan = cellHSpan;
                final int myCellVSpan = cellVSpan;
                int myCellX = useTmpCoords ? tmpCellX : cellX;
                int myCellY = useTmpCoords ? tmpCellY : cellY;

                if (invertHorizontally) {
                    myCellX = colCount - myCellX - cellHSpan;
                }

                width = myCellHSpan * cellWidth + ((myCellHSpan - 1) * widthGap) -
                        leftMargin - rightMargin;
                height = myCellVSpan * cellHeight + ((myCellVSpan - 1) * heightGap) -
                        topMargin - bottomMargin;
                x = (int) (myCellX * (cellWidth + widthGap) + leftMargin) + (startWithGap ? widthGap : 0);
                y = (int) (myCellY * (cellHeight + heightGap) + topMargin);

                if (isHideseatMode && !ignoreHideseat) {
                    if (myCellY >= HIDESEAT_CELLY) {
                        y += LauncherApplication.mLauncher.getCustomHideseat().getCustomeHideseatHeight();
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "LayoutParams [cellX=" + cellX + ", cellY=" + cellY + ", x=" + x + ", y=" + y + ", dropped="
                    + dropped + "]";
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return y;
        }

    }

    // This class stores info for two purposes:
    // 1. When dragging items (mDragInfo in Workspace), we store the View, its cellX & cellY,
    //    its spanX, spanY, and the screen it is on
    // 2. When long clicking on an empty cell in a CellLayout, we save information about the
    //    cellX and cellY coordinates and which page was clicked. We then set this as a tag on
    //    the CellLayout that was long clicked
    static final class CellInfo {
        View cell;
        int cellX = -1;
        int cellY = -1;
        int spanX;
        int spanY;
        int screen;
        long container;

        @Override
        public String toString() {
            return "Cell[view=" + cell + ", x=" + cellX + ", y=" + cellY + "]";
        }
    }

    public boolean lastDownOnOccupiedCell() {
        return mLastDownOnOccupiedCell;
    }

    /*YUNOS BEGIN*/
    //##date:2013/11/22 ##author:xiaodong.lxd
    public ShortcutAndWidgetContainer getShortcutAndWidgetContainer() {
        return mShortcutsAndWidgets;
    }

    public boolean hasChild() {
        return (mShortcutsAndWidgets.getChildCount() != 0);
    }
    /*YUNOS END*/
    protected static final int FLING_DROPDOWN_ANIM_DURATION = 300;
    private ArrayList<PendingFlingView> mPengindFlingViewList;
    private TimeInterpolator mSwitchInterpolator = new DecelerateInterpolator();
    private AnimatorSet mFlingAnimationSet;
    private boolean mAnimaFlingRuninng = false;

    private static class PendingFlingView {
        public PendingFlingView(View v, float y, float rotaion, boolean next) {
            mView = v;
            mY = y;
            mRotation = rotaion;
            mIsNext = next;
        }
        public PendingFlingView(View v, float y, float rotaion, boolean next, String title, int cellX, int cellY) {
            this(v, y, rotaion, next);
            mTitle = title;
            mCellX = cellX;
            mCellY = cellY;
        }
        public float mY;
        public float mRotation;
        public boolean mIsNext;
        public View mView;
        public String mTitle ;
        public int mCellX;
        public int mCellY;
    }

    public void addPengindFlingDropDownTarget(View v, float y, float rotation, boolean next, String title, int cellX, int cellY) {
        if(mPengindFlingViewList == null)
            mPengindFlingViewList = new ArrayList<PendingFlingView>();
        mPengindFlingViewList.add(new PendingFlingView(v, y, rotation, next, title, cellX, cellY));
        Log.d(TAG, "sxsexe------------>add fling Item  " + mPengindFlingViewList.size() + " screen " + mLauncher.getWorkspace().getCurrentPage());
        v.setVisibility(View.INVISIBLE);
    }

    public void removePendingFlingDropDownItem(View v) {
        if(mPengindFlingViewList != null && !mPengindFlingViewList.isEmpty()) {
            Log.d(TAG, "sxsexe------------>remove Fling Item  " + v);
            for(PendingFlingView flingView : mPengindFlingViewList) {
                if(flingView.mView == v) {
                    mPengindFlingViewList.remove(flingView);
                    return;
                }
            }
        }
    }

    public void startFlingDropDownAnimation() {

        if(mAnimaFlingRuninng) {
            return;
        }

        if (mPengindFlingViewList != null && mPengindFlingViewList.size() > 0) {
            ArrayList<Animator> anims = new ArrayList<Animator>();
            for (PendingFlingView p : mPengindFlingViewList) {
                View v = p.mView;
                if (mDragging) {
                    v.bringToFront();
                    v.setVisibility(View.VISIBLE);
                } else {
                    boolean next = p.mIsNext;
                    boolean rtl = isLayoutRtl();
                    next ^= rtl; // if we are in RTL locale, swap "next" flag
                    float x = next ? 0 : getWidth() + v.getWidth();
                    float y = p.mY;
                    float rotation = p.mRotation;

                    CellLayout.LayoutParams lp = (LayoutParams) v.getLayoutParams();
                    float newX = lp.x;
                    float newY = lp.y;
                    Log.d(TAG, "sxsexe-------> startFlingDropDownAnimation p " + p.mTitle
                            + " oldx " + x + " newx " + newX
                            + " oldy " + y + " newy " + newY/*
                            + " mCellWidth " + mCellWidth + " mCellHeight " + mCellHeight
                            + " cellX " + p.mCellX + " celllY " + p.mCellY
                            + " mWidthGap " + mWidthGap + " mHeightGap " + mHeightGap*/);
                    v.setPivotX(v.getWidth()/2);
                    v.setPivotY(v.getHeight()/2);
                    ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v,
                            PropertyValuesHolder.ofFloat(ViewHidePropertyName.Y, y, newY),
                            PropertyValuesHolder.ofFloat(ViewHidePropertyName.X, x, newX),
                            PropertyValuesHolder.ofFloat(ViewHidePropertyName.ROTATION, rotation, next ? 360 : -360));
                    a.setInterpolator(mSwitchInterpolator);
                    anims.add(a);
                    v.bringToFront();
                    v.setVisibility(View.VISIBLE);
                }
            }
            if (mDragging) {
                mPengindFlingViewList.clear();
                mPengindFlingViewList = null;
                return;
            }
            if(mFlingAnimationSet != null) {
                mFlingAnimationSet.cancel();
                mFlingAnimationSet = null;
            }

            mFlingAnimationSet = new AnimatorSet();
            mFlingAnimationSet.setDuration(FLING_DROPDOWN_ANIM_DURATION);
            mFlingAnimationSet.playTogether(anims);
            mFlingAnimationSet.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                    mAnimaFlingRuninng = true;
                }

                public void onAnimationRepeat(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    if (mPengindFlingViewList != null && mPengindFlingViewList.size() > 0) {
                        for (PendingFlingView p : mPengindFlingViewList) {
                            p.mView = null;
                        }
                        mPengindFlingViewList.clear();
                        mPengindFlingViewList = null;
                    }

                    mAnimaFlingRuninng = false;
                    mFlingAnimationSet = null;
                }

                public void onAnimationCancel(Animator animation) {
                }
            });
            mFlingAnimationSet.start();
        }
    }

    public int cancelFlingDropDownAnimation(){
        //Log.d(TAG, "sxsexe---------> cancelFlingDropDownAnimation mPengindFlingDropDownTarget " + mPengindFlingViewList);
        int count = 0;
        if(mPengindFlingViewList != null) {
            count = mPengindFlingViewList.size();
            for (PendingFlingView p : mPengindFlingViewList) {
                p.mView.setVisibility(View.VISIBLE);
                p.mView = null;
            }
            mPengindFlingViewList.clear();
            mPengindFlingViewList = null;
        }
        if(mFlingAnimationSet != null && mFlingAnimationSet.isRunning())
            mFlingAnimationSet.end();
        mAnimaFlingRuninng = false;
        return count;
    }

    public void flingBack(View cell, float roatation, float y, boolean next) {
        float oldX = next ? getWidth() + cell.getWidth() : 0;
        Log.d(TAG, "sxsexe---------->flingBack oldX " + oldX + " newX " + cell.getX());
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(cell,
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.ROTATION, roatation, 0),
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.X, oldX, cell.getX()),
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.Y, y, cell.getY()));
        animator.setInterpolator(mSwitchInterpolator);
        animator.setDuration(CellLayout.FLING_DROPDOWN_ANIM_DURATION);
        mLauncher.reVisibileDraggedItem((ItemInfo) cell.getTag());
        animator.start();
    }

    public void setGaps(int widthGap, int heightGap) {
        mWidthGap = mOriginalWidthGap = widthGap;
        mHeightGap = mOriginalHeightGap = heightGap;
    }
//    public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
//        return mLauncher.shouldInvalidate(this) ? super.invalidateChildInParent(location, dirty)
//                : null;
//    }

    private static final String LOG_TAG = "CellLayoutReOrder";
    private static final int MAX_INTERVAL_FOR_CLICK = 250;
    private static final int MAX_DISTANCE_FOR_CLICK = 100;
    private static final int MAX_DOUBLE_CLICK_INTERVAL = 500;
    private static final int ICON_REORDER_ANIM_DURATION = 250;
    int mDownX = 0;
    int mDownY = 0;
    int mTempX = 0;
    int mTempY = 0;
    boolean mIsWaitUpEvent = false;
    boolean mIsWaitDoubleClick = false;
    Runnable mTimerForUpEvent = new Runnable() {
        public void run() {
            if (mIsWaitUpEvent) {
                Log.d(LOG_TAG,
                        "The mTimerForUpEvent has executed, so set the mIsWaitUpEvent as false");
                mIsWaitUpEvent = false;
            } else {
                Log.d(LOG_TAG,
                        "The mTimerForUpEvent has executed, mIsWaitUpEvent is false,so do nothing");
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // only the celllayout in workspace support doubleclick, and the first
        // screen donot support
        if (!(this.getParent() instanceof Workspace)) {
            return super.onTouchEvent(event);
        }
        if (!mIsWaitUpEvent && event.getAction() != MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mDownX = (int) event.getX();
            mDownY = (int) event.getY();
            mIsWaitUpEvent = true;
            postDelayed(mTimerForUpEvent, MAX_INTERVAL_FOR_CLICK);
            break;
        case MotionEvent.ACTION_MOVE:
            mTempX = (int) event.getX();
            mTempY = (int) event.getY();
            if (Math.abs(mTempX - mDownX) > MAX_DISTANCE_FOR_CLICK
                    || Math.abs(mTempY - mDownY) > MAX_DISTANCE_FOR_CLICK) {
                mIsWaitUpEvent = false;
                removeCallbacks(mTimerForUpEvent);
                Log.d(LOG_TAG, "The move distance too far:cancel the click");
            }
            break;
        case MotionEvent.ACTION_UP:
            mTempX = (int) event.getX();
            mTempY = (int) event.getY();
            if (Math.abs(mTempX - mDownX) > MAX_DISTANCE_FOR_CLICK
                    || Math.abs(mTempY - mDownY) > MAX_DISTANCE_FOR_CLICK) {
                mIsWaitUpEvent = false;
                removeCallbacks(mTimerForUpEvent);
                Log.d(LOG_TAG,
                        "The touch down and up distance too far:cancel the click");
                break;
            } else {
                int[] cellPos = new int[2];
                pointToCellRounded(mTempX - getPaddingLeft(), mTempY - getPaddingTop(),
                        cellPos);
                mIsWaitUpEvent = false;
                removeCallbacks(mTimerForUpEvent);
                onSingleClick(cellPos);
                return super.onTouchEvent(event);
            }
        case MotionEvent.ACTION_CANCEL:
            mIsWaitUpEvent = false;
            removeCallbacks(mTimerForUpEvent);
            //Log.d(LOG_TAG, "The touch cancel state:cancel the click");
            break;
        default:
            Log.d(LOG_TAG, "irrelevant MotionEvent state:" + event.getAction());
        }
        return super.onTouchEvent(event);
    }

    Runnable mTimerForSecondClick = new Runnable() {
        @Override
        public void run() {
            if (mIsWaitDoubleClick) {
                Log.d(LOG_TAG,
                        "The mTimerForSecondClick has executed,so as a singleClick");
                mIsWaitDoubleClick = false;
                // at here can do something for singleClick!!
            } else {
                Log.d(LOG_TAG,
                        "The mTimerForSecondClick has executed, the doubleclick has executed ,so do thing");
            }
        }
    };

    int mCurrentCellX;
    int mCurrentCellY;

    private void onSingleClick(int[] cellPosition) {
        Log.d(LOG_TAG, "Execute the singleClick on cell x = " + cellPosition[0]
                + ", y = " + cellPosition[1]);
        // click on the cell which has content
        if (mOccupied[cellPosition[0]][cellPosition[1]]) {
            Log.d(LOG_TAG, "The cell position: x=" + cellPosition[0] + ",y="
                    + cellPosition[1] + " is occupied,cancel the click");
            if (mIsWaitDoubleClick) {
                mIsWaitDoubleClick = false;
                removeCallbacks(mTimerForSecondClick);
            }
            return;
        }
        if (mIsWaitDoubleClick) {
            if (mCurrentCellX == cellPosition[0]
                    && mCurrentCellY == cellPosition[1]) {
                onDoubleClick(cellPosition);
            } else {
                Log.d(LOG_TAG, "The cell position in two click is different ");
            }
            mIsWaitDoubleClick = false;
            removeCallbacks(mTimerForSecondClick);
        } else {
            mCurrentCellX = cellPosition[0];
            mCurrentCellY = cellPosition[1];
            mIsWaitDoubleClick = true;
            postDelayed(mTimerForSecondClick, MAX_DOUBLE_CLICK_INTERVAL);
        }
    }

    private void onDoubleClick(int[] cellPosition) {
        if (FeatureUtility.hasDoubleClickFeature()) {
            final Workspace workspace = mLauncher.getWorkspace();
            final int screen = workspace.getIconScreenIndex(workspace.indexOfChild(CellLayout.this));
            Log.d(LOG_TAG, "Execute the doubleClick on cell screen = " + screen + ",x = "
                    + cellPosition[0] + ",y = " + cellPosition[1]);
            if (mLauncher.getWorkspace().getSelectedViewsInLayout().size() != 0 || screen == -1) {
                return;
            }
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode()) {
                Log.i(TAG, "In launcher edit mode or launcher category mode, return");
                return;
            }
            if (AgedModeUtil.isAgedMode()) {
                Log.i(TAG, "In aged mode, double click no response");
                return;
            }
            if (mLauncher.isGadgetCardShowing()) {
                return;
            }
            /* YUNOS BEGIN */
            // ##date: 2016/04/27 ## author: yongxing.lyx
            // ##BugID:8185725:don't reOrderAllIcons when hideseat is open;
            if (isHideseatOpen) {
                return;
            }
            /* YUNOS END */
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category and sort on desk top
            final Map<String, String> param = new HashMap<String, String>();
            param.put("screen", String.valueOf(screen + 1));
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE, param);
            if (!HomeShellSetting.getFreezeValue(mLauncher)) {
                if (ConfigManager.isLandOrienSupport()) {
                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_CURRENTSCREEN, param);
                    reOrderIcons();
                    return;
                }

                if (FeatureUtility.hasArrangeCurrentScreenFeature() && FeatureUtility.hasArrangeAllScreenFeature()
                        && FeatureUtility.hasArrangeClassifyFeature()) {
                    if (mLayoutActionSheet == null) {
                        mLayoutActionSheet = new ActionSheet(getContext());
                        ArrayList<Boolean> enables = new ArrayList<Boolean>();
                        enables.add(true);
                        enables.add(true);
                        enables.add(true);
                        ArrayList<String> items = new ArrayList<String>();
                        items.add(mLayoutTitle[0]);
                        items.add(mLayoutTitle[1]);
                        items.add(mLayoutTitle[2]);
                        ActionSheet.CommonButtonListener listener = new ActionSheet.CommonButtonListener() {
                            @Override
                            public void onClick(int which) {
                                mLauncher.getHotseat().afterShowHotseat();
                                mLauncher.getHotseat().setAtomIconClickable(false);
                                switch (which) {
                                case 0:
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_CURRENTSCREEN, param);
                                    reOrderIcons();
                                    break;
                                case 1:
                                    UserTrackerHelper
                                            .sendUserReport(UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_ALLSCREEN);
                                    reOrderAllIcons();
                                    break;
                                case 2:
                                    UserTrackerHelper
                                            .sendUserReport(UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_CLASSIFY);
                                    mLauncher.getModel().reCategoryAllIcons();
                                    break;
                                }
                            }

                            @Override
                            public void onDismiss(ActionSheet arg0) {
                                workspace.setIsCatActShtStart(false);
                            }
                        };

                        mLayoutActionSheet.setCommonButtons(items, null, enables, listener);
                        mLayoutActionSheet.setDispalyStyle(ActionSheet.DisplayStyle.PHONE);
                    }
                    mLayoutActionSheet.show(workspace);
                    workspace.setIsCatActShtStart(true);
                } else if (FeatureUtility.hasArrangeCurrentScreenFeature() && FeatureUtility.hasArrangeAllScreenFeature()) {
                    if (mLayoutActionSheet == null) {
                        mLayoutActionSheet = new ActionSheet(getContext());
                        ArrayList<Boolean> enables = new ArrayList<Boolean>();
                        enables.add(true);
                        enables.add(true);
                        ArrayList<String> items = new ArrayList<String>();
                        items.add(mLayoutTitle[0]);
                        items.add(mLayoutTitle[1]);

                        ActionSheet.CommonButtonListener listener = new ActionSheet.CommonButtonListener() {
                            @Override
                            public void onClick(int which) {
                                switch (which) {
                                case 0:
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_CURRENTSCREEN, param);
                                    reOrderIcons();
                                    break;
                                case 1:
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_ALLSCREEN);
                                    reOrderAllIcons();
                                    break;
                                }
                            }
                            @Override
                            public void onDismiss(ActionSheet arg0) {
                                workspace.setIsCatActShtStart(false);
                            }
                        };
                        mLayoutActionSheet.setCommonButtons(items, null, enables, listener);
                        mLayoutActionSheet.setDispalyStyle(ActionSheet.DisplayStyle.PHONE);
                    }
                    mLayoutActionSheet.show(workspace);
                    workspace.setIsCatActShtStart(true);
                } else if (FeatureUtility.hasArrangeCurrentScreenFeature() && FeatureUtility.hasArrangeClassifyFeature()) {
                    if (mLayoutActionSheet == null) {
                        mLayoutActionSheet = new ActionSheet(getContext());
                        ArrayList<Boolean> enables = new ArrayList<Boolean>();
                        enables.add(true);
                        enables.add(true);
                        ArrayList<String> items = new ArrayList<String>();
                        items.add(mLayoutTitle[0]);
                        items.add(mLayoutTitle[2]);
                        ActionSheet.CommonButtonListener listener = new ActionSheet.CommonButtonListener() {
                            @Override
                            public void onClick(int which) {
                                switch (which) {
                                case 0:
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_CURRENTSCREEN, param);
                                    reOrderIcons();
                                    break;
                                case 1:
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_CLASSIFY);
                                    mLauncher.getModel().reCategoryAllIcons();
                                    break;
                                }
                            }
                            @Override
                            public void onDismiss(ActionSheet arg0) {
                                workspace.setIsCatActShtStart(false);
                            }
                        };
                        mLayoutActionSheet.setCommonButtons(items, null, enables, listener);
                        mLayoutActionSheet.setDispalyStyle(ActionSheet.DisplayStyle.PHONE);
                    }
                    mLayoutActionSheet.show(workspace);
                    workspace.setIsCatActShtStart(true);
                } else if (FeatureUtility.hasArrangeAllScreenFeature() && FeatureUtility.hasArrangeClassifyFeature()) {
                    if (mLayoutActionSheet == null) {
                        mLayoutActionSheet = new ActionSheet(getContext());
                        ArrayList<Boolean> enables = new ArrayList<Boolean>();
                        enables.add(true);
                        enables.add(true);
                        ArrayList<String> items = new ArrayList<String>();
                        items.add(mLayoutTitle[1]);
                        items.add(mLayoutTitle[2]);
                        ActionSheet.CommonButtonListener listener = new ActionSheet.CommonButtonListener() {
                            @Override
                            public void onClick(int which) {
                                switch (which) {
                                case 0:
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_ALLSCREEN);
                                    reOrderAllIcons();
                                    break;
                                case 1:
                                    UserTrackerHelper.sendUserReport(
                                            UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_CLASSIFY);
                                    mLauncher.getModel().reCategoryAllIcons();
                                    break;
                                }
                            }
                            @Override
                            public void onDismiss(ActionSheet arg0) {
                                workspace.setIsCatActShtStart(false);
                            }
                        };
                        mLayoutActionSheet.setCommonButtons(items, null, enables, listener);
                        mLayoutActionSheet.setDispalyStyle(ActionSheet.DisplayStyle.PHONE);
                    }
                    mLayoutActionSheet.show(workspace);
                    workspace.setIsCatActShtStart(true);
                } else if (FeatureUtility.hasArrangeCurrentScreenFeature()) {
                    UserTrackerHelper.sendUserReport(
                            UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_CURRENTSCREEN, param);
                    reOrderIcons();
                } else if (FeatureUtility.hasArrangeAllScreenFeature()) {
                    UserTrackerHelper.sendUserReport(
                            UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_ALLSCREEN);
                    reOrderAllIcons();
                } else {
                    UserTrackerHelper.sendUserReport(
                            UserTrackerMessage.MSG_DOUBLECLICK_ARRANGE_CLASSIFY);
                    mLauncher.getModel().reCategoryAllIcons();
                }
            } else {
                Toast.makeText(getContext(), R.string.aged_freeze_homeshell_toast, Toast.LENGTH_SHORT).show();
            }
            /* YUNOS END */
        }
    }

    private int mEmptyX;
    private int mEmptyY;
    private AnimatorSet reOrderAnim = null;

    public void reOrderIcons() {
        boolean hasFindEmpty = false;
        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(ICON_REORDER_ANIM_DURATION);
        List<Animator> animList = new ArrayList<Animator>();
        final Map<ItemInfo, CellInfo> itemToNewCellPos = new HashMap<ItemInfo, CellInfo>();
        Workspace workspace = mLauncher.getWorkspace();
        final int screen = workspace.getIconScreenIndex(workspace.indexOfChild(CellLayout.this));
        boolean[][] isWidget = new boolean[mCountX][mCountY];
        boolean isFirstScreen = (screen == 0) ? true : false;
        int step = isFirstScreen? -1:1;
        // The reorder rule in firstScreen from bottom to top while others from
        // top to bottom
        for (int yIndex = isFirstScreen ? (mCountY - 1) : 0; isFirstScreen ? (yIndex >= 0)
                : (yIndex < mCountY); yIndex += step) {
            for (int xIndex = 0; xIndex < mCountX; xIndex++) {
                // skip the widget
                if (mOccupied[xIndex][yIndex]
                        && this.getChildAt(xIndex, yIndex) != null) {
                    ItemInfo info = (ItemInfo) this.getChildAt(xIndex, yIndex)
                            .getTag();
                    if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                            || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_GADGET) {
                        isWidget[xIndex][yIndex] = true;
                        continue;
                    }
                }
                if (!hasFindEmpty) {
                    // find the first cell which is empty
                    if (!mOccupied[xIndex][yIndex]) {
                        mEmptyX = xIndex;
                        mEmptyY = yIndex;
                        hasFindEmpty = true;
                        Log.d(LOG_TAG, "Find the first cell which is empty,X = " + mEmptyX
                                + ",Y = " + mEmptyY);
                    }
                } else {
                    if (mOccupied[xIndex][yIndex]) {
                        //BugID:5640061:view null pointer exception
                        if (this.getChildAt(xIndex, yIndex) == null) {
                             Log.d(TAG, "the view is null xIndex:" + xIndex + " yIndex:" + yIndex);
                             mOccupied[xIndex][yIndex] = false;
                             continue;
                        }
                        // generate the animation to move this occupied cell to
                        // the emptyIndex
                        final View view = this.getChildAt(xIndex, yIndex);
                        int[] start = new int[2];
                        int[] end = new int[2];
                        cellToPoint(xIndex, yIndex, start);
                        cellToPoint(mEmptyX, mEmptyY, end);
                        ObjectAnimator animX = ObjectAnimator.ofFloat(view, "X", start[0]
                                - getPaddingLeft(), end[0] - getPaddingLeft());
                        ObjectAnimator animY = ObjectAnimator.ofFloat(view, "Y", start[1]
                                - getPaddingTop(), end[1] - getPaddingTop());
                        ItemInfo item = (ItemInfo) view.getTag();
                        Log.d(LOG_TAG, "The cell xIndex=" + xIndex + ",yIndex=" + yIndex
                                + ",title=" + item.title + " ,is occupied, move to: " + mEmptyX
                                + "," + mEmptyY);
                        CellInfo info = new CellInfo();
                        info.cellX = mEmptyX;
                        info.cellY = mEmptyY;
                        itemToNewCellPos.put(item, info);
                        animY.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // remove the view from origin position ,add it
                                // to the destination
                                CellLayout.this.removeView(view);
                                ItemInfo item = (ItemInfo) view.getTag();
                                item.cellX = itemToNewCellPos.get(item).cellX;
                                item.cellY = itemToNewCellPos.get(item).cellY;
                                if (ConfigManager.isLandOrienSupport()) {
                                    if (LauncherApplication.isInLandOrientation()) {
                                        item.cellXLand = item.cellX;
                                        item.cellYLand = item.cellY;
                                    } else {
                                        item.cellXPort = item.cellX;
                                        item.cellYPort = item.cellY;
                                    }
                                }
                                int childId = LauncherModel.getCellLayoutChildId(LauncherSettings.Favorites.CONTAINER_DESKTOP, screen,
                                        item.cellX, item.cellY, item.spanX, item.spanY);
                                CellLayout.LayoutParams lp = new CellLayout.LayoutParams(
                                        item.cellX, item.cellY, item.spanX,
                                        item.spanY);
                                CellLayout.this.addViewToCellLayout(view, -1,
                                        childId, lp, true);
                                if (ConfigManager.isLandOrienSupport()) {
                                    LauncherModel.markCellsOccupiedInNonCurrent(item, true);
                                }
                                view.setTranslationX(0.0f);
                                view.setTranslationY(0.0f);
                                LauncherModel.addOrMoveItemInDatabase(mLauncher, item, LauncherSettings.Favorites.CONTAINER_DESKTOP, screen, item.cellX,
                                        item.cellY);
                            }
                        });
                        animList.add(animX);
                        animList.add(animY);
                        moveToNextCell(isFirstScreen, isWidget);
                    } else {
                        Log.d(LOG_TAG, "The cell xIndex =" + xIndex + ", yIndex =" + yIndex
                                + ":now is empty ,do nothing");
                    }
                }
            }
        }
        Log.d(LOG_TAG, "arrange the icon positon completed!wait for animation!");
        animSet.playTogether(animList);
        if (reOrderAnim != null) {
            if (reOrderAnim.isRunning()) {
                reOrderAnim.end();
            }
        }
        reOrderAnim = animSet;
        animSet.start();
    }

    // move emptyIndex to next cell(skip the widget)
    private void moveToNextCell(boolean isFirstScreen, boolean[][] isWidget) {
        do {
            if (mEmptyX < mCountX - 1) {
                mEmptyX++;
            } else {
                // the reorder rule to firstScrren and others is
                // different
                if (!isFirstScreen && (mEmptyY < mCountY - 1)) {
                    mEmptyY++;
                    mEmptyX = 0;
                } else if (isFirstScreen && (mEmptyY > 0)) {
                    mEmptyY--;
                    mEmptyX = 0;
                } else {
                    Log.d(LOG_TAG, "mEmpty has reached the last positon");
                    break;
                }
            }
        } while (isWidget[mEmptyX][mEmptyY]);
    }

    /* YUNOS BEGIN */
    // ## Feature: Auto classify icons
    // ## Date:2015/7/30 ## Author:kaike.zkk
    private int mStartScreen = 0;
    private int mStartCellY = 0;
    private static int mMaxScreenCount = ConfigManager.getIconScreenMaxCount();

    public void reOrderAllIcons() {
        boolean hasFindEmpty = false;
        final int container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        final int screenNums = mLauncher.getWorkspace().getIconScreenCount();
        final Workspace workspace = mLauncher.getWorkspace();
        ArrayList<EmptyCellSpan> emptyCellList = new ArrayList<EmptyCellSpan>();

        ItemInfo occupied[][][] = new ItemInfo[mMaxScreenCount + 1
            + ConfigManager.getHideseatScreenMaxCount()][mCountX + 1][mCountY + 1];
        LauncherModel.createCurrentLayoutData(occupied);
        searchFirstItem(occupied);

        ItemInfo item = null;
        for (int lscreen = mStartScreen; lscreen < screenNums; lscreen ++) {
            for (int lcelly = (lscreen == mStartScreen) ? mStartCellY : 0; lcelly < mCountY;) {
                for (int lcellx = 0; lcellx < mCountX;) {
                    item = occupied[lscreen][lcellx][lcelly];
                    if (!hasFindEmpty) {
                        // find the first cell which is emtpy
                        if (item == null) {
                            hasFindEmpty = true;
                            updateEmtpyCellList(occupied, emptyCellList);
                            Log.i(TAG, "Find the first cell which is empty, Screen = "
                                    + lscreen + ", X = " + lcellx +", Y = " + lcelly);
                        }
                    } else {
                        if (item != null) {
                            if (isEmptyListContainItem(item, emptyCellList)) {
                                CellLayout cellLayout = (CellLayout) workspace.getChildAt(workspace.getRealScreenIndex(lscreen));
                                final View view = cellLayout.getChildAt(lcellx, lcelly);
                                cellLayout.removeView(view);
                                item.screen = mInsertScreen;
                                item.cellX = mInsertX;
                                item.cellY = mInsertY;

                                // add or move item in database
                                int childId = LauncherModel.getCellLayoutChildId(container, mInsertScreen,
                                        mInsertX, mInsertY, item.spanX, item.spanY);
                                CellLayout.LayoutParams lp = new CellLayout.LayoutParams(
                                        mInsertX, mInsertY, item.spanX, item.spanY);
                                CellLayout newLayout = (CellLayout) workspace.getChildAt(workspace.getRealScreenIndex(mInsertScreen));
                                newLayout.addViewToCellLayout(view, -1, childId, lp, true);
                                LauncherModel.addOrMoveItemInDatabase(mLauncher, item, container,
                                        mInsertScreen, mInsertX, mInsertY);

                                Log.i(TAG, "The cell screen = " + lscreen + ", cellX = " + lcellx
                                        + ", cellY = " + lcelly + ", title = " + item.title + ", is moved to: "
                                        + mInsertScreen + ", " + mInsertX + ", " + mInsertY);
                                // update the current layout data
                                for (int x = item.cellX; x < (item.cellX + item.spanX); x ++) {
                                    for (int y = item.cellY; y < (item.cellY + item.spanY); y ++) {
                                        occupied[mInsertScreen][x][y] = item;
                                    }
                                }
                                boolean isWidgetUp = (item.screen == lscreen) && (lcelly > item.cellY)
                                        && (lcelly - item.cellY < item.spanY);
                                for (int x = lcellx; x < (lcellx + item.spanX); x ++) {
                                    for (int y = isWidgetUp ? (item.cellY + item.spanY) : lcelly; y < (lcelly + item.spanY); y ++) {
                                        occupied[lscreen][x][y] = null;
                                    }
                                }
                                updateEmtpyCellList(occupied, emptyCellList);
                            }
                        } else {
                            Log.i(TAG, "The cell screenIndex = " + lscreen + ", xIndex = " + lcellx + ", yIndex = "
                                    + lcelly + " is empty, and do nothing");
                        }
                    }
                    lcellx += (item != null) ? item.spanX : 1;
                }
                lcelly += (item != null) ? item.spanY : 1;
            }
        }
        workspace.checkAndRemoveEmptyCell();
    }

    /**
     * This method is used to find the first item that is not widget.
     * @param occupied, the current layout data
     */
    private void searchFirstItem(ItemInfo[][][] occupied) {
        boolean found = false;
        final int screenNums = mLauncher.getWorkspace().getIconScreenCount();
        for (int lscreen = 0; !found && lscreen < screenNums; lscreen ++) {
            for (int lcelly = 0; !found && lcelly < mCountY; lcelly ++) {
                for (int lcellx = 0; !found && lcellx < mCountX; lcellx ++) {
                    ItemInfo item = occupied[lscreen][lcellx][lcelly];
                    if (item == null) {
                        Log.i(TAG, "lscreen = " + lscreen + ", cellX = " + lcellx + ", cellY = " + lcelly);
                        continue;
                    }
                    if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                            || item.itemType == LauncherSettings.Favorites.ITEM_TYPE_GADGET) {
                        Log.i(TAG, "lscreen = " + lscreen + ", cellx = " + lcellx + ", celly = " + lcelly
                                + ", spanX = " + item.spanX + ", spanY = " + item.spanY);
                        continue;
                    }
                    mStartScreen = lscreen;
                    mStartCellY = lcelly;
                    found = true;
                }
            }
        }
    }

    private int mEmptyScreen = 0;
    private int mEmptySpanScreen = 0;
    private int mEmptySpanX = 0;
    private int mEmptySpanY = 0;

    /**
     * This method is used to update the emtpy cell span list.
     * @param occupied, the current layout data
     * @param spanList, the empty cell span list
     */
    private void updateEmtpyCellList(ItemInfo[][][] occupied, ArrayList<EmptyCellSpan> spanList) {
        boolean hasFindEmpty = false;
        if (!spanList.isEmpty()) {
            spanList.clear();
        }
        final int screenNums = mLauncher.getWorkspace().getIconScreenCount();
        for (int lscreen = mStartScreen; lscreen < screenNums; lscreen ++) {
            for (int lcelly = (lscreen == mStartScreen) ? mStartCellY : 0; lcelly < mCountY; lcelly ++) {
                for (int lcellx = 0; lcellx < mCountX; lcellx ++) {
                    ItemInfo item = occupied[lscreen][lcellx][lcelly];
                    if (!hasFindEmpty) {
                        if (item == null) {
                            hasFindEmpty = true;
                            mEmptyScreen = mEmptySpanScreen = lscreen;
                            mEmptyX = mEmptySpanX = lcellx;
                            mEmptyY = mEmptySpanY = lcelly;
                        }
                    } else {
                        if (item != null) {
                            hasFindEmpty = false;
                            spanList.add(new EmptyCellSpan(mEmptyScreen, mEmptyX, mEmptyY,
                                    mEmptySpanScreen, mEmptySpanX, mEmptySpanY));
                            Log.i(TAG, "The current size of spanList = " + spanList.size());
                        } else {
                            mEmptySpanScreen = lscreen;
                            mEmptySpanX = lcellx;
                            mEmptySpanY = lcelly;
                            Log.i(TAG, "mEmptySpanScreen = " + lscreen + ", mEmptySpanX = "
                                    + lcellx + ", mEmptySpanY = " + lcelly);
                        }
                    }
                }
            }
        }
    }

    private int mInsertScreen = 0;
    private int mInsertX = 0;
    private int mInsertY = 0;

    /**
     * This method is used to determine the item can be arranged in the emtpy cell list.
     * @param item, the ItemInfo to be arranged
     * @param spanList, the emtpy cell list
     */
    private boolean isEmptyListContainItem(ItemInfo item, ArrayList<EmptyCellSpan> spanList) {
        for (EmptyCellSpan cellSpan : spanList) {
            if (item.screen < cellSpan.startScreen) {
                return false;
            }
            if (item.screen == cellSpan.startScreen && item.cellY < cellSpan.startEmptyY) {
                return false;
            }
            if (isEmptyContainItem(item, cellSpan)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is used to determine whether the item can be arranged in the cell span.
     * @param item, the ItemInfo to be arranged
     * @param cellspan, the emtpy cell span
     */
    private boolean isEmptyContainItem(ItemInfo item, EmptyCellSpan cellSpan) {
        int startScreen = cellSpan.startScreen;
        int startEmptyX = cellSpan.startEmptyX;
        int startEmptyY = cellSpan.startEmptyY;
        int endScreen = cellSpan.endScreen;
        int endEmptyX = cellSpan.endEmptyX;
        int endEmptyY = cellSpan.endEmtpyY;
        if (item.spanX == 1) {
            mInsertScreen = startScreen;
            mInsertX = startEmptyX;
            mInsertY = startEmptyY;
            return true;
        }
        if (startScreen == endScreen && endEmptyY < mCountY - 1) {
            // the emtpy cells are in the same screen
            if (startEmptyX == 0) {
                mInsertScreen = startScreen;
                mInsertX = startEmptyX;
                mInsertY = startEmptyY;
                return true;
            } else {
                if (endEmptyY > startEmptyY) {
                    mInsertScreen = startScreen;
                    mInsertX = 0;
                    mInsertY = startEmptyY + 1;
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            // the empty cells span two screens
            if (startEmptyX == 0) {
                if (item.spanY <= mCountY - startEmptyY) {
                    mInsertScreen = startScreen;
                    mInsertX = startEmptyX;
                    mInsertY = startEmptyY;
                    return true;
                }
            } else {
                if (item.spanY < mCountY - startEmptyY) {
                    mInsertScreen = startScreen;
                    mInsertX = 0;
                    mInsertY = startEmptyY + 1;
                    return true;
                }
            }
            if (endScreen > startScreen) {
                mInsertScreen = endScreen;
                mInsertX = 0;
                mInsertY = 0;
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * The EmptyCellSpan class is empty cell span of the screen.
     * @param startScreen, the start screen of the span; startEmptyX,
     *        start cell X of the span; startEmptyY, start cell Y of the span
     * @param endScreen, the end screen of the span; endEmptyX,
     *        end cell X of the span; endEmtpyY, end cell Y of the span
     */
    private class EmptyCellSpan {
        private int startScreen;
        private int startEmptyX;
        private int startEmptyY;
        private int endScreen;
        private int endEmptyX;
        private int endEmtpyY;
        private EmptyCellSpan(int startScreen, int startEmptyX, int startEmptyY,
                int endScreen, int endEmptyX, int endEmptyY) {
            this.startScreen = startScreen;
            this.startEmptyX = startEmptyX;
            this.startEmptyY = startEmptyY;
            this.endScreen = endScreen;
            this.endEmptyX = endEmptyX;
            this.endEmtpyY = endEmptyY;
        }
    }

    public ActionSheet getCellActionSheet() {
        return mLayoutActionSheet;
    }
    /* YUNOS END */

    /*YUNOS BEGIN*/
    //##date:2014/9/25 ##author:yangshan.ys BugId:5255762
    private boolean mHasChanged = false;

    public boolean hasChanged() {
        return mHasChanged;
    }

    public void setHasChanged(boolean hasChanged) {
        mHasChanged = hasChanged;
    }
    /*YUNOS END*/
    /* YUNOS BEGIN */
    // ##date:2014/10/16 ##author:yangshan.ys##5157204
    // for 3*3 layout
    final void adjustToThreeLayout() {
        mCellWidth = getResources().getDimensionPixelSize(R.dimen.bubble_icon_width_3_3);
        mCellHeight = getResources().getDimensionPixelSize(R.dimen.bubble_icon_height_3_3);
    }

    final void adjustFromThreeLayout() {
        if (this.getId() == R.id.layout_hideseat || this.getId() == R.id.layout_hotseat) {
            mCellWidth = getResources().getDimensionPixelSize(R.dimen.hotseat_cell_width);
            mCellHeight = getResources().getDimensionPixelSize(R.dimen.hotseat_cell_height);
        } else {
            mCellWidth = getResources().getDimensionPixelSize(R.dimen.workspace_cell_width);
            mCellHeight = getResources().getDimensionPixelSize(
                    ConfigManager.getCellCountY() == 4 ? R.dimen.cell_height_4_4 : R.dimen.cell_height_4_5);
        }
    }
    /* YUNSO END */

    public void onRemove() {
        mDragEnforcer.unbind();
    }

    /*YUNOS BEGIN  for LauncherEditMode*/
    private View mEditBtnContainer;
    private boolean mIsFakeChild;

    public static final int ADD_DELETE_ANIM_TIMEOUT = 100;
    private static final int SNAP_PAGE_DURATION = 200;
    private boolean mIsDeleting = false;
    public void addEditBtnContainer() {
        if (mEditBtnContainer == null) {
            mEditBtnContainer = inflate(mLauncher, R.layout.edit_btn_container, null);
            addView(mEditBtnContainer);
            final View delBtn = mEditBtnContainer.findViewById(R.id.delete_btn);
            final Workspace workspace = mLauncher.getWorkspace();
            delBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (workspace.isPageMoving()) {
                        return;
                    }
                    if (mIsDeleting) {
                        return;
                    }
                    mIsDeleting = true;
                    Drawable drawable = CellLayout.this.getBackground();
                    ObjectAnimator animator1 = ObjectAnimator.ofInt(drawable, "alpha", 255, 0);
                    ObjectAnimator animator2 = ObjectAnimator.ofFloat(view, "alpha", 1.0f, 0.01f);
                    AnimatorSet set = new AnimatorSet();
                    set.setDuration(ADD_DELETE_ANIM_TIMEOUT);
                    final int currentPage = workspace.getCurrentPage();
                    set.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            int tmpPage = currentPage + 1;
                            boolean isLast = false;
                            if (currentPage == workspace.getChildCount() - 1) {
                                tmpPage = currentPage - 1;
                                isLast = true;
                            } else {
                                tmpPage = currentPage + 1;
                            }
                            final int destPage = tmpPage;
                            final boolean currentReset = isLast;
                            /* YUNOS BEGIN */
                            // ## modules(Home Shell)
                            // ## date: 2016/03/14 ## author: wangye.wy
                            // ## BugID: 7953716: delete empty page without snapping
                        /*
                            workspace.snapToPage(destPage, SNAP_PAGE_DURATION);
                            workspace.runOnPageStopMoving(new Runnable() {
                                @Override
                                public void run() {
                                    CellLayout toDeleteCl = (CellLayout) workspace.getChildAt(currentPage);
                                    workspace.deleteEmptyScreen(toDeleteCl);
                                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_ARRANGE_DELETESCREEN);
                                    if (!workspace.isPageMoving()) {
                                        workspace.setCurrentPage(currentReset ? destPage : currentPage);
                                        workspace.onPageEndMoving();
                                    }
                                }
                            });
                        */
                            CellLayout toDeleteCl = (CellLayout) workspace.getChildAt(currentPage);
                            workspace.deleteEmptyScreen(toDeleteCl);
                            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_ARRANGE_DELETESCREEN);
                            if (!workspace.isPageMoving()) {
                                workspace.setCurrentPage(currentReset ? destPage : currentPage);
                                workspace.onPageEndMoving();
                            }
                            /* YUNOS END */
                            mIsDeleting = false;
                        }
                    });
                    set.playTogether(animator1, animator2);
                    set.start();
                }
            });
            View addBtn = mEditBtnContainer.findViewById(R.id.add_btn);
            addBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    final View celllayout = workspace.getChildAt(workspace.getCurrentPage());
                    workspace.animateAddEmptyScreen(celllayout);
                }
            });
        }
    }

    public void removeEditBtnContainer() {
        if (mEditBtnContainer != null) {
            super.removeView(mEditBtnContainer);
            mEditBtnContainer = null;
        }
    }

    public void setEditBtnContainerMode(boolean addMode) {
        if (mEditBtnContainer != null) {
            View delBtn = mEditBtnContainer.findViewById(R.id.delete_btn);
            View addBtn = mEditBtnContainer.findViewById(R.id.add_btn);
            if (addMode) {
                delBtn.setVisibility(View.INVISIBLE);
                addBtn.setVisibility(View.VISIBLE);
                mIsFakeChild = true;
            } else {
                addBtn.setVisibility(View.INVISIBLE);
                delBtn.setVisibility(View.VISIBLE);
                mIsFakeChild = false;
            }
        }
    }

    public boolean isFakeChild() {
        return mIsFakeChild;
    }

    public void setFake(boolean fake) {
        mIsFakeChild = fake;
    }

    public View getEditBtnContainer() {
        return mEditBtnContainer;
    }

    public boolean handleViewClick(View view) {
        boolean add = false;
        if (selectedViewToPos.containsKey(view)) {
            selectedViewToPos.remove(view);
            add = false;
        } else {
            int[] viewPos = new int[2];
            ItemInfo info = (ItemInfo) view.getTag();
            cellToPoint(info.cellX, info.cellY, viewPos);
            selectedViewToPos.put(view, viewPos);
            add = true;
        }
        this.invalidate();
        return add;
    }

    public void clearSelectedView() {
        selectedViewToPos.clear();
    }

    public Set<View> getSelectedView() {
        return selectedViewToPos.keySet();
    }

    protected void drawSelectFlag(Canvas canvas) {
        Drawable d = EditModeHelper.selectedFlag;
        if (d != null && mLauncher.isInLauncherEditMode() && getParent() instanceof Workspace) {
            int width = d.getIntrinsicWidth();
            int height = d.getIntrinsicHeight();
            int paddingTop = 0;
            int paddingLeft = 0;
            if (!mLauncher.getIconManager().supprtCardIcon() && !selectedViewToPos.isEmpty()) {
                paddingLeft = getResources().getDimensionPixelSize(R.dimen.em_select_flag_left_padding);
                paddingTop = getResources().getDimensionPixelSize(R.dimen.em_select_flag_top_padding);
            }
            for (View view : selectedViewToPos.keySet()) {
                int[] pos = selectedViewToPos.get(view);
                canvas.save();
                if(pos[0] < width/2){
                    pos[0] = width/2;
                }
                if(pos[1] < height/2){
                    pos[1] = height/2;
                }
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/03/10 ## author: wangye.wy
                // ## BugID: 7945871: header in cell layout
                canvas.translate(pos[0] + paddingLeft, pos[1] + paddingTop + Workspace.sHeaderHeight / Workspace.sEditScale);
                /* YUNOS END */
                d.setBounds(-width / 2, -height / 2, width / 2, height / 2);
                d.draw(canvas);
                canvas.restore();
            }
        }
    }
    /*YUNOS END  for LauncherEditMode*/
    // YUNOS BEGIN PB
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(163418) ##date:2014/08/15
    // ##description: Added support for widget page
    public boolean isWidgetPage() {
        return (mWidgetPageInfo != null);
    }

    public void setWidgetPageInfo(WidgetPageManager.WidgetPageInfo info) {
        mWidgetPageInfo = info;
    }

    public WidgetPageManager.WidgetPageInfo getWidgetPageInfo() {
        return mWidgetPageInfo;
    }

    public String getWidgetPagePackageName() {
        return (mWidgetPageInfo != null ? mWidgetPageInfo.getPackageName() : null);
    }

    public View getWidgetPageHotseat() {
        return (mWidgetPageInfo != null ? mWidgetPageInfo.getHotseatView() : null);
    }

    public View getWidgetPageRootView() {
        return (mWidgetPageInfo != null ? mWidgetPageInfo.getRootView() : null);
    }
    // YUNOS END PB

    public void setIsLeftPage(boolean isLeftPage) {
        mIsLeftPage = isLeftPage;
    }

    public boolean isLeftPage() {
        mIsLeftPage = mLauncher.isSupportLifeCenter() && (mIsLeftPage || this.getId() == R.id.lifecenter_cell);
        return mIsLeftPage;
    }

    private static int CELL_PORT_X_COUNT = -1;
    private static int CELL_PORT_Y_COUNT = -1;
    private static int CELL_LAND_X_COUNT = -1;
    private static int CELL_LAND_Y_COUNT = -1;

    public boolean containsWidget() {
        return !getWidgetList().isEmpty();
    }

    private ArrayList<ItemInfo> getWidgetList() {
        ArrayList<ItemInfo> lstWidgetInfos = new ArrayList<ItemInfo>();
        ShortcutAndWidgetContainer container = getShortcutAndWidgetContainer();
        int childCount = container.getChildCount();
        View child = null;
        for (int i = 0; i < childCount; i++) {
            child = container.getChildAt(i);
            if (child != null) {
                Object tag = child.getTag();
                if (tag instanceof LauncherAppWidgetInfo || tag instanceof GadgetItemInfo) {
                    lstWidgetInfos.add((ItemInfo) tag);
                }
            }
        }
        return lstWidgetInfos;
    }

    private ArrayList<ItemInfo> getShortcutList() {
        ArrayList<ItemInfo> lstShortcuts = new ArrayList<ItemInfo>();
        ShortcutAndWidgetContainer container = getShortcutAndWidgetContainer();
        int childCount = container.getChildCount();
        View child = null;
        for (int i = 0; i < childCount; i++) {
            child = container.getChildAt(i);
            if (child != null) {
                Object tag = child.getTag();
                if (tag instanceof ShortcutInfo || tag instanceof FolderInfo) {
                    lstShortcuts.add((ItemInfo) child.getTag());
                }
            }
        }
        return lstShortcuts;
    }

    private class WidgetGadgetCompartor implements Comparator<ItemInfo> {

        @Override
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            return rhs.spanX * rhs.spanY - lhs.spanX * lhs.spanY;
        }
    }

    private class ShortcutCompartor implements Comparator<ItemInfo> {

        @Override
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            if (lhs.cellY == rhs.cellY) {
                return lhs.cellX - rhs.cellX;
            } else {
                return lhs.cellY - rhs.cellY;
            }
        }
    }

    public void transferAllXYsOnDataChanged() {
        long start = System.currentTimeMillis();
        ArrayList<ItemInfo> lstWidgets = getWidgetList();
        ArrayList<ItemInfo> lstShortcuts = getShortcutList();
        if (LauncherApplication.isInLandOrientation()) {
            land2Port(lstWidgets, lstShortcuts);
        } else {
            port2land(lstWidgets, lstShortcuts);
        }

        // update db
        Context context = LauncherApplication.getContext();
        for (ItemInfo widgetInfo : lstWidgets) {
            ContentValues values = new ContentValues();
            values.put(LauncherSettings.Favorites.CELLX, widgetInfo.cellX);
            values.put(LauncherSettings.Favorites.CELLY, widgetInfo.cellY);
            values.put(LauncherSettings.Favorites.CELLXLAND, widgetInfo.cellXLand);
            values.put(LauncherSettings.Favorites.CELLYLAND, widgetInfo.cellYLand);
            values.put(LauncherSettings.Favorites.CELLXPORT, widgetInfo.cellXPort);
            values.put(LauncherSettings.Favorites.CELLYPORT, widgetInfo.cellYPort);
            LauncherModel.updateItemById(context, widgetInfo.id, values, false);
        }

        for (ItemInfo info : lstShortcuts) {
            ContentValues values = new ContentValues();
            values.put(LauncherSettings.Favorites.CELLX, info.cellX);
            values.put(LauncherSettings.Favorites.CELLY, info.cellY);
            values.put(LauncherSettings.Favorites.CELLXLAND, info.cellXLand);
            values.put(LauncherSettings.Favorites.CELLYLAND, info.cellYLand);
            values.put(LauncherSettings.Favorites.CELLXPORT, info.cellXPort);
            values.put(LauncherSettings.Favorites.CELLYPORT, info.cellYPort);
            LauncherModel.updateItemById(context, info.id, values, false);
        }

        long end = System.currentTimeMillis();
        Log.d(TAG, " sxsexe_pad transferXYsOnDataChanged took " + (end - start) + "ms");
    }

    private void port2land(ArrayList<ItemInfo> lstWidgets, ArrayList<ItemInfo> lstShortcuts) {
        Log.d(TAG, "sxsexe_pad port2land");
        if (lstWidgets.isEmpty()) {
            // no widget, just exchange x y
            for (ItemInfo info : lstShortcuts) {
                info.cellXLand = info.cellYPort;
                info.cellYLand = info.cellXPort;
            }
        } else {
            doArrangeAllXYs(lstWidgets, lstShortcuts, true);
        }
    }

    private void land2Port(ArrayList<ItemInfo> lstWidgets, ArrayList<ItemInfo> lstShortcuts) {
        Log.d(TAG, "sxsexe_pad land2Port");
        if (lstWidgets.isEmpty()) {
            // no widget, just exchange x y
            for (ItemInfo info : lstShortcuts) {
                info.cellXPort = info.cellYLand;
                info.cellYPort = info.cellXLand;
            }
        } else {
            doArrangeAllXYs(lstWidgets, lstShortcuts, false);
        }
    }
    private void initXYCount() {
        if(LauncherApplication.isInLandOrientation()) {
            CELL_LAND_X_COUNT = ConfigManager.getCellCountX();
            CELL_LAND_Y_COUNT = ConfigManager.getCellCountY();
            CELL_PORT_X_COUNT = CELL_LAND_Y_COUNT;
            CELL_PORT_Y_COUNT = CELL_LAND_X_COUNT;
        } else {
            CELL_PORT_X_COUNT = ConfigManager.getCellCountX();
            CELL_PORT_Y_COUNT = ConfigManager.getCellCountY();
            CELL_LAND_X_COUNT = CELL_PORT_Y_COUNT;
            CELL_LAND_Y_COUNT = CELL_PORT_X_COUNT;
        }
    }

    private void doArrangeAllXYs(ArrayList<ItemInfo> lstWidgets, ArrayList<ItemInfo> lstShortcuts, boolean port2Land) {
        initXYCount();
        // !important
        int xCount = port2Land ? CELL_LAND_X_COUNT : CELL_PORT_X_COUNT;
        int yCount = port2Land ? CELL_LAND_Y_COUNT : CELL_PORT_Y_COUNT;
        int[][] itemSpaceArray = new int[xCount][yCount];

        Log.d(TAG, "sxsexe_pad doArrangeAllXYs xCount " + xCount + " yCount " + yCount + " lstWidgets.size "
                + lstWidgets.size() + " lstShortcuts.size " + lstShortcuts.size());

        // Step1:Collcect all widget and gadget , and sort
        Collections.sort(lstWidgets, new WidgetGadgetCompartor());

        // Step2:traverse the itemSpaceArray
        int infoCount = lstWidgets.size();
        boolean[] foundFlag = new boolean[infoCount];
        for (int i = 0; i < infoCount; i++) {
            ItemInfo tempInfo = lstWidgets.get(i);
            boolean found = false;
            for (int y = 0; y < yCount; y++) {
                if (found) {
                    break;
                }
                for (int x = 0; x < xCount; x++) {
                    if (itemSpaceArray[x][y] != 0) {
                        continue;
                    }

                    // find location, test if == 1
                    found = true;
                    if ((x + tempInfo.spanX) <= xCount && (y + tempInfo.spanY) <= yCount) {
                        for (int m = x; m < (x + tempInfo.spanX); m++) {
                            if (!found) {
                                break;
                            }
                            for (int n = y; n < (y + tempInfo.spanY); n++) {
                                if (itemSpaceArray[m][n] != 0) {
                                    found = false;
                                    break;
                                }
                            }
                        }
                        if (found) {
                            if (port2Land) {
                                tempInfo.cellXLand = x;
                                tempInfo.cellYLand = y;
                            } else {
                                tempInfo.cellXPort = x;
                                tempInfo.cellYPort = y;
                            }
                            Log.d(TAG, "sxsexe_pad doArrangeAllXYs find place for" + tempInfo + " x " + x + " y " + y);
                            for (int m = x; m < (x + tempInfo.spanX); m++) {
                                for (int n = y; n < (y + tempInfo.spanY); n++) {
                                    itemSpaceArray[m][n] = 1;
                                }
                            }
                            foundFlag[i] = true;
                            break;
                        } else {
                            continue;
                        }
                    } else {
                        found = false;
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < foundFlag.length; i++) {
            if (!foundFlag[i]) {
                Log.d(TAG, "sxsexe_pad doArrangeAllXYs return false foundFlag[" + i + "] " + foundFlag[i]);
            }
        }

        // Step3:Set all left unoccupied location to shortcuts
        Collections.sort(lstShortcuts, new ShortcutCompartor());
        int index = 0;
        int count = lstShortcuts.size();
        ItemInfo sInfo = null;
        for (int x = 0; x < xCount; x++) {
            for (int y = 0; y < yCount; y++) {
                if (index < count && itemSpaceArray[x][y] == 0) {
                    itemSpaceArray[x][y] = 2;
                    sInfo = lstShortcuts.get(index++);
                    if (port2Land) {
                        sInfo.cellXLand = x;
                        sInfo.cellYLand = y;
                    } else {
                        sInfo.cellXPort = x;
                        sInfo.cellYPort = y;
                    }
                    Log.d(TAG, "sxsexe_pad doArrangeAllXYs find place for" + sInfo + "sInfo.cellXLand "
                            + sInfo.cellXLand + " sInfo.cellYLand " + sInfo.cellYLand + " sInfo.cellXPort "
                            + sInfo.cellXPort + " sInfo.cellYPort " + sInfo.cellYPort + " cellX " + sInfo.cellX
                            + " cellY " + sInfo.cellY);
                }
            }
        }
        return;
    }

    private boolean doCheckWidgetAccepted(ItemInfo info, int orientation) {

        // !important
        int xCount = ConfigManager.getCellCountX(orientation);
        int yCount = ConfigManager.getCellCountY(orientation);
        int[][] itemSpaceArray = new int[xCount][yCount];

        // Step1:Collcect all widget and gadget , and sort
        LauncherAppWidgetInfo tempWidgetInfo = new LauncherAppWidgetInfo(-1, null);
        tempWidgetInfo.spanX = info.spanX;
        tempWidgetInfo.spanY = info.spanY;
        ArrayList<ItemInfo> lstWidgets = getWidgetList();
        lstWidgets.add(tempWidgetInfo);
        Collections.sort(lstWidgets, new WidgetGadgetCompartor());

        // Step2:Traverse itemSpaceArray
        int infoCount = lstWidgets.size();
        boolean[] foundFlag = new boolean[infoCount];
        for (int i = 0; i < infoCount; i++) {
            ItemInfo tempInfo = lstWidgets.get(i);
            boolean found = false;
            for (int y = 0; y < yCount; y++) {
                if (found) {
                    break;
                }
                for (int x = 0; x < xCount; x++) {
                    if (itemSpaceArray[x][y] != 0) {
                        continue;
                    }

                    found = true;
                    if ((x + tempInfo.spanX) <= xCount && (y + tempInfo.spanY) <= yCount) {
                        for (int m = x; m < (x + tempInfo.spanX); m++) {
                            if (!found) {
                                break;
                            }
                            for (int n = y; n < (y + tempInfo.spanY); n++) {
                                if (itemSpaceArray[m][n] != 0) {
                                    found = false;
                                    break;
                                }
                            }
                        }
                        if (found) {
                            for (int m = x; m < (x + tempInfo.spanX); m++) {
                                for (int n = y; n < (y + tempInfo.spanY); n++) {
                                    itemSpaceArray[m][n] = 1;
                                }
                            }
                            foundFlag[i] = true;
                            break;
                        } else {
                            continue;
                        }
                    } else {
                        found = false;
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < foundFlag.length; i++) {
            if (!foundFlag[i]) {
                Log.d(TAG, "doCheckWidgetInPort return false foundFlag[" + i + "] " + foundFlag[i] + " " + info);
                return false;
            }
        }

        // Step3
        int unOccupiedCount = 0;
        ArrayList<ItemInfo> lstShortcuts = getShortcutList();
        for (int x = 0; x < xCount; x++) {
            for (int y = 0; y < yCount; y++) {
                if (itemSpaceArray[x][y] == 0) {
                    unOccupiedCount++;
                }
            }
        }
        if (unOccupiedCount >= lstShortcuts.size()) {
            Log.d(TAG, "doCheckWidgetInPort can place info " + info);
        } else {
            Log.d(TAG, "doCheckWidgetInPort can not place info " + info);
        }

        return unOccupiedCount >= lstShortcuts.size();
    }

    private boolean checkSpaceForWidgetInfo(ItemInfo info, int orientation) {
        initXYCount();
        int countX = ConfigManager.getCellCountX(orientation);
        int countY = ConfigManager.getCellCountY(orientation);

        if(ConfigManager.isLandOrienSupport()) {
            if (info.spanX > Math.min(countX, countY) || info.spanY > Math.min(countX, countY)) {
                Log.d(TAG, "checkSpaceForWidgetInfo info no enough space " + info.title + " spanx " + info.spanX
                        + " spany " + info.spanY);
                return false;
            }
        }

        ArrayList<ItemInfo> lstWidgets = getWidgetList();
        if (lstWidgets.isEmpty()) {
            Log.d(TAG, "checkSpaceForWidgetInfo nowidgets in screen put " + info);
            return true;
        } else {
            return doCheckWidgetAccepted(info, orientation);
        }
    }

    /**
     *
     * @param info
     * @param orientation
     *            check space in this orientation
     * @return
     */
    public boolean checkSpaceAvailable(ItemInfo info, int orientation) {
        boolean result = true;
        if (info instanceof ShortcutInfo || info instanceof FolderInfo) {
            int[] cellXY = new int[2];
            result = findCellForSpan(cellXY, 1, 1);
            if (result) {
                if (cellXY[0] != -1 && cellXY[1] != -1) {
                    markCellsForView(cellXY[0], cellXY[1], 1, 1, mOccupied, false);
                }
            }
        } else if (info instanceof PendingAddWidgetInfo || info instanceof PendingAddGadgetInfo) {
            // check if enough space for one widget
            result = checkSpaceForWidgetInfo(info, orientation);
        }
        Log.d(TAG, "sxsexe_test   checkSpaceAvailable info " + info + " result " + result);
        return result;
    }

    public void updateShortcutXYOnMove(ItemInfo info) {
        boolean isLand = LauncherApplication.isInLandOrientation();
        ScreenPosition sp = LauncherModel.findEmptyCellAndOccupy(info.screen, info.spanX, info.spanY, !isLand);
        Log.d(TAG, "sxsexe_pad  updateShortcutXYOnMove sp  " + sp);
        if (sp != null) {
            if (isLand) {
                info.cellXPort = sp.xPort;
                info.cellYPort = sp.yPort;
            } else {
                info.cellXLand = sp.xLand;
                info.cellYLand = sp.yLand;
            }
        } else {
            Log.e(TAG, "sxsexe_pad  updateShortcutXYOnMove failed to find empty cell in screen " + info.screen);
            return;
        }

        ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites.CELLX, info.cellX);
        values.put(LauncherSettings.Favorites.CELLY, info.cellY);
        values.put(LauncherSettings.Favorites.CELLXLAND, info.cellXLand);
        values.put(LauncherSettings.Favorites.CELLYLAND, info.cellYLand);
        values.put(LauncherSettings.Favorites.CELLXPORT, info.cellXPort);
        values.put(LauncherSettings.Favorites.CELLYPORT, info.cellYPort);
        info.dumpXY("CellLayout.updateShortcutXYOnMove");
        LauncherModel.updateItemById(mLauncher.getApplicationContext(), info.id, values, false);
    }
}
