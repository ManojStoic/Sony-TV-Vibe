package com.sonytvvibe.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Discover only launchable apps; TV entries take precedence over phone entries. */
final class AppLibrary {
    static final class Entry {
        final String label,pkg; final ComponentName component; final boolean tv;
        Entry(String label,String pkg,ComponentName component,boolean tv){this.label=label;this.pkg=pkg;this.component=component;this.tv=tv;}
        Intent intent(){return new Intent(Intent.ACTION_MAIN).addCategory(tv?Intent.CATEGORY_LEANBACK_LAUNCHER:Intent.CATEGORY_LAUNCHER).setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);}
    }
    static List<Entry> load(PackageManager pm,String self){
        LinkedHashMap<String,Entry> unique=new LinkedHashMap<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
            for(ResolveInfo info:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0)){
                android.content.pm.ActivityInfo a=info.activityInfo;
                if(a==null||!a.exported||!a.enabled||!a.applicationInfo.enabled||a.packageName.equals(self)||unique.containsKey(a.packageName))continue;
                unique.put(a.packageName,new Entry(info.loadLabel(pm).toString(),a.packageName,new ComponentName(a.packageName,a.name),Intent.CATEGORY_LEANBACK_LAUNCHER.equals(category)));
            }
        }
        ArrayList<Entry> result=new ArrayList<>(unique.values());Collator collator=Collator.getInstance();
        result.sort((a,b)->{int c=collator.compare(a.label,b.label);return c!=0?c:a.pkg.compareTo(b.pkg);});return result;
    }
}
