package com.bplus.share;
/** Gesture ownership is fixed at DOWN. Entering selection on long-click cannot consume that same UP. */
public final class SelectionGesture{
 private boolean captured,moved;private float downX,downY;public boolean toggle;
 public boolean event(int action,boolean active,float x,float y,float slop){toggle=false;if(action==0){captured=active;moved=false;downX=x;downY=y;}if(action==2&&(Math.abs(x-downX)>slop||Math.abs(y-downY)>slop))moved=true;if(action==3)moved=true;if(action==1)toggle=captured&&!moved;return captured;}
}
