package com.jfvag.aibox;
import android.app.*;import android.content.*;import android.net.wifi.*;import android.os.*;import android.graphics.Color;import android.graphics.Bitmap;import android.graphics.BitmapFactory;import com.google.zxing.BarcodeFormat;import com.google.zxing.EncodeHintType;import com.google.zxing.WriterException;import com.google.zxing.common.BitMatrix;import com.google.zxing.qrcode.QRCodeWriter;import android.view.*;import android.widget.*;import android.webkit.WebView;import android.webkit.WebSettings;import android.webkit.WebViewClient;import android.webkit.WebResourceRequest;import android.webkit.WebResourceError;import android.webkit.WebResourceResponse;import java.net.*;import java.util.*;
public class MainActivity extends Activity {
 private static final int JF_MIRROR_REQUEST=7107;
 private void requestMirror(){try{android.media.projection.MediaProjectionManager m=(android.media.projection.MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),JF_MIRROR_REQUEST);}catch(Exception e){Toast.makeText(this,"此 AI Box 不支援畫面擷取："+e.getMessage(),Toast.LENGTH_LONG).show();}}

 TextView status,link;LanQrPanel lanQrPanel;ImageView qrView;WebView web;LinearLayout outer,settingsPanel;String activeTab="status";TextView qrNote;Handler h=new Handler();private Runnable bridgeRestartPending;private int hostChangeRevision=0;private volatile boolean dashboardTrusted=false;private boolean licenseTransitioning=false;private int dashboardLoadAttempts=0;private static final int DASHBOARD_MAX_ATTEMPTS=8;String url="";String ip="";String lastQrUrl="";
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(30,40,30,25);l.setBackgroundColor(Color.rgb(15,23,36));TextView heading=new TextView(this);heading.setText("JF VAG");heading.setTextColor(Color.WHITE);heading.setTextSize(26);l.addView(heading);TextView sub=new TextView(this);sub.setText("連線與 Safari");sub.setTextColor(Color.LTGRAY);l.addView(sub);
 TextView hostLabel=new TextView(this);hostLabel.setText("MU1101 EXLAP IP / Host (port 25010)");hostLabel.setTextColor(Color.WHITE);l.addView(hostLabel);
 EditText hostInput=new EditText(this);hostInput.setSingleLine(true);hostInput.setText(getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).getString("exlap_host","10.173.189.1"));hostInput.setTextColor(Color.WHITE);l.addView(hostInput);
 Button saveHost=new Button(this);saveHost.setText("儲存主機位址並重新連線");saveHost.setOnClickListener(v->{String host=hostInput.getText().toString().trim();if(host.isEmpty()||host.contains(":")||host.contains("/")||host.contains(" ")){Toast.makeText(this,"請輸入 IP 或主機名稱，不含 port",Toast.LENGTH_LONG).show();return;}getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).edit().putString("exlap_host",host).apply();hostChangeRevision++;restartBridge();Toast.makeText(this,"已儲存，重新連線中",Toast.LENGTH_SHORT).show();});l.addView(saveHost);
 TextView discoverNote=new TextView(this);discoverNote.setText("一般使用不需修改主機位址。");discoverNote.setTextColor(Color.LTGRAY);l.addView(discoverNote);
 Button autoFind=new Button(this);autoFind.setText("自動偵測 MU1101 IP");autoFind.setOnClickListener(v->{autoFind.setEnabled(false);final int requestRevision=hostChangeRevision;discoverNote.setText("正在檢查目前 Wi-Fi 的 EXLAP 服務…");new Thread(()->{String found=ExlapDiscovery.find(this);runOnUiThread(()->{autoFind.setEnabled(true);if(requestRevision!=hostChangeRevision){discoverNote.setText("手動設定已更新，忽略較早的偵測結果");return;}if(found.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")){hostInput.setText(found);hostChangeRevision++;getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).edit().putString("exlap_host",found).apply();discoverNote.setText("找到 EXLAP 服務："+found+"。已儲存，正在重新連線。");restartBridge();}else discoverNote.setText(found+"。請確認 AI Box 已連車機 Wi-Fi，或手動輸入 IP。 無法自動取得憑證。");});}).start();});l.addView(autoFind);
 TextView credentialNote=new TextView(this);credentialNote.setText("EXLAP 授權資料（通常不必修改；只有登入失敗或更換車機時才設定。車輛數據／原車媒體各使用自己的授權資料，保存在 Android 本機）");credentialNote.setTextColor(Color.WHITE);l.addView(credentialNote);
 EditText telemetryUser=new EditText(this),telemetryPass=new EditText(this),mediaUser=new EditText(this),mediaPass=new EditText(this);
 telemetryUser.setHint("車輛數據帳號");telemetryPass.setHint("車輛數據密碼");mediaUser.setHint("媒體控制帳號");mediaPass.setHint("媒體控制密碼");
 for(EditText e:new EditText[]{telemetryUser,telemetryPass,mediaUser,mediaPass}){e.setSingleLine(true);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.LTGRAY);l.addView(e);}
 telemetryPass.setInputType(129);mediaPass.setInputType(129);
 Button saveCredentials=new Button(this);saveCredentials.setText("儲存授權資料並重新連線");saveCredentials.setOnClickListener(v->{try{
  if(!telemetryUser.getText().toString().isEmpty())SecureCredentials.save(this,"telemetry_user",telemetryUser.getText().toString());
  if(!telemetryPass.getText().toString().isEmpty())SecureCredentials.save(this,"telemetry_password",telemetryPass.getText().toString());
  if(!mediaUser.getText().toString().isEmpty())SecureCredentials.save(this,"media_user",mediaUser.getText().toString());
  if(!mediaPass.getText().toString().isEmpty())SecureCredentials.save(this,"media_password",mediaPass.getText().toString());
  telemetryUser.setText("");telemetryPass.setText("");mediaUser.setText("");mediaPass.setText("");
  restartBridge();
  Toast.makeText(this,"授權資料已加密保存在本機",Toast.LENGTH_SHORT).show();
 }catch(Exception e){Toast.makeText(this,"無法儲存："+e.getMessage(),Toast.LENGTH_LONG).show();}});l.addView(saveCredentials);
 status=new TextView(this);status.setTextColor(Color.WHITE);status.setTextSize(16);status.setPadding(0,30,0,20);l.addView(status);Button start=new Button(this);start.setText("啟動 Bridge");start.setOnClickListener(v->{startService(new Intent(this,BridgeService.class));refresh();});l.addView(start);Button stop=new Button(this);stop.setText("停止 Bridge");stop.setOnClickListener(v->{stopService(new Intent(this,BridgeService.class));refresh();});l.addView(stop);link=new TextView(this);link.setTextColor(Color.CYAN);link.setTextSize(14);link.setTextIsSelectable(true);link.setPadding(0,25,0,25);l.addView(link);qrNote=new TextView(this);qrNote.setText("手機掃描 QR Code；Windows 可在 Edge / Chrome 輸入同一網址");qrNote.setTextColor(Color.WHITE);qrNote.setTextSize(16);qrNote.setGravity(Gravity.CENTER);l.addView(qrNote);qrView=new ImageView(this);qrView.setBackgroundColor(Color.WHITE);qrView.setPadding(12,12,12,12);qrView.setScaleType(ImageView.ScaleType.FIT_CENTER);LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(dp(280),dp(280));qp.gravity=Gravity.CENTER_HORIZONTAL;qp.topMargin=dp(12);qp.bottomMargin=dp(12);l.addView(qrView,qp);Button redraw=new Button(this);redraw.setText("重新顯示 QR Code");redraw.setOnClickListener(v->{lastQrUrl="";renderQr();});l.addView(redraw);Button copy=new Button(this);copy.setText("複製固定連結");copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("JF Dashboard",url));Toast.makeText(this,"連結已複製",Toast.LENGTH_SHORT).show();});l.addView(copy);Button open=new Button(this);open.setText("開啟 Dashboard");open.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(Exception e){Toast.makeText(this,"無瀏覽器可以開啟",Toast.LENGTH_SHORT).show();}});l.addView(open);TextView note=new TextView(this);note.setText("手機或 Windows 必須與 AI Box 在可互通的區域網路，使用相同網址。");note.setTextColor(Color.LTGRAY);note.setPadding(0,20,0,0);l.addView(note);// Keep rarely used credentials and host adjustments, but hide them behind a deliberate advanced toggle.
 LinearLayout advancedPanel=new LinearLayout(this);advancedPanel.setOrientation(1);advancedPanel.setPadding(0,dp(8),0,dp(14));
 View[] advancedViews=new View[]{hostLabel,hostInput,saveHost,discoverNote,autoFind,credentialNote,telemetryUser,telemetryPass,mediaUser,mediaPass,saveCredentials};
 for(View item:advancedViews){l.removeView(item);advancedPanel.addView(item);}
 Button advancedToggle=new Button(this);advancedToggle.setText("進階連線設定（IP・EXLAP 授權帳密） ▾");
 advancedPanel.setVisibility(View.GONE);
 advancedToggle.setOnClickListener(v->{boolean show=advancedPanel.getVisibility()!=View.VISIBLE;advancedPanel.setVisibility(show?View.VISIBLE:View.GONE);advancedToggle.setText(show?"收起進階連線設定 ▴":"進階連線設定（IP・EXLAP 授權帳密） ▾");});
 l.addView(advancedToggle);l.addView(advancedPanel);
 // Replace the legacy single-IP QR with two explicitly labeled, validated LAN paths.
 // A network that isn't currently assigned to this AI Box must not display a fake QR.
 for(View entry:new View[]{link,qrNote,qrView,redraw,copy,open,note})l.removeView(entry);
 lanQrPanel=new LanQrPanel(this);
 Button networkToggle=new Button(this);networkToggle.setAllCaps(false);networkToggle.setText("Wi-Fi 與 Safari Bridge QR Code ▾");
 lanQrPanel.setVisibility(View.GONE);
 networkToggle.setOnClickListener(v->{boolean visible=lanQrPanel.getVisibility()!=View.VISIBLE;lanQrPanel.setVisibility(visible?View.VISIBLE:View.GONE);networkToggle.setText(visible?"Wi-Fi 與 Safari Bridge QR Code ▴":"Wi-Fi 與 Safari Bridge QR Code ▾");if(visible)lanQrPanel.refresh();});
 l.addView(networkToggle);l.addView(lanQrPanel);
 // V5.70.11: one consistent native settings hierarchy, without duplicate QR panels.
 // Reparent existing views so all event listeners and Bridge/EXLAP behavior remain unchanged.
 l.removeAllViews();
 l.setPadding(dp(22),dp(15),dp(22),dp(24));l.setBackgroundColor(0xff101721);
 heading.setText("JF AIBox Bridge");heading.setTextSize(25);heading.setTypeface(null,android.graphics.Typeface.BOLD);
 sub.setText("設定中心  ·  開源版");sub.setTextSize(13);sub.setTextColor(0xff9fadb9);
 l.addView(heading);l.addView(sub);jfSettingsRoot=l;
 LinearLayout nativeConnectivity=jfSettingsCard("連線狀態", "查看 Bridge 運作情況及服務控制");
 status.setPadding(dp(4),dp(4),dp(4),dp(8));status.setTextSize(14);nativeConnectivity.addView(status);
 LinearLayout nativeControls=new LinearLayout(this);nativeControls.setOrientation(LinearLayout.HORIZONTAL);
 LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(48),1);bp.setMargins(dp(3),0,dp(3),0);
 jfStyleAction(start,false);jfStyleAction(stop,false);nativeControls.addView(start,new LinearLayout.LayoutParams(bp));nativeControls.addView(stop,new LinearLayout.LayoutParams(bp));nativeConnectivity.addView(nativeControls);
 LinearLayout nativeNetwork=jfSettingsCard("Safari 與網路", "車機 Wi-Fi／手機 B 熱點 · 掃碼連線");
 jfStyleAction(networkToggle,false);nativeNetwork.addView(networkToggle);nativeNetwork.addView(lanQrPanel);
 LinearLayout touchCard=jfSettingsCard("iPhone 觸控板", "區域網路直接連線；首次需在 Android 設定啟用無障礙服務");
 TextView touchStatus=new TextView(this);touchStatus.setTextColor(0xffe5edf6);touchStatus.setTextSize(15);touchCard.addView(touchStatus);
 Runnable refreshTouch=()->touchStatus.setText("無須配對碼  ·  "+(getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).getBoolean("touchpad_enabled",true)?"已開啟":"已關閉")+"  ·  Safari 連線埠 18777");refreshTouch.run();
 Button touchToggle=new Button(this);touchToggle.setText("開啟／關閉 Touchpad");jfStyleAction(touchToggle,false);touchToggle.setOnClickListener(v->{boolean enabled=!getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).getBoolean("touchpad_enabled",true);getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).edit().putBoolean("touchpad_enabled",enabled).apply();refreshTouch.run();Toast.makeText(this,enabled?"Touchpad 已開啟":"Touchpad 已關閉",Toast.LENGTH_SHORT).show();});touchCard.addView(touchToggle);
 Button touchAccess=new Button(this);touchAccess.setText("無障礙服務授權");jfStyleAction(touchAccess,false);touchAccess.setOnClickListener(v->startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)));touchCard.addView(touchAccess);
 Button touchPair=new Button(this);touchPair.setText("重新產生舊版配對碼（新版不需要）");jfStyleAction(touchPair,false);touchPair.setOnClickListener(v->{getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).edit().remove("touchpad_pair").apply();refreshTouch.run();});touchCard.addView(touchPair);
 // Cards were already attached by jfSettingsCard(); do not attach twice.
 LinearLayout nativeAdvanced=jfSettingsCard("進階連線", "僅排除故障或更換 MU1101 時使用");
 jfStyleAction(advancedToggle,false);nativeAdvanced.addView(advancedToggle);nativeAdvanced.addView(advancedPanel);

 // V5.70.19: settings overview is a fixed landscape grid. Detailed controls open
 // in dialogs, so no setting can silently extend the main screen below the fold.
 final boolean landscapeSettings=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
 if(landscapeSettings){
   lanQrPanel.setVisibility(View.GONE);advancedPanel.setVisibility(View.GONE);
   networkToggle.setText("顯示 Safari QR Code");advancedToggle.setText("開啟進階連線");
   networkToggle.setOnClickListener(v->{lanQrPanel.refresh();showSettingsDetail(lanQrPanel,"Safari 與網路 QR Code");});
   advancedToggle.setOnClickListener(v->showSettingsDetail(advancedPanel,"進階連線設定"));
   // Keep the full diagnostic text in memory but avoid four lines consuming the overview.
   status.setVisibility(View.GONE);
   TextView connectionHint=new TextView(this);connectionHint.setText("Bridge 狀態請至 Safari 儀表板查看");connectionHint.setTextColor(0xffb8c6d7);connectionHint.setTextSize(12);
   nativeConnectivity.addView(connectionHint,0);
   for(LinearLayout card:new LinearLayout[]{nativeConnectivity,nativeNetwork,touchCard,nativeAdvanced}){
     card.setPadding(dp(10),dp(8),dp(10),dp(8));
     if(card.getChildCount()>1 && card.getChildAt(1) instanceof TextView)card.getChildAt(1).setVisibility(View.GONE);
     if(card.getChildAt(0) instanceof TextView)((TextView)card.getChildAt(0)).setTextSize(15);
   }
   // Touchpad operations remain on this screen, in two rows instead of a tall stack.
   touchCard.removeView(touchToggle);touchCard.removeView(touchAccess);touchCard.removeView(touchPair);
   LinearLayout touchRow=new LinearLayout(this);touchRow.setOrientation(LinearLayout.HORIZONTAL);
   touchToggle.setText("開關 Touchpad");touchAccess.setText("無障礙授權");
   touchRow.addView(touchToggle,new LinearLayout.LayoutParams(0,dp(44),1));
   touchRow.addView(touchAccess,new LinearLayout.LayoutParams(0,dp(44),1));
   touchCard.addView(touchRow);touchPair.setText("重新配對");touchCard.addView(touchPair,new LinearLayout.LayoutParams(-1,dp(40)));
   touchStatus.setTextSize(12);
 }
 LinearLayout settingsColumns=new LinearLayout(this);settingsColumns.setOrientation(LinearLayout.HORIZONTAL);
 settingsColumns.setPadding(dp(4),0,dp(4),0);
 LinearLayout[] columns=new LinearLayout[3];
 for(int i=0;i<3;i++){
   columns[i]=new LinearLayout(this);columns[i].setOrientation(LinearLayout.VERTICAL);
   columns[i].setPadding(dp(4),0,dp(4),0);
   settingsColumns.addView(columns[i],new LinearLayout.LayoutParams(0,-2,1));
 }
 for(LinearLayout card:new LinearLayout[]{nativeConnectivity,nativeNetwork,touchCard,nativeAdvanced})l.removeView(card);
 columns[0].addView(nativeConnectivity);columns[0].addView(touchCard);
 columns[1].addView(nativeNetwork);
 columns[2].addView(nativeAdvanced);
 l.addView(settingsColumns);
 ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(l);settingsPanel=l;
 if(landscapeSettings){
   // The root is short enough to fit; detailed views are opened in their own dialog.
   scroll.setVerticalScrollBarEnabled(false); // Allow emergency scrolling on short head-unit screens.
   l.setPadding(dp(8),dp(3),dp(8),dp(3));heading.setTextSize(18);sub.setTextSize(10);
 }
 outer=new LinearLayout(this);outer.setOrientation(1);outer.setBackgroundColor(Color.rgb(15,23,36));
 web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.setBackgroundColor(Color.rgb(11,17,26));
 web.getSettings().setAllowFileAccess(false);
 web.getSettings().setAllowContentAccess(false);
 web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
 web.setWebViewClient(new WebViewClient(){
   @Override public void onPageStarted(WebView view,String page,android.graphics.Bitmap favicon){dashboardTrusted=false;super.onPageStarted(view,page,favicon);}
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !isTrustedDashboardUrl(request.getUrl()==null?null:request.getUrl().toString());}
   @Override public boolean shouldOverrideUrlLoading(WebView view,String next){return !isTrustedDashboardUrl(next);}
   @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){
     if(request!=null && request.isForMainFrame())retryDashboard();
   }
   @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,WebResourceResponse response){
     if(request!=null && request.isForMainFrame() && response!=null && response.getStatusCode()>=500)retryDashboard();
   }
   @Override public void onPageFinished(WebView view,String page){
     dashboardTrusted=isTrustedDashboardUrl(page);if(dashboardTrusted)dashboardLoadAttempts=0;
   }
 });
 web.addJavascriptInterface(new Object(){@android.webkit.JavascriptInterface public void openMediaAccessSettings(){if(!trustedDashboard())return ;runOnUiThread(()->{try{startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));}catch(Exception e){Toast.makeText(MainActivity.this,"請在系統設定中開啟通知存取權",Toast.LENGTH_LONG).show();}});}@android.webkit.JavascriptInterface public void openGoogleMaps(){if(!trustedDashboard())return ;runOnUiThread(()->{try{Intent m=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://www.google.com/maps"));startActivity(m);}catch(Exception e){Toast.makeText(MainActivity.this,"無法開啟 Google Maps",Toast.LENGTH_LONG).show();}});}@android.webkit.JavascriptInterface public void openCurrentMediaApp(){if(!trustedDashboard())return ;runOnUiThread(()->{
   // This bridge is only available inside the AI Box WebView. Never accept a package name from JavaScript.
   if(web==null||web.getUrl()==null||!web.getUrl().startsWith("http://127.0.0.1:8765/"))return;
   try{
     org.json.JSONObject current=MediaSessionReader.read(MainActivity.this);
     if(!current.optBoolean("permission")||!current.optBoolean("available")){
       Toast.makeText(MainActivity.this,"目前沒有可開啟的 Android 播放器，請先播放音樂",Toast.LENGTH_SHORT).show();return;
     }
     String pkg=current.optString("packageName","");
     if(pkg.isEmpty()||pkg.equals(getPackageName())){
       Toast.makeText(MainActivity.this,"找不到目前播放 App",Toast.LENGTH_SHORT).show();return;
     }
     Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
     if(launch==null){Toast.makeText(MainActivity.this,"此播放器沒有可開啟的 App 入口",Toast.LENGTH_LONG).show();return;}
     startActivity(launch);
   }catch(Exception e){Toast.makeText(MainActivity.this,"無法開啟目前播放器",Toast.LENGTH_LONG).show();}
 });}@android.webkit.JavascriptInterface public String getAppShortcuts(){if(!trustedDashboard())return "";
   return AppShortcuts.json(MainActivity.this);
 }@android.webkit.JavascriptInterface public void launchAppShortcut(String pkg){if(!trustedDashboard())return ;
   runOnUiThread(()->{
     if(web==null||web.getUrl()==null||!web.getUrl().startsWith("http://127.0.0.1:8765/"))return;
     AppShortcuts.launch(MainActivity.this,pkg);
   });
 }@android.webkit.JavascriptInterface public void configureAppShortcuts(){if(!trustedDashboard())return ;
   runOnUiThread(()->{
     if(web==null||web.getUrl()==null||!web.getUrl().startsWith("http://127.0.0.1:8765/"))return;
     AppShortcuts.configure(MainActivity.this,()->{
       if(web!=null)web.evaluateJavascript("window.JFRefreshShortcuts&&window.JFRefreshShortcuts()",null);
     });
   });
 }@android.webkit.JavascriptInterface public void editAppShortcut(String pkg){if(!trustedDashboard())return ;
   runOnUiThread(()->{
     if(web==null||web.getUrl()==null||!web.getUrl().startsWith("http://127.0.0.1:8765/"))return;
     AppShortcuts.editSlot(MainActivity.this,pkg,()->{
       if(web!=null)web.evaluateJavascript("window.JFRefreshShortcuts&&window.JFRefreshShortcuts()",null);
     });
   });
 }@android.webkit.JavascriptInterface public void showQr(){if(!trustedDashboard())return ;runOnUiThread(()->{if(lanQrPanel!=null){lanQrPanel.refresh();android.app.AlertDialog qrDialog=new AlertDialog.Builder(MainActivity.this).setTitle("Safari 手機連線").setView(new LanQrPanel(MainActivity.this,true)).setPositiveButton("完成",(d,w)->{}).create();qrDialog.show();if(qrDialog.getWindow()!=null){android.util.DisplayMetrics dm=getResources().getDisplayMetrics();qrDialog.getWindow().setLayout((int)(dm.widthPixels*0.88f),android.view.WindowManager.LayoutParams.WRAP_CONTENT);}}});}
 @android.webkit.JavascriptInterface public String exlapHost(){if(!trustedDashboard())return "";return getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).getString("exlap_host","10.173.189.1");}
 @android.webkit.JavascriptInterface public void saveExlapHost(String host){if(!trustedDashboard())return ;runOnUiThread(()->{String v=host==null?"":host.trim();if(!v.matches("[A-Za-z0-9.-]{1,253}")||v.startsWith("-")||v.endsWith("-")){Toast.makeText(MainActivity.this,"主機位址格式不正確",Toast.LENGTH_LONG).show();return;}hostChangeRevision++;getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).edit().putString("exlap_host",v).apply();restartBridge();});}
 @android.webkit.JavascriptInterface public void discoverExlap(){if(!trustedDashboard())return ;runOnUiThread(()->{final int rev=hostChangeRevision;new Thread(()->{String found=ExlapDiscovery.find(MainActivity.this);runOnUiThread(()->{if(rev!=hostChangeRevision)return;if(found.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")){hostChangeRevision++;getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).edit().putString("exlap_host",found).apply();restartBridge();}Toast.makeText(MainActivity.this,found,Toast.LENGTH_LONG).show();});}).start();});}
 @android.webkit.JavascriptInterface public void saveExlapCredentials(String tu,String tp,String mu,String mp){if(!trustedDashboard())return ;runOnUiThread(()->{try{if(tu!=null&&!tu.isEmpty())SecureCredentials.save(MainActivity.this,"telemetry_user",tu);if(tp!=null&&!tp.isEmpty())SecureCredentials.save(MainActivity.this,"telemetry_password",tp);if(mu!=null&&!mu.isEmpty())SecureCredentials.save(MainActivity.this,"media_user",mu);if(mp!=null&&!mp.isEmpty())SecureCredentials.save(MainActivity.this,"media_password",mp);restartBridge();Toast.makeText(MainActivity.this,"授權資料已加密儲存",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(MainActivity.this,"儲存失敗",Toast.LENGTH_LONG).show();}});}
 @android.webkit.JavascriptInterface public void configureTouchpadApps(){if(!trustedDashboard())return;runOnUiThread(()->AppShortcuts.configure(MainActivity.this,()->{}));}
 @android.webkit.JavascriptInterface public void startTouchpadMirror(){if(!trustedDashboard())return;runOnUiThread(()->requestMirror());}
 @android.webkit.JavascriptInterface public void stopTouchpadMirror(){if(!trustedDashboard())return;runOnUiThread(()->MirrorCapture.stop());}
 @android.webkit.JavascriptInterface public boolean touchpadEnabled(){if(!trustedDashboard())return false;return getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).getBoolean("touchpad_enabled",true);}
 @android.webkit.JavascriptInterface public String touchpadPairCode(){if(!trustedDashboard())return "";return TouchpadServer.code(MainActivity.this);}
 @android.webkit.JavascriptInterface public void setTouchpadEnabled(boolean enabled){if(!trustedDashboard())return;runOnUiThread(()->{getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).edit().putBoolean("touchpad_enabled",enabled).apply();restartBridge();Toast.makeText(MainActivity.this,enabled?"Touchpad 已啟用，正在重新啟動服務":"Touchpad 已停用",Toast.LENGTH_SHORT).show();});}
 @android.webkit.JavascriptInterface public void resetTouchpadPair(){if(!trustedDashboard())return;runOnUiThread(()->{getSharedPreferences("JF_BRIDGE",MODE_PRIVATE).edit().remove("touchpad_pair").apply();Toast.makeText(MainActivity.this,"已更新 Touchpad 配對碼",Toast.LENGTH_SHORT).show();});}
 @android.webkit.JavascriptInterface public void openTouchpadAccessibility(){if(!trustedDashboard())return;runOnUiThread(()->startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)));}
 @android.webkit.JavascriptInterface public String bridgeState(){if(!trustedDashboard())return "";return (BridgeService.running?"HTTP 執行中":"HTTP 已停止")+" · "+(BridgeService.connected?"EXLAP 已連線":"EXLAP 未連線")+" · "+BridgeService.phase;}
 @android.webkit.JavascriptInterface public void openWifiSettings(){if(!trustedDashboard())return ;runOnUiThread(()->{try{startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));}catch(Exception e){try{startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));}catch(Exception ignored){Toast.makeText(MainActivity.this,"無法開啟 Wi-Fi 設定",Toast.LENGTH_LONG).show();}}});}
 @android.webkit.JavascriptInterface public void openAdvancedFallback(){if(!trustedDashboard())return ;runOnUiThread(()->showSettings(true));}@android.webkit.JavascriptInterface public void startBridge(){if(!trustedDashboard())return ;runOnUiThread(()->startService(new Intent(MainActivity.this,BridgeService.class)));}@android.webkit.JavascriptInterface public void stopBridge(){if(!trustedDashboard())return ;runOnUiThread(()->stopService(new Intent(MainActivity.this,BridgeService.class)));}},"JFHost");
 Button back=new Button(this);back.setText("← 返回儀表板");jfStyleAction(back,true);back.setOnClickListener(v->showSettings(false));l.addView(back,0);
 outer.addView(web,new LinearLayout.LayoutParams(-1,0,1));outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(outer);
 showSettings(false);startService(new Intent(this,BridgeService.class));h.postDelayed(this::loadDashboard,900);refresh();}
 void showSettingsDetail(View detail,String title){
   android.view.ViewGroup parent=(android.view.ViewGroup)detail.getParent();
   int index=parent.indexOfChild(detail);parent.removeView(detail);
   detail.setVisibility(View.VISIBLE);
   ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setPadding(dp(10),dp(8),dp(10),dp(8));
   sc.addView(detail,new ScrollView.LayoutParams(-1,-2));
   AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(sc).setPositiveButton("完成",null).create();
   dialog.setOnDismissListener(d->{sc.removeView(detail);detail.setVisibility(View.GONE);parent.addView(detail,Math.min(index,parent.getChildCount()));});
   dialog.show();
   android.view.Window window=dialog.getWindow();if(window!=null){window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.84f),(int)(getResources().getDisplayMetrics().heightPixels*.88f));}
 }
 private boolean isTrustedDashboardUrl(String u){
   if(u==null)return false;
   try{android.net.Uri x=android.net.Uri.parse(u);return "http".equalsIgnoreCase(x.getScheme()) && "127.0.0.1".equals(x.getHost()) && x.getPort()==8765 && x.getUserInfo()==null;}
   catch(Exception e){return false;}
 }
 private boolean trustedDashboard(){return dashboardTrusted;}
 private void loadDashboard(){if(web==null||isFinishing()||isDestroyed())return;web.loadUrl("http://127.0.0.1:8765/?token="+BridgeService.token(this));}
 private void retryDashboard(){if(dashboardLoadAttempts>=DASHBOARD_MAX_ATTEMPTS)return;dashboardLoadAttempts++;h.postDelayed(this::loadDashboard,Math.min(5000,400+dashboardLoadAttempts*500));}
 void restartBridge(){
 if(bridgeRestartPending!=null)h.removeCallbacks(bridgeRestartPending);
 stopService(new Intent(this,BridgeService.class));
 bridgeRestartPending=()->{bridgeRestartPending=null;if(!isFinishing()&&!isDestroyed())startService(new Intent(this,BridgeService.class));};
 h.postDelayed(bridgeRestartPending,1200);
}
 View detachForDialog(View v){if(v.getParent() instanceof android.view.ViewGroup){qrOriginalParent=(android.view.ViewGroup)v.getParent();qrOriginalIndex=qrOriginalParent.indexOfChild(v);qrOriginalParent.removeView(v);}return v;}
 void restoreDialogView(View v){if(v.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)v.getParent()).removeView(v);if(qrOriginalParent!=null && v.getParent()==null)qrOriginalParent.addView(v,Math.min(qrOriginalIndex,qrOriginalParent.getChildCount()));}
 void showSettings(boolean show){web.setVisibility(show?View.GONE:View.VISIBLE);((View)settingsPanel.getParent()).setVisibility(show?View.VISIBLE:View.GONE);}
 String currentIp(){try{Enumeration<NetworkInterface> nets=NetworkInterface.getNetworkInterfaces();String fallback="";while(nets.hasMoreElements()){NetworkInterface n=nets.nextElement();if(!n.isUp()||n.isLoopback())continue;Enumeration<InetAddress> ads=n.getInetAddresses();while(ads.hasMoreElements()){InetAddress a=ads.nextElement();if(a instanceof Inet4Address&&!a.isLoopbackAddress()){String x=a.getHostAddress();if(x.startsWith("10.173.189."))return x;if(!x.startsWith("169.254."))fallback=x;}}}return fallback.isEmpty()?"AI_BOX_IP":fallback;}catch(Exception e){return "AI_BOX_IP";}}
 void refresh(){ip=currentIp();url="http://"+ip+":8765/?token="+BridgeService.token(this);status.setText("HTTP："+(BridgeService.running?"執行中":"已停止")+"\nEXLAP："+(BridgeService.connected?"已連線":"未連線")+"\n階段："+BridgeService.phase+"\n錯誤："+BridgeService.error+"\n更新時間："+(BridgeService.updated==0?"尚無資料":new java.text.SimpleDateFormat("HH:mm:ss",Locale.TAIWAN).format(new Date(BridgeService.updated))));link.setText("手機連結：\n"+url);if(lanQrPanel!=null && lanQrPanel.getVisibility()==View.VISIBLE)lanQrPanel.refresh();h.postDelayed(this::refresh,2000);}
 private LinearLayout jfSettingsCard(String title,String subtitle){
   LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(15),dp(13),dp(15),dp(15));
   android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xff1e2a3a,0xff17222f});bg.setCornerRadius(dp(17));bg.setStroke(dp(1),0xff364353);card.setBackground(bg);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(13);settingsAppendCard(card,lp);
   TextView h1=new TextView(this);h1.setText(title);h1.setTextSize(18);h1.setTypeface(null,android.graphics.Typeface.BOLD);h1.setTextColor(0xfff2f5fb);card.addView(h1);
   TextView h2=new TextView(this);h2.setText(subtitle);h2.setTextSize(12);h2.setTextColor(0xffa6b3c3);h2.setPadding(0,dp(4),0,dp(10));card.addView(h2);
   return card;
 }
 private LinearLayout jfSettingsRoot;
 private android.view.ViewGroup qrOriginalParent; private int qrOriginalIndex;
 private void settingsAppendCard(LinearLayout card,LinearLayout.LayoutParams lp){
   if(jfSettingsRoot!=null)jfSettingsRoot.addView(card,lp);
 }
 private void jfStyleAction(Button button,boolean highlight){
   button.setAllCaps(false);button.setTextSize(13);button.setTextColor(highlight?0xff15171b:0xffe9f1fa);
   android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(highlight?0xffe7a45e:0xff29384b);bg.setCornerRadius(dp(12));bg.setStroke(dp(1),highlight?0xffefb87e:0xff40536a);button.setBackground(bg);
   button.setPadding(dp(8),dp(6),dp(8),dp(6));
 }
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 void renderQr(){try{if(url.contains("AI_BOX_IP")){qrNote.setText("等待 AI Box 取得有效的 IP 位址");qrView.setImageDrawable(null);lastQrUrl=url;return;}Map<EncodeHintType,Object> hints=new EnumMap<>(EncodeHintType.class);hints.put(EncodeHintType.CHARACTER_SET,"UTF-8");hints.put(EncodeHintType.MARGIN,2);BitMatrix bits=new QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,640,640,hints);int w=bits.getWidth(),hh=bits.getHeight();int[] pixels=new int[w*hh];for(int y=0;y<hh;y++)for(int x=0;x<w;x++)pixels[y*w+x]=bits.get(x,y)?Color.BLACK:Color.WHITE;Bitmap bmp=Bitmap.createBitmap(w,hh,Bitmap.Config.ARGB_8888);bmp.setPixels(pixels,0,w,0,0,w,hh);qrView.setImageBitmap(bmp);qrNote.setText("iPhone 掃描 QR Code 開啟 Safari（手機需能連到盒子）");lastQrUrl=url;}catch(Exception e){qrNote.setText("QR Code 產生失敗："+e.getMessage());}}
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
     super.onActivityResult(requestCode,resultCode,data);
     if(requestCode==JF_MIRROR_REQUEST){
         if(resultCode==RESULT_OK && data!=null){
             MirrorCapture.start(this,resultCode,data);
             Toast.makeText(this,MirrorCapture.active()?"畫面鏡像已啟動":"鏡像啟動失敗",Toast.LENGTH_LONG).show();
         }else Toast.makeText(this,"未授權畫面擷取",Toast.LENGTH_SHORT).show();
         return;
     }
 }
 @Override protected void onDestroy(){h.removeCallbacksAndMessages(null);if(web!=null)web.destroy();super.onDestroy();}
}
