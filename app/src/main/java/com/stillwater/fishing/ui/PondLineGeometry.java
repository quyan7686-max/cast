package com.stillwater.fishing.ui;

import com.stillwater.fishing.engine.FishingEngine.State;

/** Scene-space rig geometry: one water contact, independent of actor pose changes. */
final class PondLineGeometry {
    // Project the seated character's forward direction onto the visible water plane.
    // Artwork calibration: foot contact (400,444), forward reach (192,104).
    // A positive Y reach puts the float in front of the actor, not level with the dock.
    static final float SEAT_X = 400, SEAT_Y = 444;
    static final float WATER_X = SEAT_X + 192, WATER_Y = SEAT_Y + 104;
    static final float FLOAT_DRAFT = 4, EYE_ABOVE_BODY = 16;
    static final double RELEASE_AT = CastMotion.RELEASE, LAND_AT = CastMotion.LAND;
    private static final int[] BOB = {0, 4, 0, -4};
    private final float launchX, launchY, retrieveX, retrieveY;
    private State previousState = State.IDLE;
    private float reelStartX, reelStartY;
    private boolean reelHasFloat;
    float x, y;
    boolean visible, onWater;

    PondLineGeometry(float releaseTipX, float releaseTipY, float restTipX, float restTipY) {
        launchX = releaseTipX;
        launchY = releaseTipY + EYE_ABOVE_BODY;
        retrieveX = restTipX;
        retrieveY = restTipY + EYE_ABOVE_BODY;
    }

    void update(State state, double time, double castAge, double reelAge) {
        if (state == State.IDLE && previousState != State.IDLE) {
            reelStartX = x;
            reelStartY = y;
            reelHasFloat = visible;
        }
        previousState = state;
        visible = false;
        onWater = false;
        if (state == State.IDLE) {
            if (!reelHasFloat || reelAge < 0 || reelAge >= AnglerAnimation.REEL_SECONDS) return;
            float t = progress(reelAge / AnglerAnimation.REEL_SECONDS);
            visible = true;
            x = pixel(lerp(reelStartX, retrieveX, t));
            y = pixel(lerp(reelStartY, retrieveY, t) - 40 * (float)Math.sin(t * Math.PI));
            return;
        }
        if (state == State.WAITING && castAge < RELEASE_AT) return;
        visible = true;
        if (state == State.WAITING && castAge < LAND_AT) {
            // The launch point is the release pose, not whichever pose is drawn now.
            float t = progress((castAge - RELEASE_AT) / (LAND_AT - RELEASE_AT));
            x = pixel(lerp(launchX, WATER_X, t));
            y = pixel(lerp(launchY, WATER_Y - FLOAT_DRAFT, t) - 56 * (float)Math.sin(t * Math.PI));
        } else {
            onWater = true;
            x = WATER_X;
            y = WATER_Y - FLOAT_DRAFT + (state == State.BITING ? 8 : BOB[AnglerAnimation.loop(time, 4, 4)]);
        }
    }
    float eyeX() { return x; }
    float eyeY() { return y - EYE_ABOVE_BODY; }
    private static float progress(double value) { return (float)Math.max(0, Math.min(1, value)); }
    private static float lerp(float from, float to, float t) { return from + (to - from) * t; }
    private static float pixel(float value) { return Math.round(value / 4) * 4; }
}
