package com.qapp.romtester.connectivity;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.BluetoothTester;
import com.qapp.romtester.util.PermissionUtils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class BluetoothTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CONNECT_PERM = 2;
    private static final int REQUEST_SCAN_PERM = 3;

    private TextView statusText;
    private TextView pairedText;
    private TextView discoveredText;

    private BluetoothTester tester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_test);

        statusText = findViewById(R.id.text_bt_status);
        pairedText = findViewById(R.id.text_bt_paired);
        discoveredText = findViewById(R.id.text_bt_discovered);
        findViewById(R.id.button_bt_enable).setOnClickListener(this);
        findViewById(R.id.button_bt_scan).setOnClickListener(this);

        tester = new BluetoothTester(this, getString(R.string.bt_device_unknown_name), new BluetoothTester.Listener() {
            @Override
            public void onDeviceDiscovered(List<String> discoveredDescriptions) {
                discoveredText.setText(joinLines(discoveredDescriptions));
            }

            @Override
            public void onScanFinished() {
                statusText.setText(getString(R.string.bt_status_scan_finished));
            }
        });
        tester.start();

        refreshStatus();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_bt_enable) {
            enableBluetooth();
        } else if (id == R.id.button_bt_scan) {
            startScan();
        }
    }

    private void enableBluetooth() {
        if (!tester.hasHardware()) {
            statusText.setText(getString(R.string.bt_no_hardware));
            return;
        }
        if (!tester.hasConnectPermission()) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_CONNECT_PERM);
            return;
        }
        if (tester.isEnabled()) {
            statusText.setText(getString(R.string.bt_already_enabled));
            renderPaired();
            return;
        }
        tester.requestEnable();
    }

    private void startScan() {
        if (!tester.hasHardware()) {
            statusText.setText(getString(R.string.bt_no_hardware));
            return;
        }
        if (!tester.hasScanPermission()) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_SCAN_PERM);
            return;
        }
        if (!tester.isEnabled()) {
            statusText.setText(getString(R.string.bt_enable_first));
            return;
        }

        discoveredText.setText(getString(R.string.common_dash));
        statusText.setText(getString(R.string.bt_status_scanning));
        tester.startScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothTester.REQUEST_ENABLE_BT) {
            refreshStatus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = PermissionUtils.granted(grantResults);
        if (requestCode == REQUEST_CONNECT_PERM && granted) {
            enableBluetooth();
        } else if (requestCode == REQUEST_SCAN_PERM && granted) {
            startScan();
        } else if (!granted) {
            statusText.setText(getString(R.string.bt_permission_denied));
        }
    }

    private void refreshStatus() {
        if (!tester.hasHardware()) {
            statusText.setText(getString(R.string.bt_no_hardware));
            return;
        }
        statusText.setText(tester.isEnabled() ? getString(R.string.bt_status_enabled) : getString(R.string.bt_status_disabled));
        renderPaired();
    }

    private void renderPaired() {
        BluetoothTester.PairedDevicesResult result = tester.readPairedDevices();
        if (result.unavailable) {
            pairedText.setText(getString(R.string.common_dash));
        } else if (result.permissionErrorMessage != null) {
            pairedText.setText(getString(R.string.bt_permission_error_format, result.permissionErrorMessage));
        } else if (result.devices.isEmpty()) {
            pairedText.setText(getString(R.string.bt_no_paired));
        } else {
            pairedText.setText(joinLines(result.devices));
        }
    }

    private static String joinLines(List<String> lines) {
        StringBuilder joined = new StringBuilder();
        for (String line : lines) {
            if (joined.length() > 0) {
                joined.append("\n");
            }
            joined.append(line);
        }
        return joined.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tester.stop();
    }
}
