package josx.robotics;

import josx.platform.rcx.*;
import josx.util.*;

// !! Should all methods call stop() first in case it was roaming?
// OR methods account for RCX currently in moving mode
// !! All methods that change x, y must be synchronized.
/**
 * The TimingNavigator class performs navigation by timing all movements of the robot.
 *
 * Note: This class will only work for robots using two motors to steer differentially
 * that can rotate within its footprint (i.e. turn on one spot).
 * 
 * @author <a href="mailto:bbagnall@escape.ca">Brian Bagnall</a>
 * @version 0.1  23-June-2001
 */
public class TimingNavigator implements Navigator {

   // Time it takes to rotate 1 degrees (in milliseconds):
   private float ROTATION; // Calculated in constructor

   // Time it takes to go one centimeter (in milliseconds)
   private float CENTIMETER; // Calculated in constructor

   // orientation and co-ordinate data
   private float angle;
   private float x;
   private float y;

   // oldTime is used for timing purposes in stop():
   private int oldTime;

   // Motors for differential steering:
   private Motor left;
   private Motor right;
   
   /**
   * Allocates a TimingNavigator object and initializes if with the left and right wheels.
   * The x and y values will each equal 0 (cm's) on initialization, and the starting
   * angle is 0 degrees, so if the first move is forward() the robot will run along
   * the x axis.
   * <BR> Note: If you find your robot is going backwards when you tell it to go forwards, try
   * rotating the wires to the motor ports by 90 degrees.
   * @param right The motor used to drive the right wheel e.g. Motor.C.
   * @param left The motor used to drive the left wheel e.g. Motor.A.
   * @param timeOneMeter The number of seconds it takes your robot to drive one meter (e.g. 2.0 seconds is normal for a fast robot, 5 seconds for a slower robot).
   * @param timeRotate The number of seconds it takes your robot to rotate 360 degrees. (e.g. 0.646 is normal for a small axle length, 2.2 for a larger axle length)
   */
   
   public TimingNavigator(Motor right, Motor left, float timeOneMeter, float timeRotate) {
      this.right = right;
      this.left = left;

      // Convert seconds into milliseconds per degree
      // and milliseconds per centimeter
      ROTATION = (timeRotate/360)*1000;
      CENTIMETER = (timeOneMeter/100)*1000;

      // Set coordinates and starting angle:
      angle = 0.0f;
      x = 0.0f;
      y = 0.0f;
   }
   
   /**
   * Returns the current x coordinate of the RCX.
   * Note: At present it will only give an updated reading when the RCX is stopped.
   * @return float Present x coordinate.
   */
   public float getX() {
      // !! In future, if RCX is on the move it should return the present calculation of x
      return x;
   }
   
   /**
   * Returns the current y coordinate of the RCX.
   * Note: At present it will only give an updated reading when the RCX is stopped.
   * @return float Present y coordinate.
   */
   public float getY() {
      return y;
   }

   /**
   * Returns the current angle the RCX robot is facing.
   * Note: At present it will only give an updated reading when the RCX is stopped.
   * @return float Angle in degrees.
   */
   public float getAngle() {
      return angle;
   }

   /**
   * Rotates the RCX robot a specific number of degrees in a direction (+ or -).This
   * method will return once the rotation is complete.
   *
   * @param angle Angle to rotate in degrees. A positive value rotates left, a negative value right.
   */
   public void rotate(float angle) {
      // keep track of angle
      
      this.angle = this.angle + angle;
      this.angle = (int)this.angle % 360; // Must be < 360 degrees
      // Is it possible to do this with modulo (%) ???
      if(this.angle < 0)
         this.angle += 360;  // Must be > 0

      oldTime = (int)System.currentTimeMillis();

      if (angle > 0) {
         right.forward();
         left.backward();
      }
      else if (angle < 0) {
         left.forward();
         right.backward();
         angle = angle * -1;
      }

      int delay = ((int)(ROTATION * Math.abs(angle)));
      pause(delay);
      right.stop();
      left.stop();
   }

   /**
   * Rotates the RCX robot to point in a certain direction. It will take the shortest
   * path necessary to point to the desired angle. Method returns once rotation is complete.
   *
   * @param angle The angle to rotate to, in degrees.
   */
   public void gotoAngle(float angle) {
      // in future, use modulo instead of while loop???
      float difference = angle - this.angle;
      while(difference > 180)
         difference = difference - 360; // shortest path to goal angle
      while(difference < -180)
         difference = difference + 360; // shortest path to goal angle
      rotate(difference);
   }

   /**
   * Rotates the RCX robot towards the target point and moves the required distance.
   *
   * @param x The x coordinate to move to.
   * @param y The y coordinate to move to.
   */
   public void gotoPoint(float x, float y) {

      // Determine relative points
      float x1 = x - this.x;
      float y1 = y - this.y;

      // Calculate angle to go to:
      float angle = (float)Math.atan2(y1,x1);

      // Calculate distance to travel:
      float distance;
      if(y1 != 0)
         distance = y1/(float)Math.sin(angle);
      else
         distance = x1/(float)Math.cos(angle);

      // Convert angle from rads to degrees:
      angle = (float)Math.toDegrees(angle);
      
      // Now convert theory into action:
      gotoAngle(angle);
      travel(Math.round(distance));
   }

   /**
   * Moves the RCX robot a specific distance. A positive value moves it forwards and
   * a negative value moves it backwards. Method returns when movement is done.
   *
   * @param centimeters The positive or negative distance to move the robot (in centimeters).
   */
   public void travel(int distance) {
      
      if(distance > 0) {
         forward();
      } else {
         backward();
      }
      int delay = (int)(CENTIMETER * Math.abs(distance));
      
      pause(delay);
      stop();
   }
   
   /**
   * Just a simple delay helper method to avoid try-catch coding.
   * (Threads cannot be Interrupted in Lejos, therefore no need to catch.)
   *
   * @param delay The milliseconds to pause before returning.
   */
   public static void pause(long delay) {
      try {Thread.sleep(delay);} catch (InterruptedException ie) {}
   }

   /**
   * Moves the RCX robot forward until stop() is called.
   *
   * @see Navigator.stop().
   */
   public void forward() {
      // Start timer
      oldTime = (int)System.currentTimeMillis();

      right.forward();
      left.forward();
   }

   /**
   * Moves the RCX robot backward until stop() is called.
   *
   * @see Navigator.stop().
   */
   public void backward() {
      // Start timer
      oldTime = (int)System.currentTimeMillis();

      right.backward();
      left.backward();
   }

   /**
   * Halts the RCX robot and calculates new x, y coordinates.
   *
   * @see Navigator.forward().
   */
   public void stop() {
      left.stop();
      right.stop();
      // Recalculate x-y coordinates based on Timer results
      // Note: Changes oldTime to 0, then if someone
      // calls stop() twice it won't screw up x-y coordinates
      if (oldTime != 0) {
         int totalTime = (int)System.currentTimeMillis() - oldTime;
         float centimeters = totalTime / CENTIMETER;
         // update x, y coordinates
         x = x + (float)(Math.cos(Math.toRadians(angle)) * centimeters);
         y = y + (float)(Math.sin(Math.toRadians(angle)) * centimeters);
         oldTime = 0;
      }
   }
}   