package com.aliyun.homeshell.effects3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;

public class Mesh {
    // Our vertex buffer.
    private FloatBuffer verticesBuffer = null;

    // Our index buffer.
    private ShortBuffer indicesBuffer = null;

    private FloatBuffer mTexureCoordBuffer = null;

    private List<TextureBinder> textures = new ArrayList<TextureBinder>();
    // The number of indices.
    private int numOfIndices = -1;

    public void draw() {
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numOfIndices,
                GL10.GL_UNSIGNED_SHORT, indicesBuffer);
    }

    public FloatBuffer getVertexBuffer() {
        return verticesBuffer;
    }

    public FloatBuffer getTexureCoordBuffer() {
        return mTexureCoordBuffer;
    }

    protected void setVertices(float[] vertices) {
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        verticesBuffer = vbb.asFloatBuffer();
        verticesBuffer.put(vertices);
        verticesBuffer.position(0);
    }

    protected void setIndices(short[] indices) {
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indicesBuffer = ibb.asShortBuffer();
        indicesBuffer.put(indices);
        indicesBuffer.position(0);
        numOfIndices = indices.length;
    }

    protected void setTextureCoord(float[] texturesCoods) {
        ByteBuffer ibb = ByteBuffer.allocateDirect(texturesCoods.length * 4);
        ibb.order(ByteOrder.nativeOrder());
        mTexureCoordBuffer = ibb.asFloatBuffer();
        mTexureCoordBuffer.put(texturesCoods);
        mTexureCoordBuffer.position(0);
    }

    public void bindTexture(TextureBinder binder) {
        textures.add(binder);
    }

    public static class TextureBinder {
        public int shaderid;
        public int textureId;
    }

    protected boolean mTextureCoordReverse = false;

    public void reverseTextureCoord(boolean b) {
        mTextureCoordReverse = b;
        onTextureCoordChanged();
    }

    protected void onTextureCoordChanged() {

    }
}