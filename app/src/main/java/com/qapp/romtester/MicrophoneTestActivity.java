package com.qapp.romtester;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class MicrophoneTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_RECORD_AUDIO = 1;

    private TextView statusText;
    private Button recordButton;
    private Button playButton;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private File outputFile;
    private boolean isRecording;
    private boolean isPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone_test);

        statusText = findViewById(R.id.text_mic_status);
        recordButton = findViewById(R.id.button_record);
        playButton = findViewById(R.id.button_play);

        recordButton.setOnClickListener(this);
        playButton.setOnClickListener(this);

        outputFile = new File(getCacheDir(), "mic_test.3gp");
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_record) {
            if (isRecording) {
                stopRecording();
            } else if (hasRecordPermission()) {
                startRecording();
            } else {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            }
        } else if (id == R.id.button_play) {
            if (isPlaying) {
                stopPlayback();
            } else {
                startPlayback();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                statusText.setText(getString(R.string.mic_permission_denied));
            }
        }
    }

    private boolean hasRecordPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void startRecording() {
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(outputFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            isRecording = true;
            recordButton.setText(getString(R.string.mic_button_stop_recording));
            playButton.setEnabled(false);
            statusText.setText(getString(R.string.mic_status_recording));
        } catch (IOException | RuntimeException e) {
            statusText.setText(getString(R.string.mic_recording_failed_format, e.getMessage()));
            releaseRecorder();
        }
    }

    private void stopRecording() {
        try {
            if (recorder != null) {
                recorder.stop();
            }
            statusText.setText(getString(R.string.mic_status_recording_saved));
        } catch (RuntimeException e) {
            statusText.setText(getString(R.string.mic_status_recording_too_short));
        } finally {
            releaseRecorder();
        }

        isRecording = false;
        recordButton.setText(getString(R.string.mic_button_record));
        playButton.setEnabled(outputFile.exists());
    }

    private void releaseRecorder() {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    private void startPlayback() {
        try {
            player = new MediaPlayer();
            player.setDataSource(outputFile.getAbsolutePath());
            player.setOnCompletionListener(mp -> stopPlayback());
            player.prepare();
            player.start();

            isPlaying = true;
            playButton.setText(getString(R.string.mic_button_stop_playback));
            statusText.setText(getString(R.string.mic_status_playing));
        } catch (IOException | RuntimeException e) {
            statusText.setText(getString(R.string.mic_playback_failed_format, e.getMessage()));
            releasePlayer();
        }
    }

    private void stopPlayback() {
        releasePlayer();
        isPlaying = false;
        playButton.setText(getString(R.string.mic_button_play));
        statusText.setText(getString(R.string.mic_status_ready));
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            stopRecording();
        }
        if (isPlaying) {
            stopPlayback();
        }
    }
}
