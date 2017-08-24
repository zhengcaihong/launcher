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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.aliyun.homeshell.model.LauncherModel;

import java.util.ArrayList;
import java.util.Iterator;

public class UninstallShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_UNINSTALL_SHORTCUT =
            "com.aliyun.homeshell.action.UNINSTALL_SHORTCUT";

    /*YUNOS BEGIN*/
    //##date:2013/11/19 ##author:hao.liuhaolh
    //support bookmark, content and cloud app
    public static final String CATEGORY_SHORTCUT_CLOUDAPP =
            "com.aliyun.homeshell.category.SHORTCUT_CLOUDAPP";
    /*YUNOS END*/

    // The set of shortcuts that are pending uninstall
    private static ArrayList<PendingUninstallShortcutInfo> mUninstallQueue =
            new ArrayList<PendingUninstallShortcutInfo>();

    // Determines whether to defer uninstalling shortcuts immediately until
    // disableAndFlushUninstallQueue() is called.
    private static boolean mUseUninstallQueue = false;

    private static class PendingUninstallShortcutInfo {
        Intent data;

        public PendingUninstallShortcutInfo(Intent rawData) {
            data = rawData;
        }
    }

    public void onReceive(Context context, Intent data) {
        if (!ACTION_UNINSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }

        PendingUninstallShortcutInfo info = new PendingUninstallShortcutInfo(data);
        if (mUseUninstallQueue) {
            mUninstallQueue.add(info);
        } else {
            processUninstallShortcut(context, info);
        }
    }

    static void enableUninstallQueue() {
        mUseUninstallQueue = true;
    }

    static void disableAndFlushUninstallQueue(Context context) {
        mUseUninstallQueue = false;
        Iterator<PendingUninstallShortcutInfo> iter = mUninstallQueue.iterator();
        while (iter.hasNext()) {
            processUninstallShortcut(context, iter.next());
            iter.remove();
        }
    }

    private static void processUninstallShortcut(Context context,
            PendingUninstallShortcutInfo pendingInfo) {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sharedPrefs = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);

        final Intent data = pendingInfo.data;

        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        synchronized (app) {
            app.getModel().unInstallShortcutInWorkerThread(context, data, sharedPrefs);
            //removeShortcut(context, data, sharedPrefs);

            /* YUNOS BEGIN */
            // ## date: 2016/07/06 ## author: yongxing.lyx
            // ## BugID:8448699:can't remove shortcut in agemode which subscribed in normal mode.
            String title = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
            if (title != null) {
                LauncherModel.deleteShortcutFromAnotherTableByTitle(context, title);
            }
            /* YUNOS BEGIN */
        }
    }

//    private static void removeShortcut(Context context, Intent data,
//            final SharedPreferences sharedPrefs) {
//        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
//        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
//        boolean duplicate = data.getBooleanExtra(Launcher.EXTRA_SHORTCUT_DUPLICATE, true);
//
//        if (intent != null && name != null) {
//            final ContentResolver cr = context.getContentResolver();
//            Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
//                new String[] { LauncherSettings.Favorites._ID, LauncherSettings.Favorites.INTENT },
//                LauncherSettings.Favorites.TITLE + "=?", new String[] { name }, null);
//
//            final int intentIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
//            final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
//
//            boolean changed = false;
//
//            try {
//                while (c.moveToNext()) {
//                    try {
//                        if (intent.filterEquals(Intent.parseUri(c.getString(intentIndex), 0))) {
//                            final long id = c.getLong(idIndex);
//                            final Uri uri = LauncherSettings.Favorites.getContentUri(id, false);
//                            cr.delete(uri, null, null);
//                            changed = true;
//                            if (!duplicate) {
//                                break;
//                            }
//                        }
//                    } catch (URISyntaxException e) {
//                        // Ignore
//                    }
//                }
//            } finally {
//                c.close();
//            }
//
//            if (changed) {
//                cr.notifyChange(LauncherSettings.Favorites.CONTENT_URI, null);
//                Toast.makeText(context, context.getString(R.string.shortcut_uninstalled, name),
//                        Toast.LENGTH_SHORT).show();
//            }
//
//            // Remove any items due to be animated
//            boolean appRemoved;
//            Set<String> newApps = new HashSet<String>();
//            newApps = sharedPrefs.getStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, newApps);
//            synchronized (newApps) {
//                do {
//                    appRemoved = newApps.remove(intent.toUri(0).toString());
//                } while (appRemoved);
//            }
//            if (appRemoved) {
//                final Set<String> savedNewApps = newApps;
//                new Thread("setNewAppsThread-remove") {
//                    public void run() {
//                        synchronized (savedNewApps) {
//                            SharedPreferences.Editor editor = sharedPrefs.edit();
//                            editor.putStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY,
//                                    savedNewApps);
//                            if (savedNewApps.isEmpty()) {
//                                // Reset the page index if there are no more items
//                                editor.putInt(InstallShortcutReceiver.NEW_APPS_PAGE_KEY, -1);
//                            }
//                            editor.commit();
//                        }
//                    }
//                }.start();
//            }
//        }
//    }
}
