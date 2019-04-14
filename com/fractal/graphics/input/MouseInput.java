package com.fractal.graphics.input;

import org.lwjgl.glfw.GLFWCursorPosCallback;

public class MouseInput extends GLFWCursorPosCallback{

	double x, y;
	int w, h;
	
	public MouseInput(int width, int height) {
		w = width;
		h = height;
	}
	
	@Override
	public void invoke(long window, double xpos, double ypos) {
		x = xpos - (w/2);
		y = ypos - (h/2);
		//System.out.println("Mouse [x: " + x + " y: " + y + "]");
	}

}
