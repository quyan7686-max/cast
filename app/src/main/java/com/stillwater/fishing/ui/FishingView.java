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
    private final ArtRepository art;
    private final RectF sceneButton = new RectF(910, 24, 1030, 82);
    private final RectF codexButton = new RectF(1042, 24, 1162, 82);
    private final RectF helpButton = new RectF(1174, 24, 1256, 82);
    private final RectF fishButton = new RectF(1030, 548, 1200, 694);
    private final RectF closeButton = new RectF(1152, 70, 1210, 124);
    private Panel panel = Panel.NONE;
    private boolean foreground, windowFocused = true;
    private long clockMinute = Long.MIN_VALUE;
    private int clockHour;
    private String clockText = "";
    private boolean clockRaining;
    private String catchDimensions = "", catchMarks = "";
    private final List<Integer> codexEntries = new ArrayList<>();
    private boolean entriesDirty = true;
    private boolean codexDrag;
    private final RectF fishBody = new RectF(1043, 561, 1187, 681);
    private final RectF confirmSceneButton = new RectF(890, 555, 1135, 620);
    private final RectF continueButton = new RectF(140, 555, 365, 620);
    private final RectF returnCodexButton = new RectF(445, 590, 835, 635);
    private final RectF[] qualityButtons = new RectF[3], unlockButtons = new RectF[3];
    private static final String[] QUALITIES = {"全部品质", "普通", "闪光"};
    private static final String[] UNLOCKS = {"全部状态", "已解锁", "未解锁"};
    private static final String[] SCENE_DETAILS = {"荷叶 · 芦苇 · 木平台", "树影 · 流水 · 卵石岸", "远山 · 湖面 · 长栈桥", "浅水 · 石块 · 小瀑布", "木桩 · 渔船 · 远处灯", "岩石 · 浪花 · 潮池"};
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
        art = new ArtRepository(context);
        selectedScene = engine.getProgress().sceneIndex;
        for (int i = 0; i < 3; i++) {
            qualityButtons[i] = new RectF(68 + i * 138, 111, 194 + i * 138, 153);
            unlockButtons[i] = new RectF(540 + i * 138, 111, 666 + i * 138, 153);
        }
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
        clockMinute = Long.MIN_VALUE;
        if (foreground && windowFocused) postInvalidateOnAnimation();
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
        entriesDirty = true;
        catchDimensions = format(result.length) + " cm   ·   " + format(result.weight) + " g";
        StringBuilder marks = new StringBuilder();
        if (result.newDiscovery) marks.append("新发现  ");
        if (result.lengthRecord) marks.append("长度新纪录  ");
        if (result.weightRecord) marks.append("重量新纪录");
        if (result.collectionComplete) marks.append("  图鉴已集齐");
        catchMarks = marks.toString();
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
        long wallTime = System.currentTimeMillis();
        engine.tick(wallTime);
        updateClock(wallTime);
        scale = Math.min(getWidth() / W, getHeight() / H);
        offsetX = (getWidth() - W * scale) / 2;
        offsetY = (getHeight() - H * scale) / 2;
        canvas.drawColor(Color.rgb(12, 29, 35));
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);
        float time = (now % 100_000_000_000L) / 1_000_000_000f;
        art.drawScene(canvas, paint, engine.getProgress().sceneIndex,
                Catalog.period(clockHour), engine.getProgress().raining, time);
        art.drawAngler(canvas, paint, engine.getState(), time);
        drawHeader(canvas);
        drawFishingButton(canvas);
        drawMessage(canvas, now);
        if (panel == Panel.SCENES) drawScenes(canvas);
        else if (panel == Panel.CODEX) drawCodex(canvas);
        else if (panel == Panel.DETAIL) drawDetail(canvas);
        else if (panel == Panel.HELP) drawHelp(canvas);
        canvas.restore();
        if (foreground && windowFocused) postInvalidateOnAnimation();
    }

    private void updateClock(long now) {
        long minute = Math.floorDiv(now, 60_000L);
        boolean raining = engine.getProgress().raining;
        if (minute == clockMinute && raining == clockRaining) return;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        clockHour = calendar.get(Calendar.HOUR_OF_DAY);
        clockText = (raining ? "雨" : "晴") + "  " + Catalog.periodName(clockHour)
                + String.format(Locale.CHINA, "  %02d:%02d", clockHour, calendar.get(Calendar.MINUTE));
        clockMinute = minute;
        clockRaining = raining;
    }

    private void drawHeader(Canvas c) {
        panel(c, 20, 20, 338, 105, 0xE61A3138);
        text(c, "静水集", 40, 61, 32, 0xFFF8EFD7, true);
        text(c, Catalog.SCENES[engine.getProgress().sceneIndex] + "  ·  慢下来，等一尾鱼", 41, 89, 16, 0xFFB9D3C8, false);
        panel(c, 535, 25, 745, 80, 0xD9183138);
        text(c, clockText,
                640, 60, 20, 0xFFF5EDDA, true, Paint.Align.CENTER);
        button(c, sceneButton, "场景", false);
        button(c, codexButton, "图鉴 " + engine.getProgress().unlockedCount() + "/" + GameProgress.ENTRY_COUNT, false);
        button(c, helpButton, "?", false);
    }

    private void drawFishingButton(Canvas c) {
        boolean biting = engine.getState() == FishingEngine.State.BITING;
        if (biting) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8);
            paint.setColor(0x44312626);
            c.drawArc(fishButton, -90, 360, false, paint);
            paint.setColor(0xFFE49A55);
            c.drawArc(fishButton, -90, 360 * (float) engine.getBiteProgress(), false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        RectF body = fishBody;
        art.drawPanel(c, paint, body, biting ? "gold" : "slot");
        text(c, engine.getState() == FishingEngine.State.IDLE ? "抛竿" : "收竿",
                body.centerX(), body.centerY() + 12, 34, 0xFF19343A, true, Paint.Align.CENTER);
        String hint = engine.getState() == FishingEngine.State.IDLE ? "静候一处好水" :
                biting ? "就是现在" : "浮漂轻动，耐心等候";
        text(c, hint, 1115, 530, 17, biting ? 0xFFFFF0B2 : 0xFFF1F3DE, true, Paint.Align.CENTER);
    }

    private void drawMessage(Canvas c, long now) {
        if (now >= messageUntil) return;
        if (displayedCatch == null) {
            panel(c, 490, 100, 790, 156, 0xE7193034);
            text(c, escapeMessage, 640, 140, 23, 0xFFF2EADA, true, Paint.Align.CENTER);
            return;
        }
        CatchResult result = displayedCatch;
        panel(c, 390, 92, 890, 259, 0xF31B3439);
        art.drawSpecimen(c, paint, result.species, result.shiny, false, 475, 167, .85f);
        text(c, result.species.name, 560, 139, 30, 0xFFF9EFD9, true);
        text(c, result.qualityText(), 560, 169, 18, result.shiny ? 0xFFFFD477 : 0xFFB8D4C7, true);
        text(c, catchDimensions,
                560, 201, 19, 0xFFF0E9D8, false);
        text(c, catchMarks, 560, 235, 17, 0xFFF4C95D, true);
    }

    private void drawScenes(Canvas c) {
        shade(c);
        panel(c, 100, 65, 1180, 665, 0xF5EEE8DA);
        text(c, "选择一处水域", 140, 119, 32, 0xFF19343A, true);
        text(c, "切换场景会结束当前钓鱼；仅查看不会影响本轮", 140, 151, 17, 0xFF60746E, false);
        close(c);
        for (int i = 0; i < 6; i++) {
            float x = 140 + (i % 3) * 340;
            float y = 185 + (i / 3) * 170;
            boolean selected = selectedScene == i;
            art.drawPanel(c, paint, new RectF(x, y, x + 310, y + 140), selected ? "dark" : "slot");
            art.drawSceneThumbnail(c, paint, i, new RectF(x + 5, y + 5, x + 116, y + 135));
            text(c, Catalog.SCENES[i], x + 123, y + 58, 25, selected ? 0xFFFFF1D5 : 0xFF24433F, true);
            String detail = SCENE_DETAILS[i];
            text(c, detail, x + 123, y + 91, 15, selected ? 0xFFC6D8C9 : 0xFF657970, false);
            if (i == engine.getProgress().sceneIndex) text(c, "当前", x + 123, y + 119, 14, 0xFFD2A34C, true);
        }
        button(c, confirmSceneButton, "前往" + Catalog.SCENES[selectedScene], true);
        button(c, continueButton, "继续钓鱼", false);
    }

    private void drawCodex(Canvas c) {
        shade(c);
        panel(c, 38, 38, 1242, 692, 0xFAF3EBDD);
        text(c, "水岸图鉴", 68, 87, 31, 0xFF19343A, true);
        text(c, "已解锁  " + engine.getProgress().unlockedCount() + " / " + GameProgress.ENTRY_COUNT, 315, 85, 20, 0xFF597168, true);
        close(c);
        for (int i = 0; i < 3; i++) {
            button(c, qualityButtons[i], QUALITIES[i], qualityFilter == i);
            button(c, unlockButtons[i], UNLOCKS[i], unlockFilter == i);
        }
        List<Integer> entries = filteredEntries();
        codexScroll = CodexLayout.clampScroll(codexScroll, entries.size());
        int first = CodexLayout.firstVisible(codexScroll);
        int end = CodexLayout.endVisible(codexScroll, entries.size());
        c.save();
        c.clipRect(CodexLayout.LEFT, CodexLayout.TOP, CodexLayout.RIGHT,
                CodexLayout.TOP + CodexLayout.HEIGHT);
        for (int index = first; index < end; index++) {
            float x = CodexLayout.LEFT + (index % CodexLayout.COLUMNS) * CodexLayout.COLUMN_WIDTH;
            drawEntryCell(c, entries.get(index), x, CodexLayout.cellY(index, codexScroll));
        }
        c.restore();
        if (entries.isEmpty()) text(c, "没有符合筛选条件的条目", 640, 385, 22, 0xFF72867D, false, Paint.Align.CENTER);
        text(c, "上下滑动浏览 · 点击条目查看详情", 75, 677, 15, 0xFF718279, false);
        text(c, (entries.isEmpty() ? "0 / 0 项" : (first + 1) + "—" + end + " / " + entries.size() + " 项"),
                1165, 677, 15, 0xFF718279, false, Paint.Align.RIGHT);
    }

    private void drawEntryCell(Canvas c, int entryIndex, float x, float y) {
        Species species = Catalog.get(entryIndex / 2);
        boolean shiny = entryIndex % 2 == 1;
        EntryRecord record = engine.getProgress().entries[entryIndex];
        art.drawPanel(c, paint, new RectF(x, y, x + 276, y + 104), "slot");
        art.drawSpecimen(c, paint, species, shiny, !record.unlocked(), x + 54, y + 51, .48f);
        text(c, record.unlocked() ? species.name : "尚未发现", x + 108, y + 38, 20,
                record.unlocked() ? 0xFF25433E : 0xFF849087, true);
        text(c, shiny ? "闪光" : "普通", x + 108, y + 64, 14,
                shiny ? 0xFFAD792C : 0xFF71877D, true);
        text(c, "钓上 " + record.count + " 次", x + 108, y + 88, 14, 0xFF61776D, false);
    }

    private List<Integer> filteredEntries() {
        if (!entriesDirty) return codexEntries;
        codexEntries.clear();
        for (int i = 0; i < GameProgress.ENTRY_COUNT; i++) {
            if (qualityFilter != 0 && (i % 2 == 1 ? 2 : 1) != qualityFilter) continue;
            boolean unlocked = engine.getProgress().entries[i].unlocked();
            if (unlockFilter == 1 && !unlocked) continue;
            if (unlockFilter == 2 && unlocked) continue;
            codexEntries.add(i);
        }
        entriesDirty = false;
        return codexEntries;
    }

    private void drawDetail(Canvas c) {
        drawCodex(c);
        shade(c);
        panel(c, 180, 82, 1100, 650, 0xFFF6EFDF);
        Species species = Catalog.get(selectedEntry / 2);
        boolean shiny = selectedEntry % 2 == 1;
        EntryRecord record = engine.getProgress().entries[selectedEntry];
        text(c, "图鉴条目  " + String.format(Locale.CHINA, "%02d", selectedEntry + 1), 220, 129, 16, 0xFF718279, true);
        close(c);
        panel(c, 220, 160, 565, 515, 0xFFDDE6D8);
        art.drawSpecimen(c, paint, species, shiny, !record.unlocked(), 393, 330, 1.65f);
        text(c, shiny ? "闪光品质" : "普通品质", 393, 477, 20, shiny ? 0xFF9B702D : 0xFF4D6C61, true, Paint.Align.CENTER);
        text(c, record.unlocked() ? species.name : "尚未发现", 615, 201, 34, 0xFF203E39, true);
        text(c, "出现场景", 615, 259, 16, 0xFF7B887B, true);
        wrappedText(c, record.unlocked() ? Catalog.sceneText(species) : "???", 615, 288, 425, 20, 0xFF344F47);
        text(c, "出现时间", 615, 350, 16, 0xFF7B887B, true);
        wrappedText(c, record.unlocked() ? species.timeText() : "???", 615, 379, 425, 20, 0xFF344F47);
        text(c, "钓上次数", 615, 451, 16, 0xFF7B887B, true);
        text(c, Integer.toString(record.count), 615, 481, 23, 0xFF24433D, true);
        text(c, "长度记录", 615, 530, 16, 0xFF7B887B, true);
        text(c, record.unlocked() ? format(record.length) + " cm" : "—", 615, 560, 22, 0xFF24433D, true);
        text(c, "重量记录", 850, 530, 16, 0xFF7B887B, true);
        text(c, record.unlocked() ? format(record.weight) + " g" : "—", 850, 560, 22, 0xFF24433D, true);
        button(c, returnCodexButton, "返回图鉴", true);
    }

    private void drawHelp(Canvas c) {
        shade(c);
        panel(c, 250, 90, 1030, 640, 0xFAF1E8D9);
        text(c, "留一点时间给水", 295, 145, 32, 0xFF1F403B, true);
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
        for (int i = 0; i < lines.length; i++) text(c, lines[i], 300, 211 + i * 48, 20, 0xFF476159, false);
        text(c, "美术：PinkSamurai · onepixelmore · Kenney", 300, 566, 15, 0xFF708279, false);
        text(c, "Elthen · edermunizz｜授权说明已随游戏打包", 300, 594, 15, 0xFF839083, false);
    }

    private void close(Canvas c) {
        button(c, closeButton, "×", false);
    }

    private void shade(Canvas c) {
        paint.setColor(0x99101F25);
        c.drawRect(0, 0, W, H, paint);
    }

    private void panel(Canvas c, float l, float t, float r, float b, int color) {
        int red = (color >> 16) & 0xff, green = (color >> 8) & 0xff, blue = color & 0xff;
        String style = (red + green + blue) / 3 > 145 ? "slot" : "dark";
        art.drawPanel(c, paint, new RectF(l, t, r, b), style);
    }

    private void button(Canvas c, RectF rect, String label, boolean accent) {
        art.drawPanel(c, paint, rect, accent ? "gold" : "dark");
        float size = rect.width() < 90 ? 26 : rect.width() < 150 ? 16 : 19;
        text(c, label, rect.centerX(), rect.centerY() + size * .35f, size,
                accent ? 0xFF1A3637 : 0xFFF1EBD8, true, Paint.Align.CENTER);
    }

    private void text(Canvas c, String value, float x, float y, float size, int color, boolean bold) {
        text(c, value, x, y, size, color, bold, Paint.Align.LEFT);
    }

    private void text(Canvas c, String value, float x, float y, float size, int color, boolean bold, Paint.Align align) {
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = (event.getX() - offsetX) / scale;
        float y = (event.getY() - offsetY) / scale;
        if (panel == Panel.CODEX) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchY = y;
                startScroll = codexScroll;
                scrolling = false;
                codexDrag = CodexLayout.inViewport(x, y);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE && codexDrag && (scrolling || Math.abs(y - touchY) > 8)) {
                scrolling = true;
                codexScroll = CodexLayout.clampScroll(startScroll + touchY - y, filteredEntries().size());
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                scrolling = false;
                codexDrag = false;
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP && scrolling) {
                scrolling = false;
                codexDrag = false;
                return true;
            }
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
            if (closeButton.contains(x, y) || continueButton.contains(x, y)) {
                selectedScene = engine.getProgress().sceneIndex;
                openPanel(Panel.NONE);
            } else if (confirmSceneButton.contains(x, y)) {
                engine.changeScene(selectedScene);
                openPanel(Panel.NONE);
            } else {
                for (int i = 0; i < 6; i++) {
                    float left = 140 + (i % 3) * 340, top = 185 + (i / 3) * 170;
                    if (new RectF(left, top, left + 310, top + 140).contains(x, y)) selectedScene = i;
                }
            }
        } else if (panel == Panel.CODEX) {
            if (closeButton.contains(x, y)) {
                openPanel(Panel.NONE);
                return true;
            }
            for (int i = 0; i < 3; i++) {
                if (qualityButtons[i].contains(x, y)) {
                    qualityFilter = i;
                    entriesDirty = true;
                    codexScroll = 0;
                }
                if (unlockButtons[i].contains(x, y)) {
                    unlockFilter = i;
                    entriesDirty = true;
                    codexScroll = 0;
                }
            }
            List<Integer> entries = filteredEntries();
            int hit = CodexLayout.hitTest(x, y, codexScroll, entries.size());
            if (hit >= 0) {
                selectedEntry = entries.get(hit);
                openPanel(Panel.DETAIL);
            }
        } else if (panel == Panel.DETAIL) {
            if (closeButton.contains(x, y)) openPanel(Panel.NONE);
            else if (returnCodexButton.contains(x, y)) {
                panel = Panel.CODEX;
                syncPause();
            }
        }
        return true;
    }

    private void openPanel(Panel next) {
        panel = next;
        if (next == Panel.CODEX) entriesDirty = true;
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
