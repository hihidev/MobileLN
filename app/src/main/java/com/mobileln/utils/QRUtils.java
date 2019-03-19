package com.mobileln.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.TypedValue;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

public class QRUtils {

    // TOOD: No hard code value
    private static final long DIP = 300;

    public static Bitmap encodeAsBitmap(String str, Resources r) throws WriterException {
        int size = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DIP,
                r.getDisplayMetrics()
        );

        BitMatrix result;
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 0); /* default = 4 */
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, size, size, hints);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? 0xFF000000 : 0;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, size, 0, 0, w, h);
        return bitmap;
    }
}
