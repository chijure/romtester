package com.qapp.romtester;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class DeviceInfoActivity extends Activity implements View.OnClickListener {

    private TextView deviceText;
    private TextView batteryText;
    private TextView storageText;
    private TextView networkText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        deviceText = findViewById(R.id.text_device);
        batteryText = findViewById(R.id.text_battery);
        storageText = findViewById(R.id.text_storage);
        networkText = findViewById(R.id.text_network);
        findViewById(R.id.button_refresh).setOnClickListener(this);

        refresh();
    }

    @Override
    public void onClick(View view) {
        refresh();
    }

    private void refresh() {
        deviceText.setText(buildDeviceInfo());
        batteryText.setText(buildBatteryInfo());
        storageText.setText(buildStorageInfo());
        networkText.setText(buildNetworkInfo());
    }

    private String buildDeviceInfo() {
        return getString(R.string.device_info_device_format,
                Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.FINGERPRINT);
    }

    private String buildBatteryInfo() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = registerReceiver(null, filter);
        if (battery == null) {
            return getString(R.string.device_info_battery_unavailable);
        }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        int percent = (level >= 0 && scale > 0) ? Math.round(100f * level / scale) : -1;

        String source;
        if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
            source = getString(R.string.device_info_source_ac);
        } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
            source = getString(R.string.device_info_source_usb);
        } else {
            source = getString(R.string.device_info_source_none);
        }

        String levelStr = percent >= 0
                ? getString(R.string.device_info_level_percent_format, percent)
                : getString(R.string.device_info_level_unknown);
        return getString(R.string.device_info_battery_format, levelStr, source);
    }

    private String buildStorageInfo() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        long totalBytes;
        long freeBytes;
        if (Build.VERSION.SDK_INT >= 18) {
            totalBytes = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
            freeBytes = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        } else {
            totalBytes = (long) statFs.getBlockCount() * statFs.getBlockSize();
            freeBytes = (long) statFs.getAvailableBlocks() * statFs.getBlockSize();
        }
        return String.format(Locale.US, getString(R.string.device_info_storage_format),
                freeBytes / (1024f * 1024f), totalBytes / (1024f * 1024f));
    }

    private String buildNetworkInfo() {
        StringBuilder sb = new StringBuilder();

        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        boolean wifiEnabled = wifiManager != null && wifiManager.isWifiEnabled();
        sb.append(getString(R.string.device_info_wifi_line_format,
                wifiEnabled ? getString(R.string.device_info_state_enabled) : getString(R.string.device_info_state_disabled)));
        if (wifiEnabled && wifiManager.getConnectionInfo() != null) {
            sb.append(getString(R.string.device_info_wifi_ssid_suffix, wifiManager.getConnectionInfo().getSSID()));
        }

        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager != null
                ? connectivityManager.getActiveNetworkInfo() : null;
        sb.append(getString(R.string.device_info_connection_label));
        if (activeNetwork != null && activeNetwork.isConnected()) {
            sb.append(activeNetwork.getTypeName());
        } else {
            sb.append(getString(R.string.device_info_no_connection));
        }

        return sb.toString();
    }
}
