package drone.uas.research.model;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;

import drone.uas.research.R;

/**
 * DialogFragment. Used to request info from user to add a new Waypoint
 * Created by stanc on 4/24/16.
 */
public class AddWaypointDialogFragment extends DialogFragment {

	// Use this instance of the interface to deliver action events
	private AddWaypointDialogListener mListener;

	/*
	 * Callback method. Generates the layout of the dialog.
	 * @param savedInstanceState
	 * @return The new AlertDialog to display to the user.
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_waypoint_add_content, null);
		final TextInputEditText mText = (TextInputEditText) v.findViewById(R.id.addNameEditText);
		final TextInputEditText mAltitudeText = (TextInputEditText) v.findViewById(R.id.addAltitudeEditText);

		builder.setView(v);
		// Add action buttons
		builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				AddWaypointDialogFragment.this.getDialog().cancel();
				mListener.onDialogNegativeClick();
			}
		});
		// Create the AlertDialog object and return it

		final AlertDialog d = builder.create();
		d.show();
		d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Check that the altitude is not empty and is valid
				final String s = mAltitudeText.getText().toString();
				if (s == null || s.isEmpty()) {
					mAltitudeText.setError("Altitude is required");
					return;
				}

				final double altitude = Double.parseDouble(s);
				if (altitude <= 0) {
					mAltitudeText.setError("Altitude must be positive");
					return;
				}

				mListener.onDialogPositiveClick(mText.getText().toString(), Double.parseDouble(mAltitudeText.getText().toString()));
				dismiss();
			}
		});
		return d;
	}


	/**
	 * Allow classes outside of this fragment to listen for user input.
	 */
	public interface AddWaypointDialogListener {
		void onDialogPositiveClick(String name, double altitude);

		void onDialogNegativeClick();
	}

	/*
	 * Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the host
			mListener = (AddWaypointDialogListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement AddWaypointListener");
		}
	}
}
