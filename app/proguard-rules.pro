# Keep Android WebView @JavascriptInterface entry point methods called from dashboard.html.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
# Android component class names must match the Manifest; other implementation code may be obfuscated.
-keep class com.jfvag.aibox.MainActivity
-keep class com.jfvag.aibox.BridgeService
-keep class com.jfvag.aibox.MediaSessionReader
-keep class com.jfvag.aibox.VehicleWidget
