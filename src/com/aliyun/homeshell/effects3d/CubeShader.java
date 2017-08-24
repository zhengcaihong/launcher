
package com.aliyun.homeshell.effects3d;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.aliyun.homeshell.R;

public class CubeShader extends LauncherRenderer {
    private static final float DEFAULT_ALPHA = 1f;

    private static final String mVertexShader = "uniform mat4 u_MVPMatrix;\r\n" +
            "attribute vec4 a_Position;\r\n" +
            "attribute vec2 aTexCoor;\r\n" +
            "attribute vec2 aBaseTexCoor;\r\n" +
            "varying vec2 vTextureCoord;\r\n" +
            "varying vec2 vBaseTextureCoord;\r\n" +
            "void main()                    \r\n" +
            "{\r\n" +
            "gl_Position = u_MVPMatrix*a_Position;\r\n" +
            "vTextureCoord = aTexCoor;\r\n" +
            "vBaseTextureCoord = aBaseTexCoor;\r\n" +
            "}         ";
    
    private static final String mFragmentShader = "precision mediump float;\r\n"
            +
            "varying vec2 vTextureCoord;\r\n"
            +
            "varying vec2 vBaseTextureCoord;  \r\n"
            +
            "uniform float u_offsetx;\r\n"
            +
            "uniform float u_offsety;\r\n"
            +
            "uniform sampler2D u_baseTexture;    \r\n"
            +
            "uniform sampler2D u_lightTexture;   \r\n"
            +
            "uniform float u_light_alpha;\r\n"
            +
            "uniform float ubase_alpha;\r\n"
            +
            "void main()               \r\n"
            +
            "{                        \r\n"
            +
            "    vec4 baseColor; \r\n"
            +
            "    vec4 lightColor;  \r\n"
            +
            "    float x = u_offsetx*0.005;    \r\n"
            +
            "    lightColor = texture2D(u_lightTexture,vec2(vTextureCoord.x + x,vTextureCoord.y)); \r\n"
            +
            "    baseColor = texture2D(u_baseTexture,vBaseTextureCoord); \r\n" +
            "    gl_FragColor = lightColor*u_light_alpha + baseColor*ubase_alpha;\r\n" +
            "}";
                         

    
    private int mWidth;
    private int mHeight;

    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mProgramHandle;
    private int mLigthTexCoorHandle;
    private int mBaseTexCoorHandle;
    private int mOffsetXHandle;;
    private int mOffsetYHandle;;
    private int mBaseTextureHandle;
    private int mLightTextureHandle;
    private int mLightAlphaHandle;
    private int mBaseAlphaHandle;

    private FloatBuffer mCubeVertexCoorBuffer;
    private FloatBuffer mLightTextureCoorBuffer;
    // reverse top and bottom texture coord according to angle.
    private FloatBuffer mReversedLightTextureCoorBuffer;
    private FloatBuffer mBaseTextureCoorBuffer;

    private float[] mMVPMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];

    private int mLightTextureId;
    private int mSideTextureId;
    private int mTabTextureId;
    private int mFrontTextrueId;
    private int mBackTextrueId;

    float z = 0.06f;
    float y = 1.328f;
    float x = 1.0f;
    float mTextureCoorOffset = 0.5f;

    private float mLightAlpha = DEFAULT_ALPHA;
    private float mBaseAlpha = DEFAULT_ALPHA;
    private boolean isFirstDraw = true;

    public CubeShader(Context context, GLSurfaceView glView) {
        super(context, glView);
    }

    private void initVertexts() {
        final float cubeVertexCoor[] = {
                // Front face
                -x, y, z, x, -y, z, x, y, z, -x, -y, z, -x, y, z, x,
                -y,
                z,
                // Right face
                x, y, z, x, -y, z, x, -y, -z, x, -y, -z, x, y, -z, x,
                y,
                z,
                // Back face
                x, y, -z, -x, -y, -z, -x, y, -z, x, -y, -z, x, y, -z, -x,
                -y,
                -z,
                // Left face
                -x, y, -z, -x, -y, -z, -x, -y, z, -x, -y, z, -x, y, z, -x, y,
                -z,
                // Top face
                x, y, -z, -x, y, -z, -x, y, z, -x, y, z, x, y, z, x, y, -z,
                // Bottom face
                x, -y, -z, -x, -y, -z, -x, -y, z, -x, -y, z, x, -y, z, x, -y,
                -z,
        };

        float[] lightTextureCoor = new float[] {
                // Front face
                mTextureCoorOffset, 0.0f, mTextureCoorOffset, 1.0f, 1f, 0f, 0,
                1,
                mTextureCoorOffset,
                0.0f,
                mTextureCoorOffset,
                1.0f,
                // Right face
                1, 0, mTextureCoorOffset, 1, mTextureCoorOffset, 0,
                mTextureCoorOffset, 1.0f, 1, 0, 1, 1,
                // Back face
                mTextureCoorOffset, 0.0f, mTextureCoorOffset, 1.0f, 1f, 0f, 0,
                1, mTextureCoorOffset,
                0.0f,
                mTextureCoorOffset,
                1.0f, // Left face
                mTextureCoorOffset, 0, 0, 0, 0, 1,
                0, 1, mTextureCoorOffset, 1, mTextureCoorOffset, 0,
                // Top face
                1, 0, mTextureCoorOffset, 0, mTextureCoorOffset, 1,
                mTextureCoorOffset, 1, 1, 1, 1, 0,
                // Bottom face
                1, 0, mTextureCoorOffset, 0, mTextureCoorOffset, 1,
                mTextureCoorOffset, 1, 1, 1, 1, 0,
        };

        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##weimin.yanwm@alibaba-inc.com
        // ##BugID:() ##date:2015/01/07
        // ##description: new texture Coord for diffirent angle
        float[] lightTextureCoorReverse = new float[] {
                // Front face
                mTextureCoorOffset, 0.0f, mTextureCoorOffset, 1.0f, 1f, 0f, 0,
                1,
                mTextureCoorOffset,
                0.0f,
                mTextureCoorOffset,
                1.0f,
                // Right face
                1, 0, mTextureCoorOffset, 1, mTextureCoorOffset, 0,
                mTextureCoorOffset, 1.0f, 1,
                0,
                1,
                1,
                // Back face
                mTextureCoorOffset, 0.0f, mTextureCoorOffset, 1.0f, 1f, 0f, 0,
                1, mTextureCoorOffset, 0.0f,
                mTextureCoorOffset,
                1.0f,
                // Left face
                1, 0, mTextureCoorOffset, 0, mTextureCoorOffset, 1,
                mTextureCoorOffset, 1, 1, 1, 1,
                0,
                // Top face
                mTextureCoorOffset, 0, 1, 0, 1, 1, 1, 1, mTextureCoorOffset, 1,
                mTextureCoorOffset, 0,
                // Bottom face
                0, 0, mTextureCoorOffset, 0, mTextureCoorOffset, 1,
                mTextureCoorOffset, 1, 0, 1, 0, 0,
        };
        /* YUNOS END PB */

        float[] baseTextureCoor = new float[] {
                // Front face
                0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1,
                // Right face
                1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0,
                // Back face
                0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1,
                // Left face
                1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0,
                // Top face
                1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0,
                // Bottom face
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        };

        mCubeVertexCoorBuffer = GLUtil.toFloatBuffer(cubeVertexCoor);
        mLightTextureCoorBuffer = GLUtil.toFloatBuffer(lightTextureCoor);
        mReversedLightTextureCoorBuffer = GLUtil
                .toFloatBuffer(lightTextureCoorReverse);
        mBaseTextureCoorBuffer = GLUtil.toFloatBuffer(baseTextureCoor);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // TODO Auto-generated method stub
        mWidth = width;
        mHeight = height /*- mHostSeatHeight*/;
        float ratio = (float) mWidth / mHeight;
        x = 1.2f;
        y = x / ratio;
        z = 0.06f;
        initVertexts();
        GLES20.glViewport(0, 0, mWidth, mHeight);
        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0;
        final float eyeZ = 6f;

        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = eyeZ / x * ratio;
        final float far = 12.0f;
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near,
                far);

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0;
        final float lookZ = 0f;

        // Set our up vector. This is where our head would be pointing were we
        // holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY,
                lookZ, upX, upY, upZ);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        final String vertexShader = mVertexShader;
        final String fragmentShader = mFragmentShader;
        mProgramHandle = GLUtil.createProgram(vertexShader, fragmentShader);
        if (mProgramHandle == 0) {
            throw new RuntimeException("failed to create program");
        }
         mLightTextureId = GLUtil.initTexture(mContext, R.drawable.gl_glass_light);
         mSideTextureId = GLUtil.initTexture(mContext, R.drawable.gl_glass_side);
         mFrontTextrueId = GLUtil.initTexture(mContext, null);
        mBackTextrueId = GLUtil.initTexture(mContext, null);
         mTabTextureId = GLUtil.initTexture(mContext, R.drawable.gl_glass_topbottom);
         intShaderValues();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mVisible) {
            drawCube();
        }
    }

    private void intShaderValues() {
        GLES20.glUseProgram(mProgramHandle);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle,
                "a_Position");
        mLigthTexCoorHandle = GLES20.glGetAttribLocation(mProgramHandle,
                "aTexCoor");
        mBaseTexCoorHandle = GLES20.glGetAttribLocation(mProgramHandle,
                "aBaseTexCoor");
        mOffsetXHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "u_offsetx");
        mOffsetYHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "u_offsety");
        mBaseTextureHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "u_baseTexture");
        mLightTextureHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "u_lightTexture");
        mLightAlphaHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_light_alpha");
        mBaseAlphaHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "ubase_alpha");
    }

    private void drawCube() {

        if (mNeedUpdateFrontTexture) {
            GLUtil.updateTexture(mFrontTextrueId, mFrontTextureBmp);
            mNeedUpdateFrontTexture = false;
            mFrontTextureBmp = null;
        }

        mCubeVertexCoorBuffer.position(0);
        mLightTextureCoorBuffer.position(0);
        mLightTextureCoorBuffer.position(0);
        mBaseTextureCoorBuffer.position(0);

        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT,
                false, 0, mCubeVertexCoorBuffer);
        GLES20.glVertexAttribPointer(mLigthTexCoorHandle, 2, GLES20.GL_FLOAT,
                false, 0, mLightTextureCoorBuffer);
        GLES20.glVertexAttribPointer(mBaseTexCoorHandle, 2, GLES20.GL_FLOAT,
                false, 0, mBaseTextureCoorBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mLigthTexCoorHandle);
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##weimin.yanwm@alibaba-inc.com
        // ##BugID:() ##date:2015/01/07
        // ##description:use diffirent texture Coord by angle
        if (isFirstDraw) {
            setAngle(mAngle);
            isFirstDraw = false;
        }
        float abs_angle = Math.abs(mAngle);
        float targetAngle = mAngle;
        FloatBuffer targetLightBuffer = mLightTextureCoorBuffer;
        if (abs_angle <= 90) {
            targetAngle = mAngle;
        } else if ((abs_angle > 90 && abs_angle <= 180)
                || (abs_angle > 180 && abs_angle <= 270)) {
            targetAngle = mAngle < 0 ? (180 - abs_angle) : (abs_angle - 180);
            targetLightBuffer = mReversedLightTextureCoorBuffer;
        } else {
            targetAngle = mAngle < 0 ? (360 - abs_angle) : (abs_angle - 360);
        }
        GLES20.glUniform1f(mOffsetXHandle, targetAngle);
        // use diffirent texture coord.
        GLES20.glVertexAttribPointer(mLigthTexCoorHandle, 2, GLES20.GL_FLOAT,
                false, 0, targetLightBuffer);
        GLES20.glEnableVertexAttribArray(mLigthTexCoorHandle);
        /* YUNOS END PB */

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0f, 0f);
        Matrix.rotateM(mModelMatrix, 0, mAngle, 0.0f, 1.0f, 0.0f);
        Matrix.scaleM(mModelMatrix, 0, mScale, mScale, 1);
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        // --------------------- bind multi texture -----------------------

        GLES20.glUniform1f(mLightAlphaHandle, mLightAlpha);
        GLES20.glUniform1f(mBaseAlphaHandle, mBaseAlpha);

        GLES20.glDisableVertexAttribArray(mBaseTexCoorHandle);

        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##weimin.yanwm@alibaba-inc.com
        // ##BugID:() ##date:2015/01/07
        // ##description: use different texture to draw face.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTabTextureId);
        GLES20.glUniform1i(mBaseTextureHandle, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 18, 6);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 6, 6);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mSideTextureId);
        GLES20.glUniform1i(mBaseTextureHandle, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 24, 6);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 30, 6);
        /* YUNOS END PB */
        GLES20.glEnableVertexAttribArray(mBaseTexCoorHandle);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mLightTextureId);
        GLES20.glUniform1i(mLightTextureHandle, 1);

        GLES20.glUniform1f(mBaseAlphaHandle, 1f);
        if (abs_angle < 90 || abs_angle > 270) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrontTextrueId);
            GLES20.glUniform1i(mBaseTextureHandle, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        }

        if (abs_angle > 90 && abs_angle < 270) {
            // if (mNeedUpdateBackTexture) {
            // GLUtil.updateTexture(mBackTextrueId, mBackTextureBmp);
            // mNeedUpdateBackTexture = false;
            // mBackTextureBmp = null;
            // }
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrontTextrueId);
            GLES20.glUniform1i(mBaseTextureHandle, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 12, 6);
        }
    }

    @Override
    public void setAlpha(float alpha) {
        mLightAlpha = alpha;
        mBaseAlpha = alpha;
        mGLView.requestRender();
    }

}
