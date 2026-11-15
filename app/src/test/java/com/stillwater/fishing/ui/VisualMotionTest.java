package com.stillwater.fishing.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class VisualMotionTest {
    @Test public void openingPanelFreezesFishingEffectsButContinuesUiAnimation() {
        VisualMotion motion = new VisualMotion();
        motion.frame(0, true, false);
        motion.frame(20_000_000, true, false);
        motion.cast();
        motion.frame(40_000_000, true, true);
        assertEquals(.04, motion.uiSeconds, .000001);
        assertEquals(.02, motion.waterSeconds, .000001);
        assertEquals(0, motion.waterSeconds - motion.castAt, .000001);
    }

    @Test public void resumeDoesNotFastForwardAnimationsThroughBackgroundTime() {
        VisualMotion motion = new VisualMotion();
        motion.frame(0, true, false);
        motion.frame(20_000_000, true, false);
        motion.frame(3_600_000_000_000L, false, true);
        motion.resetFrame(3_600_020_000_000L);
        motion.frame(3_600_040_000_000L, true, false);
        assertEquals(.04, motion.uiSeconds, .000001);
        assertEquals(.04, motion.waterSeconds, .000001);
    }

    @Test public void renderingStallCannotSkipEntireCastAnimation() {
        VisualMotion motion = new VisualMotion();
        motion.frame(0, true, false);
        motion.cast();
        motion.frame(2_000_000_000L, true, false);
        assertEquals(.05, motion.waterSeconds, .000001);
        assertTrue(VisualMotion.progress(motion.waterSeconds, motion.castAt, .62) < .1);
    }
}
