package com.aliyun.homeshell;

import app.aliyun.v3.gadget.GadgetInfo;

public class PendingAddGadgetInfo extends PendingAddItemInfo {
    public GadgetInfo gadgetInfo;

    public PendingAddGadgetInfo(GadgetInfo info) {
        gadgetInfo = info;
        spanX = info.spanX;
        spanY = info.spanY;
        itemType = LauncherSettings.Favorites.ITEM_TYPE_GADGET;
        title = info.label;
    }

    @Override
    public String toString() {
        return "Gadget: " + gadgetInfo.path;
    }
}
