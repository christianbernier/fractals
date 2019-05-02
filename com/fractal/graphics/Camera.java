package com.fractal.graphics;

import com.fractal.compute.Vector3D;
import com.fractal.compute.UnitVector3D;
import java.nio.*;

import org.lwjgl.BufferUtils;

public class Camera {
	Vector3D position;
	double yaw;
	double pitch;
	double pitchtemp;
	UnitVector3D relativeZaxis; //Where the camera is pointing
	UnitVector3D relativeXaxis;
	UnitVector3D relativeYaxis;
	
	public Camera() {
		position = new Vector3D();
		pitch = 0;
		yaw = 0;
		constructRelativeAxes();
	}
	
	public Camera(Vector3D v) {
		position = new Vector3D(v);
		pitch = 0;
		yaw = 0;
		constructRelativeAxes();
	}
	
	public Camera(Vector3D v, double p, double y) {
		position = new Vector3D(v);
		pitch = p;
		setYaw(y);
		constructRelativeAxes();
	}
	
	public Camera(double p, double y) {
		position = new Vector3D();
		pitch = p;
		setYaw(y);
		constructRelativeAxes();
	}
	
	public void setLocation(Vector3D v) {
		position = new Vector3D(v);
	}
	
	public void setPitch(double p) {
		pitch = p;
	}
	
	public void setYaw(double y) {
		yaw = (y < 0 ? y%(Math.PI*2)+2*Math.PI : y%(Math.PI*2));
	}
	
	public Vector3D getLocation() {
		return new Vector3D(position);
	}
	
	public double getPitch() {
		return pitch;
	}
	
	public double getYaw() {
		return yaw;
	}
	
	public UnitVector3D getRelativeZ() {
		return new UnitVector3D(relativeZaxis);
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
	 * 						Z+  Y+  Direction vector is Y
	 * 						|  /
	 * 						| /
	 * 						|/
	 *		Camera --->		O------X+	
	 */
	
	public void constructRelativeAxes() {
		relativeYaxis = new UnitVector3D(pitch, yaw);
		relativeZaxis = new UnitVector3D(pitch + Math.PI / 2.0, yaw);
		//if(pitch >= 0.5*Math.PI && pitch < 1.5*Math.PI) {
		//	relativeXaxis = new UnitVector3D(0, yaw + Math.PI / 2.0);
		//} else {
			relativeXaxis = new UnitVector3D(0, yaw - Math.PI / 2.0);
		//}
		
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
		//UnitVector3D orthogonalYaxis = new UnitVector3D(pitch + Math.PI / 2, yaw);
		coords = BufferUtils.createDoubleBuffer(6);
		coords.put((new UnitVector3D(1, 0, 0)).dot(relativeXaxis));
		coords.put((new UnitVector3D(1, 0, 0)).dot(relativeYaxis));
		coords.put((new UnitVector3D(0, 1, 0)).dot(relativeXaxis));
		coords.put((new UnitVector3D(0, 1, 0)).dot(relativeYaxis));
		coords.put((new UnitVector3D(0, 0, 1)).dot(relativeXaxis));
		coords.put((new UnitVector3D(0, 0, 1)).dot(relativeYaxis));
	}
}
