package com.stillwater.fishing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.window.OnBackInvokedDispatcher;

import com.stillwater.fishing.data.GameProgress;
import com.stillwater.fishing.data.SaveStore;
import com.stillwater.fishing.engine.FishingEngine;
import com.stillwater.fishing.ui.FishingView;

import java.util.Random;

public final class MainActivity extends Activity implements FishingEngine.Listener {
    private SaveStore saveStore;
    private FishingEngine engine;
    private FishingView view;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();
        saveStore = new SaveStore(this);
        GameProgress progress = saveStore.load();
        engine = new FishingEngine(progress, new Random(), this);
        view = new FishingView(this, engine);
        view.setId(R.id.fishing_view);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentView(view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
        engine.refreshWeather(System.currentTimeMillis());
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        engine.refreshWeather(System.currentTimeMillis());
        view.setForeground(true);
    }

    @Override
    protected void onPause() {
        view.setForeground(false);
        super.onPause();
    }

    // Predictive back is already routed through the API 33+ callback registered in onCreate.
    // This override only serves API 26-32; lint does not model the per-activity opt-in.
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        handleBack();
    }

    private void handleBack() {
        if (view == null || !view.handleSystemBack()) finish();
    }

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController controller =
                    getWindow().getDecorView().getWindowInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.statusBars()
                        | android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onBite() {
        view.playBiteSound();
    }

    @Override
    public void onCatch(com.stillwater.fishing.data.CatchResult result) {
        view.showCatch(result);
    }

    @Override
    public void onEscape() {
        view.showEscape();
    }

    @Override
    public void onSaveRequested() {
        saveStore.save(engine.getProgress());
    }
}
