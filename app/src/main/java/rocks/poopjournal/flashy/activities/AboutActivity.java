package rocks.poopjournal.flashy.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import rocks.poopjournal.flashy.BuildConfig;
import rocks.poopjournal.flashy.R;
import rocks.poopjournal.flashy.databinding.ActivityAboutBinding;
import rocks.poopjournal.flashy.utils.Utils;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.applyThemeFromSettings(this);
        super.onCreate(savedInstanceState);
        ActivityAboutBinding binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        binding.appVersion.setText(getString(R.string.version_text, BuildConfig.VERSION_NAME));
        binding.appacheVersion.setText(getString(R.string.version_license));
        binding.backIcon.setOnClickListener(v -> finish());
        setupClickToGoToWebsite(binding.appVersion, "https://github.com/Crazy-Marvin/Flashy/blob/development/LICENSE");
        setupClickToGoToWebsite(binding.appIcon, "https://crazymarvin.com/flashy/");
        setupClickToGoToWebsite(binding.sourceCode, "https://github.com/Crazy-Marvin/Flashy");
        setupClickToGoToWebsite(binding.reportProblem, "https://github.com/Crazy-Marvin/Flashy/issues");
        setupClickToGoToWebsite(binding.translate, "https://hosted.weblate.org/engage/flashy/");
        setupClickToGoToWebsite(binding.featherIcons, "https://feathericons.com/");
        setupClickToGoToWebsite(binding.mdIcons, "https://fonts.google.com/icons");
        setupClickToGoToWebsite(binding.jetpack, "https://developer.android.com/jetpack");
        setupClickToGoToWebsite(binding.circularSeekbar, "https://github.com/tankery/CircularSeekBar");
        setupClickToGoToWebsite(binding.kotlin, "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt");
        setupClickToGoToWebsite(binding.java, "http://openjdk.java.net/legal/gplv2+ce.html");
    }

    private void setupClickToGoToWebsite(View view, String url) {
        view.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(url));
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_app_can_handle, Toast.LENGTH_SHORT).show();
            }
        });
    }
}