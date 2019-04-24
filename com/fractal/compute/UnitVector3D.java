package com.fractal.compute;

public class UnitVector3D extends Vector3D {
	
	public UnitVector3D() {
		super(1, 0, 0);
	}
	
	public UnitVector3D(double x, double y, double z) {
		super(x, y, z);
		normalizeSelf();
	}
	
	public UnitVector3D(double pitch, double yaw) { //Construct using Euler angles
		super(Math.cos(yaw) * Math.cos(pitch), -Math.sin(yaw), -Math.cos(yaw) * Math.sin(pitch));
		//System.out.println(Math.cos(yaw) * Math.cos(pitch) + " " + -Math.sin(yaw) + " " + -Math.cos(yaw) * Math.sin(pitch));
	}
	
	public UnitVector3D(Vector3D v) { //copy constructor, takes vector or unit vector
		super(v.x, v.y, v.z);
		normalizeSelf();
	}
	
	@Override
	public String toString() {
		return "UNIT VECTOR x: " + x + " y: " + y + " z: " + z;
	}
	
	@Override
	public void setX(double v) {
		x = v;
		normalizeSelf();
	}

	@Override
	public void setY(double v) {
		y = v;
		normalizeSelf();
	}

	@Override
	public void setZ(double v) {
		z = v;
		normalizeSelf();
	}
	
	@Override
	public double getMagnitude() {
		return 1;
	}

	@Override
	public void scaleSelf(double s) {
		return;
	}

	@Override	
	public void addSelf(Vector3D v) {
		x += v.x;
		y += v.y;
		z += v.z;
		normalizeSelf();
	}

	@Override
	public void subtractSelf(Vector3D v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;
		normalizeSelf();
	}
	
	@Override
	public UnitVector3D rotate(double yaw, double pitch, double roll) {
		UnitVector3D temp = new UnitVector3D(this);
		temp.rotateSelf(yaw, pitch, roll);
		return temp;
	}
	
	/*@Override
	public void crossSelf(Vector3D v) {
		x = y*v.z-z*v.y;
		y = z*v.x-x*v.z;
		z = x*v.y-y*v.x;
		normalizeSelf();
	}*/
}
