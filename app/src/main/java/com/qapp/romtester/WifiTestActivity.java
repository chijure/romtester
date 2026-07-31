package com.qapp.romtester;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class WifiTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_LOCATION_FOR_SCAN = 1;

    private TextView statusText;
    private TextView resultsText;
    private WifiManager wifiManager;

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showScanResults();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_test);

        statusText = findViewById(R.id.text_wifi_status);
        resultsText = findViewById(R.id.text_wifi_results);
        findViewById(R.id.button_wifi_scan).setOnClickListener(this);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        IntentFilter scanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(scanReceiver, scanFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(scanReceiver, scanFilter);
        }

        refreshStatus();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_wifi_scan) {
            startScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_FOR_SCAN
                && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScan();
        }
    }

    private void refreshStatus() {
        if (!wifiManager.isWifiEnabled()) {
            statusText.setText(getString(R.string.wifi_disabled));
            return;
        }
        String ssid = wifiManager.getConnectionInfo() != null
                ? wifiManager.getConnectionInfo().getSSID() : getString(R.string.wifi_ssid_unknown);
        statusText.setText(getString(R.string.wifi_connected_format, ssid));
    }

    @SuppressWarnings("deprecation")
    private void startScan() {
        if (!wifiManager.isWifiEnabled()) {
            statusText.setText(getString(R.string.wifi_disabled));
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_FOR_SCAN);
            return;
        }

        statusText.setText(getString(R.string.wifi_status_scanning));
        boolean started = wifiManager.startScan();
        if (!started) {
            statusText.setText(getString(R.string.wifi_scan_failed));
            showScanResults();
        }
    }

    @SuppressWarnings("deprecation")
    private void showScanResults() {
        List<ScanResult> results = wifiManager.getScanResults();
        if (results == null || results.isEmpty()) {
            resultsText.setText(getString(R.string.wifi_no_networks));
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (ScanResult result : results) {
            String ssid = result.SSID == null || result.SSID.length() == 0 ? getString(R.string.wifi_ssid_hidden) : result.SSID;
            sb.append(String.format(Locale.US, getString(R.string.wifi_result_line_format), ssid, result.level));
        }
        resultsText.setText(sb.toString().trim());
        refreshStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scanReceiver);
    }
}
