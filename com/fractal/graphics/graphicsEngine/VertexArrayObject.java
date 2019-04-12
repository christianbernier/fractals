package com.fractal.graphics.graphicsEngine;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.opengl.GL;

import static com.fractal.graphics.utils.Utilities.*;

public class VertexArrayObject {
	
	public static final int VERTEX_ARRIB = 0;
	public static final int TCOORD_ARRIB = 1;
	
	public VertexArrayObject(float[] verticies, byte[] indicies) {
		GL.createCapabilities();
		createArrayObject(verticies, indicies);
	}
	
	public void createArrayObject(float[] verticies, byte[] indicies) {
		int vao = glGenVertexArrays();
		glBindVertexArray(vao);
		
		createVerticiesBuffer(verticies);
		createIndiciesBuffer(indicies);
		
		glBindVertexArray(0);
	}
	
	private void createVerticiesBuffer(float[] verticies) {
		int vboId = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		glBufferData(GL_ARRAY_BUFFER, createFloatBuffer(verticies), GL_STATIC_DRAW);
		glVertexAttribPointer(VERTEX_ARRIB, 3, GL_FLOAT, false, 0, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}
	
	private void createIndiciesBuffer(byte[] indicies) {
		int ibo = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, createByteBuffer(indicies), GL_STATIC_DRAW);
	}
	
}
