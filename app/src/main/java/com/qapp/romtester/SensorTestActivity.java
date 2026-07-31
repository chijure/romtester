package com.qapp.romtester;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

public class SensorTestActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;

    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magneticField;
    private Sensor light;
    private Sensor proximity;

    private TextView accelerometerText;
    private TextView gyroscopeText;
    private TextView magneticText;
    private TextView lightText;
    private TextView proximityText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_test);

        accelerometerText = findViewById(R.id.text_accelerometer);
        gyroscopeText = findViewById(R.id.text_gyroscope);
        magneticText = findViewById(R.id.text_magnetic);
        lightText = findViewById(R.id.text_light);
        proximityText = findViewById(R.id.text_proximity);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = getSensor(Sensor.TYPE_ACCELEROMETER, accelerometerText);
        gyroscope = getSensor(Sensor.TYPE_GYROSCOPE, gyroscopeText);
        magneticField = getSensor(Sensor.TYPE_MAGNETIC_FIELD, magneticText);
        light = getSensor(Sensor.TYPE_LIGHT, lightText);
        proximity = getSensor(Sensor.TYPE_PROXIMITY, proximityText);
    }

    private Sensor getSensor(int type, TextView statusText) {
        Sensor sensor = sensorManager.getDefaultSensor(type);
        if (sensor == null) {
            statusText.setText(getString(R.string.sensor_not_available));
        }
        return sensor;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerIfAvailable(accelerometer);
        registerIfAvailable(gyroscope);
        registerIfAvailable(magneticField);
        registerIfAvailable(light);
        registerIfAvailable(proximity);
    }

    private void registerIfAvailable(Sensor sensor) {
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        float[] v = event.values;
        if (type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerText.setText(formatXyz(v));
        } else if (type == Sensor.TYPE_GYROSCOPE) {
            gyroscopeText.setText(formatXyz(v));
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticText.setText(formatXyz(v));
        } else if (type == Sensor.TYPE_LIGHT) {
            lightText.setText(String.format(Locale.US, getString(R.string.sensor_light_format), v[0]));
        } else if (type == Sensor.TYPE_PROXIMITY) {
            proximityText.setText(String.format(Locale.US, getString(R.string.sensor_proximity_format), v[0]));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private String formatXyz(float[] v) {
        return String.format(Locale.US, getString(R.string.sensor_xyz_format), v[0], v[1], v[2]);
    }
}
