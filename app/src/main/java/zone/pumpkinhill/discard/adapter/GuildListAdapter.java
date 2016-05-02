package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.task.ImageDownloaderTask;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;

public class GuildListAdapter extends DiscordAdapter {
    private List<Guild> mGuilds;

    public GuildListAdapter(Context context, List<Guild> guilds) {
        super(context);
        mGuilds = guilds;
    }

    @Override
    public int getCount() {
        return mGuilds.size() + 2;
    }

    @Override
    public Object getItem(int position) {
        if(position == 0 || position == getCount() - 1) return null;
        return mGuilds.get(position - 1);
    }

    @Override
    public long getItemId(int position) {
        if(position == 0) return 0;
        else if(position == getCount() - 1) return 1;
        return Long.parseLong(mGuilds.get(position - 1).getID());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.list_item_guild, parent, false);
        }
        ImageView icon = (ImageView) view.findViewById(R.id.guildIcon);
        TextView name = (TextView) view.findViewById(R.id.guildName);
        TextView status = (TextView) view.findViewById(R.id.statusText);
        // Blank this in case of scrolling
        status.setBackgroundResource(android.R.color.transparent);
        status.setText("");
        if(position == 0) { // First is DMs
            icon.setImageResource(android.R.drawable.sym_action_email);
            name.setText("Direct Messages");
            int mentions = 0;
            for(Channel c : ClientHelper.client.getChannels(true)) {
                if(c.isPrivate()) mentions += c.getMentionCount();
            }
            if(mentions > 0) {
                status.setBackgroundColor(Color.RED);
                status.setText(String.valueOf(mentions));
            }
            return view;
        } else if(position == getCount() - 1) { // Last is "New Guild"
            icon.setImageResource(android.R.drawable.ic_menu_add);
            name.setText("New Guild");
            return view;
        }
        Guild guild = mGuilds.get(position - 1);
        // Try loading message author's avatar from cache, or start to download it
        getAvatarOrIcon(icon, guild.getID(), guild.getIconURL());
        // Fill in the text
        name.setText(guild.getName());
        // Unread mentions indicator
        int mentions = 0;
        for(Channel c : guild.getChannels()) {
            mentions += c.getMentionCount();
        }
        if(mentions > 0) {
            status.setBackgroundColor(Color.RED);
            status.setText(String.valueOf(mentions));
        }
        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.list_item_guild;
    }
}
