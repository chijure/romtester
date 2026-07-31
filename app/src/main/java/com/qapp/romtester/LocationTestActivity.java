package com.qapp.romtester;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Iterator;
import java.util.Locale;

public class LocationTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_LOCATION = 1;

    private TextView statusText;
    private TextView dataText;
    private Button toggleButton;

    private LocationManager locationManager;
    private boolean tracking;
    private int satelliteCount = -1;
    private Location lastLocation;

    private Object gnssCallback;
    private GpsStatus.Listener legacyGpsListener;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
            statusText.setText(getString(R.string.location_provider_disabled));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_test);

        statusText = findViewById(R.id.text_location_status);
        dataText = findViewById(R.id.text_location_data);
        toggleButton = findViewById(R.id.button_location_toggle);
        toggleButton.setOnClickListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public void onClick(View view) {
        if (tracking) {
            stopTracking();
        } else if (hasLocationPermission()) {
            startTracking();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            } else {
                statusText.setText(getString(R.string.location_permission_denied));
            }
        }
    }

    private boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("deprecation")
    private void startTracking() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            statusText.setText(getString(R.string.location_gps_off));
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

        if (Build.VERSION.SDK_INT >= 24) {
            android.location.GnssStatus.Callback callback = new android.location.GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(android.location.GnssStatus status) {
                    satelliteCount = status.getSatelliteCount();
                    updateSatelliteText();
                }
            };
            gnssCallback = callback;
            locationManager.registerGnssStatusCallback(callback);
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
                    satelliteCount = count;
                    updateSatelliteText();
                }
            };
            locationManager.addGpsStatusListener(legacyGpsListener);
        }

        tracking = true;
        toggleButton.setText(getString(R.string.location_button_stop));
        statusText.setText(getString(R.string.location_status_tracking));
    }

    @SuppressWarnings("deprecation")
    private void stopTracking() {
        locationManager.removeUpdates(locationListener);
        if (Build.VERSION.SDK_INT >= 24 && gnssCallback != null) {
            locationManager.unregisterGnssStatusCallback((android.location.GnssStatus.Callback) gnssCallback);
            gnssCallback = null;
        } else if (legacyGpsListener != null) {
            locationManager.removeGpsStatusListener(legacyGpsListener);
            legacyGpsListener = null;
        }

        tracking = false;
        toggleButton.setText(getString(R.string.location_button_start));
        statusText.setText(getString(R.string.location_status_stopped));
    }

    private void updateLocation(Location location) {
        lastLocation = location;
        statusText.setText(getString(R.string.location_status_fix_received));
        renderData();
    }

    private void updateSatelliteText() {
        renderData();
    }

    private void renderData() {
        String satellites = satelliteCount >= 0 ? String.valueOf(satelliteCount) : getString(R.string.common_dash);
        if (lastLocation != null) {
            dataText.setText(String.format(Locale.US,
                    getString(R.string.location_data_format),
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    lastLocation.getAccuracy(), satellites));
        } else {
            dataText.setText(getString(R.string.location_data_no_fix_format, satellites));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tracking) {
            stopTracking();
        }
    }
}
