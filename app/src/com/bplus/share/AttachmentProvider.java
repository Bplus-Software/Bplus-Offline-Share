package com.bplus.share;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import java.io.*;
public class AttachmentProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 private File file(Uri uri)throws FileNotFoundException{try{if(uri.getPathSegments().size()==2&&(uri.getPathSegments().get(0).equals("voice")||uri.getPathSegments().get(0).equals("shared"))){File root=new File(getContext().getFilesDir(),uri.getPathSegments().get(0)).getCanonicalFile();File f=new File(root,uri.getLastPathSegment()).getCanonicalFile();if(!root.equals(f.getParentFile()))throw new IOException();return f;}return AttachmentPaths.resolve(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"B+ Share"),uri.getPathSegments());}catch(IOException e){throw new FileNotFoundException("Attachment unavailable");}}
 public String getType(Uri u){return Save.mime(u.getLastPathSegment());}
 public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!mode.equals("r"))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(u),ParcelFileDescriptor.MODE_READ_ONLY);}
 public Cursor query(Uri u,String[] p,String s,String[] a,String sort){MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});try{File f=file(u);c.addRow(new Object[]{f.getName(),f.length()});}catch(Exception ignored){}return c;}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){if(android.os.Binder.getCallingUid()!=android.os.Process.myUid())throw new SecurityException("Local app deletion only");try{File f=file(u);return f.isFile()&&f.delete()?1:0;}catch(Exception e){return 0;}}
}
