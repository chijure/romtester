package com.qapp.romtester.tester;

import static org.junit.Assert.assertFalse;

import android.app.Activity;

import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * CameraManager, CameraCharacteristics, and the legacy Camera class all end up touching real
 * (non-mocked) Android code paths that throw on the JVM unit-test stub jar - and the capture
 * flow constructs a real android.content.Intent, same limitation as PhoneTester. The one thing
 * reachable here without a device/Robolectric is the initial torch state, using a pre-M sdkInt
 * so the constructor skips camera2 torch-id lookup entirely.
 */
public class CameraTesterTest {

    @Test
    public void isTorchOn_falseInitially() {
        CameraTester tester = new CameraTester(mock(Activity.class), mock(CameraTester.Listener.class), 21);

        assertFalse(tester.isTorchOn());
    }
}
