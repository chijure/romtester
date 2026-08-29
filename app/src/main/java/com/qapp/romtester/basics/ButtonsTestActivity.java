package com.qapp.romtester.basics;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.ButtonsTester;
import com.qapp.romtester.util.LogViewUtils;

public class ButtonsTestActivity extends Activity implements View.OnClickListener {

    private TextView checklistText;
    private TextView logText;
    private ScrollView logScroll;
    private ButtonsTester tester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buttons_test);

        checklistText = findViewById(R.id.text_buttons_checklist);
        logText = findViewById(R.id.text_buttons_log);
        logScroll = findViewById(R.id.scroll_buttons_log);
        findViewById(R.id.button_buttons_exit).setOnClickListener(this);

        tester = new ButtonsTester(this, new ButtonsTester.Listener() {
            @Override
            public void onKeyEvent(int keyCode, boolean down) {
                renderChecklist();
                String tag = down ? getString(R.string.buttons_tag_down) : getString(R.string.buttons_tag_up);
                int colorRes = down ? R.color.primary : R.color.text_secondary;
                appendLogLine(keyName(keyCode), tag, colorRes);
            }

            @Override
            public void onHomeDetectedFromPreviousSession() {
                renderChecklist();
                appendLogLine(getString(R.string.buttons_key_home), getString(R.string.buttons_tag_detected),
                        R.color.accent);
            }
        });
        renderChecklist();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tester.checkHomePressedFlag();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        tester.markHomeLeavingHint();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_buttons_exit) {
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        tester.handleKeyEvent(keyCode, true);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        tester.handleKeyEvent(keyCode, false);
        return true;
    }

    private void renderChecklist() {
        StringBuilder sb = new StringBuilder();
        for (int keyCode : ButtonsTester.TRACKED_KEYS) {
            boolean done = tester.isTested(keyCode);
            sb.append(done ? "[X] " : "[ ] ").append(keyName(keyCode)).append("\n");
        }
        checklistText.setText(sb.toString().trim());
    }

    @SuppressWarnings("deprecation")
    private void appendLogLine(String name, String tag, int colorRes) {
        LogViewUtils.appendLine(logText, logScroll, tag, getResources().getColor(colorRes), name);
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
            case ButtonsTester.KEY_HOME_SENTINEL:
                return getString(R.string.buttons_key_home);
            default:
                if (Build.VERSION.SDK_INT >= 12) {
                    return KeyEvent.keyCodeToString(keyCode);
                }
                return getString(R.string.buttons_key_unknown_format, keyCode);
        }
    }
}
