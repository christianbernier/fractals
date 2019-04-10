package com.christianbernier.fractal.graphics;

public class Camera {
	int x, y, z;
	double a;
	
	public Camera() {
		x = 0;
		y = 0;
		z = 0;
		a = 0;
	}
	
	public Camera(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Camera(int x, int y, int z, double a) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.a = a;
	}
	
	public Point3D getLocation() {
		return new Point3D(x, y, z);
	}
	
	public String toString() {
		return "Camera: [" + getLocation() + "]";
	}
	
	public void moveCameraX(int v) {
		x += v;
	}
	
	public void moveCameraY(int v) {
		y += v;
	}
	
	public void moveCameraZ(int v) {
		z += v;
	}
	
}
