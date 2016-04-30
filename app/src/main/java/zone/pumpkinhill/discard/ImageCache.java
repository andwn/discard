package zone.pumpkinhill.discard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class ImageCache {
    private Context mContext;
    private HashMap<String, Image> mCache;

    public ImageCache(Context context) {
        mContext = context;
        mCache = new HashMap<>();
    }
    public Bitmap get(String id) {
        if(id == null) {
            Log.d("ImageCache.get", "id is null.");
            return null;
        }
        Image img = mCache.get(id);
        // Try to load from file if image is not in memory
        if(img == null) {
            File file = new File(mContext.getCacheDir(), id);
            if(!file.exists()) return null;
            img = new Image(BitmapFactory.decodeFile(file.getPath()));
            put(file.getName(), img.data);
        } else {
            img.lastHit = new Date();
        }
        return img.data;
    }
    public void put(String id, Bitmap b) {
        if(id == null) {
            Log.w("ImageCache.put", "id is null.");
            return;
        }
        Image img = new Image(b);
        mCache.put(id, img);
        File outFile = new File(mContext.getCacheDir(), id);
        if(!outFile.exists()) {
            try {
                OutputStream stream = new FileOutputStream(outFile);
                img.data.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                stream.close();
            } catch(IOException e) {
                Log.e("ImageCache", "Error saving image: " + e);
            }
        }
    }
    public void remove(String id) {
        mCache.remove(id);
        File file = new File(mContext.getCacheDir(), id);
        if(file.exists()) {
            if(!file.delete()) Log.e("ImageCache", "Failed to delete file: " + file.getName());
        }
    }
    public void prune() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -7);
        for(String id : mCache.keySet()) {
            Image img = mCache.get(id);
            if(img.lastHit.before(cal.getTime())) {
                remove(id);
            }
        }
    }

    private class Image {
        public Bitmap data;
        public Date created;
        public Date lastHit;
        public Image(Bitmap data) {
            this.data = data;
            created = lastHit = new Date();
        }
    }
}
