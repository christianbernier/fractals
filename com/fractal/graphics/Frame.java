package com.fractal.graphics;
public class Frame {
	double[][][] pixels;
	int h, w;
	
	public static final int COLOR_RGB = 0;
	public static final int COLOR_RGBA = 1;
	
	public Frame(int h, int w, int colorMode) {
		this.h = h;
		this.w = w;
		
		if(colorMode == COLOR_RGB) {
			pixels = new double[w][h][3];
			//r g b
		} else {
			pixels = new double[w][h][4];
			//r g b a
		}
	}
	
	public Frame(double[][][] pixels) {
		this.pixels = pixels;
		w = pixels.length;
		h = pixels[0].length;
	}
	
	public void fillRandom() {
		for(double[][] r : pixels) {
			for(double[] c : r) {
				for(@SuppressWarnings("unused") double v : c) {
					v = Math.random();
				}
			}
		}
	}
	
	public double[] getPixel(int x, int y) {
		return pixels[x][y];
	}
	
	public float[] getPixelFloat(int x, int y) {
		System.out.println(x);
		float[] vals = new float[pixels[x][y].length];
		for(int i = 0; i < vals.length; i++) {
			vals[i] = Float.parseFloat(((Double) pixels[x][y][i]).toString());
		}
		return vals;
	}
}
