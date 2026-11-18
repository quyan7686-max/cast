package com.stillwater.fishing.ui;

import com.stillwater.fishing.engine.FishingEngine.State;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnglerAnimationTest {
    @Test public void castUsesAnticipationSnapAndRecoveryBeforeReturningToWaiting() {
        assertEquals(4,AnglerAnimation.frame(State.WAITING,0,0,100,100));
        assertEquals(5,AnglerAnimation.frame(State.WAITING,0,.14,100,100));
        assertEquals(6,AnglerAnimation.frame(State.WAITING,0,CastMotion.RELEASE,100,100));
        assertEquals(7,AnglerAnimation.frame(State.WAITING,0,.30,100,100));
        assertEquals(0,AnglerAnimation.frame(State.WAITING,0,.46,100,100));
        assertTrue(CastMotion.DURATION < .64);
    }
    @Test public void reelIsOneShotAndImmediateRecastTakesPrecedence() {
        for(int i=0;i<4;i++) assertEquals(12+i,AnglerAnimation.frame(State.IDLE,0,100,100,i*.10+.01));
        assertEquals(0,AnglerAnimation.frame(State.IDLE,0,100,100,.40));
        assertEquals(4,AnglerAnimation.frame(State.WAITING,0,0,100,.2));
    }
    @Test public void biteOverridesCastAndLoopsWithoutIdleFrames() {
        assertEquals(8,AnglerAnimation.frame(State.BITING,0,.1,0,100));
        assertEquals(11,AnglerAnimation.frame(State.BITING,0,100,.4,100));
        assertEquals(8,AnglerAnimation.frame(State.BITING,0,100,.5,100));
    }
    @Test public void blinkIsBriefAndPauseKeepsTheExactPose() {
        assertEquals(0,AnglerAnimation.frame(State.IDLE,2.1,100,100,100));
        assertEquals(2,AnglerAnimation.frame(State.IDLE,2.25,100,100,100));
        assertEquals(3,AnglerAnimation.frame(State.IDLE,2.4,100,100,100));
        VisualMotion clock=new VisualMotion();
        clock.frame(0,true,false);clock.cast();
        clock.frame(50_000_000,true,false);
        int before=AnglerAnimation.frame(State.WAITING,clock.waterSeconds,clock.waterSeconds-clock.castAt,100,100);
        for(int i=1;i<=20;i++)clock.frame(50_000_000L+i*50_000_000L,true,true);
        assertEquals(before,AnglerAnimation.frame(State.WAITING,clock.waterSeconds,clock.waterSeconds-clock.castAt,100,100));
        clock.caught();assertEquals(clock.catchAt,clock.reelAt,0);
    }
}
