package com.qapp.romtester.tester;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.qapp.romtester.util.PermissionUtils;

public class PhoneTester {

    public static class TelephonyInfo {
        public final int simState;
        public final int phoneType;
        public final int networkType;
        public final String operatorNameOrNull;

        TelephonyInfo(int simState, int phoneType, int networkType, String operatorNameOrNull) {
            this.simState = simState;
            this.phoneType = phoneType;
            this.networkType = networkType;
            this.operatorNameOrNull = operatorNameOrNull;
        }
    }

    public interface Listener {
        void onSignalStrength(boolean isLevel, int value);

        void onDialerUnavailable();

        void onSmsUnavailable();
    }

    private final Activity activity;
    private final TelephonyManager telephonyManager;
    private final Listener listener;
    private final int sdkInt;

    private final PhoneStateListener signalListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (sdkInt >= Build.VERSION_CODES.M) {
                listener.onSignalStrength(true, signalStrength.getLevel());
            } else {
                listener.onSignalStrength(false, signalStrength.getGsmSignalStrength());
            }
        }
    };

    public PhoneTester(Activity activity, Listener listener) {
        this(activity, (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE), listener,
                Build.VERSION.SDK_INT);
    }

    PhoneTester(Activity activity, TelephonyManager telephonyManager, Listener listener, int sdkInt) {
        this.activity = activity;
        this.telephonyManager = telephonyManager;
        this.listener = listener;
        this.sdkInt = sdkInt;
    }

    public boolean hasPermission() {
        return PermissionUtils.has(activity, Manifest.permission.READ_PHONE_STATE, sdkInt);
    }

    public void start() {
        telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    public void stop() {
        telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_NONE);
    }

    public TelephonyInfo readTelephonyInfo() {
        String operator = telephonyManager.getNetworkOperatorName();
        return new TelephonyInfo(telephonyManager.getSimState(), telephonyManager.getPhoneType(),
                telephonyManager.getNetworkType(),
                (operator == null || operator.length() == 0) ? null : operator);
    }

    public void openDialer() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        } else {
            listener.onDialerUnavailable();
        }
    }

    public void openSms(String draftBody) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
        intent.putExtra("sms_body", draftBody);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        } else {
            listener.onSmsUnavailable();
        }
    }
}
