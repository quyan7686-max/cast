package com.stillwater.fishing.ui;

import com.stillwater.fishing.engine.FishingEngine.State;
import org.junit.Test;
import static org.junit.Assert.*;

public class PondLineGeometryTest {
    private PondLineGeometry rig() { return new PondLineGeometry(570,325,537,318); }
    @Test public void landingIsInFrontOfTheSeatedCharacter() {
        assertTrue(PondLineGeometry.WATER_X > PondLineGeometry.SEAT_X);
        assertTrue(PondLineGeometry.WATER_Y >= PondLineGeometry.SEAT_Y + 100);
    }
    @Test public void launchBeginsAtReleasePoseAndLandsOnWaterPlane() {
        PondLineGeometry r=rig();
        r.update(State.WAITING,0,PondLineGeometry.RELEASE_AT-.01,100);assertFalse(r.visible);
        r.update(State.WAITING,0,PondLineGeometry.RELEASE_AT,100);assertTrue(r.visible);assertFalse(r.onWater);
        assertEquals(570,r.eyeX(),2);assertEquals(325,r.eyeY(),2);
        r.update(State.WAITING,0,PondLineGeometry.LAND_AT,100);assertTrue(r.onWater);
        assertEquals(PondLineGeometry.WATER_X,r.x,0);
        assertEquals(PondLineGeometry.WATER_Y,r.y+PondLineGeometry.FLOAT_DRAFT,0);
    }
    @Test public void flightMovesForwardAcrossPoseBoundaryWithoutJumping() {
        PondLineGeometry r=rig();float last=0;
        for(double age=PondLineGeometry.RELEASE_AT;age<PondLineGeometry.LAND_AT;age+=.01) {
            r.update(State.WAITING,0,age,100);
            assertTrue(r.x>=last);if(last>0)assertTrue(r.x-last<=4);
            last=r.x;
        }
        r.update(State.WAITING,0,PondLineGeometry.LAND_AT-.0001,100);float x=r.x,y=r.y;
        r.update(State.WAITING,0,PondLineGeometry.LAND_AT,100);
        assertEquals(x,r.x,4);assertEquals(y,r.y,4);
    }
    @Test public void bitingSubmergesFloatWithoutMovingWaterContact() {
        PondLineGeometry r=rig();r.update(State.WAITING,0,1,100);float y=r.y;
        r.update(State.BITING,0,1,100);
        assertTrue(r.onWater);assertEquals(y+8,r.y,0);
        assertEquals(PondLineGeometry.WATER_X,r.eyeX(),0);
        assertEquals(r.y-16,r.eyeY(),0);
    }
    @Test public void earlyReelStartsAtLastAirbornePositionNotFarWaterPoint() {
        PondLineGeometry r=rig();r.update(State.WAITING,0,.28,100);float x=r.x,y=r.y;
        r.update(State.IDLE,0,.28,0);
        assertTrue(r.visible);assertFalse(r.onWater);
        assertEquals(x,r.x,0);assertEquals(y,r.y,0);
        r.update(State.IDLE,0,1,AnglerAnimation.REEL_SECONDS);assertFalse(r.visible);
    }
    @Test public void cancellationBeforeReleaseAndImmediateRecastDoNotShowPhantomFloat() {
        PondLineGeometry r=rig();r.update(State.WAITING,0,.1,100);
        r.update(State.IDLE,0,.1,0);assertFalse(r.visible);
        r.update(State.WAITING,0,1,100);r.update(State.IDLE,0,1,.2);assertTrue(r.visible);
        r.update(State.WAITING,0,0,.2);assertFalse(r.visible);
    }
}
