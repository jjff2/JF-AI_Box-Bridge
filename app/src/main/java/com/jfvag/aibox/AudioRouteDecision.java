package com.jfvag.aibox;

/** Pure, deterministic routing rule. Never dispatches any action itself. */
final class AudioRouteDecision {
 static String choose(boolean androidPlaying,String vehicleState,String context,String previous){
  String fallback="vehicle".equals(previous)?"vehicle":"android";
  boolean vehiclePlaying="Play".equalsIgnoreCase(vehicleState)||"Playing".equalsIgnoreCase(vehicleState);
  String source=context==null?"":context.toLowerCase(java.util.Locale.ROOT);
  if(vehiclePlaying && (!androidPlaying || "media".equals(source)))return "vehicle";
  if(androidPlaying && (!vehiclePlaying || source.contains("smartphone")))return "android";
  // In particular, showing the CarPlay UI without audio must not switch routing.
  return fallback;
 }
}
