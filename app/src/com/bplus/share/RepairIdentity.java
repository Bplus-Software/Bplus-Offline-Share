package com.bplus.share;
/** Target constraints for the re-pair shortcut. Names never establish identity. */
public final class RepairIdentity{
 public static boolean matches(String target,String actual){return target!=null&&actual!=null&&(target.isEmpty()||target.equals(actual));}
 public static boolean fresh(String target,String epoch){return target!=null&&epoch!=null&&(target.isEmpty()||epoch.matches("[a-f0-9]{64}"));}
}
