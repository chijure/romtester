package com.qapp.romtester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.pm.PackageManager;
import android.os.Vibrator;

import org.junit.Test;

public class HardwareCapabilitiesTest {

    /**
     * hasCamera's sdkInt >= GINGERBREAD branch calls the real, static
     * Camera.getNumberOfCameras(), which throws on the JVM unit-test stub jar
     * (no device/Robolectric here) - only the legacy PackageManager-feature
     * branch below is exercisable on the host JVM.
     */
    @Test
    public void hasCamera_belowGingerbread_fallsBackToPackageManagerFeature() {
        PackageManager pm = mock(PackageManager.class);
        when(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)).thenReturn(true);

        assertTrue(HardwareCapabilities.hasCamera(pm, 8));
    }

    @Test
    public void hasCamera_belowGingerbread_falseWhenFeatureMissing() {
        PackageManager pm = mock(PackageManager.class);
        when(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)).thenReturn(false);

        assertFalse(HardwareCapabilities.hasCamera(pm, 8));
    }

    @Test
    public void hasVibrator_falseWhenVibratorIsNull() {
        assertFalse(HardwareCapabilities.hasVibrator(null, 23));
    }

    @Test
    public void hasVibrator_belowApi11_trueWheneverVibratorPresent() {
        Vibrator vibrator = mock(Vibrator.class);
        when(vibrator.hasVibrator()).thenReturn(false);

        assertTrue(HardwareCapabilities.hasVibrator(vibrator, 10));
    }

    @Test
    public void hasVibrator_api11AndAbove_delegatesToHasVibrator() {
        Vibrator vibrator = mock(Vibrator.class);
        when(vibrator.hasVibrator()).thenReturn(false);

        assertFalse(HardwareCapabilities.hasVibrator(vibrator, 23));
    }
}
