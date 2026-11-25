package com.bplus.share;
import android.content.Context;import android.graphics.*;import android.view.View;
/** Consistent 24-unit line icons. No font-dependent toolbar glyphs. */
public final class SelectionIcon extends View{
 public static final int BACK=0,SEND=1,DELETE=2,ALL=3,SHARE=4;private final int type,color;private final Paint p=new Paint(3);
 public SelectionIcon(Context c,int type,int color){super(c);this.type=type;this.color=color;}
 protected void onDraw(Canvas c){float size=Math.min(getWidth(),getHeight())*.78f;float scale=size/24;c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(scale,scale);p.setColor(color);p.setStrokeWidth(1.8f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
 if(type==BACK){path(c,15,5,8,12,15,19);c.drawLine(8,12,21,12,p);}
 else if(type==SEND){path(c,3,3,22,12,3,21,7,12,3,3);c.drawLine(7,12,22,12,p);}
 else if(type==DELETE){c.drawLine(4,6,20,6,p);path(c,9,6,9,3,15,3,15,6);path(c,6,6,7,21,17,21,18,6);c.drawLine(10,10,10,17,p);c.drawLine(14,10,14,17,p);}
 else if(type==SHARE){c.drawLine(7,11,17,6,p);c.drawLine(7,13,17,18,p);c.drawCircle(5,12,2.5f,p);c.drawCircle(19,5,2.5f,p);c.drawCircle(19,19,2.5f,p);}
 else{path(c,8,3,3,3,3,8);path(c,16,3,21,3,21,8);path(c,3,16,3,21,8,21);path(c,21,16,21,21,16,21);c.drawRect(7,7,17,17,p);path(c,9,12,11,14,15,10);}
 c.restore();}
 private void path(Canvas c,float...xy){Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);c.drawPath(path,p);}
}
