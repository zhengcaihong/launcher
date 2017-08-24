
package com.aliyun.homeshell;

import android.content.pm.ActivityInfo;

public class PendingAddShortcutInfo extends PendingAddItemInfo {

    public ActivityInfo shortcutActivityInfo;

    public PendingAddShortcutInfo(ActivityInfo activityInfo) {
        shortcutActivityInfo = activityInfo;
        itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
    }

    @Override
    public String toString() {
        return "Shortcut: " + shortcutActivityInfo.packageName;
    }

}
