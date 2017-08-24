package com.aliyun.homeshell.editmode;

import java.util.HashMap;
import java.util.Map;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import app.aliyun.v3.gadget.GadgetInfo;

import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.DragController;
import com.aliyun.homeshell.DragSource;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.PendingAddGadgetInfo;
import com.aliyun.homeshell.PendingAddItemInfo;
import com.aliyun.homeshell.PendingAddShortcutInfo;
import com.aliyun.homeshell.PendingAddWidgetInfo;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.UserTrackerHelper;
import com.aliyun.homeshell.UserTrackerMessage;
import com.aliyun.homeshell.Workspace;
import com.aliyun.homeshell.editmode.PreviewContainer.PreviewContentType;
import com.aliyun.homeshell.editmode.ThemesPreviewAdapter.ThemeAttr;
import com.aliyun.homeshell.editmode.WallpapersPreviewAdapter.WallpaperAttr;
import com.aliyun.homeshell.themeutils.ThemeUtils;
import com.aliyun.homeshell.utils.Utils;
import android.os.Build;

public class ListViewEventHandler {

    private Launcher mLauncher;
    private DragController mDragController;
    private Canvas mCanvas;
    private Context mContext;
    private DragSource mDragSource;
    private WidgetPreviewLoader mWidgetPreviewLoader;
    
    private static final int OP_SET = 1;
    /*YunOS BEGIN PB*/
    //##module:(homeshell) ##author:baorui.br@alibaba-inc.com
    //##BugID:(8593505) ##date:2016-11-11
    private static final int DISSMISS_DELAY = 500;
    /*YUNOS END PB*/

    public void setup(Launcher launcher, DragController dragController, Canvas canvas, Context context, DragSource dragSource) {
        mLauncher = launcher;
        mDragController = dragController;
        mCanvas = canvas;
        mContext = context;
        mDragSource = dragSource;
        mWidgetPreviewLoader = new WidgetPreviewLoader(launcher);
    }

    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        final Workspace workspace = mLauncher.getWorkspace();
        final Adapter adapter = parent.getAdapter();
        if (adapter instanceof EffectsPreviewAdapter) {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    EffectsPreviewAdapter effectsAdapter = (EffectsPreviewAdapter) adapter;
                    effectsAdapter.setEffectValue(position);
                    effectsAdapter.notifyDataSetChanged();
                    String name = effectsAdapter.getEffectTitle(position);
                    workspace.animateScrollEffect(true);
                    Map<String, String> param = new HashMap<String, String>();
                    param.put("positon", String.valueOf(position));
                    param.put("effects", name);
                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_EFFECTS_SELECT, param);
                }
            };
            if (workspace.isPageMoving()) {
                workspace.runOnPageStopMoving(r);
            } else {
                r.run();
            }
        } else if (adapter instanceof WidgetPreviewAdapter) {
            final Object info = adapter.getItem(position);
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    final int currentScreen = workspace.getCurrentPage();
                    CellLayout layout = (CellLayout) workspace.getChildAt(currentScreen);

                    if (info instanceof AppWidgetProviderInfo) {
                        AppWidgetProviderInfo providerInfo = (AppWidgetProviderInfo) info;
                        mLauncher.addAppWidget(providerInfo, -1);
                    } else if (info instanceof GadgetInfo) {
                        GadgetInfo gadgetInfo = (GadgetInfo) info;
                        mLauncher.addGadgetWidget(gadgetInfo, -1);
                    } else if (info instanceof ResolveInfo) {
                        if ((layout != null)) {
                            if (layout.checkSpaceAvailable(new ShortcutInfo(), Configuration.ORIENTATION_UNDEFINED)) {
                                ResolveInfo resolveInfo = (ResolveInfo) info;
                                mLauncher.addShortcut(resolveInfo.activityInfo, -1);
                            } else {
                                mLauncher.showOutOfSpaceMessage(mLauncher.isHotseatLayout(layout));
                            }
                        } else {
                            mLauncher.showOutOfSpaceMessage(mLauncher.isHotseatLayout(layout));
                        }
                    }
                    if (layout != null && layout.hasChild()) {
                        layout.removeEditBtnContainer();
                    }
                }
            };
            if (workspace.isPageMoving()) {
                workspace.runOnPageStopMoving(r);
            } else {
                mLauncher.postRunnableToMainThread(r, 0);
            }
        } else if (adapter instanceof WallpapersPreviewAdapter) {
            String name = "";
            if (position == 0 && !ConfigManager.isLandOrienSupport()) {
                Intent intent = new Intent();
                intent.putExtra("fromEntry", "menu");
                intent.setAction("com.aliyun.auitheme.action.VIEW");
                intent.addCategory("com.aliyun.auitheme.category.WALLPAPERMANAGER");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    mContext.startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    ex.printStackTrace();
                }
                name = "online";
            } else {
                WallpapersPreviewAdapter wallpaperAdapter = (WallpapersPreviewAdapter) adapter;
                WallpaperAttr attr = (WallpaperAttr) wallpaperAdapter.getItem(ConfigManager.isLandOrienSupport() ? position : position - 1);
                if (attr != null) {
                    if (wallpaperAdapter.setCheckedItem(attr)) {
                        /*YunOS BEGIN PB*/
                        //##module:(homeshell) ##author:baorui.br@alibaba-inc.com
                        //##BugID:(8593505) ##date:2016-11-11
                        Utils.showLoadingDialog(mLauncher, R.string.theme_loading);
                        Runnable dis = new Runnable() {
                            public void run() {
                                Utils.dismissLoadingDialog();
                            }
                        };
                        mLauncher.postRunnableToMainThread(dis, DISSMISS_DELAY);
                        /*YunOS END PB*/
                        Intent intent = new Intent("com.yunos.theme.thememanager.ACTION_MANAGE_THEME");
                        intent.putExtra("type", "wallpaper");
                        intent.putExtra("is_system", attr.isSystem);
                        /* YUNOS BEGIN PB */
                        // ##modules(HomeShell):
                        // ##author:xy83652@alibaba-inc.com
                        // ##BugID:(5927523) ##date:2015/4/26
                        // ##description: use key id only
                        intent.putExtra("kid", attr.keyID);
                        /* YUNOS END PB */
                        intent.putExtra("operation", OP_SET);
                        name = attr.id;
                        //BugID:8364585:Service Intent must be explicit.
                        intent.setPackage("com.yunos.theme.thememanager");
                        mContext.startService(intent);
                        Map<String, String> param = new HashMap<String, String>();
                        param.put("positon", String.valueOf(position));
                        param.put("wallpaper", name);
                        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_WALLPAPER_SELECT, param);
                    }
                }
            }
        } else if (adapter instanceof ThemesPreviewAdapter) {
            String name = "";
            ThemesPreviewAdapter themeAdapter = (ThemesPreviewAdapter) adapter;
            if (position == 0 && !ConfigManager.isLandOrienSupport()) {
                Intent intent = new Intent("com.aliyun.auitheme.action.VIEW");
                intent.putExtra("fromEntry", "menu");
                if (themeAdapter.getCount() <= 1) {// chenjian added to adapter
                                                   // for low version AliTheme
                    intent.addCategory("com.aliyun.auitheme.category.THEMEMANAGER");
                } else {
                    intent.addCategory("com.aliyun.auitheme.category.LOCALTHEME");
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                name = "online";
                try {
                    mContext.startActivity(intent);
                } catch (Exception e) {
                }
            } else {
                ThemeAttr attr = (ThemeAttr) themeAdapter.getItem(ConfigManager.isLandOrienSupport() ? position : position - 1);
                if (attr != null) {
                    if (themeAdapter.setCheckedItem(attr)) {
                        EditModeHelper.setChangeThemeFromeHomeShell(true);
                        Utils.showLoadingDialog(mLauncher, R.string.theme_loading);
                        Intent intent = new Intent("com.yunos.theme.thememanager.ACTION_MANAGE_THEME");
                        intent.putExtra("type", "theme");
                        intent.putExtra("package_name", attr.packageName);
                        intent.putExtra("operation", OP_SET);
                        //BugID:8364585:Service Intent must be explicit.
                        intent.setPackage("com.yunos.theme.thememanager");
                        mContext.startService(intent);
                        name = attr.name;
                        Map<String, String> param = new HashMap<String, String>();
                        param.put("positon", String.valueOf(position));
                        param.put("theme", name);
                        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_THEME_SELECT, param);
                    }
                }
            }
        } else if (adapter instanceof CelllayoutPreviewAdapter) {
            if (position != mLauncher.getCurrentScreen()) {
                workspace.snapToPage(position);
            }
        }
    }

    private boolean beginDraggingWidget(View view, Object itemInfo) {
        // Get the widget preview as the drag representation
        ImageView image = (ImageView) view.findViewById(R.id.preview_image);
        // ImageView image = (ImageView) v.findViewById(R.id.widget_preview);
        // PendingAddItemInfo createItemInfo = (PendingAddItemInfo) v.getTag();

        // If the ImageView doesn't have a drawable yet, the widget preview
        // hasn't been loaded and
        // we abort the drag.
        if (image.getDrawable() == null) {
            return false;
        }

        // Compose the drag image
        Bitmap preview;
        Bitmap outline;
        float scale = 1f;
        Point previewPadding = null;
        PendingAddItemInfo createItemInfo = null;
        if (itemInfo instanceof AppWidgetProviderInfo) {
            // This can happen in some weird cases involving multi-touch. We
            // can't start dragging
            // the widget if this is null, so we break out.
            createItemInfo = new PendingAddWidgetInfo((AppWidgetProviderInfo) itemInfo, null, null);

            PendingAddWidgetInfo createWidgetInfo = (PendingAddWidgetInfo) createItemInfo;
            createItemInfo = createWidgetInfo;
            int[] spanXY = Launcher.getSpanForWidget(mLauncher, (AppWidgetProviderInfo) itemInfo);
            createItemInfo.spanX = spanXY[0];
            createItemInfo.spanY = spanXY[1];
            int spanX = createItemInfo.spanX;
            int spanY = createItemInfo.spanY;
            int[] minSpanXY = Launcher.getMinSpanForWidget((Context) mLauncher, (AppWidgetProviderInfo) itemInfo);
            createItemInfo.minSpanX = minSpanXY[0];
            createItemInfo.minSpanY = minSpanXY[1];
            createItemInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET;
            int[] size = mLauncher.getWorkspace().estimateItemSize(spanX, spanY, createWidgetInfo, true);

            int[] previewSizeBeforeScale = new int[1];
            /* YUNOS BEGIN */
            // ##date:2015/6/23 ##author:zhanggong.zg ##BugID:6091020
            preview = mWidgetPreviewLoader.generateWidgetPreview(createWidgetInfo.componentName, createWidgetInfo.previewImage,
                    ((AppWidgetProviderInfo) itemInfo).icon, spanX, spanY, size[0], size[1], null, previewSizeBeforeScale);
            /* YUNOS END */

            if (preview == null) {
                return false;
            }

            // locate the preview image to center of touch point
            previewPadding = new Point((image.getWidth() - preview.getWidth()) / 2, (image.getHeight() - preview.getHeight()) / 2);

            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ADD_WIDGET, createWidgetInfo.componentName == null
                    ? ""
                    : createWidgetInfo.componentName.toString());
        } else if (itemInfo instanceof GadgetInfo) {
            createItemInfo = new PendingAddGadgetInfo((GadgetInfo) itemInfo);
            PendingAddGadgetInfo info = (PendingAddGadgetInfo) createItemInfo;
            int[] size = mLauncher.getWorkspace().estimateItemSize(info.spanX, info.spanY, createItemInfo, true);
            preview = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
            mCanvas.setBitmap(preview);
            mCanvas.save();
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            Bitmap gadgetBitmap = ThemeUtils.getGadgetPreview(mContext, info.gadgetInfo, size[0], size[1]);
            mCanvas.drawBitmap(gadgetBitmap, 0, 0, p);
            mCanvas.restore();
            mCanvas.setBitmap(null);

            // locate the preview image to center of touch point
            previewPadding = new Point((image.getWidth() - gadgetBitmap.getWidth()) / 2, (image.getHeight() - gadgetBitmap.getHeight()) / 2);
        } else {
            createItemInfo = new PendingAddShortcutInfo(((ResolveInfo) itemInfo).activityInfo);
            createItemInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
            createItemInfo.componentName = new ComponentName(((ResolveInfo) itemInfo).activityInfo.packageName,
                    ((ResolveInfo) itemInfo).activityInfo.name);
            Drawable icon = ((LauncherApplication) mLauncher.getApplicationContext()).getIconManager().getFullResIcon(
                    ((PendingAddShortcutInfo) createItemInfo).shortcutActivityInfo);
            /* YUNOS BEGIN */
            // ##date:2015/1/12 ##author:zhanggong.zg ##BugID:5697618
            Workspace workspace = mLauncher.getWorkspace();
            CellLayout cellLayout = (CellLayout) workspace.getChildAt(workspace.getCurrentPage());
            final float max_width = cellLayout.getCellWidth();
            final float max_height = cellLayout.getCellHeight();
            float w = icon.getIntrinsicWidth(), h = icon.getIntrinsicHeight();
            // shrink down the size of icon if it is too big
            if (w > max_width) {
                h *= max_width / w;
                w = max_width;
            }
            if (h > max_height) {
                w *= max_height / h;
                h = max_height;
            }
            w /= 1.25f;
            h /= 1.25f;
            preview = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_8888);
            mCanvas.setBitmap(preview);
            mCanvas.save();
            WidgetPreviewLoader.renderDrawableToBitmap(icon, preview, 0, 0, (int) w, (int) h);
            /* YUNOS END */
            mCanvas.restore();
            mCanvas.setBitmap(null);
            createItemInfo.spanX = createItemInfo.spanY = 1;
        }

        // Don't clip alpha values for the drag outline if we're using the
        // default widget preview
        boolean clipAlpha = !(createItemInfo instanceof PendingAddWidgetInfo && (((PendingAddWidgetInfo) createItemInfo).previewImage == 0));

        // Save the preview for the outline generation, then dim the preview
        outline = Bitmap.createScaledBitmap(preview, preview.getWidth(), preview.getHeight(), false);

        // Start the drag
        mLauncher.lockScreenOrientation();
        mLauncher.getWorkspace().onDragStartedWithItem(createItemInfo, outline, clipAlpha);
        mDragController.startDrag(image, preview, mDragSource, createItemInfo, DragController.DRAG_ACTION_COPY, previewPadding, scale);
        outline.recycle();
        preview.recycle();
        return true;
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Adapter adapter = parent.getAdapter();
        Object info;
        if (adapter instanceof WidgetPreviewAdapter) {
            info = adapter.getItem(position);
            beginDraggingWidget(view, info);
            /* YUNOS BEGIN */
            // ##date:2015/05/22 ##author: chenjian.chenjian ##BugId: 6006081
            EditModeHelper.getInstance().switchPreviewContainerType(PreviewContentType.CellLayouts);
            /* YUNOS END */
        }
        return true;
    }

    public void indicateCellLayoutHoverItem(View view, int itemIndex, boolean in, boolean dragSource) {
        if (view != null) {
            ImageView imageView = (ImageView) view.findViewById(R.id.preview_image);
            if (imageView != null) {
                if (dragSource) {
                    imageView.setBackgroundResource(R.drawable.dock_bg_moving);
                } else {
                    imageView.setBackgroundResource(in ? R.drawable.dock_bg_selected : R.drawable.em_preview_list_item_bg);
                }
            }
        }
    }
}
