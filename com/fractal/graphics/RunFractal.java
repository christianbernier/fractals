package com.fractal.graphics;

public class RunFractal {

	public static void main(String[] args) {
		GUIWindow window = new GUIWindow();
		Camera c = new Camera();
		System.out.println("Running Fractal...");
		System.out.println(c);
		window.run();
	}

}
