package com.poopjournal.flashy;

import static android.content.Context.MODE_PRIVATE;
import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.widget.RemoteViews;

public class FlashlightWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_TOGGLE = "com.poopjournal.flashy.TOGGLE_FLASH";
    private SharedPreferences preferences;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_flashlight);
            if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                remoteViews.setImageViewResource(R.id.img, R.drawable.flash_off);
                Intent intent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 42, intent, PendingIntent.FLAG_IMMUTABLE);
                remoteViews.setOnClickPendingIntent(R.id.img, pendingIntent);
            } else {
                preferences = context.getSharedPreferences("my_prefs", MODE_PRIVATE);
                if (preferences.getBoolean("flash_enabled", false)) {
                    remoteViews.setImageViewResource(R.id.img, R.drawable.flashlight_on);
                } else
                    remoteViews.setImageViewResource(R.id.img, R.drawable.flashlight_off);
                Intent intent = new Intent(context, FlashlightWidgetProvider.class)
                        .setAction(ACTION_TOGGLE);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 69, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.img, pendingIntent);
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        preferences = context.getSharedPreferences("my_prefs", MODE_PRIVATE);
        if (intent.getAction().equals(ACTION_TOGGLE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                if (!preferences.getBoolean("flash_enabled", false)) {
                    try {
                        manager.setTorchMode(manager.getCameraIdList()[0], true);
                        preferences.edit().putBoolean("flash_enabled", true).apply();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        manager.setTorchMode(manager.getCameraIdList()[0], false);
                        preferences.edit().putBoolean("flash_enabled", false).apply();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Camera camera1 = Camera.open();
                if (!preferences.getBoolean("flash_enabled", false)) {
                    try {
                        Camera.Parameters parameters = camera1.getParameters();
                        parameters.setFlashMode(Utils.getFlashOnParameter(camera1));
                        camera1.setParameters(parameters);
                        camera1.setPreviewTexture(new SurfaceTexture(0));
                        camera1.startPreview();
                        camera1.autoFocus((success, camera) -> {});
                        preferences.edit().putBoolean("flash_enabled", true).apply();
                    } catch (Exception e) {
                        // We are expecting this to happen on devices that don't support autofocus.
                    }
                } else {
                    try {
                        Camera.Parameters parameters = camera1.getParameters();
                        parameters.setFlashMode(FLASH_MODE_OFF);
                        camera1.setParameters(parameters);
                        preferences.edit().putBoolean("flash_enabled", false).apply();
                    } catch (Exception e) {
                        // This will happen if the camera fails to turn on.
                    }
                }
            }
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            onUpdate(context, manager, manager.getAppWidgetIds(new ComponentName(context, FlashlightWidgetProvider.class)));
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }
}
