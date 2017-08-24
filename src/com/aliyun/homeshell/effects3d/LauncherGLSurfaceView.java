
package com.aliyun.homeshell.effects3d;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

public class LauncherGLSurfaceView extends GLSurfaceView {

    public LauncherGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);

        this.setEGLConfigChooser(new EGLConfigChooser() {

            @Override
            public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                int[] attrList = new int[] { 
                        EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT, 
                        EGL10.EGL_RED_SIZE, 8, 
                        EGL10.EGL_GREEN_SIZE, 8, 
                        EGL10.EGL_BLUE_SIZE, 8, 
                        EGL10.EGL_ALPHA_SIZE, 8,
                        EGL10.EGL_DEPTH_SIZE, 16, 
                        EGL10.EGL_SAMPLE_BUFFERS, 1,
                        EGL10.EGL_SAMPLES, 2,
                        EGL10.EGL_NONE
                };
                EGLConfig[] configOut = new EGLConfig[1];
                int[] configNumOut = new int[1];
                egl.eglChooseConfig(display, attrList, configOut, 1, configNumOut);
                return configOut[0];

            }
        });
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    @Override
    public void setRenderer(final Renderer renderer) {
        // if (renderer instanceof LauncherRender) {
        // setOnTouchListener(new OnTouchListener() {
        // @Override
        // public boolean onTouch(View v, MotionEvent event) {
        // ((LauncherRender) renderer).onTouch(event);
        // return false;
        // }
        // });
        // }
        super.setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

}
