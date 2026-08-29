package com.qapp.romtester.tester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;

import org.junit.Test;

public class DeviceInfoTesterTest {

    @Test
    public void readBattery_notAvailableWhenStickyIntentMissing() {
        Context context = mock(Context.class);
        when(context.registerReceiver(isNull(), any())).thenReturn(null);
        DeviceInfoTester tester = new DeviceInfoTester(context, mock(WifiManager.class), mock(ConnectivityManager.class));

        DeviceInfoTester.BatteryInfo info = tester.readBattery();

        assertFalse(info.available);
    }

    @Test
    public void readBattery_computesRoundedPercentAndPluggedState() {
        Context context = mock(Context.class);
        Intent battery = mock(Intent.class);
        when(battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(75);
        when(battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100);
        when(battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)).thenReturn(BatteryManager.BATTERY_PLUGGED_USB);
        when(context.registerReceiver(isNull(), any())).thenReturn(battery);
        DeviceInfoTester tester = new DeviceInfoTester(context, mock(WifiManager.class), mock(ConnectivityManager.class));

        DeviceInfoTester.BatteryInfo info = tester.readBattery();

        assertTrue(info.available);
        assertEquals(75, info.percent);
        assertEquals(BatteryManager.BATTERY_PLUGGED_USB, info.pluggedState);
    }

    @Test
    public void readBattery_percentUnknownWhenScaleMissing() {
        Context context = mock(Context.class);
        Intent battery = mock(Intent.class);
        when(battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(-1);
        when(battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(-1);
        when(battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)).thenReturn(-1);
        when(context.registerReceiver(isNull(), any())).thenReturn(battery);
        DeviceInfoTester tester = new DeviceInfoTester(context, mock(WifiManager.class), mock(ConnectivityManager.class));

        DeviceInfoTester.BatteryInfo info = tester.readBattery();

        assertTrue(info.available);
        assertEquals(-1, info.percent);
    }

    @Test
    public void readNetwork_wifiDisabled_reportsNoSsid() {
        WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.isWifiEnabled()).thenReturn(false);
        DeviceInfoTester tester = new DeviceInfoTester(mock(Context.class), wifiManager, mock(ConnectivityManager.class));

        DeviceInfoTester.NetworkSummary summary = tester.readNetwork();

        assertFalse(summary.wifiEnabled);
        assertNull(summary.wifiSsidOrNull);
    }

    @Test
    public void readNetwork_wifiEnabled_reportsSsidAndActiveConnection() {
        WifiManager wifiManager = mock(WifiManager.class);
        WifiInfo wifiInfo = mock(WifiInfo.class);
        when(wifiManager.isWifiEnabled()).thenReturn(true);
        when(wifiManager.getConnectionInfo()).thenReturn(wifiInfo);
        when(wifiInfo.getSSID()).thenReturn("HomeNet");

        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        NetworkInfo activeNetwork = mock(NetworkInfo.class);
        when(activeNetwork.isConnected()).thenReturn(true);
        when(activeNetwork.getTypeName()).thenReturn("WIFI");
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(activeNetwork);

        DeviceInfoTester tester = new DeviceInfoTester(mock(Context.class), wifiManager, connectivityManager);

        DeviceInfoTester.NetworkSummary summary = tester.readNetwork();

        assertTrue(summary.wifiEnabled);
        assertEquals("HomeNet", summary.wifiSsidOrNull);
        assertEquals("WIFI", summary.activeConnectionTypeNameOrNull);
    }

    @Test
    public void readNetwork_noActiveConnection_reportsNull() {
        WifiManager wifiManager = mock(WifiManager.class);
        when(wifiManager.isWifiEnabled()).thenReturn(false);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(null);

        DeviceInfoTester tester = new DeviceInfoTester(mock(Context.class), wifiManager, connectivityManager);

        DeviceInfoTester.NetworkSummary summary = tester.readNetwork();

        assertNull(summary.activeConnectionTypeNameOrNull);
    }
}
