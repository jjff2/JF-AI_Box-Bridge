package com.jfvag.aibox;
import android.content.Context;import android.net.ConnectivityManager;import android.net.Network;import android.net.NetworkCapabilities;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import javax.xml.parsers.*;import org.w3c.dom.*;import org.xml.sax.InputSource;

final class Exlap {
 static final String USER=BuildConfig.EXLAP_USER;
 // Public Vehicle Telemetry profile; this is NOT the web access token.
 static final String PASS=BuildConfig.EXLAP_PASSWORD;
 static final String[] FIELDS={"coolantTemperature","unitTemperature","recommendedGear","tankLevelPrimary","powermeter","currentConsumptionSecondary","wheelAngle","accIsActive","currentConsumptionPrimary","batteryVoltage","serviceOil","reverseGear","relChargingAirPressure","outsideTemperature","longTermConsumptionPrimary","shortTermConsumptionSecondary","combustionEngineDisplacement","oilLevel","driverIsBraking","engineTypes","gearTransmissionMode","blinkingState","hevOperationMode","fuelLevelState","tyreStates","currentTorque","absChargingAirPressure","vehicleSpeed","parkingBrake","longitudinalAcceleration","doorState","cycleConsumptionSecondary","recuperationLevel","tankLevelSecondary","lateralAcceleration","unitVolume","consumptionLongTermGeneral","fuelWarningSecondaryTank","currentOutputPower","suspensionProfile","suspensionStates","dayMilage","tyrePressures","fuelWarningPrimaryTank","yawRate","driveMode","lightState_rear","maxOutputPower","shortTermConsumptionPrimary","serviceInspection","Car_vehicleState","gearboxOilTemperature","acceleratorKickDown","clampState","oilTemperature","tyreTemperatures","longTermConsumptionSecondary","coastingIsActive","combustionEngineInjection","currentGear","totalDistance","tyreRequiredPressures","cycleConsumptionPrimary","startStopState","unitPressure","maxChargingAirPressure","brakePressure","lightState_front","clutch","consumptionShortTermGeneral","engineSpeed"};
 private final Context context;
 private final String username, password;
 Exlap(Context context){this(context,credential(context,"telemetry_user",USER),credential(context,"telemetry_password",PASS));}
 private static String credential(Context c,String key,String fallback){String v=SecureCredentials.read(c,key);return v.isEmpty()?fallback:v;}
 Exlap(Context context,String username,String password){this.context=context.getApplicationContext();this.username=username;this.password=password;}private Socket socket;private InputStream in;private OutputStream out;private final StringBuilder buffer=new StringBuilder();private int seq=200;private int fieldIndex=0; private int extraIndex=0;
 private Set<String> availableFields=null; private int directoryCount=-1;
 // Limit high-rate subscriptions; all remaining fields retain normal Get polling.
 private static final String[] FAST_FIELDS={"vehicleSpeed","engineSpeed","wheelAngle","currentTorque","relChargingAirPressure","absChargingAirPressure","maxChargingAirPressure","longitudinalAcceleration","lateralAcceleration","yawRate","currentOutputPower","powermeter","brakePressure","acceleratorKickDown","currentGear","gearTransmissionMode","recuperationLevel","coastingIsActive"};
 private final Set<String> subscribed=new HashSet<>();
 private final Map<String,Object> pushed=new LinkedHashMap<>();
 private long subscriptionStartedMs=0,lastPushMs=0;
 private int pushEvents=0;
 boolean hasSubscriptions(){return !subscribed.isEmpty();}
 int getSubscriptionCount(){return subscribed.size();}
 int getPushEvents(){return pushEvents;}
 long getLastPushMs(){return lastPushMs;}
 boolean isPushHealthy(){return hasSubscriptions() && pushEvents>0 && System.currentTimeMillis()-lastPushMs<12000;}
 private static String xmlAttr(String x){return x.replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;");}
 // A Dat may arrive while waiting for a response to another Req. Never discard it.
 private void rememberDat(Element frame){
  if(!"Dat".equals(frame.getTagName()))return;
  String url=frame.getAttribute("url");
  if(!subscribed.contains(url))return;
  List<Map<String,String>> items=extract(frame);
  if(!items.isEmpty()){pushed.put(url,items);lastPushMs=System.currentTimeMillis();pushEvents++;}
 }
 Map<String,Object> drainPushes(){Map<String,Object> result=new LinkedHashMap<>(pushed);pushed.clear();return result;}
 void subscribeFast(){
  subscribed.clear();pushed.clear();pushEvents=0;lastPushMs=0;subscriptionStartedMs=System.currentTimeMillis();
  int failures=0;
  for(String key:FAST_FIELDS){
   if(availableFields!=null&&!availableFields.contains(key))continue;
   try{
    subscribed.add(key); // Register before the immediate initial Dat can arrive.
    Element rsp=request(++seq,"<Subscribe url=\""+xmlAttr(key)+"\" ival=\"100\" content=\"true\"/>",2);
    String status=rsp.getAttribute("status");
    if(status.isEmpty()||"ok".equalsIgnoreCase(status)){failures=0;}
    else {subscribed.remove(key);pushed.remove(key);failures++;}
   }catch(SocketTimeoutException ex){subscribed.remove(key);pushed.remove(key);failures++; if(failures>=3)break;}
   catch(Exception ex){subscribed.remove(key);pushed.remove(key);failures++;if(failures>=3)break;}
  }
 }
 // Dynamic fields are eligible for faster Get reads only when Subscribe is unavailable.
 // Slow-changing fields remain readable but are polled at longer intervals.
 private final Map<String,Long> lastFieldGetMs=new HashMap<>();
 private final Set<String> fastFieldSet=new HashSet<>(Arrays.asList(FAST_FIELDS));
 private int fastCursor=0,slowCursor=0;
 private final Map<String,Long> lastExtraGetMs=new HashMap<>();
 // Priority is fixed by signal identity, never by whether a stationary value changes.
 private static final String[] PRIORITY_FIELDS={"vehicleSpeed","engineSpeed","currentTorque","longitudinalAcceleration","lateralAcceleration","absChargingAirPressure","relChargingAirPressure"};
 private static final Set<String> PRIORITY_FIELD_SET=new HashSet<>(Arrays.asList(PRIORITY_FIELDS));
 private int priorityCursor=0;
 private static final long PRIORITY_GET_INTERVAL_MS=100L;
 private static final long FAST_GET_INTERVAL_MS=250L;
 private static final long SLOW_GET_INTERVAL_MS=3500L;
 private static final long MEDIA_GET_INTERVAL_MS=1500L;
 private static final long EXTRA_GET_INTERVAL_MS=5000L;
 private Map<String,Object> scheduledGets(boolean skipSubscribed)throws Exception {
  Map<String,Object> result=new LinkedHashMap<>();
  // Bound the number of synchronous EXLAP requests to avoid saturating MU1101.
  // During Subscribe: no fast-field Gets unless Subscribe has failed.
  int priorityBudget=skipSubscribed?0:3;
  int fastBudget=skipSubscribed?0:2;
  int slowBudget=3;
  long now=System.currentTimeMillis();
  for(int category=0;category<3;category++){
   boolean priority=category==0;
   boolean fast=category!=2;
   int budget=priority?priorityBudget:(fast?fastBudget:slowBudget);
   int visited=0,done=0;
   while(visited<FIELDS.length && done<budget){
    int ix=priority?priorityCursor++:(fast?fastCursor++:slowCursor++);
    String key=FIELDS[Math.floorMod(ix,FIELDS.length)];visited++;
    if(priority){if(!PRIORITY_FIELD_SET.contains(key))continue;}
    else if(fast){if(!fastFieldSet.contains(key)||PRIORITY_FIELD_SET.contains(key))continue;}
    else if(fastFieldSet.contains(key))continue;
    if(skipSubscribed && subscribed.contains(key))continue;
    if(availableFields!=null && !availableFields.contains(key))continue;
    long interval=priority?PRIORITY_GET_INTERVAL_MS:(fast?FAST_GET_INTERVAL_MS:SLOW_GET_INTERVAL_MS);
    Long last=lastFieldGetMs.get(key);
    if(last!=null && now-last<interval)continue;
    lastFieldGetMs.put(key,now);done++;
    try{result.put(key,extract(request(++seq,"<Get url=\""+key+"\"/>",3)));}
    catch(SocketTimeoutException ex){throw ex;}
    catch(Exception ex){result.put(key,new ArrayList<Map<String,String>>());}
   }
  }
  // Audio metadata stays responsive; rarely changing HVAC metadata is slower.
  // One supplementary Get per cycle caps the overhead on busy head units.
  for(int scanned=0;scanned<EXTRA_FIELDS.length;scanned++){
   String key=EXTRA_FIELDS[extraIndex++%EXTRA_FIELDS.length];
   boolean media="Sound_Volume".equals(key)||"System_activeSource".equals(key)||
    "Media_PlayerState".equals(key)||"Media_Track".equals(key);
   if(!BridgeService.mediaExlapEnabled(context) && (media || key.startsWith("Media_") || key.startsWith("Sound_")))continue;
   long interval=media?MEDIA_GET_INTERVAL_MS:EXTRA_GET_INTERVAL_MS;
   Long last=lastExtraGetMs.get(key);
   if(last!=null && now-last<interval)continue;
   lastExtraGetMs.put(key,now);
   try{result.put(key,extract(request(++seq,"<Get url=\""+key+"\"/>",3)));}
   catch(SocketTimeoutException ex){throw ex;}
   catch(Exception ex){result.put(key,new ArrayList<Map<String,String>>());}
   break;
  }
  return result;
 }
 // Subscription events are merged with scheduled Get results. Preserve the proven
 // timeout / invalid-Subscribe fallback to Get-only operation.
 Map<String,Object> pollAdaptive()throws Exception {
  if(subscribed.isEmpty())return scheduledGets(false);
  Map<String,Object> result=drainPushes();
  if((pushEvents==0 && System.currentTimeMillis()-subscriptionStartedMs>10000) ||
     (lastPushMs>0 && System.currentTimeMillis()-lastPushMs>15000)){
   subscribed.clear();result.putAll(scheduledGets(false));return result;
  }
  result.putAll(scheduledGets(true));
  result.putAll(drainPushes());
  try{Element frame=readFrame(100);rememberDat(frame);}catch(SocketTimeoutException ignored){}
  result.putAll(drainPushes());return result;
 }

 int getDirectoryCount(){return directoryCount;}
 int getSupportedCount(){return availableFields==null?-1:availableFields.size();}
 // EXLAP Dir is read-only. A failed/unsupported directory falls back to the old poller.
 void discoverFields(){
  try {
   Element rsp=request(++seq,"<Dir urlPattern=\"*\" fromEntry=\"0\" numOfEntries=\"300\"/>",5);
   String status=rsp.getAttribute("status");
   if(!status.isEmpty()&&!"ok".equalsIgnoreCase(status))return;
   Set<String> urls=new HashSet<>();
   NodeList all=rsp.getElementsByTagName("*");
   for(int i=0;i<all.getLength();i++){
    Element e=(Element)all.item(i);String tag=e.getTagName();
    if(!"Object".equals(tag)&&!"Function".equals(tag)&&!"Entry".equals(tag))continue;
    String url=e.getAttribute("url");if(url.isEmpty())url=e.getAttribute("name");
    if(!url.isEmpty())urls.add(url);
   }
   if(urls.isEmpty())return;
   Set<String> found=new HashSet<>();
   for(String field:FIELDS)if(urls.contains(field))found.add(field);
   // Do not filter when directory is partial/unknown; avoid hiding real signals.
   if(!found.isEmpty()){availableFields=found;directoryCount=urls.size();}
  }catch(Exception ignored){}
 }
 static final String[] EXTRA_FIELDS={"Sound_Volume","System_activeSource","Media_PlayerState","Media_Track","Media_AvailableSources","seatHeater_zone1","seatHeater_zone2","seatHeater_zone3","seatHeater_zone4","hevacConfiguration","hevacFanLevelRear","temperature_control","temperatureRearLeft","temperatureRearRight"};
 // Android OEM XML parsers do not consistently implement Xerces feature URIs.
 // EXLAP frames have no need for DTDs or entities: reject declarations before parsing.
 private static Document parse(String xml)throws Exception {
  if (java.util.regex.Pattern.compile("<!\\s*(?:DOCTYPE|ENTITY)\\b", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(xml).find())
   throw new org.xml.sax.SAXException("DTD/entity declarations not allowed in EXLAP");
  DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
  f.setExpandEntityReferences(false);
  // Android DocumentBuilderFactory may throw UnsupportedOperationException even for false.
  // EXLAP parser already rejects DOCTYPE and ENTITY declarations above.
  // Do not call setXIncludeAware on Android.
  // Best effort only: unsupported feature URIs must not abort valid EXLAP parsing.
  String[] flags={"http://apache.org/xml/features/disallow-doctype-decl",
    "http://xml.org/sax/features/external-general-entities",
    "http://xml.org/sax/features/external-parameter-entities",
    "http://apache.org/xml/features/nonvalidating/load-external-dtd"};
  for(int i=0;i<flags.length;i++)try{f.setFeature(flags[i],i==0);}catch(ParserConfigurationException ignored){}
  try{f.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING,true);}catch(ParserConfigurationException ignored){}
  return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
 }
 private Element readFrame(long timeoutMs)throws Exception {long end=System.currentTimeMillis()+timeoutMs;byte[] b=new byte[4096];while(System.currentTimeMillis()<end){for(String tag:new String[]{"Rsp","Status","Dat"}){int p=buffer.indexOf("<"+tag);if(p>=0){int e=buffer.indexOf("</"+tag+">",p);int x=(e>=0?e+tag.length()+3:-1);if(x<0){int z=buffer.indexOf("/>",p);if(z>=0)x=z+2;}if(x>=0){String frame=buffer.substring(p,x);buffer.delete(0,x);return parse(frame).getDocumentElement();}}}int remaining=(int)Math.max(1,Math.min(1000,end-System.currentTimeMillis()));socket.setSoTimeout(remaining);try{int n=in.read(b);if(n<0)throw new EOFException("EXLAP disconnected");buffer.append(new String(b,0,n,StandardCharsets.UTF_8));if(buffer.length()>524288)throw new IOException("Oversized frame");}catch(SocketTimeoutException ignored){}}throw new SocketTimeoutException("EXLAP timeout");}
 private Element request(int id,String inner,int timeout)throws Exception {String q="<Req id=\""+id+"\">"+inner+"</Req>";out.write(q.getBytes(StandardCharsets.US_ASCII));out.flush();long end=System.currentTimeMillis()+timeout*1000L;while(System.currentTimeMillis()<end){Element e=readFrame(Math.max(1,end-System.currentTimeMillis()));if("Dat".equals(e.getTagName()))rememberDat(e);if("Rsp".equals(e.getTagName())&&String.valueOf(id).equals(e.getAttribute("id")))return e;}throw new SocketTimeoutException("Response id="+id);}
 private static Element first(Element e,String tag){NodeList a=e.getElementsByTagName(tag);return a.getLength()>0?(Element)a.item(0):null;}
 private static String prefix44(String s){return s.substring(0,Math.min(44,s.length()));}
 void connect()throws Exception {connectInternal(false);}
 void connectMedia()throws Exception {connectInternal(true);}
 private void connectInternal(boolean mediaOnly)throws Exception {if(!mediaOnly)BridgeService.phase="尋找車機 Wi-Fi";ConnectivityManager manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);Network wifi=null;for(Network n:manager.getAllNetworks()){NetworkCapabilities cap=manager.getNetworkCapabilities(n);if(cap!=null&&cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){wifi=n;break;}}String configuredHost=context.getSharedPreferences("JF_BRIDGE",Context.MODE_PRIVATE).getString("exlap_host","10.173.189.1");
 if(!configuredHost.matches("(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|[A-Za-z0-9][A-Za-z0-9.-]{0,252})"))throw new IOException("Invalid EXLAP host: check settings");
 if(username.isEmpty()||password.isEmpty())throw new IOException("Missing EXLAP credentials: see README and local-exlap.properties");
 if(!mediaOnly)BridgeService.phase="TCP 25010";Exception defaultFailure=null;
 try { if(!mediaOnly)BridgeService.phase="系統預設路由 TCP 25010"; socket=new Socket();socket.connect(new InetSocketAddress(configuredHost,25010),5000); }
 catch(Exception ex) { defaultFailure=ex;try{if(socket!=null)socket.close();}catch(Exception ignored){} if(!mediaOnly)BridgeService.phase="Wi-Fi 專用 TCP 25010（備援）";if(wifi==null)throw new IOException("預設路由連線失敗且未找到 Wi-Fi："+defaultFailure);socket=wifi.getSocketFactory().createSocket(); try{socket.connect(new InetSocketAddress(configuredHost,25010),5000);}catch(Exception wifiEx){throw new IOException("Default: "+defaultFailure+"; Wi-Fi: "+wifiEx,wifiEx);} }in=socket.getInputStream();out=socket.getOutputStream();if(!mediaOnly)BridgeService.phase="等待 EXLAP Init";Element greeting=readFrame(12000);if(!"Status".equals(greeting.getTagName())||first(greeting,"Init")==null)throw new IOException("Bad greeting");if(!mediaOnly)BridgeService.phase="EXLAP Protocol";if(first(request(99,"<Protocol version=\"1\" returnCapabilities=\"true\"/>",6),"Capabilities")==null)throw new IOException("No capabilities");if(!mediaOnly)BridgeService.phase="EXLAP Challenge";Element challenge=first(request(100,"<Authenticate phase=\"challenge\" useHash=\"sha256\"/>",6),"Challenge");if(challenge==null||challenge.getAttribute("nonce").isEmpty())throw new IOException("Challenge missing");String nonce=challenge.getAttribute("nonce");byte[] random=new byte[16];new SecureRandom().nextBytes(random);String cn=Base64.getEncoder().encodeToString(random);String joined=prefix44(username)+":"+prefix44(password)+":"+prefix44(nonce)+":"+prefix44(cn);String hash=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(joined.getBytes(StandardCharsets.UTF_8)));if(!mediaOnly)BridgeService.phase="EXLAP Authenticate";Element a=request(101,"<Authenticate phase=\"response\" user=\""+username+"\" cnonce=\""+cn+"\" digest=\""+hash+"\"/>",6);if(a.hasAttribute("status")&&!"ok".equals(a.getAttribute("status")))throw new IOException("Auth rejected"); if(!mediaOnly){BridgeService.phase="EXLAP 探測可用資料";discoverFields();BridgeService.phase="EXLAP 訂閱動態數據";subscribeFast();}}
 private List<Map<String,String>> extract(Element r) {
  List<Map<String,String>> vals=new ArrayList<>(); NodeList objects=r.getElementsByTagName("ObjectData");
  int count=objects.getLength()>0?objects.getLength():1;
  for(int i=0;i<count;i++){NodeList nodes=(objects.getLength()>0?(Element)objects.item(i):r).getElementsByTagName("*");
   for(int j=0;j<nodes.getLength();j++){Element n=(Element)nodes.item(j);
    if(!Arrays.asList("Abs","Rel","Enm","Bool","Int","Str","Txt","Act").contains(n.getTagName()))continue;
    if(!n.hasAttribute("val")||n.getAttribute("name").isEmpty())continue;
    Map<String,String> item=new LinkedHashMap<>();item.put("key",n.getAttribute("name"));item.put("value",n.getAttribute("val"));vals.add(item);
   }
  }return vals;
 }
 Map<String,Object> readMediaSnapshot()throws Exception {
  Map<String,Object> snapshot=new LinkedHashMap<>();
  for(String key:new String[]{"Media_Track","Media_PlayerState","System_activeSource","Sound_Volume"}){
   try {snapshot.put(key,extract(request(++seq,"<Get url=\""+key+"\"/>",3)));}
   catch(SocketTimeoutException ex){throw ex;}
   catch(Exception ex){snapshot.put(key,new ArrayList<Map<String,String>>());}
  }
  return snapshot;
 }
 String invokeMedia(String action)throws Exception {
  // Only allow documented media/sound functions, never arbitrary EXLAP calls.
  Set<String> allowed=new HashSet<>(Arrays.asList("Media_Play","Media_Pause","Media_NextTrack","Media_PreviousTrack","Sound_Mute","Sound_Unmute","Sound_IncreaseVolume","Sound_DecreaseVolume"));
  if(!allowed.contains(action))throw new IllegalArgumentException("Invalid media action");
  Element response=request(++seq,"<Call url=\""+action+"\"/>",5);
  String status=response.getAttribute("status");
  if(!status.isEmpty()&&!"ok".equalsIgnoreCase(status))throw new IOException("EXLAP "+action+": "+status);
  return status.isEmpty()?"Response received":status;
 }
 void close(){try{if(socket!=null)socket.close();}catch(Exception ignored){}}
}
