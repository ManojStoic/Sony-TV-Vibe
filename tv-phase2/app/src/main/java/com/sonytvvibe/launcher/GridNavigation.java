package com.sonytvvibe.launcher;

/** Pure focus geometry, shared by native drawer and local JVM checks. */
final class GridNavigation {
    static int pageJump(int index,int count,boolean down){
        int start=(index/10)*10,local=index-start,end=Math.min(start+10,count);
        if(down&&local>=5&&end<count)return Math.min(end+local%5,count-1);
        if(!down&&local<5&&start>0)return start-5+local;
        return index;
    }
    static int next(int index,int count,int columns,int dx,int dy){
        if(count<=0||columns<=0||index<0||index>=count)throw new IllegalArgumentException();
        if(dy<0)return index<columns?-1:index-columns;
        if(dy>0){int start=(index/columns+1)*columns;return start>=count?index:Math.min(index+columns,count-1);}
        if(dx<0)return index%columns==0?index:index-1;
        if(dx>0)return index%columns==columns-1||index==count-1?index:index+1;
        return index;
    }
}
