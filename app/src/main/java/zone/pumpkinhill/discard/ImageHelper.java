package zone.pumpkinhill.discard;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ImageHelper {
    public static String getBase64JPEG(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 95, baos);
        byte[] byteArrayImage = baos.toByteArray();
        return Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
    }

    public static Bitmap getBitmapFromBase64(String str) {
        System.out.println("Converting this to a bitmap: " + str);
        if(str.startsWith("data:image/jpg;base64, ")) {
            str = str.substring("data:image/jpg;base64, ".length());
            System.out.println("After stripping header: " + str);
        }
        final byte[] decodedBytes = Base64.decode(str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight, boolean filter) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, filter);
        bm.recycle();
        return resizedBitmap;
    }
}
