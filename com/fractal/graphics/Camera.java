package com.fractal.graphics;

import com.fractal.compute.Vector3D;
import com.fractal.compute.UnitVector3D;
import java.nio.*;

import org.lwjgl.BufferUtils;

import static org.lwjgl.system.MemoryUtil.*;

public class Camera {
	Vector3D position;
	UnitVector3D direction;
	UnitVector3D angle;
	UnitVector3D relativeXaxis;
	UnitVector3D relativeYaxis;
	
	public Camera() {
		position = new Vector3D();
		direction =  new UnitVector3D();
		angle = new UnitVector3D();
		calculateRelativeX();
		calculateRelativeY();
	}
	
	public Camera(Vector3D v) {
		position = new Vector3D(v);
		direction =  new UnitVector3D();
		angle = new UnitVector3D();
		calculateRelativeX();
		calculateRelativeY();
	}
	
	public Camera(Vector3D p, Vector3D d) {
		position = new Vector3D(p);
		direction =  (UnitVector3D)(new Vector3D(d));
		angle = new UnitVector3D();
		calculateRelativeX();
		calculateRelativeY();
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
	
	public UnitVector3D getRelativeX() {
		return new UnitVector3D(relativeXaxis);
	}
	
	public UnitVector3D getRelativeY() {
		return new UnitVector3D(relativeYaxis);
	}
	
	public double getAngle() {
		return angle.dot(new UnitVector3D())/(angle.getMagnitude()*(new UnitVector3D()).getMagnitude());
	}
	
	public String toString() {
		return "Camera - Position: [" + position + "], Direction: [" + direction + "], Angle: [" + angle + "]";
	}
	
	public void updateDirection(double mouseX, double mouseY, double sensitivity) {
		UnitVector3D temp = new UnitVector3D();
		temp.rotateSelf(mouseX / sensitivity, mouseY / sensitivity, 0);
		direction = temp;
		calculateRelativeX();
		calculateRelativeY();
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
	
	public void calculateRelativeX() {
		if(direction.equals(new UnitVector3D())) {
			relativeXaxis = new UnitVector3D(-1, 0, 0);
		} else if(direction.equals(new UnitVector3D(0, 0, -1))) {
			relativeXaxis = new UnitVector3D(1, 0, 0);
		} else {
			relativeXaxis = (UnitVector3D)(angle.cross(direction).normalize());
		}
	}
	
	public void calculateRelativeY() {
		if(direction.equals(new UnitVector3D())) {
			relativeYaxis = new UnitVector3D(0, -1, 0);
		} else if(direction.equals(new UnitVector3D(0, 0, -1))) {
			relativeYaxis = new UnitVector3D(0, 1, 0);
		} else {
			relativeYaxis = (UnitVector3D)(angle.cross(direction).cross(direction).normalize());
		}
	}
	
	public void moveRelativeZ(double m) { //move along direction vector
		position.addSelf(direction.scale(m));
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
		coords.put((new UnitVector3D(1, 0, 0)).dot(relativeYaxis));
		coords.put((new UnitVector3D(0, 1, 0)).dot(relativeXaxis));
		coords.put((new UnitVector3D(0, 1, 0)).dot(relativeYaxis));
		coords.put((new UnitVector3D(0, 0, 1)).dot(relativeXaxis));
		coords.put((new UnitVector3D(0, 0, 1)).dot(relativeYaxis));
	}
}
