package drone.uas.research;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import drone.uas.research.model.Waypoint;
import drone.uas.research.model.WaypointManager;

/**
 * Singleton Fragment class. Displays Google Maps. Allows user to place Waypoints and see drone location.
 * @author Stanley C
 * @since 2/16/16.
 */
public class MapFragment extends Fragment implements GoogleMap.OnMapClickListener,
        OnMapReadyCallback, GoogleMap.OnMapLongClickListener, WaypointManager.WPListener, GoogleMap.OnMarkerClickListener {

    //Singleton
    private static MapFragment mFragment = null;
    private GoogleMap mMap;
    private SupportMapFragment maps;
    private MapListener mListener;

    /*
    Callback function. When called, map updates with new Waypoints from the List.
     */
    @Override
    public void onDataSetChanged(List<Waypoint> l) {
        mMap.clear();
        for(Waypoint w : l){
            mMap.addMarker(new MarkerOptions().position(w.getCoords()).title(w.getName()));
        }
    }

    /**
     * Interface. Used to inform listeners of user interaction with the map.
     */
    public interface MapListener{
        void onClick(LatLng latLng);
        void onLongClick(LatLng latLng);
        void onMarkerClick(Marker m);
    }

    /**
     * Default Constructor. DO NOT USE
     */
    public MapFragment(){}

    /**
     * Returns the instance of MapFragment. Creates one if it does not exist yet.
     * @return
     */
    public static MapFragment getInstance(){
        if(mFragment == null){
            mFragment = new MapFragment();
        }
        return mFragment;
    }

    /**
     * Attaches specified listener.
     * @param listener
     */
    public void attachListener(MapListener listener){
        mListener = listener;
    }

    /*
     * Called when Fragment is first created
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /*
     * Called when Fragment is being created. Creates the UI of the fragment.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        return v;
    }

    /*
     * Called when the Fragment is being started. Displays the Google Map Fragment
     */
    @Override
    public void onStart() {
        super.onStart();
        maps = new SupportMapFragment();
        this.getChildFragmentManager().beginTransaction()
                .add(R.id.mapContainer, maps)
                .commit();
        maps.getMapAsync(this);
    }

    /*
     * Called when Google Maps is ready. Focuses Google Map onto a specific location.
     * Also initializes user interaction listeners
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(mMap == null)
            mMap = googleMap;
        else mMap = googleMap;
        LatLng wayPoint1 = new LatLng(42.7284,-73.6829);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(wayPoint1, 15));
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
    }

    /*
     * Called when user clicks on the map
     */
    @Override
    public void onMapClick(LatLng latLng) {
        mListener.onClick(latLng);
    }

    /*
     * Called when user performs a long click on the map
     */
    @Override
    public void onMapLongClick(LatLng latLng) {
        mListener.onLongClick(latLng);
    }

    /*
     * Called when user clicks on a marker. na
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        mListener.onMarkerClick(marker);
        return false;
    }

    /**
     * Adds marker to the map
     * @param options MarkerOptions representation of the marker to be added.
     * @return
     */
    public Marker addMarker(MarkerOptions options){
        return mMap.addMarker(options);
    }

    /**
     * Focus map zoom onto the specified location
     * @param cu CameraUpdate settings to inform Map on update details
     */
    public void moveCamera(CameraUpdate cu){
        mMap.moveCamera(cu);
    }

}
