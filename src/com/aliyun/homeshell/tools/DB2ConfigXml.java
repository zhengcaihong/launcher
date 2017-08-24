package com.aliyun.homeshell.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aliyun.homeshell.FolderInfo;
import com.aliyun.homeshell.GadgetItemInfo;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.LauncherAppWidgetInfo;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.utils.Utils;

public class DB2ConfigXml extends Activity {
    private static final String TAG = "DB2ConfigXml";
    private static final String XML_NAME = "default_workspace.xml";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        Button btn = new Button(this);
        btn.setText("db convert config xml");
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                db2configXml(DB2ConfigXml.this);
            }
        });

        root.addView(btn, LayoutParams.MATCH_PARENT);
        root.setGravity(Gravity.CENTER);
        setContentView(root);
    }

    private void db2configXml(Context context) {
        // 1. 从数据库中产生数据
        String buf = loadData(context);
        if (buf == null) {
            return;
        }

        // 2. 写文件到运行时路径中
        writeToLocal(context, buf);

        // 3. 写文件到 sdcard中
        writeToSdcard(context, buf);
    }

    private static String loadData(Context context) {
        List<ItemInfo> items = new ArrayList<ItemInfo>();
        List<LauncherAppWidgetInfo> appwidgets = new ArrayList<LauncherAppWidgetInfo>();
        List<FolderInfo> folders = new ArrayList<FolderInfo>();
        List<GadgetItemInfo> gadgets = new ArrayList<GadgetItemInfo>();

        for (ItemInfo item : LauncherModel.sBgWorkspaceItems) {
            if (item instanceof FolderInfo) {
                folders.add((FolderInfo) item);
            } else if (item instanceof GadgetItemInfo) {
                gadgets.add((GadgetItemInfo)item);
            } else {
                items.add(item);
            }
            Log.d(TAG, "title : " + item.title);
        }

        for (ItemInfo item : LauncherModel.sBgAppWidgets) {
            if (item instanceof LauncherAppWidgetInfo) {
                Log.d(TAG, "item : " + item.title);
                appwidgets.add((LauncherAppWidgetInfo) item);
            }
        }

        String res = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                     + "<favorites xmlns:launcher=\"http://schemas.android.com/apk/res/com.aliyun.homeshell\">\n";

        for (int i = 0; i < gadgets.size(); i++) {
            GadgetItemInfo gadget = gadgets.get(i);
            res +=  "    <gadget\n" +
                    "        launcher:packageName=\"com.aliyun.homeshell\"\n" +
                    "        launcher:screen=\"" + gadget.screen + "\"\n" +
                    "        launcher:x=\"" + gadget.cellX + "\"\n" +
                    "        launcher:y=\"" + gadget.cellY + "\"\n" +
                    "        launcher:spanX=\"" + gadget.spanX + "\"\n" +
                    "        launcher:spanY=\"" + gadget.spanY + "\"\n" +
                    "        launcher:title=\"" + "@string/clock_apollo_4x1" + "\"/>\n\n";
        }

        int container = 0;
        for (int i = 0; i < folders.size(); i++) {
            FolderInfo folder = folders.get(i);

            int screen = folder.screen;
            int cellX = folder.cellX;
            int cellY = folder.cellY;
            String title = getTitleResourceId(folder.title.toString());

            res += "    <folder\n" +
                   "        launcher:screen=\"" + screen + "\"\n" +
                   "        launcher:x=\"" + cellX + "\"\n" +
                   "        launcher:y=\"" + cellY + "\"\n" +
                   "        launcher:title=\"" + title + "\" >\n";

            container++;
            ArrayList<ShortcutInfo> children = folder.contents;
            for (int j = 0; j < children.size(); j++) {
                ShortcutInfo child = (ShortcutInfo) children.get(j);
                res += "        <favorite\n" +
                       "            launcher:packageName=\"" + child.intent.getComponent().getPackageName() + "\"\n" +
                       "            launcher:className=\"" + child.intent.getComponent().getClassName() + "\"\n" +
                       "            launcher:screen=\"" + child.screen + "\"\n" +
                       "            launcher:x=\"" + child.cellX + "\"\n" +
                       "            launcher:y=\"" + child.cellY + "\"\n" +
                       "            launcher:container=\"" + container + "\" />\n";
            }

           res += "    </folder>\n\n";
        }

        for (int i = 0; i < folders.size(); i++) {
            container ++;
            FolderInfo folder = folders.get(i);
            ArrayList<ShortcutInfo> children = folder.contents;
            for (int j = 0; j < children.size(); j++) {
                ShortcutInfo child = (ShortcutInfo) children.get(j);
                res += "    <favorite\n" +
                       "        launcher:packageName=\"" + child.intent.getComponent().getPackageName() + "\"\n" +
                       "        launcher:className=\"" + child.intent.getComponent().getClassName() + "\"\n" +
                       "        launcher:screen=\"" + child.screen + "\"\n" +
                       "        launcher:x=\"" + child.cellX + "\"\n" +
                       "        launcher:y=\"" + child.cellY + "\"\n" +
                       "        launcher:container=\"" + container + "\" />\n";
            }
        }

        for (int j = 0; j < items.size(); j++) {
            ShortcutInfo favorite = (ShortcutInfo) items.get(j);
            res += "    <favorite\n" +
                   "        launcher:packageName=\"" + favorite.intent.getComponent().getPackageName() + "\"\n" +
                   "        launcher:className=\"" + favorite.intent.getComponent().getClassName() + "\"\n" +
                   "        launcher:screen=\"" + favorite.screen + "\"\n" +
                   "        launcher:x=\"" + favorite.cellX + "\"\n" +
                   "        launcher:y=\"" + favorite.cellY + "\"\n" +
                   "        launcher:container=\"" + favorite.container + "\" />\n";
        }

        for (int n = 0; n < appwidgets.size(); n++) {
            LauncherAppWidgetInfo widget = appwidgets.get(n);
            res += "    <appwidget\n" +
                   "        launcher:packageName=\"" + widget.hostView.getAppWidgetInfo().provider.getPackageName() + "\"\n" +
                   "        launcher:className=\"" + widget.hostView.getAppWidgetInfo().provider.getClassName()  + "\"\n" +
                   "        launcher:screen=\"" + widget.screen + "\"\n" +
                   "        launcher:x=\"" + widget.cellX + "\"\n" +
                   "        launcher:y=\"" + widget.cellY + "\"\n" +
                   "        launcher:spanX=\"" + widget.spanX + "\"\n" +
                   "        launcher:spanY=\"" + widget.spanY + "\" />\n";
        }

        res += "</favorites>";
        return res;
    }

    private static String getTitleResourceId(String title) {
        // TODO :
        return "@string/title_folder_tools";
    }

    private String getLocalConfigName(Context context) {
        String name = context.getFilesDir().getAbsolutePath();
        name += "/" + XML_NAME;

        Log.d(TAG, "getConfigName name : " + name);
        return name;
    }

    private String getSdcardConfigName(Context context) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();

        if (path != null) {
            path += "/" + XML_NAME;
        }
        return path;
    }

    private void remove(String name) {
        if (name == null) {
            return;
        }

        File f = new File(name);
        if (f.exists()) {
            f.delete();
        }
    }

    private void writeToLocal(Context context, String buf) {
        String name = getLocalConfigName(context);

        remove(name);

        write(context, name, buf);
    }

    private void writeToSdcard(Context context, String buf) {
        if (Utils.isInUsbMode()) {
            Log.e(TAG, "Failed in writeToSdcard : sd card unavailable.");
            makeToast(context, "sd card unavailable, please exit usb mode.");
            return;
        }

        String name = getSdcardConfigName(context);

        remove(name);

        write(context, name, buf);
    }

    private void write(Context context, String name, String buf) {
        try {
            File f = new File(name);
            FileWriter fw = new FileWriter(f);
            BufferedWriter writer = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(writer);

            pw.println(buf);

            fw.flush();

            pw.close();
            writer.close();
            fw.close();
        } catch (Exception e) {
            String info = "Failed in write : " + e.getMessage();
            Log.e(TAG, info);
            makeToast(context, info);
            return;
        }

        makeToast(context, "write " + name + " sucessfully.");
    }

    private void makeToast(Context context, String info) {
        Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
    }
}
