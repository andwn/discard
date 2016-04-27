package zone.pumpkinhill.discord4droid.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

import zone.pumpkinhill.discord4droid.handle.obj.User;

public class ImageHelper {
    public static String getBase64JPEG(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
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
}
