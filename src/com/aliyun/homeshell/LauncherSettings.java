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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Settings related utilities.
 */
public class LauncherSettings {
    public static interface BaseLauncherColumns extends BaseColumns {
        /**
         * Descriptive name of the gesture that can be displayed to the user.
         * <P>Type: TEXT</P>
         */
        static final String TITLE = "title";

        /**
         * The Intent URL of the gesture, describing what it points to. This
         * value is given to {@link android.content.Intent#parseUri(String, int)} to create
         * an Intent that can be launched.
         * <P>Type: TEXT</P>
         */
        static final String INTENT = "intent";

        /**
         * The type of the gesture
         *
         * <P>Type: INTEGER</P>
         */
        static final String ITEM_TYPE = "itemType";

        /**
         * The gesture is an application
         */
        static final int ITEM_TYPE_APPLICATION = 0;

        /**
         * The gesture is an application created shortcut
         */
        static final int ITEM_TYPE_SHORTCUT = 1;

        /**
         * The icon type.
         * <P>Type: INTEGER</P>
         */
        static final String ICON_TYPE = "iconType";

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        static final int ICON_TYPE_RESOURCE = 0;

        /**
         * The icon is a bitmap.
         */
        static final int ICON_TYPE_BITMAP = 1;

        /**
         * The icon package name, if icon type is ICON_TYPE_RESOURCE.
         * <P>Type: TEXT</P>
         */
        static final String ICON_PACKAGE = "iconPackage";

        /**
         * The icon resource id, if icon type is ICON_TYPE_RESOURCE.
         * <P>Type: TEXT</P>
         */
        static final String ICON_RESOURCE = "iconResource";

        /**
         * The custom icon bitmap, if icon type is ICON_TYPE_BITMAP.
         * <P>Type: BLOB</P>
         */
        static final String ICON = "icon";
        
        /**
         * Indicates whether this item can be deleted
         */
        public static final String CAN_DELEDE = "canDelete";
        
        /**
         * Unread message number
         */
        public static final String MESSAGE_NUM = "messageNum";

        /**-
         * indicating whether this item is new
         */
        public static final String IS_NEW = "isNew";

        /*YUNOS BEGIN LH*/
        /**
         * indicating whether this item's app is in sdcard
         */
         public static final String IS_SDAPP = "isShortcut";
         /*YUNOS END*/

       /*YUNOS BEGIN*/
       //##date:2014/6/4 ##author:zhangqiang.zq
       // smart search
         public static final String FULL_PINYIN = "fullPinyin";
         public static final String SHORT_PINYIN = "shortPinyin";
       /*YUNOS END*/

         /*YUNOS BEGIN*/
         //##date:2014/8/1 ##author:zhangqiang.zq
         // favorite apps
         public static final String FAVORITE_WEIGHT = "favoriteWeight";
         public static final String FAVORITE_ICON = "favoriteIcon";
         /*YUNOS END*/
    }

    /**
     * Favorites.
     */
    public static final class Favorites implements BaseLauncherColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" +
                LauncherProvider.AUTHORITY + "/" + LauncherProvider.TABLE_FAVORITES +
                "?" + LauncherProvider.PARAMETER_NOTIFY + "=true");

        /**
         * The content:// style URL for this table. for normal mode favorite table
         */
        public static final Uri CONTENT_URI_NORMAL_MODE = Uri.parse("content://" +
                LauncherProvider.AUTHORITY + "/" + LauncherProvider.NORMAL_TABLE_FAVORITES +
                "?" + LauncherProvider.PARAMETER_NOTIFY + "=true");

        /**
         * The content:// style URL for this table. for aged mode favorite table
         */
        public static final Uri CONTENT_URI_AGED_MODE = Uri.parse("content://" +
                LauncherProvider.AUTHORITY + "/" + LauncherProvider.TABLE_AGED_FAVORITES +
                "?" + LauncherProvider.PARAMETER_NOTIFY + "=true");

        /**
         * The content:// style URL for this table. When this Uri is used, no notification is
         * sent if the content changes.
         */
        public static final Uri CONTENT_URI_NO_NOTIFICATION = Uri
                .parse("content://" +
                LauncherProvider.AUTHORITY + "/" + LauncherProvider.TABLE_FAVORITES +
                "?" + LauncherProvider.PARAMETER_NOTIFY + "=false");

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @param notify True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(long id, boolean notify) {
            return Uri.parse("content://" + LauncherProvider.AUTHORITY +
                    "/" + LauncherProvider.TABLE_FAVORITES + "/" + id + "?" +
                    LauncherProvider.PARAMETER_NOTIFY + "=" + notify);
        }

        public static Uri getContentUriForAnotherTable(long id, boolean notify) {
            if (LauncherProvider.getDbAgedModeState()) {
                return Uri.parse("content://" + LauncherProvider.AUTHORITY +
                        "/" + LauncherProvider.NORMAL_TABLE_FAVORITES + "/" + id + "?" +
                        LauncherProvider.PARAMETER_NOTIFY + "=" + notify);
            } else {
                return Uri.parse("content://" + LauncherProvider.AUTHORITY +
                        "/" + LauncherProvider.TABLE_AGED_FAVORITES + "/" + id + "?" +
                    LauncherProvider.PARAMETER_NOTIFY + "=" + notify);
            }
        }

        /* YUNOS BEGIN */
        //##date:2013/12/05 ##author:hongxing.whx ##bugid:71057
        public static int getContainerHotseat() {
            return CONTAINER_HOTSEAT;
        }
        /* YUNOS END */

        /*YUNOS BEGIN*/
        //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
        //add hide icon container
        public static int getContainerHideseat() {
            return CONTAINER_HIDESEAT;
        }
        /*YUNOS END*/

        /**
         * The container holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String CONTAINER = "container";

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        public static final int CONTAINER_DESKTOP = -100;
        public static final int CONTAINER_HOTSEAT = -101;
        /*YUNOS BEGIN*/
        //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
        //add hide icon container
        public static final int CONTAINER_HIDESEAT = -102;
        /*YUNOS END*/

        /**
         * The screen holding the favorite (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        public static final String SCREEN = "screen";

        /**
         * The X coordinate of the cell holding the favorite
         * (if container is CONTAINER_HOTSEAT or CONTAINER_HOTSEAT)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLX = "cellX";

        /**
         * The Y coordinate of the cell holding the favorite
         * (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLY = "cellY";

        /**
         * The X coordinate of the cell holding the favorite in land mode
         * (if container is CONTAINER_HOTSEAT or CONTAINER_HOTSEAT)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLXPORT = "cellX";

        /**
         * The Y coordinate of the cell holding the favorite in land mode
         * (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLYPORT = "cellY";
 
        /**
         * The X coordinate of the cell holding the favorite in land mode
         * (if container is CONTAINER_HOTSEAT or CONTAINER_HOTSEAT)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLXLAND = "cellXLand";

        /**
         * The Y coordinate of the cell holding the favorite in land mode
         * (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLYLAND = "cellYLand";

        /**
         * The X span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String SPANX = "spanX";

        /**
         * The Y span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String SPANY = "spanY";
        /* YUNOS BEGIN */
        // ##date:2014/06/03 ##author:yangshan.ys
        // batch operations to the icons in folder
        public static final int ITEM_FLAGS_EDIT_FOLDER = 1 <<3;
        /* YUNOS END */
        /**
         * The favorite is a user created folder
         */
        public static final int ITEM_TYPE_FOLDER = 2;

        /**
        * The favorite is a live folder
        *
        * Note: live folders can no longer be added to Launcher, and any live folders which
        * exist within the launcher database will be ignored when loading.  That said, these
        * entries in the database may still exist, and are not automatically stripped.
        */
        static final int ITEM_TYPE_LIVE_FOLDER = 3;

        /**
         * The favorite is a widget
         */
        public static final int ITEM_TYPE_APPWIDGET = 4;

        /*YUNOS BEGIN LH*/
        /**
         * The favorite is a cloudapp shortcut
         */
        public static final int ITEM_TYPE_CLOUDAPP = 5;

        /**
         * The favorite is a browser book mark shortcut
         */
        public static final int ITEM_TYPE_BOOKMARK = 6;
        /**
         * The favorite is a aliwidget for tracfficpanel etc.
         */
        public static final int ITEM_TYPE_ALIAPPWIDGET = 7;
        /* YUNOS BEGIN */
        // ##gadget
        // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com ##BugID:96378
        public static final int ITEM_TYPE_GADGET = 10;
        /* YUNOS END */
        /**
         * The favorite is a donwloading icon.
         */
        public static final int ITEM_TYPE_SHORTCUT_DOWNLOADING = 8;

        /*YUNOS BEGIN*/
        //##date:2014/02/19 ##author:hao.liuhaolh ##BugID:92481
        //vp install
        /**
         * The favorite is a vpinstall item.
         */
        public static final int ITEM_TYPE_VPINSTALL = 9;
        /*YUNOS END*/

        /**
         * The favorite is a application has no space
         */
        public static final int ITEM_TYPE_NOSPACE_APPLICATION = 100;
        /*YUNOS END*/

        /**
         * The favorite is a clock
         */
        static final int ITEM_TYPE_WIDGET_CLOCK = 1000;

        /**
         * The favorite is a search widget
         */
        static final int ITEM_TYPE_WIDGET_SEARCH = 1001;

        /**
         * The favorite is a photo frame
         */
        static final int ITEM_TYPE_WIDGET_PHOTO_FRAME = 1002;

        /**
         * The appWidgetId of the widget
         *
         * <P>Type: INTEGER</P>
         */
        public static final String APPWIDGET_ID = "appWidgetId";
        
        /**
         * Indicates whether this favorite is an application-created shortcut or not.
         * If the value is 0, the favorite is not an application-created shortcut, if the
         * value is 1, it is an application-created shortcut.
         * <P>Type: INTEGER</P>
         */
        @Deprecated
        static final String IS_SHORTCUT = "isShortcut";

        /**
         * The URI associated with the favorite. It is used, for instance, by
         * live folders to find the content provider.
         * <P>Type: TEXT</P>
         */
        public static final String URI = "uri";

        /**
         * The display mode if the item is a live folder.
         * <P>Type: INTEGER</P>
         *
         * @see android.provider.LiveFolders#DISPLAY_MODE_GRID
         * @see android.provider.LiveFolders#DISPLAY_MODE_LIST
         */
        public static final String DISPLAY_MODE = "displayMode";
        public static final String USER_ID = "userId";
    }
}
