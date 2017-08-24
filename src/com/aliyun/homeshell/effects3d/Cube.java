package com.aliyun.homeshell.effects3d;


public class Cube extends Mesh {
	public Cube(float width, float height, float depth) {
		width /= 2;
		height /= 2;
		depth /= 2;

		float vertices[] = { -width, -height, -depth, // 0
				width, -height, -depth, // 1
				width, height, -depth, // 2
				-width, height, -depth, // 3
				-width, -height, depth, // 4
				width, -height, depth, // 5
				width, height, depth, // 6
				-width, height, depth, // 7
		};

		short indices[] = { 0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2, 2, 6, 7, 2, 7,
				3, 3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3, 0, 1, 3, 1, 2, };

		setIndices(indices);
		setVertices(vertices);
		initTextureCoods();
	}

	private void initTextureCoods() {
		int currentIndex = 0;
		float[] textures = new float[72];
		for (int y = 0; y < 6; y++) {
			textures[currentIndex] = 0;
			textures[currentIndex + 1] = 0;
			textures[currentIndex + 2] = 0;
			textures[currentIndex + 3] = 1;
			textures[currentIndex + 4] = 1;
			textures[currentIndex + 5] = 1;
			textures[currentIndex + 6] = 0;
			textures[currentIndex + 7] = 0;
			textures[currentIndex + 8] = 1;
			textures[currentIndex + 9] = 1;
			textures[currentIndex + 10] = 1;
			textures[currentIndex + 11] = 0;
			currentIndex += 12;
		}
		setTextureCoord(textures);
	}
}