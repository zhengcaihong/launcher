
package com.aliyun.homeshell.effects3d;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

public class ClothShader extends LauncherRenderer {
//    private static final float DEFAULT_ALPHA = 1f;

    private static final String mVertexShader = "uniform mat4 u_MVPMatrix;\r\n" +
            "uniform float a_angle;\r\n" +
            "uniform float a_screenratio;\r\n" +
            "uniform float a_ySpan;\r\n" +
            "uniform float  a_direct;\r\n" +
            "attribute vec4 a_Position; \r\n" +
            "attribute vec2 a_frontTextureCoord;\r\n" +
            "attribute vec2 a_backTextureCoord;\r\n" +
            "varying vec2 vFrontTextureCoord;\r\n" +
            "varying vec2 vBackTextureCoord;\r\n" +
            "void main()            \r\n" +
            "{\r\n" +
            "   float pi = 3.14159265359;\r\n" +
            "   vec4 tPosition = a_Position;\r\n" +
            "   float height = a_ySpan;\r\n" +
            "   float yStart = height/2.;\r\n" +
            "   bool  right2left = a_angle<=0.;\r\n" +
            "   float targetAngle = mod(abs(a_angle),180.);\r\n" +
            "   float base = pi/2.;\r\n" +
            "   float radians = radians(targetAngle);\r\n" +
            "   float r = a_screenratio;\r\n" +
            "   float percentY = 0.0;\r\n" +
            "   if(right2left){\r\n" +
            "        percentY =  (yStart - a_Position.y)/height;\r\n" +
            "      }else{ //start from bottom.\r\n" +
            "            percentY =  (a_Position.y + yStart)/height;\r\n" +
            "      }\r\n" +
            "     float alpha  = 2.5*radians - pi/(2.*r)*percentY;\r\n" +
            "      if(alpha<0.){\r\n" +
            "      alpha = 0.;\r\n" +
            "   }\r\n" +
            "//float overRadians = 2.5*pi -  pi/(2.*r)  - pi;\r\n" +
            "float targetRadians= alpha;\r\n" +
            "if ( targetRadians > pi  ){\r\n" +
            "                     targetRadians = pi;\r\n" +
            "}\r\n" +
            "float x = a_Position.x;\r\n" +
            "if ( !right2left ){\r\n" +
            "     x= - a_Position.x;\r\n" +
            "}\r\n" +
            "  tPosition.x=cos(targetRadians)*a_Position.x;\r\n" +
            "  tPosition.z=a_Position.z + sin(targetRadians)*x ;\r\n" +
            "  gl_Position = u_MVPMatrix * tPosition;\r\n" +
            "  vFrontTextureCoord = a_frontTextureCoord;\r\n" +
            "   vBackTextureCoord = a_backTextureCoord;\r\n" +
            "}";
    
    private static final String mFragmentShader = "precision mediump float;\r\n" +
            "varying vec2 vFrontTextureCoord;  \r\n" +
            "varying vec2 vBackTextureCoord;\r\n" +
            "uniform float u_offsetx;\r\n" +
            "uniform float u_offsety;\r\n" +
            "uniform float u_alpha;\r\n" +
            "uniform sampler2D u_frontTexture;\r\n" +
            "uniform sampler2D u_maskTexture;\r\n" +
            "uniform sampler2D u_backTexture;  \r\n" +
            "void main()       \r\n" +
            "{                   \r\n" +
            "    vec4 frontColor; \r\n" +
            "    vec4 maskColor;  \r\n" +
            "     vec4 backColor;  \r\n" +

            "    maskColor = texture2D(u_maskTexture,vFrontTextureCoord); \r\n" +
            "    if(gl_FrontFacing){\r\n" +
            "        frontColor = texture2D(u_frontTexture,vFrontTextureCoord);\r\n" +
            "       gl_FragColor= maskColor*u_alpha + frontColor   ;\r\n" +
            "     }\r\n" +
            "     else{\r\n" +
            "          backColor = texture2D(u_backTexture,vBackTextureCoord); \r\n" +
            "         gl_FragColor=    maskColor*u_alpha +backColor   ;\r\n" +
            "    }\r\n" +
            "}";

    private int mWidth;
    private int mHeight;

    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mAngleHandle;

    private int mProgramHandle;
    private int mFrontTexCoordHandle;
    private int mBackTexCoordHandle;

    private int mFrontTextureHandle;
    private int mMaskTextureHandle;
    private int mBackTextureHandle;

    private int mXSpanHandle;
    private int mYSpanHandle;
    private int mDirectHandle;
    private int mAlphaHandle;

    private float[] mMVPMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];

    float y = 1.6f;
    float x = 1.0f;
    float mTextureCoorOffset = 0.5f;

    private float mOldAngle;

    public ClothShader(Context context, GLSurfaceView glView) {
        super(context, glView);
    }

    private Mesh mFrontPlane;
    private int mFrontTextureId;
    private int mBackTextureId;
    private int mMaskId;
    private FloatBuffer mReserveBuffer = null;
//    private float reverseAngle = 0f;
    private float mScreenRatio;

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // TODO Auto-generated method stub
        mWidth = width;
        mHeight = height;
        mScreenRatio = (float) mWidth / mHeight;
        y = 8f; // must *2 ,because Plane height is y/2.
        x = y * mScreenRatio;
        mFrontPlane = new Plane(x, y, 0, 100, (int) (100f / mScreenRatio));
        mFrontPlane.reverseTextureCoord(true);
        mReserveBuffer = mFrontPlane.getTexureCoordBuffer();
        mFrontPlane.reverseTextureCoord(false);
//        reverseAngle = (float) Math.toDegrees(((Math.PI / 2 + Math.PI
//                / (mScreenRatio)) / 2.5f));
        GLES20.glViewport(0, 0, mWidth, mHeight);

        final float left = -mScreenRatio;
        final float right = mScreenRatio;

        final float eyeX = 0.0f;
        final float eyeY = 0f;
        final float eyeZ = 8f;

        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 2f;
        final float far = 14f;
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near,
                far);

        final float lookX = 0.0f;
        final float lookY = 0;
        final float lookZ = 0f;

        final float upX = 0.0f;
        final float upY = 1f;
        final float upZ = 0.0f;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY,
                lookZ, upX, upY, upZ);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
//         GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA_SATURATE, GLES20.GL_ONE);
        final String vertexShader = mVertexShader;
        final String fragmentShader = mFragmentShader;
        mProgramHandle = GLUtil.createProgram(vertexShader, fragmentShader);
        if (mProgramHandle == 0) {
            throw new RuntimeException("failed to create program");
        }
        intShaderValues();
        Bitmap b = GLUtil.getBitmap(mContext, com.aliyun.homeshell.R.drawable.gl_cloth_bg,
                mGLView.getWidth(), mGLView.getHeight());
        mMaskId = GLUtil.initTexture(mContext, b);
        b.recycle();//must recycle.
        mFrontTextureId = GLUtil.initTexture(mContext, null);
        mBackTextureId = GLUtil.initTexture(mContext, null);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mVisible) {
            drawEffect();
        }
    }

    private void intShaderValues() {
        GLES20.glUseProgram(mProgramHandle);
        // uniform
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "u_MVPMatrix");
        mFrontTextureHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "u_frontTexture");
        mMaskTextureHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "u_maskTexture");
        mBackTextureHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "u_backTexture");
        mAngleHandle = GLES20.glGetUniformLocation(mProgramHandle, "a_angle");
        mXSpanHandle = GLES20.glGetUniformLocation(mProgramHandle, "a_screenratio");
        mYSpanHandle = GLES20.glGetUniformLocation(mProgramHandle, "a_ySpan");

        mDirectHandle = GLES20.glGetUniformLocation(mProgramHandle, "a_direct");
        mAlphaHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_alpha");

        // attributes
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle,
                "a_Position");
        mFrontTexCoordHandle = GLES20.glGetAttribLocation(mProgramHandle,
                "a_frontTextureCoord");
        mBackTexCoordHandle = GLES20.glGetAttribLocation(mProgramHandle,
                "a_backTextureCoord");
    }

    private void drawEffect() {
        if (mNeedUpdateFrontTexture) {
            GLUtil.updateTexture(mFrontTextureId, mFrontTextureBmp);
            mNeedUpdateFrontTexture = false;
            mFrontTextureBmp = null;
        }
        if (mNeedUpdateBackTexture) {
            GLUtil.updateTexture(mBackTextureId, mBackTextureBmp);
            mNeedUpdateBackTexture = false;
            mBackTextureBmp = null;
        }

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0f, 0f);
        int rotates = (int) (mAngle / 180);
        Matrix.rotateM(mModelMatrix, 0, -rotates * 180, 0f, 1.0f, 0.0f);
        // if (Math.abs(mAngle) % 180 > reverseAngle) {
        // GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_DST_ALPHA);
        // } else {
        // GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA_SATURATE, GLES20.GL_ONE);
        // }

        GLES20.glUniform1f(mAngleHandle, mAngle);
        GLES20.glUniform1f(mXSpanHandle, mScreenRatio);
        GLES20.glUniform1f(mYSpanHandle, y);
        GLES20.glUniform1f(mDirectHandle, mAngle - mOldAngle);
        GLES20.glUniform1f(mAlphaHandle, mAlpha);
        Matrix.scaleM(mModelMatrix, 0, mScale, mScale, 1);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // --------------------- bind multi texture -----------------------
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mFrontTexCoordHandle);
        GLES20.glEnableVertexAttribArray(mBackTexCoordHandle);

        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT,
                false, 0, mFrontPlane.getVertexBuffer());
        GLES20.glVertexAttribPointer(mFrontTexCoordHandle, 2, GLES20.GL_FLOAT,
                false, 0, mFrontPlane.getTexureCoordBuffer());
        GLES20.glVertexAttribPointer(mBackTexCoordHandle, 2, GLES20.GL_FLOAT,
                false, 0, mReserveBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrontTextureId);
        GLES20.glUniform1i(mFrontTextureHandle, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mMaskId);
        GLES20.glUniform1i(mMaskTextureHandle, 1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBackTextureId);
        GLES20.glUniform1i(mBackTextureHandle, 2);
        mFrontPlane.draw();
    }

    @Override
    public void setAngle(float angle) {
        this.mOldAngle = mAngle;
        this.mAngle = angle;
        mGLView.requestRender();
    }

}
