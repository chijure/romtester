package com.qapp.romtester;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.util.List;

public class WifiTester {

    public interface Listener {
        void onWifiDisabled();

        void onWifiStatus(String ssidOrNull);

        void onScanNeedsPermission();

        void onScanStarting();

        void onScanFailedToStart();

        void onScanResults(List<ScanResult> results);
    }

    private final Activity activity;
    private final WifiManager wifiManager;
    private final Listener listener;
    private final int sdkInt;

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            listener.onScanResults(getScanResults());
        }
    };

    public WifiTester(Activity activity, Listener listener) {
        this(activity, (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE),
                listener);
    }

    WifiTester(Activity activity, WifiManager wifiManager, Listener listener) {
        this(activity, wifiManager, listener, Build.VERSION.SDK_INT);
    }

    WifiTester(Activity activity, WifiManager wifiManager, Listener listener, int sdkInt) {
        this.activity = activity;
        this.wifiManager = wifiManager;
        this.listener = listener;
        this.sdkInt = sdkInt;
    }

    public void start() {
        ReceiverUtils.register(activity, scanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), sdkInt);
    }

    public void stop() {
        ReceiverUtils.unregisterQuietly(activity, scanReceiver);
    }

    public void refreshStatus() {
        if (!wifiManager.isWifiEnabled()) {
            listener.onWifiDisabled();
            return;
        }
        WifiInfo info = wifiManager.getConnectionInfo();
        listener.onWifiStatus(info != null ? info.getSSID() : null);
    }

    public void requestScan() {
        if (!wifiManager.isWifiEnabled()) {
            listener.onWifiDisabled();
            return;
        }
        if (!PermissionUtils.has(activity, Manifest.permission.ACCESS_FINE_LOCATION, sdkInt)) {
            listener.onScanNeedsPermission();
            return;
        }
        listener.onScanStarting();
        boolean started = wifiManager.startScan();
        if (!started) {
            listener.onScanFailedToStart();
            listener.onScanResults(getScanResults());
        }
    }

    @SuppressWarnings("deprecation")
    public List<ScanResult> getScanResults() {
        return wifiManager.getScanResults();
    }
}
