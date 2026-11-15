package com.stillwater.fishing.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.stillwater.fishing.data.Species;
import com.stillwater.fishing.engine.FishingEngine;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ArtRepository {
    private final AssetManager assets;
    private final Map<String, Bitmap> bitmaps = new HashMap<>();
    private final Rect source = new Rect();
    private final RectF destination = new RectF();

    public ArtRepository(Context context) {
        assets = context.getAssets();
    }

    private Bitmap bitmap(String name) {
        Bitmap cached = bitmaps.get(name);
        if (cached != null) return cached;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream input = assets.open("art/" + name + ".png")) {
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            bitmaps.put(name, bitmap);
            return bitmap;
        } catch (IOException exception) {
            throw new IllegalStateException("Missing bundled artwork: " + name, exception);
        }
    }

    private void prepare(Paint paint) {
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
        paint.setShader(null);
        paint.setColorFilter(null);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
    }

    public void drawScene(Canvas canvas, Paint paint, int scene, int period, boolean raining, float time) {
        prepare(paint);
        destination.set(0, 0, 1280, 720);
        canvas.drawBitmap(bitmap("scene_" + scene), null, destination, paint);
        if (period == 0) {
            paint.setColor(Color.argb(25, 255, 181, 125));
        } else if (period == 2) {
            paint.setColor(Color.argb(55, 116, 64, 105));
        } else if (period == 3) {
            paint.setColor(Color.argb(125, 10, 25, 54));
        } else {
            paint.setColor(Color.argb(8, 238, 244, 218));
        }
        canvas.drawRect(destination, paint);
        if (period == 3) drawStars(canvas, paint, time);
        drawWaterHighlights(canvas, paint, time);
        if (raining) drawRain(canvas, paint, time);
    }

    private void drawStars(Canvas canvas, Paint paint, float time) {
        paint.setColor(Color.argb(185, 238, 228, 184));
        for (int i = 0; i < 30; i++) {
            float x = 25 + (i * 173 + 41) % 1210;
            float y = 18 + (i * 67 + 29) % 245;
            if ((i + (int) (time * 2)) % 8 != 0) canvas.drawRect(x, y, x + 3, y + 3, paint);
        }
    }

    private void drawWaterHighlights(Canvas canvas, Paint paint, float time) {
        paint.setColor(Color.argb(36, 224, 240, 218));
        int shift = ((int) (time * 8) % 24) / 4 * 4;
        for (int i = 0; i < 18; i++) {
            float x = (i * 83 + shift) % 1240;
            float y = 434 + (i * 47) % 246;
            canvas.drawRect(x, y, x + 22 + (i % 3) * 9, y + 3, paint);
        }
    }

    private void drawRain(Canvas canvas, Paint paint, float time) {
        paint.setColor(Color.argb(105, 194, 220, 220));
        paint.setStrokeWidth(2);
        int shift = ((int) (time * 240) % 80);
        for (int i = 0; i < 72; i++) {
            float x = (i * 97 + shift * 3) % 1320 - 20;
            float y = (i * 61 + shift * 5) % 760 - 20;
            canvas.drawLine(x, y, x - 8, y + 23, paint);
        }
        paint.setStrokeWidth(1);
    }

    public void drawAngler(Canvas canvas, Paint paint, FishingEngine.State state, float time) {
        prepare(paint);
        String name = state == FishingEngine.State.BITING ? "angler_bite" : "angler_idle";
        destination.set(124, 220, 320, 420);
        canvas.drawBitmap(bitmap(name), null, destination, paint);
        if (state != FishingEngine.State.IDLE) {
            float bob = state == FishingEngine.State.BITING ? 14 : (float) Math.sin(time * 3) * 2;
            paint.setColor(Color.argb(210, 231, 231, 205));
            paint.setStrokeWidth(2);
            canvas.drawLine(316, 260, 552, 452 + bob, paint);
            destination.set(544, 448 + bob, 576, 480 + bob);
            canvas.drawBitmap(bitmap("hook"), null, destination, paint);
            if (state == FishingEngine.State.BITING) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.argb(160, 230, 239, 215));
                paint.setStrokeWidth(3);
                canvas.drawOval(new RectF(518, 483, 600, 498), paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        paint.setStrokeWidth(1);
    }

    public void drawSpecimen(Canvas canvas, Paint paint, Species species, boolean shiny,
                             boolean silhouette, float centerX, float centerY, float scale) {
        prepare(paint);
        String suffix = silhouette ? "_l" : shiny ? "_s" : "";
        String name = String.format(Locale.US, "species_%02d%s", species.id, suffix);
        float size = 96 * scale;
        destination.set(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2);
        canvas.drawBitmap(bitmap(name), null, destination, paint);
    }

    public void drawSceneThumbnail(Canvas canvas, Paint paint, int scene, RectF target) {
        prepare(paint);
        float targetAspect = target.width() / target.height();
        if (targetAspect > 320f / 180f) {
            int height = Math.round(320 / targetAspect);
            source.set(0, (180 - height) / 2, 320, (180 + height) / 2);
        } else {
            int width = Math.round(180 * targetAspect);
            source.set((320 - width) / 2, 0, (320 + width) / 2, 180);
        }
        canvas.drawBitmap(bitmap("scene_" + scene), source, target, paint);
    }

    public void drawPanel(Canvas canvas, Paint paint, RectF target, String style) {
        prepare(paint);
        Bitmap bitmap = bitmap("ui_" + style);
        float border = Math.min(18, Math.min(target.width(), target.height()) / 4);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int sx = column * 16, sy = row * 16;
                source.set(sx, sy, sx + 16, sy + 16);
                float left = column == 0 ? target.left : column == 1 ? target.left + border : target.right - border;
                float right = column == 0 ? target.left + border : column == 1 ? target.right - border : target.right;
                float top = row == 0 ? target.top : row == 1 ? target.top + border : target.bottom - border;
                float bottom = row == 0 ? target.top + border : row == 1 ? target.bottom - border : target.bottom;
                destination.set(left, top, right, bottom);
                canvas.drawBitmap(bitmap, source, destination, paint);
            }
        }
    }
}
