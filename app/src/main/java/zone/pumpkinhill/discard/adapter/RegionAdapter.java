package zone.pumpkinhill.discard.adapter;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.List;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.task.NetworkTask;
import zone.pumpkinhill.discord4droid.handle.obj.Region;

public class RegionAdapter extends DiscordAdapter implements SpinnerAdapter, Handler.Callback {
    private NetworkTask mGetRegionsTask = null;
    private List<Region> mRegions;

    public RegionAdapter(Context context) {
        super(context);
        mGetRegionsTask = new NetworkTask(mContext);
        mGetRegionsTask.callWhenFinished(this);
        mGetRegionsTask.execute("get-regions");
    }

    @Override
    public int getCount() {
        return mRegions == null ? 0 : mRegions.size();
    }

    @Override
    public Object getItem(int position) {
        return mRegions == null ? null : mRegions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.list_item_guild, parent, false);
        }
        ImageView icon = (ImageView) view.findViewById(R.id.guildIcon);
        TextView name = (TextView) view.findViewById(R.id.guildName);
        name.setText(mRegions == null ? "Loading" : mRegions.get(position).getName());
        return view;
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {
        mRegions = ClientHelper.client.getLoadedRegions();
        notifyDataSetChanged();
        return true;
    }
}
