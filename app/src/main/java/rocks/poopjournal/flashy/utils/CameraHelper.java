package rocks.poopjournal.flashy.utils;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import rocks.poopjournal.flashy.R;

public class CameraHelper {
    private static CameraHelper instance;
    private CameraManager manager;
    private Camera camera;
    private final MutableLiveData<Boolean> isNormalFlashOn = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSosOn = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isStroboscopeOn = new MutableLiveData<>(false);
    private final List<Integer> sosReference = Arrays.asList(250, 250, 250, 250, 250, 750, 750, 250, 750, 250, 750, 750, 250, 250, 250, 250, 250, 1750);
    private final List<Integer> actualSos = new ArrayList<>();
    private final AtomicInteger stroboscopeInterval = new AtomicInteger(500);
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private final AtomicInteger flashlightStrength = new AtomicInteger(1);
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

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public int getFlashlightStrength() {return flashlightStrength.get();}

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void setFlashlightStrength(int strength) {flashlightStrength.set(strength);}

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

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void turnOnFlashWithStrength(Context context) {
        try {
            manager.turnOnTorchWithStrengthLevel(manager.getCameraIdList()[0], flashlightStrength.get());
        } catch (CameraAccessException e) {
            Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void turnOnFlashAndroid13(Context context) throws CameraAccessException {
        if (isDeviceSupportFlashlightBrightnessControl(context)) {
            manager.turnOnTorchWithStrengthLevel(manager.getCameraIdList()[0], flashlightStrength.get());
        } else {
            manager.setTorchMode(manager.getCameraIdList()[0], true);
        }
    }

    public void toggleNormalFlash(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                applyFlashlightStrengthFromSettings(context);
                toggleNormalFlashWithStrength(context);
            } catch (CameraAccessException e) {
                Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                toggleNormalFlashMarshmallow();
            } catch (CameraAccessException e) {
                Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else toggleNormalFlashLollipop();
        Utils.updateWidgets(context);
    }

    public void turnOffNormalFlash(Context context) {
        if (Boolean.TRUE.equals(getNormalFlashStatus().getValue())) {
            toggleNormalFlash(context);
        }
    }

    public void toggleSos(Context context) {
        applySosLengthsFromSettings(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                applyFlashlightStrengthFromSettings(context);
                toggleSosWithStrength(context);
            } catch (CameraAccessException e) {
                Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                toggleSosMarshmallow();
            } catch (CameraAccessException e) {
                Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else toggleSosLollipop();
        Utils.updateWidgets(context);
    }

    public void turnOffSos(Context context) {
        if (Boolean.TRUE.equals(getSosStatus().getValue())) {
            toggleSos(context);
        }
    }

    public void toggleStroboscope(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                applyFlashlightStrengthFromSettings(context);
                toggleStroboscopeModeWithStrength(context);
            } catch (CameraAccessException e) {
                Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                toggleStroboscopeModeMarshmallow();
            } catch (CameraAccessException e) {
                Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else toggleStroboscopeModeLollipop();
        Utils.updateWidgets(context);
    }

    public void turnOffStroboscope(Context context) {
        if (Boolean.TRUE.equals(getStroboscopeStatus().getValue())) {
            toggleStroboscope(context);
        }
    }

    public void turnOffAll(Context context) {
        turnOffNormalFlash(context);
        turnOffSos(context);
        turnOffStroboscope(context);
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

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void toggleNormalFlashWithStrength(Context context) throws CameraAccessException {
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
            turnOnFlashAndroid13(context);
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
                        Thread.sleep(actualSos.get(sosIndex.getAndIncrement() % actualSos.size()));
                        toggleStroboscopeFlashMarshmallow();
                        Thread.sleep(actualSos.get(sosIndex.getAndIncrement() % actualSos.size()));
                    } catch (CameraAccessException | InterruptedException e) {
                        isSosOn.postValue(false);
                        e.printStackTrace();
                    }
                }
            }).start();
        } else isSosOn.setValue(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void toggleSosWithStrength(Context context) throws CameraAccessException {
        if (Boolean.FALSE.equals(isSosOn.getValue())) {
            if (Boolean.TRUE.equals(isNormalFlashOn.getValue())) toggleNormalFlashWithStrength(context);
            if (Boolean.TRUE.equals(isStroboscopeOn.getValue())) {
                toggleStroboscopeModeWithStrength(context);
                while (isStroboscopeFlashOn) doNothing();
            }
            isSosOn.setValue(true);
            AtomicInteger sosIndex = new AtomicInteger(0);
            new Thread(() -> {
                while (Boolean.TRUE.equals(isSosOn.getValue())) {
                    try {
                        toggleStroboscopeFlashWithStrength(context);
                        Thread.sleep(actualSos.get(sosIndex.getAndIncrement() % actualSos.size()));
                        toggleStroboscopeFlashWithStrength(context);
                        Thread.sleep(actualSos.get(sosIndex.getAndIncrement() % actualSos.size()));
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
                        Thread.sleep(actualSos.get(sosIndex.getAndIncrement() % actualSos.size()));
                        toggleStroboscopeFlashLollipop();
                        Thread.sleep(actualSos.get(sosIndex.getAndIncrement() % actualSos.size()));
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

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void toggleStroboscopeModeWithStrength(Context context) throws CameraAccessException {
        if (Boolean.FALSE.equals(isStroboscopeOn.getValue())) {
            if (Boolean.TRUE.equals(isNormalFlashOn.getValue())) toggleNormalFlashWithStrength(context);
            if (Boolean.TRUE.equals(isSosOn.getValue())) {
                toggleSosWithStrength(context);
                while (isStroboscopeFlashOn) doNothing();
            }
            isStroboscopeOn.setValue(true);
            new Thread(() -> {
                while (Boolean.TRUE.equals(isStroboscopeOn.getValue())) {
                    try {
                        toggleStroboscopeFlashWithStrength(context);
                        Thread.sleep(stroboscopeInterval.get());
                        toggleStroboscopeFlashWithStrength(context);
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

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void toggleStroboscopeFlashWithStrength(Context context) throws CameraAccessException {
        if (isStroboscopeFlashOn)
            manager.setTorchMode(manager.getCameraIdList()[0], false);
        else {
            turnOnFlashAndroid13(context);
        }
        isStroboscopeFlashOn = !isStroboscopeFlashOn;
    }

    private void applySosLengthsFromSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int ditDuration = getCurrentDitLength(context);
        actualSos.clear();
        actualSos.addAll(sosReference);
        actualSos.replaceAll(duration -> duration == 750 ? ditDuration * 3 : 250);
        actualSos.replaceAll(duration -> duration == 250 ? ditDuration : duration);
        actualSos.set(actualSos.size() - 1, ditDuration * 7);
        if (preferences.getBoolean("use_farnsworth", false)) {
            int farnsworthUnitLength = Integer.parseInt(preferences.getString("farnsworth_unit", ""));
            actualSos.set(5, farnsworthUnitLength * 3);
            actualSos.set(11, farnsworthUnitLength * 3);
            actualSos.set(actualSos.size() - 1, farnsworthUnitLength * 7);
        }
    }

    /**
     * @return The current length of a <code>dit</code> in milliseconds
     */
    public int getCurrentDitLength(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Math.round(((float) 60 / (50 * Integer.parseInt(preferences.getString("words_per_min", "5")))) * 1000);
    }

    public int getCurrentDitLength(SharedPreferences preferences) {
        return Math.round(((float) 60 / (50 * Integer.parseInt(preferences.getString("words_per_min", "5")))) * 1000);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public int getFlashlightStrengthLevel(Context context) {
        try {
            return manager.getCameraCharacteristics(manager.getCameraIdList()[0]).get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
        } catch (CameraAccessException e) {
            Toast.makeText(context, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return 1;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public boolean isDeviceSupportFlashlightBrightnessControl(Context context) {
        return getFlashlightStrengthLevel(context) > 1;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void applyFlashlightStrengthFromSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int strength = preferences.getInt("flashlight_strength", -1);
        if (strength != -1) flashlightStrength.set(strength);
        else flashlightStrength.set(getFlashlightStrengthLevel(context));
    }
    
    private void doNothing() {
        //empty method to suppress android studio warnings
    }
}
