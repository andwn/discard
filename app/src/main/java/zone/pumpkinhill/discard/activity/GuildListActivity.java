package zone.pumpkinhill.discard.activity;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;
import java.util.concurrent.ExecutionException;

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
        assert guildList != null;
        registerForContextMenu(guildList);
        guildList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long idk) {
                // Pass selected guild ID to ChatActivity
                String guildId = String.valueOf(adapter.getItemIdAtPosition(position));
                if(guildId.equals("1")) { // Create new guild
                    openContextMenu(v);
                } else { // Open the chat/guild
                    Intent i = new Intent(mContext, ChatActivity.class)
                            .putExtra("guildId", guildId);
                    startActivity(i);
                }
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

    @Override
    public void onResume() {
        super.onResume();
        ListView guildList = (ListView) findViewById(R.id.guildList);
        assert guildList != null;
        if(guildList.getAdapter() != null) {
            ((GuildListAdapter) guildList.getAdapter()).notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId() == R.id.guildList) {
            ListView lv = (ListView) v;
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            if(info.position == 0) return;
            Guild guild = (Guild) lv.getItemAtPosition(info.position);
            if(guild == null) {
                menu.setHeaderTitle("New Guild");
                menu.add(0, 0, 0, "Join");
                menu.add(0, 1, 1, "Create");
            } else {
                menu.setHeaderTitle(guild.getName());
                if (ClientHelper.client.getOurUser().getID().equals(guild.getOwnerID())) {
                    menu.add(0, 2, 2, "Edit Guild");
                    menu.add(0, 3, 3, "Delete Guild");
                } else {
                    menu.add(0, 4, 4, "Leave Guild");
                }
            }
        }
    }

    private void showGuildJoinDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle("Join Guild");
        alert.setMessage("Enter the invite link.");
        final EditText textBox = new EditText(mContext);
        textBox.setHint("discord.gg/my_invite_code");
        alert.setView(textBox);
        alert.setPositiveButton("Join", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inviteCode = textBox.getText().toString();
                if(inviteCode.isEmpty()) return;
                NetworkTask task = new NetworkTask(mContext);
                task.execute("join-guild", inviteCode);
                // Wait for it to finish
                // FIXME: This blocks the UI thread, use callback instead
                boolean result;
                try {
                    result = task.get();
                } catch(ExecutionException | InterruptedException e) {
                    return;
                }
                // If it succeeded close the dialog and go to the new guild
                if(!result) return;
                dialog.dismiss();
                Intent i = new Intent(mContext, ChatActivity.class);
                i.putExtra("guildId", mGuilds.get(mGuilds.size() - 1).getID());
                startActivity(i);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(info.position == mGuilds.size() + 1) {
            switch (item.getItemId()) {
                case 0: // Join
                    showGuildJoinDialog();
                    return true;
                case 1: // Create
                    Intent i = new Intent(mContext, EditGuildActivity.class);
                    startActivity(i);
                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        } else {
            Guild guild = mGuilds.get(info.position - 1);
            switch (item.getItemId()) {
                case 2: // Edit

                    return true;
                case 3: // Delete

                    return true;
                case 4: // Leave

                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        }
    }

    private void notifyMessage(Channel channel) {
        if(!mPref.getBoolean("notifications_new_message", true)) return;
        // So we don't vibrate every 5 minutes for the same message
        if(NotifiedChannels.contains(channel)) return;
        NotifiedChannels.add(channel);
        Log.d(TAG, "Notifying for " + channel.getName());
        // Figure out if this is from a guild or private channel
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
        // Setup the vibration, ringtone, and light
        if(mPref.getBoolean("notifications_new_message_vibrate", true)) {
            builder.setVibrate(new long[]{0, 150, 100, 150});
        }
        String ringtone = mPref.getString("notifications_new_message_ringtone", null);
        if(ringtone != null) {
            builder.setSound(Uri.parse(ringtone));
        }
        if(mPref.getBoolean("notifications_new_message_light", true)) {
            builder.setLights(Color.argb(0xFF, 0x72, 0x89, 0xDA), 3000, 3000);
        }
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
