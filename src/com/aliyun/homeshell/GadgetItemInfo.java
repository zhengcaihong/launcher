package com.aliyun.homeshell;

import app.aliyun.v3.gadget.GadgetInfo;

/*YUNOS BEGIN*/
//##gadget
//##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com##BugID:96378
public class GadgetItemInfo extends ItemInfo {

    public GadgetInfo gadgetInfo;

    public GadgetItemInfo(GadgetInfo info) {
        gadgetInfo = info;
        spanX = minSpanX = info.spanX;
        spanY = minSpanY = info.spanY;
        itemType = LauncherSettings.Favorites.ITEM_TYPE_GADGET;
        container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        title = info.label;
    }

    @Override
    public String toString() {
        return "Gadget: " + gadgetInfo.path;
    }
}
/*YUNOS END*/
