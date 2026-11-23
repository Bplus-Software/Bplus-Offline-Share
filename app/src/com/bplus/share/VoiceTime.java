package com.bplus.share;
/** Stable elapsed / total labels. Missing metadata is not a zero-duration recording. */
public final class VoiceTime{
 public static String time(int ms){int seconds=Math.max(0,ms)/1000;return String.format(java.util.Locale.US,"%02d:%02d",seconds/60,seconds%60);}
 public static String label(int elapsed,int total){return time(elapsed)+" / "+(total<0?"--:--":time(total));}
}
