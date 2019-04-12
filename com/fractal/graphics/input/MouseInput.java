package com.fractal.graphics.input;

import org.lwjgl.glfw.GLFWCursorPosCallback;

public class MouseInput extends GLFWCursorPosCallback{

	int w, h;
	
	public MouseInput(int w, int h) {
		this.w = w;
		this.h = h;
	}
	
	@Override
	public void invoke(long window, double xpos, double ypos) {
		double x = xpos - (w / 2);
		double y = ypos - (h / 2);
		System.out.println("Mouse [x: " + x + " y: " + y + "]");
	}

}
