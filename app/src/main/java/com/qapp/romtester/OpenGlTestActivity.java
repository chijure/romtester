package com.qapp.romtester;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGlTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GLSurfaceView glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setRenderer(new SpinningTriangleRenderer());
        setContentView(glSurfaceView);
    }

    private static class SpinningTriangleRenderer implements GLSurfaceView.Renderer {

        private final FloatBuffer triangleVertices;
        private final FloatBuffer triangleColors;
        private float rotation;

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
        }
    }
}
