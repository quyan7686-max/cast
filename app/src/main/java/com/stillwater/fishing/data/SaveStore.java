package com.stillwater.fishing.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class SaveStore {
    private static final String FILE = "stillwater_progress";
    private static final int VERSION = 1;
    private final SharedPreferences preferences;

    public SaveStore(Context context) {
        this(context.getSharedPreferences(FILE, Context.MODE_PRIVATE));
    }

    SaveStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public GameProgress load() {
        GameProgress progress = new GameProgress();
        progress.sceneIndex = Math.max(0, Math.min(Catalog.SCENES.length - 1, preferences.getInt("scene", 0)));
        progress.weatherHour = preferences.getLong("weatherHour", Long.MIN_VALUE);
        progress.raining = preferences.getBoolean("raining", false);
        for (int i = 0; i < GameProgress.ENTRY_COUNT; i++) {
            EntryRecord entry = progress.entries[i];
            entry.count = Math.max(0, preferences.getInt("count_" + i, 0));
            entry.length = parse(preferences.getString("length_" + i, "-1"));
            entry.weight = parse(preferences.getString("weight_" + i, "-1"));
            if (entry.count == 0) entry.clear();
        }
        // Version 0 used the same keys. Fixed catalog IDs preserve their original meaning.
        // Write records and version together, so loading again never repeats a partial migration.
        if (preferences.getInt("schema_version", 0) == 0) save(progress);
        return progress;
    }

    public void save(GameProgress progress) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("schema_version", VERSION);
        editor.putInt("scene", progress.sceneIndex);
        editor.putLong("weatherHour", progress.weatherHour);
        editor.putBoolean("raining", progress.raining);
        for (int i = 0; i < GameProgress.ENTRY_COUNT; i++) {
            EntryRecord entry = progress.entries[i];
            editor.putInt("count_" + i, entry.count);
            editor.putString("length_" + i, Double.toString(entry.length));
            editor.putString("weight_" + i, Double.toString(entry.weight));
        }
        editor.apply();
    }

    private static double parse(String value) {
        if (value == null) return -1;
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed > 0 ? parsed : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
