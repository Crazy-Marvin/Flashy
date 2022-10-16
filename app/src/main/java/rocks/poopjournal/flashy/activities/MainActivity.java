package rocks.poopjournal.flashy.activities;

import android.animation.LayoutTransition;
import android.content.Intent;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.slider.Slider;

import me.tankery.lib.circularseekbar.CircularSeekBar;
import rocks.poopjournal.flashy.utils.CameraHelper;
import rocks.poopjournal.flashy.NoFlashlightDialog;
import rocks.poopjournal.flashy.R;
import rocks.poopjournal.flashy.utils.Utils;
import rocks.poopjournal.flashy.databinding.MainActivityBinding;

public class MainActivity extends AppCompatActivity {
    //Fields
    private int brightness = -999;
    private Window window;
    private SharedPreferences legacyPreferences; //kept for legacy reasons
    private SharedPreferences defaultPreferences;
    private CameraHelper helper;
    private MainActivityBinding binding;
    private enum FlashlightMode {
        NORMAL, SOS, STROBOSCOPE
    }
    private final SharedPreferences.OnSharedPreferenceChangeListener material3Listener = (sharedPreferences, key) -> {
        if (key.equals("md3")) recreate();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        defaultPreferences.registerOnSharedPreferenceChangeListener(material3Listener);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && defaultPreferences.getString("theme", "system").equals("system"))
            defaultPreferences.edit().putString("theme", "light").apply();
        Utils.applyThemeFromSettings(this);
        super.onCreate(savedInstanceState);
        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        helper = CameraHelper.getInstance(this);
        setSupportActionBar(binding.toolbar);
        window = getWindow();
        legacyPreferences = getSharedPreferences("my_prefs", MODE_PRIVATE);
        applyListeners();
        init();
        if (savedInstanceState != null && legacyPreferences.getInt("default_option", 1) == 2) {
            Log.d("flashy_test", "saved 2, " + savedInstanceState.getInt("brightness"));
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
            CameraHelper.getNormalFlashStatus().observe(this, (isOn -> changeButtonColors(FlashlightMode.NORMAL, isOn)));
            CameraHelper.getSosStatus().observe(this, (isOn -> changeButtonColors(FlashlightMode.SOS, isOn)));
            CameraHelper.getStroboscopeStatus().observe(this, (isOn -> {
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
                public void onStartTrackingTouch(@NonNull Slider slider) {

                }

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
        //Refresh Seekbar
        binding.progressCircular.setOnSeekBarChangeListener(null);
        binding.progressCircular.setProgress(0F);
        binding.progressCircular.setEnabled(false);
        binding.progressCircular.setPointerColor(Color.parseColor("#AAAABB"));
        binding.rootLayout.setBackgroundColor(Color.parseColor("#00000000")); //transparent
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            new NoFlashlightDialog().show(getSupportFragmentManager(), null);
        else binding.powerCenter.setOnClickListener(v -> helper.toggleNormalFlash(this));
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
            turnOff();
        binding.rootLayout.setBackgroundColor(Color.parseColor("#FFFFFF")); //force set white, because it does not make sense for the app to be dark when using screen light
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
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });
        binding.powerCenter.setOnClickListener(view -> binding.progressCircular.setProgress(brightness != 100 ? 100 : 0));
    }

    public void turnOff() {
        if (Boolean.TRUE.equals(CameraHelper.getNormalFlashStatus().getValue())) {
            helper.toggleNormalFlash(this);
        }
        if (Boolean.TRUE.equals(CameraHelper.getSosStatus().getValue())) {
            helper.toggleSos(this);
        }
        if (Boolean.TRUE.equals(CameraHelper.getStroboscopeStatus().getValue())) {
            helper.toggleStroboscope(this);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (legacyPreferences.getInt("default_option", 1) == 2) {
            outState.putInt("brightness", brightness);
        }
    }
}
