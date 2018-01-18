package drone.uas.research.model.util;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import drone.uas.research.model.Waypoint;

/**
 * Singleton class. Reads CSV files for waypoints
 *
 * @author Stanley C
 * @since 12/29/17.
 */

public class WaypointFileManager {

	private static WaypointFileManager instance = null;

	private WaypointFileManager() {

	}

	public static WaypointFileManager getInstance() {
		if (instance == null) {
			instance = new WaypointFileManager();
		}
		return instance;
	}

	public List<Waypoint> getWaypointsFromFile(File f) {
		try {
			final CSVReader reader = new CSVReader(new FileReader(f.getAbsolutePath()));
			final List<Waypoint> result = new LinkedList<>();
			String[] s;
			int i = 0;

			// Skip the headers
			reader.readNext();

			while ((s = reader.readNext()) != null) {
				result.add(new Waypoint(String.valueOf(i++), new LatLng(Double.parseDouble(s[0]), Double.parseDouble(s[1])), Double.parseDouble(s[2])));
			}

			return result;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
