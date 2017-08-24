package com.aliyun.homeshell;

import java.util.ArrayList;
import java.util.Random;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public class LockScreenAnimator {

    private static final String TAG = "LockScreenAnimator";
    private static final String ALIYUN_KEYGUARD_LOCK_ACTION = "aliyun.intent.action.KEYGUARD_LOCK";

    private int STAGE_BEFORE = 200;
    private int STAGE_AFTER  = 700;
    private int STAGE_ALL    = STAGE_AFTER + STAGE_BEFORE;

    private Launcher mLauncher;
    private Workspace mWorkspace;
    private DragLayer mDragLayer;
    private AnimatorSet mAnimatorSet;
    private CellLayout mCurCellLayout;
    private View mIndicator;
    private float originHotseatY;
    
    private ArrayList<Animator> allAnimators;
    private ArrayList<View> recordViews;
    private ArrayList<CellLayout.LayoutParams> recordParams;

    private boolean needed = false;
    private boolean isRunning = false;
    private boolean needBackHomeAnim = false;
    
    private static LockScreenAnimator sInstance;
    public static LockScreenAnimator getInstance(Launcher launcher){
        if( sInstance == null ) 
            sInstance = new LockScreenAnimator(launcher);
        return sInstance;
    }
    
    private LockScreenAnimator(Launcher launcher){
        mLauncher = launcher;
        mWorkspace = mLauncher.getWorkspace();
        mDragLayer = mLauncher.getDragLayer();
        mIndicator = mDragLayer.findViewById(R.id.pageindicator_view);
    }
    
    private boolean initOK(){
        mCurCellLayout = (CellLayout)mWorkspace.getChildAt(mWorkspace.getCurrentPage());
        
        if (mCurCellLayout == null ){
            //don't do animation in case the current CellLayout may be deleted
            String loginfo = "current Celllayout is null: index="+mWorkspace.getCurrentPage();
            loginfo += " count=" + mWorkspace.getChildCount();
            Log.d(TAG,loginfo);
            return false;
        }

        mAnimatorSet = new AnimatorSet();

        if( allAnimators != null ) {
            allAnimators.clear();
        }else{
            allAnimators = new ArrayList<Animator>();
        }

        if( recordViews != null ){
            recordViews.clear();
        }else{
            recordViews = new ArrayList<View>();
        }

        if( recordParams != null ) {
            recordParams.clear();
        }else{
            recordParams = new ArrayList<CellLayout.LayoutParams>();
        }

        return true;
    }

    /**
     * record icon's origin position and prepare animators
     * @return if it is ok to go on
     */
    private boolean recordOriginAndGenerateAnimation(){
        if( !initOK() ) return false;
        
        isRunning = true;
        ViewGroup container = mCurCellLayout.getShortcutAndWidgetContainer();
        //get relative position from container to DragLayer
        Rect relative = new Rect();
        mDragLayer.getViewRectRelativeToSelf(container, relative);
        float deltaX = relative.left;
        float deltaY = relative.top;
        
        // iterate every child , record its position and add to DragLayer
        int childCount = container.getChildCount();
        for( int i = childCount - 1; i >= 0 ; i-- ){
            View child = container.getChildAt(i);
            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) child.getLayoutParams();
            
            //record origin view and its origin positions
            recordViews.add(child);
            recordParams.add(layoutParams);
            
            //removeView from ShortcutAndWidgetContainer and add it to DragLayer
            
            float nowX = child.getX() + deltaX;
            float nowY = child.getY() + deltaY;
            container.removeViewAt(i);

            DragLayer.LayoutParams newLayoutParams = 
                    new DragLayer.LayoutParams(layoutParams.width, layoutParams.height);
            
            newLayoutParams.setX((int)(layoutParams.x));
            newLayoutParams.setY((int)(layoutParams.y));
            
            Log.d("DANG","params x="+newLayoutParams.getX()+" y="+newLayoutParams.getY());
            
            newLayoutParams.customPosition = true;
            mDragLayer.addView(child, newLayoutParams);
            
            child.setX(nowX);
            child.setY(nowY);
            
            Log.d("DANGG","nowX="+nowX+"  nowY"+nowY);

            //get all needed Animator of every child
            allAnimators.addAll( getAnimatorsOfOneView(child, childCount) );
        }

        allAnimators.add(getHotseatAnimation());
        allAnimators.add(getIndicatorAnimation());

        return true;
    }
    
    private Animator getIndicatorAnimation(){
        AccelerateInterpolator acli = new AccelerateInterpolator();
        ObjectAnimator aniI = ObjectAnimator.ofFloat(mIndicator, "scaleX", 1, 0);
        aniI.setDuration(STAGE_BEFORE);
        aniI.setInterpolator(acli);
        return aniI;
    }

    private Animator getHotseatAnimation(){
        View child = mLauncher.getHotseat();
        DecelerateInterpolator accelerateInterpolator = new DecelerateInterpolator();
        originHotseatY = child.getY();
        float nowY = child.getY();
        float toY  = nowY + child.getHeight();

        ObjectAnimator aniH = ObjectAnimator.ofFloat(child, "y", nowY, toY );
        aniH.setDuration(STAGE_AFTER);
        aniH.setStartDelay(STAGE_BEFORE);
        aniH.setInterpolator(accelerateInterpolator);

        return aniH;
    }

    private ArrayList<Animator> getAnimatorsOfOneView(View child,int count){
        ArrayList<Animator> ans = new ArrayList<Animator>();
        /* YUNOS BEGIN */
        // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
        Random ran = new Random();
        int thisViewDelay = ran.nextInt(count) * 30;
        /* YUNOS END */

        ObjectAnimator aniAlpha = ObjectAnimator.ofFloat(child, "alpha", 1f, 0f);
        aniAlpha.setDuration(STAGE_ALL);
        aniAlpha.setStartDelay(thisViewDelay);
        ans.add(aniAlpha);


        // blow is for x and y
        float centerX = mDragLayer.getWidth() / 2 - child.getWidth() / 2;
        float centerY = mDragLayer.getHeight() / 2;

        float originX = child.getX();
        float originY = child.getY();

        float nowX = originX + (float)((originX - centerX)*2);
        float nowY = originY + (float)((originY - centerY)*2);

        ObjectAnimator aniXBefore = ObjectAnimator.ofFloat(child, "x", originX, originX);
        aniXBefore.setDuration(STAGE_BEFORE);
        aniXBefore.setStartDelay(thisViewDelay);
        ans.add(aniXBefore);

        ObjectAnimator aniXAfter = ObjectAnimator.ofFloat(child, "x", originX, nowX);
        aniXAfter.setDuration(STAGE_AFTER);
        aniXAfter.setStartDelay(thisViewDelay+STAGE_BEFORE);
        ans.add(aniXAfter);

        ObjectAnimator aniYBefore = ObjectAnimator.ofFloat(child, "y", originY, originY);
        aniYBefore.setDuration(STAGE_BEFORE);
        aniYBefore.setStartDelay(thisViewDelay);
        ans.add(aniYBefore);

        ObjectAnimator aniYAfter = ObjectAnimator.ofFloat(child, "y", originY, nowY);
        aniYAfter.setDuration(STAGE_AFTER);
        aniYAfter.setStartDelay(thisViewDelay+STAGE_BEFORE);
        ans.add(aniYAfter);

        // below is for scale
        ObjectAnimator aniScaleXBefore = ObjectAnimator.ofFloat(child, "scaleX", 1.0f,0.8f);
        aniScaleXBefore.setDuration(STAGE_BEFORE);
        aniScaleXBefore.setStartDelay(thisViewDelay);
        ans.add(aniScaleXBefore);

        ObjectAnimator aniScaleXAfter = ObjectAnimator.ofFloat(child, "scaleX", 0.8f,3f);
        aniScaleXAfter.setDuration(STAGE_AFTER);
        aniScaleXAfter.setStartDelay(thisViewDelay+STAGE_BEFORE);
        ans.add(aniScaleXAfter);

        ObjectAnimator aniScaleYBefore = ObjectAnimator.ofFloat(child, "scaleY", 1.0f,0.8f);
        aniScaleYBefore.setDuration(STAGE_BEFORE);
        aniScaleYBefore.setStartDelay(thisViewDelay);
        ans.add(aniScaleYBefore);

        ObjectAnimator aniScaleYAfter = ObjectAnimator.ofFloat(child, "scaleY", 0.8f,3f);
        aniScaleYAfter.setDuration(STAGE_AFTER);
        aniScaleYAfter.setStartDelay(thisViewDelay+STAGE_BEFORE);
        ans.add(aniScaleYAfter);

        // below is for rotate
        float[] angle = new float[]{15,-15,0};
        /* YUNOS BEGIN */
        // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
        float rotateXAngle = angle[ran.nextInt(3)];
        float rotateYAngle = angle[ran.nextInt(3)];
        /* YUNOS END */

        ObjectAnimator aniRotateXBefore = ObjectAnimator.ofFloat(child, "rotationX", 0,rotateXAngle);
        aniRotateXBefore.setDuration(STAGE_BEFORE);
        aniRotateXBefore.setStartDelay(thisViewDelay);
        ans.add(aniRotateXBefore);

        ObjectAnimator aniRotateXAfter = ObjectAnimator.ofFloat(child, "rotationX", rotateXAngle,0);
        aniRotateXAfter.setDuration(STAGE_AFTER);
        aniRotateXAfter.setStartDelay(thisViewDelay+STAGE_BEFORE);
        ans.add(aniRotateXAfter);

        ObjectAnimator aniRotateYBefore = ObjectAnimator.ofFloat(child, "rotationY", 0,rotateYAngle);
        aniRotateYBefore.setDuration(STAGE_BEFORE);
        aniRotateYBefore.setStartDelay(thisViewDelay);
        ans.add(aniRotateYBefore);

        ObjectAnimator aniRotateYAfter = ObjectAnimator.ofFloat(child, "rotationY", rotateYAngle,0);
        aniRotateYAfter.setDuration(STAGE_AFTER);
        aniRotateYAfter.setStartDelay(thisViewDelay+STAGE_BEFORE);
        ans.add(aniRotateYAfter);

        return ans;
    }

    public void play(){
        if( !recordOriginAndGenerateAnimation() ) return;
        needed = true;
        mAnimatorSet.addListener(new LockScreenAnimatorListener());
        mAnimatorSet.playTogether(allAnimators);
        mAnimatorSet.start();
    }

    private void putIconsToOriginPosition(CellLayout currentLayout){
        if ((recordViews != null) && (recordParams != null)) {  //6049174
            for( int i = 0; i < recordViews.size(); i++ ){
                View child = recordViews.get(i);
                //6049174
                if (child == null) {
                    continue;
                }
                mDragLayer.removeView(child);
                CellLayout.LayoutParams params = recordParams.get(i);
                child.setX(params.x);
                child.setY(params.y);
                child.setScaleX(1);
                child.setScaleY(1);
                child.setRotationX(0);
                child.setRotationY(0);
                child.setAlpha(1);
                currentLayout.addViewToCellLayout(child, i, child.getId(), params, true);
            }
        }
    }

    public void restoreIfNeeded(){
        if( needed ){
            needed = false;
            final CellLayout currentLayout = mCurCellLayout;
            if( mAnimatorSet.isRunning() ){
                // Note that this call will trigger restoreIfNeeded() again in
                // onAnimationEnd(). So we need to capture current cell layout
                // before calling cancel().
                mAnimatorSet.cancel();
            }
            mWorkspace.setVisibility(View.VISIBLE);
            mIndicator.setScaleX(1);
            putIconsToOriginPosition(currentLayout);
            mLauncher.getHotseat().setY(originHotseatY);
        }
        mCurCellLayout = null;
    }

    //running means recordOriginAndGenerateAnimation and animation
    public boolean isRuning(){
        return isRunning;
    }

    public boolean shouldPreventGesture(){
        return needed;
    }

    public boolean getNeedBackHomeAnim(){
        return needBackHomeAnim;
    }

    public void setNeedBackHomeAnim(boolean needed){
        needBackHomeAnim = needed;
    }

    class LockScreenAnimatorListener implements AnimatorListener{
        public void onAnimationStart(Animator arg0) {
            mWorkspace.setVisibility(View.INVISIBLE);
        }
        public void onAnimationRepeat(Animator arg0) {}

        // when animation ends, move view from draglayer to celllayout
        public void onAnimationEnd(Animator arg0) {
            if( mLauncher.hasWindowFocus() ){
                Intent intent = new Intent(ALIYUN_KEYGUARD_LOCK_ACTION);
                mLauncher.sendBroadcast(intent);
                /* YUNOS BEGIN added by xiaodong.lxd # 100452 */
                mLauncher.getWorkspace().cancelFlingDropDownAnimation();
                /* YUNOS END */
            }else{
                restoreIfNeeded();
            }
            isRunning = false;
            setNeedBackHomeAnim(true);
        }

        public void onAnimationCancel(Animator arg0) {}
    }

    public CellLayout getWorkingOnCellLayout() {
        return mCurCellLayout;
    }

    public void reset() {
        sInstance = null;
        mLauncher = null;
        mWorkspace = null;
        mDragLayer = null;
        mIndicator = null;
        mCurCellLayout = null;
    }

    public Launcher getLauncher() {
        return mLauncher;
    }
}
