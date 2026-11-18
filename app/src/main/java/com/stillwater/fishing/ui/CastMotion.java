package com.stillwater.fishing.ui;

/** Hand-authored poses on the existing atlas; all deformation is rasterized on the pixel canvas. */
final class CastMotion {
    static final double DURATION = .46, RELEASE = .18, LAND = .42;
    static final float PIVOT_X = 395, PIVOT_Y = 408;
    static final int[] FRAMES = {4,5,5,6,6,7,1,0};
    private static final double[] ENDS = {.04,.10,.18,.22,.27,.32,.38,DURATION};
    static final float[] SX = {.98f,.95f,1.12f,1.27f,1.18f,.98f,.93f,1};
    static final float[] SY = {1,.98f,.86f,.84f,.9f,1.05f,1.06f,1};
    static final float[] SHEAR = {0,.20f,.34f,-.34f,-.22f,-.10f,.05f,0};
    static final float[] TIP_X = {510,390,316,580,568,548,526,537};
    static final float[] TIP_Y = {308,230,252,305,346,330,308,318};
    static final float[] BEND_X = {-4,-30,-45,36,20,-14,8,0};
    static final float[] BEND_Y = {-9,-25,-12,22,8,-12,-8,-9};
    static int step(double age) {
        if(age<0 || age>=DURATION)return -1;
        for(int i=0;i<ENDS.length;i++)if(age<ENDS[i])return i;
        return -1;
    }
    static float x(int step,float x,float y) {
        return PIVOT_X+SX[step]*(x-PIVOT_X)+SHEAR[step]*(y-PIVOT_Y);
    }
    static float y(int step,float y) { return PIVOT_Y+SY[step]*(y-PIVOT_Y); }
}
