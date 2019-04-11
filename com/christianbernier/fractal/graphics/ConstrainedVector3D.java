package com.christianbernier.fractal.graphics;

public class ConstrainedVector3D extends Vector3D {
	public ConstrainedVector3D() {
		super(0, 0, 1);
	}
	
	public ConstrainedVector3D(double x, double y, double z) {
		super(x, y, z);
		constrain();
	}
	
	public ConstrainedVector3D(Vector3D v) { //copy constructor, takes vector or constrained vector
		super(v.x, v.y, v.x);
		constrain();
	}
	
	@Override
	public String toString() {
		return "UNIT VECTOR x: " + x + " y: " + y + " z: " + z;
	}
	
	@Override
	public void setX(double v) {
		x = v;
		constrain();
	}

	@Override
	public void setY(double v) {
		y = v;
		constrain();
	}

	@Override
	public void setZ(double v) {
		z = v;
		constrain();
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
		constrain();
	}

	@Override
	public Vector3D add(Vector3D v) {
		return new Vector3D(x+v.x, y+v.y, z+v.z).getUnit();
	}

	@Override
	public void subtractSelf(Vector3D v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;
		constrain();
	}
	
	@Override
	public Vector3D subtract(Vector3D v) {
		return new Vector3D(x-v.x, y-v.y, z-v.z).getUnit();
	}
}
