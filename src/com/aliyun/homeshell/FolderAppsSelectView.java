package com.aliyun.homeshell;

import java.util.ArrayList;
import java.util.List;

import com.aliyun.homeshell.AppDownloadManager.AppDownloadStatus;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.icon.BubbleController;
import com.aliyun.homeshell.icon.BubbleResources;
import com.aliyun.homeshell.model.LauncherModel;
//import com.aliyun.homeshell.vpinstall.VPUtils.VPInstallStatus;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
/*
 * The view for batch operation to the icon in folder
 */
public class FolderAppsSelectView extends LinearLayout {
    public static String LOG_TAG = "FolderAppsBatchOperation";
    private Context mContext;
    private Folder mFolder;
    private ImageView mBtnOK;
    private ImageView mBtnCancel;
    private TextView mTitle;
    private Launcher mLauncher;
    private static int folderItemMaxCount = ConfigManager.getFolderMaxCountX()*ConfigManager.getFolderMaxCountY();
    private Toast tip = null;
    private FolderSelectPager viewPager;
    private List<ShortcutInfo> selectedList;
    private List<GridView> gridViewList;
    private List<ShortcutInfo> mSelectableList;
    private int mAppsCountPerPage;
    class MyAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
        List<ShortcutInfo> mShortcuts;
        List<View> viewList ;
        public MyAdapter(List<ShortcutInfo> shortcuts) {
            mShortcuts = shortcuts;
            viewList = new ArrayList<View>(mShortcuts.size());
            for(int i = 0;i < shortcuts.size(); i++ ) {
                viewList.add(null);
            }
        }
        @Override
        public int getCount() {
            return mShortcuts.size();
        }
        @Override
        public Object getItem(int position) {
            return mShortcuts.get(position);
        }
        @Override
        public boolean hasStableIds() {
            return true;
        }
        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(viewList.get(position) != null) {
                return viewList.get(position);
            } else {
                ShortcutInfo shortcut = mShortcuts.get(position);
                View app;
                ImageView selector;
                BubbleTextView view;
                int offSet = (int) mContext.getResources().getDimension(
                        R.dimen.folder_app_select_icon_offset);
                app = LayoutInflater.from(getContext()).
                        inflate(R.layout.upside_apps_application, null, false);
                view = (BubbleTextView)app.findViewById(R.id.title);
                selector = (ImageView) app.findViewById(R.id.selector);
                BubbleController.applyToView(shortcut, view);
                Resources res = getResources();
                int bubbleWidth = BubbleResources.getIconWidth(res);
                int bubbleHeight = BubbleResources.getIconHeight(res);
                //gridview will distribute the empty by the width and height of the view
                view.setWidth(bubbleWidth);
                view.setHeight(bubbleHeight);
                app.setTag(shortcut);
                // if do not set the layoutParams,the gridview will calculate it
                // dynamically
                app.setLayoutParams(new FrameLayout.LayoutParams(
                                        bubbleWidth + offSet * 2,
                                        bubbleHeight + offSet * 2));
                /* YUNOS BEGIN */
                // ##date:2014/06/26 ##author:yangshan.ys##BugID:132291
                // update the title of BubbleTextView  ,keep the downloadstate or vpinstallstates of icon in folderappsselect the same with it in workspace
                int mDownloadStatus = shortcut.getAppDownloadStatus();
//                int mVPInstallStatus = shortcut.getVPInstallStatus();
                String title = null;
                if (mDownloadStatus != AppDownloadStatus.STATUS_NO_DOWNLOAD) {
                    switch (mDownloadStatus) {
                    case AppDownloadStatus.STATUS_WAITING:
                        title = mContext.getString(R.string.waiting);
                        break;
                    case AppDownloadStatus.STATUS_DOWNLOADING:
                        title = mContext.getString(R.string.downloading);
                        break;
                    case AppDownloadStatus.STATUS_PAUSED:
                        title = mContext.getString(R.string.paused);
                        break;
                    case AppDownloadStatus.STATUS_INSTALLING:
                        title = mContext.getString(R.string.installing);
                        break;
                    }
                }

// remove vp install
//                else if (mVPInstallStatus != VPInstallStatus.STATUS_NORMAL){
//                    title = mContext.getString(R.string.loading);
//                }

                if(title != null && !title.equals(view.getText())) {
                    view.setText(title);
                }
                /* YUNOS END */
                //not support cardtheme ,then adjust the selector position
                if(!((LauncherApplication) mContext.getApplicationContext()).getIconManager().supprtCardIcon()) {
                    FrameLayout.LayoutParams layout =  (FrameLayout.LayoutParams) selector.getLayoutParams();
                    layout.setMargins(0, offSet, offSet, 0);
                    selector.setLayoutParams(layout);
                }
                if(((GridView)parent).isItemChecked(position)) {
                    selector.setBackgroundResource(R.drawable.ic_selected);
                    view.setAlpha(0.5f);
                }
                viewList.set(position, app);
                return app;
            }
        }
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            if(selectedList.size() >= folderItemMaxCount && ((AbsListView) parent).isItemChecked(position)){
                ((AbsListView) parent).setItemChecked(position, false);
                tip.show();
            } else {
                setSelected((ViewGroup)view, ((AbsListView) parent).isItemChecked(position));
            }
            updateTitle();
        }
    }
    public FolderAppsSelectView(Context context) {
        super(context);
        mContext = context;
    }
    public FolderAppsSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
    public FolderAppsSelectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTitle = (TextView)findViewById(R.id.title);
        mBtnOK = (ImageView) findViewById(R.id.imageOk);
        mBtnCancel = (ImageView) findViewById(R.id.imageCancel);
        viewPager = (FolderSelectPager) findViewById(R.id.viewpager);
        mBtnOK.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ok();
            }
        });
        mBtnCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                cancel();
            }
        });
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }
    public void ok() {
        if(mFolder.getInfo().isEditFolderInContents()) {
            FolderUtils.removeEditFolderShortcut(mFolder.getInfo());
            mFolder.getInfo().getmEditFolderShortcutInfo().cellX = -1;
            mFolder.getInfo().getmEditFolderShortcutInfo().cellX = -1;
        }
        mFolder.dealNewSelectedApps((ArrayList<ShortcutInfo>)selectedList);
        if(selectedList.size() == 0) {
            mLauncher.closeFolderWithoutExpandAnimation();
            return;
        }
        if(mFolder.getInfo().count() < folderItemMaxCount){
            FolderUtils.addEditFolderShortcut(mLauncher, mFolder.getmFolderIcon());
        }
        mFolder.backFromSelectApps();
        clearSelectedState();
    }
    public void cancel() {
        mFolder.backFromSelectApps();
        clearSelectedState();
    }
    //the back press has been catched by DispatchKeyEvent
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_HOME){
            if(mFolder.getmRunningAnimatorSet() != null) {
                mFolder.getmRunningAnimatorSet().end();
            }
            mLauncher.closeFolderWithoutExpandAnimation();
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            cancel();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    private void updateTitle() {
        mTitle.setText(getContext().getText(R.string.upside_apps_title) + " (" + selectedList.size() +")");
    }

    public void init(Folder owner, FolderInfo folderInfo, Launcher launcher) {
        mLauncher = launcher;
        mFolder = owner;
        tip = Toast.makeText(mLauncher, getContext().getText(R.string.over_folder_capicity)+""+folderItemMaxCount, 150);
        selectedList = new ArrayList<ShortcutInfo>();
        gridViewList = new ArrayList<GridView>();
        mSelectableList = new ArrayList<ShortcutInfo>();
        /* YUNOS BEGIN */
        // ##date:2015/03/16 ##author:sunchen.sc
        // Copy bg member to solve concurrent modification issue
        List<ItemInfo> workspaceItems = LauncherModel.getSbgWorkspaceItems();
        /* YUNOS END */
        List<ShortcutInfo> inhotseat = new ArrayList<ShortcutInfo>();
        for(ItemInfo item : workspaceItems) {
            if(item instanceof ShortcutInfo) {
                ShortcutInfo info = (ShortcutInfo)item;
                if(info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT){
                    inhotseat.add(info);
                } else if (info.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                    /* YUNOS BEGIN */
                    // ##date:2014/9/2 ##author:zhanggong.zg ##BugID:5244146
                    // skip frozen apps
                    continue;
                    /* YUNOS END */
                } else {
                    mSelectableList.add((ShortcutInfo)item);
                }
            }
        }
        workspaceItems.clear();
        List<ShortcutInfo> selectedShortcuts = new ArrayList<ShortcutInfo>();
        selectedShortcuts = new ArrayList<ShortcutInfo>(folderInfo.contents);
        if (folderInfo.isEditFolderInContents()) {
            selectedShortcuts.remove(folderInfo.getmEditFolderShortcutInfo());
        }
        for (int i = selectedShortcuts.size()-1;i >= 0;i--) {
            mSelectableList.add(0, selectedShortcuts.get(i));
        }
        if (AgedModeUtil.isAgedMode()) {
            mAppsCountPerPage = 9;
        } else {
            mAppsCountPerPage = 12;
        }
        int sumLength = mSelectableList.size();
        int gridCount = 0;
        if(sumLength%mAppsCountPerPage == 0){
            gridCount = sumLength/mAppsCountPerPage;
        } else {
            gridCount = sumLength/mAppsCountPerPage +1;
        }
        for(int i = 0; i < gridCount; i++){
            GridView grid = (GridView)LayoutInflater.from(mLauncher).inflate(R.layout.folder_apps_select_grid, null);
            MyAdapter adapter;
            if(i == gridCount-1){
                adapter = new MyAdapter(mSelectableList.subList(i*mAppsCountPerPage, sumLength));
            } else {
                adapter = new MyAdapter(mSelectableList.subList(i*mAppsCountPerPage, (i+1)*mAppsCountPerPage));
            }
            grid.setAdapter(adapter);
            grid.setOnItemClickListener(adapter);
            gridViewList.add(grid);
        }
        Log.d(LOG_TAG, "The amount of apps in FolderAppsSelect is:" + sumLength
                + ",The amount of selected apps in folder is:"
                + selectedShortcuts.size());
        viewPager.setAdapter(pageAdapter);
    }

    public void initSelectedState(FolderInfo folderInfo) {
        List<ShortcutInfo> selectedShortcuts = new ArrayList<ShortcutInfo>();
        selectedShortcuts = new ArrayList<ShortcutInfo>(folderInfo.contents);
        if (folderInfo.isEditFolderInContents()) {
            selectedShortcuts.remove(folderInfo.getmEditFolderShortcutInfo());
        }
        if(selectedShortcuts != null) {
            selectedList.clear();
            for (int i = 0; i < selectedShortcuts.size(); i++) {
                ShortcutInfo selectInfo = selectedShortcuts.get(i);
                for(int j = 0; j< mSelectableList.size(); j++) {
                    if(mSelectableList.get(j).intent.equals(selectInfo.intent)) {
                        int gridIndex = j / mAppsCountPerPage;
                        gridViewList.get(gridIndex).setItemChecked(j % mAppsCountPerPage, true);
                        setSelected((ViewGroup) (gridViewList.get(gridIndex)
                                .getAdapter().getView(j % mAppsCountPerPage, null,
                                gridViewList.get(gridIndex))), true);
                        break;
                    }
                }
            }
        }
        updateTitle();
    }

    public void clearSelectedState() {
        for (GridView gridView : gridViewList) {
            int count = gridView.getChildCount();
            for (int pos = 0; pos < count; pos++) {
                gridView.setItemChecked(pos, false);
                setSelected((ViewGroup) (gridView.getChildAt(pos)), false);
            }
        }
        selectedList.clear();
    }
    private void setSelected(ViewGroup icon, boolean isSelected) {
        if (icon == null) {
            return;
        }
        BubbleTextView view = (BubbleTextView)icon.findViewById(R.id.title);
        if (isSelected) {
            icon.findViewById(R.id.selector).setBackgroundResource(R.drawable.ic_selected);
            view.setAlpha(0.5f);
            if(!selectedList.contains(icon.getTag())) {
                selectedList.add((ShortcutInfo) icon.getTag());
            } else {
                Log.e(LOG_TAG, "appear the repeat by click,the icon is:"
                        + ((ShortcutInfo) icon.getTag()).toString());
            }
        } else {
            view.setAlpha(1.0f);
            icon.findViewById(R.id.selector).setBackgroundDrawable(null);
            selectedList.remove(icon.getTag());
        }
    }

    PagerAdapter pageAdapter = new PagerAdapter(){
        @Override
        public int getCount() {
            return gridViewList.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(gridViewList.get(position));
            return gridViewList.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(gridViewList.get(position));
        }
    };
}
