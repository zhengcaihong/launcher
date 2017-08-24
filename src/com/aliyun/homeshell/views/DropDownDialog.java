/**
 * Project: SecuritCenter 3.0
 *
 * Copyright 2013 Alibaba Group.
 */
package com.aliyun.homeshell.views;

import android.app.AlertDialog;
import android.graphics.AvoidXfermode;
import android.graphics.AvoidXfermode.Mode;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.FolderIcon;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.icon.BubbleController;
import com.aliyun.homeshell.icon.IconManager;


/**
 * Created by jun.dongj on 13-6-26.
 */
public class DropDownDialog extends AlertDialog {
	private static final int DIALOG_SHOW = 0;
	private static final int DIALOG_DISMISS = 1;
	private static final int DIALOG_HIDE = 2;
	private int mCancelFlag = DIALOG_SHOW;
	private Window mWindow = null;
	private View mParent = null;
	private View mContainer = null;
	private TranslateAnimation mSlideUpAnimation;
	private TranslateAnimation mSlideDownAnimation;
    /* YUNOS BEGIN */
    // module(DELETE PROCESS)
    // ##date:2014/03/27 & 2014/04/04 ##author:yaodi.yd ##BugID:105430 & 107622
    // optimized dialog animation effect
    private long mAnimationTime = 300;
    /* YUNOS END */
	private SlideAnimationListener mSlideAnimationListener = null;
	private String mPositiveButtonText = null;
	private String mNegativeButtonText = null;
	private View.OnClickListener mPositiveListener = null;
	private View.OnClickListener mNegativeListener = null;
	private Button mPositiveButton;
	private Button mNegetiveButton;
    /* YUNOS BEGIN */
    // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
    // private CheckBox addtioncheckbox = null;
    /* YUNOS END */
	private ImageView mIcon;
	private Bitmap mIconSrc;
    private TextView mTitle;
    private String mTitleText;

	public DropDownDialog(Launcher context) {
		super(context, R.style.dialog);
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mWindow = this.getWindow();
		/*lxd added kelude#5112222*/
		int flags = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
		final WindowManager.LayoutParams wlp = mWindow.getAttributes();
		wlp.flags |= flags;
		mWindow.setAttributes(wlp);
		/*lxd add ended*/
		mWindow.setContentView(R.layout.dropdown_dialog);
		mWindow.setGravity(Gravity.TOP);
		mWindow.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		mWindow.setBackgroundDrawable(new ColorDrawable(0));
		mContainer = mWindow.findViewById(R.id.container);
		mParent = findViewById(R.id.layout_parent);
		mIcon = (ImageView)findViewById(R.id.img_icon);
		mPositiveButton = (Button) findViewById(R.id.btn_confirm);
		mNegetiveButton = (Button) findViewById(R.id.btn_cancel);
        mTitle = (TextView) findViewById(R.id.txt_title);

		mSlideAnimationListener = new SlideAnimationListener(this);

        mSlideUpAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f);
		mSlideUpAnimation.setDuration(mAnimationTime);
		mSlideUpAnimation.setAnimationListener(mSlideAnimationListener);

        mSlideDownAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
		mSlideDownAnimation.setDuration(mAnimationTime);
		mSlideDownAnimation.setAnimationListener(mSlideAnimationListener);
        /* YUNOS BEGIN */
        // module(DELETE PROCESS)
        // ##date:2014/03/27 ##author:yaodi.yd ##BugID:105430
        // optimized dialog animation effect
        DecelerateInterpolator interpolator = new DecelerateInterpolator(2);
        mSlideUpAnimation.setInterpolator(interpolator);
        mSlideDownAnimation.setInterpolator(interpolator);
        /* YUNOS END */
    }

    @Override
    protected void onStart() {
        super.onStart();
        initDialog();
        mPositiveButton.setEnabled(true);
        mContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public void show() {
        super.show();
        mContainer.startAnimation(mSlideDownAnimation);
    }

	private void initDialog() {
		mIcon.setImageBitmap(mIconSrc);
        /* YUNOS BEGIN */
        // ##date:2014/09/8 ##author:xindong.zxd ##BugID:5224576
        // remove the application, pops the top frame icon text color is not the same when it on the desktop.
        mIcon.setAlpha(1.0f);
        mContainer.setAlpha(1.0f);
        /* YUNOS END */
		mPositiveButton.setText(mPositiveButtonText);
		mPositiveButton.setOnClickListener(mPositiveListener);
		mNegetiveButton.setText(mNegativeButtonText);
		mNegetiveButton.setOnClickListener(mNegativeListener);
                mTitle.setText(mTitleText);
	}

	public void dismiss(boolean animated) {
	    mPositiveButton.setEnabled(false);
	    mCancelFlag = DIALOG_DISMISS;
	    mContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
	    if (animated) {
	        mContainer.startAnimation(mSlideUpAnimation);
	    } else {
	        mSlideAnimationListener.onAnimationEnd(mSlideUpAnimation);
	    }
	}

	@Override
	public final void dismiss() {
	    dismiss(true);
	}

	public boolean getChecked() {
		boolean checked = false;
        /* YUNOS BEGIN */
        // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
        // if (addtioncheckbox != null) {
        // checked = addtioncheckbox.isChecked();
        // }
        /* YUNOS END */
		return checked;
	}

    /**
     * Set Dialog icon on the left.Change the title color from while to black(BugID:5239064)
     * @param icon the Bitmap which will be show on the left of Dialog
     * @param originalView icon was genereted from this View
     * @author wenliang.dwl
     */
    public DropDownDialog setIcon(Bitmap icon, View originalView) {
    /* YUNOS BEGIN */
    // ##date:2014/7/23 ##author:yangshan.ys##140049
    // for 3*3 layout
        if (AgedModeUtil.isAgedMode()) {
            icon = Bitmap.createBitmap(icon, 0, 0, icon.getWidth(), icon.getHeight(),
                    AgedModeUtil.sScaleDown, false);
        }
    /*YUNOS END*/
        mIconSrc = icon;
        int iconTitleDivider = icon.getHeight();
    /* YUNOS BEGIN */
    // ##date:2014/7/23 ##author:yangshan.ys##140049
    // for 3*3 layout
        IconManager im = ((LauncherApplication) LauncherApplication.getContext()).getIconManager();
        if (im == null) {
            im = new IconManager(getContext());
        }
    /*YUNOS END*/
        if( originalView instanceof BubbleTextView ){
            BubbleTextView v = (BubbleTextView)originalView;
            if( im.supprtCardIcon() && !v.getMode().isHotseatOrHideseat() ) return this;
            iconTitleDivider = v.getCompoundPaddingTop();
        }else if( originalView instanceof FolderIcon ){
            FolderIcon v = (FolderIcon)originalView;
            if( im.supprtCardIcon() && !v.isInHotseat() ) return this;
            iconTitleDivider = v.getTitleText().getExtendedPaddingTop();
        }
        if (AgedModeUtil.isAgedMode()) {
            return this;
        }
/* TODO commented by xiaodong.lxd 
 * maybe someday reopen this for day mode and night mode
 *        Canvas canvas = new Canvas(mIconSrc);
        Paint p = new Paint();
        @SuppressWarnings("deprecation")
        AvoidXfermode mode = new AvoidXfermode(Color.WHITE, 255, Mode.TARGET);
        p.setXfermode(mode);
        p.setColor(Color.BLACK);
        canvas.drawRect(0, iconTitleDivider, mIconSrc.getWidth(), mIconSrc.getHeight(), p);*/

        return this;
    }

	public void setTitle(String hint, String title) {
		mTitleText = String.format(hint, title);
	}

	public DropDownDialog setPositiveButton(String text,
			final View.OnClickListener listener) {
		mPositiveButtonText = text;
		mPositiveListener = listener;
		return this;
	}

	public DropDownDialog setNegativeButton(String text) {
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				DropDownDialog.this.dismiss();
			}
		};
		setNegativeButton(text, listener);
		return this;
	}

	public DropDownDialog setNegativeButton(String text,
			final View.OnClickListener listener) {
		mNegativeButtonText = text;
		mNegativeListener = listener;
		return this;
	}

	class SlideAnimationListener implements AnimationListener {

		DropDownDialog mDialog;

		public SlideAnimationListener(DropDownDialog dialog) {
			mDialog = dialog;
		}

        @Override
        public void onAnimationStart(Animation animation) {
            if (animation == mSlideDownAnimation) {
                mContainer.setTranslationY(-mContainer.getHeight());
            }
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            // This method is also called directly in dismiss().

            if(mDialog != null && mDialog.mContainer != null) {
                mDialog.mContainer.setLayerType(View.LAYER_TYPE_NONE, null);
            }
            if (animation == mSlideUpAnimation) {
                switch (mCancelFlag) {
                case DIALOG_DISMISS:
                    DropDownDialog.super.dismiss();
                    break;
                case DIALOG_HIDE:
                    DropDownDialog.super.hide();
                    break;
                }
            } else if(animation == mSlideDownAnimation) {
                if(mDialog != null && mDialog.mParent != null) {
                    mDialog.mParent.setVisibility(View.VISIBLE);
                }
                if (mContainer != null) {
                    mContainer.setTranslationY(0);
                }
            }
        }

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mIconSrc != null && !mIconSrc.isRecycled()) {
			mIconSrc.recycle();
			mIconSrc = null;
		}
	}

	public Button getNegetiveButton() {
		return mNegetiveButton;
	}

	@Override
	public void onBackPressed() {
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& mNegetiveButton != null) {
	                mNegetiveButton.performClick();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}
