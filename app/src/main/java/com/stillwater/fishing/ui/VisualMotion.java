package com.stillwater.fishing.ui;

/** Animation clocks are independent of fishing deadlines and freeze across lifecycle pauses. */
final class VisualMotion {
    private long previousNanos;
    private boolean initialized;
    double uiSeconds;
    double waterSeconds;
    double castAt = -100, biteAt = -100, catchAt = -100;

    void frame(long now, boolean visible, boolean fishingPaused) {
        if (!initialized) {
            initialized = true;
            previousNanos = now;
        }
        double delta = visible ? Math.max(0, Math.min(.05, (now - previousNanos) / 1_000_000_000.0)) : 0;
        previousNanos = now;
        uiSeconds += delta;
        if (!fishingPaused) waterSeconds += delta;
    }

    void resetFrame(long now) {
        previousNanos = now;
        initialized = true;
    }

    void cast() { castAt = waterSeconds; }
    void bite() { biteAt = waterSeconds; }
    void caught() { catchAt = waterSeconds; }

    static float progress(double now, double start, double duration) {
        return (float) Math.max(0, Math.min(1, (now - start) / duration));
    }

    static float easeOut(float progress) {
        float p = 1 - Math.max(0, Math.min(1, progress));
        return 1 - p * p * p;
    }

    static float envelope(float progress, float fadeIn, float fadeOut) {
        return Math.max(0, Math.min(1, Math.min(progress / fadeIn, (1 - progress) / fadeOut)));
    }
}
