package com.stillwater.fishing.data;

public final class Species {
    public static final int DAWN = 1, DAY = 2, DUSK = 4, NIGHT = 8;
    public static final int ALL_DAY = 15;
    public final int id;
    public final String name;
    public final boolean junk;
    public final int timeMask;
    public final int[] weights;
    public final double minLength, maxLength, minWeight, maxWeight;

    Species(int id, String name, boolean junk, int timeMask, int[] weights,
            double minLength, double maxLength, double minWeight, double maxWeight) {
        this.id = id;
        this.name = name;
        this.junk = junk;
        this.timeMask = timeMask;
        this.weights = weights;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
    }

    public boolean availableAt(int period) {
        return (timeMask & (1 << period)) != 0;
    }

    public String timeText() {
        if (timeMask == ALL_DAY) return "全天";
        if (timeMask == (DAWN | DAY)) return "清晨、白天（05:00—17:00）";
        if (timeMask == (DAWN | DUSK)) return "清晨、黄昏（05:00—08:00；17:00—19:00）";
        if (timeMask == (DUSK | NIGHT)) return "黄昏、夜晚（17:00—次日05:00）";
        if (timeMask == NIGHT) return "仅夜晚（19:00—次日05:00）";
        return "";
    }
}
