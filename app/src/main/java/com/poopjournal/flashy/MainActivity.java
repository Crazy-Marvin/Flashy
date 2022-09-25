package com.poopjournal.flashy;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;

import android.animation.LayoutTransition;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import me.tankery.lib.circularseekbar.CircularSeekBar;

public class MainActivity extends AppCompatActivity implements Camera.AutoFocusCallback {
    //Views
    CircularSeekBar seekBar;
    RelativeLayout bg_options, bg_option_circle;
    ImageView iconFlash, iconScreen, powerCenter, powerIconCenter, powerIconCenterStand;
    Dialog FlashDialog = null;
    //Fields
    private int brightness = -999;
    private Window window;
    private static Camera camera = null;// has to be static, otherwise onDestroy() destroys it
    private CameraManager manager;
    private SharedPreferences preferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        if (key.equals("flash_enabled")) {
            changePowerButtonColors(sharedPreferences.getBoolean(key, false));
            Utils.updateWidgets(this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        window = getWindow();
        preferences = getSharedPreferences("my_prefs", MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(listener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        } else {
            camera = Camera.open();
        }
        findViews();
        applyListeners();
        changePowerButtonColors(preferences.getBoolean("flash_enabled", false));
        init();
        if (savedInstanceState != null && preferences.getInt("default_option", 1) == 2) {
            Log.d("flashy_test", "saved 2, " + savedInstanceState.getInt("brightness"));
            brightness = savedInstanceState.getInt("brightness");
            WindowManager.LayoutParams layoutpars = window.getAttributes();
            layoutpars.screenBrightness = (float) brightness / 100;
            window.setAttributes(layoutpars);
        }
    }

    void applyListeners() {
        bg_options.setOnClickListener(view -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("default_option", preferences.getInt("default_option", 1) == 1 ? 2 : 1);
            editor.apply();
            init();
        });
    }

    void init() {
        if (preferences.getInt("default_option", 1) == 1) {
            updateOptionsUI(true);
            refreshActivityForFlashLight();
        } else {
            updateOptionsUI(false);
            refreshActivityForScreenLight();
        }
    }

    void refreshActivityForFlashLight() {
        //Refresh Seekbar
        seekBar.setOnSeekBarChangeListener(null);
        seekBar.setProgress(0F);
        seekBar.setEnabled(false);
        seekBar.setPointerColor(Color.parseColor("#AAAABB"));
        boolean hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!hasFlash) {
            FlashDialog = DialogsUtil.showNoFlashLightDialog(this);
            powerCenter.setOnClickListener(null);
            RelativeLayout useScreen = FlashDialog.findViewById(R.id.container_use_screen);
            useScreen.setOnClickListener(view -> FlashDialog.dismiss());
            FlashDialog.setOnDismissListener((dialog -> bg_options.callOnClick()));
            FlashDialog.show();
            Log.d("flashy_dial", "showing for simple");
        } else {
            powerCenter.setOnClickListener(view -> {
                if (!preferences.getBoolean("flash_enabled", false))
                    turnOn();
                else turnOff();
            });
        }
    }

    void changePowerButtonColors(boolean isTurnedOn) {
        if (isTurnedOn) {
            powerCenter.setColorFilter(Color.parseColor("#28FFB137"));
            powerIconCenter.setColorFilter(Color.parseColor("#FFB137"));
            powerIconCenterStand.setColorFilter(Color.parseColor("#FFB137"));
        } else {
            //Refresh Power Button
            powerCenter.setColorFilter(Color.parseColor("#F3F3F7"));
            powerIconCenter.setColorFilter(Color.parseColor("#AAAABB"));
            powerIconCenterStand.setColorFilter(Color.parseColor("#AAAABB"));
        }
    }

    void updateOptionsUI(boolean isFlash) {
        if (isFlash) {
            //Change UI for options
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bg_option_circle.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            bg_option_circle.setLayoutParams(params);
            iconFlash.setColorFilter(Color.parseColor("#FFB137"));
            iconScreen.setColorFilter(Color.parseColor("#AAAABB"));
            seekBar.setProgress(0f);
        } else {
            bg_option_circle.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bg_option_circle.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            bg_option_circle.setLayoutParams(params);
            iconFlash.setColorFilter(Color.parseColor("#AAAABB"));
            iconScreen.setColorFilter(Color.parseColor("#FFB137"));
        }
    }

    void refreshActivityForScreenLight() {
        seekBar.setPointerColor(Color.parseColor("#FFB137"));
        seekBar.setEnabled(true);
        powerCenter.setOnClickListener(null);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) turnOff();
        seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                brightness = (int) progress;
                WindowManager.LayoutParams layoutpars = window.getAttributes();
                layoutpars.screenBrightness = (float) brightness / 100;
                window.setAttributes(layoutpars);
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });
        powerCenter.setOnClickListener(view -> {
            if (brightness != 100) {
                seekBar.setProgress(100);
            } else {
                seekBar.setProgress(0);
            }
        });
    }

    void findViews() {
        seekBar = findViewById(R.id.progress_circular);
        bg_options = findViewById(R.id.bg_options);
        bg_option_circle = findViewById(R.id.bg_option_circle);
        iconFlash = findViewById(R.id.flash_icon);
        iconScreen = findViewById(R.id.screen_icon);
        powerCenter = findViewById(R.id.power_center);
        powerIconCenter = findViewById(R.id.power_icon_center);
        powerIconCenterStand = findViewById(R.id.power_icon_center_stand);
    }

    public void turnOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                manager.setTorchMode(manager.getCameraIdList()[0], true);
                preferences.edit().putBoolean("flash_enabled", true).apply();
            } catch (CameraAccessException e) {
                Toast.makeText(this, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Utils.getFlashOnParameter(camera));
            camera.setParameters(parameters);
            camera.setPreviewTexture(new SurfaceTexture(0));
            camera.startPreview();
            camera.autoFocus(this);
            preferences.edit().putBoolean("flash_enabled", true).apply();
        } catch (Exception e) {
            // We are expecting this to happen on devices that don't support autofocus.
        }
    }

    public void turnOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                manager.setTorchMode(manager.getCameraIdList()[0], false);
                preferences.edit().putBoolean("flash_enabled", false).apply();
            } catch (CameraAccessException e) {
                Toast.makeText(this, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(FLASH_MODE_OFF);
            camera.setParameters(parameters);
            preferences.edit().putBoolean("flash_enabled", false).apply();
        } catch (Exception e) {
            // This will happen if the camera fails to turn on.
        }
    }

    @Override
    public void onAutoFocus(boolean b, Camera camera) {

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (FlashDialog != null) {
            if (FlashDialog.isShowing()) {
                FlashDialog.dismiss();
                refreshActivityForScreenLight();
            }
        }
        if (preferences.getInt("default_option", 1) == 2) {
            outState.putInt("brightness", brightness);
        }
    }
}
