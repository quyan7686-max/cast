package com.stillwater.fishing.data;

public final class CatchResult {
    public final Species species;
    public final boolean shiny;
    public final double length, weight, escapeSeconds;
    public boolean newDiscovery, lengthRecord, weightRecord, collectionComplete;

    public CatchResult(Species species, boolean shiny, double length, double weight, double escapeSeconds) {
        this.species = species;
        this.shiny = shiny;
        this.length = length;
        this.weight = weight;
        this.escapeSeconds = escapeSeconds;
    }

    public String qualityText() {
        return shiny ? "闪光" : "普通";
    }
}
