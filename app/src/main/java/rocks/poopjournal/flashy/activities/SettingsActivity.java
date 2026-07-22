package rocks.poopjournal.flashy.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import rocks.poopjournal.flashy.R;
import rocks.poopjournal.flashy.databinding.SettingsActivityBinding;
import rocks.poopjournal.flashy.utils.CameraHelper;
import rocks.poopjournal.flashy.utils.Utils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.applyThemeFromSettings(this);
        super.onCreate(savedInstanceState);
        SettingsActivityBinding binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.backIcon.setOnClickListener(v ->{
            finish();
        });
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private CameraHelper helper;

        /**
         * The preference layout keeps titles on a single line, which cuts the longer ones off.
         * Let them wrap onto another line instead, so the whole title stays readable.
         */
        private static void allowMultiLineTitles(PreferenceGroup group) {
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Preference preference = group.getPreference(i);
                preference.setSingleLineTitle(false);
                if (preference instanceof PreferenceGroup)
                    allowMultiLineTitles((PreferenceGroup) preference);
            }
        }

        private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
            if (key.equals("words_per_min") && Integer.parseInt(sharedPreferences.getString("farnsworth_unit", "0")) <= helper.getCurrentDitLength(sharedPreferences) ||
                    key.equals("farnsworth_unit") && sharedPreferences.getString("farnsworth_unit", "").isEmpty()) {
                int ditLength = helper.getCurrentDitLength(sharedPreferences);
                EditTextPreference farnsworthUnitLength = findPreference("farnsworth_unit");
                assert farnsworthUnitLength != null;
                sharedPreferences.edit().putString("farnsworth_unit", String.valueOf(ditLength + ditLength / 4)).apply();
                farnsworthUnitLength.setSummary(String.valueOf(ditLength + ditLength / 4));
                farnsworthUnitLength.setText(String.valueOf(ditLength + ditLength / 4));
            }
        };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            allowMultiLineTitles(getPreferenceScreen());
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            preferences.registerOnSharedPreferenceChangeListener(listener);
            helper = CameraHelper.getInstance(requireContext());
            if (Boolean.TRUE.equals(helper.getSosStatus().getValue())) helper.toggleSos(requireContext());

            ListPreference themePref = findPreference("theme");
            assert themePref != null;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                themePref.setEntries(R.array.theme_entries_p);
                themePref.setEntryValues(R.array.theme_values_p);
            }
            themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                requireActivity().recreate();
                return true;
            });

            SwitchPreferenceCompat md3Pref = findPreference("md3");
            assert md3Pref != null;
            md3Pref.setOnPreferenceChangeListener((preference, newValue) -> {
                requireActivity().recreate();
                return true;
            });

            Preference setAssistantPref = findPreference("set_as_digital_assistant");
            if (setAssistantPref != null) {
                setAssistantPref.setOnPreferenceClickListener(preference -> {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.assistant_dialog_title))
                            .setMessage(getString(R.string.assistant_dialog_message))
                            .setPositiveButton(getString(R.string.positive_dialog_button), (dialog, which) -> {
                                try {
                                    startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(requireContext(), "Settings not available on this device.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(getString(R.string.negative_dialog_button), null);

                    AlertDialog dialog = builder.show();

                    // Fix: Set text color manually to ensure visibility
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);

                    return true;
                });
            }

            EditTextPreference wordsPerMin = findPreference("words_per_min");
            assert wordsPerMin != null;
            SwitchPreferenceCompat useFarnsworth = findPreference("use_farnsworth");
            EditTextPreference farnsworthUnitLength = findPreference("farnsworth_unit");
            assert useFarnsworth != null;
            assert farnsworthUnitLength != null;
            Preference learnMoreAboutMorseTiming = findPreference("learn_more_morse_timing");
            assert learnMoreAboutMorseTiming != null;
            SwitchPreferenceCompat noFlashWhenScreen = findPreference("no_flash_when_screen");
            assert noFlashWhenScreen != null;
            SwitchPreferenceCompat noFlashOnDeviceScreenOff = findPreference("no_flash_on_device_screen_off");
            assert noFlashOnDeviceScreenOff != null;
            if (!requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                wordsPerMin.setVisible(false);
                useFarnsworth.setVisible(false);
                farnsworthUnitLength.setVisible(false);
                noFlashWhenScreen.setVisible(false);
                noFlashOnDeviceScreenOff.setVisible(false);
                learnMoreAboutMorseTiming.setVisible(false);
            } else {
                wordsPerMin.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
                wordsPerMin.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (newValue.toString().isEmpty()) {
                        Toast.makeText(requireContext(), R.string.words_per_min_error, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    int newNum = Integer.parseInt(newValue.toString());
                    if (newNum == 0) {
                        Toast.makeText(requireContext(), R.string.words_per_min_error, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    return true;
                });
                useFarnsworth.setOnPreferenceChangeListener((preference, newValue) -> {
                    farnsworthUnitLength.setVisible((boolean) newValue);
                    return true;
                });
                if (preferences.getString("farnsworth_unit", "").isEmpty()) { //initialize default value
                    int ditLength = helper.getCurrentDitLength(requireContext());
                    preferences.edit().putString("farnsworth_unit", String.valueOf(ditLength + ditLength / 4)).apply();
                    farnsworthUnitLength.setSummary(String.valueOf(ditLength + ditLength / 4));
                    farnsworthUnitLength.setText(String.valueOf(ditLength + ditLength / 4));
                } else farnsworthUnitLength.setSummary(preferences.getString("farnsworth_unit", ""));
                farnsworthUnitLength.setVisible(preferences.getBoolean("use_farnsworth", false));
                farnsworthUnitLength.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
                farnsworthUnitLength.setOnPreferenceClickListener(preference -> {
                    farnsworthUnitLength.setDialogMessage(getString(R.string.farnsworth_unit_length_explanation, helper.getCurrentDitLength(requireContext())));
                    return true;
                });
                farnsworthUnitLength.setOnPreferenceChangeListener((preference, newValue) -> {
                    if (!newValue.toString().isEmpty()) {
                        int newLength = Integer.parseInt(newValue.toString());
                        if (newLength <= helper.getCurrentDitLength(requireContext())) {
                            Toast.makeText(requireContext(), R.string.farnsworth_unit_length_error, Toast.LENGTH_LONG).show();
                            return false;
                        }
                        preference.setSummary(String.valueOf(newLength));
                    }
                    return true;
                });
                learnMoreAboutMorseTiming.setOnPreferenceClickListener(preference -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW)
                                .setData(Uri.parse("https://morsecode.world/international/timing.html"));
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(requireContext(), R.string.no_app_can_handle, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
            }
        }
    }
}