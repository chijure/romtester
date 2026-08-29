package com.qapp.romtester.media;

import com.qapp.romtester.R;
import com.qapp.romtester.tester.CameraTester;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class CameraTestActivity extends Activity implements View.OnClickListener {

    private TextView statusText;
    private ImageView imagePreview;
    private Button torchButton;
    private CameraTester tester;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_test);

        statusText = findViewById(R.id.text_camera_status);
        imagePreview = findViewById(R.id.image_preview);
        torchButton = findViewById(R.id.button_torch);

        findViewById(R.id.button_capture).setOnClickListener(this);
        torchButton.setOnClickListener(this);

        tester = new CameraTester(this, new CameraTester.Listener() {
            @Override
            public void onCaptureUnavailable() {
                statusText.setText(getString(R.string.camera_no_app));
            }

            @Override
            public void onPhotoCaptured(Bitmap thumbnail) {
                imagePreview.setImageBitmap(thumbnail);
                statusText.setText(getString(R.string.camera_photo_captured));
            }

            @Override
            public void onCaptureCancelled() {
                statusText.setText(getString(R.string.camera_capture_cancelled));
            }

            @Override
            public void onTorchStateChanged(boolean on) {
                torchButton.setText(on ? getString(R.string.camera_button_torch_off) : getString(R.string.camera_button_torch_on));
            }

            @Override
            public void onTorchError(String message) {
                statusText.setText(getString(R.string.camera_flashlight_error_format, message));
            }

            @Override
            public void onFindTorchError(String message) {
                statusText.setText(getString(R.string.camera_error_format, message));
            }
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_capture) {
            tester.capturePhoto();
        } else if (id == R.id.button_torch) {
            tester.setTorch(!tester.isTorchOn());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        tester.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tester.releaseOnPause();
    }
}
