
package com.aliyun.homeshell.editmode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import app.aliyun.v3.gadget.GadgetInfo;

import com.aliyun.homeshell.GadgetCardHelper;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherAppWidgetHost;
import com.aliyun.homeshell.PendingAddWidgetInfo;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.themeutils.ThemeUtils;
import com.aliyun.utility.FeatureUtility;

import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;

import java.io.IOException;

public class WidgetPreviewAdapter extends BaseAdapter {
    private ArrayList<AppsCustomizeAsyncTask> mRunningTasks = new ArrayList<AppsCustomizeAsyncTask>();
    private static int ITEM_PER_SCREEN = 3;

    private static final String TAG = "WidgetPreviewAdapter";
    private static final String TAG_PACKAGE = "package";

    private WidgetPreviewLoader mWidgetPreviewLoader;
    private LayoutInflater mInflater;
    private Launcher mLauncher;
    private ArrayList<Object> mWidgets;
//    private PagedViewCellLayout mWidgetSpacingLayout;
    private PackageManager mPackageManager;
    private Bitmap mLoadingBmp;

    private ArrayList<Object> mAdvancedWidgets;
    private ArrayList<String> mAdvancedWidgetComponents;
    private static final String YUNOS_WIDGET_BIGCARD = "YUNOSHS";
    public WidgetPreviewAdapter(Launcher launcher) {
        super();
        mWidgetPreviewLoader = new WidgetPreviewLoader(launcher);
        mInflater = LayoutInflater.from(launcher);
        mLauncher = launcher;
        mWidgets = new ArrayList<Object>();
        mAdvancedWidgetComponents = new ArrayList<String>();
        onPackagesUpdated(LauncherModel.getSortedWidgetsAndShortcuts(launcher));
        int w = (int) launcher.getResources().getDimension(R.dimen.preview_widget_image_width);
        int h = (int) launcher.getResources().getDimension(R.dimen.preview_widget_image_height);
        mWidgetPreviewLoader.setPreviewSize(w, h);
        mPackageManager = launcher.getPackageManager();
        mLoadingBmp = Bitmap.createBitmap(w, h, Config.ARGB_4444);
        Canvas cvs = new Canvas();
        cvs.setBitmap(mLoadingBmp);
        cvs.drawColor(0x0c0c0c0c, PorterDuff.Mode.CLEAR);
        cvs.setBitmap(null);

        ITEM_PER_SCREEN = mLauncher.getResources().getInteger(R.integer.widgets_preview_loadingtask_size);
    }

    @Override
    public int getCount() {
        synchronized (mWidgets) {
            return mWidgets.size();
        }
    }

    @Override
    public Object getItem(int position) {
        synchronized (mWidgets) {
            return mWidgets.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = mInflater.inflate(R.layout.preview_widget_item, parent, false);
            vh.previewImgView = (ImageView) convertView.findViewById(R.id.preview_image);
            vh.labelTextView = (TextView) convertView.findViewById(R.id.preview_widget_label);
            vh.sizeTextView = (TextView) convertView.findViewById(R.id.preview_widget_size);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }
        synchronized (mWidgets) {
            Object widget = mWidgets.get(position);
            int sizeX = 1;
            int sizeY = 1;
            String label = null;
            if (widget instanceof AppWidgetProviderInfo) {
                label = ((AppWidgetProviderInfo) widget).label;
                int[] spanXY = Launcher.getSpanForWidget(mLauncher,
                        (AppWidgetProviderInfo) widget);
                sizeX = spanXY[0];
                sizeY = spanXY[1];
            } else if (widget instanceof ResolveInfo) {
                label = ((ResolveInfo) widget).loadLabel(mPackageManager).toString();
            } else if (widget instanceof GadgetInfo) {
                label = ThemeUtils.getGadgetName(mLauncher, ((GadgetInfo) widget));
                sizeX = ((GadgetInfo) widget).spanX;
                sizeY = ((GadgetInfo) widget).spanY;
            }
            vh.labelTextView.setText(filterWidgetLabel(label));
            vh.sizeTextView.setText(sizeX+" x "+sizeY);
            vh.position = position;
    
            vh.previewImgView.setScaleType(ScaleType.FIT_CENTER);
            Bitmap bmp = mWidgetPreviewLoader.getCachedPreview(widget);
            // YUNOS BEGIN PB
            //##modules(HomeShell): ##author:guoshuai.lgs
            //##BugID:(168065) ##date:2014/11/04
            //##decrpition: check whether bitmap is recycled when using it.
            if ((bmp != null) && (bmp.isRecycled() == false)) {
            // YUNOS END PB
                vh.previewImgView.setImageBitmap(bmp);
            } else {
                vh.previewImgView.setImageBitmap(mLoadingBmp);
                prepareLoadWidgetPreviewsTask(position, widget, vh.previewImgView);
                android.util.Log.e("cache", "geting...");
            }
        }
        return convertView;
    }

    private String filterWidgetLabel(String label){
        String str = label;
        Pattern p = Pattern.compile(".*(\\dx\\d)$");
        Matcher m = p.matcher(str);
        if(m.matches()){
            str = label.replaceAll(m.group(1),"");
        }
        if(str.endsWith("_")){
            str = str.substring(0, str.length()-1);
        }
        str = str.trim();
        return str;
    }

    public void onPackagesUpdated(ArrayList<Object> widgetsAndShortcuts) {
        synchronized (mWidgets) {
            // Get the list of widgets and shortcuts
            ArrayList<Object> widgetInfos = new ArrayList<Object>();
            parseWidgetOrderXml();
            mWidgets.clear();
            AppWidgetProviderInfo widget;
            int[] spanXY;
            int[] minSpanXY;
            int minSpanX;
            int minSpanY;
            GadgetInfo gadget;
            PendingAddWidgetInfo createItemInfo;
            boolean isSystemUIWidget = false;
            for (Object o : widgetsAndShortcuts) {
                if (o instanceof AppWidgetProviderInfo) {
                    widget = (AppWidgetProviderInfo) o;
                    widget.label = widget.label.trim();
                    if (widget.minWidth > 0 && widget.minHeight > 0) {
                        // Ensure that all widgets we show can be added on a
                        // workspace of this size
                        spanXY = Launcher.getSpanForWidget(mLauncher, widget);
                        minSpanXY = Launcher.getMinSpanForWidget(mLauncher, widget);
                        minSpanX = Math.min(spanXY[0], minSpanXY[0]);
                        minSpanY = Math.min(spanXY[1], minSpanXY[1]);
                        if (minSpanX <= LauncherModel.getCellCountX() &&
                                minSpanY <= LauncherModel.getCellCountY()) {
                            /* YUNOS BEGIN */
                            // ##date:2015/7/23 ##author:zhiqiang.zhangzq ##BugID:6219083
                            // ##replace GadgetCard with Widget
                            // ##hide widget  which is used for big card in WidgetPreviewAdapter
                            if (widget.provider != null) {
                                isSystemUIWidget = "com.android.systemui".equals(widget.provider.getPackageName());
                            }

                            if (widget.label != null && !widget.label.toUpperCase(Locale.getDefault()).startsWith(YUNOS_WIDGET_BIGCARD)
                                    && !isSystemUIWidget) {
                                mWidgets.add(widget);
                            }
                            /* YUNOS END */
                        }
                    }
                    /* YUNOS BEGIN */
                    // ##bigcard
                    // ##date:2015/04/16
                    // ##author:sunchen.sc@alibaba-inc.com##BugID:96378
                    if (FeatureUtility.hasNotificationFeature()) {
                        createItemInfo = new PendingAddWidgetInfo(widget, null, null);

                        // Determine the widget spans and min resize spans.
                        spanXY = Launcher.getSpanForWidget(mLauncher, widget);
                        createItemInfo.spanX = spanXY[0];
                        createItemInfo.spanY = spanXY[1];
                        minSpanXY = Launcher.getMinSpanForWidget(mLauncher, widget);
                        createItemInfo.minSpanX = minSpanXY[0];
                        createItemInfo.minSpanY = minSpanXY[1];
                        widgetInfos.add(createItemInfo);
                    }
                    /* YUNOS END */
                } else if (o instanceof ResolveInfo) {
                    mWidgets.add(o);
                } else if (o instanceof GadgetInfo) {
                    gadget = (GadgetInfo) o;
                    mWidgets.add(gadget);
                }
            }
            reoderWidgetList();
            GadgetCardHelper.getInstance(mLauncher.getApplicationContext()).setWidgetInfoList(widgetInfos);
        }
    }

    private void parseWidgetOrderXml() {
        // Get the list of widgets and shortcuts
        XmlResourceParser xrp = mLauncher.getResources().getXml(R.xml.widget_order_config);  
        try {  
            while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
                if (xrp.getEventType() == XmlResourceParser.START_TAG) {
                    String tagName = xrp.getName();  
                    if (TAG_PACKAGE.endsWith(tagName)) {
                        String packageName = xrp.getAttributeValue(0);
                        if(!mAdvancedWidgetComponents.contains(packageName)) {
                            Log.d(TAG, "sxsexe------>onPackagesUpdated mAdvancedWidgetComponents add " + packageName);
                            mAdvancedWidgetComponents.add(packageName);
                        }
                    }
                }
                xrp.next();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "sxsexe-------->parseWidgetOrderXml " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "sxsexe-------->parseWidgetOrderXml " + e.getMessage());
        }
        
        if(mAdvancedWidgets == null) {
            mAdvancedWidgets = new ArrayList<Object>(mAdvancedWidgetComponents.size());
        } else {
            mAdvancedWidgets.clear();
        }
    }

    private void reoderWidgetList() {
        Log.d(TAG, "reoderWidgetList begin: " + mWidgets.size());
        //ArrayList<Object> mAdvancedWidgets = new ArrayList<Object>();
        int size = mAdvancedWidgetComponents.size();
        String packageName = null;

        ArrayList<Object> tempWidgets = null;
        try {
            tempWidgets = (ArrayList<Object>) mWidgets.clone();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        if (tempWidgets != null) {
            for(int i = 0; i < size; i++) {
                packageName = mAdvancedWidgetComponents.get(i);
                for(Object o : tempWidgets) {
                    if(o instanceof AppWidgetProviderInfo && packageName != null) {
                        AppWidgetProviderInfo widget = (AppWidgetProviderInfo)o;
                        if(widget.provider != null && packageName.equals(widget.provider.getPackageName())) {
                            mAdvancedWidgets.add(o);
                        }
                    }
                }
            }
            for(Object o : mAdvancedWidgets) {
                mWidgets.remove(o);
            }
            mWidgets.addAll(0, mAdvancedWidgets);
        }
        mAdvancedWidgetComponents.clear();
        mAdvancedWidgets.clear();
        Log.d(TAG, "reoderWidgetList end: " + mWidgets.size());
    }

    /* YUNOS BEGIN */
    // ##date:2015/7/27
    // ##author:zhiqiang.zhangzq
    // ##BugID:6219083
    // ##Desc:replace GadgetCard with Widget
    public View initWidgetView(AppWidgetManager widgetMgr, LauncherAppWidgetHost widgetHost, ComponentName cn) {
        if (widgetHost == null || widgetMgr == null || cn == null) {
            Log.w(TAG, "initNotificationWidgetView() widgetMgr or widgetHost is null");
            return null;
        }
        mWidgetMgr = widgetMgr;
        mWidgetHost = widgetHost;
        PendingAddWidgetInfo info = getWidgetInfo(cn);
        String label = null;
        if (info != null && info.getAppWidgetProviderInfo() != null) {
            label = info.getAppWidgetProviderInfo().label;
        }
        if (info == null || label == null || !label.toUpperCase(Locale.getDefault()).startsWith(YUNOS_WIDGET_BIGCARD)) {
            return null;
        }
        AppWidgetHostView notificationHostView = info.boundWidget;
        int notificationWidgetId = -1;
        Log.d(TAG, "initNotificationWidgetView() mNotificationHostView = " + notificationHostView);
        if (notificationHostView == null) {
            // In this case, we either need to start an activity to get
            // permission to bind
            // the widget, or we need to start an activity to configure the
            // widget, or both.
            notificationWidgetId = widgetHost.allocateAppWidgetId();
            Log.d(TAG, "initNotificationWidgetView() mNotificationWidgetId = " + notificationWidgetId);
            Bundle options = info.bindOptions;

            boolean success = false;
            // binding widget will failed, while uninstalled widgetApp before
            // drop widget.
            try {
                if (options != null) {
                    success = widgetMgr.bindAppWidgetIdIfAllowed(notificationWidgetId, info.componentName, options);
                } else {
                    success = widgetMgr.bindAppWidgetIdIfAllowed(notificationWidgetId, info.componentName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed in addAppWidgetFromDrop : " + e.getMessage());
            }
            if (success) {
                notificationHostView = widgetHost.createView(mLauncher.getApplicationContext(), notificationWidgetId, info.info);
                if (notificationHostView != null) {
                    notificationHostView.setAppWidget(notificationWidgetId, info.info);
                } else {
                    Log.w(TAG, "initNotificationWidgetView() create notificationHostView is null");
                }
            }
        }
        if (notificationHostView != null) {
            notificationHostView.setTag(Integer.valueOf(notificationWidgetId));
        }
        Log.d(TAG, "initWidgetView pkgName = " + cn.getPackageName() + ", notificationWidgetId = " + notificationWidgetId);
        return notificationHostView;
    }
    public void removeWidgetView(AppWidgetHostView widgetView) {
        Object tag = widgetView.getTag();
        if (tag != null && tag instanceof Integer) {
            Integer id = (Integer) tag;
            Log.d(TAG, "deleteAppWidgetId id = " + id.intValue());
            mWidgetHost.deleteAppWidgetId(id.intValue());
        }
    }
    // ##date:2015/8/21 ##author:zhanggong.zg ##BugID:6349930   715
    public boolean hasWidgetView(AppWidgetManager widgetMgr, LauncherAppWidgetHost widgetHost, ComponentName cn) {
        if (widgetHost == null || widgetMgr == null || cn == null) {
            return false;
        }
        mWidgetMgr = widgetMgr;
        mWidgetHost = widgetHost;
        PendingAddWidgetInfo info = getWidgetInfo(cn);
        if (info == null) {
            return false;
        }
        AppWidgetProviderInfo provider = info.getAppWidgetProviderInfo();
        if (provider == null) {
            return false;
        }
        String label = provider.label;
        if (label == null || !label.toUpperCase(Locale.getDefault()).startsWith(YUNOS_WIDGET_BIGCARD)) {
            return false;
        }
        return true;
    }
    /* YUNOS END */

    /**
     * Creates and executes a new AsyncTask to load a page of widget previews.
     */
    @SuppressLint("NewApi")
    private void prepareLoadWidgetPreviewsTask(int position, Object widget,
            final ImageView previewImageView) {
        // Prune all tasks that are no longer needed
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int taskPage = task.page;
            if (taskPage < position - ITEM_PER_SCREEN ||
                    taskPage > position + ITEM_PER_SCREEN) {
                task.cancel(false);
                iter.remove();
            } /*
               * else {
               * task.setThreadPriority(getThreadPriorityForPage(taskPage)); }
               */
        }

        // We introduce a slight delay to order the loading of side pages so
        // that we don't thrash
        AsyncTaskPageData pageData = new AsyncTaskPageData(position, widget,
                new AsyncTaskCallback() {
                    @Override
                    public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                        try {
                            loadWidgetPreviewsInBackground(task, data);
                        } finally {
                            if (task.isCancelled()) {
                                data.cleanup(true);
                            }
                        }
                    }
                },
                new AsyncTaskCallback() {
                    @Override
                    public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                        mRunningTasks.remove(task);
                        if (task.isCancelled())
                            return;
                        // do cleanup inside onSyncWidgetPageItems
                        // onSyncWidgetPageItems(data);
                        // YUNOS BEGIN PB
                        //##modules(HomeShell): ##author:guoshuai.lgs
                        //##BugID:(168065) ##date:2014/11/04
                        //##decrpition: check whether bitmap is recycled when using it.
                        if ((data.generatedImage != null) && (data.generatedImage.isRecycled() == false)) {
                            previewImageView.setImageBitmap(data.generatedImage);
                        }
                        // YUNOS END PB
                    }
                }, mWidgetPreviewLoader, previewImageView);

        // Ensure that the task is appropriately prioritized and runs in
        // parallel
        AppsCustomizeAsyncTask t = new AppsCustomizeAsyncTask(position,
                AsyncTaskPageData.Type.LoadWidgetPreviewData);
        t.setThreadPriority(Process.THREAD_PRIORITY_LESS_FAVORABLE);
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pageData);
        mRunningTasks.add(t);
    }

    private void loadWidgetPreviewsInBackground(AppsCustomizeAsyncTask task,
            AsyncTaskPageData data) {
        // loadWidgetPreviewsInBackground can be called without a task to load a
        // set of widget
        // previews synchronously
        if (task != null) {
            // Ensure that this task starts running at the correct priority
            task.syncThreadPriority();
        }
        if (!task.isCancelled()) {
            data.generatedImage = mWidgetPreviewLoader.getPreview(data.item);
        }
    }

    private AppWidgetManager mWidgetMgr;
    private LauncherAppWidgetHost mWidgetHost;
    private static final String NOTIFICATION_WIDGET_PACKAGE = "com.android.systemui";
    private int mNotificationWidgetId;
    private AppWidgetHostView mNotificationHostView;

    public void recreateNotificationWidgetView() {
        initNotificationWidgetView(mWidgetMgr, mWidgetHost);
    }
    public void initNotificationWidgetView(AppWidgetManager widgetMgr, LauncherAppWidgetHost widgetHost) {
        if (widgetHost == null || widgetMgr == null) {
            Log.w(TAG, "initNotificationWidgetView() widgetMgr or widgetHost is null");
            return;
        }
        mWidgetMgr = widgetMgr;
        mWidgetHost = widgetHost;
        final PendingAddWidgetInfo info = getWidgetInfo(NOTIFICATION_WIDGET_PACKAGE);
        if (info == null) {
            return;
        }
        mNotificationHostView = info.boundWidget;
        mNotificationWidgetId = -1;
        Log.d(TAG, "initNotificationWidgetView() mNotificationHostView = " + mNotificationHostView);
        if (mNotificationHostView == null) {
            // In this case, we either need to start an activity to get
            // permission to bind
            // the widget, or we need to start an activity to configure the
            // widget, or both.
            mNotificationWidgetId = widgetHost.allocateAppWidgetId();
            Log.d(TAG, "initNotificationWidgetView() mNotificationWidgetId = " + mNotificationWidgetId);
            new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... arg0) {
                    Bundle options = info.bindOptions;

                    boolean success = false;
                    // binding widget will failed, while uninstalled widgetApp before
                    // drop widget.
                    try {
                        if (options != null) {
                            success = mWidgetMgr.bindAppWidgetIdIfAllowed(mNotificationWidgetId, info.componentName, options);
                        } else {
                            success = mWidgetMgr.bindAppWidgetIdIfAllowed(mNotificationWidgetId, info.componentName);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed in addAppWidgetFromDrop : " + e.getMessage());
                    }
                    return success;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    super.onPostExecute(result);
                    if (result) {
                        mNotificationHostView = mWidgetHost.createView(mLauncher.getApplicationContext(), mNotificationWidgetId, info.info);
                        if (mNotificationHostView != null) {
                            mNotificationHostView.setAppWidget(mNotificationWidgetId, info.info);
                        } else {
                            Log.w(TAG, "initNotificationWidgetView() create notificationHostView is null");
                        }
                    }
                    GadgetCardHelper.getInstance(mLauncher.getApplicationContext()).setNotificationWidgetView(mNotificationHostView, mNotificationWidgetId);
                }

            }.execute();
        } else {
            GadgetCardHelper.getInstance(mLauncher.getApplicationContext()).setNotificationWidgetView(mNotificationHostView, mNotificationWidgetId);
        }
    }

    private PendingAddWidgetInfo getWidgetInfo(String pkgName) {
        ArrayList<Object> array = GadgetCardHelper.getInstance(mLauncher.getApplicationContext()).getWidgetInfos();
        PendingAddWidgetInfo info;
        String pkg;
        for (Object obj : array) {
            info = (PendingAddWidgetInfo) obj;
            pkg = info.info.provider.getPackageName();
            if ((pkg != null && pkg.equalsIgnoreCase(pkgName))) {
                return info;
            }
        }
        return null;
    }

    /* YUNOS BEGIN */
    // ##date:2015/7/27
    // ##author:zhiqiang.zhangzq
    // ##BugID:6219083
    // ##Desc:replace GadgetCard with Widget
    private PendingAddWidgetInfo getWidgetInfo(ComponentName cn) {
        if (cn == null) {
            return null;
        }
        ArrayList<Object> array = GadgetCardHelper.getInstance(mLauncher.getApplicationContext()).getWidgetInfos();
        PendingAddWidgetInfo info;
        String widgetReceiverPkgName;
        String widgetReceiverName;
        ActivityInfo aInfo = null;
        try {
            aInfo = mLauncher.getPackageManager().getActivityInfo(cn, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        String widgetForAcitivity = null;
        if (aInfo != null) {
            Bundle meta = aInfo.metaData;
            if (meta != null) {
                widgetForAcitivity = meta.getString("widgetInfo");
            }
        }
        if (widgetForAcitivity == null) {
            return null;
        }
        Log.d(TAG, " zzq getWidgetInfo: " + " cn.pkg=" + cn.getPackageName() + "cn.classname= " + cn.getClassName());
        Log.d(TAG, " zzq widgetForAcitivity: " + widgetForAcitivity);
        for (Object obj : array) {
            info = (PendingAddWidgetInfo) obj;
            widgetReceiverName = info.info.provider.getClassName();
            widgetReceiverPkgName = info.info.provider.getPackageName();
            Log.d(TAG, " zzq getWidgetInfo: " + "widget provider pkg=" + widgetReceiverPkgName + "widget provider widgetReceiverName= "
                    + widgetReceiverName);
            if (widgetReceiverPkgName != null && widgetReceiverName.equalsIgnoreCase(widgetForAcitivity)) {
                return info;
            }
        }
        return null;
    }
    /* YUNOS END */

    class ViewHolder {
        public int position;
        public ImageView previewImgView;
        public TextView labelTextView;
        public TextView sizeTextView;
    }

    public String getTitle(View view) {
        return ((ViewHolder) view.getTag()).labelTextView.getText().toString();
    }

}

/**
 * A simple callback interface which also provides the results of the task.
 */
interface AsyncTaskCallback {
    void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data);
}

/**
 * The data needed to perform either of the custom AsyncTasks.
 */
class AsyncTaskPageData {
    enum Type {
        LoadWidgetPreviewData
    }

    AsyncTaskPageData(int p, Object l, int cw, int ch, AsyncTaskCallback bgR,
            AsyncTaskCallback postR, WidgetPreviewLoader w, ImageView previewImageView) {
        page = p;
        item = l;
        maxImageWidth = cw;
        maxImageHeight = ch;
        doInBackgroundCallback = bgR;
        postExecuteCallback = postR;
        widgetPreviewLoader = w;
        previewView = previewImageView;
    }

    AsyncTaskPageData(int p, Object l, AsyncTaskCallback bgR,
            AsyncTaskCallback postR, WidgetPreviewLoader w, ImageView previewImageView) {
        this(p, l, 0, 0, bgR, postR, w, previewImageView);
    }

    void cleanup(boolean cancelled) {
        // Clean up any references to source/generated bitmaps
        if (generatedImage != null) {
            if (cancelled) {
                widgetPreviewLoader.recycleBitmap(item, generatedImage);
            }
        }
        // YUNOS BEGIN PB
        //##modules(HomeShell): ##author:guoshuai.lgs
        //##BugID:(168065) ##date:2014/11/04
        //##decrpition: clear generatedImage after recycling it.
        generatedImage = null;
        // YUNOS END PB
    }

    int page;
    Object item;
    // YUNOS BEGIN PB
    //##modules(HomeShell): ##author:guoshuai.lgs
    //##BugID:(168065) ##date:2014/11/04
    //##decrpition: unused.
    //Bitmap sourceImage;
    // YUNOS END PB
    Bitmap generatedImage;
    int maxImageWidth;
    int maxImageHeight;
    AsyncTaskCallback doInBackgroundCallback;
    AsyncTaskCallback postExecuteCallback;
    WidgetPreviewLoader widgetPreviewLoader;
    ImageView previewView;
}

/**
 * A generic template for an async task used in AppsCustomize.
 */
class AppsCustomizeAsyncTask extends AsyncTask<AsyncTaskPageData, Void, AsyncTaskPageData> {
    AppsCustomizeAsyncTask(int p, AsyncTaskPageData.Type ty) {
        page = p;
        threadPriority = Process.THREAD_PRIORITY_DEFAULT;
        dataType = ty;
    }

    @Override
    protected AsyncTaskPageData doInBackground(AsyncTaskPageData... params) {
        if (params.length != 1)
            return null;
        // Load each of the widget previews in the background
        params[0].doInBackgroundCallback.run(this, params[0]);
        return params[0];
    }

    @Override
    protected void onPostExecute(AsyncTaskPageData result) {
        // All the widget previews are loaded, so we can just callback to
        // inflate the page
        result.postExecuteCallback.run(this, result);
    }

    void setThreadPriority(int p) {
        threadPriority = p;
    }

    void syncThreadPriority() {
        Process.setThreadPriority(threadPriority);
    }

    // The page that this async task is associated with
    AsyncTaskPageData.Type dataType;
    int page;
    int threadPriority;
}
