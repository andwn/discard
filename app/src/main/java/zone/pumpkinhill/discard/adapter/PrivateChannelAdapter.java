package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;
import zone.pumpkinhill.discord4droid.handle.obj.User;

public class PrivateChannelAdapter extends DiscordAdapter {
    private List<Channel> mChannels;

    public PrivateChannelAdapter(Context context, List<Channel> channels) {
        super(context);
        mChannels = channels;
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.list_item_guild, parent, false);
        }
        PrivateChannel channel = (PrivateChannel)mChannels.get(position);
        User recipient = channel.getRecipient();
        // Try loading message author's avatar from cache, or start to download it
        ImageView icon = (ImageView) view.findViewById(R.id.guildIcon);
        getAvatarOrIcon(icon, recipient.getID(), recipient.getAvatarURL());
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
            nowPlaying.setText("Playing ".concat(game));
        } else {
            nowPlaying.setText("");
        }
        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.list_item_guild;
    }
}
