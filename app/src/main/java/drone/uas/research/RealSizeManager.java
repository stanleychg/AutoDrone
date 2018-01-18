package drone.uas.research;

import org.opencv.core.Point;

/**
 * Currently not integrated. Code designed to calculate and return the estimated size of a person
 * given the details of the bounding boxes surrounding people in the images from the DJI drone.
 * @author Jesse G. & Stanley C
 * @since 12/11/16.
 */
public class RealSizeManager {
    private static RealSizeManager instance;

    private static double pitch_x;
    private static double pitch_y;

    private RealSizeManager(){

    }

    public static RealSizeManager getInstance(){
        if(instance == null){
            instance = new RealSizeManager();
        }

        return instance;
    }

//    Variables to pass into fuction
    //    x - # number of pixels along x axis away from center pixel (from image)
    //    y - # number of pixels along y axis away from center pixel (from image)
    //    depth_to_target - drones height above target (from guidance/ultrasonic sensor/barometer)
    //    heading - direction the drone is pointing (east,north,west,south from compass or GPS)
//    Variables that are know before hand
    //    pitch_x - the angle between each pixel on the x axis
    //    pitch_y - the angle between each pixel on the y axis
//    Variables that are being solved for
    //    x_real - real distance along x axis from drone
    //    y_real - real distance along y axis from drone
    //    angle_x - angle from center line under drone to target in x direction
    //    angle_y - angle from center line under drone to target in y direction
    //    theta - angle target is from drone
    //    distance - distance target is from drone
    //    true_angle - true angle of target
    //    east/west - how far east or west the target is from you
    //    north/south - how far north or south the target is from you

//    get pixel x and y (starting from top left as 0,0 most likely)
//    subtract half of resolution width from x
//    subtract half of resolution height from y
//    now we have normalized x and y points from center of image instead of top left


//    get real length of both vectors
//    angle_x = x*pitch_x
//            angle_y = y*pitch_y
//    x_real = tan(angle_x)*depth_to_target
//            y_real = tan(angle_y)*depth_to_target


    /**
     * Calculate x angle given x pixel distance away from the center
     * @param xPixel
     * @return
     */
    public double getAngleX(double xPixel){
        double angle = 0.0602*Math.abs(xPixel) + 0.6903;
        return angle;
    }

    /**
     * Calculate y angle given y pixel distance away from the center
     * @param yPixel
     * @return
     */
    public double getAngleY(double yPixel){
        double angle = 0.1616*Math.abs(yPixel) - 10.013;
        return angle;
    }

    /**
     * Returns actual width and height of target in the form of a Point(actual width, actual height)
     */
    public Point getRealLength(double x, double y, double width, double height, double altitude){
        //Center the points
        Point p1 = new Point(x - width/2,y - height/2);
        Point p2 = new Point(x + width/2, y - height/2);
        Point p3 = new Point(x - width/2, y + height/2);

        //Find angle
        double angleX = getAngleX(p1.x);
        double angleY = getAngleY(p1.y);
        double angleX2 = getAngleX(p2.x);
        double angleY2 = getAngleY(p2.y);
        double angleX3 = getAngleX(p3.x);
        double angleY3 = getAngleY(p3.y);

        //Find length of vector directly underneath drone to target point
        double x1_real = Math.signum(p1.x)*Math.tan(angleX)*altitude;
        double y1_real = Math.signum(p1.y)*Math.tan(angleY)*altitude;
        double x2_real = Math.signum(p2.x)*Math.tan(angleX2)*altitude;
        double y2_real = Math.signum(p2.y)*Math.tan(angleY2)*altitude;
        double x3_real = Math.signum(p3.x)*Math.tan(angleX3)*altitude;
        double y3_real = Math.signum(p3.y)*Math.tan(angleY3)*altitude;

        //Derive real dimensions of target
        double realWidth = Math.sqrt(Math.pow(x2_real - x1_real,2) + Math.pow(y2_real - y1_real,2));
        double realHeight = Math.sqrt(Math.pow(x3_real - x1_real,2) + Math.pow(y3_real - y1_real,2));
        return new Point(realWidth, realHeight);
    }

//    drop pin
//    heading - get heading of drone by GPS
//    set east = 0, north = 90, west = 180, south = 270 degrees
//            theta = arctan(y/x)
//    make sure this is in the correct quadrent
//    x and y pos = quad 1, x-neg y-pos = quad 2, x and y neg = quad 3, x-pos y-neg = quad 4
//    true_angle = theta + heading
//            distance = sqrt(x_ave^2+y_ave^2)
//    east/west = cos(true_angle)*distance
//    north/south = sin(true_angle)*distance
//    Things worth referencing
//    angle of view
//    https://en.wikipedia.org/wiki/Angle_of_view
//    http://www.bavono.com/eng/Lens.htm
//    camera specs
//    http://www.dji.com/zenmuse-xt/info#specs

}
