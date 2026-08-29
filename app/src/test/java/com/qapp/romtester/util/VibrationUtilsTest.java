package com.qapp.romtester.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.Vibrator;

import org.junit.Test;

public class VibrationUtilsTest {

    /**
     * The sdkInt >= O branch calls the real, static VibrationEffect.createOneShot(...),
     * which throws on the JVM unit-test stub jar (no device/Robolectric here) - only the
     * legacy vibrate(long) branch below is exercisable on the host JVM.
     */
    @Test
    public void vibrate_belowApiO_usesLegacyVibrateMillis() {
        Vibrator vibrator = mock(Vibrator.class);

        VibrationUtils.vibrate(vibrator, 500L, 25);

        verify(vibrator).vibrate(500L);
    }
}
