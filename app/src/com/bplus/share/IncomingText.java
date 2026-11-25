package com.bplus.share;
/** Shared pure media classification for notifications and list previews. Never includes a media filename. */
public final class IncomingText{
 public static String type(String kind,String mime,String name){if(!"file".equals(kind))return "text";String m=mime==null?"":mime.toLowerCase(java.util.Locale.ROOT),n=name==null?"":name.toLowerCase(java.util.Locale.ROOT);if(m.startsWith("audio/")||n.matches(".*\\.(m4a|aac|mp3|ogg|opus|wav|flac|amr)$"))return "voice";if(m.startsWith("image/")||n.matches(".*\\.(jpg|jpeg|png|gif|webp|heic|heif|bmp)$"))return "photo";if(m.startsWith("video/")||n.matches(".*\\.(mp4|mkv|webm|mov|avi|3gp)$"))return "video";return "file";}
}
