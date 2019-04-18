package com.fractal.graphics.input;

import org.lwjgl.glfw.GLFWCursorPosCallback;

public class MouseInput extends GLFWCursorPosCallback{

	int w, h;
	public static double x, y;
	//double oldxpos, oldypos;
	
	public MouseInput(int width, int height) {
		w = width;
		h = height;
	}
	
	@Override
	public void invoke(long window, double xpos, double ypos) {
		x = xpos;// - oldxpos;
		y = ypos;// - oldypos;
		//oldxpos = xpos;
		//oldypos = ypos;
		//System.out.println("Mouse [x: " + x + " y: " + y + "]");
	}
}
