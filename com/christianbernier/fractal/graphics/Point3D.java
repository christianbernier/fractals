package com.christianbernier.fractal.graphics;

public class Point3D {
	int x, y, z;

	public Point3D() {
		x = 0;
		y = 0;
		z = 0;
	}
	
	public Point3D(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public String toString() {
		return "x: " + x + " y: " + y + " z: " + z;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getZ() {
		return z;
	}
	
	public void setX(int v) {
		x = v;
	}
	
	public void setY(int v) {
		y = v;
	}
	
	public void setZ(int v) {
		z = v;
	}
}
