package com.stillwater.fishing.data;

public final class EntryRecord {
    public int count;
    public double length = -1;
    public double weight = -1;

    public boolean unlocked() {
        return count > 0;
    }

    public void clear() {
        count = 0;
        length = -1;
        weight = -1;
    }
}
