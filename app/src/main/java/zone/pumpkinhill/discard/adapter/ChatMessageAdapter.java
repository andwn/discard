package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.MessageUpdateEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Attachment;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.util.MessageList;

public class ChatMessageAdapter extends DiscordAdapter {
    private MessageList mMessages;
    private final Date mYesterday;

    public ChatMessageAdapter(Context context, MessageList messages) {
        super(context);
        mMessages = messages;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        mYesterday = cal.getTime();
        ClientHelper.subscribe(this);
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
        getAvatarOrIcon(avatar, msg.getAuthor().getID(), msg.getAuthor().getAvatarURL());
        // Fill in the text
        TextView name = (TextView) view.findViewById(R.id.nameTextView);
        name.setText(msg.getAuthor().getName());
        if(mPref.getBoolean("show_discriminator", false)) {
            TextView discriminator = (TextView) view.findViewById(R.id.discriminatorTextView);
            discriminator.setText("#".concat(msg.getAuthor().getDiscriminator()));
        }
        if(mPref.getBoolean("show_timestamp", true)) {
            TextView timestamp = (TextView) view.findViewById(R.id.timestampTextView);
            Date time = msg.getCreationDate();
            String timeStr = time.after(mYesterday) ? TodayFormat.format(time) : OldFormat.format(time);
            timestamp.setText(timeStr);
        }
        // Message content and formatting
        TextView content = (TextView) view.findViewById(R.id.messageContent);
        String contentStr = msg.getContent();
        List<User> mentions = msg.getMentions();
        for(User u : mentions) {
            if(u == null) continue; // I actually got an NPE here...
            contentStr = contentStr.replaceAll("<@" + u.getID() + ">", "@" + u.getName());
        }
        String contentHtml = Markdown.markdownToHtml(contentStr);
        Spanned spanned = Html.fromHtml(contentHtml);
        content.setText(trimEnd(spanned));
        // Attachment
        ImageView attachment = (ImageView) view.findViewById(R.id.attachment);
        // Clear anything that might have been left over
        attachment.setImageResource(android.R.color.transparent);
        attachment.setOnClickListener(null);
        attachment.setOnLongClickListener(null);
        // Look for any thumbnails, display the first image
        if(msg.getAttachments().size() > 0 && mPref.getBoolean("preload_links", true)) {
            for (Attachment a : msg.getAttachments()) {
                if(getThumbnail(attachment, a)) {
                    attachment.setOnClickListener(new ThumbnailOnClickListener(a.getUrl()));
                    attachment.setOnLongClickListener(new ThumbnailOnLongClickListener(a.getUrl()));
                    break;
                }
            }
        } else if(mPref.getBoolean("preload_links", true)) {
            // No thumbnails -- check for image URLs in message content
            String[] links = extractLinks(msg.getContent());
            for(String link : links) {
                if(getLinkImage(attachment, msg.getID(), link)) {
                    attachment.setOnClickListener(new ThumbnailOnClickListener(link));
                    attachment.setOnLongClickListener(new ThumbnailOnLongClickListener(link));
                    break;
                }
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

    @Override
    public int getItemViewType(int position) {
        return R.layout.list_item_message;
    }

    @EventSubscriber
    @SuppressWarnings("unused")
    public void onMessageUpdate(MessageUpdateEvent event) {
        if(!mMessages.contains(event.getOldMessage())) return;
        mMessages.remove(event.getOldMessage());
        mMessages.add(event.getNewMessage());
        notifyDataSetChanged();
    }
}
