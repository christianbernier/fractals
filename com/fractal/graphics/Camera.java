package com.fractal.graphics;

import com.fractal.compute.Vector3D;
import com.fractal.compute.UnitVector3D;
import java.nio.*;

import org.lwjgl.BufferUtils;

public class Camera {
	Vector3D position;
	double yaw;
	double pitch;
	UnitVector3D relativeZaxis; //Where the camera is pointing
	UnitVector3D relativeXaxis;
	UnitVector3D relativeYaxis;
	
	public Camera() {
		position = new Vector3D();
		setAngle(0, 0);
	}
	
	public Camera(Vector3D v) {
		position = new Vector3D(v);
		setAngle(0, 0);
	}
	
	public Camera(Vector3D v, double p, double y) {
		position = new Vector3D(v);
		setAngle(p, y);
	}
	
	public Camera(double p, double y) {
		position = new Vector3D();
		setAngle(p, y);
	}
	
	public void setLocation(Vector3D v) {
		position = new Vector3D(v);
	}
	
	public void setAngle(double p, double y) {
		pitch = (p < 0 ? p%(Math.PI*2)+2*Math.PI : p%(Math.PI*2));
		yaw = (y < 0 ? y%(Math.PI*2)+2*Math.PI : y%(Math.PI*2));
		constructRelativeAxes();
	}
	
	public Vector3D getLocation() {
		return new Vector3D(position);
	}
	
	public UnitVector3D getDirection() {
		return new UnitVector3D(relativeZaxis);
	}
	
	public double getPitch() {
		return pitch;
	}
	
	public double getYaw() {
		return yaw;
	}
	
	public UnitVector3D getRelativeX() {
		return new UnitVector3D(relativeXaxis);
	}
	
	public UnitVector3D getRelativeY() {
		return new UnitVector3D(relativeYaxis);
	}
	
	public String toString() {
		return "Camera - Position: [" + position + "], Pitch (DEG): " + pitch*180.0/Math.PI + ", Yaw (DEG): " + yaw*180.0/Math.PI;
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
	
	public void constructRelativeAxes() { 
		relativeZaxis = new UnitVector3D(pitch, yaw);
		relativeYaxis = new UnitVector3D(0, 0, 1); //NO LONGER ORTHOGONAL
		relativeXaxis = relativeZaxis.rotate(-Math.PI / 2, 0, 0);
	}
	
	public void moveRelativeZ(double m) { //move along direction vector
		position.addSelf(relativeZaxis.scale(m));
	}
	
	public void moveRelativeY(double m) {
		position.addSelf(relativeYaxis.scale(m));
	}
	
	public void moveRelativeX(double m) {
		position.addSelf(relativeXaxis.scale(m));
	}
	
	public void guiAxes(DoubleBuffer coords) {
		coords = BufferUtils.createDoubleBuffer(6);
		coords.put((new UnitVector3D(1, 0, 0)).dot(relativeXaxis));
		coords.put((new UnitVector3D(1, 0, 0)).dot(relativeYaxis)); //RELATIVE Y AXIS IS NO LONGER ORTHOGONAL
		coords.put((new UnitVector3D(0, 1, 0)).dot(relativeXaxis));
		coords.put((new UnitVector3D(0, 1, 0)).dot(relativeYaxis));
		coords.put((new UnitVector3D(0, 0, 1)).dot(relativeXaxis));
		coords.put((new UnitVector3D(0, 0, 1)).dot(relativeYaxis));
	}
}
