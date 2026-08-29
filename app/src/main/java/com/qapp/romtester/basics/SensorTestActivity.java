package com.qapp.romtester.basics;

import android.app.Activity;
import android.hardware.Sensor;
import android.os.Bundle;
import android.widget.TextView;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.SensorTester;

import java.util.Locale;

public class SensorTestActivity extends Activity {

    private static final int[] TRACKED_TYPES = {
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_LIGHT, Sensor.TYPE_PROXIMITY
    };

    private TextView accelerometerText;
    private TextView gyroscopeText;
    private TextView magneticText;
    private TextView lightText;
    private TextView proximityText;

    private SensorTester tester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_test);

        accelerometerText = findViewById(R.id.text_accelerometer);
        gyroscopeText = findViewById(R.id.text_gyroscope);
        magneticText = findViewById(R.id.text_magnetic);
        lightText = findViewById(R.id.text_light);
        proximityText = findViewById(R.id.text_proximity);

        tester = new SensorTester(this, TRACKED_TYPES, new SensorTester.Listener() {
            @Override
            public void onReading(int sensorType, float[] values) {
                textViewFor(sensorType).setText(formatReading(sensorType, values));
            }

            @Override
            public void onSensorUnavailable(int sensorType) {
                textViewFor(sensorType).setText(getString(R.string.sensor_not_available));
            }
        });
    }

    private TextView textViewFor(int sensorType) {
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            return accelerometerText;
        } else if (sensorType == Sensor.TYPE_GYROSCOPE) {
            return gyroscopeText;
        } else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            return magneticText;
        } else if (sensorType == Sensor.TYPE_LIGHT) {
            return lightText;
        } else {
            return proximityText;
        }
    }

    private String formatReading(int sensorType, float[] v) {
        if (sensorType == Sensor.TYPE_LIGHT) {
            return String.format(Locale.US, getString(R.string.sensor_light_format), v[0]);
        } else if (sensorType == Sensor.TYPE_PROXIMITY) {
            return String.format(Locale.US, getString(R.string.sensor_proximity_format), v[0]);
        }
        return String.format(Locale.US, getString(R.string.sensor_xyz_format), v[0], v[1], v[2]);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tester.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        tester.stop();
    }
}
