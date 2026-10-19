package com.sonytvvibe.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.animation.ValueAnimator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native launcher with static local artwork. It never changes the system wallpaper or services. */
public final class MainActivity extends Activity {
    private static final String HOTSTAR="in.startv.hotstar"; // User confirmed, resolver verified.
    private static final String APPLE_TV="com.apple.atve.sony.appletv", SONYLIV="com.sonyliv";
    private FrameLayout root,content;
    private View settings,appsButton,searchButton;
    private TextView focusedName,greeting;
    private boolean media,allApps,settingsFocused,restoreAppsButton,libraryLoading=true;
    private int selected,librarySelected,artGeneration;
    private String libraryFocusPackage;
    private float unit=1,homeTileUnit=1;
    private List<AppLibrary.Entry> library=new ArrayList<>();
    private final ArrayList<View> tiles=new ArrayList<>();
    private final ExecutorService loader=Executors.newFixedThreadPool(2);
    private final LruCache<String,Art> artwork=new LruCache<>(32);
    private final DecelerateInterpolator easing=new DecelerateInterpolator(1.5f);
    // Android TV's bundled Roboto Medium keeps labels crisp from the sofa and
    // avoids a downloaded font or a mismatched fallback typeface.
    private final Typeface uiTypeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);
    private static final class Art {final Bitmap bitmap;final boolean banner;Art(Bitmap b,boolean wide){bitmap=b;banner=wide;}}

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if(state!=null){media=state.getBoolean("media");allApps=state.getBoolean("allApps");selected=state.getInt("selected");settingsFocused=state.getBoolean("settings");librarySelected=state.getInt("librarySelected");libraryFocusPackage=state.getString("libraryFocusPackage");}
        root=new FrameLayout(this){@Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);if(w>0&&h>0&&(w!=ow||h!=oh))post(()->render());}};
        root.setClipChildren(false);root.setBackgroundColor(0xFF09070C);setContentView(root);
        root.addView(new SpotlightBackground(this),new FrameLayout.LayoutParams(-1,-1));
        content=new FrameLayout(this);content.setClipChildren(false);content.setClipToPadding(false);root.addView(content,new FrameLayout.LayoutParams(-1,-1));
    }
    @Override protected void onResume(){super.onResume();if(greeting!=null)greeting.setText(greeting());refreshLibrary();}
    @Override protected void onDestroy(){loader.shutdownNow();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle s){s.putBoolean("media",media);s.putBoolean("allApps",allApps);s.putInt("selected",selected);s.putBoolean("settings",settingsFocused);s.putInt("librarySelected",librarySelected);s.putString("libraryFocusPackage",libraryFocusPackage);super.onSaveInstanceState(s);}
    private int px(float value){return Math.round(value*unit);}
    private String greeting(){int h=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);return h<12?"GOOD MORNING":h<18?"GOOD AFTERNOON":"GOOD EVENING";}
    private TextView text(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextColor(0xFFF5EDF1);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(size));v.setTypeface(uiTypeface);v.setGravity(Gravity.CENTER_VERTICAL);v.setFontFeatureSettings("kern");return v;}
    private GradientDrawable surface(boolean focused){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{focused?0xC0705665:0x80513C4B,focused?0xC043303C:0x80281E2A});d.setCornerRadius(px(18));d.setStroke(px(1),focused?0xE6FFF0F6:0x55C9B8C3);return d;}
    private GradientDrawable glassSurface(boolean focused){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{focused?0xE65D728B:0xD0384658,focused?0xD4313E50:0xC9202A39});d.setCornerRadius(px(20));d.setStroke(px(focused?2:1),focused?0xF2FFFFFF:0x55FFFFFF);return d;}
    private TextView control(String name,Runnable action){TextView v=text(name,12);v.setGravity(Gravity.CENTER);v.setId(View.generateViewId());v.setFocusable(true);v.setClickable(true);v.setBackground(surface(false));v.setOnFocusChangeListener((view,has)->focusStyle(view,has));v.setOnClickListener(view->action.run());return v;}
    private TextView secondaryControl(String name,Runnable action){TextView v=text(name,13);v.setGravity(Gravity.CENTER);v.setId(View.generateViewId());v.setFocusable(true);v.setClickable(true);v.setTextColor(0xFFF9F2F5);v.setOnFocusChangeListener((view,has)->{focusStyle(view,has);if(!has)view.setBackgroundColor(Color.TRANSPARENT);});v.setOnClickListener(view->action.run());return v;}
    private void position(View view,int width,int height,int gravity,int x,int y){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(px(width),px(height),gravity);if((gravity&Gravity.HORIZONTAL_GRAVITY_MASK)==Gravity.RIGHT)p.rightMargin=px(x);else if((gravity&Gravity.HORIZONTAL_GRAVITY_MASK)==Gravity.LEFT)p.leftMargin=px(x);if((gravity&Gravity.BOTTOM)==Gravity.BOTTOM)p.bottomMargin=px(y);else p.topMargin=px(y);content.addView(view,p);}
    private void header(boolean drawer){
        greeting=text(greeting(),10);greeting.setLetterSpacing(.16f);position(greeting,330,20,Gravity.TOP|Gravity.LEFT,48,24);
        TextClock time=new TextClock(this);time.setFormat12Hour("h:mm a");time.setFormat24Hour("HH:mm");time.setTypeface(uiTypeface);time.setTextColor(0xFFF7EEF2);time.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(32));time.setGravity(Gravity.CENTER_VERTICAL);position(time,186,44,Gravity.TOP|Gravity.LEFT,46,43);
        TextClock date=new TextClock(this);date.setFormat12Hour("EEE, d MMM");date.setFormat24Hour("EEE, d MMM");date.setTypeface(uiTypeface);date.setTextColor(0xFFD8C5D1);date.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(11));date.setLetterSpacing(.12f);date.setGravity(Gravity.CENTER_VERTICAL);position(date,205,32,Gravity.TOP|Gravity.LEFT,242,54);
        searchButton=null;
        if(!drawer){
            searchButton=control("\u2315  Search",this::searchApps);position(searchButton,94,34,Gravity.TOP|Gravity.RIGHT,166,38);
        }
        settings=control(drawer?"Back to "+(media?"Media":"Home"):"\u2699  Settings",drawer?this::closeLibrary:this::options);
        settings.setOnFocusChangeListener((v,has)->{settingsFocused=has;focusStyle(v,has);});position(settings,drawer?142:108,34,Gravity.TOP|Gravity.RIGHT,48,38);
        settings.setNextFocusUpId(settings.getId());settings.setNextFocusLeftId(settings.getId());settings.setNextFocusRightId(settings.getId());
        if(searchButton!=null){searchButton.setNextFocusUpId(searchButton.getId());searchButton.setNextFocusLeftId(searchButton.getId());searchButton.setNextFocusRightId(settings.getId());settings.setNextFocusLeftId(searchButton.getId());}
    }
    private void render(){
        if(root.getWidth()==0||root.getHeight()==0)return;
        unit=Math.min(root.getWidth()/960f,root.getHeight()/540f);boolean restoreSettings=settingsFocused;
        content.animate().cancel();content.setAlpha(1);content.setTranslationY(0);artGeneration++;content.removeAllViews();tiles.clear();appsButton=null;
        header(allApps);
        if(allApps){renderLibrary();return;}
        int count=media?3:6;
        homeTileUnit=Math.max(unit,getResources().getDisplayMetrics().density*.9f);
        int rowWidth=Math.round((count*124+(count-1)*18+32)*homeTileUnit);
        int viewportWidth=Math.min(rowWidth,root.getWidth()-px(96));
        focusedName=text("",28);focusedName.setSingleLine();focusedName.setEllipsize(TextUtils.TruncateAt.END);
        FrameLayout.LayoutParams nameParams=new FrameLayout.LayoutParams(viewportWidth-Math.round(32*homeTileUnit),px(46),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);nameParams.bottomMargin=px(110)+Math.round(104*homeTileUnit);content.addView(focusedName,nameParams);
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);int padding=Math.round(16*homeTileUnit);row.setPadding(padding,padding,padding,padding);row.setClipChildren(false);row.setClipToPadding(false);
        GradientDrawable tray=new GradientDrawable();tray.setColor(0x35120C15);tray.setCornerRadius(px(18));row.setBackground(tray);
        HorizontalScrollView viewport=new HorizontalScrollView(this);viewport.setHorizontalScrollBarEnabled(false);viewport.setFocusable(false);viewport.setSmoothScrollingEnabled(true);viewport.setClipToPadding(false);
        viewport.addView(row,new FrameLayout.LayoutParams(rowWidth,Math.round(104*homeTileUnit)));
        FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(viewportWidth,Math.round(104*homeTileUnit),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);rp.bottomMargin=px(100);content.addView(viewport,rp);
        if(media){
            tile(row,"USB player",null,this::launchConfiguredMediaPlayer);tile(row,"Plex","com.plexapp.android",()->launchPackage("com.plexapp.android"));tile(row,"VLC","org.videolan.vlc",()->launchPackage("org.videolan.vlc"));
        }else{
            tile(row,"Netflix","com.netflix.ninja",()->launchPackage("com.netflix.ninja"));tile(row,"YouTube","com.google.android.youtube.tv",()->launchPackage("com.google.android.youtube.tv"));tile(row,"JioHotstar",HOTSTAR,()->launchPackage(HOTSTAR));tile(row,"Prime Video","com.amazon.amazonvideo.livingroom",()->launchPackage("com.amazon.amazonvideo.livingroom"));tile(row,"Apple TV",APPLE_TV,()->launchVerified(APPLE_TV,"com.apple.atve.androidtv.appletv.MainActivity"));tile(row,"SonyLIV",SONYLIV,()->launchVerified(SONYLIV,"com.sonyliv.ui.splash.SplashActivity"));
        }
        appsButton=secondaryControl("Browse apps  \u203a",this::openLibrary);position(appsButton,148,32,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,46);
        selected=Math.max(0,Math.min(selected,tiles.size()-1));
        for(int i=0;i<tiles.size();i++){View v=tiles.get(i);v.setNextFocusLeftId(tiles.get(Math.max(0,i-1)).getId());v.setNextFocusRightId(tiles.get(Math.min(tiles.size()-1,i+1)).getId());v.setNextFocusUpId(settings.getId());v.setNextFocusDownId(appsButton.getId());}
        settings.setNextFocusDownId(tiles.get(selected).getId());if(searchButton!=null)searchButton.setNextFocusDownId(tiles.get(selected).getId());appsButton.setNextFocusUpId(tiles.get(selected).getId());appsButton.setNextFocusDownId(appsButton.getId());appsButton.setNextFocusLeftId(appsButton.getId());appsButton.setNextFocusRightId(appsButton.getId());
        focusedName.setText(tiles.get(selected).getContentDescription());
        if(restoreAppsButton){restoreAppsButton=false;appsButton.requestFocus();}else if(restoreSettings)settings.requestFocus();else tiles.get(selected).requestFocus();
    }
    private int brandColor(String pkg){if("com.netflix.ninja".equals(pkg)||"com.google.android.youtube.tv".equals(pkg))return Color.WHITE;if(HOTSTAR.equals(pkg))return 0xFF231261;if("com.amazon.amazonvideo.livingroom".equals(pkg))return 0xFF0066D6;return 0xFF30232F;}
    private RoundedAppTile card(String name,String pkg){RoundedAppTile v=new RoundedAppTile(this,name,brandColor(pkg),unit);loadArtwork(v,pkg);return v;}
    private void tile(LinearLayout row,String name,String pkg,Runnable action){
        final int index=tiles.size();RoundedAppTile v=new RoundedAppTile(this,name,brandColor(pkg),homeTileUnit);loadArtwork(v,pkg);
        v.setOnFocusChangeListener((view,has)->{focusStyle(view,has);if(has){selected=index;settingsFocused=false;focusedName.setText(name);settings.setNextFocusDownId(view.getId());if(appsButton!=null)appsButton.setNextFocusUpId(view.getId());int inset=Math.round(10*homeTileUnit);view.post(()->view.requestRectangleOnScreen(new android.graphics.Rect(-inset,-inset,view.getWidth()+inset,view.getHeight()+inset),false));}});
        v.setOnClickListener(view->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Math.round(124*homeTileUnit),Math.round(72*homeTileUnit));if(index>0)p.leftMargin=Math.round(18*homeTileUnit);row.addView(v,p);tiles.add(v);
    }
    private void focusStyle(View v,boolean has){
        v.animate().cancel();v.animate().scaleX(has?1.045f:1).scaleY(has?1.045f:1).translationY(has?-px(1):0).setDuration(ValueAnimator.areAnimatorsEnabled()?150:0).setInterpolator(easing).start();
        if(v instanceof RoundedAppTile)((RoundedAppTile)v).highlight(has);else{v.setBackground(surface(has));v.setElevation(px(has?6:0));}
    }
    private void loadArtwork(RoundedAppTile view,String pkg){
        String key=pkg==null?"__media":pkg;Art cached=artwork.get(key);if(cached!=null){view.setArtwork(cached.bitmap,cached.banner);return;}final int generation=artGeneration;
        loader.execute(()->{
            Art result=null;try{
                Drawable d=pkg==null?getDrawable(R.drawable.media):getPackageManager().getApplicationBanner(pkg);boolean banner=pkg!=null&&d!=null;
                if(d==null)d=getPackageManager().getApplicationIcon(pkg);
                int iw=Math.max(1,d.getIntrinsicWidth()),ih=Math.max(1,d.getIntrinsicHeight());float scale=Math.min(1f,Math.min(512f/iw,288f/ih));
                Bitmap b=Bitmap.createBitmap(Math.max(1,Math.round(iw*scale)),Math.max(1,Math.round(ih*scale)),Bitmap.Config.ARGB_8888);
                d.setBounds(0,0,b.getWidth(),b.getHeight());d.draw(new Canvas(b));result=new Art(b,banner);artwork.put(key,result);
            }catch(PackageManager.NameNotFoundException|RuntimeException ignored){}
            final Art art=result;runOnUiThread(()->{if(!isDestroyed()&&generation==artGeneration&&art!=null)view.setArtwork(art.bitmap,art.banner);});
        });
    }
    private void refreshLibrary(){loader.execute(()->{
        List<AppLibrary.Entry> result;try{result=AppLibrary.load(getPackageManager(),getPackageName());}catch(RuntimeException e){runOnUiThread(()->{if(!isDestroyed()){libraryLoading=false;if(allApps){render();message("Apps unavailable","Return home and try again.");}}});return;}
        runOnUiThread(()->{if(isDestroyed())return;result.add(0,new AppLibrary.Entry("Media","__media",null,true));library=result;libraryLoading=false;if(allApps)render();});
    });}
    private void openLibrary(){allApps=true;settingsFocused=false;if(library.isEmpty()&&!libraryLoading){libraryLoading=true;refreshLibrary();}render();animateScene();}
    private Dialog newGlassDialog(String title,View body,int width){
        Dialog dialog=new Dialog(this);dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);int pad=px(22);shell.setPadding(pad,pad,pad,pad);shell.setBackground(glassSurface(false));shell.setElevation(px(20));
        TextView heading=text(title,24);heading.setTextColor(Color.WHITE);shell.addView(heading,new LinearLayout.LayoutParams(-1,px(42)));
        shell.addView(body,new LinearLayout.LayoutParams(-1,-2));
        TextView close=secondaryControl("Close",dialog::dismiss);LinearLayout.LayoutParams closeParams=new LinearLayout.LayoutParams(px(92),px(34));closeParams.gravity=Gravity.RIGHT;closeParams.topMargin=px(10);shell.addView(close,closeParams);
        dialog.setContentView(shell);android.view.Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);android.view.WindowManager.LayoutParams a=window.getAttributes();a.dimAmount=.36f;window.setAttributes(a);}
        dialog.setOnShowListener(d->{if(dialog.getWindow()!=null)dialog.getWindow().setLayout(px(width),android.view.WindowManager.LayoutParams.WRAP_CONTENT);});
        return dialog;
    }
    private TextView glassAction(String label,Runnable action){TextView v=text(label,15);v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(px(16),0,px(16),0);v.setFocusable(true);v.setClickable(true);v.setBackground(glassSurface(false));v.setOnFocusChangeListener((view,has)->{view.animate().cancel();view.animate().alpha(has?1f:.88f).setDuration(ValueAnimator.areAnimatorsEnabled()?90:0).start();view.setElevation(px(has?8:0));view.setBackground(glassSurface(has));});v.setOnClickListener(view->action.run());return v;}
    private void searchApps(){
        final String[] query={""}; final LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        final TextView queryLabel=text("Choose letters to search installed apps",16);queryLabel.setTextColor(0xFFEAF2FA);queryLabel.setGravity(Gravity.CENTER_VERTICAL);queryLabel.setBackground(glassSurface(false));queryLabel.setPadding(px(16),0,px(16),0);box.addView(queryLabel,new LinearLayout.LayoutParams(-1,px(42)));
        final LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams resultParams=new LinearLayout.LayoutParams(-1,px(132));resultParams.topMargin=px(8);box.addView(results,resultParams);
        final LinearLayout keys=new LinearLayout(this);keys.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams keyParams=new LinearLayout.LayoutParams(-1,px(132));keyParams.topMargin=px(8);box.addView(keys,keyParams);
        final Dialog dialog=newGlassDialog("Search apps",box,500);final ArrayList<TextView> keyViews=new ArrayList<>();
        final Runnable[] refresh=new Runnable[1];
        refresh[0]=()->{String value=query[0];queryLabel.setText(value.length()==0?"Choose letters to search installed apps":"Search: "+value);results.removeAllViews();if(value.length()==0){TextView hint=text("Results appear here as you choose letters",13);hint.setTextColor(0xFFB9C8D7);hint.setGravity(Gravity.CENTER_VERTICAL);results.addView(hint,new LinearLayout.LayoutParams(-1,px(36)));return;}int shown=0;for(AppLibrary.Entry entry:library){if(!entry.label.toLowerCase(java.util.Locale.ROOT).contains(value.toLowerCase(java.util.Locale.ROOT)))continue;TextView row=glassAction(entry.label,()->{dialog.dismiss();if(entry.tv)safeStart(entry.intent());else message(entry.label,"This app has a standard Android entry point. Remote navigation may be limited.");});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,px(36));if(shown>0)p.topMargin=px(6);results.addView(row,p);shown++;if(shown==3)break;}if(shown==0){TextView none=text("No installed app matches",13);none.setTextColor(0xFFCAD5E1);none.setGravity(Gravity.CENTER_VERTICAL);results.addView(none,new LinearLayout.LayoutParams(-1,px(36)));}};
        String[][] rows={{"A","B","C","D","E","F","G","H","I"},{"J","K","L","M","N","O","P","Q","R"},{"S","T","U","V","W","X","Y","Z","⌫"}};
        for(String[] letters:rows){LinearLayout row=new LinearLayout(this);for(String letter:letters){TextView key=glassAction(letter,()->{if("⌫".equals(letter)){if(query[0].length()>0)query[0]=query[0].substring(0,query[0].length()-1);}else query[0]+=letter;refresh[0].run();});key.setGravity(Gravity.CENTER);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,px(36),1);if(row.getChildCount()>0)p.leftMargin=px(5);row.addView(key,p);keyViews.add(key);}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,px(38));if(keys.getChildCount()>0)p.topMargin=px(6);keys.addView(row,p);}
        TextView clear=glassAction("Clear search",()->{query[0]="";refresh[0].run();});clear.setGravity(Gravity.CENTER);LinearLayout.LayoutParams clearParams=new LinearLayout.LayoutParams(px(124),px(34));clearParams.gravity=Gravity.RIGHT;clearParams.topMargin=px(8);box.addView(clear,clearParams);
        refresh[0].run();
        dialog.setOnShowListener(d->{if(dialog.getWindow()!=null)dialog.getWindow().setLayout(px(500),android.view.WindowManager.LayoutParams.WRAP_CONTENT);keyViews.get(0).requestFocus();});
        dialog.show();
    }
    private void closeLibrary(){allApps=false;settingsFocused=false;restoreAppsButton=true;render();animateScene();}
    private void animateScene(){content.animate().cancel();if(!ValueAnimator.areAnimatorsEnabled())return;content.setAlpha(.85f);content.setTranslationY(px(6));content.animate().alpha(1).translationY(0).setDuration(160).setInterpolator(easing).start();}
    private void showPage(int index){librarySelected=Math.max(0,Math.min(index,library.size()-1));libraryFocusPackage=library.get(librarySelected).pkg;render();animateScene();}
    private void renderLibrary(){
        if(libraryFocusPackage!=null)for(int i=0;i<library.size();i++)if(library.get(i).pkg.equals(libraryFocusPackage)){librarySelected=i;break;}
        librarySelected=Math.max(0,Math.min(librarySelected,Math.max(0,library.size()-1)));
        focusedName=text(library.isEmpty()?(libraryLoading?"Loading apps\u2026":"No launchable apps found"):library.get(librarySelected).label,25);focusedName.setSingleLine();focusedName.setEllipsize(TextUtils.TruncateAt.END);position(focusedName,822,44,Gravity.TOP|Gravity.LEFT,69,132);
        if(library.isEmpty()){settings.setNextFocusDownId(settings.getId());settings.requestFocus();return;}
        // Two complete rows per page; no cropped third row or off-screen focus target.
        final int page=librarySelected/10,start=page*10,end=Math.min(start+10,library.size());
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);rows.setClipChildren(false);rows.setClipToPadding(false);position(rows,822,200,Gravity.TOP|Gravity.CENTER_HORIZONTAL,0,196);
        ArrayList<View> cells=new ArrayList<>();LinearLayout row=null;
        for(int i=start;i<end;i++){
            final int index=i;AppLibrary.Entry entry=library.get(i);
            if((i-start)%5==0){row=new LinearLayout(this);row.setClipChildren(false);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,px(84));if(i>start)rp.topMargin=px(24);rows.addView(row,rp);}
            RoundedAppTile cell="__media".equals(entry.pkg)?card(entry.label,null):card(entry.label,entry.pkg);cell.setContentDescription(entry.label+(entry.tv?"":". Standard Android app; remote support may vary."));
            cell.setOnFocusChangeListener((v,has)->{focusStyle(v,has);if(has){librarySelected=index;libraryFocusPackage=entry.pkg;focusedName.setText(entry.label);settings.setNextFocusDownId(v.getId());}});
            cell.setOnClickListener(v->{if("__media".equals(entry.pkg)){allApps=false;media=true;selected=0;settingsFocused=false;render();animateScene();}else if(entry.tv)safeStart(entry.intent());else new AlertDialog.Builder(this).setTitle(entry.label).setMessage("This app has a standard Android entry point. Remote navigation may be limited.").setPositiveButton("Open",(d,w)->safeStart(entry.intent())).setNegativeButton("Cancel",null).show();});
            cell.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;if(key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_UP){int target=GridNavigation.pageJump(index,library.size(),key==KeyEvent.KEYCODE_DPAD_DOWN);if(target!=index){showPage(target);return true;}}return false;});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(px(150),px(84));if((i-start)%5>0)p.leftMargin=px(18);row.addView(cell,p);cells.add(cell);
        }
        TextView previous=control("\u2039  Previous",()->showPage(start-10));TextView next=control("Next  \u203a",()->showPage(end));
        previous.setEnabled(start>0);previous.setFocusable(start>0);previous.setAlpha(start>0?1:.3f);next.setEnabled(end<library.size());next.setFocusable(end<library.size());next.setAlpha(end<library.size()?1:.3f);
        position(previous,108,32,Gravity.BOTTOM|Gravity.LEFT,69,52);position(next,108,32,Gravity.BOTTOM|Gravity.RIGHT,69,52);
        TextView count=text("All apps   \u00b7   "+(page+1)+" / "+((library.size()+9)/10),12);count.setGravity(Gravity.CENTER);count.setTextColor(0xFFCAB8C3);position(count,260,32,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,52);
        for(int i=0;i<cells.size();i++){View v=cells.get(i);v.setNextFocusLeftId(cells.get(GridNavigation.next(i,cells.size(),5,-1,0)).getId());v.setNextFocusRightId(cells.get(GridNavigation.next(i,cells.size(),5,1,0)).getId());int up=GridNavigation.next(i,cells.size(),5,0,-1);v.setNextFocusUpId(up<0?settings.getId():cells.get(up).getId());int down=GridNavigation.next(i,cells.size(),5,0,1);v.setNextFocusDownId(down==i?(next.isEnabled()?next.getId():previous.isEnabled()?previous.getId():settings.getId()):cells.get(down).getId());}
        View target=cells.get(librarySelected-start);settings.setNextFocusDownId(target.getId());
        previous.setNextFocusUpId(target.getId());next.setNextFocusUpId(target.getId());previous.setNextFocusDownId(previous.getId());next.setNextFocusDownId(next.getId());previous.setNextFocusLeftId(previous.getId());previous.setNextFocusRightId(next.isEnabled()?next.getId():previous.getId());next.setNextFocusLeftId(previous.isEnabled()?previous.getId():next.getId());next.setNextFocusRightId(next.getId());target.requestFocus();
    }
    private Intent launchIntent(String pkg){Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);return i!=null?i:getPackageManager().getLaunchIntentForPackage(pkg);}
    private void launchPackage(String pkg){Intent i=launchIntent(pkg);if(i==null){message("App unavailable","No accessible launch activity for "+pkg);return;}safeStart(i);}
    private void launchConfiguredMediaPlayer(){if(DeviceTargets.MEDIA_PLAYER_PACKAGE.length()==0||DeviceTargets.MEDIA_PLAYER_ACTIVITY.length()==0){message("USB player not configured","Use DeviceTargets.java after a read-only activity check to set this TV's built-in media-player component.");return;}launchVerified(DeviceTargets.MEDIA_PLAYER_PACKAGE,DeviceTargets.MEDIA_PLAYER_ACTIVITY);}
    private void launchVerified(String pkg,String cls){Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setComponent(new ComponentName(pkg,cls));android.content.pm.ResolveInfo resolved=getPackageManager().resolveActivity(i,0);if(resolved==null||!resolved.activityInfo.exported||!resolved.activityInfo.enabled){message("App unavailable","The verified target is no longer accessible: "+pkg);return;}safeStart(i);}
    private void safeStart(Intent i){try{startActivity(i);}catch(android.content.ActivityNotFoundException|SecurityException e){message("Unable to open","This activity is unavailable. Use the TV's Home-app setting to select its stock launcher.");}}
    private void message(String title,String body){new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK",null).show();}
    private void options(){
        LinearLayout choices=new LinearLayout(this);choices.setOrientation(LinearLayout.VERTICAL);int gap=px(8);
        Dialog dialog=newGlassDialog("Settings",choices,360);
        TextView tv=glassAction("TV settings",()->{dialog.dismiss();safeStart(new Intent(Settings.ACTION_SETTINGS));});TextView voice=glassAction("Voice search",()->{dialog.dismiss();voice();});TextView home=glassAction("Google TV",()->{dialog.dismiss();googleHome();});
        for(TextView choice:new TextView[]{tv,voice,home}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,px(42));p.bottomMargin=gap;choices.addView(choice,p);}dialog.show();tv.requestFocus();
    }
    private void voice(){Intent i=new Intent(RecognizerIntent.ACTION_WEB_SEARCH);if(i.resolveActivity(getPackageManager())==null){message("Voice search","Use the remote microphone button; no accessible system voice-search activity was found.");return;}safeStart(i);}
    private void googleHome(){Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);for(android.content.pm.ResolveInfo candidate:getPackageManager().queryIntentActivities(home,0)){if(candidate.activityInfo==null||candidate.activityInfo.packageName.equals(getPackageName()))continue;safeStart(new Intent(home).setComponent(new ComponentName(candidate.activityInfo.packageName,candidate.activityInfo.name)));return;}message("Stock launcher unavailable","No other Home activity is visible. Choose the stock launcher in the TV's Home-app setting.");}
    @Override public void onBackPressed(){if(allApps)closeLibrary();else if(media){media=false;selected=5;settingsFocused=false;render();animateScene();}else googleHome();}
}
