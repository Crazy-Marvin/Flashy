package rocks.poopjournal.flashy;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CameraHelper {
    private static CameraHelper instance;
    private CameraManager manager;
    private Camera camera;
    private static final MutableLiveData<Boolean> isNormalFlashOn = new MutableLiveData<>(false);
    private static final MutableLiveData<Boolean> isSosOn = new MutableLiveData<>(false);
    private static final MutableLiveData<Boolean> isStroboscopeOn = new MutableLiveData<>(false);
    private final List<Integer> sos = Arrays.asList(250, 250, 250, 250, 250, 250, 750, 250, 750, 250, 750, 250, 250, 250, 250, 250, 250, 1000);
    private final AtomicInteger stroboscopeInterval = new AtomicInteger(500);
    /**
     * It is an error to use this boolean for anything other than SOS and Stroboscope functions.
     */
    private boolean isStroboscopeFlashOn = false;

    public LiveData<Boolean> getNormalFlashStatus() {
        return isNormalFlashOn;
    }

    public LiveData<Boolean> getSosStatus() {
        return isSosOn;
    }

    public LiveData<Boolean> getStroboscopeStatus() {
        return isStroboscopeOn;
    }

    public void setStroboscopeInterval(int interval) {
        stroboscopeInterval.set(interval);
    }

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

    public void toggleNormalFlash(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                toggleNormalFlashMarshmallow();
            } catch (CameraAccessException e) {
                Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else toggleNormalFlashLollipop();
        Utils.updateWidgets(context);
    }

    public void toggleSos(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                toggleSosMarshmallow();
            } catch (CameraAccessException e) {
                Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else toggleSosLollipop();
        Utils.updateWidgets(context);
    }

    public void toggleStroboscope(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                toggleStroboscopeModeMarshmallow();
            } catch (CameraAccessException e) {
                Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else toggleStroboscopeModeLollipop();
        Utils.updateWidgets(context);
    }

    private void toggleNormalFlashLollipop() {
        if (Boolean.TRUE.equals(isSosOn.getValue())) {
            isSosOn.setValue(false);
            while (isStroboscopeFlashOn) doNothing();
        }
        if (Boolean.TRUE.equals(isStroboscopeOn.getValue())) {
            isStroboscopeOn.setValue(false);
            while (isStroboscopeFlashOn) doNothing();
        }
        if (Boolean.TRUE.equals(isNormalFlashOn.getValue())) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(FLASH_MODE_OFF);
            camera.setParameters(parameters);
            isNormalFlashOn.setValue(false);
        } else try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Utils.getFlashOnParameter(camera));
            camera.setParameters(parameters);
            camera.setPreviewTexture(new SurfaceTexture(0));
            camera.startPreview();
            camera.autoFocus((success, camera) -> {
            });
            isNormalFlashOn.setValue(true);
        } catch (IOException ignored) {
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void toggleNormalFlashMarshmallow() throws CameraAccessException {
        if (Boolean.TRUE.equals(isSosOn.getValue())) {
            isSosOn.setValue(false);
            while (isStroboscopeFlashOn) doNothing();
        }
        if (Boolean.TRUE.equals(isStroboscopeOn.getValue())) {
            isStroboscopeOn.setValue(false);
            while (isStroboscopeFlashOn) doNothing();
        }
        if (Boolean.TRUE.equals(isNormalFlashOn.getValue())) {
            manager.setTorchMode(manager.getCameraIdList()[0], false);
            isNormalFlashOn.setValue(false);
        } else {
            manager.setTorchMode(manager.getCameraIdList()[0], true);
            isNormalFlashOn.setValue(true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void toggleSosMarshmallow() throws CameraAccessException {
        if (Boolean.FALSE.equals(isSosOn.getValue())) {
            if (Boolean.TRUE.equals(isNormalFlashOn.getValue())) toggleNormalFlashMarshmallow();
            if (Boolean.TRUE.equals(isStroboscopeOn.getValue())) {
                toggleStroboscopeModeMarshmallow();
                while (isStroboscopeFlashOn) doNothing();
            }
            isSosOn.setValue(true);
            AtomicInteger sosIndex = new AtomicInteger(0);
            new Thread(() -> {
                while (Boolean.TRUE.equals(isSosOn.getValue())) {
                    try {
                        toggleStroboscopeFlashMarshmallow();
                        Thread.sleep(sos.get(sosIndex.getAndIncrement() % sos.size()));
                        toggleStroboscopeFlashMarshmallow();
                        Thread.sleep(sos.get(sosIndex.getAndIncrement() % sos.size()));
                    } catch (CameraAccessException | InterruptedException e) {
                        isSosOn.postValue(false);
                        e.printStackTrace();
                    }
                }
            }).start();
        } else isSosOn.setValue(false);
    }

    private void toggleSosLollipop() {
        if (Boolean.FALSE.equals(isSosOn.getValue())) {
            if (Boolean.TRUE.equals(isNormalFlashOn.getValue())) toggleNormalFlashLollipop();
            if (Boolean.TRUE.equals(isStroboscopeOn.getValue())) {
                toggleStroboscopeModeLollipop();
                while (isStroboscopeFlashOn) doNothing();
            }
            isSosOn.setValue(true);
            AtomicInteger sosIndex = new AtomicInteger(0);
            new Thread(() -> {
                while (Boolean.TRUE.equals(isSosOn.getValue())) {
                    try {
                        toggleStroboscopeFlashLollipop();
                        Thread.sleep(sos.get(sosIndex.getAndIncrement() % sos.size()));
                        toggleStroboscopeFlashLollipop();
                        Thread.sleep(sos.get(sosIndex.getAndIncrement() % sos.size()));
                    } catch (InterruptedException e) {
                        isSosOn.postValue(false);
                        e.printStackTrace();
                    }
                }
            }).start();
        } else isSosOn.setValue(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void toggleStroboscopeModeMarshmallow() throws CameraAccessException {
        if (Boolean.FALSE.equals(isStroboscopeOn.getValue())) {
            if (Boolean.TRUE.equals(isNormalFlashOn.getValue())) toggleNormalFlashMarshmallow();
            if (Boolean.TRUE.equals(isSosOn.getValue())) {
                toggleSosMarshmallow();
                while (isStroboscopeFlashOn) doNothing();
            }
            isStroboscopeOn.setValue(true);
            new Thread(() -> {
                while (Boolean.TRUE.equals(isStroboscopeOn.getValue())) {
                    try {
                        toggleStroboscopeFlashMarshmallow();
                        Thread.sleep(stroboscopeInterval.get());
                        toggleStroboscopeFlashMarshmallow();
                        Thread.sleep(stroboscopeInterval.get());
                    } catch (CameraAccessException | InterruptedException e) {
                        isStroboscopeOn.postValue(false);
                        e.printStackTrace();
                    }
                }
            }).start();
        } else isStroboscopeOn.setValue(false);
    }

    private void toggleStroboscopeModeLollipop() {
        if (Boolean.FALSE.equals(isStroboscopeOn.getValue())) {
            if (Boolean.TRUE.equals(isNormalFlashOn.getValue())) toggleNormalFlashLollipop();
            if (Boolean.TRUE.equals(isSosOn.getValue())) {
                toggleSosLollipop();
                while (isStroboscopeFlashOn) doNothing();
            }
            isStroboscopeOn.setValue(true);
            new Thread(() -> {
                while (Boolean.TRUE.equals(isStroboscopeOn.getValue())) {
                    try {
                        toggleStroboscopeFlashLollipop();
                        Thread.sleep(stroboscopeInterval.get());
                        toggleStroboscopeFlashLollipop();
                        Thread.sleep(stroboscopeInterval.get());
                    } catch (InterruptedException e) {
                        isStroboscopeOn.postValue(false);
                        e.printStackTrace();
                    }
                }
            }).start();
        } else isStroboscopeOn.setValue(false);
    }

    private void toggleStroboscopeFlashLollipop() {
        if (isStroboscopeFlashOn) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(FLASH_MODE_OFF);
            camera.setParameters(parameters);
            isStroboscopeFlashOn = false;
        } else try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Utils.getFlashOnParameter(camera));
            camera.setParameters(parameters);
            camera.setPreviewTexture(new SurfaceTexture(0));
            camera.startPreview();
            camera.autoFocus((success, camera) -> {
            });
            isStroboscopeFlashOn = true;
        } catch (IOException ignored) {
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void toggleStroboscopeFlashMarshmallow() throws CameraAccessException {
        manager.setTorchMode(manager.getCameraIdList()[0], !isStroboscopeFlashOn);
        isStroboscopeFlashOn = !isStroboscopeFlashOn;
    }
    
    private void doNothing() {}
}
