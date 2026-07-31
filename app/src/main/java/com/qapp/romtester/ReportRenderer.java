package com.qapp.romtester;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

public class ReportRenderer {

    private ReportRenderer() {
    }

    @SuppressWarnings("deprecation")
    public static int colorForStatus(Context context, String status) {
        switch (status) {
            case "PASS":
                return context.getResources().getColor(R.color.success);
            case "FAIL":
                return context.getResources().getColor(R.color.danger);
            case "MANUAL":
                return context.getResources().getColor(R.color.manual);
            default:
                return context.getResources().getColor(R.color.text_secondary);
        }
    }

    public static String tagForStatus(Context context, String status) {
        switch (status) {
            case "PASS":
                return context.getString(R.string.robot_tag_pass);
            case "FAIL":
                return context.getString(R.string.robot_tag_fail);
            case "MANUAL":
                return context.getString(R.string.robot_tag_manual);
            default:
                return context.getString(R.string.robot_tag_na);
        }
    }

    public static void appendLine(Context context, SpannableStringBuilder builder,
                                   String name, String status, String detail) {
        String tag = tagForStatus(context, status);
        String line = "[" + tag + "] " + name + " — " + detail + "\n";
        int start = builder.length();
        builder.append(line);
        builder.setSpan(new ForegroundColorSpan(colorForStatus(context, status)), start,
                start + ("[" + tag + "]").length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
