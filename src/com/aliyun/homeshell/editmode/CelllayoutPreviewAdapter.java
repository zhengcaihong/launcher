
package com.aliyun.homeshell.editmode;

import java.util.ArrayList;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.Workspace;

public class CelllayoutPreviewAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private Launcher mLauncher;
    private ArrayList<CellLayoutItem> mListData;
    CellLayoutItem mCurrentItem = null;
    
    private int mImgWidth;
    private int mImgHeight;
    private AsyncBitmapLoader bitmapLoader;
    
    public CelllayoutPreviewAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mLauncher = (Launcher)context;
        mListData = new ArrayList<CellLayoutItem>();
        mImgWidth = context.getResources().getDimensionPixelSize(R.dimen.preview_item_image_width);
        mImgHeight = context.getResources().getDimensionPixelSize(R.dimen.preview_item_image_height);
        bitmapLoader = new AsyncBitmapLoader();
        loadItems();
    }
    
    private void loadItems() {
        
        Workspace workspace = mLauncher.getWorkspace();

        int childCount = workspace.getChildCount();
        for (int i = 0; i < childCount; i++) {
            CellLayout cellLayout = (CellLayout) workspace.getChildAt(i);
            CellLayoutItem item = new CellLayoutItem();
            item.id = cellLayout.hashCode();
            item.cellLayout = cellLayout;
            item.preview = null;
            mListData.add(item);
        }
    }

    public void reloadItems() {
        if (mListData != null) {
            mListData.clear();
        }
        loadItems();
    }

    @Override
    public int getCount() {
        return mListData.size();
    }

    @Override
    public Object getItem(int position) {
        return mListData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    
    private synchronized Bitmap createPreviewBitmap(CellLayout cellLayout) {
        Bitmap tmpBmp = Bitmap.createBitmap(mImgWidth, mImgHeight, Bitmap.Config.ARGB_8888);
        float scaleW = ((float) mImgWidth) / cellLayout.getWidth();
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/04/21 ## author: wangye.wy
        // ## BugID: 8176788: reduce height of header in cell layout
        float scaleH;
        int height = cellLayout.getHeight();
        if (height > Workspace.sHeight) {
            scaleH = ((float) mImgHeight) / (height - (Workspace.sHeaderHeight / Workspace.sEditScale));
        } else {
            scaleH = ((float) mImgHeight) / height;
        }
        /* YUNOS END */

        final Canvas c = new Canvas(tmpBmp);
  
        if (cellLayout.hasChild()) {
            c.scale(scaleW, scaleH);
            if (!cellLayout.drawShortcutsAndWidgetsOnCanvas(c)) {
                // failed to draw
                tmpBmp.recycle();
                tmpBmp = null;
                return null;
            }
        } else {
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/24 ## author: wangye.wy
            // ## BugID: 8034001: draw edit button container of empty screen
            if (!cellLayout.drawEditBtnContainerOnCanvas(c)) {
                // failed to draw
                tmpBmp.recycle();
                tmpBmp = null;
                return null;
            }
            /* YUNOS END */
        }
        return tmpBmp;
    }
    
    private CellLayoutItem findItem(CellLayout celllayout) {
        for(CellLayoutItem item : mListData) {
            if (celllayout == null) {
                return null;
            }
            if (item == null) {
                continue;
            }
            if (item.id == celllayout.hashCode()) {
                return item;
            }
        }
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final CellLayoutItem item = (CellLayoutItem) getItem(position);
        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = mInflater.inflate(R.layout.preview_item, parent, false);

            vh.previewImgView = (ImageView) convertView.findViewById(R.id.preview_image);
            vh.selectNumTextView = (TextView) convertView.findViewById(R.id.preview_select_num);
            // vh.previewImgView.setImageBitmap(item.preview);
            vh.previewImgView.setScaleType(ScaleType.FIT_XY);
            vh.selectNumTextView.setVisibility(View.VISIBLE);
            /*vh.titleTextView = (TextView) convertView.findViewById(R.id.preview_title);
            vh.titleTextView.setText(item.id);*/
            convertView.setTag(vh);

        } else {
            vh = (ViewHolder) convertView.getTag();
            // vh.previewImgView.setImageBitmap(item.preview);
            /*vh.titleTextView.setText(item.id);*/
        }

        if (item.preview == null || item.preview.isRecycled()) {
            vh.previewImgView.setImageBitmap(null);
            bitmapLoader.asyncCreateBitmap(item, new ImageCallBack() {

                @Override
                public void imageLoad(ImageView imageView, Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        } else {
            vh.previewImgView.setImageBitmap(item.preview);
        }

        if(item.selected > 0){
            vh.selectNumTextView.setText(Integer.toString(item.selected));
            vh.selectNumTextView.setVisibility(View.VISIBLE);
            if(mCurrentItem == item){
                ObjectAnimator oXUp = ObjectAnimator.ofFloat(vh.selectNumTextView, "scaleX", 1f, 1.2f);
                ObjectAnimator oYUp = ObjectAnimator.ofFloat(vh.selectNumTextView, "scaleY", 1f, 1.2f);
                ObjectAnimator oXDown = ObjectAnimator.ofFloat(vh.selectNumTextView, "scaleX", 1.2f, 1);
                ObjectAnimator oYDown = ObjectAnimator.ofFloat(vh.selectNumTextView, "scaleY", 1.2f, 1);
                AnimatorSet set = new AnimatorSet();
                set.play(oXUp).with(oYUp).before(oXDown).before(oYDown);
                set.setDuration(200);
                set.start();
                mCurrentItem = null;
            }
        }else{
            vh.selectNumTextView.setVisibility(View.GONE);
        }
        vh.previewImgView.setBackgroundResource(item.isMoving ? R.drawable.dock_bg_moving : R.drawable.em_preview_list_item_bg);
        return convertView;
    }
    
    public void addOrDeleteEmptyCellLayout(boolean add, CellLayout cellLayout, int index) {
        if (add) {
            CellLayoutItem item = new CellLayoutItem();
            item.id = cellLayout.hashCode();
            item.cellLayout = cellLayout;
            item.selected = 0;
            mListData.add(item);
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/04/22 ## author: wangye.wy
            // ## BugID: 8176977: update previous screen to fake screen
            if (index > 1) {
                CellLayoutItem prevItem = mListData.get(index - 1);
                if (prevItem.preview != null && !prevItem.preview.isRecycled()) {
                    prevItem.preview.recycle();
                    prevItem.preview = null;
                }
            }
            /* YUNOS END */
        } else {
            CellLayoutItem item = findItem(cellLayout);
            if (item != null) {
                if (item.preview != null && !item.preview.isRecycled()) {
                    item.preview.recycle();
                }
                mListData.remove(item);
            }
        }
        notifyDataSetChanged();
    }

    public void exchangeScreen(int prev, int next) {
        CellLayoutItem item = mListData.get(next);
        mListData.remove(next);
        mListData.add(prev, item);
    }

    public void updateSelectedNum(CellLayout cellLayout,boolean add){
        if(cellLayout == null && add){
            for(CellLayoutItem item : mListData) {
                item.selected = 0;
            }
            mCurrentItem = null;
            notifyDataSetChanged();
        }else{
            CellLayoutItem item = findItem(cellLayout);
            if(item != null){
                mCurrentItem = item;
                if(add){
                    item.selected += 1;
                }else{
                    item.selected -= 1;
                }
                notifyDataSetChanged();
            }
        }
    }

    public void onCelllayoutBeginDrag(int index,boolean in){
        int size = mListData.size();
        for (int i = 0; i < size; i++) {
            mListData.get(i).isMoving = false;
        }
        if(index >= 0 && index < size){
            mListData.get(index).isMoving = in;
        }
        notifyDataSetChanged();
    }

    public void updateItemOnCellLayoutChanged(int index) {
        Workspace workspace = mLauncher.getWorkspace();
        updateItemOnCellLayoutChanged((CellLayout) workspace.getChildAt(index));
    }
    
    public void updateItemOnCellLayoutChanged(CellLayout cellLayout) {
        CellLayoutItem item = findItem(cellLayout);
        if (item != null) {
            if (item.preview != null && !item.preview.isRecycled()) {
                item.preview.recycle();
                item.preview = null;
            }
            notifyDataSetChanged();
        }
    }

    public void clearSelected(){
        if(mListData != null) {
            for(CellLayoutItem item : mListData) {
                item.selected = 0;
            }
        }
    }

    public void onExit() {
        if(mListData != null) {
            for(CellLayoutItem item : mListData) {
                if(item.preview != null && !item.preview.isRecycled()) {
                    item.preview.recycle();
                }
            }
            mListData.clear();
        }

        Log.d(Launcher.TAG_EDITMODE, "CelllayoutAdapter onExit mListData.clear()");
    }
    
    public void loadDataAdvance() {
        Log.d(Launcher.TAG_EDITMODE, "CelllayoutAdapter loadDataAdvance ");
        for (CellLayoutItem item : mListData) {
            bitmapLoader.asyncCreateBitmap(item, null);
        }
    }

    class CellLayoutItem {
        Bitmap preview;
        int id;
        CellLayout cellLayout;
        int selected;
        boolean isMoving;
        @Override
        public String toString() {
            return "CellLayoutItem [id=" + id + "]";
        }
    }
    
    class ViewHolder {
        public ImageView previewImgView;
        public TextView titleTextView;
        public TextView selectNumTextView;
    }

    class AsyncBitmapLoader {

        private void asyncCreateBitmap(final CellLayoutItem item, final ImageCallBack imageCallBack) {
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    Bitmap bitmap = createPreviewBitmap(item.cellLayout);
                    return bitmap;
                }

                @Override
                protected void onPostExecute(Bitmap result) {
                    if (result == null) {
                        // retry in main thread
                        result = createPreviewBitmap(item.cellLayout);
                    }
                    if (result != null) {
                        item.preview = result;
                        if (imageCallBack != null) {
                            notifyDataSetChanged();
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Void[0]);
        }
    }

    public interface ImageCallBack {
        public void imageLoad(ImageView imageView, Bitmap bitmap);
    }

}
