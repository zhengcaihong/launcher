
package com.aliyun.homeshell.editmode;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;

public class WallpapersPreviewAdapter extends ThemeWallpaperBaseAdapter {
    private static final String TAG = "WallpapersPreviewAdapter";

    private final String KEY_ID = "key_id";
    public final static String IS_CHECKED = "used_type";
    //private final String SYSTEM_ID = "sys_id";

    public WallpapersPreviewAdapter(Context context) {
        super(context, ThemeWallpaperBaseAdapter.DATACHANGE_TYPE_WALLPAPER);
    }

    @Override
    protected void populateItem(BaseAttr item, Cursor c) {
        WallpaperAttr wallpaper = (WallpaperAttr) item;
        wallpaper.id = c.getString(c.getColumnIndex(ThemeWallpaperBaseAdapter.ID));
        wallpaper.keyID = c.getString(c.getColumnIndex(KEY_ID));
        wallpaper.isSystem = c.getInt(c.getColumnIndex(ThemeWallpaperBaseAdapter.IS_SYSTEM)) == 1;
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##author:xy83652@alibaba-inc.com
        // ##BugID:(5927523) ##date:2015/4/26
        // ##description: deleted for not used
        //if (wallpaper.isSystem)
        //    wallpaper.sysWallpaperID = Integer.valueOf(c.getString(c.getColumnIndex(SYSTEM_ID)));
        /* YUNOS END PB */
        byte[] data = c.getBlob(c.getColumnIndex(ThemeWallpaperBaseAdapter.THUMBNAIL));
        wallpaper.thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length);
        wallpaper.checked = c.getInt(c.getColumnIndex(IS_CHECKED)) == 1;
    }

    @Override
    protected BaseAttr getNewItem() {
        return new WallpaperAttr();
    }

    class WallpaperAttr extends ThemeWallpaperBaseAdapter.BaseAttr {
        String keyID;
        //int sysWallpaperID;

        @Override
        public String toString() {
            return "WallpaperAttr [keyID=" + keyID + ", id=" + id + ", name=" + name + ", checked=" + checked + "]";
        }

    }
}
