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
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.task.ImageDownloaderTask;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.util.MessageList;

public class ChatMessageAdapter extends BaseAdapter {
    private final static String TAG = ChatMessageAdapter.class.getCanonicalName();

    private MessageList mMessages;
    private LayoutInflater mInflater;
    private final static SimpleDateFormat
            TodayFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH),
            OldFormat = new SimpleDateFormat("MMM dd hh:mm a", Locale.ENGLISH);
    private final Date mYesterday;
    private final Context mContext;
    private final SharedPreferences mPref;

    public ChatMessageAdapter(Context context, MessageList messages) {
        mMessages = messages;
        mInflater = LayoutInflater.from(context);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        mYesterday = cal.getTime();
        mContext = context;
        mPref = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    // Allow clicking on our own messages, to edit/delete
    @Override
    public boolean isEnabled(int position) {
        int pos = mMessages.size() - position - 1;
        return mMessages.get(pos).getAuthor().getID().equals(ClientHelper.ourUser().getID());
    }

    @Override
    public int getCount() {
        return mMessages.size();
    }

    @Override
    public Object getItem(int position) {
        return mMessages.get(mMessages.size() - position - 1);
    }

    @Override
    public long getItemId(int position) {
        int pos = mMessages.size() - position - 1;
        return Long.parseLong(mMessages.get(pos).getID());
    }

    // Discord message IDs are unique
    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.list_item_message, parent, false);
        }
        int pos = mMessages.size() - position - 1;
        Message msg = mMessages.get(pos);
        // Try loading message author's avatar from cache, or start to download it
        ImageView avatar = (ImageView) view.findViewById(R.id.avatarImageView);
        String avatarURL = msg.getAuthor().getAvatarURL();
        if(avatarURL == null || avatarURL.isEmpty()) {
            avatar.setImageResource(android.R.drawable.sym_def_app_icon);
        } else {
            Bitmap bmp = ClientHelper.getImageFromCache(avatarURL);
            if(bmp == null) {
                // Bitmap not cached and needs to download, load in background
                new ImageDownloaderTask(avatar).execute(avatarURL);
            } else {
                avatar.setImageBitmap(bmp);
            }
        }
        // Fill in the text
        TextView name = (TextView) view.findViewById(R.id.nameTextView);
        name.setText(msg.getAuthor().getName());
        if(mPref.getBoolean("show_discriminator", false)) {
            TextView discriminator = (TextView) view.findViewById(R.id.discriminatorTextView);
            discriminator.setText("#" + msg.getAuthor().getDiscriminator());
        }
        if(mPref.getBoolean("show_timestamp", true)) {
            TextView timestamp = (TextView) view.findViewById(R.id.timestampTextView);
            Date time = msg.getCreationDate();
            String timeStr = time.after(mYesterday) ? TodayFormat.format(time) : OldFormat.format(time);
            timestamp.setText(timeStr);
        }
        // Message content and formatting
        TextView content = (TextView) view.findViewById(R.id.messageTextView);
        String contentStr = msg.getContent();
        List<User> mentions = msg.getMentions();
        for(User u : mentions) {
            contentStr = contentStr.replaceAll("<@" + u.getID() + ">", "@" + u.getName());
        }
        content.setText(contentStr);
        // Attachment
        ImageView attachment = (ImageView) view.findViewById(R.id.attachment);
        if(msg.getAttachments().size() >= 1) {
            String attURL = msg.getAttachments().get(0).getUrl();
            if (isImageFilename(attURL)) {
                boolean download = mPref.getBoolean("preload_attachments", true);
                enableImageView(attachment, attURL, download);
            } else {
                Log.w(TAG, "Unknown attachment file type: " + attURL);
                disableImageView(attachment);
            }
        } else if(mPref.getBoolean("preload_links", true)){ // Links
            String[] links = extractLinks(msg.getContent());
            if(links.length > 0) {
                boolean anyImages = false;
                for(String link : links) {
                    if(isImageFilename(link)) {
                        enableImageView(attachment, link, true);
                        anyImages = true;
                        break;
                    }
                }
                if(!anyImages) {
                    disableImageView(attachment);
                }
            } else {
                disableImageView(attachment);
            }
        }
        return view;
    }

    private static String[] extractLinks(String text) {
        try {
            List<String> links = new ArrayList<>();
            Matcher m = Patterns.WEB_URL.matcher(text);
            while (m.find()) {
                String url = m.group();
                links.add(url);
            }
            return links.toArray(new String[links.size()]);
        } catch(NullPointerException e) {
            return new String[]{};
        }
    }

    private static boolean isImageFilename(String text) {
        return text.toLowerCase().endsWith(".jpg") ||
                text.toLowerCase().endsWith(".jpeg") ||
                text.toLowerCase().endsWith(".png") ||
                text.toLowerCase().endsWith(".gif");
    }

    private void enableImageView(ImageView view, final String url, boolean download) {
        Bitmap bmp = ClientHelper.getImageFromCache(url);
        if (bmp == null) {
            // Bitmap not cached and needs to download, load in background
            if(download) {
                new ImageDownloaderTask(view).execute(url);
            } else {
                view.setImageResource(android.R.drawable.gallery_thumb);
            }
        } else {
            view.setImageBitmap(bmp);
        }
        view.setEnabled(true);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "image/*");
                mContext.startActivity(intent);
            }
        });
        if(!download) return;
        view.setOnLongClickListener(new View.OnLongClickListener() {
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
                try {
                    // FIXME: Change image cache to save original format and not bitmap
                    String filename = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
                    if(filename.length() > 32) filename = filename.substring(0, 32);
                    filename += ".png";
                    Log.d(TAG, filename);
                    File file = new File(discardDir, filename);
                    FileOutputStream f = new FileOutputStream(file);
                    Bitmap bmp = ClientHelper.getImageFromCache(url);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, f);
                    f.close();
                    Toast.makeText(mContext, "Saved to " + discardDir.toString(), Toast.LENGTH_LONG)
                            .show();
                } catch(IOException e) {
                    Toast.makeText(mContext, "Error: " + e, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
    }

    private static void disableImageView(ImageView view) {
        view.setImageResource(android.R.color.transparent);
        view.setEnabled(false);
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.list_item_message;
    }

    // Same view for all items
    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return mMessages.isEmpty();
    }
}
