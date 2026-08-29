package com.qapp.romtester.util;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public final class LogViewUtils {

    private LogViewUtils() {
    }

    public static void appendLine(TextView logText, ScrollView logScroll, String tag, int color, String message) {
        SpannableStringBuilder builder = new SpannableStringBuilder(logText.getText());
        int start = builder.length();
        String line = "[" + tag + "] " + message + "\n";
        builder.append(line);
        builder.setSpan(new ForegroundColorSpan(color), start, start + ("[" + tag + "]").length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        logText.setText(builder);
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }
}
