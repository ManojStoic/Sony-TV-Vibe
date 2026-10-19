package com.sonytvvibe.launcher;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.ViewOutlineProvider;

/** One drawing surface: no nested ImageView, card, label strip or separate artwork background. */
final class RoundedAppTile extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF rect=new RectF();
    private final float radius,unit;
    private final int baseColor;
    private int artworkBackground;
    private final String fallback;
    private Bitmap image;
    private BitmapShader shader;
    private final RectF artworkRect=new RectF();
    private boolean focused,banner;
    RoundedAppTile(Context context,String name,int baseColor,float unit){
        super(context);this.unit=unit;this.radius=10*unit;this.baseColor=baseColor;this.artworkBackground=baseColor;this.fallback=name;
        setContentDescription(name);setFocusable(true);setClickable(true);setId(View.generateViewId());
        setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View view,Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),radius);outline.setAlpha(.28f);}});
        setClipToOutline(true); // Whole surface remains rounded even while the view scales.
        setElevation(3*unit);
    }
    void setArtwork(Bitmap bitmap,boolean isBanner){image=bitmap;banner=isBanner;artworkBackground=baseColor;
        if(!isBanner){
            // Match a solid icon plate, but leave sparse transparent glyphs on the default surface.
            java.util.HashMap<Integer,Integer> colors=new java.util.HashMap<>();int opaque=0,total=0,best=0,bestColor=baseColor;
            for(int y=0;y<bitmap.getHeight();y+=4)for(int x=0;x<bitmap.getWidth();x+=4){total++;int c=bitmap.getPixel(x,y);if(Color.alpha(c)<240)continue;opaque++;int n=colors.containsKey(c)?colors.get(c)+1:1;colors.put(c,n);if(n>best){best=n;bestColor=c;}}
            if(opaque>total*.65f&&best>opaque*.40f)artworkBackground=bestColor;
        }
        shader=isBanner?new BitmapShader(bitmap,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP):null;updateMatrix();invalidate();}
    void highlight(boolean value){focused=value;setElevation((value?10:3)*unit);invalidate();}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);rect.set(0,0,w,h);updateMatrix();invalidateOutline();}
    private void updateMatrix(){
        if(image==null||getWidth()==0||getHeight()==0)return;
        float[] fit=TileGeometry.fit(image.getWidth(),image.getHeight(),getWidth(),getHeight(),banner?.045f:.16f);
        artworkRect.set(fit[1],fit[2],fit[1]+image.getWidth()*fit[0],fit[2]+image.getHeight()*fit[0]);
        if(shader!=null){Matrix matrix=new Matrix();matrix.setScale(fit[0],fit[0]);matrix.postTranslate(fit[1],fit[2]);shader.setLocalMatrix(matrix);}
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(artworkBackground);canvas.drawRoundRect(rect,radius,radius,paint);
        if(shader!=null){
            // CLAMP extends the image's own edge colours through letterboxing, rather than
            // putting a white/blue rectangular banner on a different-colour outer card.
            // The logo itself is FIT_CENTER: never stretched or cropped to fill the tile.
            paint.setColor(Color.WHITE);paint.setShader(shader);canvas.drawRoundRect(rect,radius,radius,paint);paint.setShader(null);
        }else if(image!=null){
            // Transparent app icons must not extend their edge pixels into cross-shaped bands.
            paint.setColor(Color.WHITE);canvas.drawBitmap(image,null,artworkRect,paint);
        }else{
            paint.setColor(baseColor==Color.WHITE?0xFF252027:Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextSize(13*unit);
            String initials=fallback.length()>18?fallback.substring(0,17)+"\u2026":fallback;
            canvas.drawText(initials,getWidth()/2f,getHeight()/2f-(paint.ascent()+paint.descent())/2,paint);
        }
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth((focused?2:0.65f)*unit);paint.setColor(focused?0xF0F6E8EE:0x22FFFFFF);
        float inset=paint.getStrokeWidth()/2;canvas.drawRoundRect(inset,inset,getWidth()-inset,getHeight()-inset,radius,radius,paint);paint.setStyle(Paint.Style.FILL);
    }
}
