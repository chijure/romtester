package com.qapp.romtester.tester;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Environment;

import com.qapp.romtester.util.StorageUtils;

public class DeviceInfoTester {

    public static class BatteryInfo {
        public final boolean available;
        public final int percent;
        public final int pluggedState;

        BatteryInfo(boolean available, int percent, int pluggedState) {
            this.available = available;
            this.percent = percent;
            this.pluggedState = pluggedState;
        }
    }

    public static class NetworkSummary {
        public final boolean wifiEnabled;
        public final String wifiSsidOrNull;
        public final String activeConnectionTypeNameOrNull;

        NetworkSummary(boolean wifiEnabled, String wifiSsidOrNull, String activeConnectionTypeNameOrNull) {
            this.wifiEnabled = wifiEnabled;
            this.wifiSsidOrNull = wifiSsidOrNull;
            this.activeConnectionTypeNameOrNull = activeConnectionTypeNameOrNull;
        }
    }

    private final Context context;
    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;

    public DeviceInfoTester(Context context) {
        this(context,
                (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE),
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
    }

    DeviceInfoTester(Context context, WifiManager wifiManager, ConnectivityManager connectivityManager) {
        this.context = context;
        this.wifiManager = wifiManager;
        this.connectivityManager = connectivityManager;
    }

    public BatteryInfo readBattery() {
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            return new BatteryInfo(false, -1, -1);
        }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        int percent = (level >= 0 && scale > 0) ? Math.round(100f * level / scale) : -1;
        return new BatteryInfo(true, percent, plugged);
    }

    public long availableStorageBytes() {
        return StorageUtils.availableBytes(Environment.getDataDirectory());
    }

    public long totalStorageBytes() {
        return StorageUtils.totalBytes(Environment.getDataDirectory());
    }

    public NetworkSummary readNetwork() {
        boolean wifiEnabled = wifiManager != null && wifiManager.isWifiEnabled();
        String ssid = null;
        if (wifiEnabled) {
            WifiInfo info = wifiManager.getConnectionInfo();
            ssid = info != null ? info.getSSID() : null;
        }
        android.net.NetworkInfo active = connectivityManager != null
                ? connectivityManager.getActiveNetworkInfo() : null;
        String activeType = (active != null && active.isConnected()) ? active.getTypeName() : null;
        return new NetworkSummary(wifiEnabled, ssid, activeType);
    }
}
