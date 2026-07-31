package com.qapp.romtester;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class CameraTestActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private TextView statusText;
    private ImageView imagePreview;
    private Button torchButton;

    private boolean torchOn = false;
    private Camera legacyCamera;
    private String camera2TorchId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_test);

        statusText = findViewById(R.id.text_camera_status);
        imagePreview = findViewById(R.id.image_preview);
        torchButton = findViewById(R.id.button_torch);

        findViewById(R.id.button_capture).setOnClickListener(this);
        torchButton.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            camera2TorchId = findCamera2TorchId();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_capture) {
            capturePhoto();
        } else if (id == R.id.button_torch) {
            setTorch(!torchOn);
        }
    }

    private void capturePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {
            statusText.setText(getString(R.string.camera_no_app));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null
                && data.getExtras() != null) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            imagePreview.setImageBitmap(thumbnail);
            statusText.setText(getString(R.string.camera_photo_captured));
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            statusText.setText(getString(R.string.camera_capture_cancelled));
        }
    }

    private String findCamera2TorchId() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                Boolean hasFlash = cameraManager.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash != null && hasFlash) {
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            statusText.setText(getString(R.string.camera_error_format, e.getMessage()));
        }
        return null;
    }

    private void setTorch(boolean on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && camera2TorchId != null) {
            setTorchCamera2(on);
        } else {
            setTorchLegacy(on);
        }
    }

    private void setTorchCamera2(boolean on) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.setTorchMode(camera2TorchId, on);
            torchOn = on;
            torchButton.setText(on ? getString(R.string.camera_button_torch_off) : getString(R.string.camera_button_torch_on));
        } catch (CameraAccessException e) {
            statusText.setText(getString(R.string.camera_flashlight_error_format, e.getMessage()));
        }
    }

    private void setTorchLegacy(boolean on) {
        try {
            if (on) {
                legacyCamera = Camera.open();
                Camera.Parameters params = legacyCamera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                legacyCamera.setParameters(params);
                if (Build.VERSION.SDK_INT >= 11) {
                    legacyCamera.setPreviewTexture(new SurfaceTexture(10));
                }
                legacyCamera.startPreview();
            } else if (legacyCamera != null) {
                legacyCamera.stopPreview();
                legacyCamera.release();
                legacyCamera = null;
            }
            torchOn = on;
            torchButton.setText(on ? getString(R.string.camera_button_torch_off) : getString(R.string.camera_button_torch_on));
        } catch (Exception e) {
            statusText.setText(getString(R.string.camera_flashlight_unavailable_format, e.getMessage()));
            if (legacyCamera != null) {
                legacyCamera.release();
                legacyCamera = null;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (torchOn) {
            setTorch(false);
        }
    }
}
