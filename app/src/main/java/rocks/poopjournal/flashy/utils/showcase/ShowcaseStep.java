package rocks.poopjournal.flashy.utils.showcase;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

/**
 * One page of the guided tour: which views to cut out of the scrim, and what to say about them.
 *
 * <p>A step is built fluently, e.g.
 * <pre>ShowcaseStep.spotlight(R.string.body, R.id.power_center)
 *         .asCircle()
 *         .captionAbove()</pre>
 */
public final class ShowcaseStep {

    /** Put the caption wherever there is room, preferring below the highlight. */
    public static final int CAPTION_AUTO = 0;
    /** Always put the caption above the highlight. */
    public static final int CAPTION_ABOVE = 1;
    /** Always put the caption below the highlight. */
    public static final int CAPTION_BELOW = 2;

    final int[] targetIds;
    /** The copy for this step, or 0 for a step that shows no caption at all. */
    @StringRes
    final int bodyRes;

    /** One hole around all targets together, instead of one hole per target. */
    boolean unionTargets;
    /** Rounds the hole all the way off, so a square target comes out as a circle. */
    boolean circular;
    /** False leaves the home screen as it is: no scrim, no hole, caption at the bottom. */
    boolean dimmed = true;
    int captionPlacement = CAPTION_AUTO;
    float paddingDp = 10f;
    Runnable beforeShow;

    private ShowcaseStep(int bodyRes, int[] targetIds) {
        this.bodyRes = bodyRes;
        this.targetIds = targetIds;
    }

    /**
     * A step that shows the untouched screen and says nothing about it, used to open the tour. It
     * carries no caption and so no buttons either; a tap anywhere moves the tour on.
     */
    public static ShowcaseStep intro() {
        ShowcaseStep step = new ShowcaseStep(0, new int[0]);
        step.dimmed = false;
        return step;
    }

    /** A step that dims the screen and cuts {@code targets} back out of it. */
    public static ShowcaseStep spotlight(@StringRes int body, @IdRes int... targets) {
        return new ShowcaseStep(body, targets);
    }

    public ShowcaseStep asOneHole() {
        unionTargets = true;
        return this;
    }

    public ShowcaseStep asCircle() {
        circular = true;
        return this;
    }

    public ShowcaseStep captionAbove() {
        captionPlacement = CAPTION_ABOVE;
        return this;
    }

    public ShowcaseStep captionBelow() {
        captionPlacement = CAPTION_BELOW;
        return this;
    }

    /** How much room to leave around the target, in dp. */
    public ShowcaseStep padding(float dp) {
        paddingDp = dp;
        return this;
    }

    /**
     * Runs right before the step is measured, to bring whatever it points at on screen. Anything
     * changed here has to be put back by the tour's finish callback.
     */
    public ShowcaseStep before(Runnable action) {
        beforeShow = action;
        return this;
    }
}