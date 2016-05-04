package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Role;
import zone.pumpkinhill.discord4droid.handle.obj.User;

public class UserListAdapter extends DiscordAdapter {
    private final Guild mGuild;
    private List<User> mUserList;

    public UserListAdapter(Context context, Guild guild) {
        super(context);
        mGuild = guild;
        mUserList = populateUserList(mGuild);
    }

    private static List<User> populateUserList(Guild guild) {
        List<User> users = new ArrayList<>();
        for(User u : guild.getUsers()) users.add(u);
        return users;
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.list_item_guild, parent, false);
        }
        User user = mUserList.get(position);
        // Try loading message author's avatar from cache, or start to download it
        ImageView icon = (ImageView) view.findViewById(R.id.guildIcon);
        getAvatarOrIcon(icon, user.getID(), user.getAvatarURL());
        // Fill in the text
        TextView name = (TextView) view.findViewById(R.id.guildName);
        name.setText(user.getName());
        List<Role> roles = user.getRolesForGuild(mGuild);
        int color = Color.BLACK; // 0xFF000000
        for(Role r : roles) {
            if(r.getColor() != 0) {
                // Discord colors are RGB, not ARGB, so we "add" to the opaque black
                color += r.getColor();
                break;
            }
        }
        name.setTextColor(color);
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
