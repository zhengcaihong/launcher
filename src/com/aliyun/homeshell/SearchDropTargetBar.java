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

import java.util.ArrayList;

import com.aliyun.homeshell.DropTarget.DragObject;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.appclone.AppCloneManager;
import com.aliyun.homeshell.hideseat.AppFreezeUtil;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.aliyun.homeshell.searchui.SearchBridge;

/*
 * Ths bar will manage the transition between the QSB search bar and the delete drop
 * targets so that each of the individual IconDropTargets don't have to.
 */
public class SearchDropTargetBar extends FrameLayout implements DragController.DragListener, ButtonDropTarget.DragCallback {

    private static final int sTransitionInDuration = 200;
    private static final int sTransitionOutDuration = 175;

    private static final String TAG = "SearchDropTargetBar";

    private ObjectAnimator mDropTargetBarAnim;
    private static final AccelerateInterpolator sAccelerateInterpolator =
            new AccelerateInterpolator();

    private View mDropTargetBar;
    //private ButtonDropTarget mInfoDropTarget;
    private ButtonDropTarget mDeleteDropTarget;
    private ButtonDropTarget mCloneDropTarget;
    private int mBarHeight;
    private boolean mDeferOnDragEnd = false;

    private int mBarAnimationDistance;

//    private Drawable mPreviousBackground;
    private boolean mEnableDropDownDropTargets;

    private Object mDragInfo;
    //added by xiaodong.lxd
    private Launcher mLauncher;

    /*YUNOS BEGIN added by xiaodong.lxd #106631*/
    private enum AnimDirection {ANIM_DEFAULT, ANIM_UP, ANIM_DOWN};
    private AnimDirection mAnimDirection = AnimDirection.ANIM_DEFAULT;
    /*YUNOS END*/

    private DeleteTrashView mTrashTopView;

    public SearchDropTargetBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchDropTargetBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setup(Launcher launcher, DragController dragController) {
        mLauncher = launcher;
        dragController.addDragListener(this);
        //dragController.addDragListener(mInfoDropTarget);
        dragController.addDragListener(mDeleteDropTarget);
        dragController.addDragListener(mCloneDropTarget);
        //dragController.addDropTarget(mInfoDropTarget);
        dragController.addDropTarget(mDeleteDropTarget);
        dragController.addDropTarget(mCloneDropTarget);
        dragController.setFlingToDeleteDropTarget(mDeleteDropTarget);
        //mInfoDropTarget.setLauncher(launcher);
        mDeleteDropTarget.setLauncher(launcher);
        mCloneDropTarget.setLauncher(launcher);
    }

    private void prepareStartAnimation(View v) {
        // Enable the hw layers before the animation starts (will be disabled in the onAnimationEnd
        // callback below)
        v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void setupAnimation(ObjectAnimator anim, final View v) {
        anim.setInterpolator(sAccelerateInterpolator);
        anim.setDuration(sTransitionInDuration);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setLayerType(View.LAYER_TYPE_NONE, null);
                if (mAnimDirection == AnimDirection.ANIM_DOWN && !mLauncher.isDragToDelete()) {
                    exitFullScreen();
                    mLauncher.getEditModeHelper().setEditModeTipsVisible(true);
                    mAnimDirection = AnimDirection.ANIM_DEFAULT;
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                if(mAnimDirection == AnimDirection.ANIM_UP) {
                    v.setBackgroundColor(colorGrey);
                    enterFullScreen();
                }
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Get the individual components
        mDropTargetBar = findViewById(R.id.drag_target_bar);
        //mInfoDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.info_target_text);
        mDeleteDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.delete_target_text);
        mCloneDropTarget = (ButtonDropTarget) mDropTargetBar.findViewById(R.id.appclone_target_text);

        mBarHeight = getResources().getDimensionPixelSize(R.dimen.qsb_bar_height);
        mBarAnimationDistance = getResources().getDimensionPixelSize(R.dimen.qsb_bar_animation_distance);
        mDeleteDropTarget.setDragCallback(this);
        mCloneDropTarget.setDragCallback(this);

        mTrashTopView = (DeleteTrashView) mDropTargetBar.findViewById(R.id.image_trash_top);

        //mInfoDropTarget.setSearchDropTargetBar(this);
        mDeleteDropTarget.setSearchDropTargetBar(this);
        mCloneDropTarget.setSearchDropTargetBar(this);

        mEnableDropDownDropTargets =
            getResources().getBoolean(R.bool.config_useDropTargetDownTransition);

        // Create the various fade animations
        if (mEnableDropDownDropTargets) {
            mDropTargetBar.setTranslationY(-mBarHeight);
            mDropTargetBarAnim = LauncherAnimUtils.ofFloat(mDropTargetBar, "translationY",
                    -mBarHeight, 0f);
        } else {
            mDropTargetBar.setAlpha(0f);
            mDropTargetBarAnim = LauncherAnimUtils.ofFloat(mDropTargetBar, "alpha", 0f, 1f);
        }
        setupAnimation(mDropTargetBarAnim, mDropTargetBar);
    }

    public void finishAnimations() {
        prepareStartAnimation(mDropTargetBar);
        mDropTargetBarAnim.reverse();
        mAnimDirection = AnimDirection.ANIM_DOWN;
    }

    /*
     * Gets various transition durations.
     */
    public int getTransitionInDuration() {
        return sTransitionInDuration;
    }
    public int getTransitionOutDuration() {
        return sTransitionOutDuration;
    }

    /*
     * DragController.DragListener implementation
     */
    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: dragging screen
        if (info == null) {
            return;
        }
        /* YUNOS END */
        mDragInfo = info;
        if(mLauncher.isItemUnDeletable(info)) {
            return;
        }
        /* YUNOS BEGIN */
        // ## date: 2016/07/26 ## author: yongxing.lyx
        // ## BugID:8592129:update delete drop target ui.
        boolean isClonableApp = false;
        if (info instanceof ShortcutInfo) {
            String packageName = ((ShortcutInfo) info).getPackageName();
            isClonableApp = AppCloneManager.isClonable(packageName);
        }
        updateDeleteDropBarTitle(info);
        setCloneDropBarVisible(isClonableApp);
        /* YUNOS END */
        showDropTargetBar(true);
    }

    public void deferOnDragEnd() {
        mDeferOnDragEnd = true;
    }

    @Override
    public void onDragEnd() {
        if(!AgedModeUtil.isAgedMode()&& !mLauncher.isInLauncherEditMode() && mLauncher.getWorkspace().getOpenFolder() == null){
            mLauncher.setGlobalSearchVisibility(View.VISIBLE);
        }
        if(mAnimDirection == AnimDirection.ANIM_DEFAULT) {
            return;
        }
        hideDropTargetBar(true);
    }

    /*YUNOS BEGIN added by xiaodong.lxd for push to talk*/
    public void showDropTargetBar(boolean anim) {
        if(mLauncher.isItemUnDeletable(mDragInfo)) {
            return;
        }
        mLauncher.setGlobalSearchVisibility(View.GONE);
        mLauncher.getEditModeHelper().setEditModeTipsVisible(false);
        if(anim) {
            if(mDropTargetBarAnim.isStarted() && mAnimDirection == AnimDirection.ANIM_DOWN) {
                mDropTargetBarAnim.end();
            } else if (mAnimDirection == AnimDirection.ANIM_UP) {
                return;
            }
            //mTrashBarShowIn = true;
            prepareStartAnimation(mDropTargetBar);
            mAnimDirection = AnimDirection.ANIM_UP;
            mDropTargetBarAnim.start();
        } else {
            mDropTargetBar.setTranslationY(0);
            enterFullScreen();
        }
    }

    public void hideDropTargetBar(boolean anim) {
        if(anim) {
            if(mAnimDirection == AnimDirection.ANIM_UP) {
                mDropTargetBarAnim.end();
            } else if (mAnimDirection == AnimDirection.ANIM_DOWN || mAnimDirection == AnimDirection.ANIM_DEFAULT) {
                return;
            }

            if (!mDeferOnDragEnd) {
                // Restore the QSB search bar, and animate out the drop target bar
                mDropTargetBarAnim.cancel();
                prepareStartAnimation(mDropTargetBar);
                mAnimDirection = AnimDirection.ANIM_DOWN;
                mDropTargetBarAnim.reverse();
            } else {
                mDeferOnDragEnd = false;
            }
        } else {
            mDropTargetBar.setTranslationY(-mBarHeight);
            exitFullScreen();
            mLauncher.getEditModeHelper().setEditModeTipsVisible(true);

        }
    }
    /*YUNOS END*/

    private void exitFullScreen() {
        mLauncher.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private  void enterFullScreen() {
        mLauncher.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private final int colorGrey = getResources().getColor(R.color.search_bar_background_color_grey);
    private final int colorRed = getResources().getColor(R.color.search_bar_background_color_red);
    private final int colorTransparent = getResources().getColor(R.color.transparent);
    private static final int TRASH_BIN_ROTATION = -11;
    private static final int TRASH_BIN_TRANX = -2;
    private boolean mDragEnded = false;
    private boolean mDragEntered = false;
    private AnimatorSet mEnterAnimatorSet;
    private Object mEnterAnimatorSetLock = new Object();

    public void setBarHeight(float newHeight) {
        mBarHeight = (int) newHeight;
        requestLayout();
    }

    public ArrayList<Animator> getWorkSpaceAnimators(boolean in) {
        ArrayList<Animator> listAnimators = new ArrayList<Animator>();
        int tranY = mBarAnimationDistance * 2;
        if (mLauncher.getWorkspace() == null) {
            return listAnimators;
        }
        if (in && mLauncher.getWorkspace().getTranslationY() != 0) {
            return listAnimators;
        }
        if (!in && mLauncher.getWorkspace().getTranslationY() != tranY) {
            return listAnimators;
        }
        /* YUNOS BEGIN */
        // ##author:xiangnan.xn@alibaba-inc.com
        // ##BugID:8168517 ##date:2015/04/20
        // editmode may show with deleteDialog,
        // recovery workspace if needed when exit searchBar in editmode
        if (mLauncher.isInLauncherEditMode()) {
            if (!(!in && mLauncher.getWorkspace().getTranslationY() == tranY))
                return listAnimators;
        }
        /* YUNOS END */
        mLauncher.getWorkspace().clearAnimation();
        mLauncher.getHotseat().clearAnimation();
        mLauncher.getIndicatorView().clearAnimation();
        final ObjectAnimator workspaceAnimator = ObjectAnimator.ofPropertyValuesHolder(mLauncher.getWorkspace(),
                PropertyValuesHolder.ofFloat("translationY", in ? 0 : tranY, in ? tranY : 0));
        final ObjectAnimator hotseatAnimator = ObjectAnimator.ofPropertyValuesHolder(mLauncher.getHotseat(),
                PropertyValuesHolder.ofFloat("translationY", in ? 0 : tranY, in ? tranY : 0));
        final ObjectAnimator indicatorAnimator = ObjectAnimator.ofPropertyValuesHolder(mLauncher.getIndicatorView(),
                PropertyValuesHolder.ofFloat("translationY", in ? 0 : tranY, in ? tranY : 0));
        if (mLauncher.isHideseatShowing()) {
            mLauncher.getCustomHideseat().clearAnimation();
            final ObjectAnimator hideseatAnimator = ObjectAnimator.ofPropertyValuesHolder(mLauncher.getCustomHideseat(),
                    PropertyValuesHolder.ofFloat("translationY", in ? 0 : tranY, in ? tranY : 0));
            listAnimators.add(hideseatAnimator);
        }
        listAnimators.add(workspaceAnimator);
        listAnimators.add(hotseatAnimator);
        listAnimators.add(indicatorAnimator);
        return listAnimators;
    }

    protected AnimatorSet getTrashAnimatorSet(final boolean in, boolean onlyWorkspace) {
        int startHeight = getResources().getDimensionPixelSize(R.dimen.qsb_bar_height);
        synchronized(mEnterAnimatorSetLock){
            if (mEnterAnimatorSet != null && mEnterAnimatorSet.isRunning()) {
                mEnterAnimatorSet.end();
            }
        }
        AnimatorSet set = new AnimatorSet();
        ArrayList<Animator> listAnimators = getWorkSpaceAnimators(in);

        if (!onlyWorkspace) {
            mDropTargetBar.clearAnimation();
            mLauncher.getSearchBar().clearAnimation();
            mTrashTopView.clearAnimation();
            final ObjectAnimator backgroundColorAnimator = ObjectAnimator.ofObject(mDropTargetBar, "backgroundColor", new ArgbEvaluator(),
                    in ? colorGrey : colorRed, in ? colorRed : colorGrey);
            final ValueAnimator rotationAnimator = ValueAnimator.ofFloat(in ? 0 : 1, in ? 1 : 0);
            rotationAnimator.removeAllListeners();
            rotationAnimator.addUpdateListener(new AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float degree = (Float) animation.getAnimatedValue() * TRASH_BIN_ROTATION;
                    float translationX = (Float) animation.getAnimatedValue() * TRASH_BIN_TRANX;
                    mTrashTopView.setDegree(degree);
                    mTrashTopView.setCanvasTranlationX(translationX);
                    mTrashTopView.invalidate();
                }
            });
            if (mCloneDropTarget.getVisibility() != View.VISIBLE) {
                listAnimators.add(backgroundColorAnimator);
            }
            listAnimators.add(rotationAnimator);
        }
        int destY = in ? mBarAnimationDistance + startHeight : startHeight;
        final ObjectAnimator heightAnimator = LauncherAnimUtils.ofFloat(mLauncher.getSearchBar(), "barHeight", mBarHeight, destY);
        listAnimators.add(heightAnimator);
        set.playTogether(listAnimators);
        set.setDuration(300);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                synchronized(mEnterAnimatorSetLock){
                    mEnterAnimatorSet = null;
                }
            }
        });
        return set;
    }

    @Override
    public void dragStartCB(View targetView, DragSource source, Object info) {
        ImageView iv = (ImageView) findViewById(R.id.appclone_target_image);
        if (AppCloneManager.isSupportAppClone() && iv != null
                && ((DeleteDropTarget) targetView).isAppCloneTarget()) {
            String packageName = null;
            if (info instanceof ShortcutInfo
                    && ((ShortcutInfo) info).itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                packageName = ((ShortcutInfo) info).getPackageName();
            }
            if (packageName != null
                    && (info instanceof ShortcutInfo)
                    && !AppFreezeUtil.isPackageFrozen(getContext(), (ShortcutInfo) info)
                    && mLauncher.mAppCloneManager.canClone(packageName)) {
                iv.setImageResource(R.drawable.ic_appclone_able);
            } else {
                iv.setImageResource(R.drawable.ic_appclone_disable);
            }
        }
    }

    @Override
    public void dragEnterCB(View targetView, DragObject d) {
        if (mTrashTopView == null || mDragEntered || d.isFlingToDelete || d.isFlingToMove) {
            return;
        }
        mDragEnded = false;
        mDragEntered = true;

        if (((DeleteDropTarget)targetView).isDeleteTarget()) {
            if (mCloneDropTarget.getVisibility() == View.VISIBLE) {
                mDropTargetBar.setBackgroundResource(R.drawable.delete_shape);
            }
        } else {
            mDropTargetBar.setBackgroundResource(R.drawable.fission_shape);
        }
        synchronized (mEnterAnimatorSetLock) {
            mEnterAnimatorSet = getTrashAnimatorSet(true,
                    ((DeleteDropTarget) targetView).isAppCloneTarget());
            mEnterAnimatorSet.start();
        }
    }

    @Override
    public void dragExitCB(View targetView, DragObject d) {
        if (mTrashTopView == null || !mDragEntered || d.isFlingToDelete || d.isFlingToMove) {
            return;
        }
        mDragEnded = false;
        mDragEntered = false;

        /* YUNOS BEGIN */
        // #BugID:8168517, #author:xy83652, #date:2016/07/12
        // #Desc: change background when drag exit
        AnimatorSet set = getTrashAnimatorSet(false, ((DeleteDropTarget) targetView).isAppCloneTarget());
        Drawable background = mDropTargetBar.getBackground();
        final Drawable trashbackground = getResources().getDrawable(R.drawable.trash_background);
        trashbackground.setAlpha(0);
        if (mCloneDropTarget.getVisibility() == View.VISIBLE) {
            mDropTargetBar.setBackground(new LayerDrawable(new Drawable[]{background, trashbackground}));
        }
        final ObjectAnimator backgroundAnimator = ObjectAnimator.ofInt(background, "alpha", 255, 0);

        backgroundAnimator.addListener(new AnimatorListenerAdapter(){
            @Override
            public void onAnimationEnd(Animator animation) {
                /* YUNOS BEGIN */
                // ## date: 2016/07/26 ## author: yongxing.lyx
                // ## BugID:8592129:don't reset background color if just move from clone
                // target to delete target.
                if (!mDragEntered) {
                    mDropTargetBar.setBackgroundColor(colorGrey);
                }
                /* YUNOS END */
            }
        });

        backgroundAnimator.addUpdateListener(new AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator arg0) {
                float fractor = arg0.getAnimatedFraction();
                trashbackground.setAlpha((int)(255*fractor));
            }
        });

        backgroundAnimator.setDuration(set.getDuration());
        set.addListener(new AnimatorListenerAdapter(){
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (mCloneDropTarget.getVisibility() == View.VISIBLE) {
                    backgroundAnimator.start();
                }
            }
        });
        set.start();
        /* YUNOS END */
    }

    @Override
    public void dragEndCB(View targetView) {
        mDragEnded = true;
        if (mDragEntered) {
            synchronized(mEnterAnimatorSetLock){
                if (mEnterAnimatorSet != null && mEnterAnimatorSet.isStarted()) {
                    mEnterAnimatorSet.end();
                }
            }
            if(mDragInfo != null && ((DeleteDropTarget)targetView).isDeleteTarget()) {
                ItemInfo info = (ItemInfo)mDragInfo;
                if(info.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER ||
                        info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                    resetState();
                } else {
                    AnimatorSet set = getTrashAnimatorSet(false, false);
                    set.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            resetState();
                        }
                    });
                    set.start();
                }
            }
            mDragEntered = false;
        } else {
            resetState();
        }
    }

    private void resetState() {
        mDropTargetBar.setBackgroundColor(colorTransparent);
        if (mTrashTopView != null) {
            mTrashTopView.setDegree(0);
            mTrashTopView.setCanvasTranlationX(0);
            mTrashTopView.invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mLauncher.getDragController().isDragging() && !mDragEnded) {
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int newHeightSpec = MeasureSpec.makeMeasureSpec(mBarHeight, heightSpecMode);
            super.onMeasure(widthMeasureSpec, newHeightSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /* YUNOS BEGIN */
    // ## date: 2016/07/26 ## author: yongxing.lyx
    // ## BugID:8592129:update delete drop target ui.
    private void setCloneDropBarVisible(boolean visible) {
        View appCloneBar = mDropTargetBar.findViewById(R.id.appclone_target_bar);
        if (visible) {
            if (mCloneDropTarget.getVisibility() != View.VISIBLE) {
                mCloneDropTarget.setVisibility(View.VISIBLE);
            }
            if (appCloneBar != null && appCloneBar.getVisibility() != View.VISIBLE) {
                appCloneBar.setVisibility(View.VISIBLE);
            }
            // maybe mCloneDropTarget was added, so we remove it first.
            mLauncher.getDragController().removeDropTarget(mCloneDropTarget);
            mLauncher.getDragController().addDropTarget(mCloneDropTarget);
        } else {
            if (mCloneDropTarget.getVisibility() != View.GONE) {
                mCloneDropTarget.setVisibility(View.GONE);
            }
            if (appCloneBar != null && appCloneBar.getVisibility() != View.GONE) {
                appCloneBar.setVisibility(View.GONE);
            }
            mLauncher.getDragController().removeDropTarget(mCloneDropTarget);
        }
    }

    private void updateDeleteDropBarTitle(Object info) {
        int titleId = R.string.drop_title_delete;
        if (info instanceof FolderInfo) {
            titleId = R.string.drop_title_dismiss;
        } else if (info instanceof ShortcutInfo) {
            ShortcutInfo shortcutInfo = (ShortcutInfo) info;
            if (shortcutInfo.itemType == Favorites.ITEM_TYPE_APPLICATION
                    || shortcutInfo.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                titleId = R.string.drop_title_uninstall;
            }
        }
        TextView delDrop = (TextView) mDropTargetBar.findViewById(R.id.delete_drop_bar_title);
        if (delDrop != null) {
            delDrop.setText(titleId);
        }
    }
    /* YUNOS BEGIN */
}
