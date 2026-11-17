package com.stillwater.fishing.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class ViewportTest {
    @Test public void logicalSizeHasNoScaleOrLetterbox() {
        Viewport viewport = new Viewport();
        viewport.update(1280, 720);
        assertEquals(1, viewport.getScale(), 0);
        assertEquals(0, viewport.getOffsetX(), 0);
        assertEquals(0, viewport.getOffsetY(), 0);
        assertEquals(640, viewport.toLogicalX(640), 0);
        assertEquals(360, viewport.toLogicalY(360), 0);
    }

    @Test public void wideScreenCentersContentAndInvertsTouch() {
        Viewport viewport = new Viewport();
        viewport.update(2560, 1080);
        assertEquals(1.5f, viewport.getScale(), 0);
        assertEquals(320, viewport.getOffsetX(), 0);
        assertEquals(0, viewport.getOffsetY(), 0);
        assertEquals(0, viewport.toLogicalX(320), 0);
        assertEquals(1280, viewport.toLogicalX(2240), 0);
        assertEquals(640, viewport.toLogicalX(1280), 0);
        assertEquals(360, viewport.toLogicalY(540), 0);
    }

    @Test public void tallScreenCentersContentVertically() {
        Viewport viewport = new Viewport();
        viewport.update(1280, 1440);
        assertEquals(1, viewport.getScale(), 0);
        assertEquals(0, viewport.getOffsetX(), 0);
        assertEquals(360, viewport.getOffsetY(), 0);
        assertEquals(0, viewport.toLogicalY(360), 0);
        assertEquals(720, viewport.toLogicalY(1080), 0);
    }

    @Test public void uniformUpscaleMapsBothCorners() {
        Viewport viewport = new Viewport();
        viewport.update(1920, 1080);
        assertEquals(1.5f, viewport.getScale(), 0);
        assertEquals(0, viewport.getOffsetX(), 0);
        assertEquals(0, viewport.getOffsetY(), 0);
        assertEquals(0, viewport.toLogicalX(0), 0);
        assertEquals(0, viewport.toLogicalY(0), 0);
        assertEquals(1280, viewport.toLogicalX(1920), 0);
        assertEquals(720, viewport.toLogicalY(1080), 0);
    }

    @Test public void onlyTallWindowsAskForRotation() {
        assertTrue("Phone portrait", Viewport.tooNarrow(1080, 2400));
        assertTrue("Tablet portrait", Viewport.tooNarrow(1600, 2560));
        assertTrue("Narrow split screen", Viewport.tooNarrow(1080, 1400));
        assertFalse("Phone landscape", Viewport.tooNarrow(2400, 1080));
        assertFalse("Square window", Viewport.tooNarrow(1200, 1200));
        assertFalse("Unfolded foldable", Viewport.tooNarrow(2208, 1840));
        assertFalse("Thirteen by nine", Viewport.tooNarrow(1280, 720));
    }
}
