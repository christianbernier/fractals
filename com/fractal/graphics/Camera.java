package com.fractal.graphics;

public class Camera {
	Vector3D position;
	UnitVector3D direction;
	UnitVector3D angle;
	
	public Camera() {
		position = new Vector3D();
		direction =  new UnitVector3D();
		angle = new UnitVector3D();
	}
	
	public Camera(Vector3D v) {
		position = new Vector3D(v);
		direction =  new UnitVector3D();
		angle = new UnitVector3D();
	}
	
	public Camera(Vector3D p, Vector3D d) {
		position = new Vector3D(p);
		direction =  (UnitVector3D)(new Vector3D(d));
		angle = new UnitVector3D();
	}
	
	public void setLocation(Vector3D v) {
		position = new Vector3D(v);
	}
	
	public void setDirection(Vector3D v) {
		direction =  (UnitVector3D)(new Vector3D(v));
	}
	
	/*public void setAngle(double a) {
		angle = new UnitVector3D(); //MULTIPLY BY TRANSFORMATION MATRIX TO ROTATE AROUND DIRECTION
	}*/								//NOT FINISHED
	
	public Vector3D getLocation() {
		return new Vector3D(position);
	}
	
	public UnitVector3D getDirection() {
		return new UnitVector3D(direction);
	}
	
	public UnitVector3D getAngleVector() {
		return new UnitVector3D(angle);
	}
	
	public double getAngle() {
		return angle.dot(new UnitVector3D())/(angle.getMagnitude()*(new UnitVector3D()).getMagnitude());
	}
	
	public String toString() {
		return "Camera - Position: [" + position + "], Direction: [" + direction + "]";
	}
	
	/* [Q][W][E]          	    [Roll CCW / increment angle]    [Forward / increment relative Z]       [Roll CW / decrement angle]
	 * [A][S][D]			[Strafe left / decrement relative X][Backward / decrement relative Z][Strafe right / increment relative X]
	 * 
	 * [Shift]				[Move down / decrement relative Y]
	 * 			[Space]											[Move up / increment relative Y]
	 * SHIFT AND SPACE ARE COUNTERINTUITIVE BUT THATS HOW IT IS IN FPS GAMES
	 * 							 
	 * 						Y+  Z+  Direction vector is Z
	 * 						|  /
	 * 						| /
	 * 						|/
	 *		Camera --->		O------X+	
	 */
	
	public void moveRelativeZ(double m) { //move along direction vector
		position.addSelf(direction.scale(m));
	}
	
	public void moveRelativeY(double m) {
		if(direction.equals(new UnitVector3D())) {
			position.addSelf(new UnitVector3D(0, -m, 0));
		} else if(direction.equals(new UnitVector3D(0, 0, -1))) {
			position.addSelf(new UnitVector3D(0, m, 0));
		} else {
			position.addSelf(angle.cross(direction).cross(direction).scale(m));
		}
	}
	
	public void moveRelativeX(double m) {
		if(direction.equals(new UnitVector3D())) {
			position.addSelf(new UnitVector3D(-m, 0, 0));
		} else if(direction.equals(new UnitVector3D(0, 0, -1))) {
			position.addSelf(new UnitVector3D(m, 0, 0));
		} else {
			position.addSelf(angle.cross(direction).scale(m));
		}
	}
}
