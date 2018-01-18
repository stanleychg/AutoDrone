package drone.uas.research.persondetection;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by stanc on 11/1/16.
 */
public class PersonDetection {

    public void drawDetections(){

    }

//    def draw_detections(img, rects, thickness = 1):
//            for x, y, w, h in rects:
//            # the HOG detector returns slightly larger rectangles than the real objects.
//    # so we slightly shrink the rectangles to get a nicer output.
//            pad_w, pad_h = int(0.15*w), int(0.05*h)
//    cv2.rectangle(img, (x+pad_w, y+pad_h), (x+w-pad_w, y+h-pad_h), (255, 200, 0), thickness)
//            cv2.putText(img, 'Shape match', (x, y), 0,0.3,(255,200,0))

    public static void main(){
        /** Converted code */
        // define range of color yellow in HSV
        Scalar lowerBound = new Scalar(new double[]{23,41,133});
        Scalar upperBound = new Scalar(new double[]{30,255,255});

        HOGDescriptor hog = new HOGDescriptor();
        hog.setSVMDetector( HOGDescriptor.getDefaultPeopleDetector());
        // Get frame from videocaptue stream
        int width = -1, height = -1;
        byte[] data = null;

        //http://stackoverflow.com/questions/21113190/how-to-get-the-mat-object-from-the-byte-in-opencv-android

        Mat rawImage = new Mat(width, height, CvType.CV_8UC3); //Processed by primary algorithm
        Mat rawImage2 = new Mat(width,height, CvType.CV_8UC3); //Processed by secondary algorithm
        rawImage.put(0, 0, data);
        rawImage.copyTo(rawImage2);
        Mat hsvImage = new Mat(width, height, CvType.CV_8UC3);
        // Convert BGR to HSV
        Imgproc.cvtColor(rawImage,hsvImage,Imgproc.COLOR_BGR2HSV);
        Mat mask = new Mat(width, height, CvType.CV_8UC3);
        // Threshold the HSV image to get only blue colors
        Core.inRange(hsvImage,lowerBound,upperBound,mask);
        // Bitwise-AND mask and original image
        Mat resImage = new Mat(width, height, CvType.CV_8UC3);

        Core.bitwise_and(rawImage,rawImage, resImage,mask);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3));

        Imgproc.GaussianBlur(mask, mask, new Size(5,5), 0);

        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);

        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);

        Mat maskInv = new Mat(width, height, CvType.CV_8UC3);
        Core.absdiff(mask, new Scalar(255), maskInv);
        Core.bitwise_and(rawImage,rawImage, resImage,mask);

        // Contour detection
        List<MatOfPoint> contours = new LinkedList<>();
        Imgproc.findContours(mask, contours, null, 1,2);
        Imgproc.drawContours(mask, contours, -1, new Scalar(new double[]{128,255,0}),3);

        for(MatOfPoint c: contours){
            Rect r = Imgproc.boundingRect(c);
            if(r.width < 40 || r.height < 40)
                continue;

            int x = r.x;
            int y = r.y;
            int rWidth = r.width;
            int rHeight = r.height;
            Imgproc.rectangle(rawImage,
                    new Point(x,y),
                    new Point(x+rWidth, y+rHeight),
                    new Scalar(new double[]{0,255,0}),
                    1);
            Imgproc.rectangle(mask,
                    new Point(x,y),
                    new Point(x+rWidth, y+rHeight),
                    new Scalar(new double[]{0,255,0}),
                    1);

            Imgproc.putText(mask, "Heat match",
                    new Point(x,y+rHeight+10), //Location
                    0,0.3,new Scalar(0,255,0)); //fontface, fontscale, color

            Imgproc.putText(rawImage, "Heat match",
                    new Point(x,y+rHeight+10), //Location
                    0,0.3,new Scalar(0,255,0)); //fontface, fontscale, color
        }

        // Secondary detection (Face Detection)
        MatOfRect foundLocations = new MatOfRect();
        MatOfDouble foundWeights = new MatOfDouble();
        /** Some params derived from implementation of function.*/
        hog.detectMultiScale(rawImage2, foundLocations,foundWeights,
                0, new Size(8,8), new Size(32,32), 1.05, 2, false);


        // Draw detections
        for(Rect r: foundLocations.toArray()){
            // The HOG detector returns slightly larger rectangles than the real objects.
            // so we slightly shrink the rectangles to get a nicer output.
            int x = r.x;
            int y = r.y;
            int rWidth = r.width;
            int rHeight = r.height;
            int padW = (int)(0.15*rWidth);
            int padH = (int)(0.05*rHeight);
            Imgproc.rectangle(rawImage,
                    new Point(x+padW,y+padH),
                    new Point(x+rWidth-padW,y+rHeight-padH),
                    new Scalar(255,200,0),1);
            Imgproc.putText(rawImage, "Shape match", new Point(x,y), 0, 0.3, new Scalar(255,200,0));
        }

        // Check if overlap in detection
        for(Rect r: foundLocations.toArray()){

            int x = r.x;
            int y = r.y;
            int rWidth = r.width;
            int rHeight = r.height;
            int padW = (int)(0.15*rWidth);
            int padH = (int)(0.05*rHeight);
            int x2,y2,rWidth2,rHeight2;
            for(MatOfPoint c: contours){
                Rect rc = Imgproc.boundingRect(c);
                x2 = rc.x;
                y2 = rc.y;
                rWidth2 = rc.width;
                rHeight2 = rc.height;

                if(((x >= x2 && x2 <=x+rWidth) || (x >= x2+rWidth2 && x2+rWidth2 <=x+rWidth))
                        && ((y >= y2 && y2 <=y+rHeight) || (y >= y2+rHeight2 && y2+rHeight2 <=y+rHeight))){
                    Imgproc.putText(rawImage, "PERSON DETECTED!",
                            new Point(x,y+rHeight+15),
                            0, 0.4, new Scalar(255,255,255));
                    Imgproc.rectangle(rawImage,
                            new Point(x,y),
                            new Point(x+rWidth,y+rHeight),
                            new Scalar(255,255,255),3);

                    //Save to file
                    String fileName = String.format("TS: %s.jpg", new Date().toString());
                    Imgcodecs.imwrite(fileName, rawImage);
                }
            }
        }
    }

}
