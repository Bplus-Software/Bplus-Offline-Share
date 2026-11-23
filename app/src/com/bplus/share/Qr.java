package com.bplus.share;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

public class Qr {
    public static Bitmap encode(String text, int sizePx) {
        try {
            com.google.zxing.common.BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx);
            Bitmap b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < sizePx; y++)
                for (int x = 0; x < sizePx; x++)
                    b.setPixel(x, y, m.get(x, y) ? Color.rgb(17,17,17) : Color.WHITE);
            return b;
        } catch (Exception e) { return null; }
    }

    public static String decodeYUV(byte[] yuv, int w, int h) {
        try {
            PlanarYUVLuminanceSource src = new PlanarYUVLuminanceSource(yuv, w, h, 0, 0, w, h, false);
            return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(src))).getText();
        } catch (Exception e) { return null; }
    }
}
