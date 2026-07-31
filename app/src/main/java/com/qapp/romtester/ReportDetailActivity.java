package com.qapp.romtester;

import android.app.Activity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportDetailActivity extends Activity {

    public static final String EXTRA_TIMESTAMP = "timestamp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        TextView headerText = findViewById(R.id.text_report_detail_header);
        TextView logText = findViewById(R.id.text_report_detail_log);

        long timestamp = getIntent().getLongExtra(EXTRA_TIMESTAMP, -1);
        try {
            TestReport report = ReportStore.readReport(ReportStore.fileForTimestamp(this, timestamp));
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
            headerText.setText(dateFormat.format(new Date(report.timestamp)) + "\n"
                    + report.model + " — Android " + report.androidVersion + "\n"
                    + report.fingerprint + "\n"
                    + getString(R.string.robot_summary_format, report.passCount, report.failCount,
                    report.naCount, report.manualCount));

            SpannableStringBuilder builder = new SpannableStringBuilder();
            for (TestReport.CheckEntry entry : report.checks) {
                ReportRenderer.appendLine(this, builder, entry.name, entry.status, entry.detail);
            }
            logText.setText(builder);
        } catch (Exception e) {
            headerText.setText(getString(R.string.report_detail_load_error));
        }
    }
}
