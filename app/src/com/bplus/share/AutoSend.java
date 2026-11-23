package com.bplus.share;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.JSONArray;
import org.json.JSONObject;

public class AutoSend {
    // list direct children of a SAF tree folder (non-recursive for v1.0)
    public static JSONArray listFiles(Context c, Uri treeUri) {
        JSONArray out = new JSONArray();
        try {
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId);
            Cursor cur = c.getContentResolver().query(children,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_SIZE,
                            DocumentsContract.Document.COLUMN_MIME_TYPE},
                    null, null, null);
            if (cur == null) return out;
            while (cur.moveToNext()) {
                String mime = cur.getString(3);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) continue;
                String id = cur.getString(0);
                JSONObject o = new JSONObject();
                o.put("uri", DocumentsContract.buildDocumentUriUsingTree(treeUri, id).toString());
                o.put("name", cur.getString(1));
                o.put("size", cur.getLong(2));
                out.put(o);
            }
            cur.close();
        } catch (Exception ignored) {}
        return out;
    }
}
