package com.aliyun.homeshell.editmode;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.aliyun.homeshell.atom.AtomManager;
import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.CheckVoiceCommandPressHelper;
import com.aliyun.homeshell.DragLayer;
import com.aliyun.homeshell.DropTarget.DragObject;
import com.aliyun.homeshell.FolderInfo;
import com.aliyun.homeshell.Hotseat;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.PageIndicatorView;
import com.aliyun.homeshell.PagedView;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutAndWidgetContainer;
import com.aliyun.homeshell.Workspace;
import com.aliyun.homeshell.editmode.PreviewContainer.PreviewContentType;
import com.aliyun.homeshell.lifecenter.CardBridge;
import com.aliyun.homeshell.setting.ContinuousHomeShellReceiver;
import com.aliyun.utility.FeatureUtility;

public final class EditModeHelper {
    public static Drawable selectedFlag = null;
    private static EditModeHelper instance = new EditModeHelper();

    private Launcher mLauncher;
    private Workspace mWorkspace;
    private PreviewContainer mEditmodeContainer;
    private View mMenu;
    private View mSortMenu;
    private DragLayer mDragLayer;
    private Hotseat mHotseat;
    private PageIndicatorView mIndicatorView;
    private TextView mEditmodeTipsView;
    private CellLayout mLifeCenterCellLayout;

    private static boolean sChangeThemeFromHomeshell;

    private static final String TAG_EDITMODE = Launcher.TAG_EDITMODE;

    public static final int EDIT_MODE_ENTER_TIME = 200;
    public static final int EDIT_MODE_EXIT_TIME = 200;
    public static final int BACK_LAST_TIMEOUT = 300;
    private AnimatorSet mEnterAnimatorSet;
    private AnimatorSet mExitAnimatorSet;
    private AdapterView<ListAdapter> mEditPreviewList;

    private EditModeHelper() {
    }

    public static EditModeHelper getInstance() {
        return instance;
    }

    public void setup(Launcher launcher, PreviewContainer previewContainer) {
        mLauncher = launcher;
        mEditmodeContainer = previewContainer;
        mWorkspace = launcher.getWorkspace();
        mMenu = launcher.getMenu();
        mSortMenu = launcher.getSortMenu();
        mDragLayer = launcher.getDragLayer();
        mHotseat = launcher.getHotseat();
        mIndicatorView = launcher.getIndicatorView();
        mEditmodeTipsView = launcher.getEditmodeTipsView();
        mLifeCenterCellLayout = launcher.getLifeCenterCellLayout();
        mEditmodeContainer.setup(launcher, launcher.getDragController());
    }

    public void enterLauncherEditMode() {
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/01/11 ## author: wangye.wy
        // ## BugID: 7766973: pause atom on hot seat.
        AtomManager.getAtomManager().pauseAtomIcon(false);
        /* YUNOS END */

        playEnterEditModeAnimations();
    }

    private List<Animator> getHotseatAnimator(boolean toEditmode) {
        List<Animator> animList = new ArrayList<Animator>();
        if (toEditmode) {
            if (LauncherApplication.isInLandOrientation()) {
                animList.add(ObjectAnimator.ofFloat(mHotseat, "translationX", 0, mHotseat.getWidth()));
            } else {
                animList.add(ObjectAnimator.ofFloat(mHotseat, "translationY", 0, mHotseat.getHeight()));
            }
            animList.add(ObjectAnimator.ofFloat(mHotseat, "alpha", 1, 0));
        } else {
            if (LauncherApplication.isInLandOrientation()) {
                animList.add(ObjectAnimator.ofFloat(mHotseat, "translationX", mHotseat.getWidth(), 0));
            } else {
                animList.add(ObjectAnimator.ofFloat(mHotseat, "translationY", mHotseat.getHeight(), 0));
            }
            animList.add(ObjectAnimator.ofFloat(mHotseat, "alpha", 0, 1));
        }
        return animList;
    }

    private List<Animator> getIndicatorAndTipsViewAnimator(boolean toEditmode) {
        List<Animator> animList = new ArrayList<Animator>();
        if (toEditmode) {
            if (LauncherApplication.isInLandOrientation()) {
                int indicatorTransX = mLauncher.getResources().getDimensionPixelSize(R.dimen.desktop_indicator_transX);
                animList.add(ObjectAnimator.ofFloat(mIndicatorView, "translationX", indicatorTransX, 0));
            } else {
                int indicatorTransY = mLauncher.getResources().getDimensionPixelSize(R.dimen.desktop_indicator_transY);
                animList.add(ObjectAnimator.ofFloat(mIndicatorView, "translationY", 0, -indicatorTransY));
            }
            animList.add(ObjectAnimator.ofFloat(mEditmodeTipsView, "alpha", 0, 1));
        } else {
            if (LauncherApplication.isInLandOrientation()) {
                int indicatorTransX = mLauncher.getResources().getDimensionPixelSize(R.dimen.desktop_indicator_transX);
                animList.add(ObjectAnimator.ofFloat(mIndicatorView, "translationX", 0, indicatorTransX));
            } else {
                int indicatorTransY = mLauncher.getResources().getDimensionPixelSize(R.dimen.desktop_indicator_transY);
                animList.add(ObjectAnimator.ofFloat(mIndicatorView, "translationY", -indicatorTransY, 0));
            }
            animList.add(ObjectAnimator.ofFloat(mEditmodeTipsView, "alpha", 1, 0));
        }
        return animList;
    }

    public void exitLauncherEditMode(boolean anim) {
        Log.d(TAG_EDITMODE, "exitLauncherEditMode  anim  " + anim);
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/01/11 ## author: wangye.wy
        // ## BugID: 7766973: resume atom on hot seat.
        AtomManager.getAtomManager().resumeAtomIcon();
        /* YUNOS END */
        clearEditModeAnim();
        playExitEditModeAnimations(anim);
    }

    public boolean isEditModeAniamtionRunning() {
        boolean running = mEnterAnimatorSet != null && mEnterAnimatorSet.isRunning();
        running = running || (mExitAnimatorSet != null && mExitAnimatorSet.isRunning());
        return running;
    }

    private void playEnterEditModeAnimations() {
        if (mEnterAnimatorSet != null && mEnterAnimatorSet.isRunning()) {
            mEnterAnimatorSet.end();
            return;
        }
        if (mExitAnimatorSet != null && mExitAnimatorSet.isRunning()) {
            return;
        }
        mLauncher.setLauncherEditMode(true);
        mEnterAnimatorSet = new AnimatorSet().setDuration(EDIT_MODE_ENTER_TIME);

        Drawable drawable = mDragLayer.getBackground();
        if (drawable == null) {
            drawable = new ColorDrawable(0xff000000);
            mDragLayer.setBackground(drawable);
        }
        ObjectAnimator bgAnimator = ObjectAnimator.ofInt(drawable, "alpha", 0, 51);

        // Animator hotseatAntaimor = mHotseat.getHotseatAnimator(true);
        List<Animator> hotseatAnimList = getHotseatAnimator(true);
        List<Animator> indicatorAnimList = getIndicatorAndTipsViewAnimator(true);

        List<Animator> animList = new ArrayList<Animator>();
        if (mWorkspace.getFolderBatchOping() != null) {
            animList.addAll(getEditModeContainerEnterAnim());
        } else {
            animList.addAll(getMenuEnterAnim());
        }
        animList.addAll(hotseatAnimList);
        animList.addAll(indicatorAnimList);
        animList.add(bgAnimator);
        animList.addAll(mWorkspace.getCurrentPageAnimList(true));

        mEnterAnimatorSet.playTogether(animList);
        mEnterAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG_EDITMODE, "mEnterAnimatorSet onAnimationStart  +++");
                // disable continuous temporally
                PagedView.sContinuousHomeShellFeature = false;

                /* YUNOS BEGIN */
                // ##date:2015/8/4 ##author:zhanggong.zg ##BugID:6275448
                // disable page scrolling during this animation
                mWorkspace.setScrollingEnable(false);
                /* YUNOS END */

                if (FeatureUtility.hasFullScreenWidget()) {
                    mWorkspace.removeWidgetPages();
                }
                if (mLauncher.isSupportLifeCenter() &&
                        mLifeCenterCellLayout != null) {
                    mLauncher.setIsSupportLifeCenter(false);
                    mWorkspace.removeView(mLifeCenterCellLayout);
                    mWorkspace.setCurrentPage(mWorkspace.getCurrentPage() - 1);
                }

                CardBridge bridge = mLauncher.getCardBridge();
                if (bridge != null) {
                    bridge.hideLeftScreenGuide();
                }

                if (mLauncher.isHideseatShowing()) {
                    mLauncher.hideHideseat(false);
                }
                // mShortcutAndFolderNum = getShortcutAndFolderNum();
                CheckVoiceCommandPressHelper.PUSH_TO_TALK_SUPPORT = false;
                EditModeHelper.selectedFlag = mLauncher.getResources().getDrawable(R.drawable.ic_corner_mark);
                mEditmodeTipsView.setVisibility(View.VISIBLE);
                mEditPreviewList = mEditmodeContainer.getListView();
                if (mWorkspace.getFolderBatchOping() != null) {
                    if (mEditPreviewList != null) {
                        mEditPreviewList.setVisibility(View.INVISIBLE);
                    }
                    mEditmodeContainer.findViewById(R.id.add_to_folder).setVisibility(View.VISIBLE);
                    mEditmodeContainer.findViewById(R.id.preview_list_indicator).setVisibility(View.INVISIBLE);
                    updateEditModeTips(PreviewContentType.FolderSelect);
                } else {
                    if (mEditPreviewList != null) {
                        mEditPreviewList.setVisibility(View.VISIBLE);
                    }
                    mEditmodeContainer.findViewById(R.id.add_to_folder).setVisibility(View.INVISIBLE);
                    mEditmodeContainer.findViewById(R.id.preview_list_indicator).setVisibility(View.VISIBLE);
                    updateEditModeTips(PreviewContentType.CellLayouts);
                }
                Log.d(TAG_EDITMODE, "mEnterAnimatorSet onAnimationStart  -----");
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG_EDITMODE, "mEnterAnimatorSet onAnimationEnd  ++++");
                mEditmodeContainer.onEnter();
                mWorkspace.enterLauncherEditMode();
                // enable scrolling after animation
                mWorkspace.setScrollingEnable(true);
                Log.d(TAG_EDITMODE, "mEnterAnimatorSet onAnimationEnd  ----");
            }
        });
        mEnterAnimatorSet.start();
    }

    private void playExitEditModeAnimations(boolean anim) {
        if (mEnterAnimatorSet != null && mEnterAnimatorSet.isRunning()) {
            return;
        }
        if (mExitAnimatorSet != null && mExitAnimatorSet.isRunning()) {
            mExitAnimatorSet.end();
            return;
        }
        final Runnable onCompleteRunnable = new Runnable() {

            @Override
            public void run() {
                Log.d(TAG_EDITMODE, "playExitEditModeAnimations  onCompleteRunnable.run ");
                mWorkspace.exitLauncherEditMode();
                if (FeatureUtility.hasFullScreenWidget()) {
                    mWorkspace.makeSureWidgetPages();
                }

                mLauncher.checkLifeCenter();
                if (mLauncher.isSupportLifeCenter() &&
                        mLifeCenterCellLayout != null) {
                    mLauncher.setIsSupportLifeCenter(true);
                    mWorkspace.removeView(mLifeCenterCellLayout);
                    mWorkspace.addView(mLifeCenterCellLayout, 0);
                    mWorkspace.setCurrentPage(mWorkspace.getCurrentPage() + 1);
                }

                CardBridge bridge = mLauncher.getCardBridge();
                if (bridge != null) {
                    bridge.showLeftScreenGuide();
                }

                mEditmodeTipsView.setVisibility(View.GONE);
                mEditmodeContainer.onExit(false);
                mWorkspace.setFolderBatchOping(null);

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mLauncher);
                PagedView.sContinuousHomeShellFeature = (sp != null)
                        && sp.getBoolean(ContinuousHomeShellReceiver.KEY_CONTINUOUS_HOMESHELL_STYLE, true);

                mEditmodeContainer.clearCellLayoutSelected();
                EditModeHelper.selectedFlag = null;
                mWorkspace.clearSelectFlag();
                mEditmodeContainer.clearAnimationView();
                CheckVoiceCommandPressHelper.checkEnvironment();
                mWorkspace.checkAndRemoveEmptyCell();
                mWorkspace.setCurrentPage(mWorkspace.getCurrentPage());
                if (mLauncher.getUnlockAnimation() != null) {
                    mLauncher.getUnlockAnimation().setPageIndex(mWorkspace.getCurrentPage());
                }
                // enable scrolling after animation
                mWorkspace.setScrollingEnable(true);
                mMenu.setVisibility(View.INVISIBLE);
                mLauncher.setMenuShowing(false);
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/03/10 ## author: wangye.wy
                // ## BugID: 7945871: icon sort
                mSortMenu.setVisibility(View.INVISIBLE);
                mLauncher.setSortMenuShowing(false);
                /* YUNOS END */
                if (mWorkspace.isMultiSelectDragging()) {
                    mWorkspace.cancelMultiDrag();
                }
            }
        };

        if (!anim) {
            resetScreenParamsOnExitEditMode();
            mLauncher.setLauncherEditMode(false);
            onCompleteRunnable.run();
            return;
        }

        mLauncher.setLauncherEditMode(false);
        mExitAnimatorSet = new AnimatorSet().setDuration(EDIT_MODE_EXIT_TIME);

        Drawable drawable = mDragLayer.getBackground();
        if (drawable == null) {
            drawable = new ColorDrawable(0xff000000);
            mDragLayer.setBackground(drawable);
        }
        ObjectAnimator bgAnimator = ObjectAnimator.ofInt(drawable, "alpha", 51, 0);

        List<Animator> hotseatAnimList = getHotseatAnimator(false);
        List<Animator> indicatorAnimList = getIndicatorAndTipsViewAnimator(false);

        List<Animator> animList = new ArrayList<Animator>();
        if (mEditmodeContainer.isShowing()) {
            animList.addAll(getEditModeContainerExitAnim());
        }
        if (mLauncher.isMenuShowing()) {
            animList.addAll(getMenuExitAnim());
        }
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: icon sort
        if (mLauncher.isSortMenuShowing()) {
            animList.addAll(getSortMenuExitAnim());
        }
        /* YUNOS END */
        animList.addAll(hotseatAnimList);
        animList.addAll(indicatorAnimList);
        animList.add(bgAnimator);
        animList.addAll(mWorkspace.getCurrentPageAnimList(false));

        mExitAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG_EDITMODE, "mExitAnimatorSet onAnimationStart  +++");
                /* YUNOS BEGIN */
                // ##date:2015/8/4 ##author:zhanggong.zg ##BugID:6275448
                // disable page scrolling during this animation
                mWorkspace.setScrollingEnable(false);
                /* YUNOS END */
                Log.d(TAG_EDITMODE, "mExitAnimatorSet onAnimationStart  ----");
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG_EDITMODE, "mExitAnimatorSet onAnimationEnd  +++");
                onCompleteRunnable.run();
                Log.d(TAG_EDITMODE, "mExitAnimatorSet onAnimationEnd  ----");
            }
        });
        mExitAnimatorSet.playTogether(animList);
        mExitAnimatorSet.start();
    }

    public void resetScreenParamsOnExitEditMode() {
        if (!mLauncher.isInLauncherEditMode() || mWorkspace.isPageMoving()) {
            return;
        }
        if (mDragLayer != null && mDragLayer.getBackground() != null) {
            mDragLayer.getBackground().setAlpha(0);
        }
        if (mHotseat != null) {
            mHotseat.setAlpha(1);
            mHotseat.setTranslationY(0);
        }
        if (mEditmodeTipsView != null) {
            mEditmodeTipsView.setAlpha(0);
        }
        if (mIndicatorView != null) {
            mIndicatorView.setTranslationY(0);
        }
        if (mEditmodeContainer != null && mEditmodeContainer.isShowing()) {
            mEditmodeContainer.setShowing(false);
            mEditmodeContainer.setVisibility(View.INVISIBLE);
        }
        if (mMenu != null && mLauncher.isMenuShowing()) {
            mMenu.setAlpha(0);
            mMenu.setVisibility(View.INVISIBLE);
        }
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: icon sort
        if (mSortMenu != null) {
            mSortMenu.setAlpha(0);
            mSortMenu.setVisibility(View.INVISIBLE);
        }
        /* YUNOS END */
        CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(mWorkspace.getCurrentPage());
        if (cellLayout != null) {
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: header in cell layout
            View header = cellLayout.findViewById(R.id.header);
            if (header != null) {
                cellLayout.removeHeader(header);
            }
            /* YUNOS END */
            /* YUNOS BEGIN */
            // ## modules: Home Shell
            // ## date: 2016/04/25 ## author: wangye.wy
            // ## BugID: 8191267: remove edit button container
            cellLayout.removeEditBtnContainer();
            /* YUNOS END */
            cellLayout.setBackground(null);
            cellLayout.setScaleX(1.0f);
            cellLayout.setScaleY(1.0f);
            cellLayout.setPivotX(0);
            cellLayout.setPivotY(0);
            cellLayout.setRotation(0);
            cellLayout.setRotationY(0);
            cellLayout.setAlpha(1.0f);
            cellLayout.setTranslationX(0);
            cellLayout.setTranslationY(0);
            ShortcutAndWidgetContainer container = cellLayout.getShortcutAndWidgetContainer();
            container.setTranslationY(0);
        }
    }

    /**
     * Returns current playing edit-mode enter or exit animation; Otherwise,
     * returns null.
     */
    public AnimatorSet getRunningEditModeAnimations() {
        if (mEnterAnimatorSet != null && mEnterAnimatorSet.isRunning()) {
            return mEnterAnimatorSet;
        }
        if (mExitAnimatorSet != null && mExitAnimatorSet.isRunning()) {
            return mExitAnimatorSet;
        }
        return null;
    }

    /* YUNOS BEGIN */
    // ##date:2015/05/22 ##author: chenjian.chenjian ##BugId: 6006081
    public void switchPreviewContainerType(PreviewContentType desType) {
        if (mLauncher.isMenuShowing()) {
            mLauncher.dismissMenu();
        }
        mEditmodeContainer.setContentType(desType);
        showEditModeContainer();
    }
    /* YUNOS END */

    private void showEditModeContainer() {
        if (mEditmodeContainer.isShowing()) {
            return;
        }
        Log.d(TAG_EDITMODE, "showEditModeContainer isshowing " + mEditmodeContainer.isShowing());
        clearEditModeAnim();
        mEditmodeContainer.setShowing(true);
        mEditmodeContainer.setVisibility(View.VISIBLE);
        List<Animator> animList = getEditModeContainerEnterAnim();
        mEditModeModeSwitchAnim = new AnimatorSet().setDuration(EDIT_MODE_ENTER_TIME);
        mEditModeModeSwitchAnim.playTogether(animList);
        mEditModeModeSwitchAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mEditmodeContainer.setVisibility(View.VISIBLE);
            }
        });
        mEditModeModeSwitchAnim.start();
    }

    public void hideEditModeContainer() {
        if (!mEditmodeContainer.isShowing()) {
            return;
        }
        Log.d(TAG_EDITMODE, "hideEditModeContainer");
        clearEditModeAnim();
        mEditModeModeSwitchAnim = new AnimatorSet().setDuration(EDIT_MODE_EXIT_TIME);
        mEditmodeContainer.setShowing(false);
        mEditModeModeSwitchAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mEditmodeContainer.setVisibility(View.GONE);
                mLauncher.showWorkspace(true);
            }

        });
        List<Animator> animList = getEditModeContainerExitAnim();
        mEditModeModeSwitchAnim.playTogether(animList);
        mEditModeModeSwitchAnim.start();
    }

    public void setEditModeTipsVisible(boolean isShow) {
        // chenjian added for bug 5997293
        if (isShow && mLauncher.isInLauncherEditMode()) {
            mEditmodeTipsView.setVisibility(View.VISIBLE);
        } else {
            mEditmodeTipsView.setVisibility(View.INVISIBLE);
        }
    }

    public void onThemeChanged() {
        if (mLauncher.isInLauncherEditMode()) {
            if (mEditmodeContainer != null) {
                mEditmodeContainer.onExit(true);
            }
        }
    }

    public void handleClickEventInEditMode(View v) {
        Object object = v.getTag();
        if (object == null || !(object instanceof ItemInfo)) {
            return;
        }

        if (isEditModeAniamtionRunning()) {
            return;
        }

        ViewParent vp = v.getParent().getParent();

        if (!(vp instanceof CellLayout)) {
            return;
        }
        if (mWorkspace.getFolderBatchOping() == null) {
            boolean add = ((CellLayout) vp).handleViewClick(v);
            int selectedSize = mWorkspace.getSelectedViewsInLayout().size();
            onUpdateSelectNumber((CellLayout) (v.getParent().getParent()), add);
            Log.d(TAG_EDITMODE, "handleClickEventInEditMode  add " + add + " selectedSize " + selectedSize);
            if (selectedSize == 0) {
                mEditmodeContainer.setContentType(PreviewContentType.None);
                updateEditModeTips(PreviewContentType.CellLayouts);
                switchFromEmContainerToMenu();
                mWorkspace.clearSelectFlag();
            } else {
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/03/10 ## author: wangye.wy
                // ## BugID: 7945871: icon sort
                if (mEditmodeContainer.getContentType() == PreviewContentType.IconSort) {
                    switchFromSortMenuToEmContainer();
                } else {
                    switchFromMenuToEmContainer();
                }
                /* YUNOS END */
                mEditmodeContainer.setContentType(PreviewContentType.CellLayouts);
                updateEditModeTips(PreviewContentType.MultiSelect);
            }
        } else {
            if (object != null && object instanceof FolderInfo) {
                return;
            }
            ((CellLayout) vp).handleViewClick(v);
            updateEditModeTips(PreviewContentType.FolderSelect);
        }
    }

    int mDragIndex = -1;
    public int getBeginDragIndex() {
        return mDragIndex;
    }

    public void onUpdateSelectNumber(final CellLayout cellLayout, final boolean add) {
        if (mLauncher.isInLauncherEditMode() && mEditmodeContainer != null) {
            mEditmodeContainer.onUpdateSelectNumber(cellLayout, add);
        }
    }

    public void onCellLayoutBeginDrag(int index, boolean in) {
        if (index != -1) {
            mDragIndex = index;
        }
        if (mLauncher.isInLauncherEditMode() && mEditmodeContainer != null && (mDragIndex != -1)) {
            mEditmodeContainer.onCellLayoutBeginDrag(mDragIndex, in);
            if (!in) {
                mDragIndex = -1;
            }
        }
    }

    public void onCellLayoutDataChanged(CellLayout cellLayout, View view) {
        if (mLauncher.isInLauncherEditMode() && mEditmodeContainer != null) {
            mEditmodeContainer.onCellLayoutDataChanged(view != null, cellLayout, view);
        }
    }

    public void onCellLayoutAddOrDelete(boolean add, CellLayout cellLayout, int index) {
        if (mLauncher.isInLauncherEditMode() && mEditmodeContainer != null) {
            mEditmodeContainer.onCellLayoutAddOrDelete(add, cellLayout, index);
        }
    }

    public void reloadItems() {
        if (mLauncher.isInLauncherEditMode() && mEditmodeContainer != null) {
            mEditmodeContainer.reloadItems();
        }
    }

    public void updateEditModeTips(PreviewContentType type) {
        String tips = "";

        Resources res = mLauncher.getResources();
        switch (type) {
            case Widgets :
                tips = res.getString(R.string.edit_mode_tips_widgets);
                break;
            case Effects :
                tips = res.getString(R.string.edit_mode_tips_effects);
                break;
            case Wallpapers :
                tips = res.getString(R.string.edit_mode_tips_wallpapers);
                break;
            case Themes :
                tips = res.getString(R.string.edit_mode_tips_themes);
                break;
            case CellLayouts :
                tips = res.getString(R.string.edit_mode_tips_celllayouts);
                break;
            case MultiSelect :
                tips = res.getString(R.string.edit_mode_tips_multiselect);
                break;
            case FolderSelect :
                tips = res.getString(R.string.edit_mode_tips_multiselect_to_folder);
                break;
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: dragging screen and icon sort
            case DragScreen :
                tips = res.getString(R.string.edit_mode_tips_drag_screen);
                break;
            case IconSort :
                tips = res.getString(R.string.edit_mode_tips_icon_sort);
                break;
            case Preview :
                tips = res.getString(R.string.preview);
            /* YUNOS END */
                break;
            default :
                break;

        }
        mEditmodeTipsView.setText(tips);
    }

    public void backToEditmodeEntry() {
        if (mLauncher.isInLauncherEditMode() && mEditmodeContainer != null && mEditmodeContainer.isShowing()) {
            mLauncher.postRunnableToMainThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG_EDITMODE, "backToEditmodeEntry");
                    switchFromEmContainerToMenu();
                    updateEditModeTips(PreviewContentType.CellLayouts);
                    mWorkspace.clearSelectFlag();
                }
            }, BACK_LAST_TIMEOUT);
        }
    }

    public void switchToDragScreenEntry() {
        if (mLauncher.isInLauncherEditMode()) {
            mLauncher.postRunnableToMainThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG_EDITMODE, "switchToDragScreenEntry");
                    if (mEditmodeContainer.getContentType() == PreviewContentType.IconSort) {
                        switchFromSortMenuToMenu();
                    } else {
                        switchFromEmContainerToMenu();
                    }
                    mEditmodeContainer.setContentType(PreviewContentType.CellLayouts);
                    updateEditModeTips(PreviewContentType.DragScreen);
                }
            }, BACK_LAST_TIMEOUT);
        }
    }

    public void backToWidgetList(DragObject dragObject, boolean delay) {
        if (mLauncher.isInLauncherEditMode()) {
            mLauncher.postRunnableToMainThread(new Runnable() {
                @Override
                public void run() {
                    if (mLauncher.isInLauncherEditMode()) {
                        Log.d(TAG_EDITMODE, "backToWidgetList");
                        switchPreviewContainerType(PreviewContentType.Widgets);
                    }
                }
            }, delay ? BACK_LAST_TIMEOUT : 0);
        }
    }

    public List<Animator> getMenuEnterAnim() {
        mMenu.clearAnimation();
        List<Animator> animList = new ArrayList<Animator>();
        ObjectAnimator menuAlpha = ObjectAnimator.ofFloat(mMenu, "alpha", 0, 1);
        ObjectAnimator menuTranslationY = null;
        if (LauncherApplication.isInLandOrientation()) {
            menuTranslationY = ObjectAnimator.ofFloat(mMenu, "translationX", mMenu.getWidth(), 0);
        } else {
            menuTranslationY = ObjectAnimator.ofFloat(mMenu, "translationY", mMenu.getHeight(), 0);
        }
        menuAlpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mMenu.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mLauncher.isInLauncherEditMode() || ((mExitAnimatorSet != null && mExitAnimatorSet.isRunning()))) {
                    mMenu.setVisibility(View.INVISIBLE);
                    mLauncher.setMenuShowing(false);
                } else {
                    mMenu.setVisibility(View.VISIBLE);
                    mLauncher.setMenuShowing(true);
                }
            }
        });
        animList.add(menuAlpha);
        animList.add(menuTranslationY);
        return animList;
    }

    public List<Animator> getMenuExitAnim() {
        mMenu.clearAnimation();
        List<Animator> animList = new ArrayList<Animator>();
        ObjectAnimator menuAlpha = ObjectAnimator.ofFloat(mMenu, "alpha", 1, 0);
        ObjectAnimator menuTranslationY = null;
        if (LauncherApplication.isInLandOrientation()) {
            menuTranslationY = ObjectAnimator.ofFloat(mMenu, "translationX", 0, mMenu.getWidth());
        } else {
            menuTranslationY = ObjectAnimator.ofFloat(mMenu, "translationY", 0, mMenu.getHeight());
        }
        menuAlpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mMenu.setVisibility(View.INVISIBLE);
                mLauncher.setMenuShowing(false);
            }
        });
        animList.add(menuAlpha);
        animList.add(menuTranslationY);
        return animList;
    }

    public List<Animator> getSortMenuEnterAnim() {
        mSortMenu.clearAnimation();
        List<Animator> animList = new ArrayList<Animator>();
        ObjectAnimator menuAlpha = ObjectAnimator.ofFloat(mSortMenu, "alpha", 0, 1);
        ObjectAnimator menuTranslationY = null;
        if (LauncherApplication.isInLandOrientation()) {
            menuTranslationY = ObjectAnimator.ofFloat(mSortMenu, "translationX", mMenu.getWidth(), 0);
        } else {
            menuTranslationY = ObjectAnimator.ofFloat(mSortMenu, "translationY", mMenu.getHeight(), 0);
        }
        menuAlpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mSortMenu.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mLauncher.isInLauncherEditMode() || ((mExitAnimatorSet != null && mExitAnimatorSet.isRunning()))) {
                    mSortMenu.setVisibility(View.INVISIBLE);
                    mLauncher.setSortMenuShowing(false);
                } else {
                    mSortMenu.setVisibility(View.VISIBLE);
                    mLauncher.setSortMenuShowing(true);
                }
            }
        });
        animList.add(menuAlpha);
        animList.add(menuTranslationY);
        return animList;
    }

    public List<Animator> getSortMenuExitAnim() {
        mSortMenu.clearAnimation();
        List<Animator> animList = new ArrayList<Animator>();
        ObjectAnimator menuAlpha = ObjectAnimator.ofFloat(mSortMenu, "alpha", 1, 0);
        ObjectAnimator menuTranslationY = null;
        if (LauncherApplication.isInLandOrientation()) {
            menuTranslationY = ObjectAnimator.ofFloat(mSortMenu, "translationX", 0, mSortMenu.getWidth());
        } else {
            menuTranslationY = ObjectAnimator.ofFloat(mSortMenu, "translationY", 0, mSortMenu.getHeight());
        }
        menuAlpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mSortMenu.setVisibility(View.INVISIBLE);
                mLauncher.setSortMenuShowing(false);
            }
        });
        animList.add(menuAlpha);
        animList.add(menuTranslationY);
        return animList;
    }

    public List<Animator> getEditModeContainerEnterAnim() {
        List<Animator> animList = new ArrayList<Animator>();
        ObjectAnimator containerAlpha = ObjectAnimator.ofFloat(mEditmodeContainer, "alpha", 0, 1);
        ObjectAnimator containerTranslationY = null;
        if (LauncherApplication.isInLandOrientation()) {
            containerTranslationY = ObjectAnimator.ofFloat(mEditmodeContainer, "translationX", mEditmodeContainer.getWidth(), 0);
        } else {
            containerTranslationY = ObjectAnimator.ofFloat(mEditmodeContainer, "translationY", mEditmodeContainer.getHeight(), 0);
        }

        containerAlpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mEditmodeContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mEditmodeContainer.setVisibility(View.VISIBLE);
                mEditmodeContainer.setShowing(true);
            }
        });
        animList.add(containerAlpha);
        animList.add(containerTranslationY);
        return animList;
    }

    public List<Animator> getEditModeContainerExitAnim() {
        List<Animator> animList = new ArrayList<Animator>();
        ObjectAnimator containerAlpha = ObjectAnimator.ofFloat(mEditmodeContainer, "alpha", 1, 0);
        ObjectAnimator containerTranslationY = null;
        if (LauncherApplication.isInLandOrientation()) {
            containerTranslationY = ObjectAnimator.ofFloat(mEditmodeContainer, "translationX", 0, mEditmodeContainer.getWidth());
        } else {
            containerTranslationY = ObjectAnimator.ofFloat(mEditmodeContainer, "translationY", 0, mEditmodeContainer.getHeight());
        }

        containerAlpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mEditmodeContainer.setVisibility(View.INVISIBLE);
                mEditmodeContainer.setShowing(false);
            }
        });
        animList.add(containerAlpha);
        animList.add(containerTranslationY);
        return animList;
    }

    private AnimatorSet mEditModeModeSwitchAnim;

    public void clearEditModeAnim() {
        if (mEditModeModeSwitchAnim != null && mEditModeModeSwitchAnim.isRunning()) {
            mEditModeModeSwitchAnim.end();
        }
    }

    public void switchFromMenuToEmContainer() {
        clearEditModeAnim();
        if (mEditmodeContainer.isShowing()) {
            return;
        }
        mEditmodeContainer.setShowing(true);
        List<Animator> animList = getMenuExitAnim();
        animList.addAll(getEditModeContainerEnterAnim());
        mEditModeModeSwitchAnim = new AnimatorSet().setDuration(EDIT_MODE_ENTER_TIME);
        mEditModeModeSwitchAnim.playTogether(animList);
        mEditModeModeSwitchAnim.start();
    }

    public void switchFromSortMenuToEmContainer() {
        clearEditModeAnim();
        if (mEditmodeContainer.isShowing()) {
            return;
        }
        mEditmodeContainer.setShowing(true);
        List<Animator> animList = getSortMenuExitAnim();
        animList.addAll(getEditModeContainerEnterAnim());
        mEditModeModeSwitchAnim = new AnimatorSet().setDuration(EDIT_MODE_ENTER_TIME);
        mEditModeModeSwitchAnim.playTogether(animList);
        mEditModeModeSwitchAnim.start();
    }

    public void switchFromEmContainerToMenu() {
        clearEditModeAnim();
        if (mLauncher.isMenuShowing()) {
            return;
        }
        List<Animator> animList = getMenuEnterAnim();
        animList.addAll(getEditModeContainerExitAnim());
        mEditModeModeSwitchAnim = new AnimatorSet().setDuration(EDIT_MODE_ENTER_TIME);
        mEditModeModeSwitchAnim.playTogether(animList);
        mEditModeModeSwitchAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mEditmodeContainer.setShowing(false);
            }
        });
        if (mExitAnimatorSet != null && mExitAnimatorSet.isRunning()) {
            mEditmodeContainer.setShowing(false);
        } else {
            mEditModeModeSwitchAnim.start();
        }
    }

    public void switchFromMenuToSortMenu() {
        clearEditModeAnim();
        if (mLauncher.isSortMenuShowing()) {
            return;
        }
        List<Animator> animList = getMenuExitAnim();
        animList.addAll(getSortMenuEnterAnim());
        mEditModeModeSwitchAnim = new AnimatorSet().setDuration(EDIT_MODE_ENTER_TIME);
        mEditModeModeSwitchAnim.playTogether(animList);
        mEditModeModeSwitchAnim.start();
    }

    public void switchFromSortMenuToMenu() {
        clearEditModeAnim();
        if (mLauncher.isMenuShowing()) {
            return;
        }
        List<Animator> animList = getMenuEnterAnim();
        animList.addAll(getSortMenuExitAnim());
        mEditModeModeSwitchAnim = new AnimatorSet().setDuration(EDIT_MODE_ENTER_TIME);
        mEditModeModeSwitchAnim.playTogether(animList);
        mEditModeModeSwitchAnim.start();
    }

    public void playDismissMenuAnimation() {
        List<Animator> animList = getMenuExitAnim();
        clearEditModeAnim();
        mEditModeModeSwitchAnim = new AnimatorSet();
        mEditModeModeSwitchAnim.setDuration(EDIT_MODE_EXIT_TIME);
        mMenu.setVisibility(View.VISIBLE);
        mEditModeModeSwitchAnim.playTogether(animList);
        mEditModeModeSwitchAnim.start();
    }

    public void playDismissSortMenuAnimation() {
        List<Animator> animList = getSortMenuExitAnim();
        clearEditModeAnim();
        mEditModeModeSwitchAnim = new AnimatorSet();
        mEditModeModeSwitchAnim.setDuration(EDIT_MODE_EXIT_TIME);
        mSortMenu.setVisibility(View.VISIBLE);
        mEditModeModeSwitchAnim.playTogether(animList);
        mEditModeModeSwitchAnim.start();
    }

    public static void setChangeThemeFromeHomeShell(boolean fromHomeShell) {
        sChangeThemeFromHomeshell = fromHomeShell;
    }
    public static boolean isChangeThemeFromeHomeShell() {
        return sChangeThemeFromHomeshell;
    }

}
