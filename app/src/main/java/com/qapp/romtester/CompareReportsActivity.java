package com.qapp.romtester;

import android.app.Activity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CompareReportsActivity extends Activity {

    public static final String EXTRA_TIMESTAMP_NEW = "timestamp_new";
    public static final String EXTRA_TIMESTAMP_OLD = "timestamp_old";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_reports);

        TextView headerText = findViewById(R.id.text_compare_header);
        TextView logText = findViewById(R.id.text_compare_log);

        long newTimestamp = getIntent().getLongExtra(EXTRA_TIMESTAMP_NEW, -1);
        long oldTimestamp = getIntent().getLongExtra(EXTRA_TIMESTAMP_OLD, -1);

        try {
            TestReport newReport = ReportStore.readReport(ReportStore.fileForTimestamp(this, newTimestamp));
            TestReport oldReport = ReportStore.readReport(ReportStore.fileForTimestamp(this, oldTimestamp));

            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
            headerText.setText(getString(R.string.compare_header_format,
                    dateFormat.format(new Date(newReport.timestamp)),
                    dateFormat.format(new Date(oldReport.timestamp))));

            Map<String, String> oldStatusByName = new HashMap<>();
            for (TestReport.CheckEntry entry : oldReport.checks) {
                oldStatusByName.put(entry.name, entry.status);
            }

            SpannableStringBuilder builder = new SpannableStringBuilder();
            for (TestReport.CheckEntry entry : newReport.checks) {
                String oldStatus = oldStatusByName.get(entry.name);
                appendCompareLine(builder, entry.name, oldStatus, entry.status);
            }
            logText.setText(builder);
        } catch (Exception e) {
            headerText.setText(getString(R.string.report_detail_load_error));
        }
    }

    @SuppressWarnings("deprecation")
    private void appendCompareLine(SpannableStringBuilder builder, String name, String oldStatus, String newStatus) {
        String oldTag = oldStatus == null ? "—" : ReportRenderer.tagForStatus(this, oldStatus);
        String newTag = ReportRenderer.tagForStatus(this, newStatus);

        boolean regression = "PASS".equals(oldStatus) && "FAIL".equals(newStatus);
        boolean improvement = "FAIL".equals(oldStatus) && "PASS".equals(newStatus);
        boolean unchanged = newStatus.equals(oldStatus);

        int color;
        String marker;
        if (regression) {
            color = getResources().getColor(R.color.danger);
            marker = "↓ ";
        } else if (improvement) {
            color = getResources().getColor(R.color.success);
            marker = "↑ ";
        } else if (unchanged) {
            color = getResources().getColor(R.color.text_secondary);
            marker = "= ";
        } else {
            color = getResources().getColor(R.color.manual);
            marker = "~ ";
        }

        String line = marker + name + ": " + oldTag + " → " + newTag + "\n";
        int start = builder.length();
        builder.append(line);
        builder.setSpan(new ForegroundColorSpan(color), start, start + line.length() - 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
