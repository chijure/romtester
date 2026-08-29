package com.qapp.romtester.tester;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class ButtonsTester {

    /** Sentinel entry for Home, which Android never delivers to onKeyDown/onKeyUp. */
    public static final int KEY_HOME_SENTINEL = -1;

    public static final int[] TRACKED_KEYS = {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_FOCUS,
            KeyEvent.KEYCODE_SEARCH,
            KEY_HOME_SENTINEL,
    };

    private static final String PREFS_NAME = "buttons_test_prefs";
    private static final String PREF_HOME_PRESSED = "home_pressed";

    public interface Listener {
        void onKeyEvent(int keyCode, boolean down);

        void onHomeDetectedFromPreviousSession();
    }

    private final Context context;
    private final Listener listener;
    private final Map<Integer, Boolean> tested = new LinkedHashMap<>();

    public ButtonsTester(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        for (int keyCode : TRACKED_KEYS) {
            tested.put(keyCode, false);
        }
    }

    public boolean isTested(int keyCode) {
        return Boolean.TRUE.equals(tested.get(keyCode));
    }

    public void checkHomePressedFlag() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_HOME_PRESSED, false)) {
            prefs.edit().remove(PREF_HOME_PRESSED).apply();
            tested.put(KEY_HOME_SENTINEL, true);
            listener.onHomeDetectedFromPreviousSession();
        }
    }

    public void markHomeLeavingHint() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_HOME_PRESSED, true).apply();
    }

    public void handleKeyEvent(int keyCode, boolean down) {
        if (down && tested.containsKey(keyCode)) {
            tested.put(keyCode, true);
        }
        listener.onKeyEvent(keyCode, down);
    }
}
