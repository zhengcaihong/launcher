
package com.aliyun.homeshell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.aliyun.homeshell.model.LauncherModel;
//import com.aliyun.homeshell.datamodeItemInfoHelper;
//import com.aliyun.homeshell.folder.UserFolderInfo;
import com.aliyun.homeshell.IconDigitalMarkHandler;

public class NotificationReceiver extends BroadcastReceiver {
   private static String TAG = "NotificationReceiver";
   private IconDigitalMarkHandler mIconDigitalMarkHandler = null;
   private Intent mIntent = new Intent();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_PACKAGE_DATA_CLEARED)) {
                Uri data = intent.getData();
                String pkgName = null;
                if (data != null) {
                    pkgName = data.getSchemeSpecificPart();
                }

                if ("fm.xiami.yunos".equals(pkgName) || "com.xiami.walkman".equals(pkgName)) {
                    mIntent.setAction("com.aliyun.homeshell.action.CLEAR_COVER");
                    context.sendBroadcast(mIntent);
                }
            }
        }
        //bugid: 5232096:  to update icon mark in this receiver only before workspace is not loaded, this is needed in the following case:
        //the homeshell is killed or not yet launched, and we need to update icon mark. 
        //in this case, system will launch LauncherApplicaiton and call NotificationReceiver.onRecive(). After LauncherApplicaiton
        //is launched, LauncherModel.onReceive() will take over
        Log.d(TAG, "hongxing NotificationReceiver:intent=" + intent);
        LauncherApplication app = (LauncherApplication)context.getApplicationContext();
        LauncherModel model = app.getModel();
        if (model != null && model.isWorkspaceLoaded()) {
            //Log.d(TAG, "hongxing model.isWorkspaceLoaded()"+model.isWorkspaceLoaded());
            return;
        }
        //Log.d(TAG, "222 hongxing model.isWorkspaceLoaded()"+model.isWorkspaceLoaded());
        mIconDigitalMarkHandler = IconDigitalMarkHandler.getInstance();
        mIconDigitalMarkHandler.handleNotificationIntent(context, intent);
    }
}
