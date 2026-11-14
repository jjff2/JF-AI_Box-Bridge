package com.jfvag.aibox;
import android.app.*;import android.media.AudioManager;import android.view.KeyEvent;import android.content.*;import android.os.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.SecureRandom;import java.util.*;import java.util.concurrent.*;import org.json.*;
public class BridgeService extends Service {
 static volatile boolean running=false,connected=false;static volatile String phase="Stopped",error="";static volatile long updated=0;static volatile long lastGoodData=0;static volatile int goodFields=0;static volatile int pollCycles=0;static volatile int exlapDirectoryCount=-1,exlapSupportedFields=-1;static volatile int subscriptionCount=0,pushEvents=0;static volatile long lastPushMs=0;static volatile String updateMode="poll";static volatile Map<String,Object> values=new LinkedHashMap<>();static volatile Map<String,Long> fieldUpdatedMs=new LinkedHashMap<>();static String token;private TouchpadServer touchpadServer;private ServerSocket server;private Thread listener;private Thread worker;private Thread mediaWorker;private volatile Exlap mediaReader;private volatile Exlap client;private volatile boolean paused=false;
 private volatile long lastWidgetRefresh=0;
 private volatile Map<String,Object> mediaValues=new LinkedHashMap<>();
 private volatile long mediaUpdatedMs=0;
 private volatile String mediaReadError="";
 private volatile String lastAudioScope="android";
 // A single authenticated media session serves both reads and user commands.
 // All EXLAP request/response operations on that socket are serialized by synchronized(session).
 private final Object mediaCommandGate=new Object();
 private long lastMediaCommandAt=0;
 private volatile long mediaQuietUntil=0;
 private volatile int mediaCommandCount=0, mediaCommandFailures=0, mediaSessionCount=0;
 private volatile String lastMediaAction="",lastMediaCommandError="";
 static boolean mediaExlapEnabled(Context c){return c.getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).getBoolean("media_exlap_enabled",true);}
 static String token(Context c){android.content.SharedPreferences p=c.getSharedPreferences("JF_BRIDGE",MODE_PRIVATE);String t=p.getString("token",null);if(t==null){byte[] b=new byte[24];new SecureRandom().nextBytes(b);t=android.util.Base64.encodeToString(b,android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);p.edit().putString("token",t).commit();}return t;}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public int onStartCommand(Intent i,int flags,int startId){if(i!=null&&"STOP".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}if(running)return START_STICKY;token=token(this);try{server=new ServerSocket();server.setReuseAddress(true);server.bind(new InetSocketAddress("0.0.0.0",8765));listener=new Thread(this::acceptLoop,"HTTP Listener");running=true;listener.start();if(getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).getBoolean("touchpad_enabled",true)){touchpadServer=new TouchpadServer(this);touchpadServer.start();}phase="HTTP ready";VehicleWidget.refresh(this);worker=new Thread(this::loop,"EXLAP Reader");worker.start();mediaWorker=new Thread(this::mediaLoop,"EXLAP Media Reader");mediaWorker.start();}catch(Exception e){error=e.toString();phase="HTTP failed";stopSelf();}return START_STICKY;}
 private void loop(){int retry=3;int failures=0;boolean discoveryDone=false;while(running){
  
  if(paused){connected=false;phase="已暫停";try{Thread.sleep(500);}catch(InterruptedException ignored){}continue;}
  boolean hasCredentials=(!SecureCredentials.read(this,"telemetry_user").isEmpty()&&!SecureCredentials.read(this,"telemetry_password").isEmpty())||(!BuildConfig.EXLAP_USER.isEmpty()&&!BuildConfig.EXLAP_PASSWORD.isEmpty());
  if(!hasCredentials){connected=false;phase="EXLAP 驗證資料不可用";error="尚無可用的認證設定，請檢查 App 版本或自訂設定";try{Thread.sleep(1500);}catch(InterruptedException ignored){}continue;}
  if(!discoveryDone||failures>=2){
   phase="自動尋找 MU1101";
   String found=ExlapDiscovery.find(this);
   if(found.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")){
    String previous=getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).getString("exlap_host","");
    if(!found.equals(previous))getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).edit().putString("exlap_host",found).apply();
   }
   discoveryDone=true;failures=0;
  }
  Exlap e=new Exlap(this);client=e;try{phase="自動連線 EXLAP";e.connect();exlapDirectoryCount=e.getDirectoryCount();exlapSupportedFields=e.getSupportedCount();subscriptionCount=e.getSubscriptionCount();pushEvents=0;lastPushMs=0;updateMode=subscriptionCount>0?"subscribe+poll":"poll";failures=0;connected=true;phase="EXLAP 已連線・讀取車輛數據";retry=3;error="";long sessionStarted=System.currentTimeMillis();while(running&&!paused){
   
   Map<String,Object> next=e.pollAdaptive();subscriptionCount=e.getSubscriptionCount();pushEvents=e.getPushEvents();lastPushMs=e.getLastPushMs();updateMode=subscriptionCount>0?"subscribe+poll":"poll";Map<String,Object> merged=new LinkedHashMap<>(values);Map<String,Long> fieldTimes=new LinkedHashMap<>(fieldUpdatedMs);long receivedAt=System.currentTimeMillis();
   int newFields=0;
   for(Map.Entry<String,Object> item:next.entrySet()){
    Object v=item.getValue();
    if(v instanceof java.util.List && !((java.util.List<?>)v).isEmpty()){
     merged.put(item.getKey(),v);fieldTimes.put(item.getKey(),receivedAt);newFields++;
    }else if(!merged.containsKey(item.getKey()))merged.put(item.getKey(),v);
   }
   fieldUpdatedMs=fieldTimes;values=merged;updated=System.currentTimeMillis();pollCycles++;
   if(updated-lastWidgetRefresh >= 3000){lastWidgetRefresh=updated;VehicleWidget.refresh(this);}
   if(newFields>0)lastGoodData=updated;
   int count=0;for(Object v:merged.values())if(v instanceof java.util.List&&!((java.util.List<?>)v).isEmpty())count++;
   goodFields=count;
   phase=count>0?"Live・已有車輛數據":"EXLAP 已連線・等待數據";
   error="";
   // A successful TCP auth with no usable data is not a healthy telemetry session.
   if(goodFields==0 && System.currentTimeMillis()-sessionStarted>15000)throw new IOException("No telemetry data");
   Thread.sleep(30);
  }}catch(Exception ex){connected=false;subscriptionCount=0;updateMode="reconnecting";failures++;String failureStage=phase;phase="Reconnect ("+failureStage+")";error=ex.getClass().getSimpleName()+": "+String.valueOf(ex.getMessage());if(!paused)try{Thread.sleep(retry*1000L);}catch(InterruptedException ignored){}retry=Math.min(retry*2,30);}finally{e.close();}}}
 // A separate Media-profile EXLAP connection avoids depending on telemetry-profile permissions.
 // Read-only until a user explicitly presses a playback control.
 private void mediaLoop(){
  while(running){
   
   if(paused || !mediaExlapEnabled(this)){
    Exlap old=mediaReader;if(old!=null){mediaReader=null;old.close();}
    sleepMedia(600);continue;
   }
   String u=SecureCredentials.read(this,"media_user"),pw=SecureCredentials.read(this,"media_password");
   if(u.isEmpty())u=BuildConfig.EXLAP_MEDIA_USER;
   if(pw.isEmpty())pw=BuildConfig.EXLAP_MEDIA_PASSWORD;
   if(u.isEmpty()||pw.isEmpty()){mediaReadError="Media credentials missing";sleepMedia(5000);continue;}
   Exlap e=new Exlap(this,u,pw);
   try{
    // Publish only AFTER successful authentication. Otherwise controls would race handshake.
    e.connectMedia();
    if(!running || paused || !mediaExlapEnabled(this)){e.close();continue;}
    mediaReader=e;mediaSessionCount++;mediaReadError="";
    while(running&&!paused&&mediaExlapEnabled(this)){
     // Give user actions priority; avoid a four-Get media poll immediately after a Call.
     long delay=mediaQuietUntil-System.currentTimeMillis();
     if(delay>0){sleepMedia(Math.min(delay,700));continue;}
     Map<String,Object> snapshot;
     synchronized(e){
      if(mediaReader!=e || !running || paused)break;
      snapshot=e.readMediaSnapshot();
     }
     if(!snapshot.isEmpty()){mediaValues=snapshot;mediaUpdatedMs=System.currentTimeMillis();}
     sleepMedia(3500);
    }
   }catch(Exception ex){mediaReadError=ex.getClass().getSimpleName()+": "+String.valueOf(ex.getMessage());}
   finally{
    if(mediaReader==e)mediaReader=null;
    e.close();
    // Do not hammer the host with a fast series of new authentication sessions.
    if(running&&!paused)sleepMedia(8000);
   }
  }
 }
 private void sleepMedia(long ms){try{Thread.sleep(ms);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}}
 private Map<String,Object> mergedAudioValues(){
  Map<String,Object> out=new LinkedHashMap<>(values);
  // Prefer the confirmed Media authorization profile when its snapshot is fresh.
  if(mediaUpdatedMs>0&&System.currentTimeMillis()-mediaUpdatedMs<12000){
   for(Map.Entry<String,Object> item:mediaValues.entrySet()){
    Object v=item.getValue();if(v instanceof List&&!((List<?>)v).isEmpty())out.put(item.getKey(),v);
    else out.remove(item.getKey()); // NoTrack must clear an older SD title.
   }
  }
  return out;
 }
 private static String exlapValue(Object obj,String key){
  if(!(obj instanceof List))return "";
  for(Object row:(List<?>)obj)if(row instanceof Map){Map<?,?> m=(Map<?,?>)row;if(key.equals(String.valueOf(m.get("key"))))return String.valueOf(m.get("value"));}
  return "";
 }
 private String autoAudioScope(){
  Map<String,Object> readings=mergedAudioValues();
  boolean freshMedia=(mediaUpdatedMs>0&&System.currentTimeMillis()-mediaUpdatedMs<12000)
    ||(fieldUpdatedMs.containsKey("Media_PlayerState")&&System.currentTimeMillis()-fieldUpdatedMs.get("Media_PlayerState")<12000);
  String state=freshMedia?exlapValue(readings.get("Media_PlayerState"),"State"):"";
  String source=freshMedia?exlapValue(readings.get("System_activeSource"),"SourceContext"):"";
  JSONObject android=MediaSessionReader.readState(this);
  boolean androidPlaying=android.optBoolean("available")&&android.optBoolean("playing");
  if(!mediaExlapEnabled(this))return "android";
  lastAudioScope=AudioRouteDecision.choose(androidPlaying,state,source,lastAudioScope);
  return lastAudioScope;
 }
 private String json()throws Exception {JSONObject o=new JSONObject();o.put("connected",connected);o.put("httpRunning",running);o.put("paused",paused);o.put("phase",phase);o.put("error",error);o.put("updated",updated/1000.0);o.put("lastGoodData",lastGoodData/1000.0);o.put("goodFields",goodFields);o.put("pollCycles",pollCycles);o.put("exlapDirectoryCount",exlapDirectoryCount);o.put("exlapSupportedFields",exlapSupportedFields);o.put("updateMode",updateMode);o.put("subscriptionCount",subscriptionCount);o.put("pushEvents",pushEvents);o.put("lastPushMs",lastPushMs);o.put("fieldUpdatedMs",new JSONObject(fieldUpdatedMs));o.put("serverTimeMs",System.currentTimeMillis());o.put("values",new JSONObject(mergedAudioValues()));o.put("autoAudioScope",autoAudioScope());o.put("mediaProfileFresh",mediaUpdatedMs>0&&System.currentTimeMillis()-mediaUpdatedMs<12000);o.put("mediaProfileError",mediaReadError);
  o.put("mediaSessionCount",mediaSessionCount);o.put("mediaCommandCount",mediaCommandCount);
  o.put("mediaCommandFailures",mediaCommandFailures);o.put("lastMediaAction",lastMediaAction);
  o.put("lastMediaCommandError",lastMediaCommandError);
  o.put("mediaExlapEnabled",mediaExlapEnabled(this));o.put("mediaSessionReady",mediaReader!=null);return o.toString();}
 private void acceptLoop(){while(running){try{Socket socket=server.accept();new Thread(()->serve(socket),"HTTP Client").start();}catch(Exception e){if(running)error=e.toString();}}}
 private void serve(Socket socket){try(Socket s=socket){s.setSoTimeout(5000);BufferedReader r=new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.US_ASCII));String first=r.readLine();if(first==null)return;String line;while((line=r.readLine())!=null&&!line.isEmpty()){}String[] p=first.split(" ");if(p.length<2||!(p[0].equals("GET")||p[0].equals("POST"))){reply(s,405,"text/plain","Method not allowed");return;}URI uri=new URI(p[1]);String supplied="";String command="";String scope="android";String q=uri.getRawQuery();if(q!=null)for(String part:q.split("&")){String[] kv=part.split("=",2);if(kv.length==2&&kv[0].equals("token"))supplied=URLDecoder.decode(kv[1],"UTF-8");else if(kv.length==2&&kv[0].equals("cmd"))command=URLDecoder.decode(kv[1],"UTF-8");else if(kv.length==2&&kv[0].equals("scope"))scope=URLDecoder.decode(kv[1],"UTF-8");}if(!token.equals(supplied)){reply(s,403,"text/plain","Forbidden");return;}String path=uri.getPath();if("/bridge".equals(path)){if(!p[0].equals("POST")){reply(s,405,"text/plain","POST required");return;}if("reconnect".equals(command)){paused=false;phase="要求重新連線";Exlap active=client;if(active!=null)active.close();Exlap activeMedia=mediaReader;if(activeMedia!=null)activeMedia.close();reply(s,200,"application/json; charset=utf-8",new JSONObject().put("ok",true).put("message","已要求重新連線，請查看連線階段").toString());}else if("media-on".equals(command)||"media-off".equals(command)){
 boolean enabled="media-on".equals(command);getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).edit().putBoolean("media_exlap_enabled",enabled).apply();
 if(!enabled){Exlap activeMedia=mediaReader;if(activeMedia!=null){mediaReader=null;activeMedia.close();}mediaValues=new LinkedHashMap<>();mediaUpdatedMs=0;}
 reply(s,200,"application/json; charset=utf-8",new JSONObject().put("ok",true).put("enabled",enabled).put("message",enabled?"已開啟原車媒體 EXLAP 連線":"已隔離原車媒體 EXLAP 連線（不影響車輛數據）").toString());
 }else if("pause".equals(command)){paused=true;connected=false;phase="Paused by user";Exlap active=client;if(active!=null)active.close();Exlap activeMedia=mediaReader;if(activeMedia!=null)activeMedia.close();reply(s,200,"application/json; charset=utf-8",new JSONObject().put("ok",true).put("message","車機連線已暫停，網頁服務仍運作").toString());}else reply(s,400,"application/json; charset=utf-8",new JSONObject().put("ok",false).put("message","Unknown bridge action").toString());}else if("/control".equals(path)){if(!p[0].equals("POST")){reply(s,405,"text/plain","POST required");return;}reply(s,200,"application/json; charset=utf-8",control(command,scope));}else if("/diagnostics".equals(path)){if(!p[0].equals("GET")){reply(s,405,"text/plain","GET required");return;}reply(s,200,"application/json; charset=utf-8",CompatibilityReport.create(this,connected,paused,lastGoodData,pollCycles,values,fieldUpdatedMs,phase).toString(2));}else if("/media".equals(path))reply(s,200,"application/json; charset=utf-8",MediaSessionReader.read(this).toString());else if("/api".equals(path))reply(s,200,"application/json; charset=utf-8",json());else if("/touchpad".equals(path)){reply(s,200,"text/html; charset=utf-8","<html><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><script>location.replace(\"http://\"+location.hostname+\":18777/\")</script><a href=\"http://\"+s.getLocalAddress().getHostAddress()+\":18777/\">開啟 Touchpad</a></html>");}else if("/".equals(path)){try(InputStream in=getAssets().open("dashboard.html")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);reply(s,200,"text/html; charset=utf-8",out.toString("UTF-8"));}}else reply(s,404,"text/plain","Not found");}catch(Exception ignored){}}
 private String control(String cmd,String scope)throws Exception {
  if("auto".equals(scope))scope=autoAudioScope();
  if("vehicle".equals(scope))return vehicleControl(cmd);
  if(!"android".equals(scope))return new JSONObject().put("ok",false).put("message","Unknown control source").toString();
  JSONObject result=new JSONObject();result.put("scope","Android AI Box local audio");
  AudioManager am=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
  if(am==null){result.put("ok",false);result.put("message","Android AudioManager unavailable");return result.toString();}
  int key=0;
  switch(cmd){case "prev":key=KeyEvent.KEYCODE_MEDIA_PREVIOUS;break;case "play":key=KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;break;case "next":key=KeyEvent.KEYCODE_MEDIA_NEXT;break;
   case "up":am.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,0);break;
   case "down":am.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,0);break;
   case "mute":am.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_TOGGLE_MUTE,0);break;
   default:result.put("ok",false);result.put("message","Unsupported command");return result.toString();}
  if(key!=0){am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));}
  result.put("ok",true);result.put("message",key!=0?"已送出 Android 媒體鍵，需播放器支援":"已送出 Android 音量指令");
  result.put("volume",am.getStreamVolume(AudioManager.STREAM_MUSIC));result.put("maxVolume",am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));return result.toString();
 }
 private String vehicleControl(String cmd)throws Exception {
  JSONObject result=new JSONObject();result.put("scope","MU1101 EXLAP");
  if(!mediaExlapEnabled(this)){result.put("ok",false);result.put("message","媒體 EXLAP 目前隔離中，請先在設定開啟媒體連線");return result.toString();}
  if(!connected && !(mediaUpdatedMs>0&&System.currentTimeMillis()-mediaUpdatedMs<12000)){result.put("ok",false);result.put("message","尚未連上車機 EXLAP Media");return result.toString();}
  String action;
  switch(cmd){
   case "prev":action="Media_PreviousTrack";break;
   case "next":action="Media_NextTrack";break;
   case "up":action="Sound_IncreaseVolume";break;
   case "down":action="Sound_DecreaseVolume";break;
   case "mute":{
    String muted=exlapValue(mergedAudioValues().get("Sound_Volume"),"EntertainmentMuted");
    action="true".equalsIgnoreCase(muted)?"Sound_Unmute":"Sound_Mute";break;
   }
   case "play":{
    String state=exlapValue(mergedAudioValues().get("Media_PlayerState"),"State");
    if(state.isEmpty()){result.put("ok",false);result.put("message","原車播放狀態未知；為避免誤啟動，暫不送播放指令");return result.toString();}
    action=("Play".equalsIgnoreCase(state)||"Playing".equalsIgnoreCase(state))?"Media_Pause":"Media_Play";break;
   }
   default:result.put("ok",false);result.put("message","Unsupported media command");return result.toString();
  }
  // Do not create an EXLAP session per button press. An active read-only media session
  // is reused for explicit user-triggered calls; socket access is mutually exclusive.
  Exlap media=mediaReader;
  if(media==null){result.put("ok",false);result.put("message","媒體連線尚未就緒，未發送指令");return result.toString();}
  synchronized(mediaCommandGate){
   long now=System.currentTimeMillis();
   if(now-lastMediaCommandAt<500){result.put("ok",false);result.put("message","操作過快，請稍後再按");return result.toString();}
   lastMediaCommandAt=now;
  }
  try{
   synchronized(media){
    if(mediaReader!=media || paused || !running)throw new IOException("Media session unavailable");
    media.invokeMedia(action);
   }
   mediaCommandCount++;lastMediaAction=action;lastMediaCommandError="";
   mediaQuietUntil=System.currentTimeMillis()+1800;
   result.put("ok",true);result.put("message","已傳送 "+action+"（需確認車機實際動作）");result.put("action",action);
  }catch(Exception e){
   mediaCommandFailures++;lastMediaAction=action;
   lastMediaCommandError=e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage());
   result.put("ok",false);result.put("message","原車控制失敗："+e.getMessage());
   // A failed session must not be reused. Let the media reader reconnect with backoff.
   if(mediaReader==media){mediaReader=null;media.close();}
  }
  return result.toString();
 }
 private void reply(Socket socket,int code,String mime,String body)throws IOException {byte[] bytes=body.getBytes(StandardCharsets.UTF_8);String reason=code==200?"OK":code==403?"Forbidden":code==404?"Not Found":"Error";String h="HTTP/1.1 "+code+" "+reason+"\r\nContent-Type: "+mime+"\r\nContent-Length: "+bytes.length+"\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nConnection: close\r\n\r\n";OutputStream out=socket.getOutputStream();out.write(h.getBytes(StandardCharsets.US_ASCII));out.write(bytes);out.flush();}
 @Override public void onDestroy(){if(touchpadServer!=null)touchpadServer.stop();running=false;connected=false;phase="Stopped";if(client!=null)client.close();if(mediaReader!=null)mediaReader.close();if(worker!=null)worker.interrupt();if(mediaWorker!=null)mediaWorker.interrupt();if(server!=null)try{server.close();}catch(Exception ignored){}VehicleWidget.refresh(this);super.onDestroy();}
}
