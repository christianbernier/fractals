# fractals
A project for AP CS, where we render 3D fractals using distance estimators and ray marching.

## Week 1 Update (4/11)
<ul>
  <li>GUI/rendering somewhat working</li>
  <li>Some GUI features including: changing the color of a single pixel, creating a colored rectangle on the screen</li>
  <li>Keyboard and mouse controls implemented, but need to be connected to the computing classes</li>
  <li>Vector3D, UnitVector3D, and Camera classes done</li>
  <li>Vector classes have a variety of common vector math methods</li>
  <li>Camera class has methods to translate keyboard and mouse input to movement relative to direction vector</li>
</ul>

## Week 2 Update (4/18)
<ul>
  <li>Mouse and Keyboard input implemented, but still have issues</li>
  <li>Basic kernel written to render a sphere using raymarching (still has bugs)</li>
  <li>Compute and GUI handlers consolidated into one file</li>
  <li>Full kernel integration: frames are now rendered using openCL and displayed properly, and input is translated into position/direction changes in the camera class, which is then sent back to the kernel for the next frame</li>
</ul>

## Week 3 Update (4/26)
<ul>
  <li>Some OpenGL implementation drawing simple 2d shapes, but still has bugs and doesn't work</li>
  <li>The kernel now properly shades the rendered images based on the number of iterations required per pixel. Here is a sample image of a torus and a sphere:</li>
  ![Torus and Sphere Sample Image](https://raw.githubusercontent.com/christianbernier/fractals/sample.png)
  <li>Mouse input issue fixed - the screen no longer shakes incessantly</li>
</ul>
