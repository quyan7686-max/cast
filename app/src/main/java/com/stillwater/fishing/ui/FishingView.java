package com.stillwater.fishing.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import com.stillwater.fishing.R;
import com.stillwater.fishing.data.Catalog;
import com.stillwater.fishing.data.CatchResult;
import com.stillwater.fishing.data.EntryRecord;
import com.stillwater.fishing.data.GameProgress;
import com.stillwater.fishing.data.Species;
import com.stillwater.fishing.engine.FishingEngine;
import com.stillwater.fishing.ui.FishingUiState.Panel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class FishingView extends View {
    private static final float W = Viewport.LOGICAL_WIDTH, H = Viewport.LOGICAL_HEIGHT;
    /** Stable virtual ids shared by hit dispatch and the accessibility node tree. */
    public static final int ID_CAST = 1, ID_SCENES = 2, ID_CODEX = 3, ID_HELP = 4,
            ID_CLOSE = 5, ID_CONFIRM_SCENE = 6, ID_CONTINUE = 7, ID_RETURN_CODEX = 8,
            ID_QUALITY = 10, ID_UNLOCK = 20, ID_SCENE = 30, ID_ENTRY = 100,
            ID_DETAIL_TEXT = 900, ID_HELP_TEXT = 901;
    private static final float MIN_TOUCH_DP = 48;
    private final FishingEngine engine;
    private final FishingAccessibility accessibility = new FishingAccessibility(this);
    private final FishingUiState uiState = new FishingUiState();
    private final Viewport viewport = new Viewport();
    private final Paint paint = new Paint();
    private final ArtRepository art;
    private final VisualMotion motion = new VisualMotion();
    private final Bitmap worldFrame = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888);
    private final Canvas worldCanvas = new Canvas(worldFrame);
    private final Rect worldDestination = new Rect(0, 0, 1280, 720);
    private final RectF panelBounds = new RectF();
    private final RectF[] sceneCards = new RectF[6];
    private final RectF thumbnailBounds = new RectF();
    private RectF pressedButton;
    private boolean pointerDown;
    private float pointerX, pointerY;
    private double panelStarted, messageStarted = -100, sceneStarted = -100;
    private boolean panelClosing;
    private Panel closingPanel = Panel.NONE;
    private int previousScene = -1;
    private FishingEngine.State drawnState = FishingEngine.State.IDLE;
    private final RectF sceneButton = new RectF(910, 24, 1030, 82);
    private final RectF codexButton = new RectF(1042, 24, 1162, 82);
    private final RectF helpButton = new RectF(1174, 24, 1256, 82);
    private final RectF fishButton = new RectF(1026, 570, 1238, 698);
    private final RectF closeButton = new RectF(1152, 70, 1210, 124);
    private boolean foreground, windowFocused = true;
    private boolean rotatePrompt;
    private long clockMinute = Long.MIN_VALUE;
    private int clockHour;
    private String clockText = "";
    private boolean clockRaining;
    private int unlockedCount;
    private String codexLabel = "";
    private Bitmap codexCache;
    private final Canvas codexCanvas = new Canvas();
    private boolean codexDirty = true;
    private String catchDimensions = "", catchMarks = "";
    private final List<Integer> codexEntries = new ArrayList<>();
    private boolean entriesDirty = true;
    private boolean codexDrag;
    private final RectF fishBody = new RectF(1038, 584, 1226, 684);
    private final RectF confirmSceneButton = new RectF(890, 555, 1135, 620);
    private final RectF continueButton = new RectF(140, 555, 365, 620);
    private final RectF returnCodexButton = new RectF(445, 590, 835, 635);
    private final RectF[] qualityButtons = new RectF[3], unlockButtons = new RectF[3];
    private final String[] qualities;
    private final String[] unlocks;
    private final String[] sceneDetails;
    private CatchResult displayedCatch;
    private String escapeMessage = "";
    private int selectedScene;
    private float codexScroll, touchY, startScroll;
    private boolean scrolling;
    private float minTouch = 96;
    private ToneGenerator tone;

    public FishingView(Context context, FishingEngine engine) {
        super(context);
        this.engine = engine;
        art = new ArtRepository(context);
        qualities = getResources().getStringArray(R.array.quality_filters);
        unlocks = getResources().getStringArray(R.array.unlock_filters);
        sceneDetails = getResources().getStringArray(R.array.scene_details);
        selectedScene = engine.getProgress().sceneIndex;
        refreshUnlocks();
        for (int i = 0; i < 3; i++) {
            qualityButtons[i] = new RectF(68 + i * 138, 111, 194 + i * 138, 153);
            unlockButtons[i] = new RectF(540 + i * 138, 111, 666 + i * 138, 153);
        }
        for (int i = 0; i < sceneCards.length; i++) {
            float x = 140 + (i % 3) * 340, y = 185 + (i / 3) * 170;
            sceneCards[i] = new RectF(x, y, x + 310, y + 140);
        }
        setFocusable(true);
        updateContentDescription(Panel.NONE);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeJoin(Paint.Join.MITER);
        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
        } catch (RuntimeException ignored) {
            tone = null;
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        viewport.update(width, height);
        // Translate the 48dp minimum target back into logical scene units for the current scale.
        minTouch = MIN_TOUCH_DP * getResources().getDisplayMetrics().density
                / Math.max(viewport.getScale(), .001f);
        boolean taller = Viewport.tooNarrow(width, height);
        if (taller != rotatePrompt) {
            rotatePrompt = taller;
            updateContentDescription(panel());
            accessibility.rebuild();
            syncPause();
        }
    }

    public void setForeground(boolean foreground) {
        this.foreground = foreground;
        syncPause();
    }

    /** Unlock totals only change on a catch, so the HUD must not recount 84 entries every frame. */
    private void refreshUnlocks() {
        unlockedCount = engine.getProgress().unlockedCount();
        codexLabel = getResources().getString(R.string.action_codex,
                unlockedCount, GameProgress.ENTRY_COUNT);
    }

    private void syncPause() {
        if (!foreground || !windowFocused) {
            pointerDown = false;
            pressedButton = null;
        }
        engine.setPaused(!foreground || !windowFocused || panel() != Panel.NONE || rotatePrompt);
        clockMinute = Long.MIN_VALUE;
        motion.resetFrame(System.nanoTime());
        if (foreground && windowFocused && !rotatePrompt) postInvalidateOnAnimation();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        windowFocused = hasFocus;
        syncPause();
    }

    public void playBiteSound() {
        motion.bite();
        if (tone != null) tone.startTone(ToneGenerator.TONE_PROP_ACK, 140);
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    public void showCatch(CatchResult result) {
        displayedCatch = result;
        motion.caught();
        messageStarted = motion.uiSeconds;
        entriesDirty = true;
        catchDimensions = format(result.length) + " cm   ·   " + format(result.weight) + " g";
        StringBuilder marks = new StringBuilder();
        if (result.newDiscovery) marks.append(getResources().getString(R.string.mark_discovery)).append("  ");
        if (result.lengthRecord) marks.append(getResources().getString(R.string.mark_length_record)).append("  ");
        if (result.weightRecord) marks.append(getResources().getString(R.string.mark_weight_record));
        if (result.collectionComplete) marks.append("  ").append(getResources().getString(R.string.mark_collection_complete));
        catchMarks = marks.toString();
        escapeMessage = "";
        refreshUnlocks();
        accessibility.rebuild();
    }

    public void showEscape() {
        displayedCatch = null;
        messageStarted = motion.uiSeconds;
        escapeMessage = getResources().getString(R.string.message_escaped);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        motion.frame(now, foreground && windowFocused, engine.isPaused());
        long wallTime = System.currentTimeMillis();
        engine.tick(wallTime);
        updateClock(wallTime);
        observeFishingState();
        if (panelClosing && VisualMotion.progress(motion.uiSeconds, panelStarted, .16) >= 1) {
            closingPanel = Panel.NONE;
            panelClosing = false;
            releaseCodexCache();
            syncPause();
        }
        viewport.update(getWidth(), getHeight());
        canvas.drawColor(Color.rgb(12, 29, 35));
        if (rotatePrompt) {
            drawRotatePrompt(canvas);
            return;
        }
        canvas.save();
        canvas.translate(viewport.getOffsetX(), viewport.getOffsetY());
        canvas.scale(viewport.getScale(), viewport.getScale());
        // Rasterize every world layer together before nearest-neighbor display scaling.
        worldCanvas.save();
        worldCanvas.scale(.25f, .25f);
        drawWorld(worldCanvas);
        worldCanvas.restore();
        paint.setColor(Color.WHITE);
        paint.setAlpha(255);
        paint.setFilterBitmap(false);
        paint.setColorFilter(null);
        canvas.drawBitmap(worldFrame, null, worldDestination, paint);
        drawHeader(canvas);
        drawFishingButton(canvas);
        drawMessage(canvas);
        if (panel() != Panel.NONE) {
            float visibility = panelVisibility();
            paint.setColor(Color.argb((int) (153 * visibility), 16, 31, 37));
            canvas.drawRect(0, 0, W, H, paint);
            // Once the panel is fully opaque the full-screen offscreen layer is pure overhead.
            // restoreToCount also unwinds the optional layer opened only during the fade.
            int checkpoint = canvas.save();
            if (visibility < .999f) {
                canvas.saveLayerAlpha(0, 0, W, H, Math.round(255 * visibility));
            }
            canvas.translate(0, panelOffset());
            if (panel() == Panel.SCENES) drawScenes(canvas);
            else if (panel() == Panel.CODEX) drawCodex(canvas);
            else if (panel() == Panel.DETAIL) drawDetail(canvas);
            else if (panel() == Panel.HELP) drawHelp(canvas);
            canvas.restoreToCount(checkpoint);
        }
        canvas.restore();
        // Sprite poses run at 4-12 fps. A 30 fps scene avoids continuously saturating
        // the UI queue while preserving responsive input and time-based deadlines.
        if (foreground && windowFocused && !rotatePrompt) postInvalidateDelayed(33);
    }

    /**
     * Shown when the window is taller than it is wide, which happens once Android 16 ignores the
     * landscape lock on large screens. Drawn in raw view pixels because the logical scene is too
     * narrow to stay legible.
     */
    private void drawRotatePrompt(Canvas c) {
        float width = getWidth(), height = getHeight();
        float unit = Math.min(width, height);
        float panelWidth = Math.min(width * .82f, unit * 1.25f);
        float panelHeight = unit * .40f;
        RectF bounds = new RectF((width - panelWidth) / 2, (height - panelHeight) / 2,
                (width + panelWidth) / 2, (height + panelHeight) / 2);
        art.drawPanel(c, paint, bounds, "dark");
        paint.setColor(0xFFE5BA69);
        c.drawRect(width / 2 - unit * .04f, bounds.top + unit * .085f,
                width / 2 + unit * .04f, bounds.top + unit * .085f + Math.max(3, unit * .003f), paint);
        text(c, getResources().getString(R.string.rotate_title), width / 2,
                bounds.top + unit * .20f, unit * .055f, 0xFFF9EFD9, true, Paint.Align.CENTER);
        text(c, getResources().getString(R.string.rotate_hint), width / 2,
                bounds.top + unit * .275f, unit * .032f, 0xFFB9D3C8, false, Paint.Align.CENTER);
        text(c, getResources().getString(R.string.rotate_note), width / 2,
                bounds.top + unit * .345f, unit * .028f, 0xFF7F9A90, false, Paint.Align.CENTER);
    }

    private void observeFishingState() {
        FishingEngine.State current = engine.getState();
        if (current == FishingEngine.State.WAITING && drawnState == FishingEngine.State.IDLE) motion.cast();
        if (current == FishingEngine.State.IDLE && drawnState != FishingEngine.State.IDLE) motion.reel();
        if (drawnState != current) accessibility.rebuild();
        drawnState = current;
    }

    private void drawWorld(Canvas canvas) {
        int scene = engine.getProgress().sceneIndex;
        int period = Catalog.period(clockHour);
        boolean rain = engine.getProgress().raining;
        float blend = VisualMotion.easeOut(VisualMotion.progress(motion.uiSeconds, sceneStarted, .45));
        if (previousScene >= 0 && blend < 1) {
            art.drawScene(canvas, paint, previousScene, period, rain, motion.waterSeconds);
            int layer = canvas.saveLayerAlpha(0, 0, W, H, Math.round(blend * 255));
            art.drawScene(canvas, paint, scene, period, rain, motion.waterSeconds);
            canvas.restoreToCount(layer);
        } else {
            art.drawScene(canvas, paint, scene, period, rain, motion.waterSeconds);
        }
        art.drawAngler(canvas, paint, scene, engine.getState(), motion.waterSeconds,
                motion.waterSeconds - motion.castAt, motion.waterSeconds - motion.biteAt,
                motion.waterSeconds - motion.catchAt, motion.waterSeconds - motion.reelAt);
        float catchProgress = VisualMotion.progress(motion.waterSeconds, motion.catchAt, .55);
        if (displayedCatch != null && catchProgress < 1) {
            float p = VisualMotion.easeOut(catchProgress);
            float x = (scene == 0 ? PondArt.FLOAT_X : ArtRepository.FLOAT_X) - (scene == 0 ? PondArt.FLOAT_X - 416 : 325) * p;
            float y = (scene == 0 ? PondArt.FLOAT_Y : ArtRepository.FLOAT_Y) - (scene == 0 ? PondArt.FLOAT_Y - 388 : 115) * p - 90 * (float) Math.sin(p * Math.PI);
            int layer = canvas.saveLayerAlpha(330, 270, 800, 600, Math.round(255 * (1 - catchProgress)));
            canvas.rotate(-16 * p, x, y);
            art.drawSpecimen(canvas, paint, displayedCatch.species, displayedCatch.shiny, false, x, y, .65f);
            canvas.restoreToCount(layer);
        }
    }

    private Panel panel() {
        return panelClosing ? closingPanel : uiState.getPanel();
    }

    private float panelVisibility() {
        float p = VisualMotion.progress(motion.uiSeconds, panelStarted, panelClosing ? .16 : .22);
        return panelClosing ? 1 - VisualMotion.easeOut(p) : VisualMotion.easeOut(p);
    }

    private float panelOffset() { return 14 * (1 - panelVisibility()); }

    private void updateClock(long now) {
        long minute = Math.floorDiv(now, 60_000L);
        boolean raining = engine.getProgress().raining;
        if (minute == clockMinute && raining == clockRaining) return;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        clockHour = calendar.get(Calendar.HOUR_OF_DAY);
        clockText = (raining ? getResources().getString(R.string.weather_rain)
                : getResources().getString(R.string.weather_clear)) + "  " + Catalog.periodName(clockHour)
                + String.format(Locale.CHINA, "  %02d:%02d", clockHour, calendar.get(Calendar.MINUTE));
        clockMinute = minute;
        clockRaining = raining;
    }

    private void drawHeader(Canvas c) {
        panel(c, 20, 20, 264, 112, 0xFF193544);
        text(c, getResources().getString(R.string.app_name), 142, 63, 34, 0xFFFFE8AF, true, Paint.Align.CENTER);
        text(c, "·  " + Catalog.SCENES[engine.getProgress().sceneIndex] + "  ·",
                142, 94, 19, 0xFFD4C995, true, Paint.Align.CENTER);
        panel(c, 24, 644, 286, 696, 0xFF193544);
        text(c, clockText, 155, 677, 19, 0xFFEFDEAF, true, Paint.Align.CENTER);
        button(c, sceneButton, getResources().getString(R.string.action_scenes), false);
        button(c, codexButton, codexLabel, false);
        button(c, helpButton, getResources().getString(R.string.action_help), false);
    }

    private void drawFishingButton(Canvas c) {
        boolean biting = engine.getState() == FishingEngine.State.BITING;
        if (biting) {
            float pulse = .5f + .5f * (float) Math.sin((motion.waterSeconds - motion.biteAt) * 8);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(Color.argb(45 + (int) (70 * pulse), 249, 211, 125));
            float spread = 4 + pulse * 8;
            c.drawOval(fishButton.left - spread, fishButton.top - spread,
                    fishButton.right + spread, fishButton.bottom + spread, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8);
            paint.setColor(0x44312626);
            c.drawArc(fishButton, -90, 360, false, paint);
            paint.setColor(0xFFE49A55);
            c.drawArc(fishButton, -90, 360 * (float) engine.getBiteProgress(), false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        c.save();
        float press = isPressed(fishButton) ? .95f : 1;
        c.scale(press, press, fishBody.centerX(), fishBody.centerY());
        if (isPressed(fishButton)) c.translate(0, 3);
        RectF body = fishBody;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x44202E2F);
        c.drawRect(body.left + 3, body.top + 6, body.right + 3, body.bottom + 6, paint);
        art.drawPanel(c, paint, body, "gold");
        text(c, getResources().getString(engine.getState() == FishingEngine.State.IDLE
                        ? R.string.action_cast : R.string.action_reel),
                body.centerX(), body.centerY() + 12, 34, 0xFF19343A, true, Paint.Align.CENTER);
        c.restore();
        String hint = getResources().getString(engine.getState() == FishingEngine.State.IDLE
                ? R.string.hint_idle : biting ? R.string.hint_biting : R.string.hint_waiting);
        text(c, hint, fishBody.centerX(), 554, 17, biting ? 0xFFFFF0B2 : 0xFFF1F3DE, true, Paint.Align.CENTER);
    }

    private void drawMessage(Canvas c) {
        float progress = VisualMotion.progress(motion.uiSeconds, messageStarted, 1);
        if (progress >= 1) return;
        float opacity = VisualMotion.envelope(progress, .12f, .18f);
        int layer = c.saveLayerAlpha(360, 70, 920, 285, Math.round(opacity * 255));
        float lift = 12 * (1 - VisualMotion.easeOut(Math.min(1, progress / .18f)));
        c.translate(0, lift - 8 * Math.max(0, (progress - .82f) / .18f));
        drawMessageContent(c);
        c.restoreToCount(layer);
    }

    private void drawMessageContent(Canvas c) {
        if (displayedCatch == null) {
            panel(c, 490, 100, 790, 156, 0xE7193034);
            text(c, escapeMessage, 640, 140, 23, 0xFFF2EADA, true, Paint.Align.CENTER);
            return;
        }
        CatchResult result = displayedCatch;
        panel(c, 390, 92, 890, 259, 0xF31B3439);
        paint.setColor(result.shiny ? 0x665D5739 : 0x66496359);
        c.drawRect(419, 111, 531, 223, paint);
        art.drawSpecimen(c, paint, result.species, result.shiny, false, 475, 167, .85f);
        if (result.shiny || result.newDiscovery) art.drawSparkles(c, paint, 475, 167, 52, motion.uiSeconds);
        text(c, result.species.name, 560, 139, 30, 0xFFF9EFD9, true);
        text(c, result.qualityText(), 560, 169, 18, result.shiny ? 0xFFFFD477 : 0xFFB8D4C7, true);
        text(c, catchDimensions,
                560, 201, 19, 0xFFF0E9D8, false);
        text(c, catchMarks, 560, 235, 17, 0xFFF4C95D, true);
    }

    private void drawScenes(Canvas c) {
        panel(c, 100, 65, 1180, 665, 0xF5EEE8DA);
        text(c, getResources().getString(R.string.scene_picker_title), 140, 119, 32, 0xFF19343A, true);
        text(c, getResources().getString(R.string.scene_picker_description), 140, 151, 17, 0xFF60746E, false);
        close(c);
        for (int i = 0; i < 6; i++) {
            float x = 140 + (i % 3) * 340;
            float y = 185 + (i / 3) * 170;
            boolean selected = selectedScene == i;
            art.drawPanel(c, paint, sceneCards[i], selected ? "dark" : "slot");
            thumbnailBounds.set(x + 7, y + 7, x + 116, y + 133);
            art.drawSceneThumbnail(c, paint, i, thumbnailBounds);
            if (selected) {
                paint.setColor(0xFFE5BA69);
                c.drawRect(x + 123, y + 10, x + 181, y + 13, paint);
            }
            text(c, Catalog.SCENES[i], x + 123, y + 58, 25, selected ? 0xFFFFF1D5 : 0xFF24433F, true);
            String detail = sceneDetails[i];
            text(c, detail, x + 123, y + 91, 15, selected ? 0xFFC6D8C9 : 0xFF657970, false);
            if (i == engine.getProgress().sceneIndex) text(c, getResources().getString(R.string.scene_current), x + 123, y + 119, 14, 0xFFD2A34C, true);
        }
        button(c, confirmSceneButton, getResources().getString(R.string.action_go_scene,
                Catalog.SCENES[selectedScene]), true);
        button(c, continueButton, getResources().getString(R.string.action_continue_fishing), false);
    }

    private void drawCodex(Canvas c) {
        panel(c, 38, 38, 1242, 692, 0xFAF3EBDD);
        text(c, getResources().getString(R.string.codex_title), 68, 87, 31, 0xFF19343A, true);
        text(c, getResources().getString(R.string.codex_unlocked,
                unlockedCount, GameProgress.ENTRY_COUNT), 315, 85, 20, 0xFF597168, true);
        close(c);
        paint.setColor(0xFFD2D8C6);
        c.drawRect(315, 96, 535, 100, paint);
        paint.setColor(0xFF6D9279);
        c.drawRect(315, 96, 315 + 220f * unlockedCount / GameProgress.ENTRY_COUNT, 100, paint);
        for (int i = 0; i < 3; i++) {
            button(c, qualityButtons[i], qualities[i], uiState.getQualityFilter() == i);
            button(c, unlockButtons[i], unlocks[i], uiState.getUnlockFilter() == i);
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
        float contentHeight = ((entries.size() + 3) / 4) * CodexLayout.ROW_HEIGHT;
        if (contentHeight > CodexLayout.HEIGHT) {
            float thumbHeight = CodexLayout.HEIGHT * CodexLayout.HEIGHT / contentHeight;
            float thumbTop = CodexLayout.TOP + codexScroll / (contentHeight - CodexLayout.HEIGHT)
                    * (CodexLayout.HEIGHT - thumbHeight);
            paint.setColor(0xFFD4D9C9);
            c.drawRect(1229, CodexLayout.TOP, 1233, CodexLayout.TOP + CodexLayout.HEIGHT, paint);
            paint.setColor(0xFF789582);
            c.drawRect(1229, thumbTop, 1233, thumbTop + thumbHeight, paint);
        }
        if (entries.isEmpty()) text(c, getResources().getString(R.string.codex_empty), 640, 385, 22, 0xFF72867D, false, Paint.Align.CENTER);
        text(c, getResources().getString(R.string.codex_scroll_hint), 75, 677, 15, 0xFF718279, false);
        text(c, entries.isEmpty() ? getResources().getString(R.string.codex_empty_range)
                        : getResources().getString(R.string.codex_range, first + 1, end, entries.size()),
                1165, 677, 15, 0xFF718279, false, Paint.Align.RIGHT);
    }

    private void drawEntryCell(Canvas c, int entryIndex, float x, float y) {
        Species species = Catalog.get(entryIndex / 2);
        boolean shiny = entryIndex % 2 == 1;
        EntryRecord record = engine.getProgress().entries[entryIndex];
        panelBounds.set(x, y, x + 276, y + 104);
        art.drawPanel(c, paint, panelBounds, "slot");
        if (record.unlocked() && shiny) {
            paint.setColor(0xFFD2AA5F);
            c.drawRect(x + 8, y + 16, x + 11, y + 88, paint);
        }
        art.drawSpecimen(c, paint, species, shiny, !record.unlocked(), x + 54, y + 51, .48f);
        text(c, record.unlocked() ? species.name : getResources().getString(R.string.entry_undiscovered), x + 108, y + 38, 20,
                record.unlocked() ? 0xFF25433E : 0xFF849087, true);
        text(c, getResources().getString(shiny ? R.string.quality_shiny : R.string.quality_normal), x + 108, y + 64, 14,
                shiny ? 0xFFAD792C : 0xFF71877D, true);
        text(c, getResources().getString(R.string.entry_catch_count, record.count), x + 108, y + 88, 14, 0xFF61776D, false);
    }

    private List<Integer> filteredEntries() {
        if (!entriesDirty) return codexEntries;
        codexEntries.clear();
        for (int i = 0; i < GameProgress.ENTRY_COUNT; i++) {
            if (uiState.getQualityFilter() != 0
                    && (i % 2 == 1 ? 2 : 1) != uiState.getQualityFilter()) continue;
            boolean unlocked = engine.getProgress().entries[i].unlocked();
            if (uiState.getUnlockFilter() == 1 && !unlocked) continue;
            if (uiState.getUnlockFilter() == 2 && unlocked) continue;
            codexEntries.add(i);
        }
        entriesDirty = false;
        return codexEntries;
    }

    /**
     * The detail panel covers the codex with a translucent shade, so the grid behind it only needs
     * to be rasterised once per entry instead of on every frame.
     */
    private void drawCodexCached(Canvas c) {
        if (codexCache == null || codexDirty) {
            if (codexCache == null) {
                codexCache = Bitmap.createBitmap((int) W, (int) H, Bitmap.Config.ARGB_8888);
                codexCanvas.setBitmap(codexCache);
            }
            codexCache.eraseColor(Color.TRANSPARENT);
            drawCodex(codexCanvas);
            codexDirty = false;
        }
        c.drawBitmap(codexCache, 0, 0, null);
    }

    private void releaseCodexCache() {
        if (codexCache == null) return;
        codexCache = null;
        codexCanvas.setBitmap(null);
    }

    private void drawDetail(Canvas c) {
        drawCodexCached(c);
        shade(c);
        panel(c, 180, 82, 1100, 650, 0xFFF6EFDF);
        int selectedEntry = uiState.getSelectedEntry();
        Species species = Catalog.get(selectedEntry / 2);
        boolean shiny = selectedEntry % 2 == 1;
        EntryRecord record = engine.getProgress().entries[selectedEntry];
        text(c, getResources().getString(R.string.entry_number, selectedEntry + 1), 220, 129, 16, 0xFF718279, true);
        close(c);
        panel(c, 220, 160, 565, 515, 0xFFDDE6D8);
        float floatY = record.unlocked() ? (float) Math.sin(motion.uiSeconds * 1.8) * 3 : 0;
        art.drawSpecimen(c, paint, species, shiny, !record.unlocked(), 393, 330 + floatY, 1.65f);
        if (shiny && record.unlocked()) art.drawSparkles(c, paint, 393, 330, 105, motion.uiSeconds);
        text(c, getResources().getString(shiny ? R.string.quality_shiny_full : R.string.quality_normal_full), 393, 477, 20, shiny ? 0xFF9B702D : 0xFF4D6C61, true, Paint.Align.CENTER);
        text(c, record.unlocked() ? species.name : getResources().getString(R.string.entry_undiscovered), 615, 201, 34, 0xFF203E39, true);
        text(c, getResources().getString(R.string.entry_scenes), 615, 259, 16, 0xFF7B887B, true);
        wrappedText(c, record.unlocked() ? Catalog.sceneText(species) : "???", 615, 288, 425, 20, 0xFF344F47);
        text(c, getResources().getString(R.string.entry_time), 615, 350, 16, 0xFF7B887B, true);
        wrappedText(c, record.unlocked() ? species.timeText() : "???", 615, 379, 425, 20, 0xFF344F47);
        text(c, getResources().getString(R.string.entry_count), 615, 451, 16, 0xFF7B887B, true);
        text(c, Integer.toString(record.count), 615, 481, 23, 0xFF24433D, true);
        text(c, getResources().getString(R.string.entry_length), 615, 530, 16, 0xFF7B887B, true);
        text(c, record.unlocked() ? format(record.length) + " cm" : "—", 615, 560, 22, 0xFF24433D, true);
        text(c, getResources().getString(R.string.entry_weight), 850, 530, 16, 0xFF7B887B, true);
        text(c, record.unlocked() ? format(record.weight) + " g" : "—", 850, 560, 22, 0xFF24433D, true);
        button(c, returnCodexButton, getResources().getString(R.string.action_return_codex), true);
    }

    private void drawHelp(Canvas c) {
        panel(c, 250, 90, 1030, 640, 0xFAF1E8D9);
        text(c, getResources().getString(R.string.help_title), 295, 145, 32, 0xFF1F403B, true);
        close(c);
        String[] lines = getResources().getStringArray(R.array.help_lines);
        for (int i = 0; i < lines.length; i++) text(c, lines[i], 300, 211 + i * 48, 20, 0xFF476159, false);
        text(c, getResources().getString(R.string.art_credit_primary), 300, 566, 15, 0xFF708279, false);
        text(c, getResources().getString(R.string.art_credit_secondary), 300, 594, 15, 0xFF839083, false);
    }

    private void close(Canvas c) {
        button(c, closeButton, getResources().getString(R.string.action_close), false);
    }

    private void shade(Canvas c) {
        paint.setColor(0x99101F25);
        c.drawRect(0, 0, W, H, paint);
    }

    private void panel(Canvas c, float l, float t, float r, float b, int color) {
        int red = (color >> 16) & 0xff, green = (color >> 8) & 0xff, blue = color & 0xff;
        String style = (red + green + blue) / 3 > 145 ? "slot" : "dark";
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x33101F25);
        c.drawRect(l + 4, t + 6, r + 4, b + 6, paint);
        panelBounds.set(l, t, r, b);
        art.drawPanel(c, paint, panelBounds, style);
    }

    private void button(Canvas c, RectF rect, String label, boolean accent) {
        c.save();
        if (isPressed(rect)) {
            c.scale(.97f, .97f, rect.centerX(), rect.centerY());
            c.translate(0, 2);
        }
        art.drawPanel(c, paint, rect, accent ? "gold" : "dark");
        float size = rect.width() < 90 ? 26 : rect.width() < 150 ? 16 : 19;
        text(c, label, rect.centerX(), rect.centerY() + size * .35f, size,
                accent ? 0xFF1A3637 : 0xFFF1EBD8, true, Paint.Align.CENTER);
        c.restore();
    }

    private boolean isPressed(RectF rect) {
        return pointerDown && pressedButton == rect && hit(rect, pointerX, pointerY);
    }

    /** Expands small controls to the 48dp minimum without moving anything on screen. */
    private boolean hit(RectF rect, float x, float y) {
        float dx = Math.max(0, (minTouch - rect.width()) / 2);
        float dy = Math.max(0, (minTouch - rect.height()) / 2);
        return x >= rect.left - dx && x <= rect.right + dx
                && y >= rect.top - dy && y <= rect.bottom + dy;
    }

    /** Single source of truth for hit testing, shared by press feedback and release dispatch. */
    private RectF buttonAt(float x, float y) {
        if (panel() == Panel.NONE) {
            if (hit(fishButton, x, y)) return fishButton;
            if (hit(sceneButton, x, y)) return sceneButton;
            if (hit(codexButton, x, y)) return codexButton;
            if (hit(helpButton, x, y)) return helpButton;
            return null;
        }
        if (hit(closeButton, x, y)) return closeButton;
        if (panel() == Panel.SCENES) {
            if (hit(confirmSceneButton, x, y)) return confirmSceneButton;
            if (hit(continueButton, x, y)) return continueButton;
            for (RectF card : sceneCards) {
                if (hit(card, x, y)) return card;
            }
        } else if (panel() == Panel.CODEX) {
            for (int i = 0; i < 3; i++) {
                if (hit(qualityButtons[i], x, y)) return qualityButtons[i];
                if (hit(unlockButtons[i], x, y)) return unlockButtons[i];
            }
        } else if (panel() == Panel.DETAIL && hit(returnCodexButton, x, y)) {
            return returnCodexButton;
        }
        return null;
    }

    private int idFor(RectF rect) {
        if (rect == fishButton) return ID_CAST;
        if (rect == sceneButton) return ID_SCENES;
        if (rect == codexButton) return ID_CODEX;
        if (rect == helpButton) return ID_HELP;
        if (rect == closeButton) return ID_CLOSE;
        if (rect == confirmSceneButton) return ID_CONFIRM_SCENE;
        if (rect == continueButton) return ID_CONTINUE;
        if (rect == returnCodexButton) return ID_RETURN_CODEX;
        for (int i = 0; i < qualityButtons.length; i++) {
            if (rect == qualityButtons[i]) return ID_QUALITY + i;
        }
        for (int i = 0; i < unlockButtons.length; i++) {
            if (rect == unlockButtons[i]) return ID_UNLOCK + i;
        }
        for (int i = 0; i < sceneCards.length; i++) {
            if (rect == sceneCards[i]) return ID_SCENE + i;
        }
        return 0;
    }

    /** Runs the action behind a control id, shared by touch and TalkBack activation. */
    void activate(int id) {
        Panel current = panel();
        if (current == Panel.NONE) {
            if (id == ID_CAST) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                engine.press(System.currentTimeMillis());
                observeFishingState();
                invalidate();
            } else if (id == ID_SCENES) openPanel(Panel.SCENES);
            else if (id == ID_CODEX) openPanel(Panel.CODEX);
            else if (id == ID_HELP) openPanel(Panel.HELP);
            return;
        }
        if (id == ID_CLOSE) {
            openPanel(Panel.NONE);
            return;
        }
        if (current == Panel.SCENES) {
            if (id == ID_CONTINUE) {
                selectedScene = engine.getProgress().sceneIndex;
                openPanel(Panel.NONE);
            } else if (id == ID_CONFIRM_SCENE) {
                if (selectedScene != engine.getProgress().sceneIndex) {
                    previousScene = engine.getProgress().sceneIndex;
                    sceneStarted = motion.uiSeconds;
                    engine.changeScene(selectedScene);
                }
                openPanel(Panel.NONE);
            } else if (id >= ID_SCENE && id < ID_SCENE + sceneCards.length) {
                selectedScene = id - ID_SCENE;
            }
        } else if (current == Panel.CODEX) {
            if (id >= ID_QUALITY && id < ID_QUALITY + 3) {
                uiState.setQualityFilter(id - ID_QUALITY);
                entriesDirty = true;
                codexScroll = 0;
            } else if (id >= ID_UNLOCK && id < ID_UNLOCK + 3) {
                uiState.setUnlockFilter(id - ID_UNLOCK);
                entriesDirty = true;
                codexScroll = 0;
            } else if (id >= ID_ENTRY && id < ID_ENTRY + GameProgress.ENTRY_COUNT) {
                uiState.setSelectedEntry(id - ID_ENTRY);
                openPanel(Panel.DETAIL);
            }
        } else if (current == Panel.DETAIL && id == ID_RETURN_CODEX) {
            openPanel(Panel.CODEX);
        }
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return accessibility;
    }

    /** The system reads the host node from here, so the virtual controls must be attached here. */
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        for (FishingAccessibility.Item item : accessibility.items()) {
            info.addChild(this, item.id);
        }
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        return accessibility.dispatchHover(event) || super.dispatchHoverEvent(event);
    }

    float logicalX(float viewX) {
        return viewport.toLogicalX(viewX);
    }

    float logicalY(float viewY) {
        return viewport.toLogicalY(viewY);
    }

    Rect screenBoundsOf(RectF logical) {
        int[] location = new int[2];
        getLocationOnScreen(location);
        float scale = viewport.getScale();
        return new Rect(
                Math.round(location[0] + viewport.getOffsetX() + logical.left * scale),
                Math.round(location[1] + viewport.getOffsetY() + logical.top * scale),
                Math.round(location[0] + viewport.getOffsetX() + logical.right * scale),
                Math.round(location[1] + viewport.getOffsetY() + logical.bottom * scale));
    }

    int itemAt(float x, float y) {
        if (rotatePrompt) return 0;
        if (panel() != Panel.NONE) y -= panelOffset();
        RectF hit = buttonAt(x, y);
        if (hit != null) return idFor(hit);
        if (panel() == Panel.CODEX) {
            List<Integer> entries = filteredEntries();
            int index = CodexLayout.hitTest(x, y, codexScroll, entries.size());
            if (index >= 0) return ID_ENTRY + entries.get(index);
        }
        return 0;
    }

    List<FishingAccessibility.Item> accessibilityItems() {
        List<FishingAccessibility.Item> items = new ArrayList<>();
        if (rotatePrompt) return items;
        Panel current = panel();
        float offset = panelOffset();
        if (current == Panel.NONE) {
            items.add(accessibilityItem(ID_CAST, castLabel(), fishButton, true, false));
            items.add(accessibilityItem(ID_SCENES, getResources().getString(R.string.a11y_scenes,
                    Catalog.SCENES[engine.getProgress().sceneIndex]), sceneButton, true, false));
            items.add(accessibilityItem(ID_CODEX, getResources().getString(R.string.a11y_codex,
                    unlockedCount, GameProgress.ENTRY_COUNT), codexButton, true, false));
            items.add(accessibilityItem(ID_HELP, getResources().getString(R.string.a11y_help),
                    helpButton, true, false));
            return items;
        }
        items.add(accessibilityItem(ID_CLOSE, getResources().getString(R.string.a11y_close),
                shifted(closeButton, offset), true, false));
        if (current == Panel.SCENES) {
            for (int i = 0; i < sceneCards.length; i++) {
                String state = i == engine.getProgress().sceneIndex
                        ? getResources().getString(R.string.a11y_scene_current)
                        : i == selectedScene
                        ? getResources().getString(R.string.a11y_scene_selected) : "";
                String label = state.isEmpty() ? Catalog.SCENES[i]
                        : getResources().getString(R.string.a11y_scene_card, Catalog.SCENES[i], state);
                items.add(accessibilityItem(ID_SCENE + i, label, shifted(sceneCards[i], offset),
                        true, i == selectedScene));
            }
            items.add(accessibilityItem(ID_CONFIRM_SCENE, getResources().getString(
                    R.string.a11y_confirm_scene, Catalog.SCENES[selectedScene]),
                    shifted(confirmSceneButton, offset), true, false));
            items.add(accessibilityItem(ID_CONTINUE, getResources().getString(R.string.a11y_continue),
                    shifted(continueButton, offset), true, false));
        } else if (current == Panel.CODEX) {
            for (int i = 0; i < 3; i++) {
                items.add(accessibilityItem(ID_QUALITY + i, getResources().getString(
                        R.string.a11y_filter_quality, qualities[i]),
                        shifted(qualityButtons[i], offset), true, uiState.getQualityFilter() == i));
            }
            for (int i = 0; i < 3; i++) {
                items.add(accessibilityItem(ID_UNLOCK + i, getResources().getString(
                        R.string.a11y_filter_unlock, unlocks[i]),
                        shifted(unlockButtons[i], offset), true, uiState.getUnlockFilter() == i));
            }
            List<Integer> entries = filteredEntries();
            int first = CodexLayout.firstVisible(codexScroll);
            int end = CodexLayout.endVisible(codexScroll, entries.size());
            for (int position = first; position < end; position++) {
                int entry = entries.get(position);
                float x = CodexLayout.LEFT
                        + (position % CodexLayout.COLUMNS) * CodexLayout.COLUMN_WIDTH;
                float y = CodexLayout.cellY(position, codexScroll) + offset;
                items.add(accessibilityItem(ID_ENTRY + entry, entryLabel(entry),
                        new RectF(x, y, x + CodexLayout.CELL_WIDTH, y + CodexLayout.CELL_HEIGHT),
                        true, false));
            }
        } else if (current == Panel.DETAIL) {
            items.add(accessibilityItem(ID_RETURN_CODEX,
                    getResources().getString(R.string.a11y_return_codex),
                    shifted(returnCodexButton, offset), true, false));
            items.add(accessibilityItem(ID_DETAIL_TEXT, detailLabel(),
                    shifted(new RectF(560, 150, 1100, 570), offset), false, false));
        } else if (current == Panel.HELP) {
            items.add(accessibilityItem(ID_HELP_TEXT, getResources().getString(
                    R.string.a11y_help_content,
                    String.join(" ", getResources().getStringArray(R.array.help_lines))),
                    shifted(new RectF(250, 175, 1030, 640), offset), false, false));
        }
        return items;
    }

    private static FishingAccessibility.Item accessibilityItem(
            int id, String label, RectF bounds, boolean clickable, boolean selected) {
        return new FishingAccessibility.Item(id, label, bounds, clickable, selected);
    }

    private static RectF shifted(RectF source, float dy) {
        RectF result = new RectF(source);
        result.offset(0, dy);
        return result;
    }

    private String castLabel() {
        FishingEngine.State state = engine.getState();
        int string = state == FishingEngine.State.IDLE ? R.string.a11y_cast
                : state == FishingEngine.State.BITING ? R.string.a11y_reel_bite : R.string.a11y_reel;
        return getResources().getString(string);
    }

    private String entryLabel(int entryIndex) {
        Species species = Catalog.get(entryIndex / 2);
        boolean shiny = entryIndex % 2 == 1;
        String quality = getResources().getString(
                shiny ? R.string.quality_shiny : R.string.quality_normal);
        EntryRecord record = engine.getProgress().entries[entryIndex];
        return record.unlocked()
                ? getResources().getString(R.string.a11y_entry, species.name, quality, record.count)
                : getResources().getString(R.string.a11y_entry_locked, quality);
    }

    private String detailLabel() {
        int entryIndex = uiState.getSelectedEntry();
        Species species = Catalog.get(entryIndex / 2);
        boolean shiny = entryIndex % 2 == 1;
        EntryRecord record = engine.getProgress().entries[entryIndex];
        String quality = getResources().getString(
                shiny ? R.string.quality_shiny : R.string.quality_normal);
        if (!record.unlocked()) {
            return getResources().getString(R.string.a11y_entry_locked, quality);
        }
        return getResources().getString(R.string.a11y_detail, species.name,
                getResources().getString(shiny ? R.string.quality_shiny_full
                        : R.string.quality_normal_full),
                Catalog.sceneText(species), species.timeText(), record.count,
                format(record.length), format(record.weight));
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
        if (rotatePrompt) return true;
        float x = viewport.toLogicalX(event.getX());
        float y = viewport.toLogicalY(event.getY());
        if (panelClosing) return true;
        if (panel() != Panel.NONE) y -= panelOffset();
        pointerX = x;
        pointerY = y;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            pointerDown = true;
            pressedButton = buttonAt(x, y);
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            pointerDown = false;
            pressedButton = null;
            scrolling = false;
            return true;
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            pointerDown = false;
            pressedButton = null;
        }
        if (panel() == Panel.CODEX) {
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
        RectF hit = buttonAt(x, y);
        if (hit != null) {
            activate(idFor(hit));
        } else if (panel() == Panel.CODEX) {
            List<Integer> entries = filteredEntries();
            int index = CodexLayout.hitTest(x, y, codexScroll, entries.size());
            if (index >= 0) activate(ID_ENTRY + entries.get(index));
        }
        return true;
    }

    private void openPanel(Panel next) {
        Panel previous = uiState.getPanel();
        uiState.showPanel(next);
        transitionPanel(previous, next);
    }

    public boolean handleSystemBack() {
        Panel previous = uiState.getPanel();
        if (!uiState.handleSystemBack()) return false;
        transitionPanel(previous, uiState.getPanel());
        return true;
    }

    private void transitionPanel(Panel previous, Panel next) {
        pointerDown = false;
        pressedButton = null;
        panelStarted = motion.uiSeconds;
        panelClosing = next == Panel.NONE && previous != Panel.NONE;
        if (panelClosing) {
            closingPanel = previous;
        } else {
            closingPanel = Panel.NONE;
            if (next == Panel.DETAIL) closeButton.set(1002, 102, 1060, 156);
            else if (next == Panel.HELP) closeButton.set(928, 110, 986, 164);
            else if (next == Panel.SCENES) closeButton.set(1090, 83, 1148, 137);
            else closeButton.set(1152, 70, 1210, 124);
        }
        if (next == Panel.CODEX) entriesDirty = true;
        if (next == Panel.DETAIL) {
            codexDirty = true;
        } else if (next != Panel.NONE) {
            // A closing panel still draws the detail layer until its fade ends, so the cache is
            // only released here for transitions that immediately switch to another panel.
            releaseCodexCache();
        }
        updateContentDescription(next);
        accessibility.rebuild();
        syncPause();
    }

    private void updateContentDescription(Panel panel) {
        if (rotatePrompt) {
            setContentDescription(getResources().getString(R.string.content_rotate_prompt));
            return;
        }
        int description;
        switch (panel) {
            case SCENES:
                description = R.string.content_scene_panel;
                break;
            case CODEX:
                description = R.string.content_codex_panel;
                break;
            case DETAIL:
                description = R.string.content_detail_panel;
                break;
            case HELP:
                description = R.string.content_help_panel;
                break;
            default:
                description = R.string.content_main_screen;
                break;
        }
        setContentDescription(getResources().getString(description));
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
