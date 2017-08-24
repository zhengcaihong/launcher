
package com.aliyun.homeshell;

import static android.view.View.ALPHA;
import static android.view.View.LAYER_TYPE_HARDWARE;
import static android.view.View.LAYER_TYPE_NONE;
import static android.view.View.SCALE_X;
import static android.view.View.SCALE_Y;
import static android.view.View.TRANSLATION_X;
import static android.view.View.TRANSLATION_Y;

import java.util.ArrayList;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.DecelerateInterpolator;

import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.setting.HomeShellSetting;
import com.aliyun.utility.utils.ACA;

public class FolderUtils {
    private static final boolean DEBUG_THUMBNAIL = false;
    public static final boolean LOW_RAM = "true".equals(ACA.SystemProperties.get("ro.config.low_ram", "false"));
    protected static final int FOLDER_OPEN_DURATION = 250;
    protected static final int FOLDER_CLOSE_DURATION = 200;
    protected static final int ANIMATION_START_DELAY = 50;
    private TimeInterpolator mOpenInterpolator = new DecelerateInterpolator();
    private TimeInterpolator mCloseInterpolator = new DecelerateInterpolator();
    private Folder mFolder;
    private AnimatorSet mCurAnim;
    private Drawable mBackground;
    private OnPreDrawListener mPreDrawListener;
    private float folderTransX, folderTransY;
    private static boolean mAnimatingClosed = false;
    private static boolean mNeedsDelay = false;

    public boolean isFolderOpened() {
        return mCurAnim != null || mPreDrawListener != null || (mFolder != null && mFolder.getState() > Folder.STATE_SMALL);
    }

    public void animateOpen(final Folder folder, final Runnable finish) {
        if (!(folder.getParent() instanceof DragLayer))
            return;
        if (mCurAnim != null || mPreDrawListener != null)
            return;
        ACA.View.setTransitionAlpha(folder, 1);
        folder.getViewTreeObserver().addOnPreDrawListener(
                mPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {

                    public boolean onPreDraw() {
                        folder.getViewTreeObserver().removeOnPreDrawListener(this);
                        mPreDrawListener = null;
                        if (!folder.mLauncher.getWindow().isActive()) {
                            animateClosed(folder, false, false);
                            return false;
                        }
                        mFolder = folder;
                        // enable hardware texture
                        final Launcher launcher = folder.mLauncher;
                        final Hotseat hotseat = launcher.getHotseat();
                        final View indicator = launcher.getIndicatorView();
                        final CellLayout page = (CellLayout) launcher.getWorkspace().getPageAt(launcher.getCurrentWorkspaceScreen());
                        configLayerType(true, page,folder, hotseat, indicator);
                        final FolderIcon folderIcon = folder.getmFolderIcon();
                        final CellLayout content = folder.getContent();
                        final ShortcutAndWidgetContainer container = content.getShortcutAndWidgetContainer();
                        final PageIndicatorView folderIndicator = folder.getIndicatorView();
                        final DragLayer root = launcher.getDragLayer();
                        final float scale = folder.mIconScale;
                        final ArrayList<Animator> anims = new ArrayList<Animator>();

                        final Rect iconRect = new Rect();
                        root.getViewRectRelativeToSelf(folderIcon, iconRect);

                        // calculate folder pre offset
                        folderTransX = iconRect.left + folder.mIconPaddingX;
                        folderTransY = iconRect.top + folder.mIconPaddingY;
                        // init folder state
                        folder.setPivotX(0);
                        folder.setPivotY(0);
                        folder.setScaleX(scale);
                        folder.setScaleY(scale);
                        folder.setTranslationX(folderTransX - folder.getLeft());
                        folder.setTranslationY(folderTransY - folder.getTop());

                        // add folder scale up animation
                        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(
                                folder,
                                PropertyValuesHolder.ofFloat(SCALE_X, 1),
                                PropertyValuesHolder.ofFloat(SCALE_Y, 1),
                                PropertyValuesHolder.ofFloat(TRANSLATION_X, 0),
                                PropertyValuesHolder.ofFloat(TRANSLATION_Y, 0));
                        anims.add(oa);

                        // add background fade in animation
                        mBackground = new ColorDrawable(0xff000000);
                        mBackground.setAlpha(0);
                        root.setBackground(mBackground);
                        oa = ObjectAnimator.ofInt(mBackground, "alpha", 0, 0x80);
                        anims.add(oa);

                        // add workspace fade out animation
                        anims.add(ObjectAnimator.ofFloat(page, ALPHA, 0));
                        anims.add(ObjectAnimator.ofFloat(hotseat, ALPHA, 0));
                        anims.add(ObjectAnimator.ofFloat(indicator, ALPHA, 0));

                        /* YUNOS BEGIN */
                        // ##date:2015/7/29 ##author:zhanggong.zg ##BugID:6220023
                        // enhance folder animation
                        folderIndicator.setAlpha(0);
                        anims.add(ObjectAnimator.ofFloat(folderIndicator, ALPHA, 1));

                        final ArrayList<View> animatedTarget = new ArrayList<View>();
                        final float[] alphaValues = { 0, 1, 1, 1, 1, 1, 1 };
                        for (int i = 0, N = container.getChildCount(); i < N; i++) {
                            View v = container.getChildAt(i);
                            if (v != null) {
                                animatedTarget.add(v);
                                anims.add(ObjectAnimator.ofFloat(v, ALPHA, alphaValues));
                                v.setAlpha(0);
                            }
                        }

                        folderIcon.setHideIcon(false);
                        folderIcon.enableIconBitmapCache();
                        anims.add(ObjectAnimator.ofInt(folderIcon, "iconAlpha", 255, 0, 0, 0, 0));
                        /* YUNOS END */

                        final AnimatorSet animSet = new AnimatorSet();
                        animSet.playTogether(anims);
                        animSet.setStartDelay(ANIMATION_START_DELAY);
                        animSet.setDuration(FOLDER_OPEN_DURATION);
                        animSet.setInterpolator(mOpenInterpolator);
                        animSet.addListener(new Listener() {
                            public void onAnimationStart(Animator animation) {
                                folder.setState(Folder.STATE_ANIMATING);
                                folderIcon.setHideIcon(true);
                                hotseat.preHideHotseat();
                                /* YUNOS BEGIN */
                                //## modules(Home Shell): [Folder]
                                //## date: 2016/03/25 ## author: wangye.wy
                                //## BugID: 8064745: increase gap between icons
                                if (!folder.isSupportCardIcon()) {
                                    folder.setFolderLayout(false);
                                }
                                /* YUNOS END */
                            }

                            public void onAnimationEnd(Animator animation) {
                                folderIndicator.setAlpha(1);
                                folderIcon.disableIconBitmapCache();
                                for (View v : animatedTarget) {
                                    v.setAlpha(1.0f);
                                }
                                folder.setState(Folder.STATE_OPEN);
                                configLayerType(false, folder);
                                folder.setFocusOnFirstChild();
                                mCurAnim = null;
                                if (finish != null) {
                                    finish.run();
                                }
                            }
                        });
                        if (!DEBUG_THUMBNAIL) {
                            (mCurAnim = animSet).start();
                        }
                        return false;
                    }
                });
    }

    public void animateClosed(final Folder folder, final boolean scrollBack, boolean anim) {
        /* YUNOS BEGIN */
        // ##date:2015/7/29 ##author:zhanggong.zg ##BugID:6220023
        // enhance folder animation
        // folder.switchToPage(0, scrollBack);
        /* YUNOS END */
        if (mPreDrawListener != null) {
            folder.getViewTreeObserver().removeOnPreDrawListener(mPreDrawListener);
            mPreDrawListener = null;
        }
        if (folder.getState() == Folder.STATE_NONE) {
            folder.getmFolderIcon().setHideIcon(false);
            folder.onCloseComplete();
            return;
        }
        mAnimatingClosed = true;
        mFolder = folder;
        if (mCurAnim != null)
            mCurAnim.cancel();
        final Launcher launcher = folder.mLauncher;
        final Hotseat hotseat = launcher.getHotseat();
        final View indicator = launcher.getIndicatorView();
        final CellLayout page = (CellLayout) launcher.getWorkspace().getPageAt(launcher.getCurrentWorkspaceScreen());

        final float scale = folder.mIconScale;
        final boolean supportCardIcon = folder.isSupportCardIcon();
        final CellLayout content = folder.getCurrentPage();
        final ShortcutAndWidgetContainer container = content.getShortcutAndWidgetContainer();
        final FolderEditText folderName = folder.mFolderName;
        final FolderIcon folderIcon = folder.getmFolderIcon();
        final PageIndicatorView folderIndicator = folder.getIndicatorView();
        final int limit = folder.mDrawingLimit;
        final ArrayList<Animator> anims = new ArrayList<Animator>();

        // add folder scale down animation
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(
                folder,
                PropertyValuesHolder.ofFloat(SCALE_X, scale),
                PropertyValuesHolder.ofFloat(SCALE_Y, scale),
                PropertyValuesHolder.ofFloat(TRANSLATION_X, folderTransX - folder.getLeft()),
                PropertyValuesHolder.ofFloat(TRANSLATION_Y, folderTransY - folder.getTop()));
        anims.add(oa);
        if (ConfigManager.isLandOrienSupport()) {
            ObjectAnimator oa1 = LauncherAnimUtils.ofPropertyValuesHolder(
                    folder,PropertyValuesHolder.ofFloat(ALPHA, 0));
            oa1.setStartDelay(FOLDER_CLOSE_DURATION/2);
            anims.add(oa1);
        }

        // add background fade out animation
        anims.add(ObjectAnimator.ofInt(mBackground, "alpha", 0));

        // add workspace fade in animation
        anims.add(ObjectAnimator.ofFloat(page, ALPHA, 1));
        anims.add(ObjectAnimator.ofFloat(hotseat, ALPHA, 1));
        anims.add(ObjectAnimator.ofFloat(indicator, ALPHA, 1));
        anims.add(ObjectAnimator.ofFloat(folderName, ALPHA, 0));
        anims.add(ObjectAnimator.ofFloat(folderIndicator, ALPHA, 0));
        final ArrayList<View> hideTarget = new ArrayList<View>();
        for (int i = limit, N = container.getChildCount(); i < N; i++) {
            View v = container.getChildAt(i);
            if (v != null && v.getVisibility() == View.VISIBLE) {
                hideTarget.add(v);
                anims.add(ObjectAnimator.ofFloat(v, ALPHA, 0));
            }
        }

        /* YUNOS BEGIN */
        // ##date:2015/7/29 ##author:zhanggong.zg ##BugID:6220023
        // enhance folder animation
        final ArrayList<View> animatedTarget = new ArrayList<View>();
        final float[] alphaValues = { 1, 1, 1, 1, 1, 0 };
        for (int i = 0, N = Math.min(limit, container.getChildCount()); i < N; i++) {
            View v = container.getChildAt(i);
            if (v != null && v.getVisibility() == View.VISIBLE) {
                animatedTarget.add(v);
                anims.add(ObjectAnimator.ofFloat(v, ALPHA, alphaValues));
            }
        }

        folderIcon.enableIconBitmapCache();
        anims.add(ObjectAnimator.ofInt(folderIcon, "iconAlpha", 0, 0, 0, 0, 255));
        if (folder.getCurrentPageIndex() != 0) {
            float w = folderIcon.getWidth() / 2;
            anims.add(ObjectAnimator.ofFloat(folderIcon, "iconTranslateX", w, w, w, w, 0));
        }
        /* YUNOS BEGIN */

        final AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(anims);
        animSet.setStartDelay(ANIMATION_START_DELAY);
        animSet.setDuration(FOLDER_CLOSE_DURATION);
        animSet.setInterpolator(mCloseInterpolator);

        final Listener listener = new Listener() {
            public void onAnimationStart(Animator animation) {
                // hide icons which invisible inside folder icon
                configLayerType(true, folder);
                //folderName.setAlpha(0);
                if (!supportCardIcon) {
                    for (int i = 0, N = Math.min(limit, container.getChildCount()); i < N; i++)
                        ((BubbleTextView) container.getChildAt(i))
                                .setDisableLabel(true);
                }
                folder.setState(Folder.STATE_ANIMATING);
            }

            public void onAnimationEnd(Animator animation) {
                /* YUNOS BEGIN */
                //## modules(Home Shell): [Folder]
                //## date: 2016/03/25 ## author: wangye.wy
                //## BugID: 8064745: reduce gap between icons
                if (!supportCardIcon) {
                    folder.setFolderLayout(true);
                }
                /* YUNOS END */
                folder.switchToPage(0, false);
                folder.onCloseComplete();
                launcher.getDragLayer().setBackground(null);
                folderIcon.setHideIcon(false);
                folderIcon.disableIconBitmapCache();
                resetAnimatedView(folder, page, hotseat, indicator);
                folderName.setAlpha(1);
                folderIndicator.setAlpha(1);
                removeEditFolderShortcut(folder.getInfo());
                configLayerType(false, page,folder, hotseat, indicator);
                // enable icons which invisible inside folder icon
                for (View v : hideTarget) {
                    v.setAlpha(1.0f);
                }
                for (View v : animatedTarget) {
                    v.setAlpha(1.0f);
                }
                if (!supportCardIcon) {
                    for (int i = 0, N = Math.min(limit, container.getChildCount()); i < N; i++)
                        ((BubbleTextView) container.getChildAt(i))
                                .setDisableLabel(false);
                }
                mCurAnim = null;
                mFolder = null;
                folder.setState(Folder.STATE_NONE);
                configLayerType(true, page);
                hotseat.afterShowHotseat();
                /* YUNOS BEGIN */
                //## modules(Home Shell): [Folder]
                //## date: 2016/01/20 ## author: wangye.wy
                //## BugID: 7776275: delay replacing folder with final icon
                mAnimatingClosed = false;
                folderIcon.performDestroy();
                /* YUNOS END */
            }
        };
        animSet.addListener(listener);

        if (anim) {
            if (DEBUG_THUMBNAIL) {
                listener.onAnimationStart(animSet);
                mFolder.postDelayed(new Runnable() {
                    public void run() {
                        listener.onAnimationEnd(animSet);
                    }
                }, 2000);
            } else {
                try {
                    (mCurAnim = animSet).start();
                } catch (NullPointerException e) {
                    listener.onAnimationEnd(animSet);
                }
            }
        } else {
            listener.onAnimationEnd(animSet);
        }
    }

    public void clearAnimation() {
        if (mFolder != null) {
            if (mPreDrawListener != null) {
                mFolder.getViewTreeObserver().removeOnPreDrawListener(mPreDrawListener);
                mPreDrawListener = null;
            }
            mPreDrawListener = null;
            if (mCurAnim != null)
                mCurAnim.cancel();
            if (mFolder != null) {
                final Folder folder = mFolder;
                final Launcher launcher = folder.mLauncher;
                final Hotseat hotseat = launcher.getHotseat();
                final View indicator = launcher.getIndicatorView();
                final CellLayout page = (CellLayout) launcher.getWorkspace().getPageAt(launcher.getCurrentWorkspaceScreen());
                launcher.getDragLayer().setBackground(null);
                resetAnimatedView(folder, page, hotseat, indicator);
                folder.getmFolderIcon().setHideIcon(false);
                folder.onCloseComplete();
                folder.setState(Folder.STATE_NONE);
            }
        }
    }

    private void resetAnimatedView(View... vs) {
        for (View v : vs) {
            // check before reset, cause some view may be null
            if (v == null)
                continue;
            v.setAlpha(1);
        }
    }

    private void configLayerType(boolean hardware, View... vs) {
        View v;
        if (hardware) {
            for (int i = 0, N = vs.length; i < N; i++) {
                v = vs[i];
                v.setLayerType(LAYER_TYPE_HARDWARE, null);
                try {
                    v.buildLayer();
                } catch (IllegalStateException e) {
                    // if view hasn't attached to Window, it'll cause
                    // IllegalStateException
                }
            }
        } else {
            for (int i = 0, N = vs.length; i < N; i++) {
                v = vs[i];
                v.setLayerType(LAYER_TYPE_NONE, null);
            }
        }
    }

    public abstract static class Listener implements AnimatorListener {
        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
        }

        public void onAnimationCancel(Animator animation) {
        }

        public void onAnimationRepeat(Animator animation) {
        }
    }

    
    public static void addEditFolderShortcut(Launcher mLauncher, FolderIcon icon) {
        if (HomeShellSetting.getFreezeValue(LauncherApplication.getContext())
                || AgedModeUtil.isAgedMode()) {
            return;
        }
    	FolderInfo info = icon.getFolderInfo();
    	if(!info.isEditFolderInContents() && !icon.getFolder().isFull()) {
    		if(info.getmEditFolderShortcutInfo() == null) {
    			info.setmEditFolderShortcutInfo(createEditFolderShortcut(mLauncher));
    		}
    		info.getmEditFolderShortcutInfo().cellX = -1;
    		info.getmEditFolderShortcutInfo().cellY = -1;
    		info.add(info.getmEditFolderShortcutInfo());
    	}
    }

    
    public static void removeEditFolderShortcut(FolderInfo info) {
    	if(info == null) {
    		return;
    	}
    	if(info.isEditFolderInContents()) {
    		info.remove(info.getmEditFolderShortcutInfo());
    	}
    }
    private static ShortcutInfo createEditFolderShortcut(Launcher launcher) {
    	ShortcutInfo info = new ShortcutInfo();
    	info.itemFlags = Favorites.ITEM_FLAGS_EDIT_FOLDER;
        info.setIcon(launcher.getResources().getDrawable(R.drawable.files_add_icon));
    	return info;
    }

    public static int getFolderCloseDuration() {
        return FOLDER_CLOSE_DURATION + ANIMATION_START_DELAY;
    }

    public static boolean animatingClosed() {
        return mAnimatingClosed;
    }
}
