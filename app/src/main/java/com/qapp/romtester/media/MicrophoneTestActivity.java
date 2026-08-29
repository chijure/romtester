package com.qapp.romtester.media;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.MicrophoneTester;
import com.qapp.romtester.util.PermissionUtils;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MicrophoneTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_RECORD_AUDIO = 1;

    private TextView statusText;
    private Button recordButton;
    private Button playButton;
    private MicrophoneTester tester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone_test);

        statusText = findViewById(R.id.text_mic_status);
        recordButton = findViewById(R.id.button_record);
        playButton = findViewById(R.id.button_play);

        recordButton.setOnClickListener(this);
        playButton.setOnClickListener(this);

        tester = new MicrophoneTester(this, new MicrophoneTester.Listener() {
            @Override
            public void onRecordingStarted() {
                recordButton.setText(getString(R.string.mic_button_stop_recording));
                playButton.setEnabled(false);
                statusText.setText(getString(R.string.mic_status_recording));
            }

            @Override
            public void onRecordingFailed(String message) {
                statusText.setText(getString(R.string.mic_recording_failed_format, message));
            }

            @Override
            public void onRecordingSaved() {
                statusText.setText(getString(R.string.mic_status_recording_saved));
            }

            @Override
            public void onRecordingTooShort() {
                statusText.setText(getString(R.string.mic_status_recording_too_short));
            }

            @Override
            public void onPlaybackStarted() {
                playButton.setText(getString(R.string.mic_button_stop_playback));
                statusText.setText(getString(R.string.mic_status_playing));
            }

            @Override
            public void onPlaybackFailed(String message) {
                statusText.setText(getString(R.string.mic_playback_failed_format, message));
            }

            @Override
            public void onPlaybackStopped() {
                playButton.setText(getString(R.string.mic_button_play));
                statusText.setText(getString(R.string.mic_status_ready));
            }
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_record) {
            if (tester.isRecording()) {
                stopRecording();
            } else if (tester.hasPermission()) {
                tester.startRecording();
            } else {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            }
        } else if (id == R.id.button_play) {
            if (tester.isPlaying()) {
                tester.stopPlayback();
            } else {
                tester.startPlayback();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (PermissionUtils.granted(grantResults)) {
                tester.startRecording();
            } else {
                statusText.setText(getString(R.string.mic_permission_denied));
            }
        }
    }

    private void stopRecording() {
        tester.stopRecording();
        recordButton.setText(getString(R.string.mic_button_record));
        playButton.setEnabled(tester.outputFileExists());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tester.isRecording()) {
            stopRecording();
        }
        if (tester.isPlaying()) {
            tester.stopPlayback();
        }
    }
}
