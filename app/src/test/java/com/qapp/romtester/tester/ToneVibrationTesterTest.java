package com.qapp.romtester.tester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.media.ToneGenerator;
import android.os.Vibrator;

import org.junit.Test;

public class ToneVibrationTesterTest {

    private static final int PRE_O = 25; // legacy vibrate(long) branch is the only one testable on the JVM stub jar

    @Test
    public void playTestTone_startsToneAndReturnsTrueWhenAvailable() {
        ToneGenerator toneGenerator = mock(ToneGenerator.class);
        ToneVibrationTester tester = new ToneVibrationTester(toneGenerator, mock(Vibrator.class), PRE_O);

        boolean played = tester.playTestTone();

        assertTrue(played);
        verify(toneGenerator).startTone(ToneGenerator.TONE_PROP_BEEP, 500);
    }

    @Test
    public void playTestTone_falseWhenToneGeneratorUnavailable() {
        ToneVibrationTester tester = new ToneVibrationTester(null, mock(Vibrator.class), PRE_O);

        assertFalse(tester.playTestTone());
    }

    @Test
    public void vibrateOnce_returnsFalseWhenNoVibrator() {
        ToneVibrationTester tester = new ToneVibrationTester(mock(ToneGenerator.class), null, PRE_O);

        assertFalse(tester.vibrateOnce(500));
    }

    @Test
    public void vibrateOnce_vibratesAndReturnsTrueWhenAvailable() {
        Vibrator vibrator = mock(Vibrator.class);
        when(vibrator.hasVibrator()).thenReturn(true);
        ToneVibrationTester tester = new ToneVibrationTester(mock(ToneGenerator.class), vibrator, PRE_O);

        boolean vibrated = tester.vibrateOnce(500);

        assertTrue(vibrated);
        verify(vibrator).vibrate(500L);
    }

    @Test
    public void stop_stopsToneAndCancelsVibration() {
        ToneGenerator toneGenerator = mock(ToneGenerator.class);
        Vibrator vibrator = mock(Vibrator.class);
        ToneVibrationTester tester = new ToneVibrationTester(toneGenerator, vibrator, PRE_O);

        tester.stop();

        verify(toneGenerator).stopTone();
        verify(vibrator).cancel();
    }

    @Test
    public void stop_doesNothingWhenBothUnavailable() {
        ToneVibrationTester tester = new ToneVibrationTester(null, null, PRE_O);

        tester.stop(); // must not throw
    }

    @Test
    public void release_releasesToneGeneratorWhenPresent() {
        ToneGenerator toneGenerator = mock(ToneGenerator.class);
        ToneVibrationTester tester = new ToneVibrationTester(toneGenerator, mock(Vibrator.class), PRE_O);

        tester.release();

        verify(toneGenerator).release();
    }

    @Test
    public void release_doesNothingWhenToneGeneratorUnavailable() {
        ToneVibrationTester tester = new ToneVibrationTester(null, mock(Vibrator.class), PRE_O);

        tester.release(); // must not throw
    }
}
