package drone.uas.research.model;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Currently unused.
 * DialogFragment. Allows user to edit existing Waypoint objects.
 * Created by stanc on 4/24/16.
 */
public class EditWaypointDialogFragment extends DialogFragment {

    // Use this instance of the interface to deliver action events
    private EditWaypointDialogListener mListener;

    /* Callback method. Generates the layout of the dialog.
     * @param savedInstanceState
     * @return The new AlertDialog to display to the user.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete marker?");
        // Add action buttons
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mListener.onDialogPositiveClick();
            }
        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        EditWaypointDialogFragment.this.getDialog().cancel();
                        mListener.onDialogNegativeClick();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    /**
     * Allow classes outside of this fragment to listen for user input.
     */
    public interface EditWaypointDialogListener {
        void onDialogPositiveClick();
        void onDialogNegativeClick();
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (EditWaypointDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement EditWaypointDialogListener");
        }
    }
}
