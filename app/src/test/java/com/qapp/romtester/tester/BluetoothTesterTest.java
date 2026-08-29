package com.qapp.romtester.tester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.pm.PackageManager;

import org.junit.Test;

/**
 * BluetoothAdapter and BluetoothDevice are both final classes, so they can't be mocked or
 * constructed here without mockito-inline (no device/Robolectric in this test) - these tests
 * cover the null-adapter ("no hardware") path and the permission-gating logic, which is
 * everything reachable without a real/mock adapter. The scan/paired-device/enable flows stay
 * manually/device-tested, same category of limitation as elsewhere in this codebase.
 */
public class BluetoothTesterTest {

    private static final String CONNECT_PERMISSION = "android.permission.BLUETOOTH_CONNECT";
    private static final String SCAN_PERMISSION = "android.permission.BLUETOOTH_SCAN";
    private static final int PRE_S = 30;
    private static final int POST_S = 31;

    @Test
    public void hasHardware_falseWhenAdapterNull() {
        BluetoothTester tester = new BluetoothTester(mock(Activity.class), null, "unknown", mock(BluetoothTester.Listener.class), POST_S);

        assertFalse(tester.hasHardware());
        assertFalse(tester.isEnabled());
    }

    @Test
    public void readPairedDevices_unavailableWhenAdapterNull() {
        BluetoothTester tester = new BluetoothTester(mock(Activity.class), null, "unknown", mock(BluetoothTester.Listener.class), POST_S);

        BluetoothTester.PairedDevicesResult result = tester.readPairedDevices();

        assertTrue(result.unavailable);
        assertTrue(result.devices.isEmpty());
    }

    @Test
    public void hasConnectPermission_alwaysTrueBelowApi31() {
        BluetoothTester tester = new BluetoothTester(mock(Activity.class), null, "unknown", mock(BluetoothTester.Listener.class), PRE_S);

        assertTrue(tester.hasConnectPermission());
    }

    @Test
    public void hasConnectPermission_checksSelfPermissionAtApi31AndAbove() {
        Activity activity = mock(Activity.class);
        when(activity.checkSelfPermission(CONNECT_PERMISSION)).thenReturn(PackageManager.PERMISSION_DENIED);
        BluetoothTester tester = new BluetoothTester(activity, null, "unknown", mock(BluetoothTester.Listener.class), POST_S);

        assertFalse(tester.hasConnectPermission());
    }

    @Test
    public void hasScanPermission_alwaysTrueBelowApi31() {
        BluetoothTester tester = new BluetoothTester(mock(Activity.class), null, "unknown", mock(BluetoothTester.Listener.class), PRE_S);

        assertTrue(tester.hasScanPermission());
    }

    @Test
    public void hasScanPermission_checksSelfPermissionAtApi31AndAbove() {
        Activity activity = mock(Activity.class);
        when(activity.checkSelfPermission(SCAN_PERMISSION)).thenReturn(PackageManager.PERMISSION_GRANTED);
        BluetoothTester tester = new BluetoothTester(activity, null, "unknown", mock(BluetoothTester.Listener.class), POST_S);

        assertTrue(tester.hasScanPermission());
    }
}
