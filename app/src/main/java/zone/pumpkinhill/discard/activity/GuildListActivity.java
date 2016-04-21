package zone.pumpkinhill.discard.activity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
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
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.MessageReceivedEvent;
import zone.pumpkinhill.discord4droid.handle.events.ReadyEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.Message;

public class GuildListActivity extends AppCompatActivity {
    private final static String TAG = GuildListActivity.class.getCanonicalName();

    private Context mContext = this;
    private List<Guild> mGuilds;
    private int mNotifyId;

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
        Log.d(TAG, "Received Message..");
        String to = "";
        Message msg = event.getMessage();
        if(msg.mentionsHere()) {
            // Mention @here
            to = "@here";
        } else if(msg.mentionsEveryone()) {
            // Mention to everyone
            to = "@everyone";
        } else if(msg.getMentions().contains(ClientHelper.ourUser())) {
            // Explicit mention
            to = "you";
        } else  if(msg.getChannel().isPrivate()) {
            // Private message
            to = "you";
        } else {
            return;
        }
        Log.d(TAG, "Building Notification..");
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_menu_send)
                        .setContentTitle("Message from " + msg.getAuthor().getName() + " to " + to)
                        .setContentText(msg.getContent());
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, GuildListActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(GuildListActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mNotifyId, mBuilder.build());
        Log.d(TAG, "Notified.");
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
