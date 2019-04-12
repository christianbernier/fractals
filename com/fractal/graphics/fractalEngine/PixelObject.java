package com.fractal.graphics.fractalEngine;

import com.fractal.graphics.graphicsEngine.VertexArrayObject;

import static org.lwjgl.opengl.GL11.*;

public class PixelObject {
	
	public int vaoID;
	public int count;
	public float SIZE = 1.0f;
	
	private int height, width;
	
	private int[] xValues, yValues;
	private float[] rValues, gValues, bValues;
	
	public float[] verticies = new float[] {
			-0.25f, 0.25f, 0f,
			-0.25f, -0.25f, 0f,
			0.25f, -0.25f, 0f,
			0.25f, 0.25f, 0f
	};
	
	public byte[] indicies = new byte[] {
			0, 1, 2,
			2, 3, 0
	};
	
	@SuppressWarnings("unused")
	private VertexArrayObject vao;
	
	public PixelObject(int vaoID, int width, int height, int[] x, int[] y, float[] r, float[] g, float[] b) {
		this.vaoID = vaoID;
		this.count = indicies.length;
		vao = new VertexArrayObject(this.verticies, this.indicies);
		xValues = x;
		yValues = y;
		rValues = r;
		gValues = g;
		bValues = b;
		
		this.height = height;
		this.width = width;
	}
	
	public void draw() {
		
		setPixels(xValues, yValues, rValues, gValues, bValues);
		
	}
	
	public void setPixels(int[] x, int[] y, float[] r, float[] g, float[] b) {
		int size = x.length;
		
		for(int i = 0; i < size; i++) {
			glColor3f(r[i], g[i], b[i]);              
			
			float c = (height * width);
			float xP = height / c;
			float yP = width / c;

			glPushMatrix();
				glTranslatef(x[i] * xP, y[i] * yP, 0f);
			    glBegin(GL_QUADS);                  // Start Drawing Quads
			        glVertex3f(0f, 0f, 0.0f);          // Left And Up 1 Unit (Top Left)
			        glVertex3f(0f, -yP, 0.0f);          // Right And Up 1 Unit (Top Right)
			        glVertex3f(xP,-yP, 0.0f);          // Right And Down One Unit (Bottom Right)
			        glVertex3f(xP,0f, 0.0f);          // Left And Down One Unit (Bottom Left)
			    glEnd();
			glPopMatrix();
		}
	}
	
}
