package com.stillwater.fishing.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.stillwater.fishing.data.Species;
import com.stillwater.fishing.engine.FishingEngine;

/** Pixel primitives for the logical 1280 x 720 game canvas. */
public final class PixelArt {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final Path PATH = new Path();

    private static final int[] SKY_TOP = {
            0xFF789DA4, 0xFF79ADB8, 0xFF625D79, 0xFF172D42
    };
    private static final int[] SKY_HAZE = {
            0xFFF0C99B, 0xFFD5E1D2, 0xFFE69A74, 0xFF405467
    };
    private static final int[] WATER_TOP = {
            0xFF71A39C, 0xFF56979A, 0xFF657F84, 0xFF244353
    };
    private static final int[] WATER_DEEP = {
            0xFF2B6267, 0xFF275E67, 0xFF334F5C, 0xFF142F40
    };
    private static final int[] LAND_FAR = {
            0xFF719183, 0xFF628573, 0xFF6E7372, 0xFF29474A
    };
    private static final int[] LAND_NEAR = {
            0xFF496F5D, 0xFF3E6B55, 0xFF4D6256, 0xFF1D3938
    };

    private PixelArt() {
    }

    public static void drawScene(Canvas c, Paint p, int scene, int period,
                                 boolean raining, float time) {
        scene = clamp(scene, 0, 5);
        period = clamp(period, 0, 3);
        prepare(p);
        drawSky(c, p, period, time);
        drawBackdrop(c, p, scene, period);
        int horizon = horizon(scene);
        drawWater(c, p, scene, period, horizon, time);
        switch (scene) {
            case 0:
                drawPond(c, p, period, time);
                break;
            case 1:
                drawForestRiver(c, p, period, time);
                break;
            case 2:
                drawOpenLake(c, p, period, time);
                break;
            case 3:
                drawMountainStream(c, p, period, time);
                break;
            case 4:
                drawHarbor(c, p, period, time);
                break;
            default:
                drawRockyCoast(c, p, period, time);
                break;
        }
        if (raining) {
            drawRain(c, p, horizon, time);
        }
        p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(1f);
    }

    public static void drawAngler(Canvas c, Paint p, FishingEngine.State state, float time) {
        prepare(p);
        int frame = (int) (time * 8f);
        boolean biting = state == FishingEngine.State.BITING;
        boolean waiting = state == FishingEngine.State.WAITING;
        int shake = biting ? ((frame & 1) == 0 ? -2 : 2) : 0;
        int breathe = waiting ? ((frame / 4) & 1) : 0;

        fill(c, p, 0x55323F38, 218, 438, 338, 450);
        fill(c, p, 0x442E3C38, 236, 434, 316, 454);

        float gripX = biting ? 309 : 302;
        float gripY = biting ? 350 : 362;
        float tipX = biting ? 523 + shake : waiting ? 510 : 459;
        float tipY = biting ? 299 + shake : waiting ? 248 : 306;
        float middleX = biting ? 432 : waiting ? 418 : 390;
        float middleY = biting ? 270 + shake : waiting ? 303 : 331;

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStrokeJoin(Paint.Join.MITER);
        p.setColor(0xFF263E3D);
        p.setStrokeWidth(6f);
        c.drawLine(gripX, gripY, middleX, middleY, p);
        c.drawLine(middleX, middleY, tipX, tipY, p);
        p.setColor(0xFFB88A52);
        p.setStrokeWidth(2f);
        c.drawLine(gripX, gripY - 1, middleX, middleY - 1, p);
        c.drawLine(middleX, middleY - 1, tipX, tipY - 1, p);

        float bobberX = biting ? 548 : waiting ? 542 : 474;
        float bobberY = biting ? 542 + shake : waiting ? 510 + ((frame / 3) & 1) * 3 : 437;
        p.setColor(0xFFBDCCC0);
        p.setStrokeWidth(2f);
        if (biting) {
            c.drawLine(tipX, tipY, tipX + 13, 388, p);
            c.drawLine(tipX + 13, 388, bobberX, bobberY, p);
        } else if (waiting) {
            c.drawLine(tipX, tipY, bobberX, bobberY - 8, p);
        } else {
            c.drawLine(tipX, tipY, 466, 362, p);
            c.drawLine(466, 362, bobberX, bobberY - 7, p);
        }
        p.setStyle(Paint.Style.FILL);

        // Boots and trouser legs, deliberately stepped rather than rounded.
        fill(c, p, 0xFF263B3B, 250, 387, 276, 421);
        fill(c, p, 0xFF263B3B, 258, 416, 278, 438);
        fill(c, p, 0xFF182E32, 263, 432, 305, 444);
        fill(c, p, 0xFF182E32, 294, 438, 311, 447);
        fill(c, p, 0xFF345252, 274, 390, 296, 418);
        fill(c, p, 0xFF345252, 285, 413, 307, 434);
        fill(c, p, 0xFF22383B, 302, 430, 338, 442);
        fill(c, p, 0xFF22383B, 328, 437, 345, 447);
        fill(c, p, 0xFF52706A, 282, 394, 290, 411);

        int lean = biting ? 5 : 0;
        // Jacket shadow, main cloth and warm vest panels.
        quad(c, p, 0xFF243F40,
                239 + lean, 348, 274 + lean, 338, 298 + lean, 363, 287, 401);
        quad(c, p, 0xFF243F40, 287, 401, 239, 401, 232, 380, 239 + lean, 348);
        quad(c, p, 0xFF47756A,
                245 + lean, 350, 272 + lean, 344, 289 + lean, 364, 281, 394);
        quad(c, p, 0xFF47756A, 281, 394, 244, 394, 239, 378, 245 + lean, 350);
        fill(c, p, 0xFF6E9380, 248 + lean, 351, 256 + lean, 374);
        quad(c, p, 0xFFC29A5B, 261 + lean, 347, 274 + lean, 346, 279 + lean, 391, 266, 391);
        fill(c, p, 0xFFE0BB73, 266 + lean, 355, 271 + lean, 360);
        fill(c, p, 0xFF294B48, 251, 378, 265, 391);
        fill(c, p, 0xFF89A58A, 254, 380, 262, 383);

        // Neck, profile, nose and hair.
        fill(c, p, 0xFF8B6249, 254 + lean, 329, 274 + lean, 350);
        fill(c, p, 0xFFDFAE78, 263 + lean, 330, 277 + lean, 346);
        fill(c, p, 0xFF3B3B35, 241 + lean, 304, 253 + lean, 331);
        fill(c, p, 0xFFD5A16E, 247 + lean, 303, 281 + lean, 333);
        fill(c, p, 0xFFE4B87E, 258 + lean, 306, 283 + lean, 328);
        fill(c, p, 0xFFE4B87E, 280 + lean, 317, 290 + lean, 325);
        fill(c, p, 0xFFBC825C, 275 + lean, 327, 284 + lean, 333);
        fill(c, p, 0xFF273C3B, 278 + lean, 313, 283 + lean, 318);
        fill(c, p, 0xFF6B4638, 281 + lean, 327, 286 + lean, 330);

        // A broad pixel cap with a warm patch and a right-facing brim.
        fill(c, p, 0xFF263F42, 238 + lean, 286, 248 + lean, 305);
        fill(c, p, 0xFF263F42, 244 + lean, 278, 273 + lean, 305);
        fill(c, p, 0xFF263F42, 273 + lean, 285, 285 + lean, 306);
        fill(c, p, 0xFF3F6B64, 245 + lean, 283, 273 + lean, 300);
        fill(c, p, 0xFF3F6B64, 250 + lean, 279, 267 + lean, 284);
        fill(c, p, 0xFF2D504F, 237 + lean, 299, 282 + lean, 308);
        fill(c, p, 0xFF2D504F, 277 + lean, 303, 304 + lean, 310);
        fill(c, p, 0xFFD5A65C, 260 + lean, 287, 269 + lean, 295);
        fill(c, p, 0xFF92A88A, 248 + lean, 284, 255 + lean, 287);

        // Arms change height and angle on a bite.
        if (biting) {
            quad(c, p, 0xFF315952, 277, 351, 287, 344, 307, 347, 306, 359);
            fill(c, p, 0xFFDFAE78, 302, 345, 315, 355);
            quad(c, p, 0xFF3F6B60, 246, 355, 255, 350, 289, 362, 284, 371);
            fill(c, p, 0xFFD2A06B, 284, 357, 305, 367);
        } else {
            quad(c, p, 0xFF315952, 277, 352, 288, 354, 306, 361, 301, 372);
            fill(c, p, 0xFFDFAE78, 297, 358, 310, 369);
            quad(c, p, 0xFF3F6B60, 245, 354, 255, 354, 288, 373, 282, 382);
            fill(c, p, 0xFFD2A06B, 281, 369, 301, 378);
        }
        fill(c, p, 0xFF6E4836, 297, 359 + (biting ? -10 : 0), 306, 372 + (biting ? -10 : 0));
        fill(c, p, 0xFF87A18A, 244, 389, 250, 393 + breathe);

        // Float and water response remain independent of interface overlays.
        if (waiting || biting) {
            fill(c, p, 0xFF243F43, bobberX - 3, bobberY - 9, bobberX + 3, bobberY + 7);
            fill(c, p, 0xFFE2B85F, bobberX - 4, bobberY - 7, bobberX + 4, bobberY - 1);
            fill(c, p, 0xFFD5DDD0, bobberX - 4, bobberY - 1, bobberX + 4, bobberY + 4);
            fill(c, p, 0xFF88B4AA, bobberX - 14, bobberY + 7, bobberX + 15, bobberY + 10);
        }
        if (biting) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3f);
            p.setColor(0xFFC0D7C5);
            c.drawLine(bobberX - 27, bobberY + 3, bobberX - 12, bobberY - 2, p);
            c.drawLine(bobberX + 13, bobberY - 3, bobberX + 30, bobberY + 2, p);
            c.drawLine(bobberX - 8, bobberY - 18, bobberX - 13, bobberY - 28, p);
            c.drawLine(bobberX + 9, bobberY - 17, bobberX + 15, bobberY - 25, p);
            p.setStyle(Paint.Style.FILL);
        }
        p.setStrokeWidth(1f);
    }

    public static void drawSpecimen(Canvas c, Paint p, Species species, boolean shiny,
                                    boolean silhouette, float cx, float cy, float scale) {
        prepare(p);
        String name = species.name;
        int main = naturalColor(name, species.junk);
        int accent = naturalAccent(name);
        int light = mix(main, 0xFFFFF0CE, 0.44f);
        int dark = mix(main, 0xFF15343B, 0.58f);
        if (shiny) {
            main = 0xFFD8B45E;
            light = 0xFFF7E7A9;
            dark = 0xFF225C61;
            accent = 0xFF58B6A9;
        }
        if (silhouette) {
            main = 0xFF203B3E;
            light = main;
            dark = main;
            accent = main;
        }

        c.save();
        c.translate(cx, cy);
        c.scale(scale, scale);
        if (species.junk) {
            switch (name) {
                case "空罐头":
                    drawCan(c, p, main, light, dark, accent);
                    break;
                case "木棍":
                    drawStick(c, p, main, light, dark, accent);
                    break;
                case "落叶":
                    drawLeaf(c, p, main, light, dark, accent);
                    break;
                case "破陶罐":
                    drawPot(c, p, main, light, dark, accent);
                    break;
                case "漂流瓶":
                    drawBottle(c, p, main, light, dark, accent);
                    break;
                case "硬币":
                    drawCoin(c, p, main, light, dark, accent);
                    break;
                case "小石头":
                    drawStone(c, p, main, light, dark, accent);
                    break;
                default:
                    drawPlasticBag(c, p, main, light, dark, accent);
                    break;
            }
        } else if ("虾".equals(name)) {
            drawShrimp(c, p, main, light, dark, accent);
        } else if ("螃蟹".equals(name)) {
            drawCrab(c, p, main, light, dark, accent);
        } else if ("章鱼".equals(name)) {
            drawOctopus(c, p, main, light, dark, accent);
        } else if ("河蚌".equals(name) || "蛤蜊".equals(name)) {
            drawShell(c, p, "河蚌".equals(name), main, light, dark, accent);
        } else if ("泥鳅".equals(name) || "黄鳝".equals(name)
                || "鳗鱼".equals(name) || "带鱼".equals(name)) {
            drawEel(c, p, name, main, light, dark, accent);
        } else if ("河豚".equals(name)) {
            drawPuffer(c, p, main, light, dark, accent);
        } else if ("比目鱼".equals(name)) {
            drawFlatfish(c, p, main, light, dark, accent);
        } else {
            drawFish(c, p, name, main, light, dark, accent);
        }
        if (shiny && !silhouette) {
            sparkle(c, p, -51, -29, 0xFFF4D77D);
            sparkle(c, p, 51, -24, 0xFF78C9B4);
            sparkle(c, p, 47, 29, 0xFFF4D77D);
        }
        c.restore();
        p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(1f);
    }

    private static void prepare(Paint p) {
        p.setAntiAlias(false);
        p.setDither(false);
        p.setShader(null);
        p.setAlpha(255);
        p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(1f);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStrokeJoin(Paint.Join.MITER);
    }

    private static void drawSky(Canvas c, Paint p, int period, float time) {
        for (int i = 0; i < 18; i++) {
            int color = mix(SKY_TOP[period], SKY_HAZE[period], i / 17f);
            fill(c, p, color, 0, i * 24, WIDTH, (i + 1) * 24);
        }
        fill(c, p, SKY_HAZE[period], 0, 432, WIDTH, HEIGHT);

        if (period == 3) {
            for (int i = 0; i < 30; i++) {
                int x = 22 + (i * 167 + 71) % 1230;
                int y = 18 + (i * 71 + 37) % 205;
                if ((i + (int) (time * 2f)) % 9 != 0) {
                    int size = i % 6 == 0 ? 4 : 2;
                    fill(c, p, i % 4 == 0 ? 0xFFE7C88C : 0xFFCBD8CC,
                            x, y, x + size, y + size);
                    if (size == 4) {
                        fill(c, p, 0xFFCBD8CC, x - 3, y + 1, x + 7, y + 3);
                    }
                }
            }
            pixelEllipse(c, p, 0xFFDDD9B7, 1015, 103, 33, 33);
            pixelEllipse(c, p, 0xFFF1E5BC, 1008, 96, 24, 24);
            pixelEllipse(c, p, SKY_TOP[period], 1025, 87, 23, 25);
            fill(c, p, 0xFFB9BBA7, 991, 111, 998, 116);
        } else {
            int sunX = period == 0 ? 890 : period == 1 ? 1010 : 1050;
            int sunY = period == 0 ? 175 : period == 1 ? 99 : 229;
            int sun = period == 2 ? 0xFFF2B56F : 0xFFF2D38A;
            int halo = period == 2 ? 0x55EFAF78 : 0x44F3D9A0;
            pixelEllipse(c, p, halo, sunX, sunY, 52, 52);
            pixelEllipse(c, p, sun, sunX, sunY, 34, 34);
            fill(c, p, mix(sun, 0xFFFFF0C0, 0.45f), sunX - 18, sunY - 22, sunX + 15, sunY - 14);
        }

        int cloud = period == 3 ? 0xFF314958 : period == 2 ? 0xFFC68E82 : 0xFFE1DFC9;
        int cloudShade = period == 3 ? 0xFF293F50 : period == 2 ? 0xFFAB7B78 : 0xFFC4D1C3;
        int drift = ((int) (time * 2f) % 32) / 4 * 4;
        drawCloud(c, p, 150 + drift, 105, cloud, cloudShade, 1);
        drawCloud(c, p, 620 - drift / 2, 160, cloud, cloudShade, 2);
        drawCloud(c, p, 1090 + drift / 3, 66, cloud, cloudShade, 1);
    }

    private static void drawCloud(Canvas c, Paint p, int x, int y,
                                  int color, int shade, int size) {
        int u = size == 2 ? 12 : 10;
        fill(c, p, shade, x, y + u, x + u * 12, y + u * 4);
        fill(c, p, color, x + u, y, x + u * 5, y + u * 3);
        fill(c, p, color, x + u * 4, y - u, x + u * 8, y + u * 3);
        fill(c, p, color, x + u * 8, y + u, x + u * 11, y + u * 3);
        fill(c, p, mix(color, 0xFFFFF1D4, 0.18f), x + u * 2, y, x + u * 5, y + u);
    }

    private static void drawBackdrop(Canvas c, Paint p, int scene, int period) {
        int far = LAND_FAR[period];
        int near = LAND_NEAR[period];
        int haze = mix(far, SKY_HAZE[period], 0.35f);
        switch (scene) {
            case 0:
                quad(c, p, haze, 0, 260, 260, 218, 560, 282, 760, 318);
                quad(c, p, haze, 760, 318, 560, 282, 0, 318, 0, 260);
                quad(c, p, far, 420, 304, 720, 232, 1010, 272, 1280, 248);
                quad(c, p, far, 1280, 330, 420, 330, 420, 304, 1280, 248);
                fill(c, p, near, 0, 302, WIDTH, 326);
                for (int i = 0; i < 13; i++) {
                    int x = 35 + i * 99;
                    drawDeciduous(c, p, x, 306 + (i % 3) * 4, 56 + (i % 4) * 8,
                            mix(near, far, 0.35f), false);
                }
                drawWillow(c, p, 1135, 318, period);
                break;
            case 1:
                triangle(c, p, haze, 30, 305, 338, 119, 670, 305);
                triangle(c, p, mix(haze, SKY_HAZE[period], 0.18f), 420, 305, 775, 145, 1100, 305);
                triangle(c, p, haze, 830, 305, 1110, 167, 1370, 305);
                fill(c, p, far, 0, 286, WIDTH, 326);
                for (int i = 0; i < 18; i++) {
                    int x = 12 + i * 75;
                    drawPine(c, p, x, 320, 72 + (i * 19) % 63, mix(near, far, (i % 3) / 4f));
                }
                break;
            case 2:
                quad(c, p, haze, 0, 277, 260, 223, 530, 271, 760, 244);
                quad(c, p, haze, 760, 302, 0, 302, 0, 277, 760, 244);
                quad(c, p, far, 650, 289, 925, 226, 1110, 266, 1280, 230);
                quad(c, p, far, 1280, 306, 650, 306, 650, 289, 1280, 230);
                fill(c, p, mix(far, WATER_TOP[period], 0.35f), 0, 298, WIDTH, 310);
                drawBird(c, p, 655, 155, period);
                drawBird(c, p, 690, 142, period);
                drawBird(c, p, 724, 158, period);
                break;
            case 3:
                triangle(c, p, haze, -90, 320, 292, 83, 650, 320);
                triangle(c, p, mix(far, SKY_HAZE[period], 0.20f), 325, 320, 725, 57, 1120, 320);
                triangle(c, p, far, 800, 320, 1100, 117, 1390, 320);
                triangle(c, p, mix(SKY_HAZE[period], 0xFFF1E3C4, 0.45f), 604, 150, 725, 57, 840, 151);
                triangle(c, p, mix(SKY_HAZE[period], far, 0.30f), 202, 169, 292, 83, 388, 170);
                for (int i = 0; i < 12; i++) {
                    drawPine(c, p, 28 + i * 113, 326, 58 + (i % 4) * 13, near);
                }
                break;
            case 4:
                fill(c, p, haze, 0, 282, WIDTH, 319);
                int building = mix(far, 0xFF6F746E, 0.35f);
                fill(c, p, building, 620, 244, 790, 310);
                fill(c, p, building, 800, 264, 920, 310);
                fill(c, p, building, 936, 225, 1082, 310);
                fill(c, p, mix(building, 0xFF233F43, 0.25f), 603, 239, 797, 250);
                triangle(c, p, mix(building, 0xFF233F43, 0.25f), 927, 226, 1008, 190, 1090, 226);
                for (int i = 0; i < 6; i++) {
                    fill(c, p, period == 3 ? 0xFFD5AF69 : 0xFF9EB2A0,
                            646 + i * 61, 270, 662 + i * 61, 282);
                }
                drawCrane(c, p, 1135, 303, period);
                fill(c, p, near, 0, 308, WIDTH, 324);
                break;
            default:
                quad(c, p, haze, 0, 286, 165, 254, 340, 278, 480, 303);
                quad(c, p, haze, 480, 315, 0, 315, 0, 286, 480, 303);
                quad(c, p, far, 800, 304, 930, 215, 1060, 252, 1165, 183);
                quad(c, p, far, 1165, 183, 1280, 230, 1280, 320, 800, 320);
                fill(c, p, near, 1045, 280, 1280, 320);
                drawBird(c, p, 520, 133, period);
                drawBird(c, p, 558, 122, period);
                break;
        }
    }

    private static void drawWater(Canvas c, Paint p, int scene, int period,
                                  int horizon, float time) {
        int bands = 15;
        float bandHeight = (HEIGHT - horizon) / (float) bands;
        for (int i = 0; i < bands; i++) {
            int color = mix(WATER_TOP[period], WATER_DEEP[period], i / (bands - 1f));
            fill(c, p, color, 0, horizon + i * bandHeight,
                    WIDTH, horizon + (i + 1) * bandHeight + 1);
        }
        fill(c, p, mix(WATER_TOP[period], 0xFFE6D6A3, period == 2 ? 0.28f : 0.10f),
                0, horizon, WIDTH, horizon + 5);

        int reflectionX = period == 0 ? 890 : period == 1 ? 1010 : period == 2 ? 1050 : 1015;
        int reflection = period == 3 ? 0xFFB7BDA7 : period == 2 ? 0xFFE7AC72 : 0xFFE4D39B;
        for (int i = 0; i < 9; i++) {
            int y = horizon + 14 + i * 17;
            int half = 16 + i * 7;
            int shift = (i % 3 - 1) * 11;
            fill(c, p, withAlpha(reflection, 90 - i * 6), reflectionX - half + shift, y,
                    reflectionX + half + shift, y + 3);
        }

        int light = mix(WATER_TOP[period], 0xFFD6DFC4, 0.40f);
        int shade = mix(WATER_DEEP[period], 0xFF173F48, 0.25f);
        int frame = (int) (time * (scene == 3 ? 22f : 10f));
        for (int row = 0; row < 12; row++) {
            int y = horizon + 18 + row * ((HEIGHT - horizon - 25) / 12);
            int length = 16 + row * 3;
            for (int col = 0; col < 6; col++) {
                int direction = (row & 1) == 0 ? 1 : -1;
                int raw = col * 229 + row * 83 + frame * direction * (1 + row % 3);
                int x = positiveMod(raw, 1420) - 70;
                int offset = ((row + col) % 3) * 5;
                fill(c, p, (row + col) % 4 == 0 ? shade : light,
                        x, y + offset, x + length + (col % 3) * 8, y + offset + 2 + row / 7);
                if (row > 5 && (col + row) % 3 == 0) {
                    fill(c, p, light, x + 9, y + offset - 4, x + length - 5, y + offset - 2);
                }
            }
        }
    }

    private static void drawPond(Canvas c, Paint p, int period, float time) {
        int pad = mix(0xFF5F8B65, LAND_NEAR[period], period == 3 ? 0.50f : 0.15f);
        int padLight = mix(pad, 0xFFC6C78E, 0.25f);
        for (int i = 0; i < 17; i++) {
            int x = 430 + (i * 173) % 790;
            int y = 360 + (i * 89) % 300;
            int size = 13 + (i % 4) * 4;
            drawLilyPad(c, p, x, y, size, pad, padLight, WATER_DEEP[period]);
            if (i % 5 == 1) {
                drawLotus(c, p, x + 4, y - 8, period);
            }
        }
        drawReeds(c, p, 1190, 527, 105, period);
        drawReeds(c, p, 93, 584, 118, period);
        drawReeds(c, p, 365, 456, 62, period);
        drawLeftBank(c, p, 0, period);
        fill(c, p, 0xFF90734E, 5, 531, 233, 548);
        fill(c, p, 0xFF567355, 0, 523, 263, 533);
        int rippleX = 590 + ((int) (time * 7) % 16);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(0xFF9BBCAC);
        c.drawLine(rippleX - 24, 545, rippleX + 25, 545, p);
        c.drawLine(rippleX - 13, 552, rippleX + 15, 552, p);
        p.setStyle(Paint.Style.FILL);
    }

    private static void drawForestRiver(Canvas c, Paint p, int period, float time) {
        int bank = mix(0xFF536B50, LAND_NEAR[period], period == 3 ? 0.55f : 0.15f);
        quad(c, p, mix(bank, 0xFF263F39, 0.25f), 1110, 317, 1280, 294, 1280, 720, 1190, 720);
        quad(c, p, bank, 1045, 321, 1110, 317, 1190, 720, 1090, 720);
        quad(c, p, bank, 1090, 720, 1045, 321, 1010, 370, 1080, 720);
        fill(c, p, mix(bank, 0xFF9B9D68, 0.28f), 1070, 324, 1120, 334);
        drawDeciduous(c, p, 1170, 381, 150, bank, true);
        drawDeciduous(c, p, 1045, 332, 105, mix(bank, 0xFF779066, 0.25f), true);
        drawPine(c, p, 1240, 460, 196, mix(bank, 0xFF173D36, 0.30f));
        drawRock(c, p, 686, 426, 45, 22, period);
        drawRock(c, p, 757, 507, 29, 16, period);
        drawRock(c, p, 994, 374, 32, 18, period);
        int flow = (int) (time * 30) % 44;
        for (int i = 0; i < 6; i++) {
            int y = 372 + i * 43;
            int x = 560 + (i % 3) * 115 + flow;
            fill(c, p, 0xFF9EBEAE, x, y, x + 43, y + 3);
            fill(c, p, 0xFF6F9F94, x + 14, y + 6, x + 63, y + 8);
        }
        drawLeftBank(c, p, 1, period);
        drawDeciduous(c, p, 72, 409, 136, mix(bank, 0xFF284C3D, 0.20f), true);
        fill(c, p, 0xFF6E563C, 95, 480, 229, 494);
        fill(c, p, 0xFF9B7C50, 108, 477, 218, 483);
    }

    private static void drawOpenLake(Canvas c, Paint p, int period, float time) {
        int bob = ((int) (time * 3f) & 1) * 2;
        int hull = mix(0xFF715A45, WATER_DEEP[period], period == 3 ? 0.45f : 0.05f);
        quad(c, p, hull, 870, 390 + bob, 981, 390 + bob, 961, 411 + bob, 895, 411 + bob);
        fill(c, p, 0xFFD7C89D, 922, 290 + bob, 927, 390 + bob);
        triangle(c, p, 0xFFE6D6AF, 917, 302 + bob, 917, 371 + bob, 866, 369 + bob);
        triangle(c, p, 0xFFC99F69, 933, 315 + bob, 933, 374 + bob, 970, 369 + bob);
        fill(c, p, 0xFF9AB8AA, 865, 417 + bob, 987, 420 + bob);
        fill(c, p, 0xFF496F6C, 1020, 327, 1155, 335);
        fill(c, p, 0xFF5E7C64, 1060, 315, 1132, 329);
        for (int i = 0; i < 4; i++) {
            int x = 485 + i * 189;
            fill(c, p, 0xFFD5B66E, x, 461 + i * 37, x + 5, 469 + i * 37);
            fill(c, p, 0xFF8BB0A2, x - 9, 470 + i * 37, x + 15, 472 + i * 37);
        }
        drawLeftBank(c, p, 2, period);
        drawReeds(c, p, 90, 551, 76, period);
        fill(c, p, 0xFF75866A, 0, 546, 279, 558);
        int x = 745 + positiveMod((int) (time * 5), 24);
        fill(c, p, 0xFFA9C3AF, x, 566, x + 77, 569);
        fill(c, p, 0xFF69958D, x + 22, 574, x + 106, 577);
    }

    private static void drawMountainStream(Canvas c, Paint p, int period, float time) {
        int rock = mix(0xFF69766A, LAND_NEAR[period], period == 3 ? 0.45f : 0.10f);
        quad(c, p, mix(rock, 0xFF263E3B, 0.30f), 1070, 308, 1280, 281, 1280, 720, 1160, 720);
        quad(c, p, rock, 1005, 343, 1070, 308, 1160, 720, 1050, 720);
        quad(c, p, rock, 1050, 720, 1005, 343, 976, 390, 1015, 720);
        fill(c, p, mix(rock, 0xFFABB08C, 0.30f), 1050, 329, 1120, 343);
        fill(c, p, 0xFFBBD2C1, 937, 270, 962, 329);
        fill(c, p, 0xFFE0DFC2, 944, 278, 953, 329);
        fill(c, p, 0xFF91B4A7, 929, 326, 973, 334);
        drawPine(c, p, 1180, 357, 128, mix(rock, 0xFF214A3C, 0.55f));
        drawPine(c, p, 1080, 329, 89, mix(rock, 0xFF214A3C, 0.50f));
        drawRock(c, p, 580, 405, 54, 30, period);
        drawRock(c, p, 790, 487, 68, 37, period);
        drawRock(c, p, 943, 594, 54, 30, period);
        drawRock(c, p, 464, 608, 35, 21, period);
        int flow = positiveMod((int) (time * 38), 56);
        for (int i = 0; i < 8; i++) {
            int y = 352 + i * 43;
            int x = 410 + (i * 137) % 490 + flow;
            fill(c, p, 0xFFB8D0BC, x, y, x + 51, y + 4);
            fill(c, p, 0xFFE0DEC1, x + 13, y - 4, x + 32, y);
            fill(c, p, 0xFF6D9C93, x + 38, y + 7, x + 83, y + 10);
        }
        drawLeftBank(c, p, 3, period);
        drawRock(c, p, 142, 466, 77, 39, period);
        drawRock(c, p, 290, 447, 54, 26, period);
    }

    private static void drawHarbor(Canvas c, Paint p, int period, float time) {
        int bob = ((int) (time * 2.5f) & 1) * 2;
        int wood = mix(0xFF8B6E4D, WATER_DEEP[period], period == 3 ? 0.42f : 0.05f);
        int woodLight = mix(wood, 0xFFD0AA6C, 0.33f);
        // Moored harbor boat.
        quad(c, p, 0xFF273F43, 786, 419 + bob, 1010, 419 + bob, 976, 459 + bob, 827, 459 + bob);
        quad(c, p, 0xFFAF6D50, 798, 416 + bob, 999, 416 + bob, 974, 443 + bob, 828, 443 + bob);
        fill(c, p, 0xFFE0C58D, 836, 377 + bob, 936, 418 + bob);
        fill(c, p, 0xFF355D60, 851, 386 + bob, 875, 405 + bob);
        fill(c, p, 0xFF355D60, 890, 386 + bob, 916, 405 + bob);
        fill(c, p, 0xFF374F4C, 881, 305 + bob, 887, 379 + bob);
        triangle(c, p, 0xFFC7A666, 889, 316 + bob, 889, 351 + bob, 925, 348 + bob);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(0xFFBBA678);
        c.drawLine(997, 420 + bob, 1080, 433, p);
        p.setStyle(Paint.Style.FILL);
        // Right pier and its piles.
        fill(c, p, 0xFF344847, 1062, 421, 1280, 467);
        fill(c, p, wood, 1035, 414, 1280, 438);
        fill(c, p, woodLight, 1035, 414, 1280, 421);
        for (int i = 0; i < 5; i++) {
            fill(c, p, 0xFF4D4B3E, 1051 + i * 58, 434, 1067 + i * 58, 594 + i % 2 * 34);
            fill(c, p, woodLight, 1054 + i * 58, 439, 1059 + i * 58, 579);
        }
        fill(c, p, 0xFF6D5942, 1130, 372, 1202, 414);
        fill(c, p, 0xFFAA8656, 1135, 377, 1197, 409);
        fill(c, p, 0xFF5C4C3B, 1159, 377, 1165, 409);
        fill(c, p, 0xFF5C4C3B, 1135, 389, 1197, 395);
        drawDock(c, p, period, wood, woodLight);
        // Harbor lamps are scenery, not interface elements.
        fill(c, p, 0xFF293F41, 119, 282, 127, 417);
        fill(c, p, 0xFF293F41, 116, 279, 151, 286);
        fill(c, p, period == 3 || period == 2 ? 0xFFF0C477 : 0xFF9FA98E,
                137, 286, 151, 305);
        if (period >= 2) {
            pixelEllipse(c, p, 0x33F0C477, 144, 296, 28, 28);
        }
        fill(c, p, 0xFF9CB6A5, 759, 470 + bob, 1022, 473 + bob);
    }

    private static void drawRockyCoast(Canvas c, Paint p, int period, float time) {
        int stone = mix(0xFF68746C, WATER_DEEP[period], period == 3 ? 0.43f : 0.08f);
        drawSeaStack(c, p, 1010, 389, 76, 125, stone);
        drawSeaStack(c, p, 1130, 461, 105, 173, mix(stone, 0xFF293F3E, 0.22f));
        drawSeaStack(c, p, 740, 531, 48, 67, stone);
        int foamShift = positiveMod((int) (time * 13), 24);
        for (int i = 0; i < 7; i++) {
            int x = 466 + i * 113 + foamShift;
            int y = 390 + (i % 4) * 64;
            fill(c, p, 0xFFC4D3BD, x, y, x + 64, y + 4);
            fill(c, p, 0xFF8FB4A7, x + 18, y + 7, x + 91, y + 10);
            fill(c, p, 0xFFE0DCC0, x + 8, y - 4, x + 25, y);
        }
        // Left fishing ledge, layered with dark basalt and warm rock faces.
        quad(c, p, 0xFF293F40, 0, 395, 168, 395, 266, 421, 392, 449);
        quad(c, p, 0xFF293F40, 392, 449, 355, 532, 244, 720, 0, 720);
        quad(c, p, stone, 0, 406, 165, 406, 258, 430, 365, 451);
        quad(c, p, stone, 365, 451, 323, 510, 218, 720, 0, 720);
        quad(c, p, mix(stone, 0xFFB29A71, 0.20f), 0, 406, 165, 406, 254, 425, 93, 449);
        quad(c, p, mix(stone, 0xFFB29A71, 0.20f), 93, 449, 254, 425, 365, 451, 0, 454);
        fill(c, p, 0xFF89A084, 0, 395, 168, 407);
        fill(c, p, 0xFF89A084, 158, 403, 211, 414);
        triangle(c, p, 0xFF3B5150, 221, 458, 293, 467, 244, 566);
        triangle(c, p, 0xFF405957, 65, 486, 151, 462, 126, 592);
        fill(c, p, 0xFF457673, 104, 489, 182, 510);
        fill(c, p, 0xFF79A095, 116, 489, 169, 495);
        drawReeds(c, p, 44, 424, 53, period);
    }

    private static void drawLeftBank(Canvas c, Paint p, int scene, int period) {
        int soil = scene == 3 ? 0xFF59645A : scene == 2 ? 0xFF76684C : 0xFF685A42;
        soil = mix(soil, LAND_NEAR[period], period == 3 ? 0.48f : 0.10f);
        int grass = scene == 3 ? 0xFF667C60 : 0xFF66805A;
        grass = mix(grass, LAND_NEAR[period], period == 3 ? 0.48f : 0.08f);
        quad(c, p, mix(soil, 0xFF203B38, 0.35f), 0, 393, 164, 393, 252, 415, 345, 415);
        quad(c, p, mix(soil, 0xFF203B38, 0.35f), 345, 415, 401, 451, 344, 511, 282, 720);
        quad(c, p, mix(soil, 0xFF203B38, 0.35f), 282, 720, 0, 720, 0, 393, 0, 720);
        quad(c, p, soil, 0, 407, 159, 407, 247, 427, 335, 427);
        quad(c, p, soil, 335, 427, 377, 453, 325, 499, 260, 720);
        quad(c, p, soil, 260, 720, 0, 720, 0, 407, 0, 720);
        fill(c, p, grass, 0, 392, 165, 408);
        quad(c, p, grass, 158, 393, 253, 414, 345, 414, 337, 429);
        quad(c, p, grass, 337, 416, 390, 447, 378, 460, 333, 432);
        fill(c, p, mix(grass, 0xFFADB37A, 0.26f), 0, 392, 162, 398);
        fill(c, p, mix(grass, 0xFFADB37A, 0.20f), 174, 402, 246, 412);
        fill(c, p, mix(soil, 0xFFB08A5A, 0.25f), 30, 468, 168, 481);
        fill(c, p, mix(soil, 0xFF3B493D, 0.25f), 174, 532, 273, 546);
        fill(c, p, mix(soil, 0xFF3B493D, 0.20f), 25, 603, 219, 617);
        for (int i = 0; i < 6; i++) {
            fill(c, p, mix(grass, 0xFF253F36, 0.25f), 20 + i * 42, 388 - (i % 3) * 7,
                    25 + i * 42, 405);
        }
    }

    private static void drawDock(Canvas c, Paint p, int period, int wood, int woodLight) {
        fill(c, p, 0xFF263D3E, 0, 408, 344, 427);
        quad(c, p, 0xFF263D3E, 344, 408, 400, 446, 394, 469, 0, 449);
        fill(c, p, wood, 0, 414, 342, 440);
        quad(c, p, wood, 342, 414, 390, 447, 382, 457, 0, 443);
        fill(c, p, woodLight, 0, 414, 341, 420);
        for (int i = 0; i < 9; i++) {
            int x = i * 44;
            fill(c, p, mix(wood, 0xFF3A4037, 0.30f), x, 416, x + 4, 444);
            fill(c, p, 0xFFD0AD70, x + 11, 424, x + 14, 427);
        }
        fill(c, p, 0xFF4A473B, 48, 438, 67, 612);
        fill(c, p, 0xFF4A473B, 310, 437, 331, 581);
        fill(c, p, woodLight, 52, 442, 58, 589);
        fill(c, p, woodLight, 314, 443, 320, 565);
        fill(c, p, 0xFF739A8D, 28, 606, 87, 610);
        fill(c, p, 0xFF739A8D, 291, 577, 349, 581);
    }

    private static void drawRain(Canvas c, Paint p, int horizon, float time) {
        int fall = (int) (time * 280f);
        int drift = (int) (time * 115f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setStrokeWidth(2f);
        p.setColor(0x88B5CBC4);
        for (int i = 0; i < 72; i++) {
            int x = positiveMod(i * 97 + drift, 1400) - 60;
            int y = positiveMod(i * 137 + fall, 820) - 70;
            int length = 12 + (i % 4) * 4;
            c.drawLine(x, y, x - 6, y + length, p);
        }
        p.setColor(0x99C1D0C2);
        for (int i = 0; i < 18; i++) {
            int x = 380 + positiveMod(i * 149 + drift, 850);
            int y = horizon + 30 + positiveMod(i * 71 + fall / 3, HEIGHT - horizon - 60);
            c.drawLine(x - 8, y, x + 8, y, p);
            c.drawLine(x - 3, y - 4, x, y - 9, p);
            c.drawLine(x + 3, y - 4, x + 6, y - 8, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private static void drawPine(Canvas c, Paint p, float x, float base, float height, int color) {
        float trunk = Math.max(4, height * 0.07f);
        fill(c, p, mix(color, 0xFF594D38, 0.50f), x - trunk / 2, base - height * 0.38f,
                x + trunk / 2, base);
        triangle(c, p, mix(color, 0xFF173D38, 0.20f), x, base - height,
                x - height * 0.25f, base - height * 0.42f, x + height * 0.25f, base - height * 0.42f);
        triangle(c, p, color, x, base - height * 0.82f,
                x - height * 0.34f, base - height * 0.19f, x + height * 0.34f, base - height * 0.19f);
        triangle(c, p, mix(color, 0xFF9AA477, 0.10f), x - 2, base - height * 0.87f,
                x - height * 0.18f, base - height * 0.48f, x + 1, base - height * 0.52f);
        fill(c, p, mix(color, 0xFF1F3D37, 0.30f), x - height * 0.34f, base - height * 0.22f,
                x + height * 0.34f, base - height * 0.16f);
    }

    private static void drawDeciduous(Canvas c, Paint p, float x, float base, float height,
                                      int color, boolean detailed) {
        float trunk = Math.max(5, height * 0.08f);
        fill(c, p, 0xFF5B4E3B, x - trunk / 2, base - height * 0.65f, x + trunk / 2, base);
        fill(c, p, 0xFF847052, x - trunk / 2 + 2, base - height * 0.60f,
                x - trunk / 2 + 4, base - 3);
        pixelEllipse(c, p, mix(color, 0xFF173D35, 0.20f), x, base - height * 0.72f,
                height * 0.35f, height * 0.30f);
        pixelEllipse(c, p, color, x - height * 0.18f, base - height * 0.72f,
                height * 0.25f, height * 0.24f);
        pixelEllipse(c, p, mix(color, 0xFF9DAC74, 0.18f), x + height * 0.13f,
                base - height * 0.82f, height * 0.24f, height * 0.22f);
        if (detailed) {
            fill(c, p, mix(color, 0xFFBAC08A, 0.28f), x - height * 0.19f, base - height * 0.91f,
                    x - height * 0.03f, base - height * 0.86f);
            fill(c, p, mix(color, 0xFF213F35, 0.25f), x + height * 0.13f, base - height * 0.66f,
                    x + height * 0.29f, base - height * 0.58f);
            line(c, p, 0xFF5B4E3B, 2, x, base - height * 0.48f,
                    x + height * 0.19f, base - height * 0.70f);
        }
    }

    private static void drawWillow(Canvas c, Paint p, int x, int base, int period) {
        int leaf = mix(0xFF55795C, LAND_NEAR[period], period == 3 ? 0.55f : 0.10f);
        fill(c, p, 0xFF63553E, x - 8, base - 135, x + 10, base);
        pixelEllipse(c, p, leaf, x, base - 143, 73, 48);
        pixelEllipse(c, p, mix(leaf, 0xFF9AAB75, 0.20f), x - 26, base - 157, 43, 32);
        for (int i = 0; i < 7; i++) {
            int xx = x - 60 + i * 20;
            int length = 40 + (i % 3) * 15;
            fill(c, p, leaf, xx, base - 139, xx + 6, base - 139 + length);
            fill(c, p, mix(leaf, 0xFFADB47B, 0.16f), xx + 2, base - 125, xx + 4, base - 110);
        }
    }

    private static void drawBird(Canvas c, Paint p, int x, int y, int period) {
        int color = period == 3 ? 0xFF82918A : 0xFF425B58;
        line(c, p, color, 2, x - 10, y, x - 4, y - 4);
        line(c, p, color, 2, x - 4, y - 4, x, y);
        line(c, p, color, 2, x, y, x + 5, y - 5);
        line(c, p, color, 2, x + 5, y - 5, x + 11, y - 1);
    }

    private static void drawCrane(Canvas c, Paint p, int x, int base, int period) {
        int color = mix(0xFF59635C, LAND_NEAR[period], period == 3 ? 0.40f : 0.05f);
        fill(c, p, color, x - 5, base - 139, x + 5, base);
        fill(c, p, color, x - 22, base - 139, x + 96, base - 132);
        triangle(c, p, color, x - 5, base - 112, x - 62, base - 137, x - 5, base - 137);
        line(c, p, color, 3, x + 72, base - 133, x + 72, base - 61);
        fill(c, p, color, x + 65, base - 63, x + 79, base - 56);
        line(c, p, mix(color, SKY_HAZE[period], 0.25f), 2,
                x - 55, base - 134, x, base - 80);
    }

    private static void drawReeds(Canvas c, Paint p, int x, int base, int height, int period) {
        int color = mix(0xFF6F8156, LAND_NEAR[period], period == 3 ? 0.45f : 0.08f);
        for (int i = 0; i < 7; i++) {
            int xx = x + (i - 3) * 10;
            int h = height - (i * 17) % 43;
            line(c, p, color, 3, xx, base, xx + (i % 3 - 1) * 8, base - h);
            if (i % 2 == 0) {
                fill(c, p, 0xFF846A43, xx - 3 + (i % 3 - 1) * 8, base - h - 14,
                        xx + 4 + (i % 3 - 1) * 8, base - h + 3);
            }
            triangle(c, p, mix(color, 0xFFADB279, 0.16f), xx, base - h * 0.45f,
                    xx - 15, base - h * 0.62f, xx + 2, base - h * 0.53f);
        }
    }

    private static void drawLilyPad(Canvas c, Paint p, int x, int y, int radius,
                                    int color, int light, int water) {
        pixelEllipse(c, p, mix(color, 0xFF173D3B, 0.35f), x, y + 3, radius + 3, radius * 0.42f + 2);
        pixelEllipse(c, p, color, x, y, radius, radius * 0.42f);
        fill(c, p, light, x - radius * 0.55f, y - 3, x + 1, y);
        triangle(c, p, water, x + 2, y - 2, x + radius + 2, y - radius * 0.28f,
                x + radius, y + 3);
    }

    private static void drawLotus(Canvas c, Paint p, int x, int y, int period) {
        int petal = period == 3 ? 0xFFB99588 : 0xFFE1B2A0;
        triangle(c, p, petal, x, y - 18, x - 10, y + 1, x + 2, y - 1);
        triangle(c, p, mix(petal, 0xFFFFE1BD, 0.30f), x + 2, y - 22, x - 3, y, x + 10, y);
        triangle(c, p, petal, x + 12, y - 16, x + 3, y, x + 16, y + 1);
        fill(c, p, 0xFFE1BD68, x, y - 3, x + 7, y + 3);
    }

    private static void drawRock(Canvas c, Paint p, int x, int y, int rx, int ry, int period) {
        int color = mix(0xFF778076, WATER_DEEP[period], period == 3 ? 0.45f : 0.05f);
        quad(c, p, mix(color, 0xFF243D3D, 0.35f), x - rx, y + ry * 0.4f,
                x - rx * 0.55f, y - ry * 0.65f, x + rx * 0.28f, y - ry, x + rx, y + ry * 0.4f);
        quad(c, p, mix(color, 0xFF243D3D, 0.35f), x + rx, y + ry * 0.4f,
                x + rx * 0.6f, y + ry, x - rx * 0.6f, y + ry, x - rx, y + ry * 0.4f);
        quad(c, p, color, x - rx * 0.75f, y + ry * 0.25f,
                x - rx * 0.42f, y - ry * 0.48f, x + rx * 0.24f, y - ry * 0.72f, x + rx * 0.72f, y + ry * 0.25f);
        triangle(c, p, mix(color, 0xFFD0BD91, 0.22f), x - rx * 0.42f, y - ry * 0.48f,
                x + rx * 0.24f, y - ry * 0.72f, x - rx * 0.05f, y - ry * 0.15f);
        fill(c, p, 0xFF9EBDAC, x - rx - 10, y + ry, x + rx + 13, y + ry + 3);
    }

    private static void drawSeaStack(Canvas c, Paint p, int x, int base, int width, int height, int color) {
        quad(c, p, mix(color, 0xFF213C3E, 0.38f), x - width / 2f, base,
                x - width * 0.35f, base - height * 0.66f, x - width * 0.05f, base - height, x + width * 0.31f, base - height * 0.76f);
        quad(c, p, mix(color, 0xFF213C3E, 0.38f), x + width * 0.31f, base - height * 0.76f,
                x + width / 2f, base, x - width / 2f, base, x - width * 0.35f, base - height * 0.66f);
        triangle(c, p, color, x - width * 0.31f, base - 5,
                x - width * 0.22f, base - height * 0.61f, x + width * 0.13f, base - height * 0.72f);
        triangle(c, p, mix(color, 0xFFC2AD84, 0.18f), x - width * 0.22f, base - height * 0.61f,
                x - width * 0.05f, base - height * 0.91f, x + width * 0.13f, base - height * 0.72f);
        fill(c, p, 0xFF66806A, x - width * 0.11f, base - height, x + width * 0.12f, base - height + 8);
        fill(c, p, 0xFFC2D2BB, x - width * 0.7f, base, x + width * 0.72f, base + 4);
    }

    private static int naturalColor(String name, boolean junk) {
        if (junk) {
            switch (name) {
                case "空罐头": return 0xFF899590;
                case "木棍": return 0xFF8B6844;
                case "落叶": return 0xFFB17B43;
                case "破陶罐": return 0xFFA96F52;
                case "漂流瓶": return 0xFF5B9B8C;
                case "硬币": return 0xFFD0A84F;
                case "小石头": return 0xFF7C8580;
                default: return 0xFFA8B5A5;
            }
        }
        switch (name) {
            case "鲫鱼": case "鳊鱼": case "鲢鱼": case "白条鱼": case "银鱼":
                return 0xFFA8B9AA;
            case "鲤鱼": case "黄鱼": case "黄颡鱼":
                return 0xFFC09A55;
            case "草鱼": case "青鱼": case "黑鱼":
                return 0xFF5F8070;
            case "罗非鱼": case "鳜鱼": case "石斑鱼":
                return 0xFF7D8660;
            case "泥鳅": case "鲶鱼":
                return 0xFF82775A;
            case "黄鳝":
                return 0xFFB68B4E;
            case "鳗鱼":
                return 0xFF526C62;
            case "虾":
                return 0xFFD49779;
            case "螃蟹":
                return 0xFFB97858;
            case "青蟹":
                return 0xFF66816A;
            case "鳟鱼": case "马口鱼":
                return 0xFF7FA28A;
            case "鲈鱼": case "鲻鱼": case "沙丁鱼": case "鲭鱼":
                return 0xFF749B9B;
            case "带鱼":
                return 0xFFB9C4B5;
            case "鲳鱼":
                return 0xFFA8B8AC;
            case "河豚":
                return 0xFFBCAA70;
            case "鲷鱼":
                return 0xFFCB8E7D;
            case "章鱼":
                return 0xFFAE7B72;
            case "河蚌": case "蛤蜊":
                return 0xFFB99D78;
            case "比目鱼":
                return 0xFF9A8B67;
            default:
                return 0xFF6F9990;
        }
    }

    private static int naturalAccent(String name) {
        switch (name) {
            case "鳟鱼": case "马口鱼": return 0xFFD28D77;
            case "鲭鱼": case "黑鱼": case "石斑鱼": return 0xFF344F49;
            case "鲤鱼": case "黄鱼": case "黄鳝": return 0xFFE0BD69;
            case "鲷鱼": return 0xFFE4B09A;
            case "虾": case "螃蟹": return 0xFFE7B089;
            case "落叶": return 0xFFD0A252;
            case "破陶罐": return 0xFF714A3D;
            case "空罐头": return 0xFFB6654D;
            case "硬币": return 0xFFF0D27A;
            default: return 0xFFD6C58E;
        }
    }

    private static void drawFish(Canvas c, Paint p, String name,
                                 int main, int light, int dark, int accent) {
        int w = 92;
        int h = 44;
        boolean whiskers = "鲤鱼".equals(name) || "鲶鱼".equals(name) || "黄颡鱼".equals(name);
        boolean spines = "罗非鱼".equals(name) || "鳜鱼".equals(name) || "鲈鱼".equals(name);
        if ("白条鱼".equals(name) || "银鱼".equals(name) || "沙丁鱼".equals(name)) {
            w = 94;
            h = 27;
        } else if ("鳊鱼".equals(name) || "鲳鱼".equals(name) || "鲷鱼".equals(name)) {
            w = 78;
            h = 57;
        } else if ("草鱼".equals(name) || "青鱼".equals(name) || "黑鱼".equals(name)) {
            w = 99;
            h = 39;
        } else if ("鳜鱼".equals(name) || "石斑鱼".equals(name) || "罗非鱼".equals(name)) {
            w = 91;
            h = 51;
        }
        int hw = w / 2;
        int hh = h / 2;

        triangle(c, p, dark, -hw + 10, -7, -59, -27, -53, 0);
        triangle(c, p, dark, -53, 0, -59, 27, -hw + 9, 8);
        triangle(c, p, main, -hw + 6, -5, -53, -20, -48, -1);
        triangle(c, p, accent, -48, 2, -53, 20, -hw + 7, 6);

        if (spines) {
            for (int i = 0; i < 5; i++) {
                int x = -24 + i * 11;
                triangle(c, p, dark, x - 5, -hh + 5, x, -hh - 13 + i % 2 * 3, x + 6, -hh + 4);
            }
        } else {
            triangle(c, p, dark, -20, -hh + 4, -6, -hh - 12, 14, -hh + 3);
            triangle(c, p, accent, -13, -hh + 1, -5, -hh - 7, 6, -hh);
        }
        triangle(c, p, dark, -9, hh - 3, 9, hh + 13, 21, hh - 4);
        pixelEllipse(c, p, dark, 2, 0, hw, hh);
        pixelEllipse(c, p, main, 3, -1, hw - 5, hh - 5);
        fill(c, p, light, -hw + 18, -hh + 5, hw - 12, -hh + 10);
        fill(c, p, mix(main, light, 0.35f), -hw + 10, -hh + 11, -hw + 17, hh - 9);

        if ("罗非鱼".equals(name) || "鲷鱼".equals(name)) {
            for (int i = 0; i < 4; i++) {
                int x = -22 + i * 13;
                fill(c, p, withAlpha(dark, 190), x, -hh + 8, x + 5, hh - 7);
            }
        } else if ("鲭鱼".equals(name)) {
            for (int i = 0; i < 5; i++) {
                int x = -26 + i * 12;
                line(c, p, dark, 3, x, -hh + 5, x + 7, -3);
            }
        } else if ("鳟鱼".equals(name) || "马口鱼".equals(name)) {
            fill(c, p, accent, -hw + 12, 1, hw - 11, 7);
            for (int i = 0; i < 6; i++) {
                fill(c, p, dark, -28 + i * 10, -hh + 9 + (i % 2) * 6,
                        -24 + i * 10, -hh + 13 + (i % 2) * 6);
            }
        } else if ("黑鱼".equals(name) || "石斑鱼".equals(name) || "鳜鱼".equals(name)) {
            for (int i = 0; i < 8; i++) {
                int x = -30 + (i * 19) % 57;
                int y = -hh + 11 + (i * 13) % Math.max(8, h - 20);
                fill(c, p, accent, x, y, x + 6 + i % 3, y + 4);
            }
        } else if ("鲤鱼".equals(name) || "鲫鱼".equals(name)) {
            for (int i = 0; i < 4; i++) {
                line(c, p, withAlpha(dark, 150), 1, -22 + i * 12, -4,
                        -16 + i * 12, 4);
            }
        }

        line(c, p, dark, 2, hw - 20, -hh + 9, hw - 15, hh - 7);
        fill(c, p, light, hw - 17, -10, hw - 9, -2);
        fill(c, p, dark, hw - 13, -8, hw - 8, -3);
        line(c, p, dark, 2, hw - 1, 5, hw + 7, 7);
        if (whiskers) {
            line(c, p, dark, 2, hw - 2, 6, hw + 13, 14);
            line(c, p, dark, 2, hw - 5, 9, hw + 7, 22);
            if ("鲶鱼".equals(name) || "黄颡鱼".equals(name)) {
                line(c, p, dark, 2, hw - 8, -2, hw + 14, -8);
            }
        }
    }

    private static void drawEel(Canvas c, Paint p, String name,
                                int main, int light, int dark, int accent) {
        boolean ribbon = "带鱼".equals(name);
        PATH.reset();
        PATH.moveTo(-56, 17);
        PATH.lineTo(-43, -2);
        PATH.lineTo(-17, -18);
        PATH.lineTo(14, -18);
        PATH.lineTo(40, -9);
        PATH.lineTo(51, 1);
        PATH.lineTo(45, 15);
        PATH.lineTo(25, 9);
        PATH.lineTo(5, -1);
        PATH.lineTo(-14, 1);
        PATH.lineTo(-31, 14);
        PATH.lineTo(-43, 29);
        PATH.close();
        fillPath(c, p, dark);
        PATH.reset();
        PATH.moveTo(-49, 17);
        PATH.lineTo(-38, 2);
        PATH.lineTo(-14, -11);
        PATH.lineTo(12, -11);
        PATH.lineTo(38, -3);
        PATH.lineTo(44, 3);
        PATH.lineTo(40, 8);
        PATH.lineTo(24, 2);
        PATH.lineTo(4, -6);
        PATH.lineTo(-11, -4);
        PATH.lineTo(-27, 10);
        PATH.lineTo(-40, 24);
        PATH.close();
        fillPath(c, p, main);
        line(c, p, light, ribbon ? 4 : 3, -41, 8, -14, -8);
        line(c, p, light, ribbon ? 4 : 3, -12, -8, 13, -8);
        line(c, p, accent, 2, -40, 20, -26, 8);
        if (ribbon) {
            for (int i = 0; i < 6; i++) {
                triangle(c, p, accent, -26 + i * 10, -10 - (i < 3 ? i * 2 : (5 - i) * 2),
                        -21 + i * 10, -21, -16 + i * 10, -11);
            }
        }
        fill(c, p, light, 32, -4, 39, 3);
        fill(c, p, dark, 35, -3, 40, 2);
        line(c, p, dark, 2, 43, 8, 53, 9);
        if ("泥鳅".equals(name)) {
            line(c, p, dark, 1, 42, 8, 52, 16);
            line(c, p, dark, 1, 40, 9, 46, 20);
        }
    }

    private static void drawPuffer(Canvas c, Paint p,
                                   int main, int light, int dark, int accent) {
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6.0;
            float x = (float) Math.cos(angle);
            float y = (float) Math.sin(angle);
            triangle(c, p, dark, x * 34 - y * 4, y * 27 + x * 4,
                    x * 48, y * 39, x * 34 + y * 4, y * 27 - x * 4);
        }
        triangle(c, p, dark, -29, -5, -52, -20, -48, 4);
        triangle(c, p, dark, -48, 4, -52, 23, -28, 10);
        pixelEllipse(c, p, dark, 4, 0, 40, 32);
        pixelEllipse(c, p, main, 5, -1, 34, 27);
        pixelEllipse(c, p, light, 12, 10, 23, 12);
        fill(c, p, light, 20, -13, 28, -5);
        fill(c, p, dark, 23, -11, 29, -5);
        fill(c, p, dark, 37, 2, 43, 7);
        for (int i = 0; i < 7; i++) {
            fill(c, p, accent, -19 + (i * 13) % 39, -18 + (i * 11) % 25,
                    -15 + (i * 13) % 39, -14 + (i * 11) % 25);
        }
    }

    private static void drawFlatfish(Canvas c, Paint p,
                                     int main, int light, int dark, int accent) {
        triangle(c, p, dark, -34, -5, -56, -20, -51, 1);
        triangle(c, p, dark, -51, 1, -55, 20, -34, 8);
        for (int i = 0; i < 8; i++) {
            int x = -33 + i * 10;
            triangle(c, p, dark, x, -19, x + 5, -30 + i % 2 * 3, x + 10, -18);
            triangle(c, p, dark, x, 19, x + 6, 28 - i % 2 * 3, x + 10, 18);
        }
        pixelEllipse(c, p, dark, 2, 0, 47, 27);
        pixelEllipse(c, p, main, 3, -1, 41, 21);
        fill(c, p, light, -25, -15, 23, -10);
        for (int i = 0; i < 8; i++) {
            fill(c, p, accent, -28 + (i * 17) % 54, -8 + (i * 11) % 18,
                    -23 + (i * 17) % 54, -4 + (i * 11) % 18);
        }
        fill(c, p, light, 20, -12, 27, -5);
        fill(c, p, light, 31, -9, 38, -2);
        fill(c, p, dark, 23, -10, 28, -5);
        fill(c, p, dark, 34, -7, 39, -2);
        line(c, p, dark, 2, 38, 8, 48, 7);
    }

    private static void drawShrimp(Canvas c, Paint p,
                                   int main, int light, int dark, int accent) {
        // Segmented crescent body and a fan tail.
        triangle(c, p, dark, -30, 8, -55, -4, -48, 15);
        triangle(c, p, dark, -48, 15, -57, 29, -28, 20);
        pixelEllipse(c, p, dark, -14, 2, 27, 24);
        pixelEllipse(c, p, main, -14, 1, 22, 19);
        pixelEllipse(c, p, dark, 14, -9, 26, 22);
        pixelEllipse(c, p, main, 15, -10, 21, 17);
        quad(c, p, main, 26, -21, 48, -13, 42, 2, 27, 5);
        fill(c, p, light, -26, -12, -19, 13);
        fill(c, p, light, -10, -17, -4, 11);
        fill(c, p, accent, 5, -23, 10, 3);
        fill(c, p, dark, 28, -19, 34, -13);
        fill(c, p, light, 29, -20, 33, -16);
        line(c, p, dark, 2, 41, -15, 57, -30);
        line(c, p, dark, 2, 39, -13, 56, -16);
        line(c, p, dark, 1, 38, -10, 52, -3);
        for (int i = 0; i < 4; i++) {
            line(c, p, dark, 2, -5 + i * 10, 8, -13 + i * 10, 26);
            line(c, p, dark, 2, -13 + i * 10, 26, -4 + i * 10, 30);
        }
        triangle(c, p, accent, 43, -13, 57, -10, 43, -6);
    }

    private static void drawCrab(Canvas c, Paint p,
                                 int main, int light, int dark, int accent) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setColor(dark);
        p.setStrokeWidth(6f);
        for (int i = 0; i < 3; i++) {
            int y = -1 + i * 9;
            c.drawLine(-24, y, -45 - i * 2, y + 11, p);
            c.drawLine(-45 - i * 2, y + 11, -54, y + 20, p);
            c.drawLine(24, y, 45 + i * 2, y + 11, p);
            c.drawLine(45 + i * 2, y + 11, 54, y + 20, p);
        }
        p.setStyle(Paint.Style.FILL);
        line(c, p, dark, 7, -25, -11, -39, -23);
        line(c, p, dark, 7, 25, -11, 39, -23);
        pixelEllipse(c, p, dark, -42, -25, 14, 13);
        pixelEllipse(c, p, dark, 42, -25, 14, 13);
        pixelEllipse(c, p, main, -42, -26, 9, 8);
        pixelEllipse(c, p, main, 42, -26, 9, 8);
        triangle(c, p, dark, -48, -32, -42, -45, -37, -31);
        triangle(c, p, dark, 48, -32, 42, -45, 37, -31);
        pixelEllipse(c, p, dark, 0, 2, 34, 25);
        pixelEllipse(c, p, main, 0, 0, 29, 20);
        fill(c, p, light, -21, -12, 20, -6);
        fill(c, p, accent, -16, 10, 16, 15);
        fill(c, p, dark, -19, -23, -13, -11);
        fill(c, p, dark, 13, -23, 19, -11);
        fill(c, p, light, -17, -25, -11, -19);
        fill(c, p, light, 11, -25, 17, -19);
        fill(c, p, dark, -14, -22, -10, -18);
        fill(c, p, dark, 10, -22, 14, -18);
    }

    private static void drawOctopus(Canvas c, Paint p,
                                    int main, int light, int dark, int accent) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setColor(dark);
        p.setStrokeWidth(10f);
        for (int i = 0; i < 8; i++) {
            float startX = -27 + i * 8;
            float endX = -51 + i * 15;
            float endY = 29 + (i % 3) * 4;
            c.drawLine(startX, 6, startX + (i % 2 == 0 ? -6 : 6), 22, p);
            c.drawLine(startX + (i % 2 == 0 ? -6 : 6), 22, endX, endY, p);
        }
        p.setColor(main);
        p.setStrokeWidth(5f);
        for (int i = 0; i < 8; i++) {
            float startX = -27 + i * 8;
            float endX = -51 + i * 15;
            float endY = 29 + (i % 3) * 4;
            c.drawLine(startX, 8, startX + (i % 2 == 0 ? -6 : 6), 21, p);
            c.drawLine(startX + (i % 2 == 0 ? -6 : 6), 21, endX, endY, p);
        }
        p.setStyle(Paint.Style.FILL);
        pixelEllipse(c, p, dark, 0, -12, 31, 31);
        pixelEllipse(c, p, main, 0, -13, 26, 26);
        pixelEllipse(c, p, light, -7, -23, 13, 10);
        fill(c, p, light, -18, -8, -7, 3);
        fill(c, p, light, 7, -8, 18, 3);
        fill(c, p, dark, -14, -6, -8, 1);
        fill(c, p, dark, 8, -6, 14, 1);
        for (int i = 0; i < 5; i++) {
            fill(c, p, accent, -37 + i * 18, 25 + (i % 2) * 5,
                    -33 + i * 18, 29 + (i % 2) * 5);
        }
    }

    private static void drawShell(Canvas c, Paint p, boolean mussel,
                                  int main, int light, int dark, int accent) {
        if (mussel) {
            PATH.reset();
            PATH.moveTo(-51, 12);
            PATH.lineTo(-37, -12);
            PATH.lineTo(-4, -28);
            PATH.lineTo(32, -22);
            PATH.lineTo(51, -5);
            PATH.lineTo(35, 15);
            PATH.lineTo(-6, 28);
            PATH.lineTo(-39, 25);
            PATH.close();
            fillPath(c, p, dark);
            PATH.reset();
            PATH.moveTo(-43, 11);
            PATH.lineTo(-31, -7);
            PATH.lineTo(-3, -20);
            PATH.lineTo(27, -16);
            PATH.lineTo(42, -4);
            PATH.lineTo(29, 9);
            PATH.lineTo(-7, 20);
            PATH.lineTo(-34, 18);
            PATH.close();
            fillPath(c, p, main);
            line(c, p, light, 3, -33, 7, -2, -13);
            line(c, p, light, 3, -2, -13, 30, -8);
            line(c, p, accent, 2, -30, 15, 22, 4);
        } else {
            PATH.reset();
            PATH.moveTo(-47, 20);
            PATH.lineTo(-42, -3);
            PATH.lineTo(-28, -24);
            PATH.lineTo(-8, -33);
            PATH.lineTo(13, -31);
            PATH.lineTo(34, -19);
            PATH.lineTo(46, 3);
            PATH.lineTo(48, 20);
            PATH.close();
            fillPath(c, p, dark);
            PATH.reset();
            PATH.moveTo(-39, 15);
            PATH.lineTo(-34, -1);
            PATH.lineTo(-22, -18);
            PATH.lineTo(-6, -26);
            PATH.lineTo(11, -24);
            PATH.lineTo(28, -14);
            PATH.lineTo(38, 3);
            PATH.lineTo(40, 15);
            PATH.close();
            fillPath(c, p, main);
            for (int i = 0; i < 5; i++) {
                line(c, p, i % 2 == 0 ? light : accent, 2,
                        0, 15, -30 + i * 15, -12 - (2 - Math.abs(2 - i)) * 5);
            }
            fill(c, p, light, -40, 15, 41, 21);
            fill(c, p, dark, -43, 21, 44, 26);
        }
    }

    private static void drawCan(Canvas c, Paint p,
                                int main, int light, int dark, int accent) {
        quad(c, p, dark, -31, -27, 25, -30, 32, 27, -26, 31);
        quad(c, p, main, -25, -22, 20, -24, 25, 22, -21, 25);
        pixelEllipse(c, p, dark, -1, -26, 30, 9);
        pixelEllipse(c, p, light, -1, -27, 24, 5);
        pixelEllipse(c, p, dark, 1, 25, 28, 7);
        fill(c, p, light, -20, -17, -12, 20);
        quad(c, p, accent, -12, -12, 22, -17, 23, 2, -12, 8);
        fill(c, p, dark, -3, -31, 12, -26);
        fill(c, p, main, 0, -30, 9, -28);
        line(c, p, dark, 2, 20, -14, 11, -3);
        line(c, p, dark, 2, 11, -3, 23, 8);
    }

    private static void drawStick(Canvas c, Paint p,
                                  int main, int light, int dark, int accent) {
        PATH.reset();
        PATH.moveTo(-54, 15);
        PATH.lineTo(-48, 3);
        PATH.lineTo(-12, -5);
        PATH.lineTo(12, -24);
        PATH.lineTo(21, -22);
        PATH.lineTo(13, -8);
        PATH.lineTo(45, -16);
        PATH.lineTo(53, -8);
        PATH.lineTo(16, 7);
        PATH.lineTo(-14, 10);
        PATH.lineTo(-43, 25);
        PATH.close();
        fillPath(c, p, dark);
        PATH.reset();
        PATH.moveTo(-46, 14);
        PATH.lineTo(-41, 8);
        PATH.lineTo(-9, 1);
        PATH.lineTo(11, -16);
        PATH.lineTo(15, -15);
        PATH.lineTo(6, 0);
        PATH.lineTo(41, -10);
        PATH.lineTo(44, -7);
        PATH.lineTo(12, 5);
        PATH.lineTo(-13, 7);
        PATH.lineTo(-39, 20);
        PATH.close();
        fillPath(c, p, main);
        line(c, p, light, 3, -37, 10, -11, 4);
        line(c, p, light, 3, -8, 3, 8, -9);
        line(c, p, accent, 2, 17, 2, 35, -4);
        line(c, p, dark, 2, -29, 7, -25, 16);
        line(c, p, dark, 2, -4, -1, 0, 8);
    }

    private static void drawLeaf(Canvas c, Paint p,
                                 int main, int light, int dark, int accent) {
        line(c, p, dark, 4, -43, 31, 31, -29);
        PATH.reset();
        PATH.moveTo(-40, 20);
        PATH.lineTo(-45, 3);
        PATH.lineTo(-32, -5);
        PATH.lineTo(-35, -19);
        PATH.lineTo(-17, -18);
        PATH.lineTo(-9, -34);
        PATH.lineTo(5, -24);
        PATH.lineTo(22, -29);
        PATH.lineTo(25, -12);
        PATH.lineTo(40, -5);
        PATH.lineTo(27, 7);
        PATH.lineTo(24, 22);
        PATH.lineTo(5, 18);
        PATH.lineTo(-9, 30);
        PATH.lineTo(-20, 16);
        PATH.close();
        fillPath(c, p, dark);
        PATH.reset();
        PATH.moveTo(-33, 15);
        PATH.lineTo(-37, 4);
        PATH.lineTo(-24, -2);
        PATH.lineTo(-28, -13);
        PATH.lineTo(-12, -11);
        PATH.lineTo(-7, -25);
        PATH.lineTo(4, -17);
        PATH.lineTo(17, -21);
        PATH.lineTo(18, -7);
        PATH.lineTo(31, -4);
        PATH.lineTo(20, 5);
        PATH.lineTo(18, 15);
        PATH.lineTo(4, 11);
        PATH.lineTo(-8, 21);
        PATH.lineTo(-16, 10);
        PATH.close();
        fillPath(c, p, main);
        line(c, p, accent, 3, -28, 13, 21, -12);
        line(c, p, light, 2, -9, 3, -20, -8);
        line(c, p, light, 2, 4, -3, 15, 7);
    }

    private static void drawPot(Canvas c, Paint p,
                                int main, int light, int dark, int accent) {
        PATH.reset();
        PATH.moveTo(-21, -29);
        PATH.lineTo(17, -29);
        PATH.lineTo(18, -17);
        PATH.lineTo(32, -4);
        PATH.lineTo(35, 21);
        PATH.lineTo(24, 34);
        PATH.lineTo(-23, 34);
        PATH.lineTo(-35, 20);
        PATH.lineTo(-31, -5);
        PATH.lineTo(-19, -17);
        PATH.close();
        fillPath(c, p, dark);
        PATH.reset();
        PATH.moveTo(-14, -23);
        PATH.lineTo(11, -23);
        PATH.lineTo(12, -13);
        PATH.lineTo(25, -1);
        PATH.lineTo(28, 18);
        PATH.lineTo(19, 27);
        PATH.lineTo(-18, 27);
        PATH.lineTo(-27, 17);
        PATH.lineTo(-24, -1);
        PATH.lineTo(-12, -13);
        PATH.close();
        fillPath(c, p, main);
        fill(c, p, dark, -25, -34, 2, -25);
        fill(c, p, dark, 9, -34, 24, -25);
        fill(c, p, light, -21, -32, -1, -29);
        fill(c, p, light, -20, -8, -13, 19);
        fill(c, p, accent, -28, 4, 29, 11);
        line(c, p, dark, 3, 12, -13, 3, 0);
        line(c, p, dark, 3, 3, 0, 14, 12);
        line(c, p, dark, 2, 3, 0, -5, 11);
        triangle(c, p, dark, 3, -34, 10, -25, 14, -35);
    }

    private static void drawBottle(Canvas c, Paint p,
                                   int main, int light, int dark, int accent) {
        fill(c, p, dark, -15, -38, 15, -18);
        fill(c, p, dark, -24, -21, 24, 31);
        fill(c, p, dark, -18, 29, 18, 37);
        fill(c, p, main, -10, -32, 10, -17);
        fill(c, p, main, -18, -17, 18, 27);
        fill(c, p, main, -13, 27, 13, 32);
        fill(c, p, light, -14, -14, -8, 22);
        fill(c, p, withAlpha(light, 180), 9, -12, 14, 19);
        fill(c, p, 0xFF8B6B45, -13, -41, 13, -33);
        fill(c, p, accent, -18, -4, 18, 13);
        fill(c, p, 0xFFE1D2A5, -13, -1, 13, 9);
        fill(c, p, 0xFFB69A6D, -9, 1, -6, 7);
        line(c, p, dark, 2, -17, 23, 16, 23);
    }

    private static void drawCoin(Canvas c, Paint p,
                                 int main, int light, int dark, int accent) {
        pixelEllipse(c, p, dark, 2, 2, 29, 34);
        pixelEllipse(c, p, main, -1, -1, 26, 31);
        pixelEllipse(c, p, accent, -1, -1, 20, 25);
        pixelEllipse(c, p, main, -1, -1, 15, 20);
        fill(c, p, light, -14, -20, -7, 14);
        sparkle(c, p, 2, 0, light);
        fill(c, p, dark, 16, -8, 20, 10);
    }

    private static void drawStone(Canvas c, Paint p,
                                  int main, int light, int dark, int accent) {
        PATH.reset();
        PATH.moveTo(-48, 19);
        PATH.lineTo(-39, -8);
        PATH.lineTo(-18, -27);
        PATH.lineTo(13, -31);
        PATH.lineTo(40, -15);
        PATH.lineTo(50, 13);
        PATH.lineTo(31, 29);
        PATH.lineTo(-25, 32);
        PATH.close();
        fillPath(c, p, dark);
        PATH.reset();
        PATH.moveTo(-39, 16);
        PATH.lineTo(-32, -5);
        PATH.lineTo(-14, -20);
        PATH.lineTo(11, -24);
        PATH.lineTo(32, -11);
        PATH.lineTo(41, 11);
        PATH.lineTo(26, 22);
        PATH.lineTo(-21, 25);
        PATH.close();
        fillPath(c, p, main);
        triangle(c, p, light, -29, -4, -12, -18, 9, -21);
        triangle(c, p, light, 9, -21, -3, -4, -29, -4);
        quad(c, p, accent, -3, -4, 9, -21, 30, -9, 17, 4);
        line(c, p, dark, 2, 17, 4, 6, 12);
        line(c, p, dark, 2, 6, 12, 13, 21);
    }

    private static void drawPlasticBag(Canvas c, Paint p,
                                       int main, int light, int dark, int accent) {
        PATH.reset();
        PATH.moveTo(-35, -20);
        PATH.lineTo(-19, -29);
        PATH.lineTo(-12, -16);
        PATH.lineTo(12, -16);
        PATH.lineTo(20, -30);
        PATH.lineTo(35, -20);
        PATH.lineTo(29, 2);
        PATH.lineTo(43, 25);
        PATH.lineTo(23, 34);
        PATH.lineTo(5, 28);
        PATH.lineTo(-15, 36);
        PATH.lineTo(-42, 24);
        PATH.lineTo(-30, 1);
        PATH.close();
        fillPath(c, p, dark);
        PATH.reset();
        PATH.moveTo(-28, -17);
        PATH.lineTo(-20, -21);
        PATH.lineTo(-15, -9);
        PATH.lineTo(15, -9);
        PATH.lineTo(21, -22);
        PATH.lineTo(28, -17);
        PATH.lineTo(22, 3);
        PATH.lineTo(35, 21);
        PATH.lineTo(21, 27);
        PATH.lineTo(5, 21);
        PATH.lineTo(-14, 28);
        PATH.lineTo(-34, 20);
        PATH.lineTo(-23, 1);
        PATH.close();
        fillPath(c, p, main);
        fill(c, p, dark, -13, -18, 14, -9);
        fill(c, p, light, -23, 0, -15, 19);
        quad(c, p, accent, -8, 1, 18, -3, 23, 15, -5, 18);
        line(c, p, light, 2, 24, 2, 29, 18);
        line(c, p, dark, 2, -5, 20, 5, 10);
    }

    private static void sparkle(Canvas c, Paint p, float x, float y, int color) {
        fill(c, p, color, x - 2, y - 9, x + 2, y + 9);
        fill(c, p, color, x - 9, y - 2, x + 9, y + 2);
        fill(c, p, mix(color, 0xFFFFF3C4, 0.50f), x - 3, y - 3, x + 3, y + 3);
    }

    private static int horizon(int scene) {
        switch (scene) {
            case 0: return 310;
            case 1: return 306;
            case 2: return 292;
            case 3: return 312;
            case 4: return 306;
            default: return 300;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int positiveMod(int value, int modulus) {
        int result = value % modulus;
        return result < 0 ? result + modulus : result;
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int alpha = Math.round(((a >>> 24) & 0xFF) * (1f - t) + ((b >>> 24) & 0xFF) * t);
        int red = Math.round(((a >> 16) & 0xFF) * (1f - t) + ((b >> 16) & 0xFF) * t);
        int green = Math.round(((a >> 8) & 0xFF) * (1f - t) + ((b >> 8) & 0xFF) * t);
        int blue = Math.round((a & 0xFF) * (1f - t) + (b & 0xFF) * t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (clamp(alpha, 0, 255) << 24);
    }

    private static void fill(Canvas c, Paint p, int color,
                             float left, float top, float right, float bottom) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawRect(left, top, right, bottom, p);
    }

    private static void line(Canvas c, Paint p, int color, float width,
                             float x0, float y0, float x1, float y1) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(color);
        p.setStrokeWidth(width);
        c.drawLine(x0, y0, x1, y1, p);
        p.setStyle(Paint.Style.FILL);
    }

    private static void triangle(Canvas c, Paint p, int color,
                                 float x0, float y0, float x1, float y1, float x2, float y2) {
        PATH.reset();
        PATH.moveTo(x0, y0);
        PATH.lineTo(x1, y1);
        PATH.lineTo(x2, y2);
        PATH.close();
        fillPath(c, p, color);
    }

    private static void quad(Canvas c, Paint p, int color,
                             float x0, float y0, float x1, float y1,
                             float x2, float y2, float x3, float y3) {
        PATH.reset();
        PATH.moveTo(x0, y0);
        PATH.lineTo(x1, y1);
        PATH.lineTo(x2, y2);
        PATH.lineTo(x3, y3);
        PATH.close();
        fillPath(c, p, color);
    }

    private static void fillPath(Canvas c, Paint p, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawPath(PATH, p);
    }

    private static void pixelEllipse(Canvas c, Paint p, int color,
                                     float cx, float cy, float rx, float ry) {
        if (rx <= 0 || ry <= 0) return;
        float step = Math.max(2f, Math.min(6f, ry / 5f));
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        for (float y = -ry; y < ry; y += step) {
            float middle = y + step * 0.5f;
            float normalized = Math.min(1f, Math.abs(middle) / ry);
            float half = rx * (float) Math.sqrt(Math.max(0f, 1f - normalized * normalized));
            half = Math.max(1f, (int) (half / 2f) * 2f);
            float bottom = Math.min(ry, y + step);
            c.drawRect(cx - half, cy + y, cx + half, cy + bottom, p);
        }
    }
}
