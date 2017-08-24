
package com.aliyun.homeshell.appclone;

import android.content.pm.ApplicationInfo;

public class CloneApplicationInfo extends ApplicationInfo {
    public int userId;

    public CloneApplicationInfo(ApplicationInfo appInfo, int userId) {
        this.backupAgentName = appInfo.backupAgentName;
        this.className = appInfo.className;
        this.compatibleWidthLimitDp = appInfo.compatibleWidthLimitDp;
        this.dataDir = appInfo.dataDir;
        this.descriptionRes = appInfo.descriptionRes;
        this.enabled = appInfo.enabled;
        this.flags = appInfo.flags;
        this.icon = appInfo.icon;
        this.labelRes = appInfo.labelRes;
        this.largestWidthLimitDp = appInfo.largestWidthLimitDp;
        this.logo = appInfo.logo;
        this.manageSpaceActivityName = appInfo.manageSpaceActivityName;
        this.metaData = appInfo.metaData;
        this.name = appInfo.name;
        this.nativeLibraryDir = appInfo.nativeLibraryDir;
        this.nonLocalizedLabel = appInfo.nonLocalizedLabel;
        this.packageName = appInfo.packageName;
        this.permission = appInfo.packageName;
        this.processName = appInfo.processName;
        this.userId = userId;
        this.publicSourceDir = appInfo.publicSourceDir;
        this.requiresSmallestWidthDp = appInfo.requiresSmallestWidthDp;
        this.sharedLibraryFiles = appInfo.sharedLibraryFiles;
        this.sourceDir = appInfo.sourceDir;
        this.targetSdkVersion = appInfo.targetSdkVersion;
        this.taskAffinity = appInfo.taskAffinity;
        this.theme = appInfo.theme;
        this.uid = appInfo.uid;
        this.uiOptions = appInfo.uiOptions;
    }
}
