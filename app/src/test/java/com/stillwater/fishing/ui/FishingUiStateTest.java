package com.stillwater.fishing.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class FishingUiStateTest {
    @Test public void mainViewDoesNotConsumeSystemBack() {
        FishingUiState state = new FishingUiState();
        assertEquals(FishingUiState.Panel.NONE, state.getPanel());
        assertFalse(state.isPanelOpen());
        assertFalse(state.handleSystemBack());
    }

    @Test public void openPanelsAreDistinguishedFromMainView() {
        FishingUiState state = new FishingUiState();
        for (FishingUiState.Panel panel : FishingUiState.Panel.values()) {
            state.showPanel(panel);
            assertEquals(panel, state.getPanel());
            assertEquals(panel != FishingUiState.Panel.NONE, state.isPanelOpen());
        }
    }

    @Test public void detailBackReturnsToCodexAndPreservesEntryAndFilters() {
        FishingUiState state = new FishingUiState();
        state.setSelectedEntry(17);
        state.setQualityFilter(2);
        state.setUnlockFilter(1);
        state.showPanel(FishingUiState.Panel.DETAIL);

        assertTrue(state.handleSystemBack());
        assertEquals(FishingUiState.Panel.CODEX, state.getPanel());
        assertEquals(17, state.getSelectedEntry());
        assertEquals(2, state.getQualityFilter());
        assertEquals(1, state.getUnlockFilter());
    }

    @Test public void otherPanelsReturnToMainBeforeExit() {
        FishingUiState.Panel[] panels = {
                FishingUiState.Panel.SCENES,
                FishingUiState.Panel.CODEX,
                FishingUiState.Panel.HELP
        };
        for (FishingUiState.Panel panel : panels) {
            FishingUiState state = new FishingUiState();
            state.showPanel(panel);
            assertTrue(state.handleSystemBack());
            assertEquals(FishingUiState.Panel.NONE, state.getPanel());
            assertFalse(state.handleSystemBack());
        }
    }
}
