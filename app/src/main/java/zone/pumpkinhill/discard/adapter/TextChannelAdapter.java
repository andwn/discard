package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;

public class TextChannelAdapter extends BaseAdapter {
    private List<Channel> mChannels;
    private LayoutInflater mInflater;

    public TextChannelAdapter(Context context, List<Channel> channels) {
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
        Channel channel = mChannels.get(position);
        // No icons for guild channels
        ImageView icon = (ImageView) view.findViewById(R.id.guildIcon);
        icon.setImageResource(android.R.color.transparent);
        // Fill in the text
        TextView name = (TextView) view.findViewById(R.id.guildName);
        name.setText("# " + channel.getName());
        TextView discriminator = (TextView) view.findViewById(R.id.notifyCount);
        discriminator.setText("[?]");
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
