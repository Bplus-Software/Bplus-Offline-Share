package com.bplus.share;
import java.io.*;import java.util.*;
/** Canonical legacy download route, shared by open and explicitly confirmed deletion. */
public final class AttachmentPaths {
 public static File resolve(File root,List<String> parts)throws IOException{
 if((parts.size()!=2&&parts.size()!=3)||!parts.get(0).equals("received"))throw new IOException("Invalid attachment route");
 File base=root.getCanonicalFile(),dir=base;if(parts.size()==3){String cat=parts.get(1);if(!cat.equals("Videos")&&!cat.equals("Photos")&&!cat.equals("Files"))throw new IOException("Invalid category");dir=new File(base,cat).getCanonicalFile();if(!base.equals(dir.getParentFile()))throw new IOException("Outside attachment folder");}
 File f=new File(dir,parts.get(parts.size()-1)).getCanonicalFile();if(!dir.equals(f.getParentFile()))throw new IOException("Outside attachment folder");return f;
 }
}
