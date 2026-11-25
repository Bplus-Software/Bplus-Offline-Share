package com.bplus.share;
import android.content.*;import android.net.Uri;import java.util.*;
public final class ShareIntents{
 public static Intent build(ArrayList<Uri> uris,String mime){if(uris.isEmpty())throw new IllegalArgumentException("No files");for(Uri u:uris)if(u==null||!"content".equals(u.getScheme()))throw new IllegalArgumentException("Content URI required");Intent send=new Intent(uris.size()==1?Intent.ACTION_SEND:Intent.ACTION_SEND_MULTIPLE);send.setType(mime);if(uris.size()==1)send.putExtra(Intent.EXTRA_STREAM,uris.get(0));else send.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);ClipData clip=ClipData.newRawUri("attachments",uris.get(0));for(int i=1;i<uris.size();i++)clip.addItem(new ClipData.Item(uris.get(i)));send.setClipData(clip);send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);return send;}
}
