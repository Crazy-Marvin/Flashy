package rocks.poopjournal.flashy.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.RemoteViews;

import rocks.poopjournal.flashy.utils.CameraHelper;
import rocks.poopjournal.flashy.R;
import rocks.poopjournal.flashy.activities.MainActivity;

public class FlashlightWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_TOGGLE = "rocks.poopjournal.flashy.TOGGLE_FLASH";

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
                if (Boolean.TRUE.equals(CameraHelper.getNormalFlashStatus().getValue())) {
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
        if (intent.getAction().equals(ACTION_TOGGLE)) {
            CameraHelper helper = CameraHelper.getInstance(context);
            helper.toggleNormalFlash(context);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            onUpdate(context, manager, manager.getAppWidgetIds(new ComponentName(context, FlashlightWidgetProvider.class)));
        }
    }
}
