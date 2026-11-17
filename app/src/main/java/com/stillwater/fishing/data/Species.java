package com.stillwater.fishing.data;

import java.util.Locale;

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
        if (timeMask == (DAWN | DAY)) {
            return "清晨、白天（" + hour(Catalog.DAWN_START) + "—" + hour(Catalog.DUSK_START) + "）";
        }
        if (timeMask == (DAWN | DUSK)) {
            return "清晨、黄昏（" + hour(Catalog.DAWN_START) + "—" + hour(Catalog.DAY_START)
                    + "；" + hour(Catalog.DUSK_START) + "—" + hour(Catalog.NIGHT_START) + "）";
        }
        if (timeMask == (DUSK | NIGHT)) {
            return "黄昏、夜晚（" + hour(Catalog.DUSK_START) + "—次日" + hour(Catalog.DAWN_START) + "）";
        }
        if (timeMask == NIGHT) {
            return "仅夜晚（" + hour(Catalog.NIGHT_START) + "—次日" + hour(Catalog.DAWN_START) + "）";
        }
        return "";
    }

    private static String hour(int value) {
        return String.format(Locale.CHINA, "%02d:00", value);
    }
}
