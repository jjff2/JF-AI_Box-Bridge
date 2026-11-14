package com.jfvag.aibox;
public class AudioRouteDecisionTest {
 static void eq(String want,String got){if(!want.equals(got))throw new AssertionError(want+" != "+got);}
 public static void main(String[] args){
  eq("vehicle",AudioRouteDecision.choose(false,"Play","media","android"));
  eq("vehicle",AudioRouteDecision.choose(false,"Pause","media","vehicle"));
  eq("vehicle",AudioRouteDecision.choose(false,"Play","media","vehicle")); // CarPlay UI alone
  eq("android",AudioRouteDecision.choose(true,"NoTrack","SPI_smartPhone","vehicle"));
  eq("android",AudioRouteDecision.choose(false,"NoTrack","SPI_smartPhone","android"));
  eq("vehicle",AudioRouteDecision.choose(false,"Pause","media","vehicle"));
  eq("vehicle",AudioRouteDecision.choose(true,"Play","media","vehicle"));
  eq("android",AudioRouteDecision.choose(true,"Play","SPI_smartPhone","vehicle"));
  System.out.println("8 auto-routing scenarios passed");
 }
}
