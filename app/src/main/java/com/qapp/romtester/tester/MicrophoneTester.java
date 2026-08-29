package com.qapp.romtester.tester;

import android.Manifest;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;

import com.qapp.romtester.util.PermissionUtils;

import java.io.File;
import java.io.IOException;

public class MicrophoneTester {

    public interface Listener {
        void onRecordingStarted();

        void onRecordingFailed(String message);

        void onRecordingSaved();

        void onRecordingTooShort();

        void onPlaybackStarted();

        void onPlaybackFailed(String message);

        void onPlaybackStopped();
    }

    interface RecorderFactory {
        MediaRecorder create();
    }

    interface PlayerFactory {
        MediaPlayer create();
    }

    private final Context context;
    private final File outputFile;
    private final Listener listener;
    private final int sdkInt;
    private final RecorderFactory recorderFactory;
    private final PlayerFactory playerFactory;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private boolean recording;
    private boolean playing;

    public MicrophoneTester(Context context, Listener listener) {
        this(context, new File(context.getCacheDir(), "mic_test.3gp"), listener, Build.VERSION.SDK_INT,
                MediaRecorder::new, MediaPlayer::new);
    }

    MicrophoneTester(Context context, File outputFile, Listener listener, int sdkInt,
                      RecorderFactory recorderFactory, PlayerFactory playerFactory) {
        this.context = context;
        this.outputFile = outputFile;
        this.listener = listener;
        this.sdkInt = sdkInt;
        this.recorderFactory = recorderFactory;
        this.playerFactory = playerFactory;
    }

    public boolean hasPermission() {
        return PermissionUtils.has(context, Manifest.permission.RECORD_AUDIO, sdkInt);
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean outputFileExists() {
        return outputFile.exists();
    }

    public void startRecording() {
        try {
            recorder = recorderFactory.create();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(outputFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();

            recording = true;
            listener.onRecordingStarted();
        } catch (IOException | RuntimeException e) {
            listener.onRecordingFailed(e.getMessage());
            releaseRecorder();
        }
    }

    public void stopRecording() {
        try {
            if (recorder != null) {
                recorder.stop();
            }
            listener.onRecordingSaved();
        } catch (RuntimeException e) {
            listener.onRecordingTooShort();
        } finally {
            releaseRecorder();
        }
        recording = false;
    }

    private void releaseRecorder() {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    public void startPlayback() {
        try {
            player = playerFactory.create();
            player.setDataSource(outputFile.getAbsolutePath());
            player.setOnCompletionListener(mp -> stopPlayback());
            player.prepare();
            player.start();

            playing = true;
            listener.onPlaybackStarted();
        } catch (IOException | RuntimeException e) {
            listener.onPlaybackFailed(e.getMessage());
            releasePlayer();
        }
    }

    public void stopPlayback() {
        releasePlayer();
        playing = false;
        listener.onPlaybackStopped();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
