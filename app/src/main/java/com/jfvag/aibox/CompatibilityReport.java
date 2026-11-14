package com.jfvag.aibox;

import android.content.Context;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Map;
import java.util.List;

/** Read-only, credential-free diagnostic snapshot. Never sends control commands. */
final class CompatibilityReport {
 private CompatibilityReport() {}
 private static JSONObject item(String status,String description)throws Exception {
  JSONObject o=new JSONObject();o.put("status",status);o.put("detail",description);return o;
 }
 static JSONObject create(Context context, boolean connected, boolean paused, long lastGoodData,
                          int pollCycles, Map<String,Object> values, Map<String,Long> times, String connectionPhase)throws Exception {
  long now=System.currentTimeMillis();
  JSONObject out=new JSONObject();out.put("schema","jf-compatibility-1");
  out.put("generatedAtUtc",new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",java.util.Locale.US){{setTimeZone(java.util.TimeZone.getTimeZone("UTC"));}}.format(new java.util.Date(now)));
  out.put("note","Read-only snapshot. No credentials, tokens, VIN, IP addresses or raw vehicle readings are exported. Not a certification of compatibility.");
  JSONObject checks=new JSONObject();
  String discovery=ExlapDiscovery.find(context);boolean found=discovery.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}");
  checks.put("networkIp",item(found?"pass":"not_found",found?"EXLAP Init detected on connected Wi-Fi; host address omitted":"No EXLAP Init found among saved host, Wi-Fi gateway and known default; Wi-Fi or service may be unavailable"));
  boolean telemetryCredentialsConfigured=(!SecureCredentials.read(context,"telemetry_user").isEmpty()&&!SecureCredentials.read(context,"telemetry_password").isEmpty())||(!BuildConfig.EXLAP_USER.isEmpty()&&!BuildConfig.EXLAP_PASSWORD.isEmpty());
  checks.put("telemetryCredentials",item(telemetryCredentialsConfigured?"configured":"missing_credentials",telemetryCredentialsConfigured?"EXLAP reference profile is available; acceptance still requires a successful head-unit login":"No telemetry credentials configured. Open AI Box App > Connection settings > EXLAP authorization. Enter credentials you are authorized to use; IP discovery alone cannot authenticate."));
  checks.put("connectionStage",item(connected?"pass":(paused?"paused":"not_connected"),connected?"Current EXLAP connection authenticated":paused?"Bridge is paused":"No active telemetry session. Check telemetry credentials and the connection status in the Android App."));
  checks.put("exlapProtocol",item(connected?"pass":telemetryCredentialsConfigured?"unknown":"blocked_missing_credentials",connected?"EXLAP protocol handshake and authentication completed in current session":telemetryCredentialsConfigured?"No confirmed live EXLAP session":"Authentication cannot start without telemetry credentials"));
  checks.put("telemetryAuthorization",item(connected?"pass":telemetryCredentialsConfigured?"unknown":"blocked_missing_credentials",connected?"EXLAP authentication accepted; field-level access varies":telemetryCredentialsConfigured?"No successful login yet":"Set authorized telemetry credentials in the App"));
  int n=0;JSONArray readable=new JSONArray();
  for(String field:Exlap.FIELDS){Object v=values.get(field);Long t=times.get(field);
   if(v instanceof List && !((List<?>)v).isEmpty() && t!=null && t>0){n++;readable.put(field);}}
  boolean fresh=lastGoodData>0 && now-lastGoodData<15000;
  checks.put("vehicleData",item(n>0?(fresh?"pass":"stale"):"no_data",n+" / "+Exlap.FIELDS.length+" fields have previously returned nonempty data. Last successful update "+(lastGoodData>0?(now-lastGoodData)+" ms ago":"never")+". Completed poll cycles: "+pollCycles));
  boolean tpms=false;for(String k:new String[]{"tyrePressures","tyreRequiredPressures","tyreTemperatures","tyreStates"}){Object v=values.get(k);if(v instanceof List&&!((List<?>)v).isEmpty()){tpms=true;break;}}
  checks.put("tpms",item(tpms?"observed":"not_observed",tpms?"At least one tyre-related EXLAP field observed; four-wheel values not individually verified":"No tyre fields observed yet; this is not proof of unsupported TPMS"));
  boolean mediaConfigured=(!SecureCredentials.read(context,"media_user").isEmpty()&&!SecureCredentials.read(context,"media_password").isEmpty())||(!BuildConfig.EXLAP_MEDIA_USER.isEmpty()&&!BuildConfig.EXLAP_MEDIA_PASSWORD.isEmpty());
  checks.put("mediaControlPermission",item("not_tested",mediaConfigured?"Media reference profile available; media authorization and control NOT tested (no playback actions sent)":"Media credentials not configured; no playback actions sent"));
  checks.put("httpService",item("pass","Diagnostic endpoint responding on local dashboard"));
  out.put("checks",checks);out.put("observedFieldNames",readable);
  out.put("paused",paused);out.put("overall",connected&&fresh&&n>0?"telemetry_observed":"partial_or_unverified");
  return out;
 }
}
