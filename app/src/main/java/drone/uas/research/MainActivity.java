package drone.uas.research;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.DJIWaypoint;
import dji.sdk.MissionManager.DJIWaypointMission;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;
import drone.uas.research.model.AddWaypointDialogFragment;
import drone.uas.research.model.Waypoint;
import drone.uas.research.model.WaypointAdapter;
import drone.uas.research.model.WaypointManager;
import drone.uas.research.model.util.WaypointFileManager;

/**
 * Main Activity. App launches this activity when the app is first started.
 * Initializes the MapFragment and VideoFragment.
 *
 * @author Stanley C
 * @since 2/16/16.
 */
public class MainActivity extends AppCompatActivity implements MapFragment.MapListener,
		AddWaypointDialogFragment.AddWaypointDialogListener,
		VideoFragment.VideoListener {
	private static final int LOAD_WAYPOINT_REQUEST_CODE = 1;
	private static final String TAG = "MainActivity";
	public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";

	//UI
	private MapFragment fragmentMap;
	private ListView mWaypointList;
	private Button mStartMission;
	private Button mStopMission;
	private Button mPrepareMission;
	private Button mLocate;
	private Button mClear;
	private Button mLoad;
	private VideoFragment fragmentVideo;

	//Data Model
	private WaypointManager mWPManager;
	private WaypointFileManager mWPFManger;
	private LatLng mTempHolder;
	private WaypointAdapter mWPAdapter;
	private List<Waypoint> mWaypoints;

	//DJI
	private Handler mHandler;
	private DJIAircraft mAircraft;
	private DJIFlightController mFlightController;
	private static DJIBaseProduct mProduct;

	//DJI - Drone Location
	private double droneLocationLat = 181, droneLocationLng = 181;
	private Marker droneMarker = null;

	//DJI - Waypoint Mission
	private float altitude = 8.0f;
	private float mSpeed = 6.0f;

	//Final action of any Waypoint Mission is to return to HOME (Set via DJI GO app)
	private DJIWaypointMission.DJIWaypointMissionFinishedAction mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoHome;
	private DJIWaypointMission.DJIWaypointMissionHeadingMode mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;
	private DJIWaypointMission mWaypointMission;
	private DJIMissionManager mMissionManager;

	/**
	 * Get Product Instance
	 *
	 * @return
	 */
	public static synchronized DJIBaseProduct getProductInstance() {
		if (null == mProduct) {
			mProduct = DJISDKManager.getInstance().getDJIProduct();
		}
		return mProduct;
	}

	/*
	 * Callback function. Called when MainActivity runs for the first time (is being created).
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate: ");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//Initialize Data model
		mWPManager = WaypointManager.getInstance();
		mWPFManger = WaypointFileManager.getInstance();

        /*Set up UI*/
		//Load - Load from file
		mLoad = (Button) findViewById(R.id.mainLoad);
		mLoad.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				startActivityForResult(i, LOAD_WAYPOINT_REQUEST_CODE);
			}
		});

		//Clear - Clear waypoints
		mClear = (Button) findViewById(R.id.mainClear);
		mClear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mWPManager.removeAllWaypoints();
			}
		});

		//Locate - Update drone location on map
		mLocate = (Button) findViewById(R.id.mainLocate);
		mLocate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateDroneLocation();
				cameraUpdate();
			}
		});

		//Prepare - sends waypoint mission to drone
		mPrepareMission = (Button) findViewById(R.id.mainPrepareMission);
		mPrepareMission.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "onClick: Prepare mission");
				//Clear waypoint mission if it exists
				if (mWaypointMission != null) {
					mWaypointMission.removeAllWaypoints();
				}

				//Convert to DJIWaypionts
				for (int i = 0; i < mWPAdapter.getCount(); i++) {
					Waypoint w = mWPAdapter.getItem(i);
					DJIWaypoint mWaypoint = new DJIWaypoint(
							w.getCoords().latitude,
							w.getCoords().longitude,
							(float)w.getAltitude());

					//Add waypoints to Waypoint arraylist;
					if (mWaypointMission != null) {
						mWaypointMission.addWaypoint(mWaypoint);
						setResultToToast("Success: Waypoint Added");
					} else {
						setResultToToast("Cannot add Waypoint");
					}
				}

				//Configure waypoint mission
				configWayPointMission();

				//Send data
				prepareWayPointMission();
			}
		});

		//Start - starts the mission
		mStartMission = (Button) findViewById(R.id.mainStartMission);
		mStartMission.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "onClick: Start mission");
				startWaypointMission();
			}
		});

		//Stop - stops execution of mission
		mStopMission = (Button) findViewById(R.id.mainStopMission);
		mStopMission.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopWaypointMission();
			}
		});

		//Map Fragment
		fragmentMap = MapFragment.getInstance();
		fragmentMap.attachListener(this);
		getSupportFragmentManager().beginTransaction()
				.add(R.id.mainContainer, fragmentMap)
				.commit();
		mWaypointList = (ListView) findViewById(R.id.mainWaypointList);
		mWPAdapter = new WaypointAdapter(this, R.layout.item_waypoint, new LinkedList<Waypoint>());
		mWaypointList.setAdapter(mWPAdapter);
		mWPManager.attachListener(mWPAdapter);
		mWPManager.attachListener(fragmentMap);

		//Video fragment
		fragmentVideo = VideoFragment.getInstance(this);

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

		mHandler = new Handler(Looper.getMainLooper());
		DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);

		//Set up OpenCV
		if (!OpenCVLoader.initDebug())
			Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
		else
			Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");

	}

	/**
	 * Centers the Google Map View onto the drone location
	 */
	private void cameraUpdate() {
		LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
		float zoomlevel = (float) 18.0;
		CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
		fragmentMap.moveCamera(cu);
	}

	/**
	 * Send waypoint mission to drone
	 */
	private void prepareWayPointMission() {
		Log.d(TAG, "prepareWayPointMission: ");
		if (mMissionManager != null && mWaypointMission != null) {
			DJIMission.DJIMissionProgressHandler progressHandler = new DJIMission.DJIMissionProgressHandler() {
				@Override
				public void onProgress(DJIMission.DJIProgressType type, float progress) {
					setResultToToast("Progress: " + String.valueOf(progress * 100) + "%");
				}
			};
			mMissionManager.prepareMission(mWaypointMission, progressHandler, new DJIBaseComponent.DJICompletionCallback() {
				@Override
				public void onResult(DJIError error) {
					setResultToToast("Prepare: " + (error == null ? "Success: Mission Prepared" : error.getDescription()));
				}
			});

		}
	}

	/*
	 * Helper method. Starts waypoint mission once waypoint mission is prepared and sent to drone
	 */
	private void startWaypointMission() {
		Log.d(TAG, "startWaypointMission: ");
		if (mMissionManager != null) {
			mMissionManager.startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
				@Override
				public void onResult(DJIError error) {
					setResultToToast("Start: " + (error == null ? "Success" : error.getDescription()));
					if (error == null) {

						//Waypoint mission transmitted. Switch to video feed
						getSupportFragmentManager().beginTransaction()
								.replace(R.id.mainContainer, fragmentVideo)
								.commit();

					}
				}
			});
		}
	}

	/*
	 * Tells drone to stop waypoint mission.
	 */
	private void stopWaypointMission() {
		Log.d(TAG, "stopWaypointMission: ");
		if (mMissionManager != null) {
			mMissionManager.stopMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
				@Override
				public void onResult(DJIError error) {
					setResultToToast("Stop: " + (error == null ? "Success" : error.getDescription()));
					if (error == null) {
						//Hide video feed. Automatically uninitializes video feed.
						getSupportFragmentManager().beginTransaction()
								.replace(R.id.mainContainer, fragmentMap)
								.commit();
					}
				}
			});
			if (mWaypointMission != null) {
				mWaypointMission.removeAllWaypoints();
			}
		}
	}

	/**
	 * Informs phone when remote controller is connected
	 */
	protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "onReceive: ");
			onProductConnectionChange();
		}
	};

	/*
	 * Inform phone of connection change detected between phone and remote controller
	 */
	private void notifyStatusChange() {
		Log.d(TAG, "notifyStatusChange: ");
		mHandler.removeCallbacks(updateRunnable);
		mHandler.postDelayed(updateRunnable, 500);
	}

	/*
	 * Inform phone of connection change detected between phone and remote controller
	 */
	private Runnable updateRunnable = new Runnable() {
		@Override
		public void run() {
			Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
			sendBroadcast(intent);
		}
	};

	/*
	 * Initialize flight controller and mission manager
	 */
	private void onProductConnectionChange() {
		initFlightController();
		initMissionManager();
	}

	/*
	 * Initialize flight controller
	 * - Checks if product connected is DJIAircraft.
	 * -- If so, grab flight controller
	 * -- Updates drone location
	 * -- Sets data callback for when onBoard SDK sends data
	 */
	private void initFlightController() {
		Log.d(TAG, "initFlightController: ");
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
					droneLocationLat = state.getAircraftLocation().getLatitude();
					droneLocationLng = state.getAircraftLocation().getLongitude();
					Log.d(TAG, "onResult: Drone Location: " + droneLocationLat + "," + droneLocationLng);
					//setResultToToast(droneLocationLat + " " + droneLocationLng);
					updateDroneLocation();
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
	 * Sanity check. Check if GPS coordinates are valid
	 *
	 * @param latitude
	 * @param longitude
	 * @return true if GPS coordinates are valid. Else, false
	 */
	public static boolean checkGpsCoordinates(double latitude, double longitude) {
		return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
	}

	/*
	 * Updates drone image location on maps
	 */
	private void updateDroneLocation() {
		Log.d(TAG, "updateDroneLocation: ");
		LatLng pos = new LatLng(droneLocationLat, droneLocationLng);

		//Create MarkerOptions object
		final MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position(pos);
		markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (droneMarker != null) {
					droneMarker.remove();
				}
				if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
					droneMarker = fragmentMap.addMarker(markerOptions);
				}
			}
		});
	}

	/*
	 * Helper function to help configure waypoint mission
	 */
	private void configWayPointMission() {
		Log.d(TAG, "configWayPointMission: ");
		if (mWaypointMission != null) {
			mWaypointMission.finishedAction = mFinishedAction;
			mWaypointMission.headingMode = mHeadingMode;
			mWaypointMission.autoFlightSpeed = mSpeed;
		}
	}

	/*
	 * Configure MisisonManager for Waypoint tasks
	 */
	private void initMissionManager() {
		Log.d(TAG, "initMissionManager: ");
		DJIBaseProduct product = getProductInstance();
		if (product == null || !product.isConnected()) {
			mMissionManager = null;
			return;
		} else {
			mMissionManager = product.getMissionManager();
			mMissionManager.setMissionProgressStatusCallback(new DJIMissionManager.MissionProgressStatusCallback() {
				@Override
				public void missionProgressStatus(DJIMission.DJIMissionProgressStatus djiMissionProgressStatus) {

				}
			});
			mMissionManager.setMissionExecutionFinishedCallback(new DJIBaseComponent.DJICompletionCallback() {
				@Override
				public void onResult(DJIError djiError) {
					//Mission execution complete. Stop video feed

					getSupportFragmentManager().beginTransaction()
							.replace(R.id.mainContainer, fragmentMap)
							.commit();

				}
			});
		}
		mWaypointMission = new DJIWaypointMission();
		if (mWaypointMission != null) {
			mWaypointMission.removeAllWaypoints(); // Remove all the waypoints added to the task
		}
	}

	/*
	 * Initialized when onCreate is called. Check if developer is registered to use SDK
	 */
	private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {
		@Override
		public void onGetRegisteredResult(DJIError error) {
			Log.d(TAG, error == null ? "Success" : error.getDescription());
			if (error == DJISDKError.REGISTRATION_SUCCESS) {
				DJISDKManager.getInstance().startConnectionToProduct();
				Handler handler = new Handler(Looper.getMainLooper());
				handler.post(new Runnable() {
					@Override
					public void run() {
						Log.d(TAG, "run: Success");
						Toast.makeText(getApplicationContext(), "Success: DJISDKManager Registered", Toast.LENGTH_LONG).show();
					}
				});
			} else {
				Handler handler = new Handler(Looper.getMainLooper());
				handler.post(new Runnable() {
					@Override
					public void run() {
						Log.d(TAG, "run: Failed");
						Toast.makeText(getApplicationContext(), "register sdk fails, check network is available", Toast.LENGTH_LONG).show();
					}
				});
			}
			if (error != null)
				Log.e("TAG", error.toString());
		}

		@Override
		public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
			mProduct = newProduct;
			if (mProduct != null) {
				mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
			}
			notifyStatusChange();
		}
	};

	/*
	 * Detects whenever a new DJI product is connected
	 */
	private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {

		@Override
		public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
			if (newComponent != null) {
				newComponent.setDJIComponentListener(mDJIComponentListener);
			}
			notifyStatusChange();
		}

		@Override
		public void onProductConnectivityChanged(boolean isConnected) {
			notifyStatusChange();
		}
	};

	/*
	 * Detects whenever a new DJI product is connected
	 */
	private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {

		@Override
		public void onComponentConnectivityChanged(boolean isConnected) {
			notifyStatusChange();
		}
	};

	/*
	 * Push messages to the device screen
	 * @param string
	 */
	private void setResultToToast(final String string) {
		MainActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
			}
		});
	}

	/*
	 * Callback function. Called when user enters this screen of the application.
	 */
	@Override
	public void onResume() {
		Log.d(TAG, "onResume: ");
		super.onResume();

		//Register BroadcastReceiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(FLAG_CONNECTION_CHANGE);
		registerReceiver(mReceiver, filter);

		initFlightController();
		initMissionManager();
	}

	/*
	 * Callback function. Called when user leaves this screen of this application.
	 */
	@Override
	public void onPause() {
		Log.d(TAG, "onPause: ");
		super.onPause();

		//Unregister BroadcastReceiver
		unregisterReceiver(mReceiver);
	}

	/*
	 * Callback function. Called whenever user clicks on the Google Map.
	 */
	@Override
	public void onClick(LatLng latLng) {
	}

	/*
	 * Callback function. Called whenever user long clicks on the Google Map.
	 * Present user with a dialog to create a new Waypoint (displayed via marker on Google Maps)
	 * @param latLng
	 */
	@Override
	public void onLongClick(LatLng latLng) {
		//Temporarily hold coordinates
		mTempHolder = latLng;

		//Show creation dialog
		DialogFragment newFragment = new AddWaypointDialogFragment();
		newFragment.show(getSupportFragmentManager(), "add");
	}

	/*
	 * Callback function. Called when a marker on Google maps is clicked. No implementation.
	 */
	@Override
	public void onMarkerClick(Marker m) {
	}

	/*
	 * Callback function. Called when user selects OK to creating a waypoint.
	 */
	@Override
	public void onDialogPositiveClick(String mName, double mAltitude) {
		mWPManager.addWaypoint(mName, mTempHolder, mAltitude);
	}

	/*
	 * Callback function. Called when user selects no/cancel to creating a waypoint
	 */
	@Override
	public void onDialogNegativeClick() {
		mTempHolder = null;
	}

	@Override
	public void onViewInit() {
		//Initialize video feed.
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				fragmentVideo.initPreviewer(mProduct);
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == LOAD_WAYPOINT_REQUEST_CODE) {
			//Check if csv or txt
			String extension = MimeTypeMap.getFileExtensionFromUrl(data.getData().toString()).toLowerCase();
			if (extension.equals("txt") || extension.equals("csv")) {
				Toast.makeText(MainActivity.this, extension, Toast.LENGTH_LONG).show();
				final String s = data.getData().getPath();
				final Uri u = data.getData();
				System.out.println(s);
				final File env = Environment.getRootDirectory();
				final File path = new File(s);
				final List<Waypoint> l = mWPFManger.getWaypointsFromFile(path);
				if (l == null) {
					System.err.println("Failed to read");
					return;
				}

				for (Waypoint w: l) {
					mWPManager.addWaypoint(w.getName(), w.getCoords(), w.getAltitude());
				}
			}
		}
	}
}
