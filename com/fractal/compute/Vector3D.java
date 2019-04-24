package com.fractal.compute;

public class Vector3D{
	protected double x, y, z;

	public Vector3D() {
		x = 0;
		y = 0;
		z = 0;
	}
	
	public Vector3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3D(Vector3D v) { //copy constructor
		x = v.x;
		y = v.y;
		z = v.z;
	}
	
	@Override
	public String toString() {
		return "x: " + x + " y: " + y + " z: " + z + " magnitude: " + getMagnitude();
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getZ() {
		return z;
	}
	
	public double getMagnitude() {
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	public void setX(double v) {
		x = v;
	}
	
	public void setY(double v) {
		y = v;
	}
	
	public void setZ(double v) {
		z = v;
	}
	
	public void normalizeSelf() { //make this vector a unit vector
		if(getMagnitude() == 0) {
			return;
		}
		scaleSelf(1.0/getMagnitude());
	}
	
	public UnitVector3D normalize() { //Returns a new vector, use normalizeSelf() to make this vector a unit vector
		if(getMagnitude() == 0) {
			return new UnitVector3D(); //tentative
		}
		return new UnitVector3D(scale(1/getMagnitude()));
	}
	
	public void scaleSelf(double s) {
		x *= s;
		y *= s;
		z *= s;
	}
	
	public Vector3D scale(double s) {
		return new Vector3D(x*s, y*s, z*s);
	}
	
	public void negateSelf() {
		x = -x;
		y = -y;
		z = -z;
	}
	
	public Vector3D negate() {
		return new Vector3D(-x, -y, -z);
	}
	
	public void addSelf(Vector3D v) {
		x += v.x;
		y += v.y;
		z += v.z;
	}
	
	public Vector3D add(Vector3D v) {
		return new Vector3D(x+v.x, y+v.y, z+v.z);
	}
	
	public void subtractSelf(Vector3D v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;
	}
	
	public Vector3D subtract(Vector3D v) {
		return new Vector3D(x-v.x, y-v.y, z-v.z);
	}
	
	public void rotateSelf(double yaw, double pitch, double roll) { //ANGLES IN RADIANS
		double tempx = x, tempy = y, tempz = z;
		if(yaw != 0) {
			x = tempx * Math.cos(yaw) - tempy * Math.sin(yaw);
			y = tempx * Math.sin(yaw) + tempy * Math.cos(yaw);
			tempx = x; tempy = y; tempz = z;
		}
		
		if(pitch != 0) {
			x = tempx * Math.cos(pitch) + tempz * Math.sin(pitch);
			z = -tempx * Math.sin(pitch) + tempz * Math.cos(pitch);
			tempx = x; tempy = y; tempz = z;
		}
		
		if(roll != 0) {
			y = tempy * Math.cos(roll) - tempz * Math.sin(roll);
			z = tempy * Math.sin(roll) + tempz * Math.cos(roll);
		}
	}
	
	public Vector3D rotate(double yaw, double pitch, double roll) {
		Vector3D temp = new Vector3D(this);
		temp.rotateSelf(yaw, pitch, roll);
		return temp;
	}
	
	/*public void crossSelf(Vector3D v) {
		x = y*v.z-z*v.y;
		y = z*v.x-x*v.z;
		z = x*v.y-y*v.x;
	}*/
	
	public Vector3D cross(Vector3D v) {
		return new Vector3D(y*v.z-z*v.y, z*v.x-x*v.z, x*v.y-y*v.x);
	}
	
	public Vector3D project(Vector3D v) { //projects this onto v
		return v.scale(dot(v)/(v.getMagnitude()*v.getMagnitude()));
	}
	
	public double dot(Vector3D v) {
		return x * v.x + y * v.y + z * v.z;
	}
	
	public boolean equals(Vector3D v) {
		return x == v.x && y == v.y && z == v.z;
	}
}
