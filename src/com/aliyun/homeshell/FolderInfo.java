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

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;

/**
 * Represents a folder containing shortcuts or apps.
 */
public class FolderInfo extends ItemInfo {

    /**
     * Whether this folder has been opened
     */
    boolean opened;
    //the shortcutinfo for batch operation to icons
    public ShortcutInfo mEditFolderShortcutInfo = null;
    /**
     * The apps and shortcuts
     */
    public ArrayList<ShortcutInfo> contents = new ArrayList<ShortcutInfo>();

    ArrayList<FolderListener> listeners = new ArrayList<FolderListener>();

    public FolderInfo() {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_FOLDER;
    }

    /**
     * Add an app or shortcut
     *
     * @param item
     */
    public void add(ShortcutInfo item) {
        /* YUNOS BEGIN */
        // ## date: 2016/08/05 ## author: yongxing.lyx
        // ## BugID:8593934:duplicated icons on folder after clone.
        contents.remove(item);
        /* YUNOS END */
        contents.add(item);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onAdd(item);
        }
        itemsChanged();
    }

    /**
     * Remove an app or shortcut. Does not change the DB.
     *
     * @param item
     */
    public void remove(ShortcutInfo item) {
        contents.remove(item);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onRemove(item);
        }
        itemsChanged();
    }

    public void setTitle(CharSequence title) {
        this.title = title;
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onTitleChanged(title);
        }
    }

    @Override
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put(LauncherSettings.Favorites.TITLE, title.toString());
    }

    void addListener(FolderListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    void removeListener(FolderListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    void itemsChanged() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onItemsChanged();
        }
    }

    public void invalidate(View v, ShortcutInfo info) {
        for (FolderListener l : listeners)
            l.invalidateFolder(v, info);
    }

    @Override
    public void unbind() {
        super.unbind();
        listeners.clear();
    }
    /* YUNOS BEGIN */
    // ##date:2014/06/03 ##author:yangshan.ys
    // batch operations to the icons in folder
    public boolean isEditFolderInContents() {
        return contents.size() > 0 && mEditFolderShortcutInfo != null && contents.contains(mEditFolderShortcutInfo);
    }
    public ShortcutInfo getmEditFolderShortcutInfo() {
        return mEditFolderShortcutInfo;
    }
    public void setmEditFolderShortcutInfo(ShortcutInfo mEditFolderShortcutInfo) {
        this.mEditFolderShortcutInfo = mEditFolderShortcutInfo;
    }
    public int count() {
        return isEditFolderInContents() ? contents.size() - 1 : contents.size() ;
    }
    /* YUNOS END */

    interface FolderListener {
        public void onAdd(ShortcutInfo item);
        public void onRemove(ShortcutInfo item);
        public void onTitleChanged(CharSequence title);
        public void onItemsChanged();
        public void invalidateFolder(View v, ShortcutInfo info);
    }

    /* YUNOS BEGIN */
    // ## date: 2016/06/20 ## author: yongxing.lyx
    // ## BugID:8410714:edit icon position error after clone.
    public void dumpContent(String mark) {
        StringBuilder sb = new StringBuilder();
        sb.append("dumpContent() ").append(mark);
        for (ShortcutInfo info : contents) {
            sb.append(" [ ");
            if (getmEditFolderShortcutInfo() == info) {
                sb.append("+");
            } else {
                sb.append(info.title);
                if (info.userId > 0) {
                    sb.append(':');
                    sb.append(info.userId);
                }
            }
            sb.append(" ]");
        }
        android.util.Log.i("Launcher.Folder", sb.toString());
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ## date: 2016/06/20 ## author: yongxing.lyx
    // ## BugID:8431132:default folder auto change title when language changed.
    public CharSequence getDisplayTitle(Context context) {
        CharSequence dispTitle = title;
        Resources res = context.getResources();

        if (title != null && title.length() > 0 && title.charAt(0) == '#') {
            dispTitle = null;
            CharSequence resName = title.subSequence(1, title.length());

            int resId = res.getIdentifier(resName.toString(), null, context.getPackageName());
            if (resId > 0) {
                try {
                    dispTitle = res.getText(resId);
                } catch (Resources.NotFoundException nfe) {

                }
            }
            if (dispTitle == null) {
                dispTitle = res.getString(R.string.folder_name);
            }
        }
        return dispTitle;
    }
    /* YUNOS END */
}
