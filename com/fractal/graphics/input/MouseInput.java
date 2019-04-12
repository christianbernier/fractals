package com.fractal.graphics.input;

import org.lwjgl.glfw.GLFWCursorPosCallback;

public class MouseInput extends GLFWCursorPosCallback{

	@Override
	public void invoke(long window, double xpos, double ypos) {
		System.out.println("Mouse x: " + xpos + " y: " + ypos);
	}

}
