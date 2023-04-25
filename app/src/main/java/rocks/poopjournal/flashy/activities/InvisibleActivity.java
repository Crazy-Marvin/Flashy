package rocks.poopjournal.flashy.activities;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import rocks.poopjournal.flashy.utils.CameraHelper;

public class InvisibleActivity extends AppCompatActivity {
    public static final String ACTION_TOGGLE_NORMAL = "rocks.poopjournal.flashy.TOGGLE_FLASH_NORMAL";
    public static final String ACTION_TOGGLE_SOS = "rocks.poopjournal.flashy.TOGGLE_FLASH_SOS";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CameraHelper helper = CameraHelper.getInstance(this);
        String action = getIntent().getAction();
        switch (action) {
            case ACTION_TOGGLE_NORMAL:
                helper.toggleNormalFlash(this);
                break;
            case ACTION_TOGGLE_SOS:
                helper.toggleSos(this);
                break;
            default:
                Log.w(InvisibleActivity.class.getSimpleName(), "Unknown action: " + action);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }
}
