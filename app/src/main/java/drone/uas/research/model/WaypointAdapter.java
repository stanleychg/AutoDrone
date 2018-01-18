package drone.uas.research.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import drone.uas.research.R;

/**
 * ArrayAdapter class. Used to display details regarding Waypoint objects in a list.
 * Created by stanc on 4/24/16.
 */
public class WaypointAdapter extends ArrayAdapter<Waypoint> implements WaypointManager.WPListener{

    /**
     * Constructor. Pass in the current Application/Activity context and List of Waypoint objects.
     * @param context
     * @param resource
     * @param objects
     */
    public WaypointAdapter(Context context, int resource, List<Waypoint> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // If view is null, inflate it with the custom layout
        if(convertView == null){
            convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.item_waypoint, null);
        }

        // Display name and geographical coordinates of the labeled waypoint
        TextView mName = (TextView)convertView.findViewById(R.id.pointName);
        TextView mCoords = (TextView)convertView.findViewById(R.id.pointCoords);
        TextView mAlt = (TextView)convertView.findViewById(R.id.pointAlt);
        final Waypoint waypoint = this.getItem(position);
        mName.setText(waypoint.getName());
        mCoords.setText(waypoint.getCoords().toString());
        mAlt.setText("alt: " + waypoint.getAltitude() + " m.");

        return convertView;
    }

    @Override
    public void onDataSetChanged(List<Waypoint> l) {
        this.clear();
        this.addAll(l);
        this.notifyDataSetChanged();
    }
}
