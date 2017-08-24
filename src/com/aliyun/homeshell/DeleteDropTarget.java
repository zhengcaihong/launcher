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

import java.util.HashMap;
import java.util.Map;

import app.aliyun.v3.gadget.GadgetView;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.aliyun.homeshell.AppDownloadManager.AppDownloadStatus;
import com.aliyun.homeshell.CellLayout.LayoutParams;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.appclone.AppCloneManager;
import com.aliyun.homeshell.appclone.AppCloneManager.AppCloneCallback;
import com.aliyun.homeshell.hideseat.AppFreezeUtil;
import com.aliyun.homeshell.hideseat.Hideseat;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.icon.IconManager;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.setting.HomeShellSetting;
import com.aliyun.profilemanager.IAddAppCallback;

public class DeleteDropTarget extends ButtonDropTarget {
    private static int FLING_DELETE_ANIMATION_DURATION = 100;
    private static int FLING_MOVE_ANIMATION_DURATION = 10;
    private static float FLING_TO_DELETE_FRICTION = 0.035f;
    private static int MODE_FLING_DELETE_TO_TRASH = 0;
    private static int MODE_FLING_DELETE_ALONG_VECTOR = 1;

    private final int mFlingDeleteMode = MODE_FLING_DELETE_ALONG_VECTOR;

    /* private ColorStateList mOriginalTextColor; */
    private TransitionDrawable mCurrentDrawable;
    
    private Vibrator mVibrator;
    private boolean mDragEntered = false;
    private DragView mAppCloneDragView;
    private boolean mCloneAminPlaying;

    public DeleteDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeleteDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources r = getResources();
        mCurrentDrawable = (TransitionDrawable) r.getDrawable(R.drawable.remove_target_selector);
        /* YUNOS BEGIN */
        // ##date:2015/3/10 ##author:zhanggong.zg ##BugID:5816533
        // enable red hover color in delete-zone
        mHoverColor = r.getColor(R.color.delete_target_hover_tint);
        mCloneHoverColor = r.getColor(R.color.clone_target_hover_tint);
        /* YUNOS END */
/*
        // Get the drawable
        mOriginalTextColor = getTextColors();

        // Get the hover color
        mHoverColor = r.getColor(R.color.delete_target_hover_tint);
        mUninstallDrawable = (TransitionDrawable) 
                r.getDrawable(R.drawable.uninstall_target_selector);
        mRemoveDrawable = (TransitionDrawable) r.getDrawable(R.drawable.remove_target_selector);

        mRemoveDrawable.setCrossFadeEnabled(true);
        mUninstallDrawable.setCrossFadeEnabled(true);

        // The current drawable is set to either the remove drawable or the uninstall drawable 
        // and is initially set to the remove drawable, as set in the layout xml.

        // Remove the text in the Phone UI in landscape
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!LauncherApplication.isScreenLarge()) {
                setText("");
            }
        }*/
    }

    private boolean isAllAppsApplication(DragSource source, Object info) {
        return /*(source instanceof AppsCustomizePagedView) && commented by xiaodong.lxd*/(info instanceof ApplicationInfo);
    }
//    private boolean isAllAppsWidget(DragSource source, Object info) {
//        if (source instanceof AppsCustomizePagedView) {
//            if (info instanceof PendingAddItemInfo) {
//                PendingAddItemInfo addInfo = (PendingAddItemInfo) info;
//                switch (addInfo.itemType) {
//                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
//                    case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
//                        return true;
//                }
//            }
//        }
//        return false;
//    }
    private boolean isDragSourceWorkspaceOrFolder(DragObject d) {
        return (d.dragSource instanceof Workspace) || (d.dragSource instanceof Folder);
    }
    private boolean isWorkspaceOrFolderApplication(DragObject d) {
        return isDragSourceWorkspaceOrFolder(d) && (d.dragInfo instanceof ShortcutInfo);
    }
    private boolean isWorkspaceOrFolderWidget(DragObject d) {
        return isDragSourceWorkspaceOrFolder(d) && (d.dragInfo instanceof LauncherAppWidgetInfo);
    }
    private boolean isWorkspaceFolder(DragObject d) {
        return (d.dragSource instanceof Workspace) && (d.dragInfo instanceof FolderInfo);
    }
   
    /* YUNOS BEGIN */
    // ##gadget
    // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com ##BugID:96378
    private boolean isWorkspaceGadget(DragObject d) {
        return (d.dragSource instanceof Workspace) && (d.dragInfo instanceof GadgetItemInfo);
    }

    /* YUNOS END */

    private boolean isHideseatApplication(DragObject d) {
        return (d.dragSource instanceof Hideseat) && (d.dragInfo instanceof ShortcutInfo);
    }

    private void setHoverColor() {
        mCurrentDrawable.startTransition(mTransitionDuration);
        setTextColor(mHoverColor);
    }
    private void resetHoverColor() {
        mCurrentDrawable.resetTransition();
        /* setTextColor(mOriginalTextColor); */
    }

    @Override
    public boolean acceptDrop(DragObject d) {
        // We can remove everything including App shortcuts, folders, widgets,
        // etc.
        if (isAppCloneTarget()) {
            if (acceptCloneDrop(d)) {
                return true;
            } else {
                String packageName = null;
                if (d.dragInfo instanceof ShortcutInfo) {
                    packageName = ((ShortcutInfo) d.dragInfo).getPackageName();
                }
                if (LauncherModel.IsInstallingApp()) {
                    Toast.makeText(getContext(), R.string.appclone_app_installing,
                            Toast.LENGTH_LONG).show();
                } else if (packageName != null && AppCloneManager.isClonable(packageName)
                        && AppFreezeUtil.isPackageFrozen(getContext(), packageName)) {
                    Toast.makeText(getContext(), R.string.appclone_hint_clone_freezed,
                            Toast.LENGTH_LONG).show();
                } else if (packageName != null
                        && AppCloneManager.isClonable(packageName)
                        && ((ShortcutInfo) d.dragInfo).getAppDownloadStatus() != AppDownloadStatus.STATUS_NO_DOWNLOAD
                        && ((ShortcutInfo) d.dragInfo).getAppDownloadStatus() != AppDownloadStatus.STATUS_INSTALLED) {
                    Toast.makeText(getContext(), R.string.appclone_hint_clone_updating,
                            Toast.LENGTH_LONG).show();
                }
                mLauncher.getSearchBar().getTrashAnimatorSet(false, true).start();
                mLauncher.getWorkspace().checkAndRemoveEmptyCell();
                return false;
            }
        } else if (!d.isFlingToMove){
            if (d.dragInfo instanceof ShortcutInfo) {
                ShortcutInfo shortcutInfo = (ShortcutInfo) d.dragInfo;
                if (((mLauncher.mAppCloneManager != null
                        && mLauncher.mAppCloneManager.hasCloneBody(shortcutInfo.getPackageName()))
                        || AppCloneManager.isCloneShortcutInfo(shortcutInfo))
                        && ((ShortcutInfo) d.dragInfo).getAppDownloadStatus() != AppDownloadStatus.STATUS_NO_DOWNLOAD
                        && ((ShortcutInfo) d.dragInfo).getAppDownloadStatus() != AppDownloadStatus.STATUS_INSTALLED) {
                    Toast.makeText(getContext(), R.string.appclone_hint_uninstall_updating,
                            Toast.LENGTH_LONG).show();
                    mLauncher.getSearchBar().getTrashAnimatorSet(false, false).start();
                    mLauncher.getWorkspace().checkAndRemoveEmptyCell();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
       /* boolean isVisible = true;
        boolean isUninstall = false;*/
        if(mLauncher.isItemUnDeletable(info) && !isCloneable(info)) {
            return;
        }
        mActive = true;
        mCloneAminPlaying = false;

        if (mDragCallback != null) {
            mDragCallback.dragStartCB(this, source, info);
        }

        /*
        // If we are dragging a widget from AppsCustomize, hide the delete target
        if (isAllAppsWidget(source, info)) {
            isVisible = false;
        }

        // If we are dragging an application from AppsCustomize, only show the control if we can
        // delete the app (it was downloaded), and rename the string to "uninstall" in such a case
        if (isAllAppsApplication(source, info)) {
            ApplicationInfo appInfo = (ApplicationInfo) info;
            if ((appInfo.flags & ApplicationInfo.DOWNLOADED_FLAG) != 0) {
                isUninstall = true;
            } else {
                isVisible = false;
            }
        }

        if (isUninstall) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(mUninstallDrawable, null, null, null);
        } else {
            setCompoundDrawablesRelativeWithIntrinsicBounds(mRemoveDrawable, null, null, null);
        }
        mCurrentDrawable = (TransitionDrawable) getCurrentDrawable();

        mActive = isVisible;
        resetHoverColor();
        ((ViewGroup) getParent()).setVisibility(isVisible ? View.VISIBLE : View.GONE);
        if (getText().length() > 0) {
            setText(isUninstall ? R.string.delete_target_uninstall_label
                : R.string.delete_target_label);
        }*/
    }

    @Override
    public void onDragEnd() {
        super.onDragEnd();
        mActive = false;
        if (mDragCallback != null) {
            mDragCallback.dragEndCB(this);
        }
        if (mAppCloneDragView != null && !mCloneAminPlaying) {
            ViewGroup parent = (ViewGroup) mAppCloneDragView.getParent();
            if (parent != null) {
                parent.removeView(mAppCloneDragView);
            }
            mAppCloneDragView = null;
        }
    }

    public void onDragEnter(DragObject d) {
        /*YUNO BEGIN lxd #123430*/
        if(mLauncher.isItemUnDeletable(d.dragInfo)) {
            return;
        }
        /*YUNOS END lxd*/
        super.onDragEnter(d);
        if (mDragCallback != null) {
            mDragCallback.dragEnterCB(this, d);
        }
        if (isDeleteTarget()) {
            d.dragView.setColor(mHoverColor);
            setHoverColor();
        } else {
            d.dragView.setColor(mCloneHoverColor);
        }
        if(!mDragEntered) {
            mDragEntered = true;
            /* YUNOS BEGIN */
            //##date:2013/12/4 ##author:xiaodong.lxd
            //add vibrator alert
            if(mVibrator == null)
                mVibrator = (Vibrator)mLauncher.getSystemService(Context.VIBRATOR_SERVICE);
            long [] pattern = {300,100};
            mVibrator.vibrate(pattern,-1);
            /* YUNOS END */
        }
        if (isAppCloneTarget() && acceptCloneDrop(d)) {
            d.dragView.buildDrawingCache();
            Bitmap bmp = d.dragView.getDrawingCache();
            mAppCloneDragView = new DragView(mLauncher, bmp, (int)d.dragView.getX(), (int)d.dragView.getY(), 0, 0, bmp.getWidth(), bmp.getHeight(), 1.0f, false);
            //mAppCloneDragView = new ImageView(getContext());
            //mAppCloneDragView.setImageBitmap(bmp);
            ViewGroup vg = (ViewGroup)d.dragView.getParent();
            vg.addView(mAppCloneDragView);
            mAppCloneDragView.setX(d.dragView.getX()+20);
            mAppCloneDragView.setY(d.dragView.getY()+40);
            d.dragView.setOnMoveListener(new DragView.OnMoveListener() {

                @Override
                public void onMove(View dragView, int touchX, int touchY) {
                    if (mAppCloneDragView != null) {
                        mAppCloneDragView.setX(dragView.getX()+20);
                        mAppCloneDragView.setY(dragView.getY()+40);
                    }
                }
            });
        }
    }

    public void onDragExit(DragObject d) {
        super.onDragExit(d);
        if(mLauncher.isItemUnDeletable(d.dragInfo)) {
            return;
        }
        if (!d.dragComplete) {
            if (mDragCallback != null) {
                mDragCallback.dragExitCB(this, d);
            }
            d.dragView.setColor(0);
            resetHoverColor();
        } else {
            // Restore the hover color if we are deleting
            if (isDeleteTarget()) {
                d.dragView.setColor(mHoverColor);
            } else {
                d.dragView.setColor(mCloneHoverColor);
            }
        }
        /* YUNOS BEGIN */
        //##date:2013/12/4 ##author:xiaodong.lxd
        //cancel vibrator when exit this target
        if(mDragEntered) {
            mDragEntered = false;
            mVibrator.cancel();
        }
        /* YUNOS END */
        if (mAppCloneDragView != null) {
//            mAppCloneDragView.setAlpha(0.0f);
            ViewGroup parent = (ViewGroup)mAppCloneDragView.getParent();
            if (parent != null) {
                parent.removeView(mAppCloneDragView);
            }
            mAppCloneDragView = null;
         }
    }

    private void animateToTrashAndCompleteDrop(final DragObject d) {
        DragLayer dragLayer = mLauncher.getDragLayer();
        Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);
        /* YUNOS BEGIN */
        // ##date:2013/12/23 ##author:yaodi.yd
        // optimize the uninstalling process
        // Rect to = getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight(),
        // mCurrentDrawable.getIntrinsicWidth(), mCurrentDrawable.getIntrinsicHeight());
        Resources r = mLauncher.getResources();
        int left = r.getDimensionPixelSize(R.dimen.delete_droptarget_padding);
	    int top = r.getDimensionPixelSize(R.dimen.delete_droptarget_padding);
	    int right = left + mCurrentDrawable.getIntrinsicWidth();
	    int bottom = top + mCurrentDrawable.getIntrinsicHeight();
        Rect to = new Rect(left, top, right, bottom);
        /* YUNOS END */
        float scale = (float) to.width() / from.width();

        mSearchDropTargetBar.deferOnDragEnd();

        super.onDragExit(d);

        final Bitmap dragView = buildCacheBitmap(d);

        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
                mSearchDropTargetBar.onDragEnd();
                completeDrop(d, dragView);
            }
        };
        ItemInfo item = (ItemInfo) d.dragInfo;
		if (item.isDeletable())
			dragLayer.animateView(d.dragView, from, to, scale, 1f, 1f, 1f, 1f,
					0, new DecelerateInterpolator(2),
					new LinearInterpolator(), onAnimationEndRunnable,
					DragLayer.ANIMATION_END_DISAPPEAR, null);
		else {
			to = getIconRect(d.dragView.getMeasuredWidth(),
					d.dragView.getMeasuredHeight(),
					mCurrentDrawable.getIntrinsicWidth(), mCurrentDrawable.getIntrinsicHeight());
			dragLayer.animateView(d.dragView, from, to, scale, 1f, 1f, 1f,
					1f, 0,
					new DecelerateInterpolator(2), new LinearInterpolator(),
					onAnimationEndRunnable, DragLayer.ANIMATION_END_DISAPPEAR,
					null);
		}
        /* YUNOS END */
    }

    private Bitmap buildCacheBitmap(DragObject d) {
        View v = null;
        int textColor = -1;
        int shadowColor = -1;
        float shadowDx = -1;
        float shadowDy = -1;
        float shadowRadius = -1;
        BubbleTextView bubble = null;

        /* YUNOS BEGIN */
        // ##date:2014/08/18 ##author:zhanggong.zg ##BugID:5193378
        // When the DragObject comes from hide-seat, the dragged view cannot
        // be retrieved from workspace.
        if (d.dragSource instanceof Hideseat) {
            // retrieve icon bitmap from hide-seat
            v = mLauncher.getHideseat().getDragView();
            if (v instanceof BubbleTextView) {
                bubble = (BubbleTextView) v;
            } else {
                v = d.dragView;
            }
        } else {
            // retrieve icon bitmap from workspace
            CellLayout.CellInfo cellInfo = mLauncher.getWorkspace().getDragInfo();
            if (cellInfo != null) {
                View cell = cellInfo.cell;
                if (cell instanceof BubbleTextView) {
                    v = cell;
                    bubble = (BubbleTextView) v;
                } else if (cell instanceof FolderIcon) {
                    v = cell;
                    bubble = ((FolderIcon) v).getTitleText();
                }
            }
        }
        /* YUNOS END */
        /* YUNOS BEGIN */
        // ##date:2014/09/8 ##author:xindong.zxd ##BugID:5224576
        // remove the application, pops the top frame icon text color is not the same when it on the desktop.
        // ##date:2014/10/11 ##author:zhanggong.zg ##BugID:5248182,5325502
        // For card-icons, the text color won't be modified; For hotseat and hideseat icons, the text color
        // will be temporarily changed, to ensure high contrast between text and background.
        // final boolean customizeColor = bubble != null && !(v instanceof
        // FolderIcon) &&
        // (bubble.isInHotseat() || /* hotseat and hideseat icon */
        // !bubble.isSupportCard()); /* not big card*/
        IconManager im = ((LauncherApplication) LauncherApplication.getContext()).getIconManager();
        boolean fadingEffect = false;
        final boolean customizeColor = bubble != null
                && (!im.supprtCardIcon() || (!(v instanceof FolderIcon) && bubble.getMode().isHotseatOrHideseat()));
        if (customizeColor) {
            textColor = bubble.getCurrentTextColor();
            shadowColor = bubble.getShadowColor();
            shadowDx = bubble.getShadowDx();
            shadowDy = bubble.getShadowDy();
            shadowRadius = bubble.getShadowRadius();

            int uninstallTextColor = getResources().getColor(R.color.common_text_color_uninstall_title);
            bubble.setTextColor(uninstallTextColor);
            bubble.setShadowLayer(0.0f, 0.0f, 0.0f, uninstallTextColor);
            fadingEffect = bubble.isFadingEffectEnable();
            bubble.setFadingEffectEnable(false, false);
        }
        /* YUNOS END */
        if (v == null) {
            v = d.dragView;
        }

        boolean enabled = v.isDrawingCacheEnabled();
        if (!enabled) {
            v.setDrawingCacheEnabled(true);
        }
        v.destroyDrawingCache();
        Bitmap cache = v.getDrawingCache();

        if (cache != null) {
            cache = Bitmap.createBitmap(cache);
        }

        if (!enabled) {
            v.setDrawingCacheEnabled(false);
        }
        /* YUNOS BEGIN */
        // ##date:2014/09/12 ##author:zhanggong ##BugID:5244146
        // set properties back to previous values
        if (customizeColor) {
            bubble.setTextColor(textColor);
            bubble.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
            bubble.setFadingEffectEnable(fadingEffect, false);
        }
        /* YUNOS END */
        return cache;
    }

    private void startAppCloneDropedAnimation(final DragObject d) {
        int[] loc = null;
        Log.i(TAG, "startAppCloneDropedAnimation: x:"+d.x+" y:"+d.y+" xOffset:"+d.xOffset+" yOffset:"+d.yOffset);
        long id = -1;
        ViewGroup parent = null;
        View targetView = null;
        if (d.dragSource instanceof Folder) {
            Folder folder = (Folder) d.dragSource;
            id = folder.mInfo.id;
            if (folder.mInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                parent = mLauncher.getHotseat().getCellLayout();
            } else {
                parent = (CellLayout) mLauncher.getWorkspace().getChildAt(
                        mLauncher.getCurrentScreen());
            }
        } else if (d.dragInfo instanceof ShortcutInfo
                && ((ShortcutInfo) d.dragInfo).container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            id = ((ItemInfo) d.dragInfo).id;
            parent = mLauncher.getHotseat().getCellLayout();
        } else if (d.dragSource instanceof Workspace) {
            id = ((ItemInfo) d.dragInfo).id;
            parent = (CellLayout) mLauncher.getWorkspace().getChildAt(mLauncher.getCurrentScreen());
        }
        if (id >= 0 && parent != null) {
            if (parent instanceof CellLayout) {
                CellLayout cl = (CellLayout) parent;
                int size = cl.getShortcutAndWidgetContainer().getChildCount();

                for (int i = 0; i < size; i++) {
                    View child = cl.getShortcutAndWidgetContainer().getChildAt(i);
                    if (child.getTag() instanceof ItemInfo && ((ItemInfo) child.getTag()).id == id) {
                        targetView = child;
                        break;
                    }
                }
            } else {
                int size = parent.getChildCount();

                for (int i = 0; i < size; i++) {
                    View child = parent.getChildAt(i);
                    if (child.getTag() instanceof ItemInfo && ((ItemInfo) child.getTag()).id == id) {
                        targetView = child;
                        break;
                    }
                }
            }
            Runnable completeRunnable = new Runnable() {

                @Override
                public void run() {
                    mCloneAminPlaying = false;
                    CompleteDropClone(d, d.dragView);
                    mSearchDropTargetBar.onDragEnd();
                }
            };
            if (targetView != null || loc != null) {
                mCloneAminPlaying = true;
                if (loc == null) {
                    loc = new int[2];
                    mLauncher.getDragLayer().getLocationInDragLayer(targetView, loc);
                }

                mLauncher.getDragLayer().animateViewIntoPosition(d.dragView,
                        (int) d.dragView.getX(), (int) d.dragView.getY(), loc[0], loc[1], 0.5f,
                        1.0f, 1.0f, 0.2f, 0.2f, completeRunnable,
                        DragLayer.ANIMATION_END_DISAPPEAR, 500, null);
                if (mAppCloneDragView != null) {
                    mAppCloneDragView.setAlpha(1.0f);
                    mLauncher.getDragLayer().animateViewIntoPosition(mAppCloneDragView,
                            (int) mAppCloneDragView.getX(), (int) mAppCloneDragView.getY(), loc[0] + 20,
                            loc[1] + 20, 1, 1.0f, 1.0f, 0.2f, 0.2f, null,
                            DragLayer.ANIMATION_END_DISAPPEAR, 500, null);
                }
            } else {
                completeRunnable.run();
            }
        }

    }

    private void CompleteDropClone(final DragObject d, final DragView dragView) {
        final ItemInfo item = (ItemInfo) d.dragInfo;
        if (item instanceof ShortcutInfo) {
            final ShortcutInfo shortcutInfo = (ShortcutInfo) item;
            final String pkgName = shortcutInfo.intent.getComponent().getPackageName();

            AppCloneCallback cb = new AppCloneCallback() {

                @Override
                public void onCloned(int userId, String packageName, int returnCode) {

                    if (mLauncher == null
                            || mLauncher.getWorkspace() == null
                            || mLauncher.getDragController() == null
                            || mLauncher.getSearchBar() == null) {
                        Log.d(TAG, "onCloned failed because launcher or workspace is null.");
                        return;
                    }
                    if (returnCode == AppCloneManager.EXEC_SUCCESS) {
                        if (AppCloneManager.DEBUG) {
                            Log.i(TAG, "-AppClone- clone success! packageName:" + packageName
                                    + " userId:" + userId);
                        }
                        if (item.container <= 0) { // Workspace or Hotseat
                            boolean fromHotseat = item.container == Favorites.CONTAINER_HOTSEAT;
                            ShortcutInfo clonedShortcut = new ShortcutInfo(shortcutInfo);
                            clonedShortcut.userId = userId;
                            clonedShortcut.container = ItemInfo.NO_ID;
                            FolderIcon fi = mLauncher.addFolder(
                                    mLauncher.getWorkspace().getCurrentDropLayout(),
                                    shortcutInfo.container, shortcutInfo.screen,
                                    shortcutInfo.cellX, shortcutInfo.cellY, clonedShortcut);
                            fi.onDrop(d);
                            fi.addItem(clonedShortcut);
                            final View cell = mLauncher.getWorkspace().getDragItemFromList(item,
                                    false);
                            if (cell != null && cell.getParent() != null) {
                                ViewGroup parent = (ViewGroup) cell.getParent();
                                parent.removeView(cell);
                            }
                            if (dragView != null) {
                                mLauncher.getDragController().onDeferredEndDrag(dragView);
                            }
                            mLauncher.getSearchBar().getTrashAnimatorSet(false, false).start();
                            LauncherModel.clearDragInfo();
                            /* YUNOS BEGIN */
                            // ## date: 2016/06/15 ## author: yongxing.lyx
                            // ## BugID:8402847:reset hotseat after completed.
                            if (fromHotseat) {
                                mLauncher.getHotseat().cleanAndReset();
                            }
                            /* YUNOS END */
                        } else { // Folder
                            ShortcutInfo clonedShortcut = new ShortcutInfo(shortcutInfo);
                            clonedShortcut.userId = userId;
                            clonedShortcut.container = ItemInfo.NO_ID;
                            FolderInfo folderInfo = (FolderInfo) mLauncher.getModel().sBgItemsIdMap
                                    .get(item.container);
                            if (folderInfo != null) {
                                /* YUNOS BEGIN */
                                // ## date: 2016/09/26 ## author: yongxing.lyx
                                // ## BugID:8851868:icon duplicated after cloned.
                                if (!mLauncher.getWorkspace().isShortcutExist(shortcutInfo)) {
                                    mLauncher.reVisibileDraggedItemEx(item, 1, true);
                                }
                                /* YUNOS BEGIN */
                                folderInfo.add(clonedShortcut);
                            } else {
                                /* YUNOS BEGIN */
                                // ## date: 2016/07/29 ## author: yongxing.lyx
                                // ## BugID:8631127:clone lost if drag from a
                                // folder and folder has been removed.
                                Log.w(TAG, "WARNING! CompleteDropClone() can't get folder :"
                                        + item.container);
                                int startScreen = mLauncher.getWorkspace().getIconScreenIndex(
                                        mLauncher.getCurrentScreen());
                                ScreenPosition sp = LauncherModel
                                        .findEmptyCellAndOccupy(startScreen);
                                CellLayout dropLayout = (CellLayout) mLauncher.getWorkspace()
                                        .getChildAt(mLauncher.getCurrentScreen());
                                if (dropLayout != null && sp != null) {
                                    FolderIcon fi = mLauncher.addFolder(dropLayout,
                                            Favorites.CONTAINER_DESKTOP, sp.s, sp.x, sp.y,
                                            clonedShortcut);
                                    fi.onDrop(d);
                                    fi.addItem(clonedShortcut);
                                } else {
                                    Log.e(TAG,
                                            "ERROR! CompleteDropClone() can't get CellLayout to drop, dropLayout:"
                                                    + dropLayout + " ScreenPosition:" + sp);
                                    Log.e(TAG,
                                            "ERROR! CompleteDropClone() faild! we will lost clone: "
                                                    + clonedShortcut);
                                    Log.e(TAG, "ERROR! CompleteDropClone() faild! we will lost : "
                                            + item);
                                }
                                /* YUNOS END */
                            }
                            if (dragView != null) {
                                mLauncher.getDragController().onDeferredEndDrag(dragView);
                            }
                            mLauncher.getSearchBar().getTrashAnimatorSet(false, false).start();
                            LauncherModel.clearDragInfo();
                        }
                        Map<String, String> param = new HashMap<String, String>();
                        param.put("PkgName", packageName);
                        int count = mLauncher.mAppCloneManager.getCloneCount(packageName);
                        param.put("count", Integer.toString(count));
                        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_APP_DUPLICATE_ADD,
                                param);
                    } else {
                        if (AppCloneManager.DEBUG) {
                            Log.i(TAG, "-AppClone- clone failed! packageName:" + packageName
                                    + " userId:" + userId);
                        }
                        if (dragView != null) {
                            mLauncher.getDragController().onDeferredEndDrag(dragView);
                        }
                        mLauncher.reVisibileDraggedItemEx(item, 1, true);
                        LauncherModel.clearDragInfo();
                    }
                    mLauncher.getWorkspace().checkAndRemoveEmptyCell();
                }

            };

            Runnable revertRunnable = new Runnable() {

                @Override
                public void run() {

                    mLauncher.reVisibileDraggedItemEx(item, 1, true);
                    if (dragView != null) {
                        mLauncher.getDragController().onDeferredEndDrag(dragView);
                    }
                }
            };

            if (item instanceof ShortcutInfo
                    && mLauncher.mAppCloneManager != null
                    && mLauncher.mAppCloneManager.canClone((ShortcutInfo) item)) {
                if (!mLauncher.mAppCloneManager.clone((ShortcutInfo) item, cb)) {
                    mLauncher.getHandler().post(revertRunnable);
                    LauncherModel.clearDragInfo();
                    mLauncher.getSearchBar().getTrashAnimatorSet(false, false).start();
                    int errorMsgId = R.string.appclone_err_busy;
                    Toast.makeText(mLauncher, errorMsgId, Toast.LENGTH_LONG).show();
                    mLauncher.getWorkspace().checkAndRemoveEmptyCell();
                    Log.i(TAG, "-AppClone- clone : " + pkgName + " faied!");
                } else {
                    if (AppCloneManager.DEBUG) {
                        Log.i(TAG, "-AppClone- clone : " + pkgName + " in proccess");
                    }
                }
            } else {
                if (AppCloneManager.DEBUG) {
                    Log.i(TAG, "-AppClone- canClone : " + pkgName + " denied!");
                }
                mLauncher.getHandler().post(revertRunnable);
                LauncherModel.clearDragInfo();
                mLauncher.getWorkspace().checkAndRemoveEmptyCell();
                mLauncher.getSearchBar().getTrashAnimatorSet(false, false).start();
                int errorMsgId;
                if (mLauncher.mAppCloneManager != null
                        && mLauncher.mAppCloneManager.reachCountLimit(mLauncher, pkgName)) {
                    errorMsgId = R.string.appclone_err_reach_limit;
                } else {
                    errorMsgId = R.string.appclone_err_other;
                }
                Toast.makeText(mLauncher, errorMsgId, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void completeDrop(final DragObject d, Bitmap dragBitmap) {

        boolean toDelete = true;
        final ItemInfo item = (ItemInfo) d.dragInfo;
        if (AppCloneManager.isCloneShortcutInfo(item)) {
            toDelete = false; // delete item in delete dialog.
            mLauncher.getWorkspace().checkAndRemoveEmptyCell();
            mLauncher.startApplicationUninstallActivity((ShortcutInfo) item, dragBitmap);
        }  else if (isAllAppsApplication(d.dragSource, item)) {
            // Uninstall the application if it is being dragged from AppsCustomize
            toDelete = false; // delete item in delete dialog.
            mLauncher.startApplicationUninstallActivity((ApplicationInfo) item,null,dragBitmap);
        } else if (isWorkspaceOrFolderApplication(d) || isHideseatApplication(d)) {
            /*YUNOS BEGIN*/
            //##date:2013/11/19 ##author:xiaodong.lxd
            //uninstall app and remove other item type 
            mLauncher.getWorkspace().checkAndRemoveEmptyCell();
            if(!item.isDeletable()) {
                toDelete = false;
                mLauncher.reVisibileDraggedItem(item);
            } else {
                if(item.itemType == Favorites.ITEM_TYPE_APPLICATION) {
                    toDelete = false; // delete item in delete dialog.
                    mLauncher.startApplicationUninstallActivity((ShortcutInfo) item, dragBitmap);
                } else {
                    LauncherModel.deleteItemFromDatabase(mLauncher, item);
                    /*YUNOS BEGIN*/
                    //##date:2014/7/2 ##author:yangshan.ys##BugID:134407
                    //call the replace method after fling the downloading app
                    mLauncher.checkAndReplaceFolderIfNecessary(item);
                    /*YUNOS END*/
                    /*YUNOS BEGIN*/
                    //##module(component name)
                    //##date:2013/12/05 ##author:jun.dongj@alibaba-inc.com##BugID:69179
                    //add toast for delete the download app
                    if(item.itemType == Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING){
                        ShortcutInfo info = (ShortcutInfo)item;
                        sendDownloadIconDeleteBroadcastToAppStore(info);
                        mLauncher.showToastMessage(R.string.delete_download_info);
                    }
                    /*YUNOS END*/
                    else if (item.itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT) {
                        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_REMOVE_WIDGET,
                                item instanceof ShortcutInfo && ((ShortcutInfo)item).intent != null ?
                                        ((ShortcutInfo)item).intent.toString() : "");
                    }
                    /*YUNOS END*/

                    /*YUNOS BEGIN lxd : delete the icon directly and notify hotseat to clean cache*/
                    if(mLauncher.isContainerHotseat(item.container)) {
                        mLauncher.getHotseat().onDrop(true, -1, null, null, true);
                    }
                    /*YUNOS END*/
                    LauncherModel.clearDragInfo();
                }
                /*YUNOS END*/
            }
        } else if (isWorkspaceFolder(d)) {
            /*// Remove the folder from the workspace and delete the contents from launcher model
            FolderInfo folderInfo = (FolderInfo) item;
            mLauncher.removeFolder(folderInfo);
            LauncherModel.deleteFolderContentsFromDatabase(mLauncher, folderInfo);*/
            /*YUNOS BEGIN*/
            //##date:2013/11/27 ##author:xiaodong.lxd & yaodi.yd
            //show toast and revisible the folder
            //mLauncher.showToastMessage(R.string.toast_folder_undeletable);
            //mLauncher.reVisibileWorkspaceItem();
            //mLauncher.exitFullScreen();
            mLauncher.getWorkspace().checkAndRemoveEmptyCell();
            mLauncher.dismissFolder((FolderInfo) d.dragInfo, dragBitmap);
            toDelete = false;
            /*YUNOS END*/
        } else if (isWorkspaceOrFolderWidget(d)) {
            // Remove the widget from the workspace
            mLauncher.removeAppWidget((LauncherAppWidgetInfo) item);
            mLauncher.getWorkspace().checkAndRemoveEmptyCell();
            LauncherModel.deleteItemFromDatabase(mLauncher, item);

            final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;
            final LauncherAppWidgetHost appWidgetHost = mLauncher.getAppWidgetHost();
            if (appWidgetHost != null) {
                // Deleting an app widget ID is a void call but writes to disk before returning
                // to the caller...
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                    }
                }.start();
            }
            LauncherModel.clearDragInfo();
            mLauncher.getSearchBar().getTrashAnimatorSet(false, false).start();
        } else if (isWorkspaceGadget(d)) {
            /* YUNOS BEGIN */
            // ##gadget
            // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com
            // ##BugID:96378
            View cell = ((Workspace)d.dragSource).getDragInfo().cell;
            // cleanUp GadgetView immediately
            if (cell instanceof GadgetView)
                ((GadgetView)cell).cleanUp();
            LauncherModel.deleteItemFromDatabase(mLauncher, item);
            mLauncher.getWorkspace().checkAndRemoveEmptyCell();
            /* YUNOS END */
            LauncherModel.clearDragInfo();
            mLauncher.getSearchBar().getTrashAnimatorSet(false, false).start();
        }

        if (toDelete) {
            mLauncher.getWorkspace().removeDragItemFromList(item);
            if (dragBitmap != null) {
                dragBitmap.recycle();
            }
        }
    }

    public void onDrop(DragObject d) {
        /* YUNOS BEGIN */
        // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
        mLauncher.getEditModeHelper().onCellLayoutBeginDrag(-1, false);
        if (d != null) {
            if (isDeleteTarget()) {
                animateToTrashAndCompleteDrop(d);
            } else {
                startAppCloneDropedAnimation(d);
            }
        }
        /* YUNOS END */
        //added by xiaodong.lxd for user track
        if (isDeleteTarget()) {
            String name = "";
            String itemType = "";
            String operate_area = mLauncher.isInLauncherEditMode() ? "menu_arrage" : "launcher";
            if (d != null && d.dragInfo != null) {
                if (d.dragInfo instanceof LauncherAppWidgetInfo
                        && ((LauncherAppWidgetInfo) d.dragInfo).providerName != null) {
                    name = ((LauncherAppWidgetInfo) d.dragInfo).providerName
                            .toString();
                    itemType = "appwidget";
                } else if (d.dragInfo instanceof ItemInfo
                        && ((ItemInfo) d.dragInfo).title != null) {
                    name = ((ItemInfo) d.dragInfo).title.toString();
                    if (d.dragInfo instanceof GadgetItemInfo) {
                        itemType = "gadget";
                    } else if (d.dragInfo instanceof FolderInfo) {
                        itemType = "folder";
                    } else if (((ItemInfo) d.dragInfo).itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                        itemType = "app";
                    } else if (((ItemInfo) d.dragInfo).itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                        itemType = "shortcut";
                    } else if (((ItemInfo) d.dragInfo).itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) {
                        itemType = "downloading";
                    }
                }
            }
            Map<String, String> param = new HashMap<String, String>();
            param.put("itemType", itemType);
            param.put("name", name);
            param.put("operate_area", operate_area);
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_DRAG_TO_DELETE, param);
        }
    }

    /**
     * Creates an animation from the current drag view to the delete trash icon.
     */
    private AnimatorUpdateListener createFlingToTrashAnimatorListener(final DragLayer dragLayer,
            DragObject d, PointF vel, ViewConfiguration config) {
        final Rect to = getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight(),
                mCurrentDrawable.getIntrinsicWidth(), mCurrentDrawable.getIntrinsicHeight());
        final Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);

        // Calculate how far along the velocity vector we should put the intermediate point on
        // the bezier curve
        float velocity = Math.abs(vel.length());
        float vp = Math.min(1f, velocity / (config.getScaledMaximumFlingVelocity() / 2f));
        int offsetY = (int) (-from.top * vp);
        int offsetX = (int) (offsetY / (vel.y / vel.x));
        final float y2 = from.top + offsetY;                        // intermediate t/l
        final float x2 = from.left + offsetX;
        final float x1 = from.left;                                 // drag view t/l
        final float y1 = from.top;
        final float x3 = to.left;                                   // delete target t/l
        final float y3 = to.top;

        final TimeInterpolator scaleAlphaInterpolator = new TimeInterpolator() {
            @Override
            public float getInterpolation(float t) {
                return t * t * t * t * t * t * t * t;
            }
        };
        return new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final DragView dragView = (DragView) dragLayer.getAnimatedView();
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                float tp = scaleAlphaInterpolator.getInterpolation(t);
                float initialScale = dragView.getInitialScale();
                float finalAlpha = 0.5f;
                float scale = dragView.getScaleX();
                float x1o = ((1f - scale) * dragView.getMeasuredWidth()) / 2f;
                float y1o = ((1f - scale) * dragView.getMeasuredHeight()) / 2f;
                float x = (1f - t) * (1f - t) * (x1 - x1o) + 2 * (1f - t) * t * (x2 - x1o) +
                        (t * t) * x3;
                float y = (1f - t) * (1f - t) * (y1 - y1o) + 2 * (1f - t) * t * (y2 - x1o) +
                        (t * t) * y3;

                dragView.setTranslationX(x);
                dragView.setTranslationY(y);
                dragView.setScaleX(initialScale * (1f - tp));
                dragView.setScaleY(initialScale * (1f - tp));
                dragView.setAlpha(finalAlpha + (1f - finalAlpha) * (1f - tp));
            }
        };
    }
    private AnimatorUpdateListener createFlingToMoveAnimatorListener(final DragLayer dragLayer,
            DragObject d, final PointF vel, ViewConfiguration config, final long startTime, final boolean next) {
        final Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);
        final float friction = 1f - (dragLayer.getResources().getDisplayMetrics().density * FLING_TO_DELETE_FRICTION);
        int dWidth = dragLayer.getWidth();
        if(Math.abs(vel.x) < (vel.x > 0 ? dWidth - d.x : d.x))
            vel.x = vel.x > 0 ? dWidth : -dWidth;
        return new AnimatorUpdateListener() {
            private static final float FLING_ROTATION_ANGLE = 90;
            private boolean mHasOffsetForScale;
            private long prevTime = startTime;
            private final TimeInterpolator mAlphaInterpolator = new DecelerateInterpolator(0.3f);
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final DragView dragView = (DragView) dragLayer.getAnimatedView();
                long curTime = AnimationUtils.currentAnimationTimeMillis();
                float t = ((Float) animation.getAnimatedValue()).floatValue();

                if (!mHasOffsetForScale) {
                    mHasOffsetForScale = true;
                    float scale = dragView.getScaleX();
                    float xOffset = ((scale - 1f) * dragView.getMeasuredWidth()) / 2f;
                    float yOffset = ((scale - 1f) * dragView.getMeasuredHeight()) / 2f;

                    from.left += xOffset;
                    from.top += yOffset;
                }

                from.left += (vel.x * (curTime - prevTime) / 1000f);
                from.top += (vel.y * (curTime - prevTime) / 1000f);

                dragView.setTranslationX(from.left);
                dragView.setTranslationY(from.top);
                dragView.setRotation((next ? FLING_ROTATION_ANGLE : -FLING_ROTATION_ANGLE) * t);
                dragView.setAlpha(1f - mAlphaInterpolator.getInterpolation(t));

                vel.x *= friction;
                vel.y *= friction;
                prevTime = curTime;
            }
        };
    }

    /**
     * Creates an animation from the current drag view along its current velocity vector.
     * For this animation, the alpha runs for a fixed duration and we update the position
     * progressively.
     */
    private static class FlingAlongVectorAnimatorUpdateListener implements AnimatorUpdateListener {
        private DragLayer mDragLayer;
        private PointF mVelocity;
        private Rect mFrom;
        private long mPrevTime;
        private boolean mHasOffsetForScale;
        private float mFriction;

        private final TimeInterpolator mAlphaInterpolator = new DecelerateInterpolator(0.75f);

        public FlingAlongVectorAnimatorUpdateListener(DragLayer dragLayer, PointF vel, Rect from,
                long startTime, float friction) {
            mDragLayer = dragLayer;
            mVelocity = vel;
            mFrom = from;
            mPrevTime = startTime;
            mFriction = 1f - (dragLayer.getResources().getDisplayMetrics().density * friction);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            final DragView dragView = (DragView) mDragLayer.getAnimatedView();
            float t = ((Float) animation.getAnimatedValue()).floatValue();
            long curTime = AnimationUtils.currentAnimationTimeMillis();

            if (!mHasOffsetForScale) {
                mHasOffsetForScale = true;
                float scale = dragView.getScaleX();
                float xOffset = ((scale - 1f) * dragView.getMeasuredWidth()) / 2f;
                float yOffset = ((scale - 1f) * dragView.getMeasuredHeight()) / 2f;

                mFrom.left += xOffset;
                mFrom.top += yOffset;
            }

            mFrom.left += (mVelocity.x * (curTime - mPrevTime) / 1000f);
            mFrom.top += (mVelocity.y * (curTime - mPrevTime) / 1000f);

            dragView.setTranslationX(mFrom.left);
            dragView.setTranslationY(mFrom.top);
            dragView.setAlpha(1f - mAlphaInterpolator.getInterpolation(t));

            mVelocity.x *= mFriction;
            mVelocity.y *= mFriction;
            mPrevTime = curTime;
        }
    };
    private AnimatorUpdateListener createFlingAlongVectorAnimatorListener(final DragLayer dragLayer,
            DragObject d, PointF vel, final long startTime, final int duration,
            ViewConfiguration config) {
        final Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);

        return new FlingAlongVectorAnimatorUpdateListener(dragLayer, vel, from, startTime,
                FLING_TO_DELETE_FRICTION);
    }

    public void onFlingToDelete(final DragObject d, int x, int y, PointF vel) {
        mLauncher.getEditModeHelper().onCellLayoutBeginDrag(-1, false);
        // Don't highlight the icon as it's animating
        /* YUNOS BEGIN */
        // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
        if (d != null) {
            d.dragView.setColor(0);
            d.dragView.updateInitialScaleToCurrentScale();
        }
        /* YUNOS END */

        if (mFlingDeleteMode == MODE_FLING_DELETE_TO_TRASH) {
            // Defer animating out the drop target if we are animating to it
            mSearchDropTargetBar.deferOnDragEnd();
            mSearchDropTargetBar.finishAnimations();
        }

        final ViewConfiguration config = ViewConfiguration.get(mLauncher);
        final DragLayer dragLayer = mLauncher.getDragLayer();
        final int duration = FLING_DELETE_ANIMATION_DURATION;
        final long startTime = AnimationUtils.currentAnimationTimeMillis();

        // NOTE: Because it takes time for the first frame of animation to actually be
        // called and we expect the animation to be a continuation of the fling, we have
        // to account for the time that has elapsed since the fling finished.  And since
        // we don't have a startDelay, we will always get call to update when we call
        // start() (which we want to ignore).
        final TimeInterpolator tInterpolator = new TimeInterpolator() {
            private int mCount = -1;
            private float mOffset = 0f;

            @Override
            public float getInterpolation(float t) {
                if (mCount < 0) {
                    mCount++;
                } else if (mCount == 0) {
                    mOffset = Math.min(0.5f, (float) (AnimationUtils.currentAnimationTimeMillis() -
                            startTime) / duration);
                    mCount++;
                }
                return Math.min(1f, mOffset + t);
            }
        };
        AnimatorUpdateListener updateCb = null;
        if (mFlingDeleteMode == MODE_FLING_DELETE_TO_TRASH) {
            updateCb = createFlingToTrashAnimatorListener(dragLayer, d, vel, config);
        } else if (mFlingDeleteMode == MODE_FLING_DELETE_ALONG_VECTOR) {
            updateCb = createFlingAlongVectorAnimatorListener(dragLayer, d, vel, startTime,
                    duration, config);
        }
        /* YUNOS BEGIN */
        // ##date:2014/03/01 ##author:yaodi.yd ##BugID:96130
        /* YUNOS BEGIN */
        // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
        //DragView view = null;
        if (d != null) {
            super.onDragExit(d);
            //view = d.dragView;
        }
        /* YUNOS END */

        final Bitmap dragView = buildCacheBitmap(d);
        /* YUNOS END */
        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
                mSearchDropTargetBar.onDragEnd();
                // If we are dragging from AllApps, then we allow AppsCustomizePagedView to clean up
                // itself, otherwise, complete the drop to initiate the deletion process
                completeDrop(d, dragView);
                mLauncher.getDragController().onDeferredEndFling(d);
            }
        };
        /* YUNOS BEGIN */
        // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
        if (d != null) {
            dragLayer.animateView(d.dragView, updateCb, duration, tInterpolator,
                    onAnimationEndRunnable, DragLayer.ANIMATION_END_DISAPPEAR, null);
        }
        /* YUNOS END */
        //added by xiaodong.lxd for user track
        String name = "";
        if (d != null && d.dragInfo != null) {
            if (d.dragInfo instanceof LauncherAppWidgetInfo
                    && ((LauncherAppWidgetInfo) d.dragInfo).providerName != null) {
                name = ((LauncherAppWidgetInfo) d.dragInfo).providerName
                        .toString();
            } else if (d.dragInfo instanceof ItemInfo
                    && ((ItemInfo) d.dragInfo).title != null) {
                name = ((ItemInfo) d.dragInfo).title.toString();
            }
        }
        UserTrackerHelper.sendUserReport(
                UserTrackerMessage.MSG_FLING_TO_DELETE, name);
    }
    public void onFlingToMove(final DragObject d, int x, int y, PointF vel, final boolean next) {
        // Don't highlight the icon as it's animating
        /* YUNOS BEGIN */
        // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
        if (d != null) {
            d.dragView.setColor(0);
            d.dragView.updateInitialScaleToCurrentScale();
        }
        /* YUNOS END */

        final ViewConfiguration config = ViewConfiguration.get(mLauncher);
        final DragLayer dragLayer = mLauncher.getDragLayer();
        final int duration = FLING_MOVE_ANIMATION_DURATION;
        final long startTime = AnimationUtils.currentAnimationTimeMillis();

        // NOTE: Because it takes time for the first frame of animation to actually be
        // called and we expect the animation to be a continuation of the fling, we have
        // to account for the time that has elapsed since the fling finished.  And since
        // we don't have a startDelay, we will always get call to update when we call
        // start() (which we want to ignore).
        final TimeInterpolator tInterpolator = new TimeInterpolator() {
            private int mCount = -1;
            private float mOffset = 0f;

            @Override
            public float getInterpolation(float t) {
                if (mCount < 0) {
                    mCount++;
                } else if (mCount == 0) {
                    mOffset = Math.min(0.5f, (float) (AnimationUtils.currentAnimationTimeMillis() -
                            startTime) / duration);
                    mCount++;
                }
                return Math.min(1f, mOffset + t);
            }
        };
        AnimatorUpdateListener updateCb = null;
        /* YUNOS BEGIN */
        // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
        if (d != null) {
            updateCb = createFlingToMoveAnimatorListener(dragLayer, d, vel, config, startTime, next);
        }
        /* YUNOS END */
        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
                View v = dragLayer.getAnimatedView();
                final float rotation = v.getRotation();
                final float y = v.getY();

                mLauncher.getSearchBar().getTrashAnimatorSet(false, true).start(); // BugID 6750551
                completeMoveDrop(d, next, y, rotation);
                mLauncher.getDragController().onDeferredEndFling(d);
            }
        };
        /* YUNOS BEGIN */
        // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
        if (d != null) {
            dragLayer.animateView(d.dragView, updateCb, duration, tInterpolator,
                    onAnimationEndRunnable, DragLayer.ANIMATION_END_DISAPPEAR, null);
        }
        /* YUNOS END */
      //added by xiaodong.lxd for user track
        String name = "";
        if (d != null && d.dragInfo != null) {
            if (d.dragInfo instanceof LauncherAppWidgetInfo
                    && ((LauncherAppWidgetInfo) d.dragInfo).providerName != null) {
                name = ((LauncherAppWidgetInfo) d.dragInfo).providerName
                        .toString();
            } else if (d.dragInfo instanceof ItemInfo
                    && ((ItemInfo) d.dragInfo).title != null) {
                name = ((ItemInfo) d.dragInfo).title.toString();
            }
        }
        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_FLING_TO_MOVE,
                name);
    }

    private void completeMoveDrop(DragObject d, final boolean next, final float y, final float roatation) {
        final ItemInfo item = (ItemInfo) d.dragInfo;
        boolean isFolder = isWorkspaceFolder(d);
        
        final Workspace workspace = mLauncher.getWorkspace();
        final int indexInIconScreens = workspace.getIconScreenIndex(workspace.getCurrentPage());
        //CellLayout.CellInfo dragInfo = workspace.getDragInfo();
        final View cell = workspace.getDragItemFromList(item, false);
        if (isWorkspaceOrFolderApplication(d) || isFolder) {
            if (cell != null) {
                final ScreenPosition sp = LauncherModel.findEmptyCellForFlingAndOccupy(indexInIconScreens, next ? 1 : 0);
                Log.d(TAG, "sxsexe--------------------->completeMoveDrop item " + item.title + " find sp " + sp + " X " + cell.getX() + " Y " + cell.getY());
                if(sp == null) {
                    CellLayout cellLayout = (CellLayout) workspace.getChildAt(workspace
                            .getRealScreenIndex(indexInIconScreens));
                    Log.d(TAG, "sxsexe------------> fling back " + item.title);
                    workspace.checkAndRemoveEmptyCell();
                    cellLayout.flingBack(cell, roatation, y, next);
                    return;
                } else {
                    int realIndex = workspace.getRealScreenIndex(sp.s);
                    final CellLayout cellLayout = (CellLayout) workspace.getChildAt(realIndex);
                    final ViewParent parent = cell.getParent();
                    if(parent != null && cellLayout != null) {
                        ViewGroup parentView = (ViewGroup)parent;
                        parentView.removeView(cell);
                        LayoutParams lp = (CellLayout.LayoutParams)cell.getLayoutParams();
                        lp.cellX = sp.x;
                        lp.cellY = sp.y;
                        lp.useTmpCoords = false;
                        item.setPosition(sp);
                        int childId = LauncherModel.getCellLayoutChildId(item.container, indexInIconScreens, sp.x, sp.y, 1, 1);
                        cellLayout.addViewToCellLayout(cell, -1, childId, lp, true);
                        LauncherModel.moveItemInDatabase(mLauncher, item, item.container, sp.s, sp.x, sp.y);
                        cell.setVisibility(View.VISIBLE);
                        cellLayout.addPengindFlingDropDownTarget(cell, y, roatation, next, item.title.toString(), sp.x, sp.y);
                        workspace.checkAndRemoveEmptyCell();
                        workspace.mDropTargetView = null;
                    } else {
                        Log.e(TAG, "sxsexe--><><> completeMoveDrop item  " + item.title + " parent is " + parent + " cellLayout is " + cellLayout+",fling back");
                        CellLayout cellLayout1 = (CellLayout) workspace.getChildAt(workspace
                                .getRealScreenIndex(indexInIconScreens));
                        workspace.checkAndRemoveEmptyCell();
                        cellLayout1.flingBack(cell, roatation, y, next);
                    }
                }
                workspace.removeDragItemFromList(item);
                
                //added by xiaodong.lxd for bug#96460
                if(mLauncher.getHotseat().isTouchInHotseat()) {
                    mLauncher.getHotseat().onExitHotseat(false);
                }
                //end#96460
                
            } else {
                mLauncher.reVisibileDraggedItem(item);
            }
            /*} else {
                mLauncher.getWorkspace().checkAndRemoveEmptyCell();
                LauncherModel.deleteItemFromDatabase(mLauncher, item);
            }*/
        }
    }

/*    private void findLastEmptyCell(int[] xy, CellLayout layout) {
        if (layout == null)
            return;
        int cY = layout.getCountY() - 1;
        int cX = layout.getCountX() - 1;
        for (int y = cY; y >= 0; y--) {
            for (int x = cX; x >= 0; x--) {
                if (layout.getChildAt(x, y) != null) {
                    if (x == cX) {
                        if (y == cY) {
                            xy[0] = -1;
                            xy[1] = -1;
                            return;
                        }
                        xy[0] = 0;
                        xy[1] = y + 1;
                    } else {
                        xy[0] = x + 1;
                        xy[1] = y;
                    }
                    return;
                }
            }
        }
        xy[0] = -1;
        xy[1] = -1;
    }

    private void findFirstEmptyCell(int[] xy, CellLayout layout) {
        for (int y = 0, YN = layout.getCountY(); y < YN; y++) {
            for (int x = 0, XN = layout.getCountX(); x < XN; x++) {
                if (layout.getChildAt(x, y) == null) {
                    xy[0] = x;
                    xy[1] = y;
                    return;
                }
            }
        }
        xy[0] = -1;
        xy[1] = -1;
    }*/

    /**
     * when delete downloading icon, send broad cast to AppStore to stop downloading
     * @author wenliang.dwl
     */
    private void sendDownloadIconDeleteBroadcastToAppStore(ShortcutInfo info){
        String pkgName = info.intent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
        Intent intent = new Intent(AppDownloadManager.ACTION_HS_DOWNLOAD_TASK);
        intent.putExtra(AppDownloadManager.TYPE_ACTION, AppDownloadManager.ACTION_HS_DOWNLOAD_CANCEL);
        intent.putExtra(AppDownloadManager.TYPE_PACKAGENAME, pkgName);
        getContext().sendBroadcast(intent);
        LauncherApplication app = (LauncherApplication)getContext().getApplicationContext();
        app.getModel().getAppDownloadManager().updatepPckageDownloadCancelTimeByHS(pkgName);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable[] drawables = getCompoundDrawables();
        if (drawables != null) {
            Drawable left = drawables[0];
            if (left != null) {
                float drawableWidth = left.getIntrinsicWidth();
                if (getPaddingLeft() != 0) {
                    canvas.translate(-getPaddingLeft(), 0);
                }
                canvas.translate((getWidth() - drawableWidth) / 2, 0);
            }
        }
        super.onDraw(canvas);
    }

    public boolean isDeleteTarget() {
        if (getId() == R.id.delete_target_text) {
            return true;
        }
        return false;
    }

    public boolean isAppCloneTarget() {
        if (getId() == R.id.appclone_target_text) {
            return true;
        }
        return false;
    }

    public boolean isCloneable(Object info) {
        if (info instanceof ShortcutInfo) {
            ShortcutInfo scInfo = (ShortcutInfo) info;
            String packageName = scInfo.getPackageName();
            if (scInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                    && mLauncher.mAppCloneManager != null
                    && mLauncher.mAppCloneManager.canClone(packageName)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean acceptCloneDrop(DragObject d) {
        if (LauncherModel.IsInstallingApp()) {
            return false;
        }
        if (d.dragInfo instanceof ShortcutInfo) {
            ShortcutInfo shortcutInfo = (ShortcutInfo) d.dragInfo;
            String packageName = shortcutInfo.getPackageName();
            if (shortcutInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                    && (shortcutInfo.getAppDownloadStatus() == AppDownloadStatus.STATUS_NO_DOWNLOAD
                    || shortcutInfo.getAppDownloadStatus() == AppDownloadStatus.STATUS_INSTALLED)
                    && !AppFreezeUtil.isPackageFrozen(getContext(), shortcutInfo)
                    && !(d.dragSource instanceof Hideseat)
                    && (mLauncher.mAppCloneManager != null 
                            && (mLauncher.mAppCloneManager.canClone(packageName)
                            || mLauncher.mAppCloneManager.hasCloneBody(packageName))
                            || AppCloneManager.isCloneShortcutInfo(shortcutInfo))) {
                return true;
            }
        }

        return false;
    }
}
