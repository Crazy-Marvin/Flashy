package rocks.poopjournal.flashy;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = findViewById(R.id.toolbar_about);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        TextView appVersion = findViewById(R.id.app_version);
        appVersion.setText(getString(R.string.version_license, BuildConfig.VERSION_NAME));
        setupClickToGoToWebsite(appVersion, "https://github.com/Crazy-Marvin/Flashy/blob/development/LICENSE");
        ImageView appIcon = findViewById(R.id.app_icon);
        setupClickToGoToWebsite(appIcon, "https://crazymarvin.com/flashy/");
        ImageView fahadSaleemGithub = findViewById(R.id.fahadsaleem_github);
        setupClickToGoToWebsite(fahadSaleemGithub, "https://github.com/FahadSaleem/");
        ImageView crazyMarvinGithub = findViewById(R.id.crazymarvin_github);
        setupClickToGoToWebsite(crazyMarvinGithub, "https://github.com/CrazyMarvin/");
        ImageView crazyMarvinEmail = findViewById(R.id.crazymarvin_email);
        crazyMarvinEmail.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_SENDTO)
                        .setData(Uri.parse("mailto:"))
                        .putExtra(Intent.EXTRA_EMAIL, new String[]{"marvin@poopjournal.rocks"})
                        .putExtra(Intent.EXTRA_SUBJECT, "crazymarvin.com Contact")
                        .putExtra(Intent.EXTRA_TEXT, "Hello Marvin,\n...\n");
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_app_can_handle, Toast.LENGTH_SHORT).show();
            }
        });
        ImageView crazyMarvinTwitter = findViewById(R.id.crazymarvin_twitter);
        setupClickToGoToWebsite(crazyMarvinTwitter, "https://twitter.com/CrazyMarvinApps");
        TextView sourceCode = findViewById(R.id.source_code);
        setupClickToGoToWebsite(sourceCode, "https://github.com/Crazy-Marvin/Flashy");
        TextView reportProblem = findViewById(R.id.report_problem);
        setupClickToGoToWebsite(reportProblem, "https://github.com/Crazy-Marvin/Flashy/issues");
        TextView translate = findViewById(R.id.translate);
        setupClickToGoToWebsite(translate, "https://hosted.weblate.org/engage/flashy/");
        LinearLayout featherIcons = findViewById(R.id.feather_icons);
        setupClickToGoToWebsite(featherIcons, "https://feathericons.com/");
        LinearLayout mdIcons = findViewById(R.id.md_icons);
        setupClickToGoToWebsite(mdIcons, "https://fonts.google.com/icons");
        LinearLayout jetpack = findViewById(R.id.jetpack);
        setupClickToGoToWebsite(jetpack, "https://developer.android.com/jetpack");
        LinearLayout circularSeekBar = findViewById(R.id.circular_seekbar);
        setupClickToGoToWebsite(circularSeekBar, "https://github.com/tankery/CircularSeekBar");
        LinearLayout kotlin = findViewById(R.id.kotlin);
        setupClickToGoToWebsite(kotlin, "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt");
        LinearLayout java = findViewById(R.id.java);
        setupClickToGoToWebsite(java, "http://openjdk.java.net/legal/gplv2+ce.html");
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