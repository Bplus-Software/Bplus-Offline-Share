package com.bplus.share;
import android.content.Context;import android.graphics.*;import android.widget.TextView;
/** Font-independent microphone silhouette. Text send/record/cancel states keep their labels. */
public final class ComposerButton extends TextView{
 private final Paint paint=new Paint(3);
 public ComposerButton(Context c){super(c);setGravity(android.view.Gravity.CENTER);setTextSize(22);setTextColor(Color.WHITE);setBackground(Ui.round(Ui.accent,24,c));}
 @Override protected void onDraw(Canvas c){if(!getText().toString().equals("🎙")){super.onDraw(c);return;}float s=Ui.dp(getContext(),24),scale=s/24f;c.save();c.translate((getWidth()-s)/2,(getHeight()-s)/2);c.scale(scale,scale);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);c.drawRoundRect(8,2,16,15,4,4,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);c.drawArc(5,7,19,19,0,180,false,paint);c.drawLine(5,9,5,12,paint);c.drawLine(19,9,19,12,paint);c.drawLine(12,19,12,22,paint);c.drawLine(8,22,16,22,paint);c.restore();}
}
