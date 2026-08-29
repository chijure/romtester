package com.qapp.romtester;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PaintTouchTestActivity extends Activity {

    private TextView statusText;
    private PaintCanvas canvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUi();

        FrameLayout root = new FrameLayout(this);

        canvas = new PaintCanvas(this);
        root.addView(canvas, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        statusText = new TextView(this);
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(14);
        statusText.setPadding(20, 12, 20, 12);
        statusText.setBackgroundColor(Color.argb(140, 0, 0, 0));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.TOP | Gravity.START;
        root.addView(statusText, statusParams);

        Button clearButton = new Button(this);
        clearButton.setText(R.string.paint_button_clear);
        clearButton.setOnClickListener(v -> canvas.reset());
        FrameLayout.LayoutParams clearParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        clearParams.gravity = Gravity.TOP | Gravity.END;
        root.addView(clearButton, clearParams);

        setContentView(root);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void updateStatus(int touchedCells, int totalCells) {
        statusText.setText(getString(R.string.paint_status_format, touchedCells, totalCells));
    }

    private class PaintCanvas extends View {

        private static final int COLS = 4;
        private static final int ROWS = 6;

        private final int[] pointerColors = {
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN
        };

        private Bitmap bitmap;
        private Canvas bitmapCanvas;
        private final Paint strokePaint = new Paint();
        private final Paint highlightPaint = new Paint();
        private final Map<Integer, Float> lastX = new HashMap<>();
        private final Map<Integer, Float> lastY = new HashMap<>();
        private boolean[][] touchedCells;
        private int touchedCount;
        private float cellWidth;
        private float cellHeight;

        PaintCanvas(Context context) {
            super(context);
            setBackgroundColor(Color.BLACK);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeJoin(Paint.Join.ROUND);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setStrokeWidth(28);
            strokePaint.setAntiAlias(true);
            highlightPaint.setColor(Color.argb(70, 0, 255, 0));
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w <= 0 || h <= 0) {
                return;
            }
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmapCanvas = new Canvas(bitmap);
            cellWidth = w / (float) COLS;
            cellHeight = h / (float) ROWS;
            touchedCells = new boolean[ROWS][COLS];
            touchedCount = 0;
            drawGrid(w, h);
            updateStatus(0, ROWS * COLS);
        }

        private void drawGrid(int w, int h) {
            Paint gridPaint = new Paint();
            gridPaint.setColor(Color.argb(60, 255, 255, 255));
            gridPaint.setStrokeWidth(2);
            for (int c = 1; c < COLS; c++) {
                float x = c * cellWidth;
                bitmapCanvas.drawLine(x, 0, x, h, gridPaint);
            }
            for (int r = 1; r < ROWS; r++) {
                float y = r * cellHeight;
                bitmapCanvas.drawLine(0, y, w, y, gridPaint);
            }
        }

        void reset() {
            if (bitmapCanvas == null) {
                return;
            }
            bitmapCanvas.drawColor(Color.BLACK);
            drawGrid(bitmap.getWidth(), bitmap.getHeight());
            for (boolean[] row : touchedCells) {
                Arrays.fill(row, false);
            }
            touchedCount = 0;
            lastX.clear();
            lastY.clear();
            updateStatus(0, ROWS * COLS);
            invalidate();
        }

        private void markCell(float x, float y) {
            int col = (int) (x / cellWidth);
            int row = (int) (y / cellHeight);
            if (col < 0 || col >= COLS || row < 0 || row >= ROWS) {
                return;
            }
            if (!touchedCells[row][col]) {
                touchedCells[row][col] = true;
                touchedCount++;
                bitmapCanvas.drawRect(col * cellWidth, row * cellHeight,
                        (col + 1) * cellWidth, (row + 1) * cellHeight, highlightPaint);
                updateStatus(touchedCount, ROWS * COLS);
            }
        }

        private void drawSegment(int pointerId, float x, float y) {
            strokePaint.setColor(pointerColors[pointerId % pointerColors.length]);
            markCell(x, y);
            Float px = lastX.get(pointerId);
            Float py = lastY.get(pointerId);
            if (px != null && py != null) {
                bitmapCanvas.drawLine(px, py, x, y, strokePaint);
            } else {
                bitmapCanvas.drawPoint(x, y, strokePaint);
            }
            lastX.put(pointerId, x);
            lastY.put(pointerId, y);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (bitmapCanvas == null) {
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    int index = event.getActionIndex();
                    int id = event.getPointerId(index);
                    lastX.remove(id);
                    lastY.remove(id);
                    drawSegment(id, event.getX(index), event.getY(index));
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        drawSegment(event.getPointerId(i), event.getX(i), event.getY(i));
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP: {
                    int index = event.getActionIndex();
                    int id = event.getPointerId(index);
                    lastX.remove(id);
                    lastY.remove(id);
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                    lastX.clear();
                    lastY.clear();
                    break;
                default:
                    break;
            }
            invalidate();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, 0, 0, null);
            }
        }
    }
}
