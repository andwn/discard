package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.task.ImageDownloaderTask;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;

public class GuildListAdapter extends BaseAdapter {
    private List<Guild> mGuilds;
    private LayoutInflater mInflater;

    public GuildListAdapter(Context context, List<Guild> guilds) {
        mGuilds = guilds;
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
        return mGuilds.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        return position == 0 ? null : mGuilds.get(position - 1);
    }

    @Override
    public long getItemId(int position) {
        return position == 0 ? 0 : Long.parseLong(mGuilds.get(position - 1).getID());
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
        if(position == 0) {
            ImageView icon = (ImageView) view.findViewById(R.id.guildIcon);
            icon.setImageResource(R.drawable.ic_menu_camera);
            TextView name = (TextView) view.findViewById(R.id.guildName);
            name.setText("Direct Messages");
            return view;
        }
        Guild guild = mGuilds.get(position - 1);
        // Try loading message author's avatar from cache, or start to download it
        ImageView icon = (ImageView) view.findViewById(R.id.guildIcon);
        String iconURL = guild.getIconURL();
        if(iconURL != null && !iconURL.isEmpty()) {
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
        name.setText(guild.getName());
        // TODO: Figure out how to get a list of unread messages/mentions
        TextView status = (TextView) view.findViewById(R.id.statusText);

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
        return false;
    }
}
