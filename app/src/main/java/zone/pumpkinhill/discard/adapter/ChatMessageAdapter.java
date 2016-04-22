package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.task.ImageDownloaderTask;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.util.MessageList;

public class ChatMessageAdapter extends BaseAdapter {
    private final static String TAG = ChatMessageAdapter.class.getCanonicalName();

    private MessageList mMessages;
    private LayoutInflater mInflater;
    private final static SimpleDateFormat
            TodayFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH),
            OldFormat = new SimpleDateFormat("MMM dd hh:mm a", Locale.ENGLISH);
    private final Date mYesterday;

    public ChatMessageAdapter(Context context, MessageList messages) {
        mMessages = messages;
        mInflater = LayoutInflater.from(context);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        mYesterday = cal.getTime();
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
        TextView discriminator = (TextView) view.findViewById(R.id.discriminatorTextView);
        discriminator.setText("#" + msg.getAuthor().getDiscriminator());
        TextView timestamp = (TextView) view.findViewById(R.id.timestampTextView);
        Date time = msg.getCreationDate();
        String timeStr = time.after(mYesterday) ? TodayFormat.format(time) : OldFormat.format(time);
        timestamp.setText(timeStr);
        TextView content = (TextView) view.findViewById(R.id.messageTextView);
        content.setText(msg.getContent());
        // Attachment
        ImageView attachment = (ImageView) view.findViewById(R.id.attachment);
        if(msg.getAttachments().size() >= 1) {
            String attURL = msg.getAttachments().get(0).getUrl();
            if (isImageFilename(attURL)) {
                enableImageView(attachment, attURL);
            } else {
                Log.w(TAG, "Unknown attachment file type: " + attURL);
                disableImageView(attachment);
            }
        } else { // Links
            String[] links = extractLinks(msg.getContent());
            if(links.length > 0) {
                boolean anyImages = false;
                for(String link : links) {
                    if(isImageFilename(link)) {
                        enableImageView(attachment, link);
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
        List<String> links = new ArrayList<>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            Log.v(TAG, "URL extracted: " + url);
            links.add(url);
        }
        return links.toArray(new String[links.size()]);
    }

    private static boolean isImageFilename(String text) {
        return text.toLowerCase().endsWith(".jpg") ||
                text.toLowerCase().endsWith(".jpeg") ||
                text.toLowerCase().endsWith(".png") ||
                text.toLowerCase().endsWith(".gif");
    }

    private static void enableImageView(ImageView view, String url) {
        Bitmap bmp = ClientHelper.getImageFromCache(url);
        if (bmp == null) {
            // Bitmap not cached and needs to download, load in background
            new ImageDownloaderTask(view).execute(url);
        } else {
            view.setImageBitmap(bmp);
        }
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
