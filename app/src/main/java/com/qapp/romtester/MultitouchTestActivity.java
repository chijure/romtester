package com.qapp.romtester;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MultitouchTestActivity extends Activity {

    private static final int[] POINTER_COLORS = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN
    };

    private TextView statusText;
    private int maxTouches = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        statusText = new TextView(this);
        statusText.setPadding(24, 24, 24, 24);
        statusText.setTextSize(16);
        updateStatus(0);
        root.addView(statusText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TouchCanvas canvas = new TouchCanvas(this);
        root.addView(canvas, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void updateStatus(int currentTouches) {
        if (currentTouches > maxTouches) {
            maxTouches = currentTouches;
        }
        statusText.setText(getString(R.string.multitouch_status, currentTouches, maxTouches));
    }

    private class TouchCanvas extends View {

        private final Paint paint = new Paint();
        private MotionEvent lastEvent;

        TouchCanvas(Context context) {
            super(context);
            setBackgroundColor(Color.BLACK);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (lastEvent != null) {
                lastEvent.recycle();
                lastEvent = null;
            }

            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                updateStatus(0);
            } else {
                lastEvent = MotionEvent.obtain(event);
                updateStatus(event.getPointerCount());
            }
            invalidate();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            MotionEvent event = lastEvent;
            if (event == null) {
                return;
            }
            for (int i = 0; i < event.getPointerCount(); i++) {
                paint.setColor(POINTER_COLORS[event.getPointerId(i) % POINTER_COLORS.length]);
                canvas.drawCircle(event.getX(i), event.getY(i), 60, paint);
            }
        }
    }
}
