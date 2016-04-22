package zone.pumpkinhill.discard.activity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import zone.pumpkinhill.discard.BuildConfig;
import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.adapter.GuildListAdapter;
import zone.pumpkinhill.discard.task.UpdatePresenceTask;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.MessageReceivedEvent;
import zone.pumpkinhill.discord4droid.handle.events.ReadyEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Message;

public class GuildListActivity extends AppCompatActivity {
    private final static String TAG = GuildListActivity.class.getCanonicalName();

    private Context mContext = this;
    private List<Guild> mGuilds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guild_list);
        ListView guildList = (ListView) findViewById(R.id.guildList);
        guildList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long idk) {
                // Pass selected guild ID to ChatActivity
                String guildId = String.valueOf(adapter.getItemIdAtPosition(position));
                Intent i = new Intent(mContext, ChatActivity.class)
                        .putExtra("guildId", guildId);
                startActivity(i);
            }
        });
        if(ClientHelper.isReady()) {
            populateTable();
        } else {
            ClientHelper.subscribe(new OnReadySubscriber());
        }
        ClientHelper.subscribe(this);
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
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Channel channel = message.getChannel();
        // We only care if it is a mention or DM, and don't currently have the channel open
        if(!channel.isPrivate() && !message.getMentions().contains(ClientHelper.ourUser())) return;
        if(ClientHelper.getActiveChannel() != null &&
                ClientHelper.getActiveChannel().getID().equals(channel.getID())) return;
        Guild guild = channel.getGuild();
        // TODO: Find better icons
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(
                        message.getAuthor().getName() + (guild == null ?
                                " sent you a message." : " mentioned you."))
                .setContentText(message.getContent())
                .setAutoCancel(true);
        Intent i = new Intent(this, ChatActivity.class)
                .putExtra("guildId", guild == null ? "0" : guild.getID())
                .putExtra("channelId", channel.getID());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
                .addParentStack(ChatActivity.class)
                .addNextIntent(i);
        PendingIntent pi = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pi);
        ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(0, builder.build());
    }

    // Only do this once, but keep the others subscribed
    private class OnReadySubscriber {
        @EventSubscriber
        public void onReady(ReadyEvent event) {
            populateTable();
            ClientHelper.unsubscribe(this);
        }
    }
}
