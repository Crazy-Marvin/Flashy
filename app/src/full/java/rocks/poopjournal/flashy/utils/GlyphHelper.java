package rocks.poopjournal.flashy.utils;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;

import com.nothing.ketchum.Common;
import com.nothing.ketchum.Glyph;
import com.nothing.ketchum.GlyphException;
import com.nothing.ketchum.GlyphFrame;
import com.nothing.ketchum.GlyphManager;
import com.nothing.ketchum.GlyphMatrixManager;

/**
 * Mirrors the light the app is giving onto the Glyph lights on the back of a Nothing phone: the
 * Glyph lights up whenever the flashlight or the screen light does, as bright as the ring in the
 * app is turned up.
 *
 * <p>Nothing has two different Glyphs and one SDK for both of them. Phone (3) and its 25x25 matrix
 * are driven through {@link GlyphMatrixManager} with a brightness per pixel, while the LED strips
 * of the older phones are driven through {@link GlyphManager} with a brightness per channel. Every
 * other phone gets an instance that quietly does nothing.
 */
public final class GlyphHelper {

    public static final String PREFERENCE_KEY = "glyph_interface";

    private static final String TAG = "GlyphHelper";
    /** Brightest value a single pixel of the Glyph Matrix takes. */
    private static final int MATRIX_MAX_BRIGHTNESS = 255;
    /** Brightest value a single channel of a Glyph strip takes. */
    private static final int STRIP_MAX_BRIGHTNESS = 4095;

    private static GlyphHelper instance;

    private final Context context;
    /** The {@code Glyph.DEVICE_*} name of this phone, or null on anything that is not a Nothing. */
    private final String device;
    /** Side of the square matrix, 0 on the phones that have strips instead. */
    private final int matrixSide;
    /** Number of strip channels, 0 on the phones that have a matrix instead. */
    private final int channelCount;

    private GlyphMatrixManager matrixManager;
    private GlyphManager stripManager;
    private boolean connected;
    /**
     * Level to show, 0 to 100. Kept while the service connects, since the app can well have its
     * light on before the Glyph is ready to show anything.
     */
    private int level;

    private GlyphHelper(Context context) {
        this.context = context.getApplicationContext();
        this.device = detectDevice();
        this.matrixSide = matrixSideOf(device);
        this.channelCount = channelCountOf(device);
    }

    public static synchronized GlyphHelper getInstance(Context context) {
        if (instance == null) instance = new GlyphHelper(context);
        return instance;
    }

    /** True on the Nothing phones whose Glyph this app knows how to drive. */
    public static boolean isSupported() {
        return detectDevice() != null;
    }

    /**
     * Takes hold of the Glyph. The connection is asynchronous, so nothing lights up before the
     * system hands the Glyph over; whatever level was set meanwhile is shown as soon as it does.
     */
    public synchronized void connect() {
        if (device == null || matrixManager != null || stripManager != null) return;
        try {
            if (matrixSide > 0) connectMatrix();
            else connectStrip();
        } catch (Throwable t) {
            // A Glyph that cannot be reached is not worth taking the app down for.
            Log.w(TAG, "Could not connect to the Glyph service", t);
        }
    }

    /** Turns the Glyph off and hands it back, so other apps and the system can use it again. */
    public synchronized void disconnect() {
        level = 0;
        connected = false;
        try {
            if (matrixManager != null) {
                matrixManager.turnOff();
                matrixManager.unInit();
            }
            if (stripManager != null) {
                stripManager.turnOff();
                stripManager.closeSession();
                stripManager.unInit();
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not release the Glyph", t);
        } finally {
            matrixManager = null;
            stripManager = null;
        }
    }

    /**
     * Lights the Glyph at {@code level} percent of its brightness, 0 turning it off. Safe to call
     * from any thread, including the one blinking the flashlight for SOS and the stroboscope.
     */
    public synchronized void setLevel(int level) {
        this.level = Math.max(0, Math.min(100, level));
        if (!connected) return;
        try {
            if (matrixManager != null) showOnMatrix(this.level);
            else if (stripManager != null) showOnStrip(this.level);
        } catch (Throwable t) {
            Log.w(TAG, "Could not update the Glyph", t);
        }
    }

    private void connectMatrix() {
        matrixManager = GlyphMatrixManager.getInstance(context);
        matrixManager.init(new GlyphMatrixManager.Callback() {
            @Override
            public void onServiceConnected(ComponentName componentName) {
                synchronized (GlyphHelper.this) {
                    if (matrixManager == null) return;
                    connected = matrixManager.register(device);
                    if (connected) setLevel(level);
                    else Log.w(TAG, "The Glyph Matrix refused to register " + device);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                synchronized (GlyphHelper.this) {
                    connected = false;
                }
            }
        });
    }

    private void connectStrip() {
        stripManager = GlyphManager.getInstance(context);
        stripManager.init(new GlyphManager.Callback() {
            @Override
            public void onServiceConnected(ComponentName componentName) {
                synchronized (GlyphHelper.this) {
                    if (stripManager == null) return;
                    // Without a key from Nothing in the manifest this is where the older phones
                    // say no, and the Glyph is left alone.
                    connected = stripManager.register(device);
                    if (!connected) {
                        Log.w(TAG, "The Glyph refused to register " + device
                                + ", is a NothingKey set in the manifest?");
                        return;
                    }
                    try {
                        stripManager.openSession();
                    } catch (GlyphException e) {
                        connected = false;
                        Log.w(TAG, "Could not open a Glyph session", e);
                        return;
                    }
                    setLevel(level);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                synchronized (GlyphHelper.this) {
                    connected = false;
                }
            }
        });
    }

    private void showOnMatrix(int level) throws GlyphException {
        if (level == 0) {
            matrixManager.turnOff();
            return;
        }
        int[] frame = new int[matrixSide * matrixSide];
        int brightness = Math.max(1, Math.round(level / 100f * MATRIX_MAX_BRIGHTNESS));
        Arrays.fill(frame, brightness);
        // The app is the one in the foreground here, which is what the app frame is for; older
        // builds of the Glyph service only know the plain one.
        try {
            matrixManager.setAppMatrixFrame(frame);
        } catch (Throwable t) {
            matrixManager.setMatrixFrame(frame);
        }
    }

    private void showOnStrip(int level) {
        if (level == 0) {
            stripManager.turnOff();
            return;
        }
        int brightness = Math.max(1, Math.round(level / 100f * STRIP_MAX_BRIGHTNESS));
        GlyphFrame.Builder builder = stripManager.getGlyphFrameBuilder();
        for (int channel = 0; channel < channelCount; channel++) {
            builder.buildChannel(channel, brightness);
        }
        stripManager.toggle(builder.build());
    }

    /**
     * The Glyph SDK recognises its phones by their model name, so this also answers whether the
     * phone in hand is a Nothing at all.
     */
    private static String detectDevice() {
        try {
            if (Common.is23112()) return Glyph.DEVICE_23112;
            if (Common.is25111p()) return Glyph.DEVICE_25111p;
            if (Common.is20111()) return Glyph.DEVICE_20111;
            if (Common.is22111()) return Glyph.DEVICE_22111;
            if (Common.is23111()) return Glyph.DEVICE_23111;
            if (Common.is23113()) return Glyph.DEVICE_23113;
            if (Common.is24111()) return Glyph.DEVICE_24111;
            if (Common.is25111()) return Glyph.DEVICE_25111;
            if (Common.is25131()) return Glyph.DEVICE_25131;
        } catch (Throwable t) {
            Log.w(TAG, "Could not ask the Glyph SDK about this device", t);
        }
        return null;
    }

    /** Matrix phones, by the side of the square of pixels they carry. */
    private static int matrixSideOf(String device) {
        if (Glyph.DEVICE_23112.equals(device)) return Glyph.DEVICE_23112_MATRIX_LENGTH;
        if (Glyph.DEVICE_25111p.equals(device)) return Glyph.DEVICE_25111p_MATRIX_LENGTH;
        return 0;
    }

    /**
     * Strip phones, by the number of channels they light separately. The SDK keeps these to
     * itself, so they are repeated here.
     */
    private static int channelCountOf(String device) {
        if (Glyph.DEVICE_20111.equals(device)) return 15;
        if (Glyph.DEVICE_22111.equals(device)) return 33;
        if (Glyph.DEVICE_23111.equals(device) || Glyph.DEVICE_23113.equals(device)) return 26;
        if (Glyph.DEVICE_24111.equals(device)) return 36;
        if (Glyph.DEVICE_25111.equals(device)) return 6;
        if (Glyph.DEVICE_25131.equals(device)) return 4;
        return 0;
    }
}