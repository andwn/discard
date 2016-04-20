package zone.pumpkinhill.discard.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import zone.pumpkinhill.discard.BuildConfig;
import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.adapter.GuildListAdapter;
import zone.pumpkinhill.discard.task.UpdatePresenceTask;
import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.ReadyEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;

public class GuildListActivity extends AppCompatActivity {
    private final static String TAG = GuildListActivity.class.getCanonicalName();

    private Context mContext = this;
    private List<Guild> mGuilds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guild_list);
        ListView guildList = (ListView) findViewById(R.id.guildList);
        if(guildList == null) {
            Log.e(TAG, "Couldn't find guildList view.");
            return;
        }
        guildList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long idk) {
                Bundle b = new Bundle();
                b.putString("guildId", String.valueOf(adapter.getItemIdAtPosition(position)));
                Intent i = new Intent(mContext, ChatActivity.class);
                i.putExtras(b);
                startActivity(i);
            }
        });
        if(ClientHelper.isReady()) {
            populateTable();
        } else {
            ClientHelper.subscribe(this);
        }
    }

    private void populateTable() {
        mGuilds = ClientHelper.client.getGuilds();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Fill in message list
                    ListView guildList = (ListView) findViewById(R.id.guildList);
                    if(guildList == null) return;
                    guildList.setAdapter(new GuildListAdapter(mContext, mGuilds));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
        if(BuildConfig.DEBUG) {
            new UpdatePresenceTask(null, "Android Studio").execute();
        }
    }

    @Override
    public void finish() {
        super.finish();
        ClientHelper.unsubscribe(this);
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        populateTable();
        ClientHelper.unsubscribe(this);
    }
}
