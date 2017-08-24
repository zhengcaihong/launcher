
package com.aliyun.homeshell.smartlocate;

public class SuggestedAppsItem {

    private static int autoId;

    public int id;

    public String title;

    public String intent;

    public byte[] blob;

    public boolean isCardMode;

    /* YUNOS BEGIN */
    // ## date: 2016/06/27 ## author: yongxing.lyx
    // ## BugID: 8402623: add clone mark to xiaoyun suggested icon.
    public int userId;
    /* YUNOS BEGIN */

    public static int generateId() {
        if (autoId == Integer.MAX_VALUE || autoId == Integer.MIN_VALUE) {
            autoId = 0;
        }
        autoId += 1;
        return autoId;
    }

    @Override
    public String toString() {
        return "SuggestedAppsItem [id=" + id + ", title=" + title + ", intent=" + intent
                + ", isCardMode=" + isCardMode + ", userId=" + userId + "]";
    }

}
