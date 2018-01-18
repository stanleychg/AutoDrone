package drone.uas.research;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dji.sdk.Camera.DJICamera;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.base.DJIBaseProduct;

/**
 * Fragment. Displays live video feed of the drone and the people detected.
 * @author Stanley C
 * @since 10/31/16.
 */
public class VideoFragment extends Fragment implements TextureView.SurfaceTextureListener{
    private static final String TAG = "VideoFragment";

    //Singleton class
    private static VideoFragment mInstance = null;

    public interface VideoListener{
        void onViewInit();
    }
    private VideoListener mListener;

    /**
     * Default Constructor. DO NOT USE. <br/>
     * Should only be called by getInstance!
     */
    public VideoFragment(){}

    /**
     * Returns instance of VideoFragment. Creates one if an instance does not exist.
     * @return VideoFragment The single instance.
     */
    public static VideoFragment getInstance(VideoListener listener){
        if(mInstance == null){
            mInstance = new VideoFragment();
        }
        mInstance.mListener = listener;
        return mInstance;
    }

    //UI
    protected TextureView mVideoSurface = null;
    private ImageView mOutputView = null;

    //DJI
    private DJICamera mCamera;
    protected DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;
    protected DJICodecManager mCodecManager = null; //Decodes the raw H264 video format

    //Video Processing
    private Executor mThread = Executors.newSingleThreadExecutor();
    private final static int NUM_FRAME_SKIP = 30;
    private int mCounter = 0;

    /*
     * Fragment startup configuration.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReceivedVideoDataCallBack = new DJICamera.CameraReceivedVideoDataCallback() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                Log.d(TAG, "onResult: Data Received");
                if(mCodecManager != null){
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
    }

    /*
     * Initialize fragment layout UI
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_video, container, false);
        mVideoSurface = (TextureView) v.findViewById(R.id.videoTextureView);
        mOutputView = (ImageView) v.findViewById(R.id.videoImageView);
        return v;
    }

    /**
     * Prepares live video stream on fragment layout
     */
    public void initPreviewer(DJIBaseProduct mProduct) {
        Log.d(TAG, "initPreviewer: ");
        setResultToToast(TAG + ": initPreviewer start");

        try {
            mProduct = MainActivity.getProductInstance();
        }catch (Exception exception) {
            mProduct = null;
        }

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        if (null == mProduct || !mProduct.isConnected()) {
            if(mProduct == null)
                setResultToToast(TAG + ": Error: mProduct null");
            else
                setResultToToast(TAG + ": Error: mProduct not connected");

            mCamera = null;
        } else {
            if (!mProduct.getModel().equals(DJIBaseProduct.Model.UnknownAircraft)) {
                setResultToToast(TAG + ": Get Camera");
                mCamera = mProduct.getCamera();
                if (mCamera != null){
                    setResultToToast(TAG + ": Set Camera callback");
                    mCamera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    /*
     * Reset live video stream callback
     */
    private void uninitPreviewer() {
        if (mCamera != null){
            // Reset the callback
            mCamera.setDJICameraReceivedVideoDataCallback(null);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setResultToToast(TAG + ": onStart");
        if (mVideoSurface != null) {
            mVideoSurface.setSurfaceTextureListener(this);
            setResultToToast(TAG + ": mVideoSurface set");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mListener.onViewInit();
    }

    /*
     * Callback function. Called when phone is about to pause/leave the app (user switches screen)
     */
    @Override
    public void onPause() {
        super.onPause();

        //Save battery. Turn off live video feed.
        //uninitPreviewer();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(getContext(), surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    /*
     * Called whenever surface texture is updated.
     * In this context, when the mCodecManager is done decoding the byte stream
     * and has published the update on the screen.
     * @param surface
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.w(TAG, "onSurfaceTextureUpdated: Start");

        //We need to skip people detection occasionally due to it being computationally expensive
        //Check counter
        if(mCounter < NUM_FRAME_SKIP){
            //Skip frame
            mCounter ++;
            return;
        } else{
            //Reset counter
            mCounter = 0;
        }

        /*
         * Use OpenCV to detect any people in the video feed.
         * If anyone is detected, display a bounding box around the person.
         */
        final Bitmap image = mVideoSurface.getBitmap();

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

        Log.w(TAG, "onSurfaceTextureUpdated: Start Contour Detection");
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
                        0,0.3,new Scalar(255,255,255)); //fontface, fontscale, color

                Imgproc.putText(rawOutput, "Person match",
                        new Point(x,y+rHeight+10), //Location
                        0,0.5,new Scalar(255,255,255)); //fontface, fontscale, color
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

        Log.w(TAG, "onSurfaceTextureUpdated: Start Output Bitamp");
        //Push to bitmap and update

        //Bitmap output2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rawOutput, output);
        mOutputView.setImageBitmap(output);
    }

    /*
     * Push messages to the device screen
     * @param string
     */
    private void setResultToToast(final String string){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), string, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
