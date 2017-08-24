/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.aliyun.homeshell.CellLayout.Mode;
import com.aliyun.homeshell.DropTarget.DragObject;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.atom.AtomManager;
import com.aliyun.homeshell.hideseat.Hideseat;
import com.aliyun.homeshell.icon.BubbleResources;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.icon.BubbleController;
import com.aliyun.homeshell.icon.TitleColorManager;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.setting.HomeShellSetting;

import java.util.ArrayList;
import java.util.List;

public class Hotseat extends FrameLayout {
    private static final String TAG = "Hotseat";

    private Launcher mLauncher;
    private CellLayout mContent;

    private int mCellCountX;
    private int mCellCountY;
    private int mAllAppsButtonRank;
    private int mWGap;
    private int mHGap;
    private int dropTargetHeight = getResources().getDimensionPixelSize(R.dimen.droptarget_height);
    private int mTopPaddingLand = getResources().getDimensionPixelSize(R.dimen.cell_layout_top_padding_land);
    private boolean mTransposeLayoutWithOrientation;
    private boolean mIsLandscape;

    private AnimatorSet mAnimatorSet;

    /*YUNOS BEGIN*/
    //##date:2013/12/07 ##author:xiaodong.lxd
    //for hotseat auto-replace
    private BubbleTextView mInvisibleView;
    private boolean mInvisibleViewAdded = false;
    private int mCurrentInvisibleIndex = -1;
    private int mAnimStartY, mAnimEndY, mAnimStartX, mAnimEndX;
    /*YUNOS END*/

    //added by xiaodong.lxd for bug#96460
    private boolean mTouchInHotseat;

    private enum HotseatDragState {NONE, DRAG_IN, DRAG_OUT};
    private HotseatDragState mDragState = HotseatDragState.NONE;
    private View mDragedItemView;
//    private boolean mAnimBackRunning = false;
    private boolean mAnimEnterRunning = false;
    private boolean mAnimLeftRunning = false;


//    private List<SwapItemInfo> listAnimators;
//    private SwapThread mSwapThread;
//    private MyHandler myHandler;
//    private boolean mSwapThreadRunning = false;
    private int mLastTouchX = -1;
    private int mLastTouchY = -1;
    private int mMoveDireciton = 0;//0:left; 1:right
    private int mXOffset;

    private ArrayList<View> mViewCacheList = null;

    private boolean mStaticHotSeat;
    private ArrayList<Pair<Integer, Integer>> mStaticLayoutModel;
    private View mAtomView;

    private int mAtomSiblingOffset;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.Hotseat, defStyle, 0);
        Resources r = context.getResources();

        /* YUNOS BEGIN */
        //##date:2014/04/16 ##author:nater.wg ##BugID:110407
        // Get values of configures from ConfigManager
//        mCellCountX = a.getInt(R.styleable.Hotseat_cellCountX, -1);
//        mCellCountY = a.getInt(R.styleable.Hotseat_cellCountY, -1);
        mCellCountX = ConfigManager.getHotseatMaxCountX();
        mCellCountY = ConfigManager.getHotseatMaxCountY();
        /* YUNOS END */
        mViewCacheList = new ArrayList<View>(mCellCountX);

        mAtomSiblingOffset = r.getDimensionPixelSize(R.dimen.dock_atom_sibling_offset);
        mAllAppsButtonRank = r.getInteger(R.integer.hotseat_all_apps_index);
        mTransposeLayoutWithOrientation =
                r.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);
        mIsLandscape = context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
        a.recycle();

    }

    public void setup(Launcher launcher) {
        mLauncher = launcher;
        setOnKeyListener(new HotseatIconKeyEventListener());
        checkStaticHotseat();
    }

    public CellLayout getLayout() {
        return mContent;
    }

    private boolean hasVerticalHotseat() {
        return (mIsLandscape && mTransposeLayoutWithOrientation);
    }

    /* Get the orientation invariant order of the item in the hotseat for persistence. */
    public int getOrderInHotseat(int x, int y) {
        return hasVerticalHotseat() ? (mContent.getCountY() - y - 1) : x;
    }
    /* Get the orientation specific coordinates given an invariant order in the hotseat. */
    int getCellXFromOrder(int rank) {
        return hasVerticalHotseat() ? 0 : rank;
    }
    int getCellYFromOrder(int rank) {
        return hasVerticalHotseat() ? (mContent.getCountY() - (rank + 1)) : 0;
    }
    public boolean isAllAppsButtonRank(int rank) {
        return /*rank == mAllAppsButtonRank;*/false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mCellCountX < 0) mCellCountX = LauncherModel.getCellCountX();
        if (mCellCountY < 0) mCellCountY = LauncherModel.getCellCountY();
        mContent = (CellLayout) findViewById(R.id.layout_hotseat);
        if (!LauncherApplication.isInLandOrientation()) {
            mContent.setGridSize(ConfigManager.getHotseatAtomRank() >  0 ?
                    mCellCountX + 1 : mCellCountX, mCellCountY);
        } else {
            mContent.setGridSize(mCellCountX, ConfigManager.getHotseatAtomRank() >  0 ?
                    mCellCountY + 1 : mCellCountY);
        }/* YUNOS BEGIN */
        // ##date:2014/10/16 ##author:yangshan.ys##5157204
        // for 3*3 layout
        // the icons display in hotseat and workspace is same when aged mode
        if (!AgedModeUtil.isAgedMode()) {
            mContent.setMode(Mode.HOTSEAT);
        }
        //generateInvisibleView();
        if (AgedModeUtil.isAgedMode()) {
            getLayoutParams().height = getContext().getResources()
                    .getDimensionPixelSize(R.dimen.workspace_cell_height_3_3);
            int padding = 0;
            padding = (getLayoutParams().height - mContent.getCellHeight()) / 2;
            if(padding > 0){
                this.setPadding(this.getPaddingLeft(), padding, this.getPaddingRight(), padding);
            }
        }
        /* YUNSO END */
        resetLayout();
    }
    /*YUNOS BEGIN*/
    //##date:2013/12/07 ##author:xiaodong.lxd
    //generate invisible view for auto-replace
    private void generateInvisibleView() {
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        mInvisibleView = (BubbleTextView)
                inflater.inflate(R.layout.application, mContent, false);
        mInvisibleView.setAlpha(0);
        /*mInvisibleView.setCompoundDrawablesWithIntrinsicBounds(null,
                context.getResources().getDrawable(R.drawable.widget_preview_tile), null, null);
        mInvisibleView.setContentDescription(context.getString(R.string.all_apps_button_label));*/
        int x = getCellXFromOrder(mAllAppsButtonRank);
        int y = getCellYFromOrder(mAllAppsButtonRank);
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(x,y,1,1);
        lp.canReorder = false;
        mInvisibleView.setLayoutParams(lp);
    }
    /*YUNOS END*/

    void resetLayout() {
        mContent.removeAllViewsInLayout();
        checkStaticHotseat();
        Log.d(TAG, "sxsexe_dock resetLayout  isStaticHotsaet " + mStaticHotSeat + " mAtomView " + mAtomView);
        if (isStaticHotsaet()) {
            updateAtomView();
        } else if (mAtomView != null && mAtomView.getParent() != null) {
            ((ViewGroup) mAtomView.getParent()).removeView(mAtomView);
            mAtomView = null;
            if (mLauncher != null) {
                Resources r = mLauncher.getResources();
                int pl = r.getDimensionPixelSize(R.dimen.hotseat_left_padding);
                int pr = r.getDimensionPixelSize(R.dimen.hotseat_right_padding);
                mContent.setPadding(pl, mContent.getPaddingTop(), pr, mContent.getPaddingBottom());
            }
        }
    }

    private void checkStaticHotseat() {
        if (AgedModeUtil.isAgedMode()) {
            mStaticHotSeat = false;
        } else {
            if (mLauncher != null) {
                mStaticHotSeat = HomeShellSetting.getStaticHotSeat(mLauncher);
            }
        }
        if (mStaticHotSeat) {
            Resources r = mLauncher.getResources();
            int pl = r.getDimensionPixelSize(R.dimen.dock_static_padding_left);
            int pr = r.getDimensionPixelSize(R.dimen.dock_static_padding_right);
            mContent.setPadding(pl, mContent.getPaddingTop(), pr, mContent.getPaddingBottom());
            BubbleResources.sAtomSiblingOffset = r.getDimensionPixelSize(R.dimen.dock_atom_sibling_offset);
            buildStaticLayoutModel();
        } else {
            BubbleResources.sAtomSiblingOffset = 0;
        }
    }

    // author : xiaodong.lxd 2015-12-18
    // Build a list mode for static hotseat
    private void buildStaticLayoutModel() {
        int parentWidth = mContent.getShortcutAndWidgetContainer().getWidth();
        if (parentWidth == 0) {
            return;
        }
        int maxCount = ConfigManager.getHotseatMaxCount();
        int cellW = mContent.getCellWidth();
        if (mStaticLayoutModel == null) {
            mStaticLayoutModel = new ArrayList<Pair<Integer, Integer>>(maxCount);
        }

        int gap = (parentWidth - cellW * (maxCount + 1)) / maxCount;
        mStaticLayoutModel.clear();
        for (int i = 0; i < maxCount; i++) {
            int x = 0;
            int y = 0;
            if(LauncherApplication.isInLandOrientation()) {
                // TODO
                y = 0;
                x = 0;
            } else {
                x = getXYInStaticMode(i, cellW, gap, false);
                y = 0;
            }
            Pair<Integer, Integer> p = new Pair<Integer, Integer>(x, y);
            mStaticLayoutModel.add(p);
        }
    }

    /*YUNOS BEGIN*/
    //##date:2013/12/07 ##author:xiaodong.lxd
    /**
     * whle drag item into hotseat
     * @param touchX
     * @param screen
     * @param fromHotset
     * @param d
     */
    protected void onEnterHotseat(int touchX, int touchY, final int screen, boolean fromHotset, final DragObject d) {
        mTouchInHotseat = true;
        final ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        if(count < 0) {
            return;
        }
        if(!fromHotset && isFull()) {
            return;
        }
        if(fromHotset) {

            //Log.d(TAG, "sxsexe55---->11onEnterHotseat mMoveDireciton " + mMoveDireciton + " touchX " + touchX );
            if(Math.abs(mLastTouchX - touchX) >= 5 && !LauncherApplication.isInLandOrientation()) {
                mMoveDireciton = mLastTouchX < touchX ? 1 : 0;
                mLastTouchX = touchX;
            } else if (LauncherApplication.isInLandOrientation() && Math.abs(mLastTouchY - touchY) >= 5) {
                mMoveDireciton = mLastTouchY < touchY ? 3 : 2;
            }
            //Log.d(TAG, "sxsexe55---->22onEnterHotseat mDragState " + mDragState + " mDragedItemView " + mDragedItemView);

            if((mDragState == HotseatDragState.DRAG_OUT && mDragedItemView != null)
                    || (mDragedItemView != null && mDragedItemView.getParent() == null && mDragState != HotseatDragState.DRAG_IN)) {
                //drag one item belongs to hotseat from workspace back to hotseat
//                mDragState = HotseatDragState.DRAG_SWAP;
                animateBackToHotseat(count, touchX, d.y, container);
            } else {
                //drag one item from hotseat to hotseat
                mDragState = HotseatDragState.DRAG_IN;
                animateSwap(touchX, d.y, screen, fromHotset, d);
            }
        } else {
            checkAnimateOnEnter(count, touchX, d.y, container);
        }
    }

    private void checkAnimateOnEnter(int childCount, int touchX, int touchY, ShortcutAndWidgetContainer container) {
        if (mAnimEnterRunning) {
            return;
        }
        //drag one item from workspace or folder
        int index = getAppropriateIndex(touchX, touchY);
//        Log.d(TAG, "sxsexe55>>>>>>>>>>>> +++++ checkAnimateOnEnter index " + index + " mCurrentInvisibleIndex " + mCurrentInvisibleIndex);
        if(mCurrentInvisibleIndex != index || mDragState == HotseatDragState.NONE) {
            mInvisibleViewAdded = false;
            Log.d(TAG, "sxsex55>>>>>>>>>>>>  checkAnimateOnEnter removeView(mInvisibleView) ");
            //container.removeView(mInvisibleView);
            //mViewCacheList.remove(mInvisibleView);
            removeViewFromCacheList(mInvisibleView);
            mInvisibleView = null;
        }
        //childCount = container.getChildCount();
        childCount = mViewCacheList.size();

        if(!mInvisibleViewAdded) {
            /*Log.d(TAG, "sxsexe55>>>>>>>>>>>>  checkAnimateOnEnter11 mInvisibleViewAdded " + mInvisibleViewAdded
                    + " index " + index + " childCount " + childCount + " cacheSize " + mViewCacheList.size()
                    + " mCurrentInvisibleIndex " + mCurrentInvisibleIndex
                    + " mDragState " + mDragState);*/
            if(mInvisibleView == null) {
                generateInvisibleView();
            }
            if (isStaticHotsaet()) {
                addToCacheList(index, mInvisibleView);
                mCurrentInvisibleIndex = index;
                mInvisibleViewAdded = true;
                mDragState = HotseatDragState.DRAG_IN;
                animateOnEnter(index, false);
            } else {
                if(index == 0 && childCount <= 1) {
                    if(index < childCount) {
                        //container.addView(mInvisibleView, index);
                       addToCacheList(index, mInvisibleView);
                        //addViewWithoutInvalidate(mInvisibleView, index);
                    } else {
                    //container.addView(mInvisibleView);
                        addToCacheList(-1, mInvisibleView);
                        //addViewWithoutInvalidate(mInvisibleView, -1);
                    }
                    mCurrentInvisibleIndex = 0;
                 } else if(index == (childCount-1)) {
                    //View view = container.getChildAt(index);
                    View view = mViewCacheList.get(index);
                    CellLayout.LayoutParams lp = (com.aliyun.homeshell.CellLayout.LayoutParams) view.getLayoutParams();
                    if(!LauncherApplication.isInLandOrientation()) {
                        int correctedX = getCorrectedX(lp.x, mMoveDireciton == 0);
//                      Log.d(TAG, "sxsexe55>>>>>>>>>>>> lp.x " + lp.x + " correctedX " + correctedX
//                              + " touchX " + touchX + " touchY " + touchY);
                        if(touchX < correctedX) {
                            //container.addView(mInvisibleView, index);
                            addToCacheList(index, mInvisibleView);
                            mCurrentInvisibleIndex = index;
                        } else {
                            if(mMoveDireciton == 0) {
//                              container.addView(mInvisibleView, index);
                                addToCacheList(index, mInvisibleView);
                                mCurrentInvisibleIndex = index;
                            } else {
                                addToCacheList(-1, mInvisibleView);
                                //container.addView(mInvisibleView);
                                mCurrentInvisibleIndex = index + 1;
                            }
                        }
                    }else{
                        int correctedY = getCorrectedY(lp.y);
                        if(touchY < correctedY) {
                            addToCacheList(index, mInvisibleView);
                            mCurrentInvisibleIndex = index;
                        } else {
//                          if(mMoveDirection == 3) {
//                              addToCacheList(index, mInvisibleView);
//                                mCurrentInvisibleIndex = index;
//                          } else {
                                addToCacheList(-1, mInvisibleView);
                                mCurrentInvisibleIndex = index +1;
//                          }
                        }
                    }
                } else {
                //container.addView(mInvisibleView, index);
                    dumpViewCacheList();
                    addToCacheList(index, mInvisibleView);
                    mCurrentInvisibleIndex = index;
                }
                mInvisibleViewAdded = true;
                //reLayout();
                mDragState = HotseatDragState.DRAG_IN;
                animateOnEnter(false);
            }
        }
    }

    private void animateOnEnter(int index, boolean fromHotseat) {
        int count = mViewCacheList.size();
        if(mAnimLeftRunning && mAnimatorSet != null) {
            mAnimatorSet.end();
        }
        if (fromHotseat) return;
        boolean isLand = LauncherApplication.isInLandOrientation();
        int maxCount = isLand ? ConfigManager.getHotseatMaxCountY()
                : ConfigManager.getHotseatMaxCountX();
        View[] occupied = new View[maxCount];
        for (int i = 0; i < count; i++) {
            View v = mViewCacheList.get(i);
            if (v != null && v != mInvisibleView) {
                CellLayout.LayoutParams lp =
                        (CellLayout.LayoutParams) v.getLayoutParams();
                if (v.getTag() != null) {
                    occupied[isLand ? lp.cellY : lp.cellX] = v;
                }
            }
        }
        if (occupied[index] == null) return;
        int newIndex = findEmptyCell(occupied, index);
        int gap = calculateGap(count);
        int width = mContent.getCellWidth();
        ArrayList<Animator> items = new ArrayList<Animator>();
        if (newIndex < index) {
            for (int j = newIndex + 1; j <= index; j++) {
                View v = occupied[j];
                if (v != null && v != mInvisibleView) {
                    CellLayout.LayoutParams lp =
                            (CellLayout.LayoutParams) v.getLayoutParams();
                    int srcCoord = isLand ? lp.y : lp.x;
                    int destCoord = srcCoord - (gap + width);
                    if (isLand) {
                        lp.cellY--;
                    } else {
                        lp.cellX--;
                       }
                    items.add(createAnimator(v, srcCoord, destCoord, null, null, true));
                }
            }
        } else {
            for (int j = index; j < newIndex; j++) {
                View v = occupied[j];
                if (v != null && v != mInvisibleView) {
                    CellLayout.LayoutParams lp =
                            (CellLayout.LayoutParams) v.getLayoutParams();
                    int srcCoord = isLand ? lp.y : lp.x;
                    int destCoord = srcCoord + (gap + width);
                    if (isLand) {
                        lp.cellY++;
                    } else {
                        lp.cellX++;
                       }
                    items.add(createAnimator(v, srcCoord, destCoord, null, null, true));
                }
            }
        }
        if(mAnimatorSet == null) {
            mAnimatorSet = new AnimatorSet();
        } else {
            mAnimatorSet.removeAllListeners();
            if(mAnimatorSet.isRunning()) {
                mAnimatorSet.end();
            }
            clearAnimFlags();
        }
        if(!items.isEmpty()) {
            mAnimatorSet.playTogether(items);

            mAnimatorSet.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    mAnimatorSet = null;
                    mAnimEnterRunning = false;
                    fillViewsFromCache();
                    dumpViewCacheList();
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    mAnimEnterRunning = true;
                }
            });
            mAnimatorSet.start();
        }
    }

    private int findEmptyCell(View[] occupied, int index) {
        if (occupied[index] == null) return index;
        int count = occupied.length;
        for (int i = 1; i < count; i++) {
            if (index - i >= 0 && occupied[index - i] == null) return index - i;
            if (index + i < count && occupied[index + i] == null) return index + i;
        }
        return index;
    }


    private int getCorrectedX (int leftX, boolean leftOrRight) {
        int cellW = mContent.getCellWidth();
        int centerX = leftX + cellW / 2 + getLocationX() + mContent.getPaddingLeft();
        return leftOrRight ? centerX + mXOffset : centerX - mXOffset;
    }
    private int getCorrectedY (int topY) {
        int cellH = mContent.getCellHeight();
        int centerY = topY + cellH / 2 + getLocationY() + mContent.getPaddingTop();
        return centerY;
    }

    private void animateBackToHotseat(int childCount, int touchX, int touchY, ShortcutAndWidgetContainer container) {
//        Log.d(TAG, "sxsexe55---->++animateBackToHotseat");
        mDragState = HotseatDragState.DRAG_IN;
        checkAnimateOnEnter(childCount, touchX, touchY, container);
    }

    private void animateSwap(int touchX, int touchY, final int screen, boolean fromHotset, final DragObject d) {
        final ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        // Log.d(TAG, "sxsexe_dock------>animateSwap  enter mDragedItemView " + mDragedItemView);
        //final int index = getSwapIndex(touchX, touchY);
        if(mDragedItemView == null) {
            mDragedItemView = mLauncher.getWorkspace().getDragInfo().cell;
            //container.removeView(mDragedItemView);
            //replace by mInvisibleView
            mViewCacheList.remove(mDragedItemView);
            if(mInvisibleView == null) {
                generateInvisibleView();
            }
            if(screen < mViewCacheList.size()) {
                mCurrentInvisibleIndex = screen;
                addToCacheList(mCurrentInvisibleIndex, mInvisibleView);
            } else {
                addToCacheList(-1, mInvisibleView);
                mCurrentInvisibleIndex = mViewCacheList.size() - 1;
            }
            fillViewsFromCache();
            mInvisibleViewAdded = true;
//            Log.d(TAG, "sxsexe55------>animateSwap  create mDragedItemView " + mDragedItemView.getTag()
//                    + " mCurrentInvisibleIndex " + mCurrentInvisibleIndex);
            dumpViewCacheList();
        }

        checkAnimateOnEnter(container.getChildCount(), touchX, touchY, container);
   }

    private void animateOnEnter(boolean fromHotseat) {
        //ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
//        int count = container.getChildCount();
        int count = mViewCacheList.size();
        if(mAnimLeftRunning && mAnimatorSet != null) {
            mAnimatorSet.end();
        }

        if(fromHotseat) {

        } else {
            int gap = this.calculateGap(count);
            int workspaceCount;//countX when port and countY when land
            int cellDimen;//cellWidth when port and cellHeight when land
            int startCoord = 0;
            if(!LauncherApplication.isInLandOrientation()) {
                workspaceCount = ConfigManager.getCellCountX();
                cellDimen = mContent.getCellWidth();
            } else {
                workspaceCount = ConfigManager.getCellCountY();
                cellDimen = mContent.getCellHeight();
            }
            if(count < workspaceCount) {
                //if the amount of items in hotseat less than workspace,we should left gap in left and right of hotseat
                startCoord = gap;
            }

            if(mAnimatorSet == null) {
                mAnimatorSet = new AnimatorSet();
            } else {
                mAnimatorSet.removeAllListeners();
                if(mAnimatorSet.isRunning()) {
                    mAnimatorSet.end();
                }
                clearAnimFlags();
            }

            ArrayList<Animator> items = new ArrayList<Animator>();
            int srcCoord = 0;
            int destCoord = 0;
//            Log.d(TAG, "sxsexe55---->animateOnEnter count"  + count);
            for (int i = 0; i < count; i++) {
                //View v = container.getChildAt(i);
                View v = mViewCacheList.get(i);
                if(v == null) {
                    continue;
                }
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
                if(!LauncherApplication.isInLandOrientation()) {
                    srcCoord = lp.x;
                    destCoord = startCoord + lp.leftMargin;
                } else {
                    srcCoord = lp.y;
                    destCoord = startCoord + lp.topMargin;
                }
                startCoord += (cellDimen + gap);
                if(v == mInvisibleView || srcCoord == destCoord) {
                    continue;
                }
//                ItemInfo info = (ItemInfo) v.getTag();
//                Log.d(TAG, "sxsexe55---->animateOnEnter i " + i + " srcX " + srcX + " destX " + destX + " info " + (info == null ? "info" : info.title));
                items.add(createAnimator(v, srcCoord, destCoord, null, null, true));
            }
            if(!items.isEmpty()) {
                mAnimatorSet.playTogether(items);

                mAnimatorSet.addListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mAnimatorSet = null;
                        mAnimEnterRunning = false;
//                        Log.d(TAG, "sxsexe55---->animateOnEnter end ");
                        fillViewsFromCache();
                        dumpViewCacheList();
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
//                        Log.d(TAG, "sxsexe55---->animateOnEnter start ");
                        mAnimEnterRunning = true;
                    }
                });
                mAnimatorSet.start();
            }
        }
    }

    private void clearAnimFlags() {
        mAnimEnterRunning = false;
        mAnimLeftRunning = false;
    }

    private void animateRestorePostion(final Runnable r) {

        ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        ArrayList<View> visibleChild = new ArrayList<View>();
        for (int i = 0; i < count; i++) {
            final View child = container.getChildAt(i);
            if (child != mInvisibleView) {
                visibleChild.add(child);
            }
        }
        int visibleCount = visibleChild.size();
        if (visibleCount > 0) {
            int gap = this.calculateGap(visibleCount);
            int workspaceCount;//countX when port and countY when land
            int cellDimen;//cellWidth when port and cellHeight when land
            int startCoord = 0;
            if(!LauncherApplication.isInLandOrientation()) {
                workspaceCount = ConfigManager.getCellCountX();
                cellDimen = mContent.getCellWidth();
            } else {
                workspaceCount = ConfigManager.getCellCountY();
                cellDimen = mContent.getCellHeight();
            }
            if(count < workspaceCount) {
                //if the amount of items in hotseat less than workspace,we should left gap in left and right of hotseat
                startCoord = gap;
            } else {
                if (!LauncherApplication.isInLandOrientation()) {
                    startCoord = mContent.getPaddingLeft();
                } else {
                    startCoord = mContent.getPaddingTop() + mTopPaddingLand;
                }
            }

            if(mAnimatorSet == null) {
                mAnimatorSet = new AnimatorSet();
            } else {
                mAnimatorSet.removeAllListeners();
                if(mAnimatorSet.isRunning()) {
                    mAnimatorSet.end();
                }
                clearAnimFlags();
            }
            ArrayList<Animator> items = new ArrayList<Animator>();
            int srcCoord = 0;
            int destCoord = 0;
            for (int i = 0; i < visibleCount; i++) {
                View v = visibleChild.get(i);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                        .getLayoutParams();
                if(!LauncherApplication.isInLandOrientation()) {
                    srcCoord = (int)v.getX();
                    destCoord = startCoord + lp.leftMargin;
                } else {
                    srcCoord = (int)v.getY();
                    destCoord = startCoord + lp.topMargin;
                }
                startCoord += (cellDimen + gap);
                items.add(createAnimator(v, srcCoord, destCoord, null, null, true));
            }
            mAnimatorSet.playTogether(items);
            mAnimatorSet.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    r.run();
                    mAnimatorSet = null;
//                    Log.d(TAG, "sxsexe55---------->animateRestorePostion end mInvisibleViewAdded " + mInvisibleViewAdded
//                            + " mDragState " + mDragState);
                }

                @Override
                public void onAnimationStart(Animator animation) {
//                    Log.d(TAG, "sxsexe55---------->animateRestorePostion start ");
                }
            });
            mAnimatorSet.start();
        }
    }

    private void animateLeftItems() {
//        Log.d(TAG, "sxsexe55---->animateLeftItems enter ");

        final ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        ArrayList<View> leftChild = new ArrayList<View>();
        for (int i = 0; i < count; i++) {
            final View child = container.getChildAt(i);
//            Log.d(TAG, "sxsexe55---->animateLeftItems i " + i
//                    + " child == mDragedItemView " + (child == mDragedItemView)
//                    + " child == mInvisibleView " + (child == mInvisibleView));
            if (child != mDragedItemView && child != mInvisibleView) {
                leftChild.add(child);
            }
        }

        int leftCount = leftChild.size();
        if (leftCount > 0) {
            int gap = calculateGap(leftCount);
            int workspaceCount;//countX when port and countY when land
            int cellDimen;//cellWidth when port and cellHeight when land
            int startCoord = 0;
            if(!LauncherApplication.isInLandOrientation()) {
                workspaceCount = ConfigManager.getCellCountX();
                cellDimen = mContent.getCellWidth();
            } else {
                workspaceCount = ConfigManager.getCellCountY();
                cellDimen = mContent.getCellHeight();
            }
            if(count < workspaceCount) {
                //if the amount of items in hotseat less than workspace,we should left gap in left and right of hotseat
                startCoord = gap;
            } else {
                if (!LauncherApplication.isInLandOrientation()) {
                    startCoord = mContent.getPaddingLeft();
                } else {
                    startCoord = mContent.getPaddingTop() + mTopPaddingLand;
                }
            }

            if(mAnimatorSet == null) {
                mAnimatorSet = new AnimatorSet();
            } else {
                mAnimatorSet.removeAllListeners();
                if(mAnimatorSet.isRunning()) {
                    mAnimatorSet.end();
                }
                clearAnimFlags();
            }

            ArrayList<Animator> items = new ArrayList<Animator>();
            int srcCoord = 0;
            Runnable r;
            for (int i = 0; i < leftCount; i++) {
                final View v = leftChild.get(i);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                        .getLayoutParams();
                if(!LauncherApplication.isInLandOrientation()) {
                    srcCoord = (int)v.getX();
                    final int destCoord = startCoord + lp.leftMargin;
                    r = new Runnable() {
                        @Override
                        public void run() {
                            v.setX(destCoord);
                        }
                    };
                    items.add(createAnimator(v, srcCoord, destCoord, null, r, true));
                } else {
                    srcCoord = (int)v.getY();
                    final int destCoord = startCoord + lp.topMargin;
                    r = new Runnable() {
                        @Override
                        public void run() {
                            v.setY(destCoord);
                        }
                    };
                    items.add(createAnimator(v, srcCoord, destCoord, null, r, true));
                }
                startCoord += (cellDimen + gap);
            }
            mAnimatorSet.playTogether(items);
            mAnimatorSet.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    if(mAnimLeftRunning) {
                        container.removeView(mDragedItemView);
                        mViewCacheList.remove(mDragedItemView);
                        if(mInvisibleViewAdded && mDragState == HotseatDragState.DRAG_OUT) {
                            container.removeView(mInvisibleView);
                            mViewCacheList.remove(mInvisibleView);
                            mInvisibleView = null;
                            mInvisibleViewAdded = false;
                            mCurrentInvisibleIndex = -1;
                        }
                    }
//                    Log.d(TAG, "sxsexe55---->animateLeftItems end");
                    mAnimatorSet = null;
                    mAnimLeftRunning = false;
                    dumpViewCacheList();
                    if(container.getChildCount() <= mViewCacheList.size()) {
                        fillViewsFromCache();
                    }
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    mAnimLeftRunning = true;
//                    Log.d(TAG, "sxsexe55---->animateLeftItems start");
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mAnimLeftRunning = false;
                }
            });
            mAnimatorSet.start();
        }
    }

    /**
     * drag exit hotseat
     */
    protected void onExitHotseat(boolean fromHotseat) {

        //Log.d(TAG, "sxsexe22------------>onExitHotseat mDragState " + mDragState);
        if(mDragState == HotseatDragState.NONE || mDragState == HotseatDragState.DRAG_OUT) {
            return;
        }

//        Log.d(TAG, "sxsexe55------------>onExitHotseat fromHotseat " + fromHotseat
//                + " mDragedItemView " + mDragedItemView + " parent " + (mDragedItemView==null ? " null " : mDragedItemView.getParent()));
        mTouchInHotseat = false;
        mDragState = HotseatDragState.DRAG_OUT;

        if (isStaticHotsaet()) {
            animateLeftItemsStatic();
            return;
        }

        if(fromHotseat && mDragedItemView != null) {
            //drag one item belongs to hotseat to workspace
            animateLeftItems();
            return;
        }

        if(!fromHotseat && mInvisibleViewAdded) {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    //mContent.removeView(mInvisibleView);
                    mViewCacheList.remove(mInvisibleView);
                    mInvisibleView = null;
                    mInvisibleViewAdded = false;
                    mCurrentInvisibleIndex = -1;
                    fillViewsFromCache();
                }
            };
            animateRestorePostion(r);
        }
    }

    private void animateLeftItemsStatic() {
        final ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        ArrayList<Animator> items = new ArrayList<Animator>();
        boolean isLand = LauncherApplication.isInLandOrientation();
        int atomRank = ConfigManager.getHotseatAtomRank();
        for (int i = 0; i < count; i++) {
            final View child = container.getChildAt(i);
            ItemInfo info = (ItemInfo) child.getTag();
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
            if (info == null) continue;
            int srcCoord = isLand ? lp.y : lp.x;
            boolean startWithGap = false;
            if (isLand) {
                lp.cellY = info.cellY;
            } else {
                startWithGap = ConfigManager.getHotseatMaxCountX() + 1 < ConfigManager.getCellCountX();
                lp.cellX = info.cellX;
            }
            int destCoord = isLand ? (mContent.getCellHeight() + mContent.getHeightGap()) *
                    (lp.cellY >= atomRank ? lp.cellY + 1 : lp.cellY)
                    : getXYInStaticMode(lp.cellX, mContent.getCellWidth(), mContent.getWidthGap(), startWithGap);
            if (srcCoord != destCoord) {
                items.add(createAnimator(child, srcCoord, destCoord, null, null, true));
            }
        }
        if(mAnimatorSet == null) {
            mAnimatorSet = new AnimatorSet();
        } else {
            mAnimatorSet.removeAllListeners();
            if(mAnimatorSet.isRunning()) {
                mAnimatorSet.end();
            }
            clearAnimFlags();
        }
        if(!items.isEmpty()) {
            mAnimatorSet.playTogether(items);

            mAnimatorSet.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    if(mAnimLeftRunning) {
                        container.removeView(mDragedItemView);
                        mViewCacheList.remove(mDragedItemView);
                        if(mInvisibleViewAdded && mDragState == HotseatDragState.DRAG_OUT) {
                            container.removeView(mInvisibleView);
                            mViewCacheList.remove(mInvisibleView);
                            mInvisibleView = null;
                            mInvisibleViewAdded = false;
                            mCurrentInvisibleIndex = -1;
                        }
                    }

                    mAnimatorSet = null;
                    mAnimLeftRunning = false;
                    dumpViewCacheList();
                    fillViewsFromCache();
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    mAnimLeftRunning = true;
//                    Log.d(TAG, "sxsexe55---->animateLeftItems start");
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mAnimLeftRunning = false;
                }
            });
            mAnimatorSet.start();
        }
    }

    private int getXYInStaticMode(int index, int cellW, int gap, boolean startWithGap) {
        int atomRank = ConfigManager.getHotseatAtomRank();
        if (LauncherApplication.isInLandOrientation()) {
            // TODO
            return 0;
        } else {
            int destX = 0;
            if (atomRank - index == 1) {
                destX = index * (cellW + gap) + mAtomSiblingOffset + (startWithGap ? gap : 0);
            } else if (atomRank == index) {
                destX = (index + 1) * (cellW + gap) - mAtomSiblingOffset + (startWithGap ? gap : 0);
            } else {
                destX = (index >= atomRank ? index + 1 : index) * (cellW + gap) + (startWithGap ? gap : 0);
            }
            /*
            Log.d(TAG, "sxsexe_dock     getXYInStaticMode index " + index + " startWithGap " + startWithGap + " gap " + mWGap + " cellW  "
                    + cellW + " destX " + destX + " mAtomView.w " + mAtomView.getWidth() + " atomRank " + atomRank + " mAtomSiblingOffset "
                    + mAtomSiblingOffset);
            */
            return destX;
        }
    }

    public void cleanAndReset() {
//        Log.d(TAG, "sxsexe55----------->cleanAndReset ");
        mDragState = HotseatDragState.NONE;
        mInvisibleView = null;
        mInvisibleViewAdded = false;
        mCurrentInvisibleIndex = -1;
        mDragedItemView = null;
        //mAnimBackRunning = false;
        mAnimEnterRunning = false;
        mAnimLeftRunning = false;
        mAnimatorSet = null;
        mLastTouchX = -1;
        mLastTouchY = -1;
        updateItemCell();
        reLayout();
        updateItemInDatabase();
    }

    /**
     * drop dragItem in hotseat
     * @param success
     * @return
     */
    public int onDrop(boolean success, int touchX, final DragView dragView, final View cell,
            final boolean removeDragView) {
/*        Log.d(TAG, "sxsexe55---->onDrop mDragedItemView " + mDragedItemView
                + " parent " + (mDragedItemView == null ? "null" : mDragedItemView.getParent())
                + " mDragedItemView.tag " + (mDragedItemView == null ? "null" : mDragedItemView.getTag())
                + " success " + success + " mInvisibleViewAdded " + mInvisibleViewAdded
                + " mCurrentInvisibleIndex " + mCurrentInvisibleIndex
                + " mDragState " + mDragState);*/

        if(mAnimLeftRunning) {
            if (mAnimatorSet != null){
                mAnimatorSet.cancel();
            }
        } else {
            if(mAnimatorSet != null && mAnimatorSet.isRunning()) {
                mAnimatorSet.end();
            }
        }


        int index = mCurrentInvisibleIndex;
        final Runnable onDropEndRunnable = new Runnable() {
            @Override
            public void run() {
/*                Log.d(TAG, "sxsexe55---->onDropEndRunnable cell " + cell
                        + " dragView " + dragView
                        + " cell.parent " + (cell == null ? " null " : cell.getParent())); */
                if(cell != null && cell.getVisibility() != View.VISIBLE)
                    cell.setVisibility(VISIBLE);
                mLauncher.getWorkspace().checkAndRemoveEmptyCell();
                final Hideseat hideseat = mLauncher.getHideseat();
                if(hideseat != null){
                    hideseat.removeEmptyScreen();
                }
                if(mInvisibleViewAdded) {
                    //getContainer().removeView(mInvisibleView);
                    mViewCacheList.remove(mInvisibleView);
                    mInvisibleView = null;
                }
                cleanAndReset();
                if(dragView != null)
                    mLauncher.getDragController().onDeferredEndDrag(dragView);
            }
        };

        if(!success || dragView == null) {
            onDropEndRunnable.run();
            return index;
        }
        Animator a = null;
        int srcX = (int) dragView.getX();
        int srcY = (int) dragView.getY();

        if(mDragState == HotseatDragState.DRAG_IN && mDragedItemView != null && mDragedItemView.getParent() ==null) {
            //drag one item belongs to hotseat to hotseat again

            if(mInvisibleViewAdded && mCurrentInvisibleIndex != -1) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams)mInvisibleView.getLayoutParams();
                int destX;
                int destY;
                if(!LauncherApplication.isInLandOrientation()) {
                    if (isStaticHotsaet()) {
                        destX = mContent.getPaddingLeft()
                                + getXYInStaticMode(mCurrentInvisibleIndex, mContent.getCellWidth(), mContent.getWidthGap(), false);
                    } else {
                        destX = lp.x + getLocationX() + mContent.getPaddingLeft();
                    }
                    destY = getLocationY();
                } else {
                    destX = getLocationX();
                    if (isStaticHotsaet()) {
                        int atomRank = ConfigManager.getHotseatAtomRank();
                        destY = (mCurrentInvisibleIndex >= atomRank ? mCurrentInvisibleIndex + 1 :
                            mCurrentInvisibleIndex) * (mContent.getCellHeight() + mContent.getHeightGap()) + mContent.getPaddingTop();
                    } else {
                        destY = lp.y + getLocationY() + mContent.getPaddingTop();
                    }
                }

                ItemInfo item = (ItemInfo)cell.getTag();
//                int order = getAppropriateIndex(lp.x);
//                int tmpindex = getOrderInHotseat(order, 0);
                UserTrackerHelper.sendDragIconReport(item, Favorites.CONTAINER_HOTSEAT, Favorites.CONTAINER_HOTSEAT, mCurrentInvisibleIndex, mCurrentInvisibleIndex, 0);

                final Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
                        container.removeView(mInvisibleView);
                        mViewCacheList.remove(mInvisibleView);
//                        Log.d(TAG, "sxsexe55----->onDrop runnable cell.parent " + cell.getParent());
                        if(cell != null && cell.getParent() != null) {
                            ViewGroup parent = (ViewGroup)cell.getParent();
                            parent.removeView(cell);
                        }
                        int count = container.getChildCount();
                        if(mCurrentInvisibleIndex >= count || mCurrentInvisibleIndex < 0) {
                            container.addView(cell);
                            mViewCacheList.add(cell);
                        } else {
                            container.addView(cell, mCurrentInvisibleIndex);
                            mViewCacheList.add(mCurrentInvisibleIndex, cell);
                        }
                        dumpViewCacheList();
                        onDropEndRunnable.run();
                    }
                };
//                Log.d(TAG, "sxsexe55----->onDrop end drag in from (" + srcX + " " + srcY + ") to ("
//                        + destX + " " + destY);
                a = createDropAnimator(dragView, srcX, srcY, destX, destY, r);
                a.start();
            } else {
                onDropEndRunnable.run();
            }

            return index;
        } else {
            if(success) {
                if(mInvisibleViewAdded && mCurrentInvisibleIndex != -1) {
                    mContent.getShortcutAndWidgetContainer().removeView(mInvisibleView);
                    mViewCacheList.remove(mInvisibleView);
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) mInvisibleView.getLayoutParams();

                    ItemInfo item = (ItemInfo)cell.getTag();
//                    int order = mLauncher.getHotseat().getAppropriateIndex(lp.x);
//                    int tmpindex = getOrderInHotseat(order, 0);
                    UserTrackerHelper.sendDragIconReport(item, item.container, Favorites.CONTAINER_HOTSEAT, mCurrentInvisibleIndex, mCurrentInvisibleIndex, 0);

/*                    Log.d(TAG, "sxsexe55----->onDrop end drag in"
                            + " childCount " + mViewCacheList.size() + " mInvisibleView " + mInvisibleView
                            + " lp.x " + lp.x  + " lp.y " + lp.y);*/
//                    a = createDropAnimator(dragView, srcX, srcY, lp.x, getLocationY(), onDropEndRunnable);
                    if(!LauncherApplication.isInLandOrientation()) {
                        int destX;
                        if (isStaticHotsaet()) {
                            int atomRank = ConfigManager.getHotseatAtomRank();
                         destX = (mCurrentInvisibleIndex >= atomRank ? mCurrentInvisibleIndex + 1 :
                                mCurrentInvisibleIndex) * (mContent.getCellWidth() + mContent.getWidthGap()) + mContent.getPaddingLeft();
                        } else {
                            destX = lp.x + getLocationX() + mContent.getPaddingLeft();
                        }
                        a = createDropAnimator(dragView, srcX, srcY, destX, getLocationY(), onDropEndRunnable);
                    } else {
                        a = createDropAnimator(dragView, srcX, srcY, getLocationX(), lp.y + getLocationY() + mContent.getPaddingTop(), onDropEndRunnable);
                    }
                } else {
                    onDropEndRunnable.run();
                }
                if(a != null) {
                    a.start();
                }
            }
        }

        return index;
    }
    /*YUNOS END*/

    private Animator createAnimator(final View v, final int srcCoord, final int destCoord,
            final Runnable onStartRunnable,final Runnable onEndRunnable, final boolean cleanTransX) {
        ObjectAnimator a;
        if(!LauncherApplication.isInLandOrientation()) {
            a = ObjectAnimator.ofPropertyValuesHolder(v,
                    PropertyValuesHolder.ofFloat(ViewHidePropertyName.X, srcCoord, destCoord));
        } else {
            a = ObjectAnimator.ofPropertyValuesHolder(v,
                    PropertyValuesHolder.ofFloat(ViewHidePropertyName.Y, srcCoord, destCoord));
        }
        a.setDuration(200);
        a.setInterpolator(new LinearInterpolator());
        a.addListener(new AnimatorListenerAdapter(){

            @Override
            public void onAnimationEnd(Animator animation) {
                //Log.d(TAG, "sxsexe22------> onAnimationEnd tranX " + v.getTranslationX() + " x " + v.getX());
                if(onEndRunnable != null) {
                    onEndRunnable.run();
                }
                v.setTranslationX(0);
                v.setTranslationY(0);
            }
        });
        return a;
    }

    private Animator createDropAnimator(final View v, int srcX, int srcY, int destX, int destY, final Runnable onDropEndRunnable) {
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v,
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.X, srcX, destX),
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.Y, srcY, destY),
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.SCALE_X, v.getScaleX(), 1.0f),
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.SCALE_Y, v.getScaleY(), 1.0f));
        a.setDuration(200);
        a.setInterpolator(new LinearInterpolator());
        a.addListener(new AnimatorListenerAdapter(){


            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if(onDropEndRunnable != null) {
                    onDropEndRunnable.run();
                }
            }
        });
        return a;
    }

    /*YUNOS BEGIN*/
    //##date:2013/11/22 ##author:xiaodong.lxd
    //override onLayout to resize all children view in hotseat when drag icon in or out
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        /* YUNOS BEGIN */
        // ##date:2014/10/16 ##author:yangshan.ys##5157204
        // for 3*3 layout
        if(!ConfigManager.isLandOrienSupport()){    //##date:2015/09/06 ##author:xiangke.xk
            LayoutParams param = (LayoutParams) this.getLayoutParams();
            if (AgedModeUtil.isAgedMode()) {
                param.height = mLauncher.getResources().getDimensionPixelSize(
                        R.dimen.workspace_cell_height_3_3);
            } else {
                param.height = mLauncher.getResources().getDimensionPixelSize(
                        R.dimen.button_bar_height_plus_padding);
            }
            reLayout(left, right, false);
            mAnimStartY = getTop();
            mAnimEndY = getBottom();
        }else{
            if(LauncherApplication.isInLandOrientation()){
                if(top >= dropTargetHeight) {

                } else {
                    top = dropTargetHeight - top;
                }
                reLayoutLandscape(top, bottom, false);
            }else{
                reLayoutPortrait(left, right, false);
            }
            mAnimStartY = getTop();
            mAnimEndY = getBottom();
            mAnimStartX = getLeft();
            mAnimEndX = getRight();
        }
        /* YUNSO END */
        if(LauncherApplication.isInLandOrientation()){  //##date:2015/09/06 ##author:xiangke.xk
            mContent.layout(getPaddingLeft(), top,
                    getPaddingLeft() + getPaddingRight() + mContent.getMeasuredWidth(),
                    this.getMeasuredHeight());
        }else{
            super.onLayout(changed, left, top, right, bottom);
        }
        //title color changed
        if (mLauncher != null && mLauncher.getTitleColorManager() != null) {
            int startX = left + getPaddingLeft() + mContent.getPaddingLeft();
            int startY = top + getPaddingTop() + mContent.getPaddingTop();
            mLauncher.getTitleColorManager().updateHotSeatParams(startX, startY, mContent.getCellHeight(), mWGap);
        }

    }

    private void reLayout() {
        if (ConfigManager.isLandOrienSupport()) {
            if (LauncherApplication.isInLandOrientation()) {
                reLayoutLandscape(getTop(), getBottom(), false);
            } else {
                reLayoutPortrait(getLeft(), getRight(), false);
            }
        } else {
            reLayout(getLeft(), getRight(), false);
        }
    }
    private void reLayout(int left, int right, boolean unvisibleCount) {
        ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        ArrayList<View> visibleChild = new ArrayList<View>();
        boolean flag = false;
        for (int i = 0; i < count; i++) {
            final View child = container.getChildAt(i);
            //Log.d(TAG, "sxsexe relayout dragging " + mLauncher.getDragController().isDragging() + "  child == mInvisibleView " + (child == mInvisibleView) );
        if (!mLauncher.getDragController().isDragging()
            && child == mInvisibleView) {
        flag = true;
        continue;
        }
            if (!unvisibleCount && child.getVisibility() != GONE) {
                visibleChild.add(child);
            }
        }
        if(flag) {
            updateItemCell();
        }

        int visibleCount = isStaticHotsaet() ? ConfigManager.getHotseatMaxCountX() + 1 : visibleChild.size();
        if (visibleCount > 0) {
//            Resources res = getResources();
            int width = right - left;
            width = width - getPaddingLeft() - getPaddingRight()
                    - mContent.getPaddingLeft() - mContent.getPaddingRight();


            int cellW = mContent.getCellWidth();
            mXOffset = 0;
            int l = 0;
            //int wGap = 0;
            int space = width - visibleCount * cellW;

            /* YUNOS BEGIN */
            // ##date:2014/10/16 ##author:yangshan.ys##5157204
            // for 3*3 layout
            int workspaceCountX = ConfigManager.getCellCountX();
            /* YUNSO END */
            boolean startWithGap = visibleCount < workspaceCountX;
            if (visibleCount >= workspaceCountX) {
                mWGap = (int) (space / (float) (visibleCount - 1));
            } else {
                mWGap = (int) (space / (float) (visibleCount + 1));
                l = mWGap;
            }

            boolean rtl = mContent.getShortcutAndWidgetContainer().isLayoutRtl();
            /* YUNOS BEGIN */
            // ##date:2014/07/30 ##author:hongchao.ghc ##BugID:137835
            boolean textViewNeedPadding = false;
            if (visibleCount > workspaceCountX && mWGap < 0) {
                textViewNeedPadding = true;
            }
            /* YUNOS END */
           visibleCount = isStaticHotsaet() ? visibleChild.size() : visibleCount;
            for (int i = 0; i < visibleCount; i++) {
                View v = visibleChild.get(!rtl ? i : visibleCount - i - 1);
                //!!important, in some case, the view's TranslationX is not 0 on animation ended
                v.setTranslationX(0);
                v.setTop(0);
                v.setBottom(mContent.getCellHeight());
                /* YUNOS BEGIN */
                // ##date:2014/07/30 ##author:hongchao.ghc ##BugID:137835
                BubbleTextView btv;
                if (textViewNeedPadding) {
                    Context context = getContext();
                    int paddingLeftAndRight = (-mWGap + (int) context.getResources().getDimension(
                            R.dimen.textview_padding_in_hotseat)) / 2;
                    if (v instanceof BubbleTextView) {
                        btv = (BubbleTextView) v;
                        btv.setTempPadding(paddingLeftAndRight);
                    } else if (v instanceof FolderIcon) {
                        FolderIcon fi = (FolderIcon) v;
                        fi.setTempPadding(paddingLeftAndRight);
                    }
                } else {
                    if (v instanceof BubbleTextView) {
                        btv = (BubbleTextView) v;
                        btv.resetTempPadding();
                    } else if (v instanceof FolderIcon) {
                        ((FolderIcon) v).resetTempPadding();
                    }
                }
                /* YUNOS END */
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                        .getLayoutParams();
                if (isStaticHotsaet()) {
  /*                  int atomRank = ConfigManager.getHotseatAtomRank();
                    if (atomRank - lp.cellX == 1) {
                        lp.x = lp.cellX * (cellW + mWGap) + lp.leftMargin + (startWithGap ? mWGap : 0)
                                + (nearAtomOffset == cellW || nearAtomOffset == 0 ? cellW / 5 : nearAtomOffset);
                    } else if (atomRank == lp.cellX) {
                        lp.x = (lp.cellX + 1) * (cellW + mWGap) + lp.leftMargin + (startWithGap ? mWGap : 0)
                                - (nearAtomOffset == cellW || nearAtomOffset == 0 ? cellW / 5 : nearAtomOffset);
                    } else {
                        lp.x = (lp.cellX >= atomRank ? lp.cellX + 1 : lp.cellX) * (cellW + mWGap) + lp.leftMargin
                                + (startWithGap ? mWGap : 0);
                    }*/
                    lp.x = getXYInStaticMode(lp.cellX, cellW, mWGap, startWithGap);
                } else {
                    lp.cellX = i;
                    lp.x = l + lp.leftMargin;
                    l += (cellW + mWGap);
                    lp.startWithGap = (visibleCount < workspaceCountX);
                }
            }
            mContent.setGridSize(cellW, mContent.getCellHeight(), mWGap, mContent.getHeightGap());
        }

        if(count > 0) {
            mContent.getShortcutAndWidgetContainer().invalidate();
        }
    }
    /*YUNOS END*/

    private void reLayoutPortrait(int left, int right, boolean unvisibleCount) {
        ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        ArrayList<View> visibleChild = new ArrayList<View>();
        boolean flag = false;
        for (int i = 0; i < count; i++) {
            final View child = container.getChildAt(i);
            //Log.d(TAG, "sxsexe relayout dragging " + mLauncher.getDragController().isDragging() + "  child == mInvisibleView " + (child == mInvisibleView) );
        if (!mLauncher.getDragController().isDragging()
            && child == mInvisibleView) {
        flag = true;
        continue;
        }
            if (!unvisibleCount && child.getVisibility() != GONE) {
                visibleChild.add(child);
            }
        }
        if(flag) {
            updateItemCell();
        }

        int visibleCount = visibleChild.size();
        if (visibleCount > 0) {
            int workspaceCountX = ConfigManager.getCellCountX();
            int cellW = mContent.getCellWidth();
            mXOffset = 0;
            int l = 0;
            mWGap = this.calculateGap(visibleCount);
            if(visibleCount < workspaceCountX) {
                l = mWGap;
            }
            boolean rtl = mContent.getShortcutAndWidgetContainer().isLayoutRtl();
            /* YUNOS BEGIN */
            // ##date:2014/07/30 ##author:hongchao.ghc ##BugID:137835
            boolean textViewNeedPadding = false;
            if (visibleCount > workspaceCountX && mWGap < 0) {
                textViewNeedPadding = true;
            }
            /* YUNOS END */
            for (int i = 0; i < visibleCount; i++) {
                View v = visibleChild.get(!rtl ? i : visibleCount - i - 1);
                //!!important, in some case, the view's TranslationX is not 0 on animation ended
                v.setTranslationX(0);
                v.setTop(0);
                v.setBottom(mContent.getCellHeight());
                /* YUNOS BEGIN */
                // ##date:2014/07/30 ##author:hongchao.ghc ##BugID:137835
                BubbleTextView btv;
                if (textViewNeedPadding) {
                    Context context = getContext();
                    int paddingLeftAndRight = (-mWGap + (int) context.getResources().getDimension(
                            R.dimen.textview_padding_in_hotseat)) / 2;
                    if (v instanceof BubbleTextView) {
                        btv = (BubbleTextView) v;
                        btv.setTempPadding(paddingLeftAndRight);
                    } else if (v instanceof FolderIcon) {
                        FolderIcon fi = (FolderIcon) v;
                        fi.setTempPadding(paddingLeftAndRight);
                    }
                } else {
                    if (v instanceof BubbleTextView) {
                        btv = (BubbleTextView) v;
                        btv.resetTempPadding();
                    } else if (v instanceof FolderIcon) {
                        ((FolderIcon) v).resetTempPadding();
                    }
                }
                /* YUNOS END */
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                        .getLayoutParams();
                lp.x = l + lp.leftMargin;
                //ItemInfo info = (ItemInfo)v.getTag();
                //Log.d(TAG, "sxsexe33---->reLayout i : " + i + " lp.x " + lp.x + " cellW " + cellW + " info " + info);
                l += (cellW + mWGap);
            }
        }
        if(count > 0) {
            mContent.getShortcutAndWidgetContainer().invalidate();
        }
    }

    private void reLayoutLandscape(int top, int bottom, boolean unvisibleCount) {
        ShortcutAndWidgetContainer container = mContent
                .getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        ArrayList<View> visibleChild = new ArrayList<View>();
        boolean flag = false;
        for (int i = 0; i < count; i++) {
            final View child = container.getChildAt(i);
            if (!mLauncher.getDragController().isDragging()
                    && child == mInvisibleView) {
                flag = true;
                continue;
            }
            if (!unvisibleCount && child.getVisibility() != GONE) {
                visibleChild.add(child);
            }
        }
        if (flag) {
            updateItemCell();
        }
        int visibleCount = visibleChild.size();
        if (visibleCount > 0) {
            int cellH = mContent.getCellHeight();
            mXOffset = 0;
            int t = 0;
            int workspaceCountY = ConfigManager.getCellCountY();
            mHGap = this.calculateGap(visibleCount);
            if (visibleCount < workspaceCountY) {
                t = mHGap;
            }
            for (int i = 0; i < visibleCount; i++) {
                View v = visibleChild.get(i);
                v.setTranslationY(0);
                v.setLeft(0);
                v.setRight(mContent.getCellWidth());
                BubbleTextView btv = null;
                if (v instanceof BubbleTextView) {
                    btv = (BubbleTextView) v;
                    btv.resetTempPadding();
                } else if (v instanceof FolderIcon) {
                    ((FolderIcon) v).resetTempPadding();
                }
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                        .getLayoutParams();
                lp.y = t + lp.topMargin;
                t += (cellH + mHGap);
            }
        }
        if (count > 0) {
            mContent.getShortcutAndWidgetContainer().invalidate();
        }
    }

    private int calculateGap(int count) {
        if (isStaticHotsaet()) {
            return calculateGapStatic(count);
        } else {
            return calculateGapDynamic(count);
        }
    }
//    private int getSwapIndex(int touchX, int touchY) {
//        int dockChildCount = mContent.getShortcutAndWidgetContainer().getChildCount();
//        int index = -1;
//        Log.d(TAG, "sxsexe33---> getSwapIndex bengin dockChildCount " + dockChildCount + " touchX " + touchX + " touchY " + touchY);
//        if(dockChildCount <= 1) {
//            return -1;
//        }
//        View child = null;
//        int location[] = new int[2];
//        int cellW = mContent.getCellWidth();
//        int cellH = mContent.getCellHeight();
//        Point p1 = new Point();
//        Point p2 = new Point();
//        Point p3 = new Point();
//        for(int i = 0; i < dockChildCount; i++) {
//            child = mContent.getShortcutAndWidgetContainer().getChildAt(i);
//            child.getLocationInWindow(location);
//            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
//            location[0] = lp.x;
//            p1.x = location[0] + cellW / 2;
//            p1.y = location[1];
//            p2.x = location[0];
//            p2.y = location[1] + cellH;
//            p3.x = location[0] + cellW;
//            p3.y = location[1] + cellH;
//
////            Log.d(TAG, "sxsexe22---->p1 " + p1 + " p2 " + p2 + " p3 " + p3 + " lp.x " + lp.x
////                    + " touchX " + touchX + " touchY " + touchY + " i " + i);
//            Log.d(TAG, "sxsexe33---->getSwapIndex goto checkInSwapHotArea i " + i);
//            if(checkInSwapHotArea(i, p1, p2, p3, touchX, touchY)) {
//                Log.d(TAG, "sxsexe22------------------->getSwapIndex goto checkInSwapHotArea return " + i);
//                return i;
//            }
//        }
//        Log.d(TAG, "sxsexe22---->getSwapIndex end return " + index);
//        return index;
//    }

//    private double triangleArea(Point a, Point b, Point c) {
//        double result = Math.abs((a.x * b.y + b.x * c.y + c.x * a.y - b.x * a.y
//                - c.x * b.y - a.x * c.y) / 2.0D);
//        return result;
//    }

//    private boolean checkInSwapHotArea(int i, Point p1, Point p2, Point p3, int touchX, int touchY) {
//        double areaBig = triangleArea(p1, p2, p3);
//        Point touchPoint = new Point(touchX, touchY);
//        double area1 = triangleArea(p1, p2, touchPoint);
//        double area2 = triangleArea(p1, p3, touchPoint);
//        double area3 = triangleArea(p2, p3, touchPoint);
//        Log.d(TAG, "sxsexe33--->checkInSwapHotArea areaBig " + areaBig  + " other three " + (area1 + area2 + area3) + " i " + i);
//        return areaBig == area1 + area2 + area3;
//    }

    private int calculateGapStatic(int count) {
        // TODO Auto-generated method stub
        if (LauncherApplication.isInLandOrientation()) {
            int cellH = mContent.getCellHeight();
            int height = getHeight() - getPaddingTop() - getPaddingBottom()
                    - mContent.getPaddingTop() - mContent.getPaddingBottom();
            int space = height - ConfigManager.getHotseatMaxCountY() * cellH;
            return (int)(space/(ConfigManager.getHotseatMaxCountY() - 1.0f));
        } else {
            int cellW = mContent.getCellWidth();
            int width = getWidth() - getPaddingLeft() - getPaddingRight()
                    - mContent.getPaddingLeft() - mContent.getPaddingRight();
            int space = width - ConfigManager.getHotseatMaxCountX() * cellW;
            return (int)(space/(ConfigManager.getHotseatMaxCountX() - 1.0f));
        }
    }

    private int calculateGapDynamic(int count) {
        int gap = 0;
        if (LauncherApplication.isInLandOrientation()) {
            int dropTargetHeight = getResources().getDimensionPixelSize(R.dimen.droptarget_height);
            int top = Math.max(dropTargetHeight, getTop());
            int height = getBottom() - top - getPaddingTop() - getPaddingBottom() - mContent.getPaddingBottom() - mContent.getPaddingTop();
            int verticalSpace = height - count * mContent.getCellHeight();
            if (count >= ConfigManager.getCellCountY()) {
                gap = verticalSpace / (count - 1);
            } else {
                gap = verticalSpace / (count + 1);
            }
        } else {
            int width = getRight() - getLeft() - getPaddingLeft() - getPaddingRight() - mContent.getPaddingLeft()
                    - mContent.getPaddingRight();
            int horizonSpace = width - count * mContent.getCellWidth();
            if (count >= ConfigManager.getCellCountX()) {
                gap = horizonSpace / (count - 1);
            } else {
                gap = horizonSpace / (count + 1);
            }
        }
        return gap;
    }

    /*YUNOS BEGIN*/
    //##date:2013/11/22 ##author:xiaodong.lxd
    //get appropriate cellx for the draged icon
    public int getAppropriateIndex(int dx, int dy) {
        //int dockChildCount = mContent.getShortcutAndWidgetContainer().getChildCount();
        if (isStaticHotsaet()) {
            return getAppropriateIndexStatic(dx, dy);
        }
        int dockChildCount = mViewCacheList.size();
        int index = 0;

        if (dockChildCount == 0) {
            return index;
        }
        int cellWidth = mContent.getCellWidth();
        int cellHeight = mContent.getCellHeight();
        if(dockChildCount == 1) {
            //View v = mContent.getShortcutAndWidgetContainer().getChildAt(0);
            View v = mViewCacheList.get(0);
            if(v == mInvisibleView) {
                return 0;
            }
            if (!(v.getLayoutParams() instanceof CellLayout.LayoutParams)) {
                return 0;
            }
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
            if(!LauncherApplication.isInLandOrientation()) {
                int centerX = mContent.getPaddingLeft() + lp.x + cellWidth / 2;
                return dx > centerX ? 1 : 0;
            } else {
                int centerY = lp.y + cellHeight / 2;
                return dy > centerY ? 1 : 0;
            }
        }

        int minResult = Integer.MAX_VALUE;
        int minIndex = 0;
        View child = null;
        int correctedX = 0;
        int correctedY = 0;
        CellLayout.LayoutParams lp = null;
        if (!LauncherApplication.isInLandOrientation()) {
            for (int i = 0; i < dockChildCount; i++) {
                // child =
                // mContent.getShortcutAndWidgetContainer().getChildAt(i);
                child = mViewCacheList.get(i);
                if (!(child.getLayoutParams() instanceof CellLayout.LayoutParams)) {
                    continue;
                }
                // info = (ItemInfo)child.getTag();
                lp = (CellLayout.LayoutParams) child.getLayoutParams();
                correctedX = getCorrectedX(lp.x, mMoveDireciton == 0);
                // Log.d(TAG, "sxsexe55------> getAppropriateIndex  i " + i +
                // " info "
                // + (info == null ? "null" : info.title) + " lp.x " + lp.x
                // + " correctedX " + correctedX + " touchX " + dx);
                if (minResult > Math.abs(correctedX - dx)) {
                    minResult = Math.abs(correctedX - dx);
                    if(mViewCacheList.contains(mInvisibleView) || correctedX >= dx) {
                        minIndex = i;
                    } else {
                        minIndex = i + 1;
                    }
                }
            }
        }else{
            for (int i = 0; i < dockChildCount; i++) {
                child = mViewCacheList.get(i);
                if (!(child.getLayoutParams() instanceof CellLayout.LayoutParams)) {
                    continue;
                }
                lp = (CellLayout.LayoutParams) child.getLayoutParams();
                correctedY = getCorrectedY(lp.y);
                if (minResult > Math.abs(correctedY - dy)) {
                    minResult = Math.abs(correctedY - dy);
                    if(mViewCacheList.contains(mInvisibleView) || correctedY >= dy) {
                        minIndex = i;
                    } else {
                        minIndex = i + 1;
                    }
                }
            }
        }
//        Log.d(TAG, "sxsexe55------> getAppropriateIndex return " + minIndex);
        return minIndex;
    }

    private int getAppropriateIndexStatic(int dx, int dy) {
        int cellWidth = mContent.getCellWidth();
        int cellHeight = mContent.getCellHeight();
        int widthGap = mContent.getWidthGap();
        int heightGap = mContent.getHeightGap();

        int minIndex, maxIndex;
        if (LauncherApplication.isInLandOrientation()) {
            minIndex = (int)((dy - mContent.getPaddingTop()) / (cellHeight + heightGap));
            maxIndex = ConfigManager.getHotseatMaxCountY() - 1;
        } else {
            minIndex = (int)((dx - mContent.getPaddingLeft()) / (cellWidth + widthGap));
            maxIndex = ConfigManager.getHotseatMaxCountX() - 1;
        }
        int atomRank = ConfigManager.getHotseatAtomRank();
        if (minIndex >= atomRank) minIndex--;
        if (minIndex > maxIndex) minIndex = maxIndex;
        if (minIndex < 0) minIndex = 0;
        return minIndex;
    }

    private void touchToPointStatic(int touchX, int touchY, int[] topLeft, boolean fromHotseat, boolean toHotseat) {
        if (mStaticLayoutModel == null) {
            return;
        }
        int size = mStaticLayoutModel.size();
        int foundIndex = 0;
        for (int i = 0; i < size; i++) {
            Pair<Integer, Integer> p = mStaticLayoutModel.get(i);
            if (touchX > p.first) {
                foundIndex = i;
                continue;
            }
            if (touchX < p.first) {
                break;
            }
        }

        Pair<Integer, Integer> p = mStaticLayoutModel.get(foundIndex);
        topLeft[0] = p.first + mContent.getPaddingLeft();
        topLeft[1] = p.second + mContent.getPaddingTop();
    }

    private void touchToPointDynamic(int touchX, int touchY, int[] topLeft, boolean fromHotseat, boolean toHotseat) {
        //int dockChildCount = mContent.getShortcutAndWidgetContainer().getChildCount();
        int dockChildCount = mViewCacheList.size();
        int cellWidth = mContent.getCellWidth();
        int cellHeight = mContent.getCellHeight();
        int minResult = Integer.MAX_VALUE;
        int minIndex = 0;
        View child = null;
        int centerX = 0;
        int centerY = 0;
        CellLayout.LayoutParams lp = null;
        if(!LauncherApplication.isInLandOrientation()) {
            for (int i = 0; i < dockChildCount; i++) {
                //child = mContent.getShortcutAndWidgetContainer().getChildAt(i);
                child = mViewCacheList.get(i);
                lp = (CellLayout.LayoutParams) child.getLayoutParams();
                centerX = lp.x + cellWidth / 2 + getLocationX() + mContent.getPaddingLeft();
                //Log.d(TAG, "sxsexe22-----> touchToPoint i " + i + " centerX " + centerX);
                if (minResult > Math.abs(centerX - touchX)) {
                    minResult = Math.abs(centerX - touchX);
                    minIndex = i;
                }
            }
        } else {
            for (int i = 0; i < dockChildCount; i++) {
                child = mViewCacheList.get(i);
                lp = (CellLayout.LayoutParams) child.getLayoutParams();
                centerY = lp.y + cellHeight / 2 + getLocationY() +mContent.getPaddingTop();
                //Log.d(TAG, "sxsexe22-----> touchToPoint i " + i + " centerX " + centerX);
                if (minResult > Math.abs(centerY - touchY)) {
                    minResult = Math.abs(centerY - touchY);
                    minIndex = i;
                }
            }
        }
        //child = mContent.getShortcutAndWidgetContainer().getChildAt(minIndex);
        child = mViewCacheList.get(minIndex);
        if(child != null) {//FIXME
            lp = (CellLayout.LayoutParams) child.getLayoutParams();
            // BugID:5238420:make dragoutline in the right position
            topLeft[0] = lp.x + mContent.getPaddingLeft();
            topLeft[1] = lp.y + mContent.getPaddingTop();
        }
        //Log.d(TAG, "sxsexe55-----> touchToPoint touchX " + touchX + " topLeft[0] " + topLeft[0] + " topLeft[1] " + topLeft[1]);
    }

    public void touchToPoint(int touchX, int touchY, int[] topLeft, boolean fromHotseat, boolean toHotseat) {
        if (isStaticHotsaet()) {
            touchToPointStatic(touchX, touchY, topLeft, fromHotseat, toHotseat);
        } else {
            touchToPointDynamic(touchX, touchY, topLeft, fromHotseat, toHotseat);
        }
    }

//    public int getAppropriateLeft(int dx, boolean fromHotseat) {
//        int index = getAppropriateIndex(dx);
//        View child = mContent.getShortcutAndWidgetContainer().getChildAt(index);
//        int left = 0;
//        if(child != null) {
//            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
//            left = lp.x;
//        } else {
//            left = 0;
//        }
//        Log.d(TAG, "sxsexe22-----> child " + child + " left " + left + " index " + index);
//        return left;
//    }

    protected boolean isFull() {
        if(mInvisibleViewAdded) {
            return false;
        }
        if (mViewCacheList.size() > ConfigManager.getHotseatMaxCountX()) {
            fillViewsFromCache();
        }
        if (LauncherApplication.isInLandOrientation()) {
            return mContent.getShortcutAndWidgetContainer().getChildCount() >= ConfigManager
                    .getHotseatMaxCountY();
        } else {
            return mContent.getShortcutAndWidgetContainer().getChildCount() >= ConfigManager
                    .getHotseatMaxCountX();
        }
    }

    /**
     * update hotseat items in database
     */
    public void updateItemInDatabase() {
        int count = mContent.getShortcutAndWidgetContainer().getChildCount();
        int container = Favorites.CONTAINER_HOTSEAT;

        for (int i = 0; i < count; i++) {
            View v = mContent.getShortcutAndWidgetContainer().getChildAt(i);
            ItemInfo info = (ItemInfo) v.getTag();
            // Null check required as the AllApps button doesn't have an item info
            if (info != null) {
                info.requiresDbUpdate = false;
                Log.d(TAG, "sxsexe------> updateItemInDatabase info " + info);
                LauncherModel.modifyItemInDatabase(mLauncher, info, container, info.screen, info.cellX,
                        info.cellY, info.spanX, info.spanY);
            }
        }
    }

    /**
     * update the screen and cellX of items in hotseat
     */
    public void updateItemCell() {
        //fillViewsFromCache();
        if (isStaticHotsaet()) {
            updateStaticItemCell();
            return;
        }
        ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        mViewCacheList.clear();

        //clear
        for (int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
//            Log.d(TAG, "sxsexe55------->updateItemCell111 i " + i + " v " + v + " tag " + (v == null ? " null " : v.getTag()));
            if(v != null && v.getTag() != null) {
                mViewCacheList.add(v);
            }
        }
        final boolean isLand = LauncherApplication.isInLandOrientation();
        count = mViewCacheList.size();
        for (int i = 0; i < count; i++) {
            View v = mViewCacheList.get(i);
            ItemInfo info = (ItemInfo) v.getTag();
            // Null check required as the AllApps button doesn't have an item info
//            Log.d(TAG, "sxsexe55------->updateItemCell info " + (info == null ? "null" : info.title) + " screen " + i);
            if (info == null) continue;
            if (isLand) {
                info.cellX = 0;
                info.cellY = i;
            } else {
                info.cellX = i;
                info.cellY = 0;
            }
            info.screen = i;
            info.container = Favorites.CONTAINER_HOTSEAT;
        }
        fillViewsFromCache();

    }
    /*YUNOS END*/

    public void updateStaticItemCell() {
        ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        mViewCacheList.clear();
        final boolean isLand = LauncherApplication.isInLandOrientation();

        List< View> views = new ArrayList<View>();
        for (int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
            if(v != null && v.getTag() != null) {
                mViewCacheList.add(v);
                ItemInfo info = (ItemInfo) v.getTag();
                if (info == null) continue;
                views.add(v);
            }
        }
        checkParams(views, isLand);
        for (int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
            if(v != null && v.getTag() != null) {
                ItemInfo info = (ItemInfo) v.getTag();
                if (info == null) continue;
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                        .getLayoutParams();
                if (isLand) {
                    info.cellX = 0;
                    info.screen = info.cellY =  lp.cellY;
                } else {
                    info.screen = info.cellX = lp.cellX;
                    info.cellY = 0;
                }
                info.container = Favorites.CONTAINER_HOTSEAT;
            }
        }
        fillViewsFromCache();
        return;
    }

//    protected Animator getHotseatAnimator(boolean hide) {
//        int startY = hide ? mAnimStartY : mAnimEndY;
//        int endY = hide ? mAnimEndY : mAnimStartY;
//        ValueAnimator bounceAnim = ObjectAnimator.ofFloat(this, "y",startY, endY);
//        bounceAnim.setDuration(getResources().getInteger(R.integer.config_workspaceUnshrinkTime));
//        bounceAnim.setInterpolator(new LinearInterpolator());
//        return bounceAnim;
//    }

    public void updateAtomView() {
        if (mAtomView == null) {
            mAtomView = AtomManager.getAtomManager().getRootView(getContext());
            if (mAtomView == null) return;
            /* YUNOS BEGIN */
            //## modules(Home Shell)
            //## date: 2015/12/28 ## author: wangye.wy
            //## BugID: 7704083: set atom width of xiao yun on hotseat
            int iconSize = getContext().getResources().getDimensionPixelSize(R.dimen.hotseat_atom_width);
            /* YUNOS END */
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(iconSize, iconSize);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            if (mAtomView.getParent() != null) {
                ((ViewGroup)mAtomView.getParent()).removeView(mAtomView);
            }
            addView(mAtomView, lp);
            AtomManager.getAtomManager().resumeAtomIcon();
        }
    }

    private void checkParams(
            List<View> views, boolean isLand) {
        int maxCount = isLand ? ConfigManager.getHotseatMaxCountY() : ConfigManager.getHotseatMaxCountX();
        View[] occupied = new View[maxCount];
        for (View v : views) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
            int index = isLand ? lp.cellY : lp.cellX;
            if (index > maxCount - 1) index = maxCount - 1;
            if (index < 0) index = 0;
            int newIndex = findEmptyCell(occupied, index);
            if (occupied[newIndex] == null) {
                occupied[newIndex] = v;
                if (isLand) {
                    lp.cellY = newIndex;
                } else {
                    lp.cellX = newIndex;
                }
            }
        }
    }

    protected Animator getHotseatAnimator(boolean hide) {
        ValueAnimator bounceAnim;
        if (LauncherApplication.isInLandOrientation()) {
            int startX = hide ? mAnimStartX : mAnimEndX;
            int endX = hide ? mAnimEndX : mAnimStartX;
            bounceAnim = ObjectAnimator.ofFloat(this, "x", startX, endX);
        } else {
            int startY = hide ? mAnimStartY : mAnimEndY;
            int endY = hide ? mAnimEndY : mAnimStartY;
            bounceAnim = ObjectAnimator.ofFloat(this, "y", startY, endY);
        }
        bounceAnim.setDuration(getResources().getInteger(R.integer.config_workspaceUnshrinkTime));
        bounceAnim.setInterpolator(new LinearInterpolator());
        return bounceAnim;
    }

    /* YUNOS BEGIN*/
    //bug 78446  LXD
//    protected void revisibleHotseat() {
//        setY(Math.min(mAnimStartY, mAnimEndY));
//    }
    /*YUNOS END*/

    /* YUNOS BEGIN*/
    //xiangke.xk 2015/09/06
    protected void revisibleHotseat() {
        if (LauncherApplication.isInLandOrientation()) {
            setX(Math.min(mAnimStartX, mAnimEndX));
        } else {
            setY(Math.min(mAnimStartY, mAnimEndY));
        }
    }
    /*YUNOS END*/
    public boolean isTouchInHotseat() {
        return mTouchInHotseat;
    }

    public void onPause() {
        onExitHotseat(false);
    }

    public ShortcutAndWidgetContainer getContainer() {
        return mContent.getShortcutAndWidgetContainer();
    }

    public CellLayout getCellLayout() {
        return mContent;
    }

    public boolean checkDragitem(View view) {
    ItemInfo info = (ItemInfo) view.getTag();
    return view == mDragedItemView
            || (mDragedItemView == null &&
                    info != null &&    //BugID:5694287
                    info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT);
    }

    private void relayouViewCacheList() {
        int visibleCount = mViewCacheList.size();
        if (visibleCount > 0) {
            int gap = this.calculateGap(visibleCount);
            if(!LauncherApplication.isInLandOrientation()) {
                int cellW = mContent.getCellWidth();
                int l = 0;
                int workspaceCountX = ConfigManager.getCellCountX();
                if(visibleCount < workspaceCountX) {
                    l += gap;
                }
                boolean rtl = mContent.getShortcutAndWidgetContainer().isLayoutRtl();
                for (int i = 0; i < visibleCount; i++) {
                    View v = mViewCacheList.get(!rtl ? i : visibleCount - i - 1);
                    if(v != mInvisibleView) {
                        l += (cellW + gap);
                        continue;
                    } else {
                        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                                .getLayoutParams();
                        if (isStaticHotsaet()) {
                               lp.cellX = i;
                               int atomRank = ConfigManager.getHotseatAtomRank();
                               lp.x = (lp.cellX >= atomRank ? lp.cellX + 1 : lp.cellX) * (cellW + gap);
                        } else {
                            lp.x = l + lp.leftMargin;
                            }
                        return;
                    }
                }
            } else {
                int cellH = mContent.getCellHeight();
                int t = 0;
                int workspaceCountY = ConfigManager.getCellCountY();
                if(visibleCount < workspaceCountY) {
                    t += gap;
                }
                for (int i = 0; i < visibleCount; i++) {
                    View v = mViewCacheList.get(i);
                    if(v != mInvisibleView) {
                        t += (cellH + gap);
                        continue;
                    } else {
                        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                                .getLayoutParams();
                        if (isStaticHotsaet()) {
                            lp.cellY = i;
                            int atomRank = ConfigManager.getHotseatAtomRank();
                            lp.y = (lp.cellY >= atomRank ? lp.cellY + 1 : lp.cellY) * (cellH + gap);
                         } else {
                            lp.y = t + lp.topMargin;
                         }
                        return;
                    }
                }
            }
        }
    }

    private int getLocationY() {
        int[] location = new int[2];
        mContent.getLocationOnScreen(location);
        return location[1];
    }

    private int getLocationX() {
        int[] location = new int[2];
        mContent.getLocationOnScreen(location);
        return location[0];
    }

    private void dumpViewCacheList() {/*
        for(View view : mViewCacheList) {
            ItemInfo info = (ItemInfo) view.getTag();
            Log.d(TAG, "sxsexe55---------> dumpViewCacheList " + (info == null ? " null " : info.title));
        }
        ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        for(int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
            ItemInfo info = (ItemInfo) v.getTag();
            Log.d(TAG, "sxsexe55---------> dumpContainer " + (info == null ? " null " : info.title));
        }
    */}

    public void removeViewByItemInfo(ItemInfo info) {
    ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
    int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
            ItemInfo info1 = (ItemInfo) v.getTag();
           // Log.d(TAG, "sxsexe---------->removeViewByItemInfo info " + info + " info1 " + info1 + " info == info1 " + (info == info1));
            if (info == info1) {
                container.removeViewAt(i);
                break;
            }
        }
    }

    public void initViewCacheList() {
        mViewCacheList.clear();
        ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        int count = container.getChildCount();
        for(int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
            //ItemInfo info = (ItemInfo) v.getTag();
            //Log.d(TAG, "sxsexe55-----> initViewCacheList add " + (info==null ? " null " : info));
            mViewCacheList.add(v);
        }
    }

    public void removeViewFromCacheList(View v) {
        mViewCacheList.remove(v);
    }

    private void addToCacheList(int index, View view) {
        if(index == -1 || index >= mViewCacheList.size()) {
           mViewCacheList.add(view);
        } else {
           mViewCacheList.add(index, view);
        }
        relayouViewCacheList();
    }

    private void fillViewsFromCache() {
        ShortcutAndWidgetContainer container = mContent.getShortcutAndWidgetContainer();
        container.removeAllViews();
        for(View view : mViewCacheList) {
//            ItemInfo info = (ItemInfo) view.getTag();
            /*YUNOS BEGIN lxd #142163*/
            if(view.getParent() != null) {
                ((ViewGroup)view.getParent()).removeView(view);
            }
            /*YUNOS END*/
            //Log.d(TAG, "sxsexe55---------> fillViewFromCache " + (info == null ? " null " : info.title));
            container.addView(view);
        }
    }
    /* YUNOS BEGIN */
    // ##date:2014/10/16 ##author:yangshan.ys##5157204
    // for 3*3 layout
    public void adjustToThreeLayout() {
        getLayoutParams().height = getResources()
                .getDimensionPixelSize(R.dimen.workspace_cell_height_3_3);
        int cellW = getResources().getDimensionPixelSize(R.dimen.bubble_icon_width_3_3);
        mContent.setGridSize(cellW, mContent.getCellHeight(), mContent.getWidthGap(), mContent.getHeightGap());
        mCellCountX = ConfigManager.getHotseatMaxCountX();
        mViewCacheList = new ArrayList<View>(mCellCountX);
        if (!LauncherApplication.isInLandOrientation()) {
            mContent.setGridSize(ConfigManager.getHotseatAtomRank() >  0 ?
                    mCellCountX + 1 : mCellCountX, mCellCountY);
        } else {
            mContent.setGridSize(mCellCountX, ConfigManager.getHotseatAtomRank() >  0 ?
                    mCellCountY + 1 : mCellCountY);
        }
        mContent.setMode(Mode.NORMAL);
        int childCount = mContent.getShortcutAndWidgetContainer().getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = mContent.getShortcutAndWidgetContainer().getChildAt(i);
            if (view instanceof BubbleTextView) {
                BubbleController.setMode((BubbleTextView) view, Mode.NORMAL);
            } else if (view instanceof FolderIcon) {
                ((FolderIcon) view).setHotseatMode(false);
            } else {
                Log.e(TAG, "hotseat error occured when adjust from three layout");
            }
        }
        getLayout().adjustToThreeLayout();
        this.setPadding(this.getPaddingLeft(),
                getResources().getDimensionPixelSize(R.dimen.hotseat_top_padding_3_3),
                this.getPaddingRight(), this.getPaddingBottom());
        mContent.getLayoutParams().height = mContent.getDesiredHeight();
    }

    public void adjustFromThreeLayout() {
        getLayoutParams().height = mLauncher.getResources()
                .getDimensionPixelSize(R.dimen.button_bar_height_plus_padding);
        int cellW = getResources().getDimensionPixelSize(R.dimen.hotseat_cell_width);
        mContent.setGridSize(cellW, mContent.getCellHeight(), mContent.getWidthGap(), mContent.getHeightGap());
        mCellCountX = ConfigManager.getHotseatMaxCountX();
        mViewCacheList = new ArrayList<View>(mCellCountX);
        if (!LauncherApplication.isInLandOrientation()) {
            mContent.setGridSize(ConfigManager.getHotseatAtomRank() >  0 ?
                    mCellCountX + 1 : mCellCountX, mCellCountY);
        } else {
            mContent.setGridSize(mCellCountX, ConfigManager.getHotseatAtomRank() >  0 ?
                    mCellCountY + 1 : mCellCountY);
        }
        mContent.setMode(Mode.HOTSEAT);
        int childCount = mContent.getShortcutAndWidgetContainer().getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = mContent.getShortcutAndWidgetContainer().getChildAt(i);
            if(view instanceof BubbleTextView) {
                BubbleController.setMode((BubbleTextView) view, Mode.HOTSEAT);
            } else if(view instanceof FolderIcon) {
                ((FolderIcon) view).setHotseatMode(true);
            } else {
                Log.e(TAG, "hotseat error occured when adjust from three layout");
            }
        }
        getLayout().adjustFromThreeLayout();
        this.setPadding(this.getPaddingLeft(), 0, this.getPaddingRight(), mLauncher.getResources()
                .getDimensionPixelSize(R.dimen.button_bar_height_bottom_padding));
        mContent.getLayoutParams().height = LayoutParams.MATCH_PARENT;
    }
    /*YUNOS END*/
    public boolean isStaticHotsaet() {
        return mStaticHotSeat;
    }

    public void preHideHotseat() {
        if (isStaticHotsaet()) {
            Log.d(TAG, "sxsexe_dock ---------------- prehide");
            AtomManager.getAtomManager().pauseAtomIcon(false);
        }
    }

    public void afterShowHotseat() {
        if (isStaticHotsaet()) {
            /* YUNOS BEGIN */
            //##modules: homeshell ##BugID:7905144 7937582
            //##date:2016/02/24 ##author:xiangnan.xn
            // force pauseAtomIcon when Folder is open or Hideseat is Showing or FlipAnimation is Showing
            if (mLauncher.mFolderUtils.isFolderOpened()) {
                Log.d(TAG, "sxsexe_dock ---------------- aftershow FolderOpen");
                AtomManager.getAtomManager().pauseAtomIcon(false);
            } else if (mLauncher.isHideseatShowing()) {
                Log.d(TAG, "sxsexe_dock ---------------- aftershow HideseatShowing");
                AtomManager.getAtomManager().pauseAtomIcon(false);
            } else if (mLauncher.isFlipAnimationShowing()) {
                Log.d(TAG, "sxsexe_dock ---------------- aftershow FlipAnimationShowing");
                AtomManager.getAtomManager().pauseAtomIcon(false);
            } else {
                Log.d(TAG, "sxsexe_dock ---------------- aftershow ResumeAtomIcon");
                AtomManager.getAtomManager().resumeAtomIcon();
            }
            /* YUNOS END */
        }
    }

    public void setAtomIconClickable(boolean clickable) {
        if (mAtomView != null) {
            mAtomView.setClickable(clickable);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mStaticHotSeat) {
            if (visibility == View.VISIBLE) {
                AtomManager.getAtomManager().resumeAtomIcon();
            } else {
                AtomManager.getAtomManager().pauseAtomIcon(false);
            }
        }
    }

    /* YUNOS BEGIN */
    //## modules(Home Shell)
    //## date: 2016/01/05 ## author: wangye.wy
    //## BugID: 7756096: set visibility with atom
    public void setVisibility(int visibility, boolean atom) {
        super.setVisibility(visibility);
        if (mStaticHotSeat && atom) {
            if (visibility == View.VISIBLE && !mLauncher.isInLauncherEditMode()) {
                AtomManager.getAtomManager().resumeAtomIcon();
            } else {
                AtomManager.getAtomManager().pauseAtomIcon(false);
            }
        }
    }
    /* YUNOS END */
}
