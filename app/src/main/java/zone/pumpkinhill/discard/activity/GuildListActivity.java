package zone.pumpkinhill.discard.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
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
import zone.pumpkinhill.discard.task.EventSetupTask;
import zone.pumpkinhill.discard.task.UpdatePresenceTask;
import zone.pumpkinhill.discord4droid.api.DiscordClient;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.DiscordDisconnectedEvent;
import zone.pumpkinhill.discord4droid.handle.events.ReadyEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.User;

public class GuildListActivity extends AppCompatActivity {
    private List<Guild> mGuilds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guild_list);
        ClientHelper.subscribe(this);
        ListView guildList = (ListView) findViewById(R.id.guildList);
        guildList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long idk) {
                Intent i = new Intent(getApplicationContext(), ChatActivity.class);
                Bundle b = new Bundle();
                b.putString("guildId", String.valueOf(adapter.getItemIdAtPosition(position)));
                i.putExtras(b);
                startActivity(i);
                ClientHelper.unsubscribe(this);
            }
        });
    }

    @EventSubscriber
    public void onReady(ReadyEvent event) {
        new EventSetupTask().execute();
        DiscordClient client = event.getClient();
        List<Guild> guilds = client.getGuilds();
        this.mGuilds = guilds;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Fill in message list
                    ListView guildList = (ListView) findViewById(R.id.guildList);
                    if(guildList == null) return;
                    guildList.setAdapter(new GuildListAdapter(getApplicationContext(), mGuilds));
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
        if(BuildConfig.DEBUG) {
            new UpdatePresenceTask(null, "Android Studio").execute();
        }
    }

    //@EventSubscriber
    //public void onDisconnect(DiscordDisconnectedEvent event) {
    //    Log.i("onDisconnect", "Disconnected, back to login.");
    //    ClientHelper.abandonClient();
    //    finish();
    //}

    //@EventSubscriber
    //public void onMessage(MessageReceivedEvent event) {
    //    Message message = event.getMessage();
        //ClientHelper.newChatMessage(message.getChannel().getID(), message);
        //ListView msgList = (ListView) findViewById(R.id.chatMessages);

    //}
}
