package com.qapp.romtester;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.junit.Test;

public class WifiTesterTest {

    private static final String LOCATION_PERMISSION = "android.permission.ACCESS_FINE_LOCATION";
    private static final int POST_M = 23;

    @Test
    public void refreshStatus_reportsDisabledWhenWifiOff() {
        Activity activity = mock(Activity.class);
        WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.isWifiEnabled()).thenReturn(false);
        WifiTester.Listener listener = mock(WifiTester.Listener.class);
        WifiTester tester = new WifiTester(activity, wifiManager, listener);

        tester.refreshStatus();

        verify(listener).onWifiDisabled();
        verify(listener, never()).onWifiStatus(any());
    }

    @Test
    public void refreshStatus_reportsSsidWhenConnected() {
        Activity activity = mock(Activity.class);
        WifiManager wifiManager = mock(WifiManager.class);
        WifiInfo info = mock(WifiInfo.class);
        when(wifiManager.isWifiEnabled()).thenReturn(true);
        when(wifiManager.getConnectionInfo()).thenReturn(info);
        when(info.getSSID()).thenReturn("MyNetwork");
        WifiTester.Listener listener = mock(WifiTester.Listener.class);
        WifiTester tester = new WifiTester(activity, wifiManager, listener);

        tester.refreshStatus();

        verify(listener).onWifiStatus("MyNetwork");
    }

    @Test
    public void refreshStatus_reportsNullSsidWhenConnectionInfoMissing() {
        Activity activity = mock(Activity.class);
        WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.isWifiEnabled()).thenReturn(true);
        when(wifiManager.getConnectionInfo()).thenReturn(null);
        WifiTester.Listener listener = mock(WifiTester.Listener.class);
        WifiTester tester = new WifiTester(activity, wifiManager, listener);

        tester.refreshStatus();

        verify(listener).onWifiStatus(null);
    }

    @Test
    public void requestScan_reportsDisabledWithoutCheckingPermission() {
        Activity activity = mock(Activity.class);
        WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.isWifiEnabled()).thenReturn(false);
        WifiTester.Listener listener = mock(WifiTester.Listener.class);
        WifiTester tester = new WifiTester(activity, wifiManager, listener);

        tester.requestScan();

        verify(listener).onWifiDisabled();
        verify(wifiManager, never()).startScan();
    }

    @Test
    public void requestScan_needsPermissionWhenNotGranted() {
        Activity activity = mock(Activity.class);
        WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.isWifiEnabled()).thenReturn(true);
        when(activity.checkSelfPermission(LOCATION_PERMISSION)).thenReturn(PackageManager.PERMISSION_DENIED);
        WifiTester.Listener listener = mock(WifiTester.Listener.class);
        WifiTester tester = new WifiTester(activity, wifiManager, listener, POST_M);

        tester.requestScan();

        verify(listener).onScanNeedsPermission();
        verify(wifiManager, never()).startScan();
    }

    @Test
    public void requestScan_startsScanWhenPermissionGranted() {
        Activity activity = mock(Activity.class);
        WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.isWifiEnabled()).thenReturn(true);
        when(activity.checkSelfPermission(LOCATION_PERMISSION)).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(wifiManager.startScan()).thenReturn(true);
        WifiTester.Listener listener = mock(WifiTester.Listener.class);
        WifiTester tester = new WifiTester(activity, wifiManager, listener, POST_M);

        tester.requestScan();

        verify(listener).onScanStarting();
        verify(listener, never()).onScanFailedToStart();
    }

    @Test
    public void requestScan_reportsFailureAndResultsWhenStartScanReturnsFalse() {
        Activity activity = mock(Activity.class);
        WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.isWifiEnabled()).thenReturn(true);
        when(activity.checkSelfPermission(LOCATION_PERMISSION)).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(wifiManager.startScan()).thenReturn(false);
        WifiTester.Listener listener = mock(WifiTester.Listener.class);
        WifiTester tester = new WifiTester(activity, wifiManager, listener, POST_M);

        tester.requestScan();

        verify(listener).onScanFailedToStart();
        verify(listener).onScanResults(any());
    }
}
