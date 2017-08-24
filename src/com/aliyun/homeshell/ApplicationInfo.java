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
import java.util.HashMap;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.util.Log;

import com.aliyun.homeshell.appclone.CloneResolveInfo;

/**
 * Represents an app in AllAppsView.
 */
public class ApplicationInfo extends ItemInfo {
    private static final String TAG = "Launcher2.ApplicationInfo";

    /**
     * The intent used to start the application.
     */
    public Intent intent;

    /**
     * A bitmap version of the application icon.
     */
    public Bitmap iconBitmap;

    /**
     * The time at which the app was first installed.
     */
    public long firstInstallTime;

    public ComponentName componentName;

    static final int DOWNLOADED_FLAG = 1;
    static final int UPDATED_SYSTEM_APP_FLAG = 2;
    static final int SDCARD_FLAG = 4;

    int flags = 0;

    public int userId = 0;
    /* YUNOS BEGIN */
    // ##date:2015/1/14 ##author:zhanggong.zg ##BugID:5690670
    /** @see #updateLater(ResolveInfo) */
    private ResolveInfo pendingInfo = null;
    /* YUNOS END */

    ApplicationInfo() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }

    /**
     * Must not hold the Context.
     */
    public ApplicationInfo(PackageManager pm, ResolveInfo info, HashMap<Object, CharSequence> labelCache) {
        this(pm, new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name),
                info.loadLabel(pm).toString(), labelCache);
        Log.d(TAG, "title from resovle info is " + this.title);
        if (info instanceof CloneResolveInfo) {
            userId = ((CloneResolveInfo)info).userId;
        }
    }

    public ApplicationInfo(PackageManager pm, ComponentName componentName, String label,
            HashMap<Object, CharSequence> labelCache) {
        reset(pm, componentName, label, labelCache);
    }

    public ApplicationInfo(ApplicationInfo info) {
        super(info);
        componentName = info.componentName;
        title = info.title.toString();
        intent = new Intent(info.intent);
        flags = info.flags;
        firstInstallTime = info.firstInstallTime;
    }

    private void reset(PackageManager pm, ComponentName componentName, String label,
                       HashMap<Object, CharSequence> labelCache) {
        this.componentName = componentName;
        this.container = ItemInfo.NO_ID;
        this.setActivity(componentName,
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        final String packageName = componentName.getPackageName();
        try {
            int appFlags = pm.getApplicationInfo(packageName, 0).flags;
            if ((appFlags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                flags |= DOWNLOADED_FLAG;

                if ((appFlags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    flags |= UPDATED_SYSTEM_APP_FLAG;
                }
            }

            if ((appFlags & android.content.pm.ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
                flags |= SDCARD_FLAG;
            }

            firstInstallTime = pm.getPackageInfo(packageName, 0).firstInstallTime;
        } catch (NameNotFoundException e) {
            Log.d(TAG, "PackageManager.getApplicationInfo failed for " + packageName);
        }
        //remove by dongjun for IconManager
        //if(iconCache != null) {
        //    iconCache.getTitleAndIcon(this, info, labelCache);
        //}
        title = label;
    }

    /* YUNOS BEGIN */
    // ##date:2015/1/14 ##author:zhanggong.zg ##BugID:5690670
    /**
     * Temporarily stores a new <code>ResolveInfo</code> object which is used
     * to update this <code>ApplicationInfo</code> later.<p>
     * Call {@link #update(PackageManager)} to trigger the update process.
     * @param info
     */
    public void updateLater(ResolveInfo info) {
        Log.v(TAG, "updateLater: componentName=" + componentName + " title=" + title);
        this.pendingInfo = info;
    }

    public boolean needsUpdate() {
        return pendingInfo != null;
    }

    /**
     * Updates this <code>ApplicationInfo</code> using the pending <code>ResolveInfo</code> object.
     * @param pm
     */
    public void update(PackageManager pm) {
        if (pendingInfo == null) return;
        Log.v(TAG, "update: begin");
        ComponentName cmpt = new ComponentName(pendingInfo.activityInfo.applicationInfo.packageName,
                                               pendingInfo.activityInfo.name);
        String title = pendingInfo.loadLabel(pm).toString();
        reset(pm, cmpt, title, null);
        Log.v(TAG, "update: componentName=" + cmpt + " title=" + title);
        pendingInfo = null;
        Log.v(TAG, "update: end");
    }
    /* YUNOS END */

    /**
     * Creates the application intent based on a component name and various launch flags.
     * Sets {@link #itemType} to {@link LauncherSettings.BaseLauncherColumns#ITEM_TYPE_APPLICATION}.
     *
     * @param className the class name of the component representing the intent
     * @param launchFlags the launch flags
     */
    final void setActivity(ComponentName className, int launchFlags) {
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(className);
        intent.setFlags(launchFlags);
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION;
    }

    @Override
    public String toString() {
        return "ApplicationInfo [intent=" + intent + ", iconBitmap=" + iconBitmap
                + ", firstInstallTime=" + firstInstallTime + ", componentName=" + componentName
                + ", flags=" + flags + ", id=" + id + ", itemType=" + itemType + ", container="
                + container + ", screen=" + screen + ", cellX=" + cellX + ", cellY=" + cellY
                + ", spanX=" + spanX + ", spanY=" + spanY + ", minSpanX=" + minSpanX
                + ", minSpanY=" + minSpanY + ", deletable=" + deletable + ", messageNum="
                + messageNum + ", requiresDbUpdate=" + requiresDbUpdate + ", title=" + title
                + ", dropPos=" + Arrays.toString(dropPos) + ", isNew=" + isNew + "]";
    }

    public static void dumpApplicationInfoList(String tag, String label,
            ArrayList<ApplicationInfo> list) {
        Log.d(tag, label + " size=" + list.size());
        for (ApplicationInfo info: list) {
            Log.d(tag, "   title=\"" + info.title + "\" iconBitmap="
                    + info.iconBitmap + " firstInstallTime="
                    + info.firstInstallTime);
        }
    }

    public ShortcutInfo makeShortcut() {
        return new ShortcutInfo(this);
    }
}
