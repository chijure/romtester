package com.qapp.romtester;

import android.Manifest;
import android.app.Activity;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class WifiTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_LOCATION_FOR_SCAN = 1;

    private TextView statusText;
    private TextView resultsText;
    private WifiTester tester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_test);

        statusText = findViewById(R.id.text_wifi_status);
        resultsText = findViewById(R.id.text_wifi_results);
        findViewById(R.id.button_wifi_scan).setOnClickListener(this);

        tester = new WifiTester(this, new WifiTester.Listener() {
            @Override
            public void onWifiDisabled() {
                statusText.setText(getString(R.string.wifi_disabled));
            }

            @Override
            public void onWifiStatus(String ssidOrNull) {
                String ssid = ssidOrNull != null ? ssidOrNull : getString(R.string.wifi_ssid_unknown);
                statusText.setText(getString(R.string.wifi_connected_format, ssid));
            }

            @Override
            public void onScanNeedsPermission() {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_FOR_SCAN);
            }

            @Override
            public void onScanStarting() {
                statusText.setText(getString(R.string.wifi_status_scanning));
            }

            @Override
            public void onScanFailedToStart() {
                statusText.setText(getString(R.string.wifi_scan_failed));
            }

            @Override
            public void onScanResults(List<ScanResult> results) {
                showScanResults(results);
            }
        });
        tester.start();
        tester.refreshStatus();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_wifi_scan) {
            tester.requestScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_FOR_SCAN && PermissionUtils.granted(grantResults)) {
            tester.requestScan();
        }
    }

    private void showScanResults(List<ScanResult> results) {
        if (results == null || results.isEmpty()) {
            resultsText.setText(getString(R.string.wifi_no_networks));
        } else {
            StringBuilder sb = new StringBuilder();
            for (ScanResult result : results) {
                String ssid = result.SSID == null || result.SSID.length() == 0
                        ? getString(R.string.wifi_ssid_hidden) : result.SSID;
                sb.append(String.format(Locale.US, getString(R.string.wifi_result_line_format), ssid, result.level));
            }
            resultsText.setText(sb.toString().trim());
        }
        tester.refreshStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tester.stop();
    }
}
