package rocks.poopjournal.flashy;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import rocks.poopjournal.flashy.databinding.ActivityGrantSystemWritePermissionBinding;

public class GrantSystemWritePermissionActivity extends AppCompatActivity {
    private ActivityGrantSystemWritePermissionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGrantSystemWritePermissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) completeWidgetSetup();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) completeWidgetSetup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            binding.noticeGrantPermission.setText(R.string.system_settings_permission_notice);
            binding.buttonGrantPermission.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        .setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.noticeGrantPermission.setText(R.string.system_settings_permission_granted);
            binding.buttonGrantPermission.setEnabled(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            completeWidgetSetup();
        }
    }

    private void completeWidgetSetup() {
        setResult(RESULT_OK);
        finish();
    }
}