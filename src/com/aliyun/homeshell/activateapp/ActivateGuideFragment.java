package com.aliyun.homeshell.activateapp;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.utils.Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageActivateObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import com.aliyun.ams.systembar.SystemBarColorManager;

@SuppressLint("NewApi")
public class ActivateGuideFragment extends Fragment implements OnTouchListener,
        OnClickListener {
    private static final String TAG = "ActivateGuideFragment";
    private static final int MSG_CATEGORY_TIMEOUT = 1;
    private static final int MSG_ACTIVATE_CANCEL_TIMEOUT = 2;

    private static final int VIEW_POSITION_ACTIVITE = 0;
    private static final int VIEW_POSITION_CATEGORY = 1;
    private int mPosition = VIEW_POSITION_ACTIVITE;
    private static final int ACTIVITE_INIT = 0;
    private static final int ACTIVITE_SUCCESS = 1;
    private int mActiviteState = ACTIVITE_INIT;
    private View mRootView;
    private Button mNextBtn;
    private View mSkip;
    private ViewPager mContent;
    private ViewGroup mGuideActivateView;
    private ViewGroup mGuideCategoryView;
    private AlertDialog mActiviteProgressDlg;
    private View mFormatView;
    private ProgressBar mProgressBar;
    private TextView mProgressCountView;
    private Launcher mLauncher;
    private GuideCategory mGuideCategory;
    private ActivateTask mTask;
    private AlertDialog mAlertDialog = null;

    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state) {
            case ViewPager.SCROLL_STATE_IDLE:
                break;
            case ViewPager.SCROLL_STATE_DRAGGING:
                break;
            case ViewPager.SCROLL_STATE_SETTLING:
                if (mPosition == VIEW_POSITION_ACTIVITE) {
                    mNextBtn.setText(R.string.guide_activate);
                } else if (mPosition == VIEW_POSITION_CATEGORY) {
                    mNextBtn.setText(R.string.guide_category);
                    mSkip.setVisibility(View.VISIBLE);
                }
                break;
            default:
                break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.guide_fragment, container, false);
        mGuideActivateView = (ViewGroup) inflater.inflate(
                R.layout.guide_activate, null);
        mGuideCategoryView = (ViewGroup) inflater.inflate(R.layout.guide_category, null);

        initViews();
        if (mLauncher == null) {
            Activity activity = getActivity();
            if(activity instanceof Launcher){
                mLauncher = (Launcher) activity;
            }
        }
        mGuideCategory = new GuideCategory(mLauncher, mGuideCategoryView);
        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mLauncher == null && activity instanceof Launcher) {
            mLauncher = (Launcher) activity;
        }
        if (mLauncher != null) {
            mLauncher.getHotseat().preHideHotseat();
        }
        setSystemBarColor(activity, true);
    }

    private void initViews() {
        if (mRootView == null) {
            return;
        }
        mRootView.setOnTouchListener(this);
        mNextBtn = (Button) mRootView.findViewById(R.id.btn_next);
        mNextBtn.setOnClickListener(this);
        mNextBtn.setBackgroundResource(R.drawable.guide_next_btn_bg);
        mNextBtn.setText(R.string.guide_activate);
        mNextBtn.setTextColor(Color.WHITE);
        mSkip = mRootView.findViewById(R.id.skip);
        mSkip.setOnClickListener(this);
        mContent = (ViewPager) mRootView.findViewById(R.id.content);
        mContent.setOnTouchListener(this);
        mContent.setAdapter(new GuideAdapter(getActivity()));
        mContent.setCurrentItem(VIEW_POSITION_ACTIVITE);
        mContent.setOnPageChangeListener(mOnPageChangeListener);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        mPosition = mContent.getCurrentItem();
        if (id == R.id.btn_next) {
            if (VIEW_POSITION_ACTIVITE == mPosition) {
                if (mActiviteState == ACTIVITE_INIT) {
                    LayoutInflater factory = LayoutInflater.from(getActivity());
                    mFormatView = factory.inflate(
                            R.layout.dialog_progress_custom_view, null);
                    mProgressBar = (ProgressBar) mFormatView
                            .findViewById(R.id.format_progress);
                    mProgressCountView = (TextView) mFormatView
                            .findViewById(R.id.progress_count_text);
                    mActiviteProgressDlg = new AlertDialog.Builder(
                            this.getActivity(),
                            hwdroid.R.style.Theme_Ali_Dialog_Alert)
                            .setCancelable(true)
                            .setTitle(R.string.activate_yunos_service)
                            .setView(mFormatView).create();
                    mActiviteProgressDlg.getWindow().addFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    mTask = new ActivateTask();
                    mTask.execute();
                } else {
                    if (AgedModeUtil.isAgedMode()) {
                        exitActivateGuideFragment();
                    } else {
                        mPosition = VIEW_POSITION_CATEGORY;
                        mLauncher.setLauncherEditMode(true);
                        showWaitDialog(getActivity());
                        if(mLauncher.isInLauncherCategoryMode()){
                            mLauncher.updateCategoryPreviewView();
                        }else{
                            mLauncher.getModel().reCategoryAllIcons();
                        }
                    }
                }
            } else {
                mLauncher.coverAllIcons();
                showWaitDialog(getActivity());
                mHandler.sendEmptyMessageDelayed(MSG_CATEGORY_TIMEOUT, 1000);
            }
        } else if (id == R.id.skip) {
            if (VIEW_POSITION_ACTIVITE == mPosition) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity(),
                        hwdroid.R.style.Theme_Ali_Dialog_Alert)
                        .setTitle(R.string.activate_giveup_title)
                        .setMessage(R.string.activate_giveup_content)
                        .setPositiveButton(R.string.activate_giveup,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        // TODO Auto-generated method stub
                                        mLauncher.cancelActivate();
                                        exitActivateGuideFragment();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                dialog.show();
            } else {
                mLauncher.recoverAllIcons();
                showWaitDialog(getActivity());
                mHandler.sendEmptyMessageDelayed(MSG_ACTIVATE_CANCEL_TIMEOUT,
                        300);
            }
        }
    }

    private void exitActivateGuideFragment() {
        mLauncher.exitActivateGuideMode();
        getActivity().getFragmentManager().beginTransaction()
                .remove(ActivateGuideFragment.this).commit();
    }

    Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case MSG_CATEGORY_TIMEOUT:
            case MSG_ACTIVATE_CANCEL_TIMEOUT:
                Utils.dismissLoadingDialog();
                if (mAlertDialog != null && mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                    mAlertDialog = null;
                }
                exitActivateGuideFragment();
                break;
            default:
                break;
            }
        };
    };

    private void showWaitDialog(Context context) {
        if (mAlertDialog == null) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.alert_dialog_progress_view, null, false);
            TextView text = (TextView) view
                    .findViewById(R.id.dialog_progress_message_text);
            text.setText(context.getResources().getString(R.string.str_load));
            mAlertDialog = new AlertDialog.Builder(context).setView(view)
                    .setCancelable(false).create();
        }
        mAlertDialog.show();
    }

    private class ActivateTask extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            mActiviteProgressDlg.show();
        }

        @Override
        protected Boolean doInBackground(Void... unused) {
            try {
                IPackageManager mPm;
                mPm = IPackageManager.Stub
                        .asInterface(android.os.ServiceManager
                                .getService("package"));
                String[] pkgs = mPm.getAllDeferredPreloadApps();

                if (pkgs != null) {
                    int size = pkgs.length;
                    if (size == 0) {
                        Log.d(TAG, "runActivate size is 0");
                        return false;
                    }

                    for (int i = 0; i < size; i++) {
                        String pkg = pkgs[i];
                        PackageActivateObserver observer = new PackageActivateObserver();
                        Log.d(TAG, timestamp() + " ---> Pkg: " + pkg
                                + "  start activated");
                        boolean res = mPm.installDeferredPreloadApp(pkg,
                                observer);
                        String result = null;
                        if (res) {
                            synchronized (observer) {
                                while (!observer.finished) {
                                    try {
                                        observer.wait();
                                    } catch (InterruptedException e) {
                                        // do nothing
                                    }
                                }
                            }
                            result = (observer.result == 0) ? "SUCCEEDED"
                                    : "FAILED - " + observer.message;
                        } else {
                            result = "NOT START - package name is null?";
                        }
                        publishProgress((int) (((i + 1) / (float) size) * 100));
                        Log.d(TAG, timestamp() + " <--- Pkg: " + pkg
                                + " - activated" + result);
                        Log.d(TAG, " --- " + percent(size, i + 1) + "% ----- ");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... progresses) {
            Log.i(TAG, "onProgressUpdateProgress... " + progresses);
            mProgressBar.setProgress(progresses[0]);
            mProgressCountView.setText(NumberFormat.getPercentInstance()
                    .format((double) progresses[0] / 100));
        }

        @Override
        protected void onPostExecute(Boolean result) {
            TextView guideShow = (TextView) mGuideActivateView
                    .findViewById(R.id.guide_show);
            TextView guideTitle = (TextView) mGuideActivateView
                    .findViewById(R.id.guide_title);
            TextView guideContent = (TextView) mGuideActivateView
                    .findViewById(R.id.guide_content);
            TextView guideTips = (TextView) mGuideActivateView
                    .findViewById(R.id.guide_tips);
            guideTips.setVisibility(View.INVISIBLE);
            guideShow.setText(R.string.guide_activate_success);
            guideShow.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    getActivity().getResources().getDrawable(
                            R.drawable.pic_guide_02), null, null);
            guideTitle.setText(R.string.guide_activate_success_title);
            guideContent.setText(R.string.guide_activate_success_content);
            mNextBtn.setText(R.string.guide_activate_ok);
            mSkip.setVisibility(View.INVISIBLE);
            mActiviteState = ACTIVITE_SUCCESS;
            mLauncher.completeActivate();
            mActiviteProgressDlg.dismiss();
            Intent intent = new Intent(LauncherModel.ACTION_REMOVE_APP_VIEWS);
            intent.putExtra(LauncherModel.TYPE_PACKAGENAME, "com.yunos.alimobilesearch");
            getActivity().sendBroadcast(intent);
        }

        @Override
        protected void onCancelled() {
        }
    }

    class PackageActivateObserver extends IPackageActivateObserver.Stub {
        boolean finished;
        int result;
        String message;

        @Override
        public void onPackageActivated(String pkgName, int status, String msg,
                Bundle extras) {
            synchronized (this) {
                finished = true;
                result = status;
                message = msg;
                notifyAll();
            }
        }
    }

    static private String timestamp() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return "[" + sdf.format(date) + "]";
    }

    static private int percent(int total, int current) {
        if (total == 0)
            return 100;
        return (int) ((double) current * 100 / (double) total);
    }

    private class GuideAdapter extends PagerAdapter {
        ArrayList<View> mViews = new ArrayList<View>();

        public GuideAdapter(Context context) {
            mViews.clear();
            mViews.add(mGuideActivateView);
            mViews.add(mGuideCategoryView);
        }

        @Override
        public int getCount() {
            return mViews.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView(mViews.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ((ViewPager) container).addView(mViews.get(position));
            return mViews.get(position);
        }
    }

    @Override
    public void onDetach() {
        mGuideCategoryView = null;
        if (mLauncher != null) {
            mLauncher.getHotseat().afterShowHotseat();
        }
        super.onDetach();
        setSystemBarColor(mLauncher, false);
        mLauncher.updateDisplayStyle(true);
    }

    public void setCategoryPreview(ArrayList<Bitmap> bitmaps) {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        if (mGuideCategory != null) {
            mGuideCategory.setBackgroundBitmap(getWallPaper(mLauncher));
            mGuideCategory.setBitmaps(bitmaps);
        }
        mContent.setCurrentItem(VIEW_POSITION_CATEGORY, true);
    }

    private Bitmap getWallPaper(Context context) {
        WallpaperManager wallpaper = WallpaperManager.getInstance(context);
        Drawable paper = wallpaper.getDrawable();
        return ((BitmapDrawable) paper).getBitmap();
    }

    private void setSystemBarColor(Activity activity, boolean darkMode) {
        try {
            SystemBarColorManager systemBarManager = new SystemBarColorManager(
                    activity);
            systemBarManager.setStatusBarDarkMode(activity, darkMode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
