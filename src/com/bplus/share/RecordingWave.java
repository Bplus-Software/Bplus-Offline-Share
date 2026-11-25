package com.bplus.share;
import android.content.*;import android.graphics.*;import android.view.*;
/** Actual microphone peak history, no fabricated animation when input is silent. */
public final class RecordingWave extends View{
 private final float[] levels=new float[36];private final Paint paint=new Paint(3);
 public RecordingWave(Context c){super(c);setContentDescription(c.getString(R.string.voice_live_level));}
 public void sample(int amplitude){System.arraycopy(levels,1,levels,0,levels.length-1);levels[levels.length-1]=Math.min(1f,(float)Math.sqrt(Math.max(0,amplitude)/32767f));invalidate();}
 protected void onDraw(Canvas c){super.onDraw(c);paint.setColor(Ui.accent);paint.setStrokeCap(Paint.Cap.ROUND);float step=getWidth()/(float)levels.length;paint.setStrokeWidth(Math.max(2,step*.45f));for(int i=0;i<levels.length;i++){float half=Math.max(Ui.dp(getContext(),1),levels[i]*getHeight()*.43f),x=step*(i+.5f);c.drawLine(x,getHeight()/2f-half,x,getHeight()/2f+half,paint);}}
}
