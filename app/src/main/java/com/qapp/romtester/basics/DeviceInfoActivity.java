package com.qapp.romtester.basics;

import android.app.Activity;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.DeviceInfoTester;

import java.util.Locale;

public class DeviceInfoActivity extends Activity implements View.OnClickListener {

    private TextView deviceText;
    private TextView batteryText;
    private TextView storageText;
    private TextView networkText;
    private DeviceInfoTester tester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        deviceText = findViewById(R.id.text_device);
        batteryText = findViewById(R.id.text_battery);
        storageText = findViewById(R.id.text_storage);
        networkText = findViewById(R.id.text_network);
        findViewById(R.id.button_refresh).setOnClickListener(this);

        tester = new DeviceInfoTester(this);
        refresh();
    }

    @Override
    public void onClick(View view) {
        refresh();
    }

    private void refresh() {
        deviceText.setText(getString(R.string.device_info_device_format,
                Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.FINGERPRINT));
        renderBattery(tester.readBattery());
        renderStorage();
        renderNetwork(tester.readNetwork());
    }

    private void renderBattery(DeviceInfoTester.BatteryInfo info) {
        if (!info.available) {
            batteryText.setText(getString(R.string.device_info_battery_unavailable));
            return;
        }
        String source;
        if (info.pluggedState == BatteryManager.BATTERY_PLUGGED_AC) {
            source = getString(R.string.device_info_source_ac);
        } else if (info.pluggedState == BatteryManager.BATTERY_PLUGGED_USB) {
            source = getString(R.string.device_info_source_usb);
        } else {
            source = getString(R.string.device_info_source_none);
        }
        String levelStr = info.percent >= 0
                ? getString(R.string.device_info_level_percent_format, info.percent)
                : getString(R.string.device_info_level_unknown);
        batteryText.setText(getString(R.string.device_info_battery_format, levelStr, source));
    }

    private void renderStorage() {
        long free = tester.availableStorageBytes();
        long total = tester.totalStorageBytes();
        storageText.setText(String.format(Locale.US, getString(R.string.device_info_storage_format),
                free / (1024f * 1024f), total / (1024f * 1024f)));
    }

    private void renderNetwork(DeviceInfoTester.NetworkSummary info) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.device_info_wifi_line_format, info.wifiEnabled
                ? getString(R.string.device_info_state_enabled) : getString(R.string.device_info_state_disabled)));
        if (info.wifiEnabled && info.wifiSsidOrNull != null) {
            sb.append(getString(R.string.device_info_wifi_ssid_suffix, info.wifiSsidOrNull));
        }
        sb.append(getString(R.string.device_info_connection_label));
        sb.append(info.activeConnectionTypeNameOrNull != null
                ? info.activeConnectionTypeNameOrNull : getString(R.string.device_info_no_connection));
        networkText.setText(sb.toString());
    }
}
