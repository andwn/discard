package zone.pumpkinhill.discard.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import zone.pumpkinhill.discard.ClientHelper;
import zone.pumpkinhill.discard.R;
import zone.pumpkinhill.discard.adapter.ChatMessageAdapter;
import zone.pumpkinhill.discard.adapter.PrivateChannelAdapter;
import zone.pumpkinhill.discard.adapter.TextChannelAdapter;
import zone.pumpkinhill.discard.adapter.VoiceChannelAdapter;
import zone.pumpkinhill.discard.task.LoadMessagesTask;
import zone.pumpkinhill.discard.task.SendMessageTask;
import zone.pumpkinhill.discord4droid.api.Event;
import zone.pumpkinhill.discord4droid.api.EventSubscriber;
import zone.pumpkinhill.discord4droid.handle.events.GuildLeaveEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageDeleteEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageReceivedEvent;
import zone.pumpkinhill.discord4droid.handle.events.MessageSendEvent;
import zone.pumpkinhill.discord4droid.handle.events.ReadyEvent;
import zone.pumpkinhill.discord4droid.handle.obj.Channel;
import zone.pumpkinhill.discord4droid.handle.obj.Guild;
import zone.pumpkinhill.discord4droid.handle.obj.PrivateChannel;

public class ChatActivity extends AppCompatActivity {
    private final static String TAG = ChatActivity.class.getCanonicalName();

    private Context mContext = this;
    private Guild mGuild;
    private List<Channel> mChannelList;
    private Channel mChannel;
    private ListView mMessageView;
    private boolean mIsPrivate;

    private Event mEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ClientHelper.subscribe(this);
        String guildId = getIntent().getExtras().getString("guildId");
        String channelId = getIntent().getExtras().getString("channelId");
        if(guildId == null || guildId.equals("0")) {
            // Private chat
            mIsPrivate = true;
            mChannelList = getPrivateChannels();
            if(mChannelList.size() == 0) { // No private channels
                finish();
                return;
            }
            // Setup drawer
            ImageView drIcon = (ImageView) findViewById(R.id.guildIcon);
            drIcon.setImageResource(R.drawable.ic_menu_camera);
            TextView drName = (TextView) findViewById(R.id.guildName);
            drName.setText("Direct Messages");
            TextView drNotify = (TextView) findViewById(R.id.notifyCount);
            drNotify.setText("");
            // Setup adapter for text channel list
            ListView textChannels = (ListView) findViewById(R.id.textChannelList);
            textChannels.setAdapter(new PrivateChannelAdapter(mContext, mChannelList));
            // Setup onItemClick for text channel list
            textChannels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    switchChannel((Channel) parent.getAdapter().getItem(position));
                }
            });
        } else {
            // Guild
            mIsPrivate = false;
            mGuild = ClientHelper.client.getGuildByID(guildId);
            if (mGuild == null) {
                Log.e(TAG, "Something went wrong passing the guild ID to chat activity.");
                finish();
                return;
            }
            mChannelList = mGuild.getChannels();
            if(channelId != null && !channelId.isEmpty()) {
                mChannel = mGuild.getChannelByID(channelId);
            }
            // Drawer
            ImageView drIcon = (ImageView) findViewById(R.id.guildIcon);
            drIcon.setImageBitmap(ClientHelper.getImageFromCache(mGuild.getIconURL()));
            TextView drName = (TextView) findViewById(R.id.guildName);
            drName.setText(mGuild.getName());
            TextView drNotify = (TextView) findViewById(R.id.notifyCount);
            drNotify.setText("");
            // Setup adapter for text channel list
            ListView textChannels = (ListView) findViewById(R.id.textChannelList);
            textChannels.setAdapter(new TextChannelAdapter(mContext, mGuild.getChannels()));
            // Setup onItemClick for text channel list
            textChannels.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    switchChannel((Channel) parent.getAdapter().getItem(position));
                }
            });
            // Setup adapter for voice channel list
            ListView voiceChannels = (ListView) findViewById(R.id.voiceChannelList);
            voiceChannels.setAdapter(new VoiceChannelAdapter(mContext, mGuild.getVoiceChannels()));
        }
        // Fill in message list
        if(mChannel == null) mChannel = mChannelList.get(0);
        mMessageView = (ListView) findViewById(R.id.messageListView);
        if(mMessageView != null) switchChannel(mChannel);
        Button sendButton = (Button) findViewById(R.id.sendButton);
        if(sendButton != null) {
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText msg = (EditText) findViewById(R.id.editMessage);
                    if(msg != null && !msg.getText().toString().isEmpty()) {
                        new SendMessageTask(msg, mMessageView).execute(mChannel.getID());
                    }
                }
            });
        }
    }

    private List<Channel> getPrivateChannels() {
        List<Channel> channelList = new ArrayList<>();
        for(Channel c : ClientHelper.client.getChannels(true)) {
            if(!(c instanceof PrivateChannel)) continue;
            if(mChannel == null) mChannel = c;
            channelList.add(c);
        }
        return channelList;
    }

    private void switchChannel(Channel newChannel) {
        mChannel = newChannel;
        ClientHelper.setActiveChannel(newChannel);
        mMessageView.setAdapter(new ChatMessageAdapter(mContext, mChannel.getMessages()));
        new LoadMessagesTask(mChannel.getMessages(), mMessageView).execute(mChannel.getID());
        setTitle(mChannel.getName());
    }

    @Override
    public void finish() {
        super.finish();
        ClientHelper.unsubscribe(this);
        ClientHelper.setActiveChannel(null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this,GuildListActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        menu.findItem(R.id.menu_preferences).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(mContext, SettingsActivity.class);
                startActivity(i);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event) {
        if(!event.getMessage().getChannel().equals(mChannel)) return;
        mEvent = event;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChannel.getMessages().add(((MessageReceivedEvent)mEvent).getMessage());
                ((ChatMessageAdapter) mMessageView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    @EventSubscriber
    public void onMessageSent(MessageSendEvent event) {
        if(!event.getMessage().getChannel().equals(mChannel)) return;
        mEvent = event;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChannel.getMessages().add(((MessageSendEvent)mEvent).getMessage());
                ((ChatMessageAdapter) mMessageView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    @EventSubscriber
    public void onMessageDelete(MessageDeleteEvent event) {
        if(!event.getMessage().getChannel().equals(mChannel)) return;
        mEvent = event;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChannel.getMessages().remove(((MessageDeleteEvent)mEvent).getMessage());
                ((ChatMessageAdapter) mMessageView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    // This is to refresh the channel after resuming from suspend (and reconnecting websocket)
    @EventSubscriber
    public void onReady(ReadyEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchChannel(mChannel);
            }
        });
    }
}
