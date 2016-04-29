package zone.pumpkinhill.discard.activity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.adapter.GuildListAdapter;
import zone.pumpkinhill.discard.task.NetworkTask;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.MessageReceivedEvent;
import zone.pumpkinhill.discord4droid.handle.events.ReadyEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Message;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;

public class GuildListActivity extends BaseActivity {
    private final static String TAG = GuildListActivity.class.getCanonicalName();

    private List<Guild> mGuilds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guild_list);
        showMenuProfile = true;
        ListView guildList = (ListView) findViewById(R.id.guildList);
        if(guildList != null) {
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
        }
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
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ClientHelper.unsubscribe(this);
        new NetworkTask(mContext).execute("logout");
        Intent intent = new Intent(this,LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void notifyMessage(Channel channel) {
        Log.d(TAG, "Notifying for " + channel.getName());
        Guild guild = null;
        String author = "Someone";
        if(channel.isPrivate()) author = ((PrivateChannel)channel).getRecipient().getName();
        else guild = channel.getGuild();
        // TODO: Find better icons
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Discard")
                .setContentText(author + (guild == null ?
                        " sent you a message." : " mentioned you in " +channel.getName() + "."))
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

    // Notify on new messages
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Channel channel = message.getChannel();
        // We only care if it is a mention or DM, and don't currently have the channel open
        if(!channel.isPrivate() && !message.getMentions().contains(ClientHelper.ourUser())) return;
        if(!isAppInBackground && ClientHelper.getActiveChannel() != null &&
                ClientHelper.getActiveChannel().getID().equals(channel.getID())) return;
        notifyMessage(channel);
    }

    // Notify on new messages every few minutes during suspend/wake
    @EventSubscriber
    @SuppressWarnings("unused")
    public void onReady(ReadyEvent event) {
        for(Channel c : event.getClient().getChannels(true)) {
            if(!isAppInBackground && ClientHelper.getActiveChannel() != null &&
                    ClientHelper.getActiveChannel().getID().equals(c.getID())) continue;
            int mentions = c.getMentionCount();
            if(mentions > 0) notifyMessage(c);
        }
    }

    // Only do this once, but keep the others subscribed
    private class OnReadySubscriber {
        @EventSubscriber
        @SuppressWarnings("unused")
        public void onReady(ReadyEvent event) {
            populateTable();
            ClientHelper.unsubscribe(this);
        }
    }
}
