package com.qapp.romtester.basics;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.CameraTester;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Battery stress test: measures how far the cell voltage sags under a heavy,
 * fixed load (GPU + all CPU cores + torch). A worn battery has a high
 * internal resistance; its voltage drops sharply under load even when the
 * charge level looks fine, and below the PMIC cutoff the phone resets.
 *
 * Voltage is sampled from /sys/class/power_supply/battery/voltage_now every
 * 250 ms. Some kernels cache that value for tens of seconds; the test then
 * warns that the readings are not live.
 */
public class BatteryTestActivity extends Activity {

    private static final String TAG = "QAppBattery";
    private static final String VOLTAGE_NODE = "/sys/class/power_supply/battery/voltage_now";

    private static final long SAMPLE_MS = 250;
    private static final long IDLE_MS = 10_000;
    private static final long LOAD_MS = 30_000;
    private static final long RECOVERY_MS = 10_000;

    // Progress survives a reset: a worn battery can cut power mid-test.
    private static final String PREFS = "battery_test";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_PHASE = "phase";
    private static final String KEY_MIN_MV = "min_mv";
    private static final String KEY_IDLE_MV = "idle_mv";
    private static final String KEY_ELAPSED_S = "elapsed_s";

    private enum Phase { WAIT_UNPLUG, IDLE, LOAD, RECOVERY, DONE }

    private final Handler handler = new Handler();
    private final List<Integer> idleSamples = new ArrayList<>();
    private final List<Integer> loadSamples = new ArrayList<>();
    private final List<Integer> recoverySamples = new ArrayList<>();

    private TextView statusText;
    private GLSurfaceView glSurfaceView;
    private CameraTester cameraTester;
    private Phase phase = Phase.WAIT_UNPLUG;
    private long phaseStart;
    private int startLevel = -1;
    private int startTempTenths = Integer.MIN_VALUE;
    private boolean torchOn;
    private volatile boolean cpuLoad;
    private final List<Thread> cpuThreads = new ArrayList<>();
    private String interruptedReport;
    private long lastSaveMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(new LoadRenderer());
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        root.addView(glSurfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(this);
        statusText = new TextView(this);
        statusText.setTextColor(Color.WHITE);
        statusText.setBackgroundColor(0xCC000000);
        statusText.setPadding(24, 16, 24, 16);
        statusText.setTypeface(Typeface.MONOSPACE);
        scroll.addView(statusText);
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        scrollParams.gravity = Gravity.TOP;
        root.addView(scroll, scrollParams);

        setContentView(root);

        interruptedReport = readInterruptedRun();

        cameraTester = new CameraTester(this, new CameraTester.Listener() {
            @Override public void onCaptureUnavailable() { }
            @Override public void onPhotoCaptured(Bitmap thumbnail) { }
            @Override public void onCaptureCancelled() { }
            @Override public void onTorchStateChanged(boolean on) { }
            @Override public void onTorchError(String message) {
                Log.w(TAG, "torch: " + message);
            }
            @Override public void onFindTorchError(String message) {
                Log.w(TAG, "torch lookup: " + message);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        if (phase != Phase.DONE) {
            restart();
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        stopLoad();
        glSurfaceView.onPause();
        if (phase != Phase.DONE) {
            phase = Phase.WAIT_UNPLUG;
        }
        super.onPause();
    }

    private void restart() {
        idleSamples.clear();
        loadSamples.clear();
        recoverySamples.clear();
        phase = Phase.WAIT_UNPLUG;
        handler.post(tick);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.elapsedRealtime();
            switch (phase) {
                case WAIT_UNPLUG:
                    if (isPlugged()) {
                        statusText.setText(interruptedReport != null
                                ? interruptedReport + "\n\n" + getString(R.string.battery_test_unplug)
                                : getString(R.string.battery_test_unplug));
                        handler.postDelayed(this, 1000);
                        return;
                    }
                    Intent battery = batteryIntent();
                    startLevel = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    startTempTenths = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,
                            Integer.MIN_VALUE);
                    enterPhase(Phase.IDLE, now);
                    break;
                case IDLE:
                    idleSamples.add(readVoltageMv());
                    if (now - phaseStart >= IDLE_MS) {
                        startLoad();
                        enterPhase(Phase.LOAD, now);
                    }
                    break;
                case LOAD:
                    loadSamples.add(readVoltageMv());
                    if (now - phaseStart >= LOAD_MS) {
                        stopLoad();
                        enterPhase(Phase.RECOVERY, now);
                    }
                    break;
                case RECOVERY:
                    recoverySamples.add(readVoltageMv());
                    if (now - phaseStart >= RECOVERY_MS) {
                        phase = Phase.DONE;
                        clearProgress();
                        showResults();
                        return;
                    }
                    break;
                default:
                    return;
            }
            if (isPlugged() && phase != Phase.DONE) {
                // Charger current masks the sag; start over.
                stopLoad();
                restart();
                return;
            }
            showProgress(now);
            saveProgress(now);
            handler.postDelayed(this, SAMPLE_MS);
        }
    };

    private void enterPhase(Phase next, long now) {
        phase = next;
        phaseStart = now;
    }

    private void showProgress(long now) {
        int phaseName;
        long total;
        switch (phase) {
            case IDLE: phaseName = R.string.battery_test_phase_idle; total = IDLE_MS; break;
            case LOAD: phaseName = R.string.battery_test_phase_load; total = LOAD_MS; break;
            default: phaseName = R.string.battery_test_phase_recovery; total = RECOVERY_MS; break;
        }
        long left = Math.max(0, (total - (now - phaseStart)) / 1000);
        int current = lastSample();
        statusText.setText(getString(R.string.battery_test_progress_format,
                getString(phaseName), left, current / 1000.0));
    }

    private int lastSample() {
        List<Integer> list = phase == Phase.IDLE ? idleSamples
                : phase == Phase.LOAD ? loadSamples : recoverySamples;
        return list.isEmpty() ? 0 : list.get(list.size() - 1);
    }

    private void showResults() {
        int idle = median(idleSamples);
        int loadMin = min(loadSamples);
        int loadAvg = average(loadSamples);
        int recovered = median(recoverySamples);
        int drop = idle - loadMin;
        boolean live = distinct(loadSamples) >= 3;

        int verdict;
        if (!live || idle <= 0) {
            verdict = R.string.battery_test_verdict_unknown;
        } else if (drop <= 300 && loadMin >= 3650) {
            // Calibrated on a D625: a healthy cell drops ~0.25 V under this
            // load; the worn one reset the phone before the load phase ended.
            verdict = R.string.battery_test_verdict_good;
        } else if (drop <= 450 && loadMin >= 3500) {
            verdict = R.string.battery_test_verdict_fair;
        } else {
            verdict = R.string.battery_test_verdict_poor;
        }

        String temp = startTempTenths == Integer.MIN_VALUE ? "?"
                : String.format(Locale.US, "%.1f", startTempTenths / 10.0);
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.battery_test_result_format,
                startLevel, temp,
                idle / 1000.0, loadAvg / 1000.0, loadMin / 1000.0,
                drop / 1000.0, recovered / 1000.0));
        sb.append('\n').append(getString(verdict));
        if (!live) {
            sb.append('\n').append(getString(R.string.battery_test_not_live));
        }
        statusText.setText(sb.toString());

        Log.i(TAG, String.format(Locale.US,
                "level=%d%% temp=%s idle=%dmV loadAvg=%dmV loadMin=%dmV drop=%dmV"
                        + " recovered=%dmV live=%b samples=%d/%d/%d",
                startLevel, temp, idle, loadAvg, loadMin, drop, recovered, live,
                idleSamples.size(), loadSamples.size(), recoverySamples.size()));
        Log.i(TAG, "load samples (mV): " + loadSamples);
    }

    private void startLoad() {
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        cpuLoad = true;
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < cores; i++) {
            Thread t = new Thread(() -> {
                double x = 1.0;
                while (cpuLoad) {
                    for (int k = 0; k < 100000; k++) {
                        x = Math.sin(x) * Math.cos(x) + 1.0001;
                    }
                }
                if (x == 42) {
                    Log.v(TAG, "unreachable");
                }
            }, "battery-load-" + i);
            t.start();
            cpuThreads.add(t);
        }
        if (!torchOn) {
            cameraTester.setTorch(true);
            torchOn = true;
        }
    }

    private void stopLoad() {
        cpuLoad = false;
        cpuThreads.clear();
        if (glSurfaceView != null) {
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
        if (torchOn) {
            cameraTester.setTorch(false);
            torchOn = false;
        }
    }

    private void saveProgress(long now) {
        if (now - lastSaveMs < 1000) {
            return;
        }
        lastSaveMs = now;
        int minMv = phase == Phase.IDLE ? min(idleSamples)
                : Math.min(nonZeroMin(loadSamples), nonZeroMin(recoverySamples));
        // commit() (not apply()) so the value is on disk before a power cut.
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_RUNNING, true)
                .putString(KEY_PHASE, phase.name())
                .putInt(KEY_IDLE_MV, median(idleSamples))
                .putInt(KEY_MIN_MV, minMv)
                .putLong(KEY_ELAPSED_S, (now - phaseStart) / 1000)
                .commit();
    }

    private void clearProgress() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().commit();
    }

    /** Report for a run that never finished (phone reset), or null. */
    private String readInterruptedRun() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_RUNNING, false)) {
            return null;
        }
        String phaseName = prefs.getString(KEY_PHASE, Phase.IDLE.name());
        int phaseLabel = Phase.LOAD.name().equals(phaseName) ? R.string.battery_test_phase_load
                : Phase.RECOVERY.name().equals(phaseName) ? R.string.battery_test_phase_recovery
                : R.string.battery_test_phase_idle;
        String report = getString(R.string.battery_test_interrupted_format,
                getString(phaseLabel), prefs.getLong(KEY_ELAPSED_S, 0),
                prefs.getInt(KEY_IDLE_MV, 0) / 1000.0, prefs.getInt(KEY_MIN_MV, 0) / 1000.0);
        Log.i(TAG, "interrupted run: phase=" + phaseName
                + " elapsed=" + prefs.getLong(KEY_ELAPSED_S, 0) + "s idle="
                + prefs.getInt(KEY_IDLE_MV, 0) + "mV min=" + prefs.getInt(KEY_MIN_MV, 0) + "mV");
        clearProgress();
        return report;
    }

    private static int nonZeroMin(List<Integer> values) {
        return values.isEmpty() ? Integer.MAX_VALUE : Collections.min(values);
    }

    private Intent batteryIntent() {
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return intent != null ? intent : new Intent();
    }

    private boolean isPlugged() {
        return batteryIntent().getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
    }

    /** Cell voltage in mV; falls back to the (slow) battery broadcast. */
    private int readVoltageMv() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(VOLTAGE_NODE));
            String line = reader.readLine();
            if (line != null) {
                long value = Long.parseLong(line.trim());
                // Most kernels report microvolts, a few report millivolts.
                return (int) (value > 100000 ? value / 1000 : value);
            }
        } catch (Exception ignored) {
            // fall through
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                    // nothing to do
                }
            }
        }
        return batteryIntent().getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
    }

    private static int median(List<Integer> values) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    private static int min(List<Integer> values) {
        return values.isEmpty() ? 0 : Collections.min(values);
    }

    private static int average(List<Integer> values) {
        if (values.isEmpty()) {
            return 0;
        }
        long sum = 0;
        for (int v : values) {
            sum += v;
        }
        return (int) (sum / values.size());
    }

    private static int distinct(List<Integer> values) {
        return new java.util.HashSet<>(values).size();
    }

    /** Heavy full-screen fragment shader, only drawn during the load phase. */
    private static class LoadRenderer implements GLSurfaceView.Renderer {

        private static final String VS =
                "attribute vec2 a_pos;\n"
                + "varying vec2 v_uv;\n"
                + "void main() { v_uv = a_pos * 0.5 + 0.5; gl_Position = vec4(a_pos, 0.0, 1.0); }\n";

        private static final String FS =
                "precision mediump float;\n"
                + "varying vec2 v_uv;\n"
                + "uniform float u_time;\n"
                + "void main() {\n"
                + "  vec2 p = v_uv * 4.0;\n"
                + "  float a = 0.0;\n"
                + "  for (int i = 0; i < 24; i++) {\n"
                + "    float fi = float(i);\n"
                + "    a += sin(p.x * 1.3 + fi + u_time) * cos(p.y * 1.7 - fi * 0.5);\n"
                + "    p = vec2(p.y + a * 0.1, p.x - a * 0.1);\n"
                + "  }\n"
                + "  gl_FragColor = vec4(fract(a) * 0.4, fract(a * 0.5) * 0.4, fract(a * 0.25), 1.0);\n"
                + "}\n";

        private final FloatBuffer quad;
        private int program;
        private float time;

        LoadRenderer() {
            float[] v = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};
            quad = ByteBuffer.allocateDirect(v.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            quad.put(v).position(0);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, compile(GLES20.GL_VERTEX_SHADER, VS));
            GLES20.glAttachShader(program, compile(GLES20.GL_FRAGMENT_SHADER, FS));
            GLES20.glLinkProgram(program);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            time += 0.016f;
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(program);
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "u_time"), time);
            int pos = GLES20.glGetAttribLocation(program, "a_pos");
            GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 0, quad);
            GLES20.glEnableVertexAttribArray(pos);
            // Several passes per frame so the GPU stays saturated even at vsync.
            for (int i = 0; i < 4; i++) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            }
        }

        private static int compile(int type, String source) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            return shader;
        }
    }
}
