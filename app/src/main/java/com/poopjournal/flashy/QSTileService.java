package com.poopjournal.flashy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QSTileService extends TileService {
    private CameraManager manager;
    private SharedPreferences preferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        if (key.equals("flash_enabled")) {
            getQsTile().setState(sharedPreferences.getBoolean(key, false) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            getQsTile().updateTile();
            Utils.updateWidgets(this);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        preferences = getSharedPreferences("my_prefs", MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            getQsTile().setState(Tile.STATE_UNAVAILABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getQsTile().setSubtitle(getString(R.string.no_camera));
            }
        } else if (preferences.getBoolean("flash_enabled", false)) {
            getQsTile().setState(Tile.STATE_ACTIVE);
        }
        getQsTile().updateTile();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        if (preferences.getBoolean("flash_enabled", false)) {
            try {
                manager.setTorchMode(manager.getCameraIdList()[0], false);
                preferences.edit().putBoolean("flash_enabled", false).apply();
            } catch (CameraAccessException e) {
                Toast.makeText(this, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!preferences.getBoolean("flash_enabled", false)) try {
            manager.setTorchMode(manager.getCameraIdList()[0], true);
            preferences.edit().putBoolean("flash_enabled", true).apply();
        } catch (CameraAccessException e) {
            Toast.makeText(this, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        else try {
            manager.setTorchMode(manager.getCameraIdList()[0], false);
            preferences.edit().putBoolean("flash_enabled", false).apply();
        } catch (CameraAccessException e) {
            Toast.makeText(this, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
