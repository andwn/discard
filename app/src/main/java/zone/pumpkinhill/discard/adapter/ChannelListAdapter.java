package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;
import zone.pumpkinhill.discord4droid.handle.obj.User;
import zone.pumpkinhill.discord4droid.handle.obj.VoiceChannel;

public class ChannelListAdapter extends DiscordAdapter {
    private final boolean mIsPrivate;
    private final List<Channel> mTextChannels;
    private final List<VoiceChannel> mVoiceChannels;

    public ChannelListAdapter(Context context, Guild guild) {
        super(context);
        mIsPrivate = false;
        mTextChannels = guild.getChannels();
        mVoiceChannels = guild.getVoiceChannels();
    }

    public ChannelListAdapter(Context context, List<Channel> channels) {
        super(context);
        mIsPrivate = true;
        mTextChannels = channels;
        mVoiceChannels = null;
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
        return mTextChannels.size() + (mIsPrivate ? 0 : mVoiceChannels.size());
    }

    @Override
    public Object getItem(int position) {
        if(mTextChannels.size() > position) return mTextChannels.get(position);
        else return mVoiceChannels.get(position - mTextChannels.size());
    }

    @Override
    public long getItemId(int position) {
        return Long.parseLong(((Channel)getItem(position)).getID());
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
            view = mInflater.inflate(getItemViewType(position), parent, false);
        }
        Channel channel = ((Channel)getItem(position));
        if(mIsPrivate) {
            PrivateChannel pc = (PrivateChannel) channel;
            // Avatar
            ImageView icon = (ImageView) view.findViewById(R.id.guildIcon);
            getAvatarOrIcon(icon, pc.getRecipient().getID(), pc.getRecipient().getAvatarURL());
            // Name
            TextView name = (TextView) view.findViewById(R.id.guildName);
            name.setText(pc.getRecipient().getName());
            // Status or notifications
            TextView status = (TextView) view.findViewById(R.id.statusText);
            if(pc.getMentionCount() > 0) {
                status.setText(pc.getMentionCount());
                status.setBackgroundColor(Color.RED);
            } else {
                status.setText("");
                switch (pc.getRecipient().getPresence()) {
                    case ONLINE:
                        status.setBackgroundResource(R.color.colorStatusOnline);
                        break;
                    case IDLE:
                        status.setBackgroundResource(R.color.colorStatusIdle);
                        break;
                    case OFFLINE:
                        status.setBackgroundResource(R.color.colorStatusOffline);
                        break;
                }
            }
            // Now playing
            TextView nowPlaying = (TextView) view.findViewById(R.id.nowPlaying);
            String game = pc.getRecipient().getGame();
            if(game != null && !game.isEmpty()) {
                nowPlaying.setText("Playing ".concat(game));
            } else {
                nowPlaying.setText("");
            }
        } else if(channel instanceof VoiceChannel) {
            VoiceChannel vc = (VoiceChannel) channel;
            // Name
            TextView name = (TextView) view.findViewById(R.id.channelName);
            name.setText("> ".concat(vc.getName()));
            // No status for voice channel, make it blank
            TextView status = (TextView) view.findViewById(R.id.channelStatus);
            status.setText("");
            status.setBackgroundResource(android.R.color.transparent);
            // Users in voice channel
            LinearLayout layout = (LinearLayout) view.findViewById(R.id.voiceUserLayout);
            layout.removeAllViews();
            for(User u : vc.getGuild().getUsers()) {
                if(!vc.equals(u.getVoiceChannel())) continue;
                View child = mInflater.inflate(R.layout.list_item_voiceuser, null);
                ImageView userAvatar = (ImageView) child.findViewById(R.id.userAvatar);
                TextView userName = (TextView) child.findViewById(R.id.userName);
                ImageView micMute = (ImageView) child.findViewById(R.id.micMute);
                ImageView speakerMute = (ImageView) child.findViewById(R.id.speakerMute);
                getAvatarOrIcon(userAvatar, u.getID(), u.getAvatarURL());
                userName.setText(u.getName());
                if(u.getVoiceState().isMute()) {
                    micMute.setImageResource(R.drawable.mic_mute);
                }
                if(u.getVoiceState().isDeaf()) {
                    speakerMute.setImageResource(R.drawable.speaker_deafen);
                }
                if(u.isSpeaking()) {
                    userAvatar.setBackgroundColor(Color.GREEN);
                }
                layout.addView(child);
            }
        } else {
            // Name
            TextView name = (TextView) view.findViewById(R.id.channelName);
            name.setText("# ".concat(channel.getName()));
            // Notifications
            TextView status = (TextView) view.findViewById(R.id.channelStatus);
            if(channel.getMentionCount() > 0) {
                status.setText(channel.getMentionCount());
                status.setBackgroundColor(Color.RED);
            } else {
                status.setText("");
                status.setBackgroundResource(android.R.color.transparent);
            }
        }
        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return mIsPrivate ? R.layout.list_item_guild : R.layout.list_item_channel;
    }

    // Same view for all items
    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }
}
