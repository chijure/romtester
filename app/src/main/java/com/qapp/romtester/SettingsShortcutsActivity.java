package com.qapp.romtester;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

public class SettingsShortcutsActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_shortcuts);

        findViewById(R.id.button_open_tethering).setOnClickListener(this);
        findViewById(R.id.button_open_settings).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_open_tethering) {
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        } else if (id == R.id.button_open_settings) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}
