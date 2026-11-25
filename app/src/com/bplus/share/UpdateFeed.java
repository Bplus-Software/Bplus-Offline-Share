package com.bplus.share;
import org.json.*;import java.io.*;import java.util.regex.*;import javax.xml.parsers.*;import org.w3c.dom.*;
/** Atom contains no channel label: usable only when the owner includes test versions. */
public final class UpdateFeed{
 public static final String URL="https://github.com/"+UpdateAsset.REPO+"/releases.atom";
 public static JSONArray parse(String xml)throws Exception{
  String upper=xml.toUpperCase(java.util.Locale.ROOT);if(upper.contains("<!DOCTYPE")||upper.contains("<!ENTITY"))throw new IOException("DTD and entities are not allowed");
  DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setValidating(false);f.setExpandEntityReferences(false);
  Document d=f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));NodeList entries=d.getElementsByTagNameNS("http://www.w3.org/2005/Atom","entry");JSONArray out=new JSONArray();if(entries.getLength()>100)throw new IOException("Too many feed entries");
  for(int i=0;i<entries.getLength();i++)try{Element e=(Element)entries.item(i);NodeList links=e.getElementsByTagNameNS("http://www.w3.org/2005/Atom","link");String tag="",sourceRepo="";for(int j=0;j<links.getLength();j++){Element l=(Element)links.item(j);if(!l.getAttribute("rel").equals("alternate"))continue;String href=l.getAttribute("href");for(String repo:UpdateAsset.REPOS){String prefix="https://github.com/"+repo+"/releases/tag/";if(href.startsWith(prefix)&&href.substring(prefix.length()).matches("v[0-9]+\\.[0-9]+\\.[0-9]+")){tag=href.substring(prefix.length());sourceRepo=repo;}}}if(tag.isEmpty())continue;
   NodeList content=e.getElementsByTagNameNS("http://www.w3.org/2005/Atom","content");if(content.getLength()!=1)continue;String body=content.item(0).getTextContent().replaceAll("<[^>]*>","\n").replace("&nbsp;"," ").replace("&#39;","'").replace("&amp;","&");Matcher code=Pattern.compile("(?m)^\\s*BPLUS_VERSION_CODE: ([1-9][0-9]{0,8})\\s*$").matcher(body);if(!code.find())continue;String number=code.group(1);if(code.find())continue;
   String name="BplusShare-"+tag.substring(1)+".apk";Matcher asset=Pattern.compile(Pattern.quote(name)+" \\| ([1-9][0-9]{0,8}) bytes \\| SHA-256 ([a-fA-F0-9]{64})(?![a-fA-F0-9])").matcher(body);if(!asset.find())continue;long size=Long.parseLong(asset.group(1));String sha=asset.group(2);if(asset.find())continue;
   JSONObject r=new JSONObject().put("tag_name",tag).put("prerelease",true).put("body","BPLUS_VERSION_CODE: "+number).put("assets",new JSONArray().put(new JSONObject().put("name",name).put("state","uploaded").put("size",size).put("digest","sha256:"+sha).put("browser_download_url","https://github.com/"+sourceRepo+"/releases/download/"+tag+"/"+name)));UpdateAsset.exact(r);out.put(r);
  }catch(Exception badEntry){}return out;
 }
}
