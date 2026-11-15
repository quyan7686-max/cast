package com.stillwater.fishing.ui;

/** Shared geometry for drawing and hit testing the continuously scrolling codex. */
final class CodexLayout {
    static final int COLUMNS = 4;
    static final int ROW_HEIGHT = 120;
    static final int COLUMN_WIDTH = 294;
    static final int CELL_WIDTH = 276;
    static final int CELL_HEIGHT = 104;
    static final int LEFT = 67, TOP = 174, RIGHT = 1225, HEIGHT = 480;

    private CodexLayout() {}

    static float clampScroll(float scroll, int count) {
        int rows = (count + COLUMNS - 1) / COLUMNS;
        return Math.max(0, Math.min(scroll, Math.max(0, rows * ROW_HEIGHT - HEIGHT)));
    }

    static int firstVisible(float scroll) {
        return (int) (scroll / ROW_HEIGHT) * COLUMNS;
    }

    static int endVisible(float scroll, int count) {
        return Math.min(count, (int) Math.ceil((scroll + HEIGHT) / ROW_HEIGHT) * COLUMNS);
    }

    static float cellY(int index, float scroll) {
        return TOP + (index / COLUMNS) * ROW_HEIGHT - scroll;
    }

    static boolean inViewport(float x, float y) {
        return x >= LEFT && x < RIGHT && y >= TOP && y < TOP + HEIGHT;
    }

    static int hitTest(float x, float y, float scroll, int count) {
        if (!inViewport(x, y)) return -1;
        float localX = x - LEFT;
        float localY = y - TOP + clampScroll(scroll, count);
        if (localX % COLUMN_WIDTH >= CELL_WIDTH || localY % ROW_HEIGHT >= CELL_HEIGHT) return -1;
        int index = (int) (localY / ROW_HEIGHT) * COLUMNS + (int) (localX / COLUMN_WIDTH);
        return index < count ? index : -1;
    }
}
