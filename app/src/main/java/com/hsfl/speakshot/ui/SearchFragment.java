package com.hsfl.speakshot.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.hsfl.speakshot.MainActivity;
import com.hsfl.speakshot.R;
import com.hsfl.speakshot.service.camera.CameraService;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class SearchFragment extends Fragment implements Observer {
    private static final String TAG = SearchFragment.class.getSimpleName();

    /**
     * the inflated view
     */
    private View mInflatedView;

    /**
     * Service provider that handles the camera object
     */
    private CameraService mCameraService;

    /**
     * The team to search for
     */
    private String searchTerm = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mInflatedView = inflater.inflate(R.layout.search_fragment, container, false);

        mCameraService = CameraService.getInstance();
        // adds an observer for the text recognizer
        mCameraService.addObserver(this);

        // take picture
        final Button searchButton = (Button)mInflatedView.findViewById(R.id.btn_search);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (searchTerm.equals("")) {
                    // open modal
                    final EditText txt = new EditText(getActivity());
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Moustachify Link")
                            .setMessage("Paste in the link of an image to moustachify!")
                            .setView(txt)
                            .setPositiveButton("Moustachify", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // start searching
                                    searchTerm = txt.getText().toString();
                                    mCameraService.analyseStream();
                                    Toast.makeText(getActivity().getApplicationContext(), "Searching for: " + searchTerm, Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                            .show();
                } else {
                    // stop searching
                    searchTerm = "";
                    mCameraService.analyseStream();
                    Toast.makeText(getActivity().getApplicationContext(), "Stop searching", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // light toggle
        final ToggleButton lightSwitch = (ToggleButton)mInflatedView.findViewById(R.id.btn_light_toggle);
        lightSwitch.setChecked(mCameraService.isFlashLightEnabled());
        lightSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCameraService.setFlashLightEnabled(lightSwitch.isChecked());
            }
        });

        return mInflatedView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraService.deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        ArrayList<String> texts = ((Bundle)arg).getStringArrayList("texts");
        for (int i=0; i<texts.size(); i++) {
            if (texts.get(i).contains(searchTerm)) {
                Toast.makeText(getActivity().getApplicationContext(), "Term " + searchTerm + " found", Toast.LENGTH_SHORT).show();
                ((Vibrator)getActivity().getApplication().getSystemService(android.content.Context.VIBRATOR_SERVICE)).vibrate(800);
            }
        }
    }

}
