
package com.aliyun.homeshell.effects3d;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;

public abstract class LauncherRenderer implements Renderer {

    protected Context mContext;
    protected GLSurfaceView mGLView;
    protected float mAngle;
    protected float mScale;
    protected float mAlpha;
    protected boolean mVisible;
    protected boolean mNeedUpdateFrontTexture;
    protected boolean mNeedUpdateBackTexture;
    protected Bitmap mFrontTextureBmp;
    protected Bitmap mBackTextureBmp;

    public LauncherRenderer(Context context, GLSurfaceView glView) {
        this.mGLView = glView;
        this.mContext = context;
        mNeedUpdateFrontTexture = false;
        mNeedUpdateBackTexture = false;
    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    public void setAngle(float angle) {
        mAngle = angle;
        mGLView.requestRender();
    }

    public float getAngle() {
        return mAngle;
    }

    public void setScale(float scale) {
        mScale = scale;
        mGLView.requestRender();
    }

    public float getScale() {
        return mScale;
    }
    // only affect the model and reflect light's texture, 
    // and don't affect the content texture. 
    public void setAlpha(float alpha) {
        mAlpha = alpha;
        mGLView.requestRender();
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setVisible(boolean v) {
        mVisible = v;
        mGLView.requestRender();
    }

    public boolean getVisible() {
        return mVisible;
    }

    public void setFrontTexture(Bitmap bmp) {
        mFrontTextureBmp = bmp;
        if (bmp != null) {
            mNeedUpdateFrontTexture = true;
        } else {
            mNeedUpdateFrontTexture = false;
        }
    }

    public void setBackTexture(Bitmap bmp) {
        mBackTextureBmp = bmp;
        if (bmp != null) {
            mNeedUpdateBackTexture = true;
        } else {
            mNeedUpdateBackTexture = false;
        }
    }

}
