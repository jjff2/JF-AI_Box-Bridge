package com.jfvag.aibox;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Map;

/** Only within the AI Box's native settings page; never displayed on the dashboard. */
public final class LanQrPanel extends LinearLayout {
    private final Activity host;
    private final TextView vehicleStatus, sharedStatus;
    private final ImageView vehicleQr, sharedQr;
    private final Button vehicleCopy,sharedCopy;
    private String lastVehicle="",lastShared="";
    private boolean compactMode=false;
    private final int blue=Color.rgb(80,180,244);
    public LanQrPanel(Activity activity){this(activity,false);}
    public LanQrPanel(Activity activity, boolean compact){
        super(activity);compactMode=compact;host=activity;setOrientation(VERTICAL);setPadding(dp(10),dp(4),dp(10),dp(4));
        TextView intro=text("手機或 iPad 要連到能與 AI Box 互通的 Wi-Fi，掃描相對應的 QR Code。這裡顯示的是 AI Box 位址，不是 MU1101 的 10.173.189.1。",14,Color.LTGRAY);
        if(!compact)addView(intro);
        LinearLayout columns=null;
        if(compact){
            columns=new LinearLayout(activity);
            columns.setOrientation(HORIZONTAL);
            columns.setGravity(Gravity.TOP);
            addView(columns,new LayoutParams(-1,-2));
        }
        View[] a=card("車機 Wi-Fi", "手機連車機 Wi-Fi 後掃描");
        vehicleStatus=(TextView)a[0];vehicleQr=(ImageView)a[1];vehicleCopy=(Button)a[2];
        View vehicleCard=(View)vehicleStatus.getParent();
        View[] b=card("共用 Wi-Fi", "手機與 AI Box 連同一網路後掃描");
        sharedStatus=(TextView)b[0];sharedQr=(ImageView)b[1];sharedCopy=(Button)b[2];
        View sharedCard=(View)sharedStatus.getParent();
        if(compact){
            removeView(vehicleCard);removeView(sharedCard);
            LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1f);
            left.rightMargin=dp(8);
            LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-2,1f);
            right.leftMargin=dp(8);
            columns.addView(vehicleCard,left);
            columns.addView(sharedCard,right);
        }
        Button reload=new Button(activity);reload.setAllCaps(false);reload.setText("重新偵測目前網路並更新 QR");reload.setOnClickListener(v->refresh());if(!compact)addView(reload);
        if(!compact)addView(text("如目前只有一條 Wi-Fi 連線，另一種模式將顯示『未連線』，不會產生無法使用的 QR。若不同 Wi-Fi 互相隔離，掃碼也無法連線。",12,Color.LTGRAY));
        refresh();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView t=new TextView(host);t.setText(value);t.setTextColor(color);t.setTextSize(size);t.setPadding(dp(4),dp(6),dp(4),dp(6));return t;}
    private View[] card(String title,String subtitle){
        LinearLayout card=new LinearLayout(host);card.setOrientation(VERTICAL);card.setPadding(dp(compactMode?8:14),dp(compactMode?6:12),dp(compactMode?8:14),dp(compactMode?6:12));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff1d314a,0xff111e30});bg.setCornerRadius(dp(18));bg.setStroke(dp(1),0xff385574);card.setBackground(bg);
        LayoutParams cp=new LayoutParams(-1,-2);cp.topMargin=dp(compactMode?2:12);addView(card,cp);
        TextView h=text(title,compactMode?16:17,Color.WHITE);h.setTypeface(null,Typeface.BOLD);card.addView(h);
        if(!compactMode)card.addView(text(subtitle,13,0xffb0c9dd));
        TextView state=text("正在偵測…",13,blue);state.setTextIsSelectable(true);if(compactMode)state.setTextSize(12);card.addView(state);
        ImageView q=new ImageView(host);q.setBackgroundColor(Color.WHITE);q.setPadding(dp(9),dp(9),dp(9),dp(9));q.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams qp=new LayoutParams(dp(compactMode?180:200),dp(compactMode?180:200));qp.gravity=Gravity.CENTER_HORIZONTAL;qp.topMargin=dp(compactMode?3:7);card.addView(q,qp);
        Button copy=new Button(host);copy.setAllCaps(false);copy.setText(compactMode?"複製網址":"複製 Safari 連結");card.addView(copy);
        return new View[]{state,q,copy};
    }
    private static final class Addresses {String vehicle="",shared="";}
    private Addresses detect(){
        Addresses result=new Addresses();
        try {
            Enumeration<NetworkInterface> nics=NetworkInterface.getNetworkInterfaces();
            while(nics.hasMoreElements()){
                NetworkInterface nic=nics.nextElement();if(!nic.isUp()||nic.isLoopback())continue;
                String name=nic.getName().toLowerCase(java.util.Locale.ROOT);
                // Ignore VPN/cellular and virtual interfaces; QR must be reachable from the phone on local Wi-Fi.
                if(name.startsWith("tun")||name.startsWith("rmnet")||name.startsWith("pdp")||name.startsWith("dummy")||name.startsWith("lo"))continue;
                Enumeration<InetAddress> addresses=nic.getInetAddresses();
                while(addresses.hasMoreElements()){
                    InetAddress a=addresses.nextElement();if(!(a instanceof Inet4Address)||a.isLoopbackAddress())continue;
                    String ip=a.getHostAddress();if(ip.startsWith("169.254.")||ip.startsWith("127."))continue;
                    if(ip.startsWith("10.173.189.")){result.vehicle=ip;continue;}
                    // Other RFC1918 interface addresses, including common iPhone and Android hotspot ranges.
                    String[] p=ip.split("\\.");
                    if(p.length!=4)continue;
                    int second=Integer.parseInt(p[1]);
                    boolean privateIp=ip.startsWith("192.168.")||ip.startsWith("10.")||(ip.startsWith("172.")&&second>=16&&second<=31);
                    if(privateIp && (name.contains("wlan")||name.contains("wifi")||name.contains("ap")||name.contains("eth")||name.contains("usb")))result.shared=ip;
                }
            }
        }catch(Exception ignored){}
        return result;
    }
    private Bitmap qr(String url)throws Exception{
        Map<EncodeHintType,Object> hint=new EnumMap<>(EncodeHintType.class);
        hint.put(EncodeHintType.CHARACTER_SET,"UTF-8");hint.put(EncodeHintType.MARGIN,2);
        BitMatrix bits=new QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,420,420,hint);
        Bitmap bmp=Bitmap.createBitmap(bits.getWidth(),bits.getHeight(),Bitmap.Config.ARGB_8888);
        int[] out=new int[bits.getWidth()*bits.getHeight()];for(int y=0;y<bits.getHeight();y++)for(int x=0;x<bits.getWidth();x++)out[y*bits.getWidth()+x]=bits.get(x,y)?Color.BLACK:Color.WHITE;
        bmp.setPixels(out,0,bits.getWidth(),0,0,bits.getWidth(),bits.getHeight());return bmp;
    }
    private void update(String ip,TextView status,ImageView image,Button copy,boolean vehicle){
        String url=ip.isEmpty()?"":"http://"+ip+":8765/?token="+BridgeService.token(host);
        String last=vehicle?lastVehicle:lastShared;
        if(url.equals(last))return;
        if(vehicle)lastVehicle=url;else lastShared=url;
        if(url.isEmpty()){
            status.setText("目前未偵測到此網路的 AI Box IP，請先連線，再按重新偵測。");image.setImageDrawable(null);image.setVisibility(GONE);copy.setEnabled(false);return;
        }
        status.setText(compactMode?"AI Box IP："+ip:"AI Box IP："+ip+"\nSafari："+url);
        try{image.setImageBitmap(qr(url));image.setVisibility(VISIBLE);}catch(Exception ex){image.setVisibility(GONE);status.setText("QR 產生失敗："+ex.getMessage());}
        copy.setEnabled(true);copy.setOnClickListener(v->{((ClipboardManager)host.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("JF Bridge",url));android.widget.Toast.makeText(host,"網址已複製",android.widget.Toast.LENGTH_SHORT).show();});
    }
    public void refresh(){Addresses a=detect();update(a.vehicle,vehicleStatus,vehicleQr,vehicleCopy,true);update(a.shared,sharedStatus,sharedQr,sharedCopy,false);}
}
