package rocks.poopjournal.flashy.utils;

import android.graphics.Color;
import static android.hardware.Camera.Parameters.FLASH_MODE_AUTO;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.preference.PreferenceManager;

import java.util.List;

import rocks.poopjournal.flashy.R;
import rocks.poopjournal.flashy.utils.CameraHelper;
import rocks.poopjournal.flashy.widgets.FlashlightWidgetProvider;
import rocks.poopjournal.flashy.widgets.SOSWidgetProvider;

public class Utils {

    private Utils() {} //make class non-instantiable

    public static String getFlashOnParameter(Camera camera) {
        List<String> flashModes = camera.getParameters().getSupportedFlashModes();
        if (flashModes.contains(FLASH_MODE_TORCH)) {
            return FLASH_MODE_TORCH;
        } else if (flashModes.contains(FLASH_MODE_ON)) {
            return FLASH_MODE_ON;
        } else if (flashModes.contains(FLASH_MODE_AUTO)) {
            return FLASH_MODE_AUTO;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Must be called after toggling flashlight to update the widget UI.
     * @see CameraHelper#toggleNormalFlash(Context)
     * @see CameraHelper#toggleSos(Context)
     * @see CameraHelper#toggleStroboscope(Context)
     */
    public static void updateWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] flashlightWidgetIds = manager.getAppWidgetIds(new ComponentName(context, FlashlightWidgetProvider.class));
        if (flashlightWidgetIds.length > 0) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, flashlightWidgetIds)
                    .setClass(context, FlashlightWidgetProvider.class);
            context.sendBroadcast(intent);
        }
        int[] sosWidgetIds = manager.getAppWidgetIds(new ComponentName(context, SOSWidgetProvider.class));
        if (sosWidgetIds.length > 0) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, sosWidgetIds)
                    .setClass(context, SOSWidgetProvider.class);
            context.sendBroadcast(intent);
        }
    }

    public static void applyThemeFromSettings(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        switch (preferences.getString("theme", "system")) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        context.setTheme(preferences.getBoolean("md3", false) ? R.style.AppTheme_Material3 : R.style.AppTheme);
    }

    public static int invertColor(int color) {
        int red = 255 - Color.red(color);
        int green = 255 - Color.green(color);
        int blue = 255 - Color.blue(color);
        return Color.rgb(red, green, blue);
    }
}
