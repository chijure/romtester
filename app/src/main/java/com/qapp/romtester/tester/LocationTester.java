package com.qapp.romtester.tester;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import com.qapp.romtester.util.PermissionUtils;

import java.util.Iterator;

public class LocationTester {

    public interface Listener {
        void onProviderDisabled();

        void onLocationChanged(Location location);

        void onSatelliteCountChanged(int count);
    }

    private final Activity activity;
    private final LocationManager locationManager;
    private final Listener listener;
    private final int sdkInt;

    private boolean tracking;
    private GnssStatus.Callback gnssCallback;
    private GpsStatus.Listener legacyGpsListener;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            listener.onLocationChanged(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
            listener.onProviderDisabled();
        }
    };

    public LocationTester(Activity activity, Listener listener) {
        this(activity, (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE), listener,
                Build.VERSION.SDK_INT);
    }

    LocationTester(Activity activity, LocationManager locationManager, Listener listener, int sdkInt) {
        this.activity = activity;
        this.locationManager = locationManager;
        this.listener = listener;
        this.sdkInt = sdkInt;
    }

    public boolean isTracking() {
        return tracking;
    }

    public boolean hasPermission() {
        return PermissionUtils.has(activity, Manifest.permission.ACCESS_FINE_LOCATION, sdkInt);
    }

    public boolean isGpsProviderEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @SuppressWarnings("deprecation")
    public void startTracking() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

        if (sdkInt >= 24) {
            gnssCallback = new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    listener.onSatelliteCountChanged(status.getSatelliteCount());
                }
            };
            locationManager.registerGnssStatusCallback(gnssCallback);
        } else {
            legacyGpsListener = event -> {
                if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                    GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                    int count = 0;
                    Iterator<GpsSatellite> it = gpsStatus.getSatellites().iterator();
                    while (it.hasNext()) {
                        it.next();
                        count++;
                    }
                    listener.onSatelliteCountChanged(count);
                }
            };
            locationManager.addGpsStatusListener(legacyGpsListener);
        }

        tracking = true;
    }

    @SuppressWarnings("deprecation")
    public void stopTracking() {
        locationManager.removeUpdates(locationListener);
        if (sdkInt >= 24 && gnssCallback != null) {
            locationManager.unregisterGnssStatusCallback(gnssCallback);
            gnssCallback = null;
        } else if (legacyGpsListener != null) {
            locationManager.removeGpsStatusListener(legacyGpsListener);
            legacyGpsListener = null;
        }
        tracking = false;
    }
}
