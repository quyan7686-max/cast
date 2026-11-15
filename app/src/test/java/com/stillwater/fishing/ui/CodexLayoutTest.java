package com.stillwater.fishing.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class CodexLayoutTest {
    @Test public void bottomShowsLastSixteenOfEightyFour() {
        float scroll = CodexLayout.clampScroll(100_000, 84);
        assertEquals(2040, scroll, 0);
        assertEquals(68, CodexLayout.firstVisible(scroll));
        assertEquals(84, CodexLayout.endVisible(scroll, 84));
        assertEquals(83, CodexLayout.hitTest(1000, 600, scroll, 84));
    }

    @Test public void partialRowsUseSameCoordinatesForDrawingAndTouch() {
        float scroll = 45;
        assertEquals(129, CodexLayout.cellY(0, scroll), 0);
        assertEquals(20, CodexLayout.endVisible(scroll, 84));
        assertEquals(0, CodexLayout.hitTest(80, 175, scroll, 84));
        assertEquals(4, CodexLayout.hitTest(80, 250, scroll, 84));
        assertEquals(-1, CodexLayout.hitTest(80, 240, scroll, 84));
        assertEquals(-1, CodexLayout.hitTest(350, 175, scroll, 84));
    }

    @Test public void clippedCellsAndEmptySlotsCannotBeClicked() {
        assertEquals(-1, CodexLayout.hitTest(80, 173, 45, 84));
        assertEquals(-1, CodexLayout.hitTest(80, 654, 45, 84));
        assertEquals(-1, CodexLayout.hitTest(80, 180, 0, 0));
        assertEquals(-1, CodexLayout.hitTest(1000, 180, 0, 2));
    }

    @Test public void filtersClampScrollBeforeDrawingAndTouch() {
        assertEquals(0, CodexLayout.clampScroll(2040, 2), 0);
        assertEquals(0, CodexLayout.clampScroll(2040, 0), 0);
        assertEquals(120, CodexLayout.clampScroll(2040, 17), 0);
        assertEquals(0, CodexLayout.hitTest(80, 180, 2040, 2));
    }
}
