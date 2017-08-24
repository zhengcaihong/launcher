package com.aliyun.homeshell.effects3d;
//package com.aliyun.homeshell.effects3d.Geometry;
//
//import javax.microedition.khronos.opengles.GL10;
//
//import android.graphics.Bitmap;
//import android.opengl.GLES20;
//
//import com.aliyun.homeshell.effects3d.GLUtil;
//
//public class Plane3D extends Plane {
//
//    private float mWidth;
//    private float mHeight;
//    private int mWidthSegments;
//    private int mHeightSegments;
//
//    public Plane3D() {
//        this(1, 1, 0, 1, 1);
//    }
//
//    public Plane3D(float width, float height) {
//        this(width, height, 0, 1, 1);
//    }
//
//    public Plane3D(float width, float height, float z, int widthSegments,
//            int heightSegments) {
//        mWidth = width;
//        mHeight = height;
//        mWidthSegments = widthSegments;
//        mHeightSegments = heightSegments;
//        float[] vertices = new float[(widthSegments + 1) * (heightSegments + 1)
//                * 3 * 2]; // two plane.
//        short[] indices = new short[(widthSegments + 1) * (heightSegments + 1)
//                * 6 * 2];
//
//        float xOffset = width / -2;
//        float yOffset = height / -2;
//        float xWidth = width / (widthSegments);
//        float yHeight = height / (heightSegments);
//        int currentVertex = 0;
//        int currentIndex = 0;
//        short w = (short) (widthSegments + 1);
//
//        // front face.
//        for (int y = 0; y < heightSegments + 1; y++) {
//            for (int x = 0; x < widthSegments + 1; x++) {
//                vertices[currentVertex] = xOffset + x * xWidth;
//                vertices[currentVertex + 1] = yOffset + y * yHeight;
//                vertices[currentVertex + 2] = z > 0 ? z : 0;
//                currentVertex += 3;
//                int n = y * (widthSegments + 1) + x;
//
//                if (y < heightSegments && x < widthSegments) {
//                    // Face one
//                    indices[currentIndex] = (short) n;
//                    indices[currentIndex + 1] = (short) (n + 1);
//                    indices[currentIndex + 2] = (short) (n + w);
//                    // Face two
//                    indices[currentIndex + 3] = (short) (n + 1);
//                    indices[currentIndex + 4] = (short) (n + 1 + w);
//                    indices[currentIndex + 5] = (short) (n + 1 + w - 1);
//                    currentIndex += 6;
//                }
//            }
//        }
//        // back face
//        for (int y = 0; y < heightSegments + 1; y++) {
//            for (int x = 0; x < widthSegments + 1; x++) {
//                vertices[currentVertex] = xOffset + x * xWidth;
//                vertices[currentVertex + 1] = yOffset + y * yHeight;
//                vertices[currentVertex + 2] = z > 0 ? 0 : z;
//                currentVertex += 3;
//                int n = y * (widthSegments + 1) + x;
//
//                if (y < heightSegments && x < widthSegments) {
//                    // Face one
//                    indices[currentIndex] = (short) n;
//                    indices[currentIndex + 1] = (short) (n + 1);
//                    indices[currentIndex + 2] = (short) (n + w);
//                    // Face two
//                    indices[currentIndex + 3] = (short) (n + 1);
//                    indices[currentIndex + 4] = (short) (n + 1 + w);
//                    indices[currentIndex + 5] = (short) (n + 1 + w - 1);
//                    currentIndex += 6;
//                }
//            }
//        }
//
//        setIndices(indices);
//        setVertices(vertices);
//        initTextureCoods();
//    }
//
//    private void initTextureCoods() {
//        float xWidth = 1f / (mWidthSegments);
//        float yHeight = 1f / (mHeightSegments);
//        float xOffset = 0;
//        float yOffset = 1;
//        int currentIndex = 0;
//        float[] textures = new float[(mWidthSegments + 1)
//                * (mHeightSegments + 1) * 2 * 2]; // two plane.
//
//        // front
//        for (int y = 0; y < mHeightSegments + 1; y++) {
//            for (int x = 0; x < mWidthSegments + 1; x++) {
//                textures[currentIndex] = xOffset + x * xWidth;
//                textures[currentIndex + 1] = yOffset - y * yHeight;
//                currentIndex += 2;
//            }
//        }
//        // back
//        for (int y = 0; y < mHeightSegments + 1; y++) {
//            for (int x = 0; x < mWidthSegments + 1; x++) {
//                textures[currentIndex] = xOffset + x * xWidth;
//                textures[currentIndex + 1] = yOffset - y * yHeight;
//                currentIndex += 2;
//            }
//        }
//        setTextureCoord(textures);
//    }
//
//    protected int mBackTextureId;
//
//    public void bindTexture(Bitmap... bitmap) {
//        mTextureId = GLUtil.initTexture(bitmap[0]);
//        mBackTextureId = GLUtil.initTexture(bitmap[1]);
//    }
//    
//    public void draw() {
//        if(mTextureId>=0){
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
//        }
//        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numOfIndices,
//                GL10.GL_UNSIGNED_SHORT, indicesBuffer);
//    }
//}
