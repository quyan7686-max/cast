package com.stillwater.fishing.ui;
import org.junit.Test;
import static org.junit.Assert.*;
public class CastMotionTest {
    @Test public void anticipationMovesHeadBackAndSnapMovesItForward() {
        assertTrue(CastMotion.x(2,395,290)<375);
        assertTrue(CastMotion.y(2,290)>290);
        assertTrue(CastMotion.x(3,395,290)>420);
        assertTrue(CastMotion.SX[3]>1.2f);
    }
    @Test public void hipContactIsFixedAndRecoveryHasNoResidualDistortion() {
        for(int i=0;i<8;i++) {
            assertEquals(CastMotion.PIVOT_X,CastMotion.x(i,395,408),0);
            assertEquals(CastMotion.PIVOT_Y,CastMotion.y(i,408),0);
        }
        assertEquals(420,CastMotion.x(7,420,350),0);
        assertEquals(350,CastMotion.y(7,350),0);
    }
    @Test public void releaseOccursAtTheSnapAndEveryStageHasVisibleTime() {
        assertEquals(2,CastMotion.step(CastMotion.RELEASE-.001));
        assertEquals(3,CastMotion.step(CastMotion.RELEASE));
        boolean[] visited=new boolean[8];
        for(double t=0;t<CastMotion.DURATION;t+=1.0/30)visited[CastMotion.step(t)]=true;
        for(boolean v:visited)assertTrue(v);
        assertEquals(-1,CastMotion.step(CastMotion.DURATION));
    }
}
