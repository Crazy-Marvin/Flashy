package rocks.poopjournal.flashy.utils;

import android.content.Context;

/**
 * Drop-in replacement for the Glyph support of the {@code full} flavour.
 *
 * <p>The {@code fdroid} flavour is built without the proprietary Nothing Glyph SDK, which is neither
 * Free Software nor published to a public Maven repository, so it cannot be shipped by F-Droid.
 * Nothing here ever talks to the Glyph: {@link #isSupported()} is always {@code false}, the
 * preference that would switch the Glyph on stays hidden, and every other call is a no-op.
 *
 * <p>The public API is kept identical to
 * {@code app/src/full/java/rocks/poopjournal/flashy/utils/GlyphHelper.java} so that the rest of the
 * app, which is shared by both flavours, compiles against either of them.
 */
public final class GlyphHelper {

    public static final String PREFERENCE_KEY = "glyph_interface";

    private static GlyphHelper instance;

    private GlyphHelper(Context context) {
    }

    public static synchronized GlyphHelper getInstance(Context context) {
        if (instance == null) instance = new GlyphHelper(context);
        return instance;
    }

    /** Never true in this flavour: without the SDK no Nothing phone is recognised. */
    public static boolean isSupported() {
        return false;
    }

    public synchronized void connect() {
    }

    public synchronized void disconnect() {
    }

    public synchronized void setLevel(int level) {
    }
}
