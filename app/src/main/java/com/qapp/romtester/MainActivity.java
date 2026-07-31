package com.qapp.romtester;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final Object[][] MENU_ITEMS = {
            {R.id.button_robot_test, RobotTestActivity.class},
            {R.id.button_test_history, TestHistoryActivity.class},
            {R.id.button_io_test, InputOutputTestActivity.class},
            {R.id.button_screen_test, ScreenTestActivity.class},
            {R.id.button_sensor_test, SensorTestActivity.class},
            {R.id.button_camera_test, CameraTestActivity.class},
            {R.id.button_device_info, DeviceInfoActivity.class},
            {R.id.button_microphone_test, MicrophoneTestActivity.class},
            {R.id.button_multitouch_test, MultitouchTestActivity.class},
            {R.id.button_location_test, LocationTestActivity.class},
            {R.id.button_wifi_test, WifiTestActivity.class},
            {R.id.button_bluetooth_test, BluetoothTestActivity.class},
            {R.id.button_nfc_test, NfcTestActivity.class},
            {R.id.button_opengl_test, OpenGlTestActivity.class},
            {R.id.button_video_test, VideoTestActivity.class},
            {R.id.button_phone_test, PhoneTestActivity.class},
            {R.id.button_settings_shortcuts, SettingsShortcutsActivity.class},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (Object[] item : MENU_ITEMS) {
            findViewById((Integer) item[0]).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        for (Object[] item : MENU_ITEMS) {
            if ((Integer) item[0] == id) {
                startActivity(new Intent(this, (Class<?>) item[1]));
                return;
            }
        }
    }
}
