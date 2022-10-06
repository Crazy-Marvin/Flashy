package com.poopjournal.flashy;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;

public class CameraHelper {
    private static CameraHelper instance;
    private static CameraManager manager;
    private static Camera camera;
    public static final MutableLiveData<Boolean> isFlashOn = new MutableLiveData<>(false);

    private CameraHelper(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        } else {
            camera = Camera.open();
        }
    }

    public static CameraHelper getInstance(Context context) {
        if (instance == null)
            instance = new CameraHelper(context);
        return instance;
    }

    public void toggleLollipop() {
        if (Boolean.TRUE.equals(isFlashOn.getValue())) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(FLASH_MODE_OFF);
            camera.setParameters(parameters);
            isFlashOn.setValue(false);
        } else try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Utils.getFlashOnParameter(camera));
            camera.setParameters(parameters);
            camera.setPreviewTexture(new SurfaceTexture(0));
            camera.startPreview();
            camera.autoFocus((success, camera) -> {});
            isFlashOn.setValue(true);
        } catch (IOException ignored) {}
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void toggleMarshmallow() throws CameraAccessException {
        if (Boolean.TRUE.equals(isFlashOn.getValue())) {
            manager.setTorchMode(manager.getCameraIdList()[0], false);
            isFlashOn.setValue(false);
        } else {
            manager.setTorchMode(manager.getCameraIdList()[0], true);
            isFlashOn.setValue(true);
        }
    }
}
