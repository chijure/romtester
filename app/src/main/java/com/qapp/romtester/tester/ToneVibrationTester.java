package com.qapp.romtester.tester;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Vibrator;

import com.qapp.romtester.util.HardwareCapabilities;
import com.qapp.romtester.util.VibrationUtils;

public class ToneVibrationTester {

    private final ToneGenerator toneGenerator;
    private final Vibrator vibrator;
    private final int sdkInt;

    public ToneVibrationTester(Context context) {
        this(createToneGenerator(), (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE),
                Build.VERSION.SDK_INT);
    }

    ToneVibrationTester(ToneGenerator toneGenerator, Vibrator vibrator, int sdkInt) {
        this.toneGenerator = toneGenerator;
        this.vibrator = vibrator;
        this.sdkInt = sdkInt;
    }

    private static ToneGenerator createToneGenerator() {
        try {
            return new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public boolean playTestTone() {
        if (toneGenerator == null) {
            return false;
        }
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 500);
        return true;
    }

    public boolean vibrateOnce(long millis) {
        if (!HardwareCapabilities.hasVibrator(vibrator, sdkInt)) {
            return false;
        }
        VibrationUtils.vibrate(vibrator, millis, sdkInt);
        return true;
    }

    public void stop() {
        if (toneGenerator != null) {
            toneGenerator.stopTone();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    public void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}
