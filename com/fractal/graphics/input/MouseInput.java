package com.fractal.graphics.input;

import org.lwjgl.glfw.GLFWCursorPosCallback;

public class MouseInput extends GLFWCursorPosCallback{

	double prevx, prevy;
	double x, y;
	
	
	public MouseInput() {
		prevx = 0;
		prevy = 0;
	}
	
	@Override
	public void invoke(long window, double xpos, double ypos) {
		x = xpos - prevx;
		y = ypos - prevy;
		prevx = xpos;
		prevy = ypos;
		xpos = x;
		ypos = y;
		//System.out.println("Mouse [x: " + x + " y: " + y + "]");
	}

}
