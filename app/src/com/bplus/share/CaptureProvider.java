package com.bplus.share;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;import java.io.*;
/** Camera receives a grant to one random app-owned output. No other directory is exposed. */
public final class CaptureProvider extends ContentProvider{
 public boolean onCreate(){return true;}
 private File file(Uri u)throws FileNotFoundException{try{if(u.getPathSegments().size()!=1||!u.getLastPathSegment().matches("capture-[a-f0-9-]{36}\\.(jpg|mp4)"))throw new IOException();File root=new File(getContext().getFilesDir(),"captures").getCanonicalFile(),f=new File(root,u.getLastPathSegment()).getCanonicalFile();if(!root.equals(f.getParentFile())||!f.isFile())throw new IOException();return f;}catch(Exception e){throw new FileNotFoundException("Capture unavailable");}}
 public String getType(Uri u){return u.getLastPathSegment().endsWith(".mp4")?"video/mp4":"image/jpeg";}
 public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{int flags;if(mode.equals("r"))flags=ParcelFileDescriptor.MODE_READ_ONLY;else if(mode.equals("w")||mode.equals("wt"))flags=ParcelFileDescriptor.MODE_WRITE_ONLY|ParcelFileDescriptor.MODE_TRUNCATE;else if(mode.equals("rw"))flags=ParcelFileDescriptor.MODE_READ_WRITE;else if(mode.equals("rwt"))flags=ParcelFileDescriptor.MODE_READ_WRITE|ParcelFileDescriptor.MODE_TRUNCATE;else throw new FileNotFoundException("Unsupported mode");return ParcelFileDescriptor.open(file(u),flags);}
 public Cursor query(Uri u,String[] p,String s,String[] a,String sort){MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});try{File f=file(u);c.addRow(new Object[]{f.getName(),f.length()});}catch(Exception ignored){}return c;}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){if(Binder.getCallingUid()!=android.os.Process.myUid())throw new SecurityException();try{return file(u).delete()?1:0;}catch(Exception e){return 0;}}
}
