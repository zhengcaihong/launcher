
package com.aliyun.homeshell.hideseat;

import android.content.res.Configuration;
import android.view.KeyEvent;
import android.view.View;

import com.aliyun.homeshell.FocusHelper;

public class HideseatIconKeyListener implements View.OnKeyListener {
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        final Configuration configuration = v.getResources().getConfiguration();
        return FocusHelper
                .handleHotseatButtonKeyEvent(v, keyCode, event, configuration.orientation);
    }
}
