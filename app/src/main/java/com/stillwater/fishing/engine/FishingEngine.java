package com.stillwater.fishing.engine;

import com.stillwater.fishing.data.CatchResult;
import com.stillwater.fishing.data.Catalog;
import com.stillwater.fishing.data.EntryRecord;
import com.stillwater.fishing.data.GameProgress;
import com.stillwater.fishing.data.Species;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public final class FishingEngine {
    public enum State { IDLE, WAITING, BITING }

    public interface Listener {
        void onBite();
        void onCatch(CatchResult result);
        void onEscape();
        void onSaveRequested();
    }

    private final GameProgress progress;
    private final Random random;
    private final Listener listener;
    private State state = State.IDLE;
    private CatchResult pending;
    private double elapsed;
    private double waitSeconds;
    private boolean paused;
    private int roundPeriod;
    private int roundHour;

    public FishingEngine(GameProgress progress, Random random, Listener listener) {
        this.progress = progress;
        this.random = random;
        this.listener = listener;
    }

    public void press(long now) {
        if (paused) return;
        if (state == State.IDLE) {
            beginRound(now);
        } else if (state == State.WAITING) {
            cancelRound();
        } else {
            CatchResult result = pending;
            settle(result);
            state = State.IDLE;
            pending = null;
            listener.onSaveRequested();
            listener.onCatch(result);
        }
    }

    public void tick(double deltaSeconds, long now) {
        if (paused || state == State.IDLE) return;
        elapsed += Math.max(0, deltaSeconds);
        if (state == State.WAITING && elapsed >= waitSeconds) {
            state = State.BITING;
            elapsed = 0;
            listener.onBite();
        } else if (state == State.BITING && elapsed >= pending.escapeSeconds) {
            listener.onEscape();
            beginRound(now);
        }
    }

    private void beginRound(long now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        roundHour = calendar.get(Calendar.HOUR_OF_DAY);
        roundPeriod = Catalog.period(roundHour);
        refreshWeather(now);
        double minWait = progress.raining ? 8 : 10;
        double maxWait = progress.raining ? 16 : 20;
        waitSeconds = minWait + random.nextDouble() * (maxWait - minWait);
        pending = generateCatch(progress.sceneIndex, roundPeriod, progress.raining);
        elapsed = 0;
        state = State.WAITING;
    }

    public void refreshWeather(long now) {
        long hourKey = now / 3_600_000L;
        if (progress.weatherHour != hourKey) {
            progress.weatherHour = hourKey;
            progress.raining = random.nextDouble() < .30;
            listener.onSaveRequested();
        }
    }

    private CatchResult generateCatch(int scene, int period, boolean raining) {
        Species species;
        if (random.nextDouble() < .85) {
            List<Species> candidates = new ArrayList<>();
            int totalWeight = 0;
            for (Species item : Catalog.all()) {
                if (!item.junk && item.weights[scene] > 0 && item.availableAt(period)) {
                    candidates.add(item);
                    totalWeight += item.weights[scene];
                }
            }
            int roll = random.nextInt(totalWeight);
            species = candidates.get(0);
            for (Species candidate : candidates) {
                roll -= candidate.weights[scene];
                if (roll < 0) {
                    species = candidate;
                    break;
                }
            }
        } else {
            int roll = random.nextInt(100);
            species = Catalog.get(34);
            for (int id = 34; id < 42; id++) {
                roll -= Catalog.get(id).weights[scene];
                if (roll < 0) {
                    species = Catalog.get(id);
                    break;
                }
            }
        }
        boolean shiny = random.nextDouble() < (raining ? .06 : .03);
        double minLength = species.minLength;
        double maxLength = species.maxLength * (shiny ? 1.5 : 1);
        double minWeight = species.minWeight;
        double maxWeight = species.maxWeight * (shiny ? 3 : 1);
        double length = minLength + random.nextDouble() * (maxLength - minLength);
        double t = Math.log(length / minLength) / Math.log(maxLength / minLength);
        double referenceWeight = minWeight * Math.pow(maxWeight / minWeight, t);
        double d = species.junk ? .30 : .15;
        double a = Math.max(minWeight, referenceWeight * (1 - d));
        double b = Math.min(maxWeight, referenceWeight * (1 + d));
        double weight = a + random.nextDouble() * (b - a);
        length = round2(length);
        weight = round2(weight);
        return new CatchResult(species, shiny, length, weight, escapeSeconds(length, weight));
    }

    private void settle(CatchResult result) {
        EntryRecord entry = progress.entry(result.species.id, result.shiny);
        int beforeTotal = progress.unlockedCount();
        result.newDiscovery = !entry.unlocked();
        result.lengthRecord = !entry.unlocked() || result.length > entry.length;
        result.weightRecord = !entry.unlocked() || result.weight > entry.weight;
        entry.count++;
        entry.length = Math.max(entry.length, result.length);
        entry.weight = Math.max(entry.weight, result.weight);
        result.collectionComplete = beforeTotal == 83 && progress.unlockedCount() == 84;
    }

    public void cancelRound() {
        state = State.IDLE;
        pending = null;
        elapsed = 0;
    }

    public void changeScene(int index) {
        if (index == progress.sceneIndex) return;
        cancelRound();
        progress.sceneIndex = index;
        listener.onSaveRequested();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() { return paused; }
    public State getState() { return state; }
    public GameProgress getProgress() { return progress; }
    public CatchResult getPending() { return pending; }
    public double getBiteProgress() {
        return state == State.BITING ? Math.max(0, 1 - elapsed / pending.escapeSeconds) : 0;
    }
    public int getRoundHour() { return roundHour; }
    public int getRoundPeriod() { return roundPeriod; }
    public double getWaitSeconds() { return waitSeconds; }

    public static double escapeSeconds(double length, double weight) {
        double sl = clamp(Math.log(1 + length) / Math.log(301));
        double sw = clamp(Math.log(1 + weight / 100) / Math.log(1501));
        return round2(4 - 2.5 * (.5 * sl + .5 * sw));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
