package com.qapp.romtester.tester;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;

import com.qapp.romtester.util.StorageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StorageTester {

    private static final int BUFFER_SIZE = 256 * 1024;
    static final long MIN_FILE_SIZE = 16L * 1024 * 1024;
    static final long MAX_FILE_SIZE = 512L * 1024 * 1024;
    static final int DESIRED_FILE_COUNT = 128;
    private static final long MIN_SAFETY_MARGIN_BYTES = 100L * 1024 * 1024;
    private static final double SAFETY_MARGIN_FRACTION = 0.03;
    private static final long MIN_TEST_BYTES = 32L * 1024 * 1024;
    private static final long QUICK_TEST_BYTES = 128L * 1024 * 1024;

    public static final int PHASE_WRITE = 0;
    public static final int PHASE_VERIFY = 1;

    public static class Volume {
        public final String label;
        public final File dir;

        public Volume(String label, File dir) {
            this.label = label;
            this.dir = dir;
        }
    }

    public static class Result {
        public boolean cancelled;
        public boolean insufficientSpace;
        public long targetBytes;
        public long writtenBytes;
        public long verifiedGoodBytes;
        public long corruptedBytes;
        public int filesCorrupted;
        public long writeDurationMs;
        public long readDurationMs;
        public String errorMessage;
    }

    public interface Listener {
        void onProgress(int phase, long doneBytes, long totalBytes);

        void onFinished(Result result);
    }

    private final Context context;

    public StorageTester(Context context) {
        this.context = context;
    }

    public interface ExternalLabelResolver {
        String resolve(int index);
    }

    public List<Volume> detectVolumes(String internalLabel, ExternalLabelResolver externalLabel,
                                       String appLabel) {
        List<Volume> volumes = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 19) {
            File[] dirs = context.getExternalFilesDirs(null);
            for (int i = 0; i < dirs.length; i++) {
                File d = dirs[i];
                if (d == null) {
                    continue;
                }
                String label = (i == 0) ? internalLabel : externalLabel.resolve(i);
                volumes.add(new Volume(label, d));
            }
        } else {
            File d = context.getExternalFilesDir(null);
            if (d != null) {
                volumes.add(new Volume(internalLabel, d));
            }
        }
        if (volumes.isEmpty()) {
            volumes.add(new Volume(appLabel, context.getFilesDir()));
        }
        return volumes;
    }

    public long availableBytes(File dir) {
        return StorageUtils.availableBytes(dir);
    }

    public static double speedMBps(long bytes, long durationMs) {
        if (durationMs <= 0) {
            return 0;
        }
        return (bytes / (1024.0 * 1024.0)) / (durationMs / 1000.0);
    }

    public Task startTest(Volume volume, boolean quick, Listener listener) {
        Task task = new Task(volume.dir, quick, listener);
        task.execute();
        return task;
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

    public static class Task extends AsyncTask<Void, Progress, Result> {

        private final File volumeDir;
        private final boolean quick;
        private final Listener listener;

        Task(File volumeDir, boolean quick, Listener listener) {
            this.volumeDir = volumeDir;
            this.quick = quick;
            this.listener = listener;
        }

        @Override
        protected Result doInBackground(Void... params) {
            File testDir = new File(volumeDir, "qapp_storage_test");
            testDir.mkdirs();

            Result result = new Result();
            long available = StorageUtils.availableBytes(volumeDir);
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
            listener.onProgress(p.phase, p.doneBytes, p.totalBytes);
        }

        @Override
        protected void onPostExecute(Result result) {
            listener.onFinished(result);
        }

        @Override
        protected void onCancelled(Result result) {
            result.cancelled = true;
            listener.onFinished(result);
        }
    }
}
