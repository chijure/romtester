package com.qapp.romtester.tester;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.provider.MediaStore;

public class CameraTester {

    public static final int REQUEST_IMAGE_CAPTURE = 1;

    public interface Listener {
        void onCaptureUnavailable();

        void onPhotoCaptured(Bitmap thumbnail);

        void onCaptureCancelled();

        void onTorchStateChanged(boolean on);

        void onTorchError(String message);

        void onFindTorchError(String message);
    }

    private final Activity activity;
    private final Listener listener;
    private final int sdkInt;

    private boolean torchOn;
    private Camera legacyCamera;
    private String camera2TorchId;

    public CameraTester(Activity activity, Listener listener) {
        this(activity, listener, Build.VERSION.SDK_INT);
    }

    CameraTester(Activity activity, Listener listener, int sdkInt) {
        this.activity = activity;
        this.listener = listener;
        this.sdkInt = sdkInt;
        if (sdkInt >= Build.VERSION_CODES.M) {
            camera2TorchId = findCamera2TorchId();
        }
    }

    public boolean isTorchOn() {
        return torchOn;
    }

    public void capturePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {
            listener.onCaptureUnavailable();
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_IMAGE_CAPTURE) {
            return;
        }
        if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            listener.onPhotoCaptured(thumbnail);
        } else {
            listener.onCaptureCancelled();
        }
    }

    private String findCamera2TorchId() {
        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                Boolean hasFlash = cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash != null && hasFlash) {
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            listener.onFindTorchError(e.getMessage());
        }
        return null;
    }

    public void setTorch(boolean on) {
        if (sdkInt >= Build.VERSION_CODES.M && camera2TorchId != null) {
            setTorchCamera2(on);
        } else {
            setTorchLegacy(on);
        }
    }

    private void setTorchCamera2(boolean on) {
        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.setTorchMode(camera2TorchId, on);
            torchOn = on;
            listener.onTorchStateChanged(on);
        } catch (CameraAccessException e) {
            listener.onTorchError(e.getMessage());
        }
    }

    private void setTorchLegacy(boolean on) {
        try {
            if (on) {
                legacyCamera = Camera.open();
                Camera.Parameters params = legacyCamera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                legacyCamera.setParameters(params);
                if (sdkInt >= 11) {
                    legacyCamera.setPreviewTexture(new SurfaceTexture(10));
                }
                legacyCamera.startPreview();
            } else if (legacyCamera != null) {
                legacyCamera.stopPreview();
                legacyCamera.release();
                legacyCamera = null;
            }
            torchOn = on;
            listener.onTorchStateChanged(on);
        } catch (Exception e) {
            listener.onTorchError(e.getMessage());
            if (legacyCamera != null) {
                legacyCamera.release();
                legacyCamera = null;
            }
        }
    }

    public void releaseOnPause() {
        if (torchOn) {
            setTorch(false);
        }
    }
}
