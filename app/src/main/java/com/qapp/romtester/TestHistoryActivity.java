package com.qapp.romtester;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TestHistoryActivity extends Activity implements View.OnClickListener {

    private ListView listView;
    private List<TestReport> reports;
    private ArrayAdapter<TestReport> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_history);

        listView = findViewById(R.id.list_history);
        findViewById(R.id.button_history_compare).setOnClickListener(this);
        findViewById(R.id.button_history_clear).setOnClickListener(this);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, ReportDetailActivity.class);
            intent.putExtra(ReportDetailActivity.EXTRA_TIMESTAMP, reports.get(position).timestamp);
            startActivity(intent);
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(reports.get(position));
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_history_compare) {
            compareLastTwo();
        } else if (id == R.id.button_history_clear) {
            confirmClearAll();
        }
    }

    private void refresh() {
        reports = ReportStore.listReports(this);
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());

        adapter = new ArrayAdapter<TestReport>(this, android.R.layout.simple_list_item_2,
                android.R.id.text1, reports) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TestReport report = reports.get(position);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                text1.setText(dateFormat.format(new Date(report.timestamp)) + " — " + report.model);
                text2.setText(getString(R.string.robot_summary_format, report.passCount,
                        report.failCount, report.naCount, report.manualCount));
                return view;
            }
        };
        listView.setAdapter(adapter);
    }

    private void compareLastTwo() {
        if (reports.size() < 2) {
            Toast.makeText(this, getString(R.string.history_need_two), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, CompareReportsActivity.class);
        intent.putExtra(CompareReportsActivity.EXTRA_TIMESTAMP_NEW, reports.get(0).timestamp);
        intent.putExtra(CompareReportsActivity.EXTRA_TIMESTAMP_OLD, reports.get(1).timestamp);
        startActivity(intent);
    }

    private void confirmDelete(TestReport report) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.history_confirm_delete)
                .setPositiveButton(R.string.history_button_delete, (dialog, which) -> {
                    ReportStore.deleteReport(this, report.timestamp);
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.history_confirm_clear)
                .setPositiveButton(R.string.history_button_clear, (dialog, which) -> {
                    ReportStore.clearAll(this);
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
