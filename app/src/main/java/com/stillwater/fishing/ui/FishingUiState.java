package com.stillwater.fishing.ui;

public final class FishingUiState {
    public enum Panel { NONE, SCENES, CODEX, DETAIL, HELP }

    private Panel panel = Panel.NONE;
    private int selectedEntry;
    private int qualityFilter;
    private int unlockFilter;

    public Panel getPanel() {
        return panel;
    }

    public boolean isPanelOpen() {
        return panel != Panel.NONE;
    }

    public void showPanel(Panel next) {
        panel = next;
    }

    public boolean handleSystemBack() {
        if (panel == Panel.NONE) return false;
        panel = panel == Panel.DETAIL ? Panel.CODEX : Panel.NONE;
        return true;
    }

    public int getSelectedEntry() {
        return selectedEntry;
    }

    public void setSelectedEntry(int entry) {
        selectedEntry = entry;
    }

    public int getQualityFilter() {
        return qualityFilter;
    }

    public void setQualityFilter(int filter) {
        qualityFilter = filter;
    }

    public int getUnlockFilter() {
        return unlockFilter;
    }

    public void setUnlockFilter(int filter) {
        unlockFilter = filter;
    }
}
