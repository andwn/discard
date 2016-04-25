package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.task.ImageDownloaderTask;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Presences;
import zone.pumpkinhill.discord4droid.handle.obj.User;

public class UserListAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final Guild mGuild;
    private final boolean mOnline;
    private List<User> mUserList;

    public UserListAdapter(Context context, Guild guild, boolean online) {
        mInflater = LayoutInflater.from(context);
        mGuild = guild;
        mOnline = online;
        mUserList = populateUserList(mGuild, mOnline);
    }

    private static List<User> populateUserList(Guild guild, boolean online) {
        List<User> users = new ArrayList<>();
        for(User u : guild.getUsers()) {
            if((online && u.getPresence() != Presences.OFFLINE) ||
                    (!online && u.getPresence() == Presences.OFFLINE)) {
                users.add(u);
            }
        }
        return users;
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
        return mUserList.size();
    }

    @Override
    public Object getItem(int position) {
        return mUserList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return Long.parseLong(mUserList.get(position).getID());
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
        User user = mUserList.get(position);
        // Try loading message author's avatar from cache, or start to download it
        ImageView icon = (ImageView) view.findViewById(R.id.guildIcon);
        String iconURL = user.getAvatarURL();
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
        name.setText(user.getName());
        // Online status
        TextView status = (TextView) view.findViewById(R.id.statusText);
        status.setText("");
        switch(user.getPresence()) {
            case ONLINE: status.setBackgroundResource(R.color.colorStatusOnline); break;
            case IDLE: status.setBackgroundResource(R.color.colorStatusIdle); break;
            case OFFLINE: status.setBackgroundResource(R.color.colorStatusOffline); break;
        }
        // Now playing
        TextView nowPlaying = (TextView) view.findViewById(R.id.nowPlaying);
        String game = user.getGame();
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
        return mUserList.isEmpty();
    }
}
