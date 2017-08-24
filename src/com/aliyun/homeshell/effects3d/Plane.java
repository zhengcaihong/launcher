package com.aliyun.homeshell.effects3d;


public class Plane extends Mesh {

//    private float mWidth;
//    private float mHeight;
    private int mWidthSegments;
    private int mHeightSegments;

    public Plane() {
        this(1, 1, 0, 1, 1);
    }

    public Plane(float width, float height) {
        this(width, height, 0, 1, 1);
    }

    // ....4...x
    // .. .
    // . . .
    // . . .
    // . 2 3
    // . . .
    // . . .
    // x...1...x

    public Plane(float width, float height, float depth, int widthSegments,
            int heightSegments) {
//        mWidth = width;
//        mHeight = height;
        mWidthSegments = widthSegments;
        mHeightSegments = heightSegments;
        float[] vertices = new float[(widthSegments + 1) * (heightSegments + 1)
                * 3];
        short[] indices = new short[(widthSegments + 1) * (heightSegments + 1)
                * 6];

        float xOffset = width / -2;
        float yOffset = height / -2;
        float xWidth = width / (widthSegments);
        float yHeight = height / (heightSegments);
        int currentVertex = 0;
        int currentIndex = 0;
        short w = (short) (widthSegments + 1);
        for (int y = 0; y < heightSegments + 1; y++) {
            for (int x = 0; x < widthSegments + 1; x++) {
                vertices[currentVertex] = xOffset + x * xWidth;
                vertices[currentVertex + 1] = yOffset + y * yHeight;
                vertices[currentVertex + 2] = depth;
                currentVertex += 3;

                int n = y * (widthSegments + 1) + x;

                if (y < heightSegments && x < widthSegments) {
                    // Face one
                    indices[currentIndex] = (short) n;
                    indices[currentIndex + 1] = (short) (n + 1);
                    indices[currentIndex + 2] = (short) (n + w);
                    // Face two
                    indices[currentIndex + 3] = (short) (n + 1);
                    indices[currentIndex + 4] = (short) (n + 1 + w);
                    indices[currentIndex + 5] = (short) (n + 1 + w - 1);
                    currentIndex += 6;
                }
            }
        }
        setIndices(indices);
        setVertices(vertices);
        reverseTextureCoord(false);
    }

    protected void onTextureCoordChanged() {
        float xWidth = 1f / (mWidthSegments);
        float yHeight = 1f / (mHeightSegments);
        float xOffset = mTextureCoordReverse ? 1 : 0;
        float yOffset = 1;
        int currentIndex = 0;
        float[] textures = new float[(mWidthSegments + 1)
                * (mHeightSegments + 1) * 2];
        for (int y = 0; y < mHeightSegments + 1; y++) {
            for (int x = 0; x < mWidthSegments + 1; x++) {
                textures[currentIndex] = xOffset
                        + (mTextureCoordReverse ? (-x * xWidth) : (x * xWidth));
                textures[currentIndex + 1] = yOffset - y * yHeight;
                currentIndex += 2;
            }
        }
        setTextureCoord(textures);
    }
}