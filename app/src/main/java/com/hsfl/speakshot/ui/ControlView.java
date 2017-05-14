package com.hsfl.speakshot.ui;

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.hsfl.speakshot.R;

import static android.content.Context.VIBRATOR_SERVICE;

public class ControlView extends LinearLayout {
    private static final String TAG = ControlView.class.getSimpleName();

    public ControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.control_view, this);

        final Button picButton = (Button)findViewById(R.id.btn_picture);
        picButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Toast.makeText(getContext(), "MOIN", Toast.LENGTH_LONG).show();
            }
        });

        final Button vibButton = (Button)findViewById(R.id.btn_vibrate);
        vibButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // vibration for 800 milliseconds
                ((Vibrator)getContext().getSystemService(VIBRATOR_SERVICE)).vibrate(800);
            }
        });
    }
}