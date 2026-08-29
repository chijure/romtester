package com.qapp.romtester.telephony;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.PhoneTester;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class PhoneTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_READ_PHONE_STATE = 1;

    private TextView infoText;
    private PhoneTester tester;
    private String signalInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_test);

        signalInfo = getString(R.string.phone_signal_default);
        infoText = findViewById(R.id.text_phone_info);
        findViewById(R.id.button_open_dialer).setOnClickListener(this);
        findViewById(R.id.button_open_sms).setOnClickListener(this);

        tester = new PhoneTester(this, new PhoneTester.Listener() {
            @Override
            public void onSignalStrength(boolean isLevel, int value) {
                signalInfo = isLevel
                        ? getString(R.string.phone_signal_level_format, value)
                        : getString(R.string.phone_signal_asu_format, value);
                refreshInfo();
            }

            @Override
            public void onDialerUnavailable() {
                Toast.makeText(PhoneTestActivity.this, getString(R.string.phone_no_dialer_app), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSmsUnavailable() {
                Toast.makeText(PhoneTestActivity.this, getString(R.string.phone_no_sms_app), Toast.LENGTH_SHORT).show();
            }
        });

        if (tester.hasPermission()) {
            refreshInfo();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_PHONE_STATE) {
            refreshInfo();
        }
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

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_open_dialer) {
            tester.openDialer();
        } else if (id == R.id.button_open_sms) {
            tester.openSms(getString(R.string.phone_sms_draft_body));
        }
    }

    private void refreshInfo() {
        PhoneTester.TelephonyInfo info = tester.readTelephonyInfo();
        String simState = describeSimState(info.simState);
        String phoneType = describePhoneType(info.phoneType);
        String networkType = describeNetworkType(info.networkType);
        String operator = info.operatorNameOrNull != null ? info.operatorNameOrNull : getString(R.string.common_dash);

        infoText.setText(getString(R.string.phone_info_format, simState, phoneType, networkType, operator, signalInfo));
    }

    private String describeSimState(int state) {
        switch (state) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return getString(R.string.phone_sim_absent);
            case TelephonyManager.SIM_STATE_READY:
                return getString(R.string.phone_sim_ready);
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return getString(R.string.phone_sim_pin_required);
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return getString(R.string.phone_sim_puk_required);
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return getString(R.string.phone_sim_network_locked);
            default:
                return getString(R.string.phone_sim_unknown);
        }
    }

    private String describePhoneType(int type) {
        switch (type) {
            case TelephonyManager.PHONE_TYPE_GSM:
                return getString(R.string.phone_type_gsm);
            case TelephonyManager.PHONE_TYPE_CDMA:
                return getString(R.string.phone_type_cdma);
            case TelephonyManager.PHONE_TYPE_SIP:
                return getString(R.string.phone_type_sip);
            default:
                return getString(R.string.phone_type_none);
        }
    }

    private String describeNetworkType(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return getString(R.string.phone_network_gprs);
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return getString(R.string.phone_network_edge);
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return getString(R.string.phone_network_umts);
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return getString(R.string.phone_network_hsdpa);
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return getString(R.string.phone_network_hsupa);
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return getString(R.string.phone_network_hspa);
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return getString(R.string.phone_network_hspap);
            case TelephonyManager.NETWORK_TYPE_LTE:
                return getString(R.string.phone_network_lte);
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return getString(R.string.phone_network_cdma);
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return getString(R.string.phone_network_evdo);
            default:
                return getString(R.string.phone_network_unknown);
        }
    }
}
