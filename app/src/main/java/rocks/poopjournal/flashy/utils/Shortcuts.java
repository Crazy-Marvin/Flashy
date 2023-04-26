package rocks.poopjournal.flashy.utils;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import rocks.poopjournal.flashy.R;
import rocks.poopjournal.flashy.activities.InvisibleActivity;

public class Shortcuts {

    private Shortcuts() {}

    public static void createNormalToggleShortcut(Context context) {
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, "1")
                .setShortLabel(context.getString(R.string.flashlight))
                .setIcon(IconCompat.createWithResource(context, R.drawable.flashlight_off))
                .setIntent(new Intent(context, InvisibleActivity.class)
                        .setAction(InvisibleActivity.ACTION_TOGGLE_NORMAL))
                .build();
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut);
    }

    public static void createSosToggleShortcut(Context context) {
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, "2")
                .setShortLabel(context.getString(R.string.sos))
                .setIcon(IconCompat.createWithResource(context, R.drawable.sos))
                .setIntent(new Intent(context, InvisibleActivity.class)
                        .setAction(InvisibleActivity.ACTION_TOGGLE_SOS))
                .build();
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut);
    }
}
