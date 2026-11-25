package com.bplus.share;
/** Platform must be explicit metadata, never inferred from user-controlled device labels. */
public final class SharePickerRules{public static String platform(String value){if("windows".equals(value)||"linux".equals(value)||"macos".equals(value))return "computer";if("android".equals(value)||"ios".equals(value))return "phone";return "device";}public static boolean available(String expected,String observed){return expected!=null&&!expected.isEmpty()&&expected.equals(observed);}}
