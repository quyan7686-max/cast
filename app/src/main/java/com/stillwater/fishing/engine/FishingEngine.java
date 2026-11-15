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
import java.util.TimeZone;
import java.util.function.LongSupplier;

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
    private final LongSupplier nanoClock;
    private long lastUpdateNanos;
    private long elapsedNanos;
    private double waitSeconds;
    private boolean paused;
    private int roundPeriod;
    private int roundHour;

    public FishingEngine(GameProgress progress, Random random, Listener listener) {
        this(progress, random, listener, System::nanoTime);
    }

    public FishingEngine(GameProgress progress, Random random, Listener listener, LongSupplier nanoClock) {
        this.progress = progress;
        this.random = random;
        this.listener = listener;
        this.nanoClock = nanoClock;
        lastUpdateNanos = nanoClock.getAsLong();
    }

    public void press(long now) {
        if (paused) return;
        // 输入与绘制共享同一时钟。超时续钓后，本次点击不能取消新轮次。
        if (advance(now)) return;
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

    public void tick(long now) {
        advance(now);
    }

    private void updateElapsed() {
        long current = nanoClock.getAsLong();
        if (!paused && state != State.IDLE) {
            elapsedNanos += Math.max(0, current - lastUpdateNanos);
        }
        lastUpdateNanos = current;
    }

    private boolean advance(long now) {
        updateElapsed();
        refreshWeather(now);
        if (paused || state == State.IDLE) return false;
        if (state == State.WAITING && elapsedNanos >= Math.round(waitSeconds * 1_000_000_000L)) {
            state = State.BITING;
            // 咬钩提示实际发出时才启动逃脱计时，不扣除尚未呈现的等待超额时间。
            elapsedNanos = 0;
            listener.onBite();
            return true;
        } else if (state == State.BITING && elapsedNanos >= Math.round(pending.escapeSeconds * 1_000_000_000L)) {
            listener.onEscape();
            beginRound(now);
            return true;
        }
        return false;
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
        elapsedNanos = 0;
        state = State.WAITING;
    }

    public void refreshWeather(long now) {
        // 按当地时间整点划分小时键，而非 UTC 整点。
        long hourKey = (now + TimeZone.getDefault().getOffset(now)) / 3_600_000L;
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
            List<Species> candidates = new ArrayList<>();
            int totalWeight = 0;
            for (Species item : Catalog.all()) {
                if (item.junk && item.weights[scene] > 0) {
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
        // 长度/重量新纪录只属于已解锁条目；首次钓获只用“新发现”标记。
        result.lengthRecord = entry.unlocked() && result.length > entry.length;
        result.weightRecord = entry.unlocked() && result.weight > entry.weight;
        entry.count++;
        entry.length = Math.max(entry.length, result.length);
        entry.weight = Math.max(entry.weight, result.weight);
        result.collectionComplete = beforeTotal == GameProgress.ENTRY_COUNT - 1
                && progress.unlockedCount() == GameProgress.ENTRY_COUNT;
    }

    public void cancelRound() {
        state = State.IDLE;
        pending = null;
        elapsedNanos = 0;
    }

    public void changeScene(int index) {
        if (index < 0 || index >= Catalog.SCENES.length) throw new IllegalArgumentException("Unknown scene: " + index);
        if (index == progress.sceneIndex) return;
        cancelRound();
        progress.sceneIndex = index;
        listener.onSaveRequested();
    }

    public void setPaused(boolean paused) {
        // 暂停前尚未绘制的时间仍算有效时间；后台时间不计入本轮。
        updateElapsed();
        this.paused = paused;
    }

    public boolean isPaused() { return paused; }
    public State getState() { return state; }
    public GameProgress getProgress() { return progress; }
    public CatchResult getPending() { return pending; }
    public double getBiteProgress() {
        return state == State.BITING ? Math.max(0, 1 - elapsedNanos / (pending.escapeSeconds * 1_000_000_000.0)) : 0;
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
