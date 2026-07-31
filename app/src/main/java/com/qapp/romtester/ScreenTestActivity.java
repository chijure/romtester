package com.qapp.romtester;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class ScreenTestActivity extends Activity implements View.OnClickListener {

    private static final int[] COLORS = {
            Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.GRAY
    };

    private int colorIndex = 0;
    private FrameLayout root;
    private TextView label;
    private String[] colorNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        colorNames = getResources().getStringArray(R.array.screen_test_color_names);

        root = new FrameLayout(this);
        root.setOnClickListener(this);

        label = new TextView(this);
        label.setTextColor(Color.RED);
        label.setTextSize(16);
        label.setPadding(24, 24, 24, 24);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        labelParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        root.addView(label, labelParams);

        setContentView(root);
        showColor();
    }

    @Override
    public void onClick(View view) {
        colorIndex = (colorIndex + 1) % COLORS.length;
        showColor();
    }

    private void showColor() {
        root.setBackgroundColor(COLORS[colorIndex]);
        label.setText(colorNames[colorIndex] + getString(R.string.screen_test_suffix));
    }
}
