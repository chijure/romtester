package com.qapp.romtester.basics;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.ToneVibrationTester;

public class InputOutputTestActivity extends Activity implements View.OnClickListener, View.OnTouchListener {

    private ToneVibrationTester tester;

    private TextView statusText;
    private TextView touchText;

    private Button soundButton;
    private Button vibrateButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_io_test);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        tester = new ToneVibrationTester(this);

        statusText = findViewById(R.id.text_status);
        touchText = findViewById(R.id.text_touch);

        soundButton = findViewById(R.id.button_sound);
        vibrateButton = findViewById(R.id.button_vibrate);
        stopButton = findViewById(R.id.button_stop);

        soundButton.setOnClickListener(this);
        vibrateButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        View touchArea = findViewById(R.id.touch_area);
        touchArea.setOnTouchListener(this);

        updateStatus(getString(R.string.io_status_ready));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_sound) {
            runSoundTest();
        } else if (id == R.id.button_vibrate) {
            runVibrationTest();
        } else if (id == R.id.button_stop) {
            stopAllTests();
        }
    }

    private void runSoundTest() {
        boolean played = tester.playTestTone();
        updateStatus(getString(played ? R.string.io_status_sound_played : R.string.io_status_sound_unavailable));
    }

    private void runVibrationTest() {
        boolean vibrated = tester.vibrateOnce(500);
        updateStatus(getString(vibrated ? R.string.io_status_vibration_pulse : R.string.io_status_vibration_no_vibrator));
    }

    private void stopAllTests() {
        tester.stop();
        updateStatus(getString(R.string.io_status_stopped));
    }

    private void updateStatus(String text) {
        statusText.setText(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tester.release();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        String action;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                action = getString(R.string.io_touch_action_down);
                break;
            case MotionEvent.ACTION_MOVE:
                action = getString(R.string.io_touch_action_move);
                break;
            case MotionEvent.ACTION_UP:
                action = getString(R.string.io_touch_action_up);
                break;
            default:
                action = getString(R.string.io_touch_action_other);
                break;
        }

        String msg = getString(R.string.io_touch_message, action, (int) event.getX(), (int) event.getY());
        touchText.setText(msg);
        return true;
    }
}
