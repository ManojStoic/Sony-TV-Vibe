package com.sonytvvibe.launcher;

/** Proportion-preserving artwork placement, independent of Android for local validation. */
final class TileGeometry {
    static float[] fit(int imageWidth,int imageHeight,float width,float height,float padding){
        if(imageWidth<=0||imageHeight<=0||width<=0||height<=0||padding<0||padding>=.5f)throw new IllegalArgumentException();
        float scale=Math.min(width*(1-2*padding)/imageWidth,height*(1-2*padding)/imageHeight);
        return new float[]{scale,(width-imageWidth*scale)/2,(height-imageHeight*scale)/2};
    }
}
