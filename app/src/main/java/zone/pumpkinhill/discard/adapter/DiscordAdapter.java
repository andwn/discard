package zone.pumpkinhill.discard.adapter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.commonsware.cwac.anddown.AndDown;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.task.ImageDownloaderTask;
import zone.pumpkinhill.discord4droid.handle.obj.Attachment;

public abstract class DiscordAdapter extends BaseAdapter {
    protected final static AndDown Markdown = new AndDown();
    protected final static SimpleDateFormat
            TodayFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH),
            OldFormat = new SimpleDateFormat("MMM dd hh:mm a", Locale.ENGLISH);

    protected final Context mContext;
    protected final LayoutInflater mInflater;
    protected final SharedPreferences mPref;

    public DiscordAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    protected static boolean getAvatarOrIcon(ImageView view, String id, String url) {
        Bitmap b = ClientHelper.cache.get(id);
        if(b == null) {
            view.setImageResource(android.R.drawable.ic_menu_gallery);
            new ImageDownloaderTask(view).execute(id, url);
        } else {
            view.setImageBitmap(b);
        }
        return true;
    }

    protected static boolean getThumbnail(ImageView view, Attachment attachment) {
        String thumbURL = attachment.getThumbnailURL();
        if(thumbURL != null && !thumbURL.isEmpty()) {
            return getAvatarOrIcon(view, attachment.getId(), thumbURL);
        } else {
            return getLinkImage(view, attachment.getId(), attachment.getUrl());
        }
    }

    protected static boolean getLinkImage(ImageView view, String id, String url) {
        return isImageFilename(url) && getAvatarOrIcon(view, id, url);
    }

    protected static boolean isImageFilename(String text) {
        return text.toLowerCase().endsWith(".jpg") ||
                text.toLowerCase().endsWith(".jpeg") ||
                text.toLowerCase().endsWith(".png") ||
                text.toLowerCase().endsWith(".gif") ||
                // Twitter sucks tbh
                text.toLowerCase().endsWith(".jpg:orig") ||
                text.toLowerCase().endsWith(".jpg:large");
    }

    // Trims trailing whitespace
    protected static CharSequence trimEnd(CharSequence source) {
        if(source == null) return "";
        int i = source.length();
        // loop back to the first non-whitespace character
        while(--i >= 0 && Character.isWhitespace(source.charAt(i))) {}
        return source.subSequence(0, i+1);
    }

    protected class ThumbnailOnClickListener implements View.OnClickListener {
        private final String mURL;
        public ThumbnailOnClickListener(String url) {
            mURL = url;
        }
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(mURL), "image/*");
            mContext.startActivity(intent);
        }
    }

    protected class ThumbnailOnLongClickListener implements View.OnLongClickListener {
        private final String mURL;
        public ThumbnailOnLongClickListener(String url) {
            mURL = url;
        }
        @Override
        public boolean onLongClick(View v) {
            // Make sure SD card is mounted
            if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
                Toast.makeText(mContext, "External SD card not mounted", Toast.LENGTH_LONG).show();
                return true;
            }
            // Get permission from user if we don't already have it
            if(ActivityCompat.checkSelfPermission(
                    mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        (Activity)mContext,
                        new String[] {
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, 1);
            }
            // Save to <data>/Pictures/Discard
            File picturesDir = new File(Environment.getExternalStorageDirectory(),
                    Environment.DIRECTORY_PICTURES);
            File discardDir = new File(picturesDir, "Discard");
            // Double check permissions (user denied) and make directory if it doesn't exist
            if(!(discardDir.exists() || discardDir.mkdir())) {
                Toast.makeText(mContext, "Need permission", Toast.LENGTH_LONG).show();
                return true;
            }
            // Finally download the image in the background and save it to the pictures folder
            String filename = mURL.substring(mURL.lastIndexOf("/") + 1);
            File file = new File(discardDir, filename);
            new ImageDownloaderTask((ImageView) v).execute(null, mURL, file.getAbsolutePath());
            Toast.makeText(mContext, "Saving to " + discardDir.toString(), Toast.LENGTH_LONG)
                    .show();
            return true;
        }
    }
}
