
package com.aliyun.homeshell.editmode;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.DragController;
import com.aliyun.homeshell.DragSource;
import com.aliyun.homeshell.DropTarget.DragObject;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.Workspace;

public class VerticalPreviewList extends VerticalListView implements
        OnItemClickListener, DragSource, OnItemLongClickListener{

    
    private Launcher mLauncher;
    private Canvas mCanvas;

    private ListViewEventHandler eventHandler;

    public VerticalPreviewList(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
        mCanvas = new Canvas();
        eventHandler = new ListViewEventHandler();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        if (!mLauncher.isInLauncherEditMode()) {
            return;
        }
        eventHandler.onItemClick(parent, view, position, id);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        if ((!view.isInTouchMode())
                || (mLauncher.getWorkspace().isSwitchingState())
                || (!mLauncher.isDraggingEnabled())) {
            return false;
        }

        return eventHandler.onItemLongClick(parent, view, position, id);
    }

    @Override
    public boolean supportsFlingToDelete() {
        return false;
    }

    @Override
    public void onFlingToDeleteCompleted() {

    }

    @Override
    public void onDropCompleted(View target, DragObject d,
            boolean isFlingToDelete, boolean success) {
        if (!success) {
            boolean showOutOfSpaceMessage = false;
            if (target instanceof Workspace) {
                int currentScreen = mLauncher.getCurrentWorkspaceScreen();
                Workspace workspace = (Workspace) target;
                CellLayout layout = (CellLayout) workspace
                        .getChildAt(currentScreen);
                ItemInfo itemInfo = (ItemInfo) d.dragInfo;
                if (layout != null) {
                    layout.calculateSpans(itemInfo);
                    showOutOfSpaceMessage = !layout.findCellForSpan(null,
                            itemInfo.spanX, itemInfo.spanY);
                }
            }
            if (showOutOfSpaceMessage) {
                mLauncher.showOutOfSpaceMessage(false);
            }
            d.deferDragViewCleanupPostAnimation = false;
        } else {
            if (target instanceof Workspace) {
                int currentScreen = mLauncher.getCurrentWorkspaceScreen();
                Workspace workspace = (Workspace) target;
                CellLayout layout = (CellLayout) workspace
                        .getChildAt(currentScreen);
                layout.removeEditBtnContainer();
            }
        }
    }

    public void setup(Launcher launcher, DragController dragController) {
        mLauncher = launcher;
        eventHandler.setup(launcher, dragController, mCanvas, getContext(), this);
    }
    
    public void indicateCellLayoutHoverItem(int itemIndex, boolean in, boolean dragSource) {
        View view = getChildAt(itemIndex);
        eventHandler.indicateCellLayoutHoverItem(view, itemIndex, in, dragSource);
    }

}
