package drone.uas.research.opencvtest;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");

            test2();


        }
    }

    /**
     * Return output file name
     * @return
     */
    private void test1(){
        String inputFileName="test";
        String inputExtension = "png";
        String inputDir = getCacheDir().getAbsolutePath();  // use the cache directory for i/o
        String outputDir = getCacheDir().getAbsolutePath();
        String outputExtension = "png";
        String inputFilePath = inputDir + File.separator + inputFileName + "." + inputExtension;

        File f = new File(outputDir, "test.png");
        try {
            FileOutputStream of = new FileOutputStream(f);
            Drawable d = getResources().getDrawable(R.drawable.test);
            Bitmap bitmap = ((BitmapDrawable)d).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] bitmapdata = stream.toByteArray();
            of.write(bitmapdata);
            stream.close();
            of.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d (this.getClass().getSimpleName(), "loading " + inputFilePath + "...");
        Mat image = Imgcodecs.imread(inputFilePath);
        //Mat image = Imgcodecs.imread(Uri.parse("android.resource://drone.uas.research.opencvtest/" + R.drawable.test).getPath());
        Log.d (this.getClass().getSimpleName(), "width of " + inputFileName + ": " + image.width());
        // if width is 0 then it did not read your image.


        // for the canny edge detection algorithm, play with these to see different results
        int threshold1 = 70;
        int threshold2 = 100;

        Mat im_canny = new Mat();  // you have to initialize output image before giving it to the Canny method
        Imgproc.Canny(image, im_canny, threshold1, threshold2);
        String cannyFilename = outputDir + File.separator + inputFileName + "_canny-" + threshold1 + "-" + threshold2 + "." + outputExtension;
        Log.d (this.getClass().getSimpleName(), "Writing " + cannyFilename);
        Imgcodecs.imwrite(cannyFilename, im_canny);
//        View v = findViewById(R.id.mainLayout);
//        v.setBackground(Drawable.createFromPath(cannyFilename));
    }

    /**
     * Return output file name
     * @return
     */
    private void test2(){

        Bitmap image = null;

        Drawable d = getResources().getDrawable(R.drawable.test2);
        Bitmap bitmap2 = ((BitmapDrawable)d).getBitmap();
        image = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/2, bitmap2.getHeight()/2, false);

        // Perform image processing here
        // define range of colors that ARE NOT yellow in HSV
        Scalar lowerBound = new Scalar(0,0,0); //RGB: 0,0,0
        Scalar upperBound = new Scalar(240,60.8,100); //RGB: 100,100,255

        // Get frame from videocaptue stream
        int width = image.getWidth(), height = image.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        output.eraseColor(Color.TRANSPARENT);

        //http://stackoverflow.com/questions/21113190/how-to-get-the-mat-object-from-the-byte-in-opencv-android
        Mat rawImage = new Mat(width, height, CvType.CV_8UC3); //Processed by primary algorithm
        Mat rawImage2 = new Mat(width,height, CvType.CV_8UC3); //Processed by secondary algorithm
        Mat rawOutput = new Mat(width,height, CvType.CV_8UC4); //Processed by secondary algorithm
        Utils.bitmapToMat(image, rawImage);
        Utils.bitmapToMat(image, rawImage2);
        Utils.bitmapToMat(output, rawOutput);

        //http://stackoverflow.com/questions/24983649/how-to-deal-with-8uc3-and-8uc4-simultaneously-in-android-ndk
        Imgproc.cvtColor(rawImage, rawImage, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(rawImage2, rawImage2, Imgproc.COLOR_BGRA2BGR);

        Mat hsvImage = new Mat(width, height, CvType.CV_8UC3);
        // Convert BGR to HSV
        Imgproc.cvtColor(rawImage,hsvImage,Imgproc.COLOR_BGR2HSV);
        Mat mask = new Mat();//new Mat(width, height, CvType.CV_8UC3);
        // Threshold the HSV image to get only blue colors
        Core.inRange(hsvImage,lowerBound,upperBound,mask);

        Utils.matToBitmap(mask, output);
        ImageView v = (ImageView)findViewById(R.id.mainImage);
        v.setImageBitmap(output);

        // Bitwise-AND mask and original image
        Mat resImage = new Mat();//new Mat(width, height, CvType.CV_8UC3);

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
        Mat hierarchy = new Mat(width, height, CvType.CV_8UC3);
        Imgproc.findContours(mask, contours, hierarchy, 1,2);
        Imgproc.drawContours(mask, contours, -1, new Scalar(new double[]{128,255,0}),3);
//        rawOutput.setTo(new Scalar(0,0,0,0));

        for(MatOfPoint c: contours){
            Rect r = Imgproc.boundingRect(c);
            if(r.width < 80 || r.height < 80)
                continue;

            int x = r.x;
            int y = r.y;
            int rWidth = r.width;
            int rHeight = r.height;

            Log.w(TAG, "Width: " + rWidth + " Height: " + rHeight);

            if(rWidth > 250 || rHeight > 250){
                Imgproc.rectangle(rawOutput,
                        new Point(x,y),
                        new Point(x+rWidth, y+rHeight),
                        new Scalar(new double[]{255,255,255}),
                        2);
                Imgproc.rectangle(mask,
                        new Point(x,y),
                        new Point(x+rWidth, y+rHeight),
                        new Scalar(new double[]{255,255,255}),
                        2);

                Imgproc.putText(mask, "Person match",
                        new Point(x,y+rHeight+10), //Location
                        0,1,new Scalar(255,255,255)); //fontface, fontscale, color

                Imgproc.putText(rawOutput, "Person match",
                        new Point(x,y+rHeight+10), //Location
                        0,1,new Scalar(255,255,255)); //fontface, fontscale, color
            } else {
                boolean flag = false;
                //Check if two mid sized blobs are close
                for(MatOfPoint c2: contours){
                    Rect r2 = Imgproc.boundingRect(c2);
                    if(r2.width < 115 || r2.height < 115)
                        continue;

                    int x2 = r2.x;
                    int y2 = r2.y;
                    int rWidth2 = r2.width;
                    int rHeight2 = r2.height;

                    if(r2.width <115 || r2.height<115) continue;
                    if(r.width <115 || r.height<115) continue;
                    if(Math.sqrt(Math.pow(r2.width-r.width,2) + Math.pow(r2.height - r.height,2)) < 70){
                        Imgproc.rectangle(rawOutput,
                                new Point(x,y),
                                new Point(x+rWidth, y+rHeight),
                                new Scalar(new double[]{255,255,255}),
                                2);
                        Imgproc.rectangle(mask,
                                new Point(x,y),
                                new Point(x+rWidth, y+rHeight),
                                new Scalar(new double[]{255,255,255}),
                                2);

                        Imgproc.putText(mask, "Person match",
                                new Point(x,y+rHeight+10), //Location
                                0,1,new Scalar(255,255,255)); //fontface, fontscale, color

                        Imgproc.putText(rawOutput, "Person match",
                                new Point(x,y+rHeight+10), //Location
                                0,1,new Scalar(255,255,255)); //fontface, fontscale, color
                        flag = true;
                    }
                }

                if(flag == false){
                    Imgproc.rectangle(rawOutput,
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
                            0,1,new Scalar(0,255,0)); //fontface, fontscale, color

                    Imgproc.putText(rawOutput, "Heat match",
                            new Point(x,y+rHeight+10), //Location
                            0,1,new Scalar(0,255,0)); //fontface, fontscale, color

                }
            }
        }
        //Push to bitmap and update

        //Bitmap output2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);


        ImageView background = (ImageView) findViewById(R.id.mainBackground);
        background.setImageBitmap(image);

        Utils.matToBitmap(rawOutput, output);
        //ImageView v = (ImageView)findViewById(R.id.mainImage);
        v.setImageBitmap(output);

    }
}
