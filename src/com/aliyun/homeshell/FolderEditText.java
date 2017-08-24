package com.aliyun.homeshell;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class FolderEditText extends EditText {

    private Folder mFolder;

    public FolderEditText(Context context) {
        super(context);
    }

    public FolderEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // Catch the back button on the soft keyboard so that we can just close the activity
        if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
            // YUNOS BEGIN
            // ##date:2014/09/16 ##author:hongchao.ghc ##BugID:5236250
            int start = getSelectionStart();
            int oldLength = getText().length();
            mFolder.doneEditingFolderName(true);
            int length = getText().length();
            if (oldLength != length) {
                if (start == oldLength) {
                    start = length;
                } else {
                    start = 0;
                }
            }
            if (start < 0) {
                start = 0;
            }
            setSelection(start);
            // YUNOS END
        }
        return super.onKeyPreIme(keyCode, event);
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setSelection(0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && this.getSelectionStart() == 0)
                || (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && this.getSelectionStart() == this
                        .getText().length())) {
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }
}
