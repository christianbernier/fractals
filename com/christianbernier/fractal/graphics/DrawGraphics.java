package com.christianbernier.fractal.graphics;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

public class DrawGraphics extends JPanel{
	private static final long serialVersionUID = -9175286406838649119L;
	
	int sizeX, sizeY;
	int scale;
	List<List<Double>> pixels;
	boolean generated = false;
	
	public DrawGraphics(int sizeX, int sizeY, int scale) {
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.scale = scale;
	}

	public void paintComponent(Graphics g1) {
		Graphics2D g = (Graphics2D) g1;
		if(!generated) {
			generatePixels();
		}
		
		
		for(int x = 0; x < sizeX; x+=scale) {
			for(int y = 0; y < sizeY; y+=scale) {
				float num = Float.parseFloat(pixels.get(x).get(y).toString());
				Color c = new Color(num, num, num);
				g.setColor(c);
				g.fillRect(x * scale, y * scale, scale * scale, scale * scale);
			}
		}
	}
	
	public void generatePixels(){
		pixels = new ArrayList<List<Double>>(sizeX);
		for(int i = 0; i < sizeX; i++) {
			pixels.add(new ArrayList<Double>(sizeY));
		}
		
		for(int i = 0; i < sizeX; i++) {
			for(int j = 0; j < sizeY; j++) {
				pixels.get(i).add(Math.random());
			}
		}
		generated = true;
	}
}
