package com.qapp.romtester.media;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.qapp.romtester.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GPU benchmark that is not capped by the display refresh rate.
 *
 * The on-screen OpenGL test is vsync-limited (eglSwapBuffers waits for the
 * next refresh), so it reports the panel rate (usually 60) on any GPU that can
 * keep up. Here every test renders into an offscreen framebuffer object and is
 * timed with glFinish() + System.nanoTime(), which measures the GPU work itself
 * on any Android version (eglSwapInterval(0) would need EGL14, API 17).
 */
public class GpuBenchmarkActivity extends Activity {

    private static final String TAG = "QAppGpuBench";

    private GLSurfaceView glSurfaceView;
    private TextView resultsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);

        if (android.os.Build.VERSION.SDK_INT < 9) {
            // VBO-offset glVertexAttribPointer/glDrawElements need API 9.
            TextView unsupported = new TextView(this);
            unsupported.setText(R.string.gpu_bench_unsupported);
            setContentView(unsupported);
            return;
        }

        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(new BenchmarkRenderer());
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        root.addView(glSurfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(this);
        resultsText = new TextView(this);
        resultsText.setTextColor(Color.WHITE);
        resultsText.setBackgroundColor(0xAA000000);
        resultsText.setPadding(24, 16, 24, 16);
        resultsText.setTypeface(android.graphics.Typeface.MONOSPACE);
        resultsText.setText(getString(R.string.gpu_bench_status_starting));
        scroll.addView(resultsText);
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        scrollParams.gravity = Gravity.TOP;
        root.addView(scroll, scrollParams);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        super.onPause();
    }

    private void showText(final String text) {
        runOnUiThread(() -> resultsText.setText(text));
    }

    private class BenchmarkRenderer implements GLSurfaceView.Renderer {

        // Offscreen targets.
        private static final int FILL_SIZE = 1024;
        private static final int SCENE_W = 1280;
        private static final int SCENE_H = 720;
        private static final int GEOMETRY_SIZE = 256;
        private static final int TEXTURE_SIZE = 512;

        // Grid meshes (indexed, GL_UNSIGNED_SHORT).
        private static final int GEO_COLS = 200;
        private static final int GEO_ROWS = 150;
        private static final int SCENE_COLS = 100;
        private static final int SCENE_ROWS = 100;

        private static final long WARMUP_NS = 300_000_000L;
        private static final long MEASURE_NS = 2_000_000_000L;
        private static final long MIN_BATCH_NS = 50_000_000L;

        private static final String QUAD_VS =
                "attribute vec2 a_pos;\n"
                + "varying vec2 v_uv;\n"
                + "void main() {\n"
                + "  v_uv = a_pos * 0.5 + 0.5;\n"
                + "  gl_Position = vec4(a_pos, 0.0, 1.0);\n"
                + "}\n";

        private static final String FILL_FS =
                "precision mediump float;\n"
                + "uniform vec4 u_color;\n"
                + "void main() { gl_FragColor = u_color; }\n";

        private static final String ALU_FS =
                "precision mediump float;\n"
                + "varying vec2 v_uv;\n"
                + "uniform float u_time;\n"
                + "void main() {\n"
                + "  vec2 p = v_uv * 4.0;\n"
                + "  float a = 0.0;\n"
                + "  for (int i = 0; i < 16; i++) {\n"
                + "    float fi = float(i);\n"
                + "    a += sin(p.x * 1.3 + fi + u_time) * cos(p.y * 1.7 - fi * 0.5);\n"
                + "    p = vec2(p.y + a * 0.1, p.x - a * 0.1);\n"
                + "  }\n"
                + "  gl_FragColor = vec4(fract(a), fract(a * 0.5), fract(a * 0.25), 1.0);\n"
                + "}\n";

        private static final String TEXTURE_FS =
                "precision mediump float;\n"
                + "varying vec2 v_uv;\n"
                + "uniform sampler2D u_tex;\n"
                + "uniform float u_time;\n"
                + "void main() {\n"
                + "  vec2 o = vec2(u_time * 0.01, 0.0);\n"
                + "  vec4 c = texture2D(u_tex, v_uv + o)\n"
                + "         + texture2D(u_tex, v_uv * 1.7 - o)\n"
                + "         + texture2D(u_tex, v_uv * 0.6 + o.yx)\n"
                + "         + texture2D(u_tex, v_uv.yx * 1.3);\n"
                + "  gl_FragColor = c * 0.25;\n"
                + "}\n";

        private static final String MESH_VS =
                "attribute vec2 a_grid;\n"
                + "uniform float u_time;\n"
                + "varying vec3 v_normal;\n"
                + "varying vec2 v_uv;\n"
                + "void main() {\n"
                + "  float h = sin(a_grid.x * 6.0 + u_time) * cos(a_grid.y * 5.0 - u_time) * 0.15;\n"
                + "  float dx = cos(a_grid.x * 6.0 + u_time) * 6.0 * cos(a_grid.y * 5.0 - u_time) * 0.15;\n"
                + "  float dy = -sin(a_grid.x * 6.0 + u_time) * sin(a_grid.y * 5.0 - u_time) * 5.0 * 0.15;\n"
                + "  v_normal = normalize(vec3(-dx, -dy, 1.0));\n"
                + "  v_uv = a_grid;\n"
                + "  vec2 xy = a_grid * 1.8 - 0.9;\n"
                + "  float c = cos(u_time * 0.3);\n"
                + "  float s = sin(u_time * 0.3);\n"
                + "  gl_Position = vec4(xy.x * c - xy.y * s, (xy.x * s + xy.y * c) * 0.8 + h, h, 1.0);\n"
                + "}\n";

        private static final String MESH_FLAT_FS =
                "precision mediump float;\n"
                + "varying vec3 v_normal;\n"
                + "varying vec2 v_uv;\n"
                + "void main() { gl_FragColor = vec4(v_normal * 0.5 + 0.5, 1.0); }\n";

        private static final String MESH_LIT_FS =
                "precision mediump float;\n"
                + "varying vec3 v_normal;\n"
                + "varying vec2 v_uv;\n"
                + "uniform sampler2D u_tex;\n"
                + "void main() {\n"
                + "  vec3 n = normalize(v_normal);\n"
                + "  vec3 l = normalize(vec3(0.4, 0.6, 0.7));\n"
                + "  vec3 h = normalize(l + vec3(0.0, 0.0, 1.0));\n"
                + "  float diff = max(dot(n, l), 0.0);\n"
                + "  float spec = pow(max(dot(n, h), 0.0), 24.0);\n"
                + "  vec3 base = texture2D(u_tex, v_uv * 3.0).rgb;\n"
                + "  gl_FragColor = vec4(base * (0.2 + 0.8 * diff) + vec3(spec), 1.0);\n"
                + "}\n";

        private final FloatBuffer quad;

        private int fillProgram;
        private int aluProgram;
        private int textureProgram;
        private int meshFlatProgram;
        private int meshLitProgram;

        private int texture;
        private int fillFbo;
        private int sceneFbo;
        private int geometryFbo;

        private int geoVbo;
        private int geoIbo;
        private int geoIndexCount;
        private int sceneVbo;
        private int sceneIbo;
        private int sceneIndexCount;

        private String header = "";
        private final StringBuilder results = new StringBuilder();
        private int nextTest;
        private float time;

        BenchmarkRenderer() {
            float[] quadVertices = {-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};
            quad = ByteBuffer.allocateDirect(quadVertices.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            quad.put(quadVertices).position(0);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            header = getString(R.string.gpu_bench_header_format,
                    GLES20.glGetString(GLES20.GL_RENDERER),
                    GLES20.glGetString(GLES20.GL_VENDOR),
                    GLES20.glGetString(GLES20.GL_VERSION));
            results.setLength(0);
            nextTest = 0;

            fillProgram = buildProgram(QUAD_VS, FILL_FS);
            aluProgram = buildProgram(QUAD_VS, ALU_FS);
            textureProgram = buildProgram(QUAD_VS, TEXTURE_FS);
            meshFlatProgram = buildProgram(MESH_VS, MESH_FLAT_FS);
            meshLitProgram = buildProgram(MESH_VS, MESH_LIT_FS);

            texture = createPatternTexture();
            fillFbo = createFbo(FILL_SIZE, FILL_SIZE);
            sceneFbo = createFbo(SCENE_W, SCENE_H);
            geometryFbo = createFbo(GEOMETRY_SIZE, GEOMETRY_SIZE);

            int[] geo = createGrid(GEO_COLS, GEO_ROWS);
            geoVbo = geo[0];
            geoIbo = geo[1];
            geoIndexCount = geo[2];
            int[] scene = createGrid(SCENE_COLS, SCENE_ROWS);
            sceneVbo = scene[0];
            sceneIbo = scene[1];
            sceneIndexCount = scene[2];

            Log.i(TAG, header.replace('\n', ' '));
            showText(header + "\n" + getString(R.string.gpu_bench_status_running));
            glSurfaceView.requestRender();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // One test per frame so the GL thread can pause between tests.
            if (nextTest < 5) {
                runTest(nextTest++);
                showText(header + "\n" + results
                        + (nextTest < 5 ? getString(R.string.gpu_bench_status_running)
                                        : getString(R.string.gpu_bench_status_done)));
                glSurfaceView.requestRender();
            }

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glClearColor(0.05f, 0.05f, 0.08f, 1f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }

        private void runTest(int index) {
            String line;
            switch (index) {
                case 0: {
                    double perSec = measure(fillFbo, FILL_SIZE, FILL_SIZE,
                            () -> drawQuad(fillProgram), (double) FILL_SIZE * FILL_SIZE);
                    line = format(R.string.gpu_bench_fill, perSec / 1e6, "Mpix/s");
                    break;
                }
                case 1: {
                    double perSec = measure(fillFbo, FILL_SIZE, FILL_SIZE,
                            () -> drawQuad(aluProgram), (double) FILL_SIZE * FILL_SIZE);
                    line = format(R.string.gpu_bench_alu, perSec / 1e6, "Mpix/s");
                    break;
                }
                case 2: {
                    // Four texture fetches per pixel.
                    double perSec = measure(fillFbo, FILL_SIZE, FILL_SIZE,
                            () -> drawQuad(textureProgram), 4.0 * FILL_SIZE * FILL_SIZE);
                    line = format(R.string.gpu_bench_texture, perSec / 1e6, "Mtexel/s");
                    break;
                }
                case 3: {
                    double perSec = measure(geometryFbo, GEOMETRY_SIZE, GEOMETRY_SIZE,
                            () -> drawMesh(meshFlatProgram, geoVbo, geoIbo, geoIndexCount),
                            geoIndexCount / 3.0);
                    line = format(R.string.gpu_bench_geometry, perSec / 1e6, "Mtri/s");
                    break;
                }
                default: {
                    double fps = measure(sceneFbo, SCENE_W, SCENE_H, () -> {
                        drawQuad(textureProgram);
                        drawMesh(meshLitProgram, sceneVbo, sceneIbo, sceneIndexCount);
                    }, 1.0);
                    line = format(R.string.gpu_bench_scene, fps, "FPS");
                    break;
                }
            }
            Log.i(TAG, line);
            results.append(line).append('\n');
        }

        private String format(int labelRes, double value, String unit) {
            return String.format(Locale.US, "%-22s %9.1f %s", getString(labelRes), value, unit);
        }

        /**
         * Draws into an offscreen target in batches, each closed by glFinish(),
         * and returns work units per second. The batch grows until it takes at
         * least MIN_BATCH_NS so timer and glFinish overhead stay negligible.
         */
        private double measure(int fbo, int width, int height, Runnable draw, double unitsPerDraw) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
            GLES20.glViewport(0, 0, width, height);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_BLEND);

            int batch = 1;
            long warmupEnd = System.nanoTime() + WARMUP_NS;
            while (true) {
                long start = System.nanoTime();
                runBatch(draw, batch);
                long elapsed = System.nanoTime() - start;
                if (elapsed < MIN_BATCH_NS && batch < (1 << 16)) {
                    batch *= 2;
                } else if (System.nanoTime() >= warmupEnd) {
                    break;
                }
            }

            long totalNs = 0;
            long draws = 0;
            while (totalNs < MEASURE_NS) {
                long start = System.nanoTime();
                runBatch(draw, batch);
                totalNs += System.nanoTime() - start;
                draws += batch;
            }
            return draws * unitsPerDraw * 1e9 / totalNs;
        }

        private void runBatch(Runnable draw, int count) {
            for (int i = 0; i < count; i++) {
                time += 0.016f;
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                draw.run();
            }
            GLES20.glFinish();
        }

        private void drawQuad(int program) {
            GLES20.glUseProgram(program);
            setTimeAndTexture(program);
            int pos = GLES20.glGetAttribLocation(program, "a_pos");
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 0, quad);
            GLES20.glEnableVertexAttribArray(pos);
            int color = GLES20.glGetUniformLocation(program, "u_color");
            if (color >= 0) {
                GLES20.glUniform4f(color, 0.2f, 0.6f, 0.9f, 1f);
            }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        private void drawMesh(int program, int vbo, int ibo, int indexCount) {
            GLES20.glUseProgram(program);
            setTimeAndTexture(program);
            int grid = GLES20.glGetAttribLocation(program, "a_grid");
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
            GLES20.glVertexAttribPointer(grid, 2, GLES20.GL_FLOAT, false, 0, 0);
            GLES20.glEnableVertexAttribArray(grid);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        }

        private void setTimeAndTexture(int program) {
            int timeLoc = GLES20.glGetUniformLocation(program, "u_time");
            if (timeLoc >= 0) {
                GLES20.glUniform1f(timeLoc, time);
            }
            int texLoc = GLES20.glGetUniformLocation(program, "u_tex");
            if (texLoc >= 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
                GLES20.glUniform1i(texLoc, 0);
            }
        }

        /** Grid of (cols x rows) quads in [0,1]^2: {vbo, ibo, indexCount}. */
        private int[] createGrid(int cols, int rows) {
            int vertCount = (cols + 1) * (rows + 1);
            FloatBuffer verts = ByteBuffer.allocateDirect(vertCount * 2 * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            for (int y = 0; y <= rows; y++) {
                for (int x = 0; x <= cols; x++) {
                    verts.put((float) x / cols).put((float) y / rows);
                }
            }
            verts.position(0);

            int indexCount = cols * rows * 6;
            ShortBuffer indices = ByteBuffer.allocateDirect(indexCount * 2)
                    .order(ByteOrder.nativeOrder()).asShortBuffer();
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    int i0 = y * (cols + 1) + x;
                    int i1 = i0 + 1;
                    int i2 = i0 + cols + 1;
                    int i3 = i2 + 1;
                    indices.put((short) i0).put((short) i2).put((short) i1)
                            .put((short) i1).put((short) i2).put((short) i3);
                }
            }
            indices.position(0);

            int[] buffers = new int[2];
            GLES20.glGenBuffers(2, buffers, 0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertCount * 2 * 4, verts,
                    GLES20.GL_STATIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[1]);
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexCount * 2, indices,
                    GLES20.GL_STATIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
            return new int[] {buffers[0], buffers[1], indexCount};
        }

        private int createPatternTexture() {
            ByteBuffer pixels = ByteBuffer.allocateDirect(TEXTURE_SIZE * TEXTURE_SIZE * 4);
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                for (int x = 0; x < TEXTURE_SIZE; x++) {
                    boolean check = ((x >> 5) + (y >> 5)) % 2 == 0;
                    pixels.put((byte) (check ? 230 : x / 2));
                    pixels.put((byte) (y / 2));
                    pixels.put((byte) (check ? 60 : 200));
                    pixels.put((byte) 255);
                }
            }
            pixels.position(0);

            int[] tex = new int[1];
            GLES20.glGenTextures(1, tex, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_REPEAT);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, TEXTURE_SIZE,
                    TEXTURE_SIZE, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels);
            return tex[0];
        }

        /** Texture-backed RGBA framebuffer (renderbuffer RGBA8 is optional in GLES 2.0). */
        private int createFbo(int width, int height) {
            int[] tex = new int[1];
            GLES20.glGenTextures(1, tex, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            int[] fbo = new int[1];
            GLES20.glGenFramebuffers(1, fbo, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, tex[0], 0);
            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "FBO " + width + "x" + height + " incomplete: 0x"
                        + Integer.toHexString(status));
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            return fbo[0];
        }

        private int buildProgram(String vertexSource, String fragmentSource) {
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, compile(GLES20.GL_VERTEX_SHADER, vertexSource));
            GLES20.glAttachShader(program, compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource));
            GLES20.glLinkProgram(program);
            int[] linked = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
            if (linked[0] == 0) {
                Log.e(TAG, "Link failed: " + GLES20.glGetProgramInfoLog(program));
            }
            return program;
        }

        private int compile(int type, String source) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Compile failed: " + GLES20.glGetShaderInfoLog(shader));
            }
            return shader;
        }
    }
}
