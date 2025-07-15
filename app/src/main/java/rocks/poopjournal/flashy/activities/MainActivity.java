package rocks.poopjournal.flashy.activities;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.slider.Slider;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;

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
    private int invertedBackgroundColor = 0;
    private int brightness = -999;
    private Window window;
    private SharedPreferences legacyPreferences; //kept for legacy reasons
    private SharedPreferences defaultPreferences;
    private CameraHelper helper;
    private MainActivityBinding binding;
    private final ScreenOffBroadcastReceiver turnOffFlashlightOnScreenOffReceiver = new ScreenOffBroadcastReceiver();

    private View[] colorViews;
    private View selectedColorView = null;
    private int selectedScreenColor = Color.WHITE; // default

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
                    turnOffFlashlightOnScreenOffReceiver.registerWith(this);
                } else {
                    turnOffFlashlightOnScreenOffReceiver.unregisterWith(this);
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
            turnOffFlashlightOnScreenOffReceiver.registerWith(this);
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        if (savedInstanceState != null && !isFlashOption()) {
            brightness = savedInstanceState.getInt("brightness");
            WindowManager.LayoutParams layoutpars = window.getAttributes();
            layoutpars.screenBrightness = (float) brightness / 100;
            window.setAttributes(layoutpars);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            getSupportFragmentManager().setFragmentResultListener(NoFlashlightDialog.NO_FLASH_DIALOG_DISMISSED, this, ((requestKey, result) -> binding.bgOptions.callOnClick()));
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
            binding.sosIcon.setOnClickListener(v -> helper.toggleSos(this));
            binding.stroboscopeIcon.setOnClickListener(v -> helper.toggleStroboscope(this));
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
        View colorWhite = findViewById(R.id.color_white);
        View colorRed = findViewById(R.id.color_red);
        View colorGreen = findViewById(R.id.color_green);
        View colorBlue = findViewById(R.id.color_blue);

        colorWhite.setTag(Color.parseColor("#ffffff"));
        colorRed.setTag(Color.parseColor("#EF4444"));
        colorGreen.setTag(Color.parseColor("#23C760"));
        colorBlue.setTag(Color.parseColor("#3A86F7"));

        colorViews = new View[]{colorWhite, colorRed, colorGreen, colorBlue};

        for (View colorView : colorViews) {
            colorView.setOnClickListener(v -> onColorCircleSelected(colorView));
        }

        binding.rootLayout.post(() -> onColorCircleSelected(colorWhite));
        findViewById(R.id.show_palette_icon).setOnClickListener(v -> {
            binding.colorPickerView.setVisibility(View.VISIBLE);
            if (selectedColorView != null) {
                ViewGroup.LayoutParams shrinkParams = selectedColorView.getLayoutParams();
                shrinkParams.width = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
                shrinkParams.height = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
                selectedColorView.setLayoutParams(shrinkParams);
                selectedColorView = null;
            }
        });
        ColorPickerView colorPickerView = findViewById(R.id.colorPickerView);
        colorPickerView.setColorListener(new ColorListener() {
            @Override
            public void onColorSelected(int color, boolean fromUser) {
                selectedScreenColor = color;
                updateUIColors(color);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        defaultPreferences.edit().putFloat("stroboscope_interval", binding.stroboscopeIntervalSlider.getValue()).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        turnOffFlashlightOnScreenOffReceiver.unregisterWith(this);
    }

    void applyListeners() {
        binding.bgOptions.setOnClickListener(view -> {
            SharedPreferences.Editor editor = legacyPreferences.edit();
            editor.putInt("default_option", isFlashOption() ? 2 : 1);
            editor.apply();
            init();
        });
        binding.aboutIcon.setOnClickListener(view -> startActivity(new Intent(this, AboutActivity.class)));
        binding.settingsIcon.setOnClickListener(view -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    void init() {
        if (isFlashOption()) {
            updateOptionsUI(true);
            refreshActivityForFlashLight();
        } else {
            updateOptionsUI(false);
            refreshActivityForScreenLight();
        }
    }



    private void changeButtonColors(FlashlightMode mode, boolean isTurnedOn) {
        boolean isWhiteBackground = selectedScreenColor == Color.WHITE;

        int onIconColor = isWhiteBackground ? Color.parseColor("#FFB137") : Color.WHITE;
        int offIconColor = Color.parseColor("#AAAABB");
        int centerOnColor = isWhiteBackground ? Color.parseColor("#28FFB137") : Color.WHITE; // for powerCenter
        int overlayColor = isWhiteBackground
                ? Color.parseColor("#FFB137")
                : withAlpha(selectedScreenColor, 0.75f);


        switch (mode) {
            case NORMAL:
                binding.powerCenter.setColorFilter(isTurnedOn ? centerOnColor :overlayColor);
                binding.powerIcon.clearColorFilter(); // Clear any previous filters
                binding.powerIcon.setColorFilter(isTurnedOn ? onIconColor : offIconColor);

                break;
            case SOS:
                binding.sosIcon.setColorFilter(isTurnedOn ? onIconColor : offIconColor);
                break;
            case STROBOSCOPE:
                binding.stroboscopeIcon.setColorFilter(isTurnedOn ? onIconColor : offIconColor);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }


    void updateOptionsUI(boolean isFlash) {
        boolean isWhite = selectedScreenColor == Color.WHITE;
        if (isFlash) {
            //Change UI for options
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.bgOptionCircle.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            binding.bgOptionCircle.setLayoutParams(params);
            binding.flashIcon.setColorFilter(isWhite ? Color.parseColor("#FFB137") : selectedScreenColor);
            binding.screenIcon.setColorFilter(isWhite ? Color.parseColor("#AAAABB") : Color.WHITE);
            binding.progressCircular.setProgress(0f);
        } else {
            binding.bgOptionCircle.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.bgOptionCircle.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            binding.bgOptionCircle.setLayoutParams(params);
            binding.flashIcon.setColorFilter(isWhite ? Color.parseColor("#AAAABB") : Color.WHITE);
            binding.screenIcon.setColorFilter(isWhite ? Color.parseColor("#FFB137") : selectedScreenColor);
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
                public void onStartTrackingTouch(@Nullable CircularSeekBar circularSeekBar) {
                }
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
        binding.colorPickerView.setEnabled(false);
        binding.colorPickerView.setVisibility(View.GONE);
        findViewById(R.id.color_option_row).setVisibility(View.GONE);

    }
    void refreshActivityForScreenLight() {
        boolean isWhiteBackground = selectedScreenColor == Color.WHITE;
        int inactiveProgressColor = isWhiteBackground
                ? Color.parseColor("#F3F3F7") // light gray for white
                : dimColor(selectedScreenColor, 0.9f); // simulate alpha by dimming

        int activeProgressColor = isWhiteBackground
                ? Color.parseColor("#FFB137") // yellow
                : Color.WHITE; // white on colored background
        int pointerColor = isWhiteBackground ? Color.parseColor("#FFB137") : Color.WHITE;
        binding.progressCircular.setPointerColor(pointerColor);
        binding.progressCircular.setCircleProgressColor(activeProgressColor);
        binding.progressCircular.setCircleColor(inactiveProgressColor);
        binding.progressCircular.setEnabled(true);
        if (defaultPreferences.getBoolean("no_flash_when_screen", true) && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            helper.turnOffAll(this);
        findViewById(R.id.color_option_row).setVisibility(View.VISIBLE);
        binding.colorPickerView.setVisibility(View.GONE);

        binding.colorPickerView.setEnabled(true);
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
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
            }
        });
        binding.powerCenter.setOnClickListener(view -> binding.progressCircular.setProgress(brightness != 100 ? 100 : 0));
    }

    void updateUIColors(int backgroundColor) {
        selectedScreenColor = backgroundColor;

        boolean isWhite = selectedScreenColor == Color.WHITE;

        // Fallback color for white background
        int overlayColor = isWhite
                ? Color.parseColor("#EFEFF1")
                : withAlpha(selectedScreenColor, 0.75f); // Apply alpha for other colors

        // Use contrast for text/icons


        invertedBackgroundColor = isWhite ? Color.BLACK : Color.WHITE;

        PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(overlayColor, PorterDuff.Mode.SRC_ATOP);

        // Apply filters
        binding.toolbar.setTitleTextColor(invertedBackgroundColor);
        binding.bgOptions.getBackground().setColorFilter(colorFilter);
        binding.bgFlashlightMode.getBackground().setColorFilter(colorFilter);
        binding.aboutIcon.setColorFilter(invertedBackgroundColor);
        binding.settingsIcon.setColorFilter(invertedBackgroundColor);

        // Set background and system bars
        binding.rootLayout.setBackgroundColor(backgroundColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(backgroundColor);
            window.setNavigationBarColor(backgroundColor);
        }
    }


    boolean isFlashOption() {
        return legacyPreferences.getInt("default_option", 1) == 1;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!isFlashOption()) {
            outState.putInt("brightness", brightness);
        }
    }

    private void onColorCircleSelected(View selectedView) {
        // Shrink previously selected view
        if (selectedColorView != null) {
            ViewGroup.LayoutParams shrinkParams = selectedColorView.getLayoutParams();
            shrinkParams.width = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
            shrinkParams.height = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
            selectedColorView.setLayoutParams(shrinkParams);
        }

        // Enlarge newly selected view
        ViewGroup.LayoutParams enlargeParams = selectedView.getLayoutParams();
        enlargeParams.width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
        enlargeParams.height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
        selectedView.setLayoutParams(enlargeParams);

        selectedColorView = selectedView;

        // Get background color from tag or fallback
        int color = getViewColor(selectedView);
        selectedScreenColor = color;
        updateUIColors(color);

        if (!isFlashOption()) {
            refreshActivityForScreenLight();
            updateOptionsUI(false);
        }

        // Hide color picker
        binding.colorPickerView.setVisibility(View.GONE);
    }


    int getViewColor(View view) {
        Object tag = view.getTag();
        return (tag instanceof Integer) ? (Integer) tag : Color.WHITE;
    }

    private int withAlpha(int color, float alpha) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return Color.argb((int) (alpha * 255), r, g, b);
    }

    private int dimColor(int color, float factor) {
        int r = (int)(Color.red(color) * factor);
        int g = (int)(Color.green(color) * factor);
        int b = (int)(Color.blue(color) * factor);
        return Color.rgb(r, g, b); // No alpha, fully opaque
    }
}
