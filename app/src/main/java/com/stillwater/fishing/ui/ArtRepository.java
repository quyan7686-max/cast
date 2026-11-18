package com.stillwater.fishing.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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
    static final float FLOAT_X = 720, FLOAT_Y = 536, WATERLINE = 544;
    private final AssetManager assets;
    private final PondArt pond;
    private final Map<String, Bitmap> bitmaps = new HashMap<>();
    private final Path fishingLine = new Path();
    private final String[] sceneNames = new String[6];
    private final String[][] specimenNames = new String[com.stillwater.fishing.data.GameProgress.ENTRY_COUNT / 2][2];
    private final PorterDuffColorFilter silhouetteFilter = new PorterDuffColorFilter(
            Color.rgb(29, 49, 54), PorterDuff.Mode.SRC_IN);
    private final Rect source = new Rect();
    private final RectF destination = new RectF();

    public ArtRepository(Context context) {
        assets = context.getAssets();
        pond = new PondArt(context);
        for (int i = 0; i < sceneNames.length; i++) sceneNames[i] = "scene_" + i;
        for (int i = 0; i < specimenNames.length; i++) {
            String base = String.format(Locale.US, "species_%02d", i);
            specimenNames[i][0] = base;
            specimenNames[i][1] = base + "_s";
        }
        // Decode action artwork before the first cast, keeping feedback frames lightweight.
        bitmap("angler_idle");
        bitmap("angler_bite");
        bitmap("hook");
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

    public void drawScene(Canvas canvas, Paint paint, int scene, int period, boolean raining, double time) {
        prepare(paint);
        if (scene == 0) { pond.background(canvas, paint, period, raining, time); return; }
        destination.set(0, 0, 1280, 720);
        canvas.drawBitmap(bitmap(sceneNames[scene]), null, destination, paint);
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
        drawWaterHighlights(canvas, paint, period, time);
        drawAmbient(canvas, paint, scene, period, time);
        if (raining) drawRain(canvas, paint, time);
    }

    private void drawStars(Canvas canvas, Paint paint, double time) {
        for (int i = 0; i < 24; i++) {
            float x = 25 + (i * 173 + 41) % 1210;
            float y = 18 + (i * 67 + 29) % 210;
            int alpha = 65 + (int) (100 * (.5 + .5 * Math.sin(time * 1.3 + i * 2.1)));
            paint.setColor(Color.argb(alpha, 238, 228, 184));
            canvas.drawRect(x, y, x + 3, y + 3, paint);
        }
    }

    private void drawWaterHighlights(Canvas canvas, Paint paint, int period, double time) {
        // Keep shimmer on open water, clear of the bank, piers and harbor boat.
        for (int i = 0; i < 24; i++) {
            double phase = time * .45 + i * 2.37;
            float x = 400 + (i * 113) % 810 + (float) Math.sin(phase) * 9;
            float y = 586 + (i * 31) % 118;
            float width = 12 + (i % 4) * 7 + (float) Math.sin(phase) * 6;
            int alpha = (period == 3 ? 12 : 18) + (int) (24 * (.5 + .5 * Math.sin(phase)));
            paint.setColor(Color.argb(alpha, 224, 240, 218));
            canvas.drawRect(Math.round(x / 2) * 2, y, Math.round((x + width) / 2) * 2, y + 2, paint);
        }
    }

    private void drawAmbient(Canvas canvas, Paint paint, int scene, int period, double time) {
        if (period == 0) {
            // Thin dawn mist drifts horizontally without hiding the float.
            for (int i = 0; i < 6; i++) {
                double phase = time * .16 + i * 1.7;
                float x = 360 + (float) Math.sin(phase) * 55 + i * 107;
                float y = 430 + i * 29;
                paint.setColor(Color.argb(9 + i % 3 * 3, 238, 226, 198));
                canvas.drawRect(x, y, x + 115 + i * 13, y + 5, paint);
            }
        } else if (period == 2) {
            // Broken warm reflections are confined to open water.
            for (int i = 0; i < 10; i++) {
                double phase = time * .7 + i * 1.9;
                float x = 730 + (i * 67) % 410 + (float) Math.sin(phase) * 12;
                float y = 470 + i * 19;
                paint.setColor(Color.argb(18 + (int) (20 * (.5 + .5 * Math.sin(phase))), 247, 190, 112));
                canvas.drawRect(x, y, x + 24 + (i % 3) * 11, y + 3, paint);
            }
        } else if (period == 3 && scene <= 3) {
            // Freshwater fireflies use sparse, slow pulses rather than constant flashing.
            for (int i = 0; i < 11; i++) {
                double phase = time * .65 + i * 2.3;
                float x = 335 + (i * 109) % 790 + (float) Math.sin(phase) * 13;
                float y = 290 + (i * 47) % 210 + (float) Math.cos(phase * .8) * 9;
                int alpha = (int) (75 * Math.max(0, Math.sin(phase)));
                paint.setColor(Color.argb(alpha, 244, 211, 119));
                canvas.drawRect(x - 2, y - 2, x + 2, y + 2, paint);
            }
        }
        if (scene == 5) {
            for (int i = 0; i < 7; i++) {
                double phase = time * .55 + i * .8;
                float x = 430 + i * 112 + (float) Math.sin(phase) * 16;
                float y = 565 + (i % 3) * 35;
                paint.setColor(Color.argb(22 + (int) (20 * (.5 + .5 * Math.sin(phase))), 224, 239, 218));
                canvas.drawRect(x, y, x + 45, y + 3, paint);
            }
        }
    }

    private void drawRain(Canvas canvas, Paint paint, double time) {
        for (int i = 0; i < 72; i++) {
            boolean near = i % 3 == 0;
            double speed = near ? 380 : 250;
            float y = (float) ((i * 97 + time * speed) % 800) - 40;
            float x = (float) ((i * 173 + 1440 - (time * speed * .28) % 1440) % 1440) - 80;
            paint.setColor(Color.argb(near ? 105 : 48, 194, 220, 220));
            paint.setStrokeWidth(near ? 2 : 1);
            canvas.drawLine(x, y, x - 6, y + (near ? 24 : 15), paint);
        }
        for (int i = 0; i < 12; i++) {
            float phase = (float) ((time * .85 + i * .173) % 1);
            ripple(canvas, paint, 410 + (i * 173) % 800, 590 + (i * 31) % 110, phase, 17, 65);
        }
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.FILL);
    }

    private void ripple(Canvas canvas, Paint paint, float x, float y, float phase, float radius, int alpha) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.argb((int) (alpha * (1 - phase)), 218, 239, 218));
        float r = 5 + phase * radius;
        canvas.drawOval(x - r, y - r * .22f, x + r, y + r * .22f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    public void drawAngler(Canvas canvas, Paint paint, int scene, FishingEngine.State state, double time,
                           double castAge, double biteAge, double catchAge, double reelAge) {
        prepare(paint);
        if (scene == 0) { pond.actor(canvas, paint, state, time, castAge, biteAge, catchAge, reelAge, this); return; }
        boolean biting = state == FishingEngine.State.BITING;
        boolean casting = state == FishingEngine.State.WAITING && castAge < .62;
        float cast = VisualMotion.progress(castAge, 0, .62);
        float reel = VisualMotion.progress(catchAge, 0, .5);
        float angle = casting ? -9 * (float) Math.sin(cast * Math.PI * 2)
                : catchAge < .5 ? -7 * (float) Math.sin(reel * Math.PI) : 0;
        float breathe = (float) Math.sin(time * 1.7) * 1.1f;
        canvas.save();
        canvas.translate(0, breathe);
        canvas.rotate(angle, 222, 380);
        destination.set(124, 220, 320, 420);
        canvas.drawBitmap(bitmap(biting || catchAge < .3 ? "angler_bite" : "angler_idle"), null, destination, paint);
        canvas.restore();
        if (state != FishingEngine.State.IDLE) {
            float bob = biting ? 12 + (float) Math.sin(time * 17) * 2 : (float) Math.sin(time * 2.8) * 2;
            float x = casting ? 318 + (FLOAT_X - 318) * cast : FLOAT_X;
            float y = casting ? 338 + (FLOAT_Y - 338) * cast - 100 * (float) Math.sin(cast * Math.PI) : FLOAT_Y + bob;
            double radians = Math.toRadians(angle);
            // Follow the actual line endpoint in each sprite, including the body's rotation.
            float localX = biting ? 44 : 96, localY = biting ? 18 : -42;
            float tipX = 222 + (float) (localX * Math.cos(radians) - localY * Math.sin(radians));
            float tipY = 380 + (float) (localX * Math.sin(radians) + localY * Math.cos(radians)) + breathe;
            paint.setColor(Color.argb(190, 231, 231, 205));
            paint.setStrokeWidth(1.5f);
            paint.setStyle(Paint.Style.STROKE);
            fishingLine.reset();
            fishingLine.moveTo(tipX, tipY);
            fishingLine.quadTo((tipX + x) / 2, (tipY + y) / 2 + (biting ? 3 : 20), x, y - 12);
            canvas.drawPath(fishingLine, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF243E40);
            canvas.drawRect(x - 5, y - 12, x + 5, y + 8, paint);
            canvas.drawRect(x - 1, y - 18, x + 1, y - 12, paint);
            paint.setColor(0xFFE7A552);
            canvas.drawRect(x - 3, y - 10, x + 3, y - 2, paint);
            paint.setColor(0xFFEDE9CE);
            canvas.drawRect(x - 3, y - 2, x + 3, y + 5, paint);
            if (!casting) {
                ripple(canvas, paint, FLOAT_X, WATERLINE, (float) ((time * .65) % 1), 27, 85);
                ripple(canvas, paint, FLOAT_X, WATERLINE, (float) ((time * .65 + .5) % 1), 27, 65);
            }
            if (biting) {
                splash(canvas, paint, FLOAT_X, WATERLINE, VisualMotion.progress(biteAge, 0, .65), false);
                // A small pixel exclamation follows the float without obscuring the line.
                float lift = (float) Math.sin(Math.min(biteAge / .25, 1) * Math.PI) * 8;
                paint.setColor(0xFF263B3C);
                canvas.drawRect(FLOAT_X + 24, FLOAT_Y - 64 - lift, FLOAT_X + 40, FLOAT_Y - 30 - lift, paint);
                paint.setColor(0xFFFFD477);
                canvas.drawRect(FLOAT_X + 29, FLOAT_Y - 60 - lift, FLOAT_X + 35, FLOAT_Y - 44 - lift, paint);
                canvas.drawRect(FLOAT_X + 29, FLOAT_Y - 39 - lift, FLOAT_X + 35, FLOAT_Y - 33 - lift, paint);
            }
            if (castAge >= .62 && castAge < 1.22) {
                splash(canvas, paint, FLOAT_X, WATERLINE, VisualMotion.progress(castAge, .62, .6), false);
            }
        }
        if (catchAge < .65) splash(canvas, paint, FLOAT_X, WATERLINE, VisualMotion.progress(catchAge, 0, .65), true);
        paint.setStrokeWidth(1);
    }

    public void splash(Canvas canvas, Paint paint, float x, float y, float phase, boolean gold) {
        if (phase >= 1) return;
        ripple(canvas, paint, x, y, phase, 48, 160);
        for (int i = 0; i < 8; i++) {
            float dx = (i - 3.5f) * 12 * phase;
            float dy = -(28 + (i % 3) * 12) * (float) Math.sin(phase * Math.PI) + phase * 8;
            paint.setColor(Color.argb((int) ((1 - phase) * 210), gold ? 247 : 213, gold ? 208 : 235, gold ? 119 : 224));
            canvas.drawRect(x + dx, y + dy, x + dx + 4, y + dy + 4, paint);
        }
    }

    public void drawSparkles(Canvas canvas, Paint paint, float x, float y, float radius, double time) {
        prepare(paint);
        for (int i = 0; i < 5; i++) {
            double phase = time * 1.8 + i * 1.7;
            float brightness = (float) Math.max(0, Math.sin(phase));
            float px = x + (float) Math.cos(i * 2.4) * radius;
            float py = y + (float) Math.sin(i * 2.4) * radius * .7f;
            paint.setColor(Color.argb((int) (brightness * 200), 246, 207, 115));
            canvas.drawRect(px - 5, py - 1, px + 5, py + 1, paint);
            canvas.drawRect(px - 1, py - 5, px + 1, py + 5, paint);
        }
        paint.setAlpha(255);
    }

    public void drawSpecimen(Canvas canvas, Paint paint, Species species, boolean shiny,
                             boolean silhouette, float centerX, float centerY, float scale) {
        prepare(paint);
        String name = specimenNames[species.id][!silhouette && shiny ? 1 : 0];
        float size = 96 * scale;
        destination.set(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2);
        if (silhouette) paint.setColorFilter(silhouetteFilter);
        canvas.drawBitmap(bitmap(name), null, destination, paint);
        paint.setColorFilter(null);
    }

    public void drawSceneThumbnail(Canvas canvas, Paint paint, int scene, RectF target) {
        prepare(paint);
        if (scene == 0) { pond.thumbnail(canvas, paint, target); return; }
        float targetAspect = target.width() / target.height();
        if (targetAspect > 320f / 180f) {
            int height = Math.round(320 / targetAspect);
            source.set(0, (180 - height) / 2, 320, (180 + height) / 2);
        } else {
            int width = Math.round(180 * targetAspect);
            source.set((320 - width) / 2, 0, (320 + width) / 2, 180);
        }
        canvas.drawBitmap(bitmap(sceneNames[scene]), source, target, paint);
    }

    public void drawPanel(Canvas canvas, Paint paint, RectF target, String style) {
        prepare(paint);
        boolean dark = style.equals("dark"), gold = style.equals("gold");
        float l = target.left, t = target.top, r = target.right, b = target.bottom;
        float unit = Math.min(4, Math.min(target.width(), target.height()) / 10);
        // Stepped corners and solid color bands share the scene's pixel scale.
        paint.setColor(0xFF102832);
        canvas.drawRect(l + unit, t, r - unit, b, paint);
        canvas.drawRect(l, t + unit, r, b - unit, paint);
        paint.setColor(dark ? 0xFFE5BA68 : 0xFF987047);
        canvas.drawRect(l + unit, t + unit, r - unit, b - unit, paint);
        paint.setColor(dark ? 0xFF193544 : gold ? 0xFFE9BD6B : 0xFFF0DEB4);
        canvas.drawRect(l + unit * 2, t + unit * 2, r - unit * 2, b - unit * 2, paint);
        paint.setColor(dark ? 0xFF294654 : gold ? 0xFFFFDB91 : 0xFFFFEDC8);
        canvas.drawRect(l + unit * 3, t + unit * 2, r - unit * 3, t + unit * 3, paint);
        paint.setColor(dark ? 0xFF102A37 : gold ? 0xFFBC8949 : 0xFFD2B98C);
        canvas.drawRect(l + unit * 3, b - unit * 3, r - unit * 3, b - unit * 2, paint);
    }
}
