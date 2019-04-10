package com.christianbernier.fractal.graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class GUIWindow extends JPanel{
	private static final long serialVersionUID = -4156984126105636689L;
	private JFrame window;
	
	public GUIWindow() {
		DrawGraphics g = new DrawGraphics(800, 600, 1);
		window = new JFrame("Fractal");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(800, 600);
		window.add(g);
		window.setLocationRelativeTo(null);
		window.setVisible(true);
	}
}
