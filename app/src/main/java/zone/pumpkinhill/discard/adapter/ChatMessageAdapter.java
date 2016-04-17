package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
        if(msg.getAttachments().size() >= 1) {
            Log.i(TAG, "There is at least 1 attachment.");
            ImageView attachment = (ImageView) view.findViewById(R.id.attachment);
            String attURL = msg.getAttachments().get(0).getUrl();
            Log.i(TAG, "URL: " + attURL);
            if(attURL.endsWith(".jpg") || attURL.endsWith(".jpeg") || attURL.endsWith(".png")) {
                Bitmap bmp = ClientHelper.getImageFromCache(attURL);
                if(bmp == null) {
                    // Bitmap not cached and needs to download, load in background
                    new ImageDownloaderTask(attachment).execute(attURL);
                } else {
                    attachment.setImageBitmap(bmp);
                }
            } else {
                Log.w(TAG, "Unknown attachment file type: " + attURL);
            }
        } else {
            ((ImageView)view.findViewById(R.id.attachment)).setImageResource(android.R.color.transparent);
        }
        return view;
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
