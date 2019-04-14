package com.fractal.graphics;

import java.nio.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;

import com.fractal.graphics.input.KeyboardInput;
import com.fractal.graphics.input.MouseInput;
import com.fractal.graphics.fractalEngine.PixelObject;

public class GUIWindow{
	
	public boolean running = false;
	public long window;
	public static int width = 800;
	public static int height = 600;
	private static final double MOUSECOEFFICIENT = 100;
	private static final double KEYBOARDCOEFFICIENT = 2;
	
	private static final GLFWKeyCallback keyCallback = new KeyboardInput();
	private static final GLFWCursorPosCallback cursorCallback = new MouseInput(width, height);
	private DoubleBuffer mouseBufferX = BufferUtils.createDoubleBuffer(1);
	private DoubleBuffer mouseBufferY = BufferUtils.createDoubleBuffer(1);
	private DoubleBuffer guiAxesCoords = BufferUtils.createDoubleBuffer(6);
	
	private Camera camera = new Camera();
	
	PixelObject pixels;
	
	public void init() {
		running = true;
		
		if(!glfwInit()) {
			System.err.println("GLFW Initialization Failed");
		}
		
		glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
		
		window = glfwCreateWindow(width, height, "Fractal", NULL, NULL);
		
		if(window == NULL) {
			System.err.println("Could not create Window");
		}
		
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetKeyCallback(window, keyCallback);
		glfwSetCursorPosCallback(window, cursorCallback);
		
		@SuppressWarnings("unused")
		GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		
		glfwSetWindowPos(window, 100, 100);
		
		glfwMakeContextCurrent(window);
		
		glfwShowWindow(window);
		
		
		int[] x = new int[] {
			-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5
		};
		
		int[] y = new int[] {
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
		};
		
		float[] r = new float[] {
			0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f
		};
		
		float[] g = new float[] {
			0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f
		};
		
		float[] b = new float[] {
			0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f
		};
		
		
		
		pixels = new PixelObject(1, width, height, x, y, r, g, b);
	}
	
	public void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glClearColor(1.0f, 0.0f, 1.0f, 0f);
		
		pixels.draw();
		
		glfwSwapBuffers(window);
	}
	
	public void handleKeyboard(double timeDelta) {
		glfwPollEvents();
		if(KeyboardInput.isKeyDown(GLFW_KEY_Q)) {
			System.out.println("Roll Camera CCW");
		} else if(KeyboardInput.isKeyDown(GLFW_KEY_E)) {
			System.out.println("Roll Camera CW");
		} else if(KeyboardInput.isKeyDown(GLFW_KEY_W)) {
			//System.out.println("Move Camera Forward");
			camera.moveRelativeZ(KEYBOARDCOEFFICIENT * timeDelta);
		} else if(KeyboardInput.isKeyDown(GLFW_KEY_S)) {
			//System.out.println("Move Camera Backwards");
			camera.moveRelativeZ(-KEYBOARDCOEFFICIENT * timeDelta);
		} else if(KeyboardInput.isKeyDown(GLFW_KEY_A)) {
			//System.out.println("Strafe Camera Left");
			camera.moveRelativeX(-KEYBOARDCOEFFICIENT * timeDelta);
		} else if(KeyboardInput.isKeyDown(GLFW_KEY_D)) {
			//System.out.println("Strafe Camera Right");
			camera.moveRelativeX(KEYBOARDCOEFFICIENT * timeDelta);
		} else if(KeyboardInput.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
			//System.out.println("Move Camera Down");
			camera.moveRelativeY(-KEYBOARDCOEFFICIENT * timeDelta);
		} else if(KeyboardInput.isKeyDown(GLFW_KEY_SPACE)) {
			//System.out.println("Move Camera Up");
			camera.moveRelativeY(KEYBOARDCOEFFICIENT * timeDelta);
		} else if (KeyboardInput.isKeyDown(GLFW_KEY_ESCAPE))
            glfwSetWindowShouldClose(window, true);
	}
	
	public void handleMouse(double timeDelta) {
		glfwGetCursorPos(window, mouseBufferX, mouseBufferY);
		camera.updateDirection(mouseBufferX.get(0), mouseBufferY.get(0), MOUSECOEFFICIENT * timeDelta);
		System.out.println(camera);
	}
	
	public void run() {
		init();
		double previousTime = glfwGetTime();
		double timeDelta = 0;
		
		while(running) {
			handleKeyboard(timeDelta);
			handleMouse(timeDelta);
			render();
			
			timeDelta = glfwGetTime() - previousTime;
			previousTime = glfwGetTime();
			
			if(glfwWindowShouldClose(window)) {
				running = false;
			}
			
			if(timeDelta < 16.66) { //60 FPS CAP
				try {
					Thread.sleep((long)(16.66-timeDelta));
				} catch (InterruptedException e) {
					//lol
				}
			}
		}
	}
}