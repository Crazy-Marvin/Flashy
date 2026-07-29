package rocks.poopjournal.flashy.utils.showcase;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import rocks.poopjournal.flashy.R;

/**
 * The full screen sheet the tour draws on: a coloured scrim with the current targets punched back
 * out of it, and a caption placed in whichever half of the screen the holes left free.
 *
 * <p>The scrim lives in its own child view so that only it has to be rendered in software, which
 * {@link PorterDuff.Mode#CLEAR} needs; the caption above it keeps hardware rendering and with it
 * smooth ripples on its buttons.
 */
public final class ShowcaseOverlay extends FrameLayout {

    private static final long TRANSITION_MS = 240L;
    private static final float MAX_CAPTION_WIDTH_DP = 340f;
    /**
     * How much room to leave under the caption of a step that highlights nothing: enough for the
     * 24dp icons the home screen sits 20dp above its bottom edge, plus the usual margin.
     */
    private static final float UNDIMMED_BOTTOM_GAP_DP = 72f;

    /** A single cut-out of the scrim. */
    public static final class Hole {
        final RectF rect;
        final boolean circular;

        public Hole(RectF rect, boolean circular) {
            this.rect = rect;
            this.circular = circular;
        }

        float radius(float defaultRadius) {
            return circular ? Math.min(rect.width(), rect.height()) / 2f : defaultRadius;
        }
    }

    private final ScrimView scrim;
    private final View caption;
    private final TextView bodyView;
    private final TextView nextButton;
    private final int captionMargin;
    private final int undimmedBottomGap;
    private final int maxCaptionWidth;

    private int captionPlacement = ShowcaseStep.CAPTION_AUTO;
    @Nullable
    private Runnable onNext;
    @Nullable
    private Runnable onSkip;

    public ShowcaseOverlay(@NonNull Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        scrim = new ScrimView(context);
        addView(scrim, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        caption = LayoutInflater.from(context).inflate(R.layout.view_showcase_caption, this, false);
        addView(caption);
        bodyView = caption.findViewById(R.id.showcase_body);
        nextButton = caption.findViewById(R.id.showcase_next);
        TextView skipButton = caption.findViewById(R.id.showcase_skip);

        nextButton.setOnClickListener(v -> {
            if (onNext != null) onNext.run();
        });
        skipButton.setOnClickListener(v -> {
            if (onSkip != null) onSkip.run();
        });

        captionMargin = dp(24f);
        undimmedBottomGap = dp(UNDIMMED_BOTTOM_GAP_DP);
        maxCaptionWidth = dp(MAX_CAPTION_WIDTH_DP);
    }

    public void setOnNextListener(@Nullable Runnable listener) {
        onNext = listener;
    }

    public void setOnSkipListener(@Nullable Runnable listener) {
        onSkip = listener;
    }

    /**
     * Moves the tour on to {@code holes}, animating the scrim over from wherever it was. A step
     * with nothing to say shows no caption at all, buttons included, and is left for a tap
     * anywhere to move on from.
     */
    public void showStep(List<Hole> holes, boolean dimmed, CharSequence body,
                         boolean lastStep, int placement) {
        boolean hasCaption = body.length() > 0;
        caption.setVisibility(hasCaption ? VISIBLE : GONE);
        captionPlacement = placement;

        if (hasCaption) {
            bodyView.setText(body);
            nextButton.setText(lastStep ? R.string.showcase_done : R.string.showcase_next);
            caption.setAlpha(0f);
            caption.animate().alpha(1f).setDuration(TRANSITION_MS).start();
        }

        scrim.animateTo(holes, dimmed);
        requestLayout();
    }

    /** Fades the whole sheet out and hands back once it is gone. */
    public void dismiss(@Nullable Runnable onDone) {
        setOnNextListener(null);
        setOnSkipListener(null);
        animate().alpha(0f).setDuration(TRANSITION_MS).withEndAction(() -> {
            if (getParent() instanceof ViewGroup) ((ViewGroup) getParent()).removeView(this);
            if (onDone != null) onDone.run();
        }).start();
    }

    /** Swallows every touch that misses the caption, so the app underneath stays untouched. */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP && onNext != null) onNext.run();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (caption.getVisibility() == GONE) return;
        // The caption is laid out by hand below, so it is measured by hand too: as wide as the
        // screen allows up to a comfortable reading width, and no taller than the overlay.
        int width = Math.min(getMeasuredWidth() - 2 * captionMargin, maxCaptionWidth);
        caption.measure(
                MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        scrim.layout(0, 0, getWidth(), getHeight());
        if (caption.getVisibility() != GONE) layoutCaption();
    }

    private void layoutCaption() {
        int width = caption.getMeasuredWidth();
        int height = caption.getMeasuredHeight();
        int x = (getWidth() - width) / 2;

        int insetTop = 0;
        int insetBottom = 0;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(this);
        if (insets != null) {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            insetTop = bars.top;
            insetBottom = bars.bottom;
        }

        RectF bounds = scrim.targetBounds();
        int y;
        if (bounds == null) {
            // Nothing is highlighted, so the screen below shows through untouched and the caption
            // sits at its foot, clear of the row of icons the screen keeps down there.
            y = getHeight() - insetBottom - undimmedBottomGap - height;
        } else {
            int roomBelow = getHeight() - insetBottom - captionMargin - (int) bounds.bottom - captionMargin;
            int roomAbove = (int) bounds.top - captionMargin - insetTop - captionMargin;
            boolean above;
            if (captionPlacement == ShowcaseStep.CAPTION_ABOVE) above = true;
            else if (captionPlacement == ShowcaseStep.CAPTION_BELOW) above = false;
            else if (roomBelow >= height) above = false;
            else if (roomAbove >= height) above = true;
            else above = roomAbove > roomBelow; // neither fits, so take the larger gap

            y = above
                    ? (int) bounds.top - captionMargin - height
                    : (int) bounds.bottom + captionMargin;
            y = Math.max(insetTop + captionMargin, Math.min(y, getHeight() - insetBottom - captionMargin - height));
        }
        caption.layout(x, y, x + width, y + height);
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    /**
     * Paints the scrim and erases the holes out of it. Kept apart from the overlay so the software
     * layer {@link PorterDuff.Mode#CLEAR} requires covers nothing but this.
     */
    private final class ScrimView extends View {

        private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final List<Hole> from = new ArrayList<>();
        private final List<Hole> to = new ArrayList<>();
        private final List<Hole> drawn = new ArrayList<>();
        private final int scrimColor;
        private final int ringColor;
        private final float ringWidth;
        private final float cornerRadius;

        private float fromScrim;
        private float toScrim;
        private float progress = 1f;
        @Nullable
        private ValueAnimator animator;

        ScrimView(Context context) {
            super(context);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
            holePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            scrimColor = ContextCompat.getColor(context, R.color.showcase_scrim);
            ringColor = ContextCompat.getColor(context, R.color.showcase_highlight_ring);
            ringWidth = dp(2.5f);
            cornerRadius = dp(18f);

            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(ringWidth);
            // A wider, fainter stroke just outside the crisp one, so the highlight glows into the
            // scrim instead of ending on a hard edge.
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(ringWidth * 3f);
        }

        void animateTo(List<Hole> holes, boolean dimmed) {
            if (animator != null) animator.cancel();
            from.clear();
            from.addAll(drawn.isEmpty() ? to : drawn);
            fromScrim = currentScrim();
            to.clear();
            to.addAll(holes);
            toScrim = dimmed ? 1f : 0f;

            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(TRANSITION_MS);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                progress = (float) animation.getAnimatedValue();
                rebuild();
                invalidate();
            });
            progress = 0f;
            rebuild();
            animator.start();
        }

        /** The holes as they should look right now, i.e. part way between the two steps. */
        private void rebuild() {
            drawn.clear();
            if (progress >= 1f) {
                drawn.addAll(to);
            } else if (from.size() == to.size() && !to.isEmpty()) {
                for (int i = 0; i < to.size(); i++) drawn.add(lerp(from.get(i), to.get(i), progress));
            } else {
                // The step changed how many holes there are, so the old ones shrink away while the
                // new ones grow in.
                for (Hole hole : from) addScaled(hole, 1f - progress);
                for (Hole hole : to) addScaled(hole, progress);
            }
        }

        private void addScaled(Hole hole, float factor) {
            if (factor <= 0.02f) return;
            float cx = hole.rect.centerX();
            float cy = hole.rect.centerY();
            float halfWidth = hole.rect.width() / 2f * factor;
            float halfHeight = hole.rect.height() / 2f * factor;
            drawn.add(new Hole(
                    new RectF(cx - halfWidth, cy - halfHeight, cx + halfWidth, cy + halfHeight),
                    hole.circular));
        }

        private Hole lerp(Hole start, Hole end, float t) {
            return new Hole(new RectF(
                    start.rect.left + (end.rect.left - start.rect.left) * t,
                    start.rect.top + (end.rect.top - start.rect.top) * t,
                    start.rect.right + (end.rect.right - start.rect.right) * t,
                    start.rect.bottom + (end.rect.bottom - start.rect.bottom) * t),
                    t < 0.5f ? start.circular : end.circular);
        }

        private float currentScrim() {
            return fromScrim + (toScrim - fromScrim) * progress;
        }

        /** The rectangle the caption has to keep clear of, or null while nothing is highlighted. */
        @Nullable
        RectF targetBounds() {
            if (to.isEmpty()) return null;
            RectF bounds = new RectF(to.get(0).rect);
            for (int i = 1; i < to.size(); i++) bounds.union(to.get(i).rect);
            return bounds;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float visibility = currentScrim();
            if (visibility <= 0f) return;

            int alpha = Math.round(Color.alpha(scrimColor) * visibility);
            if (alpha > 0) {
                int save = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), null);
                canvas.drawColor(ColorUtils.setAlphaComponent(scrimColor, alpha));
                for (Hole hole : drawn) {
                    float radius = hole.radius(cornerRadius);
                    canvas.drawRoundRect(hole.rect, radius, radius, holePaint);
                }
                canvas.restoreToCount(save);
            }
            // Outside the layer above, so the ring is painted on rather than erased by it.
            for (Hole hole : drawn) drawRing(canvas, hole, visibility);
        }

        /** Traces the edge of a hole in white, so the highlighted control reads as picked out. */
        private void drawRing(Canvas canvas, Hole hole, float visibility) {
            float radius = hole.radius(cornerRadius);

            RectF glow = new RectF(hole.rect);
            glow.inset(-ringWidth * 2f, -ringWidth * 2f);
            glowPaint.setColor(ColorUtils.setAlphaComponent(
                    ringColor, Math.round(60 * visibility)));
            canvas.drawRoundRect(glow, radius + ringWidth * 2f, radius + ringWidth * 2f, glowPaint);

            // Half the stroke would otherwise fall inside the hole and clip the target's own edge.
            RectF ring = new RectF(hole.rect);
            ring.inset(-ringWidth / 2f, -ringWidth / 2f);
            ringPaint.setColor(ColorUtils.setAlphaComponent(
                    ringColor, Math.round(Color.alpha(ringColor) * visibility)));
            canvas.drawRoundRect(ring, radius + ringWidth / 2f, radius + ringWidth / 2f, ringPaint);
        }
    }
}