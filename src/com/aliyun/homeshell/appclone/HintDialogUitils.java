
package com.aliyun.homeshell.appclone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.icon.BubbleController;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.setting.HomeShellSetting;

public class HintDialogUitils {
    private static final String TAG = "HintDialogUitils";
    private static Boolean sIsAppCloneHintDialogShowed = null;
    private static final String APP_CLONE_HINT_SHOWED = "app_clone_hint_showed";
    private static AlertDialog sDialog;

    public static void showHintDialog(final Context context, final View iconView, int rid_logo,
            int rid_title, int rid_msg, int rid_button_txt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.hint_dialog, null);
        builder.setView(view);
        /* YUNOS BEGIN */
        // ## date: 2016/06/21 ## author: yongxing.lyx
        // ## BugID:8434736:click effect hasn't been reset when press back key.
        builder.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dif) {
                BubbleTextView bubbleView = (BubbleTextView) iconView;
                bubbleView.setShowClickEffect(false);
                bubbleView.invalidate();
            }
        });
        /* YUNOS END */
        builder.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface di, int which, KeyEvent keyEvent) {

                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_MENU:
                    case KeyEvent.KEYCODE_BACK:
                        if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                            di.dismiss();
                            return true;
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }

        });
        final AlertDialog dialog = builder.create();
        // logo
        ImageView logoView = (ImageView) view.findViewById(R.id.hint_dialog_logo);
        logoView.setImageResource(rid_logo);
        // titile
        TextView title = (TextView) view.findViewById(R.id.hint_dialog_title);
        title.setText(rid_title);
        // message
        TextView msgView = (TextView) view.findViewById(R.id.hint_dialog_msg_1);
        msgView.setText(rid_msg);
        // button
        Button btn = (Button) view.findViewById(R.id.hint_dialog_btn);
        btn.setText(rid_button_txt);
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                if (context instanceof Launcher && iconView.getTag() instanceof ShortcutInfo) {
                    ShortcutInfo info = (ShortcutInfo) iconView.getTag();
                    boolean success = ((Launcher) context).startActivitySafely(iconView,
                            info.intent, iconView.getTag());
                    if (success) {
                        info.setIsNewItem(false);
                        if (iconView instanceof BubbleTextView) {
                            BubbleController.updateView((BubbleTextView) iconView);
                            /* YUNOS BEGIN */
                            // ## date: 2016/06/28 ## author: yongxing.lyx
                            // ## BugID:8443502:show new mark after clicked
                            // wechat and changed font size.
                            LauncherModel.modifyItemNewStatusInDatabase(context, info, false);
                            Launcher.asyncIncreaseAppLaunchCount(info);
                            /* YUNOS END */
                        }
                    }
                }
                dialog.dismiss();
            }
        });

        dialog.setCanceledOnTouchOutside(true);
        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            dialog.show();
            sDialog = dialog;
            setSharedPrefAppCloneHintShowed(context);
        }
    }

    public static void showAppCloneHintDialog(Context context, final View iconView) {
        showHintDialog(context, iconView, R.drawable.appclone_hint_logo,
                R.string.appclone_hint_title, R.string.appclone_hint_msg,
                R.string.appclone_hint_btn_text);
    }

    private static boolean isAppCloneHintDialogShowed(Context context) {
        if (sIsAppCloneHintDialogShowed == null) {
            String spKey = LauncherApplication.getSharedPreferencesKey();
            SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey,
                    Context.MODE_PRIVATE);
            sIsAppCloneHintDialogShowed = sp.getBoolean(APP_CLONE_HINT_SHOWED, false);
        }
        return sIsAppCloneHintDialogShowed;
    }

    private static void setSharedPrefAppCloneHintShowed(Context context) {
        sIsAppCloneHintDialogShowed = true;
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey,
                Context.MODE_PRIVATE);
        sp.edit().putBoolean(APP_CLONE_HINT_SHOWED, true).commit();
    }

    public static boolean needShowAppCloneHintDialog(Context context, Object tag) {

        if (!isAppCloneHintDialogShowed(context)
                && (tag instanceof ShortcutInfo && AppCloneManager.getInstance().isClonable(
                        ((ShortcutInfo) tag).getPackageName()))) {
            return true;
        }
        return false;
    }

    public static boolean dismissAppCloneHintDialogIfNeed() {
        if (sDialog != null && sDialog.isShowing()) {
            sDialog.dismiss();
            sDialog = null;
            return true;
        }
        return false;
    }
}
