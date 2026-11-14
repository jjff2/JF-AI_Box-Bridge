package com.jfvag.aibox;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;

public class TouchService extends AccessibilityService {
 static volatile TouchService active;
 private float x=0.5f,y=0.5f;
 private WindowManager wm; private View cursor; private WindowManager.LayoutParams cursorParams;
 private void showCursor(){
  try{if(cursor!=null)return;wm=(WindowManager)getSystemService(WINDOW_SERVICE);View v=new View(this);GradientDrawable d=new GradientDrawable();d.setShape(GradientDrawable.OVAL);d.setColor(0xff10b981);d.setStroke(3,0xffffffff);v.setBackground(d);
   int size=(int)(24*getResources().getDisplayMetrics().density);cursorParams=new WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);cursorParams.gravity=Gravity.TOP|Gravity.LEFT;cursor=v;wm.addView(v,cursorParams);updateCursor();
  }catch(Exception e){android.util.Log.w("JF-Touchpad","cursor overlay unavailable",e);cursor=null;}
 }
 private void updateCursor(){if(cursor==null||wm==null)return;DisplayMetrics dm=getResources().getDisplayMetrics();cursorParams.x=(int)(x*dm.widthPixels)-cursorParams.width/2;cursorParams.y=(int)(y*dm.heightPixels)-cursorParams.height/2;try{wm.updateViewLayout(cursor,cursorParams);}catch(Exception ignored){}}
 private void removeCursor(){if(cursor!=null&&wm!=null){try{wm.removeView(cursor);}catch(Exception ignored){}cursor=null;}}
 @Override protected void onServiceConnected(){ super.onServiceConnected(); active=this; }
 @Override public void onAccessibilityEvent(AccessibilityEvent event){}
 @Override public void onInterrupt(){}
 @Override public void onDestroy(){removeCursor();active=null;super.onDestroy();}
 synchronized boolean swipeAt(float sx,float sy,float ex,float ey,long requestedDuration){
  DisplayMetrics dm=getResources().getDisplayMetrics();
  if(!Float.isFinite(sx)||!Float.isFinite(sy)||!Float.isFinite(ex)||!Float.isFinite(ey))return false;
  if(sx<0||sx>1||sy<0||sy>1||ex<0||ex>1||ey<0||ey>1)return false;
  float x1=sx*dm.widthPixels,y1=sy*dm.heightPixels,x2=ex*dm.widthPixels,y2=ey*dm.heightPixels;
  long duration=requestedDuration>0?Math.max(100,Math.min(1000,requestedDuration)):(long)Math.max(180,Math.min(650,Math.hypot(x2-x1,y2-y1)*0.75));
  return gesture(x1,y1,x2,y2,duration);
 }
 synchronized boolean action(String type,float dx,float dy){
  DisplayMetrics dm=getResources().getDisplayMetrics();
  int w=dm.widthPixels,h=dm.heightPixels;
  if("touch_at".equals(type)){x=Math.max(.001f,Math.min(.999f,dx));y=Math.max(.001f,Math.min(.999f,dy));showCursor();updateCursor();return gesture(x*w,y*h,x*w,y*h,75);}
  if("move".equals(type)){ x=Math.max(0.01f,Math.min(.99f,x+dx/w));y=Math.max(.01f,Math.min(.99f,y+dy/h));showCursor();updateCursor();return true; }
  if("open_bridge".equals(type)){
   try{android.content.Intent intent=new android.content.Intent(this,MainActivity.class);
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP|android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startActivity(intent);return true;
   }catch(Exception e){android.util.Log.w("JF-Touchpad","return to Bridge failed",e);return false;}
  }
  if(type.startsWith("pkg:")){String pkg=type.substring(4);if(!pkg.matches("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+"))return false;try{android.content.Intent intent=getPackageManager().getLaunchIntentForPackage(pkg);if(intent==null)return false;intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(intent);return true;}catch(Exception e){android.util.Log.w("JF-Touchpad","custom app failed",e);return false;}}
  if(type.startsWith("app_")){
   String pkg="";
   switch(type){case "app_youtube":pkg="com.google.android.youtube";break;case "app_netflix":pkg="com.netflix.mediaclient";break;case "app_music":pkg="com.google.android.apps.youtube.music";break;case "app_maps":pkg="com.google.android.apps.maps";break;}
   if(pkg.isEmpty())return false;
   android.content.Intent intent=getPackageManager().getLaunchIntentForPackage(pkg);
   if("app_maps".equals(type)&&intent==null){intent=new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("geo:0,0?q=Google+Maps"));intent.setPackage(pkg);}
   if(intent!=null){try{intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(intent);return true;}catch(Exception e){android.util.Log.w("JF-Touchpad","app launch failed: "+pkg,e);}}
   if("app_maps".equals(type)){
    try{android.content.Intent fallback=new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://www.google.com/maps"));fallback.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(fallback);return true;}catch(Exception e){android.util.Log.w("JF-Touchpad","maps fallback failed",e);}
   }
   return false;
  }
  if("recent".equals(type)){return performGlobalAction(GLOBAL_ACTION_RECENTS);}
  if("back".equals(type)){return performGlobalAction(GLOBAL_ACTION_BACK);}
  if("home".equals(type)){return performGlobalAction(GLOBAL_ACTION_HOME);}
  if("tap".equals(type)){showCursor();updateCursor();return gesture(x*w,y*h,x*w,y*h,70);}
  if("scroll".equals(type)){
   if(Math.abs(dx)>Math.abs(dy)){
    float start=dx<0?w*.25f:w*.75f;
    float end=dx<0?w*.75f:w*.25f;
    return gesture(start,h*.5f,end,h*.5f,450);
   }
   float start=dy<0?h*.75f:h*.25f;float end=dy<0?h*.25f:h*.75f;
   return gesture(w*.5f,start,w*.5f,end,450);
  }
  return false;
 }
 private boolean gesture(float sx,float sy,float ex,float ey,long ms){
  Path p=new Path();p.moveTo(sx,sy);if(sx!=ex||sy!=ey)p.lineTo(ex,ey);
  GestureDescription d=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p,0,ms)).build();
  return dispatchGesture(d,null,null);
 }
}
