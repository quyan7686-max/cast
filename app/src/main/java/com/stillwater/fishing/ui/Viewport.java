package com.stillwater.fishing.ui;

public final class Viewport {
    public static final float LOGICAL_WIDTH = 1280;
    public static final float LOGICAL_HEIGHT = 720;

    private float scale = 1;
    private float offsetX;
    private float offsetY;

    public void update(float width, float height) {
        scale = Math.min(width / LOGICAL_WIDTH, height / LOGICAL_HEIGHT);
        offsetX = (width - LOGICAL_WIDTH * scale) / 2;
        offsetY = (height - LOGICAL_HEIGHT * scale) / 2;
    }

    public float getScale() {
        return scale;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public float toLogicalX(float screenX) {
        return (screenX - offsetX) / scale;
    }

    public float toLogicalY(float screenY) {
        return (screenY - offsetY) / scale;
    }
}
