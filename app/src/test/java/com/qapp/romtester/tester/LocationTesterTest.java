package com.qapp.romtester.tester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.LocationManager;

import org.junit.Test;

public class LocationTesterTest {

    private static final String LOCATION_PERMISSION = "android.permission.ACCESS_FINE_LOCATION";
    private static final int POST_M = 23;
    private static final int GNSS_API = 24;
    private static final int LEGACY_API = 21;

    @Test
    public void hasPermission_delegatesToPermissionUtils() {
        Activity activity = mock(Activity.class);
        when(activity.checkSelfPermission(LOCATION_PERMISSION)).thenReturn(PackageManager.PERMISSION_GRANTED);
        LocationTester tester = new LocationTester(activity, mock(LocationManager.class),
                mock(LocationTester.Listener.class), POST_M);

        assertTrue(tester.hasPermission());
    }

    @Test
    public void isGpsProviderEnabled_delegatesToLocationManager() {
        LocationManager locationManager = mock(LocationManager.class);
        when(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        LocationTester tester = new LocationTester(mock(Activity.class), locationManager,
                mock(LocationTester.Listener.class), POST_M);

        assertTrue(tester.isGpsProviderEnabled());
    }

    @Test
    public void startTracking_registersGnssCallbackOnApi24AndAbove() {
        LocationManager locationManager = mock(LocationManager.class);
        LocationTester tester = new LocationTester(mock(Activity.class), locationManager,
                mock(LocationTester.Listener.class), GNSS_API);

        tester.startTracking();

        assertTrue(tester.isTracking());
        verify(locationManager).requestLocationUpdates(eq(LocationManager.GPS_PROVIDER),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyFloat(), any(android.location.LocationListener.class));
        verify(locationManager).registerGnssStatusCallback(any(GnssStatus.Callback.class));
        verify(locationManager, never()).addGpsStatusListener(any());
    }

    @Test
    public void startTracking_registersLegacyGpsListenerBelowApi24() {
        LocationManager locationManager = mock(LocationManager.class);
        LocationTester tester = new LocationTester(mock(Activity.class), locationManager,
                mock(LocationTester.Listener.class), LEGACY_API);

        tester.startTracking();

        verify(locationManager).addGpsStatusListener(any(GpsStatus.Listener.class));
        verify(locationManager, never()).registerGnssStatusCallback(any());
    }

    @Test
    public void stopTracking_unregistersGnssCallbackOnApi24AndAbove() {
        LocationManager locationManager = mock(LocationManager.class);
        LocationTester tester = new LocationTester(mock(Activity.class), locationManager,
                mock(LocationTester.Listener.class), GNSS_API);
        tester.startTracking();

        tester.stopTracking();

        assertFalse(tester.isTracking());
        verify(locationManager).unregisterGnssStatusCallback(any(GnssStatus.Callback.class));
        verify(locationManager).removeUpdates(any(android.location.LocationListener.class));
    }

    @Test
    public void stopTracking_removesLegacyGpsListenerBelowApi24() {
        LocationManager locationManager = mock(LocationManager.class);
        LocationTester tester = new LocationTester(mock(Activity.class), locationManager,
                mock(LocationTester.Listener.class), LEGACY_API);
        tester.startTracking();

        tester.stopTracking();

        verify(locationManager).removeGpsStatusListener(any(GpsStatus.Listener.class));
    }
}
