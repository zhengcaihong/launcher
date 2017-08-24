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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.aliyun.homeshell.R;
import com.aliyun.homeshell.DropTarget.DragObject;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.editmode.HorzontalPreviewList;
import com.aliyun.utility.FeatureUtility;
import com.aliyun.homeshell.icon.BubbleResources;

import java.util.ArrayList;

/**
 * Class for initiating a drag within a view or across multiple views.
 */
public class DragController {
    private static final String TAG = "Launcher.DragController";

    /* YUNOS BEGIN */
    // ##date:2014/4/21 ##author:hongchao.ghc ##BugID:111144
    /** Indicates the drag is a move.  */
    public static final int DRAG_ACTION_MOVE = 0;

    /** Indicates the drag is a copy.  */
    public static final int DRAG_ACTION_COPY = 1;
    /* YUNOS END */
    private static final int SCROLL_DELAY = 500;
    private static final int RESCROLL_DELAY = 700;
    //BugID:6055840:15ms is too short to vibrate in X2 and A800
    private static final int VIBRATE_DURATION = 40;

    private static final boolean PROFILE_DRAWING_DURING_DRAG = false;

    private static final int SCROLL_OUTSIDE_ZONE = 0;
    private static final int SCROLL_WAITING_IN_ZONE = 1;

    static final int SCROLL_NONE = -1;
    static final int SCROLL_LEFT = 0;
    static final int SCROLL_RIGHT = 1;
    static final int SCROLL_UP = 2;
    static final int SCROLL_DOWN = 3;

    private static final int PREV = 0;
    private static final int NEXT = 1;

    private static final float MAX_FLING_DEGREES = 35f;

    private Launcher mLauncher;
    private Handler mHandler;
    private final Vibrator mVibrator;

    // temporaries to avoid gc thrash
    private Rect mRectTemp = new Rect();
    private final int[] mCoordinatesTemp = new int[2];

    /** Whether or not we're dragging. */
    private boolean mDragging;

    private boolean mDraggingScreen = false;
    private boolean mDragScreenWaiting = false;

    /** X coordinate of the down event. */
    private int mMotionDownX;

    /** Y coordinate of the down event. */
    private int mMotionDownY;

    /** the area at the edge of the screen that makes the workspace go left
     *   or right while you're dragging.
     */
    private int mScrollZone;

    private DropTarget.DragObject mDragObject;
    private CellLayout mCurrentPage = null;
    private int mDragLayerY = 0;

    /** Who can receive drop events */
    private ArrayList<DropTarget> mDropTargets = new ArrayList<DropTarget>();
    private ArrayList<DragListener> mListeners = new ArrayList<DragListener>();
    private DropTarget mFlingToDeleteDropTarget;

    /** The window token used as the parent for the DragView. */
    private IBinder mWindowToken;

    /** The view that will be scrolled when dragging to the left and right edges of the screen. */
    private View mScrollView;

    private View mMoveTarget;

    private DragScroller mDragScroller;
    private int mScrollState = SCROLL_OUTSIDE_ZONE;
    private ScrollRunnable mScrollRunnable = new ScrollRunnable();
    private DragScreenRunnable mDragScreenRunnable = new DragScreenRunnable();
    private AnimatorSet mAnimatorSet = null;

    private DropTarget mLastDropTarget;

    private InputMethodManager mInputMethodManager;

    private int mLastTouch[] = new int[2];
    private long mLastTouchUpTime = -1;
    private int mDistanceSinceScroll = 0;

    private int mTmpPoint[] = new int[2];
    private Rect mDragLayerRect = new Rect();

    protected int mFlingToDeleteThresholdVelocity;
    protected int mFlingToMoveThresholdVelocity;
    private VelocityTracker mVelocityTracker;

    private static Paint mFadingPaint = new Paint();
    static {
        mFadingPaint.setColorFilter(BubbleResources.makeFadingEffectColorFilter());
    }

    /**
     * Interface to receive notifications when a drag starts or stops
     */
    public interface DragListener {
        
        /**
         * A drag has begun
         * 
         * @param source An object representing where the drag originated
         * @param info The data associated with the object that is being dragged
         * @param dragAction The drag action: either {@link DragController#DRAG_ACTION_MOVE}
         *        or {@link DragController#DRAG_ACTION_COPY}
         */
        void onDragStart(DragSource source, Object info, int dragAction);
        
        /**
         * The drag has ended
         */
        void onDragEnd();
    }
    
    /**
     * Used to create a new DragLayer from XML.
     *
     * @param context The application's context.
     */
    public DragController(Launcher launcher) {
        Resources r = launcher.getResources();
        mLauncher = launcher;
        mHandler = new Handler();
        mScrollZone = r.getDimensionPixelSize(R.dimen.scroll_zone);
        mVelocityTracker = VelocityTracker.obtain();
        mVibrator = (Vibrator) launcher.getSystemService(Context.VIBRATOR_SERVICE);

        float density = r.getDisplayMetrics().density;
        mFlingToDeleteThresholdVelocity =
                (int) (r.getInteger(R.integer.config_flingToDeleteMinVelocity) * density);
        mFlingToMoveThresholdVelocity =
                (int) (r.getInteger(R.integer.config_flingToMoveMinVelocity) * density);
    }

    public boolean dragging() {
        return mDragging;
    }

    /**
     * Starts a drag.
     *
     * @param v The view that is being dragged
     * @param bmp The bitmap that represents the view being dragged
     * @param source An object representing where the drag originated
     * @param dragInfo The data associated with the object that is being dragged
     * @param dragAction The drag action: either {@link #DRAG_ACTION_MOVE} or
     *        {@link #DRAG_ACTION_COPY}
     * @param dragRegion Coordinates within the bitmap b for the position of item being dragged.
     *          Makes dragging feel more precise, e.g. you can clip out a transparent border
     */
    public void startDrag(View v, Bitmap bmp, DragSource source, Object dragInfo, int dragAction,
            Point extraPadding, float initialDragViewScale) {
        int[] loc = mCoordinatesTemp;
        mLauncher.getDragLayer().getLocationInDragLayer(v, loc);
        int viewExtraPaddingLeft = extraPadding != null ? extraPadding.x : 0;
        int viewExtraPaddingTop = extraPadding != null ? extraPadding.y : 0;
        int dragLayerX = loc[0] + v.getPaddingLeft() + viewExtraPaddingLeft +
                (int) ((initialDragViewScale * bmp.getWidth() - bmp.getWidth()) / 2);
        int dragLayerY = loc[1] + v.getPaddingTop() + viewExtraPaddingTop +
                (int) ((initialDragViewScale * bmp.getHeight() - bmp.getHeight()) / 2);

        startDrag(bmp, dragLayerX, dragLayerY, source, dragInfo, dragAction, null,
                null, initialDragViewScale);

        if (dragAction == DRAG_ACTION_MOVE) {
            v.setVisibility(View.GONE);
        }
    }

    /**
     * Starts a drag.
     *
     * @param b The bitmap to display as the drag image.  It will be re-scaled to the
     *          enlarged size.
     * @param dragLayerX The x position in the DragLayer of the left-top of the bitmap.
     * @param dragLayerY The y position in the DragLayer of the left-top of the bitmap.
     * @param source An object representing where the drag originated
     * @param dragInfo The data associated with the object that is being dragged
     * @param dragAction The drag action: either {@link #DRAG_ACTION_MOVE} or
     *        {@link #DRAG_ACTION_COPY}
     * @param dragRegion Coordinates within the bitmap b for the position of item being dragged.
     *          Makes dragging feel more precise, e.g. you can clip out a transparent border
     */
    public void startDrag(Bitmap b, int dragLayerX, int dragLayerY,
            DragSource source, Object dragInfo, int dragAction, Point dragOffset, Rect dragRegion,
            float initialDragViewScale) {
        if (PROFILE_DRAWING_DURING_DRAG) {
            android.os.Debug.startMethodTracing("Launcher");
        }

        // Hide soft keyboard, if visible
        if (mInputMethodManager == null) {
            mInputMethodManager = (InputMethodManager)
                    mLauncher.getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        mInputMethodManager.hideSoftInputFromWindow(mWindowToken, 0);

        for (DragListener listener : mListeners) {
            listener.onDragStart(source, dragInfo, dragAction);
        }

        final int registrationX = mMotionDownX - dragLayerX;
        final int registrationY = mMotionDownY - dragLayerY;

        final int dragRegionLeft = dragRegion == null ? 0 : dragRegion.left;
        final int dragRegionTop = dragRegion == null ? 0 : dragRegion.top;

        mDragging = true;
        mLauncher.getModel().getPackageUpdateTaskQueue().retainLock("DragController");

        if ( mDragObject != null && mDragObject.dragView != null){
            mDragObject.dragView.remove();
        }
        mDragObject = new DropTarget.DragObject();

        mDragObject.dragComplete = false;
        mDragObject.xOffset = mMotionDownX - (dragLayerX + dragRegionLeft);
        mDragObject.yOffset = mMotionDownY - (dragLayerY + dragRegionTop);
        mDragObject.dragSource = source;
        mDragObject.dragInfo = dragInfo;

        mVibrator.vibrate(VIBRATE_DURATION);

        final DragView dragView = mDragObject.dragView = new DragView(mLauncher, b, registrationX,
                registrationY, 0, 0, b.getWidth(), b.getHeight(), initialDragViewScale, dragInfo == null);

        /* YUNOS BEGIN */
        // ##date:2014/11/14 ##author:zhanggong.zg ##BugID:5581508
        // apply fading effect to hide-seat icon during dragging
        if (dragInfo instanceof ItemInfo &&
            ((ItemInfo) dragInfo).container == Favorites.CONTAINER_HIDESEAT) {
            dragView.setLayerType(View.LAYER_TYPE_HARDWARE, mFadingPaint );
        }
        /* YUNOS END */

        if (dragOffset != null) {
            dragView.setDragVisualizeOffset(new Point(dragOffset));
        }
        if (dragRegion != null) {
            dragView.setDragRegion(new Rect(dragRegion));
        }

        dragView.show(mMotionDownX, mMotionDownY);
        handleMoveEvent(mMotionDownX, mMotionDownY);
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: dragging screen
        if (dragInfo == null) {
            Workspace workspace = mLauncher.getWorkspace();
            mCurrentPage = (CellLayout)workspace.getChildAt(workspace.getCurrentPage());
            mDragLayerY = mMotionDownY;
        }
        /* YUNOS END */
    }

    /**
     * Draw the view into a bitmap.
     */
    Bitmap getViewBitmap(View v) {
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);
        float alpha = v.getAlpha();
        v.setAlpha(1.0f);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            Log.e(TAG, "failed getViewBitmap(" + v + ")", new RuntimeException());
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

        // Restore the view
        v.destroyDrawingCache();
        v.setAlpha(alpha);
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        return bitmap;
    }

    /**
     * Call this from a drag source view like this:
     *
     * <pre>
     *  @Override
     *  public boolean dispatchKeyEvent(KeyEvent event) {
     *      return mDragController.dispatchKeyEvent(this, event)
     *              || super.dispatchKeyEvent(event);
     * </pre>
     */
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mDragging;
    }

    public boolean isDragging() {
        return mDragging;
    }

    /**
     * Stop dragging without dropping.
     */
    public void cancelDrag() {
        if (mDragging) {
            if (mLastDropTarget != null) {
                mLastDropTarget.onDragExit(mDragObject);
            }
            mDragObject.deferDragViewCleanupPostAnimation = false;
            mDragObject.cancelled = true;
            mDragObject.dragComplete = true;
            mDragObject.dragSource.onDropCompleted(null, mDragObject, false, false);
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: dragging screen
            if (mCurrentPage != null) {
                mCurrentPage.setVisibility(View.VISIBLE);
            }
            /* YUNOS END */
        }
        endDrag();
    }
    public void onAppsRemoved(ArrayList<ApplicationInfo> appInfos, Context context) {
        // Cancel the current drag if we are removing an app that we are dragging
        if (mDragObject != null) {
            Object rawDragInfo = mDragObject.dragInfo;
            if (rawDragInfo instanceof ShortcutInfo) {
                ShortcutInfo dragInfo = (ShortcutInfo) rawDragInfo;
                for (ApplicationInfo info : appInfos) {
                    // Added null checks to prevent NPE we've seen in the wild
                    if (dragInfo != null &&
                        dragInfo.intent != null) {
                        /* YUNOS BEGIN */
                        //##date:2013/11/25 ##author:hongxing.whx ##bugid: 66146
                        ComponentName comp = dragInfo.intent.getComponent();
                        if (comp != null) {
                            boolean isSameComponent =
                                    comp.equals(info.componentName);
                            if (isSameComponent) {
                                cancelDrag();
                                return;
                            }
                        }
                        /* YUNOS END */
                    }
                }
            }
        }
    }

    private void endDrag() {
        //Log.d(TAG,"sxsexe-------->endDrag mDragging = " + mDragging);
        if (mDragging) {
            mDragging = false;
            mLauncher.getModel().getPackageUpdateTaskQueue().releaseLock();
            clearScrollRunnable();
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: dragging screen
            clearDragScreenRunnable();
            mCurrentPage = null;
            /* YUNOS END */
            boolean isDeferred = false;
            if (mDragObject.dragView != null) {
                isDeferred = mDragObject.deferDragViewCleanupPostAnimation;
                if (!isDeferred) {
                    Log.d(TAG, "sxsexe------->remove dragview " + mDragObject.dragInfo);
                    mDragObject.dragView.remove();
                }
                mDragObject.dragView = null;
            }

            // Only end the drag if we are not deferred
            if (!isDeferred) {
                for (DragListener listener : mListeners) {
                    listener.onDragEnd();
                }
            }
        }

        releaseVelocityTracker();
        /*YUNOS BEGIN added by xiaodong.lxd for push to talk*/
        if(CheckVoiceCommandPressHelper.isPushTalkCanUse()) {
            CheckVoiceCommandPressHelper.getInstance().cancelCheckedVoiceCommand();
        }
        /*YUNOS END*/
    }

    /**
     * This only gets called as a result of drag view cleanup being deferred in endDrag();
     */
    public void onDeferredEndDrag(DragView dragView) {
        dragView.remove();

        // If we skipped calling onDragEnd() before, do it now
        for (DragListener listener : mListeners) {
            listener.onDragEnd();
        }
    }

    void onDeferredEndFling(DropTarget.DragObject d) {
        d.dragSource.onFlingToDeleteCompleted();
    }

    /**
     * Clamps the position to the drag layer bounds.
     */
    private int[] getClampedDragLayerPos(float x, float y) {
        mLauncher.getDragLayer().getLocalVisibleRect(mDragLayerRect);
        mTmpPoint[0] = (int) Math.max(mDragLayerRect.left, Math.min(x, mDragLayerRect.right - 1));
        mTmpPoint[1] = (int) Math.max(mDragLayerRect.top, Math.min(y, mDragLayerRect.bottom - 1));
        return mTmpPoint;
    }

    long getLastGestureUpTime() {
        if (mDragging) {
            return System.currentTimeMillis();
        } else {
            return mLastTouchUpTime;
        }
    }

    void resetLastGestureUpTime() {
        mLastTouchUpTime = -1;
    }

    /**
     * Call this from a drag source view.
     */
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        @SuppressWarnings("all") // suppress dead code warning
        final boolean debug = false;
        if (debug) {
            Log.d(Launcher.TAG, "DragController.onInterceptTouchEvent " + ev + " mDragging="
                    + mDragging);
        }

        // Update the velocity tracker
        acquireVelocityTrackerAndAddMovement(ev);

        final int action = ev.getAction();
        final int[] dragLayerPos = getClampedDragLayerPos(ev.getX(), ev.getY());
        final int dragLayerX = dragLayerPos[0];
        final int dragLayerY = dragLayerPos[1];

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                mMotionDownX = dragLayerX;
                mMotionDownY = dragLayerY;
                mLastDropTarget = null;
                break;
            case MotionEvent.ACTION_UP:
                mLastTouchUpTime = System.currentTimeMillis();
                if (mDragging) {
                    if(isSupportFlingToMove(mDragObject)) {
                        PointF vec = new PointF();
                        switch (getFlingingAction(mDragObject.dragSource, vec)) {
                            case FLING_ACTION_PREV:
                                dropOnFlingToMove(dragLayerX, dragLayerY, vec, false);
                                break;
                            case FLING_ACTION_NEXT:
                                dropOnFlingToMove(dragLayerX, dragLayerY, vec, true);
                                break;
                            case FLING_ACTION_DELETE:
                                dropOnFlingToDeleteTarget(dragLayerX, dragLayerY, vec);
                                break;
                            case FLING_ACTION_NONE:
                            default:
                                drop(dragLayerX, dragLayerY);
                                break;
                        }
                    } else {
                        PointF vec = isFlingingToDelete(mDragObject.dragSource);
                        if (vec != null && mDragObject.dragInfo != null) {
                            dropOnFlingToDeleteTarget(dragLayerX, dragLayerY, vec);
                        } else {
                            drop(dragLayerX, dragLayerY);
                        }
                    }
                }else{
                    mLauncher.getWorkspace().reverseGatherAmin();
                }
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelDrag();
                break;
        }

        return mDragging;
    }

    /**
     * Sets the view that should handle move events.
     */
    public void setMoveTarget(View view) {
        mMoveTarget = view;
    }    

    public boolean dispatchUnhandledMove(View focused, int direction) {
        return mMoveTarget != null && mMoveTarget.dispatchUnhandledMove(focused, direction);
    }

    private void clearScrollRunnable() {
        mHandler.removeCallbacks(mScrollRunnable);
        if (mScrollState == SCROLL_WAITING_IN_ZONE) {
            mScrollState = SCROLL_OUTSIDE_ZONE;
            if (LauncherApplication.isInLandOrientation()) {
                mScrollRunnable.setDirection(SCROLL_DOWN);
            } else {
                mScrollRunnable.setDirection(SCROLL_RIGHT);
            }
            mDragScroller.onExitScrollArea();
            mLauncher.getDragLayer().onExitScrollArea();
        }
    }

    public void clearDragScreenRunnable() {
        mHandler.removeCallbacks(mDragScreenRunnable);
        mAnimatorSet = null;
        mDraggingScreen = false;
        mDragScreenWaiting = false;
    }

    private void handleMoveEvent(int x, int y) {
        mDragObject.dragView.move(x, y);

        // Drop on someone?
        final int[] coordinates = mCoordinatesTemp;
        DropTarget dropTarget = findDropTarget(x, y, coordinates);
        mDragObject.x = coordinates[0];
        mDragObject.y = coordinates[1];
        checkTouchMove(dropTarget);

        if (mDragObject.dragInfo != null) {
            // Check if we are hovering over the scroll areas
            mDistanceSinceScroll +=
                Math.sqrt(Math.pow(mLastTouch[0] - x, 2) + Math.pow(mLastTouch[1] - y, 2));
            mLastTouch[0] = x;
            mLastTouch[1] = y;
            checkScrollState(x, y);
        }

        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: dragging screen
        if (mDragObject.dragInfo == null) {
            checkDragScreen();
        }
        /* YUNOS END */
    }

    public void checkDragScreen() {
        if (mDraggingScreen) {
            return;
        }
        int[] tempXY = new int[2];
        mLauncher.getDragLayer().getLocationInDragLayer(mDragObject.dragView, tempXY);
        float width = mDragObject.dragView.getWidth() * Workspace.sEditScale * DragView.SCREEN_SCALE;
        Workspace workspace = mLauncher.getWorkspace();
        int currentPage = workspace.getCurrentPage();
        final CellLayout current = (CellLayout)workspace.getChildAt(currentPage);
        if (mCurrentPage != null && tempXY[0] + (width * 0.25f) < 0 && currentPage > 0) {
            final CellLayout prev = (CellLayout)workspace.getChildAt(currentPage - 1);
            if (prev != null) {
                mDraggingScreen = true;
                mAnimatorSet = new AnimatorSet();
                Animator animator;
                float distance = current.getLeft() - prev.getLeft();
                float origin = prev.getWidth() * (1.0f - Workspace.sEditScale) / 2;
                final float translationX = prev.getTranslationX();
                animator = ObjectAnimator.ofFloat(prev, "translationX", origin, translationX + distance * 2);
                mAnimatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        prev.setTranslationX(translationX);
                        exchangeScreen(PREV, prev, current);
                        mHandler.postDelayed(mDragScreenRunnable, 500);
                        mDragScreenWaiting = true;
                    }
                });
                mAnimatorSet.setDuration(500);
                mAnimatorSet.play(animator);
                mAnimatorSet.start();
            }
        } else if (mCurrentPage != null && tempXY[0] + (width * 0.75f) > LauncherApplication.getScreenWidth() &&
                currentPage < workspace.getChildCount() - 1) {
            final CellLayout next = (CellLayout)workspace.getChildAt(currentPage + 1);
            if (next != null && !next.isFakeChild()) {
                mDraggingScreen = true;
                mAnimatorSet = new AnimatorSet();
                Animator animator;
                float distance = next.getLeft() - current.getLeft();
                float origin = next.getWidth() * (1.0f - Workspace.sEditScale) / 2;
                final float translationX = next.getTranslationX();
                animator = ObjectAnimator.ofFloat(next, "translationX", origin, translationX - distance * 2);
                mAnimatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        next.setTranslationX(translationX);
                        exchangeScreen(NEXT, current, next);
                        mHandler.postDelayed(mDragScreenRunnable, 500);
                        mDragScreenWaiting = true;
                    }
                });
                mAnimatorSet.setDuration(500);
                mAnimatorSet.play(animator);
                mAnimatorSet.start();
            }
        } else {
            clearDragScreenRunnable();
        }
    }

    public void forceTouchMove() {
        int[] dummyCoordinates = mCoordinatesTemp;
        DropTarget dropTarget = findDropTarget(mLastTouch[0], mLastTouch[1], dummyCoordinates);
        checkTouchMove(dropTarget);
    }

    private void checkTouchMove(DropTarget dropTarget) {
        if (dropTarget != null) {
            DropTarget delegate = dropTarget.getDropTargetDelegate(mDragObject);
            if (delegate != null) {
                dropTarget = delegate;
            }

            if (mLastDropTarget != dropTarget) {
                if (mLastDropTarget != null) {
                    mLastDropTarget.onDragExit(mDragObject);
                }
                dropTarget.onDragEnter(mDragObject);
            }
            dropTarget.onDragOver(mDragObject);
        } else {
            if (mLastDropTarget != null) {
                mLastDropTarget.onDragExit(mDragObject);
            }
        }
        mLastDropTarget = dropTarget;
    }

    private void checkScrollState(int x, int y) {
        final int slop = ViewConfiguration.get(mLauncher).getScaledWindowTouchSlop();
        final int delay = mDistanceSinceScroll < slop ? RESCROLL_DELAY : SCROLL_DELAY;
        final DragLayer dragLayer = mLauncher.getDragLayer();
        final boolean isRtl = (dragLayer.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        final int forwardDirection = isRtl ? SCROLL_RIGHT : SCROLL_LEFT;
        final int backwardsDirection = isRtl ? SCROLL_LEFT : SCROLL_RIGHT;
        final int upDirection = SCROLL_UP;
        final int downDirection = SCROLL_DOWN;
        if (LauncherApplication.isInLandOrientation()) {
            if (y < mScrollZone) {
                if (mScrollState == SCROLL_OUTSIDE_ZONE) {
                    mScrollState = SCROLL_WAITING_IN_ZONE;
                    if (mDragScroller.onEnterScrollArea(x, y, upDirection)) {
                        dragLayer.onEnterScrollArea(upDirection);
                        mScrollRunnable.setDirection(upDirection);
                        mHandler.postDelayed(mScrollRunnable, delay);
                    }
                }
            } else if (y > mScrollView.getHeight() - mScrollZone) {
                if (mScrollState == SCROLL_OUTSIDE_ZONE) {
                    mScrollState = SCROLL_WAITING_IN_ZONE;
                    if (mDragScroller.onEnterScrollArea(x, y, downDirection)) {
                        dragLayer.onEnterScrollArea(downDirection);
                        mScrollRunnable.setDirection(downDirection);
                        mHandler.postDelayed(mScrollRunnable, delay);
                    }
                }
            } else {
                clearScrollRunnable();
            }
        } else {
            if (x < mScrollZone) {
                if (mScrollState == SCROLL_OUTSIDE_ZONE) {
                    mScrollState = SCROLL_WAITING_IN_ZONE;
                    if (mDragScroller.onEnterScrollArea(x, y, forwardDirection)) {
                        dragLayer.onEnterScrollArea(forwardDirection);
                        mScrollRunnable.setDirection(forwardDirection);
                        mHandler.postDelayed(mScrollRunnable, delay);
                    }
                }
            } else if (x > mScrollView.getWidth() - mScrollZone) {
                if (mScrollState == SCROLL_OUTSIDE_ZONE) {
                    mScrollState = SCROLL_WAITING_IN_ZONE;
                    if (mDragScroller.onEnterScrollArea(x, y, backwardsDirection)) {
                        dragLayer.onEnterScrollArea(backwardsDirection);
                        mScrollRunnable.setDirection(backwardsDirection);
                        mHandler.postDelayed(mScrollRunnable, delay);
                    }
                }
            } else {
                clearScrollRunnable();
            }

        }
    }

    boolean mIsFolder;
    int mVerticalScrollZone = -1;
    int mVerticalScrollZoneDown;
    private boolean isSupportFlingToMove(DragObject d) {
        if (!FeatureUtility.hasFlingIconFeature()) {
            return false;
        }
        if(mLauncher.isInLauncherEditMode()) {
            return false;
        }
        return !mLauncher.getWorkspace().isMultiSelectDragging() && d != null && (d.dragInfo instanceof ShortcutInfo || d.dragInfo instanceof FolderInfo)
                && ((ItemInfo)d.dragInfo).container == LauncherSettings.Favorites.CONTAINER_DESKTOP;
    }

    /**
     * Call this from a drag source view.
     */
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mDragging) {
            return false;
        }

        // Update the velocity tracker
        acquireVelocityTrackerAndAddMovement(ev);

        final int action = ev.getAction();
        final int[] dragLayerPos = getClampedDragLayerPos(ev.getX(), ev.getY());
        final int dragLayerX = dragLayerPos[0];
        final int dragLayerY = dragLayerPos[1];

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            // Remember where the motion event started
            mMotionDownX = dragLayerX;
            mMotionDownY = dragLayerY;

            if ((dragLayerX < mScrollZone) || (dragLayerX > mScrollView.getWidth() - mScrollZone)) {
                mScrollState = SCROLL_WAITING_IN_ZONE;
                mHandler.postDelayed(mScrollRunnable, SCROLL_DELAY);
            } else {
                mScrollState = SCROLL_OUTSIDE_ZONE;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: dragging screen
            if (mDragObject.dragInfo == null) {
                handleMoveEvent(dragLayerX, mDragLayerY);
            /* YUNOS END */
            } else {
                handleMoveEvent(dragLayerX, dragLayerY);
            }
            break;
        case MotionEvent.ACTION_UP:
            // Ensure that we've processed a move event at the current pointer location.
            if (mDragObject.dragInfo == null) {
                handleMoveEvent(dragLayerX, mDragLayerY);
            } else {
                handleMoveEvent(dragLayerX, dragLayerY);
            }
            mHandler.removeCallbacks(mScrollRunnable);

            if (mDragging) {
                if(isSupportFlingToMove(mDragObject)) {
                    PointF vec = new PointF();
                    switch (getFlingingAction(mDragObject.dragSource, vec)) {
                        case FLING_ACTION_PREV:
                            dropOnFlingToMove(dragLayerX, dragLayerY, vec, false);
                            break;
                        case FLING_ACTION_NEXT:
                            dropOnFlingToMove(dragLayerX, dragLayerY, vec, true);
                            break;
                        case FLING_ACTION_DELETE:
                            dropOnFlingToDeleteTarget(dragLayerX, dragLayerY, vec);
                            break;
                        case FLING_ACTION_NONE:
                        default:
                            drop(dragLayerX, dragLayerY);
                            break;
                    }
                } else {
                    PointF vec = isFlingingToDelete(mDragObject.dragSource);
                    if (vec != null && mDragObject.dragInfo != null) {
                        dropOnFlingToDeleteTarget(dragLayerX, dragLayerY, vec);
                    } else {
                        drop(dragLayerX, dragLayerY);
                    }
                }
            }
            endDrag();
            break;
        case MotionEvent.ACTION_CANCEL:
            mHandler.removeCallbacks(mScrollRunnable);
            cancelDrag();
            break;
        }

        return true;
    }

    /**
     * Determines whether the user flung the current item to delete it.
     *
     * @return the vector at which the item was flung, or null if no fling was detected.
     */
    private PointF isFlingingToDelete(DragSource source) {
        if (!FeatureUtility.hasFlingIconFeature()) return null;
        if (mFlingToDeleteDropTarget == null) return null;
        if (!source.supportsFlingToDelete()) return null;

        ViewConfiguration config = ViewConfiguration.get(mLauncher);
        mVelocityTracker.computeCurrentVelocity(1000, config.getScaledMaximumFlingVelocity());

        if (mVelocityTracker.getYVelocity() < mFlingToDeleteThresholdVelocity) {
            // Do a quick dot product test to ensure that we are flinging upwards
            PointF vel = new PointF(mVelocityTracker.getXVelocity(),
                    mVelocityTracker.getYVelocity());
            PointF upVec = new PointF(0f, -1f);
            float theta = (float) Math.acos(((vel.x * upVec.x) + (vel.y * upVec.y)) /
                    (vel.length() * upVec.length()));
            if (theta <= Math.toRadians(MAX_FLING_DEGREES)) {
                return vel;
            }
        }
        return null;
    }
    private static final int FLING_ACTION_NONE = 0;
    private static final int FLING_ACTION_PREV = 1;
    private static final int FLING_ACTION_NEXT = 2;
    private static final int FLING_ACTION_DELETE = 3;
    private int getFlingingAction(DragSource source, PointF vel) {
        if (mFlingToDeleteDropTarget == null) return FLING_ACTION_NONE;
        if (!source.supportsFlingToDelete()) return FLING_ACTION_NONE;

        ViewConfiguration config = ViewConfiguration.get(mLauncher);
        mVelocityTracker.computeCurrentVelocity(1000, config.getScaledMaximumFlingVelocity());
        float xv = mVelocityTracker.getXVelocity();
        float yv = mVelocityTracker.getYVelocity();
        float axv = Math.abs(xv);
        float ayv = Math.abs(yv);
        if (axv > ayv && axv > mFlingToMoveThresholdVelocity) {
            vel.x = mVelocityTracker.getXVelocity();
            vel.y = mVelocityTracker.getYVelocity();
            boolean rtl = mLauncher.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            if(xv > 0) {
                return rtl ? FLING_ACTION_PREV : FLING_ACTION_NEXT;
            } else {
                return rtl ? FLING_ACTION_NEXT : FLING_ACTION_PREV;
            }
        }

        if (mVelocityTracker.getYVelocity() < mFlingToDeleteThresholdVelocity) {
            vel.x = mVelocityTracker.getXVelocity();
            vel.y = mVelocityTracker.getYVelocity();
            PointF upVec = new PointF(0f, -1f);
            float theta = (float) Math.acos(((vel.x * upVec.x) + (vel.y * upVec.y)) /
                    (vel.length() * upVec.length()));
            if (theta <= Math.toRadians(MAX_FLING_DEGREES)) {
                return FLING_ACTION_DELETE;
            }
        }
        return FLING_ACTION_NONE;
    }

    private void dropOnFlingToMove(float x, float y, PointF vel, boolean next) {
        if(!(mFlingToDeleteDropTarget instanceof DeleteDropTarget))
            return;
        final int[] coordinates = mCoordinatesTemp;

        mDragObject.x = coordinates[0];
        mDragObject.y = coordinates[1];

        // Clean up dragging on the target if it's not the current fling delete target otherwise,
        // start dragging to it.
        if (mLastDropTarget != null && mFlingToDeleteDropTarget != mLastDropTarget) {
            mLastDropTarget.onDragExit(mDragObject);
        }

        // Drop onto the fling-to-delete target
        boolean accepted = false;
        mDragObject.isFlingToMove = true;
        mFlingToDeleteDropTarget.onDragEnter(mDragObject);
        // We must set dragComplete to true _only_ after we "enter" the fling-to-delete target for
        // "drop"
        mDragObject.dragComplete = true;
        //if mDragObject instanceof ShortcutInfo and mDragObject.itemType==ITEM_SHORTCUTINFO
        //do not remove it in onDropComplete
        mFlingToDeleteDropTarget.onDragExit(mDragObject);
        if (mFlingToDeleteDropTarget.acceptDrop(mDragObject)) {
            ((DeleteDropTarget)mFlingToDeleteDropTarget).onFlingToMove(mDragObject, mDragObject.x, mDragObject.y,
                    vel, next);
            accepted = true;
        }
        mDragObject.deferDragViewCleanupPostAnimation = false;
        mDragObject.dragSource.onDropCompleted((View) mFlingToDeleteDropTarget, mDragObject, true,
                accepted);
    }

    private void dropOnFlingToDeleteTarget(float x, float y, PointF vel) {
        final int[] coordinates = mCoordinatesTemp;

        mDragObject.x = coordinates[0];
        mDragObject.y = coordinates[1];

        // Clean up dragging on the target if it's not the current fling delete target otherwise,
        // start dragging to it.
        if (mLastDropTarget != null && mFlingToDeleteDropTarget != mLastDropTarget) {
            mLastDropTarget.onDragExit(mDragObject);
        }
        mDragObject.isFlingToDelete = true;
        // Drop onto the fling-to-delete target
        boolean accepted = false;
        mFlingToDeleteDropTarget.onDragEnter(mDragObject);
        // We must set dragComplete to true _only_ after we "enter" the fling-to-delete target for
        // "drop"
        mDragObject.dragComplete = true;
        mFlingToDeleteDropTarget.onDragExit(mDragObject);
        if (mFlingToDeleteDropTarget.acceptDrop(mDragObject)) {
            mFlingToDeleteDropTarget.onFlingToDelete(mDragObject, mDragObject.x, mDragObject.y,
                    vel);
            accepted = true;
        }
        if (mLauncher.isInLauncherEditMode()) {
            if (mDragObject.dragSource instanceof AdapterView<?> && mDragObject.dragInfo instanceof PendingAddItemInfo) {
                mLauncher.getEditModeHelper().backToWidgetList(mDragObject, true);
            } else {
                mLauncher.getEditModeHelper().backToEditmodeEntry();
            }
        }
        mDragObject.dragSource.onDropCompleted((View) mFlingToDeleteDropTarget, mDragObject, true,
                accepted);
    }

    private void drop(float x, float y) {
        final int[] coordinates = mCoordinatesTemp;
        final DropTarget dropTarget = findDropTarget((int) x, (int) y, coordinates);

        mDragObject.x = coordinates[0];
        mDragObject.y = coordinates[1];
        boolean accepted = false;
        if (dropTarget != null) {
            mDragObject.dragComplete = true;
            if (mDragObject.dragInfo != null) {
                dropTarget.onDragExit(mDragObject);
                if (dropTarget.acceptDrop(mDragObject)) {
                    dropTarget.onDrop(mDragObject);
                    accepted = true;
                }
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: dragging screen
            } else {
                Workspace workspace = mLauncher.getWorkspace();
                workspace.onDragExit(mDragObject);
                if (workspace.acceptDrop(mDragObject)) {
                    workspace.onDrop(mDragObject);
                    accepted = true;
                }
            }
            /* YUNOS END */
            if (mLauncher.isInLauncherEditMode()) {
                if (mDragObject.dragSource instanceof AdapterView<?> && mDragObject.dragInfo instanceof PendingAddItemInfo) {
                    mLauncher.getEditModeHelper().backToWidgetList(mDragObject, true);
                } else {
                    mLauncher.getEditModeHelper().backToEditmodeEntry();
                }
            }
        }
        mDragObject.dragSource.onDropCompleted((View) dropTarget, mDragObject, false, accepted);
    }

    private DropTarget findDropTarget(int x, int y, int[] dropCoordinates) {
        final Rect r = mRectTemp;

        final ArrayList<DropTarget> dropTargets = mDropTargets;
        final int count = dropTargets.size();
        for (int i=count-1; i>=0; i--) {
            DropTarget target = dropTargets.get(i);
            if (!target.isDropEnabled())
                continue;

            target.getHitRect(r);

            /* YUNOS BEGIN */
            // ##date:2013/12/11 ##author:zhangqiang.zq
            // 72995
            if (!checkRectValide(r)) {
                Log.d(TAG, "checkRectValide fail:" + target);
                continue;
            }
            /* YUNOS END */

            // Convert the hit rect to DragLayer coordinates
            target.getLocationInDragLayer(dropCoordinates);
            r.offset(dropCoordinates[0] - target.getLeft(), dropCoordinates[1] - target.getTop());
            mDragObject.x = x;
            mDragObject.y = y;
            if (r.contains(x, y)) {
                DropTarget delegate = target.getDropTargetDelegate(mDragObject);
                if (delegate != null) {
                    target = delegate;
                    target.getLocationInDragLayer(dropCoordinates);
                }

                // Make dropCoordinates relative to the DropTarget
                dropCoordinates[0] = x - dropCoordinates[0];
                dropCoordinates[1] = y - dropCoordinates[1];

                return target;
            }
        }
        return null;
    }

    /* YUNOS BEGIN */
    // ##date:2013/12/11 ##author:zhangqiang.zq
    // 72995
    private boolean checkRectValide(Rect r) {
        // TODO Auto-generated method stub
        return r.top >= 0 && r.left >= 0 && r.bottom >= r.top
                && r.right >= r.left;
    }

    /* YUNOS END */

    public void setDragScoller(DragScroller scroller) {
        mDragScroller = scroller;
        mIsFolder = scroller instanceof Folder;
        mVerticalScrollZone = -1;
    }

    public void setWindowToken(IBinder token) {
        mWindowToken = token;
    }

    /**
     * Sets the drag listner which will be notified when a drag starts or ends.
     */
    public void addDragListener(DragListener l) {
        mListeners.add(l);
    }

    /**
     * Remove a previously installed drag listener.
     */
    public void removeDragListener(DragListener l) {
        mListeners.remove(l);
    }

    /**
     * clean the drag listener.
     */
    public void clearDragListener() {
        mListeners.clear();
    }

    /**
     * Add a DropTarget to the list of potential places to receive drop events.
     */
    public void addDropTarget(DropTarget target) {
        mDropTargets.add(target);
    }

    /**
     * Don't send drop events to <em>target</em> any more.
     */
    public void removeDropTarget(DropTarget target) {
        mDropTargets.remove(target);
    }

    /**
     * clear the dropTargets in some situation
     */
    public void clearDropTarget() {
        mDropTargets.clear();
    }

    /**
     * Sets the current fling-to-delete drop target.
     */
    public void setFlingToDeleteDropTarget(DropTarget target) {
        mFlingToDeleteDropTarget = target;
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * Set which view scrolls for touch events near the edge of the screen.
     */
    public void setScrollView(View v) {
        mScrollView = v;
    }

    DragView getDragView() {
        return mDragObject == null ? null : mDragObject.dragView;
    }

    private class ScrollRunnable implements Runnable {
        private int mDirection;

        ScrollRunnable() {
        }

        public void run() {
            if (mDragScroller != null) {
                if (mDirection == SCROLL_LEFT) {
                    mDragScroller.scrollLeft();
                } else if (mDirection == SCROLL_RIGHT) {
                    mDragScroller.scrollRight();
                } else if (mDirection == SCROLL_UP) {
                    mDragScroller.scrollUp();
                } else if (mDirection == SCROLL_DOWN) {
                    mDragScroller.scrollDown();
                }
                mScrollState = SCROLL_OUTSIDE_ZONE;
                mDistanceSinceScroll = 0;
                mDragScroller.onExitScrollArea();
                mLauncher.getDragLayer().onExitScrollArea();

                if (isDragging()) {
                    // Check the scroll again so that we can requeue the scroller if necessary
                    checkScrollState(mLastTouch[0], mLastTouch[1]);
                }
            }
        }

        void setDirection(int direction) {
            mDirection = direction;
        }
    }

    private class DragScreenRunnable implements Runnable {
        DragScreenRunnable() {
        }

        public void run() {
            mAnimatorSet = null;
            mDraggingScreen = false;
            mDragScreenWaiting = false;
        }
    }

    private void exchangeScreen(int direction, CellLayout prevPage, CellLayout nextPage) {
        final Workspace workspace = mLauncher.getWorkspace();
        int prev = workspace.indexOfChild(prevPage);
        int next = workspace.indexOfChild(nextPage);
        workspace.removeViewAt(next);
        workspace.removeViewAt(prev);
        workspace.addView(nextPage, prev);
        workspace.addView(prevPage, next);
        /* YUNOS BEGIN */
        // ## date: 2016/06/03 ## author: yongxing.lyx
        // ## BugID:8364282:icon overlap after exchange screen and fling icon.
        workspace.rebuildScreenOccupied();
        /* YUNOS END */
        PageIndicatorView indicator = mLauncher.getIndicatorView();
        if (direction == PREV) {
            workspace.setCurrentPage(prev);
            indicator.setCurrentPos(prev);
        } else {
            workspace.setCurrentPage(next);
            indicator.setCurrentPos(next);
        }
        mLauncher.getEditmodeContainer().exchangeScreen(prev, next);
        mLauncher.getModel().exchangeScreen(mLauncher, prev, next);
    }

    public boolean isDraggingScreen() {
        return mDraggingScreen;
    }

    public boolean isDragScreenWaiting() {
        return mDragScreenWaiting;
    }
}
