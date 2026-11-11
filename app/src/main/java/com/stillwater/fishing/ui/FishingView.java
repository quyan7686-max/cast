package com.stillwater.fishing.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.stillwater.fishing.data.Catalog;
import com.stillwater.fishing.data.CatchResult;
import com.stillwater.fishing.data.EntryRecord;
import com.stillwater.fishing.data.GameProgress;
import com.stillwater.fishing.data.Species;
import com.stillwater.fishing.engine.FishingEngine;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class FishingView extends View {
    private static final float W = 1280, H = 720;
    private enum Panel { NONE, SCENES, CODEX, DETAIL, HELP }
    private final FishingEngine engine;
    private final Paint paint = new Paint();
    private final RectF sceneButton = new RectF(910, 24, 1030, 82);
    private final RectF codexButton = new RectF(1042, 24, 1162, 82);
    private final RectF helpButton = new RectF(1174, 24, 1256, 82);
    private final RectF fishButton = new RectF(1030, 548, 1200, 694);
    private final RectF closeButton = new RectF(1152, 70, 1210, 124);
    private Panel panel = Panel.NONE;
    private boolean foreground, windowFocused = true;
    private long lastFrame = System.nanoTime();
    private float scale = 1, offsetX, offsetY;
    private CatchResult displayedCatch;
    private long messageUntil;
    private String escapeMessage = "";
    private int selectedScene;
    private int qualityFilter, unlockFilter;
    private int selectedEntry;
    private float codexScroll, touchY, startScroll;
    private boolean scrolling;
    private ToneGenerator tone;

    public FishingView(Context context, FishingEngine engine) {
        super(context);
        this.engine = engine;
        selectedScene = engine.getProgress().sceneIndex;
        setFocusable(true);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeJoin(Paint.Join.MITER);
        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
        } catch (RuntimeException ignored) {
            tone = null;
        }
    }

    public void setForeground(boolean foreground) {
        this.foreground = foreground;
        syncPause();
    }

    private void syncPause() {
        engine.setPaused(!foreground || !windowFocused || panel != Panel.NONE);
        lastFrame = System.nanoTime();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        windowFocused = hasFocus;
        syncPause();
    }

    public void playBiteSound() {
        if (tone != null) tone.startTone(ToneGenerator.TONE_PROP_ACK, 140);
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    public void showCatch(CatchResult result) {
        displayedCatch = result;
        escapeMessage = "";
        messageUntil = System.nanoTime() + 1_000_000_000L;
    }

    public void showEscape() {
        displayedCatch = null;
        escapeMessage = "逃脱了";
        messageUntil = System.nanoTime() + 1_000_000_000L;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        double delta = (now - lastFrame) / 1_000_000_000.0;
        lastFrame = now;
        engine.tick(delta, System.currentTimeMillis());
        scale = Math.min(getWidth() / W, getHeight() / H);
        offsetX = (getWidth() - W * scale) / 2;
        offsetY = (getHeight() - H * scale) / 2;
        canvas.drawColor(Color.rgb(12, 29, 35));
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);
        float time = (now % 100_000_000_000L) / 1_000_000_000f;
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        PixelArt.drawScene(canvas, paint, engine.getProgress().sceneIndex,
                Catalog.period(hour), engine.getProgress().raining, time);
        PixelArt.drawAngler(canvas, paint, engine.getState(), time);
        drawHeader(canvas, hour, calendar.get(Calendar.MINUTE));
        drawFishingButton(canvas);
        drawMessage(canvas, now);
        if (panel == Panel.SCENES) drawScenes(canvas);
        else if (panel == Panel.CODEX) drawCodex(canvas);
        else if (panel == Panel.DETAIL) drawDetail(canvas);
        else if (panel == Panel.HELP) drawHelp(canvas);
        canvas.restore();
        postInvalidateOnAnimation();
    }

    private void drawHeader(Canvas c, int hour, int minute) {
        panel(c, 20, 20, 338, 105, color("E61A3138"));
        text(c, "静水集", 40, 61, 32, color("F8EFD7"), true);
        text(c, Catalog.SCENES[engine.getProgress().sceneIndex] + "  ·  慢下来，等一尾鱼", 41, 89, 16, color("B9D3C8"), false);
        panel(c, 535, 25, 745, 80, color("D9183138"));
        String weather = engine.getProgress().raining ? "雨" : "晴";
        text(c, weather + "  " + Catalog.periodName(hour) + String.format(Locale.CHINA, "  %02d:%02d", hour, minute),
                640, 60, 20, color("F5EDDA"), true, Paint.Align.CENTER);
        button(c, sceneButton, "场景", false);
        button(c, codexButton, "图鉴 " + engine.getProgress().unlockedCount() + "/84", false);
        button(c, helpButton, "?", false);
    }

    private void drawFishingButton(Canvas c) {
        boolean biting = engine.getState() == FishingEngine.State.BITING;
        if (biting) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8);
            paint.setColor(color("44312626"));
            c.drawArc(fishButton, -90, 360, false, paint);
            paint.setColor(color("FFE49A55"));
            c.drawArc(fishButton, -90, 360 * (float) engine.getBiteProgress(), false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        RectF body = new RectF(fishButton);
        body.inset(13, 13);
        paint.setColor(biting ? color("F2B84B") : color("EDF0DF"));
        c.drawRoundRect(body, 26, 26, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(biting ? color("FFF4CE") : color("9DB8A9"));
        c.drawRoundRect(body, 26, 26, paint);
        paint.setStyle(Paint.Style.FILL);
        text(c, engine.getState() == FishingEngine.State.IDLE ? "抛竿" : "收竿",
                body.centerX(), body.centerY() + 12, 34, color("19343A"), true, Paint.Align.CENTER);
        String hint = engine.getState() == FishingEngine.State.IDLE ? "静候一处好水" :
                biting ? "就是现在" : "浮漂轻动，耐心等候";
        text(c, hint, 1115, 530, 17, biting ? color("FFF0B2") : color("F1F3DE"), true, Paint.Align.CENTER);
    }

    private void drawMessage(Canvas c, long now) {
        if (now >= messageUntil) return;
        if (displayedCatch == null) {
            panel(c, 490, 100, 790, 156, color("E7193034"));
            text(c, escapeMessage, 640, 140, 23, color("F2EADA"), true, Paint.Align.CENTER);
            return;
        }
        CatchResult result = displayedCatch;
        panel(c, 390, 92, 890, 259, color("F31B3439"));
        PixelArt.drawSpecimen(c, paint, result.species, result.shiny, false, 475, 167, .85f);
        text(c, result.species.name, 560, 139, 30, color("F9EFD9"), true);
        text(c, result.qualityText(), 560, 169, 18, result.shiny ? color("FFD477") : color("B8D4C7"), true);
        text(c, format(result.length) + " cm   ·   " + format(result.weight) + " g",
                560, 201, 19, color("F0E9D8"), false);
        StringBuilder marks = new StringBuilder();
        if (result.newDiscovery) marks.append("新发现  ");
        else {
            if (result.lengthRecord) marks.append("长度新纪录  ");
            if (result.weightRecord) marks.append("重量新纪录");
        }
        if (result.collectionComplete) marks.append("  图鉴已集齐");
        text(c, marks.toString(), 560, 235, 17, color("F4C95D"), true);
    }

    private void drawScenes(Canvas c) {
        shade(c);
        panel(c, 100, 65, 1180, 665, color("F5EEE8DA"));
        text(c, "选择一处水域", 140, 119, 32, color("19343A"), true);
        text(c, "切换场景会结束当前钓鱼；仅查看不会影响本轮", 140, 151, 17, color("60746E"), false);
        close(c);
        for (int i = 0; i < 6; i++) {
            float x = 140 + (i % 3) * 340;
            float y = 185 + (i / 3) * 170;
            boolean selected = selectedScene == i;
            paint.setColor(selected ? color("315B54") : color("D9E0D2"));
            c.drawRoundRect(new RectF(x, y, x + 310, y + 140), 12, 12, paint);
            PixelArt.drawSpecimen(c, paint, Catalog.get(new int[]{0, 1, 15, 19, 21, 29}[i]),
                    false, false, x + 63, y + 69, .55f);
            text(c, Catalog.SCENES[i], x + 123, y + 58, 25, selected ? color("FFF1D5") : color("24433F"), true);
            String detail = new String[]{"荷叶 · 芦苇 · 木平台", "树影 · 流水 · 卵石岸", "远山 · 湖面 · 长栈桥", "浅水 · 石块 · 小瀑布", "木桩 · 渔船 · 远处灯", "岩石 · 浪花 · 潮池"}[i];
            text(c, detail, x + 123, y + 91, 15, selected ? color("C6D8C9") : color("657970"), false);
            if (i == engine.getProgress().sceneIndex) text(c, "当前", x + 123, y + 119, 14, color("D2A34C"), true);
        }
        RectF confirm = new RectF(890, 555, 1135, 620);
        button(c, confirm, "前往" + Catalog.SCENES[selectedScene], true);
        button(c, new RectF(140, 555, 365, 620), "继续钓鱼", false);
    }

    private void drawCodex(Canvas c) {
        shade(c);
        panel(c, 38, 38, 1242, 692, color("FAF3EBDD"));
        text(c, "水岸图鉴", 68, 87, 31, color("19343A"), true);
        text(c, "已解锁  " + engine.getProgress().unlockedCount() + " / 84", 315, 85, 20, color("597168"), true);
        close(c);
        String[] qualities = {"全部品质", "普通", "闪光"};
        String[] unlocks = {"全部状态", "已解锁", "未解锁"};
        for (int i = 0; i < 3; i++) {
            RectF a = new RectF(68 + i * 138, 111, 194 + i * 138, 153);
            button(c, a, qualities[i], qualityFilter == i);
            RectF b = new RectF(540 + i * 138, 111, 666 + i * 138, 153);
            button(c, b, unlocks[i], unlockFilter == i);
        }
        List<Integer> entries = filteredEntries();
        int columns = 4, rows = 4;
        int first = Math.min((int) (codexScroll / 120) * columns, Math.max(0, entries.size() - 1));
        for (int slot = 0; slot < columns * rows && first + slot < entries.size(); slot++) {
            int entryIndex = entries.get(first + slot);
            int col = slot % columns, row = slot / columns;
            float x = 67 + col * 294, y = 174 + row * 120;
            drawEntryCell(c, entryIndex, x, y);
        }
        int maxScroll = Math.max(0, (int) Math.ceil(entries.size() / 4.0) * 120 - 480);
        codexScroll = Math.max(0, Math.min(codexScroll, maxScroll));
        if (entries.isEmpty()) text(c, "没有符合筛选条件的条目", 640, 385, 22, color("72867D"), false, Paint.Align.CENTER);
        text(c, "上下滑动浏览 · 点击条目查看详情", 75, 677, 15, color("718279"), false);
        text(c, (first / 4 + 1) + " / " + Math.max(1, (int) Math.ceil(entries.size() / 16.0)) + " 页",
                1165, 677, 15, color("718279"), false, Paint.Align.RIGHT);
    }

    private void drawEntryCell(Canvas c, int entryIndex, float x, float y) {
        Species species = Catalog.get(entryIndex / 2);
        boolean shiny = entryIndex % 2 == 1;
        EntryRecord record = engine.getProgress().entries[entryIndex];
        paint.setColor(record.unlocked() ? color("E1E8DA") : color("E7E4D8"));
        c.drawRoundRect(new RectF(x, y, x + 276, y + 104), 9, 9, paint);
        PixelArt.drawSpecimen(c, paint, species, shiny, !record.unlocked(), x + 54, y + 51, .48f);
        text(c, record.unlocked() ? species.name : "尚未发现", x + 108, y + 38, 20,
                record.unlocked() ? color("25433E") : color("849087"), true);
        text(c, shiny ? "闪光" : "普通", x + 108, y + 64, 14,
                shiny ? color("AD792C") : color("71877D"), true);
        text(c, "钓上 " + record.count + " 次", x + 108, y + 88, 14, color("61776D"), false);
    }

    private List<Integer> filteredEntries() {
        List<Integer> entries = new ArrayList<>();
        for (int i = 0; i < GameProgress.ENTRY_COUNT; i++) {
            if (qualityFilter != 0 && (i % 2 == 1 ? 2 : 1) != qualityFilter) continue;
            boolean unlocked = engine.getProgress().entries[i].unlocked();
            if (unlockFilter == 1 && !unlocked) continue;
            if (unlockFilter == 2 && unlocked) continue;
            entries.add(i);
        }
        return entries;
    }

    private void drawDetail(Canvas c) {
        drawCodex(c);
        shade(c);
        panel(c, 180, 82, 1100, 650, color("FFF6EFDF"));
        Species species = Catalog.get(selectedEntry / 2);
        boolean shiny = selectedEntry % 2 == 1;
        EntryRecord record = engine.getProgress().entries[selectedEntry];
        text(c, "图鉴条目  " + String.format(Locale.CHINA, "%02d", selectedEntry + 1), 220, 129, 16, color("718279"), true);
        close(c);
        panel(c, 220, 160, 565, 515, color("DDE6D8"));
        PixelArt.drawSpecimen(c, paint, species, shiny, !record.unlocked(), 393, 330, 1.65f);
        text(c, shiny ? "闪光品质" : "普通品质", 393, 477, 20, shiny ? color("9B702D") : color("4D6C61"), true, Paint.Align.CENTER);
        text(c, record.unlocked() ? species.name : "尚未发现", 615, 201, 34, color("203E39"), true);
        text(c, "出现场景", 615, 259, 16, color("7B887B"), true);
        wrappedText(c, record.unlocked() ? Catalog.sceneText(species) : "???", 615, 288, 425, 20, color("344F47"));
        text(c, "出现时间", 615, 350, 16, color("7B887B"), true);
        wrappedText(c, record.unlocked() ? species.timeText() : "???", 615, 379, 425, 20, color("344F47"));
        text(c, "钓上次数", 615, 451, 16, color("7B887B"), true);
        text(c, Integer.toString(record.count), 615, 481, 23, color("24433D"), true);
        text(c, "长度记录", 615, 530, 16, color("7B887B"), true);
        text(c, record.unlocked() ? format(record.length) + " cm" : "—", 615, 560, 22, color("24433D"), true);
        text(c, "重量记录", 850, 530, 16, color("7B887B"), true);
        text(c, record.unlocked() ? format(record.weight) + " g" : "—", 850, 560, 22, color("24433D"), true);
        button(c, new RectF(445, 590, 835, 635), "返回图鉴", true);
    }

    private void drawHelp(Canvas c) {
        shade(c);
        panel(c, 250, 90, 1030, 640, color("FAF1E8D9"));
        text(c, "留一点时间给水", 295, 145, 32, color("1F403B"), true);
        close(c);
        String[] lines = {
                "01  点击「抛竿」，等待浮漂下沉。",
                "02  浮漂下沉、按钮高亮时，点击「收竿」。",
                "03  在圆环消失前收竿即可钓获；超时自动续钓。",
                "04  等待时收竿会取消本轮，不会消耗任何收藏。",
                "05  图鉴与场景面板会暂停计时，切到后台也一样。",
                "06  雨天等待更短，闪光出现率更高。",
                "07  每个类目都有普通与闪光两种独立收藏。"
        };
        for (int i = 0; i < lines.length; i++) text(c, lines[i], 300, 211 + i * 48, 20, color("476159"), false);
        text(c, "所有进度仅保存在本机", 300, 581, 16, color("839083"), false);
    }

    private void close(Canvas c) {
        button(c, closeButton, "×", false);
    }

    private void shade(Canvas c) {
        paint.setColor(color("99101F25"));
        c.drawRect(0, 0, W, H, paint);
    }

    private void panel(Canvas c, float l, float t, float r, float b, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF(l, t, r, b), 12, 12, paint);
    }

    private void button(Canvas c, RectF rect, String label, boolean accent) {
        paint.setColor(accent ? color("D5A74F") : color("E92A4346"));
        c.drawRoundRect(rect, 10, 10, paint);
        float size = rect.width() < 90 ? 26 : rect.width() < 150 ? 16 : 19;
        text(c, label, rect.centerX(), rect.centerY() + size * .35f, size,
                accent ? color("1A3637") : color("F1EBD8"), true, Paint.Align.CENTER);
    }

    private void text(Canvas c, String value, float x, float y, float size, int color, boolean bold) {
        text(c, value, x, y, size, color, bold, Paint.Align.LEFT);
    }

    private void text(Canvas c, String value, float x, float y, float size, int color, boolean bold, Paint.Align align) {
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL));
        paint.setTextSize(size);
        paint.setTextAlign(align);
        paint.setColor(color);
        c.drawText(value, x, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setAntiAlias(false);
    }

    private void wrappedText(Canvas c, String value, float x, float y, float width, float size, int color) {
        StringBuilder line = new StringBuilder();
        float lineY = y;
        for (int i = 0; i < value.length(); i++) {
            line.append(value.charAt(i));
            paint.setTextSize(size);
            if (paint.measureText(line.toString()) > width || i == value.length() - 1) {
                text(c, line.toString(), x, lineY, size, color, false);
                line.setLength(0);
                lineY += size * 1.5;
            }
        }
    }

    private static String format(double value) {
        return String.format(Locale.CHINA, "%,.2f", value);
    }

    private static int color(String hex) {
        if (hex.length() == 8) {
            return Color.argb(Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16),
                    Integer.parseInt(hex.substring(6, 8), 16));
        }
        return Color.rgb(Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = (event.getX() - offsetX) / scale;
        float y = (event.getY() - offsetY) / scale;
        if (panel == Panel.CODEX) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchY = y;
                startScroll = codexScroll;
                scrolling = false;
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE && Math.abs(y - touchY) > 8) {
                scrolling = true;
                codexScroll = Math.max(0, startScroll + touchY - y);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP && scrolling) return true;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        performClick();
        if (panel == Panel.NONE) {
            if (sceneButton.contains(x, y)) openPanel(Panel.SCENES);
            else if (codexButton.contains(x, y)) openPanel(Panel.CODEX);
            else if (helpButton.contains(x, y)) openPanel(Panel.HELP);
            else if (fishButton.contains(x, y)) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                engine.press(System.currentTimeMillis());
            }
        } else if (panel == Panel.HELP) {
            if (closeButton.contains(x, y)) openPanel(Panel.NONE);
        } else if (panel == Panel.SCENES) {
            if (closeButton.contains(x, y) || new RectF(140, 555, 365, 620).contains(x, y)) {
                selectedScene = engine.getProgress().sceneIndex;
                openPanel(Panel.NONE);
            } else if (new RectF(890, 555, 1135, 620).contains(x, y)) {
                engine.changeScene(selectedScene);
                openPanel(Panel.NONE);
            } else {
                for (int i = 0; i < 6; i++) {
                    float left = 140 + (i % 3) * 340, top = 185 + (i / 3) * 170;
                    if (new RectF(left, top, left + 310, top + 140).contains(x, y)) selectedScene = i;
                }
            }
        } else if (panel == Panel.CODEX) {
            if (closeButton.contains(x, y)) openPanel(Panel.NONE);
            for (int i = 0; i < 3; i++) {
                if (new RectF(68 + i * 138, 111, 194 + i * 138, 153).contains(x, y)) {
                    qualityFilter = i;
                    codexScroll = 0;
                }
                if (new RectF(540 + i * 138, 111, 666 + i * 138, 153).contains(x, y)) {
                    unlockFilter = i;
                    codexScroll = 0;
                }
            }
            List<Integer> entries = filteredEntries();
            int first = Math.min((int) (codexScroll / 120) * 4, Math.max(0, entries.size() - 1));
            for (int slot = 0; slot < 16 && first + slot < entries.size(); slot++) {
                float left = 67 + (slot % 4) * 294, top = 174 + (slot / 4) * 120;
                if (new RectF(left, top, left + 276, top + 104).contains(x, y)) {
                    selectedEntry = entries.get(first + slot);
                    panel = Panel.DETAIL;
                    syncPause();
                    break;
                }
            }
        } else if (panel == Panel.DETAIL) {
            if (closeButton.contains(x, y)) openPanel(Panel.NONE);
            else if (new RectF(445, 590, 835, 635).contains(x, y)) {
                panel = Panel.CODEX;
                syncPause();
            }
        }
        return true;
    }

    private void openPanel(Panel next) {
        panel = next;
        syncPause();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (tone != null) tone.release();
        super.onDetachedFromWindow();
    }
}
