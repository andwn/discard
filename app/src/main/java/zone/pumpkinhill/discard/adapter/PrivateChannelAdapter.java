package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.task.ImageDownloaderTask;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Presences;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;

public class PrivateChannelAdapter extends BaseAdapter {
    private List<Channel> mChannels;
    private LayoutInflater mInflater;

    public PrivateChannelAdapter(Context context, List<Channel> channels) {
        mChannels = channels;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    // Allow clicking on our own messages, to edit/delete
    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return mChannels.size();
    }

    @Override
    public Object getItem(int position) {
        return mChannels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return Long.parseLong(mChannels.get(position).getID());
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
            view = mInflater.inflate(R.layout.list_item_guild, parent, false);
        }
        PrivateChannel channel = (PrivateChannel)mChannels.get(position);
        // Try loading message author's avatar from cache, or start to download it
        ImageView icon = (ImageView) view.findViewById(R.id.guildIcon);
        String iconURL = channel.getRecipient().getAvatarURL();
        if(iconURL == null || iconURL.isEmpty()) {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        } else {
            Bitmap bmp = ClientHelper.getAvatarFromCache(iconURL);
            if(bmp == null) {
                // Bitmap not cached and needs to download, load in background
                new ImageDownloaderTask(icon, true).execute(iconURL);
            } else {
                icon.setImageBitmap(bmp);
            }
        }
        // Fill in the text
        TextView name = (TextView) view.findViewById(R.id.guildName);
        name.setText(channel.getRecipient().getName());
        // Online status
        TextView status = (TextView) view.findViewById(R.id.statusText);
        status.setText("");
        switch(channel.getRecipient().getPresence()) {
            case ONLINE: status.setBackgroundResource(R.color.colorStatusOnline); break;
            case IDLE: status.setBackgroundResource(R.color.colorStatusIdle); break;
            case OFFLINE: status.setBackgroundResource(R.color.colorStatusOffline); break;
        }
        // Now playing
        TextView nowPlaying = (TextView) view.findViewById(R.id.nowPlaying);
        String game = channel.getRecipient().getGame();
        if(game != null && !game.isEmpty()) {
            nowPlaying.setText("Playing " + game);
        } else {
            nowPlaying.setText("");
        }
        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.list_item_guild;
    }

    // Same view for all items
    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return mChannels.isEmpty();
    }
}
