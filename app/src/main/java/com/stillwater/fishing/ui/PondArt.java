package com.stillwater.fishing.ui;

import android.content.Context;
import android.graphics.*;
import com.stillwater.fishing.engine.FishingEngine;
import java.io.IOException;
import java.io.InputStream;

/** Independent environment, actor, rod and water layers for the approved pond direction. */
final class PondArt {
    static final float FLOAT_X = PondLineGeometry.WATER_X, FLOAT_Y = PondLineGeometry.WATER_Y;
    private final Bitmap background, angler;
    private final Rect frameSource = new Rect();
    private final RectF destination = new RectF();
    private final Path line = new Path();
    // Authored atlas anchors in reference cell pixels (313px). One scale for every pose.
    // Register by the stool rather than each silhouette's changing bounding box.
    private static final float[] STOOL_X = {125.5f,132,126,126.5f,125.5f,131,131.5f,126.5f,
            125.5f,132,131.5f,132,129.5f,132,131.5f,130.5f};
    private static final float[] BASE_Y = {295,295,295,295,297,297,297,297,295,295,295,295,297,297,297,297};
    private static final float[] HAND_X = {211,211,211,211,193,221,253,211,210,202,208,211,197,202,209,211};
    private static final float[] HAND_Y = {210,198,210,210,210,159,164,210,210,201,219,200,199,171,147,208};
    private static final float[] TIP_X = {537,537,537,537,518,453,570,537,537,524,538,529,515,463,449,537};
    private static final float[] TIP_Y = {318,312,318,318,310,235,325,318,318,328,341,309,300,249,220,318};
    private final PondLineGeometry rig = new PondLineGeometry(CastMotion.TIP_X[3], CastMotion.TIP_Y[3], TIP_X[0], TIP_Y[0]);
    PondArt(Context context) {
        background = load(context, "pond_v2");
        angler = load(context, "angler_frames");
    }
    private static Bitmap load(Context context,String name) {
        try(InputStream input=context.getAssets().open("art/"+name+".png")) {
            BitmapFactory.Options options=new BitmapFactory.Options(); options.inScaled=false;
            return BitmapFactory.decodeStream(input,null,options);
        } catch(IOException e) { throw new IllegalStateException("Missing pond layer: "+name,e); }
    }
    void background(Canvas c,Paint p,int period,boolean rain,double time) {
        destination.set(0,0,1280,720); c.drawBitmap(background,null,destination,p);
        // Details are restricted to open water; no sky effects over the willow canopy.
        for(int i=0;i<13;i++) {
            float x=590+(i*113)%560, y=310+(i*61)%265;
            int frame = AnglerAnimation.loop(time + i*.21, 4, 4);
            float length=8+frame*4+(i%3)*8;
            p.setColor(Color.argb(25+frame*9,157,226,201));
            c.drawRect(x,y,x+length,y+3,p);
        }
        for(int i=0;i<2;i++) {
            float x=860+i*100+(float)Math.sin(time*.18+i*2)*52, y=365+i*110;
            p.setColor(0x55204D58);
            c.drawRect(x,y,x+25,y+9,p);c.drawRect(x+5,y-4,x+17,y+13,p);
            int tail = AnglerAnimation.loop(time + i, 6, 4);
            c.drawRect(x+25,y+(tail==1?0:-4),x+31,y+(tail==3?7:13),p);
            c.drawRect(x+10,y+(tail<2?-7:9),x+17,y+(tail<2?-3:16),p);
        }
        if(period==3) p.setColor(0x55101F48);
        else if(period==2) p.setColor(0x237E4651);
        else if(period==0) p.setColor(0x14E9BB79);
        else p.setColor(Color.TRANSPARENT);
        c.drawRect(0,0,1280,720,p);
        if(rain) {
            p.setColor(0x7799CDD1);p.setStrokeWidth(3);
            for(int i=0;i<52;i++) {
                float x=(float)((i*137+time*48)%1340)-30;
                float y=(float)((i*97+time*320)%780)-30;
                c.drawLine(x,y,x-5,y+15,p);
            }
        }
        p.setColor(Color.WHITE);
    }
    void thumbnail(Canvas c,Paint p,RectF bounds) {
        int width = Math.min(background.getWidth(), Math.round(background.getHeight()*bounds.width()/bounds.height()));
        // Show the dock and willow bank without squeezing the wide environment.
        Rect crop = new Rect(0, 0, width, background.getHeight());
        c.drawBitmap(background,crop,bounds,p);
    }
    void actor(Canvas c,Paint p,FishingEngine.State state,double time,double castAge,double biteAge,double catchAge,double reelAge,ArtRepository art) {
        boolean biting=state==FishingEngine.State.BITING;
        int frame = AnglerAnimation.frame(state,time,castAge,biteAge,reelAge);
        int col=frame%4, row=frame/4;
        frameSource.set(col*angler.getWidth()/4,row*angler.getHeight()/4,
                (col+1)*angler.getWidth()/4,(row+1)*angler.getHeight()/4);
        float scale=164f/279f;
        float left=362-STOOL_X[frame]*scale, top=444-BASE_Y[frame]*scale;
        destination.set(left,top,left+313*scale,top+313*scale);
        int castStep=state==FishingEngine.State.WAITING ? CastMotion.step(castAge) : -1;
        if(castStep>=0) {
            // Keep feet, stool and contact shadow fixed. Only the upper silhouette stores energy.
            int lower=c.save();c.clipRect(0,CastMotion.PIVOT_Y,1280,720);
            c.drawBitmap(angler,frameSource,destination,p);c.restoreToCount(lower);
            int upper=c.save();
            c.translate(CastMotion.PIVOT_X,CastMotion.PIVOT_Y);
            c.skew(CastMotion.SHEAR[castStep]/CastMotion.SY[castStep],0);
            c.scale(CastMotion.SX[castStep],CastMotion.SY[castStep]);
            c.translate(-CastMotion.PIVOT_X,-CastMotion.PIVOT_Y);
            c.clipRect(0,0,1280,CastMotion.PIVOT_Y);
            c.drawBitmap(angler,frameSource,destination,p);c.restoreToCount(upper);
        } else c.drawBitmap(angler,frameSource,destination,p);
        float handX=left+HAND_X[frame]*scale, handY=top+HAND_Y[frame]*scale;
        if(castStep>=0) {
            handX=CastMotion.x(castStep,handX,handY);
            handY=CastMotion.y(castStep,handY);
        }
        float tipX=castStep>=0?CastMotion.TIP_X[castStep]:TIP_X[frame];
        float tipY=castStep>=0?CastMotion.TIP_Y[castStep]:TIP_Y[frame];
        float bendX=(handX+tipX)*.5f+(castStep>=0?CastMotion.BEND_X[castStep]:0);
        float bendY=(handY+tipY)*.5f+(castStep>=0?CastMotion.BEND_Y[castStep]:-9);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);p.setColor(0xff17252e);
        line.reset();line.moveTo(handX,handY);line.quadTo(bendX,bendY,tipX,tipY);c.drawPath(line,p);
        p.setStrokeWidth(4);p.setColor(0xffbb804f);c.drawPath(line,p);
        p.setStrokeWidth(2);p.setColor(0xffe5bd79);c.drawLine(handX+4,handY-4,bendX,bendY,p);
        p.setStyle(Paint.Style.FILL);
        rig.update(state,time,castAge,reelAge);
        if (rig.visible) {
            float x=rig.x, y=rig.y;
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(0xfff4e6b8);
            float slack = rig.onWater && !biting ? 10 : 3;
            line.reset();line.moveTo(tipX,tipY);
            line.quadTo((tipX+rig.eyeX())/2,(tipY+rig.eyeY())/2+slack,rig.eyeX(),rig.eyeY());
            c.drawPath(line,p);
            // Clip the submerged body at the shared water plane, so a bite sinks into it.
            int checkpoint=c.save();
            if(rig.onWater)c.clipRect(0,0,1280,FLOAT_Y+4);
            p.setStyle(Paint.Style.FILL);p.setColor(0xff142d35);
            c.drawRect(x-8,y-8,x+8,y+8,p);c.drawRect(x-2,rig.eyeY(),x+2,y-8,p);
            p.setColor(0xffe97664);c.drawRect(x-5,y-6,x+5,y,p);
            p.setColor(0xfff5e6b8);c.drawRect(x-5,y,x+5,y+5,p);
            c.restoreToCount(checkpoint);
            if(rig.onWater) {
                float r=16+AnglerAnimation.loop(time,6,4)*8;
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(0x889ad7c5);
                c.drawOval(FLOAT_X-r,FLOAT_Y-r*.18f,FLOAT_X+r,FLOAT_Y+r*.18f,p);
                p.setStyle(Paint.Style.FILL);
            }
            if(biting) {
                art.splash(c,p,FLOAT_X,FLOAT_Y,VisualMotion.progress(biteAge,0,.65),false);
                p.setColor(0xff142735);c.drawRect(375,231,391,265,p);
                p.setColor(0xffffda7b);c.drawRect(379,235,387,252,p);c.drawRect(379,256,387,261,p);
            }
            if(rig.onWater && castAge>=PondLineGeometry.LAND_AT && castAge<PondLineGeometry.LAND_AT+.6)
                art.splash(c,p,FLOAT_X,FLOAT_Y,VisualMotion.progress(castAge,PondLineGeometry.LAND_AT,.6),false);
        }
        if(catchAge<.65)art.splash(c,p,FLOAT_X,FLOAT_Y,VisualMotion.progress(catchAge,0,.65),true);
        p.setStrokeWidth(1);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);
    }
}
