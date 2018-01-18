package drone.uas.research.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Singleton class. Handles user requests to manage waypoints.
 * Created by stanc on 4/24/16.
 */
public class WaypointManager {

    //Singleton
    private static WaypointManager mInstance = null;

    //Data
    private List<Waypoint> mList;

    /**
     * Interface to inform listeners whenever the list of Waypoint objects changes (caused by user).
     */
    public interface WPListener{
        /**
         * Callback method. Called whenever changes to Waypoint objects have occurred.
         * @param l The new List of Waypoint objects.
         */
        void onDataSetChanged(List<Waypoint> l);
    }

    //Listeners
    private Set<WPListener> mListeners;

    //Default constructor. Initializes data structures.
    private WaypointManager(){
        mList = new LinkedList<>();
        mListeners = new HashSet<>();
    }

    /**
     * Registers the listener for callbacks.
     * @param listener
     */
    public void attachListener(WPListener listener){
        mListeners.add(listener);
    }

    // Informs all listeners that the dataset has changed.
    private void notifyListeners(){
        List<Waypoint> l = getWaypoints();
        for(WPListener w: mListeners){
            w.onDataSetChanged(l);
        }
    }

    /**
     * Returns the instance of WaypointManager. Creates one if it does not exist.
     * @return
     */
    public static WaypointManager getInstance(){
        if(mInstance == null){
            mInstance = new WaypointManager();
        }
        return mInstance;
    }

    /**
     * Adds a Waypoint given a name and coordinates.
     * @param name String representation of the name.
     * @param coords LatLng representation of coordinates.
     */
    public void addWaypoint(String name, LatLng coords, double alt){
        mList.add(new Waypoint(name, coords, alt));
        notifyListeners();
    }

    /**
     * Removes a Waypoint from the data, should it exist.
     * @param mPoint
     */
    public void removeWaypoints(Waypoint mPoint){
        mList.remove(mPoint);
        notifyListeners();
    }

    /**
     * Removes all Waypoints
     */
    public void removeAllWaypoints(){
        mList.clear();
        notifyListeners();
    }

    // Creates a new List of the Waypoint objects.
    private List<Waypoint> getWaypoints(){
        return new LinkedList<>(mList);
    }

}
