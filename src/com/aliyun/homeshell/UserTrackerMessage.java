package com.aliyun.homeshell;

public class UserTrackerMessage {
    public static final String LABEL_LAUNCHER = "LAUNCHER";
    public static final String LABEL_WIDGET_LOADER = "WIDGET_LOADER";
    public static final String LABEL_SCREEN_MANAGER = "SCREEN_MANAGER";

    public static final String MSG_ENTRY_MENU = "ENTRY_MENU";
    public static final String MSG_ENTRY_MENU_THEME = "ENTRY_MENU_THEME";
    public static final String MSG_ENTRY_MENU_WALLPAPER = "ENTRY_MENU_WALLPAPER";
    public static final String MSG_ENTRY_MENU_SOUND = "ENTRY_MENU_SOUND";
    public static final String MSG_ENTRY_MENU_SETTINGS = "ENTRY_MENU_SETTINGS";
    /* YUNOS BEGIN */
    // ##date:2014/06/05 ##author:hongchao.ghc
    // add luancher setting Buried
    public static final String MSG_ENTRY_MENU_LAUNCHER_SETTING = "ENTRY_MENU_LAUNCHER_SETTING";
    // Launcher setting
    public static final String MSG_LAUNCHER_SETTING = "LAUNCHER_SETTING";
    public static final String MSG_LAUNCHER_SETTING_EFFECTS = "EFFECTS";
    public static final String MSG_LAUNCHER_SETTING_LAYOUT = "LAYOUT";
    public static final String MSG_LAUNCHER_SETTING_MARK_NEW = "MARK_NEW";
    public static final String MSG_LAUNCHER_SETTING_NOTIFICATION_MARK = "MARK_3RD_NOTIFICATION";
    /* YUNOS BEGIN */
    // ##date:2014/06/18 ##author:hongchao.ghc ##BugID:130477
    // add launcher setting arrange Buried
    public static final String MSG_LAUNCHER_SETTING_ARRANGE = "ARRANGE";
    /* YUNOS END */
    public static final String MSG_LAUNCHER_SETTING_LAUNCHER_SETTING_EFFECTS = "LAUNCHER_SETTING_EFFECTS";
    public static final String MSG_LAUNCHER_SETTING_EFFECTS_RESULT = "EFFECTS_RESULT";
    public static final String MSG_LAUNCHER_SETTING_LAUNCHER_SETTING_LAYOUT = "LAUNCHER_SETTING_LAYOUT";
    public static final String MSG_LAUNCHER_SETTING_LAYOUT_RESULT = "LAYOUT_RESULT";
    /* YUNOS END */
    public static final String MSG_ADD_FOLDER = "ADD_FOLDER";
    public static final String MSG_ADD_WIDGET = "ADD_WIDGET";
    public static final String MSG_REMOVE_WIDGET = "REMOVE_WIDGET";
    public static final String MSG_REMOVE_APP = "REMOVE_APP";
    public static final String MSG_DRAG_ICON = "DRAG_ICON";
    public static final String MSG_DRAG_SCREEN_RESULT = "DRAG_SCREEN_RESULT";
    public static final String MSG_ENTRY_WIDGET_LOADER = "ENTRY_WIDGET_LOADER";
    public static final String MSG_ENTRY_SCREEN_MANAGER = "ENTRY_SCREEN_MANAGER";

    public static final String MSG_DRAG_TO_DELETE = "DRAG_TO_DELETE";
    public static final String MSG_FLING_TO_DELETE = "FLING_TO_DELETE";
    public static final String MSG_FLING_TO_MOVE = "FLING_TO_MOVE";
    public static final String MSG_PUSH_TO_TALK = "PUSH_TO_TALK";
    public static final String MSG_APP_DUPLICATE_ADD = "APP_DUPLICATE_ADD";
    public static final String MSG_APP_DUPLICATE_DELATE = "APP_DUPLICATE_DELATE";

    /*YUNOS BEGIN*/
    //##date:2014/03/20 ##author:hao.liuhaolh ##BugID:103304
    //user track in vp install
    public static final String MSG_VP_ITEM_INSTALL = "VP_ITEM_INSTALL";
    public static final String MSG_VP_ITEM_INSTALL_SUCCESS = "VP_ITEM_INSTALL_SUCCESS";
    public static final String MSG_VP_ITEM_DELETE = "VP_ITEM_DELETE";
    /*YUNOS END*/

    public static final String MSG_ENTER_LIFE_CENTER_PAGE = "ENTER_LIFE_CENTER_PAGE";

    public static final String MSG_OPEN_HIDESEAT = "OPEN_HIDESEAT";
    public static final String MSG_CLOSE_HIDESEAT = "CLOSE_HIDESEAT";

    public static final String MSG_THREE_FINGER_LOCK = "THREE_FINGER_LOCK";

    public static final String MSG_START_APPLICATION_DOCK = "START_APPLICATION_DOCK";
    /* YUNOS BEGIN */
    // ##date:2014/06/18 ##author:hongchao.ghc ##BugID:130639
    // add launcher setting looping Buried
    public static final String MSG_LAUNCHER_SETTING_LOOPING = "LOOPING";
    /* YUNOS END */
    public static final String MSG_FAVORITE_APP = "pop_apps";

    /* for big card */
    public static final String MSG_CARD_SLIDE_UP = "CARD_SLIDE_UP";
    public static final String MSG_CARD_READ = "CARD_READ";
    public static final String MSG_CARD_CLICKITEM = "CARD_CLICKITEM";

    /* for aged mode & notification select */
    public static final String MSG_DIRECT_DIAL_CLICK = "DIRECT_DIAL_CLICK";
    @Deprecated public static final String MSG_MARK_NOTIFICATION = "MARK_NOTIFICATION"; // deprecated since 6406561
    public static final String MSG_MARK_NOTIFICATION_SELECT = "MARK_NOTIFICATION_SELECT";
    public static final String MSG_EDIT_LOCK = "EDIT_LOCK";

    //BugID:5695121:userTrack for card and launcher stay time. hao.liuhaolh
    public static final String MSG_LAUNCHER_STAY_TIME = "LAUNCHER_STAY_TIME";
    public static final String MSG_CARD_STAY_TIME = "CARD_STAY_TIME";

    //BugID:5717551:add userTrack
    public static final String MSG_RENAME_FOLDER = "RENAME_FOLDER";
    public static final String MSG_HIDESEAT_IN = "HIDESEAT_IN";
    public static final String MSG_HIDESEAT_OUT = "HIDESEAT_OUT";
    public static final String MSG_HIDESEAT_CLICK = "HIDESEAT_CLICK";
    public static final String MSG_WIDGET_STATUS = "WIDGET_STATUS";
    public static final String MSG_SHORTCUT_STATUS = "SHORTCUT_STATUS";
    public static final String MSG_ADD_GADGET = "ADD_GADGET";
    public static final String MSG_REMOVE_GADGET = "REMOVE_GADGET";

    public static final String MSG_CLICK_WIDGET = "CLICK_WIDGET";

    public static final String MSG_REMOVE_FOLDER = "REMOVE_FOLDER";
    public static final String MSG_EFFECT_RESULT = "EFFECT_RESULT";

    //BugID:5854395:add userTrack
    public static final String MSG_INSTALL_SHORTCUT ="INSTALL_SHORTCUT";
    public static final String MSG_LOOP_LAUNCHER_RESULTS = "MSG_LOOP_LAUNCHER_RESULTS";

    // for launcherEditMode by yangshan.ys
    public static final String MSG_PRESS_BLANK = "PRESS_BLANK";
    public static final String MSG_ENTRY_MENU_EFFECTS = "ENTRY_MENU_EFFECTS";
    public static final String MSG_ENTRY_MENU_WIDGET = "ENTRY_MENU_WIDGET";
    public static final String MSG_ENTRY_MENU_THEME_SELECT = "ENTRY_MENU_THEME_SELECT";
    public static final String MSG_ENTRY_MENU_WALLPAPER_SELECT = "ENTRY_MENU_WALLPAPER_SELECT";
    public static final String MSG_ENTRY_MENU_EFFECTS_SELECT = "ENTRY_MENU_EFFECTS_SELECT";
    public static final String MSG_ENTRY_MENU_WIDGET_SELECT = "ENTRY_MENU_WIDGET_SELECT";
    public static final String MSG_ENTRY_MENU_WIDGET_SELECT_CLICK = "ENTRY_MENU_WIDGET_SELECT_CLICK";
    public static final String MSG_ENTRY_MENU_ARRANGE = "ENTRY_MENU_ARRANGE";
    public static final String MSG_ENTRY_MENU_ARRANGE_CURRENTSCREEN = "ENTRY_MENU_ARRANGE_CURRENTSCREEN";
    public static final String MSG_ENTRY_MENU_ARRANGE_ALLSCREEN = "ENTRY_MENU_ARRANGE_ALLSCREEN";
    public static final String MSG_ENTRY_MENU_ARRANGE_CLASSIFY = "ENTRY_MENU_ARRANGE_CLASSIFY";

    public static final String MSG_ENTRY_MENU_ARRANGE_ADDSCREEN = "ENTRY_MENU_ARRANGE_ADDSCREEN";
    public static final String MSG_ENTRY_MENU_ARRANGE_DELETESCREEN = "ENTRY_MENU_ARRANGE_DELETESCREEN";
    public static final String MSG_FOLDER_CLICK_ADDAPP = "FOLDER_CLICK_ADDAPP";
    public static final String MSG_FOLDER_ADDAPP = "FOLDER_ADDAPP";
    public static final String MSG_Entry_Menu_Arrange_Icon_One = "Entry_Menu_Arrange_Icon_One";
    public static final String MSG_Entry_Menu_Arrange_Icon_Multi = "Entry_Menu_Arrange_Icon_Multi";
    public static final String MSG_Entry_Menu_Arrange_Widget = "Entry_Menu_Arrange_Widget";

    // for auto classify and arrange icons
    public static final String MSG_DOUBLECLICK_ARRANGE = "DOUBLECLICK_ARRANGE";
    public static final String MSG_DOUBLECLICK_ARRANGE_CURRENTSCREEN = "DOUBLECLICK_ARRANGE_CURRENTSCREEN";
    public static final String MSG_DOUBLECLICK_ARRANGE_ALLSCREEN = "DOUBLECLICK_ARRANGE_ALLSCREEN";
    public static final String MSG_DOUBLECLICK_ARRANGE_CLASSIFY = "DOUBLECLICK_ARRANGE_CLASSIFY";

    public static final String MSG_CONFIGURATION = "Configuration";
    public static final String MSG_PINCH_ENTRY_MENU = "PINCH_ENTRY_MENU";

    public static final String MSG_UPDATE_APP_CATEGORY_INFO ="MSG_UPDATE_APP_CATEGORY_INFO";

    public static final class Key {
        public static final String RESULT = "result";
        public static final String PATTERN = "pattern";
        public static final String POSITION = "position";
        public static final String SCREEN = "screen";
        public static final String IF_CROSS_SCREEN = "if_cross_screen";
    }
}
