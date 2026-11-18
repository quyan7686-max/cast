package com.stillwater.fishing;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.stillwater.fishing.ui.FishingView;
import com.stillwater.fishing.ui.Viewport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CastMotionDeviceTest {
    @Rule public final ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);
    private void tapCast(String expected) {
        activityRule.getScenario().onActivity(activity -> {
            View view=activity.findViewById(R.id.fishing_view);
            float scale=Math.min(view.getWidth()/Viewport.LOGICAL_WIDTH,view.getHeight()/Viewport.LOGICAL_HEIGHT);
            float x=(view.getWidth()-1280*scale)/2+1132*scale;
            float y=(view.getHeight()-720*scale)/2+634*scale;
            long now=SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0);
            MotionEvent up=MotionEvent.obtain(now,now+30,MotionEvent.ACTION_UP,x,y,0);
            try {assertTrue(view.dispatchTouchEvent(down));assertTrue(view.dispatchTouchEvent(up));}
            finally {down.recycle();up.recycle();}
            String label=view.getAccessibilityNodeProvider().createAccessibilityNodeInfo(FishingView.ID_CAST).getText().toString();
            assertTrue(label,label.startsWith(expected));
        });
    }
    @Test public void touchCastAndCancelCanRepeatThroughTheNewAnimation() {
        SystemClock.sleep(1200);
        for(int i=0;i<3;i++) {
            tapCast("收竿");SystemClock.sleep(900);
            tapCast("抛竿");SystemClock.sleep(900);
        }
        SystemClock.sleep(1200);
    }
}
