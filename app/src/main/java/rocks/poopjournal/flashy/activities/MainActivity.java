package rocks.poopjournal.flashy.activities;

import android.animation.LayoutTransition;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.slider.Slider;

import me.tankery.lib.circularseekbar.CircularSeekBar;
import rocks.poopjournal.flashy.NoFlashlightDialog;
import rocks.poopjournal.flashy.R;
import rocks.poopjournal.flashy.databinding.MainActivityBinding;
import rocks.poopjournal.flashy.receivers.ScreenOffBroadcastReceiver;
import rocks.poopjournal.flashy.utils.CameraHelper;
import rocks.poopjournal.flashy.utils.Shortcuts;
import rocks.poopjournal.flashy.utils.Utils;

public class MainActivity extends AppCompatActivity {
    //Fields
    private int brightness = -999;
    private Window window;
    private SharedPreferences legacyPreferences; //kept for legacy reasons
    private SharedPreferences defaultPreferences;
    private CameraHelper helper;
    private MainActivityBinding binding;
    private final BroadcastReceiver turnOffFlashlightOnScreenOffReceiver = new ScreenOffBroadcastReceiver();
    private enum FlashlightMode {
        NORMAL, SOS, STROBOSCOPE
    }
    private final SharedPreferences.OnSharedPreferenceChangeListener material3Listener = (sharedPreferences, key) -> {
        switch (key) {
            case "md3":
                recreate();
                break;
            case "no_flash_on_device_screen_off":
                if (sharedPreferences.getBoolean("no_flash_on_device_screen_off", false)) {
                    registerReceiver(turnOffFlashlightOnScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
                } else {
                    unregisterReceiver(turnOffFlashlightOnScreenOffReceiver);
                }
                break;
            default:
                Log.v(getClass().getSimpleName(), "Preference key received: " + key);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (defaultPreferences.getBoolean("no_flash_on_device_screen_off", false)) {
            registerReceiver(turnOffFlashlightOnScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        }
        defaultPreferences.registerOnSharedPreferenceChangeListener(material3Listener);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && defaultPreferences.getString("theme", "system").equals("system"))
            defaultPreferences.edit().putString("theme", "light").apply();
        Utils.applyThemeFromSettings(this);
        Shortcuts.createNormalToggleShortcut(this);
        Shortcuts.createSosToggleShortcut(this);
        super.onCreate(savedInstanceState);
        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        helper = CameraHelper.getInstance(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                helper.getFlashlightStrengthLevel(this) > 1 &&
                defaultPreferences.getInt("flashlight_strength", -1) == -1) { //if flash brightness is not saved into preferences
            helper.setFlashlightStrength(helper.getFlashlightStrengthLevel(this)); //then set brightness to max
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                helper.getFlashlightStrengthLevel(this) > 1 &&
                defaultPreferences.getInt("flashlight_strength", -1) != -1) { //if flash brightness is saved into preferences
            helper.setFlashlightStrength(defaultPreferences.getInt("flashlight_strength", -1)); //then set brightness from there
        }
        setSupportActionBar(binding.toolbar);
        window = getWindow();
        legacyPreferences = getSharedPreferences("my_prefs", MODE_PRIVATE);
        applyListeners();
        init();
        if (savedInstanceState != null && legacyPreferences.getInt("default_option", 1) == 2) {
            brightness = savedInstanceState.getInt("brightness");
            WindowManager.LayoutParams layoutpars = window.getAttributes();
            layoutpars.screenBrightness = (float) brightness / 100;
            window.setAttributes(layoutpars);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            getSupportFragmentManager().setFragmentResultListener(NoFlashlightDialog.NO_FLASH_DIALOG_DISMISSED, this, ((requestKey, result) -> binding.bgOptions.callOnClick()));
            binding.sosButton.setVisibility(View.GONE);
            binding.sosIcon.setVisibility(View.GONE);
            binding.stroboscopeButton.setVisibility(View.GONE);
            binding.stroboscopeIcon.setVisibility(View.GONE);
            binding.stroboscopeInterval.setVisibility(View.GONE);
            binding.stroboscopeIntervalSlider.setVisibility(View.GONE);
        } else {
            helper.getNormalFlashStatus().observe(this, (isOn -> changeButtonColors(FlashlightMode.NORMAL, isOn)));
            helper.getSosStatus().observe(this, (isOn -> changeButtonColors(FlashlightMode.SOS, isOn)));
            helper.getStroboscopeStatus().observe(this, (isOn -> {
                changeButtonColors(FlashlightMode.STROBOSCOPE, isOn);
                binding.stroboscopeInterval.setVisibility(isOn ? View.VISIBLE : View.GONE);
                binding.stroboscopeIntervalSlider.setVisibility(isOn ? View.VISIBLE : View.GONE);
            }));
            binding.sosButton.setOnClickListener(v -> helper.toggleSos(this));
            binding.stroboscopeButton.setOnClickListener(v -> helper.toggleStroboscope(this));
            float stroboscopeIntervalInPreferences = defaultPreferences.getFloat("stroboscope_interval", -1);
            helper.setStroboscopeInterval(stroboscopeIntervalInPreferences != -1 ? (int) (stroboscopeIntervalInPreferences * 1000) : 500);
            binding.stroboscopeIntervalSlider.setValue(stroboscopeIntervalInPreferences != -1 ? stroboscopeIntervalInPreferences : 0.5F);
            binding.stroboscopeIntervalSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider slider) {}
                @Override
                public void onStopTrackingTouch(@NonNull Slider slider) {
                    helper.setStroboscopeInterval((int) (slider.getValue() * 1000));
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        defaultPreferences.edit().putFloat("stroboscope_interval", binding.stroboscopeIntervalSlider.getValue()).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(turnOffFlashlightOnScreenOffReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings_menu_item) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.about_menu_item) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void applyListeners() {
        binding.bgOptions.setOnClickListener(view -> {
            SharedPreferences.Editor editor = legacyPreferences.edit();
            editor.putInt("default_option", legacyPreferences.getInt("default_option", 1) == 1 ? 2 : 1);
            editor.apply();
            init();
        });
    }

    void init() {
        if (legacyPreferences.getInt("default_option", 1) == 1) {
            updateOptionsUI(true);
            refreshActivityForFlashLight();
        } else {
            updateOptionsUI(false);
            refreshActivityForScreenLight();
        }
    }

    void refreshActivityForFlashLight() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            new NoFlashlightDialog().show(getSupportFragmentManager(), null);
        else if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                helper.getFlashlightStrengthLevel(this) > 1) {
            binding.progressCircular.setProgress(0F);
            binding.progressCircular.setMax(helper.getFlashlightStrengthLevel(this) - 1);
            binding.progressCircular.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
                @Override
                public void onProgressChanged(@Nullable CircularSeekBar circularSeekBar, float v, boolean b) {
                    helper.setFlashlightStrength(Math.round(v + 1));
                    if (Boolean.TRUE.equals(helper.getNormalFlashStatus().getValue()))
                        helper.turnOnFlashWithStrength(MainActivity.this);
                }
                @Override
                public void onStopTrackingTouch(@Nullable CircularSeekBar circularSeekBar) {
                    if (circularSeekBar != null)
                        defaultPreferences.edit().putInt("flashlight_strength", Math.round(circularSeekBar.getProgress() + 1)).apply();
                }
                @Override
                public void onStartTrackingTouch(@Nullable CircularSeekBar circularSeekBar) {}
            });
            binding.progressCircular.setProgress(helper.getFlashlightStrength() - 1);
            binding.powerCenter.setOnClickListener(v -> helper.toggleNormalFlash(this));
        } else if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            binding.progressCircular.setOnSeekBarChangeListener(null);
            binding.progressCircular.setProgress(0F);
            binding.progressCircular.setEnabled(false);
            binding.progressCircular.setPointerColor(Color.parseColor("#AAAABB"));
            binding.powerCenter.setOnClickListener(v -> helper.toggleNormalFlash(this));
        }
        binding.rootLayout.setBackgroundColor(Color.parseColor("#00000000")); //transparent
    }

    private void changeButtonColors(FlashlightMode mode, boolean isTurnedOn) {
        switch (mode) {
            case NORMAL:
                binding.powerCenter.setColorFilter(isTurnedOn ? Color.parseColor("#28FFB137") : Color.parseColor("#F3F3F7"));
                binding.powerIcon.setColorFilter(isTurnedOn ? Color.parseColor("#FFB137") : Color.parseColor("#AAAABB"));
                break;
            case SOS:
                binding.sosButton.setColorFilter(isTurnedOn ? Color.parseColor("#28FFB137") : Color.parseColor("#F3F3F7"));
                binding.sosIcon.setColorFilter(isTurnedOn ? Color.parseColor("#FFB137") : Color.parseColor("#AAAABB"));
                break;
            case STROBOSCOPE:
                binding.stroboscopeButton.setColorFilter(isTurnedOn ? Color.parseColor("#28FFB137") : Color.parseColor("#F3F3F7"));
                binding.stroboscopeIcon.setColorFilter(isTurnedOn ? Color.parseColor("#FFB137") : Color.parseColor("#AAAABB"));
                break;
            default: throw new IllegalArgumentException();
        }
    }

    void updateOptionsUI(boolean isFlash) {
        if (isFlash) {
            //Change UI for options
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.bgOptionCircle.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            binding.bgOptionCircle.setLayoutParams(params);
            binding.flashIcon.setColorFilter(Color.parseColor("#FFB137"));
            binding.screenIcon.setColorFilter(Color.parseColor("#AAAABB"));
            binding.progressCircular.setProgress(0f);
        } else {
            binding.bgOptionCircle.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.bgOptionCircle.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            binding.bgOptionCircle.setLayoutParams(params);
            binding.flashIcon.setColorFilter(Color.parseColor("#AAAABB"));
            binding.screenIcon.setColorFilter(Color.parseColor("#FFB137"));
        }
    }

    void refreshActivityForScreenLight() {
        binding.progressCircular.setPointerColor(Color.parseColor("#FFB137"));
        binding.progressCircular.setEnabled(true);
        if (defaultPreferences.getBoolean("no_flash_when_screen", true) && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            helper.turnOffAll(this);
        binding.rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF")); //force set white, because it does not make sense for the app to be dark when using screen light
        if (binding.progressCircular.getProgress() > 0) {
            binding.progressCircular.setOnSeekBarChangeListener(null);
            binding.progressCircular.setProgress(0f);
        }
        binding.progressCircular.setMax(100);
        binding.progressCircular.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                if (progress != 0) brightness = (int) progress;
                else brightness = -1;
                WindowManager.LayoutParams layoutpars = window.getAttributes();
                layoutpars.screenBrightness = (float) brightness / 100;
                window.setAttributes(layoutpars);
            }
            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {}
            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {}
        });
        binding.powerCenter.setOnClickListener(view -> binding.progressCircular.setProgress(brightness != 100 ? 100 : 0));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (legacyPreferences.getInt("default_option", 1) == 2) {
            outState.putInt("brightness", brightness);
        }
    }
}
