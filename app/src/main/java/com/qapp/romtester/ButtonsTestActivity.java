package com.qapp.romtester;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

public class ButtonsTestActivity extends Activity implements View.OnClickListener {

    private static final String PREFS_NAME = "buttons_test_prefs";
    private static final String PREF_HOME_PRESSED = "home_pressed";

    /** Sentinel entry for Home, which Android never delivers to onKeyDown/onKeyUp. */
    private static final int KEY_HOME_SENTINEL = -1;

    private static final int[] TRACKED_KEYS = {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_FOCUS,
            KeyEvent.KEYCODE_SEARCH,
            KEY_HOME_SENTINEL,
    };

    private TextView checklistText;
    private TextView logText;
    private ScrollView logScroll;
    private final Map<Integer, Boolean> tested = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buttons_test);

        checklistText = findViewById(R.id.text_buttons_checklist);
        logText = findViewById(R.id.text_buttons_log);
        logScroll = findViewById(R.id.scroll_buttons_log);
        findViewById(R.id.button_buttons_exit).setOnClickListener(this);

        for (int keyCode : TRACKED_KEYS) {
            tested.put(keyCode, false);
        }
        renderChecklist();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(PREF_HOME_PRESSED, false)) {
            prefs.edit().remove(PREF_HOME_PRESSED).apply();
            tested.put(KEY_HOME_SENTINEL, true);
            renderChecklist();
            appendLogLine(getString(R.string.buttons_key_home), getString(R.string.buttons_tag_detected),
                    R.color.accent);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(PREF_HOME_PRESSED, true).apply();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_buttons_exit) {
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        handleKeyEvent(keyCode, true);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        handleKeyEvent(keyCode, false);
        return true;
    }

    private void handleKeyEvent(int keyCode, boolean down) {
        if (down && tested.containsKey(keyCode)) {
            tested.put(keyCode, true);
            renderChecklist();
        }
        String tag = down ? getString(R.string.buttons_tag_down) : getString(R.string.buttons_tag_up);
        int colorRes = down ? R.color.primary : R.color.text_secondary;
        appendLogLine(keyName(keyCode), tag, colorRes);
    }

    private void renderChecklist() {
        StringBuilder sb = new StringBuilder();
        for (int keyCode : TRACKED_KEYS) {
            boolean done = Boolean.TRUE.equals(tested.get(keyCode));
            sb.append(done ? "[X] " : "[ ] ").append(keyName(keyCode)).append("\n");
        }
        checklistText.setText(sb.toString().trim());
    }

    @SuppressWarnings("deprecation")
    private void appendLogLine(String name, String tag, int colorRes) {
        int color = getResources().getColor(colorRes);

        SpannableStringBuilder builder = new SpannableStringBuilder(logText.getText());
        int start = builder.length();
        String line = "[" + tag + "] " + name + "\n";
        builder.append(line);
        builder.setSpan(new ForegroundColorSpan(color), start, start + ("[" + tag + "]").length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        logText.setText(builder);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    private String keyName(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                return getString(R.string.buttons_key_volume_up);
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return getString(R.string.buttons_key_volume_down);
            case KeyEvent.KEYCODE_BACK:
                return getString(R.string.buttons_key_back);
            case KeyEvent.KEYCODE_MENU:
                return getString(R.string.buttons_key_menu);
            case KeyEvent.KEYCODE_CAMERA:
                return getString(R.string.buttons_key_camera);
            case KeyEvent.KEYCODE_FOCUS:
                return getString(R.string.buttons_key_focus);
            case KeyEvent.KEYCODE_SEARCH:
                return getString(R.string.buttons_key_search);
            case KEY_HOME_SENTINEL:
                return getString(R.string.buttons_key_home);
            default:
                if (Build.VERSION.SDK_INT >= 12) {
                    return KeyEvent.keyCodeToString(keyCode);
                }
                return getString(R.string.buttons_key_unknown_format, keyCode);
        }
    }
}
