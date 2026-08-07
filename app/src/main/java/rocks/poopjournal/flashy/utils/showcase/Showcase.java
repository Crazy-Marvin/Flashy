package rocks.poopjournal.flashy.utils.showcase;

import android.content.SharedPreferences;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Walks a first time user through a screen, one {@link ShowcaseStep} at a time.
 *
 * <p>The tour remembers that it ran in the preferences, so it only greets a user once. Bump the
 * version in {@link #PREF_SEEN} to show a reworked tour to everybody again.
 */
public final class Showcase {

    public static final String PREF_SEEN = "showcase_home_v1_seen";

    public static boolean hasBeenSeen(SharedPreferences preferences) {
        return preferences.getBoolean(PREF_SEEN, false);
    }

    /** Makes the tour run again the next time the screen it belongs to comes up. */
    public static void reset(SharedPreferences preferences) {
        preferences.edit().remove(PREF_SEEN).apply();
    }

    private final ComponentActivity activity;
    private final SharedPreferences preferences;
    private final List<ShowcaseStep> steps;
    @Nullable
    private final Runnable onFinished;

    private ShowcaseOverlay overlay;
    private OnBackPressedCallback backCallback;
    /** Light status and navigation bar icons as they were before the tour, or null while unchanged. */
    private boolean[] systemBarsBefore;
    private int index = -1;
    private boolean running;

    /**
     * @param onFinished run once the tour is over, however it ended, so the screen can be put back
     *                   the way the user found it.
     */
    public Showcase(@NonNull ComponentActivity activity, @NonNull SharedPreferences preferences,
                    @NonNull List<ShowcaseStep> steps, @Nullable Runnable onFinished) {
        this.activity = activity;
        this.preferences = preferences;
        this.steps = steps;
        this.onFinished = onFinished;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        if (running || steps.isEmpty()) return;
        // The decor view rather than the content view, so the scrim runs behind the status and
        // navigation bars instead of stopping at them.
        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup)) return;
        ViewGroup host = (ViewGroup) decor;

        running = true;
        overlay = new ShowcaseOverlay(activity);
        overlay.setOnNextListener(this::advance);
        overlay.setOnSkipListener(this::finish);
        host.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Back should leave the tour, not the app.
        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, backCallback);

        advance();
    }

    /**
     * Ends the tour without marking it as seen, e.g. because the screen is going away. The finish
     * callback still runs, so a tour torn down half way through does not leave the screen in
     * whatever state its last step put it in.
     */
    public void cancel() {
        if (!running) return;
        running = false;
        restoreSystemBars();
        if (backCallback != null) backCallback.remove();
        if (overlay != null && overlay.getParent() instanceof ViewGroup) {
            ((ViewGroup) overlay.getParent()).removeView(overlay);
        }
        overlay = null;
        if (onFinished != null) onFinished.run();
    }

    private void advance() {
        if (!running) return;
        index++;
        if (index >= steps.size()) {
            finish();
            return;
        }
        showCurrent();
    }

    private void showCurrent() {
        ShowcaseStep step = steps.get(index);
        if (step.beforeShow != null) step.beforeShow.run();
        // The scrim is light, so while it is up the system bar icons have to be dark to stay
        // readable. An undimmed step leaves them to the app.
        if (step.dimmed) setLightSystemBars(true);
        else restoreSystemBars();
        // Anything the step just revealed still has to be laid out before it can be measured, so
        // the highlight waits for the layout pass that change scheduled.
        OneShotPreDrawListener.add(overlay, () -> {
            if (!running || overlay == null) return;
            overlay.showStep(
                    holesFor(step),
                    step.dimmed,
                    step.bodyRes == 0 ? "" : activity.getString(step.bodyRes),
                    index == steps.size() - 1,
                    step.captionPlacement);
        });
        // A step that reveals nothing new leaves the hierarchy idle, and a pre draw listener on an
        // idle hierarchy is never called. Asking for a draw makes sure the pass it waits for comes.
        overlay.invalidate();
    }

    /** Darkens the system bar icons for the scrim, remembering what they looked like before. */
    private void setLightSystemBars(boolean light) {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(activity.getWindow(), overlay);
        if (systemBarsBefore == null) {
            systemBarsBefore = new boolean[]{
                    controller.isAppearanceLightStatusBars(),
                    controller.isAppearanceLightNavigationBars()};
        }
        controller.setAppearanceLightStatusBars(light);
        controller.setAppearanceLightNavigationBars(light);
    }

    private void restoreSystemBars() {
        if (systemBarsBefore == null || overlay == null) return;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(activity.getWindow(), overlay);
        controller.setAppearanceLightStatusBars(systemBarsBefore[0]);
        controller.setAppearanceLightNavigationBars(systemBarsBefore[1]);
        systemBarsBefore = null;
    }

    private void finish() {
        if (!running) return;
        running = false;
        restoreSystemBars();
        if (backCallback != null) backCallback.remove();
        preferences.edit().putBoolean(PREF_SEEN, true).apply();
        ShowcaseOverlay dismissing = overlay;
        overlay = null;
        if (dismissing != null) dismissing.dismiss(onFinished);
        else if (onFinished != null) onFinished.run();
    }

    /** Where each of the step's targets sits, in the overlay's own coordinates. */
    private List<ShowcaseOverlay.Hole> holesFor(ShowcaseStep step) {
        List<ShowcaseOverlay.Hole> holes = new ArrayList<>();
        if (step.targetIds.length == 0) return holes;

        int[] overlayLocation = new int[2];
        overlay.getLocationInWindow(overlayLocation);
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                step.paddingDp, activity.getResources().getDisplayMetrics());

        RectF union = null;
        for (int id : step.targetIds) {
            View target = activity.findViewById(id);
            // A target that is hidden or not laid out yet is simply left out rather than
            // highlighting the top left corner of the screen.
            if (target == null || target.getVisibility() != View.VISIBLE
                    || target.getWidth() == 0 || target.getHeight() == 0) continue;

            int[] location = new int[2];
            target.getLocationInWindow(location);
            float left = location[0] - overlayLocation[0];
            float top = location[1] - overlayLocation[1];
            RectF rect = new RectF(left, top, left + target.getWidth(), top + target.getHeight());
            rect.inset(-padding, -padding);

            if (step.unionTargets) {
                if (union == null) union = rect;
                else union.union(rect);
            } else {
                holes.add(new ShowcaseOverlay.Hole(rect, step.circular));
            }
        }
        if (union != null) holes.add(new ShowcaseOverlay.Hole(union, step.circular));
        return holes;
    }
}