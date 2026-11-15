package com.stillwater.fishing.data;

public final class GameProgress {
    public static final int ENTRY_COUNT = Catalog.all().size() * 2;
    public final EntryRecord[] entries = new EntryRecord[ENTRY_COUNT];
    public int sceneIndex;
    public long weatherHour = Long.MIN_VALUE;
    public boolean raining;

    public GameProgress() {
        for (int i = 0; i < entries.length; i++) entries[i] = new EntryRecord();
    }

    public EntryRecord entry(int speciesId, boolean shiny) {
        return entries[speciesId * 2 + (shiny ? 1 : 0)];
    }

    public int unlockedCount() {
        int count = 0;
        for (EntryRecord entry : entries) if (entry.unlocked()) count++;
        return count;
    }
}
