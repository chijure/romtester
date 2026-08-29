package com.qapp.romtester.tester;

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

import com.qapp.romtester.util.ReceiverUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BluetoothTester {

    public static final int REQUEST_ENABLE_BT = 1;

    public interface Listener {
        void onDeviceDiscovered(List<String> discoveredDescriptions);

        void onScanFinished();
    }

    public static class PairedDevicesResult {
        public final boolean unavailable;
        public final List<String> devices;
        public final String permissionErrorMessage;

        PairedDevicesResult(boolean unavailable, List<String> devices, String permissionErrorMessage) {
            this.unavailable = unavailable;
            this.devices = devices;
            this.permissionErrorMessage = permissionErrorMessage;
        }
    }

    private final Activity activity;
    private final BluetoothAdapter adapter;
    private final String unknownNameFallback;
    private final Listener listener;
    private final int sdkInt;

    private final Set<String> discoveredSet = new LinkedHashSet<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && discoveredSet.add(describeDevice(device))) {
                    listener.onDeviceDiscovered(new ArrayList<>(discoveredSet));
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                listener.onScanFinished();
            }
        }
    };

    public BluetoothTester(Activity activity, String unknownNameFallback, Listener listener) {
        this(activity, BluetoothAdapter.getDefaultAdapter(), unknownNameFallback, listener, Build.VERSION.SDK_INT);
    }

    BluetoothTester(Activity activity, BluetoothAdapter adapter, String unknownNameFallback, Listener listener,
                    int sdkInt) {
        this.activity = activity;
        this.adapter = adapter;
        this.unknownNameFallback = unknownNameFallback;
        this.listener = listener;
        this.sdkInt = sdkInt;
    }

    public boolean hasHardware() {
        return adapter != null;
    }

    public boolean isEnabled() {
        return adapter != null && adapter.isEnabled();
    }

    public boolean hasConnectPermission() {
        if (sdkInt < 31) {
            return true;
        }
        return activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasScanPermission() {
        if (sdkInt < 31) {
            return true;
        }
        return activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestEnable() {
        activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
    }

    public void start() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        ReceiverUtils.register(activity, receiver, filter, sdkInt);
    }

    public void stop() {
        ReceiverUtils.unregisterQuietly(activity, receiver);
        if (adapter != null && hasScanPermission() && adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
    }

    public void startScan() {
        discoveredSet.clear();
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
        adapter.startDiscovery();
    }

    public PairedDevicesResult readPairedDevices() {
        if (adapter == null || !adapter.isEnabled() || !hasConnectPermission()) {
            return new PairedDevicesResult(true, Collections.emptyList(), null);
        }
        try {
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            List<String> devices = new ArrayList<>();
            for (BluetoothDevice device : bonded) {
                devices.add(describeDevice(device));
            }
            return new PairedDevicesResult(false, devices, null);
        } catch (SecurityException e) {
            return new PairedDevicesResult(false, Collections.emptyList(), e.getMessage());
        }
    }

    private String describeDevice(BluetoothDevice device) {
        try {
            String name = device.getName();
            return (name != null ? name : unknownNameFallback) + "  " + device.getAddress();
        } catch (SecurityException e) {
            return device.getAddress();
        }
    }
}
