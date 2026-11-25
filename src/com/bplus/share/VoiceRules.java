package com.bplus.share;
/** Pure recording gesture rules, testable without a microphone. */
public final class VoiceRules{
 public static boolean cancel(float down,float now,float threshold){return Math.abs(now-down)>threshold;}
 public static boolean send(boolean cancelled,long duration,long bytes){return !cancelled&&duration>=600&&duration<300000&&bytes>0;}
 public static boolean voice(String mime){return mime!=null&&mime.startsWith("audio/");}
}
