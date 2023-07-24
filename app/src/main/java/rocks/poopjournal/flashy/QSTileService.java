package rocks.poopjournal.flashy;

import android.content.pm.PackageManager;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import rocks.poopjournal.flashy.utils.CameraHelper;

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
        getQsTile().setState(Boolean.TRUE.equals(helper.getNormalFlashStatus().getValue()) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            getQsTile().setState(Tile.STATE_UNAVAILABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getQsTile().setSubtitle(getString(R.string.no_camera));
            }
        } else if (Boolean.TRUE.equals(helper.getNormalFlashStatus().getValue())) {
            getQsTile().setState(Tile.STATE_ACTIVE);
        }
        getQsTile().updateTile();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        helper.turnOffNormalFlash(this);
    }

    @Override
    public void onClick() {
        super.onClick();
        helper.toggleNormalFlash(this);
        getQsTile().setState(Boolean.TRUE.equals(helper.getNormalFlashStatus().getValue()) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        getQsTile().updateTile();
    }
}