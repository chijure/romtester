package com.qapp.romtester;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.LinkedHashSet;
import java.util.Set;

public class BluetoothTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_PERM = 2;
    private static final int REQUEST_SCAN_PERM = 3;

    private TextView statusText;
    private TextView pairedText;
    private TextView discoveredText;

    private BluetoothAdapter adapter;
    private final Set<String> discoveredDevices = new LinkedHashSet<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    discoveredDevices.add(describeDevice(device));
                    discoveredText.setText(String.join("\n", discoveredDevices));
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                statusText.setText(getString(R.string.bt_status_scan_finished));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_test);

        statusText = findViewById(R.id.text_bt_status);
        pairedText = findViewById(R.id.text_bt_paired);
        discoveredText = findViewById(R.id.text_bt_discovered);
        findViewById(R.id.button_bt_enable).setOnClickListener(this);
        findViewById(R.id.button_bt_scan).setOnClickListener(this);

        adapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);

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
        if (adapter == null) {
            statusText.setText(getString(R.string.bt_no_hardware));
            return;
        }
        if (!hasConnectPermission()) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_CONNECT_PERM);
            return;
        }
        if (adapter.isEnabled()) {
            statusText.setText(getString(R.string.bt_already_enabled));
            refreshPaired();
            return;
        }
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
    }

    private void startScan() {
        if (adapter == null) {
            statusText.setText(getString(R.string.bt_no_hardware));
            return;
        }
        if (!hasScanPermission()) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_SCAN_PERM);
            return;
        }
        if (!adapter.isEnabled()) {
            statusText.setText(getString(R.string.bt_enable_first));
            return;
        }

        discoveredDevices.clear();
        discoveredText.setText(getString(R.string.common_dash));
        statusText.setText(getString(R.string.bt_status_scanning));
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
        adapter.startDiscovery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            refreshStatus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == REQUEST_CONNECT_PERM && granted) {
            enableBluetooth();
        } else if (requestCode == REQUEST_SCAN_PERM && granted) {
            startScan();
        } else if (!granted) {
            statusText.setText(getString(R.string.bt_permission_denied));
        }
    }

    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasScanPermission() {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshStatus() {
        if (adapter == null) {
            statusText.setText(getString(R.string.bt_no_hardware));
            return;
        }
        statusText.setText(adapter.isEnabled() ? getString(R.string.bt_status_enabled) : getString(R.string.bt_status_disabled));
        refreshPaired();
    }

    private void refreshPaired() {
        if (adapter == null || !adapter.isEnabled() || !hasConnectPermission()) {
            pairedText.setText(getString(R.string.common_dash));
            return;
        }
        try {
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            if (bonded.isEmpty()) {
                pairedText.setText(getString(R.string.bt_no_paired));
            } else {
                StringBuilder sb = new StringBuilder();
                for (BluetoothDevice device : bonded) {
                    sb.append(describeDevice(device)).append("\n");
                }
                pairedText.setText(sb.toString().trim());
            }
        } catch (SecurityException e) {
            pairedText.setText(getString(R.string.bt_permission_error_format, e.getMessage()));
        }
    }

    private String describeDevice(BluetoothDevice device) {
        try {
            String name = device.getName();
            return (name != null ? name : getString(R.string.bt_device_unknown_name)) + "  " + device.getAddress();
        } catch (SecurityException e) {
            return device.getAddress();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if (adapter != null && hasScanPermission() && adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
    }
}
