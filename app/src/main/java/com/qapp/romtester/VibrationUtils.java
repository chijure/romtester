package com.qapp.romtester;

import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public final class VibrationUtils {

    private VibrationUtils() {
    }

    public static void vibrate(Vibrator vibrator, long millis) {
        vibrate(vibrator, millis, Build.VERSION.SDK_INT);
    }

    @SuppressWarnings("deprecation")
    static void vibrate(Vibrator vibrator, long millis, int sdkInt) {
        if (sdkInt >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(millis);
        }
    }
}
