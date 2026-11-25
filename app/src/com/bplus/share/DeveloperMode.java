package com.bplus.share;
/** Local UI preference only. No networking or permissions changed. */
public final class DeveloperMode{public static boolean enabled(){return "1".equals(Store.get("developerMode","0"));}}
