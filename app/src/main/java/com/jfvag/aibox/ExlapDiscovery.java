package com.jfvag.aibox;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;

/** Restrained discovery: only known address and Wi-Fi's DHCP gateway, no subnet scan. */
final class ExlapDiscovery {
 static String gateway(Context ctx) {
  WifiManager wm=(WifiManager)ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
  if(wm==null)return "";
  DhcpInfo dhcp=wm.getDhcpInfo();if(dhcp==null||dhcp.gateway==0)return "";
  int x=dhcp.gateway;return (x&255)+"."+((x>>8)&255)+"."+((x>>16)&255)+"."+((x>>24)&255);
 }
 static String find(Context ctx) {
  ConnectivityManager cm=(ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
  Network wifi=null;
  for(Network net:cm.getAllNetworks()){
   NetworkCapabilities cap=cm.getNetworkCapabilities(net);
   if(cap!=null&&cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){wifi=net;break;}
  }
  if(wifi==null)return "Wi-Fi not connected";
  LinkedHashSet<String> hosts=new LinkedHashSet<>();
  String stored=ctx.getSharedPreferences("JF_BRIDGE",Context.MODE_PRIVATE).getString("exlap_host","");
  if(stored.matches("[A-Za-z0-9.-]{1,253}"))hosts.add(stored);
  String gateway=gateway(ctx);if(!gateway.isEmpty())hosts.add(gateway);
  hosts.add("10.173.189.1");
  for(String host:hosts){
   try(Socket s=wifi.getSocketFactory().createSocket()){
    s.connect(new InetSocketAddress(host,25010),900);s.setSoTimeout(1100);
    InputStream in=s.getInputStream();byte[] b=new byte[2048];int n=in.read(b);
    if(n>0){String greeting=new String(b,0,n,StandardCharsets.UTF_8);
     // A port being open is not enough: require EXLAP init evidence.
     if(greeting.contains("<Status")&&greeting.contains("<Init"))return host;
    }
   }catch(Exception ignored){}
  }
  return "No EXLAP service found on the connected Wi-Fi";
 }
}
