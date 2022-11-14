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

public class SOSWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_TOGGLE_SOS = "rocks.poopjournal.flashy.TOGGLE_SOS";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_sos);
            if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                remoteViews.setImageViewResource(R.id.img_sos, R.drawable.flash_off);
                Intent intent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 42, intent, PendingIntent.FLAG_IMMUTABLE);
                remoteViews.setOnClickPendingIntent(R.id.img_sos, pendingIntent);
            } else {
                if (Boolean.TRUE.equals(CameraHelper.getSosStatus().getValue())) {
                    remoteViews.setImageViewResource(R.id.img_sos, R.drawable.sos_on);
                } else
                    remoteViews.setImageViewResource(R.id.img_sos, R.drawable.sos);
                Intent intent = new Intent(context, SOSWidgetProvider.class)
                        .setAction(ACTION_TOGGLE_SOS);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 69, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.img_sos, pendingIntent);
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(ACTION_TOGGLE_SOS)) {
            CameraHelper helper = CameraHelper.getInstance(context);
            helper.toggleSos(context);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            onUpdate(context, manager, manager.getAppWidgetIds(new ComponentName(context, SOSWidgetProvider.class)));
        }
    }
}
