package drone.uas.research.videodetection;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import dji.sdk.Camera.DJICamera;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";

    //DJI
    private Handler mHandler = null;
    //private DJIAircraft mAircraft;
    private DJIFlightController mFlightController;
    private static DJIBaseProduct mProduct;

    //UI
    private FragmentManager mManager;
    private VideoFragment mVideo;

    /**
     * Get Product Instance
     * @return
     */
    public static synchronized DJIBaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getDJIProduct();
        }
        return mProduct;
    }

    /**
     * Get Camera Instance
     * @return
     */
    public static synchronized DJICamera getCameraInstance() {

        if (getProductInstance() == null) return null;

        DJICamera camera = null;

        if (getProductInstance() instanceof DJIAircraft){
            camera = ((DJIAircraft) getProductInstance()).getCamera();

        }

        return camera;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler(Looper.getMainLooper());

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);

        //Set up OpenCV
        if (!OpenCVLoader.initDebug())
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        else
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");

        //Set UI
        mVideo = VideoFragment.getInstance();
        mManager = getSupportFragmentManager();
        mManager.beginTransaction().add(R.id.mainVideoFragment, mVideo).commit();
    }

    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {
        @Override
        public void onGetRegisteredResult(DJIError error) {
            Log.d(TAG, error == null ? "Success" : error.getDescription());
            setResultToToast(TAG + ": SDKManager Callback");
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Success: DJISDKManager registered", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "register sdk fails, check network is available", Toast.LENGTH_LONG).show();
                    }
                });
            }
            Log.e("TAG", error.toString());
        }

        @Override
        public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
            Log.d(TAG, "onProductChanged: ");
            setResultToToast(TAG + ": onProductChanged");
            mProduct = newProduct;
            if(mProduct != null) {
                mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
            }
            notifyStatusChange();

            //Init video stream preview
            mVideo.initPreviewer();
        }
    };

    private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {

        @Override
        public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }

        @Override
        public void onProductConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {

        @Override
        public void onComponentConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };

    /**
     * Informs phone when remote controller is connected
     */
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
        //initMissionManager();
    }

    /**
     * Initialize flight controller
     * - Checks if product connected is DJIAircraft.
     * -- If so, grab flight controller
     * -- Updates drone location
     * -- Sets data callback for when onBoard SDK sends data
     */
    private void initFlightController() {
        DJIBaseProduct product = getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof DJIAircraft) {
                mFlightController = ((DJIAircraft) product).getFlightController();
            }
        }
        if (mFlightController != null) {
            mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                @Override
                public void onResult(DJIFlightControllerDataType.DJIFlightControllerCurrentState state) {
//                    droneLocationLat = state.getAircraftLocation().getLatitude();
//                    droneLocationLng = state.getAircraftLocation().getLongitude();
//                    Log.d(TAG, "onResult: Drone Location: " + droneLocationLat + "," + droneLocationLng);
//                    //setResultToToast(droneLocationLat + " " + droneLocationLng);
//                    updateDroneLocation();
                }
            });

            //Add external data listener callback
            Log.d(TAG, "OnBoard Data Listener set!");
            Toast.makeText(MainActivity.this, "Onboard Data Callback set!", Toast.LENGTH_LONG).show();
            mFlightController.setReceiveExternalDeviceDataCallback(new DJIFlightControllerDelegate.FlightControllerReceivedDataFromExternalDeviceCallback() {
                @Override
                public void onResult(final byte[] data) {
                    Log.d(TAG, "onResult: " + data.toString());
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Data: " + new String(data));
                            Toast.makeText(MainActivity.this, "Data: " + new String(data), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }

    /**
     * Push messages to the device screen
     * @param string
     */
    private void setResultToToast(final String string){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
