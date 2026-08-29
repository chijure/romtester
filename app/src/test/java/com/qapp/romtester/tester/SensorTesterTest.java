package com.qapp.romtester.tester;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import org.junit.Test;

/**
 * Sensor is a final class with no publicly accessible constructor, so a "sensor present"
 * Sensor instance can't be created or mocked here without mockito-inline - these tests cover
 * the "sensor missing" path and start()/stop() wiring, which is what's reachable on the JVM.
 */
public class SensorTesterTest {

    private static final int[] TYPES = {Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_LIGHT};

    @Test
    public void constructor_reportsUnavailableForEveryMissingSensor() {
        SensorManager sensorManager = mock(SensorManager.class);
        when(sensorManager.getDefaultSensor(anyInt())).thenReturn(null);
        SensorTester.Listener listener = mock(SensorTester.Listener.class);

        new SensorTester(sensorManager, TYPES, listener);

        verify(listener).onSensorUnavailable(Sensor.TYPE_ACCELEROMETER);
        verify(listener).onSensorUnavailable(Sensor.TYPE_LIGHT);
        verify(listener, never()).onReading(anyInt(), any());
    }

    @Test
    public void start_registersNothingWhenNoSensorsAvailable() {
        SensorManager sensorManager = mock(SensorManager.class);
        when(sensorManager.getDefaultSensor(anyInt())).thenReturn(null);
        SensorTester tester = new SensorTester(sensorManager, TYPES, mock(SensorTester.Listener.class));

        tester.start();

        verify(sensorManager, never()).registerListener(any(), any(Sensor.class), anyInt());
    }

    @Test
    public void stop_alwaysUnregistersTheListener() {
        SensorManager sensorManager = mock(SensorManager.class);
        when(sensorManager.getDefaultSensor(anyInt())).thenReturn(null);
        SensorTester tester = new SensorTester(sensorManager, TYPES, mock(SensorTester.Listener.class));

        tester.stop();

        verify(sensorManager, times(1)).unregisterListener(any(android.hardware.SensorEventListener.class));
    }
}
