package com.poopjournal.flashy;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.RemoteViews;

public class ScreenBrightnessWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_MAX_SCREEN_BRIGHTNESS = "com.poopjournal.flashy.MAXIMIZE_SCREEN_BRIGHTNESS";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_screen);
            Intent intent = new Intent(context, ScreenBrightnessWidgetProvider.class)
                    .setAction(ACTION_MAX_SCREEN_BRIGHTNESS);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 123, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.img_screen, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(ACTION_MAX_SCREEN_BRIGHTNESS)) {
            //no need to check for permission, since we already handled it in the config activity
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        }
    }
}
