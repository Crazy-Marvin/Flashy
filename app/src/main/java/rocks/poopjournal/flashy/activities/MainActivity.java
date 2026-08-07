package rocks.poopjournal.flashy.activities;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.slider.Slider;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;

import me.tankery.lib.circularseekbar.CircularSeekBar;

import java.util.ArrayList;
import java.util.List;

import rocks.poopjournal.flashy.NoFlashlightDialog;
import rocks.poopjournal.flashy.R;
import rocks.poopjournal.flashy.databinding.MainActivityBinding;
import rocks.poopjournal.flashy.receivers.ScreenOffBroadcastReceiver;
import rocks.poopjournal.flashy.utils.CameraHelper;
import rocks.poopjournal.flashy.utils.GlyphHelper;
import rocks.poopjournal.flashy.utils.Shortcuts;
import rocks.poopjournal.flashy.utils.Utils;
import rocks.poopjournal.flashy.utils.showcase.Showcase;
import rocks.poopjournal.flashy.utils.showcase.ShowcaseStep;

public class MainActivity extends AppCompatActivity {
    //Fields
    private int invertedBackgroundColor = 0;
    private int brightness = -999;
    private Window window;
    private SharedPreferences legacyPreferences; //kept for legacy reasons
    private SharedPreferences defaultPreferences;
    private CameraHelper helper;
    private GlyphHelper glyph;
    /** Level the Glyph was last told to show, read by the thread blinking SOS and the stroboscope. */
    private volatile int glyphLevel;
    private MainActivityBinding binding;
    /** The guided tour of this screen, alive only while it is running. */
    private Showcase showcase;
    private final ScreenOffBroadcastReceiver turnOffFlashlightOnScreenOffReceiver = new ScreenOffBroadcastReceiver();

    private View[] colorViews;
    private View selectedColorView = null;
    /** Background of the current day/night theme, i.e. the colour the other screens use. */
    private int themeDefaultColor = Color.WHITE;
    private int selectedScreenColor = themeDefaultColor; // default
    /** True while the screen shows the theme background rather than a colour the user picked. */
    private boolean usingThemeBackground = true;
    /** State the UI is currently painted for, so redundant repaints can be skipped. */
    private int appliedBackgroundColor = Color.TRANSPARENT;
    private boolean appliedThemeBackground = true;

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
        applyWindowInsets();
        // The theme is already resolved for the active night mode here, so this picks up the
        // same background the settings/about screens draw.
        themeDefaultColor = MaterialColors.getColor(this, android.R.attr.colorBackground, Color.WHITE);
        selectedScreenColor = themeDefaultColor;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        helper = CameraHelper.getInstance(this);
        if (GlyphHelper.isSupported()) glyph = GlyphHelper.getInstance(this);
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
            getSupportFragmentManager().setFragmentResultListener(NoFlashlightDialog.NO_FLASH_DIALOG_DISMISSED, this, ((requestKey, result) -> {
                binding.bgOptions.callOnClick();
                // The tour holds off while this dialog is up, so this is its cue on a device
                // without a flashlight.
                maybeStartShowcase();
            }));
            binding.stroboscopeInterval.setVisibility(View.GONE);
            binding.stroboscopeIntervalSlider.setVisibility(View.GONE);
        } else {
            helper.getNormalFlashStatus().observe(this, (isOn -> {
                changeButtonColors(FlashlightMode.NORMAL, isOn);
                refreshBackground();
                updateGlyph();
            }));
            helper.getSosStatus().observe(this, (isOn -> {
                changeButtonColors(FlashlightMode.SOS, isOn);
                refreshBackground();
                updateGlyph();
            }));
            helper.getStroboscopeStatus().observe(this, (isOn -> {
                changeButtonColors(FlashlightMode.STROBOSCOPE, isOn);
                binding.stroboscopeInterval.setVisibility(isOn ? View.VISIBLE : View.GONE);
                binding.stroboscopeIntervalSlider.setVisibility(isOn ? View.VISIBLE : View.GONE);
                refreshBackground();
                updateGlyph();
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
        View colorDefault = findViewById(R.id.color_white);
        View colorRed = findViewById(R.id.color_red);
        View colorGreen = findViewById(R.id.color_green);
        View colorBlue = findViewById(R.id.color_blue);

        // The first swatch follows the theme: white in light mode, the dark background in dark
        // mode while every light is off, white again as soon as one is on.
        colorDefault.setTag(themeDefaultColor);
        applySwatchColor(colorDefault, neutralBackgroundColor());
        colorRed.setTag(Color.parseColor("#EF4444"));
        colorGreen.setTag(Color.parseColor("#23C760"));
        colorBlue.setTag(Color.parseColor("#3A86F7"));

        colorViews = new View[]{colorDefault, colorRed, colorGreen, colorBlue};

        for (View colorView : colorViews) {
            colorView.setOnClickListener(v -> onColorCircleSelected(colorView));
        }

        binding.rootLayout.post(() -> onColorCircleSelected(colorDefault));
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
        colorPickerView.setColorListener((ColorListener) (color, fromUser) -> {
            // The picker fires once from its own layout pass with the colour under the centre of a
            // palette it has never drawn (it starts out GONE, so it is never sized). Reacting to
            // that would overwrite the selection made above with a junk colour.
            if (!fromUser) return;
            usingThemeBackground = false;
            selectedScreenColor = color;
            refreshBackground();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeStartShowcase();
    }

    /**
     * Walks a first time user through the home screen. Checked on every resume rather than only on
     * creation, so clearing the flag from the settings starts the tour on the way back here.
     */
    private void maybeStartShowcase() {
        if (showcase != null && showcase.isRunning()) return;
        if (Showcase.hasBeenSeen(defaultPreferences)) return;
        // A device without a flashlight opens on the "no flashlight" dialog, which sits in a window
        // of its own above anything the tour could draw. Wait for it to be dismissed.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) && isFlashOption())
            return;
        // Two posts deep: the first gets behind the default colour selection init() queued during
        // creation, the second gives the layout that selection triggers a chance to settle before
        // any target is measured.
        binding.rootLayout.post(() -> binding.rootLayout.post(this::startShowcase));
    }

    /** Builds and starts the home screen tour, remembering what to put back once it is over. */
    private void startShowcase() {
        if (isFinishing() || isDestroyed()) return;
        if (showcase != null && showcase.isRunning()) return;

        // The tour shows off the screen light, so the mode and colour the user arrived in are kept
        // aside and restored the moment it ends, however it ends.
        final boolean wasFlashOption = isFlashOption();
        final View previousColor = selectedColorView != null ? selectedColorView : colorViews[0];

        List<ShowcaseStep> steps = new ArrayList<>();
        steps.add(ShowcaseStep
                .intro()
                .before(() -> {
                    selectScreenLightMode();
                    onColorCircleSelected(colorViews[3]); // blue
                }));
        steps.add(ShowcaseStep
                .spotlight(R.string.showcase_source_body, R.id.bg_options)
                .padding(8f));
        steps.add(ShowcaseStep
                .spotlight(R.string.showcase_colors_body,
                        R.id.flashlight_options, R.id.color_option_row)
                .asOneHole()
                .padding(12f));
        steps.add(ShowcaseStep
                .spotlight(R.string.showcase_power_body, R.id.power_center)
                .asCircle()
                .padding(14f)
                .captionAbove());
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            steps.add(ShowcaseStep
                    .spotlight(R.string.showcase_sos_body, R.id.sos_icon)
                    .asCircle()
                    .padding(10f));
            steps.add(ShowcaseStep
                    .spotlight(R.string.showcase_strobe_body, R.id.stroboscope_icon)
                    .asCircle()
                    .padding(10f));
        }
        steps.add(ShowcaseStep
                .spotlight(R.string.showcase_more_body, R.id.about_icon, R.id.settings_icon)
                .asCircle()
                .padding(12f));

        showcase = new Showcase(this, defaultPreferences, steps, () -> {
            // The colour goes back first: in the flash mode its row is hidden, so a colour left
            // picked there would keep painting the background with nothing to change it.
            onColorCircleSelected(previousColor);
            if (wasFlashOption) selectFlashMode();
        });
        showcase.start();
    }

    /** Switches to the screen light without going through the toggle the user would tap. */
    private void selectScreenLightMode() {
        if (!isFlashOption()) return;
        legacyPreferences.edit().putInt("default_option", 2).apply();
        init();
    }

    private void selectFlashMode() {
        if (isFlashOption()) return;
        legacyPreferences.edit().putInt("default_option", 1).apply();
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (glyph == null) return;
        if (defaultPreferences.getBoolean(GlyphHelper.PREFERENCE_KEY, true)) {
            glyph.connect();
            // SOS and the stroboscope blink far too fast to go through the flashlight state, so
            // the Glyph is told about every single blink instead.
            helper.setFlashPulseListener(isOn -> glyph.setLevel(isOn ? glyphLevel : 0));
            updateGlyph();
        } else {
            helper.setFlashPulseListener(null);
            glyph.disconnect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (glyph == null) return;
        helper.setFlashPulseListener(null);
        // The Glyph belongs to whatever is in front now, so hand it back on the way out.
        glyph.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        defaultPreferences.edit().putFloat("stroboscope_interval", binding.stroboscopeIntervalSlider.getValue()).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (showcase != null) showcase.cancel();
        turnOffFlashlightOnScreenOffReceiver.unregisterWith(this);
    }

    /**
     * From Android 15 on the app always draws edge to edge, so the toolbar title would end up
     * underneath the status bar and the display cutout. Inset the content by the system bars: the
     * background still fills the whole window, only the views move out from under them.
     */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
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
        updateGlyph();
    }



    private void changeButtonColors(FlashlightMode mode, boolean isTurnedOn) {
        boolean isNeutralBackground = isNeutralBackground();

        int onIconColor = isNeutralBackground ? Color.parseColor("#FFB137") : Color.WHITE;
        int offIconColor = Color.parseColor("#AAAABB");
        int centerOnColor = isNeutralBackground ? Color.parseColor("#28FFB137") : Color.WHITE; // for powerCenter
        int overlayColor = isNeutralBackground
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
        if (isFlash) {
            //Change UI for options
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.bgOptionCircle.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            binding.bgOptionCircle.setLayoutParams(params);
            binding.progressCircular.setProgress(0f);
        } else {
            binding.bgOptionCircle.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.bgOptionCircle.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            binding.bgOptionCircle.setLayoutParams(params);
        }
        updateOptionsIconColors(isFlash);
    }

    /** Tints the flash/screen switch, highlighting whichever of the two is active. */
    private void updateOptionsIconColors(boolean isFlash) {
        boolean isNeutral = isNeutralBackground();
        int activeColor = isNeutral ? Color.parseColor("#FFB137") : selectedScreenColor;
        int inactiveColor = isNeutral ? Color.parseColor("#AAAABB") : Color.WHITE;
        binding.flashIcon.setColorFilter(isFlash ? activeColor : inactiveColor);
        binding.screenIcon.setColorFilter(isFlash ? inactiveColor : activeColor);
    }

    void refreshActivityForFlashLight() {
        applyRingColors();
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
                    updateGlyph();
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
        applyRingColors();
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
                // The screen light just went on or off, so the background may have to follow.
                refreshBackground();
                updateGlyph();
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

    private void updateUIColors(int backgroundColor) {
        appliedBackgroundColor = backgroundColor;
        appliedThemeBackground = usingThemeBackground;

        boolean isNeutral = isNeutralBackground();

        // Slightly offset shade of the background, so the pills stay visible in both modes
        int overlayColor = isNeutral
                ? neutralColor(R.color.neutral_surface_light, R.color.neutral_surface_dark)
                : withAlpha(selectedScreenColor, 0.75f); // Apply alpha for other colors

        // Use contrast for text/icons
        invertedBackgroundColor = contrastColorFor(backgroundColor);

        PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(overlayColor, PorterDuff.Mode.SRC_ATOP);
        PorterDuffColorFilter selectorFilter = new PorterDuffColorFilter(
                isNeutral
                        ? neutralColor(R.color.neutral_surface_raised_light, R.color.neutral_surface_raised_dark)
                        : Color.WHITE,
                PorterDuff.Mode.SRC_ATOP);

        // Apply filters
        binding.toolbar.setTitleTextColor(invertedBackgroundColor);
        binding.bgOptions.getBackground().setColorFilter(colorFilter);
        binding.bgFlashlightMode.getBackground().setColorFilter(colorFilter);
        binding.colorOptionRow.getBackground().mutate().setColorFilter(colorFilter);
        binding.bgOptionCircle.getBackground().mutate().setColorFilter(selectorFilter);
        // Muted grey reads on both the light and the dark theme background; on a picked colour the
        // background is saturated enough that plain white works better.
        int bottomIconColor = isNeutral ? Color.parseColor("#AAAABB") : Color.WHITE;
        binding.aboutIcon.setColorFilter(bottomIconColor);
        binding.settingsIcon.setColorFilter(bottomIconColor);
        binding.stroboscopeInterval.setTextColor(invertedBackgroundColor);
        // Without a flash the power icon never gets a state driven tint, so its translucent black
        // stroke would disappear on a dark background.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            binding.powerIcon.setColorFilter(Color.parseColor("#AAAABB"));

        // Set background and system bars
        binding.rootLayout.setBackgroundColor(backgroundColor);
        window.setStatusBarColor(backgroundColor);
        window.setNavigationBarColor(backgroundColor);
        // Keep the status/navigation bar icons readable against whatever is behind them
        WindowInsetsControllerCompat barIcons = WindowCompat.getInsetsController(window, binding.getRoot());
        boolean lightBackground = invertedBackgroundColor == Color.BLACK;
        barIcons.setAppearanceLightStatusBars(lightBackground);
        barIcons.setAppearanceLightNavigationBars(lightBackground);
    }


    boolean isFlashOption() {
        return legacyPreferences.getInt("default_option", 1) == 1;
    }

    /** Puts the light the app is giving right now onto the Glyph of a Nothing phone. */
    private void updateGlyph() {
        if (glyph == null) return;
        glyphLevel = currentLightLevel();
        glyph.setLevel(glyphLevel);
    }

    /**
     * How far up the light of the active mode is turned, 0 to 100, and 0 whenever that light is
     * off. The ring keeps showing the flashlight strength while the flashlight is off, which is
     * why the two are not the same thing.
     */
    private int currentLightLevel() {
        if (!isFlashOption()) return Math.max(0, brightness);
        if (!isFlashOn()) return 0;
        float max = binding.progressCircular.getMax();
        // A flashlight without adjustable strength leaves the ring at zero, so it is simply full.
        if (max <= 0) return 100;
        // The ring counts from zero while the lowest strength the flashlight takes is one.
        return Math.round((binding.progressCircular.getProgress() + 1) / (max + 1) * 100);
    }

    /**
     * True while the volume keys should move the brightness ring instead of the device volume.
     * A ring that cannot be dragged, i.e. a flashlight without adjustable strength, is left alone
     * so the keys keep doing what they normally do.
     */
    private boolean volumeKeysAdjustBrightness() {
        return defaultPreferences.getBoolean("volume_keys_brightness", true)
                && binding.progressCircular.isEnabled();
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (isVolumeKey(keyCode) && volumeKeysAdjustBrightness()) {
            stepBrightness(keyCode == KeyEvent.KEYCODE_VOLUME_UP);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        // Swallowing the press alone still leaves the release to the system, which shows its
        // volume panel over the app, so the whole key has to be taken.
        if (isVolumeKey(keyCode) && volumeKeysAdjustBrightness()) return true;
        return super.onKeyUp(keyCode, event);
    }

    private boolean isVolumeKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
    }

    /** Moves the ring one step, taking the same path a drag on it takes. */
    private void stepBrightness(boolean up) {
        float max = binding.progressCircular.getMax();
        // Twenty steps over the whole ring on the screen light, and single steps on the much
        // shorter flashlight strength scale.
        float step = max > 20 ? Math.round(max / 20f) : 1f;
        float current = binding.progressCircular.getProgress();
        float progress = Math.max(0f, Math.min(max, current + (up ? step : -step)));
        if (progress == current) return;
        binding.progressCircular.setProgress(progress);
        // A drag stores the flashlight strength once the finger lifts; the keys have no such moment.
        if (isFlashOption())
            defaultPreferences.edit().putInt("flashlight_strength", Math.round(progress + 1)).apply();
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
        usingThemeBackground = color == themeDefaultColor;
        selectedScreenColor = color;
        refreshBackground();

        if (!isFlashOption()) {
            refreshActivityForScreenLight();
            updateOptionsUI(false);
        }

        // Hide color picker
        binding.colorPickerView.setVisibility(View.GONE);
    }


    int getViewColor(View view) {
        Object tag = view.getTag();
        return (tag instanceof Integer) ? (Integer) tag : themeDefaultColor;
    }

    /**
     * True while the screen shows the theme background rather than one of the colours the user
     * picked. That background is white in light mode, and dark in dark mode until a light is on.
     */
    private boolean isNeutralBackground() {
        return usingThemeBackground;
    }

    /** True while any of the flashlight modes has the LED lit. */
    private boolean isFlashOn() {
        return Boolean.TRUE.equals(helper.getNormalFlashStatus().getValue())
                || Boolean.TRUE.equals(helper.getSosStatus().getValue())
                || Boolean.TRUE.equals(helper.getStroboscopeStatus().getValue());
    }

    /** True while the screen light itself is turned up. */
    private boolean isScreenLightOn() {
        return brightness > 0;
    }

    /**
     * The background used when no colour is picked. It follows the theme while everything is off,
     * but a lit screen has to be white to be of any use, so in dark mode it only stays dark until
     * a light comes on.
     */
    private int neutralBackgroundColor() {
        return isFlashOn() || isScreenLightOn() ? Color.WHITE : themeDefaultColor;
    }

    /** The colour the screen should be painted with right now. */
    private int currentBackgroundColor() {
        return usingThemeBackground ? neutralBackgroundColor() : selectedScreenColor;
    }

    /** Repaints the screen whenever the light state or the picked colour changed the background. */
    private void refreshBackground() {
        // The default swatch previews the background picking it would give.
        if (colorViews != null) applySwatchColor(colorViews[0], neutralBackgroundColor());
        int background = currentBackgroundColor();
        if (background == appliedBackgroundColor && usingThemeBackground == appliedThemeBackground)
            return;
        updateUIColors(background);
        applyRingColors();
        updateOptionsIconColors(isFlashOption());
    }

    /** Colours the seek bar ring for the current background. */
    private void applyRingColors() {
        boolean isNeutral = isNeutralBackground();
        binding.progressCircular.setCircleColor(isNeutral
                ? neutralColor(R.color.neutral_track_light, R.color.neutral_track_dark)
                : dimColor(selectedScreenColor, 0.9f)); // simulate alpha by dimming
        if (isFlashOption()) return; // the flash ring keeps the pointer it was set up with
        int accentColor = isNeutral ? Color.parseColor("#FFB137") : Color.WHITE;
        binding.progressCircular.setPointerColor(accentColor);
        binding.progressCircular.setCircleProgressColor(accentColor);
    }

    /** Picks the neutral shade that reads on the background currently painted. */
    private int neutralColor(int lightColorRes, int darkColorRes) {
        boolean onLightBackground = contrastColorFor(currentBackgroundColor()) == Color.BLACK;
        return ContextCompat.getColor(this, onLightBackground ? lightColorRes : darkColorRes);
    }

    /** Black or white, whichever stays readable on top of {@code background}. */
    private int contrastColorFor(int background) {
        return ColorUtils.calculateLuminance(background) > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /** Repaints a colour circle in the picker row, keeping it visible against the row behind it. */
    private void applySwatchColor(View swatch, int color) {
        Drawable background = swatch.getBackground();
        if (background == null) return;
        Drawable mutated = background.mutate();
        if (mutated instanceof GradientDrawable) {
            GradientDrawable circle = (GradientDrawable) mutated;
            circle.setColor(color);
            circle.setStroke(Math.round(getResources().getDisplayMetrics().density),
                    withAlpha(contrastColorFor(color), 0.35f));
        } else {
            mutated.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        }
        swatch.setBackground(mutated);
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
