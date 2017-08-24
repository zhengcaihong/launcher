
package com.aliyun.homeshell;

import app.aliyun.v3.gadget.GadgetHelper;
import app.aliyun.v3.gadget.GadgetInfo;
import app.aliyun.v3.gadget.GadgetView;

import android.content.ComponentName;
import android.content.Context;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class LauncherGadgetHelper {
    private static ArrayList<WeakReference<GadgetView>> sGadgetViews = new ArrayList<WeakReference<GadgetView>>();

    /**
     * get gadget by GadgetInfo
     * 
     * @param context
     * @param info
     * @return GadgetView
     */
    public static View getGadget(Context context, GadgetInfo info) {
        Context appContext = context.getApplicationContext();
        GadgetView v = GadgetHelper.getGadget(appContext == null ? context : appContext, info);
        if (v != null)
            synchronized (sGadgetViews) {
                sGadgetViews.add(new WeakReference<GadgetView>(v));
            }
        return v;
    }

    public static View getGadget(Context context, ComponentName cn) {
        return getGadget(context, cn, false);
    }

    /**
     * get gadget card by ComponentName
     *
     * @param context
     * @param cn
     * @param isYunCard if "true" shows we get gadget from "assets/" of another app
     * @return GadgetView
     */
    public static View getGadget(Context context, ComponentName cn, boolean isYunCard) {
        Context appContext = context.getApplicationContext();
        GadgetView v = null;
        if (isYunCard) {
            v = GadgetHelper.getGadgetFromAssets(appContext == null ? context : appContext, cn.getPackageName(), null);
        } else {
            v = GadgetHelper.getGadget(appContext == null ? context : appContext, cn);
        }
        return v;
    }

    public static void cleanUp() {
        synchronized (sGadgetViews) {
            for (WeakReference<GadgetView> r : sGadgetViews) {
                GadgetView v = r.get();
                if (v != null)
                    v.cleanUp();
            }
            sGadgetViews.clear();
        }
    }

    public static void cleanUp(GadgetView view) {
        if (view != null) {
            view.cleanUp();
            synchronized (sGadgetViews) {
                sGadgetViews.remove(view);
            }
        }
    }

}
