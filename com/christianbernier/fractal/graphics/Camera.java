package com.christianbernier.fractal.graphics;

public class Camera {
	Vector3D position;
	ConstrainedVector3D direction;
	
	public Camera() {
		position = new Vector3D();
		direction =  new ConstrainedVector3D();
	}
	
	public Camera(Vector3D v) {
		position = new Vector3D(v);
		direction =  new ConstrainedVector3D();
	}
	
	public Camera(Vector3D p, Vector3D d) {
		position = new Vector3D(p);
		direction =  (ConstrainedVector3D)(new Vector3D(d));
	}
	
	public void setLocation(Vector3D v) {
		position = new Vector3D(v);
	}
	
	public void setDirection(Vector3D v) {
		direction =  (ConstrainedVector3D)(new Vector3D(v));
	}
	
	public Vector3D getLocation() {
		return new Vector3D(position);
	}
	
	public ConstrainedVector3D getDirection() {
		return new ConstrainedVector3D(position);
	}
	
	public String toString() {
		return "Camera - Position: [" + position + "], Direction: [" + direction + "]";
	}
	
	/*    [W]            										[Forward / increment relative Z]
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
		ConstrainedVector3D d = new ConstrainedVector3D(direction.getX(), -direction.getZ(), direction.getY());
		position.addSelf(d.scale(m));
	}
	
	public void moveRelativeX(double m) {
		ConstrainedVector3D d = new ConstrainedVector3D(-direction.getZ(), direction.getY(), -direction.getX());
		position.addSelf(d.scale(m));
	}
}
