package com.poopjournal.flashy;

import static android.hardware.Camera.Parameters.FLASH_MODE_AUTO;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;

import java.util.List;

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
     * Must be called after changing the "flash_enabled" value in shared preferences to update the widget UI.
     */
    public static void updateWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, FlashlightWidgetProvider.class));
        if (widgetIds.length > 0) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                    .setClass(context, FlashlightWidgetProvider.class);
            context.sendBroadcast(intent);
        }
    }

}
