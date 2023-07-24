package rocks.poopjournal.flashy.receivers;

import static java.util.Objects.requireNonNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.Display;

import androidx.annotation.RequiresApi;
import androidx.core.hardware.display.DisplayManagerCompat;

import rocks.poopjournal.flashy.utils.CameraHelper;

public class ScreenOffBroadcastReceiver extends BroadcastReceiver {

    private boolean isRegistered = false;

    public void registerWith(Context context) {
        if (isRegistered) {
            return;
        }
        context.registerReceiver(this, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        isRegistered = true;
    }

    public void unregisterWith(Context context) {
        if (!isRegistered) {
            return;
        }
        context.unregisterReceiver(this);
        isRegistered = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (Intent.ACTION_SCREEN_OFF.equals(action) && !isScreenOn(context)) {
            CameraHelper helper = CameraHelper.getInstance(context);
            helper.turnOffAll(context);
        }
    }

    /**
     * Checks if the screen is actually turned off because {@link Intent#ACTION_SCREEN_OFF} doesn't necessarily indicate that the screen is off.
     * @return true if screen is actually turned off
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private boolean isScreenOn(Context context) {
        Display display = requireNonNull(DisplayManagerCompat.getInstance(context).getDisplay(Display.DEFAULT_DISPLAY));
        return display.getState() != Display.STATE_OFF;
    }
}
