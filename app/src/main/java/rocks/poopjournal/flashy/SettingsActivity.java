package rocks.poopjournal.flashy;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import rocks.poopjournal.flashy.databinding.SettingsActivityBinding;

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
        setSupportActionBar(binding.toolbarSettings);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private CameraHelper helper;
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
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            preferences.registerOnSharedPreferenceChangeListener(listener);
            helper = CameraHelper.getInstance(requireContext());

            ListPreference themePref = findPreference("theme");
            assert themePref != null;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                themePref.setEntries(R.array.theme_entries_p);
                themePref.setEntryValues(R.array.theme_values_p);
            }
            themePref.setOnPreferenceChangeListener(((preference, newValue) -> {
                requireActivity().recreate();
                return true;
            }));

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
            if (!requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                wordsPerMin.setVisible(false);
                useFarnsworth.setVisible(false);
                farnsworthUnitLength.setVisible(false);
                noFlashWhenScreen.setVisible(false);
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