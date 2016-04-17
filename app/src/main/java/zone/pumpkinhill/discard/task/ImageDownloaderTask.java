package zone.pumpkinhill.discard.task;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.http.HttpStatus;

/**
 * This class is based from:
 * http://javatechig.com/android/loading-image-asynchronously-in-android-listview
 */

public class ImageDownloaderTask extends AsyncTask<String, Void, Bitmap> {
    private final static String TAG = ImageDownloaderTask.class.getCanonicalName();

    private static LinkedBlockingQueue<ImageDownloaderTask> ActiveTasks = new LinkedBlockingQueue<>();

    private String mURL;
    private boolean mIsDownloading;
    private final Drawable mOldDrawable;
    private final WeakReference<ImageView> mImageViewReference;

    public ImageDownloaderTask(ImageView imageView) {
        mIsDownloading = false;
        mImageViewReference = new WeakReference<>(imageView);
        mOldDrawable = imageView.getDrawable();
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        return downloadBitmap(params[0]);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap = null;
        }
        mIsDownloading = false;
        ActiveTasks.remove(this);
        if (mImageViewReference.get() != null) {
            ImageView imageView = mImageViewReference.get();
            if (imageView != null) {
                // Compare the old drawable (from before the download started) to the one in the
                // view now to make sure they match. If they don't, that means the user has scrolled
                // fast enough that our view has wrapped around the screen and is showing a
                // different message. So just cache the image without setting it to the view.
                if(mOldDrawable != null && !imageView.getDrawable().equals(mOldDrawable)) {
                    if (bitmap != null) ClientHelper.addImageToCache(mURL, bitmap);
                    return;
                }
                if (bitmap != null) {
                    ClientHelper.addImageToCache(mURL, bitmap);
                    imageView.setImageBitmap(bitmap);
                } else {
                    // The download failed so make it a camera or something
                    Drawable placeholder = imageView.getContext().getResources().getDrawable(R.drawable.ic_menu_camera);
                    imageView.setImageDrawable(placeholder);
                }
            }
        }
    }

    private Bitmap downloadBitmap(String url) {
        // Let's not download the same image 12 times
        for(ImageDownloaderTask task : ActiveTasks) {
            if(task.getURL().equals(url)) return waitForAnotherTask(task);
        }
        mURL = url;
        mIsDownloading = true;
        ActiveTasks.add(this);
        HttpURLConnection urlConnection = null;
        try {
            URL uri = new URL(url);
            urlConnection = (HttpURLConnection) uri.openConnection();
            int statusCode = urlConnection.getResponseCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream != null) {
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (Exception e) {
            //if(urlConnection != null) urlConnection.disconnect();
            Log.w(TAG, "Error downloading image from " + url);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    private Bitmap waitForAnotherTask(ImageDownloaderTask task) {
        try {
            while (task.isDownloading()) {
                Thread.sleep(200, 0);
            }
            return ClientHelper.getImageFromCache(task.getURL());
        } catch(InterruptedException e) {
            Log.w(TAG, "Wait thread interrupted.");
            return null;
        }
    }

    public String getURL() {
        return mURL;
    }

    public boolean isDownloading() {
        return mIsDownloading;
    }
}
