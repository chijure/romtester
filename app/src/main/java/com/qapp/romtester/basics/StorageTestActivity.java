package com.qapp.romtester.basics;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.StorageTester;

import java.util.List;

public class StorageTestActivity extends Activity {

    private TextView statusText;
    private TextView resultsText;
    private LinearLayout volumesContainer;
    private ProgressBar progressBar;
    private Button cancelButton;
    private StorageTester tester;
    private StorageTester.Task runningTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_test);

        statusText = findViewById(R.id.text_storage_status);
        resultsText = findViewById(R.id.text_storage_results);
        volumesContainer = findViewById(R.id.layout_storage_volumes);
        progressBar = findViewById(R.id.progress_storage);
        cancelButton = findViewById(R.id.button_storage_cancel);
        cancelButton.setOnClickListener(v -> {
            if (runningTask != null) {
                runningTask.cancel(false);
            }
        });

        tester = new StorageTester(this);
        populateVolumes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (runningTask != null) {
            runningTask.cancel(false);
        }
    }

    private void populateVolumes() {
        volumesContainer.removeAllViews();
        List<StorageTester.Volume> volumes = tester.detectVolumes(
                getString(R.string.storage_volume_internal),
                index -> getString(R.string.storage_volume_external_format, index),
                getString(R.string.storage_volume_app));
        for (StorageTester.Volume volume : volumes) {
            long available = tester.availableBytes(volume.dir);

            TextView header = new TextView(this);
            header.setText(getString(R.string.storage_volume_header_format, volume.label, formatBytes(available)));
            header.setTypeface(header.getTypeface(), Typeface.BOLD);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            headerParams.topMargin = 16;
            header.setLayoutParams(headerParams);
            volumesContainer.addView(header);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = 4;
            row.setLayoutParams(rowParams);

            Button quickButton = new Button(this);
            quickButton.setText(R.string.storage_button_quick);
            LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            quickParams.rightMargin = 4;
            quickButton.setLayoutParams(quickParams);
            quickButton.setOnClickListener(v -> startTest(volume, true));
            row.addView(quickButton);

            Button fullButton = new Button(this);
            fullButton.setText(R.string.storage_button_full);
            LinearLayout.LayoutParams fullParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            fullParams.leftMargin = 4;
            fullButton.setLayoutParams(fullParams);
            fullButton.setOnClickListener(v -> startTest(volume, false));
            row.addView(fullButton);

            volumesContainer.addView(row);
        }
        statusText.setText(getString(R.string.storage_status_ready));
    }

    private void startTest(StorageTester.Volume volume, boolean quick) {
        volumesContainer.setVisibility(View.GONE);
        cancelButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        resultsText.setText("");

        runningTask = tester.startTest(volume, quick, new StorageTester.Listener() {
            @Override
            public void onProgress(int phase, long doneBytes, long totalBytes) {
                int percent = totalBytes > 0 ? (int) (1000L * doneBytes / totalBytes) : 0;
                progressBar.setProgress(percent);
                int formatRes = phase == StorageTester.PHASE_WRITE
                        ? R.string.storage_status_writing_format
                        : R.string.storage_status_verifying_format;
                statusText.setText(getString(formatRes, formatBytes(doneBytes), formatBytes(totalBytes)));
            }

            @Override
            public void onFinished(StorageTester.Result result) {
                onTestFinished(result);
            }
        });
    }

    private void onTestFinished(StorageTester.Result result) {
        runningTask = null;
        progressBar.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        volumesContainer.setVisibility(View.VISIBLE);
        populateVolumes();

        if (result.cancelled) {
            resultsText.setText(getString(R.string.storage_status_cancelled));
            return;
        }
        if (result.insufficientSpace) {
            resultsText.setText(getString(R.string.storage_status_insufficient_space));
            return;
        }

        double writeSpeed = StorageTester.speedMBps(result.writtenBytes, result.writeDurationMs);
        double readSpeed = StorageTester.speedMBps(result.writtenBytes, result.readDurationMs);

        if (result.errorMessage != null) {
            resultsText.setText(getString(R.string.storage_result_write_error_format,
                    formatBytes(result.writtenBytes), formatBytes(result.targetBytes), result.errorMessage));
        } else if (result.corruptedBytes > 0) {
            resultsText.setText(getString(R.string.storage_result_fake_format,
                    formatBytes(result.verifiedGoodBytes), formatBytes(result.corruptedBytes),
                    result.filesCorrupted, writeSpeed, readSpeed));
        } else {
            resultsText.setText(getString(R.string.storage_result_ok_format,
                    formatBytes(result.writtenBytes), writeSpeed, readSpeed));
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) {
            return getString(R.string.storage_size_gb_format, bytes / (1024.0 * 1024 * 1024));
        }
        return getString(R.string.storage_size_mb_format, bytes / (1024.0 * 1024));
    }
}
