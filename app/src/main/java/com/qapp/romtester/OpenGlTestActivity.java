package com.qapp.romtester;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGlTestActivity extends Activity {

    private TextView infoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);

        GLSurfaceView glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setRenderer(new SpinningTriangleRenderer());
        root.addView(glSurfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        infoText = new TextView(this);
        infoText.setTextColor(Color.WHITE);
        infoText.setBackgroundColor(0xAA000000);
        infoText.setPadding(24, 16, 24, 16);
        infoText.setText(getString(R.string.opengl_status_initializing));
        FrameLayout.LayoutParams infoParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        infoParams.gravity = Gravity.BOTTOM;
        root.addView(infoText, infoParams);

        setContentView(root);
    }

    private class SpinningTriangleRenderer implements GLSurfaceView.Renderer {

        private final FloatBuffer triangleVertices;
        private final FloatBuffer triangleColors;
        private float rotation;

        private String vendor = "";
        private String renderer = "";
        private String version = "";
        private int frameCount;
        private long fpsWindowStart;
        private int fps;

        SpinningTriangleRenderer() {
            float[] vertices = {
                    0f, 0.6f, 0f,
                    -0.6f, -0.4f, 0f,
                    0.6f, -0.4f, 0f
            };
            float[] colors = {
                    1f, 0f, 0f, 1f,
                    0f, 1f, 0f, 1f,
                    0f, 0f, 1f, 1f
            };

            triangleVertices = ByteBuffer.allocateDirect(vertices.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            triangleVertices.put(vertices).position(0);

            triangleColors = ByteBuffer.allocateDirect(colors.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            triangleColors.put(colors).position(0);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glClearColor(0f, 0f, 0f, 1f);
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

            vendor = gl.glGetString(GL10.GL_VENDOR);
            renderer = gl.glGetString(GL10.GL_RENDERER);
            version = gl.glGetString(GL10.GL_VERSION);
            frameCount = 0;
            fps = 0;
            fpsWindowStart = System.currentTimeMillis();
            runOnUiThread(this::updateInfoText);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glRotatef(rotation, 0f, 0f, 1f);
            rotation = (rotation + 2f) % 360f;

            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, triangleVertices);
            gl.glColorPointer(4, GL10.GL_FLOAT, 0, triangleColors);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3);

            frameCount++;
            long now = System.currentTimeMillis();
            long elapsed = now - fpsWindowStart;
            if (elapsed >= 1000) {
                fps = (int) (frameCount * 1000L / elapsed);
                frameCount = 0;
                fpsWindowStart = now;
                runOnUiThread(this::updateInfoText);
            }
        }

        private void updateInfoText() {
            infoText.setText(getString(R.string.opengl_info_format, version, renderer, vendor, fps));
        }
    }
}
