package com.sonytvvibe.launcher;
import java.util.ArrayDeque;
public class GridNavigationTest {
    public static void main(String[] args){
        int checks=0;
        for(int count:new int[]{1,3,5,6,9,10,11,27,60,101}){
            boolean[] seen=new boolean[count];ArrayDeque<Integer> q=new ArrayDeque<>();q.add(0);seen[0]=true;
            while(!q.isEmpty()){
                int i=q.remove();
                for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}){int next=GridNavigation.next(i,count,5,d[0],d[1]);if(next < -1||next>=count)throw new AssertionError("out of range");if(next==-1&&i>=5)throw new AssertionError("bad exit");if(next>=0&&!seen[next]){seen[next]=true;q.add(next);}checks++;}
            }
            for(boolean reachable:seen)if(!reachable)throw new AssertionError("unreachable tile for count "+count);
            if(GridNavigation.next(count-1,count,5,0,1)!=count-1)throw new AssertionError("bottom boundary");
        }
        if(GridNavigation.next(4,7,5,0,1)!=6)throw new AssertionError("ragged row");
        System.out.println("PASS: "+checks+" directional checks; every app reachable for 10 list sizes; ragged rows and boundaries verified.");
    }
}
