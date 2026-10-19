package com.sonytvvibe.launcher;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Static local artwork with fixed contrast overlays; no animation or wallpaper service. */
final class SpotlightBackground extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Bitmap image;
    private final Rect source=new Rect();
    private final RectF destination=new RectF();
    private Shader headerShade,tileShade;

    SpotlightBackground(Context context){
        super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        image=BitmapFactory.decodeResource(getResources(),R.drawable.spotlight_mountain_v1);
        source.set(0,0,image.getWidth(),image.getHeight());
    }
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){
        destination.set(0,0,w,h);
        headerShade=new LinearGradient(0,0,w*.62f,0,new int[]{0x80000000,0x10000000,0x00000000},new float[]{0,.65f,1},Shader.TileMode.CLAMP);
        tileShade=new LinearGradient(0,h*.48f,0,h,new int[]{0x08000000,0x48000000,0x9A020408},new float[]{0,.25f,1},Shader.TileMode.CLAMP);
    }
    @Override protected void onDraw(Canvas canvas){
        paint.setShader(null);canvas.drawBitmap(image,source,destination,paint);
        paint.setShader(headerShade);canvas.drawRect(0,0,getWidth(),getHeight(),paint);
        paint.setShader(tileShade);canvas.drawRect(0,0,getWidth(),getHeight(),paint);paint.setShader(null);
    }
}
