package com.qapp.romtester;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Vibrator;

public final class HardwareCapabilities {

    private HardwareCapabilities() {
    }

    public static boolean hasCamera(PackageManager pm) {
        return hasCamera(pm, Build.VERSION.SDK_INT);
    }

    static boolean hasCamera(PackageManager pm, int sdkInt) {
        if (sdkInt >= Build.VERSION_CODES.GINGERBREAD) {
            return Camera.getNumberOfCameras() > 0;
        }
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public static boolean hasVibrator(Vibrator vibrator) {
        return hasVibrator(vibrator, Build.VERSION.SDK_INT);
    }

    static boolean hasVibrator(Vibrator vibrator, int sdkInt) {
        return vibrator != null && (sdkInt < 11 || vibrator.hasVibrator());
    }
}
