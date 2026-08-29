package com.qapp.romtester;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.HashMap;
import java.util.Map;

public class SensorTester {

    public interface Listener {
        void onReading(int sensorType, float[] values);

        void onSensorUnavailable(int sensorType);
    }

    private final SensorManager sensorManager;
    private final Listener listener;
    private final Map<Integer, Sensor> available = new HashMap<>();

    private final SensorEventListener eventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            listener.onReading(event.sensor.getType(), event.values);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public SensorTester(Context context, int[] sensorTypes, Listener listener) {
        this((SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE),
                sensorTypes, listener);
    }

    SensorTester(SensorManager sensorManager, int[] sensorTypes, Listener listener) {
        this.sensorManager = sensorManager;
        this.listener = listener;
        for (int type : sensorTypes) {
            Sensor sensor = sensorManager.getDefaultSensor(type);
            if (sensor == null) {
                listener.onSensorUnavailable(type);
            } else {
                available.put(type, sensor);
            }
        }
    }

    public void start() {
        for (Sensor sensor : available.values()) {
            sensorManager.registerListener(eventListener, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stop() {
        sensorManager.unregisterListener(eventListener);
    }
}
