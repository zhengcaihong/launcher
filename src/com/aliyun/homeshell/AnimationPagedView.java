package com.aliyun.homeshell;

import java.util.HashMap;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

import com.aliyun.homeshell.effects3d.ClothShader;
import com.aliyun.homeshell.effects3d.CubeShader;
import com.aliyun.homeshell.effects3d.LauncherGLSurfaceView;
import com.aliyun.homeshell.effects3d.LauncherRenderer;
import com.aliyun.homeshell.setting.HomeShellSetting;

/**
 * A subclass of SmoothPagedView to handle scroll effect
 * Mainly implemented in dispatchDraw
 */
public class AnimationPagedView extends SmoothPagedView {

    public static final float CAMERA_DISTANCE = 6500;
    public static String TAG = "AnimationPagedView";

    private Camera mCamera;
    private Matrix mMatrix;

    private Scroller mOriginScroller;
    private Scroller mBounceScroller;
    
    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(5681912) ##date:2015/01/05
    // ##description: add 3d transition effects
    private Bitmap mTextureCache;
    private int mFrontTexturedScreen = -1;
    private int mBackTexturedScreen = -1;
    private LauncherGLSurfaceView mGLSurfaceView;
    protected LauncherRenderer mGLRenderer;
    private int[] mGLSurfaceViewLoc = new int[2];
    private ValueAnimator mGLViewScaleInAnim;
    private ValueAnimator mGLViewScaleOutAnim;
    private float mGLBaseScale = 1.0f;
    private boolean mGLViewAnimating = false;
    private Canvas mGLCanvas;
    private int mGLSurfaceViewW = 0;
    private int mGLSurfaceViewH = 0;
    /* YUNOS END PB */
    private SharedPreferences mSharedPref;
    private OnSharedPreferenceChangeListener mSharedPrefListener;

    /**
     * When scroll started, we save view's drawing cache in mIconBitmapCache.
     * Every time {@link #dispatchDraw} was called, get Bitmap from it to avoid
     * repeatedly calling {@link View#getDrawingCache}.
     */
    private HashMap<String,Bitmap> mIconBitmapCache;

    public static class Type {
        public final static int HORIZONTAL = 0;
        public final static int BOX_OUT = 1;
        public final static int BOX_IN = 2;
        public final static int ROLL_UP = 3;
        public final static int ROLL_DOWN = 4;
        public final static int ROLL_WINDOW = 5;
        public final static int ROLL_OVER = 6;
        public final static int SCALE_IN_OUT = 7;
        public final static int RANDOM_SWITCH = 8;
        public final static int RIGHT_FADE = 9;
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(5681912) ##date:2015/01/05
        // ##description: add 3d transition effects
        public final static int GLASS_3D = 10;
        public final static int CLOTH_3D = 11;
        /* YUNOS END PB */
    }

    public AnimationPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public AnimationPagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        mCamera = new Camera();
        mMatrix = new Matrix();
        mLauncher = LauncherApplication.mLauncher;
        mIconBitmapCache = new HashMap<String,Bitmap>();
        mPageAnimType = Type.HORIZONTAL;
        mOriginScroller = mScroller;
        mBounceScroller = new Scroller(context, new BounceBackInterpolator());
        
        mSharedPref = context.getSharedPreferences("com.aliyun.homeshell_preferences",
                Context.MODE_PRIVATE);
        
    }
    /*YUNOS BEGIN PB*/
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(5674413) ##date:2014/12/30
    // ##description: separate mainmenu slide effect from screen slide effect. 
    protected void onTransitionEffectChanged(int oldEffect, int newEffect) {
        if (mLauncher == null || mLauncher.getWorkspace() == null) {
            return;
        }
        mPageAnimType = newEffect;
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(6718902) ##date:2015/12/01
        // ##description: init 3d resource when begin moving.
        on3DEffectChanged(oldEffect, newEffect);
        /* YUNOS END PB */
        // we don't need to run resetChildrenDrawing when first entered.
        if (oldEffect != -1) {
            resetChildrenProperties();
        }
    }

    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(6718902) ##date:2015/12/01
    // ##description: init 3d resource when begin moving.
    private void on3DEffectChanged(int oldEffect, int newEffect) {
        if (mGLSurfaceViewW == 0 || mGLSurfaceViewH == 0) {
            View page = getPageAt(mLauncher.getWorkspace().getIconScreenHomeIndex());
            if (page != null && is3DEffect(newEffect)) {
                mGLSurfaceViewW = page.getWidth();
                mGLSurfaceViewH = page.getHeight();
            }
        }
        if (is3DEffect(oldEffect)) {
            deinitGLSurfaceView();
        }
        if (is3DEffect(newEffect)) {
            initGLSurfaceView();
        }
    }
    /* YUNOS END PB */

    private void resetChildrenProperties() {
        for (int i = 0; i < getChildCount(); i++) {
            View page = getPageAt(i);
            page.setPivotX(0);
            page.setPivotY(0);
            page.setRotation(0);
            page.setRotationX(0);
            page.setRotationY(0);

            page.setVisibility(VISIBLE);
            page.setAlpha(1f);

            ViewGroup container;
            if (page instanceof CellLayout) {
                CellLayout cellLayout = (CellLayout) page;
                container = cellLayout.getShortcutAndWidgetContainer();
            } else {
                // never
                return;
            }
            for (int j = 0; j < container.getChildCount(); j++) {
                View view = container.getChildAt(j);
                view.setPivotX(view.getMeasuredWidth() * 0.5f);
                view.setPivotY(view.getMeasuredHeight() * 0.5f);
                view.setRotation(0);
                view.setRotationX(0);
                view.setRotationY(0);
                view.setScaleX(1f);
                view.setScaleY(1f);
                view.setTranslationX(0f);
                view.setTranslationY(0f);
                view.setVisibility(VISIBLE);
                view.setAlpha(1f);
            }
        }
    }
    /* YUNOS END PB*/

    
    private double getPercentage(View child, int screen){
        /*  CellLayout related size
         *                       current content
         *                       ***************
         *  *********   *********   *********   *********
         *  *       *   *       *   *       *   *       *
         *  *       *   *       *   *       *   *       *
         *  *       *   *       *   *       *   *       *
         *  *       *   *       *   *       *   *       *
         *  *********   *********   *********   *********
         *           ***         ***************
         *            *                 *
         *            *                 *
         *            *       getWidth() == child.getWidth() + 2 * gapOfCellLayouts
         *    gapOfCellLayouts
         *
         *  mScroll change (child.getWidth() + gapOfCellLayouts) every time you scroll
         */

        // the gap between two CellLayouts
        double gapOfCellLayouts = ( getWidth() - child.getWidth() ) / 2;
        double molecular   = getScrollX() - ( getChildOffset(screen) - gapOfCellLayouts );
        //chenjian modified for Bug 5997572/6006009
        double denominator;
        /* YUNOS BEGIN */
        //## modules(Home Shell): [Category]
        //## date: 2015/07/30 ## author: wangye.wy
        //## BugID: 6221911: category on desk top
        if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode()) {
        /* YUNOS END */
            denominator = getScaledMeasuredWidth(getPageAt(screen)) + mPageSpacing;
        } else {
            denominator = child.getWidth() + gapOfCellLayouts;
        }
      //chenjian modified for Bug 5997572/6006009
        double percentage  = molecular / denominator;

        if( percentage < -1 || percentage > 1 ) {
            // for the scroll between first and last screen
            if((mPageAnimType != Type.ROLL_DOWN) && (mPageAnimType != Type.ROLL_UP)){
                if( getScrollX() < 0 ) {
                    percentage = 1 + getScrollX() / denominator;
                }else{
                    int last = getChildCount() - 1;
                    int leftEdge = getChildOffset(last) + child.getWidth();
                    percentage = (getScrollX() - leftEdge) / denominator;
                }
            }
        }

        return percentage;
    }

    /**
     * Draw the CellLayout and add effect at the same time
     *
     * @param canvas the {@link Canvas} which whole content will be displayed
     * @param screen current screen index
     * @param drawingTime
     */
    protected void drawScreen(Canvas canvas, int screen, long drawingTime) {
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(5681912) ##date:2015/01/05
        // ##description: add 3d transition effects
        if( mLauncher.getDragController().isDragging() 
                || (!isPageMoving() && !mGLViewAnimating)){
            //if dragging or not moving, just use default implementation
            if (is3DEffect(mPageAnimType)) {
                mGLRenderer.setVisible(false);
            }
            super.drawScreen(canvas, screen, drawingTime);
            return;
        }
        /* YUNOS END PB */

        //if it's need to call ViewGroup#drawChild
        boolean drawChild = true;
        View child = getChildAt(screen);

        canvas.save();
        mCamera.save();

        double percentage = getPercentage(child, screen);
        if( percentage < -1 || percentage > 1 ) return;

        /*
         * scroll to right：left 0% ~ 100%   right -100% ~ 0%
         * scroll to left ：left 100% ~ 0%   right 0% ~ -100%
         */

        switch (mPageAnimType) {
            case Type.HORIZONTAL:
                break;
            case Type.BOX_OUT:
                drawChild = boxOut(canvas, screen, percentage);
                break;
            case Type.BOX_IN:
                drawChild = boxIn(canvas, screen, percentage);
                break;
            case Type.ROLL_UP:
                drawChild = rollUp(canvas, screen, percentage);
                break;
            case Type.ROLL_DOWN:
                drawChild = rollDown(canvas, screen, percentage);
                break;
            case Type.ROLL_WINDOW:
                drawChild = rollWindow(child, screen, (float)percentage);
                break;
            case Type.ROLL_OVER:
                drawChild = rollOver(child, screen, (float)percentage);
                break;
            case Type.SCALE_IN_OUT:
                drawChild = scaleInOut(canvas, screen, percentage);
                break;
            case Type.RANDOM_SWITCH:
                drawChild = randomSwitch(child, screen, (float)percentage);
                break;
            case Type.RIGHT_FADE:
                /* YUNOS BEGIN */
                //## modules(Home Shell): [Category]
                //## date: 2015/07/30 ## author: wangye.wy
                //## BugID: 6221911: category on desk top
                if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode())
                /* YUNOS END */
                    drawChild = rightFadeEditMode(child,screen,(float)percentage);
                else
                    drawChild = rightFade(child,screen,(float)percentage);
                break;
            /* YUNOS BEGIN PB */
            // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
            // ##BugID:(5681912) ##date:2015/01/05
            // ##description: add 3d transition effects
            case Type.GLASS_3D:
                drawChild = glass_3d(canvas, screen, percentage, drawingTime);
                break;
            case Type.CLOTH_3D:
                drawChild = cloth_3d(canvas, screen, percentage, drawingTime);
                break;
            /* YUNOS END PB */
            default:
                break;
        }

        if (drawChild)
            drawChild(canvas, child, drawingTime);
        mCamera.restore();
        canvas.restore();
    }

    private boolean rightFadeEditMode(View v, int i, float scrollProgress) {
        float trans = Math.abs(scrollProgress) * (float) (getScaledMeasuredWidth(v) + mPageSpacing);

        if (scrollProgress >= 0) {
            v.setPivotX(v.getWidth() / 2);
            // v.setPivotY(0);
            v.setScaleX(Workspace.sEditScale);
            v.setScaleY(Workspace.sEditScale);
            v.setAlpha(1);
            v.setTranslationX(0);
            return true;
        }

        float scaleFactor = Workspace.sEditScale / 2.0f;
        float scale = scaleFactor + scaleFactor * (float) (1 + scrollProgress);
        float alpha = (1 + scrollProgress);

        v.setPivotX(v.getWidth() / 2);
        // v.setPivotY(0);
        v.setScaleX(scale);
        v.setScaleY(scale);
        v.setAlpha(alpha);
        v.setTranslationX(-trans);

        return true;
    }
    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(5681912) ##date:2015/01/05
    // ##description: add 3d transition effects
    private void initGLSurfaceView() {
        if (mGLSurfaceView == null) {
            if (mLauncher.getWorkspace() == null) {
                return;
            }
            mGLCanvas = new Canvas();
            mGLSurfaceView = new LauncherGLSurfaceView(mLauncher);
            if (mPageAnimType == Type.GLASS_3D) {
                mGLRenderer = new CubeShader(mLauncher, mGLSurfaceView);
            } else if (mPageAnimType == Type.CLOTH_3D) {
                mGLRenderer = new ClothShader(mLauncher, mGLSurfaceView);
            }
            mGLSurfaceView.setRenderer(mGLRenderer);
            mGLSurfaceView.setZOrderOnTop(true);
            mGLSurfaceViewLoc[0] = 0;
            LayoutParams lp = new LayoutParams(mGLSurfaceViewW, mGLSurfaceViewH);
            if (mTextureCache == null || mTextureCache.isRecycled()) {
                mTextureCache = Bitmap.createBitmap(mGLSurfaceViewW, mGLSurfaceViewH,
                        Bitmap.Config.ARGB_8888);
            }
            mGLSurfaceView.setLayoutParams(lp);
            mLauncher.getDragLayer().addView(mGLSurfaceView);
            initGLTexture();

            AnimatorUpdateListener updateListener = new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (Float) animation.getAnimatedValue();
                    if (animation == mGLViewScaleInAnim) {
                        mGLRenderer.setScale(mGLBaseScale * value);
                    } else  {
                        float mScale = (Float) animation.getAnimatedValue("mScale");
                        float alpha = (Float) animation.getAnimatedValue("mAlpha");
                        mGLRenderer.setScale(mGLBaseScale * mScale);
                        mGLRenderer.setAlpha(alpha);
                    }
                }
            };
            AnimatorListener animatorListener = new AnimatorListener() {

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animation == mGLViewScaleInAnim) {

                    } else if (animation == mGLViewScaleOutAnim) {
                        mGLViewAnimating = false;
                        AnimationPagedView.this.invalidate();
                        mFrontTexturedScreen = -1;
                        mBackTexturedScreen = -1;
                    }
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }

                @Override
                public void onAnimationStart(Animator animation) {

                    if (animation == mGLViewScaleInAnim) {
                        mGLViewAnimating = true;
                        if (mGLSurfaceView.getVisibility() != View.VISIBLE) {
                            mGLSurfaceView.setVisibility(View.VISIBLE);
                        }
                        mGLRenderer.setAlpha(1.0f);
                    } else if (animation == mGLViewScaleOutAnim) {

                    }
                }
            };
            mGLViewScaleInAnim = ValueAnimator.ofFloat(1.0f, 0.85f);
            mGLViewScaleInAnim.setDuration(200);
            mGLViewScaleInAnim.addUpdateListener(updateListener);
            mGLViewScaleInAnim.addListener(animatorListener);
            
            PropertyValuesHolder scalePH = PropertyValuesHolder.ofFloat(
                    "mScale", 0.85f, 1f);
            PropertyValuesHolder anglePH = PropertyValuesHolder.ofFloat("mAlpha",
                    1f,0f);
            if(mGLViewScaleOutAnim == null){
                mGLViewScaleOutAnim = new ValueAnimator();
            }
            mGLViewScaleOutAnim.removeAllListeners();
            mGLViewScaleOutAnim.removeAllUpdateListeners();
            mGLViewScaleOutAnim.setValues(scalePH, anglePH);
            mGLViewScaleOutAnim.setDuration(200);
            mGLViewScaleOutAnim.addUpdateListener(updateListener);
            mGLViewScaleOutAnim.addListener(animatorListener);
            mGLSurfaceView.setVisibility(View.INVISIBLE);
        }
    }
    
    public void initGLTexture() {
        CellLayout curPage = (CellLayout)getChildAt(mCurrentPage);
        if (curPage == null) {
            return;
        }
        drawScreenToBitmap(mCurrentPage, mTextureCache);
        mGLRenderer.setFrontTexture(mTextureCache);
        mFrontTexturedScreen = mCurrentPage;
    }

    private void deinitGLSurfaceView() {
        if (mGLRenderer != null) {
            mGLRenderer.setBackTexture(null);
            mGLRenderer.setFrontTexture(null);
            mGLRenderer = null;
        }
        if (mGLViewScaleInAnim != null) {
            mGLViewScaleInAnim.cancel();
            mGLViewScaleInAnim.removeAllListeners();
            mGLViewScaleInAnim.removeAllUpdateListeners();
            mGLViewScaleInAnim = null;
        }

        if (mGLViewScaleOutAnim != null) {
            mGLViewScaleOutAnim.cancel();
            mGLViewScaleOutAnim.removeAllListeners();
            mGLViewScaleOutAnim.removeAllUpdateListeners();
            mGLViewScaleOutAnim = null;
        }

        if (mGLSurfaceView != null) {
            mGLSurfaceView.onPause();
            mLauncher.getDragLayer().removeView(mGLSurfaceView);
            mGLSurfaceView.clearAnimation();
            mGLSurfaceView = null;
        }
        if (mTextureCache != null && !mTextureCache.isRecycled()) {
            mTextureCache.recycle();
            mTextureCache = null;
        }
        mGLCanvas = null;
    }

    private void drawScreenToBitmap(int screen, Bitmap bmp) {
        View child = getPageAt(screen);
        if (child == null) {
            return;
        }
        int w = child.getWidth();
        int h = child.getHeight();
        if (mTextureCache == null || mTextureCache.isRecycled()) {
            mTextureCache = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        mGLCanvas.setBitmap(mTextureCache);
        mGLCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        if (child instanceof CellLayout) {
            CellLayout cellLayout = (CellLayout) child;
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: header in cell layout
        /*
            if (cellLayout.isFakeChild() || !cellLayout.hasChild()) {
                cellLayout.draw(mGLCanvas);
            } else {
                cellLayout.drawShortcutsAndWidgetsOnCanvas(mGLCanvas);
            }
        */
            if (mLauncher.isInLauncherEditMode()) {
                cellLayout.draw(mGLCanvas);
            } else {
                cellLayout.drawShortcutsAndWidgetsOnCanvas(mGLCanvas);
            }
            /* YUNOS END */
        }
        mGLCanvas.setBitmap(null);
    }
    
    private boolean cloth_3d(Canvas canvas, int screen, double percentage, long drawingTime) {

        if (mGLViewAnimating) {
            if (mGLRenderer != null) {
                mGLRenderer.setVisible(true);
            }

        } else {
            if (mGLRenderer != null) {
                mGLRenderer.setVisible(false);
            }
            return true;
        }
        double absPercentage = Math.abs(percentage);
        int transOffsetX = 0;
        int transOffsetY = 0;
        mGLBaseScale = 1.0f;
        if (mLauncher.isInLauncherEditMode()) {
            mGLBaseScale = Workspace.sEditScale;
            transOffsetX = (int) (mGLSurfaceView.getWidth() * (1 - Workspace.sEditScale)
                    / 2 + 0.5);
            transOffsetY = (int) (mGLSurfaceView.getHeight() * (1 - Workspace.sEditScale)
                    / 2 + 0.5);
        }
        /* YUNOS BEGIN */
        //## modules(Home Shell): [Category]
        //## date: 2015/07/30 ## author: wangye.wy
        //## BugID: 6221911: category on desk top
        if (mLauncher.isInLauncherCategoryMode()) {
            mGLBaseScale = Workspace.CATEGORY_SCALE;
            transOffsetX = (int) (mGLSurfaceView.getWidth() * (1 - Workspace.CATEGORY_SCALE)
                    / 2 + 0.5);
            transOffsetY = (int) (mGLSurfaceView.getHeight() * (1 - Workspace.CATEGORY_SCALE)
                    / 2 + 0.5);
        }
        /* YUNOS END */
        mGLSurfaceView.setTranslationX(mGLSurfaceViewLoc[0] - transOffsetX);
        mGLSurfaceView.setTranslationY(mGLSurfaceViewLoc[1] - transOffsetY);

        int nextScreen = percentage < 0 ? (screen - 1) : (screen + 1);
        if (nextScreen >= getChildCount()) {
            nextScreen = 0;
        } else if (nextScreen < 0) {
            nextScreen = getChildCount() - 1;
        }
        View nextView = getChildAt(nextScreen);
        double nextPercentage = getPercentage(nextView, nextScreen);
        double angle = percentage * -180;
        if (screen == mCurrentPage) {
            // in edit mode, scrolling will stop at about 0.8 or -0.8,
            // we need to correct it to 1 or -1.
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode()) {
            /* YUNOS END */
                if (nextScreen >= 0 && nextScreen < getChildCount()) {
                    percentage = (1 - Math.abs(nextPercentage)) * Math.signum(percentage);
                    absPercentage = Math.abs(percentage);
                }
            }
        }
        if (mFrontTexturedScreen != screen && mBackTexturedScreen == screen) {
            // backward
            angle = nextPercentage * -180;
            if (mFrontTexturedScreen != nextScreen && nextPercentage < 0.75) {
                // update Front by next
                drawScreenToBitmap(nextScreen, mTextureCache);
                mGLRenderer.setFrontTexture(mTextureCache);
                mFrontTexturedScreen = nextScreen;
            }
        } else if (mFrontTexturedScreen != screen && mBackTexturedScreen != screen) {
            if (mFrontTexturedScreen == nextScreen && absPercentage < 0.75) {
                // backward
                angle = nextPercentage * -180;
                // update background by cur
                drawScreenToBitmap(screen, mTextureCache);
                mGLRenderer.setBackTexture(mTextureCache);
                mBackTexturedScreen = screen;
            }
            if (mFrontTexturedScreen != nextScreen && absPercentage < 0.75) {
                // forward
                // update Front by cur
                drawScreenToBitmap(screen, mTextureCache);
                mGLRenderer.setFrontTexture(mTextureCache);
                mFrontTexturedScreen = screen;
            }
        }

        if (screen == mCurrentPage) {
            mGLRenderer.setAngle((float) angle);
        }
        return !mGLViewAnimating;
    }

    private boolean glass_3d(Canvas canvas, int screen, double percentage, long drawingTime) {
        
        if (mGLViewAnimating) {
            if (mGLRenderer != null) {
                mGLRenderer.setVisible(true);
            }

        } else {
            if (mGLRenderer != null) {
                mGLRenderer.setVisible(false);
            }
            return true;
        }

        double absPercentage = Math.abs(percentage);
        int transOffsetX = 0;
        int transOffsetY = 0;
        mGLBaseScale = 1.0f;
        if (mLauncher.isInLauncherEditMode()) {
            mGLBaseScale = Workspace.sEditScale;
            transOffsetX = (int) (mGLSurfaceView.getWidth() * (1 - Workspace.sEditScale) / 2);
            transOffsetY = (int) (mGLSurfaceView.getHeight() * (1 - Workspace.sEditScale) / 2);
        }
        /* YUNOS BEGIN */
        //## modules(Home Shell): [Category]
        //## date: 2015/07/30 ## author: wangye.wy
        //## BugID: 6221911: category on desk top
        if (mLauncher.isInLauncherCategoryMode()) {
            mGLBaseScale = Workspace.CATEGORY_SCALE;
            transOffsetX = (int) (mGLSurfaceView.getWidth() * (1 - Workspace.CATEGORY_SCALE) / 2);
            transOffsetY = (int) (mGLSurfaceView.getHeight() * (1 - Workspace.CATEGORY_SCALE) / 2);
        }
        /* YUNOS END */
        mGLSurfaceView.setTranslationX(mGLSurfaceViewLoc[0] - transOffsetX);
        mGLSurfaceView.setTranslationY(mGLSurfaceViewLoc[1] - transOffsetY);

        if (screen == mCurrentPage) {
            // in edit mode, scrolling will stop at about 0.8 or -0.8,
            // we need to correct it to 1 or -1.
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode()) {
            /* YUNOS END */
                int movingPage = percentage < 0 ? (screen - 1) : (screen + 1);
                if (movingPage >= 0 && movingPage < getChildCount()) {
                    View showingView = getChildAt(movingPage);
                    double movingPercentage = getPercentage(showingView, movingPage);
                    percentage = (1 - Math.abs(movingPercentage)) * Math.signum(percentage);
                    absPercentage = Math.abs(percentage);
                }
            }
            mGLRenderer.setAngle((float) percentage * -180);
        }

        if (absPercentage < 0.5) {
            if (mFrontTexturedScreen != screen) {
                drawScreenToBitmap(screen, mTextureCache);
                mGLRenderer.setFrontTexture(mTextureCache);
                mFrontTexturedScreen = screen;
            }
        }
        return !mGLViewAnimating;
    }
    
    public void onScreenOn() {
        if (mGLSurfaceView != null && is3DEffect(mPageAnimType)) {
            mGLSurfaceView.onResume();
        }
    }
    
    public void onScreenOff() {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.onPause();
        }
        if (mTextureCache != null && !mTextureCache.isRecycled()) {
            mTextureCache.recycle();
            mTextureCache = null;
        }
        if (mGLRenderer != null) {
            mGLRenderer.setBackTexture(null);
            mGLRenderer.setFrontTexture(null);
        }
        mFrontTexturedScreen = -1;
        mBackTexturedScreen = -1;
    }
    
    /* YUNOS END PB */
    
    /*
     * Scroll like looking from outsize of a box
     */
    private boolean boxOut(Canvas canvas, int screen, double percentage ){
        float angle = 90 * (float)percentage;
        float centerX, centerY;

        View child = getChildAt(screen);
        int childW = child.getWidth();
        int childH = child.getHeight();
        int pageOffsetX = getChildOffset(screen);
        int pageOffsetY = (int) ((getHeight() - child.getHeight()) / 2);

        if( angle >= 0 ){
            centerX = childW + pageOffsetX;
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode())
            /* YUNOS END */
                centerX -= mPageSpacing;
            centerY = pageOffsetY + childH / 2;
        }else{
            centerX = pageOffsetX;
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode())
            /* YUNOS END */
                centerX += mPageSpacing;
            centerY = pageOffsetY + childH / 2;
        }
        mCamera.rotateY(-angle); // rotate around Y-axis reversely
        mCamera.setLocation(0, 0, -14 * getDensity());
        mCamera.getMatrix(mMatrix);
        mMatrix.preScale(1.0f - (Math.abs((float) percentage) * 0.3f), 1.0f);
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);
        return true;
    }

    /*
     * Scroll like looking from inside of a box
     */
    private boolean boxIn(Canvas canvas, int screen, double percentage ){
        float angle = 90 * (float)percentage;
        float centerX, centerY, changeZ;

        View child = getChildAt(screen);
        int childW = child.getWidth();
        int childH = child.getHeight();
        int pageOffsetX = getChildOffset(screen);
        int pageOffsetY = (int) ((getHeight() - child.getHeight()) / 2);

        if( angle >= 0 ){
            centerX = childW + pageOffsetX;
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode())
            /* YUNOS END */
                centerX -= mPageSpacing;
            centerY = pageOffsetY + childH / 2;
        }else{
            centerX = pageOffsetX;
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode())
            /* YUNOS END */
                centerX += mPageSpacing;
            centerY = pageOffsetY + childH / 2;
        }

        // In case of image expand, change Z-order
        if (angle >= 0) {
            // far to near (0-45), near to far (45-90)
            if (angle <= 45.0f) {
                changeZ = childW*(float)Math.sin(2 * Math.PI * angle /360f);
                mCamera.translate( 0, 0, changeZ );
            } else {
                changeZ = childW*(float)Math.sin(2 * Math.PI *(90-angle)/360f);
                mCamera.translate( 0, 0, changeZ );
            }
        } else {
            // make sure that two views join well
            if (angle > -45.0f) {
                changeZ = childW * (float) Math.sin(2 * Math.PI * (-angle)/ 360f);
                mCamera.translate( 0, 0, changeZ );
            } else {
                changeZ = childW * (float) Math.sin(2 * Math.PI* (90.0f + angle) / 360f);
                mCamera.translate( 0, 0, changeZ );
            }
        }
        mCamera.rotateY(angle);
        mCamera.setLocation(0, 0, -12 * getDensity());
        mCamera.getMatrix(mMatrix);
        mMatrix.preScale(1.0f - (Math.abs((float) percentage) * 0.5f), 1.0f);
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);

        return true;
    }

    /*
     * Rotate around the top of the screen
     */
    private boolean rollUp(Canvas canvas, int screen, double percentage ){
        float angle = 90 * (float)percentage;
        float baseAngle = angle * 0.25f; // Maximum Angle 30
        float centerX, centerY;

        View child = getChildAt(screen);
        int childW = child.getWidth();
        int childH = child.getHeight();
        double gapOfCellLayouts = ( getWidth() - childW ) / 2;
        double switchWidth = getChildCount() * ( childW + gapOfCellLayouts );
        double wholeWidth = gapOfCellLayouts + switchWidth;

        if( getScrollX() < 0 && screen == getChildCount()-1 ) {
            centerX = getScrollX() + (float)switchWidth + getWidth() / 2;
        }else if( getScrollX() + getWidth() > wholeWidth && screen == 0){
            centerX = getScrollX() - (float)switchWidth + getWidth() / 2;
        }else{
            centerX = getScrollX() + getWidth() / 2;
        }
        centerY = getScrollY() - childH * 0.3f;

        mMatrix.reset();
        mMatrix.setRotate(baseAngle);
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);

        return true;
    }

    /*
     * Rotate around the bottom of the screen
     */
    private boolean rollDown(Canvas canvas, int screen, double percentage ){
        float angle = 90 * (float)percentage;
        float baseAngle = -angle * 0.333f; // Maximum Angle 30
        float centerX, centerY;

        View child = getChildAt(screen);
        int childW = child.getWidth();
        int childH = child.getHeight();
        double gapOfCellLayouts = ( getWidth() - childW ) / 2;
        double switchWidth = getChildCount() * ( childW + gapOfCellLayouts );
        double wholeWidth = gapOfCellLayouts + switchWidth;

        if( getScrollX() < 0 && screen == getChildCount()-1 ) {
            centerX = getScrollX() + (float)switchWidth + getWidth() / 2;
        }else if( getScrollX() + getWidth() > wholeWidth && screen == 0){
            centerX = getScrollX() - (float)switchWidth + getWidth() / 2;
        }else{
            centerX = getScrollX() + getWidth() / 2;
        }
        centerY = childH * 1.3f;

        mMatrix.reset();
        mMatrix.setRotate(baseAngle);
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);
        
        return true;
    }

    private boolean rollOver(View v, int i, float scrollProgress){
        /* YUNOS BEGIN */
        //## modules(Home Shell): [Category]
        //## date: 2015/07/30 ## author: wangye.wy
        //## BugID: 6221911: category on desk top
        if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode()) {
            scrollProgress = scrollProgress / 0.85f;
            scrollProgress = Math.min(1.0f, scrollProgress);
            scrollProgress = Math.max(-1.0f, scrollProgress);
        }
        /* YUNOS END */
        
        v.setCameraDistance( getDensity() * CAMERA_DISTANCE);
        boolean drawChild;
        if (scrollProgress >= -0.5f && scrollProgress <= 0.5f) {
            drawChild = true;
        }else{
            drawChild = false;
        }

        int offset = 0;
        if( scrollProgress > 0.5 ){
            scrollProgress = 1 - scrollProgress;
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode()) {
            /* YUNOS END */
                offset = - mPageSpacing;
            }
        }else if( scrollProgress < -0.5){
            scrollProgress = - 1 - scrollProgress;
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode()) {
            /* YUNOS END */
                offset = + mPageSpacing;
            }
        }

        float rotation = -180.0f * Math.max(-1f, Math.min(1f, scrollProgress));
        v.setPivotX(v.getMeasuredWidth() * 0.5f);
        /* YUNOS BEGIN */
        //## modules(Home Shell): [Category]
        //## date: 2015/07/30 ## author: wangye.wy
        //## BugID: 6221911: category on desk top
        if (!mLauncher.isInLauncherEditMode() && !mLauncher.isInLauncherCategoryMode())
        /* YUNOS END */
            v.setPivotY(v.getMeasuredHeight() * 0.5f);
        v.setRotationY(rotation);
        v.setTranslationX(v.getMeasuredWidth() * scrollProgress + offset);

        return drawChild;
    }

    /*
     * Flip around y-axis
     */
    private boolean rollWindow(View v, int screen, float scrollProgress){
        CellLayout cellLayout = (CellLayout) v;
        ShortcutAndWidgetContainer container = cellLayout.getShortcutAndWidgetContainer();

        if (Math.abs(scrollProgress) < 0.5f) {
            v.setAlpha(1);
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if ((mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode())
            /* YUNOS END */
                    && scrollProgress != 0) {
                //chenjian modified for Bug 5997572
                //float trans = scrollProgress * (getWidth() + v.getWidth()) / 2;
                int width = getScaledMeasuredWidth(getPageAt(screen)) + mPageSpacing;
                float trans = scrollProgress * width + (getWidth() - (width-mPageSpacing))/2;
                v.setTranslationX(trans);
                //chenjian modified for Bug 5997572
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            } else if (!mLauncher.isInLauncherEditMode() && !mLauncher.isInLauncherCategoryMode()) {
            /* YUNOS END */
                v.setTranslationX(scrollProgress * (getWidth() + v.getWidth()) / 2);
            }
        } else {
            //chenjian modified for Bug 5997572
            if (mLauncher.isInLauncherEditMode()) {
                v.setTranslationX(v.getWidth() * (1 - Workspace.sEditScale) / 2);
            }
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherCategoryMode()) {
                v.setTranslationX(v.getWidth() * (1 - Workspace.CATEGORY_SCALE) / 2);
            }
            /* YUNOS END */
            //chenjian modified for Bug 5997572
            v.setAlpha(0);
        }

        if (!cellLayout.isLeftPage()) {
            int count = container.getChildCount();
            for (int i = 0; i < count; i++) {
                View view = container.getChildAt(i);

                // (BugID:5220027 by wenliang.dwl) make FolderIcon display normally
                view.setCameraDistance(10000);

                view.setPivotX(view.getWidth() * 0.5f);
                view.setRotationY(-scrollProgress * 180f);
            }
        } else {
            // ##date:2015/8/3 ##author:zhanggong.zg ##BugID:6263718
            // for life center, apply the transformation on the cell layout
            v.setCameraDistance(getDensity() * CAMERA_DISTANCE);
            v.setPivotX(v.getWidth() * 0.5f);
            v.setRotationY(-scrollProgress * 180f);
        }

        return true;
    }

    /*
     * Squash and Stretch
     */
    private boolean scaleInOut(Canvas canvas, int screen, double percentage){
        float angle = 90f * (float)percentage;
        View child = getChildAt(screen);
        int pageOffsetX = getChildOffset(screen);
        int pageOffsetY = (int) ((getHeight() - child.getHeight()) / 2);

        if (angle >= 0) { // left page
            float centerX = pageOffsetX + child.getWidth();
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode())
            /* YUNOS END */
                centerX -= mPageSpacing;
            float centerY = pageOffsetY;
            canvas.translate(centerX, centerY);
            canvas.scale((90.0f - angle) / 90.0f, 1.0f);
            canvas.translate(-centerX, -centerY);
        } else {
            float centerX = pageOffsetX;
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncher.isInLauncherEditMode() || mLauncher.isInLauncherCategoryMode())
            /* YUNOS END */
                centerX += mPageSpacing;
            float centerY = pageOffsetY;
            canvas.translate(centerX, centerY);
            canvas.scale((90.0f + angle) / 90.0f, 1.0f);
            canvas.translate(-centerX, -centerY);
        }
        return true;
    }

    /*
     * Icons change it's position randomly in Y-axis.
     */
    private boolean randomSwitch(View v, int screen, float scrollProgress){
        CellLayout cellLayout = (CellLayout) v;
        ShortcutAndWidgetContainer container = cellLayout.getShortcutAndWidgetContainer();

        final float verticalDelta = 0.7f * cellLayout.getCellHeight()
                                    * (float) (1 - Math.abs(2 * Math.abs(scrollProgress) - 1));

        for (int i = 0; i < container.getChildCount(); i++) {
            View view = container.getChildAt(i);
            ItemInfo info = (ItemInfo) view.getTag();
            if ((info.cellX % 2 == 0)) {
                // even columns
                view.setTranslationY(verticalDelta);
            } else {
                // odd columns
                view.setTranslationY(-verticalDelta);
            }
        }
        return true;
    }

    /*
     * Left side scroll to left.Right side fade away.
     */
    private boolean rightFade(View v, int i, float scrollProgress){
        if( scrollProgress >= 0 ){
            v.setScaleX(1);
            v.setScaleY(1);
            v.setAlpha(1);
            v.setTranslationX(0);
            return true;
        }

        double gapOfCellLayouts = ( getWidth() - v.getWidth() ) / 2;
        float scale = 0.5f + 0.5f * (float)(1+scrollProgress);
        float alpha = (1+scrollProgress);
        float trans = Math.abs(scrollProgress) * (float)(gapOfCellLayouts + v.getWidth());

        v.setPivotX(v.getWidth()/2);
        v.setPivotY(v.getHeight()/2);
        v.setScaleX(scale);
        v.setScaleY(scale);
        v.setAlpha(alpha);
        v.setTranslationX(-trans);

        return true;
    }

    @Override
    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(6718902) ##date:2015/12/01
        // ##description: init 3d resource when begin moving.
        int oldPageAnimType = mPageAnimType;
        if (mLauncher.getEditmodeContainer() != null) {
            mPageAnimType = Integer.parseInt(mLauncher.getEditmodeContainer()
                    .getEffectsPreviewAdapter().getCurrentEffectValue());
        } else {
            mPageAnimType = HomeShellSetting.getSlideEffectMode(this.getContext());
        }
        if (oldPageAnimType != mPageAnimType) {
            on3DEffectChanged(oldPageAnimType, mPageAnimType);
        }
        /* YUNOS END PB */
        if( mPageAnimType == Type.ROLL_UP || mPageAnimType == Type.ROLL_DOWN ){
            mScroller = mBounceScroller;
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(5681912) ##date:2015/01/05
        // ##description: add 3d transition effects
        } else if (is3DEffect(mPageAnimType)) {
            View child = getPageAt(mCurrentPage);
            child.getLocationOnScreen(mGLSurfaceViewLoc);
            initGLTexture();
            if (mGLViewScaleOutAnim.isRunning()) {
                mGLViewScaleOutAnim.cancel();
            }
            if (mGLViewScaleInAnim.isRunning()) {
                mGLViewScaleInAnim.cancel();
            } 
            mGLViewScaleInAnim.start();
        /* YUNOS END PB */
        /* YUNOS BEGIN */
        // ##modules(HomeShell): ##xiangnan.xn@alibaba-inc.com
        // ##BugID:7826299 ##date:2016/01/21/
        //  ROLL_WINDOW effect can only run correctly after reset properties under EditMode
        } else if (mPageAnimType == Type.ROLL_WINDOW
                && mLauncher.isInLauncherEditMode()) {
            resetChildrenProperties();
        /* YUNOS END */
        }
    }
    
    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();
        mIconBitmapCache.clear();
        if( mPageAnimType == Type.ROLL_UP || mPageAnimType == Type.ROLL_DOWN ){
            mScroller = mOriginScroller;
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(5681912) ##date:2015/01/05
        // ##description: add 3d transition effects
        } else if (is3DEffect(mPageAnimType)) {
            if (mGLViewScaleOutAnim.isRunning()) {
                mGLViewScaleOutAnim.cancel();
            }
            if (mGLViewScaleInAnim.isRunning()) {
                mGLViewScaleInAnim.cancel();
            }
            if (mGLViewAnimating) {
                mGLViewScaleOutAnim.start();
            }
        }
        /* YUNOS END PB */
        
        // recovers the transformation during sliding
        if (mPageAnimType == Type.RIGHT_FADE) {
            if(this instanceof Workspace && ((Workspace)this).getOpenFolder() != null ){
                // BugID:5306404,wenliang.dwl,don't restore when folder is open
                return;
            }
            if (mLauncher.getDragController().isDragging()) {
                return;
            }

            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View v = getPageAt(i);
                if( v != null ){
                    float scaleX = 1.0f;
                    float scaleY = 1.0f;
                    float transX = 0f;

                    if (mLauncher.isInLauncherEditMode()) {
                        scaleX = Workspace.sEditScale;
                        scaleY = Workspace.sEditScale;
                    }

                    /* YUNOS BEGIN */
                    //## modules(Home Shell): [Category]
                    //## date: 2015/07/30 ## author: wangye.wy
                    //## BugID: 6221911: category on desk top
                    if (mLauncher.isInLauncherCategoryMode()) {
                        scaleX = Workspace.CATEGORY_SCALE;
                        scaleY = Workspace.CATEGORY_SCALE;
                    }
                    /* YUNOS END */

                    v.setScaleX(scaleX);
                    v.setScaleY(scaleY);
                    v.setAlpha(1);
                    /* YUNOS BEGIN */
                    // ## date: 2016/09/27 ## author: yongxing.lyx
                    // ## BugID:8894691:screen position error after added and deleted screen.
                    if (mCurrentPage != i) {
                        v.setTranslationX(-transX);
                    }
                    /* YUNOS BEGIN */
                }
            }
        } else if (mPageAnimType == Type.ROLL_WINDOW ||
                   mPageAnimType == Type.RANDOM_SWITCH) {
            recoverPageTransformation();
        } else if (mPageAnimType == Type.ROLL_OVER){
            // for bug 5236360, restore state
            for( int i = 0; i < getChildCount(); i++ ){
                View v = getChildAt(i);
                /* YUNOS BEGIN */
                // ## date: 2016/09/27 ## author: yongxing.lyx
                // ## BugID:8894691:screen position error after added and deleted screen.
                if( v != null && mCurrentPage != i) {
                    v.setRotationY(0);
                    v.setTranslationX(0);
                }
                /* YUNOS BEGIN */
            }
        }
    }

    /**
     * Recovers the transformation that applied to page and icons
     * during the sliding effect.<p>
     * Currently, {@link Type#ROLL_WINDOW} and {@link Type#RANDOM_SWITCH}
     * use this method to recover transformation to default state in
     * {@link #onPageEndMoving()}.
     */
    private void recoverPageTransformation() {
        for( int i = 0; i < getChildCount(); i++ ){
            CellLayout cl = (CellLayout)getChildAt(i);
            if( cl == null ) continue;
            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (!mLauncher.isInLauncherEditMode() && !mLauncher.isInLauncherCategoryMode())
            /* YUNOS END */
                cl.setTranslationX(0);
            if (cl.isLeftPage())
                cl.setRotationY(0);
            cl.setAlpha(1);
            ShortcutAndWidgetContainer container = cl.getShortcutAndWidgetContainer();
            if( container == null ) continue;
            for( int j = 0; j < container.getChildCount(); j++ ){
                View v = container.getChildAt(j);
                v.setRotationY(0);
                v.setTranslationY(0);
            }
        }
    }

    private static class BounceBackInterpolator extends ScrollInterpolator {
        public BounceBackInterpolator() {
        }

        public float getInterpolation(float t) {
            t = super.getInterpolation(t);
            float UP_BOUND = 1.1f;
            float TURN_POINT = 0.9f;
            if( t < TURN_POINT ){
                return t * ( UP_BOUND / TURN_POINT );
            }else{
                return UP_BOUND - (t - TURN_POINT) * ( (UP_BOUND-1) / (1-TURN_POINT) );
            }
        }
    }

    @Override
    public void syncPages() {}
    @Override
    public void syncPageItems(int page, boolean immediate) {}
}
