package com.poopjournal.flashy;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GrantSystemWritePermissionActivity extends AppCompatActivity {
    TextView noticeGrantPermission;
    Button buttonGrantPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grant_system_write_permission);
        noticeGrantPermission = findViewById(R.id.notice_grant_permission);
        buttonGrantPermission = findViewById(R.id.button_grant_permission);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) completeWidgetSetup();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) completeWidgetSetup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            noticeGrantPermission.setText(R.string.system_settings_permission_notice);
            buttonGrantPermission.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        .setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            noticeGrantPermission.setText(R.string.system_settings_permission_granted);
            buttonGrantPermission.setEnabled(false);
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