package com.qapp.romtester;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class InputOutputTestActivity extends Activity implements View.OnClickListener, View.OnTouchListener {

    private ToneGenerator toneGenerator;
    private Vibrator vibrator;

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
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

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
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 500);
            updateStatus(getString(R.string.io_status_sound_played));
        } else {
            updateStatus(getString(R.string.io_status_sound_unavailable));
        }
    }

    private void runVibrationTest() {
        if (vibrator == null || !vibrator.hasVibrator()) {
            updateStatus(getString(R.string.io_status_vibration_no_vibrator));
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            // Deprecated for API 26+, used for old Android compatibility.
            vibrator.vibrate(500);
        }

        updateStatus(getString(R.string.io_status_vibration_pulse));
    }

    private void stopAllTests() {
        if (toneGenerator != null) {
            toneGenerator.stopTone();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        updateStatus(getString(R.string.io_status_stopped));
    }

    private void updateStatus(String text) {
        statusText.setText(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
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
