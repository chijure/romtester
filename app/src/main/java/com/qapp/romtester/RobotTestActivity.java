package com.qapp.romtester;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RobotTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_ALL_PERMISSIONS = 1;

    private enum Status { PASS, FAIL, NOT_APPLICABLE, MANUAL }

    private interface Callback {
        void done(Status status, String detail);
    }

    private abstract class Check {
        final int nameResId;

        Check(int nameResId) {
            this.nameResId = nameResId;
        }

        abstract void run(Callback callback);
    }

    private static class CheckResult {
        final String name;
        final Status status;
        final String detail;

        CheckResult(String name, Status status, String detail) {
            this.name = name;
            this.status = status;
            this.detail = detail;
        }
    }

    private TextView summaryText;
    private TextView logText;
    private ScrollView logScroll;
    private Button runButton;
    private Button shareButton;

    private final Handler handler = new Handler();
    private final List<Check> checks = new ArrayList<>();
    private final List<CheckResult> results = new ArrayList<>();
    private int stepIndex;

    private SensorManager sensorManager;
    private LocationManager locationManager;
    private WifiManager wifiManager;
    private PackageManager pm;
    private TelephonyManager telephonyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_test);

        summaryText = findViewById(R.id.text_robot_summary);
        logText = findViewById(R.id.text_robot_log);
        logScroll = findViewById(R.id.scroll_robot_log);
        runButton = findViewById(R.id.button_robot_run);
        shareButton = findViewById(R.id.button_robot_share);
        runButton.setOnClickListener(this);
        shareButton.setOnClickListener(this);

        pm = getPackageManager();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        buildChecks();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_robot_run) {
            requestPermissionsThenRun();
        } else if (id == R.id.button_robot_share) {
            shareReport();
        }
    }

    private void requestPermissionsThenRun() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startRun();
            return;
        }
        List<String> missing = new ArrayList<>();
        addIfMissing(missing, Manifest.permission.RECORD_AUDIO);
        addIfMissing(missing, Manifest.permission.ACCESS_FINE_LOCATION);
        addIfMissing(missing, Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= 31) {
            addIfMissing(missing, Manifest.permission.BLUETOOTH_SCAN);
            addIfMissing(missing, Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (missing.isEmpty()) {
            startRun();
        } else {
            requestPermissions(missing.toArray(new String[0]), REQUEST_ALL_PERMISSIONS);
        }
    }

    private void addIfMissing(List<String> list, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            list.add(permission);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            startRun();
        }
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void startRun() {
        results.clear();
        stepIndex = 0;
        logText.setText("");
        shareButton.setEnabled(false);
        runButton.setEnabled(false);
        runButton.setText(getString(R.string.robot_button_running));
        summaryText.setText(getString(R.string.robot_running_format, 0, checks.size()));
        runNext();
    }

    private void runNext() {
        if (stepIndex >= checks.size()) {
            finishRun();
            return;
        }
        Check check = checks.get(stepIndex);
        summaryText.setText(getString(R.string.robot_running_format, stepIndex + 1, checks.size()));
        check.run((status, detail) -> {
            results.add(new CheckResult(getString(check.nameResId), status, detail));
            appendResultLine(results.get(results.size() - 1));
            stepIndex++;
            runNext();
        });
    }

    private void appendResultLine(CheckResult result) {
        SpannableStringBuilder builder = new SpannableStringBuilder(logText.getText());
        ReportRenderer.appendLine(this, builder, result.name, result.status.name(), result.detail);
        logText.setText(builder);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void finishRun() {
        int pass = 0;
        int fail = 0;
        int na = 0;
        int manual = 0;
        List<TestReport.CheckEntry> entries = new ArrayList<>();
        for (CheckResult r : results) {
            switch (r.status) {
                case PASS:
                    pass++;
                    break;
                case FAIL:
                    fail++;
                    break;
                case MANUAL:
                    manual++;
                    break;
                default:
                    na++;
                    break;
            }
            entries.add(new TestReport.CheckEntry(r.name, r.status.name(), r.detail));
        }
        summaryText.setText(getString(R.string.robot_summary_format, pass, fail, na, manual));
        runButton.setEnabled(true);
        runButton.setText(getString(R.string.robot_button_run_again));
        shareButton.setEnabled(true);

        try {
            TestReport report = new TestReport(System.currentTimeMillis(),
                    Build.MANUFACTURER + " " + Build.MODEL, Build.VERSION.RELEASE, Build.FINGERPRINT,
                    pass, fail, na, manual, entries);
            ReportStore.saveReport(this, report);
        } catch (Exception ignored) {
            // If saving fails (rare I/O issue) the on-screen report is still shown and shareable.
        }
    }

    private void shareReport() {
        int pass = 0;
        int fail = 0;
        int na = 0;
        int manual = 0;
        StringBuilder body = new StringBuilder();
        for (CheckResult r : results) {
            String tag;
            switch (r.status) {
                case PASS:
                    tag = getString(R.string.robot_tag_pass);
                    pass++;
                    break;
                case FAIL:
                    tag = getString(R.string.robot_tag_fail);
                    fail++;
                    break;
                case MANUAL:
                    tag = getString(R.string.robot_tag_manual);
                    manual++;
                    break;
                default:
                    tag = getString(R.string.robot_tag_na);
                    na++;
                    break;
            }
            body.append("[").append(tag).append("] ").append(r.name).append(" - ").append(r.detail).append("\n");
        }

        String header = getString(R.string.robot_share_header_format,
                Build.MANUFACTURER + " " + Build.MODEL, Build.VERSION.RELEASE,
                pass, fail, na, manual);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.robot_share_subject));
        intent.putExtra(Intent.EXTRA_TEXT, header + body);
        startActivity(Intent.createChooser(intent, getString(R.string.robot_button_share)));
    }

    private void buildChecks() {
        checks.add(new Check(R.string.robot_check_sound) {
            @Override
            void run(Callback cb) {
                try {
                    ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 400);
                    handler.postDelayed(() -> {
                        tone.release();
                        cb.done(Status.PASS, getString(R.string.robot_detail_sound_played));
                    }, 500);
                } catch (Exception e) {
                    cb.done(Status.FAIL, getString(R.string.robot_detail_sound_failed));
                }
            }
        });

        checks.add(new Check(R.string.robot_check_vibration) {
            @Override
            void run(Callback cb) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                boolean hasVibrator = vibrator != null
                        && (Build.VERSION.SDK_INT < 11 || vibrator.hasVibrator());
                if (!hasVibrator) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_no_vibrator));
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(300);
                }
                cb.done(Status.PASS, getString(R.string.robot_detail_vibration_triggered));
            }
        });

        checks.add(new Check(R.string.robot_check_touch) {
            @Override
            void run(Callback cb) {
                cb.done(Status.MANUAL, getString(R.string.robot_detail_manual_touch));
            }
        });

        checks.add(new Check(R.string.robot_check_screen) {
            @Override
            void run(Callback cb) {
                cb.done(Status.MANUAL, getString(R.string.robot_detail_manual_screen));
            }
        });

        addSensorCheck(R.string.robot_check_accelerometer, Sensor.TYPE_ACCELEROMETER);
        addSensorCheck(R.string.robot_check_gyroscope, Sensor.TYPE_GYROSCOPE);
        addSensorCheck(R.string.robot_check_magnetic, Sensor.TYPE_MAGNETIC_FIELD);
        addSensorCheck(R.string.robot_check_light, Sensor.TYPE_LIGHT);
        addSensorCheck(R.string.robot_check_proximity, Sensor.TYPE_PROXIMITY);

        checks.add(new Check(R.string.robot_check_camera) {
            @Override
            void run(Callback cb) {
                boolean hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
                if (!hasCamera) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_camera_not_present));
                    return;
                }
                boolean hasFlash = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
                cb.done(Status.PASS, getString(R.string.robot_detail_camera_present_format,
                        getString(hasFlash ? R.string.robot_detail_yes : R.string.robot_detail_no)));
            }
        });

        checks.add(new Check(R.string.robot_check_device_info) {
            @Override
            void run(Callback cb) {
                cb.done(Status.PASS, buildDeviceInfoSummary());
            }
        });

        checks.add(new Check(R.string.robot_check_microphone) {
            @Override
            void run(Callback cb) {
                if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
                    cb.done(Status.MANUAL, getString(R.string.robot_detail_mic_permission));
                    return;
                }
                try {
                    File outputFile = new File(getCacheDir(), "robot_mic_test.3gp");
                    MediaRecorder recorder = new MediaRecorder();
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    recorder.setOutputFile(outputFile.getAbsolutePath());
                    recorder.prepare();
                    recorder.start();
                    handler.postDelayed(() -> {
                        try {
                            recorder.stop();
                        } catch (RuntimeException ignored) {
                        }
                        recorder.release();
                        long size = outputFile.exists() ? outputFile.length() : 0;
                        if (size > 500) {
                            cb.done(Status.PASS, getString(R.string.robot_detail_mic_recorded_format, size / 1024));
                        } else {
                            cb.done(Status.FAIL, getString(R.string.robot_detail_mic_no_data));
                        }
                    }, 1500);
                } catch (Exception e) {
                    cb.done(Status.FAIL, getString(R.string.robot_detail_mic_no_data));
                }
            }
        });

        checks.add(new Check(R.string.robot_check_multitouch) {
            @Override
            void run(Callback cb) {
                cb.done(Status.MANUAL, getString(R.string.robot_detail_manual_multitouch));
            }
        });

        checks.add(new Check(R.string.robot_check_gps) {
            @Override
            @SuppressWarnings("deprecation")
            void run(Callback cb) {
                if (!pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_gps_not_present));
                    return;
                }
                if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    cb.done(Status.MANUAL, getString(R.string.robot_detail_gps_permission));
                    return;
                }
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    cb.done(Status.FAIL, getString(R.string.robot_detail_gps_disabled));
                    return;
                }

                boolean[] finished = {false};
                LocationListener listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (finished[0]) {
                            return;
                        }
                        finished[0] = true;
                        locationManager.removeUpdates(this);
                        cb.done(Status.PASS, getString(R.string.robot_detail_gps_fix_format,
                                location.getLatitude(), location.getLongitude()));
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                };
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, listener);
                handler.postDelayed(() -> {
                    if (!finished[0]) {
                        finished[0] = true;
                        locationManager.removeUpdates(listener);
                        cb.done(Status.FAIL, getString(R.string.robot_detail_gps_no_fix));
                    }
                }, 8000);
            }
        });

        checks.add(new Check(R.string.robot_check_wifi) {
            @Override
            @SuppressWarnings("deprecation")
            void run(Callback cb) {
                if (!pm.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_wifi_not_present));
                    return;
                }
                if (!wifiManager.isWifiEnabled()) {
                    cb.done(Status.FAIL, getString(R.string.robot_detail_wifi_disabled));
                    return;
                }
                if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    cb.done(Status.MANUAL, getString(R.string.robot_detail_wifi_permission));
                    return;
                }

                boolean[] finished = {false};
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (finished[0]) {
                            return;
                        }
                        finished[0] = true;
                        unregisterReceiver(this);
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        int count = scanResults == null ? 0 : scanResults.size();
                        cb.done(count > 0 ? Status.PASS : Status.FAIL,
                                count > 0 ? getString(R.string.robot_detail_wifi_networks_format, count)
                                        : getString(R.string.robot_detail_wifi_no_networks));
                    }
                };
                IntentFilter wifiFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                if (Build.VERSION.SDK_INT >= 33) {
                    registerReceiver(receiver, wifiFilter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    registerReceiver(receiver, wifiFilter);
                }
                boolean started = wifiManager.startScan();
                handler.postDelayed(() -> {
                    if (!finished[0]) {
                        finished[0] = true;
                        try {
                            unregisterReceiver(receiver);
                        } catch (IllegalArgumentException ignored) {
                        }
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        int count = scanResults == null ? 0 : scanResults.size();
                        cb.done(count > 0 ? Status.PASS : Status.FAIL,
                                count > 0 ? getString(R.string.robot_detail_wifi_networks_format, count)
                                        : getString(R.string.robot_detail_wifi_no_networks));
                    }
                }, 6000);
                if (!started) {
                    // startScan() may return false on throttled devices; the timeout above
                    // still reports whatever results are already cached.
                }
            }
        });

        checks.add(new Check(R.string.robot_check_bluetooth) {
            @Override
            void run(Callback cb) {
                if (!pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_bt_not_present));
                    return;
                }
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_bt_not_present));
                    return;
                }
                boolean hasBtPermission = Build.VERSION.SDK_INT < 31
                        || (hasPermission(Manifest.permission.BLUETOOTH_SCAN)
                        && hasPermission(Manifest.permission.BLUETOOTH_CONNECT));
                if (!hasBtPermission) {
                    cb.done(Status.MANUAL, getString(R.string.robot_detail_bt_permission));
                    return;
                }
                if (!adapter.isEnabled()) {
                    cb.done(Status.FAIL, getString(R.string.robot_detail_bt_disabled));
                    return;
                }

                try {
                    final Set<String> discovered = new LinkedHashSet<>();
                    int pairedCount = adapter.getBondedDevices().size();
                    boolean[] finished = {false};
                    BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                if (device != null) {
                                    discovered.add(device.getAddress());
                                }
                            }
                        }
                    };
                    IntentFilter btFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    if (Build.VERSION.SDK_INT >= 33) {
                        registerReceiver(receiver, btFilter, Context.RECEIVER_NOT_EXPORTED);
                    } else {
                        registerReceiver(receiver, btFilter);
                    }
                    adapter.startDiscovery();
                    handler.postDelayed(() -> {
                        if (finished[0]) {
                            return;
                        }
                        finished[0] = true;
                        adapter.cancelDiscovery();
                        unregisterReceiver(receiver);
                        cb.done(Status.PASS, getString(R.string.robot_detail_bt_devices_format,
                                pairedCount, discovered.size()));
                    }, 6000);
                } catch (SecurityException e) {
                    cb.done(Status.MANUAL, getString(R.string.robot_detail_bt_permission));
                }
            }
        });

        checks.add(new Check(R.string.robot_check_nfc) {
            @Override
            void run(Callback cb) {
                if (Build.VERSION.SDK_INT < 9 || !pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_nfc_not_present));
                    return;
                }
                android.nfc.NfcAdapter adapter = android.nfc.NfcAdapter.getDefaultAdapter(RobotTestActivity.this);
                if (adapter == null) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_nfc_not_present));
                    return;
                }
                if (adapter.isEnabled()) {
                    cb.done(Status.PASS, getString(R.string.robot_detail_nfc_enabled));
                } else {
                    cb.done(Status.FAIL, getString(R.string.robot_detail_nfc_disabled));
                }
            }
        });

        checks.add(new Check(R.string.robot_check_opengl) {
            @Override
            void run(Callback cb) {
                ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                ConfigurationInfo info = am.getDeviceConfigurationInfo();
                cb.done(Status.PASS, getString(R.string.robot_detail_opengl_format, info.getGlEsVersion()));
            }
        });

        checks.add(new Check(R.string.robot_check_video) {
            @Override
            void run(Callback cb) {
                if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_video_not_present));
                    return;
                }
                boolean hasAvc = false;
                if (Build.VERSION.SDK_INT >= 21) {
                    MediaCodecList list = new MediaCodecList(MediaCodecList.ALL_CODECS);
                    for (MediaCodecInfo codecInfo : list.getCodecInfos()) {
                        if (!codecInfo.isEncoder()) {
                            continue;
                        }
                        for (String type : codecInfo.getSupportedTypes()) {
                            if (type.equalsIgnoreCase("video/avc")) {
                                hasAvc = true;
                            }
                        }
                    }
                } else {
                    hasAvc = true;
                }
                cb.done(hasAvc ? Status.PASS : Status.FAIL, getString(hasAvc
                        ? R.string.robot_detail_video_codec_found : R.string.robot_detail_video_codec_missing));
            }
        });

        checks.add(new Check(R.string.robot_check_phone) {
            @Override
            void run(Callback cb) {
                if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_phone_not_present));
                    return;
                }
                if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
                    cb.done(Status.MANUAL, getString(R.string.robot_detail_phone_permission));
                    return;
                }
                boolean ready = telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
                cb.done(ready ? Status.PASS : Status.FAIL, ready
                        ? getString(R.string.robot_detail_phone_ready_format, telephonyManager.getNetworkOperatorName())
                        : getString(R.string.robot_detail_phone_not_ready));
            }
        });
    }

    private void addSensorCheck(int nameResId, int sensorType) {
        checks.add(new Check(nameResId) {
            @Override
            void run(Callback cb) {
                Sensor sensor = sensorManager.getDefaultSensor(sensorType);
                if (sensor == null) {
                    cb.done(Status.NOT_APPLICABLE, getString(R.string.robot_detail_sensor_not_present));
                    return;
                }
                boolean[] finished = {false};
                SensorEventListener listener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        if (finished[0]) {
                            return;
                        }
                        finished[0] = true;
                        sensorManager.unregisterListener(this);
                        StringBuilder values = new StringBuilder();
                        for (int i = 0; i < event.values.length; i++) {
                            if (i > 0) {
                                values.append(", ");
                            }
                            values.append(String.format(Locale.US, "%.2f", event.values[i]));
                        }
                        cb.done(Status.PASS, getString(R.string.robot_detail_sensor_reading_format, values.toString()));
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    }
                };
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI);
                handler.postDelayed(() -> {
                    if (!finished[0]) {
                        finished[0] = true;
                        sensorManager.unregisterListener(listener);
                        cb.done(Status.FAIL, getString(R.string.robot_detail_sensor_no_reading));
                    }
                }, 1500);
            }
        });
    }

    private String buildDeviceInfoSummary() {
        String model = Build.MANUFACTURER + " " + Build.MODEL;

        int batteryPercent = -1;
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                batteryPercent = Math.round(100f * level / scale);
            }
        }

        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        long freeBytes;
        if (Build.VERSION.SDK_INT >= 18) {
            freeBytes = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        } else {
            freeBytes = (long) statFs.getAvailableBlocks() * statFs.getBlockSize();
        }

        return getString(R.string.robot_detail_device_info_format, model, Build.VERSION.RELEASE,
                batteryPercent, freeBytes / (1024f * 1024f));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
