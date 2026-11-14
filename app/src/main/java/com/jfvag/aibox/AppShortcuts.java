package com.jfvag.aibox;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import java.io.ByteArrayOutputStream;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Local device-only app launcher shortcuts. No server, EXLAP or media commands. */
final class AppShortcuts {
    private static final String PREFS="JF_APP_SHORTCUTS";
    private static final String KEY="packages";
    private static final int MAX=20;
    private AppShortcuts(){}

    private static final class Entry {
        final String name, pkg;
        Entry(String name,String pkg){this.name=name;this.pkg=pkg;}
    }
    private static List<Entry> installed(Activity activity){
        PackageManager pm=activity.getPackageManager();
        Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved=pm.queryIntentActivities(launcher,0);
        Set<String> seen=new HashSet<>();
        List<Entry> result=new ArrayList<>();
        for(ResolveInfo r:resolved){
            if(r.activityInfo==null)continue;
            String pkg=r.activityInfo.packageName;
            if(pkg==null||pkg.equals(activity.getPackageName())||!seen.add(pkg))continue;
            if(pm.getLaunchIntentForPackage(pkg)==null)continue;
            CharSequence label=r.loadLabel(pm);
            result.add(new Entry(label==null?pkg:label.toString(),pkg));
        }
        Collections.sort(result,Comparator.comparing(e->e.name.toLowerCase(java.util.Locale.ROOT)));
        return result;
    }
    private static List<String> saved(Activity activity){
        SharedPreferences p=activity.getSharedPreferences(PREFS,Activity.MODE_PRIVATE);
        String raw=p.getString(KEY,null);
        List<String> out=new ArrayList<>();
        if(raw==null){
            // Presets are used only when actually installed. Settings may override to empty.
            for(String pkg:new String[]{"com.google.android.youtube","com.netflix.mediaclient","com.google.android.apps.youtube.music"}){
                if(activity.getPackageManager().getLaunchIntentForPackage(pkg)!=null)out.add(pkg);
            }
            return out;
        }
        try{
            JSONArray a=new JSONArray(raw);
            for(int i=0;i<a.length()&&out.size()<MAX;i++){
                String pkg=a.optString(i,"");
                if(!pkg.isEmpty()&&!out.contains(pkg))out.add(pkg);
            }
        }catch(Exception ignored){}
        return out;
    }
    /** The icon is resolved on the AI Box itself, never downloaded or shipped in the APK. */
    private static Drawable appIcon(PackageManager pm,String pkg){
        try{return pm.getApplicationIcon(pkg);}catch(Exception ignored){return null;}
    }
    private static String appIconDataUri(PackageManager pm,String pkg){
        Drawable drawable=appIcon(pm,pkg);
        if(drawable==null)return "";
        try{
            // 72px is ample for the small launcher dock; cap the JSON payload.
            int size=72;
            Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);
            drawable.setBounds(0,0,size,size);
            drawable.draw(canvas);
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
            String base64=Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
            bitmap.recycle();
            return "data:image/png;base64,"+base64;
        }catch(Exception ignored){return "";}
    }
    static String touchpadJson(android.content.Context context){
        org.json.JSONArray out=new org.json.JSONArray();android.content.pm.PackageManager pm=context.getPackageManager();
        String raw=context.getSharedPreferences(PREFS,android.content.Context.MODE_PRIVATE).getString(KEY,"[]");
        try{org.json.JSONArray saved=new org.json.JSONArray(raw);for(int i=0;i<saved.length();i++){
            String pkg=saved.optString(i);if(pkg.isEmpty())continue;
            org.json.JSONObject o=new org.json.JSONObject();o.put("package",pkg);
            try{o.put("label",pm.getApplicationLabel(pm.getApplicationInfo(pkg,0)).toString());}catch(Exception e){o.put("label",pkg);}
            out.put(o);
        }}catch(Exception ignored){}return out.toString();
    }
    static String json(Activity activity){
        JSONArray out=new JSONArray();
        PackageManager pm=activity.getPackageManager();
        for(String pkg:saved(activity)){
            Intent launch=pm.getLaunchIntentForPackage(pkg);
            if(launch==null)continue;
            try{
                String name=String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg,0)));
                JSONObject e=new JSONObject();e.put("package",pkg);e.put("label",name);e.put("icon",appIconDataUri(pm,pkg));out.put(e);
            }catch(Exception ignored){}
        }
        return out.toString();
    }
    static void launch(Activity activity,String pkg){
        if(pkg==null||!saved(activity).contains(pkg))return;
        Intent intent=activity.getPackageManager().getLaunchIntentForPackage(pkg);
        if(intent==null){Toast.makeText(activity,"App 不存在或無法開啟",Toast.LENGTH_SHORT).show();return;}
        try{activity.startActivity(intent);}catch(Exception e){Toast.makeText(activity,"無法開啟 App",Toast.LENGTH_SHORT).show();}
    }
    static void configure(Activity activity,Runnable refresh){
        List<Entry> entries=installed(activity);
        if(entries.isEmpty()){Toast.makeText(activity,"沒有找到可啟動的 App",Toast.LENGTH_LONG).show();return;}
        List<String> picked=new ArrayList<>(saved(activity));
        final PackageManager pm=activity.getPackageManager();
        ListView list=new ListView(activity);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        ArrayAdapter<Entry> adapter=new ArrayAdapter<Entry>(activity,android.R.layout.simple_list_item_multiple_choice,entries){
            @Override public View getView(int position,View convertView,ViewGroup parent){
                Entry entry=getItem(position);
                LinearLayout row=new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                int pad=Math.round(12*activity.getResources().getDisplayMetrics().density);
                row.setPadding(pad,pad/2,pad,pad/2);
                ImageView icon=new ImageView(activity);
                int iconSize=Math.round(36*activity.getResources().getDisplayMetrics().density);
                Drawable art=appIcon(pm,entry.pkg);
                if(art!=null)icon.setImageDrawable(art);
                LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(iconSize,iconSize);
                ip.rightMargin=pad;row.addView(icon,ip);
                CheckedTextView label=new CheckedTextView(activity);
                label.setText(entry.name);
                label.setTextSize(15);
                label.setSingleLine(true);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                android.util.TypedValue checkStyle = new android.util.TypedValue();
                if (activity.getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorMultiple, checkStyle, true)
                        && checkStyle.resourceId != 0) {
                    label.setCheckMarkDrawable(checkStyle.resourceId);
                }
                // Reflect the picked set, including changes made without closing dialog.
                label.setChecked(picked.contains(entry.pkg));
                row.addView(label,new LinearLayout.LayoutParams(0,Math.round(50*activity.getResources().getDisplayMetrics().density),1));
                return row;
            }
        };
        list.setAdapter(adapter);
        for(int i=0;i<entries.size();i++)list.setItemChecked(i,picked.contains(entries.get(i).pkg));
        list.setOnItemClickListener((parent,view,position,id)->{
            String pkg=entries.get(position).pkg;
            if(picked.contains(pkg)){picked.remove(pkg);list.setItemChecked(position,false);}
            else if(picked.size()>=MAX){
                list.setItemChecked(position,false);
                Toast.makeText(activity,"最多選擇 20 個快捷 App",Toast.LENGTH_SHORT).show();
            }else{picked.add(pkg);list.setItemChecked(position,true);}
            adapter.notifyDataSetChanged();
        });
        new AlertDialog.Builder(activity)
            .setTitle("Touchpad 快捷 App（最多 20 個）")
            .setView(list)
            .setNegativeButton("取消",(d,w)->{})
            .setPositiveButton("儲存",(d,w)->{
                JSONArray a=new JSONArray();for(String pkg:picked)a.put(pkg);
                activity.getSharedPreferences(PREFS,Activity.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply();
                refresh.run();
            }).show();
    }
    private static void persist(Activity activity,List<String> packages){
        JSONArray a=new JSONArray();
        for(String pkg:packages)if(pkg!=null&&!pkg.isEmpty())a.put(pkg);
        activity.getSharedPreferences(PREFS,Activity.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply();
    }
    /** Long-press an occupied slot for replace / remove / reorder. */
    static void editSlot(Activity activity,String pkg,Runnable refresh){
        List<String> slots=new ArrayList<>(saved(activity));
        int index=slots.indexOf(pkg);
        if(index<0){configure(activity,refresh);return;}
        final String[] actions={"替換這個快捷 App","往上移動","往下移動","移除這個快捷 App","選擇全部快捷 App"};
        new AlertDialog.Builder(activity).setTitle("編輯快捷 App · 位置 "+(index+1))
            .setItems(actions,(dialog,which)->{
                if(which==0){chooseReplacement(activity,pkg,refresh);return;}
                if(which==4){configure(activity,refresh);return;}
                if(which==3)slots.remove(index);
                else if(which==1 && index>0)Collections.swap(slots,index,index-1);
                else if(which==2 && index<slots.size()-1)Collections.swap(slots,index,index+1);
                else {Toast.makeText(activity,"已經是最前或最後一個",Toast.LENGTH_SHORT).show();return;}
                persist(activity,slots);refresh.run();
            }).setNegativeButton("取消",null).show();
    }
    private static void chooseReplacement(Activity activity,String oldPkg,Runnable refresh){
        List<Entry> entries=installed(activity);
        List<String> picked=new ArrayList<>(saved(activity));
        int index=picked.indexOf(oldPkg);
        if(index<0){configure(activity,refresh);return;}
        // Do not offer apps that already have a shortcut in another position.
        entries.removeIf(e->picked.contains(e.pkg)&&!e.pkg.equals(oldPkg));
        if(entries.isEmpty()){Toast.makeText(activity,"找不到其他可選擇的 App",Toast.LENGTH_SHORT).show();return;}
        PackageManager pm=activity.getPackageManager();
        ArrayAdapter<Entry> adapter=new ArrayAdapter<Entry>(activity,android.R.layout.simple_list_item_1,entries){
            @Override public View getView(int position,View convertView,ViewGroup parent){
                Entry e=getItem(position);
                LinearLayout row=new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                int pad=Math.round(12*activity.getResources().getDisplayMetrics().density);
                row.setPadding(pad,pad/2,pad,pad/2);
                ImageView image=new ImageView(activity);
                Drawable icon=appIcon(pm,e.pkg);if(icon!=null)image.setImageDrawable(icon);
                int iconSize=Math.round(34*activity.getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(iconSize,iconSize);
                ip.rightMargin=pad;row.addView(image,ip);
                android.widget.TextView label=new android.widget.TextView(activity);
                label.setText(e.name+(e.pkg.equals(oldPkg)?"（目前使用）":""));
                label.setTextSize(15);
                label.setSingleLine(true);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                row.addView(label,new LinearLayout.LayoutParams(0,Math.round(49*activity.getResources().getDisplayMetrics().density),1));
                return row;
            }
        };
        new AlertDialog.Builder(activity).setTitle("選擇要替換的 App")
            .setAdapter(adapter,(dialog,position)->{
                picked.set(index,entries.get(position).pkg);
                persist(activity,picked);refresh.run();
            }).setNegativeButton("取消",null).show();
    }

}
