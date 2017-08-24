package com.aliyun.homeshell.gadgets;

import com.aliyun.homeshell.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

public class OneKeyAccelerationCreateShortcut extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent addShortCut = new Intent();
        addShortCut.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                getResources().getString(R.string.str_one_key_accelerate));
        Parcelable icon = Intent.ShortcutIconResource.fromContext(this,
                R.drawable.preview_clean_up);
        addShortCut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        Intent outStartIntent = new Intent(this, GadgetsMainActivity.class);
        outStartIntent.putExtra(GadgetsConsts.TYPE_SHORTCUT, GadgetsConsts.TYPE_ONE_KEY_ACCELERATE);
        addShortCut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, outStartIntent);
        setResult(RESULT_OK, addShortCut);
        finish();
    }
}
