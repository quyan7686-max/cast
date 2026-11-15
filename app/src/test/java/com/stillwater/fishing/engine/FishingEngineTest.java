package com.stillwater.fishing.engine;

import com.stillwater.fishing.data.*;
import org.junit.Test;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.TimeZone;
import java.util.function.LongSupplier;
import static org.junit.Assert.*;

public class FishingEngineTest {
    private static final long NOW = ZonedDateTime.parse("2026-11-13T12:00:00+08:00").toInstant().toEpochMilli();

    private static final class Clock implements LongSupplier {
        long nanos;
        public long getAsLong() { return nanos; }
        void advance(double seconds) { nanos += Math.round(seconds * 1_000_000_000L); }
    }

    private static final class Events implements FishingEngine.Listener {
        int bites, catches, escapes, saves;
        CatchResult last;
        public void onBite() { bites++; }
        public void onCatch(CatchResult result) { catches++; last = result; }
        public void onEscape() { escapes++; }
        public void onSaveRequested() { saves++; }
    }

    private final Clock clock = new Clock();
    private final Events events = new Events();
    private final GameProgress progress = new GameProgress();
    private final FishingEngine engine = new FishingEngine(progress, new Random(7), events, clock);

    private CatchResult bite() {
        engine.press(NOW);
        clock.advance(engine.getWaitSeconds());
        engine.tick(NOW);
        assertEquals(FishingEngine.State.BITING, engine.getState());
        return engine.getPending();
    }

    @Test public void expiredPressWithoutFrameEscapesAndKeepsAutomaticRound() {
        CatchResult result = bite();
        clock.advance(result.escapeSeconds - .01);
        engine.tick(NOW);
        clock.advance(1.01);
        engine.press(NOW);
        assertEquals(0, events.catches);
        assertEquals(1, events.escapes);
        assertEquals(FishingEngine.State.WAITING, engine.getState());
        assertNotSame(result, engine.getPending());
        assertEquals(0, progress.unlockedCount());
    }

    @Test public void exactDeadlineEscapes() {
        CatchResult result = bite();
        clock.advance(result.escapeSeconds);
        engine.press(NOW);
        assertEquals(1, events.escapes);
        assertEquals(0, events.catches);
    }

    @Test public void oneNanosecondBeforeDeadlineSucceeds() {
        CatchResult result = bite();
        clock.advance(result.escapeSeconds);
        clock.nanos--;
        engine.press(NOW);
        assertEquals(1, events.catches);
        assertEquals(0, events.escapes);
        assertTrue(result.newDiscovery);
        assertFalse(result.lengthRecord);
        assertFalse(result.weightRecord);
        assertEquals(FishingEngine.State.IDLE, engine.getState());
    }

    @Test public void pauseCountsActiveTimeBeforeLastFrameAndExcludesBackground() {
        CatchResult result = bite();
        clock.advance(.5);
        engine.setPaused(true);
        double frozen = engine.getBiteProgress();
        clock.advance(3600);
        engine.tick(NOW + 3_600_000);
        engine.press(NOW);
        assertEquals(frozen, engine.getBiteProgress(), 0);
        assertEquals(0, events.catches);
        engine.setPaused(false);
        clock.advance(result.escapeSeconds - .5);
        engine.press(NOW);
        assertEquals(1, events.escapes);
        assertEquals(1, events.bites);
    }

    @Test public void wallClockChangesDoNotConsumeBiteTime() {
        bite();
        engine.tick(NOW + 86_400_000);
        engine.tick(NOW - 86_400_000);
        assertEquals(1, engine.getBiteProgress(), 0);
        engine.press(NOW);
        assertEquals(1, events.catches);
    }

    @Test public void lateWaitingFrameStartsFullBiteWindow() {
        engine.press(NOW);
        clock.advance(engine.getWaitSeconds() + 10);
        engine.tick(NOW);
        assertEquals(1, events.bites);
        assertEquals(1, engine.getBiteProgress(), 0);
        assertEquals(0, events.escapes);
    }

    @Test public void pressAtBiteTransitionDoesNotCatchUnseenFish() {
        engine.press(NOW);
        clock.advance(engine.getWaitSeconds());
        engine.press(NOW);
        assertEquals(1, events.bites);
        assertEquals(0, events.catches);
        assertEquals(FishingEngine.State.BITING, engine.getState());
    }

    @Test public void waitingCancelAndSceneChangeDiscardPendingCatch() {
        engine.press(NOW);
        clock.advance(1);
        engine.press(NOW);
        assertEquals(FishingEngine.State.IDLE, engine.getState());
        assertNull(engine.getPending());
        engine.press(NOW);
        engine.changeScene(3);
        assertEquals(3, progress.sceneIndex);
        assertEquals(FishingEngine.State.IDLE, engine.getState());
        assertEquals(0, progress.unlockedCount());
    }

    @Test public void completionOnlyOccursOnFinalDiscoveryAndRecordsUseStrictComparison() {
        CatchResult result = bite();
        for (EntryRecord entry : progress.entries) { entry.count = 1; entry.length = 999; entry.weight = 999; }
        EntryRecord target = progress.entry(result.species.id, result.shiny);
        target.clear();
        engine.press(NOW);
        assertTrue(result.collectionComplete);
        CatchResult next = bite();
        EntryRecord existing = progress.entry(next.species.id, next.shiny);
        existing.length = next.length;
        existing.weight = next.weight - .01;
        engine.press(NOW);
        assertFalse(next.newDiscovery);
        assertFalse(next.lengthRecord);
        assertTrue(next.weightRecord);
        assertFalse(next.collectionComplete);
    }

    @Test public void hourlyWeatherUsesLocalBoundaryAndKeepsRoundLocked() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+05:45"));
            long before = ZonedDateTime.parse("2026-11-13T12:59:59+05:45").toInstant().toEpochMilli();
            engine.press(before);
            CatchResult pending = engine.getPending();
            double wait = engine.getWaitSeconds();
            int saves = events.saves;
            engine.setPaused(true);
            engine.tick(before + 500);
            assertEquals(saves, events.saves);
            engine.tick(before + 1000);
            assertEquals(saves + 1, events.saves);
            assertSame(pending, engine.getPending());
            assertEquals(wait, engine.getWaitSeconds(), 0);
            engine.tick(before + 2000);
            assertEquals(saves + 1, events.saves);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test public void allScenePeriodWeatherCombinationsGenerateValidCatches() {
        for (int scene = 0; scene < Catalog.SCENES.length; scene++) {
            int fishWeight = 0, junkWeight = 0;
            for (Species species : Catalog.all()) {
                if (species.junk) junkWeight += species.weights[scene];
                else fishWeight += species.weights[scene];
            }
            assertEquals(100, fishWeight);
            assertEquals(100, junkWeight);
            for (int hour : new int[]{6, 12, 18, 23}) for (boolean rain : new boolean[]{false, true}) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(2026, 10, 13, hour, 0, 0);
                long now = cal.getTimeInMillis();
                progress.sceneIndex = scene;
                progress.raining = rain;
                progress.weatherHour = (now + TimeZone.getDefault().getOffset(now)) / 3_600_000;
                for (int i = 0; i < 1000; i++) {
                    engine.press(now);
                    CatchResult result = engine.getPending();
                    Species species = result.species;
                    assertTrue(species.availableAt(Catalog.period(hour)));
                    assertTrue(species.weights[scene] > 0);
                    assertTrue(result.length >= species.minLength);
                    assertTrue(result.length <= species.maxLength * (result.shiny ? 1.5 : 1) + 1e-8);
                    assertTrue(result.weight >= species.minWeight);
                    assertTrue(result.weight <= species.maxWeight * (result.shiny ? 3 : 1) + 1e-8);
                    assertTrue(result.escapeSeconds >= 1.5 && result.escapeSeconds <= 4);
                    assertTrue(engine.getWaitSeconds() >= (rain ? 8 : 10));
                    assertTrue(engine.getWaitSeconds() < (rain ? 16 : 20));
                    clock.advance(engine.getWaitSeconds());
                    engine.tick(now);
                    engine.press(now);
                }
            }
        }
        assertEquals(48_000, events.catches);
    }
}
