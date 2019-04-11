package com.christianbernier.fractal.graphics;

public class UnitVector3D extends Vector3D {
	public UnitVector3D() {
		super(0, 0, 1); //DONT CHANGE THESE COORDS
	}
	
	public UnitVector3D(double x, double y, double z) {
		super(x, y, z);
		normalizeSelf();
	}
	
	public UnitVector3D(Vector3D v) { //copy constructor, takes vector or normalizeSelfed vector
		super(v.x, v.y, v.x);
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
	public void crossSelf(Vector3D v) {
		x = y*v.z-z*v.y;
		y = z*v.x-x*v.z;
		z = x*v.y-y*v.x;
		normalizeSelf();
	}
}
