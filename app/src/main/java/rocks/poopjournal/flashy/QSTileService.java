package rocks.poopjournal.flashy;

import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QSTileService extends TileService {
    private CameraHelper helper;

    @Override
    public void onCreate() {
        super.onCreate();
        helper = CameraHelper.getInstance(this);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        getQsTile().setState(Boolean.TRUE.equals(CameraHelper.isFlashOn.getValue()) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        getQsTile().updateTile();
        Utils.updateFlashlightWidgets(this);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            getQsTile().setState(Tile.STATE_UNAVAILABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getQsTile().setSubtitle(getString(R.string.no_camera));
            }
        } else if (Boolean.TRUE.equals(CameraHelper.isFlashOn.getValue())) {
            getQsTile().setState(Tile.STATE_ACTIVE);
        }
        getQsTile().updateTile();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        if (Boolean.TRUE.equals(CameraHelper.isFlashOn.getValue())) {
            try {
                helper.toggleMarshmallow();
                Utils.updateFlashlightWidgets(this);
            } catch (CameraAccessException e) {
                Toast.makeText(this, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            helper.toggleMarshmallow();
            Utils.updateFlashlightWidgets(this);
            getQsTile().setState(Boolean.TRUE.equals(CameraHelper.isFlashOn.getValue()) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            getQsTile().updateTile();
        } catch (CameraAccessException e) {
            Toast.makeText(this, R.string.cannot_access_camera, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}