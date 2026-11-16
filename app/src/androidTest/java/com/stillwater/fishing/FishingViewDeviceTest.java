package com.stillwater.fishing;

import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import com.stillwater.fishing.ui.Viewport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class FishingViewDeviceTest {
    private static final long TIMEOUT_MS = 5_000;

    @Rule
    public final ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private final UiDevice device = UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation());

    @Before
    public void dismissFirstRunImmersiveHint() {
        if (device.wait(Until.hasObject(By.res("android:id/ok")), 1_000)) {
            device.findObject(By.res("android:id/ok")).click();
            device.waitForIdle();
        }
        awaitWindowFocus();
    }

    private void awaitWindowFocus() {
        long deadline = android.os.SystemClock.uptimeMillis() + TIMEOUT_MS;
        final boolean[] focused = new boolean[1];
        do {
            activityRule.getScenario().onActivity(activity ->
                    focused[0] = activity.findViewById(R.id.fishing_view).hasWindowFocus());
            if (focused[0]) return;
            android.os.SystemClock.sleep(50);
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        assertTrue("Game window did not gain focus", focused[0]);
    }

    @Test
    public void launchesMainScreen() {
        assertDescription(R.string.content_main_screen);
    }

    @Test
    public void sceneButtonOpensSceneAndBackReturnsToMain() {
        clickLogical(970, 53);
        assertDescription(R.string.content_scene_panel);

        device.pressBack();

        assertDescription(R.string.content_main_screen);
    }

    @Test
    public void codexEntryOpensDetailAndBackReturns() {
        clickLogical(1102, 53);
        assertDescription(R.string.content_codex_panel);

        clickLogical(205, 226);
        assertDescription(R.string.content_detail_panel);

        device.pressBack();
        assertDescription(R.string.content_codex_panel);

        device.pressBack();
        assertDescription(R.string.content_main_screen);
    }

    @Test
    public void backFromMainFinishesActivity() {
        device.pressBack();
        long deadline = android.os.SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (activityRule.getScenario().getState() != Lifecycle.State.DESTROYED
                && android.os.SystemClock.uptimeMillis() < deadline) {
            android.os.SystemClock.sleep(50);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }

        assertEquals(Lifecycle.State.DESTROYED, activityRule.getScenario().getState());
    }

    private void assertDescription(int stringResource) {
        String description = InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getString(stringResource);
        assertTrue("Missing UI description: " + description,
                device.wait(Until.hasObject(By.desc(description)), TIMEOUT_MS));
    }

    private void clickLogical(float logicalX, float logicalY) {
        final int[] screenPoint = new int[2];
        activityRule.getScenario().onActivity(activity -> {
            android.view.View view = activity.findViewById(R.id.fishing_view);
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            float scale = Math.min(view.getWidth() / Viewport.LOGICAL_WIDTH,
                    view.getHeight() / Viewport.LOGICAL_HEIGHT);
            float offsetX = (view.getWidth() - Viewport.LOGICAL_WIDTH * scale) / 2f;
            float offsetY = (view.getHeight() - Viewport.LOGICAL_HEIGHT * scale) / 2f;
            screenPoint[0] = Math.round(location[0] + offsetX + logicalX * scale);
            screenPoint[1] = Math.round(location[1] + offsetY + logicalY * scale);
        });
        device.click(screenPoint[0], screenPoint[1]);
        device.waitForIdle();
    }
}
