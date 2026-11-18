package com.stillwater.fishing.ui;

import com.stillwater.fishing.engine.FishingEngine;

/** Atlas timeline; seconds come exclusively from the pausable fishing animation clock. */
final class AnglerAnimation {
    static final double REEL_SECONDS = .40;
    static int frame(FishingEngine.State state, double time, double castAge, double biteAge, double reelAge) {
        if (state == FishingEngine.State.BITING) return 8 + loop(biteAge, 8, 4);
        int castStep = CastMotion.step(castAge);
        if (state == FishingEngine.State.WAITING && castStep >= 0)
            return CastMotion.FRAMES[castStep];
        if (state == FishingEngine.State.IDLE && reelAge >= 0 && reelAge < REEL_SECONDS)
            return 12 + Math.min(3, (int) (reelAge / .10));
        // A brief blink, not a metronomic open/closed toggle.
        double phase = Math.max(0, time) % 2.6;
        if (phase < 1.0) return 0;
        if (phase < 1.5) return 1;
        if (phase < 2.2) return 0;
        if (phase < 2.3) return 2;
        return 3;
    }
    static int loop(double age, int fps, int count) {
        return (int) (Math.floor(Math.max(0, age) * fps) % count);
    }
}
