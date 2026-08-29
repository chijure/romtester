package com.qapp.romtester.connectivity;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.LocationTester;
import com.qapp.romtester.util.PermissionUtils;

import android.Manifest;
import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class LocationTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_LOCATION = 1;

    private TextView statusText;
    private TextView dataText;
    private Button toggleButton;

    private LocationTester tester;
    private int satelliteCount = -1;
    private Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_test);

        statusText = findViewById(R.id.text_location_status);
        dataText = findViewById(R.id.text_location_data);
        toggleButton = findViewById(R.id.button_location_toggle);
        toggleButton.setOnClickListener(this);

        tester = new LocationTester(this, new LocationTester.Listener() {
            @Override
            public void onProviderDisabled() {
                statusText.setText(getString(R.string.location_provider_disabled));
            }

            @Override
            public void onLocationChanged(Location location) {
                lastLocation = location;
                statusText.setText(getString(R.string.location_status_fix_received));
                renderData();
            }

            @Override
            public void onSatelliteCountChanged(int count) {
                satelliteCount = count;
                renderData();
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (tester.isTracking()) {
            stopTracking();
        } else if (tester.hasPermission()) {
            startTracking();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (PermissionUtils.granted(grantResults)) {
                startTracking();
            } else {
                statusText.setText(getString(R.string.location_permission_denied));
            }
        }
    }

    private void startTracking() {
        if (!tester.isGpsProviderEnabled()) {
            statusText.setText(getString(R.string.location_gps_off));
            return;
        }
        tester.startTracking();
        toggleButton.setText(getString(R.string.location_button_stop));
        statusText.setText(getString(R.string.location_status_tracking));
    }

    private void stopTracking() {
        tester.stopTracking();
        toggleButton.setText(getString(R.string.location_button_start));
        statusText.setText(getString(R.string.location_status_stopped));
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
        if (tester.isTracking()) {
            stopTracking();
        }
    }
}
