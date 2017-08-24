
package com.aliyun.homeshell.appclone;

import android.content.pm.ResolveInfo;

public class CloneResolveInfo extends ResolveInfo {
    public int userId;

    public CloneResolveInfo(ResolveInfo rslvInfo, int userId) {
        this.activityInfo = rslvInfo.activityInfo;
        this.filter = rslvInfo.filter;
        this.icon = rslvInfo.icon;
        this.isDefault = rslvInfo.isDefault;
        this.labelRes = rslvInfo.labelRes;
        this.match = rslvInfo.match;
        this.nonLocalizedLabel = rslvInfo.nonLocalizedLabel;
        this.preferredOrder = rslvInfo.preferredOrder;
        this.priority = rslvInfo.priority;
        this.userId = userId;
        this.providerInfo = rslvInfo.providerInfo;
        this.resolvePackageName = rslvInfo.resolvePackageName;
        this.serviceInfo = rslvInfo.serviceInfo;
        this.specificIndex = rslvInfo.specificIndex;
    }
}
