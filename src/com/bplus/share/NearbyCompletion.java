package com.bplus.share;
/** Only confirmed, saved pairing can leave discovery. Accept alone is not success. */
public final class NearbyCompletion{
 public static int state(String status,long expires,long now,boolean saved){if("paired".equals(status)&&saved)return 1;if(now>=expires||"cancelled".equals(status)||"declined".equals(status)||"blocked".equals(status))return -1;return 0;}
}
