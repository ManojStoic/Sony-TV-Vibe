package com.sonytvvibe.launcher;
public final class TileGeometryTest {
    public static void main(String[] args){
        int checks=0;
        for(int[] image:new int[][]{{320,180},{256,144},{100,100},{512,288},{400,80},{80,400}})
        for(float unit:new float[]{.75f,1f,1.333333f,2f,4f})
        for(int[] tile:new int[][]{{124,72},{150,84}}){
            float w=tile[0]*unit,h=tile[1]*unit;
            float[] fit=TileGeometry.fit(image[0],image[1],w,h,.045f);
            float drawnW=image[0]*fit[0],drawnH=image[1]*fit[0];
            if(fit[1]<0||fit[2]<0||drawnW>w||drawnH>h)throw new AssertionError("cropped logo");
            if(Math.abs(drawnW/drawnH-(float)image[0]/image[1])>.0001)throw new AssertionError("stretched logo");
            if(Math.abs(2*fit[1]+drawnW-w)>.001||Math.abs(2*fit[2]+drawnH-h)>.001)throw new AssertionError("not centered");
            float focusExtra=w*.045f/2;
            if(18*unit-2*focusExtra<=0)throw new AssertionError("focused neighbours overlap");checks++;
        }
        for(int count=1;count<=103;count++)for(int i=0;i<count;i++)for(boolean down:new boolean[]{true,false}){
            int target=GridNavigation.pageJump(i,count,down);if(target<0||target>=count)throw new AssertionError("page bounds");
            if(target!=i&&Math.abs(target/10-i/10)!=1)throw new AssertionError("skipped page");checks++;
        }
        if(GridNavigation.pageJump(9,11,true)!=10||GridNavigation.pageJump(10,11,false)!=5)throw new AssertionError("partial final page");
        System.out.println("PASS: "+checks+" geometry/page assertions. These test math and bounds, NOT native visual rendering.");
    }
}
