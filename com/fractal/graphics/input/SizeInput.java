package com.fractal.graphics.input;

import org.lwjgl.glfw.GLFWWindowSizeCallback;

public class SizeInput extends GLFWWindowSizeCallback{
	
	public static int width, height;
	
	public SizeInput(int w, int h) {
		width = w;
		height = h;
	}
	
	@Override
	public void invoke(long window, int w, int h) {
		width = w;
		height = h;
	}
}
