package com.qapp.romtester.basics;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qapp.romtester.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class StorageTestActivity extends Activity {

    private static final int BUFFER_SIZE = 256 * 1024;
    static final long MIN_FILE_SIZE = 16L * 1024 * 1024;
    static final long MAX_FILE_SIZE = 512L * 1024 * 1024;
    static final int DESIRED_FILE_COUNT = 128;
    private static final long MIN_SAFETY_MARGIN_BYTES = 100L * 1024 * 1024;
    private static final double SAFETY_MARGIN_FRACTION = 0.03;
    private static final long MIN_TEST_BYTES = 32L * 1024 * 1024;
    private static final long QUICK_TEST_BYTES = 128L * 1024 * 1024;

    private static final int PHASE_WRITE = 0;
    private static final int PHASE_VERIFY = 1;

    private TextView statusText;
    private TextView resultsText;
    private LinearLayout volumesContainer;
    private ProgressBar progressBar;
    private Button cancelButton;
    private StorageTestTask runningTask;

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
        List<Volume> volumes = detectVolumes();
        for (Volume volume : volumes) {
            long available = getAvailableBytes(volume.dir);

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

    private void startTest(Volume volume, boolean quick) {
        volumesContainer.setVisibility(View.GONE);
        cancelButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        resultsText.setText("");
        runningTask = new StorageTestTask();
        runningTask.execute(new TestRequest(volume.dir, quick));
    }

    private void onTestFinished(Result result) {
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

        double writeSpeed = speedMBps(result.writtenBytes, result.writeDurationMs);
        double readSpeed = speedMBps(result.writtenBytes, result.readDurationMs);

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

    private static double speedMBps(long bytes, long durationMs) {
        if (durationMs <= 0) {
            return 0;
        }
        return (bytes / (1024.0 * 1024.0)) / (durationMs / 1000.0);
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) {
            return getString(R.string.storage_size_gb_format, bytes / (1024.0 * 1024 * 1024));
        }
        return getString(R.string.storage_size_mb_format, bytes / (1024.0 * 1024));
    }

    @SuppressWarnings("deprecation")
    private long getAvailableBytes(File dir) {
        StatFs statFs = new StatFs(dir.getAbsolutePath());
        return (long) statFs.getBlockSize() * (long) statFs.getAvailableBlocks();
    }

    private List<Volume> detectVolumes() {
        List<Volume> volumes = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 19) {
            File[] dirs = getExternalFilesDirs(null);
            for (int i = 0; i < dirs.length; i++) {
                File d = dirs[i];
                if (d == null) {
                    continue;
                }
                String label = (i == 0)
                        ? getString(R.string.storage_volume_internal)
                        : getString(R.string.storage_volume_external_format, i);
                volumes.add(new Volume(label, d));
            }
        } else {
            File d = getExternalFilesDir(null);
            if (d != null) {
                volumes.add(new Volume(getString(R.string.storage_volume_internal), d));
            }
        }
        if (volumes.isEmpty()) {
            volumes.add(new Volume(getString(R.string.storage_volume_app), getFilesDir()));
        }
        return volumes;
    }

    static long computeFileSize(long target) {
        long size = target / DESIRED_FILE_COUNT;
        if (size < MIN_FILE_SIZE) {
            size = MIN_FILE_SIZE;
        }
        if (size > MAX_FILE_SIZE) {
            size = MAX_FILE_SIZE;
        }
        return size;
    }

    static int readFully(InputStream in, byte[] buf, int len) throws IOException {
        int total = 0;
        while (total < len) {
            int r = in.read(buf, total, len - total);
            if (r < 0) {
                break;
            }
            total += r;
        }
        return total;
    }

    static boolean regionsEqual(byte[] a, byte[] b, int len) {
        for (int i = 0; i < len; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static class Volume {
        final String label;
        final File dir;

        Volume(String label, File dir) {
            this.label = label;
            this.dir = dir;
        }
    }

    private static class TestRequest {
        final File dir;
        final boolean quick;

        TestRequest(File dir, boolean quick) {
            this.dir = dir;
            this.quick = quick;
        }
    }

    private static class Progress {
        final int phase;
        final long doneBytes;
        final long totalBytes;

        Progress(int phase, long doneBytes, long totalBytes) {
            this.phase = phase;
            this.doneBytes = doneBytes;
            this.totalBytes = totalBytes;
        }
    }

    private static class Result {
        boolean cancelled;
        boolean insufficientSpace;
        long targetBytes;
        long writtenBytes;
        long verifiedGoodBytes;
        long corruptedBytes;
        int filesCorrupted;
        long writeDurationMs;
        long readDurationMs;
        String errorMessage;
    }

    private class StorageTestTask extends AsyncTask<TestRequest, Progress, Result> {

        @Override
        protected Result doInBackground(TestRequest... params) {
            File volumeDir = params[0].dir;
            boolean quick = params[0].quick;
            File testDir = new File(volumeDir, "qapp_storage_test");
            testDir.mkdirs();

            Result result = new Result();
            long available = getAvailableBytes(volumeDir);
            long margin = Math.max(MIN_SAFETY_MARGIN_BYTES, (long) (available * SAFETY_MARGIN_FRACTION));
            long target = available - margin;
            if (quick) {
                target = Math.min(target, QUICK_TEST_BYTES);
            }
            if (target < MIN_TEST_BYTES) {
                result.insufficientSpace = true;
                return result;
            }
            result.targetBytes = target;

            long fileSize = computeFileSize(target);
            long runSeed = System.nanoTime();
            byte[] buffer = new byte[BUFFER_SIZE];
            List<File> files = new ArrayList<>();

            long writeStart = SystemClock.elapsedRealtime();
            long writtenTotal = 0;
            int fileIndex = 0;
            writeLoop:
            while (writtenTotal + fileSize <= target) {
                if (isCancelled()) {
                    break;
                }
                File f = new File(testDir, "chunk_" + fileIndex + ".bin");
                Random rnd = new Random(runSeed ^ (fileIndex * 0x9E3779B97F4A7C15L));
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    long remaining = fileSize;
                    while (remaining > 0) {
                        if (isCancelled()) {
                            break writeLoop;
                        }
                        int chunk = (int) Math.min(buffer.length, remaining);
                        rnd.nextBytes(buffer);
                        fos.write(buffer, 0, chunk);
                        remaining -= chunk;
                        writtenTotal += chunk;
                        publishProgress(new Progress(PHASE_WRITE, writtenTotal, target));
                    }
                    fos.getFD().sync();
                } catch (IOException e) {
                    result.errorMessage = e.getMessage();
                    break;
                }
                files.add(f);
                fileIndex++;
            }
            result.writeDurationMs = SystemClock.elapsedRealtime() - writeStart;
            result.writtenBytes = writtenTotal;

            long readStart = SystemClock.elapsedRealtime();
            long verifiedGood = 0;
            long corrupted = 0;
            int filesCorrupted = 0;
            boolean brokenStreakStarted = false;
            byte[] expected = new byte[BUFFER_SIZE];
            byte[] actual = new byte[BUFFER_SIZE];
            for (int i = 0; i < files.size(); i++) {
                if (isCancelled()) {
                    break;
                }
                File f = files.get(i);
                Random rnd = new Random(runSeed ^ (i * 0x9E3779B97F4A7C15L));
                boolean fileOk = true;
                try (FileInputStream fis = new FileInputStream(f)) {
                    long remaining = fileSize;
                    long doneBeforeFile = (long) i * fileSize;
                    while (remaining > 0) {
                        int chunk = (int) Math.min(BUFFER_SIZE, remaining);
                        rnd.nextBytes(expected);
                        int read = readFully(fis, actual, chunk);
                        if (read != chunk || !regionsEqual(expected, actual, chunk)) {
                            fileOk = false;
                        }
                        remaining -= chunk;
                        publishProgress(new Progress(PHASE_VERIFY, doneBeforeFile + (fileSize - remaining), writtenTotal));
                    }
                } catch (IOException e) {
                    fileOk = false;
                }
                if (fileOk) {
                    if (!brokenStreakStarted) {
                        verifiedGood += fileSize;
                    }
                } else {
                    filesCorrupted++;
                    corrupted += fileSize;
                    brokenStreakStarted = true;
                }
            }
            result.readDurationMs = SystemClock.elapsedRealtime() - readStart;
            result.verifiedGoodBytes = verifiedGood;
            result.corruptedBytes = corrupted;
            result.filesCorrupted = filesCorrupted;

            File[] leftover = testDir.listFiles();
            if (leftover != null) {
                for (File lf : leftover) {
                    lf.delete();
                }
            }
            testDir.delete();

            return result;
        }

        @Override
        protected void onProgressUpdate(Progress... values) {
            Progress p = values[0];
            int percent = p.totalBytes > 0 ? (int) (1000L * p.doneBytes / p.totalBytes) : 0;
            progressBar.setProgress(percent);
            int formatRes = p.phase == PHASE_WRITE
                    ? R.string.storage_status_writing_format
                    : R.string.storage_status_verifying_format;
            statusText.setText(getString(formatRes, formatBytes(p.doneBytes), formatBytes(p.totalBytes)));
        }

        @Override
        protected void onPostExecute(Result result) {
            onTestFinished(result);
        }

        @Override
        protected void onCancelled(Result result) {
            result.cancelled = true;
            onTestFinished(result);
        }
    }
}
