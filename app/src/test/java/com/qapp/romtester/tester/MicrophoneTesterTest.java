package com.qapp.romtester.tester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class MicrophoneTesterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String RECORD_PERMISSION = "android.permission.RECORD_AUDIO";
    private static final int SDK = 23;

    private MicrophoneTester tester(File outputFile, MediaRecorder recorder, MediaPlayer player,
                                     MicrophoneTester.Listener listener) {
        Context context = mock(Context.class);
        return new MicrophoneTester(context, outputFile, listener, SDK, () -> recorder, () -> player);
    }

    @Test
    public void hasPermission_delegatesToPermissionUtils() throws IOException {
        Context context = mock(Context.class);
        when(context.checkSelfPermission(RECORD_PERMISSION)).thenReturn(PackageManager.PERMISSION_GRANTED);
        MicrophoneTester tester = new MicrophoneTester(context, temporaryFolder.newFile(), mock(MicrophoneTester.Listener.class),
                SDK, MediaRecorder::new, MediaPlayer::new);

        assertTrue(tester.hasPermission());
    }

    @Test
    public void startRecording_succeedsAndNotifiesListener() throws IOException {
        MediaRecorder recorder = mock(MediaRecorder.class);
        MicrophoneTester.Listener listener = mock(MicrophoneTester.Listener.class);
        MicrophoneTester tester = tester(temporaryFolder.newFile(), recorder, mock(MediaPlayer.class), listener);

        tester.startRecording();

        assertTrue(tester.isRecording());
        verify(recorder).start();
        verify(listener).onRecordingStarted();
    }

    @Test
    public void startRecording_reportsFailureWhenPrepareThrows() throws IOException {
        MediaRecorder recorder = mock(MediaRecorder.class);
        doThrow(new IOException("boom")).when(recorder).prepare();
        MicrophoneTester.Listener listener = mock(MicrophoneTester.Listener.class);
        MicrophoneTester tester = tester(temporaryFolder.newFile(), recorder, mock(MediaPlayer.class), listener);

        tester.startRecording();

        assertFalse(tester.isRecording());
        verify(listener).onRecordingFailed("boom");
        verify(recorder).release();
    }

    @Test
    public void stopRecording_savedWhenStopSucceeds() throws IOException {
        MediaRecorder recorder = mock(MediaRecorder.class);
        MicrophoneTester.Listener listener = mock(MicrophoneTester.Listener.class);
        MicrophoneTester tester = tester(temporaryFolder.newFile(), recorder, mock(MediaPlayer.class), listener);
        tester.startRecording();

        tester.stopRecording();

        assertFalse(tester.isRecording());
        verify(listener).onRecordingSaved();
    }

    @Test
    public void stopRecording_tooShortWhenStopThrows() throws IOException {
        MediaRecorder recorder = mock(MediaRecorder.class);
        doThrow(new RuntimeException("too short")).when(recorder).stop();
        MicrophoneTester.Listener listener = mock(MicrophoneTester.Listener.class);
        MicrophoneTester tester = tester(temporaryFolder.newFile(), recorder, mock(MediaPlayer.class), listener);
        tester.startRecording();

        tester.stopRecording();

        verify(listener).onRecordingTooShort();
        verify(recorder).release();
    }

    @Test
    public void startPlayback_succeedsAndNotifiesListener() throws IOException {
        MediaPlayer player = mock(MediaPlayer.class);
        MicrophoneTester.Listener listener = mock(MicrophoneTester.Listener.class);
        MicrophoneTester tester = tester(temporaryFolder.newFile(), mock(MediaRecorder.class), player, listener);

        tester.startPlayback();

        assertTrue(tester.isPlaying());
        verify(player).start();
        verify(listener).onPlaybackStarted();
    }

    @Test
    public void startPlayback_reportsFailureWhenPrepareThrows() throws IOException {
        MediaPlayer player = mock(MediaPlayer.class);
        doThrow(new IOException("no data")).when(player).prepare();
        MicrophoneTester.Listener listener = mock(MicrophoneTester.Listener.class);
        MicrophoneTester tester = tester(temporaryFolder.newFile(), mock(MediaRecorder.class), player, listener);

        tester.startPlayback();

        assertFalse(tester.isPlaying());
        verify(listener).onPlaybackFailed("no data");
        verify(player).release();
    }

    @Test
    public void stopPlayback_releasesAndNotifiesListener() throws IOException {
        MediaPlayer player = mock(MediaPlayer.class);
        MicrophoneTester.Listener listener = mock(MicrophoneTester.Listener.class);
        MicrophoneTester tester = tester(temporaryFolder.newFile(), mock(MediaRecorder.class), player, listener);
        tester.startPlayback();

        tester.stopPlayback();

        assertFalse(tester.isPlaying());
        verify(player).release();
        verify(listener).onPlaybackStopped();
    }

    @Test
    public void outputFileExists_reflectsRealFileState() throws IOException {
        File missing = new File(temporaryFolder.getRoot(), "does_not_exist.3gp");
        MicrophoneTester tester = tester(missing, mock(MediaRecorder.class), mock(MediaPlayer.class),
                mock(MicrophoneTester.Listener.class));

        assertFalse(tester.outputFileExists());
    }
}
